package com.ej.rovadiahyosefcalendar.activities.ui.zmanim

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ej.rovadiahyosefcalendar.R
import com.ej.rovadiahyosefcalendar.activities.GetUserLocationWithMapActivity
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.materialToolbar
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sNavView
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSetupLauncher
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sViewPager
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLastTimeUserWasInApp
import com.ej.rovadiahyosefcalendar.activities.JerusalemDirectionMapsActivity
import com.ej.rovadiahyosefcalendar.activities.MoladActivity
import com.ej.rovadiahyosefcalendar.activities.NetzActivity
import com.ej.rovadiahyosefcalendar.activities.SettingsActivity
import com.ej.rovadiahyosefcalendar.activities.SetupElevationActivity
import com.ej.rovadiahyosefcalendar.activities.WelcomeScreenActivity
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava
import com.ej.rovadiahyosefcalendar.classes.DummyZmanAdapter
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog
import com.ej.rovadiahyosefcalendar.classes.LocationResolver
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment
import com.ej.rovadiahyosefcalendar.classes.Utils
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry
import com.ej.rovadiahyosefcalendar.classes.OnClickListeners
import com.ej.rovadiahyosefcalendar.classes.Utils.getCurrentCalendarDrawableDark
import com.ej.rovadiahyosefcalendar.classes.Utils.getCurrentCalendarDrawableLight
import com.ej.rovadiahyosefcalendar.databinding.FragmentZmanimBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kosherjava.zmanim.AstronomicalCalendar.getTimeOffset
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateUtils
import org.json.JSONArray
import org.json.JSONException
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.function.Consumer
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuCompat
import androidx.core.widget.TextViewCompat
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar.MILLISECONDS_PER_MINUTE
import com.ej.rovadiahyosefcalendar.classes.Utils.inputStreamToString
import androidx.core.view.isVisible
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri

class DailyZmanimFragment : Fragment() {

    // ---- Binding & ViewModel ----

    private var binding: FragmentZmanimBinding? = null
    private val viewModel: ZmanimViewModel by activityViewModels()

    // ---- Context helpers ----

    private lateinit var fragmentContext: Context
    private lateinit var locationResolver: LocationResolver

    // ---- Scroll state ----

    private var savedScrollPosition = 0

    // ---- Shabbat scrolling ----

    private var scrollingThread: Thread? = null

    // ---- Notification launcher ----

    companion object {
        @JvmField
		var sShabbatMode: Boolean = false   // ← add this
        var notificationLauncher: ActivityResultLauncher<Intent>? = null
        private var makamNames: JSONArray? = null

        @JvmStatic
        fun createLocationCallback(fragment: DailyZmanimFragment): Consumer<Location?> {
            return Consumer { location -> fragment.accept(location) }
        }
    }

    // ---- Shared preference listener ----

    private lateinit var sharedPrefListener: SharedPreferences.OnSharedPreferenceChangeListener

    // ---- Lifecycle ----

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
        locationResolver = LocationResolver(context, requireActivity())
        viewModel.init(locationResolver)
        notificationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.setAllNotifications()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentZmanimBinding.inflate(inflater, container, false)

        initMakamNames()
        setupShabbatModeBanner()
        setupDailyViews()
        initMenu()

        setSafeSettingsForXiaomi()

        sharedPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "isSetup") initMainView()
        }
        sSharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefListener)

        if (sSharedPreferences.getBoolean("isSetup", false)) {
            initMainView()
        }

        handleShareIntent()

        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        initMenu()
        setupButtons()
        if (sSettingsPreferences.getBoolean("mainAlwaysOn", false) || sShabbatMode) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        resetSystemTheme()
        setCustomThemeColors()

        binding?.clock?.let { clock ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                clock.visibility = View.VISIBLE
                if (Utils.isLocaleHebrew(fragmentContext)) clock.format24Hour = "H:mm:ss"
            } else if (sShabbatMode) {
                clock.visibility = View.VISIBLE
            }
        }

        val rv = binding?.mainRV ?: return
        sJewishDateInfo.jewishCalendar.inIsrael = sSharedPreferences.getBoolean("inIsrael", false)
        sJewishDateInfo.resetLocale(fragmentContext)
        initMakamNames()

        if (sLastTimeUserWasInApp != null) {
            viewModel.loadZmanim()
        }

        val now = Date()
        if (sLastTimeUserWasInApp == null) sLastTimeUserWasInApp = now
        if (!DateUtils.isSameDay(sCurrentDateShown.time, now) &&
            (now.time - sLastTimeUserWasInApp!!.time) > 7_200_000
        ) {
            sCurrentDateShown.time = now
            sROZmanimCalendar.setCalendar(sCurrentDateShown)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
            viewModel.onDateChanged()
        }
        sLastTimeUserWasInApp = now
        Utils.PrefToWatchSender.send(fragmentContext)
    }

    override fun onPause() {
        savedScrollPosition = binding?.nestedScrollView?.scrollY ?: 0
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPrefListener)
        binding = null
    }

    // ---- Observers ----

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Zmanim list
                launch {
                    viewModel.dailyZmanimState.collectLatest { state ->
                        when (state) {
                            is ZmanimUiState.Loading -> {
                                binding?.shimmerLayout?.visibility = View.VISIBLE
                                binding?.swipeRefreshLayout?.visibility = View.GONE
                            }
                            is ZmanimUiState.Success -> {
                                binding?.shimmerLayout?.visibility = View.GONE
                                binding?.swipeRefreshLayout?.visibility = View.VISIBLE
                                updateHeaderUi()
                                applyZmanimToRecyclerView(state.zmanim)
                            }
                            is ZmanimUiState.Error -> {
                                binding?.shimmerLayout?.visibility = View.GONE
                            }
                        }
                    }
                }

                // Location name
                launch {
                    viewModel.locationName.collectLatest { name ->
                        binding?.dailyLocationName?.text = name
                    }
                }

                // Shabbat mode
                launch {
                    viewModel.isShabbatMode.collectLatest { isShabbat ->
                        sShabbatMode = isShabbat
                        onShabbatModeChanged(isShabbat)
                    }
                }

                // Scroll position restore after refresh
                launch {
                    viewModel.zmanimRefreshTick.collectLatest {
                        if (it > 0) {
                            binding?.nestedScrollView?.scrollTo(0, savedScrollPosition)
                        }
                    }
                }
            }
        }
    }

    // ---- Init ----

    private fun initMainView() {
        if (sLatitude == 0.0 && sLongitude == 0.0) {
            locationResolver.acquireLatitudeAndLongitude { location -> accept(location) }
        }
        binding?.shimmerLayout?.visibility = View.VISIBLE
        binding?.swipeRefreshLayout?.visibility = View.GONE
        viewModel.loadZmanim()
        setupButtons()
        viewModel.setAllNotifications()
        askForRealTimeNotificationPermissions()
        checkIfUserIsInIsraelOrNot()
    }

    private fun initMakamNames() {
        try {
            makamNames = JSONArray(inputStreamToString(
                requireActivity().resources.openRawResource(
                    if (Utils.isLocaleHebrew(fragmentContext)) R.raw.makam_names_he else R.raw.makam_names
                )
            ))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    // ---- Header UI (date card, parsha, chanuka, makam) ----

    /**
     * Updates all non-list UI in the daily view: date card, weekday label, parsha,
     * haftara, makam, and Chanuka card. Called whenever the zmanim list is refreshed.
     * Separated from list building so each responsibility is independently testable.
     */
    private fun updateHeaderUi() {
        val b = binding ?: return

        // Date strings
        val locale = fragmentContext.resources.configuration.locales[0]
        val engDate = "${sROZmanimCalendar.calendar.get(Calendar.DATE)} " +
            "${sROZmanimCalendar.calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)}, " +
            "${sROZmanimCalendar.calendar.get(Calendar.YEAR)}"

        if (DateUtils.isSameDay(sROZmanimCalendar.calendar.time, Date())) {
			b.dailyCard.strokeColor = ContextCompat.getColor(fragmentContext, R.color.sunset_orange)
			b.dailyCard.strokeWidth = 6

            val todayLabel = "${fragmentContext.getString(R.string.today)} — $engDate"
            val hebDate = if (sROZmanimCalendar.sunset != null && Date().after(sROZmanimCalendar.sunset)) {
                fragmentContext.getString(R.string.post_sunset_date)
            } else {
                fragmentContext.getString(R.string.pre_sunset_date)
            } + " " + sJewishDateInfo.jewishCalendar.currentToString(sROZmanimCalendar)
            if (Utils.isLocaleHebrew(fragmentContext)) {
                b.topDate.text = hebDate
                b.bottomDate.text = todayLabel
            } else {
                b.topDate.text = todayLabel
                b.bottomDate.text = hebDate
            }
        } else {
			b.dailyCard.strokeColor = ContextCompat.getColor(fragmentContext, R.color.cardview_border)
			b.dailyCard.strokeWidth = 3
            val jewishStr = sJewishDateInfo.jewishCalendar.toString()
            if (Utils.isLocaleHebrew(fragmentContext)) {
                b.topDate.text = jewishStr
                b.bottomDate.text = engDate
            } else {
                b.topDate.text = engDate
                b.bottomDate.text = jewishStr
            }
        }

        // Weekday label
        val heLocale = Locale.Builder().setLanguage("he").setRegion("IL").build()
        val dayName = sROZmanimCalendar.calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale)
        b.weekday.text = if (Utils.isLocaleHebrew(fragmentContext)) dayName
        else "$dayName / ${sROZmanimCalendar.calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, heLocale)}"

        // Parsha / haftara / makam
        b.parsha.text = sJewishDateInfo.thisWeeksParsha
        val haftara = sJewishDateInfo.thisWeeksHaftarah
        if (haftara.isEmpty()) {
            b.haftaraLayout.visibility = View.GONE
            b.makamLayout.visibility = View.GONE
        } else {
            b.haftaraLayout.visibility = View.VISIBLE
            b.haftara.text = haftara
            updateMakamText()
        }

        // Chanuka card
        updateChanukaCard()
    }

    private fun updateMakamText() {
        val b = binding ?: return
        try {
            val makamData = sJewishDateInfo.thisWeeksMakam
            val names = makamNames ?: return
            val entry = makamData["GABRIEL A SHREM 1964 SUHV"] ?: makamData["ADES: 24793"]
            if (entry != null) {
                val sb = StringBuilder(fragmentContext.getString(R.string.makam))
                for (i in entry.indices) sb.append(names.get(entry[i].ordinal)).append(" ")
                b.makam.text = sb.toString().trim()
                b.makamLayout.visibility = View.VISIBLE
            } else {
                b.makamLayout.visibility = View.GONE
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            b.makamLayout.visibility = View.GONE
        }
    }

    private fun updateChanukaCard() {
        val b = binding ?: return
        val isChanuka = sJewishDateInfo.jewishCalendar.isChanukah || sJewishDateInfo.tomorrow().jewishCalendar.isChanukah
        val notLastNight = sJewishDateInfo.jewishCalendar.dayOfChanukah != 8

        if (isChanuka && notLastNight) {
            var night = sJewishDateInfo.jewishCalendar.dayOfChanukah + 1
            if (night == 0) night = 1 // erev Chanuka edge case

            b.nightOfChanuka.text = fragmentContext.getString(R.string.chanukah_night, Utils.toOrdinal(night, Utils.isLocaleHebrew(fragmentContext)))

            b.lchatchilaLightingTime.paintFlags = b.nightOfChanuka.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            b.lchatchilaLightingTime.text = buildString {
                append(fragmentContext.getString(R.string.ideal))
                append(" ")
                append(Utils.formatZmanTime(fragmentContext, sROZmanimCalendar.getTzeit(), SecondTreatment.ROUND_LATER))
                append(" - ")
                append(Utils.formatZmanTime(fragmentContext, getTimeOffset(sROZmanimCalendar.getTzeit(), 30.0 * MILLISECONDS_PER_MINUTE), SecondTreatment.ROUND_EARLIER))
            }

            val tomorrow = sROZmanimCalendar.getCopy().also { it.calendar.add(Calendar.DATE, 1) }
            b.fullWindowLightingTime.text = buildString {
                append(fragmentContext.getString(R.string.earliest))
                append(" ")
                append(Utils.formatZmanTime(fragmentContext, sROZmanimCalendar.plagHaminchaYalkutYosef, SecondTreatment.ROUND_LATER))
                append(" - ")
                append(fragmentContext.getString(R.string.latest))
                append(" ")
                append(Utils.formatZmanTime(fragmentContext, tomorrow.alotHashachar, SecondTreatment.ROUND_EARLIER))
            }

            b.chanukaCard.visibility = View.VISIBLE
            b.chanukaCard.cardElevation = 12f
        } else {
            b.chanukaCard.visibility = View.GONE
        }
    }

    // ---- RecyclerView ----

    private fun applyZmanimToRecyclerView(zmanim: List<ZmanListEntry>) {
        val rv = binding?.mainRV ?: return
        rv.adapter = ZmanAdapter(fragmentContext, zmanim,
            viewModel,
            object : OnClickListeners.OnZmanClickListener {
                override fun onItemClick() {
                    // Called by adapter when user triggers the secondary (66-minute misheyakir) zman
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.rebuildZmanimList(add66MisheyakirZman = true)
                    }
                }
            }
        )
        binding?.nestedScrollView?.scrollTo(0, 0)
    }

    // ---- Daily view setup ----

    private fun setupDailyViews() {
        val b = binding ?: return

        b.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh { b.swipeRefreshLayout.isRefreshing = false }
        }

        b.nestedScrollView // referenced via binding; no extra setup needed

        b.dailyLocationName.paintFlags = b.dailyLocationName.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        b.dailyLocationName.setOnClickListener { showLocationInfoDialog() }

        b.dailyCard.cardElevation = 12f
        b.parshaCard.cardElevation = 12f

        b.parsha.setOnClickListener { showParshaDialog() }

        b.mainRV.layoutManager = LinearLayoutManager(fragmentContext)
        b.mainRV.addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            b.mainRV.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        b.dummyRV.layoutManager = LinearLayoutManager(fragmentContext)
        b.dummyRV.addItemDecoration(DividerItemDecoration(fragmentContext, DividerItemDecoration.VERTICAL))
        b.dummyRV.adapter = DummyZmanAdapter(30)
    }

    // ---- Buttons ----

    private fun setupButtons() {
        setupDayHopButton(previous = true)
        setupCalendarButton()
        setupDayHopButton(previous = false)
    }

    private fun setupDayHopButton(previous: Boolean) {
        val b = binding ?: return
        val button = if (previous) b.prevDay else b.nextDay
        button.setOnClickListener {
            if (sShabbatMode) return@setOnClickListener
            sCurrentDateShown = (sROZmanimCalendar.calendar.clone() as Calendar)
            val delta = (if (sSharedPreferences.getBoolean("weeklyMode", false)) 7 else 1) * (if (previous) -1 else 1)
            sCurrentDateShown.add(Calendar.DATE, delta)
            sROZmanimCalendar.setCalendar(sCurrentDateShown)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
            viewModel.onDateChanged()
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
            seeIfTablesNeedToBeUpdated(fromButton = true)
        }
    }

    private fun setupCalendarButton() {
        val b = binding ?: return
        b.calendar.setOnClickListener {
            if (sShabbatMode) return@setOnClickListener
            showMaterialDatePicker()
        }
        b.dailyCard.setOnClickListener { b.calendar.performClick() }
        b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
    }

    private fun showMaterialDatePicker() {
        val b = binding ?: return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setPositiveButtonText(R.string.ok)
            .setNegativeButtonText(R.string.switch_calendar)
            .setSelection(sCurrentDateShown.timeInMillis)
            .build()

        val onDateSet = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            sCurrentDateShown.set(year, month, day)
            sROZmanimCalendar.setCalendar(sCurrentDateShown)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
            viewModel.onDateChanged()
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
            seeIfTablesNeedToBeUpdated(fromButton = true)
        }

        picker.addOnPositiveButtonClickListener { selection ->
            val epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC")).also { it.timeInMillis = selection }
            sCurrentDateShown.set(epoch.get(Calendar.YEAR), epoch.get(Calendar.MONTH), epoch.get(Calendar.DATE),
                epoch.get(Calendar.HOUR_OF_DAY), epoch.get(Calendar.MINUTE))
            sROZmanimCalendar.setCalendar(sCurrentDateShown)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
            viewModel.onDateChanged()
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
            seeIfTablesNeedToBeUpdated(fromButton = true)
        }

        picker.addOnNegativeButtonClickListener {
            val hdmypd = HebrewDayMonthYearPickerDialog(picker, requireActivity().supportFragmentManager, sJewishDateInfo.jewishCalendar)
            hdmypd.updateDate(sJewishDateInfo.jewishCalendar.gregorianYear, sJewishDateInfo.jewishCalendar.gregorianMonth, sJewishDateInfo.jewishCalendar.gregorianDayOfMonth)
            hdmypd.setListener(onDateSet)
            hdmypd.show(requireActivity().supportFragmentManager, null)
        }

        picker.show(requireActivity().supportFragmentManager, null)
    }

    // ---- Shabbat mode ----

    private fun onShabbatModeChanged(isShabbat: Boolean) {
        val b = binding ?: return
        val activity = activity ?: return

        if (isShabbat) {
            b.shabbatMode.visibility = View.VISIBLE
            setShabbatBannerColors(isFirstTime = true)
            sNavView?.visibility = View.GONE
            sViewPager?.isUserInputEnabled = false
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) activity.window.setHideOverlayWindows(true)
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                b.clock.visibility = View.VISIBLE
                if (Utils.isLocaleHebrew(fragmentContext)) b.clock.format24Hour = "hh:mm:ss"
            }
            b.nextDay.visibility = View.GONE
            b.prevDay.visibility = View.GONE
            startScrollingThread()
        } else {
            b.shabbatMode.visibility = View.GONE
            if (sNavView != null && sViewPager != null && !sSettingsPreferences.getBoolean("hideBottomBar", false)) {
                sNavView?.visibility = View.VISIBLE
                sViewPager?.isUserInputEnabled = true
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) activity.window.setHideOverlayWindows(false)
            if (sSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
                b.calendar.setBackgroundColor(fragmentContext.getColor(R.color.dark_blue))
            } else {
                b.calendar.setBackgroundColor(sSharedPreferences.getInt("CalButtonColor", 0x18267C))
            }
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sROZmanimCalendar.calendar))
            b.nextDay.visibility = View.VISIBLE
            b.prevDay.visibility = View.VISIBLE
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                b.clock.visibility = View.GONE
            }
            scrollingThread?.interrupt()
            scrollingThread = null
        }
    }

    private fun setShabbatBannerColors(isFirstTime: Boolean) {
        val b = binding ?: return
        if (isFirstTime) {
            sCurrentDateShown.add(Calendar.DATE, 1)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
        }

        val isShabbat = sCurrentDateShown.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY

        fun banner(textRes: Int, bg: Int, fg: Int, lightDrawable: Boolean) {
            val sb = StringBuilder()
            repeat(4) { sb.append(fragmentContext.getString(textRes)).append(if (isShabbat) fragmentContext.getString(R.string.slash_SHABBAT) else "").append(" ").append(fragmentContext.getString(R.string.MODE)).append("                ") }
            b.shabbatMode.text = sb.toString()
            b.shabbatMode.setBackgroundColor(fragmentContext.getColor(bg))
            b.shabbatMode.setTextColor(fragmentContext.getColor(fg))
            b.calendar.setBackgroundColor(fragmentContext.getColor(bg))
            b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
                if (lightDrawable) getCurrentCalendarDrawableLight(sCurrentDateShown) else getCurrentCalendarDrawableDark(sCurrentDateShown))
        }

        when (sJewishDateInfo.jewishCalendar.yomTovIndex) {
            JewishCalendar.PESACH -> banner(R.string.PESACH, R.color.lightYellow, R.color.black, lightDrawable = false)
            JewishCalendar.SHAVUOS -> banner(R.string.SHAVUOT, R.color.light_blue, R.color.white, lightDrawable = true)
            JewishCalendar.SUCCOS -> banner(R.string.SUCCOT, R.color.light_green, R.color.black, lightDrawable = false)
            JewishCalendar.SHEMINI_ATZERES -> banner(R.string.SHEMINI_ATZERET, R.color.light_green, R.color.black, lightDrawable = false)
            JewishCalendar.SIMCHAS_TORAH -> banner(R.string.SIMCHAT_TORAH, R.color.green, R.color.black, lightDrawable = false)
            JewishCalendar.ROSH_HASHANA -> banner(R.string.ROSH_HASHANA, R.color.dark_red, R.color.white, lightDrawable = true)
            JewishCalendar.YOM_KIPPUR -> banner(R.string.YOM_KIPPUR, R.color.white, R.color.black, lightDrawable = false)
            else -> {
                val def = listOf(R.string.SHABBAT_MODE, R.string.SHABBAT_MODE, R.string.SHABBAT_MODE, R.string.SHABBAT_MODE, R.string.SHABBAT_MODE)
                    .joinToString("               ") { fragmentContext.getString(it) }
                b.shabbatMode.text = def
                b.shabbatMode.setBackgroundColor(fragmentContext.getColor(R.color.dark_blue))
                b.shabbatMode.setTextColor(fragmentContext.getColor(R.color.white))
                b.calendar.setBackgroundColor(fragmentContext.getColor(R.color.dark_blue))
                b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(sCurrentDateShown))
            }
        }

        if (isFirstTime) {
            sCurrentDateShown.add(Calendar.DATE, -1)
            sJewishDateInfo.setCalendar(sCurrentDateShown)
        }
    }

    @Suppress("BusyWait")
    private fun startScrollingThread() {
        scrollingThread?.interrupt()
        scrollingThread = Thread {
            try {
                val nsv = binding?.nestedScrollView ?: return@Thread
                while (nsv.canScrollVertically(1)) {
                    if (!sShabbatMode) return@Thread
                    nsv.smoothScrollBy(0, 1)
                    Thread.sleep(10)
                }
                Thread.sleep(1_000)
                while (nsv.canScrollVertically(-1)) {
                    if (!sShabbatMode) return@Thread
                    nsv.smoothScrollBy(0, -1)
                    Thread.sleep(10)
                }
                Thread.sleep(1_000)
                if (sShabbatMode) startScrollingThread()
            } catch (_: InterruptedException) { }
        }.also { it.start() }
    }

    // ---- Shabbat mode banner setup ----

    private fun setupShabbatModeBanner() {
        binding?.shabbatMode?.apply {
            isSelected = true
            setOnClickListener { v ->
                v.visibility = if (v.isVisible) View.GONE else View.VISIBLE
            }
        }
    }

    // ---- Israel check ----

    private fun checkIfUserIsInIsraelOrNot() {
        if (sSharedPreferences.getBoolean("neverAskInIsraelOrNot", false)) return

        if (Utils.isInOrNearIsrael(sLatitude, sLongitude)) {
            if (!sSharedPreferences.getBoolean("inIsrael", false)) {
                MaterialAlertDialogBuilder(fragmentContext)
                    .setTitle(R.string.are_u_in_israel)
                    .setMessage(R.string.if_you_are_in_israel_now_please_confirm_below)
                    .setPositiveButton(R.string.yes_i_am_in_israel) { _, _ ->
                        sJewishDateInfo.jewishCalendar.inIsrael = true
                        sSettingsPreferences.edit {
                            putBoolean("inIsrael", true)
                            putBoolean("useElevation", true)
                            putBoolean("LuachAmudeiHoraah", false)
                        }
                        Toast.makeText(fragmentContext, R.string.settings_updated, Toast.LENGTH_SHORT).show()
                        viewModel.loadZmanim()
                        initMenu()
                    }
                    .setNegativeButton(R.string.no_i_am_not_in_israel) { d, _ -> d.dismiss() }
                    .setNeutralButton(R.string.do_not_ask_me_again) { d, _ ->
                        sSharedPreferences.edit { putBoolean("neverAskInIsraelOrNot", true) }
                        d.dismiss()
                    }
                    .show()
            }
        } else {
            if (sSharedPreferences.getBoolean("inIsrael", false)) {
                MaterialAlertDialogBuilder(fragmentContext)
                    .setTitle(R.string.have_you_left_israel)
                    .setMessage(R.string.if_you_are_not_in_israel_now_please_confirm_below_otherwise_ignore_this_message)
                    .setPositiveButton(R.string.yes_i_have_left_israel) { _, _ ->
                        sJewishDateInfo.jewishCalendar.inIsrael = false
                        sSharedPreferences.edit {
                            putBoolean("inIsrael", false)
                            putBoolean("useElevation", false)
                            putBoolean("LuachAmudeiHoraah", true)
                        }
                        Toast.makeText(fragmentContext, R.string.settings_updated, Toast.LENGTH_SHORT).show()
                        sElevation = 0.0
                        viewModel.instantiateZmanimCalendar()
                        viewModel.onDateChanged()
                        initMenu()
                    }
                    .setNegativeButton(R.string.no_i_have_not_left_israel) { d, _ -> d.dismiss() }
                    .setNeutralButton(R.string.do_not_ask_me_again) { d, _ ->
                        sSharedPreferences.edit { putBoolean("neverAskInIsraelOrNot", true) }
                        d.dismiss()
                    }
                    .show()
            }
        }
    }

    // ---- Tables ----

    private var updateTablesDialogShown = false

    private fun seeIfTablesNeedToBeUpdated(fromButton: Boolean) {
        if (!sSharedPreferences.getBoolean("isSetup", false)) return
        if (!sSharedPreferences.getBoolean("UseTable$sCurrentLocationName", false)) return
        if (!ChaiTablesWebJava.checkIfFileDoesNotExist(requireActivity().getExternalFilesDir(null), sCurrentLocationName, sJewishDateInfo.jewishCalendar.jewishYear)) return
        if (updateTablesDialogShown) return

        MaterialAlertDialogBuilder(fragmentContext)
            .setTitle(R.string.update_tables)
            .setMessage(R.string.the_visible_sunrise_tables_for_the_current_location_and_year_need_to_be_updated_do_you_want_to_update_the_tables_now)
            .setPositiveButton(R.string.yes) { _, _ ->
                val url = sSharedPreferences.getString("chaitablesLink$sCurrentLocationName", "") ?: ""
                if (url.isNotEmpty()) {
                    viewModel.downloadTablesIfNeeded(sJewishDateInfo.jewishCalendar.jewishYear, url)
                }
            }
            .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
            .show()

        if (fromButton) updateTablesDialogShown = true
    }

    // ---- Dialogs ----

    private fun showLocationInfoDialog() {
        val geo = sROZmanimCalendar.geoLocation
        MaterialAlertDialogBuilder(fragmentContext)
            .setTitle("${fragmentContext.getString(R.string.location_info_for)} ${geo.locationName}")
            .setMessage(
                "${fragmentContext.getString(R.string.location_name)} ${geo.locationName}\n" +
                "${fragmentContext.getString(R.string.latitude)} ${geo.latitude}\n" +
                "${fragmentContext.getString(R.string.longitude)} ${geo.longitude}\n" +
                "${fragmentContext.getString(R.string.elevation)} ${if (sSharedPreferences.getBoolean("useElevation", true)) sSharedPreferences.getString("elevation${geo.locationName}", "0") else "0"} ${fragmentContext.getString(R.string.meters)}\n" +
                "${fragmentContext.getString(R.string.time_zone)}${geo.timeZone.id}"
            )
            .setPositiveButton(R.string.share) { _, _ ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT,
                        "https://royzmanim.com/calendar?locationName=${sCurrentLocationName.replace(" ", "+").replace(",", "%2C")}&lat=$sLatitude&long=$sLongitude&elevation=$sElevation&timeZone=$sCurrentTimeZoneID")
                }
                fragmentContext.startActivity(Intent.createChooser(shareIntent, fragmentContext.getString(R.string.share)))
            }
            .setNeutralButton(R.string.change_location) { _, _ ->
                fragmentContext.startActivity(Intent(fragmentContext, GetUserLocationWithMapActivity::class.java).putExtra("loneActivity", true))
            }
            .setNegativeButton(fragmentContext.getString(R.string.setup_elevation)) { _, _ ->
                fragmentContext.startActivity(Intent(fragmentContext, SetupElevationActivity::class.java).putExtra("loneActivity", true))
            }
            .show()
    }

    private fun showParshaDialog() {
        val b = binding ?: return
        val title = b.parsha.text.toString()
        if (title == "No Weekly Parsha" || title == "אין פרשת שבוע") return

        val parsha = when {
            title in listOf("לך לך", "חיי שרה", "כי תשא", "אחרי מות", "שלח לך", "כי תצא", "כי תבוא", "וזאת הברכה ") -> title
            title.contains("אחרי מות") -> "אחרי מות"
            else -> title.split(" ")[0]
        }

        MaterialAlertDialogBuilder(fragmentContext)
            .setTitle("${fragmentContext.getString(R.string.open_sefaria_link_for)}$parsha?")
            .setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_weekly_parsha)
            .setPositiveButton(R.string.ok) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, "https://www.sefaria.org/$parsha".toUri()))
			}
			.setNegativeButton(R.string.dismiss) { d, _ -> d.dismiss() }
            .show()
    }

    // ---- Menu ----

    private fun initMenu() {
        val toolbar = materialToolbar ?: return
        if (Utils.isLocaleHebrew(fragmentContext)) toolbar.subtitle = "" else toolbar.title = fragmentContext.getString(R.string.app_name)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.weekly_mode -> {
                    MainFragmentManagerActivity.switchZmanimFragment(true)
                    true
                }
                R.id.search_for_a_place -> {
                    startActivity(Intent(fragmentContext, GetUserLocationWithMapActivity::class.java).putExtra("loneActivity", true)); true
                }
                R.id.shabbat_mode -> {
                    if (!sShabbatMode) viewModel.enterShabbatMode() else viewModel.exitShabbatMode()
                    item.isChecked = sShabbatMode; true
                }
                R.id.use_elevation -> {
                    sSharedPreferences.edit {
						putBoolean(
							"useElevation",
							!sSharedPreferences.getBoolean("useElevation", false)
						)
					}
                    item.isChecked = sSharedPreferences.getBoolean("useElevation", false)
                    viewModel.loadZmanim(); true
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
        toolbar.menu.findItem(R.id.shabbat_mode)?.isChecked = sShabbatMode
        toolbar.menu.findItem(R.id.weekly_mode)?.isChecked = false // always unchecked — we are the daily fragment
        toolbar.menu.findItem(R.id.use_elevation)?.isChecked = sSharedPreferences.getBoolean("useElevation", true)
        // weekly_mode item is not shown in daily fragment's menu
    }

    // ---- Permissions ----

    private fun askForRealTimeNotificationPermissions() {
        if (ActivityCompat.checkSelfPermission(fragmentContext, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(fragmentContext, Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        if (sSharedPreferences.getBoolean("askedForRealtimeNotifications", false)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        MaterialAlertDialogBuilder(fragmentContext)
            .setTitle(R.string.would_you_like_to_receive_real_time_notifications_for_zmanim)
            .setMessage(R.string.if_you_would_like_to_receive_real_time_zmanim_notifications)
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { _, _ ->
                if (ActivityCompat.checkSelfPermission(fragmentContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
                }
                sSharedPreferences.edit { putBoolean("askedForRealtimeNotifications", true) }
            }
            .setNegativeButton(R.string.no) { d, _ ->
                sSharedPreferences.edit { putBoolean("askedForRealtimeNotifications", true) }
                d.dismiss()
            }
            .apply { if (!requireActivity().isFinishing) show() }
    }

    // ---- Theme ----

    private fun setSafeSettingsForXiaomi() {
        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
            val b = binding ?: return
            arrayOf(b.weekday, b.topDate, b.bottomDate, b.parsha, b.haftara, b.makam, b.nightOfChanuka, b.lchatchilaLightingTime, b.fullWindowLightingTime).forEach {
                TextViewCompat.setAutoSizeTextTypeWithDefaults(it, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
            }
        }
    }

    private fun setCustomThemeColors() {
        val b = binding ?: return
        if (sSharedPreferences.getBoolean("useImage", false)) {
            val bitmap: Bitmap? = BitmapFactory.decodeFile(sSharedPreferences.getString("imageLocation", ""))
            b.mainLayout.background = bitmap?.toDrawable(resources)
        } else if (sSharedPreferences.getBoolean("customBackgroundColor", false) && !sSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
            val bg = sSharedPreferences.getInt("bColor", 0x32312C)
            b.mainLayout.setBackgroundColor(bg)
            b.dailyCard.setCardBackgroundColor(bg)
            b.parshaCard.setCardBackgroundColor(bg)
            b.nextDay.setBackgroundColor(bg)
            b.prevDay.setBackgroundColor(bg)
        } else {
            b.mainLayout.setBackgroundColor(0)
            b.dailyCard.setCardBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.cardview_background))
            b.parshaCard.setCardBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.cardview_background))
            b.nextDay.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.buttonColor))
            b.prevDay.setBackgroundColor(ContextCompat.getColor(fragmentContext, R.color.buttonColor))
        }

        val textColor = if (sSharedPreferences.getBoolean("customTextColor", false))
            sSharedPreferences.getInt("tColor", 0xFFFFFFFF.toInt())
        else ContextCompat.getColor(fragmentContext, R.color.textColor)

        with(b) {
            dailyLocationName.setTextColor(textColor)
            weekday.setTextColor(textColor)
            topDate.setTextColor(textColor)
            bottomDate.setTextColor(textColor)
            parsha.setTextColor(textColor)
            haftara.setTextColor(textColor)
            makam.setTextColor(textColor)
            nightOfChanuka.setTextColor(textColor)
            lchatchilaLightingTime.setTextColor(textColor)
            fullWindowLightingTime.setTextColor(textColor)
        }

        if (!sShabbatMode) {
            b.calendar.setBackgroundColor(
                if (sSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) fragmentContext.getColor(R.color.dark_blue)
                else sSharedPreferences.getInt("CalButtonColor", 0x18267C)
            )
        }
        b.calendar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown))
    }

    private fun resetSystemTheme() {
        val theme = sSettingsPreferences.getString("theme", "Auto (Follow System Theme)")
        val delegate = (requireActivity() as AppCompatActivity).delegate
        when (theme) {
            "Auto (Follow System Theme)" -> if (delegate.localNightMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "Day" -> if (delegate.localNightMode != AppCompatDelegate.MODE_NIGHT_NO) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Night" -> if (delegate.localNightMode != AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    // ---- Share intent ----

    private fun handleShareIntent() {
        val intent = requireActivity().intent
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            locationResolver.getLatitudeAndLongitudeFromSearchQuery(sharedText)
            viewModel.loadZmanim()
        }
    }

    // ---- Consumer<Location> (GPS callback) ----

    fun accept(location: Location?) {
        if (sLatitude == 0.0 && sLongitude == 0.0) {
            sLatitude = java.lang.Double.longBitsToDouble(sSharedPreferences.getLong("Lat", 0))
            sLongitude = java.lang.Double.longBitsToDouble(sSharedPreferences.getLong("Long", 0))
        }
        if (location != null) {
            sLatitude = location.latitude
            sLongitude = location.longitude
            sSharedPreferences.edit {
				putLong("Lat", java.lang.Double.doubleToRawLongBits(sLatitude))
                putLong("Long", java.lang.Double.doubleToRawLongBits(sLongitude))
			}
            locationResolver.resolveCurrentLocationName()
            locationResolver.setTimeZoneID()
            viewModel.loadZmanim()
        } else {
            Thread.sleep(500)
            if (binding?.shimmerLayout?.visibility == View.VISIBLE) {
                viewModel.loadZmanim()
                locationResolver.acquireLatitudeAndLongitude { location -> accept(location) }
            }
        }
    }
}
