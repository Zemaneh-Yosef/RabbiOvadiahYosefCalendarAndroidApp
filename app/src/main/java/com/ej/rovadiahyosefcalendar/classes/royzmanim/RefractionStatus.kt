package com.ej.rovadiahyosefcalendar.classes.royzmanim

import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Whether the times shown for a place are still going to change: its weather, terrain or haze data are downloading, the server
 * is still building its terrain horizon, or a day's visible sunrise is still being computed. The screens show a banner meanwhile
 * (the times first appear from the saved data or an estimate, and are redrawn when the new data land).
 * Listeners are told on any change, on any thread.
 */
object RefractionStatus {

    private val downloads = ConcurrentHashMap<String, Int>()
    private val visibleSunrises = ConcurrentHashMap<String, Int>()
    private val horizonWaits = ConcurrentHashMap<String, Boolean>()
    private val failedAt = ConcurrentHashMap<String, Long>()
    private val listeners = CopyOnWriteArrayList<Runnable>()

    private fun place(lat: Double, lon: Double) = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon)

    @JvmStatic fun addListener(l: Runnable) { listeners.add(l) }
    @JvmStatic fun removeListener(l: Runnable) { listeners.remove(l) }
    private fun changed() = listeners.forEach { try { it.run() } catch (_: Exception) {} }

    /**
     * Whether the times of the place are still going to change.
     * @param date the date shown: a visible sunrise still being computed for another date doesn't change what is on the screen
     */
    @JvmStatic
    fun isUpdating(lat: Double, lon: Double, date: LocalDate?): Boolean {
        if (!RefractionSupport.enabled) return false
        val p = place(lat, lon)
        return (downloads[p] ?: 0) > 0 || horizonWaits.containsKey(p) || (date != null && (visibleSunrises["$p|$date"] ?: 0) > 0)
    }

    /** Whether the place's data are downloading (or the server is still building its terrain horizon), as opposed to being computed. */
    @JvmStatic
    fun isDownloading(lat: Double, lon: Double): Boolean {
        if (!RefractionSupport.enabled) return false
        val p = place(lat, lon)
        return (downloads[p] ?: 0) > 0 || horizonWaits.containsKey(p)
    }

    /**
     * When the last download for the place failed (the saved data are shown instead), or null when it didn't or a new one has
     * started since. The screens tell the user once, when the place has stopped updating.
     */
    @JvmStatic
    fun lastFailure(lat: Double, lon: Double): Long? = failedAt[place(lat, lon)]

    internal fun downloadStarted(lat: Double, lon: Double) {
        val p = place(lat, lon)
        downloads.merge(p, 1, Int::plus)
        failedAt.remove(p)
        changed()
    }

    internal fun downloadEnded(lat: Double, lon: Double, failed: Boolean, waitingForHorizon: Boolean) {
        val p = place(lat, lon)
        downloads.compute(p) { _, n -> if (n == null || n <= 1) null else n - 1 }
        if (waitingForHorizon) horizonWaits[p] = true else horizonWaits.remove(p)
        if (failed) failedAt[p] = System.currentTimeMillis()
        changed()
    }

    /** The server's horizon didn't arrive in time (offline, or still not built after the retries): stop waiting for it. */
    internal fun horizonWaitEnded(lat: Double, lon: Double) {
        val p = place(lat, lon)
        if (horizonWaits.remove(p) != null) changed()
    }

    /** Computing with the place's data failed (e.g. a visible sunrise): reported like a failed download. */
    internal fun computeFailed(lat: Double, lon: Double) {
        failedAt[place(lat, lon)] = System.currentTimeMillis()
        changed()
    }

    internal fun visibleSunriseStarted(lat: Double, lon: Double, date: LocalDate) {
        visibleSunrises.merge("${place(lat, lon)}|$date", 1, Int::plus)
        changed()
    }

    internal fun visibleSunriseEnded(lat: Double, lon: Double, date: LocalDate) {
        visibleSunrises.compute("${place(lat, lon)}|$date") { _, n -> if (n == null || n <= 1) null else n - 1 }
        changed()
    }
}
