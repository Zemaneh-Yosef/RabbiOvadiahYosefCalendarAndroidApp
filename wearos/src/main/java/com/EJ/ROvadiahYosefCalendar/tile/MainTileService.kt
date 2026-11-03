package com.EJ.ROvadiahYosefCalendar.tile

import android.content.Context
import android.content.SharedPreferences
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.classes.LocationResolver
import com.EJ.ROvadiahYosefCalendar.classes.ROZmanimCalendar
import com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntry
import com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment
import com.EJ.ROvadiahYosefCalendar.classes.Utils
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val RESOURCES_VERSION = "1"
class MainTileService : TileService() {

    private lateinit var sharedPref: SharedPreferences
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation(), null)
    private var mJewishDateInfo = JewishDateInfo(false)

    override fun onCreate() {
        super.onCreate()
        sharedPref = getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> =
        getNextUpcomingZman(applicationContext)?.zman?.let {
            Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(it.toEpochMilli() - Instant.now().toEpochMilli() + 100) // add 100 just in case
                .setTileTimeline(Timeline.fromLayoutElement(
                    Text.Builder(applicationContext, getNextUpcomingZmanAsString(applicationContext))
                        .setModifiers(
                            ModifiersBuilders.Modifiers
                                .Builder()
                                .setClickable(
                                    Clickable.Builder()
                                        .setId("launchApp")
                                        .setOnClick(
                                            ActionBuilders.LaunchAction.Builder()
                                                .setAndroidActivity(ActionBuilders.AndroidActivity.Builder()
                                                    .setPackageName(applicationContext.packageName)
                                                    .setClassName("com.EJ.ROvadiahYosefCalendar.presentation.MainActivity").build())
                                                .build()
                                        )
                                        .build()
                                ).build()
                        )
                        .setTypography(Typography.TYPOGRAPHY_BUTTON)
                        .setMaxLines(10)
                        .setColor(argb(0xFFFFFFFF.toInt()))
                        .build())
                ).build()
        }?.let {
            Futures.immediateFuture(
                it
            )
        }!!

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build())

    private fun getNextUpcomingZmanAsString(context: Context?): String {
        val theZman: ZmanListEntry? = getNextUpcomingZman(context)

        if (theZman != null) {
            val zmanTime = Utils.formatZmanTime(baseContext, theZman)

            if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
                return theZman.title + "\n\n" + zmanTime
            }
            return theZman.title + "\n\nis at\n\n" + zmanTime
        }
        return ""
    }

    private fun getNextUpcomingZman(context: Context?): ZmanListEntry? {
        if (context != null) {
            sharedPref = context.getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)
        }
        mROZmanimCalendar = ROZmanimCalendar(LocationResolver.getLastGeoLocation(sharedPref), sharedPref)
        mROZmanimCalendar.candleLightingOffset =
            sharedPref.getString("CandleLightingOffset", "20")?.toDoubleOrNull() ?: 20.0
        mROZmanimCalendar.ateretTorahSunsetOffset =
            sharedPref.getString("EndOfShabbatOffset", null)?.toDoubleOrNull()
                ?: if (sharedPref.getBoolean("inIsrael", false)) 30.0 else 40.0
        if (sharedPref.getBoolean("inIsrael", false) && sharedPref.getString(
                "EndOfShabbatOffset",
                "40"
            ) == "40"
        ) {
            mROZmanimCalendar.ateretTorahSunsetOffset = 30.0
        }

        var sUserIsOffline = false
        var elevation: Double
        if (mROZmanimCalendar.geoLocation.locationName.contains("Lat:") && mROZmanimCalendar.geoLocation.locationName.contains(
                "Long:"
            )
            && sharedPref.getBoolean("SetElevationToLastKnownLocation", false)
        ) { //only if the user has enabled the setting to set the elevation to the last known location
            sUserIsOffline = true
            elevation =
                sharedPref.getString("elevation" + sharedPref.getString("name", ""), "0")
                    ?.toDouble() ?: 0.0 //lastKnownLocation
        } else { //user is online, get the elevation from the shared preferences for the current location
            elevation =
                sharedPref.getString("elevation${mROZmanimCalendar.geoLocation.locationName}", "0")
                    ?.toDouble()
                    ?: 0.0 //get the last value of the current location or 0 if it doesn't exist
        }
        if (!sUserIsOffline && sharedPref.getBoolean(
                "useElevation",
                true
            ) && !sharedPref.getBoolean("LuachAmudeiHoraah", false)
        ) { //update if the user is online and the elevation setting is enabled
            elevation =
                sharedPref.getString("elevation${mROZmanimCalendar.geoLocation.locationName}", "0")
                    ?.toDouble() ?: 0.0
        }
        if (!sharedPref.getBoolean(
                "useElevation",
                true
            )
        ) { //if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0.0
        }
        mROZmanimCalendar.geoLocation.elevation = elevation.coerceAtLeast(0.0) //GeoLocation.setElevation rejects negative values

        mJewishDateInfo.resetLocale(context)
        mJewishDateInfo.jewishCalendar.inIsrael = sharedPref.getBoolean("inIsrael", false)

        return ZmanimFactory.getNextUpcomingZman(LocalDate.now(), mROZmanimCalendar, mJewishDateInfo, sharedPref)
    }

}

