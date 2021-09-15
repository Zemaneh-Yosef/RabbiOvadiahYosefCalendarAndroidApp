package com.elyjacobi.ROvadiahYosefCalendar.classes;

import android.os.Environment;

import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ChaiTablesScraper extends Thread {

    /**
     * The result of the highest point of elevation from the chai tables website.
     */
    private double mResult;

    /**
     * The URL to scrape, this must be set before we do any actual work.
     */
    private String mUrl;

    /**
     * The File to save to, this must be set before you call the {@link #writeZmanimTableToFile}
     * method
     *
     * @see #writeZmanimTableToFile()
     */
    private File mExt;

    private final JewishDate jewishDate = new JewishDate();

    /**
     * Boolean that you can set to only download the tables and not the elevation data
     *
     * @see #writeZmanimTableToFile()
     */
    private boolean mOnlyDownloadTable;

    /**
     * The setter method for the URL to scrape.
     *
     * @param url The chai tables url to scrape
     */
    public void setUrl(String url) {
        mUrl = url;
    }

    /**
     * The setter method for the file directory to save to.
     *
     * @param externalFilesDir The file directory to use
     */
    public void setExternalFilesDir(File externalFilesDir) {
        mExt = externalFilesDir;
    }

    /**
     * The setter method for whether or not to download only the table.
     *
     * @param onlyDownloadTable boolean whether you want to only download the table
     */
    public void setOnlyDownloadTable(boolean onlyDownloadTable) {
        mOnlyDownloadTable = onlyDownloadTable;
    }

    /**
     * The setter method for whether or not to download only the table.
     *
     * @param url the url of the chai tables website
     * @param externalFilesDir the directory where to save the tables
     * @param onlyDownloadTable boolean whether you want to only download the table
     */
    public void setDownloadSettings(String url, File externalFilesDir, boolean onlyDownloadTable) {
        setUrl(url);
        setExternalFilesDir(externalFilesDir);
        setOnlyDownloadTable(onlyDownloadTable);
    }

    /**
     * The getter for the result of the elevation
     *
     * @return the double value of the result from the tables
     */
    public double getResult() {
        return mResult;
    }

    /**
     * This is a convenience method that automatically searches the URL passed in and makes sure
     * that the page actually has the elevation data. The chai table website has 6 different options
     * for which type of sunrise/sunset table you can choose from. Astronomical, mishor (sea level,
     * and visible sunrise/sunset. However, only the webpage for the astronomical sunrise/sunset
     * tables contain the elevation data for whatever reason. Therefore, to help the user out, I
     * simply understood that I can replace whatever option they choose with the correct webpage.
     * All they need to do is choose the city, and it doesn't matter which table they choose.
     */
    private void assertElevationURL() {
        if (mUrl.contains("&cgi_types=0")) {
            mUrl = mUrl.replace("&cgi_types=0", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=1")) {
            mUrl = mUrl.replace("&cgi_types=1", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=2")) {
            mUrl = mUrl.replace("&cgi_types=2", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=3")) {
            mUrl = mUrl.replace("&cgi_types=3", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=4")) {
            mUrl = mUrl.replace("&cgi_types=4", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=-1")) {
            mUrl = mUrl.replace("&cgi_types=-1", "&cgi_types=5");
        }
    }

    /**
     * This is a convenience method that automatically searches the URL passed in and makes sure
     * that the page actually has the visible sunrise data. The chai table website has 6 different
     * options for which type of sunrise/sunset table you can choose from. Astronomical,
     * mishor (sea level), and visible sunrise/sunset. However, only the webpage for the visible
     * sunrise tables contain the data that we need. Therefore, to help the user out, I
     * simply understood that I can replace whatever option they choose with the correct webpage.
     * All they need to do is choose the city, and it doesn't matter which table they choose, this
     * code will correct their mistakes.
     */
    private void assertVisibleSunriseURL() {
        if (mUrl.contains("&cgi_types=5")) {
            mUrl = mUrl.replace("&cgi_types=5", "&cgi_types=0");
        } else if (mUrl.contains("&cgi_types=1")) {
            mUrl = mUrl.replace("&cgi_types=1", "&cgi_types=0");
        } else if (mUrl.contains("&cgi_types=2")) {
            mUrl = mUrl.replace("&cgi_types=2", "&cgi_types=0");
        } else if (mUrl.contains("&cgi_types=3")) {
            mUrl = mUrl.replace("&cgi_types=3", "&cgi_types=0");
        } else if (mUrl.contains("&cgi_types=4")) {
            mUrl = mUrl.replace("&cgi_types=4", "&cgi_types=0");
        } else if (mUrl.contains("&cgi_types=-1")) {
            mUrl = mUrl.replace("&cgi_types=-1", "&cgi_types=0");
        }
    }

    /**
     * This method uses the Jsoup API to download the whole page as one very long string.
     * (It could probably be refactored to be more efficient) After it has the whole webpage, it
     * searches it for the elevation data. Currently the data is displayed in the webpage like
     * this, " height: 30m " The method searches for the word height and a new line character.
     * This is probably not future proof, but it works for now and I do not think the website will
     * be updated for a while.
     *
     * @return a double containing the highest elevation of the city in meters
     * @throws IOException because of the Jsoup API if an error occurs
     */
    public double getElevationData() throws IOException {
        assertElevationURL();
        StringBuilder stringBuilder = new StringBuilder();
        Document doc = Jsoup.connect(mUrl)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").get();
        Elements tableElement = doc.select("table");

        Elements tableHeaderEles = tableElement.select("thead tr th");
        for (int i = 0; i < tableHeaderEles.size(); i++) {
            stringBuilder.append(tableHeaderEles.get(i).text());
            if (i != tableHeaderEles.size() - 1) {
                stringBuilder.append(',');
            }
        }
        stringBuilder.append('\n');

        Elements tableRowElements = tableElement.select(":not(thead) tr");

        for (Element row : tableRowElements) {
            Elements rowItems = row.select("td");
            for (int j = 0; j < rowItems.size(); j++) {
                stringBuilder.append(rowItems.get(j).text());
                if (j != rowItems.size() - 1) {
                    stringBuilder.append(',');
                }
            }
            stringBuilder.append('\n');
        }
        return findElevation(stringBuilder.toString());
    }

    /**
     * Method to find the elevation of the string passed in.
     *
     * @param s a string containing the chai tables webpage
     * @return a double containing the highest elevation of the city in meters
     */
    private double findElevation(String s) {
        return Double.parseDouble(
                s.substring(s.indexOf("height:"), s.indexOf("m\n"))//This is probably not future proof
                        .replaceAll("[^\\d.]", ""));//get rid of all the letters
    }

    /**
     * This method uses the Jsoup API to download the whole page into a csv file.
     *
     * @throws IOException because of the Jsoup API if an error occurs
     */
    public void writeZmanimTableToFile() throws IOException {
        assertVisibleSunriseURL();
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            throw new IOException("Something went wrong writing to disk");
        }
        String filename = "visibleSunriseTable" + jewishDate.getJewishYear() + ".csv";
        File file = new File(mExt, filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file, false);

            Document doc = Jsoup.connect(mUrl)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6)" +
                            " Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com").get();

            if (doc.text().contains("You can increase the search radius and try again")) {
                throw new Exception("search radius is too small!");
            }
            Elements tableElement = doc.select("table");

            Elements tableHeaderElements = tableElement.select("thead tr th");
            for (int i = 0; i < tableHeaderElements.size(); i++) {
                outputStream.write(tableHeaderElements.get(i).text().getBytes());
                if (i != tableHeaderElements.size() - 1) {
                    outputStream.write(',');
                }
            }
            outputStream.write('\n');

            Elements tableRowElements = tableElement.select(":not(thead) tr");

            for (Element row : tableRowElements) {
                Elements rowItems = row.select("td");
                for (int j = 0; j < rowItems.size(); j++) {
                    outputStream.write(rowItems.get(j).text().getBytes());
                    if (j != rowItems.size() - 1) {
                        outputStream.write(',');
                    }
                }
                outputStream.write('\n');
            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeURLtoNextYear() {
        int currentJewishYear = jewishDate.getJewishYear();
        jewishDate.setJewishYear(currentJewishYear + 1);
        mUrl = mUrl.replace("&cgi_yrheb=" + currentJewishYear,
                "&cgi_yrheb=" + jewishDate.getJewishYear());
    }

    /**
     * This method should be called through the {@link #start()} method. If the URL and file
     * directory have been set, this method will get the tables from the URL and save them to that
     * directory and get the elevation data unless the use has explicitly set the boolean to not
     * download the elevation data. If only the URL has been specified, then this method
     * will just find the elevation data from the webpage.
     *
     * @see #setUrl(String)
     * @see #setExternalFilesDir(File)
     * @see #setOnlyDownloadTable(boolean)
     * @see #setDownloadSettings(String, File, boolean)
     * @see #getElevationData()
     * @see #writeZmanimTableToFile()
     */
    @Override
    public void run() {
        if (mExt != null && mUrl != null) {
            try {
                writeZmanimTableToFile();
                changeURLtoNextYear();
                writeZmanimTableToFile();//for the next year
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mUrl != null && !mOnlyDownloadTable) {
            try {
                mResult = getElevationData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
