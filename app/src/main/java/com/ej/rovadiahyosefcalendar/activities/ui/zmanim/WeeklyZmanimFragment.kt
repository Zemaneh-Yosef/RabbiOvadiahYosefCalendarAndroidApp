package com.ej.rovadiahyosefcalendar.activities.ui.zmanim

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ej.rovadiahyosefcalendar.R
import com.ej.rovadiahyosefcalendar.activities.GetUserLocationWithMapActivity
import com.ej.rovadiahyosefcalendar.activities.JerusalemDirectionMapsActivity
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.materialToolbar
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSetupLauncher
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences
import com.ej.rovadiahyosefcalendar.activities.MoladActivity
import com.ej.rovadiahyosefcalendar.activities.NetzActivity
import com.ej.rovadiahyosefcalendar.activities.SettingsActivity
import com.ej.rovadiahyosefcalendar.activities.WelcomeScreenActivity
import com.ej.rovadiahyosefcalendar.classes.LocationResolver
import com.ej.rovadiahyosefcalendar.classes.MakamJCal
import com.ej.rovadiahyosefcalendar.classes.Utils
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory
import com.ej.rovadiahyosefcalendar.databinding.FragmentWeeklyZmanimBinding
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateUtils
import org.json.JSONArray
import org.json.JSONException
import java.util.Calendar
import java.util.Date
import androidx.core.net.toUri

/**
 * Displays a 7-day weekly zmanim grid (Sun–Sat), short zmanim per column,
 * and daily announcements. Completely separate from [DailyZmanimFragment].
 *
 * NOTE: This fragment expects a layout named fragment_weekly_zmanim.xml.
 * If you are reusing the same XML as the original ZmanimFragment, swap the
 * binding type accordingly.
 */
class WeeklyZmanimFragment : Fragment() {

    // ---- Binding & ViewModel ----

    private var binding: FragmentWeeklyZmanimBinding? = null
    private val viewModel: ZmanimViewModel by activityViewModels()

    // ---- Context ----

    private lateinit var fragmentContext: Context
    private lateinit var locationResolver: LocationResolver

    // ---- Views: one entry per day (index 0 unused, 1–5 used) ----
    // Layout indices:
    //   [1] = announcements TextView
    //   [2] = hebrew day name
    //   [3] = hebrew date number
    //   [4] = english day name (short)
    //   [5] = english date number

    private val listViews = arrayOfNulls<ListView>(7)
    private val sunday    = arrayOfNulls<TextView>(6)
    private val monday    = arrayOfNulls<TextView>(6)
    private val tuesday   = arrayOfNulls<TextView>(6)
    private val wednesday = arrayOfNulls<TextView>(6)
    private val thursday  = arrayOfNulls<TextView>(6)
    private val friday    = arrayOfNulls<TextView>(6)
    private val saturday  = arrayOfNulls<TextView>(6)

    private var weeklyParsha: TextView? = null
    private var weeklyHaftorah: TextView? = null
    private var locationNameView: TextView? = null
    private var englishMonthYear: TextView? = null
    private var hebrewMonthYear: TextView? = null

    // Per-week announcement zmanim collected during getShortZmanim()
    private val zmanimForAnnouncements = mutableListOf<String>()

    private companion object {
        var makamNames: JSONArray? = null
    }

    // ---- Lifecycle ----

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
        locationResolver = LocationResolver(context, requireActivity())
        viewModel.init(locationResolver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWeeklyZmanimBinding.inflate(inflater, container, false)
        initMakamNames()
        findAllWeeklyViews()
        initMenu()
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeViewModel()

        if (sSharedPreferences.getBoolean("isSetup", false)) {
            viewModel.loadZmanim()
        }
    }

    override fun onResume() {
        super.onResume()
        initMenu()
        setupButtons()
        initMakamNames()
        updateWeeklyTextViewTextColor()
        updateWeeklyZmanim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // ---- Observers ----

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Rebuild the weekly grid whenever the zmanim data refreshes.
                launch {
                    viewModel.zmanimRefreshTick.collectLatest { tick ->
                        if (tick > 0) updateWeeklyZmanim()
                    }
                }

                launch {
                    viewModel.locationName.collectLatest { name ->
                        locationNameView?.text = name
                    }
                }
            }
        }
    }

    // ---- View setup ----

    private fun findAllWeeklyViews() {
        val b = binding ?: return
        locationNameView   = b.locationName
        englishMonthYear   = b.englishMonthYear
        hebrewMonthYear    = b.hebrewMonthYear

        listViews[0] = b.zmanim1
        sunday[1] = b.announcements;  sunday[2] = b.hebrewDay;  sunday[3] = b.hebrewDate;  sunday[4] = b.englishDay;  sunday[5] = b.englishDateNumber

        listViews[1] = b.zmanim2
        monday[1] = b.announcements2; monday[2] = b.hebrewDay2; monday[3] = b.hebrewDate2; monday[4] = b.englishDay2; monday[5] = b.englishDateNumber2

        listViews[2] = b.zmanim3
        tuesday[1] = b.announcements3; tuesday[2] = b.hebrewDay3; tuesday[3] = b.hebrewDate3; tuesday[4] = b.englishDay3; tuesday[5] = b.englishDateNumber3

        listViews[3] = b.zmanim4
        wednesday[1] = b.announcements4; wednesday[2] = b.hebrewDay4; wednesday[3] = b.hebrewDate4; wednesday[4] = b.englishDay4; wednesday[5] = b.englishDateNumber4

        listViews[4] = b.zmanim5
        thursday[1] = b.announcements5; thursday[2] = b.hebrewDay5; thursday[3] = b.hebrewDate5; thursday[4] = b.englishDay5; thursday[5] = b.englishDateNumber5

        listViews[5] = b.zmanim6
        friday[1] = b.announcements6; friday[2] = b.hebrewDay6; friday[3] = b.hebrewDate6; friday[4] = b.englishDay6; friday[5] = b.englishDateNumber6

        listViews[6] = b.zmanim7
        saturday[1] = b.announcements7; saturday[2] = b.hebrewDay7; saturday[3] = b.hebrewDate7; saturday[4] = b.englishDay7; saturday[5] = b.englishDateNumber7

        weeklyParsha   = b.weeklyParsha
        weeklyHaftorah = b.weeklyHaftorah

        updateWeeklyTextViewTextColor()

        // Adjust text size on small screens
        val screenHeight = resources.displayMetrics.heightPixels / resources.displayMetrics.density
        if (screenHeight < 750) {
            listOf(b.hebrewDay, b.hebrewDay2, b.hebrewDay3, b.hebrewDay4, b.hebrewDay5, b.hebrewDay6, b.hebrewDay7)
                .forEach { it.textSize = 12f }
            listOf(b.hebrewDate, b.hebrewDate2, b.hebrewDate3, b.hebrewDate4, b.hebrewDate5, b.hebrewDate6, b.hebrewDate7)
                .forEach { it.textSize = 16f }
        }
    }

    private fun updateWeeklyTextViewTextColor() {
        if (!sSharedPreferences.getBoolean("customTextColor", false)) return
        val color = sSharedPreferences.getInt("tColor", 0xFFFFFFFF.toInt())
        locationNameView?.setTextColor(color)
        englishMonthYear?.setTextColor(color)
        hebrewMonthYear?.setTextColor(color)
        weeklyParsha?.setTextColor(color)
        weeklyHaftorah?.setTextColor(color)
        for (i in 1..5) {
            sunday[i]?.setTextColor(color)
            monday[i]?.setTextColor(color)
            tuesday[i]?.setTextColor(color)
            wednesday[i]?.setTextColor(color)
            thursday[i]?.setTextColor(color)
            friday[i]?.setTextColor(color)
            saturday[i]?.setTextColor(color)
        }
    }

    // ---- Weekly grid population ----

    private fun updateWeeklyZmanim() {
        val backupCal = sCurrentDateShown.clone() as Calendar

        // Rewind to Sunday
        while (sCurrentDateShown.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            sCurrentDateShown.add(Calendar.DATE, -1)
        }
        sROZmanimCalendar.setCalendar(sCurrentDateShown)
        sJewishDateInfo.setCalendar(sCurrentDateShown)

        val hdf = HebrewDateFormatter().also {
            if (Utils.isLocaleHebrew(fragmentContext)) it.isHebrewFormat = true
        }
        val locale = fragmentContext.resources.configuration.locales[0]

        var startMonth = sCurrentDateShown.getDisplayName(Calendar.MONTH, Calendar.LONG, locale) ?: ""
        var startYear  = sCurrentDateShown.get(Calendar.YEAR).toString()
        var startHebrewMonth = hdf.formatMonth(sJewishDateInfo.jewishCalendar)
            .replace("Tishrei", "Tishri").replace("Teves", "Tevet")
        var startHebrewYear = if (Utils.isLocaleHebrew(fragmentContext))
            hdf.formatHebrewNumber(sJewishDateInfo.jewishCalendar.jewishYear)
        else sJewishDateInfo.jewishCalendar.jewishYear.toString()

        val weeklyInfo = listOf(sunday, monday, tuesday, wednesday, thursday, friday, saturday)

        for (i in 0 until 7) {
            // Highlight today
            if (DateUtils.isSameDay(sROZmanimCalendar.calendar.time, Date())) {
                weeklyInfo[i][4]?.setBackgroundColor(fragmentContext.getColor(R.color.dark_gold))
            } else {
                weeklyInfo[i][4]?.background = null
            }

            // Short zmanim + announcements
            zmanimForAnnouncements.clear()
            val shortZmanim = getShortZmanim() // fills zmanimForAnnouncements as a side effect

            listViews[i]?.adapter = object : ArrayAdapter<String>(fragmentContext, R.layout.zman_list_view, shortZmanim) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val tv = view.findViewById<TextView>(R.id.zman_in_list)
                    if (sSharedPreferences.getBoolean("customTextColor", false)) {
                        tv?.setTextColor(sSharedPreferences.getInt("tColor", 0xFFFFFFFF.toInt()))
                    }
                    return view
                }
            }

            val announcements = buildString {
                zmanimForAnnouncements.forEach { append(it).append("\n") }
                append(getAnnouncements())
            }.trimEnd('\n')

            weeklyInfo[i][1]?.text = announcements
            weeklyInfo[i][1]?.visibility = if (announcements.isEmpty()) View.INVISIBLE else View.VISIBLE
            weeklyInfo[i][2]?.text = sJewishDateInfo.jewishDayOfWeek
            weeklyInfo[i][3]?.text = sHebrewDateFormatter.formatHebrewNumber(sJewishDateInfo.jewishCalendar.jewishDayOfMonth)
            weeklyInfo[i][4]?.text = sROZmanimCalendar.calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale)
            weeklyInfo[i][5]?.text = sROZmanimCalendar.calendar.get(Calendar.DAY_OF_MONTH).toString()

            if (i != 6) {
                sROZmanimCalendar.calendar.add(Calendar.DATE, 1)
                sJewishDateInfo.setCalendar(sROZmanimCalendar.calendar)
            }
        }

        // Month/year headers — show range if week spans two months or years
        val endMonth = sROZmanimCalendar.calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale) ?: ""
        val endYear  = sROZmanimCalendar.calendar.get(Calendar.YEAR).toString()
        val endHebrewMonth = hdf.formatMonth(sJewishDateInfo.jewishCalendar).replace("Tishrei", "Tishri").replace("Teves", "Tevet")
        val endHebrewYear = if (Utils.isLocaleHebrew(fragmentContext))
            hdf.formatHebrewNumber(sJewishDateInfo.jewishCalendar.jewishYear)
        else sJewishDateInfo.jewishCalendar.jewishYear.toString()

        if (startMonth != endMonth) startMonth += " - $endMonth"
        if (startYear  != endYear)  startYear  += " / $endYear"
        if (startHebrewMonth != endHebrewMonth) startHebrewMonth += " - $endHebrewMonth"
        if (startHebrewYear  != endHebrewYear)  startHebrewYear  += " / $endHebrewYear"

        englishMonthYear?.text = "$startMonth $startYear"
        hebrewMonthYear?.text  = "$startHebrewMonth $startHebrewYear"

        if (Utils.isLocaleHebrew(fragmentContext)) {
            englishMonthYear?.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            hebrewMonthYear?.textAlignment  = View.TEXT_ALIGNMENT_TEXT_START
        }

        locationResolver.getFullLocationName(true) { name -> locationNameView?.text = name }

        weeklyParsha?.text = sJewishDateInfo.thisWeeksParsha
        weeklyParsha?.visibility = View.VISIBLE
        val haftara = sJewishDateInfo.thisWeeksHaftarah
        if (haftara.isEmpty()) {
            weeklyHaftorah?.visibility = View.GONE
        } else {
            weeklyHaftorah?.visibility = View.VISIBLE
            weeklyHaftorah?.text = haftara
        }

        // Restore calendar state
        sROZmanimCalendar.calendar.timeInMillis = backupCal.timeInMillis
        sJewishDateInfo.setCalendar(backupCal)
        sCurrentDateShown = backupCal
    }

    // ---- Short zmanim for weekly columns ----

    /**
     * Builds the compact list of zmanim strings for one column of the weekly grid.
     * Notable zmanim (candles, Rabbenu Tam, etc.) are extracted into [zmanimForAnnouncements]
     * and removed from the column so they don't appear twice.
     */
    private fun getShortZmanim(): Array<String> {
        val allZmanim = mutableListOf<ZmanListEntry>()
        ZmanimFactory.addZmanim(allZmanim, true, sSettingsPreferences, sSharedPreferences, sROZmanimCalendar, sJewishDateInfo, true)

        val toRemove = mutableListOf<ZmanListEntry>()
        val isHebrew = sSharedPreferences.getBoolean("isZmanimInHebrew", false)
        val localeHebrew = Utils.isLocaleHebrew(fragmentContext)

        for (zman in allZmanim) {
            if (!zman.isNoteworthyZman) continue
            val cleanTitle = zman.title.replace(Regex("\\(.*\\)"), "").trim()
            val time = Utils.formatZmanTime(fragmentContext, zman)
            val formatted = if (isHebrew && !localeHebrew) "$time :$cleanTitle" else "$cleanTitle: $time"
            zmanimForAnnouncements.add(formatted)
            toRemove.add(zman)
        }
        allZmanim.removeAll(toRemove)

        val nextZman = viewModel.nextUpcomingZman
        return Array(allZmanim.size) { index ->
            val zman = allZmanim[index]
            val cleanTitle = zman.title
                .replace("סוף זמן ", "").replace("Earliest ", "").replace("Sof Zeman ", "").replace("Latest ", "")
                .replace("(", "").replace(")", "")
            val time = Utils.formatZmanTime(fragmentContext, zman)
            val indicator = if (zman.zman == nextZman) (if (localeHebrew) " ➤ " else " ◄ ") else ""
            if (isHebrew && !localeHebrew) "$time :$cleanTitle$indicator" else "$cleanTitle: $time$indicator"
        }
    }

    // ---- Announcements ----

    /**
     * Builds the announcement text shown beneath the zmanim for each day column.
     * Mirrors the logic of the original [ZmanimFragment.getAnnouncements()].
     */
    private fun getAnnouncements(): String = buildString {
        val day = sJewishDateInfo.getSpecialDay(true)
        if (day.isNotEmpty()) append(day.replace("/ ", "\n")).append("\n")

        if (sJewishDateInfo.isPurimMeshulash) append(fragmentContext.getString(R.string.no_tachanun_in_yerushalayim))

        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", true) && sJewishDateInfo.jewishCalendar.isShabbosMevorchim) {
            append("שבת מברכים\n")
        }

        val music = sJewishDateInfo.isOKToListenToMusic()
        if (music.isNotEmpty()) append(music).append("\n")

        val ulChaparat = sJewishDateInfo.isUlChaparatPeshaSaid
        if (ulChaparat.isNotEmpty()) append(ulChaparat).append("\n")

        val hallel = sJewishDateInfo.hallelOrChatziHallel
        if (hallel.isNotEmpty()) append(hallel).append("\n")

        val rules = TefilaRules()
        if (rules.isMashivHaruachEndDate(sJewishDateInfo.jewishCalendar)) append("מוריד הטל/ברכנו\n")
        if (rules.isMashivHaruachStartDate(sJewishDateInfo.jewishCalendar)) append("משיב הרוח\n")
        if (rules.isVeseinTalUmatarStartDate(sJewishDateInfo.jewishCalendar)) append("ברך עלינו\n")

        val tachanun = sJewishDateInfo.isTachanunSaid
        if (tachanun != fragmentContext.getString(R.string.there_is_tachanun_today)) append(tachanun).append("\n")

        val birchatLevana = sJewishDateInfo.birchatLevana
        if (birchatLevana.isNotEmpty() && !birchatLevana.contains("until") && !birchatLevana.contains("עד")) {
            append(birchatLevana).append("\n")
        }

        if (sJewishDateInfo.jewishCalendar.isBirkasHachamah) append(fragmentContext.getString(R.string.birchat_hachamah_is_said_today)).append("\n")

        if (sJewishDateInfo.tomorrow().jewishCalendar.dayOfWeek == Calendar.SATURDAY &&
            sJewishDateInfo.tomorrow().jewishCalendar.yomTovIndex == JewishCalendar.EREV_PESACH) {
            append(fragmentContext.getString(R.string.burn_your_ametz_today))
        }

        // Tekufa
        val tekufaList = mutableListOf<ZmanListEntry>()
        val opinion = sSettingsPreferences.getString("TekufaOpinions", "1") ?: "1"
        viewModel.addTekufaEntries(tekufaList, shortStyle = true, opinion = opinion)
        tekufaList.forEach { append(it.title).append("\n") }

        // Makam
        try {
            val makamData = MakamJCal.getMakamData(sJewishDateInfo.jewishCalendar)
            val names = makamNames
            if (names != null) {
                val entry = makamData["GABRIEL A SHREM 1964 SUHV"] ?: makamData["ADES: 24793"]
                if (entry != null) {
                    val sb = StringBuilder(fragmentContext.getString(R.string.makam))
                    entry.forEachIndexed { _, makam -> sb.append(names.get(makam.ordinal)).append(" ") }
                    append(sb.toString().trim()).append("\n")
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }.trimEnd('\n')

    // ---- Buttons (date navigation) ----

    private fun setupButtons() {
        val b = binding ?: return

        b.prevDay.setOnClickListener {
            sCurrentDateShown = (sROZmanimCalendar.calendar.clone() as Calendar)
            sCurrentDateShown.add(Calendar.DATE, -7)
            sROZmanimCalendar.setCalendar(sCurrentDateShown)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
            updateWeeklyZmanim()
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
        }

        b.nextDay.setOnClickListener {
            sCurrentDateShown = (sROZmanimCalendar.calendar.clone() as Calendar)
            sCurrentDateShown.add(Calendar.DATE, 7)
            sROZmanimCalendar.setCalendar(sCurrentDateShown)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
            updateWeeklyZmanim()
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
        }

        b.calendar.setOnClickListener {
            // Reuse the same material date picker pattern from DailyZmanimFragment
            val picker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setPositiveButtonText(R.string.ok)
                .setNegativeButtonText(R.string.switch_calendar)
                .setSelection(sCurrentDateShown.timeInMillis)
                .build()
            picker.addOnPositiveButtonClickListener { selection ->
                val epoch = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).also { it.timeInMillis = selection }
                sCurrentDateShown.set(epoch.get(Calendar.YEAR), epoch.get(Calendar.MONTH), epoch.get(Calendar.DATE))
                sROZmanimCalendar.setCalendar(sCurrentDateShown)
                sJewishDateInfo.setCalendar(sCurrentDateShown)
                updateWeeklyZmanim()
                b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
            }
            picker.addOnNegativeButtonClickListener {
                val hdmypd = com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog(picker, requireActivity().supportFragmentManager, sJewishDateInfo.jewishCalendar)
                hdmypd.updateDate(sJewishDateInfo.jewishCalendar.gregorianYear, sJewishDateInfo.jewishCalendar.gregorianMonth, sJewishDateInfo.jewishCalendar.gregorianDayOfMonth)
                hdmypd.setListener { _, year, month, day ->
                    val cal = Calendar.getInstance().also { it.set(year, month, day) }
                    sROZmanimCalendar.setCalendar(cal)
                    sJewishDateInfo.setCalendar(cal)
                    sCurrentDateShown = cal.clone() as Calendar
                    updateWeeklyZmanim()
                    b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
                }
                hdmypd.show(requireActivity().supportFragmentManager, null)
            }
            picker.show(requireActivity().supportFragmentManager, null)
        }

        b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
    }

    // ---- Menu ----

    private fun initMenu() {
        val toolbar = materialToolbar ?: return
        if (Utils.isLocaleHebrew(fragmentContext)) toolbar.subtitle = "" else toolbar.title = fragmentContext.getString(R.string.app_name)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.search_for_a_place -> { startActivity(Intent(fragmentContext, GetUserLocationWithMapActivity::class.java).putExtra("loneActivity", true)); true }
                R.id.weekly_mode -> {
                    MainFragmentManagerActivity.switchZmanimFragment(false)
                    true
                }
                R.id.jerDirection -> { startActivity(Intent(fragmentContext, JerusalemDirectionMapsActivity::class.java)); true }
                R.id.netzView -> { startActivity(Intent(fragmentContext, NetzActivity::class.java)); true }
                R.id.molad -> { startActivity(Intent(fragmentContext, MoladActivity::class.java)); true }
                R.id.fullSetup -> { sSetupLauncher.launch(Intent(fragmentContext, WelcomeScreenActivity::class.java)); true }
                R.id.settings -> { startActivity(Intent(fragmentContext, SettingsActivity::class.java)); true }
                R.id.website -> { startActivity(Intent(Intent.ACTION_VIEW,
					"https://www.royzmanim.com".toUri())); true }
                else -> false
            }
        }
        MenuCompat.setGroupDividerEnabled(toolbar.menu, true)
        toolbar.menu.findItem(R.id.weekly_mode)?.isChecked = true // always checked — we are the weekly fragment
        // Weekly mode and shabbat mode items are not shown in the weekly fragment's menu
    }

    // ---- Makam ----

    private fun initMakamNames() {
        try {
            makamNames = JSONArray(Utils.inputStreamToString(
                requireActivity().resources.openRawResource(
                    if (Utils.isLocaleHebrew(fragmentContext)) R.raw.makam_names_he else R.raw.makam_names
                )
            ))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}
