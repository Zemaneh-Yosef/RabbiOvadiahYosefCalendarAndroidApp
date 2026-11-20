package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.kosherjava.zmanim.ZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import com.kosherjava.zmanim.util.SPACalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class ROYSPACalculatorTest {
    private val jerusalem = GeoLocation("Jerusalem", 31.778, 35.2354, 754.0, ZoneId.of("Asia/Jerusalem"))

    private fun calendar(calc: com.kosherjava.zmanim.util.AstronomicalCalculator, date: LocalDate) =
        ZmanimCalendar(jerusalem).apply { astronomicalCalculator = calc; localDate = date }

    /** The corrected Sun's place moves sunrise / sunset by well under a second; the 34.48' refraction by ~2.5 s at sea level. */
    @Test
    fun seaLevelCloseToKosherJava() {
        val roy = ROYSPACalculator(34.0).apply { configureForLocation(jerusalem) }
        val kj = SPACalculator()
        for (date in listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))) {
            val a = calendar(roy, date).seaLevelSunrise
            val b = calendar(kj, date).seaLevelSunrise
            val diff = Duration.between(b, a).toMillis() / 1000.0
            println("$date sea-level sunrise ROY $a KJ $b diff ${diff}s")
            assertTrue("sea-level sunrise within 2 s of KosherJava's SPA ($diff)", kotlin.math.abs(diff) < 2)
            val dawn = Duration.between(calendar(kj, date).getSunriseOffsetByDegrees(106.04), calendar(roy, date).getSunriseOffsetByDegrees(106.04)).toMillis() / 1000.0
            assertTrue("16.04 deg dawn within 2 s ($dawn)", kotlin.math.abs(dawn) < 2)
        }
    }

    /** Elevated sunrise: the physical depression is ~5.7' deeper than KosherJava's at 800 m, so sunrise comes ~25 s earlier. */
    @Test
    fun elevatedSunriseEarlierThanGeometricDip() {
        val date = LocalDate.of(2026, 3, 20)
        val roy = ROYSPACalculator().apply { configureForLocation(jerusalem) }
        val kj = ROYSPACalculator(elevationModel = ROYSPACalculator.ElevationModel.KOSHERJAVA).apply { configureForLocation(jerusalem) }
        val d = Duration.between(calendar(kj, date).sunrise, calendar(roy, date).sunrise).toMillis() / 1000.0
        println("elevated sunrise physical - kosherjava: ${d}s")
        assertTrue(d < -15 && d > -40)
    }

    /** A one-temperature atmosphere (ray traced) at ISA gives a horizon refraction near 33', i.e. sunrise later than 34.48'. */
    @Test
    fun rayTracedStandardAir() {
        val date = LocalDate.of(2026, 3, 20)
        val plain = ROYSPACalculator().apply { configureForLocation(jerusalem) }
        val traced = ROYSPACalculator().apply {
            configureForLocation(jerusalem)
            setAtmosphereProvider { _, _, _ -> SurfaceAir(15.0, 1013.25, 0.0) }
        }
        val d = Duration.between(calendar(plain, date).seaLevelSunrise, calendar(traced, date).seaLevelSunrise).toMillis() / 1000.0
        println("ISA traced - 34.48': ${d}s")
        assertTrue(d > 3 && d < 15)
    }

    @Test
    fun deltaT2026() {
        assertEquals(69.1, DeltaT.deltaT(2461041.5), 0.3)
    }

    /** The earliest nightfall of the year in Jerusalem, moonless sky, ~18 min after sea-level sunset. */
    @Test
    fun starNightfallJerusalem() {
        val date = LocalDate.of(2026, 1, 25)
        val aod = StarNightfall.defaultAod(754.0)
        val n = StarNightfall.starNightfall(date, 31.778, 35.2354, StarNightfall.Options(aod = aod, elevationM = 754.0, marginMag = 0.2992))
        val sunset = StarNightfall.eveningSunAt(date, 31.778, 35.2354, -0.833)
        println("nightfall ${(n.ms - sunset) / 60000} min after sunset, reached ${n.reached}")
        assertTrue(n.reached && (n.ms - sunset) / 60000 in 12.0..30.0)
    }
}
