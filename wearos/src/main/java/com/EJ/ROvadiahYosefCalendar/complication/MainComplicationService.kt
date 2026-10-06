package com.EJ.ROvadiahYosefCalendar.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import java.util.Calendar

/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingTimelineComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val jewishDateInfo = JewishDateInfo(false)// in Israel should not matter
        jewishDateInfo.resetLocale(applicationContext)
        val hebrewDateFormatter = HebrewDateFormatter()
        hebrewDateFormatter.isHebrewFormat = true
        if (type == ComplicationType.SHORT_TEXT) {
            return createShortComplicationData(hebrewDateFormatter.formatDayOfWeek(jewishDateInfo.jewishCalendar), jewishDateInfo.jewishDayOfWeek)
        } else if (type == ComplicationType.LONG_TEXT) {
            return createLongComplicationData(jewishDateInfo.jewishCalendar.toString(), jewishDateInfo.jewishCalendar.toString())
        }
        return null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val jewishDateInfo = JewishDateInfo(false)// in Israel should not matter
        jewishDateInfo.resetLocale(baseContext)
        val hebrewDateFormatter = HebrewDateFormatter()
        hebrewDateFormatter.isHebrewFormat = true
        val today = createComplicationData(request.complicationType, jewishDateInfo, hebrewDateFormatter) ?: return null
        val midnight = Calendar.getInstance()
        midnight.add(Calendar.DATE, 1)
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.set(Calendar.MILLISECOND, 0)
        val dayAfter = midnight.clone() as Calendar
        dayAfter.add(Calendar.DATE, 1)
        jewishDateInfo.setCalendar(midnight)
        val tomorrow = createComplicationData(request.complicationType, jewishDateInfo, hebrewDateFormatter) ?: return null
        return ComplicationDataTimeline(today, listOf(TimelineEntry(TimeInterval(midnight.toInstant(), dayAfter.toInstant()), tomorrow)))
    }

    private fun createComplicationData(type: ComplicationType, jewishDateInfo: JewishDateInfo, hebrewDateFormatter: HebrewDateFormatter): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                createShortComplicationData(
                    hebrewDateFormatter.formatDayOfWeek(jewishDateInfo.jewishCalendar),
                    jewishDateInfo.jewishDayOfWeek
                )
            }
            ComplicationType.LONG_TEXT -> {
                createLongComplicationData(
                    jewishDateInfo.jewishCalendar.toString(),
                    jewishDateInfo.jewishCalendar.toString()
                )
            }
            else -> {
                null
            }
        }
    }

    private fun createShortComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
            .setTapAction(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()

    private fun createLongComplicationData(text: String, contentDescription: String) =
        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
            .setTapAction(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
}