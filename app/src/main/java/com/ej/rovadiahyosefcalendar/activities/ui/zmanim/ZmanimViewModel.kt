package com.ej.rovadiahyosefcalendar.activities.ui.zmanim

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ej.rovadiahyosefcalendar.BuildConfig
import com.ej.rovadiahyosefcalendar.R
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo
import com.ej.rovadiahyosefcalendar.classes.LocationResolver
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment
import com.ej.rovadiahyosefcalendar.classes.Utils
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications
import com.ej.rovadiahyosefcalendar.notifications.NotificationUtils
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.DateUtils
import org.shredzone.commons.suncalc.MoonTimes
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.regex.Pattern
import androidx.core.content.edit

/**
 * Sealed class representing the loading state of the zmanim data.
 */
sealed class ZmanimUiState {
    object Loading : ZmanimUiState()
    data class Success(val zmanim: List<ZmanListEntry>) : ZmanimUiState()
    data class Error(val message: String) : ZmanimUiState()
}

class ZmanimViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // ---- UI State Flows ----

    private val _dailyZmanimState = MutableStateFlow<ZmanimUiState>(ZmanimUiState.Loading)
    val dailyZmanimState: StateFlow<ZmanimUiState> = _dailyZmanimState.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _isShabbatMode = MutableStateFlow(false)
    val isShabbatMode: StateFlow<Boolean> = _isShabbatMode.asStateFlow()

    // Emits once whenever the zmanim list has been refreshed, so fragments can
    // react (e.g. scroll back to saved position) without coupling to the list itself.
    private val _zmanimRefreshTick = MutableStateFlow(0L)
    val zmanimRefreshTick: StateFlow<Long> = _zmanimRefreshTick.asStateFlow()

    // ---- Internal state ----

    var nextUpcomingZman: Date? = null
        private set

    private var nextZmanJob: Job? = null
    private lateinit var locationResolver: LocationResolver

    // ---- Initialisation ----

    fun init(locationResolver: LocationResolver) {
        this.locationResolver = locationResolver
    }

    // ---- Public API called by Fragments ----

    /**
     * Primary entry point. Resolves location name → elevation → instantiates calendar → builds list.
     * Safe to call from any thread; all heavy work runs on [Dispatchers.IO].
     */
    fun loadZmanim() {
        viewModelScope.launch {
            _dailyZmanimState.value = ZmanimUiState.Loading
            withContext(Dispatchers.IO) {
                resolveLocationName()
                resolveElevation()
                instantiateZmanimCalendar()
                computeNextUpcomingZman()
                scheduleNextZmanRefresh()
            }
            rebuildZmanimList()
        }
    }

    /**
     * Called when the user hops to a different date (prev/next buttons or date picker).
     * Assumes [sROZmanimCalendar] and [sJewishDateInfo] have already been updated by the fragment.
     */
    fun onDateChanged() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                computeNextUpcomingZman()
            }
            rebuildZmanimList()
        }
    }

    /**
     * Called on swipe-to-refresh. Re-resolves GPS location from scratch.
     */
    fun refresh(onComplete: () -> Unit) {
        viewModelScope.launch {
            _dailyZmanimState.value = ZmanimUiState.Loading
            withContext(Dispatchers.IO) {
                sCurrentDateShown.time = Date()
                sJewishDateInfo.setCalendar(GregorianCalendar())
                resolveLocationName()
                resolveElevation()
                instantiateZmanimCalendar()
                computeNextUpcomingZman()
                scheduleNextZmanRefresh()
            }
            rebuildZmanimList()
            onComplete()
        }
    }

    // ---- Shabbat mode ----

    fun enterShabbatMode() {
        if (_isShabbatMode.value) return
        _isShabbatMode.value = true
        // Reset to today when entering shabbat mode
        sCurrentDateShown.time = Date()
        sROZmanimCalendar.setCalendar(GregorianCalendar())
        sJewishDateInfo.setCalendar(GregorianCalendar())
        scheduleMidnightRollover()
    }

    fun exitShabbatMode() {
        if (!_isShabbatMode.value) return
        _isShabbatMode.value = false
        midnightRolloverJob?.cancel()
    }

    private var midnightRolloverJob: Job? = null

    /**
     * Schedules a coroutine that wakes at 12:00:02 AM the next day and rolls the date forward.
     * Reschedules itself automatically while shabbat mode remains active.
     */
    private fun scheduleMidnightRollover() {
        midnightRolloverJob?.cancel()
        midnightRolloverJob = viewModelScope.launch {
            val now = Calendar.getInstance()
            val midnight = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                add(Calendar.DATE, 1)
            }
            val delayMs = midnight.timeInMillis - now.timeInMillis
            delay(delayMs)

            if (!_isShabbatMode.value) return@launch

            val today = Calendar.getInstance()
            sCurrentDateShown.timeInMillis = today.timeInMillis
            sROZmanimCalendar.setCalendar(today)
            sJewishDateInfo.setCalendar(today)

            withContext(Dispatchers.IO) { computeNextUpcomingZman() }
            rebuildZmanimList()

            if (_isShabbatMode.value) scheduleMidnightRollover()
        }
    }

    // ---- Zmanim list building ----

    /**
     * Rebuilds the full zmanim list on the IO dispatcher, then pushes the result to [_dailyZmanimState]
     * on the main dispatcher. Also bumps [_zmanimRefreshTick] so observers can react independently.
     */
    suspend fun rebuildZmanimList(add66MisheyakirZman: Boolean = false) {
        val zmanim = withContext(Dispatchers.IO) {
            buildZmanimList(add66MisheyakirZman)
        }
        _dailyZmanimState.value = ZmanimUiState.Success(zmanim)
        _zmanimRefreshTick.value = System.currentTimeMillis()
    }

    /**
     * Builds and returns the full ordered list of [ZmanListEntry] objects for the currently
     * selected date/location. Pure computation — no side effects, no UI calls.
     */
    fun buildZmanimList(add66MisheyakirZman: Boolean = false): List<ZmanListEntry> {
        val zmanim = mutableListOf<ZmanListEntry>()

        if (BuildConfig.DEBUG) {
            sSharedPreferences.edit {
				putString(
					"debugNotifs",
					sSharedPreferences.getString("debugNotifs", "") +
							"buildZmanimList() called:\n" +
							"userChosenDate= ${sCurrentDateShown.time}\n" +
							"sROZmanimCal= ${sROZmanimCalendar.calendar.time}\n" +
							"sJewishDateInfo= ${sJewishDateInfo.jewishCalendar.gregorianCalendar.time}\n\n"
				)
			}
        }

        // Special day banners
        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", false)) {
            if (sJewishDateInfo.jewishCalendar.isShabbosMevorchim) {
                zmanim.add(ZmanListEntry("שבת מברכים"))
            }
        }

        val specialDay = sJewishDateInfo.getSpecialDay(false)
        if (specialDay.isNotEmpty()) zmanim.add(ZmanListEntry(specialDay))

        if (sJewishDateInfo.isEruvTavshilimMadeToday()) {
            zmanim.add(ZmanListEntry(context.getString(R.string.eruv_tavshilim)));
        }

        val dayOfOmer = sJewishDateInfo.addDayOfOmer("")
        if (dayOfOmer.isNotEmpty()) zmanim.add(ZmanListEntry(dayOfOmer))

        if (sJewishDateInfo.jewishCalendar.isRoshHashana && sJewishDateInfo.isShmitaYear) {
            zmanim.add(ZmanListEntry(context.getString(R.string.this_year_is_a_shmita_year)))
        }

        if (sJewishDateInfo.is3Weeks()) {
            when {
                sJewishDateInfo.isShevuahShechalBo() -> zmanim.add(ZmanListEntry(context.getString(R.string.shevuah_shechal_bo)))
                sJewishDateInfo.is9Days() -> zmanim.add(ZmanListEntry(context.getString(R.string.nine_days)))
                else -> zmanim.add(ZmanListEntry(context.getString(R.string.three_weeks)))
            }
        }

        val music = sJewishDateInfo.isOKToListenToMusic()
        if (music.isNotEmpty()) zmanim.add(ZmanListEntry(music))

        val hallel = sJewishDateInfo.hallelOrChatziHallel
        if (hallel.isNotEmpty()) zmanim.add(ZmanListEntry(hallel))

        val ulChaparatPesha = sJewishDateInfo.isUlChaparatPeshaSaid
        if (ulChaparatPesha.isNotEmpty()) zmanim.add(ZmanListEntry(ulChaparatPesha))

        zmanim.add(ZmanListEntry(sJewishDateInfo.isTachanunSaid))

        if (sJewishDateInfo.isPurimMeshulash) {
            zmanim.add(ZmanListEntry(context.getString(R.string.no_tachanun_in_yerushalayim)))
        }

        val birchatLevana = sJewishDateInfo.birchatLevana
        if (birchatLevana.isNotEmpty()) {
            zmanim.add(ZmanListEntry(birchatLevana))
            addMoonTimes(zmanim)
        }

        if (sJewishDateInfo.jewishCalendar.isBirkasHachamah) {
            zmanim.add(ZmanListEntry(context.getString(R.string.birchat_hachamah_is_said_today)))
        }

        if (sJewishDateInfo.tomorrow().jewishCalendar.dayOfWeek == Calendar.SATURDAY &&
            sJewishDateInfo.tomorrow().jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH
        ) {
            zmanim.add(ZmanListEntry(context.getString(R.string.burn_your_ametz_today)))
        }

        // Tekufa
        val tekufaOpinion = sSettingsPreferences.getString("TekufaOpinions", "1") ?: "1"
        addTekufaEntries(zmanim, shortStyle = false, opinion = tekufaOpinion)
        addTekufaLength(zmanim, tekufaOpinion)

        // Core zmanim
        ZmanimFactory.addZmanim(zmanim, false, sSettingsPreferences, sSharedPreferences, sROZmanimCalendar, sJewishDateInfo, add66MisheyakirZman)

        // Seasonal prayer changes
        zmanim.add(ZmanListEntry(getSeasonalPrayerChanges()))

        // Shaah zmanit
        zmanim.add(ZmanListEntry("${context.getString(R.string.shaah_zmanit_gr_a)} ${formatShaahZmanis(sROZmanimCalendar.shaahZmanisGra)}"))
        zmanim.add(ZmanListEntry(
            "${context.getString(R.string.mg_a)} " +
            "(${context.getString(if (sROZmanimCalendar.isUseAmudehHoraah) R.string.amudei_horaah else R.string.ohr_hachaim)}) " +
            formatShaahZmanis(sROZmanimCalendar.shaahZmanis72MinutesZmanis)
        ))

        // Optional display items
        if (sSettingsPreferences.getBoolean("ShowLeapYear", false)) {
            zmanim.add(ZmanListEntry(sJewishDateInfo.isJewishLeapYear))
        }

        if (sSettingsPreferences.getBoolean("ShowDST", false)) {
            val inDst = sROZmanimCalendar.geoLocation.timeZone.inDaylightTime(sROZmanimCalendar.seaLevelSunrise)
            zmanim.add(ZmanListEntry(
                context.getString(if (inDst) R.string.daylight_savings_time_is_on else R.string.daylight_savings_time_is_off)
            ))
        }

        if (sSettingsPreferences.getBoolean("ShowShmitaYear", false)) {
            val stringRes = when (sJewishDateInfo.yearOfShmitaCycle) {
                1 -> R.string.first_year_of_shmita
                2 -> R.string.second_year_of_shmita
                3 -> R.string.third_year_of_shmita
                4 -> R.string.fourth_year_of_shmita
                5 -> R.string.fifth_year_of_shmita
                6 -> R.string.sixth_year_of_shmita
                else -> R.string.this_year_is_a_shmita_year
            }
            zmanim.add(ZmanListEntry(context.getString(stringRes)))
        }

        if (sSettingsPreferences.getBoolean("ShowElevation", false)) {
            zmanim.add(ZmanListEntry("${context.getString(R.string.elevation)} $sElevation ${context.getString(R.string.meters)}"))
        }

        return zmanim
    }

    // ---- Tekufa ----

    /**
     * Adds tekufa entries to [zmanim] according to [opinion].
     * Replaces the four nearly-identical Java methods with a single parameterised function.
     *
     * @param shortStyle true → "Tekufa Nissan : 4:30", false → "Tekufa Nissan is today at 4:30"
     * @param opinion    "1" auto, "2" Rav Adda, "3" Shmuel/Amudei Horaah, "4" both
     */
    fun addTekufaEntries(zmanim: MutableList<ZmanListEntry>, shortStyle: Boolean, opinion: String) {
        val useAmudei = sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)
        when (opinion) {
            "1" -> addSingleTekufa(zmanim, shortStyle, useAmudei)
            "2" -> addSingleTekufa(zmanim, shortStyle, useAmudeiHoraah = false)
            "3" -> addSingleTekufa(zmanim, shortStyle, useAmudeiHoraah = true)
            else -> { // "4" — show both
                addSingleTekufa(zmanim, shortStyle, useAmudeiHoraah = true)
                addSingleTekufa(zmanim, shortStyle, useAmudeiHoraah = false)
            }
        }
    }

    /**
     * Checks both today and tomorrow (tekufa can cross midnight) and adds an entry if
     * the tekufa falls on the currently displayed date.
     */
    private fun addSingleTekufa(
        zmanim: MutableList<ZmanListEntry>,
        shortStyle: Boolean,
        useAmudeiHoraah: Boolean
    ) {
        val isHebrew = Utils.isLocaleHebrew(context)
        val fmt = SimpleDateFormat(Utils.dateFormatPattern(context, false), context.resources.configuration.locales[0]).apply {
            timeZone = TimeZone.getTimeZone(sCurrentTimeZoneID)
        }
        val calCopy = sROZmanimCalendar.getCopy()
        val dateCopy = sJewishDateInfo.getCopy()

        // Check tomorrow first (tekufa time can go back a day)
        calCopy.calendar.add(Calendar.DATE, 1)
        dateCopy.setCalendar(calCopy.calendar)
        calCopy.calendar.add(Calendar.DATE, -1)
        tryAddTekufaForDate(zmanim, dateCopy, calCopy, fmt, shortStyle, useAmudeiHoraah, isHebrew)

        // Check today
        dateCopy.setCalendar(calCopy.calendar)
        tryAddTekufaForDate(zmanim, dateCopy, calCopy, fmt, shortStyle, useAmudeiHoraah, isHebrew)
    }

    private fun tryAddTekufaForDate(
        zmanim: MutableList<ZmanListEntry>,
        dateCopy: JewishDateInfo,
        calCopy: ROZmanimCalendar,
        fmt: DateFormat,
        shortStyle: Boolean,
        useAmudeiHoraah: Boolean,
        isHebrew: Boolean
    ) {
        if (dateCopy.jewishCalendar.tekufa == null) return
        val tekufaDate = if (useAmudeiHoraah)
            dateCopy.jewishCalendar.amudeiHoraahTekufaAsDate
        else
            dateCopy.jewishCalendar.tekufaAsDate ?: return

        val cal1 = calCopy.calendar.clone() as Calendar
        val cal2 = (calCopy.calendar.clone() as Calendar).also { it.time = tekufaDate }

        val sameDay = cal1[Calendar.ERA] == cal2[Calendar.ERA] &&
            cal1[Calendar.YEAR] == cal2[Calendar.YEAR] &&
            cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]

        if (!sameDay) return

        val name = dateCopy.jewishCalendar.getTekufaName(isHebrew)
        val time = fmt.format(tekufaDate)
        val text = if (isHebrew)
            "תקופת $name${if (shortStyle) " : " else " היום בשעה "}$time"
        else
            "Tekufa $name${if (shortStyle) " : " else " is today at "}$time"

        zmanim.add(ZmanListEntry(text))
    }

    /**
     * Adds the half-hour window around the tekufa (the time during which water should not be drunk).
     */
    fun addTekufaLength(zmanim: MutableList<ZmanListEntry>, opinion: String) {
        val fmt = SimpleDateFormat(Utils.dateFormatPattern(context, false), context.resources.configuration.locales[0]).apply {
            timeZone = TimeZone.getTimeZone(sCurrentTimeZoneID)
        }
        val calCopy = sROZmanimCalendar.getCopy()
        val dateCopy = sJewishDateInfo.getCopy()

        var tekufa: Date? = null
        var aHTekufa: Date? = null

        // Check tomorrow first
        calCopy.calendar.add(Calendar.DATE, 1)
        dateCopy.setCalendar(calCopy.calendar)
        calCopy.calendar.add(Calendar.DATE, -1)
        if (dateCopy.jewishCalendar.tekufa != null) {
            val t = dateCopy.jewishCalendar.tekufaAsDate ?: return
            val cal1 = calCopy.calendar.clone() as Calendar
            val cal2 = (calCopy.calendar.clone() as Calendar).also { it.time = t }
            if (isSameDay(cal1, cal2)) {
                tekufa = t
                aHTekufa = dateCopy.jewishCalendar.amudeiHoraahTekufaAsDate
            }
        }

        // Check today
        dateCopy.setCalendar(calCopy.calendar)
        if (tekufa == null && dateCopy.jewishCalendar.tekufa != null) {
            val t = dateCopy.jewishCalendar.tekufaAsDate ?: return
            val cal1 = calCopy.calendar.clone() as Calendar
            val cal2 = (calCopy.calendar.clone() as Calendar).also { it.time = t }
            if (isSameDay(cal1, cal2)) {
                tekufa = t
                aHTekufa = dateCopy.jewishCalendar.amudeiHoraahTekufaAsDate
            }
        }

        if (tekufa == null || aHTekufa == null) return

        val useAmudei = sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)
        val isHebrew = Utils.isLocaleHebrew(context)
        val halfHour = DateUtils.MILLIS_PER_HOUR / 2

        val (before, after) = when (opinion) {
            "1" -> {
                val anchor = if (useAmudei) aHTekufa else tekufa
                Date(anchor.time - halfHour) to Date(anchor.time + halfHour)
            }
            "2" -> Date(tekufa.time - halfHour) to Date(tekufa.time + halfHour)
            "3" -> Date(aHTekufa.time - halfHour) to Date(aHTekufa.time + halfHour)
            else -> Date(aHTekufa.time - halfHour) to Date(tekufa.time + halfHour)
        }

        val text = if (isHebrew)
            "${context.getString(R.string.tekufa_length)}${fmt.format(after)} - ${fmt.format(before)}"
        else
            "${context.getString(R.string.tekufa_length)}${fmt.format(before)} - ${fmt.format(after)}"

        zmanim.add(ZmanListEntry(text))
    }

    // ---- Seasonal prayer changes ----

    fun getSeasonalPrayerChanges(): String {
        val rules = TefilaRules()
        return when {
            rules.isMashivHaruachEndDate(sJewishDateInfo.jewishCalendar) -> "במוסף מוריד הטל / ברכנו"
            rules.isMashivHaruachStartDate(sJewishDateInfo.jewishCalendar) -> "במוסף משיב הרוח / ברכנו"
            rules.isVeseinTalUmatarStartDate(sJewishDateInfo.tomorrow().jewishCalendar) -> "בערבית משיב הרוח / ברך עלינו"
            else -> "${sJewishDateInfo.isMashivHaruchOrMoridHatalSaid} / ${sJewishDateInfo.isBarcheinuOrBarechAleinuSaid}"
        }
    }

    // ---- Next upcoming zman ----

    fun computeNextUpcomingZman() {
        val entry = ZmanimFactory.getNextUpcomingZman(
            sCurrentDateShown, sROZmanimCalendar, sJewishDateInfo, sSettingsPreferences, sSharedPreferences
        )
        nextUpcomingZman = if (entry?.zman != null) entry.zman
        else Date(System.currentTimeMillis() + 30_000) // retry in 30s
    }

    /**
     * Schedules a coroutine that fires just after the next upcoming zman passes,
     * then recomputes the list and reschedules itself.
     */
    private fun scheduleNextZmanRefresh() {
        nextZmanJob?.cancel()
        val target = nextUpcomingZman ?: return
        val delayMs = (target.time - Date().time + 1_000).coerceAtLeast(1_000)
        nextZmanJob = viewModelScope.launch {
            delay(delayMs)
            withContext(Dispatchers.IO) { computeNextUpcomingZman() }
            rebuildZmanimList()
            scheduleNextZmanRefresh()
        }
    }

    // ---- Location & calendar setup ----

    private suspend fun resolveLocationName() {
        locationResolver.getFullLocationName(true) { name ->
            val resolved = name?.takeIf { it.isNotEmpty() }
                ?: sROZmanimCalendar.geoLocation.locationName
            sCurrentLocationName = resolved
            _locationName.value = resolved
        }
    }

    private fun resolveElevation() {
        val isOffline = sCurrentLocationName.contains("Lat:") &&
            sCurrentLocationName.contains("Long:") &&
            sSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false)

        if (isOffline) {
            sElevation = sSharedPreferences.getString("elevation${sSharedPreferences.getString("name", "")}", "0")
                ?.toDoubleOrNull() ?: 0.0
            return
        }

        if (!sSharedPreferences.getBoolean("useElevation", true)) {
            sElevation = 0.0
            return
        }

        sElevation = if (!sSharedPreferences.contains("elevation$sCurrentLocationName")) {
            // First time for this location — fetch from web service (blocking, already on IO)
            locationResolver.getElevationFromWebServiceSync()
            sSharedPreferences.getString("elevation$sCurrentLocationName", "0")?.toDoubleOrNull() ?: 0.0
        } else {
            sSharedPreferences.getString("elevation$sCurrentLocationName", "0")?.toDoubleOrNull() ?: 0.0
        }
    }

    fun instantiateZmanimCalendar() {
        sROZmanimCalendar = ROZmanimCalendar(
            GeoLocation(sCurrentLocationName, sLatitude, sLongitude, sElevation, TimeZone.getTimeZone(sCurrentTimeZoneID))
        ).also { cal ->
            cal.setExternalFilesDir(getApplication<Application>().getExternalFilesDir(null))
            val candles = sSettingsPreferences.getString("CandleLightingOffset", "20")
                ?.takeIf { it.isNotEmpty() } ?: "20"
            cal.setCandleLightingOffset(candles.toDouble())
            val inIsrael = sSharedPreferences.getBoolean("inIsrael", false)
            val shabbatDefault = if (inIsrael) "30" else "40"
            val shabbat = sSettingsPreferences.getString("EndOfShabbatOffset", shabbatDefault)
                ?.takeIf { it.isNotEmpty() }
            if (shabbat == null) {
                cal.setAteretTorahSunsetOffset(if (inIsrael) 30.0 else 40.0)
            } else {
                cal.setAteretTorahSunsetOffset(shabbat.toDouble())
                if (inIsrael && shabbat == "40") cal.setAteretTorahSunsetOffset(30.0)
            }
            cal.setAmudehHoraah(sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false))
        }
    }

    // ---- Notifications ----

    fun setAllNotifications() {
        if (sROZmanimCalendar.geoLocation == GeoLocation()) return // skip default location

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        val calCopy = sROZmanimCalendar.getCopy().also { it.setCalendar(Calendar.getInstance()) }

        // Daily notifications at sunrise
        val sunrise = calCopy.sunrise ?: Date()
        calendar.timeInMillis = sunrise.time
        if (calendar.time.before(Date())) calendar.add(Calendar.DATE, 1)
        val dailyIntent = PendingIntent.getBroadcast(context, 0, Intent(context, DailyNotifications::class.java), PendingIntent.FLAG_IMMUTABLE)
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.timeInMillis, dailyIntent)

        if (BuildConfig.DEBUG) {
            sSharedPreferences.edit {putString(
                    "debugNotifs",
				sSharedPreferences.getString(
					"debugNotifs",
					""
				) + "Daily Notifications set for: ${calendar.time}\n\n"
			)
			}
		}

		// Omer notifications at tzeit
        val tzeit = calCopy.getTzeit() ?: Date()
        calendar.timeInMillis = tzeit.time
        if (calendar.time.before(Date())) calendar.add(Calendar.DATE, 1)
        val omerIntent = PendingIntent.getBroadcast(context, 0, Intent(context, OmerNotifications::class.java), PendingIntent.FLAG_IMMUTABLE)
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.timeInMillis, omerIntent)

        // Zmanim notifications (self-triggering broadcast)
        val zmanimIntent = PendingIntent.getBroadcast(context, 0, Intent(context, ZmanimNotifications::class.java), PendingIntent.FLAG_IMMUTABLE)
        try { zmanimIntent.send() } catch (e: PendingIntent.CanceledException) { e.printStackTrace() }
    }

    // ---- Chai tables ----

    fun updateChaitablesIfNeeded(locationName: String, onComplete: () -> Unit) {
        // Currently a no-op stub matching the commented-out Java code.
        // Uncomment and implement when chai tables logic is ready.
        onComplete()
    }

    fun downloadTablesIfNeeded(jewishYear: Int, chaitablesUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedUrl = updateYearInUrl(chaitablesUrl, jewishYear)
                val scraper = ChaiTablesWebJava(sROZmanimCalendar.geoLocation, sJewishDateInfo.jewishCalendar)
                val results = scraper.formatInterfacer(updatedUrl)
                var year = jewishYear
                for (result in results) {
                    ChaiTablesWebJava.saveResultsToFile(result, getApplication<Application>().getExternalFilesDir(null), sCurrentLocationName, year)
                    year++
                }
                rebuildZmanimList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ---- Helpers ----

    private fun addMoonTimes(zmanim: MutableList<ZmanListEntry>) {
        val moonTimes = MoonTimes.compute()
            .on(sCurrentDateShown.time)
            .at(sLatitude, sLongitude)
            .timezone(sCurrentTimeZoneID ?: TimeZone.getDefault().id)
            .midnight()
            .oneDay()
            .execute()

        when {
            moonTimes.isAlwaysUp -> zmanim.add(ZmanListEntry(context.getString(R.string.the_moon_is_up_all_night)))
            moonTimes.isAlwaysDown -> zmanim.add(ZmanListEntry(context.getString(R.string.there_is_no_moon_tonight)))
            else -> {
                val moonFmt = DateTimeFormatter.ofPattern(if (Utils.isLocaleHebrew(context)) "H:mm" else "h:mm a")
                val sb = StringBuilder()
                moonTimes.rise?.let { sb.append("${context.getString(R.string.moonrise)}${it.format(moonFmt)}") }
                moonTimes.set?.let {
                    if (sb.isNotEmpty()) sb.append(" - ")
                    sb.append("${context.getString(R.string.moonset)}${it.format(moonFmt)}")
                }
                if (sb.isNotEmpty()) zmanim.add(ZmanListEntry(sb.toString()))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar) =
        cal1[Calendar.ERA] == cal2[Calendar.ERA] &&
        cal1[Calendar.YEAR] == cal2[Calendar.YEAR] &&
        cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]

    private fun updateYearInUrl(url: String, jewishYear: Int): String {
        val matcher = Pattern.compile("&cgi_yrheb=\\d{4}").matcher(url)
        return if (matcher.find()) url.replace(matcher.group(), "&cgi_yrheb=$jewishYear") else url
    }

    private fun formatShaahZmanis(millis: Long): String {
        // Delegates to the same ZmanimFormatter logic used in the original Java code.
        // Using a simple HH:mm:ss format here; inject ZmanimFormatter if you need sexagesimal.
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        nextZmanJob?.cancel()
        midnightRolloverJob?.cancel()
    }
}
