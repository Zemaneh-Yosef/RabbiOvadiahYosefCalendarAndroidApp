
package com.EJ.ROvadiahYosefCalendar.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.curvedText
import androidx.wear.tiles.TileService
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.classes.HebrewDatePickerDialog
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.classes.LocationResolver
import com.EJ.ROvadiahYosefCalendar.classes.OnChangeListener
import com.EJ.ROvadiahYosefCalendar.classes.PreferenceListener
import com.EJ.ROvadiahYosefCalendar.classes.ROZmanimCalendar
import com.EJ.ROvadiahYosefCalendar.classes.Utils
import com.EJ.ROvadiahYosefCalendar.classes.ZmanListEntry
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory.addZmanim
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimNotifications
import com.EJ.ROvadiahYosefCalendar.classes.SecondTreatment
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
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArrayList
import androidx.core.content.edit
import androidx.core.net.toUri
import com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory

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
    private var zmanim: CopyOnWriteArrayList<ZmanListEntry> = CopyOnWriteArrayList()
    private var mROZmanimCalendar = ROZmanimCalendar(GeoLocation(), null)
    private var mJewishDateInfo = JewishDateInfo(false)
    private var mLastTimeUserWasInApp: Date = Date()
    private val mHebrewDateFormatter = HebrewDateFormatter()
    private val mZmanimFormatter = ZmanimFormatter(TimeZone.getDefault())
    private lateinit var yesSecondsDFormat: SimpleDateFormat
    private lateinit var noSecondsDFormat: SimpleDateFormat
    private var showSeconds = false
    private val mHandler: Handler? = null
    private val dafYomiStartDate: Calendar = GregorianCalendar(1923, Calendar.SEPTEMBER, 11)
    private val dafYomiYerushalmiStartDate: Calendar = GregorianCalendar(1980, Calendar.FEBRUARY, 2)
    private val listener: PreferenceListener = PreferenceListener()
    private var sNotificationLauncher: ActivityResultLauncher<Intent>? = null
    private var nextUpcomingZmanIndex = 0

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
                sharedPref.edit { putBoolean("hasGottenDataFromApp", true) }
            }
            if (messageEvent.path == "chaiTable/") {
                val message = String(messageEvent.data, StandardCharsets.UTF_8) // convert bytes to String
                sharedPref.edit {
                    putString(
                        "chaiTable" + Utils.removePostalCode(sharedPref.getString("locationName", "")),
                        message
                    )
                } // The location name should be set already from the previous message
            }
            updateAppContents() // with the new preferences
        }, this)
        startService(Intent(this, listener.javaClass))
        updateAppContents()

        // To test JSON object transfer, uncomment:

//        savePreferencesToLocalDevice(JSONTest.getJSONPreferencesObject(sharedPref, sharedPref))
//        sharedPref.edit {
//            putString("chaiTable" + Utils.removePostalCode(sharedPref.getString("locationName", "")), "1766319591:1768825127:1771415244:1773918088:1776507280:1779010685:1781601992:1784108342:1786702047:1766406020:1768911495:1771501579:1774004396:1776593595:1779097028:1781688399:1784194785:1786788502:1766492447:1768997858:1771587905:1774090697:1776679914:1779183371:1781774806:1784281229:1786874961:1766578872:1769084212:1771674221:1774176989:1776766236:1779269725:1781861214:1784367681:1786961423:1766665294:1769170572:1771760536:1774263295:1776852544:1779356079:1781947624:1784454128:1787047885:1766751715:1769256934:1771846856:1774349601:1776938848:1779442436:1782034037:1784540572:1787134347:1766838134:1769343295:1771933174:1774435901:1777025163:1779528794:1782120451:1784627022:1787220811:1766924550:1769429651:1772019486:1774522193:1777111474:1779615159:1782206867:1784713472:1787307278:1767010964:1769516002:1772105797:1774608484:1777197789:1779701521:1782293284:1784799920:1787393732:1767097376:1769602358:1772192099:1774694787:1777284107:1779787883:1782379701:1784886373:1787480184:1764590649:1767183783:1769688711:1772278395:1774781090:1777370431:1779874248:1782466122:1784972830:1787566636:1764677111:1767270191:1769775061:1772364721:1774867391:1777456751:1779960618:1782552545:1785059292:1787653090:1764763568:1767356599:1769861405:1772451046:1774953692:1777543060:1780046986:1782638967:1785145750:1787739540:1764850026:1767443002:1769947746:1772537358:1775039993:1777629367:1780133358:1782725393:1785232198:1787825981:1764936481:1767529402:1770034085:1772623664:1775126287:1777715678:1780219734:1782811821:1785318649:1787912425:1765022936:1767615799:1770120406:1772709970:1775212584:1777801996:1780306111:1782898250:1785405101:1787998879:1765109393:1767702195:1770206734:1772796268:1775298894:1777888315:1780392499:1782984678:1785491567:1788085330:1765195846:1767788592:1770293067:1772882543:1775385202:1777974637:1780478886:1783071111:1785578025:1788171786:1765282297:1767874983:1770379397:1772968842:1775471512:1778060961:1780565269:1783157544:1785664480:1788258245:1765368745:1767961375:1770465728:1773055136:1775557823:1778147289:1780651655:1783243979:1785750927:1788344701:1765455194:1768047763:1770552057:1773141429:1775644139:1778233618:1780738043:1783330408:1785837385:1788431151:1765541644:1768134145:1770638383:1773227732:1775730447:1778319945:1780824433:1783416843:1785923849:1788517605:1765628090:1768220527:1770724703:1773314014:1775816757:1778406294:1780910820:1783503277:1786010310:1788604054:1765714534:1768306909:1770811021:1773400319:1775903064:1778492632:1780997209:1783589713:1786096771:1788690505:1765800976:1768393282:1770897341:1773486610:1775989371:1778578971:1781083603:1783676142:1786183234:1788776959:1765887416:1768479653:1770983658:1773572917:1776075682:1778665301:1781169995:1783762574:1786269701:1788863410:1765973855:1768566032:1771069991:1773659221:1776161997:1778751646:1781256390:1783849011:1786356167:1788949865:1766060292:1768652406:1771156320:1773745507:1776248315:1778837994:1781342787:1783935453:1786442639:1789036329:1766146727:1768738766:1771242623:1773831792:1776334638:1778924342:1781429186:1784021896:1786529113:1789122795:1766233160:1771328933:1776420962:1781515589:1786615585:")
//            putBoolean("hasGottenDataFromApp", true)
//        }
    }

    private fun savePreferencesToLocalDevice(jsonPreferences: JSONObject) {
        val editor = sharedPref.edit()

        try {
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
                .putString(
                    "elevation" + jsonPreferences.getString("locationName"),
                    jsonPreferences.getString("elevation" + jsonPreferences.getString("locationName"))
                )//use the locationName in JSON since we do not know if the location name is the same in the watch
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
        } catch (e:JSONException) {
            e.printStackTrace()
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.error)
            builder.setMessage(getString(R.string.json_error))
            builder.setCancelable(false)
            builder.setNeutralButton(getString(R.string.ok)) { dialog: DialogInterface?, _: Int ->
                dialog?.dismiss()
            }
            builder.show()
        }
    }

    private fun showHebrewDatePickerDialog() {
        val hebrewDatePickerDialog = HebrewDatePickerDialog(
            this, this,
            { _, selectedYear, selectedMonth, selectedDay ->
                mCurrentDateShown = GregorianCalendar(selectedYear, selectedMonth, selectedDay)
            },
            mJewishDateInfo.jewishCalendar,
            {
                syncCalendars()
                updateAppContents() },
            { showDatePickerDialog() },
            {  }//Do nothing the dismiss function will be called internally
        )
        hebrewDatePickerDialog.show()
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(this)
        datePickerDialog.datePicker.init(
            mCurrentDateShown.get(Calendar.YEAR),
            mCurrentDateShown.get(Calendar.MONTH),
            mCurrentDateShown.get(Calendar.DAY_OF_MONTH))
        { _, selectedYear, selectedMonth, selectedDay ->
            mCurrentDateShown = GregorianCalendar(selectedYear, selectedMonth, selectedDay)
        }
        datePickerDialog.setButton(BUTTON_POSITIVE, getString(R.string.ok)) { _, _ ->
            syncCalendars()
            updateAppContents()
        }
        datePickerDialog.setButton(BUTTON_NEUTRAL, getString(R.string.switch_calendar)) { dialog, _ ->
            dialog.dismiss()
            showHebrewDatePickerDialog()
        }
        datePickerDialog.setButton(-2, getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        datePickerDialog.show()
    }

    override fun onResume() {
        TileService.getUpdater(applicationContext).requestUpdate(MainTileService::class.java)
        if (System.currentTimeMillis() - mLastTimeUserWasInApp.time > 120_000) {
            mCurrentDateShown.time = Date()
            sharedPref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE) //sharedPref could be null if the app was not used for a while
            syncCalendars()
            updateAppContents()
            mLastTimeUserWasInApp = Date()
        }
        return super.onResume()
    }

    private fun updateAppContents() {
        OnChangeListener.addListener {
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
            sharedPref.edit { putString("name", sCurrentLocationName) }
            setDateFormats() // should happen after we get the geolocation object because of the timezone
            updateZmanimList()
            setNextUpcomingZman()
            createBackgroundThreadForNextUpcomingZman()
            OnChangeListener.notifyListeners()
            OnChangeListener.removeAllListeners()
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
                        sharedPref.edit { putBoolean("hasAskedForNotificationPermissions", true) }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !(getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) { // more annoying android permission garbage
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.zmanim_notifications_will_not_work)
                builder.setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications)
                builder.setCancelable(false)
                builder.setPositiveButton(getString(R.string.yes)) { _: DialogInterface?, _: Int ->
                    sNotificationLauncher?.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri()))
                }
                builder.setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                if (!isFinishing && !isDestroyed) {
                    builder.show()
                }
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
                TimeZone.getTimeZone(if (sCurrentTimeZoneID != "") sCurrentTimeZoneID else TimeZone.getDefault().id)
            ),
            sharedPref
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
        mROZmanimCalendar.calendar = mCurrentDateShown
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
        var secondFormatPattern = "H:mm:ss"
        if (!Utils.isLocaleHebrew()) {
            secondFormatPattern = "h:mm:ss aa"
        }
        yesSecondsDFormat = SimpleDateFormat(secondFormatPattern, Locale.getDefault())
        yesSecondsDFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone

        showSeconds = sharedPref.getBoolean("ShowSeconds", false)

        var noSecondFormatPattern = "H:mm"
        if (!Utils.isLocaleHebrew()) {
            noSecondFormatPattern = "h:mm aa"
        }
        noSecondsDFormat = SimpleDateFormat(noSecondFormatPattern, Locale.getDefault())
        noSecondsDFormat.timeZone = mROZmanimCalendar.geoLocation.timeZone
    }

    private fun updateZmanimList() {
        zmanim = CopyOnWriteArrayList()

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
            sb.append("\n   ▼   \n") //add a down arrow to indicate that this is the current day
        } else {
            sb.append("\n")
        }

        sb.append(
            mJewishDateInfo.jewishCalendar.toString()
        )

        zmanim.add(ZmanListEntry(sb.toString()))

        zmanim.add(ZmanListEntry(mJewishDateInfo.thisWeeksParsha))
        
        val haftorah = mJewishDateInfo.thisWeeksHaftarah
        if (haftorah.isNotEmpty()) {
            zmanim.add(ZmanListEntry(haftorah))
        }

        if (sharedPref.getBoolean("showShabbatMevarchim", true)) {
            if (mJewishDateInfo.tomorrow().jewishCalendar.isShabbosMevorchim) {
                zmanim.add(ZmanListEntry("שבת מברכים"))
            }
        }

        if (Utils.isLocaleHebrew()) {
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
                                Locale.Builder().setLanguage("he").setRegion("IL").build()
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

        if (mJewishDateInfo.jewishCalendar.isRoshHashana && mJewishDateInfo.isShmitaYear) {
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

        if (mJewishDateInfo.isPurimMeshulash) {
            zmanim.add(ZmanListEntry(getString(R.string.no_tachanun_in_yerushalayim_or_a_safek_mukaf_choma)))
        }

        val birchatLevana: String = mJewishDateInfo.birchatLevana
        if (birchatLevana.isNotEmpty()) {
            zmanim.add(ZmanListEntry(birchatLevana))
        }

        if (mJewishDateInfo.jewishCalendar.isBirkasHachamah) {
            zmanim.add(ZmanListEntry(getString(R.string.birchat_hachamah_is_said_today)))
        }

        if (mJewishDateInfo.tomorrow().jewishCalendar.dayOfWeek == Calendar.SATURDAY
            && mJewishDateInfo.tomorrow().jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            zmanim.add(ZmanListEntry(getString(R.string.burn_your_ametz_today)))
        }

        val tekufaOpinions: String? = sharedPref.getString("TekufaOpinions", "1")
        when (tekufaOpinions) {
            "1" -> if (sharedPref.getBoolean("LuachAmudeiHoraah", false)) {
                addAmudeiHoraahTekufaTime()
            } else {
                addTekufaTime()
            }
            "2" -> addTekufaTime()
            "3" -> addAmudeiHoraahTekufaTime()
            else -> {
                addAmudeiHoraahTekufaTime()
                addTekufaTime()
            }
        }
        addTekufaLength(zmanim, tekufaOpinions)

        addZmanim(zmanim, false, sharedPref, mROZmanimCalendar, mJewishDateInfo, sharedPref.getBoolean("isZmanimInHebrew", false), sharedPref.getBoolean("isZmanimEnglishTranslated", false), true)

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
                            )
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
                )
            )
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.mg_a) + " (" + getString(R.string.ohr_hachaim) + ") " + mZmanimFormatter.format(
                       mROZmanimCalendar.shaahZmanis72MinutesZmanis.toDouble()
                    )
                )
            )
        } else {
            val shaahZmanitMGA: Long = mROZmanimCalendar.getTemporalHour(
                mROZmanimCalendar.getAlotAmudeiHoraah(),
                mROZmanimCalendar.getTzais72ZmanisAmudeiHoraah()
            )
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(
                       mROZmanimCalendar.shaahZmanisGra.toDouble()
                    )
                )
            )
            zmanim.add(
                ZmanListEntry(
                    getString(R.string.mg_a) + " (" + getString(R.string.amudei_horaah) + ") " + mZmanimFormatter.format(
                        shaahZmanitMGA.toDouble()
                    )
                )
            )
        }

        if (sharedPref.getBoolean("ShowLeapYear", false)) {
            zmanim.add(ZmanListEntry(mJewishDateInfo.isJewishLeapYear()))
        }

        if (sharedPref.getBoolean("ShowDST", false)) {
            if (mROZmanimCalendar.getGeoLocation().getTimeZone()
                    .inDaylightTime(mROZmanimCalendar.getSeaLevelSunrise())
            ) {
                zmanim.add(ZmanListEntry(getString(R.string.daylight_savings_time_is_on)))
            } else {
                zmanim.add(ZmanListEntry(getString(R.string.daylight_savings_time_is_off)))
            }
        }

        if (sharedPref.getBoolean("ShowShmitaYear", false)) {
            when (mJewishDateInfo.yearOfShmitaCycle) {
                1 -> zmanim.add(ZmanListEntry(getString(R.string.first_year_of_shmita)))
                2 -> zmanim.add(ZmanListEntry(getString(R.string.second_year_of_shmita)))
                3 -> zmanim.add(ZmanListEntry(getString(R.string.third_year_of_shmita)))
                4 -> zmanim.add(ZmanListEntry(getString(R.string.fourth_year_of_shmita)))
                5 -> zmanim.add(ZmanListEntry(getString(R.string.fifth_year_of_shmita)))
                6 -> zmanim.add(ZmanListEntry(getString(R.string.sixth_year_of_shmita)))
                else -> zmanim.add(ZmanListEntry(getString(R.string.this_year_is_a_shmita_year)))
            }
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

    private fun addTekufaTime() {
        val zmanimFormat: DateFormat =
            if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
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
                if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
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
                if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
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
            if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
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
                if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
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
                if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
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

    private fun addTekufaLength(zmanim: MutableList<ZmanListEntry>, opinion: String?) {
        val millisPerHour = 3_600_000
        val zmanimFormat: DateFormat = if (Locale.getDefault().getDisplayLanguage(Locale.Builder().setLanguage("en").setRegion("US").build()) == "Hebrew") {
            SimpleDateFormat("H:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm aa", Locale.getDefault())
        }
        zmanimFormat.timeZone = TimeZone.getTimeZone(sCurrentTimeZoneID)

        var tekufa: Date? = null
        var aHTekufa: Date? = null

        mROZmanimCalendar.calendar.add(Calendar.DATE, 1) //check next day for tekufa, because the tekufa time can go back a day
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar)
        mROZmanimCalendar.calendar.add(Calendar.DATE, -1) //reset the calendar to check for the current date

        if (mJewishDateInfo.jewishCalendar.tekufa != null) {
            val cal1 = mROZmanimCalendar.calendar.clone() as Calendar
            val cal2 = mROZmanimCalendar.calendar.clone() as Calendar
            cal2.time = mJewishDateInfo.jewishCalendar.tekufaAsDate // should not be null in this if block

            if (cal1[Calendar.ERA] == cal2[Calendar.ERA] && cal1[Calendar.YEAR] == cal2[Calendar.YEAR] && cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]) {
                tekufa = mJewishDateInfo.jewishCalendar.tekufaAsDate
                aHTekufa = mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate
            }
        }
        mJewishDateInfo.setCalendar(mROZmanimCalendar.calendar) //reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (mJewishDateInfo.jewishCalendar.tekufa != null) {
            val cal1 = mROZmanimCalendar.calendar.clone() as Calendar
            val cal2 = mROZmanimCalendar.calendar.clone() as Calendar
            cal2.time = mJewishDateInfo.jewishCalendar.tekufaAsDate // should not be null in this if block

            if (cal1[Calendar.ERA] == cal2[Calendar.ERA] && cal1[Calendar.YEAR] == cal2[Calendar.YEAR] && cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]) {
                tekufa = mJewishDateInfo.jewishCalendar.tekufaAsDate
                aHTekufa = mJewishDateInfo.jewishCalendar.amudeiHoraahTekufaAsDate
            }
        }

        if (tekufa != null && aHTekufa != null) {
            val halfHourBefore: Date?
            val halfHourAfter: Date?
            when (opinion) {
                "1" -> {
                    if (sharedPref.getBoolean("LuachAmudeiHoraah", false)) {
                        halfHourBefore = Date(aHTekufa.time - (millisPerHour / 2))
                        halfHourAfter = Date(aHTekufa.time + (millisPerHour / 2))
                    } else {
                        halfHourBefore = Date(tekufa.time - (millisPerHour / 2))
                        halfHourAfter = Date(tekufa.time + (millisPerHour / 2))
                    }
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourAfter
                                ) + " - " + zmanimFormat.format(halfHourBefore)
                            )
                        )
                    } else {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourBefore
                                ) + " - " + zmanimFormat.format(halfHourAfter)
                            )
                        )
                    }
                }

                "2" -> {
                    halfHourBefore = Date(tekufa.time - (millisPerHour / 2))
                    halfHourAfter = Date(tekufa.time + (millisPerHour / 2))
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourAfter
                                ) + " - " + zmanimFormat.format(halfHourBefore)
                            )
                        )
                    } else {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourBefore
                                ) + " - " + zmanimFormat.format(halfHourAfter)
                            )
                        )
                    }
                }

                "3" -> {
                    halfHourBefore = Date(aHTekufa.time - (millisPerHour / 2))
                    halfHourAfter = Date(aHTekufa.time + (millisPerHour / 2))
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourAfter
                                ) + " - " + zmanimFormat.format(halfHourBefore)
                            )
                        )
                    } else {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourBefore
                                ) + " - " + zmanimFormat.format(halfHourAfter)
                            )
                        )
                    }
                }

                else -> {
                    halfHourBefore = Date(aHTekufa.time - (millisPerHour / 2))
                    halfHourAfter = Date(tekufa.time + (millisPerHour / 2))
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourAfter
                                ) + " - " + zmanimFormat.format(halfHourBefore)
                            )
                        )
                    } else {
                        zmanim.add(
                            ZmanListEntry(
                                getString(R.string.tekufa_length) + zmanimFormat.format(
                                    halfHourBefore
                                ) + " - " + zmanimFormat.format(halfHourAfter)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun setNextUpcomingZman() {
        sNextUpcomingZman = ZmanimFactory.getNextUpcomingZman(Calendar.getInstance(), mROZmanimCalendar, mJewishDateInfo, sharedPref).zman
        setNextUpcomingZmanIndex()
    }

    private fun setNextUpcomingZmanIndex() {
        for ((index, entry) in zmanim.asSequence().withIndex()) {
            if (entry.zman == sNextUpcomingZman) {
                nextUpcomingZmanIndex = index
            }
        }
    }

    private fun createBackgroundThreadForNextUpcomingZman() {
        val nextZmanUpdater = Runnable {
            updateZmanimList()
            setNextUpcomingZman()
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
        if (zmanimList.isEmpty()) {
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
            updateZmanimList()
            setNextUpcomingZman()
            createBackgroundThreadForNextUpcomingZman()
            setContent {
                WearApp(zmanim)
            }
            refreshing = false
        }
        val pullRefreshState = rememberPullRefreshState(refreshing, ::refresh)
        val scalingLazyListState = rememberScalingLazyListState(initialCenterItemIndex = nextUpcomingZmanIndex)
        val drag = remember { mutableFloatStateOf(1.0f) }
        val height = remember { mutableIntStateOf(1) }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (drag.floatValue == 1.0f) {
                        Thread {
                            while (drag.floatValue < 2.0f) {
                                drag.floatValue += 0.05f
                                Thread.sleep(10)
                            }
                        }.start()
                    }
                    return super.onPostScroll(consumed, available, source)
                }
            }
        }
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
                    .pullRefresh(pullRefreshState)
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
                            .nestedScroll(nestedScrollConnection)
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
                                    val zmanTime: String
                                    if (showSeconds || zmanimList[index].secondTreatment == SecondTreatment.ALWAYS_DISPLAY)
                                        zmanTime = yesSecondsDFormat.format(zmanimList[index].zman)
                                    else {
                                        val calendar = Calendar.getInstance()
                                        calendar.time = zmanimList[index].zman

                                        var zmanDate = zmanimList[index].zman
                                        if (calendar[Calendar.SECOND] > 40 || calendar[Calendar.SECOND] > 20 && zmanimList[index].secondTreatment === SecondTreatment.ROUND_LATER) {
                                            zmanDate = Utils.addMinuteToZman(zmanimList[index].zman)
                                        }

                                        zmanTime = noSecondsDFormat.format(zmanDate)
                                    }

                                    val zmanTitleAndTime: String =
                                        if (sharedPref.getBoolean("isZmanimInHebrew", false)) {
                                            zmanTime + " :" + zmanimList[index].title
                                        } else {
                                            zmanimList[index].title + ": " + zmanTime
                                        }
                                    DarkChip(
                                        text = zmanTitleAndTime,
                                        upcoming = sNextUpcomingZman == zmanimList[index].zman,
                                        drag = drag.floatValue
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
                                                        text = getString(R.string.settings_not_recieved), drag = 1f
                                                    )
                                                }
                                            }
                                        }

                                        DarkChip(text = zmanimList[index].title, upcoming = index == 1, drag = if (index == 1) 2f else 1f, onClick = {if (index == 1) showDatePickerDialog() }, isEnabled = index == 1)

                                        if (index == zmanimList.size - 1) {
                                            RedChipWithWhiteX("", onRemove = { finish() })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }

    @Composable
    fun DarkChip(text: String, upcoming: Boolean = false, drag: Float, onClick: () -> Unit = { }, isEnabled: Boolean = false) {
        var modifier = Modifier
            .clickable(onClick = onClick, enabled = isEnabled)
            .background(
                if (upcoming) DarkGray else Color.Transparent,
                if (resources.configuration.isScreenRound) CircleShape else RoundedCornerShape(2)
            )
            .fillMaxWidth()

        modifier = if (upcoming && drag <= 2.0f) {
            modifier.aspectRatio(drag)
        } else {
            modifier.padding(8.dp)
        }
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary
            )
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

    @Preview(device = "id:wearos_large_round", showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
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