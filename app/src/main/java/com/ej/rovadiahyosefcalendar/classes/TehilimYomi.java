package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The chapters of Tehilim said each day, either to finish the book every month or every week.
 */
public class TehilimYomi {

    private TehilimYomi() {}

    /**
     * @return the chapters said on this day of the Jewish month, e.g. "1 - 9"
     */
    public static String byDayOfMonth(Context context, JewishCalendar jewishCalendar) {
        ArrayList<String> dailyMonthlyTehilim;
        if (Utils.isLocaleHebrew(context)) {
            dailyMonthlyTehilim = new ArrayList<>(Arrays.asList(
                    "א - ט",       // 1 - 9
                    "י - יז",      // 10 - 17
                    "יח - כב",     // 18 - 22
                    "כג - כח",     // 23 - 28
                    "כט - לד",     // 29 - 34
                    "לה - לח",     // 35 - 38
                    "לט - מג",     // 39 - 43
                    "מד - מח",     // 44 - 48
                    "מט - נד",     // 49 - 54
                    "נה - נט",     // 55 - 59
                    "ס - סה",      // 60 - 65
                    "סו - סח",     // 66 - 68
                    "סט - עא",     // 69 - 71
                    "עב - עו",     // 72 - 76
                    "עז - עח",     // 77 - 78
                    "עט - פב",     // 79 - 82
                    "פג - פז",     // 83 - 87
                    "פח - פט",     // 88 - 89
                    "צ - צו",      // 90 - 96
                    "צז - קג",     // 97 - 103
                    "קד - קה",     // 104 - 105
                    "קו - קז",     // 106 - 107
                    "קח - קיב",    // 108 - 112
                    "קיג - קיח",   // 113 - 118
                    "קיט:א - קיט:צו", // 119:1 - 119:96
                    "קיט:צז - קיט:קעו", // 119:97 - 119:176
                    "קכ - קלד",     // 120 - 134
                    "קל - קלט",     // 135 - 139
                    "קמ - " + (jewishCalendar.getDaysInJewishMonth() == 29 ? "קנ" : "קמה"), // 140 - 150 or 145
                    "קמה - קנ"       // 145 - 150
            ));
        } else {
            dailyMonthlyTehilim = new ArrayList<>(Arrays.asList(
                    "1 - 9",
                    "10 - 17",
                    "18 - 22",
                    "23 - 28",
                    "29 - 34",
                    "35 - 38",
                    "39 - 43",
                    "44 - 48",
                    "49 - 54",
                    "55 - 59",
                    "60 - 65",
                    "66 - 68",
                    "69 - 71",
                    "72 - 76",
                    "77 - 78",
                    "79 - 82",
                    "83 - 87",
                    "88 - 89",
                    "90 - 96",
                    "97 - 103",
                    "104 - 105",
                    "106 - 107",
                    "108 - 112",
                    "113 - 118",
                    "119:1 - 119:96",
                    "119:97 - 119:176",
                    "120 - 134",
                    "135 - 139",
                    "140 - " + (jewishCalendar.getDaysInJewishMonth() == 29 ? 150 : 145),
                    "145 - 150"));
        }
        return dailyMonthlyTehilim.get(jewishCalendar.getJewishDayOfMonth() - 1);
    }

    /**
     * @return the chapters said on this day of the week, e.g. "1 - 29"
     */
    public static String byDayOfWeek(Context context, JewishCalendar jewishCalendar) {
        ArrayList<String> dailyWeeklyTehilim;
        if (Utils.isLocaleHebrew(context)) {
            dailyWeeklyTehilim = new ArrayList<>(Arrays.asList(
                    "א - כט",      // 1 - 29
                    "ל - נ",       // 30 - 50
                    "נא - עב",     // 51 - 72
                    "עג - פט",     // 73 - 89
                    "צ - קו",      // 90 - 106
                    "קז - קיט",    // 107 - 119
                    "קכ - קנ"      // 120 - 150
            ));
        } else {
            dailyWeeklyTehilim = new ArrayList<>(Arrays.asList(
                    "1 - 29",
                    "30 - 50",
                    "51 - 72",
                    "73 - 89",
                    "90 - 106",
                    "107 - 119",
                    "120 - 150"
            ));
        }
        return dailyWeeklyTehilim.get(jewishCalendar.getDayOfWeek() - 1);
    }
}
