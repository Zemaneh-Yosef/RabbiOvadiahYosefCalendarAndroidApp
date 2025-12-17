package com.ej.rovadiahyosefcalendar.classes

import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

data class RambamReading(
    val bookName: String,
    val chapter: String // String because it can be a number ("1") or a range ("1-21")
)

object DailyMishnehTorah {
    private const val CYCLE_LENGTH = 1017
    // April 29, 1984
    private val START_DATE = LocalDate.of(1984, Month.APRIL, 29)

    // Special formatting for the first 4 books
    private val specialVerseRanges = listOf(
        listOf("1-21", "22-33", "34-45"), // Transmission of the Oral Law
        listOf("1-83", "84-166", "167-248"), // Positive Mitzvot
        listOf("1-122", "123-245", "246-365"), // Negative Mitzvot
        listOf("1:1-4:8", "5:1-9:9", "10:1-14:10") // Overview of Mishneh Torah Contents
    )

    private data class Book(val name: String, val chapterCount: Int)

    private val books = listOf(
        Book("הקדמת הרמב\"ם", 3),
        Book("מצוות עשה", 3),
        Book("מצוות לא תעשה", 3),
        Book("תוכן ההלכות", 3),
        Book("הלכות יסודי התורה", 10),
        Book("הלכות דעות", 7),
        Book("הלכות תלמוד תורה", 7),
        Book("הלכות עבודה זרה וחוקות הגויים", 12),
        Book("הלכות תשובה", 10),
        Book("הלכות קריאת שמע", 4),
        Book("הלכות תפילה וברכת כהנים", 15),
        Book("הלכות תפילין ומזוזה וספר תורה", 10),
        Book("הלכות ציצית", 3),
        Book("הלכות ברכות", 11),
        Book("הלכות מילה", 3),
        Book("סדר התפילה", 4),
        Book("הלכות שבת", 30),
        Book("הלכות ערובין", 8),
        Book("הלכות שביתת עשור", 3),
        Book("הלכות שביתת יום טוב", 8),
        Book("הלכות חמץ ומצה", 9),
        Book("הלכות שופר וסוכה ולולב", 8),
        Book("הלכות שקלים", 4),
        Book("הלכות קידוש החודש", 19),
        Book("הלכות תעניות", 5),
        Book("הלכות מגילה וחנוכה", 4),
        Book("הלכות אישות", 25),
        Book("הלכות גירושין", 13),
        Book("הלכות ייבום וחליצה", 8),
        Book("הלכות נערה בתולה", 3),
        Book("הלכות סוטה", 4),
        Book("הלכות איסורי ביאה", 22),
        Book("הלכות מאכלות אסורות", 17),
        Book("הלכות שחיטה", 14),
        Book("הלכות שבועות", 12),
        Book("הלכות נדרים", 13),
        Book("הלכות נזירות", 10),
        Book("הלכות ערכים וחרמים", 8),
        Book("הלכות כלאיים", 10),
        Book("הלכות מתנות עניים", 10),
        Book("הלכות תרומות", 15),
        Book("הלכות מעשרות", 14),
        Book("הלכות מעשר שני ונטע רבעי", 11),
        Book("הלכות ביכורים ושאר מתנות כהונה שבגבולין", 12),
        Book("הלכות שמיטה ויובל", 13),
        Book("הלכות בית הבחירה", 8),
        Book("הלכות כלי המקדש והעובדים בו", 10),
        Book("הלכות ביאת המקדש", 9),
        Book("הלכות איסורי מזבח", 7),
        Book("הלכות מעשה הקרבנות", 19),
        Book("הלכות תמידין ומוספין", 10),
        Book("הלכות פסולי המוקדשין", 19),
        Book("הלכות עבודת יום הכיפורים", 5),
        Book("הלכות מעילה", 8),
        Book("הלכות קרבן פסח", 10),
        Book("הלכות חגיגה", 3),
        Book("הלכות בכורות", 8),
        Book("הלכות שגגות", 15),
        Book("הלכות מחוסרי כפרה", 5),
        Book("הלכות תמורה", 4),
        Book("הלכות טומאת מת", 25),
        Book("הלכות פרה אדומה", 15),
        Book("הלכות טומאת צרעת", 16),
        Book("הלכות מטמאי משכב ומושב", 13),
        Book("הלכות שאר אבות הטומאות", 20),
        Book("הלכות טומאת אוכלין", 16),
        Book("הלכות כלים", 28),
        Book("הלכות מקוואות", 11),
        Book("הלכות נזקי ממון", 14),
        Book("הלכות גנבה", 9),
        Book("הלכות גזילה ואבידה", 18),
        Book("הלכות חובל ומזיק", 8),
        Book("הלכות רוצח ושמירת נפש", 13),
        Book("הלכות מכירה", 30),
        Book("הלכות זכייה ומתנה", 12),
        Book("הלכות שכנים", 14),
        Book("הלכות שלוחין ושותפין", 10),
        Book("הלכות עבדים", 9),
        Book("הלכות שכירות", 13),
        Book("הלכות שאלה ופיקדון", 8),
        Book("הלכות מלווה ולווה", 27),
        Book("הלכות טוען ונטען", 16),
        Book("הלכות נחלות", 11),
        Book("הלכות סנהדרין והעונשין המסורין להם", 26),
        Book("הלכות עדות", 22),
        Book("הלכות ממרים", 7),
        Book("הלכות אבל", 14),
        Book("הלכות מלכים ומלחמות", 12)
    )

    // --- 1 Chapter Per Day Logic ---

    /**
     * Calculates Daily Rambam (Mishneh Torah) for 1 chapter a day cycle.
     */
    fun getDailyLearning(date: LocalDate): RambamReading? {
        if (date.isBefore(START_DATE)) return null

        val daysDifference = ChronoUnit.DAYS.between(START_DATE, date)
        val cycleIndex = (daysDifference % CYCLE_LENGTH).toInt()

        return getChapterByIndex(cycleIndex)
    }

    // --- 3 Chapters Per Day Logic ---

    /**
     * Calculates Daily Rambam (Mishneh Torah) for 3 chapters a day cycle.
     * Returns a list. If the 3 chapters are in the same book, returns a list of size 1
     * with the combined range. Otherwise, returns a list of size 3.
     */
    fun getDailyLearning3(date: LocalDate): List<RambamReading>? {
        if (date.isBefore(START_DATE)) return null

        val daysDifference = ChronoUnit.DAYS.between(START_DATE, date)

        // Cycle length for 3 chapters is 1/3 of the main cycle (339 days)
        val cycleLength3 = CYCLE_LENGTH / 3
        val dayInCycle = (daysDifference % cycleLength3).toInt()

        // Get the base index for the 1-chapter cycle
        val baseIndex = dayInCycle * 3

        val r1 = getChapterByIndex(baseIndex) ?: return null
        val r2 = getChapterByIndex(baseIndex + 1) ?: return null
        val r3 = getChapterByIndex(baseIndex + 2) ?: return null

        // Check if all 3 chapters are from the same section/book
        if (r1.bookName == r2.bookName && r2.bookName == r3.bookName) {
            return listOf(combineReadings(r1, r3))
        } else if (r1.bookName == r2.bookName) {
            return listOf(combineReadings(r1, r2), r3)
        } else if (r2.bookName == r3.bookName) {
            return listOf(r1, combineReadings(r2, r3))
        }

        return listOf(r1, r2, r3)
    }

    private fun combineReadings(first: RambamReading, last: RambamReading): RambamReading {
        val startChapter = first.chapter
        val endChapter = last.chapter

        val rangeString = if (startChapter.contains("-") || endChapter.contains("-")) {
            // Logic for the first 4 books where chapters are actually ranges (e.g., "1-21")
            val firstPart = startChapter.split("-").first()
            val lastPart = endChapter.split("-").last()
            "$firstPart-$lastPart"
        } else {
            // Standard logic (e.g., "1" and "3" becomes "1-3")
            "$startChapter-$endChapter"
        }

        return RambamReading(first.bookName, rangeString)
    }

    // --- Core Calculation Logic ---

    private fun getChapterByIndex(index: Int): RambamReading? {
        var remainingChapters = index

        for ((bookIndex, book) in books.withIndex()) {
            if (remainingChapters < book.chapterCount) {
                val chapterNum = remainingChapters + 1

                val chapterDisplay = if (bookIndex < 4) {
                    specialVerseRanges[bookIndex][chapterNum - 1]
                } else {
                    chapterNum.toString()
                }

                return RambamReading(book.name, chapterDisplay)
            }
            remainingChapters -= book.chapterCount
        }
        return null
    }
}