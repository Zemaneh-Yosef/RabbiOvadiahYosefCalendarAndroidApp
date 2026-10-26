package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;

public class MoladActivity extends AppCompatActivity {

    private TextView mCurrentEnglishMonthYear;
    private TextView mCurrentHebrewMonthYear;
    private TextView mMoladAnnouncementTime;
    private TextView mJerMoladDate;
    private TextView mMoladDate;
    private TextView mMoladDate7Days;
    private final LocalDate mUserChosenDate = LocalDate.now();
    private final JewishCalendar mJewishCalendar = new JewishCalendar();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d H:mm:ss a").withZone(ZoneId.systemDefault());
    private String[] mHebrewMonths = { "Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishri", "Ḥeshvan", "Kislev", "Tevet", "Shevat", "Adar", "Adar II", "Adar I"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_molad);
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        if (Utils.isLocaleHebrew(this)) {
            formatter = DateTimeFormatter.ofPattern("EEE MMM d H:mm:ss a")
                    .withZone(ZoneId.systemDefault());
            mHebrewMonths = new String[]{ "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר", "אדר ב", "אדר א" };
        }

        mCurrentEnglishMonthYear = findViewById(R.id.currentEnglishMonthYear);
        ImageButton moladButton = findViewById(R.id.molad_button);
        mCurrentHebrewMonthYear = findViewById(R.id.currentHebrewMonthYear);
        mMoladAnnouncementTime = findViewById(R.id.moladAnnouncementTime);
        mJerMoladDate = findViewById(R.id.moladDateJer);
        mMoladDate = findViewById(R.id.moladDate);
        mMoladDate7Days = findViewById(R.id.moladDate7Days);

        updateMoladDates();

        View.OnClickListener onClickListener = v -> {
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
            MaterialDatePicker<Long> materialDatePicker = builder
                    .setPositiveButtonText(R.string.ok)
                    .setNegativeButtonText(R.string.switch_calendar)
                    .setSelection(mUserChosenDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                    .build();
            materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                sCurrentDateShown = Instant.ofEpochMilli(selection)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();
                mJewishCalendar.setGregorianDate(mUserChosenDate);
                MoladActivity.this.updateMoladDates();
            });
            DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                mJewishCalendar.setGregorianDate(LocalDate.of(year, month, day));
                MoladActivity.this.updateMoladDates();
            };
            materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, MoladActivity.this.getSupportFragmentManager(), mJewishCalendar);
                hdmypd.updateDate(mJewishCalendar.getLocalDate());
                hdmypd.setListener(onDateSetListener);
                hdmypd.show(MoladActivity.this.getSupportFragmentManager(), null);
            });
            materialDatePicker.show(MoladActivity.this.getSupportFragmentManager(), null);
        };

        mCurrentEnglishMonthYear.setOnClickListener(onClickListener);
        moladButton.setOnClickListener(onClickListener);
        mCurrentHebrewMonthYear.setOnClickListener(onClickListener);

        TextView disclaimer = findViewById(R.id.molad_disclaimer);
        if (disclaimer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(disclaimer, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.leftMargin = insets.left;
                mlp.bottomMargin = insets.bottom;
                mlp.rightMargin = insets.right;
                v.setLayoutParams(mlp);
                // Return CONSUMED if you don't want the window insets to keep passing
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
        String currentEnglishMonthYear = mJewishCalendar.getLocalDate().getMonth().getDisplayName(TextStyle.FULL, getResources()
                .getConfiguration()
                .getLocales()
                .get(0)) + "\n"
                + mJewishCalendar.getLocalDate().getYear();
        mCurrentEnglishMonthYear.setText(currentEnglishMonthYear);

        String currentHebrewMonthYear = currentHebrewMonth + "\n" + mJewishCalendar.getJewishYear();
        mCurrentHebrewMonthYear.setText(currentHebrewMonthYear);

        JewishDate molad = mJewishCalendar.getMolad();
        int moladHours = molad.getMoladHours();
        int moladMinutes = molad.getMoladMinutes();
        int moladChalakim = molad.getMoladChalakim();

        String moladTime = moladHours + getString(R.string.h) + moladMinutes + getString(R.string.m_and) + " " + moladChalakim + " " + getString(R.string.chalakim);

        if (Utils.isLocaleHebrew(this)) {
            moladTime = moladMinutes + " : " + moladHours + " \n" + moladChalakim + " " + getString(R.string.chalakim);
        }

        mMoladAnnouncementTime.setText(moladTime);
        mJerMoladDate.setText(formatter.withZone(ZoneId.of("Asia/Jerusalem")).format(mJewishCalendar.getMoladAsInstant()));
        mMoladDate.setText(formatter.withZone(ZoneId.systemDefault()).format(mJewishCalendar.getMoladAsInstant()));
        mMoladDate7Days.setText(formatter.withZone(ZoneId.systemDefault()).format(mJewishCalendar.getTchilasZmanKidushLevana7Days()));
    }
}