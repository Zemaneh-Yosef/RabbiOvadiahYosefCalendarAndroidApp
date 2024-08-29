package com.ej.rovadiahyosefcalendar.activities.ui.limudiim;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.dafYomiStartDate;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.dafYomiYerushalmiStartDate;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSharedPreferences;

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
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LimudAdapter;
import com.ej.rovadiahyosefcalendar.classes.LimudListEntry;
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
import java.util.Calendar;
import java.util.Date;
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
        if (mJewishDateInfo == null) {
            mJewishDateInfo = new JewishDateInfo(mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).getBoolean("inIsrael", false));
        }
        setDate();
        initMenu();
        setupRecyclerViews();
        setupButtons();
        updateLists();
        return root;
    }

    private void setDate() {
        StringBuilder sb = new StringBuilder();
        sb.append(mROZmanimCalendar.getCalendar().get(Calendar.DATE));
        sb.append(" ");
        sb.append(mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        sb.append(", ");
        sb.append(mROZmanimCalendar.getCalendar().get(Calendar.YEAR));
        if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
            sb.append("   ▼   ");//add a down arrow to indicate that this is the current day
        } else {
            sb.append("      ");
        }
        sb.append(mJewishDateInfo.getJewishCalendar().toString()
                .replace("Teves", "Tevet")
                .replace("Tishrei", "Tishri"));
        binding.hillulotDate.setText(sb.toString());
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        materialToolbar.setTitle(getString(R.string.limudim_hillulot));
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            materialToolbar.setSubtitle(getString(R.string.app_name));
        } else {
            materialToolbar.setSubtitle(getString(R.string.short_app_name));
        }
        //materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(mContext, R.drawable.baseline_arrow_back_24)); // if you want to show the back button
        //materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.getMenu().clear();
    }

    private void setupRecyclerViews() {
        limudRV = binding.limudRV;
        limudRV.setLayoutManager(new LinearLayoutManager(mContext));
        limudRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        hillulotRV = binding.HillulotRV;
        hillulotRV.setLayoutManager(new LinearLayoutManager(mContext));
        hillulotRV.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
    }

    private void updateLists() {
        limudRV.setAdapter(new LimudAdapter(mContext, getLimudList()));
        hillulotRV.setAdapter(new LimudAdapter(mContext, getHillulotList()));
    }

    private List<LimudListEntry> getLimudList() {
        List<LimudListEntry> limudim = new ArrayList<>();
        if (!mCurrentDateShown.before(dafYomiStartDate)) {
            limudim.add(new LimudListEntry(getString(R.string.daf_yomi)  + " " + YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechta()
                    + " " +
                    mHebrewDateFormatter.formatHebrewNumber(YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf())));
        }
        if (!mCurrentDateShown.before(dafYomiYerushalmiStartDate)) {
            Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
            if (dafYomiYerushalmi != null) {
                String masechta = dafYomiYerushalmi.getYerushalmiMasechta();
                String daf = mHebrewDateFormatter.formatHebrewNumber(dafYomiYerushalmi.getDaf());
                limudim.add(new LimudListEntry(getString(R.string.yerushalmi_yomi) + " " + masechta + " " + daf));
            } else {
                limudim.add(new LimudListEntry(getString(R.string.no_daf_yomi_yerushalmi)));
            }
        }
        return limudim;
    }

    private List<LimudListEntry> getHillulotList() {
        List<LimudListEntry> hillulot = new ArrayList<>();

        // Read JSON file from the 'raw' directory
        String jsonFileString = readJSONFromRawResource(mContext,
                Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew") ? R.raw.hiloulah_he : R.raw.hiloulah_en);

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
                        hillulot.add(new LimudListEntry(name,"(" + src + ")"));
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
        Button previousDate = binding.prevDay;
        previousDate.setOnClickListener(v -> {
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();//just get a calendar object with the same date as the current one
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    mCurrentDateShown.add(Calendar.DATE, -7);//subtract seven days
                } else {
                    mCurrentDateShown.add(Calendar.DATE, -1);//subtract one day
                }
                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                setDate();
                updateLists();
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, CalendarDrawable.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
        });
    }

    /**
     * Sets up the next day button
     */
    private void setupNextDayButton() {
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

    /**
     * Setup the calendar button to show a DatePickerDialog with an additional button to switch the calendar to the hebrew one.
     */
    private void setupCalendarButton() {
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

    @Override
    public void onResume() {
        super.onResume();
        setDate();
        initMenu();
        setupRecyclerViews();
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