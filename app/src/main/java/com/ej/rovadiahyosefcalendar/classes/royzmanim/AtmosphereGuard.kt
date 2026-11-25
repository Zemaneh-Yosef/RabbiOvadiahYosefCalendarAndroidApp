package com.ej.rovadiahyosefcalendar.classes.royzmanim

import android.util.Log
import com.kosherjava.zmanim.util.GeoLocation
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Collections
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign

/**
 * Guardrails for the weather and climate data the zmanim are computed from (port of the website's atmosphere-guard.js).
 * A value that cannot be real air is refused, and the next source in line is used instead (chainProviders), or, when
 * none is left, the calculator's standard model. The limits are the extremes the Earth has recorded (WMO), with a
 * margin, so real weather - however rare - is never refused; they catch values no air can have, not unusual ones.
 * See the website file for the sources of each limit.
 *
 * The terrain horizon the visible sunrise is computed over ([horizonProblem]) gets the same treatment, stored or downloaded: a
 * refused horizon is not used (a stored one is downloaded again; without one the zmanim use the sea-level sunrise). It must be
 * this location's - its area contains it, or its points lie within 5 km of it - and real ground: observer and terrain heights
 * within the ground limits (an observer up to 1,000 m higher: a tower), terrain distances more than 0 and at most 500 km, and
 * terrain angles at most 60 deg above the observer's eye level by geometry alone (the highest skylines the server finds are
 * ~20 deg; a cliff 1,000 m high a kilometre away is 45 deg).
 */
object AtmosphereGuard {
    val SURFACE_C = doubleArrayOf(-95.0, 60.0)
    val ALOFT_C = doubleArrayOf(-110.0, 60.0)
    val SEA_LEVEL_MB = doubleArrayOf(860.0, 1090.0)
    val GROUND_M = doubleArrayOf(-500.0, 9000.0)
    const val PROFILE_TOP_M = 80000.0
    val SKIN_C = doubleArrayOf(-95.0, 75.0)
    const val DEW_BELOW_C = 0.5
    const val DEW_MIN_C = -120.0
    val RH_PERCENT = doubleArrayOf(0.0, 105.0)
    val AOD = doubleArrayOf(0.0, 10.0)
    val COOLING_C = doubleArrayOf(5.0, 0.05)
    val WARMING_C = doubleArrayOf(20.0, 0.1)
    const val HYPSOMETRIC_TOLERANCE = 0.15
    const val HYPSOMETRIC_SLACK_M = 30.0
    const val MAX_ANOMALY_C = 45.0
    const val SPIKE_C = 15.0
    const val SPIKE_MB = 10.0
    const val PROFILE_MAX_KM = 1000.0
    const val OBSERVER_ABOVE_GROUND_M = 1000.0
    const val HORIZON_MAX_KM = 500.0
    const val HORIZON_MAX_DEG = 60.0
    const val HORIZON_PLACE_KM = 5.0
    private const val EARTH_M = 6371000.0
    private const val KM_PER_DEG = 111.195

    private const val RD = 287.05
    private const val G = 9.80665
    private const val TAG = "AtmosphereGuard"

    private val reported: MutableSet<String> = Collections.synchronizedSet(HashSet())

    /** Each refusal is reported once, so a bad source shows up in the log without flooding it. */
    @JvmStatic
    @JvmOverloads
    fun report(what: String, kind: String = "weather data") {
        if (!reported.add(what)) return
        if (reported.size > 200) reported.clear()
        try { Log.w(TAG, "$kind refused: $what") } catch (e: RuntimeException) { /* unit tests: no Android log */ }
    }

    private fun within(v: Double?, range: DoubleArray) = v != null && v.isFinite() && v >= range[0] && v <= range[1]

    /** Pressure at height h (m) carried to sea level with the air's own temperature (C), hPa. */
    @JvmStatic
    fun seaLevelPressure(pMb: Double, heightM: Double, tC: Double) = pMb * exp(G * heightM / (RD * (tC + 273.15 + 0.00325 * heightM)))

    /** Why a surface spec cannot be real air, or null if it can. */
    @JvmStatic
    fun surfaceProblem(spec: SurfaceAir): String? {
        val t = spec.temperatureC
        if (!within(t, SURFACE_C)) return "temperature $t C"
        if (spec.heightM != null && !within(spec.heightM, GROUND_M)) return "height ${spec.heightM} m"
        val p = spec.pressureMb
        if (p != null) {
            if (!p.isFinite() || p <= 0) return "pressure $p hPa"
            val slp = seaLevelPressure(p, spec.heightM ?: 0.0, t)
            // without a height the pressure may be a station's anywhere up to the highest ground
            val range = if (spec.heightM != null) SEA_LEVEL_MB else doubleArrayOf(250.0, SEA_LEVEL_MB[1])
            if (!within(if (spec.heightM != null) slp else p, range)) return "pressure $p hPa at ${spec.heightM ?: "?"} m"
        }
        val td = spec.dewPointC
        if (td != null && (!td.isFinite() || td > t + DEW_BELOW_C || td < DEW_MIN_C)) return "dew point $td C with air at $t C"
        if (spec.relativeHumidity != null && !within(spec.relativeHumidity, RH_PERCENT)) return "relative humidity ${spec.relativeHumidity}%"
        return null
    }

    /** Why a path cannot be real air, or null if it can (checked once per path object). */
    @JvmStatic
    fun pathProblem(path: PathAir): String? = path.checkedProblem()

    internal fun checkPath(path: List<PathProfile>): String? {
        if (path.isEmpty()) return "empty path"
        for (col in path) {
            val at = "at ${col.distanceKm} km"
            if (!within(col.distanceKm, doubleArrayOf(0.0, PROFILE_MAX_KM))) return "profile distance ${col.distanceKm} km"
            if (col.skinC != null && !within(col.skinC, SKIN_C)) return "water ${col.skinC} C $at"
            val lv = col.levels
            if (lv.size < 2) return "profile with ${lv.size} levels $at"
            for (i in lv.indices) {
                val (h, t, p) = Triple(lv[i].h, lv[i].t, lv[i].p)
                if (!within(h, doubleArrayOf(GROUND_M[0], PROFILE_TOP_M))) return "level height $h m $at"
                // the air at the bottom of the column is surface air; above it, the air aloft's limits
                val tr = if (h <= GROUND_M[1] && i == 0) SURFACE_C else ALOFT_C
                if (!within(t, tr)) return "$t C at $h m $at"
                if (!p.isFinite() || p <= 0 || p > SEA_LEVEL_MB[1] + 30) return "$p hPa at $h m $at"
                val td = lv[i].td
                val rh = lv[i].rh
                if (td != null && (!td.isFinite() || td > t + DEW_BELOW_C || td < DEW_MIN_C)) return "dew point $td C with air at $t C, $h m $at"
                if (rh != null && !within(rh, RH_PERCENT)) return "relative humidity $rh% at $h m $at"
                if (i == 0) {
                    if (!within(seaLevelPressure(p, h, t), SEA_LEVEL_MB)) return "$p hPa at $h m $at"
                    continue
                }
                val b = lv[i - 1]
                val dz = h - b.h
                if (!(dz > 0)) return "levels not rising (${b.h} m, then $h m) $at"
                if (!(p < b.p)) return "pressure not falling (${b.p} hPa at ${b.h} m, $p hPa at $h m) $at"
                // the hypsometric equation: the layer is as thick as its air's mean temperature says
                val thick = RD * ((b.t + t) / 2 + 273.15) / G * ln(b.p / p)
                if (abs(thick - dz) > HYPSOMETRIC_TOLERANCE * dz + HYPSOMETRIC_SLACK_M) return "${b.p} -> $p hPa over ${dz.toInt()} m (the air says ${thick.toInt()} m) $at"
                val dt = t - b.t
                if (-dt > COOLING_C[0] + COOLING_C[1] * dz || dt > WARMING_C[0] + WARMING_C[1] * dz) return "${b.t} -> $t C over ${dz.toInt()} m $at"
            }
        }
        return null
    }

    /** Why an atmosphere spec cannot be real air, or null if it can. */
    @JvmStatic
    fun specProblem(spec: AtmosphereSpec?): String? = when (spec) {
        null -> null
        is PathAir -> pathProblem(spec)
        is SurfaceAir -> surfaceProblem(spec)
    }

    /** The temperature (C) near the ground a spec describes; NaN if none. */
    @JvmStatic
    fun groundTemperature(spec: AtmosphereSpec?): Double = when (spec) {
        null -> Double.NaN
        is SurfaceAir -> spec.temperatureC
        is PathAir -> spec.path.minByOrNull { it.distanceKm }?.levels?.firstOrNull()?.t ?: Double.NaN
    }

    /**
     * The height (m) of the temperature [groundTemperature] reads: the surface spec's heightM, or the lowest level of the path's
     * column nearest the observer; else the observer's. 0 if none.
     */
    @JvmStatic
    fun groundHeight(spec: AtmosphereSpec?, geo: GeoLocation?): Double {
        var h = when (spec) {
            is PathAir -> spec.path.minByOrNull { it.distanceKm }?.levels?.firstOrNull()?.h
            is SurfaceAir -> spec.heightM
            null -> null
        }
        if (h == null || !h.isFinite()) h = geo?.elevation
        return if (h != null && h.isFinite()) h else 0.0
    }

    /**
     * Why a spec's ground temperature is too far from the place's climate to be real air, or null. The reference is the place's
     * normal for the date when there is one (normalC: within [MAX_ANOMALY_C] of it), else the refraction server's climate envelope
     * for the place ([ClimateEnvelope]) at the spec's height: more than [MAX_ANOMALY_C] outside the month's whole range. With
     * neither, nothing is checked.
     */
    @JvmStatic
    @JvmOverloads
    fun anomalyProblem(spec: AtmosphereSpec?, date: LocalDate?, geo: GeoLocation?, normalC: Double = Double.NaN): String? {
        val here = groundTemperature(spec)
        if (!here.isFinite()) return null
        if (normalC.isFinite())
            return if (abs(here - normalC) > MAX_ANOMALY_C) "$here C against a normal of ${String.format(java.util.Locale.ROOT, "%.1f", normalC)} C" else null
        if (geo == null || date == null) return null
        val h = groundHeight(spec, geo)
        val range = ClimateEnvelope.at(geo.latitude, geo.longitude, date.monthValue, h) ?: return null
        return if (here < range[0] - MAX_ANOMALY_C || here > range[1] + MAX_ANOMALY_C)
            "$here C at ${h.toInt()} m, outside this place's climate (${range[0].toInt()}..${range[1].toInt()} C)" else null
    }

    private val checkedHorizons = java.util.WeakHashMap<JSONObject, MutableMap<String, String?>>()

    /**
     * Why a horizon set from the server (its JSON, as [HorizonClient] downloads and [RefractionRepository] stores it) cannot be
     * this location's terrain, or null if it can (see the header). Checked once per set and location.
     */
    @JvmStatic
    fun horizonProblem(set: JSONObject?, lat: Double, lon: Double): String? {
        if (set == null) return "not a horizon"
        val key = "$lat,$lon"
        synchronized(checkedHorizons) { checkedHorizons[set]?.let { if (it.containsKey(key)) return it[key] } }
        val r = checkHorizon(set, lat, lon)
        synchronized(checkedHorizons) { checkedHorizons.getOrPut(set) { HashMap() }[key] = r }
        return r
    }

    private fun num(o: JSONObject?, k: String): Double? = if (o == null || !o.has(k) || o.isNull(k)) null else o.optDouble(k, Double.NaN)

    private fun num2(a: JSONArray, i: Int): Double? = if (i >= a.length() || a.isNull(i)) null else a.optDouble(i, Double.NaN)

    private fun checkHorizon(set: JSONObject, lat: Double, lon: Double): String? {
        val points = set.optJSONArray("points")
        if (points == null || points.length() == 0) return "no points"
        val placeDeg = HORIZON_PLACE_KM / KM_PER_DEG
        // in an area: the location inside its box, the points too (the vantage grid, 100 m, may reach a little past it);
        // elsewhere the points around the location
        val area = set.optJSONObject("area")
        val b = area?.optJSONArray("bbox")
        val box = if (b != null && b.length() == 4 && (0 until 4).all { b.optDouble(it, Double.NaN).isFinite() }) DoubleArray(4) { b.getDouble(it) } else null
        fun inBox(la: Double, lo: Double): Boolean {
            val m = 1 / KM_PER_DEG
            val c = max(0.01, cos(Math.toRadians(la)))
            return la >= box!![0] - m && la <= box[2] + m && lo >= box[1] - m / c && lo <= box[3] + m / c
        }
        if (box != null && !inBox(lat, lon))
            return "area ${area?.optString("name", area.optString("id", ""))} [${box.joinToString(",")}] does not contain $lat, $lon"
        val eyeRange = doubleArrayOf(GROUND_M[0], GROUND_M[1] + OBSERVER_ABOVE_GROUND_M)
        for (i in 0 until points.length()) {
            val p = points.optJSONObject(i) ?: return "observer $i"
            val pLat = num(p, "lat")
            val pLon = num(p, "lon")
            if (!within(pLat, doubleArrayOf(-90.0, 90.0)) || !within(pLon, doubleArrayOf(-180.0, 180.0))) return "observer at $pLat, $pLon"
            if (box != null) {
                if (!inBox(pLat!!, pLon!!)) return "observer at $pLat, $pLon, outside its area"
            } else {
                val dy = pLat!! - lat
                val dx = (((pLon!! - lon) % 360 + 540) % 360 - 180) * cos(Math.toRadians(lat))
                if (hypot(dx, dy) > placeDeg) return "observer at $pLat, $pLon, more than $HORIZON_PLACE_KM km from $lat, $lon"
            }
            val height = num(p, "height")
            val ground = num(p, "ground") ?: height
            if (!within(ground, GROUND_M)) return "ground $ground m at $pLat, $pLon"
            if (!within(height, eyeRange)) return "observer $height m at $pLat, $pLon"
            for (event in arrayOf("sunrise", "sunset")) {
                if (!p.has(event) || p.isNull(event)) continue
                val entries = p.optJSONArray(event) ?: return "$event horizon at $pLat, $pLon"
                for (k in 0 until entries.length()) {
                    val e = entries.optJSONObject(k) ?: return "$event horizon entry $k"
                    val az = num(e, "azimuthDeg")
                    if (!within(az, doubleArrayOf(-360.0, 720.0))) return "$event azimuth $az"
                    // a measured horizon (an apparent angle) instead of terrain
                    if (!e.has("distanceKm") && e.has("elevationDeg")) {
                        val el = num(e, "elevationDeg")
                        if (!within(el, doubleArrayOf(-90.0, HORIZON_MAX_DEG))) return "$event horizon $el deg at $az"
                        continue
                    }
                    val alts = e.optJSONArray("alternates")
                    val floor = num(e, "floorM")
                    val why = terrainPointProblem(height!!, num(e, "distanceKm"), num(e, "heightM"))
                        ?: alts?.let { a -> (0 until a.length()).firstNotNullOfOrNull { j -> a.optJSONArray(j)?.let { terrainPointProblem(height, num2(it, 0), num2(it, 1)) } } }
                        ?: (if (e.has("floorM") && !e.isNull("floorM") && !within(floor, GROUND_M)) "floor $floor m" else null)
                    if (why != null) return "$event horizon at $az deg from $pLat, $pLon: $why"
                }
            }
        }
        // the composite moon horizon: each direction seen from its own spot's eye (observerM)
        val moon = set.optJSONObject("moon")
        for (event in arrayOf("moonrise", "moonset")) {
            val entries = moon?.optJSONArray(event) ?: continue
            for (k in 0 until entries.length()) {
                val e = entries.optJSONObject(k) ?: return "$event horizon entry $k"
                val eye = num(e, "observerM")
                if (!within(eye, eyeRange)) return "$event observer $eye m"
                val why = terrainPointProblem(eye!!, num(e, "distanceKm"), num(e, "heightM"))
                if (why != null) return "$event horizon at ${num(e, "azimuthDeg")} deg: $why"
            }
        }
        return null
    }

    /** Why a terrain point (km, m) seen from an eye at eyeM cannot be real ground, or null. */
    private fun terrainPointProblem(eyeM: Double, km: Double?, hM: Double?): String? {
        if (!within(km, doubleArrayOf(0.0, HORIZON_MAX_KM)) || km == 0.0) return "distance $km km"
        if (!within(hM, GROUND_M)) return "terrain $hM m"
        // geometric angle above the eye's level, the Earth's curvature included
        val th = km!! * 1000 / EARTH_M
        val r1 = EARTH_M + eyeM
        val r2 = EARTH_M + hM!!
        val deg = Math.toDegrees(atan2(r2 * cos(th) - r1, r2 * sin(th)))
        return if (deg > HORIZON_MAX_DEG) "$hM m at $km km is ${String.format(java.util.Locale.ROOT, "%.1f", deg)} deg up" else null
    }

    /** One value of an hourly series. */
    data class Interval(val start: Long, val end: Long, val value: Double)

    enum class SeriesKind { TEMPERATURE, DEW, PRESSURE }

    /** Hourly intervals with the impossible values and the lone spikes taken out. */
    @JvmStatic
    fun cleanSeries(series: List<Interval>, kind: SeriesKind, source: String = "forecast"): List<Interval> {
        val range = when (kind) {
            SeriesKind.PRESSURE -> doubleArrayOf(250.0, SEA_LEVEL_MB[1] + 30)
            SeriesKind.DEW -> doubleArrayOf(DEW_MIN_C, SURFACE_C[1])
            SeriesKind.TEMPERATURE -> SURFACE_C
        }
        val spike = if (kind == SeriesKind.PRESSURE) SPIKE_MB else SPIKE_C
        val ok = series.filter { within(it.value, range) }
        if (ok.size < series.size) report("$source: ${series.size - ok.size} ${kind.name.lowercase()} values out of range")
        return ok.filterIndexed { i, x ->
            val a = ok.getOrNull(i - 1)
            val b = ok.getOrNull(i + 1)
            if (a == null || b == null) return@filterIndexed true
            val da = x.value - a.value
            val db = x.value - b.value
            val lone = abs(da) > spike && abs(db) > spike && sign(da) == sign(db) && abs(a.value - b.value) < spike / 3
            if (lone) report("$source: a lone ${kind.name.lowercase()} spike (${a.value}, ${x.value}, ${b.value})")
            !lone
        }
    }

    /** An aerosol optical depth if it can be real, else null. */
    @JvmStatic
    fun plausibleAod(v: Double?): Double? = if (within(v, AOD)) v else null
}
