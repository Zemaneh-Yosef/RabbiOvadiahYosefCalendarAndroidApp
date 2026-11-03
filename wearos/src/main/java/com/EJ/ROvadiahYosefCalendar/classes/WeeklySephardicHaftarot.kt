package com.EJ.ROvadiahYosefCalendar.classes

import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import androidx.core.text.inSpans
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar.Parshah
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar

/**
 * This class's main goal is to return the Weekly Haftorah reading said after the Weekly Parasha
 * reading. Which readings to say were taken from the Chumash "L'maan Shemo B'Ahavah" according to
 * the Sepharadic Minhag.
 */
class WeeklySephardicHaftarot {

    companion object {
        private val specialShabbatot: Map<Parshah, HaftarahReading> = mapOf(
            Parshah.SHKALIM to HaftarahReading("ויכרות יהוידע", "מלכים ב י\"א"),
            Parshah.ZACHOR to HaftarahReading("ויאמר שמואל", "שמואל א ט\"ו"),
            Parshah.PARA to HaftarahReading("ויהי דבר", "יחזקאל ל\"ו"),
            Parshah.HACHODESH to HaftarahReading("כה אמר", "יחזקאל מ\"ה"),
            Parshah.HAGADOL to HaftarahReading("וערבה", "מלאכי ג")
        )

        /**
         * This method returns a Haftorah reading for the given week.
         * The [JewishCalendar] object passed into this method should be set to Saturday because
         * [JewishCalendar.getParshah] returns [Parshah.NONE] during the week.
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
            if (jCal.parshah !in listOf(Parshah.MATOS_MASEI, Parshah.REEH)) {
                if (jCal.isErevRoshChodesh) {
                    return HaftarahReading("מחר חודש", "שמואל א כ")
                } else if (jCal.isRoshChodesh) {
                    return HaftarahReading("השמים כסאי", "ישעיה ס\"ו" + " [הפטרת ר\"ח]")
                }
            }

            // Prepare 17 Tammuz date (for later conditional logic)
            val tammuz17 = jCal.clone() as JewishCalendarWithExtraMethods
            tammuz17.setJewishDate(tammuz17.jewishYear, JewishCalendar.TAMMUZ, 17)

            // Default weekly parasha mapping
            return when (jCal.parshah) {
                Parshah.BERESHIS -> HaftarahReading("כה אמר", "ישעיה מ\"ב")
                Parshah.NOACH -> HaftarahReading("רני עקרה", "ישעיה נ\"ד")
                Parshah.LECH_LECHA -> HaftarahReading("למה תאמר", "ישעיה מ")
                Parshah.VAYERA -> HaftarahReading("ואשה אחת", "מלכים ב ד")
                Parshah.CHAYEI_SARA -> HaftarahReading("והמלך דוד", "מלכים א א")
                Parshah.TOLDOS -> HaftarahReading("משא דבר", "מלאכי א")
                Parshah.VAYETZEI -> HaftarahReading("ועמי תלואים", "הושע י\"א")
                Parshah.VAYISHLACH -> HaftarahReading("חזון עובדיה", "עובדיה")
                Parshah.VAYESHEV -> HaftarahReading("כה אמר", "עמוס ב")
                Parshah.MIKETZ -> HaftarahReading("ויקץ שלמה", "מלכים א ג")
                Parshah.VAYIGASH -> HaftarahReading("ויהי דבר", "יחזקאל ל\"ז")
                Parshah.VAYECHI -> HaftarahReading("ויקרבו ימי", "מלכים א ב")
                Parshah.SHEMOS -> HaftarahReading("דברי ירמיהו", "ירמיה א")
                Parshah.VAERA -> HaftarahReading("כה אמר", "יחזקאל כ\"ח")
                Parshah.BO -> HaftarahReading("הדבר אשר", "ירמיה מ\"ו")
                Parshah.BESHALACH -> HaftarahReading("ותשר דבורה", "שופטים ד")
                Parshah.YISRO -> HaftarahReading("בשנת מות", "ישעיה ו")
                Parshah.MISHPATIM -> HaftarahReading("הדבר אשר", "ירמיה ל\"ד")
                Parshah.TERUMAH -> HaftarahReading("וה' נתן", "מלכים א ה")
                Parshah.TETZAVEH -> HaftarahReading("אתה בן אדם", "יחזקאל מ\"ג")// TODO double check this title
                Parshah.KI_SISA -> HaftarahReading("וישלח אחאב", "מלכים א י\"ח")
                Parshah.VAYAKHEL -> HaftarahReading("וישלח המלך", "מלכים א ז")
                Parshah.PEKUDEI, Parshah.VAYAKHEL_PEKUDEI -> HaftarahReading("ויעש חירום", "מלכים א ז")
                Parshah.VAYIKRA -> HaftarahReading("עם זו", "ישעיה מ\"ג")
                Parshah.TZAV -> HaftarahReading("כה אמרס", "ירמיה ז")
                Parshah.SHMINI -> HaftarahReading("ויסף עוד", "שמואל ב ו")
                Parshah.TAZRIA -> HaftarahReading("ואיש בא", "מלכים ב ד")
                Parshah.METZORA, Parshah.TAZRIA_METZORA -> HaftarahReading("וארבעה אנשים", "מלכים ב ז")
                Parshah.ACHREI_MOS -> HaftarahReading("ויהי דבר", "יחזקאל כ\"ב")
                Parshah.KEDOSHIM, Parshah.ACHREI_MOS_KEDOSHIM -> HaftarahReading("ויהי דבר", "יחזקאל כ")
                Parshah.EMOR -> HaftarahReading("והכהנים הלוים", "יחזקאל מ\"ד")
                Parshah.BEHAR -> HaftarahReading("ויאמר ירמיהו", "ירמיה ל\"ב")
                Parshah.BECHUKOSAI, Parshah.BEHAR_BECHUKOSAI -> HaftarahReading("ה' עזי", "ירמיה ט\"ז")
                Parshah.BAMIDBAR -> HaftarahReading("והיה מספר", "הושע ב")
                Parshah.NASSO -> HaftarahReading("ויהי איש", "שופטים י\"ג")
                Parshah.BEHAALOSCHA -> HaftarahReading("רני ושמחי", "זכריה ב")
                Parshah.SHLACH -> HaftarahReading("וישלח יהושע", "יהושע ב")
                Parshah.KORACH -> HaftarahReading("ויאמר שמואל", "שמואל א י\"א")
                Parshah.CHUKAS -> HaftarahReading("ויפתח הגלעדי", "שופטים י\"א")
                Parshah.BALAK, Parshah.CHUKAS_BALAK -> HaftarahReading("והיה שארית", "מיכה ה")
                Parshah.PINCHAS -> {
                    if (jCal < tammuz17) {
                        HaftarahReading("ויד יהוה", "מלכים י\"ח")
                    } else {
                        HaftarahReading("דברי ירמיהו", "ירמיה א")
                    }
                }
                Parshah.MATOS -> HaftarahReading("דברי ירמיהו", "ירמיה א")
                Parshah.MASEI, Parshah.MATOS_MASEI -> HaftarahReading("שמעו דבר", "ירמיה ב")
                Parshah.DEVARIM -> HaftarahReading("חזון ישעיהו", "ישעיה א")
                Parshah.VAESCHANAN -> HaftarahReading("נחמו נחמו", "ישעיה מ")
                Parshah.EIKEV -> HaftarahReading("ותאמר ציון", "ישעיה מ\"ט")
                Parshah.REEH -> HaftarahReading("עניה סערה", "ישעיה נ\"ד")
                Parshah.SHOFTIM -> HaftarahReading("אנכי אנכי", "ישעיה נ\"א")
                Parshah.KI_SEITZEI -> HaftarahReading("רוני עקרה", "ישעיה נ\"ד")
                Parshah.KI_SAVO -> HaftarahReading("קומי אורי", "ישעיה ס")
                Parshah.NITZAVIM, Parshah.NITZAVIM_VAYEILECH -> HaftarahReading("שוש אשיש", "ישעיה ס\"א")
                Parshah.VAYEILECH -> HaftarahReading("שובה ישראל", "הושע י\"ד")
                Parshah.HAAZINU -> {
                    if (jCal.jewishMonth == JewishCalendar.TISHREI && jCal.jewishDayOfMonth > 10) {
                        HaftarahReading("וידבר דוד", "שמואל ב כ\"ב")
                    } else {
                        HaftarahReading("שובה ישראל", "הושע י\"ד")
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