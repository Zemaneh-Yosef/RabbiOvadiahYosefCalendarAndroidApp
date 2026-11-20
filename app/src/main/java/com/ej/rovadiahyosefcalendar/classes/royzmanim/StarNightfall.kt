package com.ej.rovadiahyosefcalendar.classes.royzmanim

import java.time.LocalDate
import java.time.ZoneOffset

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Nightfall by the stars (port of the website's star-nightfall.js): the moment three stars of a given brightness can be
 * seen, from the twilight sky's brightness, light pollution and the air's clarity (haze). A. Taylor's halakhic_calc model
 * (calc_time.py; Taylor 2026, arXiv:2608.04064) written again from its formulas, with its twilight table read the right way
 * up and the search bisected to ~5 s, and with the haze taken from data. See the website file for the sources and checks.
 *
 * The haze delay (hazeDelay): nightfall at a day's haze minus nightfall at a reference haze (the place's average), the
 * twilight sky held at the reference haze. Added to a degree-based Tzet, hazier evenings come later and clearer ones earlier.
 */
object StarNightfall {
    private const val DEG = PI / 180
    private const val RAYLEIGH_K = 0.1066
    private const val OZONE_K = 0.031
    private val AOD_TO_K = 2.5 * kotlin.math.log10(kotlin.math.E)
    private const val NATURAL_SKY_CDM2 = 1.7e-4
    private const val MIN_ALT_DEG = 10.0
    private const val EARTH_R_M = 6371000.0
    private const val ARCSEC = DEG / 3600

    enum class StarClass { MEDIUM, SMALL }

    /** The Moon's topocentric right ascension / declination of date (deg) and phase angle (deg) at an instant, or null. */
    fun interface MoonSource { fun at(ms: Double): MoonPlace? }
    class MoonPlace(val raDeg: Double, val decDeg: Double, val phaseAngleDeg: Double)

    /**
     * aod: aerosol optical depth at 550 nm. skyAod: the one the twilight sky's brightness is figured with (default aod).
     * blpCdM2: light pollution, cd/m^2. together: three within nearDeg (default: true for small stars). elevationM:
     * observer's height (airmass only). marginMag: threshold margin (default 1.0, halakhic_calc's). moon: the Moon's light.
     */
    data class Options(
        val aod: Double,
        val skyAod: Double? = null,
        val blpCdM2: Double? = null,
        val stars: StarClass = StarClass.MEDIUM,
        val together: Boolean? = null,
        val nearDeg: Double = 10.0,
        val elevationM: Double = 0.0,
        val marginMag: Double? = null,
        val moon: MoonSource? = null,
    )

    private class Params(val k: Double, val kSky: Double, val blp: Double, val together: Boolean, val nearDeg: Double, val elevationM: Double, val marginMag: Double, val moon: MoonSource?)

    private val SUN = ROYSPACalculator()
    private val sunLock = Any()
    private var sunJd = Double.NaN
    private var sunCoords = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

    private fun jdOf(ms: Double) = ms / 86400000 + 2440587.5

    private fun sunAt(jd: Double): DoubleArray = synchronized(sunLock) {
        if (jd != sunJd) { sunCoords = SUN.apparentSolarCoords(jd); sunJd = jd }
        sunCoords
    }

    private fun lst(sun: DoubleArray, lonDeg: Double) = ((sun[3] + lonDeg) % 360 + 360) % 360

    private fun sunHorizontal(jd: Double, latDeg: Double, lonDeg: Double): DoubleArray {
        val sun = sunAt(jd)
        val aa = altAz(sun[0], sun[1], lst(sun, lonDeg), latDeg)
        return doubleArrayOf(aa[0] - 8.794 / 3600 / sun[4] * cos(aa[0] * DEG), aa[1])
    }

    private fun altAz(raDeg: Double, decDeg: Double, lstDeg: Double, latDeg: Double): DoubleArray {
        val H = (lstDeg - raDeg) * DEG
        val d = decDeg * DEG
        val p = latDeg * DEG
        val sinAlt = sin(p) * sin(d) + cos(p) * cos(d) * cos(H)
        val az = atan2(-cos(d) * sin(H), sin(d) * cos(p) - cos(d) * sin(p) * cos(H))
        return doubleArrayOf(asin(max(-1.0, min(1.0, sinAlt))) / DEG, ((az / DEG) % 360 + 360) % 360)
    }

    private fun apply(m: Array<DoubleArray>, v: DoubleArray) = DoubleArray(3) { i -> m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2] }
    private fun mul(a: Array<DoubleArray>, b: Array<DoubleArray>) = Array(3) { i -> DoubleArray(3) { j -> a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j] } }
    private fun rotX(a: Double) = arrayOf(doubleArrayOf(1.0, 0.0, 0.0), doubleArrayOf(0.0, cos(a), sin(a)), doubleArrayOf(0.0, -sin(a), cos(a)))
    private fun rotY(a: Double) = arrayOf(doubleArrayOf(cos(a), 0.0, -sin(a)), doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(sin(a), 0.0, cos(a)))
    private fun rotZ(a: Double) = arrayOf(doubleArrayOf(cos(a), sin(a), 0.0), doubleArrayOf(-sin(a), cos(a), 0.0), doubleArrayOf(0.0, 0.0, 1.0))

    /** Apparent places of date [ra, dec, mag] of J2000 catalogue stars (proper motion, IAU 2006 precession, nutation, aberration). */
    private fun apparentStars(stars: List<DoubleArray>, jd: Double): List<DoubleArray> {
        val t = (jd + DeltaT.deltaT(jd) / 86400 - 2451545) / 36525
        val years = 100 * t
        val zeta = (2.650545 + t * (2306.083227 + t * (0.2988499 + t * (0.01801828 + t * (-0.000005971 - t * 0.0000003173))))) * ARCSEC
        val z = (-2.650545 + t * (2306.077181 + t * (1.0927348 + t * (0.01826837 + t * (-0.000028596 - t * 0.0000002904))))) * ARCSEC
        val theta = t * (2004.191903 + t * (-0.4294934 + t * (-0.04182264 + t * (-0.000007089 - t * 0.0000001274)))) * ARCSEC
        val nut = Nutation.nutation(t)
        val dpsi = nut[0]
        val deps = nut[1]
        val sun = sunAt(jd)
        val eps = sun[2] * DEG
        val m = mul(mul(rotX(-eps), mul(rotZ(-dpsi * ARCSEC), rotX(eps - deps * ARCSEC))), mul(rotZ(-z), mul(rotY(theta), rotZ(-zeta))))
        val kappa = 20.49552 * ARCSEC
        val e = 0.016708634 - 0.000042037 * t
        val pi = (102.93735 + 1.71946 * t) * DEG
        val lon = (sun[5] + (20.4898 / sun[4] - dpsi) / 3600) * DEG
        val vx = kappa * (sin(lon) - e * sin(pi))
        val vy = -kappa * (cos(lon) - e * cos(pi))
        val v = doubleArrayOf(vx, vy * cos(eps), vy * sin(eps))
        return stars.map { s ->
            val ra = s[0]; val dec = s[1]; val mag = s[2]; val pmRa = s[3]; val pmDec = s[4]
            val d = (dec + pmDec * years / 3600) * DEG
            val a = (ra + pmRa * years / 3600 / cos(dec * DEG)) * DEG
            val u0 = apply(m, doubleArrayOf(cos(d) * cos(a), cos(d) * sin(a), sin(d)))
            val u = DoubleArray(3) { u0[it] + v[it] }
            val n = sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2])
            doubleArrayOf(((atan2(u[1], u[0]) / DEG) % 360 + 360) % 360, asin(u[2] / n) / DEG, mag)
        }
    }

    private fun axisPos(ax: DoubleArray, x: Double): Pair<Int, Double> {
        var i = 0
        while (i < ax.size - 2 && x > ax[i + 1]) i++
        return Pair(i, (x - ax[i]) / (ax[i + 1] - ax[i]))
    }

    /** Twilight sky luminance (cd/m^2): trilinear in log10, linearly extrapolated outside the table. */
    @JvmStatic
    fun twilightLuminance(altDeg: Double, dAzDeg: Double, sunAltDeg: Double): Double {
        val (i, fi) = axisPos(StarData.TW_ALT, altDeg)
        val (j, fj) = axisPos(StarData.TW_DAZ, dAzDeg)
        val (k, fk) = axisPos(StarData.TW_SUN, sunAltDeg)
        var v = 0.0
        for (di in 0..1) for (dj in 0..1) for (dk in 0..1) {
            val w = (if (di == 0) 1 - fi else fi) * (if (dj == 0) 1 - fj else fj) * (if (dk == 0) 1 - fk else fk)
            v += w * StarData.TW_LOG_B[((i + di) * 6 + (j + dj)) * 8 + (k + dk)]
        }
        return 10.0.pow(v)
    }

    /** Crumey (2014) threshold illuminance (lux) of a point source against luminance B (cd/m^2). */
    private fun brightLimit(B: Double): Double {
        val a1 = 6.112e-8; val a2 = -1.598e-7; val a3 = 1.167e-7; val a4 = 4.988e-4; val a5 = -3.014e-4
        val r = sqrt(max(0.0, a1 * sqrt(B) + a2 * B.pow(0.75) + a3 * B))
        return (r + a4 * B.pow(0.25) + a5 * sqrt(B)).pow(2)
    }

    private fun gasExtinction(elevationM: Double) = RAYLEIGH_K * (1 - 0.0065 * max(0.0, elevationM) / 288.15).pow(5.25588) + OZONE_K

    private fun airmass(altDeg: Double, elevationM: Double): Double {
        val s = cos(altDeg * DEG) / (1 + elevationM / EARTH_R_M)
        return 1 / sqrt(1 - s * s)
    }

    /** The aerosol optical depth that gives halakhic_calc's default extinction 0.1066 + 0.12 e^(-h/1500) at a height. */
    @JvmStatic
    @JvmOverloads
    fun defaultAod(elevationM: Double = 0.0): Double {
        val h = max(0.0, elevationM)
        return max(0.0, RAYLEIGH_K + 0.12 * exp(-h / 1500) - gasExtinction(h)) / AOD_TO_K
    }

    private fun separation(a: DoubleArray, b: DoubleArray): Double {
        val c = sin(a[0] * DEG) * sin(b[0] * DEG) + cos(a[0] * DEG) * cos(b[0] * DEG) * cos((a[1] - b[1]) * DEG)
        return acos(max(-1.0, min(1.0, c))) / DEG
    }

    private fun threeTogether(pts: List<DoubleArray>, d: Double): Boolean {
        for (i in pts.indices) {
            val nb = ArrayList<DoubleArray>()
            for (j in pts.indices) if (j != i && separation(pts[i], pts[j]) <= d) nb.add(pts[j])
            for (k in nb.indices) for (l in k + 1 until nb.size) if (separation(nb[k], nb[l]) <= d) return true
        }
        return false
    }

    /** A star above 10 degrees, with the faintest magnitude visible where it is (visible when mag <= limit). */
    class StarSeen(val alt: Double, val az: Double, val mag: Double, val limit: Double)

    private fun skyAt(ms: Double, latDeg: Double, lonDeg: Double, stars: List<DoubleArray>, p: Params): List<StarSeen> {
        val jd = jdOf(ms)
        val t = lst(sunAt(jd), lonDeg)
        val sh = sunHorizontal(jd, latDeg, lonDeg)
        val sunAlt = sh[0]
        val sunAz = sh[1]
        var moonPos: DoubleArray? = null
        var moonIstar = 0.0
        var moonI = 0.0
        val m = p.moon?.at(ms)
        if (m != null) {
            val mm = altAz(m.raDeg, m.decDeg, t, latDeg)
            if (mm[0] > 0) {
                val a = m.phaseAngleDeg
                val mag = 0.026 * a + 4e-9 * a.pow(4) - 12.73
                moonIstar = 10.0.pow(-0.4 * (mag + 13.99))
                moonI = moonIstar * 10.0.pow(-0.4 * p.k * airmass(mm[0], p.elevationM))
                moonPos = mm
            }
        }
        val out = ArrayList<StarSeen>()
        for (s in stars) {
            val ta = altAz(s[0], s[1], t, latDeg)
            val trueAlt = ta[0]
            val az = ta[1]
            val alt = trueAlt + 1 / tan((trueAlt + 7.31 / (trueAlt + 4.4)) * DEG) / 60
            if (alt <= MIN_ALT_DEG) continue
            var dAz = abs(az - sunAz)
            if (dAz > 180) dAz = 360 - dAz
            val X = airmass(alt, p.elevationM)
            var B = twilightLuminance(alt, dAz, sunAlt) * (1 - 10.0.pow(-0.4 * p.kSky * X)) + p.blp + NATURAL_SKY_CDM2
            if (moonPos != null) {
                val d = max(separation(moonPos, doubleArrayOf(alt, az)), 0.25)
                val ext = 10.0.pow(-0.4 * p.k * X)
                B += 3.183e-6 * 4.63e7 * moonI / 10.76 / (d * d)
                if (d <= 5) B += 3.183e-6 * 6.25e7 * moonIstar / 10.76 * (ext - ext * ext) / (d * d)
                val cosd = cos(d * DEG)
                val f = 10.0.pow(5.36) * (1.06 + cosd * cosd) + 10.0.pow(6.15 - d / 40) + 6.2e7 / (d * d)
                B += 3.183e-6 * f * (1 - ext) * moonI / 10.76
            }
            val I = brightLimit(B) * 10.0.pow(0.4 * p.k * X)
            out.add(StarSeen(alt, az, s[2], -2.5 * log10(I) - 13.99 - p.marginMag))
        }
        return out
    }

    private fun starsVisible(ms: Double, latDeg: Double, lonDeg: Double, stars: List<DoubleArray>, p: Params): Boolean {
        val seen = skyAt(ms, latDeg, lonDeg, stars, p).filter { it.mag <= it.limit }.map { doubleArrayOf(it.alt, it.az) }
        return if (p.together) threeTogether(seen, p.nearDeg) else seen.size >= 3
    }

    /** The stars of a class above 10 degrees at an instant, each with its limit (for display or checking). */
    @JvmStatic
    fun starsAt(ms: Double, latDeg: Double, lonDeg: Double, options: Options) = skyAt(ms, latDeg, lonDeg, starsFor(options.stars, ms), paramsOf(options))

    private fun paramsOf(o: Options): Params {
        val gas = gasExtinction(o.elevationM)
        return Params(
            k = gas + AOD_TO_K * max(0.0, o.aod),
            kSky = gas + AOD_TO_K * max(0.0, o.skyAod?.takeIf { it.isFinite() } ?: o.aod),
            blp = max(0.0, o.blpCdM2 ?: 0.0),
            together = o.together ?: (o.stars == StarClass.SMALL),
            nearDeg = o.nearDeg,
            elevationM = max(0.0, o.elevationM),
            marginMag = o.marginMag?.takeIf { it.isFinite() } ?: 1.0,
            moon = o.moon,
        )
    }

    /** The Sun's topocentric altitude and azimuth (deg) at an instant, unrefracted. */
    @JvmStatic
    fun sunAltAz(ms: Double, latDeg: Double, lonDeg: Double) = sunHorizontal(jdOf(ms), latDeg, lonDeg)

    private fun noonMs(date: LocalDate, lonDeg: Double) = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli() - lonDeg / 15 * 3600000

    /** Instant (epoch ms) the Sun's centre reaches altitude hDeg in the evening of a date, or NaN. */
    @JvmStatic
    fun eveningSunAt(date: LocalDate, latDeg: Double, lonDeg: Double, hDeg: Double): Double {
        val noon = noonMs(date, lonDeg)
        fun alt(ms: Double) = sunHorizontal(jdOf(ms), latDeg, lonDeg)[0]
        var lo = noon
        var hi = noon + 12 * 3600000
        if (alt(lo) < hDeg || alt(hi) > hDeg) return Double.NaN
        for (i in 0 until 40) { val m = (lo + hi) / 2; if (alt(m) > hDeg) lo = m else hi = m }
        return (lo + hi) / 2
    }

    /** Instant (epoch ms) the Sun's centre reaches altitude hDeg in the morning of a date, or NaN. */
    @JvmStatic
    fun morningSunAt(date: LocalDate, latDeg: Double, lonDeg: Double, hDeg: Double): Double {
        val noon = noonMs(date, lonDeg)
        fun alt(ms: Double) = sunHorizontal(jdOf(ms), latDeg, lonDeg)[0]
        var lo = noon - 12 * 3600000
        var hi = noon
        if (alt(lo) > hDeg || alt(hi) < hDeg) return Double.NaN
        for (i in 0 until 40) { val m = (lo + hi) / 2; if (alt(m) < hDeg) lo = m else hi = m }
        return (lo + hi) / 2
    }

    private val starCache = ROYSPACalculator.lru<String, List<DoubleArray>>(16)
    private val catalogue: List<DoubleArray> by lazy { (0 until StarData.STARS.size / 5).map { i -> DoubleArray(5) { StarData.STARS[5 * i + it] } } }

    private fun starsFor(cls: StarClass, ms: Double): List<DoubleArray> {
        val day = floor(ms / 86400000).toLong()
        val key = "$cls$day"
        synchronized(starCache) { starCache[key] }?.let { return it }
        val lo = if (cls == StarClass.SMALL) 3.0 else 2.0
        val hi = if (cls == StarClass.SMALL) 4.0 else 3.0
        val s = apparentStars(catalogue.filter { it[2] in lo..hi }, jdOf((day + 0.5) * 86400000))
        synchronized(starCache) { starCache[key] = s }
        return s
    }

    /** ms: the moment three stars are visible; if not before the Sun is 18 degrees down, that moment (reached false). */
    data class Nightfall(val ms: Double, val reached: Boolean)

    /** Nightfall by the stars on the evening of a date (ms NaN if the Sun doesn't set). */
    @JvmStatic
    fun starNightfall(date: LocalDate, latDeg: Double, lonDeg: Double, options: Options): Nightfall {
        val p = paramsOf(options)
        val start = eveningSunAt(date, latDeg, lonDeg, -0.833)
        val end = eveningSunAt(date, latDeg, lonDeg, -18.0)
        if (!start.isFinite()) return Nightfall(Double.NaN, false)
        val stars = starsFor(options.stars, start)
        val stop = if (end.isFinite()) end else start + 4 * 3600000
        val step = 120000.0
        var prev = start
        var t = start
        while (t <= stop) {
            if (starsVisible(t, latDeg, lonDeg, stars, p)) {
                var lo = prev
                var hi = t
                while (hi - lo > 5000) { val m = (lo + hi) / 2; if (starsVisible(m, latDeg, lonDeg, stars, p)) hi = m else lo = m }
                return Nightfall(hi, true)
            }
            prev = t
            t += step
        }
        return Nightfall(stop, false)
    }

    /**
     * none: equal hazes or no sunset (ms 0). modeled: three stars seen under both hazes. at-least: not under the day's haze
     * before the Sun is 18 degrees down (ms is the least the delay can be). at-most: not under the reference haze. unresolved: neither.
     */
    enum class HazeDelayStatus { NONE, MODELED, AT_LEAST, AT_MOST, UNRESOLVED }
    data class HazeDelay(val ms: Double, val status: HazeDelayStatus)

    /** Nightfall at the day's haze minus at a reference haze (ms; positive = later), the sky held at the reference haze. */
    @JvmStatic
    fun hazeDelay(date: LocalDate, latDeg: Double, lonDeg: Double, options: Options, aodRef: Double): HazeDelay {
        if (!(options.aod.isFinite() && aodRef.isFinite()) || options.aod == aodRef) return HazeDelay(0.0, HazeDelayStatus.NONE)
        val a = starNightfall(date, latDeg, lonDeg, options.copy(skyAod = aodRef))
        val b = starNightfall(date, latDeg, lonDeg, options.copy(aod = aodRef, skyAod = aodRef))
        val d = a.ms - b.ms
        if (!d.isFinite()) return HazeDelay(0.0, HazeDelayStatus.NONE)
        if (a.reached && b.reached) return HazeDelay(d, HazeDelayStatus.MODELED)
        if (b.reached) return HazeDelay(d, HazeDelayStatus.AT_LEAST)
        if (a.reached) return HazeDelay(d, HazeDelayStatus.AT_MOST)
        return HazeDelay(Double.NaN, HazeDelayStatus.UNRESOLVED)
    }

    // ------------------------------------------------------------------------------------------------
    // Haze data for a place (filled by HazeLoader / OpenMeteoHaze)
    // ------------------------------------------------------------------------------------------------

    /** Hourly aerosol optical depth (550 nm) at lat / lon: aod[i] at unix second t0 + i * step (NaN = gap). */
    class HazeSeries(val lat: Double, val lon: Double, val t0: Double, val step: Double, val aod: DoubleArray) {
        @Volatile internal var alt: DoubleArray? = null
        internal val normals = HashMap<String, MonthlyHaze?>()
    }

    /**
     * normals: CAMS's archive, from which the average at any moment is taken. forecast: yesterday to ~5 days ahead, null when
     * none is fresh. blpCdM2: light pollution, null if unknown. aodScale: the server's AERONET calibration of CAMS (1 = none).
     */
    data class HazeData(
        val normals: HazeSeries,
        val forecast: HazeSeries?,
        val blpCdM2: Double?,
        val forecastFetchedAt: Long?,
        val normalsRange: Pair<String, String>,
        val source: String,
        val forecastSource: String? = null,
        val aodScale: Double? = null,
    )

    /** The Sun's centre depressionDeg below the horizon (unrefracted), before sunrise (rising) or after sunset. */
    data class HazeMoment(val depressionDeg: Double, val rising: Boolean)

    /** aod: the haze at the moment, aodRef: the place's average then, forecast: whether aod is a forecast's. */
    data class HazeAt(val aod: Double, val aodRef: Double, val forecast: Boolean)

    internal class MonthlyHaze(val monthly: DoubleArray, val reference: Double)

    /** A series' value at an instant (linear between hours), NaN outside it or at a gap. */
    @JvmStatic
    fun hazeSeriesAt(s: HazeSeries, ms: Double): Double {
        val x = (ms / 1000 - s.t0) / s.step
        if (!(x >= 0 && x <= s.aod.size - 1)) return Double.NaN
        val i = min(floor(x).toInt(), s.aod.size - 2)
        val f = x - i
        val a = s.aod[i]
        val b = s.aod[i + 1]
        if (a.isNaN() || b.isNaN()) return if (f < 0.5) a else b
        return a + (b - a) * f
    }

    private fun momentOf(date: LocalDate, lat: Double, lon: Double, m: HazeMoment) =
        if (m.rising) morningSunAt(date, lat, lon, -m.depressionDeg) else eveningSunAt(date, lat, lon, -m.depressionDeg)

    private val MID_MONTH_DOY = doubleArrayOf(15.5, 45.0, 74.5, 105.0, 135.5, 166.0, 196.5, 227.5, 258.0, 288.5, 319.0, 349.5)

    /** Average haze per month at a moment, and the mean of the 12; null with fewer than 9 months with 10 days. */
    private fun normalsAt(s: HazeSeries, m: HazeMoment): MonthlyHaze? {
        val key = "${m.depressionDeg}|${if (m.rising) 'r' else 's'}"
        synchronized(s) { if (s.normals.containsKey(key)) return s.normals[key] }
        val n = s.aod.size
        val alt = s.alt ?: DoubleArray(n) { i -> sunHorizontal(jdOf((s.t0 + i * s.step) * 1000), s.lat, s.lon)[0] }.also { s.alt = it }
        val h = -m.depressionDeg
        val sum = DoubleArray(12)
        val cnt = IntArray(12)
        val noonOffset = -s.lon / 15 * 3600
        var day = floor(s.t0 / 86400) * 86400
        while (day <= s.t0 + (n - 1) * s.step) {
            val noon = day + 43200 + noonOffset
            val from = if (m.rising) noon - 43200 else noon
            val to = from + 43200
            val i0 = max(0, ceil((from - s.t0) / s.step).toInt())
            val i1 = min(n - 1, floor((to - s.t0) / s.step).toInt())
            for (i in i0 until i1) {
                val a = alt[i]
                val b = alt[i + 1]
                if (if (m.rising) (a < h && b >= h) else (a > h && b <= h)) {
                    val f = (h - a) / (b - a)
                    val va = s.aod[i]
                    val vb = s.aod[i + 1]
                    val v = if (va.isNaN() || vb.isNaN()) (if (f < 0.5) va else vb) else va + (vb - va) * f
                    if (AtmosphereGuard.plausibleAod(v) != null) {
                        val mo = java.time.Instant.ofEpochSecond(day.toLong()).atZone(ZoneOffset.UTC).monthValue - 1
                        sum[mo] += v; cnt[mo]++
                    }
                    break
                }
            }
            day += 86400
        }
        val have = cnt.count { it >= 10 }
        var res: MonthlyHaze? = null
        if (have >= 9) {
            val raw = DoubleArray(12) { if (cnt[it] >= 10) sum[it] / cnt[it] else Double.NaN }
            val mean = raw.filter { it.isFinite() }.sum() / have
            val monthly = DoubleArray(12) { if (raw[it].isFinite()) raw[it] else mean }
            res = MonthlyHaze(monthly, monthly.sum() / 12)
        }
        synchronized(s) { s.normals[key] = res }
        return res
    }

    /**
     * The aerosol optical depth at a date's moment: the forecast's when it covers that moment, else the month's average at that
     * moment, plus the place's average then. null where the Sun doesn't get that low for most of the year.
     */
    @JvmStatic
    fun hazeOn(haze: HazeData?, date: LocalDate, moment: HazeMoment): HazeAt? {
        val s = haze?.normals ?: return null
        if (s.aod.isEmpty()) return null
        val nm = normalsAt(s, moment) ?: return null
        var scale = 1.0
        val sc = haze.aodScale
        if (sc != null) {
            if (sc.isFinite() && sc >= 0.1 && sc <= 10) scale = sc else AtmosphereGuard.report("haze calibration factor $sc")
        }
        val aodRef = nm.reference * scale
        if (aodRef.isFinite() && aodRef > AtmosphereGuard.AOD[1]) { AtmosphereGuard.report("haze average $aodRef"); return null }
        val t = momentOf(date, s.lat, s.lon, moment)
        val fc = haze.forecast
        val f = if (fc != null && t.isFinite()) hazeSeriesAt(fc, t) else Double.NaN
        if (f.isFinite() && f >= 0 && AtmosphereGuard.plausibleAod(f) == null) AtmosphereGuard.report("haze forecast $f")
        if (AtmosphereGuard.plausibleAod(f) != null) return HazeAt(if (haze.forecastSource == "gefs") f else f * scale, aodRef, true)
        val doy = date.dayOfYear.toDouble()
        val mo = nm.monthly
        val d = if (doy < MID_MONTH_DOY[0]) doy + 365 else doy
        var aod = mo[0]
        for (i in 0 until 12) {
            val a = MID_MONTH_DOY[i]
            val b = if (i < 11) MID_MONTH_DOY[i + 1] else MID_MONTH_DOY[0] + 365
            if (d in a..b) { aod = mo[i] + (mo[(i + 1) % 12] - mo[i]) * (d - a) / (b - a); break }
        }
        return HazeAt(aod * scale, aodRef, false)
    }

}
