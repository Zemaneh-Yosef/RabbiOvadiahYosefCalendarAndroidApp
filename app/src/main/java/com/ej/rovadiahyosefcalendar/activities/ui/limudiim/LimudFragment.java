package com.ej.rovadiahyosefcalendar.activities.ui.limudiim;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity;
import com.ej.rovadiahyosefcalendar.classes.ChafetzChayimYomiCalculator;
import com.ej.rovadiahyosefcalendar.classes.DailyMishnehTorah;
import com.ej.rovadiahyosefcalendar.classes.HalachaSegment;
import com.ej.rovadiahyosefcalendar.classes.HalachaYomi;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LimudAdapter;
import com.ej.rovadiahyosefcalendar.classes.LimudListEntry;
import com.ej.rovadiahyosefcalendar.classes.MishnaYomi;
import com.ej.rovadiahyosefcalendar.classes.NisanLimudYomi;
import com.ej.rovadiahyosefcalendar.classes.RambamReading;
import com.ej.rovadiahyosefcalendar.classes.TehilimYomi;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.classes.WeeklyHillulotAdapter;
import com.ej.rovadiahyosefcalendar.classes.WeeklyLimudRanges;
import com.ej.rovadiahyosefcalendar.databinding.FragmentLimudBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LimudFragment extends Fragment {

    private FragmentLimudBinding binding;
    private Context mContext;
    private FragmentActivity mActivity;
    private RecyclerView limudRV;
    private RecyclerView hillulotRV;
    private Button mCalendarButton;
    private boolean mSeeMore = true;
    private boolean mAdjustDate = true;
    private boolean mIsTwoPane;
    /**
     * These calendars are used to know when daf/yerushalmi yomi started
     */
    public final static LocalDate dafYomiStartDate = LocalDate.of(1923, Month.SEPTEMBER, 11);
    public final static LocalDate dafYomiYerushalmiStartDate = LocalDate.of(1980, Month.FEBRUARY, 2);

    public LimudFragment() {}

    public static LimudFragment newInstance(String data) {
        LimudFragment fragment = new LimudFragment();
        Bundle args = new Bundle();
        args.putString("key", data);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = requireActivity();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLimudBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mIsTwoPane = getResources().getBoolean(R.bool.is_two_pane);
        if (sCurrentDateShown == null) {
            sCurrentDateShown = LocalDate.now();
        }
        setDate();
        setupRecyclerViews();
        setupButtons();
        updateLists();
        return root;
    }

    private boolean isToday() {
        return sCurrentDateShown.equals(LocalDate.now());
    }

    /**
     * Weekly mode only changes this tab on wide screens, where it shows the week's learning and hillulot.
     */
    private boolean isWeekly() {
        return mIsTwoPane && sSharedPreferences.getBoolean("weeklyMode", false);
    }

    /**
     * @return the Sunday of the week shown in weekly mode, the same week as the weekly zmanim
     */
    private LocalDate getWeekStart() {
        return sCurrentDateShown.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    private boolean isAfterSunset() {
        return sROZmanimCalendar.getSunset() != null && Instant.now().isAfter(sROZmanimCalendar.getSunset());
    }

    private void setDate() {
        if (isWeekly()) {
            setWeekDate();
            return;
        }
        if (binding != null && binding.hillulotDate != null) {
            binding.hillulotDate.setText("");// only used for the week in weekly mode
        }
        boolean today = isToday();
        boolean afterSunset = today && mAdjustDate && isAfterSunset();

        if (!Utils.isLocaleHebrew(mContext)) {// if not hebrew, show the date in english
            StringBuilder engDate = new StringBuilder()
				.append(sCurrentDateShown.getDayOfMonth())
				.append(" ")
				.append(sCurrentDateShown.getMonth().getDisplayName(TextStyle.SHORT,
                        mContext.getResources().getConfiguration().getLocales().get(0)))
				.append(", ")
				.append(sCurrentDateShown.getYear());

			if (binding != null) {
                if (binding.englishDateTextView != null) {
                    binding.englishDateTextView.setText(engDate.toString());
                }

                if (binding.currentDateArrow != null) {
                    binding.currentDateArrow.setVisibility(today ? View.VISIBLE : View.GONE);
                }
                if (binding.currentDateArrow != null) {
                    binding.currentDateArrow.setText(afterSunset ? "🌙" : "☀️");
                }

                if (binding.hebrewDateTextView != null) {
                    binding.hebrewDateTextView.setText(
                        afterSunset
                            ? sJewishDateInfo.getJewishCalendar().currentToShortString(sROZmanimCalendar)
                            : sJewishDateInfo.getJewishCalendar().toShortString()
                    );
                }
            }
        } else {
            if (binding != null) {
                if (binding.englishDateTextView != null) {
                    binding.englishDateTextView.setVisibility(View.GONE);
                }
                if (binding.currentDateArrow != null) {
                    binding.currentDateArrow.setVisibility(View.GONE);
                }
                if (binding.hebrewDateTextView != null) {
                    binding.hebrewDateTextView.setText(
                        today ?
                            afterSunset ? "🌙 " + sJewishDateInfo.getJewishCalendar().currentToString(sROZmanimCalendar)
                                : "☀️ " + sJewishDateInfo.getJewishCalendar().toString()
                            : sJewishDateInfo.getJewishCalendar().toString()
                    );
                }
            }
		}
    }

    /**
     * Shows the week instead of the day at the top, e.g. "25 Oct – 31 Oct, 2026" and "14 Ḥesh – 20 Ḥesh, 5787".
     */
    private void setWeekDate() {
        if (binding == null) {
            return;
        }
        LocalDate firstDay = getWeekStart();
        LocalDate lastDay = firstDay.plusDays(6);
        JewishDateInfo firstDayInfo = sJewishDateInfo.getCopy();
        firstDayInfo.setLocalDate(firstDay);
        JewishDateInfo lastDayInfo = sJewishDateInfo.getCopy();
        lastDayInfo.setLocalDate(lastDay);
        DateTimeFormatter dayAndMonth = DateTimeFormatter.ofPattern("d MMM", mContext.getResources().getConfiguration().getLocales().get(0));
        String englishWeek = firstDay.format(dayAndMonth) + " – " + lastDay.format(dayAndMonth) + ", " + lastDay.getYear();
        String hebrewWeek = firstDayInfo.getJewishCalendar().toShortString() + " – " + lastDayInfo.getJewishCalendar().toShortString();
        boolean hebrew = Utils.isLocaleHebrew(mContext);
        if (binding.englishDateTextView != null) {
            binding.englishDateTextView.setVisibility(hebrew ? View.GONE : View.VISIBLE);
            binding.englishDateTextView.setText(englishWeek);
        }
        if (binding.currentDateArrow != null) {
            binding.currentDateArrow.setVisibility(View.GONE);
        }
        if (binding.hebrewDateTextView != null) {
            binding.hebrewDateTextView.setText(hebrewWeek);
        }
        if (binding.hillulotDate != null) {
            binding.hillulotDate.setText(hebrew ? hebrewWeek : englishWeek + "         " + hebrewWeek);
        }
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        materialToolbar.setTitle(mContext.getString(R.string.limudim_hillulot));
        if (Utils.isLocaleHebrew(mContext)) {
            materialToolbar.setSubtitle(mContext.getString(R.string.app_name));
        } else {
            materialToolbar.setSubtitle(mContext.getString(R.string.short_app_name));
        }
        //materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(mContext, R.drawable.baseline_arrow_back_24)); // if you want to show the back button
        //materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.getMenu().clear();
    }

    private void setupRecyclerViews() {
        if (binding != null) {// it should never be null, but just in case
            limudRV = binding.limudRV;
            limudRV.setLayoutManager(new LinearLayoutManager(mContext));
            limudRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
            limudRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));// add 2 to make it look bold
            limudRV.setHasFixedSize(false); // apparently not needed as it is false by default, but chatGPT says to keep it in
            hillulotRV = binding.HillulotRV;
            hillulotRV.setLayoutManager(new LinearLayoutManager(mContext));
            hillulotRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
            hillulotRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));// add 2 to make it look bold
            hillulotRV.setHasFixedSize(false); // apparently not needed as it is false by default, but chatGPT says to keep it in
            RecyclerView weeklyLimudRV = binding.weeklyLimud.weeklyLimudRV;
            weeklyLimudRV.setLayoutManager(new LinearLayoutManager(mContext));
            weeklyLimudRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
            weeklyLimudRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));// add 2 to make it look bold
            binding.weeklyLimud.weeklyHillulotRV.setLayoutManager(new LinearLayoutManager(mContext));
        }
    }

    private void updateLists() {
        if (binding == null) {
            return;
        }
        boolean weekly = isWeekly();
        View dailyLimudim = binding.dailyLimudim != null ? binding.dailyLimudim : binding.dailyLimudimSplit;// the portrait and landscape layouts
        if (dailyLimudim != null) {
            dailyLimudim.setVisibility(weekly ? View.GONE : View.VISIBLE);
        }
        binding.weeklyLimud.getRoot().setVisibility(weekly ? View.VISIBLE : View.GONE);
        if (weekly) {
            updateWeeklyLists();
            return;
        }
        boolean isNowAdjustable = isToday() && mAdjustDate && isAfterSunset();
        if (isNowAdjustable && mAdjustDate) {
            sCurrentDateShown = sCurrentDateShown.plusDays(1);
            sJewishDateInfo.setLocalDate(sCurrentDateShown);
        }
        limudRV.setAdapter(new LimudAdapter(mContext, getLimudList(), sJewishDateInfo.getCopy(), v -> {
            mSeeMore = false;
            limudRV.setAdapter(new LimudAdapter(mContext, getLimudList(), sJewishDateInfo.getCopy()));
        }));
        List<LimudListEntry> list = getHillulotList();
        hillulotRV.setAdapter(new LimudAdapter(mContext, list, sJewishDateInfo.getCopy()));
        if (list.isEmpty()) {
            hillulotRV.setVisibility(View.GONE);
        } else {
            hillulotRV.setVisibility(View.VISIBLE);
        }
        if (isNowAdjustable && mAdjustDate) {// reset
            sCurrentDateShown = sCurrentDateShown.minusDays(1);
            sJewishDateInfo.setLocalDate(sCurrentDateShown);
        }
    }

    /**
     * Fills the weekly view: the week's learning as ranges, and the hillulot of each day of the week that has any.
     */
    private void updateWeeklyLists() {
        LocalDate firstDay = getWeekStart();
        LocalDate lastDay = firstDay.plusDays(6);
        JewishDateInfo firstDayInfo = sJewishDateInfo.getCopy();
        firstDayInfo.setLocalDate(firstDay);
        binding.weeklyLimud.weeklyLimudRV.setAdapter(new LimudAdapter(mContext, WeeklyLimudRanges.getRanges(mContext, firstDay, lastDay), firstDayInfo));

        List<WeeklyHillulotAdapter.Day> days = new ArrayList<>();
        Locale locale = mContext.getResources().getConfiguration().getLocales().get(0);
        for (int i = 0; i < 7; i++) {
            LocalDate day = firstDay.plusDays(i);
            sJewishDateInfo.setLocalDate(day);// getHillulotList() reads the shared date
            List<LimudListEntry> hillulot = getHillulotList();
            if (!hillulot.isEmpty()) {
                String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale) + " " + day.getDayOfMonth()
                        + "\n" + sJewishDateInfo.getJewishCalendar().toShortString();
                days.add(new WeeklyHillulotAdapter.Day(label, day.equals(LocalDate.now()), sJewishDateInfo.getCopy(), hillulot));
            }
        }
        sJewishDateInfo.setLocalDate(sCurrentDateShown);// reset
        binding.weeklyLimud.weeklyHillulotRV.setAdapter(new WeeklyHillulotAdapter(mContext, days));
    }

    private List<LimudListEntry> getLimudList() {
        List<LimudListEntry> limudim = new ArrayList<>();

        if (!sCurrentDateShown.isBefore(dafYomiStartDate)) {
            limudim.add(new LimudListEntry(mContext.getString(R.string.daf_yomi)  + " " + YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " + sHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getDaf())));
        }

        if (!sCurrentDateShown.isBefore(dafYomiYerushalmiStartDate)) {
            Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(sJewishDateInfo.getJewishCalendar());
            if (dafYomiYerushalmi != null) {
                String masechta = dafYomiYerushalmi.getYerushalmiMasechta();
                String daf = sHebrewDateFormatter.formatHebrewNumber(dafYomiYerushalmi.getDaf());
                limudim.add(new LimudListEntry(mContext.getString(R.string.yerushalmi_yomi) + " " + masechta + " " + daf));
            } else {
                limudim.add(new LimudListEntry(mContext.getString(R.string.no_daf_yomi_yerushalmi)));
            }
        }

        String mishnaYomi = new MishnaYomi().getMishnaForDate(sJewishDateInfo.getJewishCalendar(), true);
        if (mishnaYomi != null) {
            limudim.add(new LimudListEntry(getString(R.string.mishna_yomi) + " " + mishnaYomi));
        }

        List<HalachaSegment> halachaSegments = HalachaYomi.INSTANCE.getDailyLearning(sCurrentDateShown);
        if (halachaSegments != null) {
            StringBuilder halacha = new StringBuilder();
            halacha.append(halachaSegments.get(0).getBookName()).append(" ");// book name shouldn't change on the same day
            for (HalachaSegment segment : halachaSegments) {
                halacha.append(sHebrewDateFormatter.formatHebrewNumber(segment.getSiman())).append(" ").append(segment.getSeifim()).append(", ");
            }
            halacha.delete(halacha.length() - 2, halacha.length());// remove the last comma and space
            limudim.add(new LimudListEntry(getString(R.string.daily_halacha) + " " + halacha));
        }

        if (mSeeMore) {
            limudim.add(new LimudListEntry(getString(R.string.see_more)));
            return limudim;
        }

        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_chafetz_chaim) + ChafetzChayimYomiCalculator.getChafetzChayimYomi(sJewishDateInfo.getJewishCalendar())));

        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_tehilim) + mContext.getString(R.string.monthly) + ": " + TehilimYomi.byDayOfMonth(mContext, sJewishDateInfo.getJewishCalendar())));

        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_tehilim) + mContext.getString(R.string.weekly) + ": " + TehilimYomi.byDayOfWeek(mContext, sJewishDateInfo.getJewishCalendar())));

        DailyMishnehTorah dailyMishnehTorah = DailyMishnehTorah.INSTANCE;

        RambamReading rambamYomi = dailyMishnehTorah.getDailyLearning(sCurrentDateShown);
        if (rambamYomi != null) {
            limudim.add(new LimudListEntry(getString(R.string.rambam_yomi) + rambamYomi.getBookName() + " " + rambamYomi.getChapter()));
        }

        List<RambamReading> rambamYomi3 = dailyMishnehTorah.getDailyLearning3(sCurrentDateShown);

        if (rambamYomi3 != null) {
            StringBuilder rambam3Learnings = new StringBuilder();
            for (RambamReading reading : rambamYomi3) {
                rambam3Learnings.append(reading.getBookName())
                        .append(" ")
                        .append(reading.getChapter())
                        .append("\n");
            }
            rambam3Learnings.delete(rambam3Learnings.length() - 1, rambam3Learnings.length());// remove the last new line
            limudim.add(new LimudListEntry(getString(R.string.rambam_yomi_3_chapters) + "\n" + rambam3Learnings));
        }

        if (sJewishDateInfo.getJewishCalendar().getJewishMonth() == JewishDate.NISSAN) {
            String title = NisanLimudYomi.getNisanLimudYomiTitle(sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth());
            String reading = NisanLimudYomi.getNisanLimudYomiReading(sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth());

            if (!title.isEmpty()) {
                LimudListEntry limudEntry = new LimudListEntry(getString(R.string.daily_nasi) + title, reading);
                limudEntry.setHasSource(false);
                limudim.add(limudEntry);
            }
        }

        return limudim;
    }

    private List<LimudListEntry> getHillulotList() {
        List<LimudListEntry> hillulot = new ArrayList<>();

        // Read JSON file from the 'raw' directory
        String jsonFileString = readJSONFromRawResource(mContext, Utils.isLocaleHebrew(mContext) ? R.raw.he : R.raw.en);

        if (jsonFileString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonFileString);

                int month = sJewishDateInfo.getJewishCalendar().getJewishMonth();
                int day = sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth();
                String monthString;// e.g. 1225 == 25th of Adar
                String dayString;
                if (month <= 9) {
                    monthString = "0" + month;
                } else {
                    monthString = String.valueOf(month);// 10, 11, 12, 13 do not need a 0
                }
                if (day <= 9) {
                    dayString = "0" + day;
                } else {
                    dayString = String.valueOf(day);// 10 and up do not need a 0
                }
                JSONArray currentHillulot = new JSONArray(jsonObject.getString(monthString + dayString));
                for (int i = 0; i < currentHillulot.length(); i++) {
                    String name = (String) currentHillulot.getJSONObject(i).get("name");
                    String desc = (String) currentHillulot.getJSONObject(i).get("desc");
                    String src = (String) currentHillulot.getJSONObject(i).get("src");
                    hillulot.add(new LimudListEntry(name, desc, src));
                }
                if (!sJewishDateInfo.getJewishCalendar().isJewishLeapYear()) {
                    if (month == JewishDate.ADAR) { // if it's Adar I, add the other adar's hillulot
                        currentHillulot = new JSONArray(jsonObject.getString(JewishDate.ADAR_II + dayString));
                        for (int i = 0; i < currentHillulot.length(); i++) {
                            String name = (String) currentHillulot.getJSONObject(i).get("name");
                            String desc = (String) currentHillulot.getJSONObject(i).get("desc");
                            String src = (String) currentHillulot.getJSONObject(i).get("src");
                            if (hillulot.stream().noneMatch(entry -> entry.getLimudTitle().equals(name))) {// don't add duplicates
                                hillulot.add(new LimudListEntry(name, desc, src));
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("LimudFragment", "Failed to read hillulot JSON file");
        }
        return hillulot;
    }

    private void setupButtons() {
        setupPreviousDayButton();
        setupCalendarButton();
        setupNextDayButton();
    }

    /**
     * Sets up the previous day button
     */
    private void setupPreviousDayButton() {
        if (binding != null) {
            binding.prevDay.setOnClickListener(v -> {
                if (isWeekly()) {
                    sCurrentDateShown = sCurrentDateShown.minusDays(7);
                } else if (isToday() && isAfterSunset() && mAdjustDate) {
                    mAdjustDate = false; // disable adjust date logic on today if after sunset
                } else {
                    sCurrentDateShown = sCurrentDateShown.minusDays(1);
                }
                sROZmanimCalendar.setLocalDate(sCurrentDateShown);
                sJewishDateInfo.setLocalDate(sCurrentDateShown);
                setDate();
                updateLists();
                MainFragmentManagerActivity.refreshSidebar();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
            });
        }
    }

    /**
     * Sets up the next day button
     */
    private void setupNextDayButton() {
        if (binding != null) {
            binding.nextDay.setOnClickListener(v -> {
                sCurrentDateShown = sCurrentDateShown.plusDays(isWeekly() ? 7 : 1);
                sROZmanimCalendar.setLocalDate(sCurrentDateShown);
                sJewishDateInfo.setLocalDate(sCurrentDateShown);
                setDate();
                updateLists();
                MainFragmentManagerActivity.refreshSidebar();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                if (isToday()) {
                    mAdjustDate = true;
                }
            });
        }
    }

    /**
     * Setup the calendar button to show a DatePickerDialog with an additional button to switch the calendar to the hebrew one.
     */
    private void setupCalendarButton() {
        if (binding != null) {
            mCalendarButton = binding.calendar;

            mCalendarButton.setOnClickListener(v -> {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                MaterialDatePicker<Long> materialDatePicker = builder
                        .setPositiveButtonText(R.string.ok)
                        .setNegativeButtonText(R.string.switch_calendar)
                        .setSelection(sCurrentDateShown.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                    // Convert the UTC milliseconds selection directly to a LocalDate
                    sCurrentDateShown = Instant.ofEpochMilli(selection)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate();
                    sROZmanimCalendar.setLocalDate(sCurrentDateShown);
                    sJewishDateInfo.setLocalDate(sCurrentDateShown);
                    setDate();
                    updateLists();
                    MainFragmentManagerActivity.refreshSidebar();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown)
                    );
                });
                DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                    LocalDate userChosenDate = LocalDate.of(year, month, day);
                    sROZmanimCalendar.setLocalDate(userChosenDate);
                    sJewishDateInfo.setLocalDate(userChosenDate);
                    sCurrentDateShown = userChosenDate;
                    setDate();
                    updateLists();
                    MainFragmentManagerActivity.refreshSidebar();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                };
                materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                    HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, mActivity.getSupportFragmentManager(), sJewishDateInfo.getJewishCalendar());
                    hdmypd.updateDate(sJewishDateInfo.getJewishCalendar().getLocalDate());
                    hdmypd.setListener(onDateSetListener);
                    hdmypd.show(mActivity.getSupportFragmentManager(), null);
                });
                materialDatePicker.show(mActivity.getSupportFragmentManager(), null);
            });

            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setDate();
        initMenu();
        setupButtons();
        updateLists();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private String readJSONFromRawResource(Context context, int rawResourceId) {
        String jsonString = null;
        try {
            InputStream inputStream = context.getResources().openRawResource(rawResourceId);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            jsonString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return jsonString;
    }
}