package com.EJ.ROvadiahYosefCalendar.presentation

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.EJ.ROvadiahYosefCalendar.classes.Utils
import com.EJ.ROvadiahYosefCalendar.databinding.ActivityZmanAlarmBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ZmanAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityZmanAlarmBinding
    private lateinit var sharedPref: SharedPreferences
    private var notificationId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()

        binding = ActivityZmanAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences(MainActivity.SHARED_PREF, MODE_PRIVATE)

        processIntent(intent)

        binding.dismissButton.setOnClickListener { dismissAlarm() }

        // Back press = dismiss, same as tapping the button
        onBackPressedDispatcher.addCallback(this) { dismissAlarm() }
    }

    /**
     * A second alarm fires while the screen is already showing — update the display in place
     * rather than stacking a new activity (requires launchMode="singleTop" in manifest).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    // -------------------------------------------------------------------------
    // Window flags — must be set before setContentView
    // -------------------------------------------------------------------------

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Modern API: declarative helpers (also set in manifest, but belt-and-suspenders)
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        // Keep screen on for the full duration the alarm is displayed
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // -------------------------------------------------------------------------
    // Intent handling
    // -------------------------------------------------------------------------

    private fun processIntent(intent: Intent) {
        val zmanName    = intent.getStringExtra("zmanName")  ?: return finish()
        val zmanTimeStr = intent.getStringExtra("zmanTime")  ?: return finish()
        val zmanTimeLong = zmanTimeStr.toLongOrNull()        ?: return finish()

        // Mirror the notification ID formula from ZmanNotification.java so we can cancel it
        notificationId = (zmanTimeLong % Int.MAX_VALUE).toInt()

        binding.zmanNameText.text = zmanName
        binding.zmanTimeText.text = formatZmanTime(Date(zmanTimeLong))
    }

    // -------------------------------------------------------------------------
    // Time formatting — mirrors ZmanNotification.java logic
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Dismiss
    // -------------------------------------------------------------------------

    private fun dismissAlarm() {
        // Cancel the companion notification so its sound/vibration stops immediately
        if (notificationId != -1) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
        finish()
    }
}
