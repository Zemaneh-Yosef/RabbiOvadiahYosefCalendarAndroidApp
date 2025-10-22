package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;

import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to get the information about the Jewish date. It is a helper class to manipulate the classes in the kosherjava library.
 * @author Elyahu Jacobi
 * @version 1.0
 * @since 1.0
 * @see com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
 * @see com.kosherjava.zmanim.hebrewcalendar.TefilaRules
 * @see com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
 * @see com.kosherjava.zmanim.hebrewcalendar.JewishDate
 */
public class JewishDateInfo {

    /**
     * The internal Jewish Calendar to manipulate and keep track of the date
     */
    private final JewishCalendarWithExtraMethods jewishCalendar;
    /**
     * The formatter class used to change the raw numbers/enums from the Jewish Calendar to text
     */
    private final HebrewDateFormatter hebrewDateFormatter;
    /**
     * A class that has multiple methods that can help you find out the "rules" of the day for tefilot/prayers
     */
    private final TefilaRules tefilaRules;
    /**
     * The internal gregorian calendar to manipulate and keep track of the date
     */
    private Calendar currentDate = Calendar.getInstance();
    /**
     * A boolean that in instantiated by the constructor that checks if the user's device is using Hebrew as it's main language
     */
    private boolean isLocaleHebrew = false;

    /**
     * Constructor of the class that initializes the objects and variables to the current date.
     * It will also check the user's device to see if it's using Hebrew as it's main language.
     * @param inIsrael boolean value that indicates if the user is in Israel or not
     */
    public JewishDateInfo(boolean inIsrael) {
        this.jewishCalendar = new JewishCalendarWithExtraMethods();
        this.jewishCalendar.setInIsrael(inIsrael);
        this.jewishCalendar.setUseModernHolidays(true);
        this.hebrewDateFormatter = new HebrewDateFormatter();
        this.hebrewDateFormatter.setUseGershGershayim(false);
        if (Utils.isLocaleHebrew()) {
            this.hebrewDateFormatter.setHebrewFormat(true);
            this.isLocaleHebrew = true;
        }
        this.tefilaRules = new TefilaRules();
    }

    public void resetLocale() {
        if (Utils.isLocaleHebrew()) {
            this.hebrewDateFormatter.setHebrewFormat(true);
            this.isLocaleHebrew = true;
        }
    }

    /**
     * This method is used to get the current jewish calendar object.
     * @return the current jewish calendar object
     */
    public JewishCalendarWithExtraMethods getJewishCalendar() {
        return this.jewishCalendar;
    }

    /**
     * This method is used to get the current jewishDateInfo object plus one day ahead.
     * @return the current jewish calendar object plus one day ahead
     */
    public JewishDateInfo tomorrow() {
        Calendar clonedDate = (Calendar) this.currentDate.clone(); // Clone the current date to avoid modifying it directly
        clonedDate.add(Calendar.DATE, 1); // Move to tomorrow

        JewishDateInfo tomorrow = new JewishDateInfo(this.jewishCalendar.getInIsrael());
        tomorrow.setCalendar(clonedDate);
        return tomorrow;
    }

    public JewishDateInfo yesterday() {
        Calendar clonedDate = (Calendar) this.currentDate.clone(); // Clone the current date to avoid modifying it directly
        clonedDate.add(Calendar.DATE, -1); // Move to tomorrow

        JewishDateInfo tomorrow = new JewishDateInfo(this.jewishCalendar.getInIsrael());
        tomorrow.setCalendar(clonedDate);
        return tomorrow;
    }

    public JewishDateInfo getCopy() {
        Calendar clonedDate = (Calendar) this.currentDate.clone(); // Clone the current date to avoid modifying it directly
        JewishDateInfo copy = new JewishDateInfo(this.jewishCalendar.getInIsrael());
        copy.setCalendar(clonedDate);
        return copy;
    }

    /**
     * This method is used to set the current date.
     * @param calendar the calendar to change the current date
     */
    public void setCalendar(Calendar calendar) {
        this.currentDate = calendar;
        this.jewishCalendar.setDate(this.currentDate);
    }

    public void forward() {
        this.currentDate.add(Calendar.DATE, 1);
        this.jewishCalendar.setDate(this.currentDate);
    }

    public void back() {
        this.currentDate.add(Calendar.DATE, -1);
        this.jewishCalendar.setDate(this.currentDate);
    }

    /**
     * This method is used to get the current Rosh Chodesh or Erev Rosh Chodesh as a string.
     * For example: "Erev Rosh Chodesh Nissan" or "Rosh Chodesh Nissan"
     * @return a string containing the current Rosh Chodesh or Erev Rosh Chodesh
     */
    private String getRoshChodeshOrErevRoshChodesh() {
        String result;
        if (isLocaleHebrew) {
            hebrewDateFormatter.setHebrewFormat(true);
            if (this.jewishCalendar.isRoshChodesh()) {
                result = hebrewDateFormatter.formatRoshChodesh(this.jewishCalendar);
            } else if (this.jewishCalendar.isErevRoshChodesh()) {
                String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(tomorrow().getJewishCalendar());
                result = "ערב " + hebrewMonth;
            } else {
                result = "";
            }
        } else {
            if (this.jewishCalendar.isRoshChodesh()) {
                result = hebrewDateFormatter.formatRoshChodesh(this.jewishCalendar)
                    .replace("Teves", "Tevet")
                    .replace("Tishrei", "Tishri");
            } else if (this.jewishCalendar.isErevRoshChodesh()) {
                String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(tomorrow().getJewishCalendar())
                    .replace("Teves", "Tevet")
                    .replace("Tishrei", "Tishri");
                result = "Erev " + hebrewMonth;
            } else {
                result = "";
            }
        }
        return result;
    }

    /**
     * This method is the main method used to get the current holiday or special day and the next holiday or special day if there is one.
     * Strings produced by this method will look like this: "Lag Ba'Omer" or if there's more than one
     * special thing occurring on that day or the next day, it will separate each entry with a slash "/".
     * If {@link #isLocaleHebrew} is set to true by the constructor, the string produced will be in hebrew.
     * @return a string containing the current special day and the next special day if there is one.
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
     * This method checks if Taanit Bechorot is on the the current day and if it is, it will concat it to the string passed in.
     * If {@link #isLocaleHebrew} is set to true by the constructor, the string produced will be in hebrew.
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
            if (this.jewishCalendar.isTaanisBechoros()) {
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
            if (this.jewishCalendar.isTaanisBechoros()) {
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
     * Utility method to check if the next day is Taanit Bechorot.
     * @return a boolean value indicating if the next day is Taanit Bechorot
     */
    private boolean tomorrowIsTaanitBechorot() {
        return tomorrow().getJewishCalendar().isTaanisBechoros();
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
        int dayOfChanukah = this.jewishCalendar.getDayOfChanukah();
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
    public String addDayOfOmer(String result) {
        int dayOfOmer = this.jewishCalendar.getDayOfOmer();
        if (dayOfOmer != -1) {
            if (isLocaleHebrew) {
                hebrewDateFormatter.setUseGershGershayim(true);
                if (!result.isEmpty()) {
                    result += " / " + hebrewDateFormatter.formatHebrewNumber(dayOfOmer) + " ימים לעומר (לפני השקיעה)";
                } else {
                    result = hebrewDateFormatter.formatHebrewNumber(dayOfOmer) + " ימים לעומר (לפני השקיעה)";
                }
                hebrewDateFormatter.setUseGershGershayim(false);
            } else {
                if (!result.isEmpty()) {
                    result += " / " + getOrdinal(dayOfOmer) + " day of Omer (before sunset)";
                } else {
                    result = getOrdinal(dayOfOmer) + " day of Omer (before sunset)";
                }
            }
        }
        return result;
    }

    /**
     * This method is used to convert the int constants in the {@link JewishCalendar} class into a string.
     * TODO: consider replacing this method with a simpler one that uses the {@link HebrewDateFormatter#setTransliteratedHolidayList(String[])} method
     * @return a string containing the {@link JewishCalendar} class's int constants as a string
     * @see JewishCalendar
     */
    private String getYomTov() {
        if (isLocaleHebrew) {
            if (isPurimMeshulash()) {
                return "פורים משולש";
            }
            return hebrewDateFormatter.formatYomTov(this.jewishCalendar)
                    .replace("פורים שושן", "שושן פורים")
                    .replace("פורים שושן קטן", "שושן פורים קטן");
        }
        switch (this.jewishCalendar.getYomTovIndex()) {
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
                if (this.jewishCalendar.getInIsrael()) {
                    return "Shemini Atzeret & Simchat Torah";
                } else {
                    return "Shemini Atzeret";
                }
            case JewishCalendar.SIMCHAS_TORAH:
                if (!this.jewishCalendar.getInIsrael()) {
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
            case JewishCalendar.YOM_HAATZMAUT:
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
                if (isPurimMeshulash()) {
                    return "Purim Meshulash";
                }
                return "";
        }
    }

    /**
     * Utility method used to get the yom tov/holiday for the next day in the form of a string.
     * @return a string containing the next day's yom tov/holiday
     */
    private String getYomTovForNextDay() {
        return tomorrow().getYomTov();
    }

    /**
     * Utility method used to get the yom tov/holiday int constant for the next day
     * @see JewishCalendar#getYomTovIndex()
     * @return an int containing the next day's holiday as an index
     */
    private int getYomTovIndexForNextDay() {
        return tomorrow().getJewishCalendar().getYomTovIndex();
    }

    /**
     * This method will return a string containing when to say tachanun for the current date.
     * Here are a list of holidays that it will return a string for no tachanun:
     * TODO fix this list
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
        int yomTovIndex = this.jewishCalendar.getYomTovIndex();
        if (this.jewishCalendar.isRoshChodesh()
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
                || this.jewishCalendar.isChanukah()
                || this.jewishCalendar.getJewishMonth() == JewishDate.NISSAN
                || (this.jewishCalendar.getJewishMonth() == JewishDate.SIVAN && this.jewishCalendar.getJewishDayOfMonth() <= 12)
                || (this.jewishCalendar.getJewishMonth() == JewishDate.TISHREI && this.jewishCalendar.getJewishDayOfMonth() >= 11)) {
            if (yomTovIndex == JewishCalendar.ROSH_HASHANA && this.jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {//Edge case for rosh hashana that falls on shabbat (Shulchan Aruch, Chapter 598 and Chazon Ovadia page 185)
                return "צדקתך";
            }
            if (isLocaleHebrew) {
                return "לא אומרים תחנון";
            }
            return "No Tachanun today";
        }
        int yomTovIndexForNextDay = getYomTovIndexForNextDay();
        if (this.jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                || yomTovIndexForNextDay == JewishCalendar.PURIM
                || yomTovIndexForNextDay == JewishCalendar.TISHA_BEAV
                || yomTovIndexForNextDay == JewishCalendar.CHANUKAH
                || yomTovIndexForNextDay == JewishCalendar.TU_BEAV
                || yomTovIndexForNextDay == JewishCalendar.TU_BESHVAT
                || yomTovIndexForNextDay == JewishCalendar.LAG_BAOMER
                || yomTovIndexForNextDay == JewishCalendar.PESACH_SHENI
                || yomTovIndexForNextDay == JewishCalendar.PURIM_KATAN
                || this.jewishCalendar.isErevRoshChodesh()) {
            if (this.jewishCalendar.getGregorianCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                if (isLocaleHebrew) {
                    return "לא אומרים תחנון";
                }
                return "No Tachanun today";
            }
            if (isLocaleHebrew) {
                return "אומרים תחנון רק בבוקר";
            }
            return "Tachanun only in the morning";
        }
        // According to Rabbi Meir Gavriel Elbaz, Rabbi Ovadiah would only skip tachanun on the day of Yom Yerushalayim itself as is the custom of the Yeshiva of Yechaveh Daat.
        // He WOULD say tachanun on Erev Yom Yerushalayim and on Yom Ha'atmaut. However, since there are disagreements (for example: Rabbi Yonatan Nacson writes that you may skip tachanun on both days), it was recommended for the app to just say that "Some say tachanun" on both days.
        if (yomTovIndex == JewishCalendar.YOM_YERUSHALAYIM || yomTovIndex == JewishCalendar.YOM_HAATZMAUT) {
            if (isLocaleHebrew) {
                return "יש אומרים תחנון";
            }
            return "Some say Tachanun today";
        }
        if (yomTovIndexForNextDay == JewishCalendar.YOM_YERUSHALAYIM || yomTovIndexForNextDay == JewishCalendar.YOM_HAATZMAUT) {
            if (isLocaleHebrew) {
                return "יש מדלגים תחנון במנחה";
            }
            return "Some skip Tachanun by mincha";
        }

        if (this.currentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return "צדקתך";
        }
        if (isLocaleHebrew) {
            return "אומרים תחנון";
        }
        return "There is Tachanun today";
    }

    /**
     * This method will return whether or not this jewish year is a leap year.
     * @return "This year is a jewish leap year!" if the jewish year is a leap year, "This year is a not a jewish leap year!" otherwise.
     * If {@link #isLocaleHebrew} is true, it will be: "שנה מעוברת" or "אינה שנת מעוברת" respectively
     */
    public String isJewishLeapYear() {
        if (this.jewishCalendar.isJewishLeapYear()) {
            if (isLocaleHebrew) {
                return "שנה מעוברת";
            }
            return "This year is a jewish leap year!";
        } else {
            if (isLocaleHebrew) {
                return "אינה שנת מעוברת";
            }
            return "This year is a not a jewish leap year!";
        }
    }

    /**
     * This method will return the parsha of the current week by rolling the calendar to saturday.
     * @return a string containing the parsha of the current week or "No Weekly Parsha"
     */
    public String getThisWeeksParsha() {

        this.currentDate = this.jewishCalendar.getGregorianCalendar();
        Calendar parshaCalendar = this.jewishCalendar.getGregorianCalendar();

        while (parshaCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            parshaCalendar.add(Calendar.DATE, 1);
        }

        this.jewishCalendar.setDate(parshaCalendar);

        hebrewDateFormatter.setHebrewFormat(true);
        String parsha = hebrewDateFormatter.formatParsha(this.jewishCalendar);
        String specialParsha = hebrewDateFormatter.formatSpecialParsha(this.jewishCalendar);
        if (!isLocaleHebrew) {
            hebrewDateFormatter.setHebrewFormat(false);//return to default setting
        }
        this.jewishCalendar.setDate(this.currentDate);

        if (parsha.isEmpty() && specialParsha.isEmpty()) {
            if (isLocaleHebrew) {
                return "אין פרשת שבוע";
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
     * This method will return the haftarah or haftorah of the current week by rolling the calendar to saturday.
     * @see WeeklyHaftarahReading
     * @return a string containing the haftarah or haftorah of the current week
     */
    public String getThisWeeksHaftarah() {

        this.currentDate = this.jewishCalendar.getGregorianCalendar();
        Calendar parshaCalendar = this.jewishCalendar.getGregorianCalendar();

        while (parshaCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            parshaCalendar.add(Calendar.DATE, 1);
        }

        this.jewishCalendar.setDate(parshaCalendar);
        String haftarah = WeeklyHaftarahReading.getThisWeeksHaftarah(this.jewishCalendar)
                .replace("מפטירין", Utils.isLocaleHebrew() ? "מפטירין" : "Haftarah: \u202B");
        this.jewishCalendar.setDate(this.currentDate);
        return haftarah;
    }

    /**
     * This method will return the makam of the current week by rolling the calendar to saturday.
     * @see MakamJCal
     * @return a map containing the makam of the current week according to multiple books
     */
    public Map<String, List<MakamJCal.Makam>> getThisWeeksMakam() {
        this.currentDate = this.jewishCalendar.getGregorianCalendar();
        Calendar parshaCalendar = this.jewishCalendar.getGregorianCalendar();

        while (parshaCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            parshaCalendar.add(Calendar.DATE, 1);
        }

        this.jewishCalendar.setDate(parshaCalendar);
        Map<String, List<MakamJCal.Makam>> makamData = MakamJCal.Companion.getMakamData(this.jewishCalendar);
        this.jewishCalendar.setDate(this.currentDate);
        return makamData;
    }

    /**
     * This method will return the ordinal of a number. For example, the number 1 will return 1st.
     * @param number the number to get the ordinal number of
     */
    private String getOrdinal(int number) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        return switch (number % 100) {
            case 11, 12, 13 -> number + "th";
            default -> number + suffixes[number % 10];
        };
    }

    /**
     * This method will return the day of the week of the current date as a string in Hebrew.
     * @return a string containing the day of the week of the current date in Hebrew
     */
    public String getJewishDayOfWeek() {
        hebrewDateFormatter.setHebrewFormat(true);
        String result = hebrewDateFormatter.formatDayOfWeek(this.jewishCalendar).replace("ששי", "שישי");
        if (!isLocaleHebrew) {
            hebrewDateFormatter.setHebrewFormat(false);
        }
        return result;
    }

    /**
     * This method will return whether or not the current date is the start time, middle, or end time
     * for Birchat HaLevana or an empty string on neither of those days
     * @return a string containing the status of birchat halevana this month
     */
    public String getBirchatLevana() {
        Calendar sevenDays = Calendar.getInstance();
        sevenDays.setTime(this.jewishCalendar.getTchilasZmanKidushLevana7Days());
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        JewishCalendar latest = (JewishCalendar) this.jewishCalendar.clone();
        latest.setJewishDayOfMonth(14);

        if (this.jewishCalendar.getJewishMonth() != JewishDate.AV) {
            if (DateUtils.isSameDay(this.jewishCalendar.getGregorianCalendar(), sevenDays)) {
                if (isLocaleHebrew) {
                    return "ברכת הלבנה מתחילה הלילה";
                }
                return "Birkat Halevana starts tonight";
            }
        } else {// Special case for Tisha Beav, see Shulchan Aruch Orach Chaim 426:2
            if (this.jewishCalendar.getJewishDayOfMonth() < 9) {
                return "";
            }
            if (this.jewishCalendar.isTishaBav()) {
                if (isLocaleHebrew) {
                    return "ברכת הלבנה מתחילה הלילה";
                }
                return "Birkat Halevana starts tonight";
            }
        }

        if (this.jewishCalendar.getJewishDayOfMonth() == 14) {
            if (isLocaleHebrew) {
                return "לילה אחרון לברכת הלבנה";
            }
            return "Last night for Birkat Halevana";
        }

        if (this.jewishCalendar.getGregorianCalendar().getTime().after(sevenDays.getTime())
        && this.jewishCalendar.getGregorianCalendar().getTime().before(latest.getGregorianCalendar().getTime())) {
            if (isLocaleHebrew) {
                return "ברכת הלבנה עד ליל טו'";
            }
            return "Birkat Halevana until " + sdf.format(latest.getGregorianCalendar().getTime());
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
        if (tefilaRules.isMashivHaruachRecited(this.jewishCalendar)) {
            return "משיב הרוח";
        }

        if (tefilaRules.isMoridHatalRecited(this.jewishCalendar)) {
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
        if (tefilaRules.isVeseinBerachaRecited(this.jewishCalendar)) {
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
        if (this.jewishCalendar.isRoshChodesh()) {
            if (this.jewishCalendar.isJewishLeapYear()) {
                int month = this.jewishCalendar.getJewishMonth();
                if (month == JewishCalendar.TISHREI || // Even if there is no Rosh Chodesh Tishri, Rosh Hodesh Cheshvan includes the 30th of Tishri
                    month == JewishCalendar.CHESHVAN ||
                    month == JewishCalendar.KISLEV ||
                    month == JewishCalendar.TEVES ||
                    month == JewishCalendar.SHEVAT ||
                    month == JewishCalendar.ADAR ||
                    month == JewishCalendar.ADAR_II) {
                    if (isLocaleHebrew) {
                        return "אומרים וּלְכַפָּרַת פֶּשַׁע";
                    }
                    return "Say וּלְכַפָּרַת פֶּשַׁע";
                } else {
                    if (isLocaleHebrew) {
                        return "לא אומרים וּלְכַפָּרַת פֶּשַׁע";
                    }
                    return "Do not say וּלְכַפָּרַת פֶּשַׁע";
                }
            } else {
                if (isLocaleHebrew) {
                    return "לא אומרים וּלְכַפָּרַת פֶּשַׁע";
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
        if (this.jewishCalendar.getDayOfOmer() >= 8 && this.jewishCalendar.getDayOfOmer() <= 32) {
            if (isLocaleHebrew) {
                return "לא שומעים מוזיקה";
            }
            return "No Music";
        } else if (this.jewishCalendar.getJewishMonth() == JewishDate.TAMMUZ) {
            if (this.jewishCalendar.getJewishDayOfMonth() >= 17) {
                if (isLocaleHebrew) {
                    return "לא שומעים מוזיקה";
                }
                return "No Music";
            }
        } else if (this.jewishCalendar.getJewishMonth() == JewishDate.AV) {
            if (this.jewishCalendar.getJewishDayOfMonth() <= 9) {
                if (isLocaleHebrew) {
                    return "לא שומעים מוזיקה";
                }
                return "No Music";
            }
        }
        return "";
    }

    /**
     * This method returns a string containing the words הלל שלם or חצי הלל depending on the occasion.
     * Hallel is said on the first day of Pesach, Shavuot, Succot, and Shmini Atzeret. It is also said everyday of Chanukah and chol hamoed Succot.
     * Chatzi Hallel is said on Rosh Chodesh, Chol Hamoed Pesach, and the last day of Pesach.
     * @return a string containing whether הלל שלם or חצי הלל in Hebrew. It will be empty if there is no hallel said.
     */
    public String getHallelOrChatziHallel() {
        int yomTovIndex = this.jewishCalendar.getYomTovIndex();
        if ((this.jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN
                && this.jewishCalendar.getJewishDayOfMonth() == 15)//First day of Pesach
                || (!this.jewishCalendar.getInIsrael() &&
                this.jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN
                && this.jewishCalendar.getJewishDayOfMonth() == 16)//First day of Pesach outside of israel
                || yomTovIndex == JewishCalendar.SHAVUOS
                || yomTovIndex == JewishCalendar.SUCCOS
                || yomTovIndex == JewishCalendar.SHEMINI_ATZERES
                || this.jewishCalendar.isSimchasTorah()// 2nd day of shemini atzeret, only outside of Israel
                || this.jewishCalendar.isCholHamoedSuccos()
                || this.jewishCalendar.isChanukah()) {
            return "הלל שלם";
        } else if (this.jewishCalendar.isRoshChodesh() || this.jewishCalendar.isCholHamoedPesach()
                || (this.jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN && this.jewishCalendar.getJewishDayOfMonth() == 21)
                || (!this.jewishCalendar.getInIsrael() && this.jewishCalendar.getJewishMonth() == JewishCalendar.NISSAN && this.jewishCalendar.getJewishDayOfMonth() == 22)) {
            return "חצי הלל";
        } else {
            return "";
        }
    }

    /**
     * Returns true if the current date is within the three weeks known as Bein Hametzarim.
     * @return Returns true if the current date is within the three weeks known as Bein Hametzarim
     */
    public boolean is3Weeks() {
        if (this.jewishCalendar.getJewishMonth() == JewishDate.TAMMUZ) {
            return this.jewishCalendar.getJewishDayOfMonth() >= 17;
        } else if (this.jewishCalendar.getJewishMonth() == JewishDate.AV) {
            return this.jewishCalendar.getJewishDayOfMonth() < 9;
        }
        return false;
    }

    /**
     * Returns true if the current date is within the nine days between Rosh Chodesh Av and Tisha Beav.
     * @return Returns true if the current date is within the nine days.
     */
    public boolean is9Days() {
        if (this.jewishCalendar.getJewishMonth() == JewishDate.AV) {
            return this.jewishCalendar.getJewishDayOfMonth() < 9;
        }
        return false;
    }

    /**
     * Returns true if the current date is within Shevua Shechal Bo, which occurs between Sunday of the week that Tisha Beav falls on until Tisha Beav.
     * @return Returns true if the current date is within Shevua Shechal Bo.
     */
    public boolean isShevuahShechalBo() {
        if (this.jewishCalendar.getJewishMonth() != JewishDate.AV) {
            return false;
        }

        this.jewishCalendar.setJewishDayOfMonth(9);
        if (this.jewishCalendar.getDayOfWeek() == Calendar.SUNDAY || this.jewishCalendar.getDayOfWeek() == Calendar.SATURDAY) {
            this.jewishCalendar.setDate(this.currentDate);//reset
            return false;//there is no shevua shechal bo if tisha beav falls out on a sunday or shabbat
        }
        this.jewishCalendar.setDate(this.currentDate);//reset

        JewishDate tishaBeav = new JewishDate(
                this.jewishCalendar.getJewishYear(),
                JewishDate.AV,
                8);// 1 day before to not include tisha beav itself

        ArrayList<Integer> daysOfShevuahShechalBo = new ArrayList<>();
        while (tishaBeav.getDayOfWeek() != Calendar.SATURDAY) {
            daysOfShevuahShechalBo.add(tishaBeav.getJewishDayOfMonth());
            tishaBeav.setJewishDayOfMonth(tishaBeav.getJewishDayOfMonth() - 1);
        }
        return daysOfShevuahShechalBo.contains(this.jewishCalendar.getJewishDayOfMonth());
    }

    /**
     * Returns true if Selichot are said on the current date, which occurs between the beginning of Elul (excluding Rosh Chodesh) until Yom Kippur.
     * @return Returns true Selichot are said on the current date.
     */
    public boolean isSelichotSaid() {
        if (this.jewishCalendar.getJewishMonth() == JewishDate.ELUL) {
            if (!this.jewishCalendar.isRoshChodesh()) {
                return true;
            }
        }
        return this.jewishCalendar.isAseresYemeiTeshuva();
    }

    /**
     * This method returns whether or not the current year is a Shmita year according to the
     * general established minhag. It should be noted that Rashi and other rishonim hold that the
     * year before what this method returns is actually the year of Shmita.
     *
     * @return a boolean whether or not the current year is Shmita according to the general minhag
     * NOTE: Some Rishonim hold that the year before this one is the Shmita year.
     */
    public boolean isShmitaYear() {
        return this.jewishCalendar.getJewishYear() % 7 == 0;
    }

    /**
     * This method returns which year of the shmita cycle is the current hebrew year in.
     * 0 = Shmita, 1 = First Year, 2 = Second Year, 3 = Third Year, 4 = Fourth Year, 5 = Fifth Year, 6 = Sixth Year.
     *
     * @return an int indicating what year of the shmita cycle the current hebrew year is in.
     * NOTE: Some Rishonim hold that the year of shmita is a year off.
     */
    public int getYearOfShmitaCycle() {
        return this.jewishCalendar.getJewishYear() % 7;
    }

    /**
     * Returns true if for the current date (i.e. the night before) we say Tikkun Chatzot.
     * @return Returns true if for the current date (i.e. the night before) we say Tikkun Chatzot
     * @see #isOnlyTikkunLeiaSaid(boolean)
     */
    public boolean isNightTikkunChatzotSaid() {
        // These are all days that Tikkun Chatzot is not said at all, we NOT it to know if Tikkun Chatzot IS said
        return !(this.jewishCalendar.getDayOfWeek() == Calendar.SATURDAY ||
                this.jewishCalendar.isRoshHashana() ||
                this.jewishCalendar.isYomKippur() ||
                this.jewishCalendar.getYomTovIndex() == JewishCalendar.SUCCOS ||
                this.jewishCalendar.isShminiAtzeres() ||
                this.jewishCalendar.isSimchasTorah() ||
                this.jewishCalendar.isPesach() ||
                this.jewishCalendar.isShavuos());
    }

    /**
     * Returns true if for the current date (daytime) we say Tikkun Chatzot. The minhag is to say Tikkun Rachel during the three weeks from chatzot
     * until sunset.
     * @return Returns true if for the current date (daytime) we say Tikkun Chatzot (Tikkun Rachel)
     * @see #is3Weeks()
     * @see #isOnlyTikkunLeiaSaid(boolean)
     */
    public boolean isDayTikkunChatzotSaid() {
        String tachanun = getIsTachanunSaid();
        // Tikkun Rachel is said during the daytime for the three weeks, but not in these cases. Tikkun Rachel IS said on Erev Tisha Beav, even though some say that the custom is to NOT say it then, Maran Rabbi Ovadia said to say it on Erev Tisha Beav as well.
        return !((this.jewishCalendar.isErevRoshChodesh() && this.jewishCalendar.getJewishMonth() == JewishDate.TAMMUZ) ||// Use tammuz to check for erev rosh chodesh Av
                this.jewishCalendar.isRoshChodesh() ||// rosh chodesh Av
                this.jewishCalendar.getDayOfWeek() == Calendar.FRIDAY ||
                this.jewishCalendar.getDayOfWeek() == Calendar.SATURDAY ||
                tachanun.equals("No Tachanun today") || tachanun.equals("לא אומרים תחנון") ||
                tachanun.equals("Tachanun only in the morning") || tachanun.equals("אומרים תחנון רק בבוקר"));
    }

    /**
     * Returns true if for the current date (i.e. the night before) we say the SECOND part of Tikkun Chatzot i.e. Tikkun Leia. Tikkun Leia contains
     * prayers that praise Hashem for his glory. It is usually not skipped, however, there are exceptions like Tisha Beav night.
     * @return Returns true if for the current date (i.e. the night before) we say the SECOND part of Tikkun Chatzot i.e. Tikkun Leia
     */
    public boolean isOnlyTikkunLeiaSaid(boolean forNightTikkun) {
        if (forNightTikkun) {
            // These are days where we ONLY say Tikkun Leia
            return (this.jewishCalendar.isAseresYemeiTeshuva() ||
                    this.jewishCalendar.isCholHamoedSuccos() ||
                    this.jewishCalendar.getDayOfOmer() != -1 ||
                    (this.jewishCalendar.getInIsrael() && isShmitaYear()) ||
                    getIsTachanunSaid().equals("No Tachanun today") || getIsTachanunSaid().equals("לא אומרים תחנון") ||
                    isAfterTheMoladAndBeforeRoshChodesh());
            // Tikkun Rachel is also skipped in the house of a Mourner, Chatan, or Brit Milah (Specifically the father of the boy)
        } else { // for day tikkun, we do not say Tikkun Rachel if there is no tachanun
            return getIsTachanunSaid().equals("No Tachanun today") || getIsTachanunSaid().equals("לא אומרים תחנון");
        }
    }

    /**
     * Based off of Chazon Ovadia Yamim Noraim pg 46 that you do not say Tikkun Chatzot if the Molad has passed and only Tikkun Leia is said.
     * @return if the CURRENT SYSTEM TIME is after the molad and before Rosh Chodesh
     */
    public boolean isAfterTheMoladAndBeforeRoshChodesh() {
        int currentHebrewMonth = this.jewishCalendar.getJewishMonth();
        while (currentHebrewMonth == this.jewishCalendar.getJewishMonth()) {
            this.jewishCalendar.forward(Calendar.DATE, 1); // go forward until the next month
        }
        Date molad = this.jewishCalendar.getMoladAsDate(); // now we can get the molad for the next month
        this.jewishCalendar.setDate(this.currentDate); // reset
        while (!this.jewishCalendar.isRoshChodesh()) {
            this.jewishCalendar.forward(Calendar.DATE, 1); // go forward until the next rosh chodesh
        }
        Date roshChodesh = this.jewishCalendar.getGregorianCalendar().getTime();
        this.jewishCalendar.setDate(this.currentDate); // reset
        return molad.before(new Date()) && roshChodesh.after(new Date()) && !this.jewishCalendar.isRoshChodesh(); // Tikkun Leia (only) is said if it is after the molad but before Rosh Chodesh, this condition is time based even though all the other methods are date based
    }

    /**
     * Checks if yesterday was a Saturday and Shushan Purim in order to determine if today is Purim Meshulash
     * @return if today is Purim Meshulash
     */
    public boolean isPurimMeshulash() {
        Calendar clonedDate = (Calendar) this.currentDate.clone();// Clone the current date to avoid modifying it directly
        JewishCalendar yesterday = new JewishCalendar();
        yesterday.setDate(clonedDate);
        yesterday.back(); // Move to yesterday
        return yesterday.getYomTovIndex() == JewishCalendar.SHUSHAN_PURIM && yesterday.getDayOfWeek() == Calendar.SATURDAY;
    }

}
