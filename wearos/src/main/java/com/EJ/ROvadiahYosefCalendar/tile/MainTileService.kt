package com.EJ.ROvadiahYosefCalendar.tile

import android.content.Context
import android.content.SharedPreferences
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.LoadAction
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Border
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.classes.LocationResolver
import com.EJ.ROvadiahYosefCalendar.classes.ROZmanimCalendar
import com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntry
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory.addZmanim
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimNames
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val RESOURCES_VERSION = "1"
class MainTileService : TileService() {

    private lateinit var sharedPref: SharedPreferences
    private var mCurrentDateShown = Calendar.getInstance()
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation(), null)
    private var mJewishDateInfo = JewishDateInfo(false)
    private lateinit var zmanimFormat: SimpleDateFormat
    private lateinit var visibleSunriseFormat: SimpleDateFormat
    private lateinit var roundUpFormat: SimpleDateFormat

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
            val zmanTime: String =
                if (theZman.isVisibleSunriseZman) {
                    visibleSunriseFormat.format(theZman.zman)
                } else if (theZman.isRTZman) { // we already checked and set if the rounded up format should be used
                    roundUpFormat.format(theZman.zman)
                } else { // just format it normally
                    zmanimFormat.format(theZman.zman)
                }

            if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
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

        zmanimFormat = if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
            if (sharedPref.getBoolean("ShowSeconds", false)) {
                SimpleDateFormat("H:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("H:mm", Locale.getDefault())
            }
        } else {
            if (sharedPref.getBoolean("ShowSeconds", false)) {
                SimpleDateFormat("h:mm:ss aa", Locale.getDefault())
            } else {
                SimpleDateFormat("h:mm aa", Locale.getDefault())
            }
        }
        zmanimFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        visibleSunriseFormat =
            if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                SimpleDateFormat("H:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("h:mm:ss aa", Locale.getDefault())
            }
        visibleSunriseFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
            roundUpFormat = SimpleDateFormat("H:mm", Locale.getDefault())
            roundUpFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone
        } else {
            roundUpFormat = SimpleDateFormat("h:mm aa", Locale.getDefault())
            roundUpFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone
        }
        roundUpFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        var theZman: ZmanListEntry? = null
        val zmanim: MutableList<ZmanListEntry> = java.util.ArrayList()
        val today = Calendar.getInstance()
        today.add(Calendar.DATE, -1)
        mROZmanimCalendar.calendar = today
        mJewishDateInfo.setCalendar(today)
        addZmanim(zmanim, false, sharedPref, sharedPref, mROZmanimCalendar, mJewishDateInfo, sharedPref.getBoolean("isZmanimInHebrew", false), sharedPref.getBoolean("isZmanimEnglishTranslated", false), true) //for the previous day
        today.add(Calendar.DATE, 1)
        mROZmanimCalendar.calendar = today
        mJewishDateInfo.setCalendar(today)
        addZmanim(zmanim, false, sharedPref, sharedPref, mROZmanimCalendar, mJewishDateInfo, sharedPref.getBoolean("isZmanimInHebrew", false), sharedPref.getBoolean("isZmanimEnglishTranslated", false), true) //for the current day
        today.add(Calendar.DATE, 1)
        mROZmanimCalendar.calendar = today
        mJewishDateInfo.setCalendar(today)
        addZmanim(zmanim, false, sharedPref, sharedPref, mROZmanimCalendar, mJewishDateInfo, sharedPref.getBoolean("isZmanimInHebrew", false), sharedPref.getBoolean("isZmanimEnglishTranslated", false), true) //for the next day
        mROZmanimCalendar.calendar = mCurrentDateShown
        mJewishDateInfo.setCalendar(mCurrentDateShown) //reset
        //find the next upcoming zman that is after the current time and before all the other zmanim
        for (zmanEntry in zmanim) {
            val zman: Date? = zmanEntry.zman
            if (zman != null && zman.after(Date()) && (theZman == null || zman.before(theZman.zman))) {
                theZman = zmanEntry
            }
        }
        return theZman
    }

}

