package com.ej.rovadiahyosefcalendar.classes;

import android.content.SharedPreferences;

import com.ej.rovadiahyosefcalendar.R;

import java.util.Calendar;

public class CalendarDrawable {

    /**
     * Returns the current calendar drawable depending on the current day of the month.
     */
    public static int getCurrentCalendarDrawable(SharedPreferences mSettingsPreferences, Calendar calendar) {
        if (!mSettingsPreferences.getBoolean("useDarkCalendarIcon", false)) {
            return getCurrentCalendarDrawableLight(calendar);
        } else {
            return getCurrentCalendarDrawableDark(calendar);
        }
    }

    public static int getCurrentCalendarDrawableLight(Calendar calendar) {
        switch (calendar.get(Calendar.DATE)) {
            case (1):
                return R.drawable.calendar1;
            case (2):
                return R.drawable.calendar2;
            case (3):
                return R.drawable.calendar3;
            case (4):
                return R.drawable.calendar4;
            case (5):
                return R.drawable.calendar5;
            case (6):
                return R.drawable.calendar6;
            case (7):
                return R.drawable.calendar7;
            case (8):
                return R.drawable.calendar8;
            case (9):
                return R.drawable.calendar9;
            case (10):
                return R.drawable.calendar10;
            case (11):
                return R.drawable.calendar11;
            case (12):
                return R.drawable.calendar12;
            case (13):
                return R.drawable.calendar13;
            case (14):
                return R.drawable.calendar14;
            case (15):
                return R.drawable.calendar15;
            case (16):
                return R.drawable.calendar16;
            case (17):
                return R.drawable.calendar17;
            case (18):
                return R.drawable.calendar18;
            case (19):
                return R.drawable.calendar19;
            case (20):
                return R.drawable.calendar20;
            case (21):
                return R.drawable.calendar21;
            case (22):
                return R.drawable.calendar22;
            case (23):
                return R.drawable.calendar23;
            case (24):
                return R.drawable.calendar24;
            case (25):
                return R.drawable.calendar25;
            case (26):
                return R.drawable.calendar26;
            case (27):
                return R.drawable.calendar27;
            case (28):
                return R.drawable.calendar28;
            case (29):
                return R.drawable.calendar29;
            case (30):
                return R.drawable.calendar30;
            default:
                return R.drawable.calendar31;
        }
    }

    public static int getCurrentCalendarDrawableDark(Calendar calendar) {
        switch (calendar.get(Calendar.DATE)) {
            case (1):
                return R.drawable.calendar_1_dark;
            case (2):
                return R.drawable.calendar_2_dark;
            case (3):
                return R.drawable.calendar_3_dark;
            case (4):
                return R.drawable.calendar_4_dark;
            case (5):
                return R.drawable.calendar_5_dark;
            case (6):
                return R.drawable.calendar_6_dark;
            case (7):
                return R.drawable.calendar_7_dark;
            case (8):
                return R.drawable.calendar_8_dark;
            case (9):
                return R.drawable.calendar_9_dark;
            case (10):
                return R.drawable.calendar_10_dark;
            case (11):
                return R.drawable.calendar_11_dark;
            case (12):
                return R.drawable.calendar_12_dark;
            case (13):
                return R.drawable.calendar_13_dark;
            case (14):
                return R.drawable.calendar_14_dark;
            case (15):
                return R.drawable.calendar_15_dark;
            case (16):
                return R.drawable.calendar_16_dark;
            case (17):
                return R.drawable.calendar_17_dark;
            case (18):
                return R.drawable.calendar_18_dark;
            case (19):
                return R.drawable.calendar_19_dark;
            case (20):
                return R.drawable.calendar_20_dark;
            case (21):
                return R.drawable.calendar_21_dark;
            case (22):
                return R.drawable.calendar_22_dark;
            case (23):
                return R.drawable.calendar_23_dark;
            case (24):
                return R.drawable.calendar_24_dark;
            case (25):
                return R.drawable.calendar_25_dark;
            case (26):
                return R.drawable.calendar_26_dark;
            case (27):
                return R.drawable.calendar_27_dark;
            case (28):
                return R.drawable.calendar_28_dark;
            case (29):
                return R.drawable.calendar_29_dark;
            case (30):
                return R.drawable.calendar_30_dark;
            default:
                return R.drawable.calendar_31_dark;
        }
    }
}
