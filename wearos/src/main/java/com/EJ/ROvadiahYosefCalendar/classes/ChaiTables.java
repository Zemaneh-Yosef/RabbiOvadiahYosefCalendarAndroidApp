package com.EJ.ROvadiahYosefCalendar.classes;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to read the chai tables from a CSV file after the chai tables are downloaded from the web.
 * @author Elyahu Jacobi
 */
public class ChaiTables {
    private final String currentLocation;
    private final SharedPreferences sharedPreferences;

    /**
     * Constructor for the ChaiTables class.
     *
     * @param currentLocation   The current location of the user/table.
     * @param sharedPreferences The SharedPreferences object to get the chai tables from.
     */
    public ChaiTables(String currentLocation, SharedPreferences sharedPreferences) {
        this.currentLocation = currentLocation;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * This method gets the time of the visible sunrise for the current day that was set in the constructor.
     * @return The time of the visible sunrise for the current day as a string.
     */
    public List<Long> getVisibleSunrise() {
        if (sharedPreferences == null) {
            return null;
        }
        String visibleSunriseTable = sharedPreferences.getString("chaiTable" + Utils.removePostalCode(currentLocation), "");
        if (visibleSunriseTable.isEmpty() || visibleSunriseTable.contains("\t") || visibleSunriseTable.contains("\n")) {
            return null; // string is empty, or uses a \n or \t which is old code; return null
        }
        return parseColonList(visibleSunriseTable);
    }

    public List<Long> parseColonList(String str) {
        List<Long> out = new ArrayList<>();
        for (String s : str.split(":")) {
            if (s.length() == 1 || s.length() == 2) {
                return null;// invalid string, probably from old code; return null.
            }
            if (!s.isEmpty()) {
                out.add(Long.parseLong(s));
            }
        }
        return out;
    }
}
