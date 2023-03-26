package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sCurrentLocationName;

import androidx.annotation.Nullable;

import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ROZmanimCalendar extends ComplexZmanimCalendar {

    private final JewishCalendar jewishCalendar;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MILLISECONDS_PER_MINUTE = 60_000;
    private File externalFilesDir;
    private Date visibleSunriseDate;

    public ROZmanimCalendar(GeoLocation location) {
        super(location);
        jewishCalendar = new JewishCalendar();
        setUseElevation(true);
    }

    public Date getEarliestTalitTefilin() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getAlos72Zmanis(), (6 * dakahZmanit));//use getTimeOffset to handle nulls
    }

    public Date getHaNetz() {
        try {
            ChaiTables chaiTables = new ChaiTables(externalFilesDir, sCurrentLocationName, jewishCalendar);

            if (chaiTables.visibleSunriseFileExists()) {
                String currentVisibleSunrise = chaiTables.getVisibleSunrise();

                int visibleSunriseHour = Integer.parseInt(currentVisibleSunrise.substring(0, 1));
                int visibleSunriseMinutes = Integer.parseInt(currentVisibleSunrise.substring(2, 4));

                Calendar tempCal = (Calendar) getCalendar().clone();
                tempCal.set(Calendar.HOUR_OF_DAY, visibleSunriseHour);
                tempCal.set(Calendar.MINUTE, visibleSunriseMinutes);

                if (currentVisibleSunrise.length() == 7) {
                    int visibleSunriseSeconds = Integer.parseInt(currentVisibleSunrise.substring(5, 7));
                    tempCal.set(Calendar.SECOND, visibleSunriseSeconds);
                } else {
                    tempCal.set(Calendar.SECOND, 0);
                }
                tempCal.set(Calendar.MILLISECOND, 0);
                visibleSunriseDate = tempCal.getTime();
            } else {
                visibleSunriseDate = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return visibleSunriseDate;
    }

    public Date getHaNetz(String locationName) {
        try {
            ChaiTables chaiTables = new ChaiTables(externalFilesDir, locationName, jewishCalendar);

            if (chaiTables.visibleSunriseFileExists()) {
                String currentVisibleSunrise = chaiTables.getVisibleSunrise();

                int visibleSunriseHour = Integer.parseInt(currentVisibleSunrise.substring(0, 1));
                int visibleSunriseMinutes = Integer.parseInt(currentVisibleSunrise.substring(2, 4));

                Calendar tempCal = (Calendar) getCalendar().clone();
                tempCal.set(Calendar.HOUR_OF_DAY, visibleSunriseHour);
                tempCal.set(Calendar.MINUTE, visibleSunriseMinutes);

                if (currentVisibleSunrise.length() == 7) {
                    int visibleSunriseSeconds = Integer.parseInt(currentVisibleSunrise.substring(5, 7));
                    tempCal.set(Calendar.SECOND, visibleSunriseSeconds);
                } else {
                    tempCal.set(Calendar.SECOND, 0);
                }
                tempCal.set(Calendar.MILLISECOND, 0);
                visibleSunriseDate = tempCal.getTime();
            } else {
                visibleSunriseDate = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return visibleSunriseDate;
    }

    public Date getSofZmanBiurChametzMGA() {
        long shaahZmanit = getTemporalHour(getAlos72Zmanis(), getTzais72Zmanis());
        return getTimeOffset(getAlos72Zmanis(), shaahZmanit * 5);
    }

    public Date getChatzot() {
        return getSunTransit(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
    }

    @Override
    public Date getPlagHamincha() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getTzeit(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    public Date getPlagHaminchaAmudeiHoraah() {
        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getTzeitAmudeiHoraah(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    public Date getPlagHaminchaHalachaBerurah() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getSunset(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    @Override
    public Date getCandleLighting() {
        return getTimeOffset(getElevationAdjustedSunset(), -getCandleLightingOffset() * MILLISECONDS_PER_MINUTE);
    }

    public Date getTzeit() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getElevationAdjustedSunset(),(13 * dakahZmanit) + (dakahZmanit / 2));
    }

    public Date getTzaitTaanit() {
        return getTimeOffset(getElevationAdjustedSunset(), (20 * MILLISECONDS_PER_MINUTE));
    }

    public Date getTzaitTaanitLChumra() {
        return getTimeOffset(getElevationAdjustedSunset(), (30 * MILLISECONDS_PER_MINUTE));
    }

    @Override
    public Date getSolarMidnight() {
        ZmanimCalendar clonedCal = (ZmanimCalendar) clone();
        clonedCal.getCalendar().add(Calendar.DAY_OF_MONTH, 1);
        Date sunset = getSunset();
        Date sunrise = clonedCal.getSunrise();
        return getTimeOffset(sunset, getTemporalHour(sunset, sunrise) * 6);
    }

    public void setExternalFilesDir(File externalFilesDir) {
        this.externalFilesDir = externalFilesDir;
    }

    @Override
    public void setCalendar(Calendar calendar) {
        super.setCalendar(calendar);
        if (getCalendar() != null && jewishCalendar != null) {
            jewishCalendar.setDate(getCalendar());
        }
    }

    @Nullable
    public Date getAlotAmudeiHoraah() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(2023, Calendar.MARCH, 20));//set the calendar to the equinox
        Date sunrise = getSeaLevelSunrise();
        Date alotBy16Degrees = getAlos16Point1Degrees();//Get the time of 16.1° below the horizon on the equinox, just as we do for Israel to get the time of 16.1° below the horizon on the equinox
        setCalendar(tempCal);//reset the calendar to the current day
        //get the amount of minutes between the two Date objects
        long numberOfMinutes = ((sunrise.getTime() - alotBy16Degrees.getTime()) / 60000);
        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        //now that we have the number of minutes (should be 80 minutes for NY), we can calculate the time of Alot Hashachar for the current day using zmaniyot minutes
        //so in NY, Alot Hashachar is 80 zmaniyot minutes before sunrise
        return getTimeOffset(getSeaLevelSunrise(), -(numberOfMinutes * dakahZmanit));
    }

    public Date getEarliestTalitTefilinAmudeiHoraah() {
        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getSeaLevelSunrise(), -(shaahZmanit + dakahZmanit * 6));
    }

    public Date getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah() {
        return getSofZmanShma(getAlotAmudeiHoraah(), getTzais72ZmanisAmudeiHoraah());
    }

    public Date getTzeitAmudeiHoraah() {
            Calendar tempCal = (Calendar) getCalendar().clone();
            setCalendar(new GregorianCalendar(2023, Calendar.MARCH, 20));
            Date sunset = getSeaLevelSunset();
            Date tzaitBy3point65degrees = getTzaisGeonim3Point65Degrees();
            long numberOfMinutes = ((tzaitBy3point65degrees.getTime() - sunset.getTime()) / MILLISECONDS_PER_MINUTE);
            setCalendar(tempCal);//reset the calendar to the current day
            long shaahZmanit = getTemporalHour(getAlos72Zmanis(), getTzais72Zmanis());
            long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
            return getTimeOffset(getSeaLevelSunset(), numberOfMinutes * dakahZmanit);
    }

    public Date getTzeitAmudeiHoraahLChumra() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(2023, Calendar.MARCH, 20));
        Date sunset = getSeaLevelSunset();
        Date tzaitBy3point65degrees = getTzaisGeonim3Point65Degrees();
        setCalendar(tempCal);//reset the calendar to the current day
        //get the amount of minutes between the two Date objects
        int numberOfMinutes = (int) ((tzaitBy3point65degrees.getTime() - sunset.getTime()) / MILLISECONDS_PER_MINUTE);
        long shaahZmanit = getTemporalHour(getAlos72Zmanis(), getTzais72Zmanis());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getSeaLevelSunset(), 20 * dakahZmanit);
    }

    public Date getTzaitShabbatAmudeiHoraah() {
        return getSunsetOffsetByDegrees(GEOMETRIC_ZENITH + 7.14);
    }

    public Date getTzais72ZmanisAmudeiHoraah() {
        //find the amount of minutes between sunrise and 16.1° below the horizon on a day that occurs on the equinox
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(2023, Calendar.MARCH, 20));
        Date sunset = getSeaLevelSunset();
        Date tzaitBy16Degrees = getTzais16Point1Degrees();//Get the time of 16.1° below the horizon on the equinox, just as we do for Israel to get the time of 16.1° below the horizon on the equinox
        setCalendar(tempCal);//reset the calendar to the current day
        //get the amount of minutes between the two Date objects
        int numberOfMinutes = (int) ((tzaitBy16Degrees.getTime() - sunset.getTime()) / MILLISECONDS_PER_MINUTE);
        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getSeaLevelSunset(), (numberOfMinutes * dakahZmanit));
    }

    public Date getTzais72ZmanisAmudeiHoraahLkulah() {
        if (getTzais72().before(getTzais72ZmanisAmudeiHoraah())) {//return the earlier of the two times
            return getTzais72();
        } else {
            return getTzais72ZmanisAmudeiHoraah();
        }
    }

}
