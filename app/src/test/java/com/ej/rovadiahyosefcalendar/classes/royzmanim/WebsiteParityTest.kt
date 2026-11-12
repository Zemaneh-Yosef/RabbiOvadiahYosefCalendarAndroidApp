package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.kosherjava.zmanim.util.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The port against the website's own JavaScript (values printed by running royzmanim-spa-corrections.js, star-nightfall.js
 * and horizon-glow.js in Node with a Temporal polyfill, 2026-03-20 / 2026-01-25, Jerusalem).
 */
class WebsiteParityTest {
    private val geo = GeoLocation("J", 31.778, 35.2354, 754.0, ZoneId.of("Asia/Jerusalem"))
    private val date = LocalDate.of(2026, 3, 20)

    private fun assertMs(expectedIso: String, ms: Double, toleranceMs: Double) =
        assertEquals("$expectedIso vs ${Instant.ofEpochMilli(ms.toLong())}", Instant.parse(expectedIso).toEpochMilli().toDouble(), ms, toleranceMs)

    @Test
    fun sunrises() {
        val calc = ROYSPACalculator().apply { configureForLocation(geo) }
        val r = calc.getSunrises(date, geo)
        assertMs("2026-03-20T03:43:05.651Z", r[0], 50.0)
        assertMs("2026-03-20T03:38:31.184Z", r[1], 50.0)
        calc.setAtmosphereProvider { _, _, _ -> SurfaceAir(15.0, 1013.25, 0.0) }
        assertMs("2026-03-20T03:43:12.764Z", calc.getSunrises(date, geo)[0], 50.0)
    }

    @Test
    fun visibleSunriseOverTheServersHorizon() {
        val json = org.json.JSONObject(javaClass.classLoader!!.getResource("horizon-jerusalem.json")!!.readText())
        val set = HorizonClient.parseSet(json)
        val g2 = GeoLocation("J", 31.778, 35.235, 744.5, ZoneId.of("Asia/Jerusalem"))
        val calc = ROYSPACalculator().apply { configureForLocation(g2) }
        assertMs("2026-03-20T03:55:08.657Z", calc.getVisibleSunrise(date, g2, set), 50.0)
        assertMs("2026-03-20T03:57:25.995Z", calc.getVisibleSunrise(date, g2, set, VisibleOptions(limb = Limb.FULL)), 50.0)
    }

    @Test
    fun hazeDelay() {
        val d = LocalDate.of(2026, 1, 25)
        val hd = StarNightfall.hazeDelay(d, 31.778, 35.2354, StarNightfall.Options(aod = 0.4, elevationM = 754.0), 0.15)
        assertEquals(2.5625, hd.ms / 60000, 0.01)
        assertEquals(StarNightfall.HazeDelayStatus.MODELED, hd.status)
    }
}
