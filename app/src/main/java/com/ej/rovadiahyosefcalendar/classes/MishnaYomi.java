package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class MishnaYomi {

    /** The start date of the Mishna Yomi Cycle. */
    private static final Calendar CYCLE_START_DATE = new GregorianCalendar(1947, 5, 20);
    /** The number of mishnas in a day. */
    private static final int MISHNAS_PER_DAY = 2;
    /** The number of milliseconds in a day. */
    private final static int DAY_MILIS = 1000 * 60 * 60 * 24;
    private static final int NUM_MISHNAS = 4192;
    private static final int CYCLE_LENGTH = NUM_MISHNAS / 2;  // 2075 mishnas

    private static final Map<String, int[]> UNITS = new LinkedHashMap<String, int[]>() {{
        put("Berachot", new int[]{5, 8, 6, 7, 5, 8, 5, 8, 5});
        put("Peah", new int[]{6, 8, 8, 11, 8, 11, 8, 9});
        put("Demai", new int[]{4, 5, 6, 7, 11, 12, 8});
        put("Kilayim", new int[]{9, 11, 7, 9, 8, 9, 8, 6, 10});
        put("Sheviit", new int[]{8, 10, 10, 10, 9, 6, 7, 11, 9, 9});
        put("Terumot", new int[]{10, 6, 9, 13, 9, 6, 7, 12, 7, 12, 10});
        put("Maasrot", new int[]{8, 8, 10, 6, 8});
        put("Maaser Sheni", new int[]{7, 10, 13, 12, 15});
        put("Challah", new int[]{9, 8, 10, 11});
        put("Orlah", new int[]{9, 17, 9});
        put("Bikurim", new int[]{11, 11, 12, 5});
        put("Shabbat", new int[]{11, 7, 6, 2, 4, 10, 4, 7, 7, 6, 6, 6, 7, 4, 3, 8, 8, 3, 6, 5, 3, 6, 5, 5});
        put("Eruvin", new int[]{10, 6, 9, 11, 9, 10, 11, 11, 4, 15});
        put("Pesachim", new int[]{7, 8, 8, 9, 10, 6, 13, 8, 11, 9});
        put("Shekalim", new int[]{7, 5, 4, 9, 6, 6, 7, 8});
        put("Yoma", new int[]{8, 7, 11, 6, 7, 8, 5, 9});
        put("Sukkah", new int[]{11, 9, 15, 10, 8});
        put("Beitzah", new int[]{10, 10, 8, 7, 7});
        put("Rosh Hashanah", new int[]{9, 8, 9, 9});
        put("Taanit", new int[]{7, 10, 9, 8});
        put("Megillah", new int[]{11, 6, 6, 10});
        put("Moed Katan", new int[]{10, 5, 9});
        put("Chagigah", new int[]{8, 7, 8});
        put("Yevamot", new int[]{4, 10, 10, 13, 6, 6, 6, 6, 6, 9, 7, 6, 13, 9, 10, 7});
        put("Ketubot", new int[]{10, 10, 9, 12, 9, 7, 10, 8, 9, 6, 6, 4, 11});
        put("Nedarim", new int[]{4, 5, 11, 8, 6, 10, 9, 7, 10, 8, 12});
        put("Nazir", new int[]{7, 10, 7, 7, 7, 11, 4, 2, 5});
        put("Sotah", new int[]{9, 6, 8, 5, 5, 4, 8, 7, 15});
        put("Gittin", new int[]{6, 7, 8, 9, 9, 7, 9, 10, 10});
        put("Kiddushin", new int[]{10, 10, 13, 14});
        put("Bava Kamma", new int[]{4, 6, 11, 9, 7, 6, 7, 7, 12, 10});
        put("Bava Metzia", new int[]{8, 11, 12, 12, 11, 8, 11, 9, 13, 6});
        put("Bava Batra", new int[]{6, 14, 8, 9, 11, 8, 4, 8, 10, 8});
        put("Sanhedrin", new int[]{6, 5, 8, 5, 5, 6, 11, 7, 6, 6, 6});
        put("Makkot", new int[]{10, 8, 16});
        put("Shevuot", new int[]{7, 5, 11, 13, 5, 7, 8, 6});
        put("Eduyot", new int[]{14, 10, 12, 12, 7, 3, 9, 7});
        put("Avodah Zarah", new int[]{9, 7, 10, 12, 12});
        put("Avot", new int[]{18, 16, 18, 22, 23, 11});
        put("Horiyot", new int[]{5, 7, 8});
        put("Zevachim", new int[]{4, 5, 6, 6, 8, 7, 6, 12, 7, 8, 8, 6, 8, 10});
        put("Menachot", new int[]{4, 5, 7, 5, 9, 7, 6, 7, 9, 9, 9, 5, 11});
        put("Chullin", new int[]{7, 10, 7, 7, 5, 7, 6, 6, 8, 4, 2, 5});
        put("Bechorot", new int[]{7, 9, 4, 10, 6, 12, 7, 10, 8});
        put("Arachin", new int[]{4, 6, 5, 4, 6, 5, 5, 7, 8});
        put("Temurah", new int[]{6, 3, 5, 4, 6, 5, 6});
        put("Keritot", new int[]{7, 6, 10, 3, 8, 9});
        put("Meilah", new int[]{4, 9, 8, 6, 5, 6});
        put("Tamid", new int[]{4, 5, 9, 3, 6, 4, 3});
        put("Midot", new int[]{9, 6, 8, 7, 4});
        put("Kinnim", new int[]{4, 5, 6});
        put("Keilim", new int[]{9, 8, 8, 4, 11, 4, 6, 11, 8, 8, 9, 8, 8, 8, 6, 8, 17, 9, 10, 7, 3, 10, 5, 17, 9, 9, 12, 10, 8, 4});
        put("Ohalot", new int[]{8, 7, 7, 3, 7, 7, 6, 6, 16, 7, 9, 8, 6, 7, 10, 5, 5, 10});
        put("Negaim", new int[]{6, 5, 8, 11, 5, 8, 5, 10, 3, 10, 12, 7, 12, 13});
        put("Parah", new int[]{4, 5, 11, 4, 9, 5, 12, 11, 9, 6, 9, 11});
        put("Tahorot", new int[]{9, 8, 8, 13, 9, 10, 9, 9, 9, 8});
        put("Mikvaot", new int[]{8, 10, 4, 5, 6, 11, 7, 5, 7, 8});
        put("Niddah", new int[]{7, 7, 7, 7, 9, 14, 5, 4, 11, 8});
        put("Machshirin", new int[]{6, 11, 8, 10, 11, 8});
        put("Zavim", new int[]{6, 4, 3, 7, 12});
        put("Tevul Yom", new int[]{5, 8, 6, 7});
        put("Yadayim", new int[]{5, 4, 5, 8});
        put("Uktzin", new int[]{6, 10, 12});
    }};
    private static String sFirstMasechta = "";
    private static int sFirstPerek = 0;
    private static int sFirstMishna = 0;
    private static String sSecondMasechta = "";
    private static int sSecondPerek = 0;
    private static int sSecondMishna = 0;

    public static String getMishnaForDate(JewishCalendar calendar) {
        Calendar nextCycle = new GregorianCalendar();
        Calendar prevCycle = new GregorianCalendar();
        Calendar requested = calendar.getGregorianCalendar();

        if (requested.before(CYCLE_START_DATE)) {
            return null;
        }

        // Start to calculate current cycle. init the start day
        nextCycle.setTime(CYCLE_START_DATE.getTime());

        // Go cycle by cycle, until we get the next cycle
        while (requested.after(nextCycle)) {
            prevCycle.setTime(nextCycle.getTime());
            // Adds the number of whole mishna cycles.
            nextCycle.add(Calendar.DAY_OF_MONTH, CYCLE_LENGTH);
        }

        // Get the number of days from cycle start until request.
        long t1 = prevCycle.getTime().getTime()/1000;
        long t2 = requested.getTime().getTime()/1000;
        int numberOfMishnasRead = (int) (getDiffBetweenDays(prevCycle, requested)) * 2;

        // Finally find the mishna.
        findFirstMishna(numberOfMishnasRead);

        // Again for the second mishna which could be in the next masechta
        findSecondMishna(numberOfMishnasRead + 1);

        if (sFirstMasechta.equals(sSecondMasechta)) {
            if (sFirstPerek == sSecondPerek) {
                return sFirstMasechta + " " + sFirstPerek + ":" + sFirstMishna + "-" + sSecondMishna;
            } else {// Different Perakim
                return sFirstMasechta + " " + sFirstPerek + ":" + sFirstMishna + "-" + sSecondPerek + ":" + sSecondMishna;
            }
        } else {// Different Masechtas
            return sFirstMasechta + " " + sFirstPerek + ":" + sFirstMishna + " - " + sSecondMasechta + " " + sSecondPerek + ":" + sSecondMishna;
        }
    }

    private static void findSecondMishna(int numberOfMishnasRead) {
        for (Map.Entry<String, int[]> entry: UNITS.entrySet()) {
            String masechta = entry.getKey();
            int[] perakim = entry.getValue();
            for (int i = 0; i < perakim.length; i++) {
                int perek = i + 1;
                int numberOfMishnayot = perakim[i];
                int currentMishna = 1;
                if (numberOfMishnasRead >= 0) {
                    for (int j = 0; j < numberOfMishnayot; j++) {
                        if (numberOfMishnasRead == 0) {
                            sSecondMasechta = masechta;
                            sSecondPerek = perek;
                            sSecondMishna = currentMishna;
                            return;
                        }
                        numberOfMishnasRead -= 1;
                        currentMishna += 1;
                    }
                }
            }
            if (!sSecondMasechta.isEmpty()) {
                return;
            }
        }
    }

    private static void findFirstMishna(int numberOfMishnasRead) {
        for (Map.Entry<String, int[]> entry: UNITS.entrySet()) {
            String masechta = entry.getKey();
            int[] perakim = entry.getValue();
            for (int i = 0; i < perakim.length; i++) {
                int perek = i + 1;
                int numberOfMishnayot = perakim[i];
                int currentMishna = 1;
                if (numberOfMishnasRead >= 0) {
                    for (int j = 0; j < numberOfMishnayot; j++) {
                        if (numberOfMishnasRead == 0) {
                            sFirstMasechta = masechta;
                            sFirstPerek = perek;
                            sFirstMishna = currentMishna;
                            return;
                        }
                        numberOfMishnasRead -= 1;
                        currentMishna += 1;
                    }
                }
            }
            if (!sFirstMasechta.isEmpty()) {
                return;
            }
        }
    }

    /**
     * Return the number of days between the dates passed in
     * @param start the start date
     * @param end the end date
     * @return the number of days between the start and end dates
     */
    private static long getDiffBetweenDays(Calendar start, Calendar end) {
        long test = (end.getTimeInMillis() - start.getTimeInMillis());
        return (end.getTimeInMillis() - start.getTimeInMillis()) / DAY_MILIS;
    }
}
