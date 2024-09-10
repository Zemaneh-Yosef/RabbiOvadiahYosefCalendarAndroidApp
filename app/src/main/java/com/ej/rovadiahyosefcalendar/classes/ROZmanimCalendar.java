package com.ej.rovadiahyosefcalendar.classes;

import androidx.annotation.Nullable;

import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This class extends the ComplexZmanimCalendar class to add a few methods that are specific to the opinion of the Rabbi Ovadiah Yosef ZT"L.
 * The zmanim that are added are based on the zmanim published in the calendar "Luach Hamaor, Ohr HaChaim" which was created under the guidance of
 * Rabbi Ovadiah Yosef ZT"L, Rabbi Shlomo Benizri Shlita, and Rabbi Asher Darshan Shlita.
 * <br><br>
 * It is important to note that Rabbi Ovadiah Yosef ZT"L used the Ohr HaChaim calendar himself and it is the only calendar that he
 * used.
 * <br><br>
 * Thankfully, KosherJava's complex zmanim calculator has all the zmanim and tools that are needed to calculate the zmanim in the
 * "Luach Hamaor, Ohr HaChaim" except for the zmanim that are based on visible sunrise. Visible sunrise is the exact time that the sun is visible
 * above the horizon. This time needs to take into account the horizon around the city. The {@link #getHaNetz()} method gets the visible sunrise
 * from a file that is used by the {@link ChaiTables} class. The {@link ChaiTablesScraper} class downloads the sunrise times from the ChaiTables website
 * and creates a file that contains the sunrise times for the current location.
 * <br><br>
 * All the other zmanim that were not included in KosherJava's complex zmanim calculator are calculated in this class.
 * <br><br>
 * On the subject of zmanim outside of Israel, Rabbi Ovadiah Yosef ZT"L never gave a definite psak on how to calculate zmanim outside of Israel, and
 * it is not so simple to say that the same way the Ohr HaChaim calendar calculates the zmanim should be used also outside of Israel. The Ohr HaChaim
 * calendar calculates the zmanim based on the Minchat Cohen that used Shaot Zmaniyot. This works well in Israel, however, even the Minchat Cohen
 * agrees that outside of Israel there should be a slight adjustment to the times for alot and tzait because of the curvature of the earth.
 * <br><br>
 * Therefore, Rabbi Lior Dahan Shlita, who is the author of the Amudei Horaah calendar (and sefarim under the same name on the mishna berurah) uses
 * degrees to adjust the times for alot and tzait. I have sat
 * down with him and he explained to me exactly how he calculated his zmanim, and I have implemented his calculations in this class as well.
 * <br><br>
 * I have also spoken with Rabbi Asher Darshan Shlita (programmer of the Ohr HaChaim Calendar) about this subject, and he said to ask Rabbi Shlomo
 * Benizri Shlita's opinion as we was the decision maker on the Ohr HaChaim calendar based on his talks with Rabbi Ovadiah Yosef ZT"L.
 * <br><br>
 * I then talked with Rabbi Shlomo Benizri Shlita about this subject, and he is of the firm opinion that the same way the Ohr HaChaim calendar
 * calculates the zmanim in Israel, that same way can be used anywhere in the world. He also said that he heard about Rabbi Leeor Dahan Shlita's
 * calculations for alot and tzait, and he disagrees because Rabbi Ovadiah Yosef ZT"L never liked to used degrees since it is not our minhag.
 * Rabbi Shlomo Benizri Shlita also said that he has spoken with Rabbi Ovadiah Yosef ZT"L several times, while creating the calendar, about whether
 * or not to use degrees for zmanim, and Rabbi Ovadiah Yosef ZT"L always told him not to use degrees. He firmly told me that he believes that the Ohr
 * HaChaim calendars calculation should be used anywhere in the world.
 * <br><br>
 * Of course, when I asked Rabbi Dahan about this, he said that he disagrees with Rabbi Shlomo Benizri Shlita's opinion, since the Minchat Cohen which
 * Rabbi Ovadiah Yosef ZT"L bases his zmanim on says to use degrees in Northern/Southern areas of the world.
 * <br><br>
 * I want to also point out that Rabbi David Yosef Shlita writes in Halacha Berurah (Chelek 15, Siman 261, halacha 13) to adjust the times for alot
 * and tzait based on the curvature of the earth. However, Rabbi David Yosef Shlita is known to argue with Rabbi Ovadiah Yosef ZT"L on many subjects.
 * <br><br>
 * Since the matter is not so simple, I have decided to give the user the option to choose which way to calculate the zmanim. The user can choose
 * to use the Ohr HaChaim method, which is the same way the Ohr HaChaim calendar calculates the zmanim for Israel without any degree adjustment, or
 * the user can choose to use the Amudei Horaah method with degree adjustments, which is identical to the way the Amudei Horaah calendar calculates
 * the zmanim adjusted to KosherJava's methods.
 * <br><br>
 * Note: Rabbi Dahan used an Excel spreadsheet to calculate the zmanim for the Amudei Horaah calendar. I could not just copy the calculations from
 * the spreadsheet, because the spreadsheet uses a different algorithm to calculate the zmanim. However, I have talked with Rabbi Dahan about his
 * methodology and once I understood it, I was able to implement it in this class. I have also tested the zmanim based on his calendars (with the
 * seconds of the zmanim added) that he publishes, and I have gone over the zmanim with him personally to make sure that they are correct.
 * <br><br>
 * I want to just end by saying that I am not a rabbi, and I am not a halachic authority. I am just a programmer who is trying to help the Sephardic
 * community. I have tried to do my best to make sure that the zmanim are calculated correctly. However, if you are still trying to figure out
 * whether or not to use the Ohr HaChaim method or the Amudei Horaah method, please read the following story brought down by Rabbi David Yosef
 * Shlita in his Sefer, "Halacha Berurah" (Chelek 14, Kuntrus Ki Ba Hashemesh (in the back of the sefer), End of Perek 17):
 * <br><br>
 * "Rabbi Ovadiah Yosef ZT"L was once traveling to New York with his son, Rabbi Dovid Yosef Shlita, and it was a Tisha Be'av. Rabbi Ovadiah Yosef ZT"L
 * told his son, Rabbi David Yosef Shlita, that at Tzait Hacochavim according to the Geonim (13 and a half zmaniyot minutes after sunset)
 * is when the fast ends, and you don't need to be any more machmir (Stringent) than that."
 * <br><br>
 * Therefore, based on this story, I have decided to use the Ohr HaChaim method to calculate the zmanim by default, since Rabbi Ovadiah Yosef ZT"L
 * was the posek for the Ohr HaChaim calendar, and he said that you don't need to be any more machmir than the Geonim's opinion on a fast day even
 * outside of Israel. It seems to me that Rabbi Shlomo Benizri Shlita's opinion that the Ohr HaChaim calendar should be used anywhere in the world is
 * more in line with Rabbi Ovadiah Yosef ZT"L's opinion.
 * <br><br>
 * Methods used to replicate the Ohr Hachaim calendar's zmanim:
 * {@link #getAlos72Zmanis()}
 * <br>
 * {@link #getEarliestTalitTefilin()} not in KosherJava, but implemented by this class.
 * <br>
 * {@link #getHaNetz()} gets the sunrise times from chaitables, otherwise we should default to {@link #getSeaLevelSunrise()} since it is similar to visible sunrise.
 * <br>
 * {@link #getSofZmanShmaMGA72MinutesZmanis()}
 * <br>
 * {@link #getSofZmanShmaGRA()}
 * <br>
 * {@link #getSofZmanTfilaMGA72MinutesZmanis()} for sof zman achilat chametz.
 * <br>
 * {@link #getSofZmanTfilaGRA()}
 * <br>
 * {@link #getSofZmanBiurChametzMGA()} for sof zman biur chametz, not in kosherjava.
 * <br>
 * {@link #getChatzot()} reimplemented to use elevation,
 * even though it does not affect the time,
 * I did it this way just because the Ohr Hachaim calendar does the same.
 * <br>
 * {@link #getMinchaGedolaGreaterThan30()}
 * <br>
 * {@link #getMinchaKetana()}
 * <br>
 * {@link #getPlagHaminchaYalkutYosef()} not in kosherjava.
 * <br>
 * {@link #getCandleLighting()} reimplemented to use elevation.
 * Remember to change {@link #setAteretTorahSunsetOffset(double)} to 20 if you want to replicate
 * the exact times of the Ohr Hachaim calendar. Note that the calendar also shows 40 minutes before sunset.
 * <br>
 * {@link #getSunset()}
 * <br>
 * {@link #getTzeit()} Not in KosherJava as of now since it is a very early zman for Tzeit and
 * Eliyahu Hershfeld is concerned about how people will use the method.
 * <br>
 * {@link #getTzaisAteretTorah()} is used for the end time for Shabbat/Yom Tov. The method
 * {@link #setAteretTorahSunsetOffset(double)} is used to set the time to 30 minutes after sunset in
 * Israel and outside of Israel (as instructed by Rabbi Meir Gavriel Elbaz) the time is set to
 * 40 minutes (which it is set to by default)
 * <br>
 * {@link #getTzais72Zmanis()} is used for the zman of Rabbeinu Tam
 * <br>
 * {@link #getSolarMidnight()} is used for the zman of Chatzot Layla
 * <br> <br>
 * The Amudei Horaah calendar uses all the methods above, however, the Alot Hashachar and
 * Tzeit Hacochavim methods are changed to include a slight adjustment based on degrees, and two
 * additional zmanim are added in that calendar. The two additional zmanim are for Plag HaMincha
 * according to the Halacha Berurah (also known as the GRA's Plag HaMincha) and Tzeit HaCochavim
 * L'Chumra which is a zman that is only used for ending fasts and determining which day a baby is
 * born on for a brit milah. The Ohr Hachaim calendar did not include these two times because the
 * halacha berurah was not well known by the time the calendar was made and according to Rav Benizri
 * the zman for Tzeit HaCochavim L'Chumra is merely 20 minutes after sunset which is easy to calculate.
 * <br> <br>
 * See these methods for more information on the Amudei Horaah's zmanim:
 * <br>
 * {@link #getAlotAmudeiHoraah()},
 * {@link #getEarliestTalitTefilinAmudeiHoraah()},
 * {@link #getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah()},
 * {@link #getSofZmanAchilatChametzAmudeiHoraah()},
 * {@link #getSofZmanBiurChametzMGAAmudeiHoraah()},
 * {@link #getTzeitAmudeiHoraah()},
 * {@link #getTzeitAmudeiHoraahLChumra()},
 * {@link #getTzaitShabbatAmudeiHoraah()},
 * {@link #getTzais72ZmanisAmudeiHoraah()},
 * {@link #getTzais72ZmanisAmudeiHoraahLkulah()}
 * <br>
 * Created by EJ on 9/13/2021.
 */
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

    /**
     * This method returns the earliest time that your are allowed to put on your Talit and Tefilin, otherwise known as Misheyakir. This is calculated by taking the
     * {@link #getAlos72Zmanis()} method and adding 6 Dakot Zmaniyot to it. This is the same calculation that is used in the Ohr HaChaim calendar.
     * @return the earliest time that your are allowed to put on your Talit and Tefilin based on the Ohr HaChaim calendar. 66 minutes before sunrise.
     */
    public Date getEarliestTalitTefilin() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getAlos72Zmanis(), (6 * dakahZmanit));//use getTimeOffset to handle nulls
    }

    /**
     * This method returns the earliest time that your are allowed to put on your Talit and Tefilin, otherwise known as Misheyakir. This is calculated by taking the
     * {@link #getElevationAdjustedSunrise()} method and subtracting 1 zmaniyot hour from it.
     * @return the earliest time that your are allowed to put on your Talit and Tefilin. 60 seasonal minutes before sunrise.
     */
    public Date getEarliestProperTalitTefilin() {
        return getTimeOffset(getElevationAdjustedSunrise(), -getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset()));//use getTimeOffset to handle nulls
    }

    /**
     * This method returns the time for Visible Sunrise calculated by the Chai Tables website. This is the same calculation that is used in the
     * Ohr HaChaim calendar. If the date is out of scope of the file which lasts for 1 jewish year, then the method will return null.
     * @return the time for Visible Sunrise calculated by the Chai Tables website, otherwise null.
     */
    public Date getHaNetz() {
        try {
            jewishCalendar.setDate(getCalendar());
            ChaiTables chaiTables = new ChaiTables(externalFilesDir, getGeoLocation().getLocationName(), jewishCalendar);

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
    /**
     * This method returns the time for Visible Sunrise calculated by the Chai Tables website for the given location.
     * This is the same calculation that is used in the Ohr HaChaim calendar. If the date is out of scope of the file which lasts for 1 jewish year,
     * then the method will return null.
     * @param locationName the name of the location to use for the filename.
     * @return the time for Visible Sunrise calculated by the Chai Tables website for the given location passed in, otherwise null.
     */
    public Date getHaNetz(String locationName) {
        try {
            jewishCalendar.setDate(getCalendar());
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

    /**
     * This method returns the time for the latest time to do Biur Chametz (Burning Chametz). The calculation is based on the Ohr HaChaim calendar
     * which uses 5 zmaniyot hours into the day based on the MG"A.
     * @return the time for the latest time to do Biur Chametz (Burning Chametz) based on the Ohr HaChaim calendar.
     */
    public Date getSofZmanBiurChametzMGA() {
        long shaahZmanit = getTemporalHour(getAlos72Zmanis(), getTzais72Zmanis());
        return getTimeOffset(getAlos72Zmanis(), shaahZmanit * 5);
    }

    /**
     * Returns mid-day but with elevation included if set to true. The {@link #getSunTransit()} method uses sea level sunrise and sunset. Whereas, all
     * of the zmanim in the Ohr HaChaim use the sunrise and sunset adjusted for elevation. This method allows the option of using elevation.
     * This has almost no effect on the time for mid-day.
     * @return mid-day but with elevation included if set, and setUseElevation() was set to true.
     */
    public Date getChatzot() {
        Date chatzot = getSunTransit(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        if (chatzot == null) {
            return getChatzos();
        }
        return chatzot;
    }

    /**
     * Yalkut Yosef holds that the time for Plag Hamincha is calculated by taking 1.25 "seasonal hours" (Sha'ot Zmaniot) from tzait hacochavim.
     * This is how the Ohr HaChaim calculates Plag Hamincha as well.
     * @return the time for Plag Hamincha as calculated by the Ohr HaChaim and Yalkut Yosef.
     */
    public Date getPlagHaminchaYalkutYosef() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getTzeit(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    /**
     * Yalkut Yosef holds that the time for Plag Hamincha is calculated by taking 1.25 "seasonal hours" (Sha'ot Zmaniot) from tzait hacochavim.
     * This is how Rabbi Dahan calculates Plag Hamincha in his Amudei Horaah calendar with his own algorithm for tzait hacochavim.
     * Note: The Amudei Horaah calendar provides both the Yalkut Yosef and Halacha Berurah times for Plag Hamincha. (No elevation adjustment is used)
     * @return the time for Plag Hamincha as calculated by the Amudei Horaah calendar and Yalkut Yosef.
     */
    public Date getPlagHaminchaYalkutYosefAmudeiHoraah() {
        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getTzeitAmudeiHoraah(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    /**
     * The Ohr Hachaim calendar uses elevation adjusted sunrise and sunset for all of its zmanim. This method reimplements the getCandleLighting method
     * to use the elevation adjusted sunset instead of sea level sunset.
     * @return the time for candle lighting as calculated by the Ohr HaChaim calendar.
     */
    @Override
    public Date getCandleLighting() {
        return getTimeOffset(getElevationAdjustedSunset(), -getCandleLightingOffset() * MILLISECONDS_PER_MINUTE);
    }

    /**
     * This method returns the time for tzait hacochavim (nightfall) calculated by the Ohr HaChaim calendar according to the opinion of the
     * Geonim. This is calculated as 13.5 zmaniyot minutes after elevated sunset.
     * @return the time for tzait hacochavim (nightfall) calculated by the Ohr HaChaim calendar according to the opinion of the Geonim.
     */
    public Date getTzeit() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getElevationAdjustedSunset(),(13 * dakahZmanit) + (dakahZmanit / 2));
    }

    /**
     * This method returns the time for tzait hacochavim (nightfall) l'chumra calculated by the Ohr HaChaim calendar according to the opinion of Rabbi
     * Ovadiah Yosef. This is calculated as 20 regular minutes after elevated sunset.
     * Rabbi Ovadiah Yosef writes that the average fast ends around 20 minutes after sunset. Rabbi Shlomo Benizri is of the opinion that this time is
     * calculated as 20 regular minutes after sunset. This is what the Ohr HaChaim refers to when it just writes that the fast ends at Tzait Hacochavim.
     * It could mean 13.5 zmaniyot minutes after sunset, or 20 regular minutes after sunset.
     * @return the time when the average fast ends based on the opinion of Rabbi Shlomo Benizri who holds that it is 20 regular minutes after sunset L'Chumra.
     */
    public Date getTzaitTaanit() {
        return getTimeOffset(getElevationAdjustedSunset(), (20 * MILLISECONDS_PER_MINUTE));
    }

    /**
     * This method returns the time when the average fast ends based on the opinion of Chacham Ben Tzion Abba Shaul. This is calculated as 30 minutes
     * after sunset. This zman is not shown in either the Ohr Hachaim or Amudei Horaah calendars, however, I added it because many communities use this
     * time for the end of the fast outside of Israel.
     * @return the time when the average fast ends based on the opinion of Chacham Ben Tzion Abba Shaul. 30 minutes after sunset.
     */
    public Date getTzaitTaanitLChumra() {
        return getTimeOffset(getElevationAdjustedSunset(), (30 * MILLISECONDS_PER_MINUTE));
    }

    /**
     * This method overrides the {@link #getSolarMidnight()} method in the base class to return the time of solar midnight as calculated by the Ohr
     * HaChaim calendar. This is calculated as 6 * shaot zmaniyot after sunset. The only difference between this method and
     * the method in the base class is that this method uses elevation adjusted sunset always. It does not affect the time that much, but still, it is
     * how the Ohr HaChaim calendar calculates it.
     * @return the time of solar midnight (chatzot) as calculated by the Ohr HaChaim calendar.
     */
    @Override
    public Date getSolarMidnight() {
        ROZmanimCalendar clonedCal = (ROZmanimCalendar) clone();
        clonedCal.getCalendar().add(Calendar.DAY_OF_MONTH, 1);
        Date chatzotForTomorrow = clonedCal.getChatzot();
        Date chatzotForToday = getChatzot();

        if (chatzotForTomorrow == null || chatzotForToday == null) {
            return super.getSolarMidnight();
        }

        return getTimeOffset(getChatzot(), (chatzotForTomorrow.getTime() - chatzotForToday.getTime()) / 2);
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

    //Amudei Horaah methods start here

    /**
     * This method returns the time of alot hashachar (dawn) calculated by the Amudei Horaah calendar. While normally this is calculated as 72 zmaniyot
     * minutes before sunrise, Rabbi Dahan says that the zmanim need to be adjusted for more northern/southern locations. He calculates the time as
     * zmaniyot minutes/seconds, however, he adjusts it based on the location and 16.1 degrees (72 zmaniyot minutes in Israel).
     * <p>
     * For example: If you wanted to calculate when alot is for NY, USA, you would first calculate the amount of regular minutes there are in an equinox
     * day between sunrise and 16.1 degrees before sunrise. In NY, this would lead you to around 80 minutes. You would then minus 80 zmaniyot minutes to
     * the time of sunrise to get the time of alot.
     * <p>
     * This is how Rabbi Dahan calculates the zmanim for alot and tzait in the Amudei Horaah calendar. This zman should NOT be used in Israel.
     *
     * @return the time of alot hashachar (dawn) calculated by the Amudei Horaah calendar by adjusting the zman based off of degrees. This zman
     * should NOT be used in Israel.
     */
    @Nullable
    public Date getAlotAmudeiHoraah() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.04, false);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        return getTimeOffset(getSeaLevelSunrise(), -(percentage * shaahZmanit));
    }

    /**
     * This method returns the time of misheyakir calculated by the Amudei Horaah calendar.
     * Rabbi Dahan calculates this zman for as 5/6 of the time between alot and sunrise in the Amudei Horaah calendar.
     * This zman should NOT be used in Israel.
     *
     * @return the time of misheyakir calculated by the Amudei Horaah calendar by adjusting the zman based off of degrees. This zman
     * should NOT be used in Israel.
     */
    public Date getEarliestTalitTefilinAmudeiHoraah() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.04, false);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        return getTimeOffset(getSeaLevelSunrise(), -(percentage * shaahZmanit) * 5 / 6);
    }

    // These methods are similar to the ones in the base class, but they use the Amudei Horaah zmanim instead of the regular zmanim
    public Date getSofZmanShmaMGA72MinutesZmanisAmudeiHoraah() {
        return getSofZmanShma(getAlotAmudeiHoraah(), getTzais72ZmanisAmudeiHoraah());
    }

    public Date getSofZmanAchilatChametzAmudeiHoraah() {
        return getSofZmanTfila(getAlotAmudeiHoraah(), getTzais72ZmanisAmudeiHoraah());
    }

    public Date getSofZmanBiurChametzMGAAmudeiHoraah() {
        long shaahZmanit = getTemporalHour(getAlotAmudeiHoraah(), getTzais72ZmanisAmudeiHoraah());
        return getTimeOffset(getAlotAmudeiHoraah(), shaahZmanit * 5);
    }

    /**
     * This method calculates the time for Nightfall according to the opinion of the Amudei Horaah Calendar. This is calculated as 13.5
     * adjusted zmaniyot minutes after sunset. This is based on the calculation of the 3.77&deg which is the time at 13.5 minutes in Netanya, Israel
     * on the equinox. Why Netanya and not Jerusalem? Because Netanya is the mid point between Israel and Iraq. Therefore, Rabbi Dahan equates them.
     * @return the Date representing 13.5 minutes zmaniyot after sunset adjusted to the users location using degrees based on Netanya, Israel. This zman
     * should NOT be used in Israel.
     */
    public Date getTzeitAmudeiHoraah() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(3.77, true);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        return getTimeOffset(getSeaLevelSunset(), percentage * shaahZmanit);
    }

    /**
     * This method returns another time for tzeit (l'chumra) according to the opinion of the Amudei Horaah Calendar. This time is calculated as 20
     * zmaniyot minutes after sunset adjusted to the users location using degrees. 5.135 degrees is 20 minutes after sunset in Netanya on the equinox.
     * We then calculate the number of minutes between sunset and this time on the equinox and multiply it by the zmaniyot minutes.
     * @return the Date representing 20 minutes zmaniyot after sunset adjusted to the users location using degrees. This zman
     * should NOT be used in Israel.
     */
    public Date getTzeitAmudeiHoraahLChumra() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(5.135, true);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        return getTimeOffset(getSeaLevelSunset(), percentage * shaahZmanit);
    }

    /**
     * This time is when the sun is 7.14° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). This calculation was provided by Rabbi Dahan himself.
     * The way Rabbi Dahan calculated this time for motzei shabbat was to find out at what degree would the sun be always 30 minutes or more after sunset
     * throughout the entire year. As Rabbi Ovadiah Yosef held that Shabbat ends after 30 minutes after sunset. Rabbi Dahan used degrees to calculate
     * when the sun is 30 minutes after sunset all year round.
     * @return the <code>Date</code> of 7.14° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). If the calculation can't be computed such as in the Arctic
     * Circle where there is at least one day a year where the sun does not rise, and one where it does not set, a null will be returned. See detailed
     * explanation on top of the {@link AstronomicalCalendar} documentation.
     */
    public Date getTzaitShabbatAmudeiHoraah() {
        Date tzait = getSunsetOffsetByDegrees(GEOMETRIC_ZENITH + 7.14);
        if (tzait != null) {
            if (getTzaitTaanit() != null && getTzaitTaanit().after(tzait)) { // if shabbat ends before 20 minutes after sunset, use 20 minutes
                return getTzaitTaanit();
            }
            if (getSolarMidnight().before(tzait)) { // if chatzot is before when shabbat ends, just use chatzot
                return getSolarMidnight();
            }
        }
        return tzait;
    }

    /**
     * Convenience method that returns the earlier of {@link #getTzaisAteretTorah()} and {@link #getTzaitShabbatAmudeiHoraah()}.
     * I created this method myself and it is not part of Rabbi Dahan's calendar. I created it because I believe that we do not need to follow the
     * degree based zman for Motzei Shabbat L'chumra. I believe that we can use the {@link #getTzaitShabbatAmudeiHoraah()} for Motzei Shabbat L'kulah,
     * however, we don't need to be so stringent to follow it L'chumra as well. 40 minutes is a good time for Motzei Shabbat all around the world,
     * and that is the default time used in {@link #getTzaisAteretTorah()}.
     * @return the earlier of {@link #getTzaisAteretTorah()} and {@link #getTzaitShabbatAmudeiHoraah()}.
     */
    public Date getTzaitShabbatAmudeiHoraahLesserThan40() {
        if (getTzaisAteretTorah() != null && getTzaitShabbatAmudeiHoraah() != null) {
            if (getTzaisAteretTorah().before(getTzaitShabbatAmudeiHoraah())) {//return the earlier of the two times
                return getTzaisAteretTorah();
            } else {
                return getTzaitShabbatAmudeiHoraah();
            }
        } else {
            return null;
        }
    }

    /**
     * This method returns the time for Rabbeinu Tam adjusted to the degrees of Netanya, Israel. The 16.01 degree is calculated as 72 minutes after
     * sunset in Netanya, Israel. This calculation was provided by Rabbi Dahan himself. The way Rabbi Dahan calculated this time was to find out at
     * what degree would the sun be 72 minutes after sunset on the equinox (March 17). Then he used that degree to calculate the time for 72 minutes
     * after sunset on the equinox for the current location. Then he got the minutes between sunset and that degree and used that to calculate the
     * zmaniyot minutes for the current location.
     * @return Rabbeinu Tam adjusted according to Rabbi Dahan's calculations in the Amudei Horaah. This zman
     * should NOT be used in Israel.
     */
    public Date getTzais72ZmanisAmudeiHoraah() {
        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.01, true);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getSeaLevelSunrise(), getSeaLevelSunset());
        return getTimeOffset(getSeaLevelSunset(), percentage * shaahZmanit);
    }

    /**
     * Convenience method that returns the earlier of {@link #getTzais72()} and {@link #getTzais72ZmanisAmudeiHoraah()}.
     * This is the time printed for Rabbeinu Tam in the Amudei Horaah for Motzei Shabbat every week.
     * Note: Rabbi Ovadiah ZT"L himself was machmir to follow Rabbeinu Tam for Motzei Shabbat/Chag bein lehakel bein lehachmir. No matter what the
     * time was he followed Rabbeinu tam and never used 72 regular minutes. Rabbi Dahan told me that many poskim argue on him and that we do not
     * need to be machmir for that extra time. I personally disagree with this decision, but I am not a posek. Just beware that this time is not
     * the time that Rabbi Ovadiah ZT"L himself followed, but his sons and many other poskim do follow this time.
     * @return the earlier of {@link #getTzais72()} and {@link #getTzais72ZmanisAmudeiHoraah()}.
     */
    public Date getTzais72ZmanisAmudeiHoraahLkulah() {
        if (getTzais72() != null && getTzais72ZmanisAmudeiHoraah() != null) {
            if (getTzais72().before(getTzais72ZmanisAmudeiHoraah())) {//return the earlier of the two times
                return getTzais72();
            } else {
                return getTzais72ZmanisAmudeiHoraah();
            }
        }
        return null;
    }

    //TODO Remove this method when it becomes available in future version of KosherJava
    /**
     * A utility method that returns the percentage of a <em>shaah zmanis</em> after sunset (or before sunrise) for a given degree
     * offset. For the <a href="https://kosherjava.com/2022/01/12/equinox-vs-equilux-zmanim-calculations/">equilux</a> where there
     * is a 720-minute day, passing 16.1&deg; for the location of Jerusalem will return about 1.2. This will work for any location
     * or date, but will typically only be of interest at the equinox/equilux to calculate the percentage of a <em>shaah zmanis</em>
     * for those who want to use the <a href="https://en.wikipedia.org/wiki/Abraham_Cohen_Pimentel">Minchas Cohen</a> in Ma'amar 2:4
     * and the <a href="https://en.wikipedia.org/wiki/Hezekiah_da_Silva">Pri Chadash</a> who calculate <em>tzais</em> as a percentage
     * of the day after sunset. While the Minchas Cohen only applies this to 72 minutes or a 1/10 of the day around the world (based
     * on the equinox / equilux in Israel, this method allows calculations for any degrees level for any location.
     *
     * @param degrees the number of degrees below the horizon after sunset.
     * @param sunset if <code>true</code> the calculation should be degrees after sunset, or if <code>false</code>, degrees before sunrise.
     * @return the <code>double</code> percentage of a <em>sha'ah zmanis</em> for a given set of degrees below the astronomical horizon
     * for the current calendar. If the calculation can't be computed a {@link Double#MIN_VALUE} will be returned. See detailed
     * explanation on top of the page.
     */
    public double getPercentOfShaahZmanisFromDegrees(double degrees, boolean sunset) {
        Date seaLevelSunrise = getSeaLevelSunrise();
        Date seaLevelSunset = getSeaLevelSunset();
        Date twilight;
        if (sunset) {
            twilight = getSunsetOffsetByDegrees(GEOMETRIC_ZENITH + degrees);
        } else {
            twilight = getSunriseOffsetByDegrees(GEOMETRIC_ZENITH + degrees);
        }
        if (seaLevelSunrise == null || seaLevelSunset == null || twilight == null) {
            return Double.MIN_VALUE;
        }
        double shaahZmanis = (seaLevelSunset.getTime() - seaLevelSunrise.getTime()) / 12.0;
        long riseSetToTwilight;
        if (sunset) {
            riseSetToTwilight = twilight.getTime() - seaLevelSunset.getTime();
        } else {
            riseSetToTwilight = seaLevelSunrise.getTime() - twilight.getTime();
        }
        return riseSetToTwilight / shaahZmanis;
    }

    // override super method to use chatzot based on sunrise and sunset
    @Override
    public Date getMinchaGedolaGreaterThan30() {
        Date minchaGedola30 = getTimeOffset(getChatzot(), MILLISECONDS_PER_MINUTE * 30);
        Date minchaGedola = getMinchaGedola();

        if (minchaGedola30 == null || minchaGedola == null) {
            return null; // no point in returning super.getMinchaGedolaGreaterThan30 because it is already checked above
        } else {
            return minchaGedola30.compareTo(minchaGedola) > 0 ? minchaGedola30
                    : minchaGedola;
        }
    }
}
