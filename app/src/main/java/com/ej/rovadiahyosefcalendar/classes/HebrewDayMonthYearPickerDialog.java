package com.ej.rovadiahyosefcalendar.classes;

import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ADAR;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ADAR_II;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.CHESHVAN;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ELUL;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.IYAR;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.KISLEV;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.NISSAN;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.TAMMUZ;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.TEVES;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.getDaysInJewishYear;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Locale;
import java.util.Objects;

public class HebrewDayMonthYearPickerDialog extends DialogFragment {

    private final int MIN_YEAR;
    private final int MAX_YEAR;
    private DatePickerDialog.OnDateSetListener listener;
    private final MaterialDatePicker<Long> materialDatePicker;
    private final FragmentManager fragmentManager;
    private final JewishCalendar mJewishCalendar;
    private String[] mHebrewMonths = {"Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishri", "Ḥeshvan", "Kislev", "Tevet", "Shevat", "Adar"};
    private String[] mHebrewMonthsLeap = {"Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishri", "Ḥeshvan", "Kislev", "Tevet", "Shevat", "Adar I", "Adar II"};
    private NumberPicker mDayPicker;
    private NumberPicker mMonthPicker;
    private NumberPicker mYearPicker;

    public HebrewDayMonthYearPickerDialog(MaterialDatePicker<Long> materialDatePicker, FragmentManager fragmentManager, JewishCalendar jewishCalendar) {
        super();
        this.materialDatePicker = materialDatePicker;
        this.fragmentManager = fragmentManager;
        mJewishCalendar = jewishCalendar;
        MIN_YEAR = jewishCalendar.getJewishYear() - 100;
        MAX_YEAR = jewishCalendar.getJewishYear() + 100;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
            mHebrewMonths = new String[]{"ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר"};
            mHebrewMonthsLeap = new String[]{"ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר ב׳"};
        }
    }

    public void setListener(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();// Get the layout inflater

        View dialog = inflater.inflate(R.layout.hebrew_date_picker_dialog, null);
        mDayPicker = dialog.findViewById(R.id.picker_day);
        mMonthPicker = dialog.findViewById(R.id.picker_month);
        mYearPicker = dialog.findViewById(R.id.picker_year);

        mDayPicker.setMinValue(1);
        int month = mJewishCalendar.getJewishMonth();
        if ((month == IYAR) || (month == TAMMUZ) || (month == ELUL) || ((month == CHESHVAN) && !(isCheshvanLong(mJewishCalendar.getJewishYear())))
                || ((month == KISLEV) && isKislevShort(mJewishCalendar.getJewishYear())) || (month == TEVES)
                || ((month == ADAR) && !(isJewishLeapYear(mJewishCalendar.getJewishYear()))) || (month == ADAR_II)) {
            mDayPicker.setMaxValue(29);
        } else {
            mDayPicker.setMaxValue(30);
        }
        mDayPicker.setValue(mJewishCalendar.getJewishDayOfMonth());

        mMonthPicker.setMinValue(NISSAN);
        if (isJewishLeapYear(mJewishCalendar.getJewishYear())) {
            mMonthPicker.setMaxValue(ADAR_II);
        } else {
            mMonthPicker.setMaxValue(ADAR);
        }
        mMonthPicker.setValue(mJewishCalendar.getJewishMonth());
        if (isJewishLeapYear(mJewishCalendar.getJewishYear())) {
            mMonthPicker.setDisplayedValues(mHebrewMonthsLeap);
        } else {
            mMonthPicker.setDisplayedValues(mHebrewMonths);
        }

        mYearPicker.setMinValue(MIN_YEAR);
        mYearPicker.setMaxValue(MAX_YEAR);
        mYearPicker.setValue(mJewishCalendar.getJewishYear());

        NumberPicker.OnValueChangeListener onValueChangeListener = (picker, oldVal, newVal) -> {
            int monthPickerValue = mMonthPicker.getValue();
            if (!isJewishLeapYear(mYearPicker.getValue())) {
                if (mMonthPicker.getValue() == 13) {
                    monthPickerValue = 12;
                }
            }
            int dayPickerValue = mDayPicker.getValue();
            if (dayPickerValue == 30) {
                if ((monthPickerValue == IYAR) || (monthPickerValue == TAMMUZ) || (monthPickerValue == ELUL) || ((monthPickerValue == CHESHVAN) &&
                        !(isCheshvanLong(mJewishCalendar.getJewishYear())))
                        || ((monthPickerValue == KISLEV) && isKislevShort(mJewishCalendar.getJewishYear())) || (monthPickerValue == TEVES)
                        || ((monthPickerValue == ADAR) && !(isJewishLeapYear(mJewishCalendar.getJewishYear()))) || (monthPickerValue == ADAR_II)) {
                    dayPickerValue = 29;
                }
            }
            mJewishCalendar.setJewishDate(mYearPicker.getValue(), monthPickerValue, dayPickerValue);
            updateValues();
        };

        mDayPicker.setOnValueChangedListener(onValueChangeListener);
        mMonthPicker.setOnValueChangedListener(onValueChangeListener);
        mYearPicker.setOnValueChangedListener(onValueChangeListener);

        builder.setView(dialog)
                .setPositiveButton(R.string.ok, (dialog1, id) -> {
                    mJewishCalendar.setJewishDate(mYearPicker.getValue(), mMonthPicker.getValue(), mDayPicker.getValue());
                    listener.onDateSet(null,
                            mJewishCalendar.getGregorianYear(), mJewishCalendar.getGregorianMonth(), mJewishCalendar.getGregorianDayOfMonth());
                })
                .setNegativeButton(requireContext().getString(R.string.cancel), (dialog2, id) -> Objects.requireNonNull(HebrewDayMonthYearPickerDialog.this.getDialog()).cancel())
                .setNeutralButton(requireContext().getString(R.string.switch_calendar), (dialog3, which) -> {
                    materialDatePicker.show(fragmentManager, null);
                });
        return builder.create();
    }

    private static boolean isJewishLeapYear(int year) {
        return ((7 * year) + 1) % 19 < 7;
    }

    private static boolean isCheshvanLong(int year) {
        return getDaysInJewishYear(year) % 10 == 5;
    }

    private static boolean isKislevShort(int year) {
        return getDaysInJewishYear(year) % 10 == 3;
    }

    private void updateValues() {
        int month = mJewishCalendar.getJewishMonth();
        if ((month == IYAR) || (month == TAMMUZ) || (month == ELUL) || ((month == CHESHVAN) && !(isCheshvanLong(mJewishCalendar.getJewishYear())))
                || ((month == KISLEV) && isKislevShort(mJewishCalendar.getJewishYear())) || (month == TEVES)
                || ((month == ADAR) && !(isJewishLeapYear(mJewishCalendar.getJewishYear()))) || (month == ADAR_II)) {
            mDayPicker.setMaxValue(29);
        } else {
            mDayPicker.setMaxValue(30);
        }
        mMonthPicker.setDisplayedValues(new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""});//reset the values
        if (isJewishLeapYear(mJewishCalendar.getJewishYear())) {
            mMonthPicker.setMaxValue(ADAR_II);
        } else {
            mMonthPicker.setMaxValue(ADAR);
        }
        if (isJewishLeapYear(mJewishCalendar.getJewishYear())) {
            mMonthPicker.setDisplayedValues(mHebrewMonthsLeap);
        } else {
            mMonthPicker.setDisplayedValues(mHebrewMonths);
        }
    }

    public void updateDate(int year, int month, int dayOfMonth) {
        mJewishCalendar.setGregorianDate(year, month, dayOfMonth);
    }
}
