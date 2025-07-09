package com.EJ.ROvadiahYosefCalendar.classes;

import java.util.Date;
import java.util.Locale;

/**
 * This class contains static methods that are used multiple times
 */
public class Utils {

    public static boolean isLocaleHebrew() {
        return Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew");
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

}
