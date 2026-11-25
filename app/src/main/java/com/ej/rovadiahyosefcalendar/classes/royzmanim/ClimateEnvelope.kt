package com.ej.rovadiahyosefcalendar.classes.royzmanim

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The refraction server's climate envelope (/v1/climate-envelope; port of the website's climate-envelope.js): for a 5 degree cell
 * and each month, the lowest and highest near-surface air in NOAA's 1991-2020 long-term means, at sea level. [AtmosphereGuard]'s
 * anomaly check uses it as the reference where the place's monthly normals did not download, so a broken forecast (-50 C in a
 * New York July) is still refused. [RefractionRepository] fetches and stores it; this only holds what was loaded.
 */
object ClimateEnvelope {
    private const val RES = 5

    class Envelope(val south: Double, val west: Double, val minC: DoubleArray, val maxC: DoubleArray, val lapseKPerM: Double, val json: JSONObject)

    private val loaded = ConcurrentHashMap<String, Envelope>()

    /** The id of the cell holding a place (the server's 5 degree grid). */
    @JvmStatic
    fun cellId(lat: Double, lon: Double): String {
        val south = -90 + RES * min(180 / RES - 1, floor((lat + 90) / RES).toInt())
        val n = 360 / RES
        val west = -180 + RES * ((floor((lon + 180) / RES).toInt() % n + n) % n)
        return "$south,$west"
    }

    /** The envelope in a server answer, or null when it isn't a usable one. */
    @JvmStatic
    fun parse(j: JSONObject?): Envelope? {
        if (j == null) return null
        val cell = j.optJSONObject("cell") ?: return null
        val sea = j.optJSONObject("seaLevelC") ?: return null
        fun arr(k: String): DoubleArray? {
            val a = sea.optJSONArray(k) ?: return null
            if (a.length() != 12) return null
            val d = DoubleArray(12) { a.optDouble(it, Double.NaN) }
            return if (d.all { it.isFinite() }) d else null
        }
        val south = cell.optDouble("south", Double.NaN)
        val west = cell.optDouble("west", Double.NaN)
        val lapse = j.optDouble("lapseKPerM", Double.NaN)
        val lo = arr("min") ?: return null
        val hi = arr("max") ?: return null
        if (!south.isFinite() || !west.isFinite() || cell.optInt("res") != RES || !lapse.isFinite()) return null
        if ((0 until 12).any { lo[it] > hi[it] }) return null
        return Envelope(south, west, lo, hi, lapse, j)
    }

    /** Hold an envelope for its cell. */
    @JvmStatic
    fun set(e: Envelope) { loaded["${e.south.toInt()},${e.west.toInt()}"] = e }

    /** Whether the envelope for a place's cell is held. */
    @JvmStatic
    fun has(lat: Double, lon: Double) = loaded.containsKey(cellId(lat, lon))

    /**
     * (lo, hi) (C) at height h (m) for a place and month (1..12), widened by the neighbouring months (a date near a month's edge),
     * or null when its cell's envelope isn't loaded.
     */
    @JvmStatic
    fun at(lat: Double, lon: Double, month: Int, h: Double): DoubleArray? {
        if (!lat.isFinite() || !lon.isFinite()) return null
        val e = loaded[cellId(lat, lon)] ?: return null
        var lo = Double.POSITIVE_INFINITY
        var hi = Double.NEGATIVE_INFINITY
        for (m in intArrayOf(month - 2, month - 1, month)) {// 0-based: previous, this, next
            val k = (m + 12) % 12
            lo = min(lo, e.minC[k])
            hi = max(hi, e.maxC[k])
        }
        val drop = e.lapseKPerM * (if (h.isFinite()) h else 0.0)
        return doubleArrayOf(lo - drop, hi - drop)
    }
}
