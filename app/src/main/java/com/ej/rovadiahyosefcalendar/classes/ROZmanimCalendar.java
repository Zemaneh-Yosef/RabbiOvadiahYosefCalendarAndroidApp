package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.SPACalculator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class extends the ZmanimCalendar class to add a few methods that are specific to the opinion of the Rabbi Ovadiah Yosef ZT"L.
 * The zmanim that are added are based on the zmanim published in the calendar "Luach Hamaor, Ohr HaChaim" which was created under the guidance of
 * Rabbi Ovadiah Yosef ZT"L, Rabbi Shlomo Benizri Shlita, and Rabbi Asher Darshan Shlita.
 * <br><br>
 * It is important to note that Rabbi Ovadiah Yosef ZT"L used the Ohr HaChaim calendar himself, and it is the only calendar that he
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
 * down with him, and he explained to me exactly how he calculated his zmanim, and I have implemented his calculations in this class as well.
 * <br><br>
 * I have also spoken with Rabbi Asher Darshan Shlita (programmer of the Ohr HaChaim Calendar) about this subject, and he said to ask Rabbi Shlomo
 * Benizri Shlita's opinion as he was the decision maker on the Ohr HaChaim calendar based on his talks with Rabbi Ovadiah Yosef ZT"L.
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
 * {@link #getMinchaKetanaGRA()}
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
    private static final long MINUTES_PER_HOUR = 60L;
    public List<Instant> vSunriseDates = new ArrayList<>();
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

    /**
     * The SPA calculator, shared by every instance (KosherJava's default is the NOAA calculator). It is much more expensive than NOAA on
     * Android (its Math.fma calls fall back to BigDecimal), and in KosherJava 3.0.0 it does not cache anything: every sunrise or sunset is
     * calculated again. So keep big batches of zmanim off the main thread (see ZmanimFragment#setNextUpcomingZman). It only holds its settings,
     * which are never changed, so sharing it between instances and threads is safe.
     */
    private static final SPACalculator SHARED_CALCULATOR = new SPACalculator();

    /**
     * Cache for {@link #getEquinoxPercentOfShaahZmanis(double, boolean)}. The result only depends on the location, the year, the degrees and
     * whether it is for sunrise or sunset, yet it was being recalculated for every Amudei Horaah zman.
     */
    private static final Map<String, Double> EQUINOX_PERCENT_CACHE = new ConcurrentHashMap<>();

    public ROZmanimCalendar(GeoLocation location) {
        super(location);
        super.setAstronomicalCalculator(SHARED_CALCULATOR);
        jewishCalendar = new JewishCalendar(getLocalDate());
        setUseElevation(true);
    }

    @SuppressWarnings("unchecked")
    private void loadVSunriseFile() {
        if (getGeoLocation().getLocationName() != null && getGeoLocation().getLocationName().isEmpty()) {
            return;
        }

        File vsFile = ChaiTablesWebJava.getVisibleSunriseFile(externalFilesDir, getGeoLocation().getLocationName(), jewishCalendar.getJewishYear());
        if (!vsFile.isFile()) {
            return;
        }

        List<Long> vSunriseTimes = Collections.emptyList();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(vsFile))) {
            vSunriseTimes = (List<Long>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        List<Instant> fresh = new ArrayList<>(vSunriseTimes.size());

        // Convert each seconds-since-midnight long into a Calendar
        for (Long seconds : vSunriseTimes) {
            fresh.add(Instant.ofEpochSecond(seconds));
        }
        vSunriseDates = fresh; // swap, never append
    }

    public boolean isUseAmudehHoraah() {
        return amudehHoraah;
    }

    public void setAmudehHoraah(boolean amudehHoraah) {
        this.amudehHoraah = amudehHoraah;
    }

    /**
     * Method to return <em>alos</em> (dawn) calculated using 72 minutes <em>zmaniyos</em> or 1/10th of the day before
     * sunrise. This is based on an 18-minute <em>Mil</em> so the time for 4 <em>Mil</em> is 72 minutes which is 1/10th
     * of a day (12 * 60 = 720) based on the day being from {@link #getSeaLevelSunrise() sea level sunrise} to
     * {@link #getSeaLevelSunrise sea level sunset} or {@link #getSunrise() sunrise} to {@link #getSunset() sunset}
     * (depending on the {@link #isUseElevation()} setting).
     * The actual calculation is {@link #getSeaLevelSunrise()} - ({@link #getShaahZmanisGRA()} * 1.2). This calculation
     * is used in the calendars published by the <a href=
     * "https://en.wikipedia.org/wiki/Central_Rabbinical_Congress">Hisachdus Harabanim D'Artzos Habris Ve'Canada</a>.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below the horizon on the equinox.
     *
     * @return the <code>Instant</code> representing the time. If the calculation can't be computed such as in the Arctic
     *         Circle where there is at least one day a year where the sun does not rise, and one where it does not set,
     *         a null will be returned. See detailed explanation on top of the {@link AstronomicalCalendar}
     *         documentation.
     * @see #getShaahZmanisGRA()
     */
    public Instant getAlotHashachar() {
        if (!amudehHoraah) {
            return getZmanisBasedOffset(-1.2);
        }
        return getAmudeiHoraahBeforeSunrise(1);
    }

    /**
     * This method returns the earliest time that you are allowed to put on your Talit and Tefilin according to the Pri Chadash,
     * otherwise known as Misheyakir. THIS TIME SHOULD ONLY BE USED FOR PEOPLE IN GREAT NEED. This is calculated by taking
     * 1.1 zmaniyot hours and subtracting it from sunrise.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below the horizon on the equinox.
     * @return the earliest time that you are allowed to put on your Talit and Tefilin IN GREAT NEED. 66 zmaniyot minutes before sunrise.
     */
    public Instant getMisheyakir66ZmaniyotMinutes() {
        if (!amudehHoraah) {
            return getZmanisBasedOffset(-1.1);
        }
        return getAmudeiHoraahBeforeSunrise(11.0 / 12.0);// 11 / 12 == 66 / 72
    }

    /**
     * This method returns the earliest time that you are allowed to put on your Talit and Tefilin L'chatchila, otherwise known as Misheyakir.
     * This is calculated by taking one zmaniyot hour and subtracting it from sunrise.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below the horizon on the equinox.
     * @return the earliest time that you are allowed to put on your Talit and Tefilin. 60 zmaniyot minutes before sunrise.
     */
    public Instant getMisheyakir60ZmaniyotMinutes() {
        if (!amudehHoraah) {
            return getZmanisBasedOffset(-1);
        }
        return getAmudeiHoraahBeforeSunrise(5.0 / 6.0);// 5 / 6 == 60 / 72
    }

    /**
     * This method returns the time for Visible Sunrise calculated by the Chai Tables website. This is the same calculation that is used in the
     * Ohr HaChaim calendar. If the date is out of scope of the file which lasts for 1 jewish year, then the method will return null.
     * @return the time for Visible Sunrise calculated by the Chai Tables website, otherwise null.
     */
    public Instant getHaNetz() {
        LocalDate date = getLocalDate();
        ZoneId zone = getGeoLocation().getZoneId();
        for (Instant vSunrise : vSunriseDates) {
            if (vSunrise.atZone(zone).toLocalDate().equals(date)) {
                return vSunrise;
            }
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
     * @return the <code>Instant</code> of the latest <em>zman krias shema</em>. If the calculation can't be computed such
     *         as in the Arctic Circle where there is at least one day a year where the sun does not rise, and one where
     *         it does not set, a null will be returned. See detailed explanation on top of the
     *         {@link AstronomicalCalendar} documentation.
     * @see #getAlotHashachar()
     */
    public Instant getSofZmanShmaMGA72MinutesZmanis() {
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
     * @return the <code>Instant</code> of the latest <em>zman krias shema</em>. If the calculation can't be computed such
     *         as in the Arctic Circle where there is at least one day a year where the sun does not rise, and one where
     *         it does not set, a null will be returned. See detailed explanation on top of the
     *         {@link AstronomicalCalendar} documentation.
     * @see #getAlotHashachar()
     */
    public Instant getSofZmanTfilaMGA72MinutesZmanis() {
        return getSofZmanTfila(getAlotHashachar(), getTzais72Zmanis());
    }

    /**
     * This method returns the time for the latest time to do Biur Chametz (Burning Chametz). The calculation is based on the Ohr HaChaim calendar
     * which uses 5 zmaniyot hours into the day based on the MG"A.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunrise and when the sun is 16.04° below (and above) the horizon on the equinox.
     * @return the time for the latest time to do Biur Chametz (Burning Chametz).
     */
    public Instant getSofZmanBiurChametzMGA() {
        Instant alos = getAlotHashachar();
        Duration shaahZmanit = getTemporalHour(alos, getTzais72Zmanis());
        return shaahZmanit == null ? null : getTimeOffset(alos, shaahZmanit.multipliedBy(5));
    }

    /**
     * Returns midday but with elevation included if set to true. The {@link #getSunTransit()} method uses sea level sunrise and sunset. Whereas, all
     * of the zmanim in the Ohr HaChaim use sunrise and sunset adjusted for elevation. This method allows the option of using elevation.
     * This has almost no effect on the time for midday.
     * @return midday but with elevation included if set, and setUseElevation() was set to true.
     */
    public Instant getChatzot() {
        Instant chatzot = getChatzos(getSunriseBasedOnElevationSetting(), getSunsetBasedOnElevationSetting());
        if (chatzot == null) {
            return getSunTransit();
        }
        return chatzot;
    }

    public Instant getMinchaGedolaGreaterThan30() {
        Instant minchaGedola30 = getTimeOffset(getChatzot(), Duration.of(30, ChronoUnit.MINUTES));
        Instant minchaGedola = getMinchaGedolaGRA();

        if (minchaGedola != null && minchaGedola30 != null) {
            return minchaGedola30.compareTo(minchaGedola) > 0 ? minchaGedola30 : minchaGedola;
        } else {
            return minchaGedola30;// should never be null, but even if it is, return it.
        }
    }

    /**
     * The Yalkut Yosef holds that the time for Plag Hamincha is calculated by taking 1.25 seasonal hours (based on sunrise to sunset) from tzait hacochavim.
     * This is how the Ohr HaChaim calendar calculates Plag HaMincha as well.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 3.7° below the horizon on the equinox.
     * @return the time for Plag HaMincha as calculated by the Yalkut Yosef.
     */
    public Instant getPlagHaminchaYalkutYosef() {
        Duration shaahZmanit = getTemporalHour(getSunriseBasedOnElevationSetting(), getSunsetBasedOnElevationSetting());
        if (shaahZmanit == null) {
            return null;
        }
        Duration dakahZmanit = shaahZmanit.dividedBy(MINUTES_PER_HOUR);
        return getTimeOffset(getTzeit(), dakahZmanit.multipliedBy(15).plus(shaahZmanit).negated());
    }

    @Override
    public Instant getSunriseBasedOnElevationSetting() {
        return super.getSunriseBasedOnElevationSetting();
    }

    @Override
    public Instant getSunsetBasedOnElevationSetting() {
        return super.getSunsetBasedOnElevationSetting();
    }

    /**
     * The Ohr Hachaim calendar uses elevation adjusted sunrise and sunset for all of its zmanim. This method reimplements the getCandleLighting method
     * to use the elevation adjusted sunset instead of sea level sunset.
     * @return the time for candle lighting as calculated by the Ohr HaChaim calendar.
     */
    @Override
    public Instant getCandleLighting() {
        long totalNanos = (long) (-getCandleLightingOffset() * MINUTE_NANOS);
        return getTimeOffset(getSunsetBasedOnElevationSetting(), Duration.ofNanos(totalNanos));
    }

    /**
     * This method returns the time for tzeit hacochavim (nightfall) calculated by the Ohr HaChaim calendar according to the opinion of the
     * Geonim. This is calculated as 13.5 zmaniyot minutes after elevated sunset.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 3.7° below the horizon on the equinox.
     * @return the time for tzeit hacochavim (nightfall) according to the opinion of the Geonim.
     */
    public Instant getTzeit() {
        if (!amudehHoraah) {
            return getZmanisBasedOffset(0.225); // 13.5 / 60
        }
        return getAmudeiHoraahAfterSunset(3.7);
    }

    /**
     * This method returns a time for tzeit l'chumra (stringent) as shown on the Amudei Horaah Calendar. This time is calculated as 20
     * zmaniyot minutes after sunset.
     * If {@link #isUseAmudehHoraah()} is true, the calculation will take into account the distance from the equator by
     * checking the time difference between sunset and when the sun is 5.075° below the horizon on the equinox.
     * @return the Instant representing 20 zmaniyot minutes after sunset. Adjusted to the users location using degrees if {@link #isUseAmudehHoraah()} is true.
     */
    public Instant getTzeitLChumra() {
        if (!amudehHoraah) {
            Duration shaahZmanis = getShaahZmanisGRA();
            if (shaahZmanis == null) {
                return null;
            }
            return getTimeOffset(getSunsetBasedOnElevationSetting(), shaahZmanis.dividedBy(MINUTES_PER_HOUR).multipliedBy(20));// avoid using getZmanisOffset because of the trailing 33333
        }
        return getAmudeiHoraahAfterSunset(5.075);
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
     * @return the <code>Instant</code> representing 40 minutes (configurable via {@link #setAteretTorahSunsetOffset})
     *         after sea level sunset. If the calculation can't be computed such as in the Arctic Circle where there is
     *         at least one day a year where the sun does not rise, and one where it does not set, a null will be
     *         returned. See detailed explanation on top of the {@link AstronomicalCalendar} documentation.
     * @see #getAteretTorahSunsetOffset()
     * @see #setAteretTorahSunsetOffset(double)
     */
    public Instant getTzaisAteretTorah() {
        long totalNanos = (long) (getAteretTorahSunsetOffset() * MINUTE_NANOS);
        return getTimeOffset(getSunsetBasedOnElevationSetting(), Duration.ofNanos(totalNanos));
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
     * 40 minutes. <em>Chacham</em> Yosef Harari-Raful of Yeshivat Ateret Torah uses 40 minutes globally except
     * Israel where a 25-minute offset is used. This 40 minute (or any other) offset can be overridden by this method.
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
     * @return the <code>Instant</code> representing the time. If the calculation can't be computed such as in the Arctic
     *         Circle where there is at least one day a year where the sun does not rise, and one where it does not set,
     *         a null will be returned. See detailed explanation on top of the {@link AstronomicalCalendar}
     *         documentation.
     * @see #getAlotHashachar()
     */
    public Instant getTzais72Zmanis() {
        if (!amudehHoraah) {
            return getTzais72ForceRegZmanis();
        }
        return getAmudeiHoraahAfterSunset(16.04);
    }

    public Instant getTzais72ForceRegZmanis() {
        return getZmanisBasedOffset(1.2);
    }

    /**
     * The halachic night is divided into 3 ashmoras (watches). Each ashmora is 2 seasonal hours, for a total of 6 seasonal hours.
     * This method returns the time for the second ashmora, which is two seasonal hours after sunset. Right now, this method is used to get the
     * earliest time possible to say selichot according to Rabbi David Yosef in Halacha Berurah.
     * @return the time for the second ashmora, which is two seasonal hours after sunset. A.K.A. earliest selichot time
     */
    public Instant getSecondAshmora() {
        ROZmanimCalendar clonedCal = getCopy();
        clonedCal.setLocalDate(getLocalDate().plusDays(1));
        Instant sunsetForToday = getSunsetBasedOnElevationSetting();
        Instant sunriseForTomorrow = clonedCal.getSunriseBasedOnElevationSetting();
        return getShaahZmanisBasedZman(sunsetForToday, sunriseForTomorrow, 4);
    }

    public boolean isNowBeforeSecondAshmora() {
        ZoneId zone = getGeoLocation().getZoneId();
        Instant now = Instant.now();
        Instant solarMidnight = getSolarMidnight();
        Instant secondAshmora = getSecondAshmora();
        // After 12 AM the calendar has already rolled to the new date. If solar midnight is also between 12 AM and 3 AM,
        // we need yesterday's second ashmora. If solar midnight is before 12 AM we are already looking at the right one.
        if (ZonedDateTime.ofInstant(now, zone).getHour() <= 3
                && solarMidnight != null
                && ZonedDateTime.ofInstant(solarMidnight, zone).getHour() <= 3) {
            ROZmanimCalendar yesterday = getCopy();
            yesterday.setLocalDate(getLocalDate().minusDays(1));
            secondAshmora = yesterday.getSecondAshmora();
        }
        return now.isBefore(secondAshmora == null ? now : secondAshmora);
    }

    public boolean isNowAfterHalachicSolarMidnight() {
        Instant now = Instant.now();
        Instant alotHashachar = getAlotHashachar();
        Instant solarMidnight = getSolarMidnight();
        if (alotHashachar == null) {
            alotHashachar = now;
        }
        if (solarMidnight == null) {
            solarMidnight = now;
        }
        if (now.isAfter(alotHashachar) && now.isBefore(solarMidnight)) {// the majority of cases, except when now and solar midnight are both after 12 AM
            return false;
        } else if (now.isBefore(alotHashachar)) {// the date has changed, so solar midnight is yesterday's and now could still be before it
            ROZmanimCalendar yesterday = getCopy();
            yesterday.setLocalDate(getLocalDate().minusDays(1));
            Instant yesterdaysSolarMidnight = yesterday.getSolarMidnight();
            return yesterdaysSolarMidnight != null && now.isAfter(yesterdaysSolarMidnight);
        } else {// now is after alot hashachar and solar midnight, so it is definitely after solar midnight
            return true;
        }
    }

    /**
     * Overrides the getSolarMidnight method to use the halfway point between two mid-days instead of
     * the default NOAA calculation.
     * @return midnight between two mid-days or the default NOAA calculation if sunrise or sunset is null
     */
    @Override
    public Instant getSolarMidnight() {
        ROZmanimCalendar tomorrow = getCopy();
        tomorrow.setLocalDate(getLocalDate().plusDays(1));
        Instant chatzotToday = getChatzot();
        Instant chatzotTomorrow = tomorrow.getChatzot();
        if (getSunsetBasedOnElevationSetting() == null || getSunriseBasedOnElevationSetting() == null) {
            return super.getSolarMidnight();
        }
        return getTimeOffset(chatzotToday, Duration.between(chatzotToday, chatzotTomorrow).dividedBy(2));
    }

    public void setExternalFilesDir(File externalFilesDir) {
        this.externalFilesDir = externalFilesDir;
        loadVSunriseFile();
    }

    @Override
    public void setLocalDate(LocalDate localDate) {
        int curYear = 0;
        if (jewishCalendar != null) {
            curYear = jewishCalendar.getJewishYear();
        }

        super.setLocalDate(localDate);
        if (getLocalDate() != null && jewishCalendar != null) {
            jewishCalendar.setGregorianDate(getLocalDate());
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
     * @return the <code>Instant</code> of 7.165° below {@link #GEOMETRIC_ZENITH geometric zenith} (90°). If the calculation can't be computed such as in the Arctic
     * Circle where there is at least one day a year where the sun does not rise, and one where it does not set, a null will be returned. See detailed
     * explanation on top of the {@link AstronomicalCalendar} documentation.
     */
    public Instant getTzeitShabbatAmudeiHoraah() {
        Instant tzait = getSunsetOffsetByDegrees(GEOMETRIC_ZENITH + 7.165);
        if (tzait != null) {
            Instant tzait20 = getTimeOffset(getSunsetBasedOnElevationSetting(), Duration.of(20, ChronoUnit.MINUTES));
            if (tzait20 != null && tzait20.isAfter(tzait)) { // if shabbat ends before 20 minutes after sunset, use 20 minutes
                return tzait20;
            }
            if (getSolarMidnight().isBefore(tzait)) { // if chatzot is before when shabbat ends, just use chatzot
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
    public Instant getTzeitShabbatAmudeiHoraahLesserThan40() {
        if (getTzaisAteretTorah() != null && getTzeitShabbatAmudeiHoraah() != null) {
            if (getTzaisAteretTorah().isBefore(getTzeitShabbatAmudeiHoraah())) {//return the earlier of the two times
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
     * Convenience method that returns the earlier of {@link #getTzais72Minutes()} and {@link #getTzais72Zmanis()}.
     * This is the time printed for Rabbeinu Tam in the Amudei Horaah for Motzei Shabbat every week.
     * Note: Rabbi Ovadiah ZT"L himself was machmir to follow Rabbeinu Tam for Motzei Shabbat/Chag bein lehakel bein lehachmir. No matter what the
     * time was he followed Rabbeinu tam and never used 72 regular minutes. Rabbi Dahan told me that many poskim (including his sons) argue on him
     * and that we do not need to be machmir for that extra time especially when it will take so long in more northern locations.
     * @return the earlier of {@link #getTzais72Minutes()} and {@link #getTzais72Zmanis()}.
     */
    public Instant getTzais72ZmanisAmudeiHoraahLkulah() {
        if (getTzais72Minutes() != null && getTzais72Zmanis() != null) {
            if (getTzais72Minutes().isBefore(getTzais72Zmanis())) {//return the earlier of the two times
                return getTzais72Minutes();
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
    public Duration getShaahZmanis72MinutesZmanis() {
        return getTemporalHour(getAlotHashachar(), getTzais72Zmanis());
    }

    private Duration getAmudeiHoraahOffset(double degrees, boolean sunset, double fraction) {
        Double percentage = getEquinoxPercentOfShaahZmanis(degrees, sunset);
        Duration shaahZmanis = getShaahZmanisGRA();
        if (percentage == null || shaahZmanis == null) {
            return null;
        }
        return Duration.ofNanos(Math.round(shaahZmanis.toNanos() * percentage * fraction));
    }

    private Instant getAmudeiHoraahBeforeSunrise(double fraction) {
        Duration offset = getAmudeiHoraahOffset(16.04, false, fraction);
        return offset == null ? null : getTimeOffset(getSunriseBasedOnElevationSetting(), offset.negated());
    }

    private Instant getAmudeiHoraahAfterSunset(double degrees) {
        Duration offset = getAmudeiHoraahOffset(degrees, true, 1);
        return offset == null ? null : getTimeOffset(getSunsetBasedOnElevationSetting(), offset);
    }

    /**
     * Percentage of a shaah zmanis for a dip of the given degrees, measured on the equinox (March 17).
     * @return the percentage, or null if it can't be computed
     */
    private Double getEquinoxPercentOfShaahZmanis(double degrees, boolean sunset) {
        GeoLocation geoLocation = getGeoLocation();
        int year = getLocalDate().getYear();
        String key = geoLocation.getLatitude() + "," + geoLocation.getLongitude() + "," + geoLocation.getZoneId().getId()
                + "," + year + "," + degrees + "," + sunset;// sea level based, so elevation does not matter
        Double cached = EQUINOX_PERCENT_CACHE.get(key);
        if (cached == null) {
            ROZmanimCalendar equinox = getCopy();
            equinox.setLocalDate(LocalDate.of(year, Month.MARCH, 17));
            cached = equinox.getPercentOfShaahZmanisFromDegrees(degrees, sunset);
            EQUINOX_PERCENT_CACHE.put(key, cached);
        }
        return cached == Double.MIN_VALUE ? null : cached;
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
        copy.setLocalDate(getLocalDate());
        return copy;
    }
}
