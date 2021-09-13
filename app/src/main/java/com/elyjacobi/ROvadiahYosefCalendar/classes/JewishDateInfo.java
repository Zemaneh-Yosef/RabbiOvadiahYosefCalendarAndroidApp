package com.elyjacobi.ROvadiahYosefCalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

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
            result = roshChodesh;
        } else if (jewishCalendar.isErevRoshChodesh()) {
            JewishCalendar temp = new JewishCalendar();
            temp.setDate(jewishCalendar.getGregorianCalendar());
            temp.forward(Calendar.DATE,1);
            String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(temp);
            if (hebrewMonth.contains("Teves")) {
                hebrewMonth = "Rosh Chodesh Tevet";
            }
            result = "Erev " + hebrewMonth;
        } else {
            result = "";
        }
        return result;
    }

    public String getSpecialDay() {
        String result;
        String yomTovOfToday = getYomTov();
        String yomTovOfNextDay = checkNextDayForSpecialDay();
        if (yomTovOfToday.isEmpty() && yomTovOfNextDay.isEmpty()) {//if empty
            result = "";
        } else if (yomTovOfToday.isEmpty() && !yomTovOfNextDay.startsWith("Erev")) {//if next day has yom tov
            result = "Erev " + yomTovOfNextDay;
        } else if (!yomTovOfNextDay.isEmpty()
                && !yomTovOfNextDay.startsWith("Erev")
                && !yomTovOfToday.endsWith(yomTovOfNextDay)) {//if today and the next day have yom tov
            result = yomTovOfToday + " / Erev " + yomTovOfNextDay;
        } else {
            if (jewishCalendar.isChanukah()) {
                int dayOfChanukah = jewishCalendar.getDayOfChanukah();
                yomTovOfToday = yomTovOfToday + " " + dayOfChanukah;
            }
            result = yomTovOfToday;
        }
        if (pesachFallsOnMotzeiShabbat()) {
            if (jewishCalendar.getJewishMonth() == 1 && jewishCalendar.getJewishDayOfMonth() == 11) {
                result = "Erev Ta'anit Bechorot";
            }
            if (jewishCalendar.getJewishMonth() == 1 && jewishCalendar.getJewishDayOfMonth() == 12) {
                result = "Ta'anit Bechorot";
            }
        } else {
            if (yomTovOfNextDay.contains("Erev Pesach")) {
                result = "Erev Ta'anit Bechorot";
            } else if (yomTovOfToday.contains("Erev Pesach")) {
                result += " / Ta'anit Bechorot";
            }
        }
        if (!getRoshChodeshOrErevRoshChodesh().isEmpty()) {
            if (result.isEmpty()) {
                result = getRoshChodeshOrErevRoshChodesh();
            } else {
                result = getRoshChodeshOrErevRoshChodesh() + " / " + result;
            }
        }
        result = addDayOfOmer(result);
        return result;
    }

    private boolean pesachFallsOnMotzeiShabbat() {
        boolean result;
        jewishCalendar.setJewishDate(jewishCalendar.getJewishYear(),1,15);
        result = jewishCalendar.getDayOfWeek() == Calendar.SUNDAY;
        jewishCalendar.setDate(currentDate);
        return result;
    }

    private String addDayOfOmer(String result) {
        int dayOfOmer = jewishCalendar.getDayOfOmer();
        if (dayOfOmer != -1){
            if (!result.isEmpty()){
                result += " / " + getOrdinal(dayOfOmer) + " day of Omer";
            } else {
                result = getOrdinal(dayOfOmer) + " day of Omer";
            }
        }
        return result;
    }

    private String checkNextDayForSpecialDay() {
        jewishCalendar.forward(Calendar.DATE, 1);
        String result = getYomTov();
        jewishCalendar.setDate(currentDate);
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
            case JewishCalendar.YOM_HAATZMAUT:
                return "Yom Haatzmaut";
            case JewishCalendar.YOM_YERUSHALAYIM:
                return "Yom Yerushalayim";
            case JewishCalendar.LAG_BAOMER:
                return "Lag B'Omer";
            case JewishCalendar.SHUSHAN_PURIM_KATAN:
                return "Shushan Purim Katan";
            default:
                return "";
        }
    }

    /**
     * This method will return a string containing when to say tachanun for the current date.
     * Here are a list of holidays that it will return a string for no tachanun:
     *
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
     *
     * @return a String containing whether or not tachanun is said today, and if only at specific times
     */
    public String getIsTachanunSaid() {
        if (jewishCalendar.isRoshChodesh()
                || jewishCalendar.getJewishMonth() == JewishDate.NISSAN
                || jewishCalendar.getYomTovIndex() == JewishCalendar.PESACH_SHENI
                || jewishCalendar.getYomTovIndex() == JewishCalendar.LAG_BAOMER
                || (jewishCalendar.getJewishMonth() == JewishDate.SIVAN && jewishCalendar.getJewishDayOfMonth() <= 12)
                || jewishCalendar.getYomTovIndex() == JewishCalendar.TISHA_BEAV
                || jewishCalendar.getYomTovIndex() == JewishCalendar.TU_BEAV
                || jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_ROSH_HASHANA
                || jewishCalendar.getYomTovIndex() == JewishCalendar.ROSH_HASHANA
                || jewishCalendar.getYomTovIndex() == JewishCalendar.EREV_YOM_KIPPUR
                || jewishCalendar.getYomTovIndex() == JewishCalendar.YOM_KIPPUR
                || (jewishCalendar.getJewishMonth() == JewishDate.TISHREI && jewishCalendar.getJewishDayOfMonth() >= 11)
                || jewishCalendar.isChanukah()
                || jewishCalendar.getYomTovIndex() == JewishCalendar.TU_BESHVAT
                || jewishCalendar.getYomTovIndex() == JewishCalendar.PURIM_KATAN
                || jewishCalendar.getYomTovIndex() == JewishCalendar.PURIM
                || jewishCalendar.getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM
                || jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return "There is no Tachanun today";
        }
        if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                || jewishCalendar.getYomTovIndex() == JewishCalendar.FAST_OF_ESTHER
                || checkNextDayForSpecialDay().equals("Tisha Be'Av")
                || checkNextDayForSpecialDay().equals("Tu Be'Av")
                || checkNextDayForSpecialDay().equals("Tu Be'Shevat")
                || checkNextDayForSpecialDay().equals("Lag B'Omer")
                || checkNextDayForSpecialDay().equals("Pesach Sheni")
                || checkNextDayForSpecialDay().equals("Erev Shavuot")
                || checkNextDayForSpecialDay().equals("Tisha Be'Av")
                || checkNextDayForSpecialDay().equals("Tu Be'Av")) {
            return "There is no Tachanun in the morning";
        }
        return "There is Tachanun today";
    }

    public String getJewishDate() {
        return jewishCalendar.toString().replace("Teves","Tevet");
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

        while (parshaCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY){
            parshaCalendar.add(Calendar.DATE, 1);
        }

        jewishCalendar.setDate(parshaCalendar);

        hebrewDateFormatter.setHebrewFormat(true);
        String parsha = hebrewDateFormatter.formatParsha(jewishCalendar);
        String specialParsha = hebrewDateFormatter.formatSpecialParsha(jewishCalendar);
        hebrewDateFormatter.setHebrewFormat(false);//return to default setting

        jewishCalendar.setDate(currentDate);

        if (parsha.isEmpty() && specialParsha.isEmpty()){
            return "No Parsha this week";
        } else if (specialParsha.isEmpty()) {
            return parsha;
        } else {
            return parsha + " / " + specialParsha;
        }
    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
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

        String[] let1000 = { " א'", " ב'", " ג'", " ד'", " ה'" };
        String[] let100 = { "ק", "ר", "ש", "ת" };
        String[] let10 = { "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ" };
        String[] let1 = { "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט" };

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
}
