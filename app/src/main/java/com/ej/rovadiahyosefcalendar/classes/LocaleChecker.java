package com.ej.rovadiahyosefcalendar.classes;

import java.util.Locale;

public class LocaleChecker {
    public static boolean isLocaleHebrew() {
        return Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew");
    }
}
