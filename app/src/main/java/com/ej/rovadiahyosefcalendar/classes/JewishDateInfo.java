package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;

public class JewishDateInfo {

    private final JewishCalendar jewishCalendar;
    private final HebrewDateFormatter hebrewDateFormatter;
    private Calendar currentDate = Calendar.getInstance();

    public JewishDateInfo(boolean inIsrael, boolean useModernHoliday) {
        hebrewDateFormatter = new HebrewDateFormatter();
        jewishCalendar = new JewishCalendar();
        jewishCalendar.setInIsrael(inIsrael);
        jewishCalendar.setUseModernHolidays(useModernHoliday);
    }

    public JewishCalendar getJewishCalendar() {
        return this.jewishCalendar;
    }

    public void setCalendar(Calendar calendar) {
        currentDate = calendar;
        jewishCalendar.setDate(currentDate);
    }

    private String getRoshChodeshOrErevRoshChodesh() {
        String result;
        if (jewishCalendar.isRoshChodesh()) {
            String roshChodesh = hebrewDateFormatter.formatRoshChodesh(jewishCalendar);
            if (roshChodesh.contains("Teves")) {
                roshChodesh = "Rosh Chodesh Tevet";
            }
            if (roshChodesh.contains("Tishrei")) {
                roshChodesh = "Rosh Chodesh Tishri";
            }
            result = roshChodesh;
        } else if (jewishCalendar.isErevRoshChodesh()) {
            jewishCalendar.forward(Calendar.DATE, 1);
            String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(jewishCalendar);
            jewishCalendar.setDate(currentDate);
            if (hebrewMonth.contains("Teves")) {
                hebrewMonth = "Rosh Chodesh Tevet";
            }
            if (hebrewMonth.contains("Tishrei")) {
                hebrewMonth = "Rosh Chodesh Tishri";
            }
            result = "Erev " + hebrewMonth;
        } else {
            result = "";
        }
        return result;
    }

    public String getSpecialDay() {
        String result = "";
        String yomTovOfToday = getYomTov();
        String yomTovOfNextDay = getYomTovForNextDay();

        if (yomTovOfToday.isEmpty() && yomTovOfNextDay.isEmpty()) {//NEEDED if both empty
            result = "";
        } else if (yomTovOfToday.isEmpty() && !yomTovOfNextDay.startsWith("Erev")) {//if next day has yom tov
            result = "Erev " + yomTovOfNextDay;
        } else if (!yomTovOfNextDay.isEmpty()
                && !yomTovOfNextDay.startsWith("Erev")
                && !yomTovOfToday.endsWith(yomTovOfNextDay)) {//if today and the next day have yom tov
            result = yomTovOfToday + " / Erev " + yomTovOfNextDay;
        } else {
            result = yomTovOfToday;
        }

        result = addTaanitBechorot(result);
        result = addRoshChodesh(result);
        result = addDayOfOmer(result);
        result = addDayOfChanukah(result);
        return result;
    }

    private String addTaanitBechorot(String result) {
        if (tomorrowIsTaanitBechorot()) {//edge case
            if (result.isEmpty()) {
                result = "Erev Ta'anit Bechorot";
            } else {
                result = "Erev Ta'anit Bechorot / " + result;
            }
        }
        if (jewishCalendar.isTaanisBechoros()) {
            if (result.isEmpty()) {
                result = "Ta'anit Bechorot";
            } else {
                result = "Ta'anit Bechorot / " + result;
            }
        }
        return result;
    }

    private boolean tomorrowIsTaanitBechorot() {
        jewishCalendar.forward(Calendar.DATE, 1);
        boolean result = jewishCalendar.isTaanisBechoros();
        jewishCalendar.setDate(currentDate);
        return result;
    }

    private String addRoshChodesh(String result) {
        String roshChodeshOrErevRoshChodesh = getRoshChodeshOrErevRoshChodesh();
        if (!roshChodeshOrErevRoshChodesh.isEmpty()) {
            if (!result.isEmpty()) {
                result = roshChodeshOrErevRoshChodesh + " / " + result;
            } else {
                result = roshChodeshOrErevRoshChodesh;
            }
        }
        return result;
    }

    private String addDayOfChanukah(String result) {
        int dayOfChanukah = jewishCalendar.getDayOfChanukah();
        if (dayOfChanukah != -1) {
            if (!result.isEmpty()) {
                result += " / " + getOrdinal(dayOfChanukah) + " day of Chanukah";
            } else {
                result = getOrdinal(dayOfChanukah) + " day of Chanukah";
            }
        }
        return result;
    }

    private String addDayOfOmer(String result) {
        int dayOfOmer = jewishCalendar.getDayOfOmer();
        if (dayOfOmer != -1) {
            if (!result.isEmpty()) {
                result += " / " + getOrdinal(dayOfOmer) + " day of Omer";
            } else {
                result = getOrdinal(dayOfOmer) + " day of Omer";
            }
        }
        return result;
    }

    private String getYomTov() {
        switch (jewishCalendar.getYomTovIndex()) {
            case JewishCalendar.EREV_PESACH:
                return "Erev Pesach";
            case JewishCalendar.PESACH:
                return "Pesach";
            case JewishCalendar.CHOL_HAMOED_PESACH:
                return "Chol HaMoed Pesach";
            case JewishCalendar.PESACH_SHENI:
                return "Pesach Sheni";
            case JewishCalendar.EREV_SHAVUOS:
                return "Erev Shavuot";
            case JewishCalendar.SHAVUOS:
                return "Shavuot";
            case JewishCalendar.SEVENTEEN_OF_TAMMUZ:
                return "Seventeenth of Tammuz";
            case JewishCalendar.TISHA_BEAV:
                return "Tisha Be'Av";
            case JewishCalendar.TU_BEAV:
                return "Tu Be'Av";
            case JewishCalendar.EREV_ROSH_HASHANA:
                return "Erev Rosh Hashana";
            case JewishCalendar.ROSH_HASHANA:
                return "Rosh Hashana";
            case JewishCalendar.FAST_OF_GEDALYAH:
                return "Tzom Gedalya";
            case JewishCalendar.EREV_YOM_KIPPUR:
                return "Erev Yom Kippur";
            case JewishCalendar.YOM_KIPPUR:
                return "Yom Kippur";
            case JewishCalendar.EREV_SUCCOS:
                return "Erev Succot";
            case JewishCalendar.SUCCOS:
                return "Succot";
            case JewishCalendar.CHOL_HAMOED_SUCCOS:
                return "Chol HaMoed Succot";
            case JewishCalendar.HOSHANA_RABBA:
                return "7th day of Sukkot (Hoshana Rabba)";
            case JewishCalendar.SHEMINI_ATZERES:
                if (jewishCalendar.getInIsrael()) {
                    return "Shemini Atzeret / Simchat Torah";
                } else {
                    return "Shemini Atzeret";
                }
            case JewishCalendar.SIMCHAS_TORAH:
                return "Simchat Torah";
            //20 was erev chanuka which was deleted
            case JewishCalendar.CHANUKAH:
                return "Chanuka";
            case JewishCalendar.TENTH_OF_TEVES:
                return "Asarah Be'Tevet";
            case JewishCalendar.TU_BESHVAT:
                return "Tu Be'Shevat";
            case JewishCalendar.FAST_OF_ESTHER:
                return "Ta'anit Ester";
            case JewishCalendar.PURIM:
                return "Purim";
            case JewishCalendar.SHUSHAN_PURIM:
                return "Shushan Purim";
            case JewishCalendar.PURIM_KATAN:
                return "Purim Katan";
            case JewishCalendar.ROSH_CHODESH:
                return "Rosh Chodesh";
            case JewishCalendar.YOM_HASHOAH:
                return "Yom Hashoah";
            case JewishCalendar.YOM_HAZIKARON:
                return "Yom Hazikaron";
            case JewishCalendar.YOM_HAATZMAUT://tachanun is said
                return "Yom Haatzmaut";
            case JewishCalendar.YOM_YERUSHALAYIM://tachanun erev before, however, Rav ovadia would not say on the day itself
                return "Yom Yerushalayim";
            case JewishCalendar.LAG_BAOMER:
                return "Lag B'Omer";
            case JewishCalendar.SHUSHAN_PURIM_KATAN:
                return "Shushan Purim Katan";
            default:
                return "";
        }
    }

    private String getYomTovForNextDay() {
        jewishCalendar.forward(Calendar.DATE, 1);
        String result = getYomTov();
        jewishCalendar.setDate(currentDate);
        return result;
    }

    private int getYomTovIndexForNextDay() {
        jewishCalendar.forward(Calendar.DATE, 1);
        int result = jewishCalendar.getYomTovIndex();
        jewishCalendar.setDate(currentDate);
        return result;
    }

    /**
     * This method will return a string containing when to say tachanun for the current date.
     * Here are a list of holidays that it will return a string for no tachanun:
     * <p>
     * Rosh Chodesh
     * The entire month of Nissan
     * Pesach Sheni (14th of Iyar)
     * Lag Ba'Omer
     * Rosh Chodesh Sivan until the 12th of Sivan (12th included)
     * 9th of Av
     * 15th of Av
     * Erev Rosh Hashanah
     * Erev Yom Kippur
     * From the 11th of Tishrei until the end of Tishrei
     * All of Chanuka
     * 15th of Shevat
     * 14th and 15th of Adar I and Adar II (and only 14th of Adar I in a leap year)
     * Every Shabbat
     * <p>
     * Here are the days that we skip tachanun the day before at mincha:
     * Every Friday
     * Every Erev Rosh Chodesh
     * Fast of Esther
     * Tisha Be'av
     * Tu Be'Shvat
     * Lag Ba'Omer
     * Pesach Sheni
     *
     * @return a String containing whether or not tachanun is said today, and if only in the morning
     */
    public String getIsTachanunSaid() {
        int yomTovIndex = jewishCalendar.getYomTovIndex();
        if (jewishCalendar.isRoshChodesh()
                || yomTovIndex == JewishCalendar.PESACH_SHENI
                || yomTovIndex == JewishCalendar.LAG_BAOMER
                || yomTovIndex == JewishCalendar.TISHA_BEAV
                || yomTovIndex == JewishCalendar.TU_BEAV
                || yomTovIndex == JewishCalendar.EREV_ROSH_HASHANA
                || yomTovIndex == JewishCalendar.ROSH_HASHANA
                || yomTovIndex == JewishCalendar.EREV_YOM_KIPPUR
                || yomTovIndex == JewishCalendar.YOM_KIPPUR
                || yomTovIndex == JewishCalendar.TU_BESHVAT
                || yomTovIndex == JewishCalendar.PURIM_KATAN
                || yomTovIndex == JewishCalendar.SHUSHAN_PURIM_KATAN
                || yomTovIndex == JewishCalendar.PURIM
                || yomTovIndex == JewishCalendar.SHUSHAN_PURIM
                || yomTovIndex == JewishCalendar.YOM_YERUSHALAYIM
                || jewishCalendar.isChanukah()
                || jewishCalendar.getJewishMonth() == JewishDate.NISSAN
                || (jewishCalendar.getJewishMonth() == JewishDate.SIVAN && jewishCalendar.getJewishDayOfMonth() <= 12)
                || (jewishCalendar.getJewishMonth() == JewishDate.TISHREI && jewishCalendar.getJewishDayOfMonth() >= 11)) {
            return "There is no Tachanun today";
        }
        int yomTovIndexForNextDay = getYomTovIndexForNextDay();
        if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                || yomTovIndex == JewishCalendar.FAST_OF_ESTHER
                || yomTovIndexForNextDay == JewishCalendar.TISHA_BEAV
                || yomTovIndexForNextDay == JewishCalendar.TU_BEAV
                || yomTovIndexForNextDay == JewishCalendar.TU_BESHVAT
                || yomTovIndexForNextDay == JewishCalendar.LAG_BAOMER
                || yomTovIndexForNextDay == JewishCalendar.PESACH_SHENI
                || yomTovIndexForNextDay == JewishCalendar.PURIM_KATAN
                || jewishCalendar.isErevRoshChodesh()) {
            return "There is only Tachanun in the morning";
        }
        if (currentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return "צדקתך";
        }
        return "There is Tachanun today";
    }

    public String getJewishDate() {
        return jewishCalendar.toString().replace("Teves", "Tevet");
    }

    public String isJewishLeapYear() {
        if (jewishCalendar.isJewishLeapYear()) {
            return "This year is a jewish leap year!";
        } else {
            return "This year is a not a jewish leap year!";
        }
    }

    public String getThisWeeksParsha() {

        currentDate = jewishCalendar.getGregorianCalendar();
        Calendar parshaCalendar = jewishCalendar.getGregorianCalendar();

        while (parshaCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            parshaCalendar.add(Calendar.DATE, 1);
        }

        jewishCalendar.setDate(parshaCalendar);

        hebrewDateFormatter.setHebrewFormat(true);
        String parsha = hebrewDateFormatter.formatParsha(jewishCalendar);
        String specialParsha = hebrewDateFormatter.formatSpecialParsha(jewishCalendar);
        hebrewDateFormatter.setHebrewFormat(false);//return to default setting

        jewishCalendar.setDate(currentDate);

        if (parsha.isEmpty() && specialParsha.isEmpty()) {
            return "No Parsha this week";
        } else if (specialParsha.isEmpty()) {
            return parsha;
        } else {
            return parsha + " / " + specialParsha;
        }
    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    public String getJewishDayOfWeek() {
        hebrewDateFormatter.setHebrewFormat(true);
        String result = "\u05D9\u05D5\u05DD ";//this says יוֹם
        result += hebrewDateFormatter.formatDayOfWeek(jewishCalendar);
        hebrewDateFormatter.setHebrewFormat(false);
        return result;
    }

    public static String formatHebrewNumber(int num) {
        if (num <= 0 || num >= 6000) return null;// should refactor

        String[] let1000 = {" א'", " ב'", " ג'", " ד'", " ה'"};
        String[] let100 = {"ק", "ר", "ש", "ת"};
        String[] let10 = {"י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ"};
        String[] let1 = {"א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט"};

        StringBuilder result = new StringBuilder();

        if (num >= 100) {
            if (num >= 1000) {
                result.append(let1000[num / 1000 - 1]);
                num %= 1000;
            }

            if (num < 500) {
                result.append(let100[(num / 100) - 1]);
            } else if (num < 900) {
                result.append("ת");
                result.append(let100[((num - 400) / 100) - 1]);
            } else {
                result.append("תת");
                result.append(let100[((num - 800) / 100) - 1]);
            }

            num %= 100;
        }
        switch (num) {
            // Avoid letter combinations from the Tetragrammaton
            case 16:
                result.append("טז");
                break;
            case 15:
                result.append("טו");
                break;
            default:
                if (num >= 10) {
                    result.append(let10[(num / 10) - 1]);
                    num %= 10;
                }
                if (num > 0) {
                    result.append(let1[num - 1]);
                }
                break;
        }
        return result.toString();
    }

    public String getIsTonightStartOrEndBirchatLevana() {//TODO check if this method might need fixing
        Calendar sevenDays = Calendar.getInstance();
        sevenDays.setTime(jewishCalendar.getTchilasZmanKidushLevana7Days());

        Calendar fifteenDays = Calendar.getInstance();
        fifteenDays.setTime(jewishCalendar.getSofZmanKidushLevana15Days());
        fifteenDays.add(Calendar.DATE, -1);

        if (DateUtils.isSameDay(jewishCalendar.getGregorianCalendar(), sevenDays)) {
            return "Birchat HeLevana starts tonight";
        }

        if (DateUtils.isSameDay(jewishCalendar.getGregorianCalendar(), fifteenDays)) {
            return "Last night for Birchat HaLevana";
        }
        return "";
    }

    public String getIsMashivHaruchOrMoridHatalSaid() {
        if (jewishCalendar.isMashivHaruachRecited()) {
            return "משיב הרוח";
        }

        if (jewishCalendar.isMoridHatalRecited()) {
            return "מוריד הטל";
        }
        return "";
    }

    public String getIsBarcheinuOrBarechAleinuSaid() {
        if (jewishCalendar.isVeseinBerachaRecited()) {
            return "ברכנו";
        }

        if (jewishCalendar.isVeseinTalUmatarRecited()) {
            return "ברך עלינו";
        }
        return "";
    }

    public String getIsUlChaparatPeshaSaid() {
        if (jewishCalendar.isRoshChodesh()) {
            if (jewishCalendar.isJewishLeapYear()) {
                int month = jewishCalendar.getJewishMonth();
                if (month == JewishCalendar.CHESHVAN ||
                        month == JewishCalendar.KISLEV ||
                        month == JewishCalendar.TEVES ||
                        month == JewishCalendar.SHEVAT ||
                        month == JewishCalendar.ADAR ||
                        month == JewishCalendar.ADAR_II) {
                    return "Say וּלְכַפָּרַת פֶּשַׁע";
                } else {
                    return "Do not say וּלְכַפָּרַת פֶּשַׁע";
                }
            } else {
                return "Do not say וּלְכַפָּרַת פֶּשַׁע";
            }
        }
        return "";
    }
}
