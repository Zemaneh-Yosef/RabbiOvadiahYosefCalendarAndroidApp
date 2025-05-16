package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;

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
     * This is a convenience method that automatically searches the URL passed in and makes sure
     * that the page actually has the elevation data. The chai table website has 6 different options
     * for which type of sunrise/sunset table you can choose from. Astronomical, mishor (sea level,
     * and visible sunrise/sunset. However, only the webpage for the astronomical sunrise/sunset
     * tables contain the elevation data for whatever reason. Therefore, to help the user out, I
     * simply understood that I can replace whatever option they choose with the correct webpage.
     * All they need to do is choose the city, and it doesn't matter which table they choose.
     * @deprecated This is no longer used because the elevation on the chai tables website is NOT the highest point of elevation in the city.
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
     * code will correct their mistakes. 0 is the value for the visible sunrise table.
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
     * @deprecated This is no longer used because the elevation on the chai tables website is NOT the highest point of elevation in the city.
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
     * @deprecated This is no longer used because the elevation on the chai tables website is NOT the highest point of elevation in the city like
     * I originally assumed. Rather it was an arbitrary number chosen at random.
     */
    private double findElevation(String s) {
        if (!isSearchRadiusTooSmall) {
            return Double.parseDouble(
                    s.substring(s.indexOf("height:"), s.indexOf("m\n"))//This is probably not future proof
                            .replaceAll("[^\\d.]", ""));//get rid of all the letters
        }
        return 0;
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
        String filename = "visibleSunriseTable" + sCurrentLocationName + jewishDate.getJewishYear() + ".csv";
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
     * @see #setOnlyDownloadTable(boolean)
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
