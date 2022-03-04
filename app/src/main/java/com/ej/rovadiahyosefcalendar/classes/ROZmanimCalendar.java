package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

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
        long shaahZmanit = getTemporalHour(getSunrise(), getSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getAlos72Zmanis(),(6 * dakahZmanit));//use getTimeOffset to handle nulls
    }

    public Date getHaNetz() {
        try {
            ChaiTables chaiTables = new ChaiTables(externalFilesDir, jewishCalendar);

            if (ChaiTables.visibleSunriseFileExists(externalFilesDir, jewishCalendar)) {
                String currentVisibleSunrise = chaiTables.getVisibleSunrise();

                int visibleSunriseHour = Integer.parseInt(currentVisibleSunrise.substring(0, 1));
                int visibleSunriseMinutes = Integer.parseInt(currentVisibleSunrise.substring(2, 4));

                Calendar tempCal = getCalendar();
                tempCal.set(Calendar.HOUR_OF_DAY, visibleSunriseHour);
                tempCal.set(Calendar.MINUTE, visibleSunriseMinutes);

                if (currentVisibleSunrise.length() == 7) {
                    int visibleSunriseSeconds = Integer.parseInt(currentVisibleSunrise.substring(5, 7));
                    tempCal.set(Calendar.SECOND, visibleSunriseSeconds);
                }
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
        return getSunTransit(getSunrise(), getSunset());
    }

    @Override
    public Date getPlagHamincha() {
        long shaahZmanit = getTemporalHour(getSunrise(), getSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getTzait(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    @Override
    public Date getCandleLighting() {
        return getTimeOffset(getElevationAdjustedSunset(), -getCandleLightingOffset() * MILLISECONDS_PER_MINUTE);
    }

    public Date getTzait() {
        long shaahZmanit = getTemporalHour(getSunrise(), getSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getSunset(),(13 * dakahZmanit) + (dakahZmanit / 2));
    }

    public Date getTzaitTaanit() {
        return getTimeOffset(getSunset(), (20 * MILLISECONDS_PER_MINUTE));
    }

    public Date getTzaitTaanitLChumra() {
        return getTimeOffset(getSunset(), (30 * MILLISECONDS_PER_MINUTE));
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
}
