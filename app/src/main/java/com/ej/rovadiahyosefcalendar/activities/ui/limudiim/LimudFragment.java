package com.ej.rovadiahyosefcalendar.activities.ui.limudiim;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences;

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
import com.ej.rovadiahyosefcalendar.classes.ChafetzChayimYomiCalculator;
import com.ej.rovadiahyosefcalendar.classes.DailyMishnehTorah;
import com.ej.rovadiahyosefcalendar.classes.HalachaSegment;
import com.ej.rovadiahyosefcalendar.classes.HalachaYomi;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.LimudAdapter;
import com.ej.rovadiahyosefcalendar.classes.LimudListEntry;
import com.ej.rovadiahyosefcalendar.classes.MishnaYomi;
import com.ej.rovadiahyosefcalendar.classes.NisanLimudYomi;
import com.ej.rovadiahyosefcalendar.classes.RambamReading;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.databinding.FragmentLimudBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class LimudFragment extends Fragment {

    private FragmentLimudBinding binding;
    private Context mContext;
    private FragmentActivity mActivity;
    private RecyclerView limudRV;
    private RecyclerView hillulotRV;
    private Button mCalendarButton;
    private boolean mSeeMore = true;
    private boolean mAdjustDate = true;
    /**
     * These calendars are used to know when daf/yerushalmi yomi started
     */
    public final static Calendar dafYomiStartDate = new GregorianCalendar(1923, Calendar.SEPTEMBER, 11);
    public final static Calendar dafYomiYerushalmiStartDate = new GregorianCalendar(1980, Calendar.FEBRUARY, 2);

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = requireActivity();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLimudBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        if (sCurrentDateShown == null) {
            sCurrentDateShown = Calendar.getInstance();
        }
        setDate();
        setupRecyclerViews();
        setupButtons();
        updateLists();
        return root;
    }

    private boolean isToday() {
        return DateUtils.isSameDay(sCurrentDateShown.getTime(), new Date());
    }

    private boolean isAfterSunset() {
        return sROZmanimCalendar.getSunset() != null && new Date().after(sROZmanimCalendar.getSunset());
    }

    private void setDate() {
        boolean today = isToday();
        boolean afterSunset = today && mAdjustDate && isAfterSunset();

        if (!Utils.isLocaleHebrew(mContext)) {// if not hebrew, show the date in english
            StringBuilder hebrewDate = new StringBuilder()
				.append(sCurrentDateShown.get(Calendar.DATE))
				.append(" ")
				.append(sCurrentDateShown.getDisplayName(
						Calendar.MONTH,
						Calendar.SHORT,
						mContext.getResources().getConfiguration().getLocales().get(0)))
				.append(", ")
				.append(sCurrentDateShown.get(Calendar.YEAR))
				.append("   ");

			if (binding != null) {
				assert binding.hebrewDateTextView != null;
				binding.hebrewDateTextView.setText(hebrewDate.toString());

				assert binding.currentDateArrow != null;
				binding.currentDateArrow.setVisibility(today ? View.VISIBLE : View.GONE);
				binding.currentDateArrow.setText(afterSunset ? "🌙" : "☀️");

				assert binding.englishDateTextView != null;
				binding.englishDateTextView.setText(
					afterSunset
						? sJewishDateInfo.getJewishCalendar().currentToShortString(sROZmanimCalendar)
						: sJewishDateInfo.getJewishCalendar().toShortString()
				);
			}
        } else {
			assert binding.currentDateArrow != null;
			assert binding.englishDateTextView != null;
			binding.currentDateArrow.setVisibility(View.GONE);
			binding.englishDateTextView.setVisibility(View.GONE);

			assert binding.hebrewDateTextView != null;
			binding.hebrewDateTextView.setText(
				today ?
					afterSunset ? "🌙 " + sJewishDateInfo.getJewishCalendar().currentToString(sROZmanimCalendar)
						: "☀️ " + sJewishDateInfo.getJewishCalendar().toString()
					: sJewishDateInfo.getJewishCalendar().toString()
			);
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
        }
    }

    private void updateLists() {
        boolean isNowAdjustable = isToday() && mAdjustDate && isAfterSunset();
        if (isNowAdjustable && mAdjustDate) {
            sCurrentDateShown.add(Calendar.DATE, 1);
            sJewishDateInfo.setCalendar(sCurrentDateShown);
        }
        limudRV.setAdapter(new LimudAdapter(mContext, getLimudList(), sJewishDateInfo, v -> {
            mSeeMore = false;
            limudRV.setAdapter(new LimudAdapter(mContext, getLimudList(), sJewishDateInfo));
        }));
        List<LimudListEntry> list = getHillulotList();
        hillulotRV.setAdapter(new LimudAdapter(mContext, list, sJewishDateInfo));
        if (list.isEmpty()) {
            hillulotRV.setVisibility(View.GONE);
        } else {
            hillulotRV.setVisibility(View.VISIBLE);
        }
        if (isNowAdjustable && mAdjustDate) {// reset
            sCurrentDateShown.add(Calendar.DATE, -1);
            sJewishDateInfo.setCalendar(sCurrentDateShown);
        }
    }

    private List<LimudListEntry> getLimudList() {
        List<LimudListEntry> limudim = new ArrayList<>();

        if (!sCurrentDateShown.before(dafYomiStartDate)) {
            limudim.add(new LimudListEntry(mContext.getString(R.string.daf_yomi)  + " " + YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " + sHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(sJewishDateInfo.getJewishCalendar()).getDaf())));
        }

        if (!sCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
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

        LocalDate currentDate = LocalDate.of(
                sCurrentDateShown.get(Calendar.YEAR),
                sCurrentDateShown.get(Calendar.MONTH) + 1,
                sCurrentDateShown.get(Calendar.DATE));

        List<HalachaSegment> halachaSegments = HalachaYomi.INSTANCE.getDailyLearning(currentDate);
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

        ArrayList<String> dailyMonthlyTehilim;
        if (Utils.isLocaleHebrew(mContext)) {
            dailyMonthlyTehilim = new ArrayList<>(Arrays.asList(
                    "א - ט",       // 1 - 9
                    "י - יז",      // 10 - 17
                    "יח - כב",     // 18 - 22
                    "כג - כח",     // 23 - 28
                    "כט - לד",     // 29 - 34
                    "לה - לח",     // 35 - 38
                    "לט - מג",     // 39 - 43
                    "מד - מח",     // 44 - 48
                    "מט - נד",     // 49 - 54
                    "נה - נט",     // 55 - 59
                    "ס - סה",      // 60 - 65
                    "סו - סח",     // 66 - 68
                    "סט - עא",     // 69 - 71
                    "עב - עו",     // 72 - 76
                    "עז - עח",     // 77 - 78
                    "עט - פב",     // 79 - 82
                    "פג - פז",     // 83 - 87
                    "פח - פט",     // 88 - 89
                    "צ - צו",      // 90 - 96
                    "צז - קג",     // 97 - 103
                    "קד - קה",     // 104 - 105
                    "קו - קז",     // 106 - 107
                    "קח - קיב",    // 108 - 112
                    "קיג - קיח",   // 113 - 118
                    "קיט:א - קיט:צו", // 119:1 - 119:96
                    "קיט:צז - קיט:קעו", // 119:97 - 119:176
                    "קכ - קלד",     // 120 - 134
                    "קל - קלט",     // 135 - 139
                    "קמ - " + (sJewishDateInfo.getJewishCalendar().getDaysInJewishMonth() == 29 ? "קנ" : "קמה"), // 140 - 150 or 145
                    "קמה - קנ"       // 145 - 150
            ));
        } else {
            dailyMonthlyTehilim = new ArrayList<>(Arrays.asList(
                    "1 - 9",
                    "10 - 17",
                    "18 - 22",
                    "23 - 28",
                    "29 - 34",
                    "35 - 38",
                    "39 - 43",
                    "44 - 48",
                    "49 - 54",
                    "55 - 59",
                    "60 - 65",
                    "66 - 68",
                    "69 - 71",
                    "72 - 76",
                    "77 - 78",
                    "79 - 82",
                    "83 - 87",
                    "88 - 89",
                    "90 - 96",
                    "97 - 103",
                    "104 - 105",
                    "106 - 107",
                    "108 - 112",
                    "113 - 118",
                    "119:1 - 119:96",
                    "119:97 - 119:176",
                    "120 - 134",
                    "135 - 139",
                    "140 - " + (sJewishDateInfo.getJewishCalendar().getDaysInJewishMonth() == 29 ? 150 : 145),
                    "145 - 150"));
        }
        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_tehilim) + mContext.getString(R.string.monthly) + ": " + dailyMonthlyTehilim.get(sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth() - 1)));

        ArrayList<String> dailyWeeklyTehilim;
        if (Utils.isLocaleHebrew(mContext)) {
            dailyWeeklyTehilim = new ArrayList<>(Arrays.asList(
                    "א - כט",      // 1 - 29
                    "ל - נ",       // 30 - 50
                    "נא - עב",     // 51 - 72
                    "עג - פט",     // 73 - 89
                    "צ - קו",      // 90 - 106
                    "קז - קיט",    // 107 - 119
                    "קכ - קנ"      // 120 - 150
            ));
        } else {
            dailyWeeklyTehilim = new ArrayList<>(Arrays.asList(
                    "1 - 29",
                    "30 - 50",
                    "51 - 72",
                    "73 - 89",
                    "90 - 106",
                    "107 - 119",
                    "120 - 150"
            ));
        }
        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_tehilim) + mContext.getString(R.string.weekly) + ": " + dailyWeeklyTehilim.get(sJewishDateInfo.getJewishCalendar().getDayOfWeek() - 1)));

        DailyMishnehTorah dailyMishnehTorah = DailyMishnehTorah.INSTANCE;

        RambamReading rambamYomi = dailyMishnehTorah.getDailyLearning(currentDate);
        if (rambamYomi != null) {
            limudim.add(new LimudListEntry(getString(R.string.rambam_yomi) + rambamYomi.getBookName() + " " + rambamYomi.getChapter()));
        }

        List<RambamReading> rambamYomi3 = dailyMishnehTorah.getDailyLearning3(currentDate);

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
                String currentDate;// e.g. 1225 == 25th of Adar
                if (month <= 9) {
                    currentDate = "0" + month;
                } else {
                    currentDate = String.valueOf(month);// 10, 11, 12, 13 do not need a 0
                }
                if (day <= 9) {
                    currentDate += "0" + day;
                } else {
                    currentDate += String.valueOf(day);// 10 and up do not need a 0
                }
                JSONArray currentHillulot = new JSONArray(jsonObject.getString(currentDate));
                for (int i = 0; i < currentHillulot.length(); i++) {
                    String name = (String) currentHillulot.getJSONObject(i).get("name");
                    String desc = (String) currentHillulot.getJSONObject(i).get("desc");
                    String src = (String) currentHillulot.getJSONObject(i).get("src");
                    hillulot.add(new LimudListEntry(name, desc, src));
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
                if (isToday() && isAfterSunset() && mAdjustDate) {
                    mAdjustDate = false; // disable adjust date logic on today if after sunset
                } else {
                    sCurrentDateShown.add(Calendar.DATE, -1);
                }
                sROZmanimCalendar.setCalendar(sCurrentDateShown);
                sJewishDateInfo.setCalendar(sCurrentDateShown);
                setDate();
                updateLists();
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
                sCurrentDateShown.add(Calendar.DATE, 1);// add one day
                sROZmanimCalendar.setCalendar(sCurrentDateShown);
                sJewishDateInfo.setCalendar(sCurrentDateShown);
                setDate();
                updateLists();
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
                        .setSelection(sCurrentDateShown.getTimeInMillis())// can be in local timezone
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                    Calendar epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    epoch.setTimeInMillis(selection);
                    sCurrentDateShown.set(
                            epoch.get(Calendar.YEAR),
                            epoch.get(Calendar.MONTH),
                            epoch.get(Calendar.DATE),
                            epoch.get(Calendar.HOUR_OF_DAY),
                            epoch.get(Calendar.MINUTE)
                    );
                    sROZmanimCalendar.setCalendar(sCurrentDateShown);
                    sJewishDateInfo.setCalendar(sCurrentDateShown);
                    setDate();
                    updateLists();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                });
                DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                    Calendar mUserChosenDate = Calendar.getInstance();
                    mUserChosenDate.set(year, month, day);
                    sROZmanimCalendar.setCalendar(mUserChosenDate);
                    sJewishDateInfo.setCalendar(mUserChosenDate);
                    sCurrentDateShown = (Calendar) sROZmanimCalendar.getCalendar().clone();
                    setDate();
                    updateLists();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                };
                materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                    HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, mActivity.getSupportFragmentManager(), sJewishDateInfo.getJewishCalendar());
                    hdmypd.updateDate(sJewishDateInfo.getJewishCalendar().getGregorianYear(),
                            sJewishDateInfo.getJewishCalendar().getGregorianMonth(),
                            sJewishDateInfo.getJewishCalendar().getGregorianDayOfMonth());
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