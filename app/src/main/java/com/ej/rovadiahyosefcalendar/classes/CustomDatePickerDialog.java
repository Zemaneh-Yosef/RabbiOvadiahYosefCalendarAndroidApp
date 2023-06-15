package com.ej.rovadiahyosefcalendar.classes;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

public class CustomDatePickerDialog extends DatePickerDialog {

    private final Context mContext;
    private final OnDateSetListener mListener;
    private final JewishCalendar mJewishCalendar;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CustomDatePickerDialog(@NonNull Context context,
                                  OnDateSetListener listener,
                                  int year,
                                  int month,
                                  int dayOfMonth, JewishCalendar jewishCalendar) {
        super(context, listener, year, month, dayOfMonth);
        mContext = context;
        mListener = listener;
        mJewishCalendar = jewishCalendar;
        setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.switch_calendar), this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == BUTTON_NEUTRAL) {//if we want to change calendars
            HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(this, mJewishCalendar);
            hdmypd.updateDate(getDatePicker().getYear(), getDatePicker().getMonth(), getDatePicker().getDayOfMonth());//keep both calendars up to date
            hdmypd.setListener(mListener);
            final FragmentActivity activity = (FragmentActivity) mContext;
            hdmypd.show(activity.getSupportFragmentManager(), null);
        }
    }
}

