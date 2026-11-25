package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.kosherjava.zmanim.util.GeoLocation
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** The website's changes of 5-22 November 2026, as ported. */
class WebsiteNov2026Test {

    private fun point(lat: Double, lon: Double, height: Double, km: Double, hM: Double) = JSONObject()
        .put("lat", lat).put("lon", lon).put("ground", height).put("height", height)
        .put("sunrise", JSONArray().put(JSONObject().put("azimuthDeg", 90).put("distanceKm", km).put("heightM", hM)))

    @Test
    fun horizonIsRefusedWhenNotThisPlacesTerrain() {
        val good = JSONObject().put("points", JSONArray().put(point(40.0, -74.0, 10.0, 20.0, 300.0)))
        assertNull(AtmosphereGuard.horizonProblem(good, 40.0, -74.0))
        // another place's horizon (more than 5 km away)
        assertNotNull(AtmosphereGuard.horizonProblem(good, 40.5, -74.0))
        // a wall 2 km high 1 km away: 63 deg up
        val wall = JSONObject().put("points", JSONArray().put(point(40.0, -74.0, 10.0, 1.0, 2000.0)))
        assertNotNull(AtmosphereGuard.horizonProblem(wall, 40.0, -74.0))
        // terrain 600 km away
        val far = JSONObject().put("points", JSONArray().put(point(40.0, -74.0, 10.0, 600.0, 300.0)))
        assertNotNull(AtmosphereGuard.horizonProblem(far, 40.0, -74.0))
    }

    @Test
    fun climateEnvelopeRefusesAirOutsideThePlacesClimate() {
        val geo = GeoLocation("NY", 40.7, -74.0, 10.0, ZoneId.of("America/New_York"))
        val july = LocalDate.of(2026, 7, 15)
        val cold = SurfaceAir(-60.0, heightM = 10.0, source = "forecast")
        // nothing loaded for the cell: nothing is checked
        assertNull(AtmosphereGuard.anomalyProblem(cold, july, geo))
        val e = ClimateEnvelope.parse(JSONObject()
            .put("cell", JSONObject().put("south", 40).put("west", -75).put("res", 5))
            .put("seaLevelC", JSONObject().put("min", JSONArray(DoubleArray(12) { -10.0 + it }.toList())).put("max", JSONArray(DoubleArray(12) { 25.0 + it }.toList())))
            .put("lapseKPerM", 0.0065))
        assertNotNull(e)
        ClimateEnvelope.set(e!!)
        assertEquals("40,-75", ClimateEnvelope.cellId(40.7, -74.0))
        assertNotNull(AtmosphereGuard.anomalyProblem(cold, july, geo))
        assertNull(AtmosphereGuard.anomalyProblem(SurfaceAir(28.0, heightM = 10.0, source = "forecast"), july, geo))
        // the place's normal, when there is one, is the reference
        assertNull(AtmosphereGuard.anomalyProblem(cold, july, geo, -30.0))
    }

    @Test
    fun realHeightsChangeNothingWithoutAHorizonElevation() {
        val geo = GeoLocation("Denver", 39.74, -104.99, 1600.0, ZoneId.of("America/Denver"))
        val date = LocalDate.of(2026, 6, 21)
        val off = ROYSPACalculator().apply { configureForLocation(geo) }
        val on = ROYSPACalculator().apply { configureForLocation(geo); setRefractionAtRealHeights(true) }
        assertEquals(off.getUTCSunset(date, geo, 90.0, true), on.getUTCSunset(date, geo, 90.0, true), 1e-12)
        // with the base at 1600 m too (the eye 0 m above it) the real-heights trace moves the elevated sunset
        val raised = GeoLocation("Denver", 39.74, -104.99, 1600.0, ZoneId.of("America/Denver")).apply { horizonElevationAtSunset = 1590.0 }
        val a = off.getUTCSunset(date, raised, 90.0, true)
        val b = on.getUTCSunset(date, raised, 90.0, true)
        assertTrue("real heights should move it (${(a - b) * 3600} s)", Math.abs(a - b) * 3600 > 1)
    }
}
