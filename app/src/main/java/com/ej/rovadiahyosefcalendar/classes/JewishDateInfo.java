package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.text.SpannableStringBuilder;

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
    public final HebrewDateFormatter hebrewDateFormatter;
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
    public boolean isLocaleHebrew = false;

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
        this.hebrewDateFormatter.setTransliteratedMonthList(new String[]{ "Nissan", "Iyar", "Sivan", "Tammuz", "Av", "Elul", "Tishri", "Ḥeshvan", "Kislev", "Tevet", "Shevat", "Adar", "Adar II", "Adar I" });
        this.hebrewDateFormatter.setTransliteratedHolidayList(new String[]{"Erev Pesach", "Pesach", "Chol Hamoed Pesach", "Pesach Sheni", "Erev Shavuot", "Shavuot", "Fast of the Seventeenth of Tammuz", "Tishah B'Av", "Tu B'Av", "Erev Rosh Hashana", "Rosh Hashana", "Fast of Gedalyah", "Erev Yom Kippur", "Yom Kippur", "Erev Succot", "Succot", "Chol Hamoed Succot", "Hoshana Rabbah", "Shemini Atzeret", "Shemini Atzeret & Simchat Torah", "Erev Chanukah", "Chanukah", "Fast of Asarah B'Tevet", "Tu B'Shevat", "Fast of Esther", "Purim", "Shushan Purim", "Purim Katan", "Rosh Chodesh", "Yom HaShoah", "Yom Hazikaron", "Yom Ha'atzmaut", "Yom Yerushalayim", "Lag La'Omer", "Shushan Purim Katan", "Isru Chag"});
        this.tefilaRules = new TefilaRules();
    }

    public void resetLocale(Context context) {
        this.jewishCalendar.mContext = context;
        if (Utils.isLocaleHebrew(context)) {
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
        tomorrow.jewishCalendar.mContext = this.jewishCalendar.mContext;
        tomorrow.isLocaleHebrew = this.isLocaleHebrew;
        tomorrow.hebrewDateFormatter.setHebrewFormat(this.isLocaleHebrew);
        tomorrow.setCalendar(clonedDate);
        return tomorrow;
    }

    public JewishDateInfo yesterday() {
        Calendar clonedDate = (Calendar) this.currentDate.clone(); // Clone the current date to avoid modifying it directly
        clonedDate.add(Calendar.DATE, -1); // Move to yesterday

        JewishDateInfo yesterday = new JewishDateInfo(this.jewishCalendar.getInIsrael());
        yesterday.jewishCalendar.mContext = this.jewishCalendar.mContext;
        yesterday.isLocaleHebrew = this.isLocaleHebrew;
        yesterday.hebrewDateFormatter.setHebrewFormat(this.isLocaleHebrew);
        yesterday.setCalendar(clonedDate);
        return yesterday;
    }

    public JewishDateInfo getCopy() {
        Calendar clonedDate = (Calendar) this.currentDate.clone(); // Clone the current date to avoid modifying it directly
        JewishDateInfo copy = new JewishDateInfo(this.jewishCalendar.getInIsrael());
        copy.jewishCalendar.mContext = this.jewishCalendar.mContext;
        copy.isLocaleHebrew = this.isLocaleHebrew;
        copy.hebrewDateFormatter.setHebrewFormat(this.isLocaleHebrew);
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
     * This method is used to check if Eruv Tavshilim (the food set aside to enable cooking on Yom Tov for Shabbat) is made today.
     * It will first check if there is candle lighting today and that we are not already in the Yom Tov.
     * Then it will check if it is Wednesday or Thursday. If it is Wednesday, it will check if Thursday and Friday are assur b'melacha.
     * If it is Thursday, it will check if Friday is assur b'melacha.
     * @return a boolean value indicating if Eruv Tavshilim is made today before going into Yom Tov
     */
    public boolean isEruvTavshilimMadeToday() {
        if (this.jewishCalendar.hasCandleLighting() && !this.jewishCalendar.isYomTovAssurBemelacha()) {// i.e. we are right before the Yom Tov starts and not into Yom Tov
            JewishCalendar tomorrow = tomorrow().getJewishCalendar();
            JewishCalendar afterTomorrow = tomorrow().tomorrow().getJewishCalendar();
            if (this.jewishCalendar.getDayOfWeek() == Calendar.WEDNESDAY) {
                return tomorrow.isYomTovAssurBemelacha() && afterTomorrow.isYomTovAssurBemelacha();// two day Yom Tov going into shabbat
            }
            if (this.jewishCalendar.getDayOfWeek() == Calendar.THURSDAY) {
                return tomorrow.isYomTovAssurBemelacha();// yom tov (1 or 2 days) going into shabbat
            }
        }
        return false;
    }

    /**
     * This method is used to get the current Rosh Chodesh or Erev Rosh Chodesh as a string.
     * For example: "Erev Rosh Chodesh Nissan" or "Rosh Chodesh Nissan"
     * @return a string containing the current Rosh Chodesh or Erev Rosh Chodesh
     */
    private String getRoshChodeshOrErevRoshChodesh() {
        String result;
        if (this.jewishCalendar.isRoshChodesh()) {
            result = hebrewDateFormatter.formatRoshChodesh(this.jewishCalendar);
        } else if (this.jewishCalendar.isErevRoshChodesh()) {
            String hebrewMonth = hebrewDateFormatter.formatRoshChodesh(tomorrow().getJewishCalendar());
            result = (isLocaleHebrew ? "ערב " : "Erev ") + hebrewMonth;
        } else {
            result = "";
        }
        if (!result.isEmpty() && !this.jewishCalendar.isErevRoshChodesh()) {// do not show the numbers for Erev Rosh Chodesh or if Rosh Chodesh is one day
            if (yesterday().jewishCalendar.getJewishDayOfMonth() == 30) {
                result += " (2)";
            } else if (tomorrow().jewishCalendar.getJewishDayOfMonth() == 1) {
                result += " (1)";
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
        String yomTovOfNextDay = tomorrow().getYomTov();

        if (!yomTovOfToday.isEmpty() || !yomTovOfNextDay.isEmpty()) {
            if (yomTovOfToday.isEmpty() && !yomTovOfNextDay.startsWith("Erev")) {//if next day has yom tov, but today doesn't
                if (isLocaleHebrew) {
                    if (!yomTovOfNextDay.startsWith("ערב")) {
                        result = "ערב " + yomTovOfNextDay;
                    }
                } else {
                    result = "Erev " + yomTovOfNextDay;
                }
            } else if (!yomTovOfToday.endsWith(yomTovOfNextDay)) {//if today and the next day have yom tov
                if (isLocaleHebrew) {
                    if (!yomTovOfNextDay.startsWith("ערב")) {
                        result = yomTovOfToday + " / ערב " + yomTovOfNextDay;
                    } else {
                        result = yomTovOfToday;
                    }
                } else {
                    if (!yomTovOfNextDay.startsWith("Erev")) {
                        result = yomTovOfToday + " / Erev " + yomTovOfNextDay;
                    } else {
                        result = yomTovOfToday;
                    }
                }
            } else {// cut off the second yom tov
                result = yomTovOfToday;
                if (isLocaleHebrew) {// small edge case for the 7th of pesach where I do not want the second day to get cut off
                    if (result.equals("חול המועד פסח") && !yomTovOfNextDay.equals(yomTovOfToday)) {
                        result = yomTovOfToday + " / ערב " + yomTovOfNextDay;
                    }
                }
                if (result.equals("Chol Hamoed Pesach") && !yomTovOfNextDay.equals(yomTovOfToday)) {
                    result = yomTovOfToday + " / Erev " + yomTovOfNextDay;
                }
            }
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
     * This method is used to get the current day yom tov/holiday as a string. Or an empty string if there is no holiday.
     * @return a string containing the {@link JewishCalendar} class's holiday as a string or an empty string if there is no holiday
     * @see JewishCalendar
     */
    private String getYomTov() {
        String result = hebrewDateFormatter.formatYomTov(this.jewishCalendar)
                .replace("פורים שושן", "שושן פורים")
                .replace("פורים שושן קטן", "שושן פורים קטן")
                .replace("ל״ג בעומר", "ל״ג לעומר");
        if (result.contains("Shemini Atzeret")) {
            if (getJewishCalendar().getInIsrael()) {
                result = "Shemini Atzeret & Simchat Torah";
            }
        }
        if (isPurimMeshulash()) {
            if (result.isEmpty()) {
                result = isLocaleHebrew ? "פורים משולש" : "Purim Meshulash";
            } else {// This should never happen, but just in case
                result += " / " + (isLocaleHebrew ? "פורים משולש" : "Purim Meshulash");
            }
        }
        return result;
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
     * Here are a list of days that affect tachanun:<br>
     * Every Rosh Chodesh<br>
     * Every Shabbat<br>
     * Every Yom Tov<br>
     * Chol HaMoed<br>
     * Isru Chag (day after chag)<br>
     * The entire month of Nissan<br>
     * Pesach Sheni (14th of Iyar)<br>
     * Lag Ba\'Omer<br>
     * Rosh Chodesh Sivan until the 12th of Sivan (7 days after Shavuot and the 12th is included)<br>
     * 9th of Av<br>
     * 15th of Av<br>
     * Erev Rosh Hashanah and Rosh Hashanah<br>
     * Erev Yom Kippur and Yom Kippur<br>
     * From the 11th of Tishri (day after Yom Kippur) until the end of Tishri<br>
     * Every day of Chanukah<br>
     * 15th of Shevat<br>
     * 14th and 15th of Adar I and Adar II<br>
     * Purim Meshulash for walled cities (safek included)<br>
     * Tu Be'Shvat<br>
     *
     * @return a String containing whether one of these possible results:
     * "There is Tachanun today",<br>
     * "Tachanun only in the morning",<br>
     * "No Tachanun today",<br>
     * "Some say Tachanun today",<br>
     * "Some skip Tachanun by mincha",<br>
     * "Some say Tachanun in the morning; no Tachanun by mincha".<br>
     * If {@link #isLocaleHebrew} is true, it will be:<br>
     * "אומרים תחנון",<br>
     * "אומרים תחנון רק בבוקר"<br>
     * "לא אומרים תחנון",<br>
     * "יש אומרים תחנון",<br>
     * "יש מדלגים תחנון במנחה",<br>
     * "יש אומרים תחנון בשחרית; אין תחנון במנחה"<br>
     * "צדקתך"
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
            if (yomTovIndex == JewishCalendar.YOM_YERUSHALAYIM || yomTovIndex == JewishCalendar.YOM_HAATZMAUT) {
                if (isLocaleHebrew) {
                    return "יש אומרים תחנון בשחרית; אין תחנון במנחה";
                }
                return "Some say Tachanun in the morning; no Tachanun by mincha";
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
     * @see WeeklySephardicHaftarot
     * @return a string containing the haftarah or haftorah of the current week
     */
    public CharSequence getThisWeeksHaftarah() {
        this.currentDate = this.jewishCalendar.getGregorianCalendar();
        Calendar parshaCalendar = this.jewishCalendar.getGregorianCalendar();

        while (parshaCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            parshaCalendar.add(Calendar.DATE, 1);
        }

        this.jewishCalendar.setDate(parshaCalendar);

        CharSequence thisWeekHaftara = WeeklySephardicHaftarot.Companion.formatWeeklyHaftara(this.jewishCalendar);
		SpannableStringBuilder haftarah = new SpannableStringBuilder();
        haftarah.append(isLocaleHebrew ? "מפטירין: " : "Haftara: \u202B");
        haftarah.append(thisWeekHaftara);

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
        } else {
            return "מוריד הטל";
        }
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
