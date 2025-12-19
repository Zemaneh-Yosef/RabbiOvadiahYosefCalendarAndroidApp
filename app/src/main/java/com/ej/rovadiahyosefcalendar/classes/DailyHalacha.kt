package com.ej.rovadiahyosefcalendar.classes

import com.ej.rovadiahyosefcalendar.classes.SimanRepository.shulchanAruchStructure
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class HalachaSegment(
    val bookName: String,
    val siman: Int,
    val seifim: String
)

object HalachaYomi {

    private val START_DATE = LocalDate.of(2020, Month.NOVEMBER, 12) // 5781 start date
    private const val RATE_SA = 3
    private const val RATE_KITZUR = 5

    // Constant names for cleaner usage
    private const val NAME_SA = "שו\"ע - או\"ח"
    private const val NAME_KITZUR = "קיצשו\"ע"

    fun getDailyLearning(date: LocalDate): List<HalachaSegment>? {
        if (date.isBefore(START_DATE)) return null

        // 1. Load the new structured lists (List<SimanInfo>)
        // Ensure your SimanRepository returns the List<SimanInfo> created in the previous step
        val saStructure = SimanRepository.shulchanAruchCounts
        val kitzurStructure = SimanRepository.kitzurStructure

        // 2. Sum using 'seifCount' from the objects
        val totalSaSeifim = saStructure.sum()
        val totalKitzurSeifim = kitzurStructure.sumOf { it.seifCount }

        // 3. Calculate length of one full cycle in days
        val daysInSaPhase = ceil(totalSaSeifim.toDouble() / RATE_SA).toLong()
        val daysInKitzurPhase = ceil(totalKitzurSeifim.toDouble() / RATE_KITZUR).toLong()
        val totalDaysInCycle = daysInSaPhase + daysInKitzurPhase

        // 4. Find the current day within the repeating cycle
        val totalDaysPassed = ChronoUnit.DAYS.between(START_DATE, date)
        val dayInCurrentCycle = totalDaysPassed % totalDaysInCycle

        // 5. Determine which book we are in for this cycle
        return if (dayInCurrentCycle < daysInSaPhase) {
            val startSeifIndex = (dayInCurrentCycle * RATE_SA).toInt()
            calculateSegments(
                globalStartIndex = startSeifIndex,
                amountToRead = RATE_SA,
                structure = shulchanAruchStructure,
                bookName = NAME_SA,
                totalSeifimInBook = totalSaSeifim
            )
        } else {
            val daysIntoKitzur = dayInCurrentCycle - daysInSaPhase
            val startSeifIndex = (daysIntoKitzur * RATE_KITZUR).toInt()
            calculateSegments(
                globalStartIndex = startSeifIndex,
                amountToRead = RATE_KITZUR,
                structure = kitzurStructure,
                bookName = NAME_KITZUR,
                totalSeifimInBook = totalKitzurSeifim
            )
        }
    }

    private fun calculateSegments(
        globalStartIndex: Int,
        amountToRead: Int,
        structure: List<SimanInfo>, // UPDATED: Now takes List<SimanInfo>
        bookName: String,
        totalSeifimInBook: Int
    ): List<HalachaSegment> {
        val results = mutableListOf<HalachaSegment>()

        // Ensure we don't read past the end of the book in the final day of a phase
        val actualAmountToRead = minOf(amountToRead, totalSeifimInBook - globalStartIndex)
        val globalEndIndex = globalStartIndex + actualAmountToRead

        var currentSimanStartGlobalIndex = 0

        // UPDATED LOOP: Iterate over SimanInfo objects directly
        for (simanInfo in structure) {
            // No more "index + 1" or "simanNumber += 10".
            // We get the exact Siman Number from the object.
            val simanNumber = simanInfo.simanNumber
            val seifCountInSiman = simanInfo.seifCount

            val currentSimanEndGlobalIndex = currentSimanStartGlobalIndex + seifCountInSiman

            // Calculate overlap between [globalStartIndex, globalEndIndex] and [simanStart, simanEnd]
            val overlapStart = maxOf(currentSimanStartGlobalIndex, globalStartIndex)
            val overlapEnd = minOf(currentSimanEndGlobalIndex, globalEndIndex)

            if (overlapStart < overlapEnd) {
                val localStart = (overlapStart - currentSimanStartGlobalIndex) + 1
                val localEnd = (overlapEnd - currentSimanStartGlobalIndex)

                val numberFormatter = HebrewDateFormatter()
                numberFormatter.isHebrewFormat = true
                numberFormatter.isUseGershGershayim = false

                val rangeString = if (localStart == localEnd) "${numberFormatter.formatHebrewNumber(localStart)}"
                else "${numberFormatter.formatHebrewNumber(localStart)}-${numberFormatter.formatHebrewNumber(localEnd)}"

                results.add(HalachaSegment(bookName, simanNumber, rangeString))
                if (bookName == NAME_SA && simanNumber == 696 && localEnd == 8) { // add the last Siman because it is merged with the last 3 readings
                    results.add(HalachaSegment(bookName, 697, numberFormatter.formatHebrewNumber(1)))
                }
            }

            // Optimization: if we've passed the end of our reading range, stop looping
            if (currentSimanEndGlobalIndex >= globalEndIndex) break

            currentSimanStartGlobalIndex += seifCountInSiman
        }

        return results
    }
}

/**
 * Data repository holding the structure of the books.
 */
object SimanRepository {

    // ACTUAL array of Seif counts for all 697 Simanim of Orach Chaim taken from here: https://judaism.stackexchange.com/questions/99853/total-number-of-simanim-in-shulchan-aruch and touched up to fit the schedule.
    // Example: Siman 1 has 9 seifim, Siman 2 has 6, etc.
    // While researching this list, it seems like the original learning calendar from 1954 had a few differences.
    // See here: https://www.hebrewbooks.org/pdfpager.aspx?req=40421&st=&pgnum=5&hilite=
    // I will note them here, however, because I am trying to follow the already wide spread schedule, I will change the list to that version and note
    // the differences below. It may also be that the Shulchan Aruch that they used back then might have had a different ordering of the simanim/seifim.
    val shulchanAruchCounts: List<Int> by lazy {
        listOf(9, 6, 17, 23, 1, 4, 4, 17, 6, 12, 15, 3, 3, 5, 6, 1, 3, 3, 2, 2, 4, 1,
            3, // Siman 23 has 4 seifim in our editions
            6, 13, 2, 11, 3, 1, 5, 2, 52, 5, 4, 1, 3, 3, 13, 10, 8, 1, 3,
            9, // Siman 43 has 8 seifim is some editions?
            1, 2, 9, 14, 1, 1, 1, 9, 1, 26, 3, 22, 5, 2, 7, 5, 5, 26, 5, 9, 4, 3, 10, 1, 1, 2, 5, 7, 5, 4, 6, 6, 8, 2, 1, 9, 1, 2, 2, 5, 1, 2, 1, 3, 1, 8, 27, 6, 10, 4, 9, 4, 2, 5, 5, 3, 1, 4, 5, 3, 8, 1,
            2, // Siman 106 has 3 seifim is some editions?
            4, 12, 3, 8, 3, 2, 9, 9, 1, 1, 5, 1, 4, 1, 3, 3, 6, 12, 2, 4, 2, 45, 2, 1, 8, 2, 1, 2, 14, 1, 6, 1, 11, 3, 8, 2, 5, 4, 3, 4, 8, 1, 1, 5, 12, 1, 22, 15, 2, 1, 1, 13, 20, 15, 4, 10, 2, 2, 2, 1, 20, 17, 3, 22, 5, 2, 3, 8, 6, 1, 5, 7, 6, 5, 10, 7, 12, 6, 5, 2, 4, 10, 2, 5, 3, 2, 6, 3, 3, 4, 4, 1, 11, 2, 4, 18, 8, 13, 5, 6, 1, 18, 3, 2, 6, 2, 3, 1, 4, 14, 8, 9, 9, 2, 2, 4, 6, 13, 10, 1, 3, 3, 2, 5, 1, 3, 2, 2, 4, 4, 1, 2, 2, 17, 1, 1, 2, 6, 6, 5, 6, 4, 4, 2, 2, 7, 5, 9, 3, 1, 8, 1, 7, 2, 4, 3, 17, 10, 4, 13, 3, 13, 1, 2, 17, 10, 7, 4, 12, 5, 5, 1, 7, 2, 1, 7, 1, 7, 7, 5, 1, 10, 2, 2, 6, 2, 3, 5, 1, 8, 5, 15, 10, 1, 51, 13, 27, 3, 23, 14, 22, 52, 5, 9, 9, 10, 10, 12, 13, 12, 7, 19, 17, 20, 19, 6, 10, 15, 16, 13, 4, 49, 9, 11, 10, 4, 3, 27, 5, 13, 4, 8, 7, 14, 3, 1, 1, 2, 19, 3, 1, 1, 5, 3, 1, 2, 3, 2, 5, 2, 3, 14, 1, 3, 2, 12, 36, 5, 8, 15, 1, 5, 1, 8, 6, 19, 1, 4, 4, 4, 1, 5, 2, 4, 7, 20, 1, 2, 4, 9, 1, 1, 1, 2, 2, 8, 3, 3, 1, 2, 18, 11, 11, 1, 1, 1, 1, 1, 9, 1, 3, 4, 13, 3, 1, 1, 1, 2, 4, 5, 1, 5, 1, 2, 1, 7, 4, 1, 3, 4, 1, 8, 2, 1, 2, 2, 11, 4, 1, 3, 4, 2, 4, 4, 2, 11, 3, 8, 3, 4, 12, 7, 1, 7, 27, 7, 9, 4, 6, 3, 2, 1, 6, 7, 5, 7, 3, 1, 3, 6, 16, 10, 1, 3, 3, 16, 7, 1, 7, 2, 2, 2, 1, 1, 2, 1, 1, 1, 1, 1, 4, 3, 10, 9, 2, 1, 4, 3, 4, 3, 17, 20, 5, 6, 7, 4, 2, 4, 1, 9, 7, 2, 7, 11, 4, 3, 8, 11, 9, 3, 4, 9, 5, 1, 3, 4, 4, 2, 2, 12, 24, 2, 4, 1, 8, 2, 5, 3, 3, 4, 16, 6, 14, 8, 5, 2, 3, 2, 11, 5, 12, 20, 2, 4, 18, 12, 2, 25, 2, 1, 1, 1, 10, 5, 5, 13, 1, 1, 6, 8, 3, 12, 2, 3, 3, 3, 1, 5, 13, 16, 1, 1, 3, 3, 4, 9, 2, 4, 5, 23, 3, 5, 9, 9, 8, 4, 2, 1, 1, 1, 3, 1, 1, 3, 2, 1, 1, 2, 1, 4, 6, 4, 1, 4, 2, 10, 12, 4, 2, 2, 4, 10, 6, 1, 6, 4, 6, 5, 1, 3, 4, 3, 19, 13, 10, 4, 10, 4, 1, 2, 3, 2, 8, 10, 1, 1, 3, 2, 9, 11, 2, 22, 6, 2, 15, 2, 2, 1, 1, 1, 1, 9, 1, 3, 1, 3, 3, 11, 2, 1, 1, 2, 1, 3, 8, 2, 4, 2, 3, 5, 4, 1, 1, 2, 2, 3, 1, 3, 7, 3, 2, 8, 6, 18, 11, 4, 4, 4, 4,
            8) // deleted last siman 697 with one seif because the schedule merges it with 696
    }

    /**
     * Converts the list of counts into a list of SimanInfo objects.
     * Since there are no skips, simanNumber is simply index + 1.
     */
    val shulchanAruchStructure: List<SimanInfo> by lazy {
        shulchanAruchCounts.mapIndexed { index, count ->
            SimanInfo(simanNumber = index + 1, seifCount = count)
        }
    }

    // ACTUAL Kitzur seifim counts for all 221 Simanim of Orach Chaim based on Wiki Source and some editing.
    // The learning schedule skips many parts of the book in order to only learn the relevant chapters. See kitzurStructure below.
    val actualKitzurSeifimCounts: List<Int> by lazy {
        listOf(
            7,
            9,
            8,
            6,
            17,
            11,
            8,
            6,
            21,
            26,
            25, // Siman 11
            15,
            5,
            8,
            13,
            5,
            10,
            22,
            14,
            12,
            10,
            10,
            30,
            12,
            8,
            22,
            5,
            13,
            21,
            9,
            7,
            27,
            14,
            16,
            9,
            28,
            13,
            15,
            3,
            21,
            10,
            23,
            7,
            18,
            23,
            46,
            22,
            10,
            49,
            16,
            15,
            18,
            6,
            9,
            5,
            7,
            7,
            14,
            21,
            15,
            10,
            18,
            5,
            4,
            30,
            12,
            11,
            12,
            9,
            5,
            5,
            23,
            11,
            4,
            14,
            23,
            24,
            11,
            10,
            94,
            5,
            13,
            6,
            19,
            8,
            7,
            24,
            18,
            6,
            23,
            18,
            10,
            5,
            27,
            18,
            15,
            15,
            37,
            5,
            22,
            6,
            7,
            14,
            21,
            2,
            8,
            3,
            7,
            9,
            15,
            17,
            6,
            9,
            13,
            6,
            18,
            13,
            11,
            12,
            11,
            11,
            17,
            5,
            22,
            8,
            4,
            18,
            16,
            23,
            6,
            17,
            5,
            31,
            15,
            22,
            10,
            13,
            10,
            26,
            3,
            23,
            10,
            22,
            9,
            26,
            4,
            5,
            4,
            13,
            17,
            7,
            17,
            16,
            7,
            12,
            3,
            8,
            4,
            10,
            6,
            20,
            14,
            8,
            10,
            16,
            5,
            15,
            7, // wiki source had 6 for 168, but chabad.org and sefaria had 7, so it's probably a mistake
            3,
            2,
            3,
            3,
            4,
            3,
            6,
            8,
            15,
            5,
            15,
            16,
            22,
            16,
            7,
            11,
            6,
            4,
            5,
            5,
            6,
            3,
            6,
            10,
            14,
            12,
            14,
            22,
            13,
            16,
            17,
            11,
            7,
            16,
            5,
            11,
            9,
            11,
            7,
            15,
            8,
            9,
            15,
            5,
            5,
            3,
            3,
            2,
            4,
            2,
            9,
            10,
            8
        )
    }

    /**
     * The structured list of Kitzur Simanim to learn.
     * Maps the specific ranges defined in the schedule to the actual Seif counts.
     */
    val kitzurStructure: List<SimanInfo> by lazy {
        val rangesToLearn = listOf(
            11..11,   // "first 10 are skipped" -> Start at 11
            24..24,   // "12-23 are skipped" -> Resume at 24
            27..38,   // "25 and 26 are skipped" -> Resume at 27, stop before 39
            46..47,   // "39-45 are skipped" -> Resume at 46, stop before 48
            62..67,   // "48-61 are skipped" -> Resume at 62, stop before 68
            71..71,   // "68, 69, 70 are skipped" -> Resume at 71
            143..221  // "72-142 are skipped" -> Resume at 143 until the end
        )

        val structure = mutableListOf<SimanInfo>()

        for (range in rangesToLearn) {
            for (simanNum in range) {
                // Determine the correct index in the master list (Siman 1 is at index 0)
                val listIndex = simanNum - 1

                if (listIndex in actualKitzurSeifimCounts.indices) {
                    structure.add(
                        SimanInfo(
                            simanNumber = simanNum,
                            seifCount = actualKitzurSeifimCounts[listIndex]
                        )
                    )
                }
            }
        }
        structure
    }
}

/**
 * Represents a specific Siman and its length.
 */
data class SimanInfo(
    val simanNumber: Int,
    val seifCount: Int
)