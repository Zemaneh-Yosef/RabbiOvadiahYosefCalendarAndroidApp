package com.EJ.ROvadiahYosefCalendar.presentation

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.EJ.ROvadiahYosefCalendar.R
import com.EJ.ROvadiahYosefCalendar.classes.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Gold = Color(0xFFC6AA14)

class ZmanAlarmActivity : ComponentActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var notificationId: Int = -1

    // Compose-observable: updated from both onCreate and onNewIntent
    private val zmanName = mutableStateOf("")
    private val zmanTime = mutableStateOf("")

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private fun startAlarmFeedback() {
        // Vibration — primary feedback on Wear OS (works on all watches)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 600, 300, 600, 300, 600),
                1  // loop from index 1 so it keeps buzzing until dismissed
            )
        )

        // Alarm tone — secondary feedback for watches with audio output
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)?.also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.isLooping = true
                it.play()
            }
        } catch (_: Exception) {
            // Silent fail — vibration alone is sufficient on watches without speakers
        }
    }

    private fun stopAlarmFeedback() {
        ringtone?.stop();  ringtone = null
        vibrator?.cancel(); vibrator = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()

        sharedPref = getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)
        processIntent(intent)
        startAlarmFeedback()

        setContent {
            ZmanAlarmScreen(
                zmanName = zmanName.value,
                zmanTime = zmanTime.value,
                onDismiss = ::dismissAlarm
            )
        }

        onBackPressedDispatcher.addCallback(this) { dismissAlarm() }
    }

    /**
     * A second alarm fires while the screen is already showing — update state in place.
     * Requires android:launchMode="singleTop" in the manifest.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmFeedback()
    }

    // ── Window flags ──────────────────────────────────────────────────────────

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // ── Intent ────────────────────────────────────────────────────────────────

    private fun processIntent(intent: Intent) {
        val name     = intent.getStringExtra("zmanName") ?: return finish()
        val timeStr  = intent.getStringExtra("zmanTime") ?: return finish()
        val timeLong = timeStr.toLongOrNull()             ?: return finish()

        notificationId = (timeLong % Int.MAX_VALUE).toInt()
        zmanName.value = name
        zmanTime.value = formatZmanTime(Date(timeLong))
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    private fun formatZmanTime(date: Date): String {
        val showSeconds = sharedPref.getBoolean("ShowSeconds", false)
        val timezone    = sharedPref.getString("currentTimezone", "") ?: ""
        val isHebrew    = Utils.isLocaleHebrew(this)

        val pattern = buildString {
            append(if (isHebrew) "H:mm" else "h:mm")
            if (showSeconds) append(":ss")
            if (!isHebrew) append(" aa")
        }

        return SimpleDateFormat(pattern, Locale.getDefault()).apply {
            if (timezone.isNotEmpty()) timeZone = TimeZone.getTimeZone(timezone)
        }.format(date)
    }

    // ── Dismiss ───────────────────────────────────────────────────────────────

    private fun dismissAlarm() {
        if (notificationId != -1) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
        stopAlarmFeedback()
        finish()
    }
}

// UI
@Composable
private fun ZmanAlarmScreen(
    zmanName: String,
    zmanTime: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        // Content stacks from the top, mirroring the original ConstraintLayout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 18.dp, start = 18.dp, end = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Alarm icon
            Icon(
                painter = painterResource(R.drawable.outline_alarm_24),
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(26.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Zman name — up to two lines for long Hebrew names
            Text(
                text = zmanName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // Zman time — the focal point of the screen
            Text(
                text = zmanTime,
                color = Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // Dismiss
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = stringResource(R.string.alarm_dismiss),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}