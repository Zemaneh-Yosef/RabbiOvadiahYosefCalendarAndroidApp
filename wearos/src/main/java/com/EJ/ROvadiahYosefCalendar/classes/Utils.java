package com.EJ.ROvadiahYosefCalendar.classes;

import java.util.Locale;

/**
 * This class contains static methods that are used multiple times
 */
public class Utils {

    public static boolean isLocaleHebrew() {
        return Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew");
    }

}
