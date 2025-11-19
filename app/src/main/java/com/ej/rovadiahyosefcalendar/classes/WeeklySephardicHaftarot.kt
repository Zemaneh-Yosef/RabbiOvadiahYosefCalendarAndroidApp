package com.ej.rovadiahyosefcalendar.classes

import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import androidx.core.text.inSpans
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar.Parsha
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar

/**
 * This class's main goal is to return the Weekly Haftorah reading said after the Weekly Parasha
 * reading. Which readings to say were taken from the Chumash "L'maan Shemo B'Ahavah" according to
 * the Sepharadic Minhag.
 * @see WeeklyParashaReadings
 */
class WeeklySephardicHaftarot {

    companion object {
        private val specialShabbatot: Map<Parsha, HaftarahReading> = mapOf(
            Parsha.SHKALIM to HaftarahReading("ויכרת יהוידע", "מלכים ב י\"א"),
            Parsha.ZACHOR to HaftarahReading("ויאמר שמואל", "שמואל א ט\"ו"),
            Parsha.PARA to HaftarahReading("ויהי דבר", "יחזקאל ל\"ו"),
            Parsha.HACHODESH to HaftarahReading("כה אמר", "יחזקאל מ\"ה"),
            Parsha.HAGADOL to HaftarahReading("וערבה", "מלאכי ג")
        )

        /**
         * This method returns a Haftorah reading for the given week.
         * The [JewishCalendar] object passed into this method should be set to Saturday because
         * [JewishCalendar.getParshah] returns [Parsha.NONE] during the week.
         *
         * @param jCal the JewishCalendar object set to Saturday
         * @return The haftorah for this week as a [HaftarahReading]
         */
        fun getThisWeeksHaftarah(jCal: JewishCalendar): HaftarahReading? {
            jCal.specialShabbos?.let { special ->
                if (specialShabbatot.containsKey(special)) {
                    return specialShabbatot[special]
                }
            }

            // Yom Tov cases that should be specifically on those days
            if (jCal.isRoshHashana
                || jCal.isYomKippur
                || (jCal.isSuccos && (!jCal.isHoshanaRabba)) // Chol HaMoed is included, but exclude Hoshana Rabba
                || jCal.isShminiAtzeres
                || jCal.isPesach // Chol HaMoed is included
                || jCal.isShavuos) {
                return when (jCal.yomTovIndex) {
                    JewishCalendar.ROSH_HASHANA -> HaftarahReading("ויהי איש", "שמואל א א")
                    JewishCalendar.YOM_KIPPUR -> HaftarahReading("סלו סלו", "ישעיה נ\"ז")
                    JewishCalendar.SUCCOS -> HaftarahReading("הנה יום", "זכריה י\"ד")
                    JewishCalendar.CHOL_HAMOED_SUCCOS -> HaftarahReading("ויהי ביום", "יחזקאל לח")
                    JewishCalendar.SHEMINI_ATZERES -> {
                        if (jCal.inIsrael) {
                            HaftarahReading("ויהי אחרי", "יהושע א")
                        } else {
                            HaftarahReading("ויהי ככלות", "מלכים א ח")
                        }
                    }
                    JewishCalendar.PESACH -> when (jCal.jewishDayOfMonth) {
                        15 -> HaftarahReading("בעת ההיא", "יהושע ה")
                        16 -> HaftarahReading("וישלח המלך", "מלכים ב כ\"ג")
                        21 -> HaftarahReading("וידבר דוד", "שמואל ב כ\"ב")
                        22 -> HaftarahReading("עוד היום", "ישעיהו י")
                        else -> null
                    }
                    JewishCalendar.CHOL_HAMOED_PESACH -> HaftarahReading("היתה עלי", "יחזקאל ל\"ז")
                    JewishCalendar.SHAVUOS -> {
                        if (jCal.jewishDayOfMonth == 7) {
                            HaftarahReading("וה' בהיכל", "חבקוק ב")
                        } else {
                            HaftarahReading("ויהי בשלושים", "יחזקאל א")
                        }
                    }
                    else -> null
                }
            }

            // Chanukah
            if (jCal.isChanukah) {
                return if (jCal.dayOfChanukah in listOf(7, 8)) {
                    HaftarahReading("ויעש חירום", "מלכים א ז")
                } else {
                    HaftarahReading("רני ושמחי", "זכריה ב")
                }
            }

            // Rosh Chodesh / Erev Rosh Chodesh
            if (jCal.parshah !in listOf(Parsha.MATOS_MASEI, Parsha.REEH)) {
                if (jCal.isErevRoshChodesh) {
                    return HaftarahReading("מחר חודש", "שמואל א כ")
                } else if (jCal.isRoshChodesh) {
                    return HaftarahReading("כה אמר", "ישעיה ס\"ו" + " [הפטרת ר\"ח]")
                }
            }

            // Prepare 17 Tammuz date (for later conditional logic)
            val tammuz17 = jCal.clone() as JewishCalendarWithExtraMethods
            tammuz17.setJewishDate(tammuz17.jewishYear, JewishCalendar.TAMMUZ, 17)

            // Default weekly parasha mapping
            return when (jCal.parshah) {
                Parsha.BERESHIS -> HaftarahReading("כה אמר", "ישעיה מ\"ב")
                Parsha.NOACH -> HaftarahReading("רני עקרה", "ישעיה נ\"ד")
                Parsha.LECH_LECHA -> HaftarahReading("למה תאמר", "ישעיה מ")
                Parsha.VAYERA -> HaftarahReading("ואשה אחת", "מלכים ב ד")
                Parsha.CHAYEI_SARA -> HaftarahReading("והמלך דוד", "מלכים א א")
                Parsha.TOLDOS -> HaftarahReading("משא דבר", "מלאכי א")
                Parsha.VAYETZEI -> HaftarahReading("ועמי תלואים", "הושע י\"א")
                Parsha.VAYISHLACH -> HaftarahReading("חזון עובדיה", "עובדיה")
                Parsha.VAYESHEV -> HaftarahReading("כה אמר", "עמוס ב")
                Parsha.MIKETZ -> HaftarahReading("ויקץ שלמה", "מלכים א ג")
                Parsha.VAYIGASH -> HaftarahReading("ויהי דבר", "יחזקאל ל\"ז")
                Parsha.VAYECHI -> HaftarahReading("ויקרבו", "מלכים א ב")
                Parsha.SHEMOS -> HaftarahReading("דברי ירמיה", "ירמיה א")
                Parsha.VAERA -> HaftarahReading("כה אמר", "יחזקאל כ\"ח")
                Parsha.BO -> HaftarahReading("הדבר אשר", "ירמיה מ\"ו")
                Parsha.BESHALACH -> HaftarahReading("ותשר דבורה", "שופטים ד")
                Parsha.YISRO -> HaftarahReading("בשנת מות", "ישעיה ו")
                Parsha.MISHPATIM -> HaftarahReading("הדבר אשר", "ירמיה ל\"ד")
                Parsha.TERUMAH -> HaftarahReading("ויהוה נתן", "מלכים א ה")
                Parsha.TETZAVEH -> HaftarahReading("אתה בן אדם", "יחזקאל מ\"ג")
                Parsha.KI_SISA -> HaftarahReading("וישלח אחאב", "מלכים א י\"ח")
                Parsha.VAYAKHEL -> HaftarahReading("וישלח המלך", "מלכים א ז")
                Parsha.PEKUDEI, Parsha.VAYAKHEL_PEKUDEI -> HaftarahReading("ויעש חירום", "מלכים א ז")
                Parsha.VAYIKRA -> HaftarahReading("עם זו", "ישעיה מ\"ג")
                Parsha.TZAV -> HaftarahReading("כה אמרס", "ירמיה ז")
                Parsha.SHMINI -> HaftarahReading("ויסף עוד", "שמואל ב ו")
                Parsha.TAZRIA -> HaftarahReading("ואיש בא", "מלכים ב ד")
                Parsha.METZORA, Parsha.TAZRIA_METZORA -> HaftarahReading("וארבעה אנשים", "מלכים ב ז")
                Parsha.ACHREI_MOS -> HaftarahReading("ויהי דבר", "יחזקאל כ\"ב")
                Parsha.KEDOSHIM, Parsha.ACHREI_MOS_KEDOSHIM -> HaftarahReading("ויהי דבר", "יחזקאל כ")
                Parsha.EMOR -> HaftarahReading("והכהנים", "יחזקאל מ\"ד")
                Parsha.BEHAR -> HaftarahReading("ויאמר ירמיהו", "ירמיה ל\"ב")
                Parsha.BECHUKOSAI, Parsha.BEHAR_BECHUKOSAI -> HaftarahReading("יהיה עזי", "ירמיה ט\"ז")
                Parsha.BAMIDBAR -> HaftarahReading("והיה מספר", "הושע ב")
                Parsha.NASSO -> HaftarahReading("ויהי איש", "שופטים י\"ג")
                Parsha.BEHAALOSCHA -> HaftarahReading("רני ושמחי", "זכריה ב")
                Parsha.SHLACH -> HaftarahReading("וישלח", "יהושע ב")
                Parsha.KORACH -> HaftarahReading("ויאמר", "שמואל א י\"א")
                Parsha.CHUKAS -> HaftarahReading("ויפתח", "שופטים י\"א")
                Parsha.BALAK, Parsha.CHUKAS_BALAK -> HaftarahReading("והיה", "מיכה ה")
                Parsha.PINCHAS -> {
                    if (jCal < tammuz17) {
                        HaftarahReading("ויד יהוה", "מלכים י\"ח")
                    } else {
                        HaftarahReading("דברי ירמיהו", "ירמיהו א")
                    }
                }
                Parsha.MATOS -> HaftarahReading("דברי ירמיהו", "ירמיהו א")
                Parsha.MASEI, Parsha.MATOS_MASEI -> HaftarahReading("שמעו דבר", "ירמיהו ב")
                Parsha.DEVARIM -> HaftarahReading("חזון", "ישעיה א")
                Parsha.VAESCHANAN -> HaftarahReading("נחמו", "ישעיה מ")
                Parsha.EIKEV -> HaftarahReading("ותאמר ציון", "ישעיה מ\"ט")
                Parsha.REEH -> HaftarahReading("עניה סערה", "ישעיה נ\"ד")
                Parsha.SHOFTIM -> HaftarahReading("אנכי אנכי", "ישעיה נ\"א")
                Parsha.KI_SEITZEI -> HaftarahReading("רני עקרה", "ישעיה נ\"ד")
                Parsha.KI_SAVO -> HaftarahReading("קומי אורי", "ישעיה ס")
                Parsha.NITZAVIM, Parsha.NITZAVIM_VAYEILECH -> HaftarahReading("שוש אשיש", "ישעיה ס\"א")
                Parsha.VAYEILECH -> HaftarahReading("שובה", "הושע י\"ד")
                Parsha.HAAZINU -> {
                    if (jCal.jewishMonth == JewishCalendar.TISHREI && jCal.jewishDayOfMonth > 10) {
                        HaftarahReading("וידבר דוד", "שמואל ב כ\"ב")
                    } else {
                        HaftarahReading("שובה", "הושע י\"ד")
                    }
                }
                else -> null // V'zos Haberachah unused
            }
        }

        fun formatWeeklyHaftara(jCal: JewishCalendar): CharSequence {
            val haftara = getThisWeeksHaftarah(jCal)
            val builder = SpannableStringBuilder()
            if (haftara != null) {
                builder.append(haftara.text)
                builder.inSpans(RelativeSizeSpan(0.7f)) {
                    append(" (")
                    append(haftara.source)
                    append(")")
                }
            }

            return builder
        }
    }
}

/**
 * Represents a Haftarah reading with its text and source.
 */
data class HaftarahReading(
    val text: String,
    val source: String
)