package com.EJ.ROvadiahYosefCalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.EJ.ROvadiahYosefCalendar.R;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    public static String dateFormatPattern(Context context, boolean showSeconds) {
        return (Utils.isLocaleHebrew(context) ? "H" : "h")
                + ":mm"
                + (showSeconds ? ":ss" : "")
                + (Utils.isLocaleHebrew(context) ? "" : " a");
    }

    public static String formatZmanTime(Context context, ZmanListEntry zmanListEntry) {
        return formatZmanTime(context, zmanListEntry.getZman(), zmanListEntry.getSecondTreatment());
    }

    /**
     * Formats the zman time to a string formatted this: "12:30 AM" or "5:30 PM".
     * If the context is Hebrew, the time will be in military time, I.E. "0:30" or "17:30".
     * Note that this method does not handle null dates, it should be checked before calling this method.
     *
     * @param context the app context to use
     * @param zman the Date to format
     * @param secondTreatment how to handle seconds
     * @return the formatted time as a string
     */
    public static String formatZmanTime(Context context, Instant zman, SecondTreatment secondTreatment) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        boolean showSeconds = sharedPreferences.getBoolean("ShowSeconds", false);
        ZoneId timezone = LocationResolver.getLastGeoLocation(sharedPreferences).getZoneId();
        Locale contextLocale = context
                .getResources()
                .getConfiguration()
                .getLocales()
                .get(0);
        DateTimeFormatter noSecondDateFormat = DateTimeFormatter.ofPattern(dateFormatPattern(context,false))
                .withLocale(contextLocale)
                .withZone(timezone);
        DateTimeFormatter yesSecondDateFormat = DateTimeFormatter.ofPattern(dateFormatPattern(context,true))
                .withLocale(contextLocale)
                .withZone(timezone);

        String zmanTime;
        if (secondTreatment == SecondTreatment.ALWAYS_DISPLAY || showSeconds) {
            zmanTime = yesSecondDateFormat.format(zman);
        } else {
            Instant zmanInstant = zman;
            if ((zman.getEpochSecond() > 40) || (zman.getEpochSecond() > 20 && secondTreatment == SecondTreatment.ROUND_LATER)) {
                zmanInstant = Utils.addMinuteToZman(zman);
            }

            zmanTime = noSecondDateFormat.format(zmanInstant);
        }
        return zmanTime;
    }

    /**
     * This is a simple convenience method to add a minute to a date object. If the date is not null,
     * it will return the same date with a minute added to it. Otherwise, if the date is null, it will return null.
     * It is important NOT to use the Calendar class because it takes into account the time zones and leap seconds. If you would like to
     * simply add a minute to a date, use this method.
     * @param instant the date object to add a minute to
     * @return the given date a minute ahead if not null
     */
    public static Instant addMinuteToZman(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.plusSeconds(60);
    }

    public static String removePostalCode(String input) {
        if (input == null) return null;
        return input.replaceAll("\\s*\\([^)]*\\)$", "").trim();// Removes ANY trailing " (something)"
    }

    /**
     * This method parses a number that was stored as a string in the shared preferences. The preference screens let the user type freely, so a
     * stored value can be empty or not a number at all, and every read of it happens on a path where throwing would crash the app.
     * @param value the string to parse, possibly null, empty or not a number
     * @param defaultValue the value to return if the string cannot be parsed
     * @return the parsed value, or defaultValue if the string is null, empty or not a number
     */
    public static double parseDoubleOrDefault(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * convenience method to schedule notifications
     * @param am an AlarmManager object
     * @param timeInMillis the time the alarm should go off at
     * @param pendingIntent the intent to be broadcast
     */
    public static void setExactAndAllowWhileIdle(AlarmManager am, long timeInMillis, PendingIntent pendingIntent) {
        am.cancel(pendingIntent);//cancel any previous alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);// Doze will not hold this until the next maintenance window
        } else {// on lower android version, app will not crash by setting exact alarms
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }
}
