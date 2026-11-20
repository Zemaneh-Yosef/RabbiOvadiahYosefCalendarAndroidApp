package com.ej.rovadiahyosefcalendar.classes.royzmanim

import android.util.Log
import java.util.Collections
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign

/**
 * Guardrails for the weather and climate data the zmanim are computed from (port of the website's atmosphere-guard.js).
 * A value that cannot be real air is refused, and the next source in line is used instead (chainProviders), or, when
 * none is left, the calculator's standard model. The limits are the extremes the Earth has recorded (WMO), with a
 * margin, so real weather - however rare - is never refused; they catch values no air can have, not unusual ones.
 * See the website file for the sources of each limit.
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

    private const val RD = 287.05
    private const val G = 9.80665
    private const val TAG = "AtmosphereGuard"

    private val reported: MutableSet<String> = Collections.synchronizedSet(HashSet())

    /** Each refusal is reported once, so a bad source shows up in the log without flooding it. */
    @JvmStatic
    fun report(what: String) {
        if (!reported.add(what)) return
        if (reported.size > 200) reported.clear()
        Log.w(TAG, "weather data refused: $what")
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
