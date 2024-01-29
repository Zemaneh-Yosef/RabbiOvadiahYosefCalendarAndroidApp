package com.EJ.ROvadiahYosefCalendar.classes;

import android.content.SharedPreferences;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to read the chai tables from a CSV file after the chai tables are downloaded from the web.
 * @author Elyahu Jacobi
 */
public class ChaiTables {
    private final JewishCalendar jewishCalendar;
    private final String currentLocation;
    private final SharedPreferences sharedPreferences;

    /**
     * Constructor for the ChaiTables class.
     *
     * @param currentLocation   The current location of the user/table.
     * @param jewishCalendar    The JewishCalendar object that is used to get the current year.
     * @param sharedPreferences The SharedPreferences object to get the chai tables from.
     */
    public ChaiTables(String currentLocation, JewishCalendar jewishCalendar, SharedPreferences sharedPreferences) {
        this.currentLocation = currentLocation;
        this.jewishCalendar = jewishCalendar;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * This method gets the time of the visible sunrise for the current day that was set in the constructor.
     * @return The time of the visible sunrise for the current day as a string.
     */
    public String getVisibleSunrise() {
        String visibleSunriseTable = sharedPreferences.getString("chaiTable" + currentLocation, "");
        if (visibleSunriseTable.equals("")) {
            return ""; // string is empty return an empty string
        }

        int currentHebrewMonth = jewishCalendar.getJewishMonth();

        currentHebrewMonth -= 6;
        if (currentHebrewMonth < 1) {
            if (jewishCalendar.isJewishLeapYear()){
                currentHebrewMonth += 13;
            } else {
                currentHebrewMonth += 12;
            }
        }
        List<String[]> actualVSunriseTable = parseTableString(visibleSunriseTable);
        return actualVSunriseTable.get(jewishCalendar.getJewishDayOfMonth())[currentHebrewMonth];
    }

    public static List<String[]> parseTableString(String tableString) {
        List<String[]> parsedTable = new ArrayList<>();

        // Split the input string into rows
        String[] rows = tableString.split("\n");

        for (String row : rows) {
            // Split each row into cells
            String[] cells = row.split("\t");

            // Add the array of cells to the parsed table
            parsedTable.add(cells);
        }

        return parsedTable;
    }
}
