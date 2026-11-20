package com.ej.rovadiahyosefcalendar.classes.netz

import kotlin.math.tan

/**
 * The sky's colours by how far the Sun is below the horizon (the website's /horizon-3d stops): the colour high up, the colour
 * at the horizon, and how much to darken the ground (0 = day, 0.84 = night).
 */
object SkyColors {
    class Sky(val zenith: Int, val horizon: Int, val dark: Float)

    /** depression (deg below the horizon), zenith, horizon, ground darkening */
    private val STOPS = arrayOf(
        Stop(-6.0, 0x6fa3dc, 0xe3edf7, 0f),
        Stop(0.0, 0x5b88c2, 0xf3c993, 0.05f),
        Stop(3.0, 0x3f5f92, 0xe7955f, 0.2f),
        Stop(6.0, 0x263a63, 0xa8677a, 0.4f),
        Stop(9.0, 0x16223f, 0x4c4466, 0.58f),
        Stop(12.0, 0x0c1427, 0x25283f, 0.7f),
        Stop(15.0, 0x070b18, 0x11141f, 0.78f),
        Stop(18.0, 0x04060c, 0x06080f, 0.84f),
    )

    private class Stop(val depression: Double, val zenith: Int, val horizon: Int, val dark: Float)

    /** The sky with the Sun's centre depressionDeg below the horizon (negative: above it), as opaque ARGB. */
    @JvmStatic
    fun at(depressionDeg: Double): Sky {
        val d = depressionDeg.coerceIn(STOPS.first().depression, STOPS.last().depression)
        var i = 0
        while (i < STOPS.size - 2 && d > STOPS[i + 1].depression) i++
        val a = STOPS[i]
        val b = STOPS[i + 1]
        val f = ((d - a.depression) / (b.depression - a.depression)).toFloat()
        return Sky(mix(a.zenith, b.zenith, f), mix(a.horizon, b.horizon, f), a.dark + (b.dark - a.dark) * f)
    }

    private fun mix(a: Int, b: Int, f: Float): Int {
        fun ch(shift: Int): Int {
            val x = (a shr shift) and 0xff
            val y = (b shr shift) and 0xff
            return Math.round(x + (y - x) * f) shl shift
        }
        return (0xff shl 24) or ch(16) or ch(8) or ch(0)
    }

    /** Refraction (deg) to add to a true altitude (deg) for the apparent one (Saemundsson), as the website draws the Sun. */
    @JvmStatic
    fun refractionTrue(altDeg: Double): Double = 1.02 / tan(Math.toRadians(altDeg + 10.3 / (altDeg + 5.11))) / 60

    /** The Sun's apparent altitude from its true one; below -1.5 deg it is left as is (out of sight anyway). */
    @JvmStatic
    fun apparentAltitude(trueAltDeg: Double): Double =
        if (trueAltDeg > -1.5) trueAltDeg + refractionTrue(maxOf(trueAltDeg, -1.0)) else trueAltDeg
}
