package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;

import android.content.Context;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.ui.limudiim.LimudFragment;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the learning of a whole week as ranges, from what is learned on the first day of the week to what is learned on the
 * last day, the same way the website's weekly print calendar shows the daf yomi (WebsiteLimudCalendar.getWeeklyLearning):
 * "Book first – last" when the book stays the same all week, and "Book first → Other book last" when it changes mid-week.
 */
public class WeeklyLimudRanges {

    private WeeklyLimudRanges() {}

    /**
     * @param firstDay the first day of the week (Sunday)
     * @param lastDay the last day of the week (Shabbat)
     * @return the week's learning, in the same order as the daily list in the limudim tab
     */
    public static List<LimudListEntry> getRanges(Context context, LocalDate firstDay, LocalDate lastDay) {
        List<LimudListEntry> limudim = new ArrayList<>();
        JewishDateInfo first = dayInfo(firstDay);
        JewishDateInfo last = dayInfo(lastDay);

        if (!firstDay.isBefore(LimudFragment.dafYomiStartDate)) {
            Daf firstDaf = YomiCalculator.getDafYomiBavli(first.getJewishCalendar());
            Daf lastDaf = YomiCalculator.getDafYomiBavli(last.getJewishCalendar());
            limudim.add(new LimudListEntry(context.getString(R.string.daf_yomi) + " " + range(
                    firstDaf.getMasechta(), hebrewNumber(firstDaf.getDaf()),
                    lastDaf.getMasechta(), hebrewNumber(lastDaf.getDaf()))));
        }

        if (!firstDay.isBefore(LimudFragment.dafYomiYerushalmiStartDate)) {
            // there is no daf yerushalmi on some days (e.g. Yom Kippur), so use the first and last days that have one
            Daf firstDaf = null;
            Daf lastDaf = null;
            for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
                Daf daf = YerushalmiYomiCalculator.getDafYomiYerushalmi(dayInfo(day).getJewishCalendar());
                if (daf != null) {
                    if (firstDaf == null) {
                        firstDaf = daf;
                    }
                    lastDaf = daf;
                }
            }
            if (firstDaf != null) {
                limudim.add(new LimudListEntry(context.getString(R.string.yerushalmi_yomi) + " " + range(
                        firstDaf.getYerushalmiMasechta(), hebrewNumber(firstDaf.getDaf()),
                        lastDaf.getYerushalmiMasechta(), hebrewNumber(lastDaf.getDaf()))));
            } else {
                limudim.add(new LimudListEntry(context.getString(R.string.no_daf_yomi_yerushalmi)));
            }
        }

        MishnaYomi firstMishna = new MishnaYomi(first.getJewishCalendar(), true);
        MishnaYomi lastMishna = new MishnaYomi(last.getJewishCalendar(), true);
        if (!firstMishna.getFirstMasechta().isEmpty() && !lastMishna.getSecondMasechta().isEmpty()) {// empty before the mishna yomi cycle started
            limudim.add(new LimudListEntry(context.getString(R.string.mishna_yomi) + " " + range(
                    firstMishna.replaceEnglishWithHebrew(firstMishna.getFirstMasechta()),
                    hebrewNumber(firstMishna.getFirstPerek()) + ":" + hebrewNumber(firstMishna.getFirstMishna()),
                    lastMishna.replaceEnglishWithHebrew(lastMishna.getSecondMasechta()),
                    hebrewNumber(lastMishna.getSecondPerek()) + ":" + hebrewNumber(lastMishna.getSecondMishna()))));
        }

        List<HalachaSegment> firstHalacha = HalachaYomi.INSTANCE.getDailyLearning(firstDay);
        List<HalachaSegment> lastHalacha = HalachaYomi.INSTANCE.getDailyLearning(lastDay);
        if (firstHalacha != null && !firstHalacha.isEmpty() && lastHalacha != null && !lastHalacha.isEmpty()) {
            HalachaSegment start = firstHalacha.get(0);
            HalachaSegment end = lastHalacha.get(lastHalacha.size() - 1);
            String lastSeif = end.getSeifim().contains("-") ? end.getSeifim().substring(end.getSeifim().lastIndexOf('-') + 1) : end.getSeifim();
            limudim.add(new LimudListEntry(context.getString(R.string.daily_halacha) + " " + range(
                    start.getBookName(), hebrewNumber(start.getSiman()) + ":" + hebrewNumber(start.getFirstSeif()),
                    end.getBookName(), hebrewNumber(end.getSiman()) + ":" + lastSeif)));
        }

        limudim.add(new LimudListEntry(context.getString(R.string.daily_chafetz_chaim) + fromTo(
                ChafetzChayimYomiCalculator.getChafetzChayimYomi(first.getJewishCalendar()),
                ChafetzChayimYomiCalculator.getChafetzChayimYomi(last.getJewishCalendar()))));

        String firstMonthly = TehilimYomi.byDayOfMonth(context, first.getJewishCalendar());
        String lastMonthly = TehilimYomi.byDayOfMonth(context, last.getJewishCalendar());
        boolean monthRestarted = last.getJewishCalendar().getJewishDayOfMonth() < first.getJewishCalendar().getJewishDayOfMonth();
        limudim.add(new LimudListEntry(context.getString(R.string.daily_tehilim) + context.getString(R.string.monthly) + ": "
                + (monthRestarted ? fromTo(firstMonthly, lastMonthly) : startOf(firstMonthly) + " - " + endOf(lastMonthly))));

        limudim.add(new LimudListEntry(context.getString(R.string.daily_tehilim) + context.getString(R.string.weekly) + ": "
                + startOf(TehilimYomi.byDayOfWeek(context, first.getJewishCalendar())) + " - " + endOf(TehilimYomi.byDayOfWeek(context, last.getJewishCalendar()))));

        DailyMishnehTorah dailyMishnehTorah = DailyMishnehTorah.INSTANCE;
        RambamReading firstRambam = dailyMishnehTorah.getDailyLearning(firstDay);
        RambamReading lastRambam = dailyMishnehTorah.getDailyLearning(lastDay);
        if (firstRambam != null && lastRambam != null) {
            limudim.add(new LimudListEntry(context.getString(R.string.rambam_yomi) + range(
                    firstRambam.getBookName(), startOf(firstRambam.getChapter()),
                    lastRambam.getBookName(), endOf(lastRambam.getChapter()))));
        }

        List<RambamReading> firstRambam3 = dailyMishnehTorah.getDailyLearning3(firstDay);
        List<RambamReading> lastRambam3 = dailyMishnehTorah.getDailyLearning3(lastDay);
        if (firstRambam3 != null && !firstRambam3.isEmpty() && lastRambam3 != null && !lastRambam3.isEmpty()) {
            RambamReading start = firstRambam3.get(0);
            RambamReading end = lastRambam3.get(lastRambam3.size() - 1);
            limudim.add(new LimudListEntry(context.getString(R.string.rambam_yomi_3_chapters) + "\n" + range(
                    start.getBookName(), startOf(start.getChapter()),
                    end.getBookName(), endOf(end.getChapter()))));
        }

        // the nasi is only read during the first days of Nisan, so use the first and last days of the week that have one
        String firstNasi = "";
        String lastNasi = "";
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            JewishDateInfo dayInfo = dayInfo(day);
            if (dayInfo.getJewishCalendar().getJewishMonth() == JewishDate.NISSAN) {
                String title = NisanLimudYomi.getNisanLimudYomiTitle(dayInfo.getJewishCalendar().getJewishDayOfMonth());
                if (!title.isEmpty()) {
                    if (firstNasi.isEmpty()) {
                        firstNasi = title;
                    }
                    lastNasi = title;
                }
            }
        }
        if (!firstNasi.isEmpty()) {
            String nasi = fromTo(firstNasi, lastNasi);
            LimudListEntry nasiEntry = new LimudListEntry(context.getString(R.string.daily_nasi) + nasi, nasi);
            nasiEntry.setHasSource(false);
            limudim.add(nasiEntry);
        }

        return limudim;
    }

    private static JewishDateInfo dayInfo(LocalDate day) {
        JewishDateInfo info = sJewishDateInfo.getCopy();
        info.setLocalDate(day);
        return info;
    }

    /**
     * "Book first – last" when the book is the same at the start and end of the week, otherwise "Book first → Other book last".
     */
    static String range(String firstBook, String firstPosition, String lastBook, String lastPosition) {
        if (firstBook.equals(lastBook)) {
            if (firstPosition.equals(lastPosition)) {
                return firstBook + " " + firstPosition;
            }
            return firstBook + " " + firstPosition + " – " + lastPosition;
        }
        return firstBook + " " + firstPosition + " → " + lastBook + " " + lastPosition;
    }

    /**
     * For learning that only exists as text: shown once if it is the same all week, otherwise "first → last".
     */
    private static String fromTo(String first, String last) {
        if (first == null || last == null) {
            return first == null ? String.valueOf(last) : first;
        }
        return first.equals(last) ? first : first + " → " + last;
    }

    /** @return the start of a "start - end" range, e.g. "1" from "1 - 9", or the whole text if it is not a range */
    private static String startOf(String range) {
        int dash = range.indexOf('-');
        return dash == -1 ? range.trim() : range.substring(0, dash).trim();
    }

    /** @return the end of a "start - end" range, e.g. "9" from "1 - 9", or the whole text if it is not a range */
    private static String endOf(String range) {
        int dash = range.lastIndexOf('-');
        return dash == -1 ? range.trim() : range.substring(dash + 1).trim();
    }

    private static String hebrewNumber(int number) {
        return sHebrewDateFormatter.formatHebrewNumber(number);
    }
}
