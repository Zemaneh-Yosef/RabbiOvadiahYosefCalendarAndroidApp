package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.kosherjava.zmanim.util.GeoLocation
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction as AeRefraction
import io.github.cosinekitty.astronomy.SearchContext
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.atmosphere
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.search
import io.github.cosinekitty.astronomy.siderealTime
import java.time.Instant
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rise / set of the Moon's highest SUNLIT point (port of the moonrise project's moon-calc.ts and lit-limb.ts, the website's
 * mooncalc patch over Astronomy Engine). For Kiddush Levana the event is the lit part of the Moon crossing the horizon, not the
 * upper limb of the full disk (Astronomy Engine's SearchRiseSet): the highest illuminated point (bright-limb position angle
 * less the parallactic angle, the topocentric phase angle, and the finite-disk 3D terms) against a horizon of ROYZmanim's
 * refraction (34.48', scaled by the air's density at the ground) and Sweer's dip.
 *
 * Mode LIT (default) uses the 3D highest point; LIT_FLAT the flat-disk table; GEOMETRIC the upper limb of the full disk.
 */
class MoonCalc @JvmOverloads constructor(
    refractionArcmin: Double = ROYSPACalculator.ROY_REFRACTION_ARCMIN,
    private val scaleRefractionByDensity: Boolean = true,
    moonRadiusKm: Double = MOON_EQUATORIAL_RADIUS_KM,
    private val dipModel: DipModel = DipModel.PHYSICAL,
) {
    enum class Mode { LIT, LIT_FLAT, GEOMETRIC }
    enum class DipModel { PHYSICAL, LIBRARY }

    private val refractionDeg = refractionArcmin / 60
    private val moonRadiusAu = moonRadiusKm / KM_PER_AU

    /** Geocentric distance (m) of the ground point below the observer, oblate Earth (as in the library). */
    private fun groundRadiusM(observer: Observer, metersAboveGround: Double): Double {
        val phi = observer.latitude * DEG
        val sinphi = sin(phi)
        val cosphi = cos(phi)
        val c = 1 / hypot(cosphi, sinphi * EARTH_FLATTENING)
        val s = c * (EARTH_FLATTENING * EARTH_FLATTENING)
        val htKm = (observer.height - metersAboveGround) / 1000
        val ach = EARTH_EQUATORIAL_RADIUS_KM * c + htKm
        val ash = EARTH_EQUATORIAL_RADIUS_KM * s + htKm
        return 1000 * hypot(ach * cosphi, ash * sinphi)
    }

    /** Astronomy Engine's HorizonDipAngle(), verbatim (negative degrees). */
    private fun libraryHorizonDipAngle(observer: Observer, metersAboveGround: Double): Double {
        val radiusM = groundRadiusM(observer, metersAboveGround)
        val k = 0.175 * (1 - (6.5e-3 / 283.15) * (observer.height - (2.0 / 3) * metersAboveGround)).pow(3.256)
        return RAD * -(sqrt(2 * (1 - k) * metersAboveGround / radiusM) / (1 - k))
    }

    /** Angular dip of the visible horizon below the horizontal plane, degrees (<= 0). */
    fun horizonDipAngle(observer: Observer, metersAboveGround: Double): Double =
        if (dipModel == DipModel.LIBRARY) libraryHorizonDipAngle(observer, metersAboveGround)
        else -Refraction.sweerHorizon(metersAboveGround, observer.height - metersAboveGround, groundRadiusM(observer, metersAboveGround))[0]

    /** The geometric altitude (degrees, negative) the Moon's highest point must reach for rise / set. */
    @JvmOverloads
    fun horizonTarget(observer: Observer, metersAboveGround: Double = 0.0): Double {
        require(metersAboveGround.isFinite() && metersAboveGround >= 0) { "Invalid value for metersAboveGround: $metersAboveGround" }
        val ground = observer.height - metersAboveGround
        val density = if (scaleRefractionByDensity) atmosphere(ground).density else 1.0
        if (dipModel == DipModel.LIBRARY) return libraryHorizonDipAngle(observer, metersAboveGround) - refractionDeg * density
        val h = Refraction.sweerHorizon(metersAboveGround, ground, groundRadiusM(observer, metersAboveGround))
        return -h[0] - refractionDeg * density * h[1]
    }

    private fun hourAngleHours(t: Time, observer: Observer, raHours: Double): Double {
        var ha = (observer.longitude / 15 + siderealTime(t) - raHours) % 24
        if (ha < 0) ha += 24
        return ha
    }

    private fun sub(a: Vector, b: Vector) = doubleArrayOf(a.x - b.x, a.y - b.y, a.z - b.z)
    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun norm(a: DoubleArray) = sqrt(dot(a, a))

    /** Topocentric phase angle (Sun-Moon-observer), degrees. */
    private fun topocentricPhaseAngleDeg(t: Time, observer: Observer): Double {
        val moonG = geoVector(Body.Moon, t, Aberration.Corrected)
        val sunG = geoVector(Body.Sun, t, Aberration.Corrected)
        val obsV = observer.toVector(t, EquatorEpoch.J2000)
        val toSun = sub(sunG, moonG)
        val toObs = sub(obsV, moonG)
        val c = max(-1.0, min(1.0, dot(toSun, toObs) / (norm(toSun) * norm(toObs))))
        return RAD * acos(c)
    }

    /** Altitude of the relevant highest point (unrefracted, degrees). */
    private fun topAltitude(t: Time, observer: Observer, mode: Mode): Double {
        val eqMoon = equator(Body.Moon, t, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val hor = horizon(t, observer, eqMoon.ra, eqMoon.dec, AeRefraction.None)
        val semi = RAD * asin(moonRadiusAu / eqMoon.dist)
        if (mode == Mode.GEOMETRIC) return hor.altitude + semi
        val eqSun = equator(Body.Sun, t, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val haDeg = hourAngleHours(t, observer, eqMoon.ra) * 15
        val theta = LitLimb.limbAngleFromVertical(LitLimb.brightLimbPositionAngle(eqSun.ra, eqSun.dec, eqMoon.ra, eqMoon.dec),
            LitLimb.parallacticAngle(haDeg, observer.latitude, eqMoon.dec))
        val i = topocentricPhaseAngleDeg(t, observer)
        val h = if (mode == Mode.LIT_FLAT) LitLimb.highestLitPointFactor(theta, i) else LitLimb.highestLitPointFactor3D(theta, i, semi)
        return hor.altitude + h * semi
    }

    /** Degrees the relevant highest point is above the horizon target; zero at rise / set. */
    @JvmOverloads
    fun litLimbAltitudeExcess(time: Time, observer: Observer, mode: Mode = Mode.LIT, metersAboveGround: Double = 0.0) =
        topAltitude(time, observer, mode) - horizonTarget(observer, metersAboveGround)

    private fun maxAltitudeSlope(latitude: Double): Double {
        require(latitude in -90.0..90.0) { "Invalid geographic latitude: $latitude" }
        val latrad = latitude * DEG
        return abs(((360 / SOLAR_DAYS_PER_SIDEREAL_DAY) - 4.5) * cos(latrad)) + abs(8.2 * sin(latrad))
    }

    private class Ascent(val tx: Time, val ty: Time)

    /** Recursive bisection hunting for a sub-interval where altdiff rises through zero (the library's FindAscent). */
    private fun findAscent(depth: Int, altdiff: (Time) -> Double, maxDeriv: Double, t1: Time, t2: Time, a1: Double, a2: Double): Ascent? {
        if (a1 < 0 && a2 >= 0) return Ascent(t1, t2)
        if (a1 >= 0 && a2 < 0) return null
        if (depth > 17) throw IllegalStateException("Excessive recursion in rise/set ascent search.")
        val dt = t2.ut - t1.ut
        if (dt * SECONDS_PER_DAY < 1) return null
        val da = min(abs(a1), abs(a2))
        if (da > maxDeriv * (dt / 2)) return null
        val tmid = Time((t1.ut + t2.ut) / 2)
        val amid = altdiff(tmid)
        return findAscent(1 + depth, altdiff, maxDeriv, t1, tmid, a1, amid) ?: findAscent(1 + depth, altdiff, maxDeriv, tmid, t2, amid, a2)
    }

    /** Next moonrise (+1) or moonset (-1) after startTime, within limitDays (negative: searching backward); null if none. */
    @JvmOverloads
    fun nextMoonEvent(direction: Int, startTime: Time, observer: Observer, mode: Mode = Mode.LIT, limitDays: Double = 2.0, metersAboveGround: Double = 0.0): Time? {
        require(direction == 1 || direction == -1) { "Invalid direction: $direction (must be +1 or -1)" }
        val target = horizonTarget(observer, metersAboveGround)
        val maxDeriv = maxAltitudeSlope(observer.latitude) + (if (mode == Mode.GEOMETRIC) 0 else 5)
        val altdiff = { t: Time -> direction * (topAltitude(t, observer, mode) - target) }
        var t1 = startTime
        var t2 = startTime
        var a1 = altdiff(t1)
        var a2 = a1
        while (true) {
            if (limitDays < 0) { t1 = t2.addDays(-RISE_SET_DT); a1 = altdiff(t1) } else { t2 = t1.addDays(RISE_SET_DT); a2 = altdiff(t2) }
            val ascent = findAscent(0, altdiff, maxDeriv, t1, t2, a1, a2)
            if (ascent != null) {
                val time = search(ascent.tx, ascent.ty, 0.1, object : SearchContext { override fun eval(time: Time) = altdiff(time) })
                    ?: throw IllegalStateException("Rise/set search failed after finding ascent.")
                if (limitDays < 0) { if (time.ut < startTime.ut + limitDays) return null } else if (time.ut > startTime.ut + limitDays) return null
                return time
            }
            if (limitDays < 0) {
                if (t1.ut < startTime.ut + limitDays) return null
                t2 = t1; a2 = a1
            } else {
                if (t2.ut > startTime.ut + limitDays) return null
                t1 = t2; a1 = a2
            }
        }
    }

    /** Rise and set of the lit Moon in a window (MoonTimes' role): times, or whether it is up / down throughout. */
    class Window(@JvmField val rise: Instant?, @JvmField val set: Instant?, @JvmField val alwaysUp: Boolean, @JvmField val alwaysDown: Boolean)

    /** Moonrise and moonset of the lit part of the Moon between two instants, for a place. */
    fun riseSetWithin(geo: GeoLocation, start: Instant, end: Instant): Window {
        val observer = Observer(geo.latitude, geo.longitude, max(0.0, geo.elevation))
        val t0 = Time.fromMillisecondsSince1970(start.toEpochMilli())
        val days = (end.toEpochMilli() - start.toEpochMilli()) / 86400000.0
        val rise = nextMoonEvent(1, t0, observer, Mode.LIT, days)?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()) }
        val set = nextMoonEvent(-1, t0, observer, Mode.LIT, days)?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()) }
        if (rise != null || set != null) return Window(rise, set, false, false)
        val up = litLimbAltitudeExcess(t0, observer) > 0
        return Window(null, null, up, !up)
    }

    companion object {
        const val MOON_EQUATORIAL_RADIUS_KM = 1738.1
        private const val EARTH_FLATTENING = 0.996647180302104
        private const val EARTH_EQUATORIAL_RADIUS_KM = 6378.1366
        private const val SOLAR_DAYS_PER_SIDEREAL_DAY = 0.9972695717592592
        private const val SECONDS_PER_DAY = 86400.0
        /** Astronomy Engine's built-in horizon refraction (arcmin). */
        const val LIBRARY_REFRACTION_ARCMIN = 34.0
        /** Same bracketing step the library uses: 10.08 h, Nyquist-safe for the Moon's ~22 h cycle. */
        const val RISE_SET_DT = 0.42
        private const val DEG = PI / 180
        private const val RAD = 180 / PI

        /** The default calculator (ROYZmanim refraction, physical dip). */
        @JvmField val DEFAULT = MoonCalc()
    }
}

/** The lit limb's geometry (port of lit-limb.ts). */
object LitLimb {
    private const val DEG = PI / 180
    private const val RAD = 180 / PI
    const val MOON_RADIUS_OVER_AU = 1738.1 / 1.4959787069098932e8

    @JvmStatic
    fun normalizeAngle180(deg: Double): Double {
        var a = deg % 360
        if (a <= -180) a += 360
        if (a > 180) a -= 360
        if (a <= -180) a += 360
        return a
    }

    /** Position angle of the Moon's bright limb (Meeus 48.5), degrees. */
    @JvmStatic
    fun brightLimbPositionAngle(raSunHours: Double, decSunDeg: Double, raMoonHours: Double, decMoonDeg: Double): Double {
        val dAlpha = (raSunHours - raMoonHours) * 15 * DEG
        val d0 = decSunDeg * DEG
        val d = decMoonDeg * DEG
        val y = cos(d0) * sin(dAlpha)
        val x = sin(d0) * cos(d) - cos(d0) * sin(d) * cos(dAlpha)
        return normalizeAngle180(RAD * atan2(y, x))
    }

    /** Parallactic angle (Meeus 14.1), degrees. */
    @JvmStatic
    fun parallacticAngle(hourAngleDeg: Double, latDeg: Double, decDeg: Double): Double {
        val H = hourAngleDeg * DEG
        val phi = latDeg * DEG
        val dec = decDeg * DEG
        return normalizeAngle180(RAD * atan2(sin(H), tan(phi) * cos(dec) - sin(dec) * cos(H)))
    }

    @JvmStatic
    fun limbAngleFromVertical(chi: Double, q: Double) = normalizeAngle180(chi - q)

    /** Flat-disk height of the highest lit point above the disk's centre, in units of the semi-diameter. */
    @JvmStatic
    fun highestLitPointFactor(thetaDeg: Double, phaseAngleDeg: Double): Double {
        val th = thetaDeg * DEG
        val i = phaseAngleDeg * DEG
        val cosTh = cos(th)
        val sinTh = sin(th)
        if (cosTh >= 0) return 1.0
        if (phaseAngleDeg <= 90) {
            val cosI = cos(i)
            return sqrt(sinTh * sinTh + cosI * cosI * cosTh * cosTh)
        }
        return abs(sinTh)
    }

    @JvmStatic
    fun litDiskAreaFraction(phaseAngleDeg: Double) = (1 + cos(phaseAngleDeg * DEG)) / 2

    /** The highest lit point with the finite-disk terms (cusps on the true limb, perspective), in units of the semi-diameter. */
    @JvmStatic
    @JvmOverloads
    fun highestLitPointFactor3D(thetaDeg: Double, phaseAngleDeg: Double, semiDiameterDeg: Double, epsSun: Double = MOON_RADIUS_OVER_AU): Double {
        if (!(semiDiameterDeg > 0)) return highestLitPointFactor(thetaDeg, phaseAngleDeg)
        val rho = semiDiameterDeg * DEG
        val sinRho = sin(rho)
        val th = thetaDeg * DEG
        val i = phaseAngleDeg * DEG
        val cosTh = cos(th)
        val sinI = sin(i)
        val cosI = cos(i)
        val rLimb = sqrt(max(0.0, 1 - sinRho * sinRho))
        fun heightDeg(nw: Double, c: Double): Double {
            val r = sqrt(max(0.0, 1 - c * c))
            if (r <= 0) return 0.0
            return RAD * atan(sinRho * r / (1 - sinRho * c)) * (nw / r)
        }
        var best = Double.NEGATIVE_INFINITY
        val denomA = rLimb * sinI
        val numA = epsSun - sinRho * cosI
        val m = if (denomA > 0) numA / denomA else if (numA > 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
        if (m < 1) {
            val beta0 = if (m <= -1) PI else acos(m)
            val absTh = abs(th)
            val cosBest = if (absTh <= beta0) 1.0 else cos(beta0 - absTh)
            best = max(best, heightDeg(rLimb * cosBest, sinRho))
        }
        val root = sqrt(max(0.0, 1 - sinI * sinI * cosTh * cosTh))
        val sq = sqrt(max(0.0, 1 - epsSun * epsSun))
        if (root > 0) {
            val cT = epsSun * cosI - sq * sinI * cosI * cosTh / root
            if (cT > sinRho) best = max(best, heightDeg(epsSun * sinI * cosTh + sq * root, cT))
        }
        if (best == Double.NEGATIVE_INFINITY) return 0.0
        return best / semiDiameterDeg
    }
}
