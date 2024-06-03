
package com.EJ.ROvadiahYosefCalendar.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.*
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.curvedText
import androidx.wear.tiles.TileService
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.classes.BooleanListener
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.classes.LocationResolver
import com.EJ.ROvadiahYosefCalendar.classes.PreferenceListener
import com.EJ.ROvadiahYosefCalendar.classes.ROZmanimCalendar
import com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntry
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimNames
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimNotifications
import com.EJ.ROvadiahYosefCalendar.presentation.theme.DarkGray
import com.EJ.ROvadiahYosefCalendar.presentation.theme.RabbiOvadiahYosefCalendarTheme
import com.EJ.ROvadiahYosefCalendar.tile.MainTileService
import com.kosherjava.zmanim.hebrewcalendar.Daf
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator
import com.kosherjava.zmanim.util.GeoLocation
import com.kosherjava.zmanim.util.ZmanimFormatter
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    companion object {
        @JvmField
        var SHARED_PREF: String = "MyWatchPrefsFile"

        @JvmField
        var sCurrentLocationName: String = ""

        @JvmField
        var sLatitude: Double = 0.0

        @JvmField
        var sLongitude: Double = 0.0

        @JvmField
        var sElevation: Double = 0.0

        @JvmField
        var sCurrentTimeZoneID: String = ""

        @JvmField
        var sNextUpcomingZman: Date? = Date()
    }

    private lateinit var sharedPref: SharedPreferences
    private var mCurrentDateShown = Calendar.getInstance()
    private lateinit var locationResolver: LocationResolver
    private var zmanim: MutableList<ZmanListEntry> = ArrayList()
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation())
    private var mJewishDateInfo = JewishDateInfo(false, true)
    private var mLastTimeUserWasInApp: Date = Date()
    private val mHebrewDateFormatter = HebrewDateFormatter()
    private val mZmanimFormatter = ZmanimFormatter(TimeZone.getDefault())
    private lateinit var zmanimFormat: SimpleDateFormat
    private lateinit var visibleSunriseFormat: SimpleDateFormat
    private lateinit var roundUpFormat: SimpleDateFormat
    private val mHandler: Handler? = null
    private val dafYomiStartDate: Calendar = GregorianCalendar(1923, Calendar.SEPTEMBER, 11)
    private val dafYomiYerushalmiStartDate: Calendar = GregorianCalendar(1980, Calendar.FEBRUARY, 2)
    private val listener: PreferenceListener = PreferenceListener()
    private var sNotificationLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        sNotificationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { setNotifications() }
        mHebrewDateFormatter.isUseGershGershayim = false
        mZmanimFormatter.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_FORMAT)
        sharedPref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE)
        locationResolver = LocationResolver(this, this)
        listener.setOnMessageReceivedListener( { messageEvent ->
            if (messageEvent.path == "prefs/") {
                val message = String(messageEvent.data, StandardCharsets.UTF_8) // convert bytes to String
                val jsonPreferences = JSONObject(message) // We can just pass that JSON string into the constructor! :)
                savePreferencesToLocalDevice(jsonPreferences)
                sharedPref.edit().putBoolean("hasGottenDataFromApp", true).apply()
            }
            if (messageEvent.path == "chaiTable/") {
                val message = String(messageEvent.data, StandardCharsets.UTF_8) // convert bytes to String
                sharedPref.edit().putString("chaiTable" + sharedPref.getString("locationName", ""), message).apply() // The location name should be set already from the previous message
            }
            updateAppContents() // with the new preferences
        }, this)
        startService(Intent(this, listener.javaClass))
    }

    private fun savePreferencesToLocalDevice(jsonPreferences: JSONObject) {
        val editor = sharedPref.edit()

        editor.putBoolean("useElevation", jsonPreferences.getBoolean("useElevation"))
            .putBoolean("ShowSeconds", jsonPreferences.getBoolean("ShowSeconds"))
            .putBoolean("inIsrael", jsonPreferences.getBoolean("inIsrael"))
            .putBoolean("ShowElevation", jsonPreferences.getBoolean("ShowElevation"))
            .putString("tekufaOpinions", jsonPreferences.getString("tekufaOpinions"))
            .putBoolean("RoundUpRT", jsonPreferences.getBoolean("RoundUpRT"))
            .putBoolean("LuachAmudeiHoraah", jsonPreferences.getBoolean("LuachAmudeiHoraah"))
            .putBoolean("isZmanimInHebrew", jsonPreferences.getBoolean("isZmanimInHebrew"))
            .putBoolean("isZmanimEnglishTranslated", jsonPreferences.getBoolean("isZmanimEnglishTranslated"))
            .putBoolean("ShowMishorAlways", jsonPreferences.getBoolean("ShowMishorAlways"))
            .putString("plagOpinion", jsonPreferences.getString("plagOpinion"))
            .putString("CandleLightingOffset", jsonPreferences.getString("CandleLightingOffset"))
            .putBoolean("ShowWhenShabbatChagEnds", jsonPreferences.getBoolean("ShowWhenShabbatChagEnds"))
            .putString("EndOfShabbatOffset", jsonPreferences.getString("EndOfShabbatOffset"))
            .putString("EndOfShabbatOpinion", jsonPreferences.getString("EndOfShabbatOpinion"))
            .putBoolean("alwaysShowTzeitLChumra", jsonPreferences.getBoolean("alwaysShowTzeitLChumra"))
            .putBoolean("AlwaysShowRT", jsonPreferences.getBoolean("AlwaysShowRT"))
            .putBoolean("useZipcode", jsonPreferences.getBoolean("useZipcode"))
            .putString("Zipcode", jsonPreferences.getString("Zipcode"))
            .putString("oldZipcode", jsonPreferences.getString("oldZipcode"))
            .putString("oldLocationName", jsonPreferences.getString("oldLocationName"))
            .putLong("oldLat", jsonPreferences.getLong("oldLat"))
            .putLong("oldLong", jsonPreferences.getLong("oldLong"))
            .putString("locationName", jsonPreferences.getString("locationName"))
            .putString("elevation" + jsonPreferences.getString("locationName"), jsonPreferences.getString("elevation" + jsonPreferences.getString("locationName")))//use the locationName in JSON since we do not know if the location name is the same in the watch
            .putBoolean("SetElevationToLastKnownLocation", jsonPreferences.getBoolean("SetElevationToLastKnownLocation"))

            .putString("currentLN", jsonPreferences.getString("currentLN"))
            .putString("currentLat", jsonPreferences.getString("currentLat"))
            .putString("currentLong", jsonPreferences.getString("currentLong"))
            .putString("currentTimezone", jsonPreferences.getString("currentTimezone"))

            .putBoolean("useAdvanced", jsonPreferences.getBoolean("useAdvanced"))
            .putString("advancedLN", jsonPreferences.getString("advancedLN"))
            .putString("advancedLat", jsonPreferences.getString("advancedLat"))
            .putString("advancedLong", jsonPreferences.getString("advancedLong"))
            .putString("advancedTimezone", jsonPreferences.getString("advancedTimezone"))

            .putBoolean("useLocation1", jsonPreferences.getBoolean("useLocation1"))
            .putString("location1", jsonPreferences.getString("location1"))
            .putLong("location1Lat", jsonPreferences.getLong("location1Lat"))
            .putLong("location1Long", jsonPreferences.getLong("location1Long"))
            .putString("location1Timezone", jsonPreferences.getString("location1Timezone"))

            .putBoolean("useLocation2", jsonPreferences.getBoolean("useLocation2"))
            .putString("location2", jsonPreferences.getString("location2"))
            .putLong("location2Lat", jsonPreferences.getLong("location2Lat"))
            .putLong("location2Long", jsonPreferences.getLong("location2Long"))
            .putString("location2Timezone", jsonPreferences.getString("location2Timezone"))

            .putBoolean("useLocation3", jsonPreferences.getBoolean("useLocation3"))
            .putString("location3", jsonPreferences.getString("location3"))
            .putLong("location3Lat", jsonPreferences.getLong("location3Lat"))
            .putLong("location3Long", jsonPreferences.getLong("location3Long"))
            .putString("location3Timezone", jsonPreferences.getString("location3Timezone"))

            .putBoolean("useLocation4", jsonPreferences.getBoolean("useLocation4"))
            .putString("location4", jsonPreferences.getString("location4"))
            .putLong("location4Lat", jsonPreferences.getLong("location4Lat"))
            .putLong("location4Long", jsonPreferences.getLong("location4Long"))
            .putString("location4Timezone", jsonPreferences.getString("location4Timezone"))

            .putBoolean("useLocation5", jsonPreferences.getBoolean("useLocation5"))
            .putString("location5", jsonPreferences.getString("location5"))
            .putLong("location5Lat", jsonPreferences.getLong("location5Lat"))
            .putLong("location5Long", jsonPreferences.getLong("location5Long"))
            .putString("location5Timezone", jsonPreferences.getString("location5Timezone"))

            .putBoolean("zmanim_notifications", jsonPreferences.getBoolean("zmanim_notifications"))
            .putInt("NightChatzot", jsonPreferences.getInt("NightChatzot"))
            .putInt("RT", jsonPreferences.getInt("RT"))
            .putInt("ShabbatEnd", jsonPreferences.getInt("ShabbatEnd"))
            .putInt("FastEndStringent", jsonPreferences.getInt("FastEndStringent"))
            .putInt("FastEnd", jsonPreferences.getInt("FastEnd"))
            .putInt("TzeitHacochavimLChumra", jsonPreferences.getInt("TzeitHacochavimLChumra"))
            .putInt("TzeitHacochavim", jsonPreferences.getInt("TzeitHacochavim"))
            .putInt("Shkia", jsonPreferences.getInt("Shkia"))
            .putInt("CandleLighting", jsonPreferences.getInt("CandleLighting"))
            .putInt("PlagHaMinchaYY", jsonPreferences.getInt("PlagHaMinchaYY"))
            .putInt("PlagHaMinchaHB", jsonPreferences.getInt("PlagHaMinchaHB"))
            .putInt("MinchaKetana", jsonPreferences.getInt("MinchaKetana"))
            .putInt("MinchaGedola", jsonPreferences.getInt("MinchaGedola"))
            .putInt("Chatzot", jsonPreferences.getInt("Chatzot"))
            .putInt("SofZmanBiurChametz", jsonPreferences.getInt("SofZmanBiurChametz"))
            .putInt("SofZmanTefila", jsonPreferences.getInt("SofZmanTefila"))
            .putInt("SofZmanAchilatChametz", jsonPreferences.getInt("SofZmanAchilatChametz"))
            .putInt("SofZmanShmaGRA", jsonPreferences.getInt("SofZmanShmaGRA"))
            .putInt("SofZmanShmaMGA", jsonPreferences.getInt("SofZmanShmaMGA"))
            .putInt("HaNetz", jsonPreferences.getInt("HaNetz"))
            .putInt("TalitTefilin", jsonPreferences.getInt("TalitTefilin"))
            .putInt("Alot", jsonPreferences.getInt("Alot"))
            .putBoolean("zmanim_notifications_on_shabbat", jsonPreferences.getBoolean("zmanim_notifications_on_shabbat"))
            .putInt("autoDismissNotifications", jsonPreferences.getInt("autoDismissNotifications"))
            .apply()

        if (sharedPref.getBoolean("ShowWhenShabbatChagEnds", false)) {
            editor.putBoolean("Show Regular Minutes", jsonPreferences.getBoolean("Show Regular Minutes"))
                .putBoolean("Show Rabbeinu Tam", jsonPreferences.getBoolean("Show Rabbeinu Tam")).apply()
        }
    }

    override fun onResume() {
        TileService.getUpdater(applicationContext).requestUpdate(MainTileService::class.java)
        mCurrentDateShown.time = Date()
        mROZmanimCalendar.calendar = mCurrentDateShown
        mJewishDateInfo.setCalendar(mCurrentDateShown)
        updateAppContents()
        mLastTimeUserWasInApp = Date()
        return super.onResume()
    }

    private fun updateAppContents() {
        BooleanListener.setMyBoolean(false)
        BooleanListener.addMyBooleanListener {
            runOnUiThread {
                setContent {
                    WearApp(zmanim)
                }
            }
            setNotifications()
        }
        Thread {// I hope this offloads some of the work on the main thread
            Looper.prepare()
            locationResolver.acquireLatitudeAndLongitude()
            resolveElevation()
            initZmanimCalendar()
            sharedPref.edit().putString("name", sCurrentLocationName).apply()
            setDateFormats() // should happen after we get the geolocation object because of the timezone
            setNextUpcomingZman()
            createBackgroundThreadForNextUpcomingZman()
            updateZmanimList()
            BooleanListener.setMyBoolean(true)
        }.start()
        setContent {
            WearApp(zmanim)
        }
    }

    private fun setNotifications() {
        if (sharedPref.getBoolean("zmanim_notifications", false)) { //if the user wants notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // ask for permission to send notifications for newer versions of android ughhhh...
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    if (!sharedPref.getBoolean("hasAskedForNotificationPermissions", false)) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                        sharedPref.edit().putBoolean("hasAskedForNotificationPermissions", true).apply()
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !(getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) { // more annoying android permission garbage
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.zmanim_notifications_will_not_work)
                builder.setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications)
                builder.setCancelable(false)
                builder.setPositiveButton(getString(R.string.yes)) { _: DialogInterface?, _: Int ->
                    sNotificationLauncher?.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                }
                builder.setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                builder.show()
            }
        }
        setAllNotifications()
    }

    private fun setAllNotifications() {
        val zmanIntent = Intent(applicationContext, ZmanimNotifications::class.java)
        val zmanimPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            zmanIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        try {
            zmanimPendingIntent.send()
        } catch (e: CanceledException) {
            e.printStackTrace()
        }
    }

    private fun initZmanimCalendar() {
        mROZmanimCalendar = ROZmanimCalendar(
            GeoLocation(
                sCurrentLocationName,
                sLatitude,
                sLongitude,
                sElevation,
                TimeZone.getTimeZone(sCurrentTimeZoneID)
            )
        )
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
    }

    private fun resolveElevation() {
        var sUserIsOffline = false
        if (sCurrentLocationName.contains("Lat:") && sCurrentLocationName.contains("Long:")
            && sharedPref.getBoolean("SetElevationToLastKnownLocation", false)
        ) { //only if the user has enabled the setting to set the elevation to the last known location
            sUserIsOffline = true
            sElevation =
                sharedPref.getString("elevation" + sharedPref.getString("name", ""), "0")?.toDouble() ?: 0.0 //lastKnownLocation
        } else { //user is online, get the elevation from the shared preferences for the current location
            sElevation =
                sharedPref.getString("elevation$sCurrentLocationName", "0")?.toDouble() ?: 0.0 //get the last value of the current location or 0 if it doesn't exist
        }
        if (!sUserIsOffline && sharedPref.getBoolean("useElevation", true) && !sharedPref.getBoolean("LuachAmudeiHoraah", false)) { //update if the user is online and the elevation setting is enabled
            sElevation = sharedPref.getString("elevation$sCurrentLocationName", "0")?.toDouble() ?: 0.0
        }
        if (!sharedPref.getBoolean("useElevation", true)) { //if the user has disabled the elevation setting, set the elevation to 0
            sElevation = 0.0
        }
    }

    private fun setDateFormats() {
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
    }

    private fun updateZmanimList() {
        zmanim = ArrayList()

        zmanim.add(ZmanListEntry(mROZmanimCalendar.geoLocation.locationName))

        val sb = StringBuilder()

        sb.append(mROZmanimCalendar.calendar.get(Calendar.DATE))
        sb.append(" ")
        sb.append(
            mROZmanimCalendar.calendar.getDisplayName(
                Calendar.MONTH,
                Calendar.SHORT,
                Locale.getDefault()
            )
        )
        sb.append(", ")
        sb.append(mROZmanimCalendar.calendar.get(Calendar.YEAR))

        if (DateUtils.isToday(mROZmanimCalendar.calendar.time.time)) {
            sb.append("   ▼   ") //add a down arrow to indicate that this is the current day
        } else {
            sb.append("      ")
        }

        sb.append(
            mJewishDateInfo.jewishCalendar.toString()
                .replace("Teves", "Tevet")
                .replace("Tishrei", "Tishri")
        )

        zmanim.add(ZmanListEntry(sb.toString()))

        zmanim.add(ZmanListEntry(mJewishDateInfo.thisWeeksParsha))
        
        val haftorah = mJewishDateInfo.thisWeeksHaftarah
        if (haftorah.isNotEmpty()) {
            zmanim.add(ZmanListEntry(haftorah))
        }

        mROZmanimCalendar.calendar.add(Calendar.DATE, 1)
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        if (sharedPref.getBoolean("showShabbatMevarchim", true)) {
            if (mJewishDateInfo.jewishCalendar.isShabbosMevorchim) {
                zmanim.add(ZmanListEntry("שבת מברכים"))
            }
        }
        mROZmanimCalendar.calendar.add(Calendar.DATE, -1)
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar) //reset


        if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
            zmanim.add(
                ZmanListEntry(
                    mROZmanimCalendar.calendar
                        .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                )
            )
        } else {
            zmanim.add(
                ZmanListEntry(
                    (mROZmanimCalendar.calendar.getDisplayName(
                        Calendar.DAY_OF_WEEK,
                        Calendar.LONG,
                        Locale.getDefault()
                    )
                        ?.plus(" / ") ?: "") +
                            mROZmanimCalendar.calendar.getDisplayName(
                                Calendar.DAY_OF_WEEK,
                                Calendar.LONG,
                                Locale("he", "IL")
                            )
                )
            )
        }

        val day: String = mJewishDateInfo.getSpecialDay(false)
        if (day.isNotEmpty()) {
            zmanim.add(ZmanListEntry(day))
        }
        val dayOfOmer = mJewishDateInfo.addDayOfOmer("")
        if (dayOfOmer.isNotEmpty()) {
            zmanim.add(ZmanListEntry(dayOfOmer))
        }

        if (mJewishDateInfo.jewishCalendar.yomTovIndex == JewishCalendar.ROSH_HASHANA &&
            mJewishDateInfo.isShmitaYear
        ) {
            zmanim.add(ZmanListEntry(getString(R.string.this_year_is_a_shmita_year)))
        }

        if (mJewishDateInfo.is3Weeks) {
            if (mJewishDateInfo.is9Days) {
                if (mJewishDateInfo.isShevuahShechalBo) {
                    zmanim.add(ZmanListEntry(getString(R.string.shevuah_shechal_bo)))
                } else {
                    zmanim.add(ZmanListEntry(getString(R.string.nine_days)))
                }
            } else {
                zmanim.add(ZmanListEntry(getString(R.string.three_weeks)))
            }
        }

        val isOKToListenToMusic: String = mJewishDateInfo.isOKToListenToMusic()
        if (isOKToListenToMusic.isNotEmpty()) {
            zmanim.add(ZmanListEntry(isOKToListenToMusic))
        }

        val hallel: String = mJewishDateInfo.hallelOrChatziHallel
        if (hallel.isNotEmpty()) {
            zmanim.add(ZmanListEntry(hallel))
        }

        val ulChaparatPesha: String = mJewishDateInfo.isUlChaparatPeshaSaid
        if (ulChaparatPesha.isNotEmpty()) {
            zmanim.add(ZmanListEntry(ulChaparatPesha))
        }

        zmanim.add(ZmanListEntry(mJewishDateInfo.isTachanunSaid))

        val birchatLevana: String = mJewishDateInfo.birchatLevana
        if (birchatLevana.isNotEmpty()) {
            zmanim.add(ZmanListEntry(birchatLevana))
        }

        if (mJewishDateInfo.jewishCalendar.isBirkasHachamah) {
            zmanim.add(ZmanListEntry(getString(R.string.birchat_hachamah_is_said_today)))
        }

        val tekufaOpinions: String? = sharedPref.getString("TekufaOpinions", "1")
        if (tekufaOpinions == "1" || tekufaOpinions == null) {
            addTekufaTime()
        }
        if (tekufaOpinions == "2") {
            addAmudeiHoraahTekufaTime()
        }
        if (tekufaOpinions == "3") {
            addTekufaTime()
            addAmudeiHoraahTekufaTime()
        }

        addZmanim(zmanim)

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.daf_yomi) + " " + YomiCalculator.getDafYomiBavli(
                        mJewishDateInfo.jewishCalendar
                    ).masechta
                            + " " +
                            mHebrewDateFormatter.formatHebrewNumber(
                                YomiCalculator.getDafYomiBavli(
                                    mJewishDateInfo.jewishCalendar
                                ).daf
                            ),
                    mJewishDateInfo.jewishCalendar.gregorianCalendar.time, false
                )
            )
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            val dafYomiYerushalmi: Daf? =
                YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.jewishCalendar)
            if (dafYomiYerushalmi != null) {
                val masechta: String = dafYomiYerushalmi.yerushalmiMasechta
                val daf: String = mHebrewDateFormatter.formatHebrewNumber(dafYomiYerushalmi.daf)
                zmanim.add(ZmanListEntry(getString(R.string.yerushalmi_yomi) + " " + masechta + " " + daf))
            } else {
                zmanim.add(ZmanListEntry(getString(R.string.no_daf_yomi_yerushalmi)))
            }
        }

        zmanim.add(
            ZmanListEntry(
                mJewishDateInfo.isMashivHaruchOrMoridHatalSaid
                        + " / "
                        + mJewishDateInfo.isBarcheinuOrBarechAleinuSaid
            )
        )

        if (!sharedPref.getBoolean("LuachAmudeiHoraah", false)) {
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(
                        mROZmanimCalendar.shaahZmanisGra.toDouble()
                    )
                            + " / " + getString(R.string.mg_a) + " " + mZmanimFormatter.format(
                        mROZmanimCalendar.shaahZmanis72MinutesZmanis.toDouble()
                    )
                )
            )
        } else {
            val shaahZmanitMGA: Long = mROZmanimCalendar.getTemporalHour(
                mROZmanimCalendar.alotAmudeiHoraah,
                mROZmanimCalendar.tzais72ZmanisAmudeiHoraah
            )
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(
                        mROZmanimCalendar.shaahZmanisGra.toDouble()
                    )
                            + " / " + getString(R.string.mg_a) + " " + mZmanimFormatter.format(
                        shaahZmanitMGA.toDouble()
                    )
                )
            )
        }

        if (sharedPref.getBoolean("ShowElevation", false)) {
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.elevation) + " " + mROZmanimCalendar.geoLocation.elevation + " " + getString(
                        R.string.meters
                    )
                )
            )
        }
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

    private fun addTekufaTime() {
        val zmanimFormat: DateFormat =
            if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                SimpleDateFormat("H:mm", Locale.getDefault())
            } else {
                SimpleDateFormat("h:mm aa", Locale.getDefault())
            }
        zmanimFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone
        mROZmanimCalendar.calendar.add(
            Calendar.DATE,
            1
        ) //check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        mROZmanimCalendar.calendar.add(Calendar.DATE, -1) //reset the calendar
        if (mJewishDateInfo.jewishCalendar.tekufa != null) {
            val cal1 = mROZmanimCalendar.calendar.clone() as Calendar
            val cal2 = mROZmanimCalendar.calendar.clone() as Calendar
            cal2.time =
                mJewishDateInfo.jewishCalendar.tekufaAsDate // should not be null in this if block
            if (cal1[Calendar.ERA] == cal2[Calendar.ERA] && cal1[Calendar.YEAR] == cal2[Calendar.YEAR] && cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]) {
                if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                    zmanim.add(
                        ZmanListEntry(
                            "תקופת " + mJewishDateInfo.jewishCalendar.tekufaName +
                                    " היום בשעה " + zmanimFormat.format(mJewishDateInfo.jewishCalendar.tekufaAsDate)
                        )
                    )
                } else {
                    zmanim.add(
                        ZmanListEntry(
                            "Tekufa " + mJewishDateInfo.jewishCalendar.tekufaName + " is today at " +
                                    zmanimFormat.format(mJewishDateInfo.jewishCalendar.tekufaAsDate)
                        )
                    )
                }
            }
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar) //reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (mJewishDateInfo.jewishCalendar.tekufa != null) {
            val cal1 = mROZmanimCalendar.calendar.clone() as Calendar
            val cal2 = mROZmanimCalendar.calendar.clone() as Calendar
            cal2.time =
                mJewishDateInfo.jewishCalendar.tekufaAsDate // should not be null in this if block
            if (cal1[Calendar.ERA] == cal2[Calendar.ERA] && cal1[Calendar.YEAR] == cal2[Calendar.YEAR] && cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]) {
                if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                    zmanim.add(
                        ZmanListEntry(
                            "תקופת " + mJewishDateInfo.jewishCalendar.tekufaName +
                                    " היום בשעה " + zmanimFormat.format(mJewishDateInfo.jewishCalendar.tekufaAsDate)
                        )
                    )
                }
            } else {
                zmanim.add(
                    ZmanListEntry(
                        "Tekufa " + mJewishDateInfo.jewishCalendar.tekufaName + " is today at " +
                                zmanimFormat.format(mJewishDateInfo.jewishCalendar.tekufaAsDate)
                    )
                )
            }
        }
    }

    private fun addAmudeiHoraahTekufaTime() {
        val zmanimFormat: DateFormat =
            if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                SimpleDateFormat("H:mm", Locale.getDefault())
            } else {
                SimpleDateFormat("h:mm aa", Locale.getDefault())
            }
        zmanimFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone
        mROZmanimCalendar.calendar.add(
            Calendar.DATE,
            1
        ) //check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        mROZmanimCalendar.calendar.add(Calendar.DATE, -1) //reset the calendar
        if (mJewishDateInfo.jewishCalendar.tekufa != null) {
            val cal1 = mROZmanimCalendar.calendar.clone() as Calendar
            val cal2 = mROZmanimCalendar.calendar.clone() as Calendar
            cal2.time =
                mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate // should not be null in this if block
            if (cal1[Calendar.ERA] == cal2[Calendar.ERA] && cal1[Calendar.YEAR] == cal2[Calendar.YEAR] && cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]) {
                if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                    zmanim.add(
                        ZmanListEntry(
                            "תקופת " + mJewishDateInfo.jewishCalendar.tekufaName +
                                    " היום בשעה " + zmanimFormat.format(mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate)
                        )
                    )
                } else {
                    zmanim.add(
                        ZmanListEntry(
                            "Tekufa " + mJewishDateInfo.jewishCalendar.tekufaName + " is today at " +
                                    zmanimFormat.format(mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate)
                        )
                    )
                }
            }
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar) //reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (mJewishDateInfo.jewishCalendar.tekufa != null) {
            val cal1 = mROZmanimCalendar.calendar.clone() as Calendar
            val cal2 = mROZmanimCalendar.calendar.clone() as Calendar
            cal2.time =
                mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate // should not be null in this if block
            if (cal1[Calendar.ERA] == cal2[Calendar.ERA] && cal1[Calendar.YEAR] == cal2[Calendar.YEAR] && cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]) {
                if (Locale.getDefault().getDisplayLanguage(Locale("en", "US")) == "Hebrew") {
                    zmanim.add(
                        ZmanListEntry(
                            "תקופת " + mJewishDateInfo.jewishCalendar.tekufaName +
                                    " היום בשעה " + zmanimFormat.format(mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate)
                        )
                    )
                } else {
                    zmanim.add(
                        ZmanListEntry(
                            "Tekufa " + mJewishDateInfo.jewishCalendar.tekufaName + " is today at " +
                                    zmanimFormat.format(mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate)
                        )
                    )
                }
            }
        }
    }

    private fun setNextUpcomingZman() {
        var theZman: Date? = null
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
            if (zman != null && zman.after(Date()) && (theZman == null || zman.before(theZman))) {
                theZman = zman
            }
        }
        sNextUpcomingZman = theZman
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

    private fun createBackgroundThreadForNextUpcomingZman() {
        val nextZmanUpdater = Runnable {
            setNextUpcomingZman()
            updateZmanimList()
            createBackgroundThreadForNextUpcomingZman() //start a new thread to update the next upcoming zman
        }
        if (sNextUpcomingZman != null) {
            mHandler?.postDelayed(
                nextZmanUpdater,
                sNextUpcomingZman!!.time - Date().time + 1000
            ) //add 1 second to make sure we don't get the same zman again
        }
    }

    private fun syncCalendars() {
        mROZmanimCalendar.calendar = mCurrentDateShown
        mJewishDateInfo.jewishCalendar.setDate(mCurrentDateShown)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun WearApp(zmanimList: MutableList<ZmanListEntry>) {
        if (zmanimList.size == 0) {
            zmanimList.add(ZmanListEntry(""))
        }
        val refreshScope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }

        fun refresh() = refreshScope.launch {
            refreshing = true
            locationResolver.acquireLatitudeAndLongitude()
            initZmanimCalendar()
            mCurrentDateShown = Calendar.getInstance()
            syncCalendars()
            setDateFormats() // should happen after we get the geolocation object because of the timezone
            setNextUpcomingZman()
            createBackgroundThreadForNextUpcomingZman()
            updateZmanimList()
            setContent {
                WearApp(zmanim)
            }
            refreshing = false
        }
        val state = rememberPullRefreshState(refreshing, ::refresh)
        val scalingLazyListState = rememberScalingLazyListState(initialCenterItemIndex = 0)
        val height = remember { mutableIntStateOf(1) }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        RabbiOvadiahYosefCalendarTheme {
            Scaffold(
                timeText = { TimeText(
                    endLinearContent = { Text(text = "זמני יוסף") },
                    endCurvedContent = { curvedText(text = "זמני יוסף")} ) },
                modifier = Modifier.onGloballyPositioned { height.intValue = it.size.height },
                positionIndicator = {
                    // Hack to ALWAYS show the scrollbars...Google happy now
                    PositionIndicator(
                        state = AlwaysShowScrollBarScalingLazyColumnStateAdapter(
                            state = scalingLazyListState,
                            viewportHeightPx = height,
                        ),
                        //region Original values from PositionIndicator
                        indicatorHeight = 50.dp,
                        indicatorWidth = 4.dp,
                        paddingHorizontal = 5.dp,
                        reverseDirection = false,
                        //endregion
                    )
                }
            ) {
                var swipedRight = false
                Box(modifier = Modifier
                    .pullRefresh(state)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(onDragEnd = {
                            if (swipedRight) {
                                mCurrentDateShown.add(Calendar.DATE, 1)
                            } else {
                                mCurrentDateShown.add(Calendar.DATE, -1)
                            }
                            syncCalendars()
                            updateZmanimList()
                            setContent {
                                WearApp(zmanim)
                            }
                        }) { _, dragAmount ->
                            swipedRight = dragAmount <= 0
                        }
                    }) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    ScalingLazyColumn(
                        modifier = Modifier
                            .background(MaterialTheme.colors.background)
                            .onRotaryScrollEvent {
                                coroutineScope.launch {
                                    scalingLazyListState.scrollBy(it.verticalScrollPixels)

                                    scalingLazyListState.animateScrollBy(0f)
                                }
                                true
                            }
                            .focusRequester(focusRequester)
                            .focusable(),
                        autoCentering = AutoCenteringParams(
                            itemIndex = 0
                        ),
                        state = scalingLazyListState
                    ) {
                        items(zmanimList.size) { index ->
                            if (refreshing.not()) {
                                if (zmanimList[index].isZman) {
                                    val zmanTime: String =
                                        if (zmanimList[index].isVisibleSunriseZman) {
                                            visibleSunriseFormat.format(zmanimList[index].zman)
                                        } else if (zmanimList[index].isRTZman) { // we already checked and set if the rounded up format should be used
                                            roundUpFormat.format(zmanimList[index].zman)
                                        } else { // just format it normally
                                            zmanimFormat.format(zmanimList[index].zman)
                                        }
                                    val zmanTitleAndTime: String =
                                        if (sharedPref.getBoolean("isZmanimInHebrew", false)) {
                                            zmanTime + " : " + zmanimList[index].title
                                        } else {
                                            zmanimList[index].title + " : " + zmanTime
                                        }
                                    DarkChip(
                                        text = zmanTitleAndTime,
                                        textDecoration = if (sNextUpcomingZman == zmanimList[index].zman) TextDecoration.Underline else TextDecoration.None
                                    )
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (index == 0) {
                                            if (!sharedPref.getBoolean(
                                                    "hasGottenDataFromApp",
                                                    false
                                                )
                                            ) {
                                                Box(modifier = Modifier.padding(vertical = 36.dp)) {
                                                    DarkChip(
                                                        text = getString(R.string.settings_not_recieved)
                                                    )
                                                }
                                            }
                                        }

                                        DarkChip(text = zmanimList[index].title)

                                        if (index == zmanimList.size - 1) {
                                            RedChipWithWhiteX("", onRemove = { finish() })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }

    @Composable
    fun DarkChip(text: String, textDecoration: TextDecoration = TextDecoration.None) {
        Box(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .background(DarkGray, CircleShape)
                    .padding(8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = text,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        textDecoration = textDecoration
                    )
                }
            }
        }
    }

    @Composable
    fun RedChipWithWhiteX(text: String, onRemove: () -> Unit) {
        Box(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .background(Color.Red, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.body2
                    )

                    IconButton(
                        onClick = { onRemove() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    @Preview(device = "id:wearos_square", showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
    @Composable
    fun DefaultPreview() {
        WearApp(zmanimList = arrayListOf(
            ZmanListEntry("Test"),
            ZmanListEntry("Test"),
            ZmanListEntry("Test"),
            ZmanListEntry("Test"),
        ))
    }
}