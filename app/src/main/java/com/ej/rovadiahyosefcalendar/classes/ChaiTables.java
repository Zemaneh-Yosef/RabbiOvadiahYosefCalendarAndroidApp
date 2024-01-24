package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * This class is used to read the chai tables from a CSV file after the chai tables are downloaded from the web.
 * @author Elyahu Jacobi
 */
public class ChaiTables {

    private static final int ROWS_UNDER_MONTHS = 31;
    private final JewishCalendar jewishCalendar;
    private final File externalFilesDir;
    private final String currentLocation;
    private List<String[]> actualVSunriseTable;

    /**
     * Constructor for the ChaiTables class.
     * @param jewishCalendar The JewishCalendar object that is used to get the current year.
     * @param externalFilesDir The external files directory of the application.
     * @param currentLocation The current location of the user/table.
     */
    public ChaiTables(File externalFilesDir, String currentLocation, JewishCalendar jewishCalendar) throws Exception {
        this.externalFilesDir = externalFilesDir;
        this.currentLocation = currentLocation;
        this.jewishCalendar = jewishCalendar;

        if (visibleSunriseFileExists()) {
            File file = new File(externalFilesDir, "visibleSunriseTable"+ currentLocation + jewishCalendar.getJewishYear() +".csv");
            CSVReader visibleSunriseReader = new CSVReader(new FileReader(file));

            List<String[]> visibleSunriseCSVBody = visibleSunriseReader.readAll();
            actualVSunriseTable = formatToTheActualTable(visibleSunriseCSVBody);
            visibleSunriseReader.close();
        }
//        CSVWriter writer = new CSVWriter(new FileWriter(inputFile), ',');
//        writer.writeAll(csvBody);
//        writer.flush();
//        writer.close();
    }

    /**
     * This method is used to check if the visible sunrise table exists in the external files directory and if it does, it will initialize the
     * actual visible sunrise table to a list of strings.
     * @param csvBody The whole body of the CSV file.
     */
    public List<String[]> formatToTheActualTable(List<String[]> csvBody) throws Exception {
        int startingRow = 0;
        int lastRow = 0;
        ListIterator<String[]> listIterator = csvBody.listIterator();
        while (listIterator.hasNext()) {
            String currentRow = Arrays.toString(listIterator.next());
            if (currentRow.contains("Tishrey, Chesvan, Kislev, Teves, Shvat, Adar I, Adar II, Nisan, Iyar, Sivan, Tamuz, Av, Elul")
                    || currentRow.contains("Tishrey, Chesvan, Kislev, Teves, Shvat, Adar, Nisan, Iyar, Sivan, Tamuz, Av, Elul")) {
                startingRow = listIterator.nextIndex()-1;
                lastRow = startingRow + ROWS_UNDER_MONTHS;
            }
            listIterator.previous();
            listIterator.next();
        }
        if (startingRow == 0 && lastRow == 0) {
            File file = new File(externalFilesDir, "visibleSunriseTable"+ currentLocation + jewishCalendar.getJewishYear() +".csv");
            file.delete();
            throw new Exception("The file is corrupted. Deleting it...");
        } else {
            return csvBody.subList(startingRow, lastRow);
        }
    }

    /**
     * This method gets the time of the visible sunrise for the current day that was set in the constructor.
     * @return The time of the visible sunrise for the current day as a string.
     */
    public String getVisibleSunrise() {
        int currentHebrewMonth = jewishCalendar.getJewishMonth();

        currentHebrewMonth -= 6;
        if (currentHebrewMonth < 1) {
            if (jewishCalendar.isJewishLeapYear()){
                currentHebrewMonth += 13;
            } else {
                currentHebrewMonth += 12;
            }
        }
        return actualVSunriseTable.get(jewishCalendar.getJewishDayOfMonth())[currentHebrewMonth];
    }

    /**
     * This method checks if the visible sunrise fie exists in the external files directory.
     * @return True if the visible sunrise file exists in the external files directory, false otherwise.
     */
    public boolean visibleSunriseFileExists() {
        File sunriseCSV = new File(externalFilesDir, "visibleSunriseTable" + currentLocation + jewishCalendar.getJewishYear() + ".csv");
        return sunriseCSV.isFile();
    }

    /**
     * This method is a static version of {@link #visibleSunriseFileExists()}. It will check if the file exists in the external files directory.
     * @return True if the visible sunrise file does not exist in the external files directory, false otherwise.
     */
    public static boolean visibleSunriseFileDoesNotExist(File externalFilesDir, String currentLocation, JewishCalendar jewishCalendar) {
        File sunriseCSV = new File(externalFilesDir, "visibleSunriseTable" + currentLocation + jewishCalendar.getJewishYear() + ".csv");
        return !sunriseCSV.isFile();
    }

    public String getFullChaiTable() {
        StringBuilder tableString = new StringBuilder();

        // Check if the list is not null
        if (actualVSunriseTable != null) {
            // Iterate through the list
            for (String[] row : actualVSunriseTable) {
                // Check if the array is not null
                if (row != null) {
                    // Iterate through the array and append each element to the string
                    for (String cell : row) {
                        tableString.append(cell).append("\t"); // Assuming you want to separate columns with a tab
                    }
                    tableString.append("\n"); // Move to the next line for the next row
                }
            }
        }

        return tableString.toString();
    }

//Not needed for now, using android's shared preferences to save year of the table
//  public String getYearOfTable() throws Exception {
//        ListIterator<String[]> listIterator = visibleSunriseCSVBody.listIterator();
//        String year = null;
//        while (listIterator.hasNext()) {
//            String s = Arrays.toString(listIterator.next());
//            String[] arrayOfString = s.split("[^0-9]");
//            for (String s2: arrayOfString) {
//                if (s2.matches("[0-9]{4}")) {
//                    year=s2;
//                }
//            }
//            listIterator.previous();
//            listIterator.next();
//        }
//        if (year == null) {
//            throw new Exception();
//        } else {
//            return year;
//        }
//    }
}
