package com.ej.rovadiahyosefcalendar.activities.ui.limudiim;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSettingsPreferences;

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
import com.ej.rovadiahyosefcalendar.classes.CalendarDrawable;
import com.ej.rovadiahyosefcalendar.classes.ChafetzChayimYomiCalculator;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.LimudAdapter;
import com.ej.rovadiahyosefcalendar.classes.LimudListEntry;
import com.ej.rovadiahyosefcalendar.classes.LocaleChecker;
import com.ej.rovadiahyosefcalendar.classes.MishnaYomi;
import com.ej.rovadiahyosefcalendar.databinding.FragmentLimudBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class LimudFragment extends Fragment {

    private FragmentLimudBinding binding;
    private Context mContext;
    private FragmentActivity mActivity;
    RecyclerView limudRV;
    private RecyclerView hillulotRV;
    private Button mCalendarButton;
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
        if (mCurrentDateShown == null) {
            mCurrentDateShown = Calendar.getInstance();
        }
        setDate();
        setupRecyclerViews();
        setupButtons();
        updateLists();
        return root;
    }

    private void setDate() {
        StringBuilder sb = new StringBuilder();
        sb.append(mCurrentDateShown.get(Calendar.DATE));
        sb.append(" ");
        sb.append(mCurrentDateShown.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        sb.append(", ");
        sb.append(mCurrentDateShown.get(Calendar.YEAR));
        if (DateUtils.isSameDay(mCurrentDateShown.getTime(), new Date())) {
            sb.append("   ▼   ");//add a down arrow to indicate that this is the current day
        } else {
            sb.append("      ");
        }
        sb.append(mJewishDateInfo.getJewishCalendar().toString()
                .replace("Teves", "Tevet")
                .replace("Tishrei", "Tishri"));
        if (binding != null) {
            binding.hillulotDate.setText(sb.toString());
        }
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        materialToolbar.setTitle(mContext.getString(R.string.limudim_hillulot));
        if (LocaleChecker.isLocaleHebrew()) {
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
            limudRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));//add 2 to make it look bold
            hillulotRV = binding.HillulotRV;
            hillulotRV.setLayoutManager(new LinearLayoutManager(mContext));
            hillulotRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
            hillulotRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));//add 2 to make it look bold
        }
    }

    private void updateLists() {
        limudRV.setAdapter(new LimudAdapter(mContext, getLimudList()));
        hillulotRV.setAdapter(new LimudAdapter(mContext, getHillulotList()));
    }

    private List<LimudListEntry> getLimudList() {
        List<LimudListEntry> limudim = new ArrayList<>();

        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            limudim.add(new LimudListEntry(mContext.getString(R.string.daf_yomi)  + " " + YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " + mHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf())));
        }

        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
            if (dafYomiYerushalmi != null) {
                String masechta = dafYomiYerushalmi.getYerushalmiMasechta();
                String daf = mHebrewDateFormatter.formatHebrewNumber(dafYomiYerushalmi.getDaf());
                limudim.add(new LimudListEntry(mContext.getString(R.string.yerushalmi_yomi) + " " + masechta + " " + daf));
            } else {
                limudim.add(new LimudListEntry(mContext.getString(R.string.no_daf_yomi_yerushalmi)));
            }
        }

        String mishnaYomi = MishnaYomi.getMishnaForDate(mJewishDateInfo.getJewishCalendar(), true);
        if (mishnaYomi != null) {
            limudim.add(new LimudListEntry(getString(R.string.mishna_yomi) + " " + mishnaYomi));
        }

        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_chafetz_chaim) + ChafetzChayimYomiCalculator.getChafetzChayimYomi(mJewishDateInfo.getJewishCalendar())));

        ArrayList<String> dailyMonthlyTehilim;
        if (LocaleChecker.isLocaleHebrew()) {
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
                    "קמ - " + (mJewishDateInfo.getJewishCalendar().getDaysInJewishMonth() == 29 ? "קנ" : "קמה"), // 140 - 150 or 145
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
                    "140 - " + (mJewishDateInfo.getJewishCalendar().getDaysInJewishMonth() == 29 ? 150 : 145),
                    "145 - 150"));
        }

        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_tehilim) + mContext.getString(R.string.monthly) + ": " + dailyMonthlyTehilim.get(mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth() - 1)));

        ArrayList<String> dailyWeeklyTehilim;
        if (LocaleChecker.isLocaleHebrew()) {
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
        limudim.add(new LimudListEntry(mContext.getString(R.string.daily_tehilim) + mContext.getString(R.string.weekly) + ": " + dailyWeeklyTehilim.get(mCurrentDateShown.get(Calendar.DAY_OF_WEEK) - 1)));

        return limudim;
    }

    private List<LimudListEntry> getHillulotList() {
        List<LimudListEntry> hillulot = new ArrayList<>();

        // Read JSON file from the 'raw' directory
        String jsonFileString = readJSONFromRawResource(mContext,
                LocaleChecker.isLocaleHebrew() ? R.raw.hiloulah_he : R.raw.hiloulah_en);

        if (jsonFileString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonFileString);

                int month = mJewishDateInfo.getJewishCalendar().getJewishMonth();
                int day = mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth();
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
                    String src = (String) currentHillulot.getJSONObject(i).get("src");
                    if (!src.isEmpty() && !src.equals("-")) {
                        hillulot.add(new LimudListEntry(name, src));
                    } else {
                        hillulot.add(new LimudListEntry(name));
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
            Button previousDate = binding.prevDay;
            previousDate.setOnClickListener(v -> {
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();//just get a calendar object with the same date as the current one
                mCurrentDateShown.add(Calendar.DATE, -1);//subtract one day
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                setDate();
                updateLists();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
            });
        }
    }

    /**
     * Sets up the next day button
     */
    private void setupNextDayButton() {
        if (binding != null) {
            Button nextDate = binding.nextDay;
            nextDate.setOnClickListener(v -> {
                mCurrentDateShown.add(Calendar.DATE, 1);//add one day
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                setDate();
                updateLists();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
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
                        .setSelection(mCurrentDateShown.getTimeInMillis())// can be in local timezone
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                    Calendar epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    epoch.setTimeInMillis(selection);
                    mCurrentDateShown.set(
                            epoch.get(Calendar.YEAR),
                            epoch.get(Calendar.MONTH),
                            epoch.get(Calendar.DATE),
                            epoch.get(Calendar.HOUR_OF_DAY),
                            epoch.get(Calendar.MINUTE)
                    );
                    mROZmanimCalendar.setCalendar(mCurrentDateShown);
                    mJewishDateInfo.setCalendar(mCurrentDateShown);
                    setDate();
                    updateLists();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                });
                DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                    Calendar mUserChosenDate = Calendar.getInstance();
                    mUserChosenDate.set(year, month, day);
                    mROZmanimCalendar.setCalendar(mUserChosenDate);
                    mJewishDateInfo.setCalendar(mUserChosenDate);
                    mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();
                    setDate();
                    updateLists();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                };
                materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                    HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, mActivity.getSupportFragmentManager(), mJewishDateInfo.getJewishCalendar());
                    hdmypd.updateDate(mJewishDateInfo.getJewishCalendar().getGregorianYear(),
                            mJewishDateInfo.getJewishCalendar().getGregorianMonth(),
                            mJewishDateInfo.getJewishCalendar().getGregorianDayOfMonth());
                    hdmypd.setListener(onDateSetListener);
                    hdmypd.show(mActivity.getSupportFragmentManager(), null);
                });
                materialDatePicker.show(mActivity.getSupportFragmentManager(), null);
            });

            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
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