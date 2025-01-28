package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.NonNull;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class JewishCalendarWithExtraMethods extends JewishCalendar {

    private boolean isSafekMukafChoma = false;

    public boolean getIsSafekMukafChoma() {
        return isSafekMukafChoma;
    }

    public void setIsSafekMukafChoma(boolean isSafekMukafChoma) {
        this.isSafekMukafChoma = isSafekMukafChoma;
    }

    @Override
    public boolean isPurim() {
        if (getIsMukafChoma()) {
            return getYomTovIndex() == SHUSHAN_PURIM;
        } else if (isSafekMukafChoma) {
            return getYomTovIndex() == PURIM || getYomTovIndex() == SHUSHAN_PURIM;
        } else {
            return getYomTovIndex() == PURIM;
        }
    }

    public Double getTekufa() {
        double INITIAL_TEKUFA_OFFSET = 12.625;  // the number of days Tekufas Tishrei occurs before JEWISH_EPOCH

        double days = getJewishCalendarElapsedDays(getJewishYear()) + getDaysSinceStartOfJewishYear() + INITIAL_TEKUFA_OFFSET - 1;  // total days since first Tekufas Tishrei event

        double solarDaysElapsed = days % 365.25;  // total days elapsed since start of solar year
        double tekufaDaysElapsed = solarDaysElapsed % 91.3125;  // the number of days that have passed since a tekufa event
        if (tekufaDaysElapsed > 0 && tekufaDaysElapsed <= 1) {  // if the tekufa happens in the upcoming 24 hours
            return ((1.0 - tekufaDaysElapsed) * 24.0) % 24;// rationalize the tekufa event to number of hours since start of jewish day
        } else {
            return null;
        }
    }

    public String getTekufaName() {
        String[] tekufaNames;
        if (Utils.isLocaleHebrew()) {
            tekufaNames = new String[]{"תשרי", "טבת", "ניסן", "תמוז"};
        } else {
            tekufaNames = new String[]{"Tishri", "Tevet", "Nissan", "Tammuz"};
        }

        double INITIAL_TEKUFA_OFFSET = 12.625;  // the number of days Tekufas Tishrei occurs before JEWISH_EPOCH
        double days = getJewishCalendarElapsedDays(getJewishYear()) + getDaysSinceStartOfJewishYear() + INITIAL_TEKUFA_OFFSET - 1;  // total days since first Tekufas Tishrei event

        double solarDaysElapsed = days % 365.25;  // total days elapsed since start of solar year
        int currentTekufaNumber = (int) (solarDaysElapsed / 91.3125);  // the current quarter of the solar year
        double tekufaDaysElapsed = solarDaysElapsed % 91.3125;  // the number of days that have passed since a tekufa event
        if (tekufaDaysElapsed > 0 && tekufaDaysElapsed <= 1) {  // if the tekufa happens in the upcoming 24 hours
            return tekufaNames[currentTekufaNumber];//0 for Tishrei, 1 for Tevet, 2, for Nissan, 3 for Tammuz
        } else {
            return "";
        }
    }

    public Date getTekufaAsDate() {
        // The tekufa Date (point in time) must be generated using standard time. Using "Asia/Jerusalem" timezone will result in the time
        // being incorrectly off by an hour in the summer due to DST. Proper adjustment for the actual time in DST will be done by the date
        // formatter class used to display the Date.
        TimeZone yerushalayimStandardTZ = TimeZone.getTimeZone("GMT+2");
        Calendar cal = Calendar.getInstance(yerushalayimStandardTZ);
        cal.clear();
        if (getTekufa() == null) {
            return null;
        }
        double hours = getTekufa() - 6;
        int minutes = (int) ((hours - (int) hours) * 60);
        cal.set(getGregorianYear(), getGregorianMonth(), getGregorianDayOfMonth(), 0, 0, 0);
        cal.add(Calendar.HOUR_OF_DAY, (int) hours);
        cal.add(Calendar.MINUTE, minutes);

        return cal.getTime();
    }

    public Date getAmudeiHoraahTekufaAsDate() {
        //The Luach Amudei Horaah uses the same calculation for the tekufa, however, it uses the local midday time of Israel as the starting point,
        //instead of 12pm.

        // The tekufa Date (point in time) must be generated using standard time. Using "Asia/Jerusalem" timezone will result in the time
        // being incorrectly off by an hour in the summer due to DST. Proper adjustment for the actual time in DST will be done by the date
        // formatter class used to display the Date.
        TimeZone yerushalayimStandardTZ = TimeZone.getTimeZone("GMT+2");
        Calendar cal = Calendar.getInstance(yerushalayimStandardTZ);
        cal.clear();
        if (getTekufa() == null) {
            return null;
        }
        double hours = getTekufa() - 6;
        int minutes = (int) ((hours - (int) hours) * 60);
        minutes -= 21; //minus 21 minutes to get to local midday
        cal.set(getGregorianYear(), getGregorianMonth(), getGregorianDayOfMonth(), 0, 0, 0);
        cal.add(Calendar.HOUR_OF_DAY, (int) hours);
        cal.add(Calendar.MINUTE, minutes);

        return cal.getTime();
    }

    @NonNull
    @Override
    public String toString() {
        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        hebrewDateFormatter.setHebrewFormat(Utils.isLocaleHebrew());
        return hebrewDateFormatter.format(this).replace("Teves", "Tevet").replace("Tishrei", "Tishri");
    }
}
