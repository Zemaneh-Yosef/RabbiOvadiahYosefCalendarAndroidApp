package com.EJ.ROvadiahYosefCalendar.classes;

import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ADAR;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ADAR_II;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.CHESHVAN;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.ELUL;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.IYAR;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.KISLEV;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.TAMMUZ;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.TEVES;
import static com.kosherjava.zmanim.hebrewcalendar.JewishDate.getDaysInJewishYear;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.EJ.ROvadiahYosefCalendar.R;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Locale;

public class HebrewDatePickerDialog extends Dialog {

    private final NumberPicker mMonthPicker;
    private final NumberPicker mDayPicker;
    private final JewishCalendar mJewishCalendar;
    private final NumberPicker mYearPicker;
    DatePickerDialog.OnDateSetListener mListener;
    private String[] mHebrewMonths = {"Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishri", "Ḥeshvan", "Kislev", "Tevet", "Shevat", "Adar"};
    private String[] mHebrewMonthsLeap = {"Nissan", "Iyar", "Sivan", "Tammuz", "Av",
            "Elul", "Tishri", "Ḥeshvan", "Kislev", "Tevet", "Shevat", "Adar I", "Adar II"};

    public HebrewDatePickerDialog(Activity activity, Context context,
                                  DatePickerDialog.OnDateSetListener listener,
                                  JewishCalendar jewishCalendar,
                                  View.OnClickListener okOnClick,
                                  View.OnClickListener switchOnClick,
                                  View.OnClickListener cancelOnClick) {
        super(context, 0);
        mListener = listener;
        if (Locale.getDefault().getDisplayLanguage(new Locale.Builder().setLanguage("en").setRegion("US").build()).equals("Hebrew")) {
            mHebrewMonths = new String[]{"ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר"};
            mHebrewMonthsLeap = new String[]{"ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר ב׳"};
        }
        View pickers = activity.getLayoutInflater().inflate(R.layout.hebrew_date_picker, null);

        mDayPicker = pickers.findViewById(R.id.picker_day);
        mDayPicker.setMinValue(1);
        mJewishCalendar = jewishCalendar;
        int month = mJewishCalendar.getJewishMonth();
        if ((month == IYAR) || (month == TAMMUZ) || (month == ELUL) || ((month == CHESHVAN) && !(isCheshvanLong(jewishCalendar.getJewishYear())))
                || ((month == KISLEV) && isKislevShort(jewishCalendar.getJewishYear())) || (month == TEVES)
                || ((month == ADAR) && !(isJewishLeapYear(jewishCalendar.getJewishYear()))) || (month == ADAR_II)) {
            mDayPicker.setMaxValue(29);
        } else {
            mDayPicker.setMaxValue(30);
        }
        mDayPicker.setValue(jewishCalendar.getJewishDayOfMonth());

        mMonthPicker = pickers.findViewById(R.id.picker_month);
        mMonthPicker.setMinValue(JewishCalendar.NISSAN);
        if (isJewishLeapYear(jewishCalendar.getJewishYear())) {
            mMonthPicker.setMaxValue(ADAR_II);
        } else {
            mMonthPicker.setMaxValue(ADAR);
        }
        if (isJewishLeapYear(jewishCalendar.getJewishYear())) {
            mMonthPicker.setDisplayedValues(mHebrewMonthsLeap);
        } else {
            mMonthPicker.setDisplayedValues(mHebrewMonths);
        }
        mMonthPicker.setValue(jewishCalendar.getJewishMonth());

        mYearPicker = pickers.findViewById(R.id.picker_year);
        mYearPicker.setMinValue(jewishCalendar.getJewishYear() - 100);
        mYearPicker.setMaxValue(jewishCalendar.getJewishYear() + 100);
        mYearPicker.setValue(jewishCalendar.getJewishYear());

        Button ok = pickers.findViewById(R.id.OK);
        ok.setOnClickListener(l -> {
            dismiss();
            listener.onDateSet(null, mJewishCalendar.getGregorianYear(), mJewishCalendar.getGregorianMonth(), mJewishCalendar.getGregorianDayOfMonth());
            okOnClick.onClick(ok);
        });
        Button switch_calendars = pickers.findViewById(R.id.Switch_Calendar);
        switch_calendars.setOnClickListener(l -> {
            dismiss();
            switchOnClick.onClick(switch_calendars);
        });
        Button cancel = pickers.findViewById(R.id.Cancel);
        cancel.setOnClickListener(l -> {
            dismiss();
            cancelOnClick.onClick(cancel);
        });

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

        setContentView(pickers);
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
}
