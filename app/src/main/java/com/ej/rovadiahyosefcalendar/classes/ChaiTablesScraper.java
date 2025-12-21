package com.ej.rovadiahyosefcalendar.classes;

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
     * The name of the location to add to the file name.
     */
    private final String locationName;

    /**
     * The URL to scrape, this must be set before we can scrape any webpage.
     */
    private String mUrl;

    /**
     * The File to save to, this must be set before you call the {@link #writeZmanimTableToFile}
     * method
     *
     * @see #writeZmanimTableToFile()
     */
    private File mExt;

    /**
     * Jewish Date to check the time of the tables
     */
    private JewishDate jewishDate;

    /**
     * Boolean that you can set to only download the tables and not the elevation data
     * @see #writeZmanimTableToFile()
     */
    private boolean isSearchRadiusTooSmall;
    private boolean mWebsiteError;

    private OnClickListeners.ScraperCallback callback;

    public ChaiTablesScraper(String locationName) {
        this.locationName = locationName;
    }

    public void setCallback(OnClickListeners.ScraperCallback callback) {
        this.callback = callback;
    }

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
     * @param url the url of the chai tables website
     * @param externalFilesDir the directory where to save the tables
     * @param jewishDate the jewish date/year to save the tables
     */
    public void setDownloadSettings(String url, File externalFilesDir, JewishDate jewishDate) {
        setJewishDate(jewishDate);
        setUrl(url);
        setExternalFilesDir(externalFilesDir);
    }

    /**
     * The setter method for the Jewish Date to save the tables for.
     *
     * @param jewishDate The Jewish Date to save the tables for
     */
    public void setJewishDate(JewishDate jewishDate) {
        this.jewishDate = jewishDate;
    }

    /**
     * This method uses the Jsoup API to download the whole page into a csv file.
     * @throws IOException because of the Jsoup API if an error occurs
     */
    public void writeZmanimTableToFile() throws IOException {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            throw new IOException("Something went wrong writing to disk");
        }
        String filename = "visibleSunriseTable" + locationName + jewishDate.getJewishYear() + ".csv";
        File file = new File(mExt, filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file, false);

            Document doc = Jsoup.connect(mUrl)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com").get();

            if (doc.text().contains("You can increase the search radius and try again")) {
                isSearchRadiusTooSmall = true;
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
            mWebsiteError = true;
        }
    }

    /**
     * This method finds the value of the cgi_yrheb in the URL and replaces it with the next year.
     */
    private void changeURLtoNextYear() {
        int currentJewishYear = jewishDate.getJewishYear();
        if (jewishDate.isJewishLeapYear() && jewishDate.getJewishMonth() == JewishDate.ADAR_II) {
            jewishDate.setJewishMonth(JewishDate.ADAR);//edge case
        }
        jewishDate.setJewishYear(currentJewishYear + 1);
        mUrl = mUrl.replace("&cgi_yrheb=" + currentJewishYear,
                "&cgi_yrheb=" + jewishDate.getJewishYear());
    }

    /**
     * This method should be called through the {@link #start()} method. If the URL and file
     * directory have been set, this method will get the tables from the URL and save them to that
     * directory.
     *
     * @see #setUrl(String)
     * @see #setExternalFilesDir(File)
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

        if (callback != null) {
            callback.onScraperFinished();
        }

//        if (mUrl != null && !mOnlyDownloadTable) {
//            try {
//                mResult = getElevationData();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public boolean isSearchRadiusTooSmall() {
        return isSearchRadiusTooSmall;
    }

    public boolean isWebsiteError() {
        return mWebsiteError;
    }
}
