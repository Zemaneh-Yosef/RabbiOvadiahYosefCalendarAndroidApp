package com.EJ.ROvadiahYosefCalendar.classes;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.EJ.ROvadiahYosefCalendar.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * A dialog that prompts the user to select a date on the Gregorian calendar.
 */
public class EnglishDatePickerDialog extends Dialog {

    private final NumberPicker mMonthPicker;
    private final NumberPicker mDayPicker;
    private final NumberPicker mYearPicker;
    private final Calendar mCalendar;

    public EnglishDatePickerDialog(Activity activity, Context context,
                                   DatePickerDialog.OnDateSetListener listener,
                                   Calendar initialDate,
                                   View.OnClickListener okOnClick,
                                   View.OnClickListener switchOnClick,
                                   View.OnClickListener cancelOnClick) {
        super(context, 0);

        // Get localized month names (January, February...)
        Locale locale = Utils.isLocaleHebrew(context) ? new Locale("he") : Locale.getDefault();
        String[] monthNames = new DateFormatSymbols(locale).getShortMonths();

        // Use a copy of the calendar to track state within the picker
        mCalendar = (Calendar) initialDate.clone();

        View pickers = activity.getLayoutInflater().inflate(R.layout.hebrew_date_picker, null);

        // Year Picker Setup
        mYearPicker = pickers.findViewById(R.id.picker_year);
        mYearPicker.setMinValue(mCalendar.get(Calendar.YEAR) - 100);
        mYearPicker.setMaxValue(mCalendar.get(Calendar.YEAR) + 100);
        mYearPicker.setValue(mCalendar.get(Calendar.YEAR));

        // Month Picker Setup
        mMonthPicker = pickers.findViewById(R.id.picker_month);
        mMonthPicker.setMinValue(0);
        mMonthPicker.setMaxValue(11);
        mMonthPicker.setDisplayedValues(monthNames);
        mMonthPicker.setValue(mCalendar.get(Calendar.MONTH));

        // Day Picker Setup
        mDayPicker = pickers.findViewById(R.id.picker_day);
        mDayPicker.setMinValue(1);
        updateMaxDay(); // Set max days based on current month/year
        mDayPicker.setValue(mCalendar.get(Calendar.DAY_OF_MONTH));

        // Buttons Setup
        Button ok = pickers.findViewById(R.id.OK);
        ok.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onDateSet(null,
                        mCalendar.get(Calendar.YEAR),
                        mCalendar.get(Calendar.MONTH),
                        mCalendar.get(Calendar.DAY_OF_MONTH));
            }
            okOnClick.onClick(ok);
        });

        Button switch_calendars = pickers.findViewById(R.id.Switch_Calendar);
        switch_calendars.setOnClickListener(v -> {
            dismiss();
            switchOnClick.onClick(switch_calendars);
        });

        Button cancel = pickers.findViewById(R.id.Cancel);
        cancel.setOnClickListener(v -> {
            dismiss();
            cancelOnClick.onClick(cancel);
        });

        // Value Change Listener
        NumberPicker.OnValueChangeListener onValueChangeListener = (picker, oldVal, newVal) -> {
            mCalendar.set(Calendar.YEAR, mYearPicker.getValue());
            mCalendar.set(Calendar.MONTH, mMonthPicker.getValue());

            // Adjust max day first (e.g. if switching from Jan 31 to Feb)
            updateMaxDay();

            mCalendar.set(Calendar.DAY_OF_MONTH, mDayPicker.getValue());
        };

        mDayPicker.setOnValueChangedListener(onValueChangeListener);
        mMonthPicker.setOnValueChangedListener(onValueChangeListener);
        mYearPicker.setOnValueChangedListener(onValueChangeListener);

        setContentView(pickers);
    }

    /**
     * Updates the Day NumberPicker's maximum value based on the currently
     * selected month and year in mCalendar.
     */
    private void updateMaxDay() {
        int maxDays = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        mDayPicker.setMaxValue(maxDays);

        // If the current value is now higher than the max (e.g. was 31, now 28)
        if (mDayPicker.getValue() > maxDays) {
            mDayPicker.setValue(maxDays);
        }
    }
}