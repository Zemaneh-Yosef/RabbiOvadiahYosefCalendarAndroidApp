package com.EJ.ROvadiahYosefCalendar.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.classes.LocationResolver
import com.EJ.ROvadiahYosefCalendar.classes.ROZmanimCalendar
import com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntry
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory.addZmanim
import com.EJ.ROvadiahYosefCalendar.classes.secondTreatment
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class NextZmanComplicationService : SuspendingComplicationDataSourceService() {

    private lateinit var sharedPref: SharedPreferences
    private var mCurrentDateShown = Calendar.getInstance()
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation(), null)
    private var mJewishDateInfo = JewishDateInfo(false)
    private lateinit var noSecondFormat: SimpleDateFormat
    private lateinit var yesSecondFormat: SimpleDateFormat
    private var showSeconds = false

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        getComplicationData()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.RANGED_VALUE) {
            return null
        }
        return getComplicationData()
    }

    private fun getComplicationData(): ComplicationData {
        val monochromaticImage: MonochromaticImage?
        val minValue = 0
        val maxValue = 100
        val currentTime = Date().time
        val nextZmanTime = getNextUpcomingZman(applicationContext)?.zman?.time ?: 0
        val totalTime = nextZmanTime - currentTime // time in milliseconds until next zman
        val totalTimeInSeconds = totalTime / 1000
        var totalTimeInMinutes = totalTimeInSeconds / 60 // if it's >= 100 minutes, show full bar

        if (totalTimeInMinutes > 100) {
            totalTimeInMinutes = 100
        }

        val text = PlainComplicationText.Builder(
            text = getNextUpcomingZmanTimeAsString(applicationContext)
        ).build()

        monochromaticImage = MonochromaticImage.Builder(
            image = Icon.createWithResource(this, R.drawable.baseline_av_timer_24),
        ).setAmbientImage(
            ambientImage = Icon.createWithResource(
                this,
                R.drawable.baseline_av_timer_24_burn_protect,
            ),
        ).build()

        // Create a content description that includes the value information
        val contentDescription = PlainComplicationText.Builder(
            text = "$totalTimeInMinutes complete until next zman."
        ).build()

        return RangedValueComplicationData.Builder(
            value = totalTimeInMinutes.toFloat(),
            min = minValue.toFloat(),
            max = maxValue.toFloat(),
            contentDescription = contentDescription,
        )
            .setText(text)
            .setMonochromaticImage(monochromaticImage)
            .setTitle(getNextUpcomingZman(applicationContext)?.let {
                PlainComplicationText.Builder(text = it.title
                    .replace("סוף זמן ", "")
                    .replace("Earliest ","")
                    .replace("Sof Zeman ", "")
                    .replace("Ha'Kokhavim", "")
                    .replace("Latest ", "")).build()
            })
            .setTapAction(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    private fun getNextUpcomingZmanTimeAsString(context: Context?): String {
        val theZman: ZmanListEntry? = getNextUpcomingZman(context)

        if (theZman != null) {
            val zmanTime: String
            if (showSeconds || theZman.secondTreatment == secondTreatment.ALWAYS_DISPLAY)
                zmanTime = yesSecondFormat.format(theZman.zman)
            else {
                // I would normally use the internal .getSeconds() function on the Date itself
                // but Android Studio complains that it's deprecated
                // https://stackoverflow.com/a/70448399
                val calendar = Calendar.getInstance()
                calendar.time = theZman.zman

                if (calendar[Calendar.SECOND] > 40 || calendar[Calendar.SECOND] > 20 && theZman.secondTreatment === secondTreatment.ROUND_LATER)
                    calendar.add(Calendar.MINUTE, 1)

                val zmanDate = calendar.time
                zmanTime = noSecondFormat.format(zmanDate)
            }
            return zmanTime
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
        if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew")
            secondFormatPattern = secondFormatPattern.lowercase() + "aa"
        yesSecondFormat = SimpleDateFormat(secondFormatPattern, Locale.getDefault())
        yesSecondFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        showSeconds = sharedPref.getBoolean("ShowSeconds", false)

        var noSecondFormatPattern = "H:mm"
        if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew")
            noSecondFormatPattern = secondFormatPattern.lowercase() + "aa"
        noSecondFormat = SimpleDateFormat(noSecondFormatPattern, Locale.getDefault())
        noSecondFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

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