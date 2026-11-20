package com.ej.rovadiahyosefcalendar.classes.royzmanim

import android.util.Log
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionRepository.RefractionData
import com.kosherjava.zmanim.util.GeoLocation
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The per-place state the website's ROYZmanim.js keeps per config (WeakMaps keyed by the config object), for every calendar
 * built for a place to share: the astronomical calculator (its ray traces are cached), the equinox seasonal-hour ratios, the
 * haze delays and the visible sunrises. Keyed by the [RefractionData] in use (a new download is a new key) and the place.
 */
object RefractionSupport {
    private const val TAG = "RefractionSupport"

    /** Whether the weather / terrain / haze data are used at all (settings); off = the calculator's default model. */
    @JvmStatic @Volatile var enabled = true
    /** Apply the haze delay to Tzet by degrees (the website's hazeTzet; default true where there is haze data). */
    @JvmStatic @Volatile var hazeTzet = true
    /** Largest haze delay either way, minutes (null = none). */
    @JvmStatic @Volatile var hazeMaxDelayMinutes: Double? = 15.0

    private fun dataKey(data: RefractionData?) = if (data == null) "none" else "d" + System.identityHashCode(data)
    private fun placeKey(geo: GeoLocation) = "${geo.latitude}|${geo.longitude}|${geo.elevation}|${geo.zoneId}"

    /** The data for a place as stored (null when off or when nothing is stored yet). */
    @JvmStatic
    fun dataFor(geo: GeoLocation): RefractionData? = if (!enabled) null else try { RefractionRepository.cached(geo.latitude, geo.longitude) } catch (e: Exception) { null }

    private val calculators = ROYSPACalculator.lru<String, ROYSPACalculator>(16)

    /** One calculator per data and latitude, shared by every calendar for the place (as the website's calculatorFor). */
    @JvmStatic
    fun calculatorFor(data: RefractionData?, geo: GeoLocation): ROYSPACalculator {
        val key = dataKey(data) + "|" + String.format(java.util.Locale.ROOT, "%.6f", geo.latitude)
        synchronized(calculators) {
            calculators[key]?.let { return it }
            val calc = ROYSPACalculator()
            calc.setAtmosphereProvider(data?.provider)
            calc.configureForLocation(geo)
            calculators[key] = calc
            return calc
        }
    }

    private val equinoxCache = ROYSPACalculator.lru<String, Double>(256)

    /** A cached number for a place / data / key (the equinox ratios: the same for every date of a year). */
    @JvmStatic
    fun cachedNumber(data: RefractionData?, geo: GeoLocation, key: String, compute: () -> Double): Double {
        val k = dataKey(data) + "|" + placeKey(geo) + "|" + key
        synchronized(equinoxCache) { equinoxCache[k] }?.let { return it }
        val v = compute()
        synchronized(equinoxCache) { equinoxCache[k] = v }
        return v
    }

    // ------------------------------------------------------------------------------------------- haze delay

    /** ms (positive = later) and the model's status. */
    class HazeDelayEntry(@JvmField val ms: Double, @JvmField val status: StarNightfall.HazeDelayStatus)

    private val hazeCache = ROYSPACalculator.lru<String, HazeDelayEntry>(128)

    /**
     * How much later (negative: earlier) nightfall by the stars comes on a date at the haze when the Sun is `degree` down after
     * sunset than at the place's average haze then; null when off or without data. Where the model has only a bound, the cap
     * stands in on that side (a fallback, not a modeled time).
     */
    @JvmStatic
    fun hazeDelay(data: RefractionData?, geo: GeoLocation, date: LocalDate, degree: Double): HazeDelayEntry? {
        val haze = data?.haze ?: return null
        if (!hazeTzet) return null
        val key = dataKey(data) + "|" + placeKey(geo) + "|" + date + "|" + degree + "|" + hazeMaxDelayMinutes
        synchronized(hazeCache) { hazeCache[key] }?.let { return it }
        val d = try {
            val h = StarNightfall.hazeOn(haze, date, StarNightfall.HazeMoment(degree, false))
            if (h != null) StarNightfall.hazeDelay(date, geo.latitude, geo.longitude,
                StarNightfall.Options(aod = h.aod, blpCdM2 = haze.blpCdM2, elevationM = geo.elevation), h.aodRef)
            else StarNightfall.HazeDelay(0.0, StarNightfall.HazeDelayStatus.NONE)
        } catch (e: Exception) {
            Log.e(TAG, "Haze delay failed", e)
            StarNightfall.HazeDelay(0.0, StarNightfall.HazeDelayStatus.NONE)
        }
        val capMin = hazeMaxDelayMinutes
        val cap = if (capMin == null) Double.POSITIVE_INFINITY else capMin * 60000
        var ms = d.ms
        when (d.status) {
            StarNightfall.HazeDelayStatus.UNRESOLVED -> ms = 0.0
            StarNightfall.HazeDelayStatus.AT_LEAST -> ms = max(ms, if (cap.isInfinite()) ms else cap)
            StarNightfall.HazeDelayStatus.AT_MOST -> ms = min(ms, if (cap.isInfinite()) ms else -cap)
            else -> {}
        }
        ms = max(-cap, min(cap, ms))
        val entry = HazeDelayEntry(ms, d.status)
        synchronized(hazeCache) { hazeCache[key] = entry }
        return entry
    }

    // ------------------------------------------------------------------------------------------- visible sunrise

    private val visibleSunriseListeners = java.util.concurrent.CopyOnWriteArrayList<(Double, Double, LocalDate) -> Unit>()

    /** Called (on the visible sunrise thread) with the place and date of each visible sunrise computed in the background. */
    @JvmStatic
    fun addVisibleSunriseListener(l: (Double, Double, LocalDate) -> Unit) { visibleSunriseListeners.add(l) }

    private val visibleCache = ROYSPACalculator.lru<String, Double>(512)
    private val pending = HashSet<String>()
    private val visibleExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "visible-sunrise").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }

    /**
     * Visible sunrise over the terrain (epoch ms) for a date: the cached value; NaN when there is no horizon or the Sun is not
     * seen to rise. Ray tracing is not cheap, so with `defer` a miss is computed on a background thread (listeners of
     * [RefractionRepository] are told when it lands) and an estimate from a neighbouring day stands in meanwhile, else NaN
     * (the caller then shows sea level) - as the website computes it in a worker.
     * @return the value and whether it is an estimate
     */
    /**
     * Whether the visible sunrise for a date is already known without computing it: in memory, or stored from these same data
     * (true too when there is nothing to compute: no horizon).
     */
    @JvmStatic
    fun isVisibleSunriseKnown(data: RefractionData?, geo: GeoLocation, date: LocalDate, options: VisibleOptions, tag: String = ""): Boolean {
        if (data?.horizon == null) return true
        val key = dataKey(data) + "|" + placeKey(geo) + "|" + options + "|" + tag + "|"
        if (synchronized(visibleCache) { visibleCache[key + date] } != null) return true
        val stored = VisibleSunriseStore.get(placeKey(geo) + "|" + options + "|" + tag + "|" + date)
        return stored != null && stored.first == fingerprint(data)
    }

    @JvmStatic
    fun visibleSunrise(data: RefractionData?, geo: GeoLocation, date: LocalDate, options: VisibleOptions, defer: Boolean, tag: String = ""): Pair<Double, Boolean> {
        val horizon = data?.horizon ?: return Pair(Double.NaN, false)
        val key = dataKey(data) + "|" + placeKey(geo) + "|" + options + "|" + tag + "|"
        synchronized(visibleCache) { visibleCache[key + date] }?.let { return Pair(it, false) }
        // computed in an earlier run of the app: the same data -> the same time; older data -> a close estimate meanwhile
        val storeKey = placeKey(geo) + "|" + options + "|" + tag + "|" + date
        val fingerprint = fingerprint(data)
        val stored = VisibleSunriseStore.get(storeKey)
        if (stored != null && stored.first == fingerprint) {
            synchronized(visibleCache) { visibleCache[key + date] = stored.second }
            return Pair(stored.second, false)
        }
        val calc = calculatorFor(data, geo)
        val compute = {
            val ms = try { calc.getVisibleSunrise(date, geo, horizon, options) } catch (e: Exception) { Log.e(TAG, "Visible sunrise failed", e); Double.NaN }
            synchronized(visibleCache) { visibleCache[key + date] = ms }
            VisibleSunriseStore.put(storeKey, fingerprint, ms)
            ms
        }
        if (!defer) return Pair(compute(), false)
        val first: Boolean
        synchronized(pending) { first = pending.add(key + date) }
        if (first) {
            RefractionStatus.visibleSunriseStarted(geo.latitude, geo.longitude, date)
            visibleExecutor.execute {
                try { compute() } finally { synchronized(pending) { pending.remove(key + date) } }
                RefractionRepository.notifyListeners(geo.latitude, geo.longitude)
                visibleSunriseListeners.forEach { try { it(geo.latitude, geo.longitude, date) } catch (e: Exception) { Log.e(TAG, "listener failed", e) } }
                RefractionStatus.visibleSunriseEnded(geo.latitude, geo.longitude, date)
            }
        }
        if (stored != null && stored.second.isFinite()) return Pair(stored.second, true)
        // the visible-minus-sea-level gap of the nearest day known (within 3 days), applied to this day's sea level
        for (k in 1L..3L) for (d in listOf(date.minusDays(k), date.plusDays(k))) {
            val known = synchronized(visibleCache) { visibleCache[key + d] } ?: continue
            if (!known.isFinite()) continue
            val gap = known - calc.getSunrises(d, geo)[0]
            val est = calc.getSunrises(date, geo)[0] + gap
            if (est.isFinite()) return Pair(est, true)
        }
        return Pair(Double.NaN, false)
    }

    private val fingerprints = ROYSPACalculator.lru<String, String>(16)

    /**
     * What a visible sunrise depends on, in a form that survives a restart of the app (unlike [dataKey]): the forecast's download
     * time, whether there are monthly normals, and the terrain horizon's contents.
     */
    private fun fingerprint(data: RefractionData): String {
        val k = dataKey(data)
        synchronized(fingerprints) { fingerprints[k] }?.let { return it }
        var h = 17L
        fun mix(v: Double) { h = h * 31 + v.toBits() }
        fun mixProfile(p: HorizonProfile?) {
            if (p == null) { mix(-1.0); return }
            mix(p.points.size.toDouble())
            for (pt in p.points) {
                mix(pt.azimuthDeg)
                when (pt) {
                    is DemHorizonPoint -> { mix(pt.distanceKm); mix(pt.heightM) }
                    is ObservedHorizonPoint -> mix(pt.elevationDeg)
                }
            }
        }
        data.horizon?.let { set ->
            for (p in set.points) { mix(p.lat); mix(p.lon); mix(p.height); mixProfile(p.sunrise); mixProfile(p.sunset) }
        }
        val f = "${data.forecastFetchedAt ?: "none"}|${data.normals != null}|${java.lang.Long.toHexString(h)}"
        synchronized(fingerprints) { fingerprints[k] = f }
        return f
    }

    /**
     * The visible sunrises computed, kept in a file so that the next run of the app shows them at once instead of the sea-level
     * sunrise while they are computed again. Only today and later are kept.
     */
    private object VisibleSunriseStore {
        private const val FILE = "visible-sunrises.json"
        private const val MAX_ENTRIES = 400
        private var entries: LinkedHashMap<String, Pair<String, Double>>? = null
        private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "visible-sunrise-store").apply { isDaemon = true } }
        @Volatile private var writeQueued = false

        private fun file() = RefractionRepository.storageDir()?.let { java.io.File(it, FILE) }

        @Synchronized
        private fun load(): LinkedHashMap<String, Pair<String, Double>> {
            entries?.let { return it }
            val map = LinkedHashMap<String, Pair<String, Double>>()
            try {
                val f = file()
                if (f != null && f.exists()) {
                    val json = org.json.JSONObject(f.readText())
                    for (k in json.keys()) {
                        val e = json.getJSONObject(k)
                        map[k] = Pair(e.getString("f"), if (e.isNull("ms")) Double.NaN else e.getDouble("ms"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not read the stored visible sunrises", e)
            }
            entries = map
            return map
        }

        @Synchronized
        fun get(key: String): Pair<String, Double>? = load()[key]

        @Synchronized
        fun put(key: String, fingerprint: String, ms: Double) {
            val map = load()
            map.remove(key)
            map[key] = Pair(fingerprint, ms)
            if (!writeQueued) {
                writeQueued = true
                writer.execute { save() }// one write for the several days computed in a row
            }
        }

        private fun save() {
            val json = org.json.JSONObject()
            synchronized(this) {
                writeQueued = false
                val map = load()
                val today = LocalDate.now().minusDays(1)// the time zone of the place may be a day behind
                map.entries.removeAll { e ->
                    val date = try { LocalDate.parse(e.key.substringAfterLast('|')) } catch (ex: Exception) { null }
                    date == null || date.isBefore(today)
                }
                while (map.size > MAX_ENTRIES) map.remove(map.keys.first())
                for ((k, v) in map) json.put(k, org.json.JSONObject().put("f", v.first).put("ms", if (v.second.isFinite()) v.second else org.json.JSONObject.NULL))
            }
            try {
                val f = file() ?: return
                val tmp = java.io.File(f.parentFile, "$FILE.tmp")
                tmp.writeText(json.toString())
                if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            } catch (e: Exception) {
                Log.e(TAG, "Could not store the visible sunrises", e)
            }
        }
    }

    /** epoch ms -> Instant, null for NaN; and the website's sanity check (more than an hour from `sunrise` is a bad profile). */
    @JvmStatic
    fun checkedInstant(ms: Double, sunrise: Instant?): Instant? {
        if (!ms.isFinite()) return null
        val t = Instant.ofEpochMilli(Math.round(ms))
        if (sunrise != null && abs(t.toEpochMilli() - sunrise.toEpochMilli()) > 3600000) return null
        return t
    }
}
