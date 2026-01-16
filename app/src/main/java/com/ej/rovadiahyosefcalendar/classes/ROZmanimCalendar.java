package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;

import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This class extends the ZmanimCalendar class to add a few methods that are specific to the opinion of the Rabbi Ovadiah Yosef ZT"L.
 * The zmanim that are added are based on the zmanim published in the calendar "Luach Hamaor, Ohr HaChaim" which was created under the guidance of
 * Rabbi Ovadiah Yosef ZT"L, Rabbi Shlomo Benizri Shlita, and Rabbi Asher Darshan Shlita.
 * <br><br>
 * It is important to note that Rabbi Ovadiah Yosef ZT"L used the Ohr HaChaim calendar himself and it is the only calendar that he
 * used. Therefore, it is a strong base of knowledge for what Rabbi Ovadiah Yosef ZT"L held to use for zmanim.
 * <br><br>
 * Thankfully, KosherJava's ZmanimCalendar class has all the zmanim and tools that are needed to calculate the zmanim in the
 * "Luach Hamaor, Ohr HaChaim" except for the zmanim that are based on visible sunrise. Visible sunrise is the exact time that the sun is visible
 * above the horizon. This time needs to take into account the horizon around the city. The {@link #getHaNetz()} method gets the visible sunrise
 * from a file that is gotten by the {@link ChaiTablesWebJava} class. The {@link ChaiTablesWebJava} class downloads the sunrise times from the ChaiTables website
 * and creates a file that contains the sunrise times for the current location.
 * <br><br>
 * All the other zmanim that were not included in KosherJava's zmanim calculator are calculated in this class.
 * <br><br>
 * For zmanim outside of Israel, Rabbi Ovadiah Yosef ZT"L never gave a definite psak on how to calculate zmanim outside of Israel, and
 * it is not so simple to say that the same way the Ohr HaChaim calendar calculates the zmanim should be used also outside of Israel. The Ohr HaChaim
 * calendar calculates the zmanim based on the Minchat Cohen that used Shaot Zmaniyot. This works well in Israel, however, even the Minchat Cohen
 * agrees that outside of Israel there should be a slight adjustment to the times for alot and tzait because of the curvature of the earth.
 * <br><br>
 * Therefore, Rabbi Leeor Dahan Shlita, who authored the Amudei Horaah calendar (and the sefarim under the same name on the Mishna Berurah) uses
 * degrees to adjust the times for the zmanim reliant on alot and tzait. I have sat
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
 * Rabbi Ovadiah Yosef ZT"L bases his zmanim on says to apply a deviation using degrees in Northern/Southern areas of the world. The Halacha Berurah
 * writes this clearly in Chelek 14, Siman 261, halacha 13. However, the Yalkut Yosef (Siman 293:1) seems to say like Rabbi Benizri.
 * <br><br>
 * UPDATE: We have since confirmed with Rabbi Yitchak Yosef (through Rabbi Meir Gavriel Elbaz) that the zmanim do need to be adjusted as
 * Rabbi Dahan has done in his calendar. There does not seem to be a machloket anymore.
 * <br><br>
 * Note: Rabbi Dahan used an Excel spreadsheet to calculate the zmanim for the Amudei Horaah calendar. I could not just copy the calculations from
 * the spreadsheet, because the spreadsheet uses different methods to calculate the zmanim than those found in KosherJava. However, I have talked
 * with Rabbi Dahan about his methodology and once I understood it, I was able to implement it in this class. I have also tested the zmanim based
 * on his calendars (with the seconds of the zmanim added) that he publishes, and I have gone over the zmanim with him personally to make sure that
 * they are correct.
 * <br><br>
 * Methods used to replicate the Ohr Hachaim calendar's zmanim:
 * {@link #getAlotHashachar()}
 * <br>
 * {@link #getMisheyakir66ZmaniyotMinutes()} not in KosherJava, but implemented by this class.
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
 * {@link #getChatzot()} reimplemented to use elevation, even though it does not affect the time, I did it this way just because the Ohr Hachaim
 * calendar does the same.
 * <br>
 * {@link #getMinchaGedolaGreaterThan30()}
 * <br>
 * {@link #getMinchaKetana()}
 * <br>
 * {@link #getPlagHaminchaYalkutYosef()} not in kosherjava.
 * <br>
 * {@link #getCandleLighting()} reimplemented to use elevation.
 * Remember to change {@link #setCandleLightingOffset(double)} to 20 if you want to replicate
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
 * <br><br>
 * The Amudei Horaah calendar uses all the methods above, however, the Alot Hashachar and
 * Tzeit Hacochavim methods are changed to include a slight adjustment based on degrees and the distance from the equator, and two
 * additional zmanim are added in that calendar. The two additional zmanim are for Plag HaMincha
 * according to the Halacha Berurah (also known as the GRA's Plag HaMincha) and Tzeit HaCochavim
 * L'Chumra which is a zman that is only used for ending fasts and determining which day a baby is
 * born on for a brit milah.
 * <br><br>
 * See these methods for more information on the Amudei Horaah's zmanim:
 * <br>
 * {@link #getTzeitLChumra()},
 * {@link #getTzeitShabbatAmudeiHoraah()},
 * {@link #getTzais72ZmanisAmudeiHoraahLkulah()}
 * <br>
 * Created by EJ on 9/13/2021.
 */
public class ROZmanimCalendar extends ZmanimCalendar {

    private final JewishCalendar jewishCalendar;
    private static final int MINUTES_PER_HOUR = 60;
    public static final int MILLISECONDS_PER_MINUTE = 60_000;
    public List<Calendar> vSunriseDates = new ArrayList<>();
    private File externalFilesDir;

    /**
     * Enable the calculations of the Amudeh Horaah calendar where applicable.
     */
    private boolean amudehHoraah = false;

    /**
     * The offset in minutes (defaults to 40) after sunset used for <em>tzeit</em> based on calculations of
     * <em>Chacham</em> Yosef Harari-Raful of Yeshivat Ateret Torah.
     * @see #getTzaisAteretTorah()
     * @see #getAteretTorahSunsetOffset()
     * @see #setAteretTorahSunsetOffset(double)
     */
    private double ateretTorahSunsetOffset = 40;

    public ROZmanimCalendar(GeoLocation location) {
        super(location);
        jewishCalendar = new JewishCalendar();
        setUseElevation(true);
    }

    @SuppressWarnings("unchecked")
    private void loadVSunriseFile() {
        if (sCurrentLocationName != null && sCurrentLocationName.isEmpty()) {
            return;
        }

        File vsFile = ChaiTablesWebJava.getVisibleSunriseFile(externalFilesDir, sCurrentLocationName, jewishCalendar.getJewishYear());
        if (!vsFile.isFile()) {
            return;
        }

        List<Long> vSunriseTimes = Collections.emptyList();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(vsFile))) {
            vSunriseTimes = (List<Long>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Convert each seconds-since-midnight long into a Calendar
        for (Long seconds : vSunriseTimes) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(seconds * 1000));  // convert seconds → millis
            vSunriseDates.add(cal);
        }
    }

    public boolean isUseAmudehHoraah() {
        return amudehHoraah;
    }

    public void setAmudehHoraah(boolean amudehHoraah) {
        this.amudehHoraah = amudehHoraah;
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

    /**
     * A utility method to return <em>alos</em> (dawn) or <em>tzais</em> (dusk) based on a fractional day offset.
     * @param hours the number of <em>shaaos zmaniyos</em> (temporal hours) before sunrise or after sunset that defines dawn
     *        or dusk. If a negative number is passed in, it will return the time of <em>alos</em> (dawn) (subtracting the
     *        time from sunrise) and if a positive number is passed in, it will return the time of <em>tzais</em> (dusk)
     *        (adding the time to sunset). If 0 is passed in, a null will be returned (since we can't tell if it is sunrise
     *        or sunset based).
     * @return the <code>Date</code> representing the time. If the calculation can't be computed such as in the Arctic
     *         Circle where there is at least one day a year where the sun does not rise, and one where it does not set,
     *         a null will be returned. A null will also be returned if 0 is passed in, since we can't tell if it is sunrise
     *         or sunset based. See detailed explanation on top of the {@link AstronomicalCalendar} documentation.
     */
    private Date getZmanisBasedOffset(double hours) {
        long shaahZmanis = getShaahZmanisGra();
        if (shaahZmanis == Long.MIN_VALUE || hours == 0) {
            return null;
        }

        if (hours > 0) {
            return getTimeOffset(getElevationAdjustedSunset(), (long) (shaahZmanis * hours));
        } else {
            return getTimeOffset(getElevationAdjustedSunrise(), (long) (shaahZmanis * hours));
        }
    }

    /**
     * Method to return <em>alos</em> (dawn) calculated using 72 minutes <em>zmaniyos</em> or 1/10th of the day before
     * sunrise. This is based on an 18-minute <em>Mil</em> so the time for 4 <em>Mil</em> is 72 minutes which is 1/10th
     * of a day (12 * 60 = 720) based on the a day being from {@link #getSeaLevelSunrise() sea level sunrise} to
     * {@link #getSeaLevelSunrise sea level sunset} or {@link #getSunrise() sunrise} to {@link #getSunset() sunset}
     * (depending on the {@link #isUseElevation()} setting).
     * The actual calculation is {@link #getSeaLevelSunrise()} - ({@link #getShaahZmanisGra()} * 1.2). This calculation
     * is used in the calendars published by the <a href=
     * "https://en.wikipedia.org/wiki/Central_Rabbinical_Congress">Hisachdus Harabanim D'Artzos Habris Ve'Canada</a>.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below the horizon on the equinox.
     *
     * @return the <code>Date</code> representing the time. If the calculation can't be computed such as in the Arctic
     *         Circle where there is at least one day a year where the sun does not rise, and one where it does not set,
     *         a null will be returned. See detailed explanation on top of the {@link AstronomicalCalendar}
     *         documentation.
     * @see #getShaahZmanisGra()
     */
    public Date getAlotHashachar() {
        if (!amudehHoraah)
            return getZmanisBasedOffset(-1.2);

        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.04, false);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        return getTimeOffset(getElevationAdjustedSunrise(), -(percentage * shaahZmanit));
    }

    /**
     * This method returns the earliest time that your are allowed to put on your Talit and Tefilin according to the Pri Chadash,
     * otherwise known as Misheyakir. THIS TIME SHOULD ONLY BE USED FOR PEOPLE IN GREAT NEED. This is calculated by taking
     * 1.1 zmaniyot hours and subtracting it from sunrise.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below the horizon on the equinox.
     * @return the earliest time that your are allowed to put on your Talit and Tefilin IN GREAT NEED. 66 zmaniyot minutes before sunrise.
     */
    public Date getMisheyakir66ZmaniyotMinutes() {
        if (!amudehHoraah)
            return getZmanisBasedOffset(-1.1);

        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.04, false);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        return getTimeOffset(getElevationAdjustedSunrise(), -(percentage * shaahZmanit) * 11 / 12);// 11 / 12 == 66 / 72
    }

    /**
     * This method returns the earliest time that your are allowed to put on your Talit and Tefilin L'chatchila, otherwise known as Misheyakir.
     * This is calculated by taking one zmaniyot hour and subtracting it from sunrise.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below the horizon on the equinox.
     * @return the earliest time that your are allowed to put on your Talit and Tefilin. 60 zmaniyot minutes before sunrise.
     */
    public Date getMisheyakir60ZmaniyotMinutes() {
        if (!amudehHoraah)
            return getZmanisBasedOffset(-1);

        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.04, false);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        return getTimeOffset(getElevationAdjustedSunrise(), -(percentage * shaahZmanit) * 5 / 6);// 5 / 6 == 60 / 72
    }

    /**
     * This method returns the time for Visible Sunrise calculated by the Chai Tables website. This is the same calculation that is used in the
     * Ohr HaChaim calendar. If the date is out of scope of the file which lasts for 1 jewish year, then the method will return null.
     * @return the time for Visible Sunrise calculated by the Chai Tables website, otherwise null.
     */
    public Date getHaNetz() {
        jewishCalendar.setDate(getCalendar());
        for (Calendar vSunriseDate : vSunriseDates) {
            if (vSunriseDate.get(Calendar.ERA) == jewishCalendar.getGregorianCalendar().get(Calendar.ERA) &&
                vSunriseDate.get(Calendar.YEAR) == jewishCalendar.getGregorianCalendar().get(Calendar.YEAR) &&
                vSunriseDate.get(Calendar.DAY_OF_YEAR) == jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_YEAR))
                return vSunriseDate.getTime();
        }

        return null;
    }

    /**
     * This method returns the latest <em>zman krias shema</em> (time to recite <em>Shema</em> in the morning) according
     * to the opinion of the <a href="https://en.wikipedia.org/wiki/Avraham_Gombinern">Magen Avraham (MGA)</a> based
     * on <em>alos</em> being {@link #getAlotHashachar() 72} minutes <em>zmaniyos</em>, or 1/10th of the day before
     * {@link #getSunrise() sunrise}. This time is 3 shaot zemaniyot (solar hours) after {@link #getAlotHashachar() dawn}
     * based on the opinion of the MGA that the day is calculated from a {@link #getAlotHashachar() dawn} of 72 minutes
     * <em>zmaniyos</em>, or 1/10th of the day before {@link #getSeaLevelSunrise() sea level sunrise} to
     * {@link #getTzais72Zmanis() nightfall} of 72 minutes <em>zmaniyos</em> after {@link #getSeaLevelSunset() sea level
     * sunset}.
     *
     * @return the <code>Date</code> of the latest <em>zman krias shema</em>. If the calculation can't be computed such
     *         as in the Arctic Circle where there is at least one day a year where the sun does not rise, and one where
     *         it does not set, a null will be returned. See detailed explanation on top of the
     *         {@link AstronomicalCalendar} documentation.
     * @see #getAlotHashachar()
     */
    public Date getSofZmanShmaMGA72MinutesZmanis() {
        return getSofZmanShma(getAlotHashachar(), getTzais72Zmanis());
    }

    /**
     * This method returns the latest <em>zman tfila</em> (time to the morning prayers) according to the opinion of the
     * <a href="https://en.wikipedia.org/wiki/Avraham_Gombinern">Magen Avraham (MGA)</a> based on <em>alos</em>
     * being {@link #getAlotHashachar() 72} minutes <em>zmaniyos</em> before {@link #getSunrise() sunrise}. This time is 4
     * shaot zemaniyot (solar hours) after {@link #getAlotHashachar() dawn} based on the opinion of the MGA that the day is
     * calculated from a {@link #getAlotHashachar() dawn} of 72 minutes <em>zmaniyos</em> before sunrise to
     * {@link #getTzais72Zmanis() nightfall} of 72 minutes <em>zmaniyos</em> after sunset.
     *
     * @return the <code>Date</code> of the latest <em>zman krias shema</em>. If the calculation can't be computed such
     *         as in the Arctic Circle where there is at least one day a year where the sun does not rise, and one where
     *         it does not set, a null will be returned. See detailed explanation on top of the
     *         {@link AstronomicalCalendar} documentation.
     * @see #getAlotHashachar()
     */
    public Date getSofZmanTfilaMGA72MinutesZmanis() {
        return getSofZmanTfila(getAlotHashachar(), getTzais72Zmanis());
    }

    /**
     * This method returns the time for the latest time to do Biur Chametz (Burning Chametz). The calculation is based on the Ohr HaChaim calendar
     * which uses 5 zmaniyot hours into the day based on the MG"A.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below (and above) the horizon on the equinox.
     * @return the time for the latest time to do Biur Chametz (Burning Chametz).
     */
    public Date getSofZmanBiurChametzMGA() {
        long shaahZmanit = getTemporalHour(getAlotHashachar(), getTzais72Zmanis());
        return getTimeOffset(getAlotHashachar(), shaahZmanit * 5);
    }

    /**
     * Returns mid-day but with elevation included if set to true. The {@link #getSunTransit()} method uses sea level sunrise and sunset. Whereas, all
     * of the zmanim in the Ohr HaChaim use sunrise and sunset adjusted for elevation. This method allows the option of using elevation.
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

    public Date getMinchaGedolaGreaterThan30() {
        Date minchaGedola30 = getTimeOffset(getChatzot(), MILLISECONDS_PER_MINUTE * 30);
        Date minchaGedola = getMinchaGedola();

        if (minchaGedola != null && minchaGedola30 != null) {
            return minchaGedola30.compareTo(minchaGedola) > 0 ? minchaGedola30 : minchaGedola;
        } else return minchaGedola30;// should never be null, but even if it is, return it.
    }

    /**
     * The Yalkut Yosef holds that the time for Plag Hamincha is calculated by taking 1.25 seasonal hours (based on sunrise to sunset) from tzait hacochavim.
     * This is how the Ohr HaChaim calendar calculates Plag HaMincha as well.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 3.7° below the horizon on the equinox.
     * @return the time for Plag HaMincha as calculated by the Yalkut Yosef.
     */
    public Date getPlagHaminchaYalkutYosef() {
        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        long dakahZmanit = shaahZmanit / MINUTES_PER_HOUR;
        return getTimeOffset(getTzeit(), -(shaahZmanit + (15 * dakahZmanit)));
    }

    @Override
    public Date getElevationAdjustedSunrise() {
        return super.getElevationAdjustedSunrise();
    }

    @Override
    public Date getElevationAdjustedSunset() {
        return super.getElevationAdjustedSunset();
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
     * This method returns the time for tzeit hacochavim (nightfall) calculated by the Ohr HaChaim calendar according to the opinion of the
     * Geonim. This is calculated as 13.5 zmaniyot minutes after elevated sunset.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 3.7° below the horizon on the equinox.
     * @return the time for tzeit hacochavim (nightfall) according to the opinion of the Geonim.
     */
    public Date getTzeit() {
        if (!amudehHoraah) {
            return getZmanisBasedOffset(0.225); // 13.5 / 60
        }

        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(3.7, true);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        return getTimeOffset(getElevationAdjustedSunset(), percentage * shaahZmanit);
    }

    /**
     * This method returns a time for tzeit l'chumra (stringent) as shown on the Amudei Horaah Calendar. This time is calculated as 20
     * zmaniyot minutes after sunset.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 5.075° below the horizon on the equinox.
     * @return the Date representing 20 zmaniyot minutes after sunset. Adjusted to the users location using degrees if {@link #isUseAmudehHoraah()} is true.
     */
    public Date getTzeitLChumra() {
        if (!amudehHoraah) {
            return getTimeOffset(getElevationAdjustedSunset(), 20 * (getShaahZmanisGra() / MINUTES_PER_HOUR));// avoid using getZmanisOffset because of the trailing 33333
        }

        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(5.075, true);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        return getTimeOffset(getElevationAdjustedSunset(), percentage * shaahZmanit);
    }

    /**
     * This method returns <em>tzais</em> usually calculated as 40 minutes (configurable to any offset via
     * {@link #setAteretTorahSunsetOffset(double)}) after sunset. Please note that <em>Chacham</em> Yosef Harari-Raful
     * of Yeshivat Ateret Torah who uses this time, does so only for calculating various other <em>zmanai hayom</em>
     * such as <em>Sof Zman Krias Shema</em> and <em>Plag Hamincha</em>. His calendars do not publish a <em>zman</em>
     * for <em>Tzais</em>. It should also be noted that <em>Chacham</em> Harari-Raful provided a 25 minute <em>zman</em>
     * for Israel. This API uses 40 minutes year round in any place on the globe by default. This offset can be change
     *  by calling {@link #setAteretTorahSunsetOffset(double)}.
     *
     * @return the <code>Date</code> representing 40 minutes (configurable via {@link #setAteretTorahSunsetOffset})
     *         after sea level sunset. If the calculation can't be computed such as in the Arctic Circle where there is
     *         at least one day a year where the sun does not rise, and one where it does not set, a null will be
     *         returned. See detailed explanation on top of the {@link AstronomicalCalendar} documentation.
     * @see #getAteretTorahSunsetOffset()
     * @see #setAteretTorahSunsetOffset(double)
     */
    public Date getTzaisAteretTorah() {
        return getTimeOffset(getElevationAdjustedSunset(), getAteretTorahSunsetOffset() * MILLISECONDS_PER_MINUTE);
    }

    /**
     * Returns the offset in minutes after sunset used to calculate <em>tzais</em> based on the calculations of
     * <em>Chacham</em> Yosef Harari-Raful of Yeshivat Ateret Torah calculations. The default value is 40 minutes.
     * This affects most <em>zmanim</em>, since almost all zmanim use subset as part of their calculation.
     *
     * @return the number of minutes after sunset for <em>Tzait</em>.
     * @see #setAteretTorahSunsetOffset(double)
     */
    public double getAteretTorahSunsetOffset() {
        return ateretTorahSunsetOffset;
    }

    /**
     * Allows setting the offset in minutes after sunset for the Ateret Torah <em>zmanim</em>. The default if unset is
     * 40 minutes. <em>Chacham</em> Yosef Harari-Raful of Yeshivat Ateret Torah uses 40 minutes globally with the exception
     * of Israel where a 25 minute offset is used. This 40 minute (or any other) offset can be overridden by this method.
     * This offset impacts all Ateret Torah <em>zmanim</em>.
     *
     * @param ateretTorahSunsetOffset
     *            the number of minutes after sunset to use as an offset for the Ateret Torah <em>tzais</em>
     * @see #getAteretTorahSunsetOffset()
     */
    public void setAteretTorahSunsetOffset(double ateretTorahSunsetOffset) {
        this.ateretTorahSunsetOffset = ateretTorahSunsetOffset;
    }

    /**
     * Method to return <em>tzais</em> (dusk) calculated as 72 minutes zmaniyos, or 1/10th of the day after
     * {@link #getSeaLevelSunset() sea level sunset}. This is the way that the <a href=
     * "https://en.wikipedia.org/wiki/Abraham_Cohen_Pimentel">Minchas Cohen</a> in Ma'amar 2:4 calculates Rebbeinu Tam's
     * time of <em>tzeis</em>. It should be noted that this calculation results in the shortest time from sunset to
     * <em>tzais</em> being during the winter solstice, the longest at the summer solstice and 72 clock minutes at the
     * equinox. This does not match reality, since there is no direct relationship between the length of the day and
     * twilight. The shortest twilight is during the equinox, the longest is during the the summer solstice, and in the
     * winter with the shortest daylight, the twilight period is longer than during the equinoxes.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 16.04° below the horizon on the equinox.
     *
     * @return the <code>Date</code> representing the time. If the calculation can't be computed such as in the Arctic
     *         Circle where there is at least one day a year where the sun does not rise, and one where it does not set,
     *         a null will be returned. See detailed explanation on top of the {@link AstronomicalCalendar}
     *         documentation.
     * @see #getAlotHashachar()
     */
    public Date getTzais72Zmanis() {
        if (!amudehHoraah)
            return getTzais72ForceRegZmanis();

        Calendar tempCal = (Calendar) getCalendar().clone();
        setCalendar(new GregorianCalendar(getCalendar().get(Calendar.YEAR), Calendar.MARCH, 17));//set the calendar to the equinox

        double percentage = getPercentOfShaahZmanisFromDegrees(16.04, true);
        if (percentage == Double.MIN_VALUE) {
            return null;
        }
        setCalendar(tempCal);//reset the calendar to the current day

        long shaahZmanit = getTemporalHour(getElevationAdjustedSunrise(), getElevationAdjustedSunset());
        return getTimeOffset(getElevationAdjustedSunset(), percentage * shaahZmanit);
    }

    public Date getTzais72ForceRegZmanis() {
        return getZmanisBasedOffset(1.2);
    }

    /**
     * The halachic night is divided into 3 ashmoras (watches). Each ashmora is 2 seasonal hours, for a total of 6 seasonal hours.
     * This method returns the time for the second ashmora, which is two seasonal hours after sunset. Right now, this method is used to get the
     * earliest time possible to say selichot according to Rabbi David Yosef in Halacha Berurah.
     * @return the time for the second ashmora, which is two seasonal hours after sunset. A.K.A. earliest selichot time
     */
    public Date getSecondAshmora() {
        ROZmanimCalendar clonedCal = (ROZmanimCalendar) clone();
        clonedCal.getCalendar().add(Calendar.DATE, 1);
        Date sunsetForToday = getElevationAdjustedSunset();
        Date sunriseForTomorrow = clonedCal.getElevationAdjustedSunrise();
        return getShaahZmanisBasedZman(sunsetForToday, sunriseForTomorrow, 4);
    }

    public boolean isNowBeforeSecondAshmora() {
        Date now = new Date();
        Date solarMidnight = getSolarMidnight();
        Date secondAshmora = getSecondAshmora();
        // Handle possible edge case when solarMidnight is "tomorrow"
        Calendar midnightCal = Calendar.getInstance();
        if (midnightCal.get(Calendar.HOUR_OF_DAY) < 3) {// now is before 3 AM
            if (solarMidnight != null) {
                midnightCal.setTime(solarMidnight);
            }
            // The calendar changes at 12 AM. If solarMidnight occurs between 12 AM–3 AM and now is after 12 AM, we need to go back to yesterday to get the correct solarMidnight.
            // However, if solarMidnight occurs before 12 AM, there is no need to go back to yesterday because we are already checking for the correct solarMidnight.
            if (midnightCal.get(Calendar.HOUR_OF_DAY) < 3) {
                getCalendar().add(Calendar.DATE, -1);
                secondAshmora = getSecondAshmora();
                getCalendar().add(Calendar.DATE, 1);
            }
        }
        return now.before(secondAshmora == null ? new Date() : secondAshmora);
    }

    public boolean isNowAfterHalachicSolarMidnight() {
        Date now = new Date();
        Date alotHashachar = getAlotHashachar();
        Date solarMidnight = getSolarMidnight();
        if (alotHashachar == null) {
            alotHashachar = new Date();
        }
        if (solarMidnight == null) {
            solarMidnight = new Date();
        }
        // V2: figure out if it is after solar midnight based on alot hashachar
        if (now.after(alotHashachar) && now.before(solarMidnight)) {// this takes care of majority of cases except for when now is after 12 AM and solar midnight is after 12 AM
            return false;
        } else if (now.before(alotHashachar)) {// if it is before alot hashachar, the date has changed, solar midnight will be shown yesterday and now could still be before solar midnight
            getCalendar().add(Calendar.DATE, -1);
            solarMidnight = getSolarMidnight();
            getCalendar().add(Calendar.DATE, 1);
            return now.after(solarMidnight);
        } else {// now is after alot hashachar and solar midnight, it is definitely after solar midnight
            return true;
        }
    }

    public Date getSolarMidnight() {
        ROZmanimCalendar clonedCal = (ROZmanimCalendar) clone();
        clonedCal.getCalendar().add(Calendar.DATE, 1);
        Date chatzotForTomorrow = clonedCal.getChatzot();
        Date chatzotForToday = getChatzot();

        return getTimeOffset(getChatzot(), (chatzotForTomorrow.getTime() - chatzotForToday.getTime()) / 2);
    }

    public void setExternalFilesDir(File externalFilesDir) {
        this.externalFilesDir = externalFilesDir;
        loadVSunriseFile();
    }

    @Override
    public void setCalendar(Calendar calendar) {
        int curYear = 0;
        if (jewishCalendar != null) {
            curYear = jewishCalendar.getJewishYear();
        }

        super.setCalendar((Calendar) calendar.clone());
        if (getCalendar() != null && jewishCalendar != null) {
            jewishCalendar.setDate(getCalendar());
        }

        if (this.externalFilesDir != null && curYear != 0 && curYear != jewishCalendar.getJewishYear()) {
            loadVSunriseFile();
        }
    }

    /**
     * This time is when the sun is 7.165° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). The method for this calculation was taught to us
     * by Rabbi Dahan himself. The way Rabbi Dahan calculated this time for motzei shabbat was to find out at what degree would the sun always be
     * 30 minutes or more after sunset throughout the entire year. As Rabbi Ovadiah Yosef held that Shabbat ends 30 minutes after sunset in Israel.
     * Rabbi Dahan used degrees to calculate when the sun is 30 minutes after sunset all year round in the northern most point of Israel.
     * There is a hard bottom limit of 20 regular minutes after sunset as instructed by Rabbi Dahan. If halachic midnight occurs before the sun
     * reaches 7.165°, this method wil return halachic midnight as it does not make sense to go past that point.
     * @return the <code>Date</code> of 7.165° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). If the calculation can't be computed such as in the Arctic
     * Circle where there is at least one day a year where the sun does not rise, and one where it does not set, a null will be returned. See detailed
     * explanation on top of the {@link AstronomicalCalendar} documentation.
     */
    public Date getTzeitShabbatAmudeiHoraah() {
        Date tzait = getSunsetOffsetByDegrees(GEOMETRIC_ZENITH + 7.165);
        if (tzait != null) {
            Date tzait20 = getTimeOffset(getElevationAdjustedSunset(), (20 * MILLISECONDS_PER_MINUTE));
            if (tzait20 != null && tzait20.after(tzait)) { // if shabbat ends before 20 minutes after sunset, use 20 minutes
                return tzait20;
            }
            if (getSolarMidnight().before(tzait)) { // if chatzot is before when shabbat ends, just use chatzot
                return getSolarMidnight();
            }
        }
        return tzait;
    }

    /**
     * Convenience method that returns the earlier of {@link #getTzaisAteretTorah()} and {@link #getTzeitShabbatAmudeiHoraah()}.
     * I created this method myself and it is not part of Rabbi Dahan's calendar. I created it because I believe that we do not need to follow the
     * degree based zman for Motzei Shabbat L'chumra. I believe that we can use the {@link #getTzeitShabbatAmudeiHoraah()} for Motzei Shabbat L'kulah,
     * however, we don't need to be so stringent to follow it L'chumra as well. 40 minutes is good enough for Motzei Shabbat all around the world,
     * as instructed to me by Rabbi Meir Gavriel Elbaz, and that is the default time used in {@link #getTzaisAteretTorah()}.
     * @return the earlier of {@link #getTzaisAteretTorah()} and {@link #getTzeitShabbatAmudeiHoraah()}.
     */
    public Date getTzeitShabbatAmudeiHoraahLesserThan40() {
        if (getTzaisAteretTorah() != null && getTzeitShabbatAmudeiHoraah() != null) {
            if (getTzaisAteretTorah().before(getTzeitShabbatAmudeiHoraah())) {//return the earlier of the two times
                return getTzaisAteretTorah();
            } else {
                return getTzeitShabbatAmudeiHoraah();
            }
        } else {
            if (getTzaisAteretTorah() != null) {
                return getTzaisAteretTorah();
            } else {
                return getTzeitShabbatAmudeiHoraah();
            }
        }
    }

    /**
     * Convenience method that returns the earlier of {@link #getTzais72()} and {@link #getTzais72Zmanis()}.
     * This is the time printed for Rabbeinu Tam in the Amudei Horaah for Motzei Shabbat every week.
     * Note: Rabbi Ovadiah ZT"L himself was machmir to follow Rabbeinu Tam for Motzei Shabbat/Chag bein lehakel bein lehachmir. No matter what the
     * time was he followed Rabbeinu tam and never used 72 regular minutes. Rabbi Dahan told me that many poskim (including his sons) argue on him
     * and that we do not need to be machmir for that extra time especially when it will take so long in more northern locations.
     * @return the earlier of {@link #getTzais72()} and {@link #getTzais72Zmanis()}.
     */
    public Date getTzais72ZmanisAmudeiHoraahLkulah() {
        if (getTzais72() != null && getTzais72Zmanis() != null) {
            if (getTzais72().before(getTzais72Zmanis())) {//return the earlier of the two times
                return getTzais72();
            } else {
                return getTzais72Zmanis();
            }
        }
        return null;
    }

    /**
     * Method to return a <em>shaah zmanis</em> (temporal hour) according to the opinion of the <a href=
     * "https://en.wikipedia.org/wiki/Avraham_Gombinern">Magen Avraham (MGA)</a> based on <em>alos</em> being
     * {@link #getAlotHashachar() 72} minutes <em>zmaniyos</em> before {@link #getSunrise() sunrise}. This calculation
     * divides the day based on the opinion of the MGA that the day runs from dawn to dusk. Dawn for this calculation
     * is 72 minutes <em>zmaniyos</em> before sunrise and dusk is 72 minutes <em>zmaniyos</em> after sunset. This day
     * is split into 12 equal parts with each part being a <em>shaah zmanis</em>. This is identical to 1/10th of the day
     * from {@link #getSunrise() sunrise} to {@link #getSunset() sunset}.
     *
     * @return the <code>long</code> millisecond length of a <em>shaah zmanis</em>. If the calculation can't be computed
     *         such as in the Arctic Circle where there is at least one day a year where the sun does not rise, and one
     *         where it does not set, a {@link Long#MIN_VALUE} will be returned. See detailed explanation on top of the
     *         {@link AstronomicalCalendar} documentation.
     * @see #getAlotHashachar()
     * @see #getTzais72Zmanis()
     */
    public long getShaahZmanis72MinutesZmanis() {
        return getTemporalHour(getAlotHashachar(), getTzais72Zmanis());
    }

    /**
     * This method creates a copy of the ROZmanimCalendar object in order for date changes to not affect the original object.
     * @return a copy of this ROZmanimCalendar object
     */
    public ROZmanimCalendar getCopy() {
        ROZmanimCalendar copy = new ROZmanimCalendar(getGeoLocation());
        copy.vSunriseDates = this.vSunriseDates;
        copy.setCandleLightingOffset(getCandleLightingOffset());
        copy.setAteretTorahSunsetOffset(getAteretTorahSunsetOffset());
        copy.setAmudehHoraah(isUseAmudehHoraah());
        copy.setCalendar((Calendar) getCalendar().clone());
        return copy;
    }
}
