package com.ej.rovadiahyosefcalendar.classes.royzmanim

/** A point of a terrain horizon, by azimuth (degrees east of north). */
sealed interface HorizonPoint { val azimuthDeg: Double }

/**
 * A horizon point from a DEM: the highest-angle terrain point at this azimuth, its distance along the surface and its height
 * above sea level. Its apparent elevation is found by tracing the ray to it. alternates: (distanceKm, heightM) of other points
 * that would be the highest in other air. floorM: the lowest surface the line of sight crosses (rays come down no lower).
 */
class DemHorizonPoint(
    override val azimuthDeg: Double,
    val distanceKm: Double,
    val heightM: Double,
    val alternates: List<DoubleArray> = emptyList(),
    val floorM: Double? = null,
) : HorizonPoint

/** A measured (theodolite / photo) apparent elevation angle; used as-is (refraction already included). */
class ObservedHorizonPoint(override val azimuthDeg: Double, val elevationDeg: Double) : HorizonPoint

/** What a visible sunrise / sunset is computed over: a profile for this observer, or the server's HorizonSet. */
sealed interface Horizon

/** Horizon points for this observer. A class (not a list) so the calculator's caches can key it by identity. */
class HorizonProfile(val points: List<HorizonPoint>) : Horizon

/**
 * A vantage point of a HorizonSet: its position, ground and eye height (may be below sea level), and its horizon only around
 * the directions it wins (wins: event name -> [lo, hi] azimuth windows).
 */
class HorizonSetPoint(
    val lat: Double,
    val lon: Double,
    val ground: Double,
    val height: Double,
    val sunrise: HorizonProfile?,
    val sunset: HorizonProfile?,
    val wins: Map<SolarEvent, List<DoubleArray>> = emptyMap(),
) {
    fun profile(event: SolarEvent) = if (event == SolarEvent.SUNRISE) sunrise else sunset
}

/** What the refraction server's /v1/horizon returns: the exact point ("point"), or vantage points within a radius ("vantage"). */
class HorizonSet(val mode: String, val points: List<HorizonSetPoint>, val algorithm: Int? = null) : Horizon

/** Which part of the Sun's disk decides a visible sunrise / sunset. */
enum class Limb {
    /** any part of the disk: the first / last light an observer sees */
    ANY,
    /** the top of the disk (the upper limb) */
    TOP,
    /** the bottom of the disk (the lower limb) */
    BOTTOM,
    /** the whole disk */
    FULL
}

/**
 * limbPoints: points tested around the solar limb; stepSeconds: scan step; maxGapDeg: azimuth gaps wider than this count as
 * unknown (default 10, 0.5 for a HorizonSet); inversionCorrection: Keller & Hall's (2022) dawn inversion correction;
 * observerHeightM: the eye's height above sea level when the GeoLocation can't hold it (below sea level).
 */
data class VisibleOptions @JvmOverloads constructor(
    val limbPoints: Int = 24,
    val stepSeconds: Double = 10.0,
    val maxGapDeg: Double? = null,
    val inversionCorrection: Boolean = false,
    val limb: Limb = Limb.ANY,
    val observerHeightM: Double? = null,
)

/** A visible event from a vantage point. */
data class VisibleEvent(val epochMs: Double, val lat: Double, val lon: Double, val height: Double)
