package com.ej.rovadiahyosefcalendar

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.ej.rovadiahyosefcalendar.classes.DpadSupport
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
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        RefractionSupport.enabled = settings.getBoolean(PREF_REFRACTION, true)
        RefractionSupport.hazeTzet = settings.getBoolean(PREF_HAZE_TZET, true)
        settings.registerOnSharedPreferenceChangeListener(settingsListener)
        registerActivityLifecycleCallbacks(foreground)
    }

    // While any screen is showing, the weather is refreshed when it goes stale (every 3 hours), not only when a screen opens:
    // someone can leave the Netz countdown or the zmanim open all night. The screens listening to RefractionRepository redraw.
    private val handler = Handler(Looper.getMainLooper())
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
    }

    companion object {
        /** Settings: use weather, terrain and haze data for the zmanim (the website's refraction data). */
        const val PREF_REFRACTION = "useRefractionData"
        /** Settings: move Tzet by degrees by the evening's haze. */
        const val PREF_HAZE_TZET = "useHazeTzet"
    }
}
