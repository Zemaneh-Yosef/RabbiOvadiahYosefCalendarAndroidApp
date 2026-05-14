package com.EJ.ROvadiahYosefCalendar.classes;

import android.content.Context;

import com.EJ.ROvadiahYosefCalendar.R;

import java.util.Date;

/**
 * This class contains static methods that are used multiple times
 */
public class Utils {

    public static boolean isLocaleHebrew(Context context) {
        if (context == null) {
            return false;
        }
        return context.getString(R.string.locale).equalsIgnoreCase("Hebrew");// my own implementation because context.getResources().getConfiguration().getLocales().get(0) is not reliable enough, but the strings are always working
    }

    /**
     * This is a simple convenience method to add a minute to a date object. If the date is not null,
     * it will return the same date with a minute added to it. Otherwise, if the date is null, it will return null.
     * It is important NOT to use the Calendar class because it takes into account the time zones and leap seconds. If you would like to
     * simply add a minute to a date, use this method.
     * @param date the date object to add a minute to
     * @return the given date a minute ahead if not null
     */
    public static Date addMinuteToZman(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() + 60_000);
    }

    public static String removePostalCode(String input) {
        if (input == null) return null;
        return input.replaceAll("\\s*\\([^)]*\\)$", "").trim();// Removes ANY trailing " (something)"
    }
}
