package com.ej.rovadiahyosefcalendar.classes.royzmanim

import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** Against the moonrise project's moon-calc.ts (dist/moonRiseSetCalc.js run in Node), Jerusalem at 754 m, lit mode. */
class MoonCalcTest {
    private val obs = Observer(31.778, 35.2354, 754.0)

    private fun at(iso: String) = Time.fromMillisecondsSince1970(Instant.parse(iso).toEpochMilli())
    private fun ms(t: Time?) = t!!.toMillisecondsSince1970().toDouble()

    @Test
    fun matchesTheTypeScript() {
        val c = MoonCalc()
        assertEquals(Instant.parse("2026-11-05T00:08:24.673Z").toEpochMilli().toDouble(), ms(c.nextMoonEvent(1, at("2026-11-05T00:00:00Z"), obs, limitDays = 1.0)), 500.0)
        assertEquals(Instant.parse("2026-11-05T12:25:46.917Z").toEpochMilli().toDouble(), ms(c.nextMoonEvent(-1, at("2026-11-05T00:00:00Z"), obs, limitDays = 1.0)), 500.0)
        assertEquals(Instant.parse("2026-11-20T11:38:20.876Z").toEpochMilli().toDouble(), ms(c.nextMoonEvent(1, at("2026-11-20T00:00:00Z"), obs, limitDays = 1.0)), 500.0)
        assertNull(c.nextMoonEvent(-1, at("2026-11-20T00:00:00Z"), obs, limitDays = 1.0))
    }
}
