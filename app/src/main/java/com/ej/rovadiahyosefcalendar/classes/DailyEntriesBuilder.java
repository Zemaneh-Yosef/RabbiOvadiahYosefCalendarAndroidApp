package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.BIRCHAT_HALEVANA;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.BIRKAT_HACHAMAH;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.BURN_CHAMETZ;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.DAY_OF_OMER;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.DST;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.ELEVATION_VALUE;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.ERUV_TAVSHILIN;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.HALLEL_OR_CHATZI_HALLEL;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.LEAP_YEAR;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.MOON_STATUS;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.MUSIC;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.NINE_DAYS;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.PURIM_MESHULASH;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SEASONAL_PRAYER_CHANGES;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SHAAH_ZMANIT_GRA;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SHAAH_ZMANIT_MGA;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SHABBAT_MEVARCHIM;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SHEVUA_SHECHAL_BO;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SHMITA_YEAR;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.SPECIAL_DAY;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.TACHANUN;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.TEKUFA_LENGTH;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.TEKUFA_TIME;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.THREE_WEEKS;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.ULCHAPARAT_PESHA;
import static com.ej.rovadiahyosefcalendar.classes.ZmanimFactory.addZmanim;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ej.rovadiahyosefcalendar.R;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
import com.kosherjava.zmanim.util.ZmanimFormatter;

import org.shredzone.commons.suncalc.MoonTimes;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Builds the list of entries shown for the current day (sROZmanimCalendar / sJewishDateInfo): the info rows
 * (Tachanun, Birchat Halevana, seasonal prayer changes, etc.) and the zmanim themselves. Phones show the whole list
 * in the zmanim tab, while wide screens split it between the sidebar ({@link #infoOnly(List)}) and the zmanim tab
 * ({@link #timesOnly(List)}).
 */
public class DailyEntriesBuilder {

    private static final ZmanimFormatter ZMANIM_FORMATTER = new ZmanimFormatter(ZoneId.systemDefault());

    static {
        ZMANIM_FORMATTER.setTimeFormat(ZmanimFormatter.SEXAGESIMAL_SECONDS_FORMAT);
    }

    private DailyEntriesBuilder() {}

    public static List<ZmanListEntry> build(Context context, boolean add66MisheyakirZman) {
        List<ZmanListEntry> zmanim = new ArrayList<>();

        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", false)) {
            if (sJewishDateInfo.getJewishCalendar().isShabbosMevorchim()) {
                zmanim.add(new ZmanListEntry("שבת מברכים", SHABBAT_MEVARCHIM));
            }
        }

        String day = sJewishDateInfo.getSpecialDay(false);
        if (!day.isEmpty()) {
            zmanim.add(new ZmanListEntry(day, SPECIAL_DAY));
        }

        if (sJewishDateInfo.isEruvTavshilimMadeToday()) {
            zmanim.add(new ZmanListEntry(context.getString(R.string.eruv_tavshilin), ERUV_TAVSHILIN));
        }

        String dayOfOmer = sJewishDateInfo.addDayOfOmer("");
        if (!dayOfOmer.isEmpty()) {
            zmanim.add(new ZmanListEntry(dayOfOmer, DAY_OF_OMER));
        }

        if (sJewishDateInfo.getJewishCalendar().isRoshHashana() && sJewishDateInfo.isShmitaYear()) {
            zmanim.add(new ZmanListEntry(context.getString(R.string.this_year_is_a_shmita_year), SHMITA_YEAR));
        }

        if (sJewishDateInfo.is3Weeks()) {
            if (sJewishDateInfo.is9Days()) {
                if (sJewishDateInfo.isShevuahShechalBo()) {
                    zmanim.add(new ZmanListEntry(context.getString(R.string.shevuah_shechal_bo), SHEVUA_SHECHAL_BO));
                } else {
                    zmanim.add(new ZmanListEntry(context.getString(R.string.nine_days), NINE_DAYS));
                }
            } else {
                zmanim.add(new ZmanListEntry(context.getString(R.string.three_weeks), THREE_WEEKS));
            }
        }

        String isOKToListenToMusic = sJewishDateInfo.isOKToListenToMusic();
        if (!isOKToListenToMusic.isEmpty()) {
            zmanim.add(new ZmanListEntry(isOKToListenToMusic, MUSIC));
        }

        String hallel = sJewishDateInfo.getHallelOrChatziHallel();
        if (!hallel.isEmpty()) {
            zmanim.add(new ZmanListEntry(hallel, HALLEL_OR_CHATZI_HALLEL));
        }

        String ulChaparatPesha = sJewishDateInfo.getIsUlChaparatPeshaSaid();
        if (!ulChaparatPesha.isEmpty()) {
            zmanim.add(new ZmanListEntry(ulChaparatPesha, ULCHAPARAT_PESHA));
        }

        zmanim.add(new ZmanListEntry(sJewishDateInfo.getIsTachanunSaid(), TACHANUN));

        if (sJewishDateInfo.isPurimMeshulash()) {
            zmanim.add(new ZmanListEntry(context.getString(R.string.no_tachanun_in_yerushalayim), PURIM_MESHULASH));
        }

        String birchatLevana = sJewishDateInfo.getBirchatLevana(ZoneId.of(sCurrentTimeZoneID));
        if (!birchatLevana.isEmpty()) {
            zmanim.add(new ZmanListEntry(birchatLevana, BIRCHAT_HALEVANA));
            MoonTimes moonTimes = MoonTimes.compute()
                    .on(sCurrentDateShown)
                    .at(sLatitude, sLongitude)
                    .timezone(sCurrentTimeZoneID == null ? TimeZone.getDefault().getID() : sCurrentTimeZoneID)
                    .midnight()
                    .oneDay() // since we want to only show the moon times for the current day, set it to one day with this call. In V2 the default was one day, but in V3 the default is fullCycle (i.e. 365 days).
                    .execute();
            if (moonTimes.isAlwaysUp()) {
                zmanim.add(new ZmanListEntry(context.getString(R.string.the_moon_is_up_all_night), MOON_STATUS));
            } else if (moonTimes.isAlwaysDown()) {
                zmanim.add(new ZmanListEntry(context.getString(R.string.there_is_no_moon_tonight), MOON_STATUS));
            } else {
                DateTimeFormatter moonFormat = DateTimeFormatter.ofPattern((Utils.isLocaleHebrew(context) ? "H:mm" : "h:mm a"));
                String moonRiseSet = "";
                if (moonTimes.getRise() != null) {
                    moonRiseSet += context.getString(R.string.moonrise) + moonTimes.getRise().format(moonFormat);
                }
                if (moonTimes.getSet() != null) {
                    if (!moonRiseSet.isEmpty()) {
                        moonRiseSet += " - ";
                    }
                    moonRiseSet += context.getString(R.string.moonset) + moonTimes.getSet().format(moonFormat);
                }
                if (!moonRiseSet.isEmpty()) {
                    zmanim.add(new ZmanListEntry(moonRiseSet, MOON_STATUS));
                }
            }
        }

        if (sJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            zmanim.add(new ZmanListEntry(context.getString(R.string.birchat_hachamah_is_said_today), BIRKAT_HACHAMAH));
        }

        if (sJewishDateInfo.tomorrow().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY
                && sJewishDateInfo.tomorrow().getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            zmanim.add(new ZmanListEntry(context.getString(R.string.burn_your_ametz_today), BURN_CHAMETZ));
        }

        String tekufaOpinions = sSettingsPreferences.getString("TekufaOpinions", "1");
        switch (tekufaOpinions) {
            case "1":
                addTekufaTime(context, zmanim, false, sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false));
                break;
            case "2":
                addTekufaTime(context, zmanim, false, false);
                break;
            case "3":
                addTekufaTime(context, zmanim, false, true);
                break;
            default:// 4
                addTekufaTime(context, zmanim, false, true);
                addTekufaTime(context, zmanim, false, false);
                break;
        }
        addTekufaLength(context, zmanim, tekufaOpinions, sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false));

        addZmanim(zmanim, false, sSettingsPreferences, sSharedPreferences, sROZmanimCalendar, sJewishDateInfo, add66MisheyakirZman);

        zmanim.add(new ZmanListEntry(getSeasonalPrayerChanges(), SEASONAL_PRAYER_CHANGES));

        zmanim.add(new ZmanListEntry(context.getString(R.string.shaah_zmanit_gr_a) + " " + ZMANIM_FORMATTER.format(sROZmanimCalendar.getShaahZmanisGRA()), SHAAH_ZMANIT_GRA));
        zmanim.add(new ZmanListEntry(context.getString(R.string.mg_a)
                + " (" + context.getString(sROZmanimCalendar.isUseAmudehHoraah() ? R.string.amudei_horaah : R.string.ohr_hachaim) + ") "
                + ZMANIM_FORMATTER.format(sROZmanimCalendar.getShaahZmanis72MinutesZmanis()), SHAAH_ZMANIT_MGA));

        if (sSettingsPreferences.getBoolean("ShowLeapYear", false)) {
            zmanim.add(new ZmanListEntry(sJewishDateInfo.isJewishLeapYear(), LEAP_YEAR));
        }

        if (sSettingsPreferences.getBoolean("ShowDST", false)) {
            zmanim.add(new ZmanListEntry(getDSTStatus(context), DST));
        }

        if (sSettingsPreferences.getBoolean("ShowShmitaYear", false)) {
            switch (sJewishDateInfo.getYearOfShmitaCycle()) {
                case 1:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.first_year_of_shmita), SHMITA_YEAR));
                    break;
                case 2:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.second_year_of_shmita), SHMITA_YEAR));
                    break;
                case 3:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.third_year_of_shmita), SHMITA_YEAR));
                    break;
                case 4:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.fourth_year_of_shmita), SHMITA_YEAR));
                    break;
                case 5:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.fifth_year_of_shmita), SHMITA_YEAR));
                    break;
                case 6:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.sixth_year_of_shmita), SHMITA_YEAR));
                    break;
                default:
                    zmanim.add(new ZmanListEntry(context.getString(R.string.this_year_is_a_shmita_year), SHMITA_YEAR));
                    break;
            }
        }

        if (sSettingsPreferences.getBoolean("ShowElevation", false)) {
            zmanim.add(new ZmanListEntry(context.getString(R.string.elevation) + " " + sElevation + " " + context.getString(R.string.meters), ELEVATION_VALUE));
        }

        return zmanim;
    }

    /**
     * @return the zmanim and the seasonal hours (shown under the zmanim, like the website's "Seasonal Hour" card)
     */
    public static List<ZmanListEntry> timesOnly(List<ZmanListEntry> entries) {
        List<ZmanListEntry> times = new ArrayList<>();
        for (ZmanListEntry entry : entries) {
            if (isTimeEntry(entry)) {
                times.add(entry);
            }
        }
        return times;
    }

    /**
     * @return everything that is not a zman or a seasonal hour (Tachanun, Birchat Halevana, prayer changes, etc.), in order
     */
    public static List<ZmanListEntry> infoOnly(List<ZmanListEntry> entries) {
        List<ZmanListEntry> info = new ArrayList<>();
        for (ZmanListEntry entry : entries) {
            if (!isTimeEntry(entry)) {
                info.add(entry);
            }
        }
        return info;
    }

    private static boolean isTimeEntry(ZmanListEntry entry) {
        return entry.isZman()
                || entry.getZmanListEntryType() == SHAAH_ZMANIT_GRA
                || entry.getZmanListEntryType() == SHAAH_ZMANIT_MGA;
    }

    @NonNull
    private static String getDSTStatus(Context context) {
        ZoneId zoneId = sROZmanimCalendar.getGeoLocation().getZoneId();
        LocalDate currentDate = sROZmanimCalendar.getLocalDate();

        // atStartOfDay() safely handles edge cases where 00:00 is skipped due to a DST gap
        ZonedDateTime startOfDay = currentDate.atStartOfDay(zoneId);
        ZonedDateTime endOfDay = startOfDay.plusDays(1);

        boolean isDSTAtStartOfDay = zoneId.getRules().isDaylightSavings(startOfDay.toInstant());

        String dstStatus = isDSTAtStartOfDay
                ? context.getString(R.string.daylight_savings_time_is_on)
                : context.getString(R.string.daylight_savings_time_is_off);

        // Find the next transition starting from just before the start of the day
        ZoneOffsetTransition transition = zoneId.getRules().nextTransition(startOfDay.toInstant().minusNanos(1));

        // If a transition exists and occurs before the next day begins, it happens today
        if (transition != null && transition.getInstant().isBefore(endOfDay.toInstant())) {
            String transitionTime = DateTimeFormatter
                    .ofPattern(Utils.dateFormatPattern(context, sSettingsPreferences.getBoolean("ShowSeconds", false)))
                    .withZone(zoneId)
                    .format(transition.getInstant());

            String transitionText = Utils.isLocaleHebrew(context) ? " ב " : " at ";

            // Update the base string to reflect the transition
            dstStatus = (isDSTAtStartOfDay
                    ? context.getString(R.string.daylight_savings_time_is_off)
                    : context.getString(R.string.daylight_savings_time_is_on))
                    + transitionText + transitionTime;
        }

        return dstStatus;
    }

    @NonNull
    private static String getSeasonalPrayerChanges() {
        TefilaRules rules = new TefilaRules();
        if (rules.isMashivHaruachEndDate(sJewishDateInfo.getJewishCalendar())) {
            return "במוסף מוריד הטל / ברכנו";
        }
        if (rules.isMashivHaruachStartDate(sJewishDateInfo.getJewishCalendar())) {
            return "במוסף משיב הרוח / ברכנו";
        }
        if (rules.isVeseinTalUmatarStartDate(sJewishDateInfo.tomorrow().getJewishCalendar())) {
            return "בערבית משיב הרוח / ברך עלינו";
        }
        return sJewishDateInfo.getIsMashivHaruchOrMoridHatalSaid()
                + " / " +
                sJewishDateInfo.getIsBarcheinuOrBarechAleinuSaid();
    }

    /**
     * True if the tekufa (checked via the given accessor) falls on the same calendar day as
     * zmanimCalendarCopy's current date. Tekufa can land a day earlier than expected, so callers
     * should have already advanced the copy by one day before checking, per the existing pattern.
     */
    private static boolean isTekufaToday(ROZmanimCalendar zmanimCalendarCopy, Instant tekufaDate) {
        if (tekufaDate == null) return false;
        LocalDate localDate = zmanimCalendarCopy.getLocalDate();
        LocalDate localDate2 = tekufaDate.atZone(ZoneId.of(sCurrentTimeZoneID)).toLocalDate();
        return localDate.equals(localDate2);
    }

    /**
     * Adds the time for the Tekufa/Season Change to the list passed in as a string.
     * @param list the list of ZmanListEntry to add to
     * @param shortStyle if the tekufa should be added as "Tekufa Nissan : 4:30" or "Tekufa Nissan is today at 4:30"
     * @param amudeiHoraah method JewishCalendar::getTekufaAsDate or JewishCalendar::getAmudeiHoraahTekufaAsDate
     */
    public static void addTekufaTime(Context context, List<ZmanListEntry> list, boolean shortStyle, boolean amudeiHoraah) {
        DateTimeFormatter noSecondDateFormat = DateTimeFormatter.ofPattern(Utils.dateFormatPattern(context,false))
                .withLocale(context
                        .getResources()
                        .getConfiguration()
                        .getLocales()
                        .get(0))
                .withZone(TimeZone.getTimeZone(sCurrentTimeZoneID).toZoneId());
        ROZmanimCalendar zmanimCalendarCopy = sROZmanimCalendar.getCopy();
        JewishDateInfo jewishDateInfoCopy = sJewishDateInfo.getCopy();

        // Check tomorrow first (tekufa can land a day earlier than expected), then today.
        for (int dayOffset : new int[]{1, 0}) {
            zmanimCalendarCopy.setLocalDate(zmanimCalendarCopy.getLocalDate().plusDays(dayOffset));
            jewishDateInfoCopy.setLocalDate(zmanimCalendarCopy.getLocalDate());
            zmanimCalendarCopy.setLocalDate(zmanimCalendarCopy.getLocalDate().plusDays(-dayOffset));

            JewishCalendarWithExtraMethods jc = jewishDateInfoCopy.getJewishCalendar();
            if (jc.getTekufaAsInstant(amudeiHoraah) == null) continue;

            Instant tekufaDate = jc.getTekufaAsInstant(amudeiHoraah);
            if (!isTekufaToday(zmanimCalendarCopy, tekufaDate)) continue;

            String label = "תקופת " + jc.getTekufaName(Utils.isLocaleHebrew(context))
                    + (shortStyle ? " : " : " היום בשעה ") + noSecondDateFormat.format(tekufaDate);
            String labelEn = "Tekufa " + jc.getTekufaName(Utils.isLocaleHebrew(context))
                    + (shortStyle ? " : " : " is today at ") + noSecondDateFormat.format(tekufaDate);
            list.add(new ZmanListEntry(Utils.isLocaleHebrew(context) ? label : labelEn, TEKUFA_TIME));
        }
    }

    private static void addTekufaLength(Context context, List<ZmanListEntry> zmanim, String opinion, boolean amudeiHoraah) {
        DateTimeFormatter noSecondDateFormat = DateTimeFormatter.ofPattern(Utils.dateFormatPattern(context,false))
                .withLocale(context
                        .getResources()
                        .getConfiguration()
                        .getLocales()
                        .get(0))
                .withZone(TimeZone.getTimeZone(sCurrentTimeZoneID).toZoneId());

        Instant tekufa = findTodayTekufaDate(false);
        Instant aHTekufa = findTodayTekufaDate(true);

        if (tekufa == null || aHTekufa == null) return;

        Duration HALF_HOUR = Duration.ofMinutes(30);
        Instant start, end;
        switch (opinion) {
            case "1":
                Instant base = amudeiHoraah ? aHTekufa : tekufa;
                start = base.minus(HALF_HOUR);
                end = base.plus(HALF_HOUR);
                break;
            case "2":
                start = tekufa.minus(HALF_HOUR);
                end = tekufa.plus(HALF_HOUR);
                break;
            case "3":
                start = aHTekufa.minus(HALF_HOUR);
                end = aHTekufa.plus(HALF_HOUR);
                break;
            default:// 4
                start = aHTekufa.minus(HALF_HOUR);
                end = tekufa.plus(HALF_HOUR);
                break;
        }

        String startText = noSecondDateFormat.format(start);
        String endText = noSecondDateFormat.format(end);
        String range = Utils.isLocaleHebrew(context) ? endText + " - " + startText : startText + " - " + endText;
        zmanim.add(new ZmanListEntry(context.getString(R.string.tekufa_length) + range, TEKUFA_LENGTH));
    }

    /** Same tomorrow-then-today walk as addTekufaTimeUnified, but returns the Date instead of appending a ZmanListEntry. */
    private static Instant findTodayTekufaDate(boolean amudeiHoraah) {
        ROZmanimCalendar zmanimCalendarCopy = sROZmanimCalendar.getCopy();
        JewishDateInfo jewishDateInfoCopy = sJewishDateInfo.getCopy();

        for (int dayOffset : new int[]{1, 0}) {
            zmanimCalendarCopy.setLocalDate(zmanimCalendarCopy.getLocalDate().plusDays(dayOffset));
            jewishDateInfoCopy.setLocalDate(zmanimCalendarCopy.getLocalDate());
            zmanimCalendarCopy.setLocalDate(zmanimCalendarCopy.getLocalDate().plusDays(-dayOffset));

            JewishCalendarWithExtraMethods jc = jewishDateInfoCopy.getJewishCalendar();
            if (jc.getTekufaAsInstant(amudeiHoraah) == null) continue;

            Instant tekufaDate = jc.getTekufaAsInstant(amudeiHoraah);
            if (isTekufaToday(zmanimCalendarCopy, tekufaDate)) return tekufaDate;
        }
        return null;
    }
}
