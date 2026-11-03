package com.EJ.ROvadiahYosefCalendar.complication

import android.content.Context
import android.content.SharedPreferences
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.classes.LocationResolver
import com.EJ.ROvadiahYosefCalendar.classes.ROZmanimCalendar
import com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment
import com.EJ.ROvadiahYosefCalendar.classes.Utils
import com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntry
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

abstract class BaseZmanComplicationService : SuspendingComplicationDataSourceService() {

    protected lateinit var sharedPref: SharedPreferences
    protected var mROZmanimCalendar = ROZmanimCalendar(GeoLocation(), null)
    protected var mJewishDateInfo = JewishDateInfo(false)

    /**
     * Returns the next upcoming zman. Also initializes noSecondFormat, yesSecondFormat,
     * and showSeconds as a side effect — always call this before formatZmanTime().
     */
    protected fun getNextUpcomingZman(context: Context?): ZmanListEntry? {
        if (context != null) {
            sharedPref = context.getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)
        }
        mROZmanimCalendar = ROZmanimCalendar(LocationResolver.getLastGeoLocation(sharedPref), sharedPref)
        mROZmanimCalendar.candleLightingOffset =
            sharedPref.getString("CandleLightingOffset", "20")?.toDoubleOrNull() ?: 20.0
        mROZmanimCalendar.ateretTorahSunsetOffset =
            sharedPref.getString("EndOfShabbatOffset", null)?.toDoubleOrNull()
                ?: if (sharedPref.getBoolean("inIsrael", false)) 30.0 else 40.0
        if (sharedPref.getBoolean("inIsrael", false) &&
            sharedPref.getString("EndOfShabbatOffset", "40") == "40") {
            mROZmanimCalendar.ateretTorahSunsetOffset = 30.0
        }

        var sUserIsOffline = false
        var elevation: Double
        if (mROZmanimCalendar.geoLocation.locationName.contains("Lat:") &&
            mROZmanimCalendar.geoLocation.locationName.contains("Long:") &&
            sharedPref.getBoolean("SetElevationToLastKnownLocation", false)) {
            sUserIsOffline = true
            elevation = sharedPref.getString("elevation" + sharedPref.getString("name", ""), "0")
                ?.toDouble() ?: 0.0
        } else {
            elevation = sharedPref.getString("elevation${mROZmanimCalendar.geoLocation.locationName}", "0")
                ?.toDouble() ?: 0.0
        }
        if (!sUserIsOffline && sharedPref.getBoolean("useElevation", true) &&
            !sharedPref.getBoolean("LuachAmudeiHoraah", false)) {
            elevation = sharedPref.getString("elevation${mROZmanimCalendar.geoLocation.locationName}", "0")
                ?.toDouble() ?: 0.0
        }
        if (!sharedPref.getBoolean("useElevation", true)) elevation = 0.0
        mROZmanimCalendar.geoLocation.elevation = elevation.coerceAtLeast(0.0) //GeoLocation.setElevation rejects negative values

        mJewishDateInfo.resetLocale(baseContext)
        mJewishDateInfo.jewishCalendar.inIsrael = sharedPref.getBoolean("inIsrael", false)

        return ZmanimFactory.getNextUpcomingZman(LocalDate.now(), mROZmanimCalendar, mJewishDateInfo, sharedPref)
    }

    fun shortenTitle(raw: String): String {
        var s = raw
            .replace("סוף זמן ", "")
            .replace("עלות השחר",   "עלות")
            .replace("ברכות שמע",   "ב. שמע")
            .replace("צאת הכוכבים", "צאת הכו׳")
            .replace("מנחה גדולה",  "מנחה ג׳")
            .replace("מנחה קטנה",   "מנחה ק׳")
            .replace("פלג המנחה",   "פלג")
            .replace("חצות הלילה",  "חצות ל׳")
            .replace("הדלקת נרות",  "הדלקה")
            .replace("רבינו תם",   "ר\"ת")
            .replace("הלכה ברורה",   "ה\"ב")
            .replace("ילקוט יוסף",   "י\"י")
            .replace("Earliest ", "")
            .replace("Sof Zeman ", "")
            .replace("Latest ", "")
            .replace("Alot HaShachar",   "Alot")
            .replace("Mincha Gedola",    "Min. G.")
            .replace("Minḥa Gedola",    "Min. G.")
            .replace("Mincha Ketana",    "Min. K.")
            .replace("Minḥa Ketana",    "Min. K.")
            .replace("Pelag HaMincha",    "Pelag")
            .replace("Halacha Berura",    "H\"B")
            .replace("Yalkut Yosef",    "Y\"Y")
            .replace("Candle Lighting",  "Candles")
            .replace("Ha'Kokhavim", "")
            .replace("Ha'Kochavim", "")
            .replace("Rabbenu Tam",   "R.T.")
            .trim()

        if (s.length > 10) s = s.take(10).trimEnd()
        return s
    }
}