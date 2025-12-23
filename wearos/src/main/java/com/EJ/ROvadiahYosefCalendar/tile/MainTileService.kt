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
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val RESOURCES_VERSION = "1"
class MainTileService : TileService() {

    private lateinit var sharedPref: SharedPreferences
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation(), null)
    private var mJewishDateInfo = JewishDateInfo(false)
    private lateinit var noSecondFormat: SimpleDateFormat
    private lateinit var yesSecondFormat: SimpleDateFormat
    private var showSeconds = false

    override fun onCreate() {
        super.onCreate()
        sharedPref = getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> =
        getNextUpcomingZman(applicationContext)?.zman?.let {
            Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(it.time - Date().time + 100) // add 100 just in case
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
            val zmanTime: String
            if (showSeconds || theZman.secondTreatment == SecondTreatment.ALWAYS_DISPLAY) {
                zmanTime = yesSecondFormat.format(theZman.zman)
            } else {
                val calendar = Calendar.getInstance()
                calendar.time = theZman.zman

                var zmanDate: Date = theZman.zman
                if (calendar[Calendar.SECOND] > 40 || calendar[Calendar.SECOND] > 20 && theZman.secondTreatment === SecondTreatment.ROUND_LATER) {
                    zmanDate = Utils.addMinuteToZman(theZman.zman)
                }

                zmanTime = noSecondFormat.format(zmanDate)
            }

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
            (sharedPref.getString("CandleLightingOffset", "20")?.toDouble() ?: 0) as Double
        mROZmanimCalendar.ateretTorahSunsetOffset = (sharedPref.getString(
            "EndOfShabbatOffset",
            if (sharedPref.getBoolean("inIsrael", false)) "30" else "40"
        )?.toDouble() ?: 0) as Double
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
        mROZmanimCalendar.geoLocation.elevation = elevation

        var secondFormatPattern = "H:mm:ss"
        if (!Utils.isLocaleHebrew()) {
            secondFormatPattern = "h:mm:ss aa"
        }
        yesSecondFormat = SimpleDateFormat(secondFormatPattern, Locale.getDefault())
        yesSecondFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        showSeconds = sharedPref.getBoolean("ShowSeconds", false)

        var noSecondFormatPattern = "H:mm"
        if (!Utils.isLocaleHebrew()) {
            noSecondFormatPattern = "h:mm aa"
        }
        noSecondFormat = SimpleDateFormat(noSecondFormatPattern, Locale.getDefault())
        noSecondFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        return ZmanimFactory.getNextUpcomingZman(Calendar.getInstance(), mROZmanimCalendar, mJewishDateInfo, sharedPref)
    }

}

