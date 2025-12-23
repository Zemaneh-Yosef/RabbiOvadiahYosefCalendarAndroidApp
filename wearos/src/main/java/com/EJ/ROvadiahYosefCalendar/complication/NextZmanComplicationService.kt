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
import com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment
import com.EJ.ROvadiahYosefCalendar.classes.Utils
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class NextZmanComplicationService : SuspendingComplicationDataSourceService() {

    private lateinit var sharedPref: SharedPreferences
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

        val monochromaticImage: MonochromaticImage = MonochromaticImage.Builder(
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
            if (showSeconds || theZman.secondTreatment == SecondTreatment.ALWAYS_DISPLAY)
                zmanTime = yesSecondFormat.format(theZman.zman)
            else {
                val calendar = Calendar.getInstance()
                calendar.time = theZman.zman

                var zmanDate: Date = theZman.zman
                if (calendar[Calendar.SECOND] > 40 || calendar[Calendar.SECOND] > 20 && theZman.secondTreatment === SecondTreatment.ROUND_LATER) {
                    zmanDate = Utils.addMinuteToZman(theZman.zman)
                }

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