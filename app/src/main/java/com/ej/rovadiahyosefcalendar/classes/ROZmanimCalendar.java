package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
 * from a file that is used by the {@link ChaiTables} class. The {@link ChaiTablesScraper} class downloads the sunrise times from the ChaiTables website
 * and creates a file that contains the sunrise times for the current location.
 * <br><br>
 * All the other zmanim that were not included in KosherJava's zmanim calculator are calculated in this class.
 * <br><br>
 * On the subject of zmanim outside of Israel, Rabbi Ovadiah Yosef ZT"L never gave a definite psak on how to calculate zmanim outside of Israel, and
 * it is not so simple to say that the same way the Ohr HaChaim calendar calculates the zmanim should be used also outside of Israel. The Ohr HaChaim
 * calendar calculates the zmanim based on the Minchat Cohen that used Shaot Zmaniyot. This works well in Israel, however, even the Minchat Cohen
 * agrees that outside of Israel there should be a slight adjustment to the times for alot and tzait because of the curvature of the earth.
 * <br><br>
 * Therefore, Rabbi Leeor Dahan Shlita, who the authored the Amudei Horaah calendar (and sefarim under the same name on the mishna berurah) uses
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
 * Tzeit Hacochavim methods are changed to include a slight adjustment based on degrees, and two
 * additional zmanim are added in that calendar. The two additional zmanim are for Plag HaMincha
 * according to the Halacha Berurah (also known as the GRA's Plag HaMincha) and Tzeit HaCochavim
 * L'Chumra which is a zman that is only used for ending fasts and determining which day a baby is
 * born on for a brit milah. The Ohr Hachaim calendar did not include these two times because (assumingly) the
 * Halacha Berurah was not well known by the time the calendar was made and according to Rav Benizri
 * the zman for Tzeit HaCochavim L'Chumra is merely 20 minutes after sunset which is easy to calculate.
 * <br><br>
 * See these methods for more information on the Amudei Horaah's zmanim:
 * <br>
 * {@link #getTzeitAmudeiHoraahLChumra()},
 * {@link #getTzaitShabbatAmudeiHoraah()},
 * {@link #getTzais72ZmanisAmudeiHoraahLkulah()}
 * <br>
 * Created by EJ on 9/13/2021.
 */
public class ROZmanimCalendar extends ZmanimCalendar {

    private final JewishCalendar jewishCalendar;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MILLISECONDS_PER_MINUTE = 60_000;
    private File externalFilesDir;
    private Date visibleSunriseDate;

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
     * This method returns the earliest time that your are allowed to put on your Talit and Tefilin, otherwise known as Misheyakir. This is calculated by taking the
     * {@link #getAlotHashachar()} method and adding 6 Dakot Zmaniyot to it. This is the same calculation that is used in the Ohr HaChaim calendar.
     * @return the earliest time that your are allowed to put on your Talit and Tefilin based on the Ohr HaChaim calendar. 66 minutes before sunrise.
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
        return getTimeOffset(getElevationAdjustedSunrise(), -(percentage * shaahZmanit) * 11 / 12);
    }

    /**
     * This method returns the earliest time that your are allowed to put on your Talit and Tefilin, otherwise known as Misheyakir. This is calculated by taking the
     * {@link #getElevationAdjustedSunrise()} method and subtracting 1 zmaniyot hour from it.
     * @return the earliest time that your are allowed to put on your Talit and Tefilin. 60 seasonal minutes before sunrise.
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
        return getTimeOffset(getElevationAdjustedSunrise(), -(percentage * shaahZmanit) * 5 / 6);
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
     * @return the time for the latest time to do Biur Chametz (Burning Chametz) based on the Ohr HaChaim calendar.
     */
    public Date getSofZmanBiurChametzMGA() {
        long shaahZmanit = getTemporalHour(getAlotHashachar(), getTzais72Zmanis());
        return getTimeOffset(getAlotHashachar(), shaahZmanit * 5);
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
     * This method returns the time for tzait hacochavim (nightfall) calculated by the Ohr HaChaim calendar according to the opinion of the
     * Geonim. This is calculated as 13.5 zmaniyot minutes after elevated sunset. If AmudehHoraah is true, then this time is calculated
     * using the getPercentOfShaahZmanisFromDegrees method.
     * @return the time for tzait hacochavim (nightfall) calculated by the Ohr HaChaim calendar according to the opinion of the Geonim.
     */
    public Date getTzeit() {
        if (!amudehHoraah)
            return getZmanisBasedOffset(0.225); // 13.5 / 60

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
     * This method returns another time for tzeit (l'chumra) according to the opinion of the Amudei Horaah Calendar. This time is calculated as 20
     * zmaniyot minutes after sunset adjusted to the users location using degrees. 5.075 degrees is 20 minutes after sunset in Netanya on the equinox.
     * We then calculate the number of minutes between sunset and this time on the equinox and multiply it by the zmaniyot minutes.
     * @return the Date representing 20 minutes zmaniyot after sunset adjusted to the users location using degrees. This zman
     * should NOT be used in Israel.
     */
    public Date getTzeitAmudeiHoraahLChumra() {
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
        Date solarMidnight = getSolarMidnight();
        // Handle possible edge case when solarMidnight is "tomorrow"
        Calendar midnightCal = Calendar.getInstance();
        if (solarMidnight != null) {
            midnightCal.setTime(solarMidnight);
        }
        // The calendar changes at 12 AM. If solarMidnight occurs between 12 AM–3 AM and now is after 12 AM, we need to go back to yesterday to get the correct solarMidnight.
        // However, if solarMidnight occurs before 12 AM, there is no need to go back to yesterday because we are already checking for the correct solarMidnight.
        if (midnightCal.get(Calendar.HOUR_OF_DAY) < 3) {
            getCalendar().add(Calendar.DATE, -1);
            solarMidnight = getSolarMidnight();
            getCalendar().add(Calendar.DATE, 1);
        }
        return now.after(solarMidnight == null ? new Date() : solarMidnight);
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
    }

    public File getExternalFilesDir() {
        return this.externalFilesDir;
    }

    @Override
    public void setCalendar(Calendar calendar) {
        super.setCalendar(calendar);
        if (getCalendar() != null && jewishCalendar != null) {
            jewishCalendar.setDate(getCalendar());
        }
    }

    /**
     * This time is when the sun is 7.165° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). This calculation was provided by Rabbi Dahan himself.
     * The way Rabbi Dahan calculated this time for motzei shabbat was to find out at what degree would the sun be always 30 minutes or more after sunset
     * throughout the entire year. As Rabbi Ovadiah Yosef held that Shabbat ends after 30 minutes after sunset. Rabbi Dahan used degrees to calculate
     * when the sun is 30 minutes after sunset all year round.
     * @return the <code>Date</code> of 7.165° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). If the calculation can't be computed such as in the Arctic
     * Circle where there is at least one day a year where the sun does not rise, and one where it does not set, a null will be returned. See detailed
     * explanation on top of the {@link AstronomicalCalendar} documentation.
     */
    public Date getTzaitShabbatAmudeiHoraah() {
        Date tzait = getSunsetOffsetByDegrees(GEOMETRIC_ZENITH + 7.165);
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
            if (getTzaisAteretTorah() != null) {
                return getTzaisAteretTorah();
            } else {
                return getTzaitShabbatAmudeiHoraah();
            }
        }
    }

    /**
     * Convenience method that returns the earlier of {@link #getTzais72()} and {@link #getTzais72Zmanis()}.
     * This is the time printed for Rabbeinu Tam in the Amudei Horaah for Motzei Shabbat every week.
     * Note: Rabbi Ovadiah ZT"L himself was machmir to follow Rabbeinu Tam for Motzei Shabbat/Chag bein lehakel bein lehachmir. No matter what the
     * time was he followed Rabbeinu tam and never used 72 regular minutes. Rabbi Dahan told me that many poskim argue on him and that we do not
     * need to be machmir for that extra time. I personally disagree with this decision, but I am not a posek. Just beware that this time is not
     * the time that Rabbi Ovadiah ZT"L himself followed, but his sons and many other poskim do follow this time.
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
     * This method creates a copy of the roZmanimCalendar object in order for date changes to not affect the original object.
     * @return a copy of the roZmanimCalendar object
     */
    public ROZmanimCalendar getCopy() {
        ROZmanimCalendar copy = new ROZmanimCalendar(getGeoLocation());
        copy.setExternalFilesDir(getExternalFilesDir());
        copy.setCandleLightingOffset(getCandleLightingOffset());
        copy.setAteretTorahSunsetOffset(getAteretTorahSunsetOffset());
        copy.setAmudehHoraah(isUseAmudehHoraah());
        copy.setCalendar((Calendar) getCalendar().clone());
        return copy;
    }
}
