package com.ej.rovadiahyosefcalendar.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MoladActivity extends AppCompatActivity {

    private TextView mCurrentMonths;
    private TextView mMoladAnnouncementTime;
    private TextView mMoladDate;
    private TextView mMoladDate7Days;
    private TextView mMoladDate15Days;
    private final Calendar mUserChosenDate = Calendar.getInstance();
    private final JewishCalendar mJewishCalendar = new JewishCalendar();
    private SimpleDateFormat mSDF = new SimpleDateFormat("EEE MMM d h:mm:ss aa", Locale.getDefault());
    private String[] mHebrewMonths = { "Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishri", "Cheshvan", "Kislev", "Tevet", "Shevat", "Adar", "Adar II", "Adar I"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_molad);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (Utils.isLocaleHebrew()) {
            mSDF = new SimpleDateFormat("EEE MMM d H:mm:ss", Locale.getDefault());
            mHebrewMonths = new String[]{ "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר", "אדר ב", "אדר א"};
        }

        mCurrentMonths = findViewById(R.id.currentMonths);
        mMoladAnnouncementTime = findViewById(R.id.moladAnnouncementTime);
        mMoladDate = findViewById(R.id.moladDate);
        mMoladDate7Days = findViewById(R.id.moladDate7Days);
        mMoladDate15Days = findViewById(R.id.moladDate15Days);

        updateMoladDates();

        Button moladButton = findViewById(R.id.molad_button);

        moladButton.setOnClickListener(v -> {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                MaterialDatePicker<Long> materialDatePicker = builder
                        .setPositiveButtonText(R.string.ok)
                        .setNegativeButtonText(R.string.switch_calendar)
                        .setSelection(mUserChosenDate.getTimeInMillis())// can be in local timezone
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                    Calendar epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    epoch.setTimeInMillis(selection);
                    mUserChosenDate.set(
                            epoch.get(Calendar.YEAR),
                            epoch.get(Calendar.MONTH),
                            epoch.get(Calendar.DATE),
                            epoch.get(Calendar.HOUR_OF_DAY),
                            epoch.get(Calendar.MINUTE)
                    );
                    mJewishCalendar.setDate(mUserChosenDate);
                    updateMoladDates();
                });
                DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                    Calendar mUserChosenDate = Calendar.getInstance();
                    mUserChosenDate.set(year, month, day);
                    mJewishCalendar.setDate(mUserChosenDate);
                    updateMoladDates();
                };
                materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                    HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, getSupportFragmentManager(), mJewishCalendar);
                    hdmypd.updateDate(mJewishCalendar.getGregorianYear(),
                            mJewishCalendar.getGregorianMonth(),
                            mJewishCalendar.getGregorianDayOfMonth());
                    hdmypd.setListener(onDateSetListener);
                    hdmypd.show(getSupportFragmentManager(), null);
                });
                materialDatePicker.show(getSupportFragmentManager(), null);
        });

        TextView disclaimer = findViewById(R.id.molad_disclaimer);
        if (disclaimer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(disclaimer, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.leftMargin = insets.left;
                mlp.bottomMargin = insets.bottom;
                mlp.rightMargin = insets.right;
                v.setLayoutParams(mlp);
                // Return CONSUMED if you don't want want the window insets to keep passing
                // down to descendant views.
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    private void updateMoladDates() {
        String currentHebrewMonth;
        if (mJewishCalendar.isJewishLeapYear() && mJewishCalendar.getJewishMonth() == JewishDate.ADAR) {
            currentHebrewMonth = mHebrewMonths[13]; // return Adar I, not Adar in a leap year
        } else {
            currentHebrewMonth = mHebrewMonths[mJewishCalendar.getJewishMonth() - 1];
        }
        String currentMonths = mJewishCalendar.getGregorianCalendar().getDisplayName(
                Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " "
                + mJewishCalendar.getGregorianYear()
                + " / "
                + currentHebrewMonth + " " + mJewishCalendar.getJewishYear();
        mCurrentMonths.setText(currentMonths);

        JewishDate molad = mJewishCalendar.getMolad();
        int moladHours = molad.getMoladHours();
        int moladMinutes = molad.getMoladMinutes();
        int moladChalakim = molad.getMoladChalakim();

        String moladTime = moladHours + getString(R.string.h) + moladMinutes + getString(R.string.m_and) + " " + moladChalakim + " " + getString(R.string.chalakim);

        if (Utils.isLocaleHebrew()) {
            TextView m = findViewById(R.id.moladAnnouncement);
            m.setGravity(Gravity.END);
            mMoladAnnouncementTime.setGravity(Gravity.END);
            moladTime = moladMinutes + " : " + moladHours + " \n" + moladChalakim + " " + getString(R.string.chalakim);
        }

        mMoladAnnouncementTime.setText(moladTime);
        mMoladDate.setText(mSDF.format(mJewishCalendar.getMoladAsDate()));
        mMoladDate7Days.setText(mSDF.format(mJewishCalendar.getTchilasZmanKidushLevana7Days()));
        mMoladDate15Days.setText(R.string.the_whole_night_of_the_15th_day_of_the_hebrew_month);
    }
}