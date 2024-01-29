package com.ej.rovadiahyosefcalendar.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.CustomDatePickerDialog;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
        setContentView(R.layout.activity_molad);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            mSDF = new SimpleDateFormat("EEE MMM d H:mm:ss", Locale.getDefault());
            mHebrewMonths = new String[]{ "ניסן", "אייר", "סיון", "תמוז", "אב",
                    "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר", "אדר ב", "אדר א"};
        }

        mCurrentMonths = findViewById(R.id.currentMonths);
        mMoladAnnouncementTime = findViewById(R.id.moladAnnouncementTime);
        mMoladDate = findViewById(R.id.moladDate);
        mMoladDate7Days = findViewById(R.id.moladDate7Days);
        mMoladDate15Days = findViewById(R.id.moladDate15Days);

        updateMoladDates();

        Button moladButton = findViewById(R.id.molad_button);
        DatePickerDialog dialog = createDialog();

        moladButton.setOnClickListener(v -> {
            dialog.updateDate(mUserChosenDate.get(Calendar.YEAR),
                    mUserChosenDate.get(Calendar.MONTH),
                    mUserChosenDate.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });


    }

    private DatePickerDialog createDialog() {
        DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
            mUserChosenDate.set(year, month, day);
            mJewishCalendar.setDate(mUserChosenDate);
            updateMoladDates();
        };

        return new CustomDatePickerDialog(this, onDateSetListener,
                mUserChosenDate.get(Calendar.YEAR),
                mUserChosenDate.get(Calendar.MONTH),
                mUserChosenDate.get(Calendar.DAY_OF_MONTH),
                mJewishCalendar);
    }

    @SuppressLint("SetTextI18n")
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

        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}