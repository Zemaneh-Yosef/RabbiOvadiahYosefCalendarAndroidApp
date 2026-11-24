package com.ej.rovadiahyosefcalendar.classes.royzmanim

import java.util.concurrent.ConcurrentHashMap

/**
 * When the weather models behind a stored forecast have published a newer run, from Open-Meteo's own record of each model's last
 * run (https://api.open-meteo.com/data/<domain>/static/meta.json: a few hundred bytes, not counted against the forecast quota). So
 * a forecast is downloaded again when a run comes out, not on a fixed timer (port of the website's model-runs.js):
 *
 *   NOAA HRRR (US, first 48 h)  every hour        DWD ICON-EU / ICON-D2         every 3 h
 *   NOAA GFS, DWD ICON          every 6 h         Canada's GEM (HRDPS / RDPS)   every 6 h, GDPS every 12 h
 *   CAMS (haze)                 every 12 h        CAMS Europe                   every 24 h
 *
 * A record that has stopped being updated can't say when a run is out; for such a model the stored forecast counts as old once the
 * model's update interval has passed since it was fetched. Where no record can be read at all (offline), `fallbackTtlMs` is the age
 * limit. Reading a record is a network request: [runState] with network = false (the main thread) uses only those already read.
 */
object ModelRuns {
    private const val FORECAST_DATA = "https://api.open-meteo.com/data/"
    private const val AIR_QUALITY_DATA = "https://air-quality-api.open-meteo.com/data/"
    /** A record is read again after this (the run it names is still the newest until then). */
    private const val RECORD_REUSE_MS = 5 * 60 * 1000L
    /** A run later than its schedule: check again this often until it is out. */
    private const val LATE_RECHECK_MS = 10 * 60 * 1000L
    /** A record whose last run is this many update intervals old (plus a day) has stopped being updated. */
    private const val STALE_INTERVALS = 3

    data class Domain(val host: String, val domain: String)
    private class Run(val availableAt: Long, val intervalMs: Long)
    private class Record(val at: Long, val run: Run?)

    /** Whether data fetched then is still the newest, and when to look again (epoch ms). */
    data class State(val current: Boolean, val nextCheckAt: Long)

    private fun inBox(lat: Double, lon: Double, s: Double, n: Double, w: Double, e: Double) = lat in s..n && lon in w..e

    /** The model domains a place's path forecast comes from: the chosen model's, and GFS's where that model ends. */
    @JvmStatic
    fun forecastDomains(lat: Double, lon: Double): List<Domain> {
        val model = OpenMeteoPath.chooseModel(lat, lon)
        val names = LinkedHashSet<String>()
        // HRRR only where gfs_seamless is the chosen model: as the fallback, GFS serves only days past HRRR's 48 h
        if (model == "gfs_seamless" && inBox(lat, lon, 21.0, 53.0, -135.0, -60.0)) names.add("ncep_hrrr_conus")
        if (model == "icon_seamless") {
            names.add("dwd_icon")
            if (inBox(lat, lon, 29.5, 70.5, -23.5, 62.5)) names.add("dwd_icon_eu")
            if (inBox(lat, lon, 43.18, 58.08, -3.94, 20.34)) names.add("dwd_icon_d2")
        } else if (model == "gem_seamless") {
            names.add("cmc_gem_gdps"); names.add("cmc_gem_rdps")
            if (inBox(lat, lon, 39.6, 75.0, -152.0, -40.7)) names.add("cmc_gem_hrdps")
        }
        // GFS: the chosen model, or the fallback where the chosen one ends
        names.add("ncep_gfs013"); names.add("ncep_gfs025")
        return names.map { Domain(FORECAST_DATA, it) }
    }

    /** The domains of the haze forecast (CAMS through Open-Meteo's air-quality API, CAMS Europe inside its area). */
    @JvmStatic
    fun hazeDomains(lat: Double, lon: Double): List<Domain> {
        val out = arrayListOf(Domain(AIR_QUALITY_DATA, "cams_global"))
        if (inBox(lat, lon, 30.05, 71.95, -24.95, 44.95)) out.add(Domain(AIR_QUALITY_DATA, "cams_europe"))
        return out
    }

    private val records = ConcurrentHashMap<String, Record>()

    /** A domain's last run, or null (unreadable / not read yet without the network). */
    private fun lastRun(d: Domain, network: Boolean): Run? {
        val url = "${d.host}${d.domain}/static/meta.json"
        val now = System.currentTimeMillis()
        val hit = records[url]
        if (hit != null && (!network || now - hit.at < RECORD_REUSE_MS)) return hit.run
        if (!network) return null
        val run = try {
            val j = WeatherHttp.getJson(url, timeoutMs = 5000, what = "model run record")
            val availableAt = (j.optDouble("last_run_availability_time", Double.NaN) * 1000).toLong()
            val intervalMs = (j.optDouble("update_interval_seconds", Double.NaN) * 1000).toLong()
            if (availableAt > 0 && intervalMs > 0) Run(availableAt, intervalMs) else null
        } catch (e: Exception) {
            null
        }
        records[url] = Record(now, run)
        return run
    }

    /**
     * Whether data fetched at `fetchedAt` is still the newest these domains have, and when to look again: when the next run is due
     * (the earliest of the domains'), or the age limit's end.
     * @param network read records not read in the last few minutes (never on the main thread)
     */
    @JvmStatic
    fun runState(domains: List<Domain>, fetchedAt: Long, fallbackTtlMs: Long, network: Boolean): State {
        val now = System.currentTimeMillis()
        var current = true
        var nextCheckAt = Long.MAX_VALUE
        var anyRecord = false
        for (d in domains) {
            val run = lastRun(d, network) ?: continue
            anyRecord = true
            val stale = now - run.availableAt > STALE_INTERVALS * run.intervalMs + 86400000L
            if (stale) {
                // no record of its runs: old after one interval
                if (now >= fetchedAt + run.intervalMs) current = false
                nextCheckAt = minOf(nextCheckAt, fetchedAt + run.intervalMs)
                continue
            }
            if (run.availableAt > fetchedAt) current = false
            val due = run.availableAt + run.intervalMs
            nextCheckAt = minOf(nextCheckAt, if (due > now) due else now + LATE_RECHECK_MS)
        }
        if (!anyRecord) {
            current = now < fetchedAt + fallbackTtlMs
            nextCheckAt = fetchedAt + fallbackTtlMs
        }
        // old already: whoever asked downloads now; the time after that is the next run's
        if (!current && nextCheckAt <= now) nextCheckAt = now + LATE_RECHECK_MS
        return State(current, nextCheckAt)
    }
}
