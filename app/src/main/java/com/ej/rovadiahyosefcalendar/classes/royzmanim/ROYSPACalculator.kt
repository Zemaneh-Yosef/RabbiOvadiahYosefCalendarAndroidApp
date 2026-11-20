package com.ej.rovadiahyosefcalendar.classes.royzmanim

import android.util.Log
import com.kosherjava.zmanim.util.AstronomicalCalculator
import com.kosherjava.zmanim.util.GeoLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

/**
 * KosherJava's SPA calculator with the accuracy corrections of the website's royzmanim-spa-corrections.js (a port of it):
 *
 *  1. Refraction constant 34.478885263888294' (Calendrical Calculations') instead of 34'.
 *  2. Elevation model ('physical', default): the dip and horizon-ray refraction from Sweer's (1938) ray equations in a
 *     standard atmosphere instead of KosherJava's geometric acos(R / (R + h)). Sea-level results are unchanged.
 *  3. Earth radius: the WGS84 radius of curvature along the line of sight ([configureForLocation]).
 *  5. Optional solar radius in km ([setSolarRadiusKm]).
 *  6. Optional ray-traced refraction for the actual air ([setAtmosphereProvider]): the whole horizon depression is traced
 *     through van der Werf's modified US1976 atmosphere anchored to the supplied temperature and pressure.
 *  7. Optional visible sunrise / sunset over a terrain horizon ([getVisibleSunrise] / [getVisibleSunset]), every point of the
 *     limb tested against the profile at its own azimuth, DEM points traced through the same air.
 *  8. Optional air that varies along the path (weather-model profiles, [PathAir]).
 *  9. Observers below sea level (the eye's real height from the HorizonSet).
 * 10. Optional water vapour ([setHumidity]).
 * 11. The Sun's apparent place: VSOP87D with 412 terms, IAU 2000B nutation, the VSOP87-to-FK5 correction, IAU 2006
 *     precession and sidereal time, Delta-T from IERS measurements and the Earth's rotation from UT1. Computed once per day
 *     and interpolated between the five nearest days (Meeus 3.8).
 * 12. A result cache, keyed also by the air the provider gives for the date and event; every setter clears it.
 *
 * Depression-angle zmanim (16.04 degrees, 7.165 degrees) are untouched by 1-10, as in KosherJava.
 */
class ROYSPACalculator @JvmOverloads constructor(
    refractionArcmin: Double = ROY_REFRACTION_ARCMIN,
    private val elevationModel: ElevationModel = ElevationModel.PHYSICAL,
) : AstronomicalCalculator() {

    enum class ElevationModel { PHYSICAL, KOSHERJAVA }

    private var solarRadiusScale = 1.0
    @Volatile private var atmosphereProvider: AtmosphereProvider? = null
    private var seaSurfaceLayer = false
    private var humidity = false
    private var terrainRays = true
    private var terrainImages = Refraction.ImageSearch.OFF
    private var earlySunImages = false
    private var applyDeltaT = true
    private var deltaTOverride: Double? = null
    private var pressure = 1013.25
    private var temperature = 10.0

    private val horizonCache = lru<String, DoubleArray>(256)
    private val refrTableCache = lru<String, RefractionTable>(64)
    private val thresholdCache: MutableMap<HorizonProfile, MutableMap<String, Thresholds>> = Collections.synchronizedMap(WeakHashMap())
    private val sunNodes = lru<String, DoubleArray>(32)
    private val sunDays = lru<String, DoubleArray>(16)
    private val resultCache = lru<String, Double>(512)

    init {
        refraction = refractionArcmin / 60
    }

    // ---------------------------------------------------------------------------------------- settings

    /** Include water vapour wherever the atmosphere data carry humidity. Off by default (a few seconds at most). */
    fun setHumidity(on: Boolean) { humidity = on; clearResultCache() }
    fun getHumidity() = humidity
    /** Trace the ray to each DEM horizon point through a path atmosphere (default on). */
    fun setTerrainRayTracing(on: Boolean) { terrainRays = on; clearResultCache() }
    /** Let the Sun be seen first through a ray over cold water bent more than the horizon ray. Off by default. */
    fun setEarlySunImages(on: Boolean) { earlySunImages = on; clearResultCache() }
    /** How a ridge seen through a duct is handled; OFF (default) keeps the first solution. */
    fun setTerrainImageSearch(mode: Refraction.ImageSearch) { terrainImages = mode; clearResultCache() }
    /** Model the sea surface layer over water in path profiles. Off by default: unvalidated. */
    fun setSeaSurfaceLayer(on: Boolean) { seaSurfaceLayer = on; clearResultCache() }
    fun setApplyDeltaT(on: Boolean) { applyDeltaT = on; clearResultCache() }
    fun isApplyDeltaT() = applyDeltaT
    fun setDeltaTOverride(seconds: Double?) { deltaTOverride = seconds; clearResultCache() }
    fun getDeltaTOverride() = deltaTOverride
    /** SPA's own refraction for getSolarElevation (not used for sunrise / sunset). */
    fun setPressure(mb: Double) { pressure = mb }
    fun setTemperature(c: Double) { temperature = c }

    override fun setRefraction(refraction: Double) { super.setRefraction(refraction); clearResultCacheSafe() }
    override fun setEarthRadius(earthRadius: Double) { super.setEarthRadius(earthRadius); clearResultCacheSafe() }
    override fun setSolarRadius(solarRadius: Double) { super.setSolarRadius(solarRadius); clearResultCacheSafe() }

    // the superclass constructor calls setters before the caches exist
    @Suppress("SENSELESS_COMPARISON")
    private fun clearResultCacheSafe() { if (resultCache != null) clearResultCache() }

    // KosherJava 3.1.0 keeps its own result cache in AstronomicalCalculator (keyed by date and place only): clear it too, since
    // these settings change the results for the same date and place
    public override fun clearResultCache() {
        synchronized(resultCache) { resultCache.clear() }
        super.clearResultCache()
    }

    override fun getCalculatorName() = "Solar Position Algorithm (ROYZmanim corrections)"

    /** Sets the per-latitude Earth radius (radius of curvature, east-west). Call when the location changes. */
    @JvmOverloads
    fun configureForLocation(geoLocation: GeoLocation, azimuthDeg: Double = 90.0) {
        setEarthRadius(Refraction.earthRadiusAtLatitude(geoLocation.latitude, azimuthDeg))
    }

    /** Use the actual air at sunrise / sunset instead of the 34.48' average; null to remove. */
    fun setAtmosphereProvider(provider: AtmosphereProvider?) { atmosphereProvider = provider; clearResultCache() }
    fun getAtmosphereProvider() = atmosphereProvider

    /** A solar radius other than the table's 696,000 km basis (e.g. 695,700); null = table. */
    fun setSolarRadiusKm(km: Double?) { solarRadiusScale = if (km == null) 1.0 else km / 696000; clearResultCache() }

    override fun getApparentSolarRadius(localDate: LocalDate?): Double = super.getApparentSolarRadius(localDate) * solarRadiusScale

    // ---------------------------------------------------------------------------------------- sunrise / sunset

    override fun getUTCSunrise(localDate: LocalDate, geoLocation: GeoLocation, zenith: Double, adjustForElevation: Boolean) =
        cachedRiseSet(localDate, geoLocation, zenith, adjustForElevation, SolarEvent.SUNRISE, true)

    override fun getUTCSunset(localDate: LocalDate, geoLocation: GeoLocation, zenith: Double, adjustForElevation: Boolean) =
        cachedRiseSet(localDate, geoLocation, zenith, adjustForElevation, SolarEvent.SUNSET, true)

    private fun cachedRiseSet(date: LocalDate, geo: GeoLocation, zenith: Double, adjust: Boolean, event: SolarEvent, useProvider: Boolean): Double {
        val provider = if (useProvider) atmosphereProvider else null
        var key = "$date|${geo.latitude}|${geo.longitude}|${geo.elevation}|$zenith|$adjust|$event|${earthRadius}|${refraction}|$solarRadiusScale|$applyDeltaT|$deltaTOverride|$humidity|$seaSurfaceLayer"
        if (zenith == GEOMETRIC_ZENITH && elevationModel == ElevationModel.PHYSICAL && provider != null) key += "|" + airKey(safeAir(provider, date, event, geo))
        synchronized(resultCache) { resultCache[key] }?.let { return it }
        val elevation = if (adjust) geo.elevation else 0.0
        val adjusted = adjustZenith(zenith, elevation, date, event, geo, provider)
        var riseSet = solveRiseSet(date, geo, adjusted, event)
        if (!riseSet.isNaN()) {
            riseSet /= 60
            riseSet = (riseSet % 24 + 24) % 24
        }
        synchronized(resultCache) { resultCache[key] = riseSet }
        return riseSet
    }

    private fun safeAir(provider: AtmosphereProvider, date: LocalDate, event: SolarEvent, geo: GeoLocation): AtmosphereSpec? = try {
        provider.airFor(date, event, geo)
    } catch (e: Exception) {
        Log.e(TAG, "Atmosphere provider failed", e)
        null
    }

    /** The atmosphere for an event, or null when the default (34.48' + Sweer) model applies. */
    private fun atmosphereFor(date: LocalDate?, event: SolarEvent?, geo: GeoLocation?, heightM: Double, provider: AtmosphereProvider? = atmosphereProvider): Medium? {
        if (provider == null || date == null || event == null || geo == null) return null
        val spec = safeAir(provider, date, event, geo) ?: return null
        val problem = AtmosphereGuard.specProblem(spec)
        if (problem != null) { AtmosphereGuard.report("$date $event: $problem"); return null }
        return Refraction.atmosphereFromSpec(heightM, spec, seaSurfaceLayer, humidity, geo.latitude)
    }

    private fun rayTracedHorizon(atm: Medium, heightM: Double): DoubleArray {
        val R = earthRadius * 1000
        val key = atm.key + "|" + heightM + "|" + R
        synchronized(horizonCache) { horizonCache[key] }?.let { return it }
        val dip = -atm.seaHorizon(heightM, R)
        // nudge just above the grazing ray so the tracer does not register it as reaching the sea
        val v = doubleArrayOf(dip, atm.refraction(heightM, -dip + (if (heightM > 0) 1e-7 else 0.0), R))
        synchronized(horizonCache) { horizonCache[key] = v }
        return v
    }

    /** Observed dip of the sea horizon in degrees for an observer elevationMeters above it. */
    fun getObservedDip(elevationMeters: Double) = Refraction.sweerHorizon(elevationMeters, 0.0, earthRadius * 1000)[0]

    /**
     * Total depression of the Sun's upper limb below the geometric horizon, excluding the solar radius, degrees: observed dip
     * plus the horizon ray's refraction. Ray traced with an atmosphere; without, the 34.48' constant scaled by Sweer's ratio.
     */
    @JvmOverloads
    fun getHorizonDepression(elevationMeters: Double, atmosphere: Medium? = null): Double {
        val h0 = max(0.0, elevationMeters)
        if (atmosphere != null) {
            val r = rayTracedHorizon(atmosphere, h0)
            val total = r[0] + r[1]
            // never let odd weather data take sunrise / sunset away: fall back to the default model
            if (total.isFinite()) return total
        }
        val h = Refraction.sweerHorizon(h0, 0.0, earthRadius * 1000)
        return h[0] + refraction * h[1]
    }

    private fun adjustZenith(zenith: Double, elevation: Double, date: LocalDate?, event: SolarEvent?, geo: GeoLocation?, provider: AtmosphereProvider?): Double {
        if (zenith != GEOMETRIC_ZENITH) return zenith
        val radius = if (isUseApparentSolarRadius && date != null) getApparentSolarRadius(date) else solarRadius
        if (elevationModel == ElevationModel.KOSHERJAVA) {
            return zenith + radius + refraction + Math.toDegrees(acos(earthRadius / (earthRadius + elevation / 1000)))
        }
        // Negative elevations (e.g. Dead Sea) are treated as sea level: the horizon model needs height >= 0.
        val heightM = if (elevation.isFinite()) max(0.0, elevation) else 0.0
        val atm = atmosphereFor(date, event, geo, heightM, provider)
        return zenith + radius + getHorizonDepression(heightM, atm)
    }

    // KosherJava 3.0's SPACalculator solver, with 3.1's bracketing fallback where the closed-form guess fails
    private fun solveRiseSet(localDate: LocalDate, geo: GeoLocation, adjustedZenith: Double, event: SolarEvent): Double {
        val jdDay = julianDay(localDate)
        val lonWest = -geo.longitude
        val lat = geo.latitude
        val elevation = geo.elevation
        val noonMin = solveNoonMidnight(jdDay, lonWest, true)
        val declNoon = solarCoords(jdDay + noonMin / 1440.0)[1]
        val eot = equationOfTime(jdDay + noonMin / 1440.0)
        val cosH0 = (cosD(adjustedZenith) - sinD(lat) * sinD(declNoon)) / (cosD(lat) * cosD(declNoon))
        if (cosH0 < -1.0 || cosH0 > 1.0) return bracketRiseSet(jdDay, lat, geo.longitude, elevation, adjustedZenith, event)
        val h0 = Math.toDegrees(acos(cosH0))
        val signed = if (event == SolarEvent.SUNRISE) h0 else -h0
        val guess = 720 + 4 * (lonWest - signed) - eot
        var t0 = guess
        var t1 = guess + 0.5
        var f0 = topocentricTrueZenith(jdDay + t0 / 1440.0, lat, geo.longitude, elevation) - adjustedZenith
        var f1 = topocentricTrueZenith(jdDay + t1 / 1440.0, lat, geo.longitude, elevation) - adjustedZenith
        var minSlope = abs((f1 - f0) / (t1 - t0))
        var i = 0
        while (i < 12 && abs(f1) > 1e-9) {
            val denom = f1 - f0
            if (denom == 0.0) break
            minSlope = min(minSlope, abs(denom / (t1 - t0)))
            val t2 = t1 - f1 * (t1 - t0) / denom
            t0 = t1; f0 = f1; t1 = t2
            f1 = topocentricTrueZenith(jdDay + t1 / 1440.0, lat, geo.longitude, elevation) - adjustedZenith
            i++
        }
        val window = riseSetWindowMinutes(geo.longitude, event)
        val inWindow = if (event == SolarEvent.SUNRISE) t1 > window[0] && t1 <= window[1] else t1 >= window[0] && t1 < window[1]
        return if (inWindow && abs(f1) <= 1e-4 && minSlope >= 0.02) t1 else bracketRiseSet(jdDay, lat, geo.longitude, elevation, adjustedZenith, event)
    }

    private fun riseSetWindowMinutes(longitude: Double, event: SolarEvent): DoubleArray {
        val n = (if (event == SolarEvent.SUNRISE) -6 else 6) - longitude / 15
        return doubleArrayOf(n * 60, (n + 24) * 60)
    }

    /** Scan the day's window for the first crossing of the zenith in the event's direction, then bisect. NaN if none. */
    private fun bracketRiseSet(jdDay: Double, lat: Double, lon: Double, elevation: Double, zenith: Double, event: SolarEvent): Double {
        val w = riseSetWindowMinutes(lon, event)
        val dir = if (event == SolarEvent.SUNRISE) 1 else -1
        val target = sin(Math.toRadians(90 - zenith))
        fun f(minutes: Double) = dir * (sin(Math.toRadians(topocentric(jdDay + minutes / 1440, lat, lon, elevation)[0])) - target)
        val step = 6.0
        var prevT = w[0]
        var prev = f(prevT)
        var t = w[0] + step
        while (t <= w[1]) {
            val cur = f(t)
            if (prev < 0 && cur >= 0) {
                var a = prevT
                var b = t
                while (b - a > 1e-6) { val m = (a + b) / 2; if (f(m) < 0) a = m else b = m }
                return b
            }
            prev = cur; prevT = t
            t += step
        }
        return Double.NaN
    }

    override fun getUTCNoon(localDate: LocalDate, geoLocation: GeoLocation): Double {
        val noon = solveNoonMidnight(julianDay(localDate), -geoLocation.longitude, true) / 60
        return (noon % 24 + 24) % 24
    }

    override fun getUTCMidnight(localDate: LocalDate, geoLocation: GeoLocation): Double {
        val midnight = solveNoonMidnight(julianDay(localDate), -geoLocation.longitude, false) / 60
        return (midnight % 24 + 24) % 24
    }

    private fun solveNoonMidnight(julianDay: Double, lonWest: Double, noon: Boolean): Double {
        val lonEast = -lonWest
        val targetHourAngle = if (noon) 0.0 else 180.0
        var dayFraction = (if (noon) 0.5 else 1.0) - lonEast / 360.0
        for (i in 0 until 3) {
            val sc = solarCoords(julianDay + dayFraction)
            val hourAngle = ((sc[3] + lonEast - sc[0] - targetHourAngle) % 360 + 540) % 360 - 180
            dayFraction -= hourAngle / 360.0
        }
        return dayFraction * 1440.0
    }

    override fun getTimeAtAzimuth(localDate: LocalDate, geoLocation: GeoLocation, azimuth: Double): Double {
        require(azimuth == 90.0 || azimuth == 270.0) { "The targetAzimuth must be 90 or 270. Other azimuth values are not supported" }
        val julianDay = julianDay(localDate)
        val solarNoonBase = 0.5 - geoLocation.longitude / 360.0
        var dateTime = solarNoonBase + if (azimuth == 90.0) 0.25 else 0.75
        for (i in 0 until 4) {
            val jd = julianDay + dateTime
            val decl = solarCoords(jd)[1]
            val ratio = tanD(decl) / tanD(geoLocation.latitude)
            if (ratio.isNaN() || ratio > 1.0 || ratio < -1.0) return Double.NaN
            val offset = (if (azimuth == 90.0) -1.0 else 1.0) * (Math.toDegrees(acos(ratio)) / 360.0)
            dateTime = solarNoonBase + offset - equationOfTime(jd) / 1440.0
        }
        val timeUTC = dateTime * 24.0
        return (timeUTC % 24 + 24) % 24
    }

    override fun getSolarElevation(instant: Instant, geoLocation: GeoLocation) =
        topocentric(julianDayFromInstant(instant), geoLocation.latitude, geoLocation.longitude, geoLocation.elevation)[1]

    override fun getSolarAzimuth(instant: Instant, geoLocation: GeoLocation) =
        topocentric(julianDayFromInstant(instant), geoLocation.latitude, geoLocation.longitude, geoLocation.elevation)[2]

    /** Topocentric true (unrefracted) altitude and azimuth of the Sun's centre at a UT Julian day. */
    fun sunTrue(jd: Double, geo: GeoLocation): DoubleArray {
        val t = topocentric(jd, geo.latitude, geo.longitude, geo.elevation)
        return doubleArrayOf(t[0], t[2])
    }

    private fun topocentric(jdUT: Double, latitude: Double, longitude: Double, elevationMeters: Double): DoubleArray {
        val sc = solarCoords(jdUT)
        val alpha = sc[0]; val delta = sc[1]; val nu = sc[3]; val radius = sc[4]
        var h = (nu + longitude - alpha) % 360.0
        h = (h + 360) % 360
        val xi = 8.794 / (3600.0 * radius)
        val u = atan(0.99664719 * tanD(latitude))
        val x = cos(u) + (elevationMeters / 6378140.0) * cosD(latitude)
        val y = 0.99664719 * sin(u) + (elevationMeters / 6378140.0) * sinD(latitude)
        val deltaAlpha = Math.toDegrees(atan2(-x * sinD(xi) * sinD(h), cosD(delta) - x * sinD(xi) * cosD(h)))
        val deltaPrime = Math.toDegrees(atan2((sinD(delta) - y * sinD(xi)) * cosD(deltaAlpha), cosD(delta) - x * sinD(xi) * cosD(h)))
        val hPrime = h - deltaAlpha
        val e0 = Math.toDegrees(asin(sinD(latitude) * sinD(deltaPrime) + cosD(latitude) * cosD(deltaPrime) * cosD(hPrime)))
        val e = e0 + refractionCorrection(e0)
        val gamma = Math.toDegrees(atan2(sinD(hPrime), cosD(hPrime) * sinD(latitude) - tanD(deltaPrime) * cosD(latitude)))
        val azimuth = ((gamma + 180.0) % 360.0 + 360) % 360
        return doubleArrayOf(e0, e, azimuth)
    }

    private fun topocentricTrueZenith(jdUT: Double, latitude: Double, longitude: Double, elevationMeters: Double) =
        90.0 - topocentric(jdUT, latitude, longitude, elevationMeters)[0]

    private fun refractionCorrection(trueElevation: Double): Double {
        if (trueElevation < -1.0 * (solarRadius + refraction)) return 0.0
        return (pressure / 1010.0) * (283.0 / (273.0 + temperature)) * 1.02 / (60.0 * tanD(trueElevation + 10.3 / (trueElevation + 5.11)))
    }

    // ---------------------------------------------------------------------------------------- the Sun's apparent place (item 11)

    private fun dut1(jdUT: Double) = if (applyDeltaT) DeltaT.ut1MinusUtc(jdUT) else 0.0

    private fun julianCenturiesTT(jdUT: Double): Double {
        val dt = if (applyDeltaT) deltaTOverride ?: DeltaT.deltaT(jdUT) else 0.0
        return (jdUT + (dt + dut1(jdUT)) / 86400 - 2451545) / 36525
    }

    /** [RA, declination, true obliquity, equation of the equinoxes (deg), distance (AU), apparent longitude] */
    private fun solarPlace(jdUT: Double): DoubleArray {
        val t = julianCenturiesTT(jdUT)
        val tau = t / 10
        val radius = Vsop87Earth.sumSeries(Vsop87Earth.EARTH_R, tau)
        var theta = Vsop87Earth.sumSeries(Vsop87Earth.EARTH_L, tau) / DEG + 180
        var beta = -Vsop87Earth.sumSeries(Vsop87Earth.EARTH_B, tau) / DEG
        val lp = (theta - 1.397 * t - 0.00031 * t * t) * DEG
        theta += (-0.09033 - 0.300405 * t) / 3600
        beta += 0.03916 / 3600 * (cos(lp) - sin(lp))
        val nut = Nutation.nutation(t)
        val eps = (meanObliquity(t) + nut[1]) / 3600 * DEG
        val lambda = (theta + (nut[0] - 20.4898 / radius) / 3600) * DEG
        val b = beta * DEG
        val alpha = atan2(sin(lambda) * cos(eps) - tan(b) * sin(eps), cos(lambda)) / DEG
        val delta = asin(sin(b) * cos(eps) + cos(b) * sin(eps) * sin(lambda)) / DEG
        return doubleArrayOf((alpha % 360 + 360) % 360, delta, eps / DEG, nut[0] / 3600 * cos(eps), radius, ((lambda / DEG) % 360 + 360) % 360)
    }

    /**
     * [RA, declination, true obliquity, apparent Greenwich sidereal time, distance (AU), apparent longitude] (degrees): the
     * place computed at 0h UT of each day and interpolated between the five nearest days (Meeus 3.8).
     */
    private fun solarCoords(jdUT: Double): DoubleArray {
        val n0 = round(jdUT - 0.5) + 0.5
        val n = jdUT - n0
        val tag = if (applyDeltaT) (deltaTOverride?.toString() ?: "iers-ut1") else "none"
        val dayKey = "$n0|$tag"
        val c = synchronized(sunDays) { sunDays[dayKey] } ?: run {
            val y = Array(5) { k ->
                val nodeKey = "${n0 + k - 2}|$tag"
                synchronized(sunNodes) { sunNodes[nodeKey] } ?: solarPlace(n0 + k - 2).also { v -> synchronized(sunNodes) { sunNodes[nodeKey] = v } }
            }
            val out = DoubleArray(30)
            for (i in 0 until 6) {
                val angle = i == 0 || i == 5
                val d = DoubleArray(4) { k -> if (angle) ((y[k + 1][i] - y[k][i] + 540) % 360 - 180) else y[k + 1][i] - y[k][i] }
                val A = d[0]; val B = d[1]; val C = d[2]; val Dd = d[3]
                val E = B - A; val F = C - B; val G = Dd - C; val H = F - E; val J = G - F
                out[5 * i] = y[2][i]; out[5 * i + 1] = (B + C) / 2; out[5 * i + 2] = F / 2; out[5 * i + 3] = (H + J) / 12; out[5 * i + 4] = (J - H) / 24
            }
            synchronized(sunDays) { sunDays[dayKey] = out }
            out
        }
        val n2 = n * n
        val m = n * (n2 - 1)
        val q = n2 * (n2 - 1)
        fun at(i: Int) = c[5 * i] + n * c[5 * i + 1] + n2 * c[5 * i + 2] + m * c[5 * i + 3] + q * c[5 * i + 4]
        fun wrap(x: Double) = (x % 360 + 360) % 360
        val gast = greenwichMeanSiderealTime(jdUT + dut1(jdUT) / 86400, julianCenturiesTT(jdUT)) + at(3)
        return doubleArrayOf(wrap(at(0)), at(1), at(2), wrap(gast), at(4), wrap(at(5)))
    }

    /** The Sun's geocentric apparent place at a UT Julian day (see solarCoords), for the star model. */
    fun apparentSolarCoords(jdUT: Double): DoubleArray = solarCoords(jdUT)

    /** Equation of time in minutes (Meeus 28.1), from the apparent place above. */
    private fun equationOfTime(jdUT: Double): Double {
        val sc = solarCoords(jdUT)
        val jce = julianCenturiesTT(jdUT)
        val jme = jce / 10
        val l0 = 280.4664567 + jme * (360007.6982779 + jme * (0.03032028 + jme * (1 / 49931.0 - jme * (1 / 15300.0 + jme / 2000000))))
        val e = (l0 % 360 + 360) % 360 - 0.0057183 - sc[0] + Nutation.nutation(jce)[0] / 3600 * cos(sc[2] * DEG)
        return (((e + 180) % 360 + 360) % 360 - 180) * 4
    }

    // ---------------------------------------------------------------------------------------- visible sunrise / sunset (item 7)

    /**
     * Visible sunrise: the first moment the chosen part of the Sun's limb clears the horizon. With a HorizonSet in 'vantage'
     * mode, the earliest among its points. Epoch milliseconds (UTC), or NaN if the Sun is not seen to rise that day.
     */
    @JvmOverloads
    fun getVisibleSunrise(date: LocalDate, geoLocation: GeoLocation, horizon: Horizon, options: VisibleOptions = VisibleOptions()) = visible(date, geoLocation, horizon, SolarEvent.SUNRISE, options)

    /** Visible sunset: the last moment the chosen part of the limb is above the horizon; with a vantage set, the latest. */
    @JvmOverloads
    fun getVisibleSunset(date: LocalDate, geoLocation: GeoLocation, horizon: Horizon, options: VisibleOptions = VisibleOptions()) = visible(date, geoLocation, horizon, SolarEvent.SUNSET, options)

    private fun visible(date: LocalDate, geo: GeoLocation, horizon: Horizon, event: SolarEvent, options: VisibleOptions): Double = when (horizon) {
        is HorizonProfile -> visibleEvent(date, geo, horizon, event, options, options.observerHeightM, null)
        is HorizonSet -> fromSet(date, geo, horizon, event, options).firstOrNull()?.epochMs ?: Double.NaN
    }

    /** Visible sunrise or sunset from every relevant point of a HorizonSet, best first. */
    fun getVisibleEvents(date: LocalDate, geo: GeoLocation, set: HorizonSet, event: SolarEvent, options: VisibleOptions = VisibleOptions()) = fromSet(date, geo, set, event, options)

    /** Sea-level and elevated sunrise for a date (epoch ms, NaN where none), from the same refraction model and air. */
    fun getSunrises(date: LocalDate, geo: GeoLocation): DoubleArray {
        val d = solarDate(date, geo)
        return doubleArrayOf(
            utcToEpochMs(d, geo, getUTCSunrise(d, geo, GEOMETRIC_ZENITH, false), SolarEvent.SUNRISE),
            utcToEpochMs(d, geo, getUTCSunrise(d, geo, GEOMETRIC_ZENITH, true), SolarEvent.SUNRISE),
        )
    }

    /** KosherJava's antimeridian adjustment: the date whose noon by longitude is this place's civil noon on `date`. */
    private fun solarDate(date: LocalDate, geo: GeoLocation): LocalDate {
        val k = geo.getAntimeridianAdjustment(date.atStartOfDay(geo.zoneId).toInstant())
        return if (k != 0) date.plusDays(k.toLong()) else date
    }

    /** UTC hours (modulo 24) to epoch ms on the right day for this place. */
    private fun utcToEpochMs(date: LocalDate, geo: GeoLocation, hours: Double, event: SolarEvent): Double {
        if (!hours.isFinite()) return Double.NaN
        val noon = julianDay(date) + 0.5 - geo.longitude / 360
        val target = noon + if (event == SolarEvent.SUNRISE) -0.25 else 0.25
        var jd = julianDay(date) + hours / 24
        while (jd - target > 0.5) jd -= 1
        while (target - jd > 0.5) jd += 1
        return (jd - JD_UNIX_EPOCH) * 86400000
    }

    private fun fromSet(date: LocalDate, geo: GeoLocation, set: HorizonSet, event: SolarEvent, options: VisibleOptions): List<VisibleEvent> {
        require(set.points.isNotEmpty()) { "Not a horizon profile or HorizonSet" }
        val opts = options.copy(maxGapDeg = options.maxGapDeg ?: 0.5)
        var pts = set.points.filter { it.profile(event)?.points?.isNotEmpty() == true }
        if (set.mode == "vantage" && pts.size > 1) {
            // only points that win near today's rise / set direction need tracing; the direction from the default model
            val d = solarDate(date, geo)
            val t = utcToEpochMs(d, geo, cachedRiseSet(d, geo, GEOMETRIC_ZENITH, true, event, false), event)
            if (t.isFinite()) {
                val az0 = sunTrue(t / 86400000 + JD_UNIX_EPOCH, geo)[1]
                val near = pts.filter { p ->
                    (p.wins[event] ?: emptyList()).any { w ->
                        val lo = w[0]; val hi = w[1]
                        ((az0 - (lo - 3)) % 360 + 360) % 360 <= ((hi + 3 - (lo - 3)) % 360 + 360) % 360
                    }
                }
                if (near.isNotEmpty()) pts = near
            }
        }
        val out = ArrayList<VisibleEvent>()
        for (p in pts) {
            var ms = Double.NaN
            try {
                // A GeoLocation cannot be below sea level; the real height goes to the horizon model separately
                val pointGeo = GeoLocation(geo.locationName, p.lat, p.lon, max(0.0, p.height), geo.zoneId)
                ms = visibleEvent(date, pointGeo, p.profile(event)!!, event, opts, p.height, p.ground)
            } catch (e: Exception) {
                Log.e(TAG, "Visible $event failed for vantage point ${p.lat}, ${p.lon}", e)
            }
            if (ms.isFinite()) out.add(VisibleEvent(ms, p.lat, p.lon, p.height))
        }
        out.sortWith { a, b -> if (event == SolarEvent.SUNRISE) a.epochMs.compareTo(b.epochMs) else b.epochMs.compareTo(a.epochMs) }
        return out
    }

    class Thresholds(val azimuthDeg: DoubleArray, val apparentDeg: DoubleArray, val thresholdDeg: DoubleArray)
    private class RefractionTable(val a: DoubleArray, val refr: DoubleArray, val rough: BooleanArray)

    /** The profile reduced to the true (unrefracted) altitude the Sun must reach at each azimuth (cached, read-only). */
    fun getHorizonThresholds(date: LocalDate, geo: GeoLocation, profile: HorizonProfile, event: SolarEvent, observerHeightM: Double? = null, groundM: Double? = null): Thresholds {
        val ho = if (observerHeightM != null && observerHeightM.isFinite()) observerHeightM else max(0.0, geo.elevation)
        var floorM = 0.0
        if (ho < 0) {
            floorM = min(if (groundM != null && groundM.isFinite()) groundM else ho, ho)
            for (p in profile.points) if (p is DemHorizonPoint && p.heightM.isFinite()) floorM = min(floorM, p.heightM)
        }
        val floors = profile.points.mapNotNull { (it as? DemHorizonPoint)?.floorM }.filter { it.isFinite() }
        if (floors.size == profile.points.size) floorM = min(if (ho < 0) floorM else Double.POSITIVE_INFINITY, floors.min())
        val R = earthRadius * 1000
        val provided = atmosphereFor(date, event, geo, ho)
        val atm = provided ?: Refraction.atmosphereFromSpec(ho, null)
        // Without a provider, match the default sunrise model: the ISA trace scaled so that the sea-level horizon
        // refraction equals getRefraction(), as Sweer's ratio is
        val scale = if (provided != null) 1.0 else {
            val k = "isa0|$R"
            val isa = synchronized(horizonCache) { horizonCache[k] } ?: doubleArrayOf(0.0, atm.refraction(0.0, 0.0, R)).also { synchronized(horizonCache) { horizonCache[k] = it } }
            refraction / isa[1]
        }
        val byProfile = synchronized(thresholdCache) { thresholdCache.getOrPut(profile) { lruMap(64) } }
        val key = listOf(atm.key, scale, ho, floorM, R, terrainRays, terrainImages, earlySunImages).joinToString("|")
        synchronized(byProfile) { byProfile[key] }?.let { return it }
        val th = computeThresholds(profile, atm, ho, floorM, R, scale)
        synchronized(byProfile) { byProfile[key] = th }
        return th
    }

    private fun computeThresholds(profile: HorizonProfile, atm: Medium, ho: Double, floorM: Double, R: Double, scale: Double): Thresholds {
        val seaAlt = atm.seaHorizon(ho, R, floorM)
        val seaAlts = HashMap<Double, Double>()
        seaAlts[floorM] = seaAlt
        fun seaAltOver(fl: Double) = seaAlts.getOrPut(fl) { atm.seaHorizon(ho, R, fl) }
        class P(val az: Double, val app: Double)
        val pts = profile.points.map { p ->
            var sea = seaAlt
            val app = when (p) {
                is ObservedHorizonPoint -> p.elevationDeg
                is DemHorizonPoint -> {
                    val fl = p.floorM?.takeIf { it.isFinite() } ?: floorM
                    sea = seaAltOver(fl)
                    fun elevation(hm: Double, dm: Double) = if (terrainRays) Refraction.terrainElevation(atm, ho, hm, dm, R, fl, terrainImages)
                    else Refraction.geometricElevation(ho, hm, dm, R) + Refraction.terrestrialRefraction(atm, ho, hm, kotlin.math.hypot(dm, (R + hm) * cos(dm / R) - (R + ho)))
                    var a = elevation(p.heightM, p.distanceKm * 1000)
                    for (alt in p.alternates) a = max(a, elevation(alt[1], alt[0] * 1000))
                    a
                }
            }
            P(((p.azimuthDeg % 360) + 360) % 360, max(app, sea))
        }.sortedBy { it.az }
        require(pts.size >= 2) { "Horizon profile needs at least two points" }
        val maxApp = pts.maxOf { it.app }
        val table = refractionTable(atm, ho, R, seaAlt, maxApp, floorM)
        val A = table.a
        val tab = table.refr
        val N = tab.size
        fun slope(k: Int): Double {
            if (k <= 0) return (tab[1] - tab[0]) / (A[1] - A[0])
            if (k >= N - 1) return (tab[N - 1] - tab[N - 2]) / (A[N - 1] - A[N - 2])
            val h0 = A[k] - A[k - 1]
            val h1 = A[k + 1] - A[k]
            return ((tab[k] - tab[k - 1]) / h0 * h1 + (tab[k + 1] - tab[k]) / h1 * h0) / (h0 + h1)
        }
        fun refrAt(a: Double): Double {
            var i = 0
            var j = N - 1
            if (a <= A[0]) j = 1
            else if (a >= A[N - 1]) i = N - 2
            else while (j - i > 1) { val k = (i + j) shr 1; if (A[k] <= a) i = k else j = k }
            i = min(i, N - 2)
            if (table.rough[i]) return scale * atm.refraction(ho, a, R, floorM)
            val h = A[i + 1] - A[i]
            val t = (a - A[i]) / h
            val t2 = t * t
            val t3 = t2 * t
            val v = (2 * t3 - 3 * t2 + 1) * tab[i] + (t3 - 2 * t2 + t) * h * slope(i) + (-2 * t3 + 3 * t2) * tab[i + 1] + (t3 - t2) * h * slope(i + 1)
            return scale * (if (v.isFinite()) v else atm.refraction(ho, a, R, floorM))
        }
        var lowestAbove: DoubleArray? = null
        if (earlySunImages) {
            val la = DoubleArray(N + 1) { Double.POSITIVE_INFINITY }
            for (k in N - 1 downTo 0) la[k] = min(la[k + 1], if (tab[k].isFinite()) A[k] - scale * tab[k] else Double.POSITIVE_INFINITY)
            lowestAbove = la
        }
        fun threshold(app: Double): Double {
            val t = app - refrAt(app)
            val la = lowestAbove ?: return t
            var k = 0
            while (k < N && A[k] <= app) k++
            return min(t, la[k])
        }
        return Thresholds(DoubleArray(pts.size) { pts[it].az }, DoubleArray(pts.size) { pts[it].app }, DoubleArray(pts.size) { threshold(pts[it].app) })
    }

    /** Refraction versus apparent altitude from the sea horizon up, every 0.1 deg, refined near the horizon. */
    private fun refractionTable(atm: Medium, ho: Double, R: Double, seaAlt: Double, maxApp: Double, floorM: Double): RefractionTable {
        val step = 0.1
        val top = max(maxApp, seaAlt) + 0.5
        val n = kotlin.math.ceil((top - seaAlt) / step).toInt() + 1
        val key = listOf(atm.key, ho, R, n, floorM).joinToString("|")
        synchronized(refrTableCache) { refrTableCache[key] }?.let { return it }
        fun at(a: Double) = atm.refraction(ho, a, R, floorM)
        val nodes = ArrayList<DoubleArray>()
        for (i in 0 until n) {
            val a = if (i == 0 && ho > floorM) seaAlt + 1e-7 else seaAlt + i * step
            nodes.add(doubleArrayOf(a, at(a)))
        }
        val out = ArrayList<DoubleArray>()
        out.add(nodes[0])
        val rough = ArrayList<Int>()
        fun refine(p: DoubleArray, q: DoubleArray, depth: Int) {
            if (p[0] < seaAlt + 1) {
                val am = (p[0] + q[0]) / 2
                val m = doubleArrayOf(am, at(am))
                val lin = (p[1] + q[1]) / 2
                if (!(abs(m[1] - lin) <= 1e-4)) {
                    if (depth < 5) { refine(p, m, depth + 1); refine(m, q, depth + 1); return }
                    out.add(m); rough.add(out.size - 2); rough.add(out.size - 1)
                }
            }
            out.add(q)
        }
        for (i in 0 until n - 1) refine(nodes[i], nodes[i + 1], 0)
        val flags = BooleanArray(out.size)
        for (i in rough) flags[i] = true
        val table = RefractionTable(DoubleArray(out.size) { out[it][0] }, DoubleArray(out.size) { out[it][1] }, flags)
        synchronized(refrTableCache) { refrTableCache[key] = table }
        return table
    }

    private fun visibleEvent(date: LocalDate, geo: GeoLocation, profile: HorizonProfile, event: SolarEvent, options: VisibleOptions, observerHeightM: Double?, groundM: Double?): Double {
        val th = getHorizonThresholds(date, geo, profile, event, observerHeightM, groundM)
        val sd = getApparentSolarRadius(date)
        val single = when (options.limb) { Limb.TOP -> PI / 2; Limb.BOTTOM -> -PI / 2; else -> null }
        val nLimb = if (single != null) 1 else options.limbPoints
        val all = options.limb == Limb.FULL
        val fine = options.stepSeconds / 86400
        val az = th.azimuthDeg
        val thr = th.thresholdDeg
        val n = az.size
        val maxGap = options.maxGapDeg ?: 10.0
        fun thresholdAt(a0: Double): Double {
            val a = ((a0 % 360) + 360) % 360
            var lo: Int
            var hi: Int
            if (a < az[0] || a >= az[n - 1]) { lo = n - 1; hi = 0 } else {
                lo = 0; hi = n - 1
                while (hi - lo > 1) { val m = (lo + hi) shr 1; if (az[m] <= a) lo = m else hi = m }
            }
            val gap = ((az[hi] - az[lo]) % 360 + 360) % 360
            if (gap == 0.0) return max(thr[lo], thr[hi])
            if (gap > maxGap) return Double.NaN
            val off = ((a - az[lo]) % 360 + 360) % 360
            return thr[lo] + (thr[hi] - thr[lo]) * off / gap
        }
        var outside = false
        fun visible(jd: Double): Boolean {
            val st = sunTrue(jd, geo)
            val alt = st[0]
            val azi = st[1]
            val c = max(cos(alt * DEG), 1e-6)
            for (i in 0 until nLimb) {
                val t = single ?: (2 * PI * i / nLimb)
                val lim = thresholdAt(azi + sd * cos(t) / c)
                if (lim.isNaN()) { outside = true; if (all) return false; continue }
                val clear = alt + sd * sin(t) >= lim
                if (clear && !all) return true
                if (!clear && all) return false
            }
            return all
        }
        val solarDate = solarDate(date, geo)
        val jd0 = julianDay(solarDate)
        val approx = jd0 + 0.5 - geo.longitude / 360
        val nh = getUTCNoon(solarDate, geo) / 24
        var jdNoon = jd0 + nh
        for (k in intArrayOf(-1, 1)) if (abs(jd0 + nh + k - approx) < abs(jdNoon - approx)) jdNoon = jd0 + nh + k
        val dir = if (event == SolarEvent.SUNRISE) 1 else -1
        val minThr = thr.min()
        val coarse = 5.0 / 1440
        val margin = 1.5
        var jd = jdNoon - dir * 0.5
        if (visible(jd)) return Double.NaN
        fun stillBelow(x: Double) = dir * (jdNoon - x) > 0 && sunTrue(x + dir * coarse, geo)[0] < minThr - sd - margin
        val steps = ArrayList<Double>()
        steps.add(jd)
        while (dir * (jdNoon - steps.last()) > 0) steps.add(steps.last() + dir * coarse)
        if (stillBelow(steps[0])) {
            var lo = 0
            var hi = steps.size - 1
            while (hi - lo > 1) { val mid = (lo + hi) shr 1; if (stillBelow(steps[mid])) lo = mid else hi = mid }
            jd = steps[hi]
        }
        var prev = jd
        jd = prev + dir * fine
        while (dir * (jdNoon - jd) > 0) {
            if (visible(jd)) break
            prev = jd
            jd += dir * fine
        }
        if (dir * (jdNoon - jd) <= 0) return Double.NaN
        if (outside && thresholdAt(sunTrue(jd, geo)[1]).isNaN()) return Double.NaN
        var a = prev
        var b = jd
        var i = 0
        while (i < 30 && abs(b - a) * 86400 > 1e-3) { val m = (a + b) / 2; if (visible(m)) b = m else a = m; i++ }
        var ms = (b - JD_UNIX_EPOCH) * 86400000
        if (options.inversionCorrection && event == SolarEvent.SUNRISE && inversionLikely(date, geo, th, b)) ms -= 15000
        return ms
    }

    /** Keller & Hall's inversion criteria: night longer than 13 h, and a horizon within 0.01 deg of level where the Sun rises. */
    private fun inversionLikely(date: LocalDate, geo: GeoLocation, th: Thresholds, jd: Double): Boolean {
        val rise = getUTCSunrise(date, geo, GEOMETRIC_ZENITH, false)
        val set = getUTCSunset(date.minusDays(1), geo, GEOMETRIC_ZENITH, false)
        val night = ((rise - set) % 24 + 24) % 24
        if (!(night > 13)) return false
        val az = sunTrue(jd, geo)[1]
        var k = 0
        for (i in 1 until th.azimuthDeg.size) if (abs(th.azimuthDeg[i] - az) < abs(th.azimuthDeg[k] - az)) k = i
        return th.apparentDeg[k] <= 0.01
    }

    companion object {
        const val ROY_REFRACTION_ARCMIN = 34.478885263888294
        private const val TAG = "ROYSPACalculator"
        private const val GEOMETRIC_ZENITH = 90.0
        private const val DEG = PI / 180
        const val JD_UNIX_EPOCH = 2440587.5

        private fun sinD(a: Double) = sin(Math.toRadians(a))
        private fun cosD(a: Double) = cos(Math.toRadians(a))
        private fun tanD(a: Double) = tan(Math.toRadians(a))

        private fun meanObliquity(t: Double) = 84381.406 + t * (-46.836769 + t * (-0.0001831 + t * (0.0020034 + t * (-0.000000576 - t * 0.0000000434))))

        private fun greenwichMeanSiderealTime(jdUT: Double, t: Double): Double {
            val era = 360 * ((0.7790572732640 + 0.00273781191135448 * (jdUT - 2451545) + jdUT % 1) % 1)
            return era + (0.014506 + t * (4612.156534 + t * (1.3915817 + t * (-0.00000044 + t * (-0.000029956 - t * 0.0000000368))))) / 3600
        }

        /** Julian day at 0h UT of a calendar date (Meeus ch. 7). */
        @JvmStatic
        fun julianDay(d: LocalDate): Double {
            var y = d.year
            var m = d.monthValue
            if (m <= 2) { y -= 1; m += 12 }
            val a = floor(y / 100.0)
            val b = 2 - a + floor(a / 4)
            return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d.dayOfMonth + b - 1524.5
        }

        @JvmStatic
        fun julianDayFromInstant(instant: Instant): Double {
            val utc = instant.atZone(ZoneOffset.UTC)
            val frac = (utc.hour + (utc.minute + (utc.second + utc.nano / 1e9) / 60.0) / 60.0) / 24.0
            return julianDay(utc.toLocalDate()) + frac
        }

        internal fun <K, V> lru(limit: Int): LinkedHashMap<K, V> = object : LinkedHashMap<K, V>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > limit
        }

        private fun <K, V> lruMap(limit: Int): MutableMap<K, V> = lru(limit)
    }
}
