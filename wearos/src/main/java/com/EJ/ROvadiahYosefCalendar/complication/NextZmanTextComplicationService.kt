package com.EJ.ROvadiahYosefCalendar.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity

class NextZmanTextComplicationService : BaseZmanComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.LONG_TEXT  -> buildLongTextData()
            ComplicationType.SHORT_TEXT -> buildShortTextData()
            else -> null
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        when (request.complicationType) {
            ComplicationType.LONG_TEXT  -> buildLongTextData()
            ComplicationType.SHORT_TEXT -> buildShortTextData()
            else -> null
        }

    private fun buildLongTextData(): LongTextComplicationData {
        val zman = getNextUpcomingZman(applicationContext)
        val timeStr = zman?.let { formatZmanTime(it) } ?: ""
        val zmanName = zman?.title ?: ""

        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = timeStr).build(),
            contentDescription = PlainComplicationText.Builder(
                text = "$zmanName $timeStr"
            ).build()
        )
            .setTitle(PlainComplicationText.Builder(text = zmanName).build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    image = Icon.createWithResource(this, R.drawable.baseline_av_timer_24)
                ).setAmbientImage(
                    ambientImage = Icon.createWithResource(this, R.drawable.baseline_av_timer_24_burn_protect)
                ).build()
            )
            .setTapAction(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private fun buildShortTextData(): ShortTextComplicationData {
        val zman = getNextUpcomingZman(applicationContext)
        val timeStr = zman?.let { formatZmanTime(it) } ?: ""
        val shortName = zman?.let { shortenTitle(it.title) } ?: ""

        return ShortTextComplicationData.Builder(
            // For SHORT_TEXT: time is the hero, short name is the label
            text = PlainComplicationText.Builder(text = timeStr).build(),
            contentDescription = PlainComplicationText.Builder(
                text = "$shortName $timeStr"
            ).build()
        )
            .setTitle(PlainComplicationText.Builder(text = shortName).build())
            // Remove the image for SHORT_TEXT complications because we don't have a lot of room as is
//            .setMonochromaticImage(
//                MonochromaticImage.Builder(
//                    image = Icon.createWithResource(this, R.drawable.baseline_av_timer_24)
//                ).setAmbientImage(
//                    ambientImage = Icon.createWithResource(this, R.drawable.baseline_av_timer_24_burn_protect)
//                ).build()
//            )
            .setTapAction(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }
}