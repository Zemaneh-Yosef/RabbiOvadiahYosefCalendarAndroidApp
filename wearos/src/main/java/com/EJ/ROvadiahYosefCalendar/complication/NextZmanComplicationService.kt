package com.EJ.ROvadiahYosefCalendar.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationText
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
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimNames
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NextZmanComplicationService : SuspendingComplicationDataSourceService() {

    private lateinit var sharedPref: SharedPreferences
    private var mCurrentDateShown = Calendar.getInstance()
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation())
    private var mJewishDateInfo = JewishDateInfo(false)
    private lateinit var zmanimFormat: SimpleDateFormat
    private lateinit var visibleSunriseFormat: SimpleDateFormat
    private lateinit var roundUpFormat: SimpleDateFormat

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
                    .replace("Sof Zman ", "")
                    .replace("Hacochavim", "")
                    .replace("Latest ", "")).build()
            })
            .setTapAction(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    private fun getNextUpcomingZmanTimeAsString(context: Context?): String {
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
            return zmanTime
        }
        return ""
    }

    private fun getNextUpcomingZman(context: Context?): ZmanListEntry? {
        if (context != null) {
            sharedPref = context.getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)
        }
        mROZmanimCalendar = ROZmanimCalendar(LocationResolver.getLastGeoLocation(sharedPref))
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
        mROZmanimCalendar.setSharedPreferences(sharedPref)

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
        addZmanim(zmanim) //for the previous day
        today.add(Calendar.DATE, 1)
        mROZmanimCalendar.calendar = today
        mJewishDateInfo.setCalendar(today)
        addZmanim(zmanim) //for the current day
        today.add(Calendar.DATE, 1)
        mROZmanimCalendar.calendar = today
        mJewishDateInfo.setCalendar(today)
        addZmanim(zmanim) //for the next day
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

    private fun addZmanim(zmanim: MutableList<ZmanListEntry>) {
        if (sharedPref.getBoolean("LuachAmudeiHoraah", false)) {
            addAmudeiHoraahZmanim(zmanim)
            return
        }
        val zmanimNames = ZmanimNames(
            sharedPref.getBoolean("isZmanimInHebrew", false),
            sharedPref.getBoolean("isZmanimEnglishTranslated", false)
        )
        if (mJewishDateInfo.jewishCalendar.isTaanis && mJewishDateInfo.jewishCalendar.yomTovIndex != JewishCalendar.TISHA_BEAV && mJewishDateInfo.jewishCalendar.yomTovIndex != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.taanitString + zmanimNames.startsString,
                    mROZmanimCalendar.alos72Zmanis,
                    true
                )
            )
        }
        zmanim.add(ZmanListEntry(zmanimNames.alotString, mROZmanimCalendar.alos72Zmanis, true))
        zmanim.add(
            ZmanListEntry(
                zmanimNames.talitTefilinString,
                mROZmanimCalendar.earliestTalitTefilin,
                true
            )
        )
        if (sharedPref.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString + " " + zmanimNames.elevatedString,
                    mROZmanimCalendar.sunrise,
                    true
                )
            )
        }
        if (mROZmanimCalendar.haNetz != null) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString,
                    mROZmanimCalendar.haNetz,
                    true,
                    true
                )
            )
        } else {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString + " (" + zmanimNames.mishorString + ")",
                    mROZmanimCalendar.seaLevelSunrise,
                    true
                )
            )
        }
        if (mROZmanimCalendar.haNetz != null &&
            sharedPref.getBoolean("ShowMishorAlways", false)
        ) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString + " (" + zmanimNames.mishorString + ")",
                    mROZmanimCalendar.seaLevelSunrise,
                    true
                )
            )
        }
        zmanim.add(
            ZmanListEntry(
                zmanimNames.shmaMgaString,
                mROZmanimCalendar.sofZmanShmaMGA72MinutesZmanis,
                true
            )
        )
        zmanim.add(ZmanListEntry(zmanimNames.shmaGraString, mROZmanimCalendar.sofZmanShmaGRA, true))
        if (mJewishDateInfo.jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH) {
            var zman = ZmanListEntry(
                zmanimNames.achilatChametzString,
                mROZmanimCalendar.sofZmanTfilaMGA72MinutesZmanis,
                true
            )
            zman.isNoteworthyZman = true
            zmanim.add(zman)
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.brachotShmaString,
                    mROZmanimCalendar.sofZmanTfilaGRA,
                    true
                )
            )
            zman = ZmanListEntry(
                zmanimNames.biurChametzString,
                mROZmanimCalendar.sofZmanBiurChametzMGA,
                true
            )
            zman.isNoteworthyZman = true
            zmanim.add(zman)
        } else {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.brachotShmaString,
                    mROZmanimCalendar.sofZmanTfilaGRA,
                    true
                )
            )
        }
        zmanim.add(ZmanListEntry(zmanimNames.chatzotString, mROZmanimCalendar.chatzot, true))
        zmanim.add(
            ZmanListEntry(
                zmanimNames.minchaGedolaString,
                mROZmanimCalendar.minchaGedolaGreaterThan30,
                true
            )
        )
        zmanim.add(
            ZmanListEntry(
                zmanimNames.minchaKetanaString,
                mROZmanimCalendar.minchaKetana,
                true
            )
        )
        val plagOpinions: String? = sharedPref.getString("plagOpinion", "1")
        if (plagOpinions == "1" || plagOpinions == null) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.plagHaminchaString,
                    mROZmanimCalendar.plagHaminchaYalkutYosef,
                    true
                )
            )
        }
        if (plagOpinions == "2") {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.plagHaminchaString,
                    mROZmanimCalendar.plagHamincha,
                    true
                )
            )
        }
        if (plagOpinions == "3") {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.plagHaminchaString + " " + zmanimNames.abbreviatedHalachaBerurahString,
                    mROZmanimCalendar.plagHamincha, true
                )
            )
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.plagHaminchaString + " " + zmanimNames.abbreviatedYalkutYosefString,
                    mROZmanimCalendar.plagHaminchaYalkutYosef, true
                )
            )
        }
        if (mJewishDateInfo.jewishCalendar.hasCandleLighting() &&
            !mJewishDateInfo.jewishCalendar.isAssurBemelacha ||
            mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY
        ) {
            val candleLightingZman = ZmanListEntry(
                zmanimNames.candleLightingString + " (" + mROZmanimCalendar.candleLightingOffset.toInt() + ")",
                mROZmanimCalendar.candleLighting,
                true
            )
            candleLightingZman.isNoteworthyZman = true
            zmanim.add(candleLightingZman)
        }
        if (sharedPref.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.jewishCalendar.isTomorrowShabbosOrYomTov) {
                mROZmanimCalendar.calendar.add(Calendar.DATE, 1)
                mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
                if (!mJewishDateInfo.jewishCalendar.isTomorrowShabbosOrYomTov) { //only add if shabbat/yom tov ends tomorrow and not the day after
                    if (sharedPref.getBoolean("Show Regular Minutes", false)) {
                        val endShabbat: ZmanListEntry = if (sharedPref.getString(
                                "EndOfShabbatOpinion",
                                "1"
                            ) == "1" || sharedPref.getBoolean("inIsrael", false)
                        ) {
                            ZmanListEntry(
                                zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString
                                        + " (" + mROZmanimCalendar.ateretTorahSunsetOffset.toInt() + ")" + zmanimNames.macharString,
                                mROZmanimCalendar.tzaisAteretTorah,
                                true
                            )
                        } else if (sharedPref.getString("EndOfShabbatOpinion", "1") == "2") {
                            ZmanListEntry(
                                zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString + zmanimNames.macharString,
                                mROZmanimCalendar.tzaitShabbatAmudeiHoraah,
                                true
                            )
                        } else {
                            ZmanListEntry(
                                zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString + zmanimNames.macharString,
                                mROZmanimCalendar.tzaitShabbatAmudeiHoraahLesserThan40,
                                true
                            )
                        }
                        endShabbat.isNoteworthyZman = true
                        zmanim.add(endShabbat)
                    }
                    if (sharedPref.getBoolean("Show Rabbeinu Tam", false)) {
                        if (sharedPref.getBoolean("RoundUpRT", true)) {
                            val rt = ZmanListEntry(
                                zmanimNames.rtString + zmanimNames.macharString,
                                addMinuteToZman(mROZmanimCalendar.tzais72Zmanis),
                                true
                            )
                            rt.isRTZman = true
                            zmanim.add(rt)
                        } else {
                            val rt = ZmanListEntry(
                                zmanimNames.rtString + zmanimNames.macharString,
                                mROZmanimCalendar.tzais72Zmanis,
                                true
                            )
                            rt.isRTZman = true
                            zmanim.add(rt)
                        }
                    }
                }
                mROZmanimCalendar.calendar.add(Calendar.DATE, -1)
                mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
            }
        }
        mROZmanimCalendar.calendar.add(Calendar.DATE, 1)
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        mROZmanimCalendar.calendar.add(Calendar.DATE, -1)
        if (mJewishDateInfo.jewishCalendar.yomTovIndex == JewishCalendar.TISHA_BEAV) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.taanitString + zmanimNames.startsString,
                    mROZmanimCalendar.sunset,
                    true
                )
            )
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        zmanim.add(ZmanListEntry(zmanimNames.sunsetString, mROZmanimCalendar.sunset, true))
        zmanim.add(ZmanListEntry(zmanimNames.tzaitHacochavimString, mROZmanimCalendar.tzeit, true))
        if (mJewishDateInfo.jewishCalendar.hasCandleLighting() &&
            mJewishDateInfo.jewishCalendar.isAssurBemelacha
        ) {
            if (mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] != Calendar.FRIDAY) {
                if (mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY) { //When today is Shabbat
                    val endShabbat: ZmanListEntry = if (sharedPref.getString(
                            "EndOfShabbatOpinion",
                            "1"
                        ) == "1" || sharedPref.getBoolean("inIsrael", false)
                    ) {
                        ZmanListEntry(
                            zmanimNames.candleLightingString,
                            mROZmanimCalendar.tzaisAteretTorah,
                            true
                        )
                    } else if (sharedPref.getString("EndOfShabbatOpinion", "1") == "2") {
                        ZmanListEntry(
                            zmanimNames.candleLightingString,
                            mROZmanimCalendar.tzaitShabbatAmudeiHoraah,
                            true
                        )
                    } else {
                        ZmanListEntry(
                            zmanimNames.candleLightingString,
                            mROZmanimCalendar.tzaitShabbatAmudeiHoraahLesserThan40,
                            true
                        )
                    }
                    endShabbat.isNoteworthyZman = true
                    zmanim.add(endShabbat)
                } else { //When today is Yom Tov
                    zmanim.add(
                        ZmanListEntry(
                            zmanimNames.candleLightingString,
                            mROZmanimCalendar.tzeit,
                            true
                        )
                    )
                }
            }
        }
        if (mJewishDateInfo.jewishCalendar.isTaanis && mJewishDateInfo.jewishCalendar.yomTovIndex != JewishCalendar.YOM_KIPPUR) {
            var fastEnds = ZmanListEntry(
                zmanimNames.tzaitString + zmanimNames.taanitString + zmanimNames.endsString,
                mROZmanimCalendar.tzaitTaanit,
                true
            )
            fastEnds.isNoteworthyZman = true
            zmanim.add(fastEnds)
            fastEnds = ZmanListEntry(
                zmanimNames.tzaitString + zmanimNames.taanitString + zmanimNames.endsString + " " + zmanimNames.lChumraString,
                mROZmanimCalendar.tzaitTaanitLChumra,
                true
            )
            fastEnds.isNoteworthyZman = true
            zmanim.add(fastEnds)
        } else if (sharedPref.getBoolean("alwaysShowTzeitLChumra", false)) {
            val tzeitLChumra = ZmanListEntry(
                zmanimNames.tzaitHacochavimString + " " + zmanimNames.lChumraString,
                mROZmanimCalendar.tzaitTaanit,
                true
            )
            zmanim.add(tzeitLChumra)
        }
        if (mJewishDateInfo.jewishCalendar.isAssurBemelacha && !mJewishDateInfo.jewishCalendar.hasCandleLighting()) {
            val endShabbat: ZmanListEntry = if (sharedPref.getString(
                    "EndOfShabbatOpinion",
                    "1"
                ) == "1" || sharedPref.getBoolean("inIsrael", false)
            ) {
                ZmanListEntry(
                    zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString
                            + " (" + mROZmanimCalendar.ateretTorahSunsetOffset.toInt() + ")",
                    mROZmanimCalendar.tzaisAteretTorah,
                    true
                )
            } else if (sharedPref.getString("EndOfShabbatOpinion", "1") == "2") {
                ZmanListEntry(
                    zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString,
                    mROZmanimCalendar.tzaitShabbatAmudeiHoraah,
                    true
                )
            } else {
                ZmanListEntry(
                    zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString,
                    mROZmanimCalendar.tzaitShabbatAmudeiHoraahLesserThan40,
                    true
                )
            }
            endShabbat.isNoteworthyZman = true
            zmanim.add(endShabbat)
            if (sharedPref.getBoolean("RoundUpRT", true)) {
                val rt = ZmanListEntry(
                    zmanimNames.rtString,
                    addMinuteToZman(mROZmanimCalendar.tzais72Zmanis),
                    true
                )
                rt.isRTZman = true
                rt.isNoteworthyZman = true
                zmanim.add(rt)
            } else {
                val rt = ZmanListEntry(zmanimNames.rtString, mROZmanimCalendar.tzais72Zmanis, true)
                rt.isRTZman = true
                rt.isNoteworthyZman = true
                zmanim.add(rt)
            }
            //If it is shabbat/yom tov,we want to dim the tzeit hacochavim zmanim in the GUI
            for (zman in zmanim) {
                if (zman.title == zmanimNames.tzaitHacochavimString || zman.title == zmanimNames.tzaitHacochavimString + " " + zmanimNames.lChumraString) {
                    zman.isShouldBeDimmed = true
                }
            }
        }
        if (sharedPref.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.jewishCalendar.isAssurBemelacha && !mJewishDateInfo.jewishCalendar.hasCandleLighting())) { //if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (sharedPref.getBoolean("RoundUpRT", true)) {
                    val rt = ZmanListEntry(
                        zmanimNames.rtString,
                        addMinuteToZman(mROZmanimCalendar.tzais72Zmanis),
                        true
                    )
                    rt.isRTZman = true
                    zmanim.add(rt)
                } else {
                    val rt =
                        ZmanListEntry(zmanimNames.rtString, mROZmanimCalendar.tzais72Zmanis, true)
                    rt.isRTZman = true
                    zmanim.add(rt)
                }
            }
        }
        zmanim.add(
            ZmanListEntry(
                zmanimNames.chatzotLaylaString,
                mROZmanimCalendar.solarMidnight,
                true
            )
        )
    }

    private fun addAmudeiHoraahZmanim(zmanim: MutableList<ZmanListEntry>) {
        mROZmanimCalendar.isUseElevation = false
        val zmanimNames = ZmanimNames(
            sharedPref.getBoolean("isZmanimInHebrew", false),
            sharedPref.getBoolean("isZmanimEnglishTranslated", false)
        )
        if (mJewishDateInfo.jewishCalendar.isTaanis && mJewishDateInfo.jewishCalendar.yomTovIndex != JewishCalendar.TISHA_BEAV && mJewishDateInfo.jewishCalendar.yomTovIndex != JewishCalendar.YOM_KIPPUR) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.taanitString + zmanimNames.startsString,
                    mROZmanimCalendar.alotAmudeiHoraah,
                    true
                )
            )
        }
        zmanim.add(ZmanListEntry(zmanimNames.alotString, mROZmanimCalendar.alotAmudeiHoraah, true))
        zmanim.add(
            ZmanListEntry(
                zmanimNames.talitTefilinString,
                mROZmanimCalendar.earliestTalitTefilinAmudeiHoraah,
                true
            )
        )
        if (sharedPref.getBoolean("ShowElevatedSunrise", false)) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString + " " + zmanimNames.elevatedString,
                    mROZmanimCalendar.sunrise,
                    true
                )
            )
        }
        if (mROZmanimCalendar.haNetz != null) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString,
                    mROZmanimCalendar.haNetz,
                    true,
                    true
                )
            )
        } else {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString + " (" + zmanimNames.mishorString + ")",
                    mROZmanimCalendar.seaLevelSunrise,
                    true
                )
            )
        }
        if (mROZmanimCalendar.haNetz != null &&
            sharedPref.getBoolean("ShowMishorAlways", false)
        ) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.haNetzString + " (" + zmanimNames.mishorString + ")",
                    mROZmanimCalendar.seaLevelSunrise,
                    true
                )
            )
        }
        zmanim.add(
            ZmanListEntry(
                zmanimNames.shmaMgaString,
                mROZmanimCalendar.sofZmanShmaMGA72MinutesZmanisAmudeiHoraah,
                true
            )
        )
        zmanim.add(ZmanListEntry(zmanimNames.shmaGraString, mROZmanimCalendar.sofZmanShmaGRA, true))
        if (mJewishDateInfo.jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH) {
            var zman = ZmanListEntry(
                zmanimNames.achilatChametzString,
                mROZmanimCalendar.sofZmanAchilatChametzAmudeiHoraah,
                true
            )
            zman.isNoteworthyZman = true
            zmanim.add(zman)
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.brachotShmaString,
                    mROZmanimCalendar.sofZmanTfilaGRA,
                    true
                )
            )
            zman = ZmanListEntry(
                zmanimNames.biurChametzString,
                mROZmanimCalendar.sofZmanBiurChametzMGAAmudeiHoraah,
                true
            )
            zman.isNoteworthyZman = true
            zmanim.add(zman)
        } else {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.brachotShmaString,
                    mROZmanimCalendar.sofZmanTfilaGRA,
                    true
                )
            )
        }
        zmanim.add(ZmanListEntry(zmanimNames.chatzotString, mROZmanimCalendar.chatzot, true))
        zmanim.add(
            ZmanListEntry(
                zmanimNames.minchaGedolaString,
                mROZmanimCalendar.minchaGedolaGreaterThan30,
                true
            )
        )
        zmanim.add(
            ZmanListEntry(
                zmanimNames.minchaKetanaString,
                mROZmanimCalendar.minchaKetana,
                true
            )
        )
        zmanim.add(
            ZmanListEntry(
                zmanimNames.plagHaminchaString + " " + zmanimNames.abbreviatedHalachaBerurahString,
                mROZmanimCalendar.plagHamincha,
                true
            )
        )
        zmanim.add(
            ZmanListEntry(
                zmanimNames.plagHaminchaString + " " + zmanimNames.abbreviatedYalkutYosefString,
                mROZmanimCalendar.plagHaminchaYalkutYosefAmudeiHoraah,
                true
            )
        )
        if (mJewishDateInfo.jewishCalendar.hasCandleLighting() &&
            !mJewishDateInfo.jewishCalendar.isAssurBemelacha ||
            mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY
        ) {
            val candleLightingZman = ZmanListEntry(
                zmanimNames.candleLightingString + " (" + mROZmanimCalendar.candleLightingOffset.toInt() + ")",
                mROZmanimCalendar.candleLighting,
                true
            )
            candleLightingZman.isNoteworthyZman = true
            zmanim.add(candleLightingZman)
        }
        if (sharedPref.getBoolean("ShowWhenShabbatChagEnds", false)) {
            if (mJewishDateInfo.jewishCalendar.isTomorrowShabbosOrYomTov) {
                mROZmanimCalendar.calendar.add(Calendar.DATE, 1)
                mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
                if (!mJewishDateInfo.jewishCalendar.isTomorrowShabbosOrYomTov) {
                    if (sharedPref.getBoolean("Show Regular Minutes", false)) {
                        zmanim.add(
                            ZmanListEntry(
                                zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString + zmanimNames.macharString,
                                mROZmanimCalendar.tzaitShabbatAmudeiHoraah,
                                true
                            )
                        )
                    }
                    if (sharedPref.getBoolean("Show Rabbeinu Tam", false)) {
                        if (sharedPref.getBoolean("RoundUpRT", true)) {
                            val rt = ZmanListEntry(
                                zmanimNames.rtString + zmanimNames.macharString,
                                addMinuteToZman(mROZmanimCalendar.tzais72ZmanisAmudeiHoraahLkulah),
                                true
                            )
                            rt.isRTZman = true
                            zmanim.add(rt)
                        } else {
                            val rt = ZmanListEntry(
                                zmanimNames.rtString + zmanimNames.macharString,
                                mROZmanimCalendar.tzais72ZmanisAmudeiHoraahLkulah,
                                true
                            )
                            rt.isRTZman = true
                            zmanim.add(rt)
                        }
                    }
                }
            }
            mROZmanimCalendar.calendar.add(Calendar.DATE, -1)
            mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        }
        mROZmanimCalendar.calendar.add(Calendar.DATE, 1)
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        mROZmanimCalendar.calendar.add(Calendar.DATE, -1)
        if (mJewishDateInfo.jewishCalendar.yomTovIndex == JewishCalendar.TISHA_BEAV) {
            zmanim.add(
                ZmanListEntry(
                    zmanimNames.taanitString + zmanimNames.startsString,
                    mROZmanimCalendar.sunset,
                    true
                )
            )
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        zmanim.add(ZmanListEntry(zmanimNames.sunsetString, mROZmanimCalendar.seaLevelSunset, true))
        zmanim.add(
            ZmanListEntry(
                zmanimNames.tzaitHacochavimString,
                mROZmanimCalendar.tzeitAmudeiHoraah,
                true
            )
        )
        zmanim.add(
            ZmanListEntry(
                zmanimNames.tzaitHacochavimString + " " + zmanimNames.lChumraString,
                mROZmanimCalendar.tzeitAmudeiHoraahLChumra,
                true
            )
        )
        if (mJewishDateInfo.jewishCalendar.hasCandleLighting() &&
            mJewishDateInfo.jewishCalendar.isAssurBemelacha
        ) {
            if (mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] != Calendar.FRIDAY) {
                if (mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY) { //When today is Shabbat
                    zmanim.add(
                        ZmanListEntry(
                            zmanimNames.candleLightingString,
                            mROZmanimCalendar.tzaitShabbatAmudeiHoraah,
                            true
                        )
                    )
                } else { //When today is Yom Tov
                    zmanim.add(
                        ZmanListEntry(
                            zmanimNames.candleLightingString,
                            mROZmanimCalendar.tzeitAmudeiHoraahLChumra,
                            true
                        )
                    )
                }
            }
        }
        if (mJewishDateInfo.jewishCalendar.isTaanis && mJewishDateInfo.jewishCalendar.yomTovIndex != JewishCalendar.YOM_KIPPUR) {
            val fastEnds = ZmanListEntry(
                zmanimNames.tzaitString + zmanimNames.taanitString + zmanimNames.endsString,
                mROZmanimCalendar.tzeitAmudeiHoraahLChumra,
                true
            )
            fastEnds.isNoteworthyZman = true
            zmanim.add(fastEnds)
        }
        if (mJewishDateInfo.jewishCalendar.isAssurBemelacha && !mJewishDateInfo.jewishCalendar.hasCandleLighting()) {
            val endShabbat = ZmanListEntry(
                zmanimNames.tzaitString + getShabbatAndOrChag() + zmanimNames.endsString,
                mROZmanimCalendar.tzaitShabbatAmudeiHoraah,
                true
            )
            endShabbat.isNoteworthyZman = true
            zmanim.add(endShabbat)
            if (sharedPref.getBoolean("RoundUpRT", true)) {
                val rt = ZmanListEntry(
                    zmanimNames.rtString,
                    addMinuteToZman(mROZmanimCalendar.tzais72ZmanisAmudeiHoraahLkulah),
                    true
                )
                rt.isRTZman = true
                rt.isNoteworthyZman = true
                zmanim.add(rt)
            } else {
                val rt = ZmanListEntry(
                    zmanimNames.rtString,
                    mROZmanimCalendar.tzais72ZmanisAmudeiHoraahLkulah,
                    true
                )
                rt.isRTZman = true
                rt.isNoteworthyZman = true
                zmanim.add(rt)
            }
            //If it is shabbat/yom tov,we want to dim the tzeit hacochavim zmanim in the GUI
            for (zman in zmanim) {
                if (zman.title == zmanimNames.tzaitHacochavimString || zman.title == zmanimNames.tzaitHacochavimString + " " + zmanimNames.lChumraString) {
                    zman.isShouldBeDimmed = true
                }
            }
        }
        if (sharedPref.getBoolean("AlwaysShowRT", false)) {
            if (!(mJewishDateInfo.jewishCalendar.isAssurBemelacha && !mJewishDateInfo.jewishCalendar.hasCandleLighting())) { //if we want to always show the zman for RT, we can just NOT the previous cases where we do show it
                if (sharedPref.getBoolean("RoundUpRT", true)) {
                    val rt = ZmanListEntry(
                        zmanimNames.rtString,
                        addMinuteToZman(mROZmanimCalendar.tzais72ZmanisAmudeiHoraahLkulah),
                        true
                    )
                    rt.isRTZman = true
                    zmanim.add(rt)
                } else {
                    val rt = ZmanListEntry(
                        zmanimNames.rtString,
                        mROZmanimCalendar.tzais72ZmanisAmudeiHoraahLkulah,
                        true
                    )
                    rt.isRTZman = true
                    zmanim.add(rt)
                }
            }
        }
        zmanim.add(
            ZmanListEntry(
                zmanimNames.chatzotLaylaString,
                mROZmanimCalendar.solarMidnight,
                true
            )
        )
    }

    private fun getShabbatAndOrChag(): String {
        return if (sharedPref.getBoolean("isZmanimInHebrew", false)) {
            if (mJewishDateInfo.jewishCalendar.isYomTovAssurBemelacha
                && mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY
            ) {
                "שבת/חג"
            } else if (mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY) {
                "שבת"
            } else {
                "חג"
            }
        } else {
            if (mJewishDateInfo.jewishCalendar.isYomTovAssurBemelacha
                && mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY
            ) {
                "Shabbat/Chag"
            } else if (mJewishDateInfo.jewishCalendar.gregorianCalendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY) {
                "Shabbat"
            } else {
                "Chag"
            }
        }
    }

    private fun addMinuteToZman(date: Date?): Date? {
        return if (date == null) {
            null
        } else Date(date.time + 60000)
    }
}