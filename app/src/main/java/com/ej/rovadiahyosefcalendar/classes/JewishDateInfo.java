package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;

import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * This class is used to get the information about the Jewish date. It is a helper class to manipulate the classes in the kosherjava library.
 * @author Elyahu Jacobi
 * @version 1.0
 * @since 1.0
 * @see com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
 * @see com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
 * @see com.kosherjava.zmanim.hebrewcalendar.JewishDate
 */
public class JewishDateInfo {

    private final JewishCalendar jewishCalendar;
    private final HebrewDateFormatter hebrewDateFormatter;
    private final TefilaRules tefilaRules;
    private Calendar currentDate = Calendar.getInstance();
    private boolean isLocaleHebrew = false;

    /**
     * Constructor of the class.
     * @param inIsrael boolean value that indicates if the user is in Israel or not
     * @param useModernHoliday boolean value to indicate whether or not to use modern holidays
     */
    public JewishDateInfo(boolean inIsrael, boolean useModernHoliday) {
        jewishCalendar = new JewishCalendarWithExtraMethods();
        jewishCalendar.setInIsrael(inIsrael);
        jewishCalendar.setUseModernHolidays(useModernHoliday);
        hebrewDateFormatter = new HebrewDateFormatter();
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            hebrewDateFormatter.setHebrewFormat(true);
            isLocaleHebrew = true;
        }
        tefilaRules = new TefilaRules();
    }

    /**
     * This method is used to get the current calendar object.
     * @return the current jewish calendar object
     */
    public JewishCalendarWithExtraMethods getJewishCalendar() {
        return (JewishCalendarWithExtraMethods) this.jewishCalendar;
    }

    /**
     * This method is used to set the current date.
     * @param calendar the calendar to change the current date
     */
    public void setCalendar(Calendar calendar) {
        currentDate = calendar;
        jewishCalendar.setDate(currentDate);
    }

    /**
     * This method is used to get the current Rosh Chodesh or Erev Rosh Chodesh.
     * @return a string containing the current Rosh Chodesh or Erev Rosh Chodesh
     */
    private String getRoshChodeshOrErevRoshChodesh() {
        String result;
        if (isLocaleHebrew) {
            hebrewDateFormatter.setHebrewFormat(true);
            if (jewishCalendar.isRoshChodesh()) {
                result = hebrewDateFormatter.formatRoshChodesh(jewishCalendar);
            } else if (jewishCalendar.isErevRoshChodesh()) {
                jewishCalendar.forward(Calendar.DATE, 1);
                String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(jewishCalendar);
                jewishCalendar.setDate(currentDate);
                result = "ערב " + hebrewMonth;
            } else {
                result = "";
            }
        } else {
            if (jewishCalendar.isRoshChodesh()) {
                result = hebrewDateFormatter.formatRoshChodesh(jewishCalendar)
                        .replace("Teves", "Tevet")
                        .replace("Tishrei", "Tishri");
            } else if (jewishCalendar.isErevRoshChodesh()) {
                jewishCalendar.forward(Calendar.DATE, 1);
                String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(jewishCalendar)
                        .replace("Teves", "Tevet")
                        .replace("Tishrei", "Tishri");
                jewishCalendar.setDate(currentDate);
                result = "Erev " + hebrewMonth;
            } else {
                result = "";
            }
        }
        return result;
    }

    /**
     * This method is the main method used to get the current holiday or special day and the next holiday or special day if there is one.
     * @return a string containing the current holiday and the next holiday if there is one
     */
    public String getSpecialDay(boolean addOmer) {
        String result = "";
        String yomTovOfToday = getYomTov();
        String yomTovOfNextDay = getYomTovForNextDay();

        if (yomTovOfToday.isEmpty() && yomTovOfNextDay.isEmpty()) {//NEEDED if both empty
            //do nothing
        } else if (yomTovOfToday.isEmpty() && !yomTovOfNextDay.startsWith("Erev")) {//if next day has yom tov
            if (isLocaleHebrew) {
                if (!yomTovOfNextDay.startsWith("ערב")) {
                    result = "ערב " + yomTovOfNextDay;
                }
            } else {
                result = "Erev " + yomTovOfNextDay;
            }
        } else if (!yomTovOfNextDay.isEmpty()
                && !yomTovOfNextDay.startsWith("Erev")
                && !yomTovOfToday.endsWith(yomTovOfNextDay)) {//if today and the next day have yom tov
            if (isLocaleHebrew) {
                if (!yomTovOfNextDay.startsWith("ערב")) {
                    result = yomTovOfToday + " / ערב " + yomTovOfNextDay;
                } else {
                    result = yomTovOfToday;
                }
            } else {
                result = yomTovOfToday + " / Erev " + yomTovOfNextDay;
            }
        } else {
            result = yomTovOfToday;
        }

        result = addTaanitBechorot(result);
        result = addRoshChodesh(result);
        if (addOmer) {
            result = addDayOfOmer(result);
        }
        result = replaceChanukahWithDayOfChanukah(result);
        return result;
    }

    /**
     * This method is used to add the Taanit Bechorot to the current holiday if it is on the current day.
     * @return a string containing taanit bechorot or erev taanit bechorot
     */
    private String addTaanitBechorot(String result) {
        if (isLocaleHebrew) {
            if (tomorrowIsTaanitBechorot()) {//edge case
                if (result.isEmpty()) {
                    result = "ערב תענית בכורות";
                } else {
                    result = "ערב תענית בכורות / " + result;
                }
            }
            if (jewishCalendar.isTaanisBechoros()) {
                if (result.isEmpty()) {
                    result = "תענית בכורות";
                } else {
                    result = "תענית בכורות / " + result;
                }
            }
        } else {
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
        }

        return result;
    }

    /**
     * This method is used to check if the next day is Taanit Bechorot.
     * @return a boolean value indicating if the next day is Taanit Bechorot
     */
    private boolean tomorrowIsTaanitBechorot() {
        jewishCalendar.forward(Calendar.DATE, 1);
        boolean result = jewishCalendar.isTaanisBechoros();
        jewishCalendar.setDate(currentDate);
        return result;
    }

    /**
     * This method is used to add the Rosh Chodesh to the current holiday if it is on the current day.
     * @return a string containing Rosh Chodesh or Erev Rosh Chodesh and the current holiday
     */
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

    /**
     * This method is used to add the Day of Chanuka to the current holiday if it is on the current day.
     * @return a string containing the Day of Chanuka and the current holiday
     */
    private String replaceChanukahWithDayOfChanukah(String result) {
        int dayOfChanukah = jewishCalendar.getDayOfChanukah();
        if (dayOfChanukah != -1) {
            if (!isLocaleHebrew) {
                result = result.replace("Chanukah", getOrdinal(dayOfChanukah) + " day of Chanukah");
            }
        }
        return result;
    }

    /**
     * This method is used to add the Day of Omer to the current holiday if it is on the current day.
     * @return a string containing the Day of Omer and the current holiday
     */
    private String addDayOfOmer(String result) {
        int dayOfOmer = jewishCalendar.getDayOfOmer();
        if (dayOfOmer != -1) {
            if (isLocaleHebrew) {
                if (!result.isEmpty()) {
                    result += " / " + formatHebrewNumber(dayOfOmer) + " יום של עומר";
                } else {
                    result = formatHebrewNumber(dayOfOmer) + " יום של עומר";
                }
            } else {
                if (!result.isEmpty()) {
                    result += " / " + getOrdinal(dayOfOmer) + " day of Omer";
                } else {
                    result = getOrdinal(dayOfOmer) + " day of Omer";
                }
            }
        }
        return result;
    }

    /**
     * This method is used to get the holiday for the current day in the form of a string.
     * @return a string containing the current holiday
     */
    private String getYomTov() {
        if (isLocaleHebrew) {
            return hebrewDateFormatter.formatYomTov(jewishCalendar);
        }
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
                return "Fast of the Seventeenth of Tammuz";
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
                    return "Shemini Atzeret & Simchat Torah";
                } else {
                    return "Shemini Atzeret";
                }
            case JewishCalendar.SIMCHAS_TORAH:
                if (!jewishCalendar.getInIsrael()) {
                    return "Shemini Atzeret & Simchat Torah";
                } else {
                    return "Shemini Atzeret";
                }
                //20 was erev chanuka which was deleted
            case JewishCalendar.CHANUKAH:
                return "Chanukah";
            case JewishCalendar.TENTH_OF_TEVES:
                return "Fast of Asarah Be'Tevet";
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
            case JewishCalendar.YOM_YERUSHALAYIM:
                return "Yom Yerushalayim";
            case JewishCalendar.LAG_BAOMER:
                return "Lag B'Omer";
            case JewishCalendar.SHUSHAN_PURIM_KATAN:
                return "Shushan Purim Katan";
            case JewishCalendar.ISRU_CHAG:
                return "Isru Chag";
            default:
                return "";
        }
    }

    /**
     * This method is used to get the holiday for the next day in the form of a string.
     * @return a string containing the next day's holiday
     */
    private String getYomTovForNextDay() {
        jewishCalendar.forward(Calendar.DATE, 1);
        String result = getYomTov();
        jewishCalendar.setDate(currentDate);
        return result;
    }

    /**
     * This method is used to get the holiday for the next day in the form of a int as the index in the @see JewishCalendar.getYomTovIndex()
     * @return an int containing the next day's holiday as an index
     */
    private int getYomTovIndexForNextDay() {
        jewishCalendar.forward(Calendar.DATE, 1);
        int result = jewishCalendar.getYomTovIndex();
        jewishCalendar.setDate(currentDate);
        return result;
    }

    /**
     * This method will return a string containing when to say tachanun for the current date.
     * Here are a list of holidays that it will return a string for no tachanun:
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
                || yomTovIndex == JewishCalendar.YOM_YERUSHALAYIM //tachanun erev before, however, Rav ovadia would not say on the day itself
                || jewishCalendar.isChanukah()
                || jewishCalendar.getJewishMonth() == JewishDate.NISSAN
                || (jewishCalendar.getJewishMonth() == JewishDate.SIVAN && jewishCalendar.getJewishDayOfMonth() <= 12)
                || (jewishCalendar.getJewishMonth() == JewishDate.TISHREI && jewishCalendar.getJewishDayOfMonth() >= 11)) {
            if (yomTovIndex == JewishCalendar.ROSH_HASHANA && jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {//Edge case for rosh hashana that falls on shabbat (Shulchan Aruch, Chapter 598 and Chazon Ovadia page 185)
                return "צדקתך";
            }//TODO check source on this
            if (isLocaleHebrew) {
                return "אין תחנון היום";
            }
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
            if (jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                if (isLocaleHebrew) {
                    return "אין תחנון היום";
                }
                return "There is no Tachanun today";
            }
            if (isLocaleHebrew) {
                return "תחנון נאמר רק בבוקר";
            }
            return "There is only Tachanun in the morning";
        }
        if (currentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return "צדקתך";
        }
        if (isLocaleHebrew) {
            return "יש תחנון";
        }
        return "There is Tachanun today";
    }

    /**
     * This method will return the jewish date as a string.
     * @return a string containing the jewish date. The format is: 15, Iyar 5782
     * If the Locale language is hebrew, it will be: ט"ו, אייר תשפ"ב
     */
    public String getJewishDate() {
        return jewishCalendar.toString().replace("Teves", "Tevet");
    }

    /**
     * This method will return whether or not this jewish year is a leap year.
     * @return "This year is a jewish leap year!" if the jewish year is a leap year, "This year is a not a jewish leap year!" otherwise
     */
    public String isJewishLeapYear() {
        if (jewishCalendar.isJewishLeapYear()) {
            if (isLocaleHebrew) {
                return "שנה זו היא שנת מעוברת";
            }
            return "This year is a jewish leap year!";
        } else {
            if (isLocaleHebrew) {
                return "שנה זו אינה שנת מעוברת";
            }
            return "This year is a not a jewish leap year!";
        }
    }

    /**
     * This method will return the parsha of the current week by rolling the calendar to saturday.
     * @return a string containing the parsha of the current week or "No Weekly Parsha"
     */
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
        if (!isLocaleHebrew) {
            hebrewDateFormatter.setHebrewFormat(false);//return to default setting
        }
        jewishCalendar.setDate(currentDate);

        if (parsha.isEmpty() && specialParsha.isEmpty()) {
            if (isLocaleHebrew) {
                return "אין פרשת השבוע";
            } else {
                return "No Weekly Parsha";
            }
        } else if (specialParsha.isEmpty()) {
            return parsha;
        } else {
            return parsha + " / " + specialParsha;
        }
    }

    /**
     * This method will return the ordinal of a number. For example, the number 1 will return 1st.
     * @param number the number to get the ordinal number of
     */
    private String getOrdinal(int number) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (number % 100) {
            case 11:
            case 12:
            case 13:
                return number + "th";
            default:
                return number + suffixes[number % 10];
        }
    }

    /**
     * This method will return the day of the week of the current date as a string in Hebrew.
     * @return a string containing the day of the week of the current date in Hebrew
     */
    public String getJewishDayOfWeek() {
        hebrewDateFormatter.setHebrewFormat(true);
        String result = "יום ";
        result += hebrewDateFormatter.formatDayOfWeek(jewishCalendar);
        if (!isLocaleHebrew) {
            hebrewDateFormatter.setHebrewFormat(false);
        }
        return result;
    }

    /**
     * This method will take a number and return the number in Hebrew.
     * @param num the number to convert to Hebrew
     * @return a string containing the number in Hebrew
     */
    public static String formatHebrewNumber(int num) {
        if (num <= 0 || num >= 6000) return "X";

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
                if (num > 100) {
                    result.append(let100[(num / 100) - 1]);
                }
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

    /**
     * This method will return whether or not the current date is the start time, middle, or end time
     * for Birchat HaLevana or an empty string
     * either of those days
     * @return a string containing the status of birchat halevana this month
     */
    public String getBirchatLevana() {
        Calendar sevenDays = Calendar.getInstance();
        sevenDays.setTime(jewishCalendar.getTchilasZmanKidushLevana7Days());
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        JewishCalendar latest = (JewishCalendar) jewishCalendar.clone();
        latest.setJewishDayOfMonth(14);

        if (jewishCalendar.getJewishMonth() != JewishDate.AV) {
            if (DateUtils.isSameDay(jewishCalendar.getGregorianCalendar(), sevenDays)) {
                if (isLocaleHebrew) {
                    return "בִּרְכַּת הַלְּבָנָה מַתְחִילָה הַלַּיְלָה";
                }
                return "Birchat HaLevana starts tonight";
            }
        } else {// Special case for Tisha Beav
            if (jewishCalendar.getJewishDayOfMonth() < 9) {
                return "";
            }
            if (jewishCalendar.isTishaBav()) {
                if (isLocaleHebrew) {
                    return "בִּרְכַּת הַלְּבָנָה מַתְחִילָה הַלַּיְלָה";
                }
                return "Birchat HaLevana starts tonight";
            }
        }

        if (jewishCalendar.getJewishDayOfMonth() == 14) {
            if (isLocaleHebrew) {
                return "הלילה האחרון לברכת הלבנה";
            }
            return "Last night for Birchat HaLevana";
        }

        if (jewishCalendar.getGregorianCalendar().getTime().after(sevenDays.getTime())
        && jewishCalendar.getGregorianCalendar().getTime().before(latest.getGregorianCalendar().getTime())) {
            if (isLocaleHebrew) {
                return "ברכת הלבנה עד ליל חמשה עשר";
            }
            return "Birchat HaLevana until " + sdf.format(latest.getGregorianCalendar().getTime());
        }
        return "";
    }

    /**
     * This method returns if Mashiv Haruach Umorid Hageshem is recited.
     * @return a string containing whether or not Mashiv Haruach Umorid Hageshem is recited.
     * For example, if the current date is the 7th day of the month of Nissan, it will return "Mashiv Haruach" in Hebrew and
     * if the current date is the 17th day of the month of Nissan, it will return "Morid Hatal"
     */
    public String getIsMashivHaruchOrMoridHatalSaid() {
        if (tefilaRules.isMashivHaruachRecited(jewishCalendar)) {
            return "משיב הרוח";
        }

        if (tefilaRules.isMoridHatalRecited(jewishCalendar)) {
            return "מוריד הטל";
        }
        return "";
    }

    /**
     * This method returns if Barcheinu or Barech Aleinu is recited.
     * @return a string containing whether or not Barcheinu or Barech Aleinu is recited in the Amidah.
     *  For example, if the current date is the 7th day of the month of Nissan, it will return "Barech Aleinu" in Hebrew and
     *  if the current date is the 17th day of the month of Nissan, it will return "Barcheinu"
     */
    public String getIsBarcheinuOrBarechAleinuSaid() {
        if (tefilaRules.isVeseinBerachaRecited(jewishCalendar)) {
            return "ברכנו";
        } else {
            return "ברך עלינו";
        }
    }

    /**
     * This method returns true if ulchaparat pesha is said in musaf on rosh chodesh.
     * Ulchaparat pesha is said in musaf on rosh chodesh in a leap year if the rosh chodesh is in the months of Cheshvan, Kislev, Teves, Shevat, Adar, or Adar II.
     * @return a string containing whether or not to say ulchaparat pesha in musaf on rosh chodesh
     */
    public String getIsUlChaparatPeshaSaid() {
        if (jewishCalendar.isRoshChodesh()) {
            if (jewishCalendar.isJewishLeapYear()) {
                int month = jewishCalendar.getJewishMonth();
                if (month == JewishCalendar.TISHREI || // Even if there is no Rosh Chodesh Tishri, Rosh Hodesh Cheshvan includes the 30th of Tishri
                    month == JewishCalendar.CHESHVAN ||
                    month == JewishCalendar.KISLEV ||
                    month == JewishCalendar.TEVES ||
                    month == JewishCalendar.SHEVAT ||
                    month == JewishCalendar.ADAR ||
                    month == JewishCalendar.ADAR_II) {
                    if (isLocaleHebrew) {
                        return "אֱמֹר וּלְכַפָּרַת פֶּשַׁע";
                    }
                    return "Say וּלְכַפָּרַת פֶּשַׁע";
                } else {
                    if (isLocaleHebrew) {
                        return "אַל תֹּאמַר וּלְכַפָּרַת פֶּשַׁע";
                    }
                    return "Do not say וּלְכַפָּרַת פֶּשַׁע";
                }
            } else {
                if (isLocaleHebrew) {
                    return "אַל תֹּאמַר וּלְכַפָּרַת פֶּשַׁע";
                }
                return "Do not say וּלְכַפָּרַת פֶּשַׁע";
            }
        }
        return "";
    }

    /**
     * This method returns a string containing the words "No Music" if you are not allowed to listen to music on the current day. (Pesach to lag
     * ba'omer and the three weeks)
     * @return a string containing whether or not you are allowed to listen to music on the current day.
     * If you are not allowed to listen to music, it will return "No Music". If you are allowed to listen to music, it will return an empty string.
     */
    public String isOKToListenToMusic() {

        if (jewishCalendar.getDayOfOmer() >= 8 && jewishCalendar.getDayOfOmer() <= 32) {
            if (isLocaleHebrew) {
                return "אין שמיעת מוזיקה";
            }
            return "No Music";
        } else if (jewishCalendar.getJewishMonth() == JewishDate.TAMMUZ) {
            if (jewishCalendar.getJewishDayOfMonth() >= 17) {
                if (isLocaleHebrew) {
                    return "אין שמיעת מוזיקה";
                }
                return "No Music";
            }
        } else if (jewishCalendar.getJewishMonth() == JewishDate.AV) {
            if (jewishCalendar.getJewishDayOfMonth() <= 9) {
                if (isLocaleHebrew) {
                    return "אין שמיעת מוזיקה";
                }
                return "No Music";
            }
        }
        return "";
    }

    /**
     * This method returns a string containing the words Hallel or Chatzi Hallel depending on the occasion.
     * Hallel is said on the first day of Pesach, Shavuot, Succot, and Shmini Atzeret. It is also said everyday of Chanukah and chol hamoed Succot.
     * Chatzi Hallel is said on Rosh Chodesh, Chol Hamoed Pesach, and the last day of Pesach.
     * @return a string containing whether or not to say Hallel or Chatzi Hallel in Hebrew.
     */
    public String getHallelOrChatziHallel() {
        int yomTovIndex = jewishCalendar.getYomTovIndex();
        if ((jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN
                && jewishCalendar.getJewishDayOfMonth() == 15)//First day of Pesach
                || (!jewishCalendar.getInIsrael() &&
                jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN
                && jewishCalendar.getJewishDayOfMonth() == 16)//First day of Pesach outside of israel
                || yomTovIndex == JewishCalendar.SHAVUOS
                || yomTovIndex == JewishCalendar.SUCCOS
                || yomTovIndex == JewishCalendar.SHEMINI_ATZERES
                || jewishCalendar.isCholHamoedSuccos()
                || jewishCalendar.isChanukah()) {
            return "הלל שלם";
        } else if (jewishCalendar.isRoshChodesh() || jewishCalendar.isCholHamoedPesach()
                || (jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN && jewishCalendar.getJewishDayOfMonth() == 21)
                || (!jewishCalendar.getInIsrael() && jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN && jewishCalendar.getJewishDayOfMonth() == 22)) {
            return "חצי הלל";
        } else {
            return "";
        }
    }

    public boolean is3Weeks() {
        if (jewishCalendar.getJewishMonth() == JewishDate.TAMMUZ) {
            return jewishCalendar.getJewishDayOfMonth() >= 17;
        } else if (jewishCalendar.getJewishMonth() == JewishDate.AV) {
            return jewishCalendar.getJewishDayOfMonth() < 9;
        }
        return false;
    }

    public boolean is9Days() {
        if (jewishCalendar.getJewishMonth() == JewishDate.AV) {
            return jewishCalendar.getJewishDayOfMonth() < 9;
        }
        return false;
    }

    public boolean isShevuahShechalBo() {
        if (jewishCalendar.getJewishMonth() != JewishDate.AV) {
            return false;
        }

        jewishCalendar.setJewishDayOfMonth(9);
        if (jewishCalendar.getDayOfWeek() == 1 || jewishCalendar.getDayOfWeek() == 7) {
            return false;//there is no shevua shechal bo if tisha beav falls out on a sunday or shabbat
        }
        jewishCalendar.setDate(currentDate);//reset

        JewishDate tishaBeav = new JewishDate(
                jewishCalendar.getJewishYear(),
                JewishDate.AV,
                8);// 1 day before to not include tisha beav itself

        ArrayList<Integer> daysOfShevuahShechalBo = new ArrayList<>();
        while (tishaBeav.getDayOfWeek() != 7) {
            daysOfShevuahShechalBo.add(tishaBeav.getJewishDayOfMonth());
            tishaBeav.setJewishDayOfMonth(tishaBeav.getJewishDayOfMonth() - 1);
        }
        return daysOfShevuahShechalBo.contains(jewishCalendar.getJewishDayOfMonth());
    }

    public boolean isSelichotSaid() {
        if (jewishCalendar.getJewishMonth() == JewishDate.ELUL) {
            if (!jewishCalendar.isRoshChodesh()) {
                return true;
            }
        }
        return jewishCalendar.isAseresYemeiTeshuva();
    }
}
