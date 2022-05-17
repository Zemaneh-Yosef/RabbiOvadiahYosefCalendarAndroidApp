package com.ej.rovadiahyosefcalendar.classes;

import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ADAR;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ADAR_II;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.CHESHVAN;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ELUL;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.IYAR;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.KISLEV;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.ej.rovadiahyosefcalendar.R;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Objects;

public class HebrewDayMonthYearPickerDialog extends DialogFragment {

    private final int MIN_YEAR;
    private final int MAX_YEAR;
    private DatePickerDialog.OnDateSetListener listener;
    private final CustomDatePickerDialog mCustomDatePickerDialog;
    private final JewishCalendar mJewishCalendar;
    private final String[] mHebrewMonths = {"Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishrei", "Cheshvan", "Kislev", "Tevet", "Shevat", "Adar"};
    private final String[] mHebrewMonthsLeap = {"Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishrei", "Cheshvan", "Kislev", "Tevet", "Shevat", "Adar I", "Adar II"};
    private NumberPicker mDayPicker;
    private NumberPicker mMonthPicker;

    public HebrewDayMonthYearPickerDialog(CustomDatePickerDialog customDatePickerDialog, JewishCalendar jewishCalendar) {
        super();
        mCustomDatePickerDialog = customDatePickerDialog;
        mJewishCalendar = jewishCalendar;
        MIN_YEAR = jewishCalendar.getJewishYear() - 100;
        MAX_YEAR = jewishCalendar.getJewishYear() + 100;
    }

    public void setListener(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();// Get the layout inflater

        View dialog = inflater.inflate(R.layout.hebrew_date_picker_dialog, null);
        mDayPicker = dialog.findViewById(R.id.picker_day);
        mMonthPicker = dialog.findViewById(R.id.picker_month);
        final NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);

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

        mMonthPicker.setMinValue(1);
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

        yearPicker.setMinValue(MIN_YEAR);
        yearPicker.setMaxValue(MAX_YEAR);
        yearPicker.setValue(mJewishCalendar.getJewishYear());

        NumberPicker.OnValueChangeListener onValueChangeListener = (picker, oldVal, newVal) -> {
            int monthPickerValue = mMonthPicker.getValue();
            if (!isJewishLeapYear(yearPicker.getValue())) {
                if (mMonthPicker.getValue() == 13) {
                    monthPickerValue = 12;
                }
            }
            int dayPickerValue = mDayPicker.getValue();
            if ((monthPickerValue == IYAR) || (monthPickerValue == TAMMUZ) || (monthPickerValue == ELUL) || ((monthPickerValue == CHESHVAN) &&
                    !(isCheshvanLong(mJewishCalendar.getJewishYear())))
                    || ((monthPickerValue == KISLEV) && isKislevShort(mJewishCalendar.getJewishYear())) || (monthPickerValue == TEVES)
                    || ((monthPickerValue == ADAR) && !(isJewishLeapYear(mJewishCalendar.getJewishYear()))) || (monthPickerValue == ADAR_II)) {
                dayPickerValue = 29;
            } else {
                dayPickerValue = 30;
            }
            mJewishCalendar.setJewishDate(yearPicker.getValue(), monthPickerValue, dayPickerValue);
            updateValues();
        };

        mDayPicker.setOnValueChangedListener(onValueChangeListener);
        mMonthPicker.setOnValueChangedListener(onValueChangeListener);
        yearPicker.setOnValueChangedListener(onValueChangeListener);

        builder.setView(dialog)
                .setPositiveButton(R.string.ok, (dialog1, id) -> {
                    mJewishCalendar.setJewishDate(yearPicker.getValue(), mMonthPicker.getValue(), mDayPicker.getValue());
                    listener.onDateSet(null,
                            mJewishCalendar.getGregorianYear(), mJewishCalendar.getGregorianMonth(), mJewishCalendar.getGregorianDayOfMonth());
                })
                .setNegativeButton("Cancel", (dialog2, id) -> Objects.requireNonNull(HebrewDayMonthYearPickerDialog.this.getDialog()).cancel())
                .setNeutralButton("                Switch Calendar", (dialog3, which) -> {
                    mCustomDatePickerDialog.create();
                    mCustomDatePickerDialog.show();
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
