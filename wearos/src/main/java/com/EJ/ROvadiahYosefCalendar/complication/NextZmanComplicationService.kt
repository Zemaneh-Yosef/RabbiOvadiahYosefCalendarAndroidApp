package com.EJ.ROvadiahYosefCalendar.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.core.graphics.toColorInt
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity
import java.util.Date

class NextZmanComplicationService : BaseZmanComplicationService() {

    companion object {
        private const val ALERT_WINDOW_MINUTES = 15
        private val COLOR_GREEN = "#4CAF50".toColorInt()
        private val COLOR_YELLOW = "#FFC107".toColorInt()
        private val COLOR_RED = "#F44336".toColorInt()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        getComplicationData()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.RANGED_VALUE) {
            return null
        }
        return getComplicationData()
    }

    private fun getComplicationData(): ComplicationData {
        val currentTime = Date().time
        val zman = getNextUpcomingZman(applicationContext)  // single call; sets up formatters
        val msLeft = (zman?.zman?.time ?: 0L) - currentTime
        val minutesLeft = (msLeft / 60_000L).toInt()

        val arcValue: Float
        val colorRamp: ColorRamp?
        when {
            msLeft <= 0 || minutesLeft > ALERT_WINDOW_MINUTES -> {
                arcValue = 0f; colorRamp = null
            }

            minutesLeft > 10 -> {
                arcValue = minutesLeft.toFloat(); colorRamp = ColorRamp(intArrayOf(COLOR_GREEN, COLOR_GREEN), false)
            }

            minutesLeft > 5 -> {
                arcValue = minutesLeft.toFloat(); colorRamp = ColorRamp(intArrayOf(COLOR_YELLOW, COLOR_YELLOW), false)
            }

            else -> {
                arcValue = minutesLeft.coerceAtLeast(0).toFloat(); colorRamp = ColorRamp(intArrayOf(COLOR_RED, COLOR_RED), false)
            }
        }

        val monochromaticImage = MonochromaticImage.Builder(
            image = Icon.createWithResource(this, R.drawable.baseline_av_timer_24),
        ).setAmbientImage(
            ambientImage = Icon.createWithResource(this, R.drawable.baseline_av_timer_24_burn_protect)
        ).build()

        val contentDescription = PlainComplicationText.Builder(
            text = getString(R.string.complete_until_next_zman, minutesLeft.coerceAtLeast(0))
        ).build()

        val timeStr = zman?.let { formatZmanTime(it) } ?: ""

        val builder = RangedValueComplicationData.Builder(
            value = arcValue,
            min = 0f,
            max = ALERT_WINDOW_MINUTES.toFloat(),
            contentDescription = contentDescription,
        )
            .setText(
                PlainComplicationText.Builder(
                    text = timeStr
                ).build()
            )
            .setMonochromaticImage(monochromaticImage)
            .setTitle(zman?.let {
                PlainComplicationText.Builder(text = shortenTitle(it.title)).build()
            })
            .setTapAction(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        colorRamp?.let { builder.setColorRamp(it) }

        return builder.build()
    }
}