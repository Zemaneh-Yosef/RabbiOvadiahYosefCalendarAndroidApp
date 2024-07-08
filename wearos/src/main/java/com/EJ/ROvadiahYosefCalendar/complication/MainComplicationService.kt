package com.EJ.ROvadiahYosefCalendar.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.EJ.ROvadiahYosefCalendar.classes.JewishDateInfo
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import java.util.Calendar

/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val jewishDateInfo = JewishDateInfo(false)// in Israel should not matter
        val hebrewDateFormatter = HebrewDateFormatter()
        hebrewDateFormatter.isHebrewFormat = true
        if (type == ComplicationType.SHORT_TEXT) {
            return createComplicationData(hebrewDateFormatter.formatDayOfWeek(jewishDateInfo.jewishCalendar), jewishDateInfo.jewishDayOfWeek)
        } else if (type == ComplicationType.LONG_TEXT) {
            return createComplicationData(jewishDateInfo.jewishDate, jewishDateInfo.jewishDate)
        }
        return null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val jewishDateInfo = JewishDateInfo(false)// in Israel should not matter
        val hebrewDateFormatter = HebrewDateFormatter()
        hebrewDateFormatter.isHebrewFormat = true
        return if (request.complicationType == ComplicationType.SHORT_TEXT) {
            createComplicationData(hebrewDateFormatter.formatDayOfWeek(jewishDateInfo.jewishCalendar), jewishDateInfo.jewishDayOfWeek)
        } else {
            createComplicationData(jewishDateInfo.jewishDate, jewishDateInfo.jewishDate)
        }
    }

    private fun createComplicationData(text: String, contentDescription: String) =
            ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder(contentDescription).build()
            )
                .setTapAction(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .build()
}