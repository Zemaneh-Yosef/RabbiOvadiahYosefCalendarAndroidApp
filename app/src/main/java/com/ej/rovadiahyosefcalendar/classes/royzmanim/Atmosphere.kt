package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * The refraction physics of the website's royzmanim-spa-corrections.js: Sweer's horizon (no data), the ray tracer through
 * van der Werf's modified US1976 atmosphere (one temperature), and the path atmosphere (weather-model profiles along the
 * Sun's azimuth). See ROYSPACalculator for what each changes, and the website file's header for the sources.
 */

/** Sunrise or sunset: the air is asked for separately for each. */
enum class SolarEvent { SUNRISE, SUNSET;
    val isRising get() = this == SUNRISE
}

/** The air for a sunrise / sunset: one temperature ([SurfaceAir]) or weather-model profiles along the path ([PathAir]). */
sealed interface AtmosphereSpec { val key: String? }

/**
 * Air at heightM metres above sea level (default: the observer's height); pressureMb defaults to the standard pressure
 * for that height. dewPointC / relativeHumidity (%): the air's moisture, used only with humidity on.
 * source: where it came from ("forecast" or "normals"), for display only.
 */
data class SurfaceAir(
    val temperatureC: Double,
    val pressureMb: Double? = null,
    val heightM: Double? = null,
    val dewPointC: Double? = null,
    val relativeHumidity: Double? = null,
    val source: String? = null,
    override val key: String? = null,
) : AtmosphereSpec

/** A level of a weather-model column: h (m above sea level), t (C), p (hPa), td dew point (C) / rh (%) when known. */
data class ProfileLevel(val h: Double, val t: Double, val p: Double, val td: Double? = null, val rh: Double? = null)

/** A weather-model column at distanceKm along the Sun's azimuth; water: over water, with its surface temperature skinC. */
data class PathProfile(val distanceKm: Double, val water: Boolean = false, val skinC: Double? = null, val levels: List<ProfileLevel>)

/**
 * Air that varies along the path. The built medium and the guardrail check are kept on the object, as the website keys
 * them by the profile array's identity.
 */
class PathAir(val path: List<PathProfile>, override val key: String? = null, val source: String? = null) : AtmosphereSpec {
    private val id = NEXT_ID.incrementAndGet()
    internal val identityKey get() = key ?: "path#$id"
    private var problemChecked = false
    private var problem: String? = null
    internal val media = HashMap<String, Medium>()

    @Synchronized
    internal fun checkedProblem(): String? {
        if (!problemChecked) {
            problem = AtmosphereGuard.checkPath(path)
            problemChecked = true
        }
        return problem
    }

    private companion object { val NEXT_ID = AtomicInteger() }
}

/** The actual air at sunrise / sunset for a date; null = no data for that event (the next provider or the default model). */
fun interface AtmosphereProvider {
    fun airFor(date: LocalDate, event: SolarEvent, geo: GeoLocation): AtmosphereSpec?
}

/** A provider built from monthly normals (chainProviders checks the other sources' air against it). */
interface ClimatologyProvider : AtmosphereProvider

/** What identifies a provider's answer in the result cache: equal keys must mean equal air. */
internal fun airKey(spec: AtmosphereSpec?): String = when (spec) {
    null -> "std"
    is PathAir -> spec.identityKey
    is SurfaceAir -> spec.key ?: spec.copy(source = null).toString()
}

/** The common interface over the 1D (spherically symmetric) atmosphere and the path atmosphere. */
interface Medium {
    val key: String
    fun nm1(h: Double): Double
    fun dndh(h: Double): Double
    /** Total refraction (degrees) of the ray leaving ho at apparent altitude aDeg; NaN if it meets the surface. */
    fun refraction(ho: Double, aDeg: Double, R: Double, floorM: Double = 0.0): Double
    /** Apparent altitude (degrees, <= 0) of the lowest direction the Sun can be seen in. */
    fun seaHorizon(ho: Double, R: Double, floorM: Double = 0.0): Double
    /** Path media only: the ray's height at ground distance distM (see terrainElevation); null for 1D air. */
    fun heightAt(ho: Double, aDeg: Double, R: Double, distM: Double, floorM: Double = 0.0): Double? = null
    fun maxCurvature(zLo: Double, zHi: Double): Double? = null
    fun trapLimit(ho: Double, aDeg: Double, R: Double): Double? = null
    val hasHeightAt: Boolean get() = false
}

object Refraction {
    const val DEG = PI / 180

    // ------------------------------------------------------------------------------------------------
    // Sweer (1938), "The Path of a Ray of Light Tangent to the Surface of the Earth", J. Opt. Soc. Am. 28:327.
    // Used for the default (no provider) path.
    // ------------------------------------------------------------------------------------------------
    private const val SWEER_A = 0.000277
    private const val ISA_T0 = 288.15
    private const val ISA_LAPSE = 0.0065
    private const val ISA_EXP = 4.2559
    private val ISA_RHO11 = (216.65 / ISA_T0).pow(ISA_EXP)
    private const val ISA_H11 = 11000.0
    private const val ISA_SCALE_H = 6341.6
    private const val SWEER_TOP_M = 45000.0

    private fun isaDensityRatio(h: Double) =
        if (h <= ISA_H11) (1 - ISA_LAPSE * h / ISA_T0).pow(ISA_EXP) else ISA_RHO11 * exp(-(h - ISA_H11) / ISA_SCALE_H)

    private fun isaDensityGradient(h: Double) =
        if (h <= ISA_H11) ISA_EXP * ISA_LAPSE / ISA_T0 * (1 - ISA_LAPSE * h / ISA_T0).pow(ISA_EXP - 1) else isaDensityRatio(h) / ISA_SCALE_H

    private fun sweerDip(s: Double, g: Double, R: Double): Double {
        val rho0 = isaDensityRatio(g)
        val rho = isaDensityRatio(g + s)
        val mu = 1 + SWEER_A * rho
        val oneMinusX = (s * mu - R * SWEER_A * (rho0 - rho)) / ((R + s) * mu)
        return 2 * asin(sqrt(max(oneMinusX, 0.0) / 2))
    }

    private fun sweerDelta(s: Double, g: Double, R: Double, steps: Int = 400): Double {
        if (s <= 0) return 0.0
        val ds = sqrt(s) / steps
        var sum = 0.0
        for (i in 0 until steps) {
            val u = (i + 0.5) * ds
            val z = u * u
            val mu = 1 + SWEER_A * isaDensityRatio(g + z)
            sum += (SWEER_A * isaDensityGradient(g + z) / mu) / tan(sweerDip(z, g, R)) * 2 * u * ds
        }
        return sum
    }

    /** dip (degrees) and refractionRatio = (delta_inf + delta(h)) / delta_inf for an observer heightM above the surface. */
    @JvmStatic
    fun sweerHorizon(heightM: Double, groundElevM: Double, R: Double): DoubleArray {
        val h = max(0.0, heightM)
        if (h == 0.0) return doubleArrayOf(0.0, 1.0)
        val dInf = sweerDelta(SWEER_TOP_M, groundElevM, R, 800)
        return doubleArrayOf(sweerDip(h, groundElevM, R) * 180 / PI, (dInf + sweerDelta(h, groundElevM, R)) / dInf)
    }

    // ------------------------------------------------------------------------------------------------
    // General ray tracer (ChaiTables / van der Werf physics, without their curve fits)
    // ------------------------------------------------------------------------------------------------
    private const val G0 = 9.80665
    const val R_DRY = 287.053
    private val P_EXP = G0 / (R_DRY * ISA_LAPSE)
    private const val RAY_TOP_M = 90000.0
    private const val WAVELENGTH_UM = 0.574
    private const val SIGMA2 = 1 / (WAVELENGTH_UM * WAVELENGTH_UM)
    /** n - 1 = REFR_K (P - VAPOUR_BETA e) / T: Ciddor's dry air */
    val REFR_K = 1e-8 * (5792105 / (238.0185 - SIGMA2) + 167917 / (57.362 - SIGMA2)) * 288.15 / 1013.25
    private val REFR_KW = 1.022e-8 * (295.235 + 2.6422 * SIGMA2 - 0.03238 * SIGMA2 * SIGMA2 + 0.004028 * SIGMA2 * SIGMA2 * SIGMA2) * 293.15 / 13.33
    val VAPOUR_BETA = 1 - REFR_KW / REFR_K
    private const val VAPOUR_LIGHT = 1 - 18.016 / 28.964
    const val VAPOUR_SCALE_M = 2000.0
    private const val GEOPOTENTIAL_R = 6356766.0
    private val US76_UPPER = arrayOf(
        doubleArrayOf(20000.0, 216.65), doubleArrayOf(32000.0, 228.65), doubleArrayOf(47000.0, 270.65),
        doubleArrayOf(51000.0, 270.65), doubleArrayOf(71000.0, 214.65), doubleArrayOf(84852.0, 186.946)
    )
    private const val US76_TROPOPAUSE_K = 216.65

    /** Surface gravity (m/s^2) at a geodetic latitude, as REF2023. */
    @JvmStatic
    fun gravityAtLatitude(latitudeDeg: Double): Double {
        val s = sin(latitudeDeg * DEG)
        val s2 = sin(2 * latitudeDeg * DEG)
        return 9.780356 * (1 + 0.0052885 * s * s - 0.0000059 * s2 * s2)
    }

    /** Saturation vapour pressure over water (mb) at a temperature (C), Bolton (1980). */
    @JvmStatic
    fun saturationVapourMb(tC: Double) = 6.112 * exp(17.67 * tC / (tC + 243.5))

    /** Vapour pressure (mb) from a dew point or a relative humidity; NaN if neither. Never above saturation. */
    @JvmStatic
    fun vapourPressureMb(tC: Double, dewPointC: Double?, rhPercent: Double?): Double {
        val es = saturationVapourMb(tC)
        if (dewPointC != null && dewPointC.isFinite()) return min(saturationVapourMb(dewPointC), es)
        if (rhPercent != null && rhPercent.isFinite()) return min(max(rhPercent, 0.0), 100.0) / 100 * es
        return Double.NaN
    }

    /** Standard-atmosphere pressure (mb) at a height. */
    @JvmStatic
    fun standardPressure(heightM: Double) = 1013.25 * (1 - ISA_LAPSE * heightM / ISA_T0).pow(P_EXP)

    /** A spherically symmetric atmosphere: [both] writes N = n - 1 and dN/dh at height h. */
    class Atmosphere internal constructor(val T0: Double, val P0: Double, val key: String, private val bothFn: (Double, DoubleArray) -> Unit) {
        fun both(h: Double, out: DoubleArray) = bothFn(h, out)
        fun nm1(h: Double): Double { val o = DoubleArray(2); bothFn(h, o); return o[0] }
        fun dndh(h: Double): Double { val o = DoubleArray(2); bothFn(h, o); return o[1] }
    }

    /**
     * The refractivity profile for a surface temperature / pressure at heightM: standard lapse up to the US1976
     * tropopause temperature, the US1976 layers above, geopotential heights, Ciddor refractivity; vapourMb > 0 adds water
     * vapour falling off with a 2 km scale height (capped at saturation); latitudeDeg sets the surface gravity.
     */
    @JvmStatic
    @JvmOverloads
    fun createAtmosphere(temperatureC: Double, pressureMb: Double, heightM: Double, vapourMb: Double = 0.0, latitudeDeg: Double = Double.NaN): Atmosphere {
        val g = if (latitudeDeg.isFinite()) gravityAtLatitude(latitudeDeg) else G0
        val gR = g / R_DRY
        fun geo(h: Double) = GEOPOTENTIAL_R * h / (GEOPOTENTIAL_R + h)
        val Ts = temperatureC + 273.15
        val za = geo(heightM)
        val T0 = Ts + ISA_LAPSE * za
        fun upperAt(z: Double): Double {
            if (z <= US76_UPPER[0][0]) return US76_UPPER[0][1]
            for (i in 1 until US76_UPPER.size) {
                val z0 = US76_UPPER[i - 1][0]; val t0 = US76_UPPER[i - 1][1]
                val z1 = US76_UPPER[i][0]; val t1 = US76_UPPER[i][1]
                if (z <= z1) return t0 + (t1 - t0) * (z - z0) / (z1 - z0)
            }
            return US76_UPPER[US76_UPPER.size - 1][1]
        }
        val d = max(0.0, upperAt(za) - Ts)
        var ztp = (T0 - US76_TROPOPAUSE_K + d) / ISA_LAPSE
        run {
            var i = 0
            while (i < US76_UPPER.size && ztp > US76_UPPER[i][0]) {
                val zb = US76_UPPER[i][0]; val tb = US76_UPPER[i][1]
                val next = US76_UPPER.getOrNull(i + 1)
                val s = if (next != null) (next[1] - tb) / (next[0] - zb) else 0.0
                ztp = (T0 - tb + d + s * zb) / (ISA_LAPSE + s)
                if (next == null || ztp <= next[0]) break
                i++
            }
        }
        val lnP0 = ln(pressureMb)
        val lnPtp = lnP0 + g / (R_DRY * ISA_LAPSE) * ln((T0 - ISA_LAPSE * ztp) / Ts)
        // layers above the tropopause: base height, base T, lapse dT/dz, ln P at the base
        val nodes = ArrayList<DoubleArray>()
        nodes.add(doubleArrayOf(ztp, T0 - ISA_LAPSE * ztp))
        for (n in US76_UPPER) if (n[0] > ztp) nodes.add(doubleArrayOf(n[0], n[1] - d))
        val lz = DoubleArray(nodes.size); val lT = DoubleArray(nodes.size); val ls = DoubleArray(nodes.size); val llnP = DoubleArray(nodes.size)
        run {
            var lnP = lnPtp
            for (i in nodes.indices) {
                val z = nodes[i][0]; val T = nodes[i][1]
                val next = nodes.getOrNull(i + 1)
                val s = if (next != null) (next[1] - T) / (next[0] - z) else 0.0
                lz[i] = z; lT[i] = T; ls[i] = s; llnP[i] = lnP
                if (next != null) lnP += if (s == 0.0) -gR * (next[0] - z) / T else -gR / s * ln(next[1] / T)
            }
        }
        /** T, ln P (dry hydrostatics), dT/dz, d(ln P)/dz at geopotential height z */
        fun stateZ(z: Double, o: DoubleArray) {
            if (z <= ztp) {
                val T = T0 - ISA_LAPSE * z
                o[0] = T; o[1] = lnP0 + g / (R_DRY * ISA_LAPSE) * ln(T / Ts); o[2] = -ISA_LAPSE; o[3] = -gR / T
                return
            }
            var i = lz.size - 1
            while (lz[i] > z) i--
            val T = lT[i] + ls[i] * (z - lz[i])
            o[0] = T
            o[1] = if (ls[i] == 0.0) llnP[i] - gR * (z - lz[i]) / lT[i] else llnP[i] - gR / ls[i] * ln(T / lT[i])
            o[2] = ls[i]; o[3] = -gR / T
        }
        val s0 = DoubleArray(4)
        stateZ(0.0, s0)
        val P0 = exp(s0[1])
        val baseKey = String.format(java.util.Locale.ROOT, "%.4f|%.4f|g%.5f", T0, P0, g)
        if (!(vapourMb > 0)) {
            return Atmosphere(T0, P0, baseKey) { h, out ->
                val st = DoubleArray(4)
                val k = GEOPOTENTIAL_R / (GEOPOTENTIAL_R + h)
                stateZ(GEOPOTENTIAL_R * h / (GEOPOTENTIAL_R + h), st)
                val N = REFR_K * exp(st[1]) / st[0]
                out[0] = N; out[1] = N * (st[3] - st[2] / st[0]) * k * k
            }
        }
        /** e, de/dh at height h (dT/dh given), capped at saturation */
        fun vapour(h: Double, T: Double, dTdh: Double, o: DoubleArray) {
            val e = vapourMb * exp(-(h - heightM) / VAPOUR_SCALE_M)
            val tC = T - 273.15
            val es = saturationVapourMb(tC)
            if (es < e) { o[0] = es; o[1] = es * 17.67 * 243.5 / ((tC + 243.5) * (tC + 243.5)) * dTdh } else { o[0] = e; o[1] = -e / VAPOUR_SCALE_M }
        }
        fun light(h: Double): Double {
            val k = GEOPOTENTIAL_R / (GEOPOTENTIAL_R + h)
            val st = DoubleArray(4); stateZ(geo(h), st)
            val v = DoubleArray(2); vapour(h, st[0], st[2] * k * k, v)
            return gR / st[0] * VAPOUR_LIGHT * v[0] / exp(st[1]) * k * k
        }
        val vlStep = 20.0
        val vlLo = min(heightM, 0.0) - 1000
        val vlN = ceil((30000 - vlLo) / vlStep).toInt()
        val vlTab = DoubleArray(vlN + 1)
        run {
            var prev = light(vlLo)
            for (i in 1..vlN) {
                val cur = light(vlLo + i * vlStep)
                vlTab[i] = vlTab[i - 1] + (prev + cur) / 2 * vlStep
                prev = cur
            }
        }
        val vlLight0 = light(vlLo)
        fun vlAt(h: Double): Double {
            val x = (h - vlLo) / vlStep
            if (x <= 0) return x * vlStep * vlLight0
            if (x >= vlN) return vlTab[vlN]
            val i = floor(x).toInt()
            return vlTab[i] + (vlTab[i + 1] - vlTab[i]) * (x - i)
        }
        val vlA = vlAt(heightM)
        return Atmosphere(T0, P0, baseKey + String.format(java.util.Locale.ROOT, "|e%.4f@%.1f", vapourMb, heightM)) { h, out ->
            val k = GEOPOTENTIAL_R / (GEOPOTENTIAL_R + h)
            val st = DoubleArray(4); stateZ(geo(h), st)
            val T = st[0]
            val dT = st[2] * k * k
            val P = exp(st[1] + vlAt(h) - vlA)
            val v = DoubleArray(2); vapour(h, T, dT, v)
            val e = v[0]; val de = v[1]
            val dP = P * (st[3] * k * k + gR / T * VAPOUR_LIGHT * e / P * k * k)
            val N = REFR_K * (P - VAPOUR_BETA * e) / T
            out[0] = N; out[1] = REFR_K * (dP - VAPOUR_BETA * de) / T - N * dT / T
        }
    }

    /** Total refraction (degrees) through a 1D atmosphere out to its top; NaN if the ray meets the surface first. */
    @JvmStatic
    @JvmOverloads
    fun rayRefraction(atm: Atmosphere, ho: Double, aDeg: Double, R: Double, steps: Int = 600, floorM: Double = 0.0): Double {
        val a = aDeg * DEG
        val ro = R + ho
        val Rb = R + floorM
        val buf = DoubleArray(2)
        atm.both(ho, buf)
        val k = (1 + buf[0]) * ro * cos(a)
        fun nr(r: Double): Double { atm.both(r - R, buf); return (1 + buf[0]) * r }
        fun g(r: Double): Double {
            atm.both(r - R, buf)
            val n = 1 + buf[0]
            val q = n * r
            val d = q * q - k * k
            return if (d > 0) -buf[1] / n * k / sqrt(d) else 0.0
        }
        fun seg(rb: Double, re: Double, m: Int): Double {
            val du = sqrt(re - rb) / m
            var s = 0.0
            for (i in 0 until m) { val u = (i + 0.5) * du; s += g(rb + u * u) * 2 * u * du }
            return s
        }
        val total: Double
        if (a < 0) {
            if (nr(Rb) > k) return Double.NaN
            var lo = Rb
            var hi = ro
            for (i in 0 until 60) { val mid = (lo + hi) / 2; if (nr(mid) < k) lo = mid else hi = mid }
            total = 2 * seg(hi, ro, max(60, steps shr 2)) + seg(ro, R + RAY_TOP_M, steps)
        } else {
            total = seg(ro, R + RAY_TOP_M, steps)
        }
        return total / DEG
    }

    /** Apparent altitude (degrees, <= 0) of the sea horizon (or a level basin floor) for an observer at height ho. */
    @JvmStatic
    fun seaHorizonAltitude(atm: Atmosphere, ho: Double, R: Double, floorM: Double = 0.0): Double {
        if (ho <= floorM) return 0.0
        return -acos(min(1.0, (1 + atm.nm1(floorM)) * (R + floorM) / ((1 + atm.nm1(ho)) * (R + ho)))) / DEG
    }

    /** Terrestrial refraction (degrees) toward a target: the ray's curvature at the mean height times half the path. */
    @JvmStatic
    fun terrestrialRefraction(atm: Medium, h1: Double, h2: Double, pathM: Double) = -atm.dndh((h1 + h2) / 2) * pathM / 2 / DEG

    /** Geometric (unrefracted) elevation angle, degrees, of a point at surface distance distM and height targetM. */
    @JvmStatic
    fun geometricElevation(observerM: Double, targetM: Double, distM: Double, R: Double): Double {
        val phi = distM / R
        val rt = R + targetM
        return atan2(rt * cos(phi) - (R + observerM), rt * sin(phi)) / DEG
    }

    /** Height at distM of the ray traced through a refractivity field (RK4); -Infinity if it comes down before it. */
    internal fun rayHeightAt(field: (Double, Double, DoubleArray) -> Unit, ho: Double, aDeg: Double, R: Double, distM: Double, floorM: Double): Double {
        val F = DoubleArray(3)
        val D = DoubleArray(3)
        fun deriv(r: Double, th: Double, b: Double) {
            field(r - R, R * th, F)
            val cb = cos(b); val sb = sin(b)
            D[0] = sb; D[1] = cb / r; D[2] = cb / r + (F[1] * cb - (R / r) * F[2] * sb) / (1 + F[0])
        }
        val thEnd = distM / R
        val n = max(16, ceil(distM / 250).toInt())
        val s = distM / cos(aDeg * DEG) / n
        var r = R + ho
        var th = 0.0
        var b = aDeg * DEG
        for (i in 0 until 4 * n) {
            val r0 = r; val th0 = th
            deriv(r, th, b); val r1 = D[0]; val t1 = D[1]; val b1 = D[2]
            deriv(r + s / 2 * r1, th + s / 2 * t1, b + s / 2 * b1); val r2 = D[0]; val t2 = D[1]; val b2 = D[2]
            deriv(r + s / 2 * r2, th + s / 2 * t2, b + s / 2 * b2); val r3 = D[0]; val t3 = D[1]; val b3 = D[2]
            deriv(r + s * r3, th + s * t3, b + s * b3)
            r += s / 6 * (r1 + 2 * r2 + 2 * r3 + D[0])
            th += s / 6 * (t1 + 2 * t2 + 2 * t3 + D[1])
            b += s / 6 * (b1 + 2 * b2 + 2 * b3 + D[2])
            if (th >= thEnd) return r0 + (r - r0) * (thEnd - th0) / (th - th0) - R
            if (r - R < floorM) return Double.NEGATIVE_INFINITY
        }
        return Double.NEGATIVE_INFINITY
    }

    enum class ImageSearch { AUTO, SCAN, OFF }

    /**
     * Apparent elevation (degrees) of a terrain point: the ray from the observer that reaches it, traced through the air
     * (path media) or the one-curvature estimate (1D air). See the website file for the image search's choices.
     */
    @JvmStatic
    fun terrainElevation(atm: Medium, ho: Double, targetM: Double, distM: Double, R: Double, floorM: Double = 0.0, images: ImageSearch = ImageSearch.OFF): Double {
        val geo = geometricElevation(ho, targetM, distM, R)
        val chord = hypot(distM, (R + targetM) * cos(distM / R) - (R + ho))
        val est = geo + terrestrialRefraction(atm, ho, targetM, chord)
        if (!atm.hasHeightAt) return est
        val f = { a: Double -> atm.heightAt(ho, a, R, distM, floorM)!! - targetM }
        val slope = distM * DEG
        if (images == ImageSearch.SCAN) {
            val sag = distM * distM / (8 * R)
            val mc = atm.maxCurvature(min(ho, targetM) - sag - 100, max(ho, targetM) + 100)
            if (mc != null && mc > 1 / R) {
                val first = firstSolution(f, est, slope)
                return max(highestImage(f, first), first)
            }
        }
        val first = firstSolution(f, est, slope)
        if (images != ImageSearch.AUTO) return first
        val top = atm.trapLimit(ho, first, R) ?: return first
        return scanBetween(f, first, top)
    }

    private fun firstSolution(f: (Double) -> Double, est: Double, slope: Double): Double {
        val tried = ArrayList<DoubleArray>()
        val g = { a: Double -> val v = f(a); tried.add(doubleArrayOf(a, v)); v }
        val a = secantElevation(g, est, slope)
        if (a != null) return a
        tried.sortBy { it[0] }
        var lo = Double.NaN
        var hi = Double.NaN
        for (i in 0 until tried.size - 1) {
            if (tried[i][1] < 0 && !(tried[i + 1][1] < 0)) { lo = tried[i][0]; hi = tried[i + 1][0]; break }
        }
        if (lo.isNaN()) {
            val bottom = tried[0]
            val top = tried[tried.size - 1]
            if (bottom[1] < 0) {
                lo = top[0]
                var k = 1
                while (k <= 40 && hi.isNaN()) { if (!(f(lo + 0.05) < 0)) hi = lo + 0.05 else lo += 0.05; k++ }
            } else {
                hi = bottom[0]
                var k = 1
                while (k <= 40 && lo.isNaN()) { if (f(hi - 0.05) < 0) lo = hi - 0.05 else hi -= 0.05; k++ }
            }
            if (lo.isNaN() || hi.isNaN()) return est
        }
        while (hi - lo > 1e-5) { val m = (lo + hi) / 2; if (f(m) < 0) lo = m else hi = m }
        return hi
    }

    private fun secantElevation(f: (Double) -> Double, est: Double, slope: Double): Double? {
        var a0 = est
        var f0 = f(a0)
        var i = 0
        while (i < 20 && f0 == Double.NEGATIVE_INFINITY) { a0 += 0.05; f0 = f(a0); i++ }
        if (f0 == Double.NEGATIVE_INFINITY) return null
        var a1 = a0 - f0 / slope
        var f1 = f(a1)
        i = 0
        while (i < 8 && kotlin.math.abs(f1) > 0.01) {
            val d = f1 - f0
            val a2 = if (f1.isFinite() && d != 0.0) a1 - f1 * (a1 - a0) / d else a1 + 0.01
            a0 = a1; f0 = f1; a1 = a2; f1 = f(a1)
            i++
        }
        return if (f1.isFinite() && kotlin.math.abs(f1) < 1) a1 else null
    }

    private fun highestImage(f: (Double) -> Double, est: Double): Double {
        var start = est
        var k = 0
        while (k < 12 && !(f(start) < 0)) { start = est - 0.05 * 2.0.pow(k); k++ }
        if (!(f(start) < 0)) return est
        return scanUp(f, start)
    }

    private fun scanBetween(f: (Double) -> Double, lo: Double, hi: Double): Double {
        var below = Double.NaN
        var a = lo + 0.005
        while (a <= hi + 0.005) { if (f(a) < 0) below = a; a += 0.005 }
        if (below.isNaN()) return lo
        var l = below
        var h = below + 0.005
        while (h - l > 1e-5) { val m = (l + h) / 2; if (f(m) < 0) l = m else h = m }
        return h
    }

    private fun scanUp(f: (Double) -> Double, start: Double): Double {
        var below = start
        var clear = 0.0
        var a = start + 0.005
        while (clear < 0.15 && a < start + 3) {
            if (f(a) < 0) { below = a; clear = 0.0 } else clear += 0.005
            a += 0.005
        }
        var lo = below
        var hi = below + 0.005
        while (hi - lo > 1e-5) { val m = (lo + hi) / 2; if (f(m) < 0) lo = m else hi = m }
        return hi
    }

    // ------------------------------------------------------------------------------------------------
    // Path-varying atmosphere (temperature profiles at several distances along the Sun's azimuth)
    // ------------------------------------------------------------------------------------------------
    private const val SURFACE_Z0H = 1e-4
    private const val PATH_TOP_M = 60000.0
    private const val WAVE_CLEARANCE_M = 0.5
    private const val COL_Z1 = 3000.0
    private const val COL_D1 = 2.0
    private const val COL_D2 = 20.0
    private const val COL_ZEXACT = 3.0
    private val COL_N1 = (COL_Z1 / COL_D1).toInt()
    private val COL_N2 = ceil((PATH_TOP_M + 2000 - COL_Z1) / COL_D2).toInt()

    /** A column's lazily filled table of N and dN/dz, plus the exact model for what the table doesn't cover. */
    internal class ColumnTable(val exact: (Double, DoubleArray) -> Unit) {
        val size = COL_N1 + COL_N2 + 2
        val tN = DoubleArray(size)
        val tD = DoubleArray(size)
        val filled = BooleanArray(size)
        private val tmp = DoubleArray(2)
        fun fill(i: Int) {
            val z = if (i <= COL_N1) i * COL_D1 else COL_Z1 + (i - COL_N1) * COL_D2
            exact(z, tmp)
            tN[i] = tmp[0]; tD[i] = tmp[1]; filled[i] = true
        }
    }

    /** N and dN/dz at z written to out[o], out[o + 1] (the ray tracer's inner loop). */
    internal fun columnInto(t: ColumnTable, z: Double, out: DoubleArray, o: Int) {
        if (z < COL_ZEXACT) { val v = DoubleArray(2); t.exact(z, v); out[o] = v[0]; out[o + 1] = v[1]; return }
        val x = if (z < COL_Z1) z / COL_D1 else COL_N1 + (z - COL_Z1) / COL_D2
        val i = floor(x).toInt()
        if (i >= COL_N1 + COL_N2 + 1) { val v = DoubleArray(2); t.exact(z, v); out[o] = v[0]; out[o + 1] = v[1]; return }
        if (!t.filled[i]) t.fill(i)
        if (!t.filled[i + 1]) t.fill(i + 1)
        val f = x - i
        out[o] = t.tN[i] + (t.tN[i + 1] - t.tN[i]) * f
        out[o + 1] = t.tD[i] + (t.tD[i + 1] - t.tD[i]) * f
    }

    private class Pt(val z: Double, val T: Double, val lp: Double, var le: Double = Double.NaN)

    /** ln(vapour pressure) at every level, or null when no level carries humidity. */
    private fun vapourColumn(pts: List<Pt>, levels: List<ProfileLevel>): DoubleArray? {
        val le = DoubleArray(levels.size) { i ->
            val e = vapourPressureMb(pts[i].T - 273.15, levels[i].td, levels[i].rh)
            if (e > 0) ln(e) else Double.NaN
        }
        val known = le.indices.filter { le[it].isFinite() }
        if (known.isEmpty()) return null
        return DoubleArray(le.size) { i ->
            if (le[i].isFinite()) return@DoubleArray le[i]
            val lo = known.lastOrNull { it < i }
            val hi = known.firstOrNull { it > i }
            when {
                lo != null && hi != null -> le[lo] + (le[hi] - le[lo]) * (pts[i].z - pts[lo].z) / (pts[hi].z - pts[lo].z)
                lo != null -> le[lo] - (pts[i].z - pts[lo].z) / VAPOUR_SCALE_M
                else -> le[hi!!]
            }
        }
    }

    private fun buildColumn(prof: PathProfile, surfaceLayer: Boolean, humidity: Boolean, latitudeDeg: Double): ColumnTable {
        val sortedLevels = prof.levels.filter { it.h.isFinite() && it.t.isFinite() && it.p.isFinite() && it.p > 0 }.sortedBy { it.h }
        val levels = ArrayList<ProfileLevel>()
        for (l in sortedLevels) if (levels.isEmpty() || l.h > levels.last().h + 1e-6) levels.add(l)
        var pts: List<Pt> = levels.map { Pt(it.h, it.t + 273.15, ln(it.p)) }
        if (pts.isEmpty()) throw IllegalArgumentException("Path profile has no usable levels")
        val vap = if (humidity) vapourColumn(pts, levels) else null
        if (vap != null) pts.forEachIndexed { i, q -> q.le = vap[i] }
        val s = pts[0]
        val skin = prof.skinC
        if (surfaceLayer && prof.water && skin != null && skin.isFinite() && s.z > 0 && s.z <= 10) {
            val gammaD = G0 / 1004.7
            val Tsk = skin + 273.15
            val L = ln(1 + s.z / SURFACE_Z0H)
            val dTheta = s.T + gammaD * s.z - Tsk
            val low = doubleArrayOf(0.0, 0.002, 0.01, 0.03, 0.1, 0.3, 0.7, 1.2).filter { it < s.z * 0.95 }.map { z ->
                val T = Tsk + dTheta * ln(1 + z / SURFACE_Z0H) / L - gammaD * z
                Pt(z, T, s.lp + (s.z - z) * G0 / (R_DRY * (T + s.T) / 2), s.le)
            }
            pts = low + pts
        }
        val top = pts[pts.size - 1]
        fun eOf(q: Pt) = if (vap != null && q.le.isFinite()) exp(q.le) else 0.0
        val above = createAtmosphere(top.T - 273.15, exp(top.lp), top.z, eOf(top), latitudeDeg)
        val bottom = pts[0]
        val below = createAtmosphere(bottom.T - 273.15, exp(bottom.lp), bottom.z, eOf(bottom), latitudeDeg)
        val zs = DoubleArray(pts.size) { pts[it].z }
        val ptsArr = pts
        val exact: (Double, DoubleArray) -> Unit = { z, out ->
            when {
                z >= top.z -> above.both(z, out)
                z <= bottom.z -> below.both(z, out)
                else -> {
                    var lo = 0
                    var hi = zs.size - 1
                    while (hi - lo > 1) { val m = (lo + hi) shr 1; if (zs[m] <= z) lo = m else hi = m }
                    val a = ptsArr[lo]; val b = ptsArr[hi]
                    val dz = b.z - a.z
                    val f = (z - a.z) / dz
                    val dT = (b.T - a.T) / dz
                    val dlp = (b.lp - a.lp) / dz
                    val T = a.T + dT * f * dz
                    val P = exp(a.lp + dlp * f * dz)
                    if (vap == null) {
                        val N = REFR_K * P / T
                        out[0] = N; out[1] = N * (dlp - dT / T)
                    } else {
                        val dle = (b.le - a.le) / dz
                        val e = exp(a.le + dle * f * dz)
                        val N = REFR_K * (P - VAPOUR_BETA * e) / T
                        out[0] = N; out[1] = REFR_K * (P * dlp - VAPOUR_BETA * e * dle) / T - N * dT / T
                    }
                }
            }
        }
        return ColumnTable(exact)
    }

    private fun hashString(s: String): String {
        var h = 5381
        for (c in s) h = (h shl 5) + h + c.code
        return (h.toLong() and 0xffffffffL).toString(36)
    }

    /**
     * Atmosphere that varies along the path, from profiles at increasing distances (km) along the Sun's azimuth.
     * surfaceLayer: model the sea surface layer, wave crests and inferior mirages over water.
     */
    @JvmStatic
    fun createPathAtmosphere(profiles: List<PathProfile>, key: String?, surfaceLayer: Boolean = true, humidity: Boolean = false, latitudeDeg: Double = Double.NaN): Medium =
        PathMedium(profiles, key, surfaceLayer, humidity, latitudeDeg)

    private class PathMedium(profiles: List<PathProfile>, key: String?, surfaceLayer: Boolean, humidity: Boolean, latitudeDeg: Double) : Medium {
        private val sorted = profiles.sortedBy { it.distanceKm }
        private val ds = DoubleArray(sorted.size) { sorted[it].distanceKm * 1000 }
        private val tabs = sorted.map { buildColumn(it, surfaceLayer, humidity, latitudeDeg) }
        private val moist = humidity && sorted.any { p -> p.levels.any { (it.td?.isFinite() ?: false) || (it.rh?.isFinite() ?: false) } }
        private val F = DoubleArray(3)
        private val ab = DoubleArray(4)
        private val D = DoubleArray(3)
        private val water = BooleanArray(sorted.size) { surfaceLayer && sorted[it].water }
        private val anyWater = water.any { it }
        private val warmWater = surfaceLayer && sorted.any { p -> p.water && (p.skinC?.isFinite() ?: false) && p.levels.isNotEmpty() && p.skinC!! > p.levels[0].t }
        private val dLast = if (tabs.size == 1) Double.NEGATIVE_INFINITY else ds[ds.size - 1]
        private val climbSafeFrom = HashMap<Double, Double>()
        private val grids = HashMap<Double, Pair<DoubleArray, List<DoubleArray>>>()

        init {
            if (sorted.isEmpty()) throw IllegalArgumentException("No path profiles")
        }

        override val key: String = (key ?: ("path|" + hashString(sorted.toString()))) + (if (surfaceLayer) "|sl" else "") + (if (moist) "|h" else "") +
            (if (latitudeDeg.isFinite()) String.format(java.util.Locale.ROOT, "|lat%.2f", latitudeDeg) else "")

        private fun fieldAt(z: Double, d: Double) {
            if (tabs.size == 1 || d <= ds[0]) { columnInto(tabs[0], z, F, 0); F[2] = 0.0; return }
            val last = ds.size - 1
            if (d >= ds[last]) { columnInto(tabs[last], z, F, 0); F[2] = 0.0; return }
            var j = 0
            while (ds[j + 1] < d) j++
            val w = (d - ds[j]) / (ds[j + 1] - ds[j])
            columnInto(tabs[j], z, ab, 0); columnInto(tabs[j + 1], z, ab, 2)
            F[0] = ab[0] + (ab[2] - ab[0]) * w
            F[1] = ab[1] + (ab[3] - ab[1]) * w
            F[2] = (ab[2] - ab[0]) / (ds[j + 1] - ds[j])
        }

        private fun deriv(R: Double, r: Double, th: Double, b: Double) {
            fieldAt(r - R, R * th)
            val cb = cos(b); val sb = sin(b)
            D[0] = sb; D[1] = cb / r; D[2] = cb / r + (F[1] * cb - (R / r) * F[2] * sb) / (1 + F[0])
        }

        private fun safeHeight(R: Double): Double {
            climbSafeFrom[R]?.let { return it }
            val t = tabs[tabs.size - 1]
            val rMax = R + COL_Z1 + (COL_N2 + 1) * COL_D2
            var i = COL_N1 + COL_N2 + 1
            while (i >= 0) {
                if (!t.filled[i]) t.fill(i)
                if (!(1 + t.tN[i] + rMax * t.tD[i] >= 0)) break
                i--
            }
            val lowest = i + 1
            val z = if (lowest > COL_N1 + COL_N2 + 1) Double.POSITIVE_INFINITY
            else max(COL_ZEXACT, if (lowest <= COL_N1) lowest * COL_D1 else COL_Z1 + (lowest - COL_N1) * COL_D2)
            climbSafeFrom[R] = z
            return z
        }

        private fun clearance(d: Double): Double {
            var j = 0
            while (j < ds.size - 1 && kotlin.math.abs(ds[j + 1] - d) < kotlin.math.abs(ds[j] - d)) j++
            return if (water[j]) WAVE_CLEARANCE_M else 0.0
        }

        private fun run(ho: Double, aDeg: Double, R: Double, floorM: Double, probe: Boolean): Double {
            val zSafe = if (probe) safeHeight(R) else Double.POSITIVE_INFINITY
            var r = R + ho
            var th = 0.0
            var b = aDeg * DEG
            var i = 0
            while (i < 200000 && r - R <= PATH_TOP_M) {
                val s = min(2000.0, 80 + 0.3 * (r - R))
                deriv(R, r, th, b); val r1 = D[0]; val t1 = D[1]; val b1 = D[2]
                deriv(R, r + s / 2 * r1, th + s / 2 * t1, b + s / 2 * b1); val r2 = D[0]; val t2 = D[1]; val b2 = D[2]
                deriv(R, r + s / 2 * r2, th + s / 2 * t2, b + s / 2 * b2); val r3 = D[0]; val t3 = D[1]; val b3 = D[2]
                deriv(R, r + s * r3, th + s * t3, b + s * b3)
                r += s / 6 * (r1 + 2 * r2 + 2 * r3 + D[0])
                th += s / 6 * (t1 + 2 * t2 + 2 * t3 + D[1])
                b += s / 6 * (b1 + 2 * b2 + 2 * b3 + D[2])
                if (r - R < (if (floorM < 0) floorM else floorM + (if (anyWater) clearance(R * th) else 0.0))) return Double.NaN
                if (b > 0 && b < 1 && r - R >= zSafe && R * th >= dLast) return 0.0
                i++
            }
            return aDeg - (b - th) / DEG
        }

        private fun near(z: Double): DoubleArray { val o = DoubleArray(2); columnInto(tabs[0], z, o, 0); return o }

        @Synchronized override fun nm1(h: Double) = near(h)[0]
        @Synchronized override fun dndh(h: Double) = near(h)[1]
        @Synchronized override fun refraction(ho: Double, aDeg: Double, R: Double, floorM: Double) = run(ho, aDeg, R, floorM, false)
        override val hasHeightAt get() = true

        @Synchronized
        override fun heightAt(ho: Double, aDeg: Double, R: Double, distM: Double, floorM: Double): Double =
            rayHeightAt({ z, d, out -> fieldAt(z, d); out[0] = F[0]; out[1] = F[1]; out[2] = F[2] }, ho, aDeg, R, distM, floorM)

        @Synchronized
        override fun maxCurvature(zLo: Double, zHi: Double): Double {
            val v = DoubleArray(2)
            var best = Double.NEGATIVE_INFINITY
            for (t in tabs) {
                var z = max(0.0, zLo)
                while (z <= zHi) { columnInto(t, z, v, 0); best = max(best, -v[1] / (1 + v[0])); z += 2 }
            }
            return best
        }

        private fun invariantGrid(R: Double): Pair<DoubleArray, List<DoubleArray>> {
            grids[R]?.let { return it }
            val zl = ArrayList<Double>()
            var z = -440.0
            while (z < COL_Z1) { zl.add(z); z += COL_D1 }
            z = COL_Z1
            while (z <= COL_Z1 + COL_N2 * COL_D2) { zl.add(z); z += COL_D2 }
            val zs = zl.toDoubleArray()
            val v = DoubleArray(2)
            val g = Pair(zs, tabs.map { t -> DoubleArray(zs.size) { k -> columnInto(t, zs[k], v, 0); (1 + v[0]) * (R + zs[k]) } })
            grids[R] = g
            return g
        }

        private fun gridIndex(zs: DoubleArray, h: Double): Int {
            var lo = 0
            var hi = zs.size - 1
            if (h <= zs[0]) return 0
            if (h >= zs[hi]) return hi
            while (hi - lo > 1) { val k = (lo + hi) shr 1; if (zs[k] <= h) lo = k else hi = k }
            return lo
        }

        @Synchronized
        override fun trapLimit(ho: Double, aDeg: Double, R: Double): Double? {
            val (zs, ms) = invariantGrid(R)
            val cosA = cos(aDeg * DEG)
            var top: Double? = null
            for (m in ms) {
                val k0 = min(gridIndex(zs, ho), zs.size - 2)
                val m0 = m[k0] + (m[k0 + 1] - m[k0]) * (ho - zs[k0]) / (zs[k0 + 1] - zs[k0])
                val c = m0 * cosA
                var i = gridIndex(zs, ho)
                if (aDeg < 0) while (i > 0 && m[i - 1] >= c) i--
                var mMin = Double.POSITIVE_INFINITY
                for (k in i until m.size - 1) if (m[k + 1] < m[k] && m[k + 1] < mMin) mMin = m[k + 1]
                val cMax = if (aDeg < 0) m0 else c
                if (mMin < cMax) {
                    val lim = acos(min(1.0, mMin / m0)) / DEG
                    top = max(top ?: Double.NEGATIVE_INFINITY, lim)
                }
            }
            return top
        }

        @Synchronized
        override fun seaHorizon(ho: Double, R: Double, floorM: Double): Double {
            if (ho <= floorM) return 0.0
            val seen = HashMap<Double, Double>()
            fun tr(a: Double): Double = seen.getOrPut(a) { run(ho, a, R, floorM, true) }
            val n0 = 1 + near(floorM)[0]
            val no = 1 + near(ho)[0]
            val est = -acos(min(1.0, n0 * (R + floorM) / (no * (R + ho)))) / DEG
            val geo = acos((R + floorM) / (R + ho)) / DEG
            var lo = est - 0.005
            var hi = est + 0.005
            while (tr(hi).isNaN() && hi < 0.5) { lo = hi; hi += 0.1 }
            while (!tr(lo).isNaN() && lo > -2 * geo - 1) { hi = lo; lo -= 0.1 }
            if (!tr(lo).isNaN()) return lo
            while (hi - lo > 1e-5) { val m = (lo + hi) / 2; if (tr(m).isNaN()) lo = m else hi = m }
            val aHit = hi + 1e-7
            if (!warmWater || floorM < 0) return aHit
            fun trueAlt(a: Double): Double { val x = run(ho, a, R, 0.0, false); return if (x.isNaN()) Double.POSITIVE_INFINITY else a - x }
            val span = 0.6
            val n = 16
            val asArr = DoubleArray(n) { aHit + span * (it.toDouble() / (n - 1)).pow(2) }
            val vs = DoubleArray(n) { trueAlt(asArr[it]) }
            var k = 0
            for (i in 1 until n) if (vs[i] < vs[k]) k = i
            if (k == 0) return aHit
            var a = asArr[k - 1]
            var b = asArr[min(k + 1, n - 1)]
            val gr = (sqrt(5.0) - 1) / 2
            var c = b - gr * (b - a)
            var d = a + gr * (b - a)
            var fc = trueAlt(c)
            var fd = trueAlt(d)
            for (i in 0 until 14) {
                if (fc < fd) { b = d; d = c; fd = fc; c = b - gr * (b - a); fc = trueAlt(c) }
                else { a = c; c = d; fc = fd; d = a + gr * (b - a); fd = trueAlt(d) }
            }
            return (a + b) / 2
        }
    }

    /** The common interface over a 1D atmosphere. */
    @JvmStatic
    fun mediumOf(atm: Atmosphere): Medium = object : Medium {
        override val key = atm.key
        override fun nm1(h: Double) = atm.nm1(h)
        override fun dndh(h: Double) = atm.dndh(h)
        override fun refraction(ho: Double, aDeg: Double, R: Double, floorM: Double) = rayRefraction(atm, ho, aDeg, R, 600, floorM)
        override fun seaHorizon(ho: Double, R: Double, floorM: Double) = seaHorizonAltitude(atm, ho, R, floorM)
    }

    /** The medium for a spec (the standard atmosphere when null). */
    @JvmStatic
    fun atmosphereFromSpec(heightM: Double, spec: AtmosphereSpec?, surfaceLayer: Boolean = false, humidity: Boolean = false, latitudeDeg: Double = Double.NaN): Medium {
        return when (spec) {
            null -> mediumOf(createAtmosphere(ISA_T0 - 273.15, 1013.25, 0.0))
            is PathAir -> synchronized(spec) {
                val cacheKey = "$surfaceLayer|$humidity|$latitudeDeg"
                spec.media.getOrPut(cacheKey) { createPathAtmosphere(spec.path, spec.key, surfaceLayer, humidity, latitudeDeg) }
            }
            is SurfaceAir -> {
                val h = spec.heightM ?: heightM
                val e = if (humidity) vapourPressureMb(spec.temperatureC, spec.dewPointC, spec.relativeHumidity) else 0.0
                mediumOf(createAtmosphere(spec.temperatureC, spec.pressureMb ?: standardPressure(h), h, if (e.isFinite()) e else 0.0, latitudeDeg))
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Earth radius of curvature (WGS84)
    // ------------------------------------------------------------------------------------------------
    private const val WGS84_A_KM = 6378.137
    private const val WGS84_B_KM = 6356.7523142
    private const val WGS84_E2 = 1 - (WGS84_B_KM * WGS84_B_KM) / (WGS84_A_KM * WGS84_A_KM)

    /** Radius of curvature of the WGS84 ellipsoid (km) at a geodetic latitude, in the given azimuth (Euler's formula). */
    @JvmStatic
    @JvmOverloads
    fun earthRadiusAtLatitude(latitudeDeg: Double, azimuthDeg: Double = 90.0): Double {
        val sinLat = sin(latitudeDeg * DEG)
        val w = 1 - WGS84_E2 * sinLat * sinLat
        val N = WGS84_A_KM / sqrt(w)
        val M = WGS84_A_KM * (1 - WGS84_E2) / (w * sqrt(w))
        val az = azimuthDeg * DEG
        return 1 / (cos(az).pow(2) / M + sin(az).pow(2) / N)
    }

    // ------------------------------------------------------------------------------------------------
    // Climate helper (ChaiTables' sunrise = mean daily minimum, sunset = mean daily temperature)
    // ------------------------------------------------------------------------------------------------
    private val MID_MONTH_DOY = doubleArrayOf(15.5, 45.0, 74.5, 105.0, 135.5, 166.0, 196.5, 227.5, 258.0, 288.5, 319.0, 349.5)

    /** 12 monthly values (assigned to mid-month) interpolated to a day of year, wrapping around the year. */
    @JvmStatic
    fun interpolateMonthly(values: DoubleArray, doy: Double): Double {
        for (i in 0 until 12) {
            val a = MID_MONTH_DOY[i]
            val b = if (i < 11) MID_MONTH_DOY[i + 1] else MID_MONTH_DOY[0] + 365
            val d = if (doy < MID_MONTH_DOY[0]) doy + 365 else doy
            if (d in a..b) return values[i] + (values[(i + 1) % 12] - values[i]) * (d - a) / (b - a)
        }
        return values[0]
    }

    /** Monthly climate normals: sunrise uses the mean daily minimum, sunset the mean temperature (ChaiTables). */
    class MonthlyNormals(
        val minC: DoubleArray, val meanC: DoubleArray, val pressureMb: Double? = null, val heightM: Double? = null,
        val sunriseDewC: DoubleArray? = null, val sunsetDewC: DoubleArray? = null,
    )

    @JvmStatic
    fun monthlyClimate(normals: MonthlyNormals): ClimatologyProvider {
        require(normals.minC.size == 12 && normals.meanC.size == 12) { "minC and meanC need 12 monthly values" }
        return object : ClimatologyProvider {
            override fun airFor(date: LocalDate, event: SolarEvent, geo: GeoLocation): AtmosphereSpec {
                val doy = date.dayOfYear.toDouble()
                val dew = if (event.isRising) normals.sunriseDewC else normals.sunsetDewC
                return SurfaceAir(
                    temperatureC = interpolateMonthly(if (event.isRising) normals.minC else normals.meanC, doy),
                    pressureMb = normals.pressureMb, heightM = normals.heightM,
                    dewPointC = if (dew != null && dew.size == 12 && dew.all { it.isFinite() }) interpolateMonthly(dew, doy) else null,
                    source = "normals",
                )
            }
        }
    }

    /**
     * Combine providers: each is asked in turn and the first answer that can be real air is used. An answer the guardrails
     * refuse, or air more than 45 C from the place's normal when a climatology provider is in the chain, passes on.
     */
    @JvmStatic
    fun chainProviders(vararg providers: AtmosphereProvider?): AtmosphereProvider {
        val list = providers.filterNotNull()
        val climate = list.firstOrNull { it is ClimatologyProvider }
        return AtmosphereProvider { date, event, geo ->
            for (p in list) {
                val spec = p.airFor(date, event, geo) ?: continue
                val source = (spec as? SurfaceAir)?.source ?: (spec as? PathAir)?.source ?: "provider"
                val problem = AtmosphereGuard.specProblem(spec)
                if (problem != null) { AtmosphereGuard.report("$source for $date $event: $problem"); continue }
                if (climate != null && p !== climate) {
                    val normal = AtmosphereGuard.groundTemperature(climate.airFor(date, event, geo))
                    val here = AtmosphereGuard.groundTemperature(spec)
                    if (normal.isFinite() && here.isFinite() && kotlin.math.abs(here - normal) > AtmosphereGuard.MAX_ANOMALY_C) {
                        AtmosphereGuard.report("$source for $date $event: $here C against a normal of $normal C")
                        continue
                    }
                }
                return@AtmosphereProvider spec
            }
            null
        }
    }
}
