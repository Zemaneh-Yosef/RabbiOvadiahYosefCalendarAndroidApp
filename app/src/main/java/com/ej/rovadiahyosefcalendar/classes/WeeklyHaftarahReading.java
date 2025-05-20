package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

/**
 * This class's main goal is to return the Weekly Haftorah reading said after the Weekly Parasha
 * reading. Which readings to say were taken from the Chumash "L'maan Shemo B'Ahavah" according to
 * the Sephardic Minhag.
 * @see WeeklyParashaReadings
 */
public class WeeklyHaftarahReading {

    /**
     * This method returns a string that contains the weekly Haftorah. The {@link JewishCalendar}
     * object passed into this method should be set to Saturday because the {@link JewishCalendar#getParshah()}
     * method returns {@link com.kosherjava.zmanim.hebrewcalendar.JewishCalendar.Parsha#NONE} during
     * the week.
     * @param jewishCalendar the JewishCalendar object set to Saturday
     * @return The haftorah for this week as a string
     */
    public static String getThisWeeksHaftarah(JewishCalendar jewishCalendar) {
        String result = "מפטירין ";
        switch (jewishCalendar.getSpecialShabbos()) {
            case SHKALIM:
                result += "\"ויכרת יהוידע\" מלכים ב י\"א";
                return result;
            case ZACHOR:
                result += "\"ויאמר שמואל\" שמואל א ט\"ו";
                return result;
            case PARA:
                result += "\"ויהי דבר\" יחזקאל ל\"ו";
                return result;
            case HACHODESH:
                result += "\"כה אמר\" יחזקאל מ\"ה";
                return result;
            case HAGADOL:
                result += "\"וערבה\" מלאכי ג";
                return result;
        }

        switch (jewishCalendar.getParshah()) {
            case BERESHIS:
                result += "\"כה אמר\" ישעיה מ\"ב";
                break;
            case NOACH:
            case KI_SEITZEI:
                result += "\"רני עקרה\" ישעיה נ\"ד";
                break;
            case LECH_LECHA:
                result += "\"למה תאמר\" ישעיה מ";
                break;
            case VAYERA:
                result += "\"ואשה אחת\" מלכים ב ד";
                break;
            case CHAYEI_SARA:
                result += "\"והמלך דוד\" מלכים א א";
                break;
            case TOLDOS:
                result += "\"משא דבר\" מלאכי א";
                break;
            case VAYETZEI:
                result += "\"ועמי תלואים\" הושע י\"א";
                break;
            case VAYISHLACH:
                result += "\"חזון עובדיה\" עובדיה";
                break;
            case VAYESHEV:
                if (jewishCalendar.isChanukah()) {// Vayeshev either happens on the first shabbat of chanukah or not on chanukah at all
                    result += "\"רני ושמחי\" זכריה ב";
                } else {
                    result += "\"כה אמר\" עמוס ב";
                }
                break;
            case MIKETZ:
                // Even if it's Rosh Chodesh or Erev Rosh Chodesh, we say the haftorah of chanukah
                if (jewishCalendar.getDayOfChanukah() == 8 || jewishCalendar.getDayOfChanukah() == 7) {// Second Shabbat of Chanukah
                    result += "\"ויעש חירום\" מלכים א ז";
                    return result;
                } else if (jewishCalendar.isChanukah()) {// First Shabbat of Chanukah
                    result += "\"רני ושמחי\" זכריה ב";
                    return result;
                } else  {// Regular week
                    result += "\"ויקץ שלמה\" מלכים א ג";
                    // Do not return here in case it's rosh chodesh
                }
                break;
            case VAYIGASH:
                result += "\"ויהי דבר\" יחזקאל ל\"ז";
                break;
            case VAYECHI:
                result += "\"ויקרבו\" מלכים א ב";
                break;
            case SHEMOS:
                result += "\"דברי ירמיה\" ירמיה א";
                break;
            case VAERA:
                result += "\"כה אמר\" יחזקאל כ\"ח";
                break;
            case BO:
                result += "\"הדבר אשר\" ירמיה מ\"ו";
                break;
            case BESHALACH:
                result += "\"ותשר דבורה\" שופטים ד";
                break;
            case YISRO:
                result += "\"בשנת מות\" ישעיה ו";
                break;
            case MISHPATIM:
                result += "\"הדבר אשר\" ירמיה ל\"ד";// Usually replaced with Shekalim
                break;
            case TERUMAH:
                result += "\"ויהוה נתן\" מלכים א ה";
                break;
            case TETZAVEH:
                result += "\"אתה בן אדם\" יחזקאל מ\"ג";// Usually replaced with Zachor
                break;
            case KI_SISA:
                result += "\"וישלח אחאב\" מלכים א י\"ח";
                break;
            case VAYAKHEL:
                result += "\"וישלח המלך\" מלכים א ז";
                break;
            case PEKUDEI:
                result += "\"ויעש חירום\" מלכים א ז";
                break;
            case VAYAKHEL_PEKUDEI:
                result += "\"ויעש חירום\" מלכים א ז";
                break;
            case VAYIKRA:
                result += "\"עם זו\" ישעיה מ\"ג";
                break;
            case TZAV:
                result += "\"כה אמרס\" ירמיה ז";
                break;
            case SHMINI:
                result += "\"ויסף עוד\" שמואל ב ו";
                break;
            case TAZRIA:
                result += "\"ואיש בא\" מלכים ב ד";
                break;
            case METZORA:
                result += "\"וארבעה אנשים\" מלכים ב ז";
                break;
            case TAZRIA_METZORA:
                result += "\"וארבעה אנשים\" מלכים ב ז";
                break;
            case ACHREI_MOS:
                result += "\"ויהי דבר\" יחזקאל כ\"ב";
                break;
            case KEDOSHIM:
                result += "\"ויהי דבר\" יחזקאל כ";
                break;
            case ACHREI_MOS_KEDOSHIM:
                result += "\"ויהי דבר\" יחזקאל כ";
                break;
            case EMOR:
                result += "\"והכהנים\" יחזקאל מ\"ד";
                break;
            case BEHAR:
                result += "\"ויאמר ירמיהו\" ירמיה ל\"ב";
                break;
            case BECHUKOSAI:
                result += "\"ה' עזי\" ירמיה ט\"ז";
                break;
            case BEHAR_BECHUKOSAI:
                result += "\"ה' עזי\" ירמיה ט\"ז";
                break;
            case BAMIDBAR:
                result += "\"והיה מספר\" הושע ב";
                break;
            case NASSO:
                result += "\"ויהי איש\" שופטים י\"ג";
                break;
            case BEHAALOSCHA:
                result += "\"רני ושמחי\" זכריה ב";
                break;
            case SHLACH:
                result += "\"וישלח\" יהושע ב";
                break;
            case KORACH:
                result += "\"ויאמר\" שמואל א י\"א";
                break;
            case CHUKAS:
                result += "\"ויפתח\" שופטים י\"א";
                break;
            case BALAK:
                result += "\"והיה\" מיכה ה";
                break;
            case CHUKAS_BALAK:
                result += "\"והיה\" מיכה ה";
                break;
            case PINCHAS:
                if (jewishCalendar.getJewishMonth() == JewishDate.TAMMUZ) {
                    if (jewishCalendar.getJewishDayOfMonth() >= 17) {// If after the seventeenth of Tammuz
                        result += "\"דברי ירמיהו\" ירמיהו א";
                    } else {
                        result += "\"ויד ה'\" מלכים י\"ח";
                    }
                } else {
                    if (jewishCalendar.getJewishMonth() == JewishDate.AV) {// I don't think this case is possible, where Parshat Pinchas falls out in the month of Av. However, I included it to be careful just in case.
                        result += "\"דברי ירמיהו\" ירמיהו א";
                    } else {
                        result += "\"ויד ה'\" מלכים י\"ח";
                    }
                }
                break;
            case MATOS:
                result += "\"דברי ירמיהו\" ירמיהו א";
                break;
            case MASEI:
                result += "\"שמעו דבר\" ירמיהו ב";
                break;
            case MATOS_MASEI:
                result += "\"שמעו דבר\" ירמיהו ב";
                return result;// Early return, because we say this even if it's Rosh Chodesh
            case DEVARIM:
                result += "\"חזון\" ישעיה א";
                break;
            case VAESCHANAN:
                result += "\"נחמו\" ישעיה מ";
                break;
            case EIKEV:
                result += "\"ותאמר ציון\" ישעיה מ\"ט";
                break;
            case REEH:
                result += "\"עניה סערה\" ישעיה נ\"ד";
                return result;// We always say this haftorah on Parshat Re'eh, however, the minhag is to add pesukim at the end if rosh chodesh falls on shabbat or after shabbat
            case SHOFTIM:
                result += "\"אנכי אנכי\" ישעיה נ\"א";
                break;
                //KI_SEITZEI was above with NOACH
            case KI_SAVO:
                result += "\"קומי אורי\" ישעיה ס";
                break;
            case NITZAVIM:
                result += "\"שוש אשיש\" ישעיה ס\"א";
                break;
            case VAYEILECH:
                result += "\"שובה\" הושע י\"ד";
                break;
            case NITZAVIM_VAYEILECH:
                result += "\"שוש אשיש\" ישעיה ס\"א";
                break;
            case HAAZINU:
                if (jewishCalendar.getJewishMonth() == JewishDate.TISHREI) {
                    if (jewishCalendar.getJewishDayOfMonth() == 10) {// On Yom Kippur itself we say: Yeshayahu Chapter 57 ... I think
                        result = "\"סלו סלו\" ישעיה נ\"ז";
                    }
                    if (jewishCalendar.getJewishDayOfMonth() > 10) {// After Yom Kippur
                        result += "\"וידבר דוד\" שמואל ב כ\"ב";
                    } else {// Before Yom Kippur
                        result += "\"שובה\" הושע י\"ד";
                    }
                } else if (jewishCalendar.getJewishMonth() == JewishDate.ELUL) {// Definitely before Yom Kippur
                    result += "\"שובה\" הושע י\"ד";
                }
                break;
            case VZOS_HABERACHA:
                result += "\"ויהי אחרי\" יהושע א";
                break;
            case NONE:
                result = "";
                break;
        }

        // The weekly Haftorah is replaced if it's Rosh Chodesh or Erev Rosh Chodesh
        if (jewishCalendar.isErevRoshChodesh()) {
            result = "מפטירין \"מחר חודש\" שמואל א כ";// Overwrite the string
            return result;
        } else if (jewishCalendar.isRoshChodesh()) {
            result = "מפטירין \"כה אמר\" ישעיה ס\"ו (הפטרת ר\"ח)";// Overwrite the string
            return result;
        }

        return result;
    }
}
