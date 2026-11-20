package com.ej.rovadiahyosefcalendar

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import java.time.LocalDate
import com.ej.rovadiahyosefcalendar.classes.DpadSupport
import com.ej.rovadiahyosefcalendar.activities.ZmanimAppWidget
import com.ej.rovadiahyosefcalendar.classes.WatchZmanimSync
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionRepository
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionSupport

/**
 * Installs D-pad support and sets up the refraction data store before any calendar is built (activities, widgets, notifications and boot receivers all
 * build ROZmanimCalendar objects, which read the stored weather / terrain / haze data for their place).
 */
class ZemanehYosefApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DpadSupport.install(this)// D-pad focus ring and TV keyboard, in every screen
        RefractionRepository.init(this)
        RefractionRepository.preloadLastUsed()// in the background: the first screen then finds the data in memory
        WatchZmanimSync.init(this)
        // New data for a place: the watch gets the times they change (building its table computes its visible sunrises itself),
        // the zman notifications are set again and the widget is redrawn with them (they move the times both show)
        RefractionRepository.addDataListener { _, _ ->
            WatchZmanimSync.requestSync(this, false)
            scheduleNotificationsAndWidgetUpdate()
        }
        // A visible sunrise finished computing: only one the notifications or the widget use (yesterday to tomorrow) sets them
        // again. One for another date, e.g. a day the user went to in the app, changes nothing there.
        RefractionSupport.addVisibleSunriseListener { _, _, date ->
            val today = LocalDate.now()
            if (!date.isBefore(today.minusDays(1)) && !date.isAfter(today.plusDays(1))) scheduleNotificationsAndWidgetUpdate()
        }
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        RefractionSupport.enabled = settings.getBoolean(PREF_REFRACTION, true)
        RefractionSupport.hazeTzet = settings.getBoolean(PREF_HAZE_TZET, true)
        settings.registerOnSharedPreferenceChangeListener(settingsListener)
        registerActivityLifecycleCallbacks(foreground)
    }

    // While any screen is showing, the weather is refreshed when it goes stale (every 3 hours), not only when a screen opens:
    // someone can leave the Netz countdown or the zmanim open all night. The screens listening to RefractionRepository redraw.
    private val handler = Handler(Looper.getMainLooper())

    /** Several data updates in a row (e.g. the visible sunrise of each day finishing) make one update. */
    private val updateNotificationsAndWidget = Runnable {
        sendBroadcast(Intent(this, ZmanimNotifications::class.java))
        ZmanimAppWidget.updateAllWidgets(this)
    }
    private fun scheduleNotificationsAndWidgetUpdate() {
        handler.post {
            handler.removeCallbacks(updateNotificationsAndWidget)
            handler.postDelayed(updateNotificationsAndWidget, 3000)
        }
    }

    private var startedActivities = 0
    private val refreshTick = object : Runnable {
        override fun run() {
            handler.postDelayed(this, RefractionRepository.refreshLastPlace())
        }
    }
    private val foreground = object : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            if (startedActivities++ == 0) handler.post(refreshTick)
        }
        override fun onActivityStopped(activity: Activity) {
            if (--startedActivities == 0) handler.removeCallbacks(refreshTick)
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    /** Kept as a field: SharedPreferences holds its listeners weakly. */
    private val settingsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            PREF_REFRACTION -> RefractionSupport.enabled = prefs.getBoolean(PREF_REFRACTION, true)
            PREF_HAZE_TZET -> RefractionSupport.hazeTzet = prefs.getBoolean(PREF_HAZE_TZET, true)
        }
        if (key == PREF_REFRACTION || key == PREF_HAZE_TZET) {
            WatchZmanimSync.requestSync(this, false)
            handler.removeCallbacks(updateNotificationsAndWidget)
            updateNotificationsAndWidget.run()// the switches change the times the notifications and the widget show
        }
    }

    companion object {
        /** Settings: use weather, terrain and haze data for the zmanim (the website's refraction data). */
        const val PREF_REFRACTION = "useRefractionData"
        /** Settings: move Tzet by degrees by the evening's haze. */
        const val PREF_HAZE_TZET = "useHazeTzet"
    }
}
