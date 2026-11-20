package com.ej.rovadiahyosefcalendar.classes.royzmanim

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

/** Which stored place serves a new GPS fix: each kind of data by its own source's cell, never a shared distance. */
class RefractionRepositoryPlacesTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var dir: File

    private fun field(name: String) = RefractionRepository::class.java.getDeclaredField(name).apply { isAccessible = true }

    @Before
    fun reset() {
        dir = tmp.newFolder("refraction-v2")
        field("dir").set(null, dir)
        field("knownLoaded").setBoolean(null, false)
        for (m in listOf("known", "cells", "memory")) (field(m).get(null) as MutableMap<*, *>).clear()
    }

    private fun name(lat: Double, lon: Double, kind: String) = String.format(Locale.ROOT, "%.5f_%.5f.%s.json", lat, lon, kind)
    private fun write(lat: Double, lon: Double, kind: String, j: JSONObject) = File(dir, name(lat, lon, kind)).writeText(j.toString())

    private fun storeForecast(lat: Double, lon: Double, cellLat: Double, cellLon: Double) {
        write(lat, lon, "forecast", JSONObject().put("fetchedAt", System.currentTimeMillis()).put("table", JSONObject()))
        write(lat, lon, "cells", JSONObject().put("forecast", JSONArray().put(cellLat).put(cellLon)))
    }

    @Test
    fun forecastServesInsideItsModelCellOnly() {
        // Jerusalem: ICON-EU, 0.0625 degree cells; the circle inside one is about 2.95 km (the stored fix is 2 km from the centre)
        storeForecast(31.7780, 35.2354, 31.7650, 35.2500)
        val stored = RefractionRepository.cached(31.7780, 35.2354)
        assertNotNull(stored)
        assertEquals(stored!!.forecastFetchedAt, RefractionRepository.cached(31.7600, 35.2400)?.forecastFetchedAt) // 1.1 km from the centre
        assertNull(RefractionRepository.cached(31.7950, 35.2500)) // 3.3 km from the centre: maybe the next cell
    }

    @Test
    fun forecastWhoseSourceReportedNoCellServesWithinGpsNoise() {
        write(31.7780, 35.2354, "forecast", JSONObject().put("fetchedAt", System.currentTimeMillis()).put("table", JSONObject()))
        assertNotNull(RefractionRepository.cached(31.77815, 35.2354)) // 17 m
        assertNull(RefractionRepository.cached(31.7785, 35.2354)) // 56 m
    }

    @Test
    fun horizonServesItsWholeAreaAndTheSmallestAreaWins() {
        val horizon = JSONObject(javaClass.classLoader!!.getResource("horizon-jerusalem.json")!!.readText())
        fun area(name: String, s: Double, w: Double, n: Double, e: Double) =
            JSONObject(horizon.toString()).put("area", JSONObject().put("name", name).put("bbox", JSONArray(listOf(s, w, n, e))))
        fun store(lat: Double, lon: Double, h: JSONObject) {
            write(lat, lon, "static", JSONObject().put("horizon", h))
            write(lat, lon, "cells", JSONObject().put("horizon", JSONObject().put("hasHorizon", true).put("bbox", h.getJSONObject("area").getJSONArray("bbox"))))
        }
        store(31.7780, 35.2350, area("City", 31.70, 35.15, 31.85, 35.30))
        store(31.7700, 35.2200, area("Neighbourhood", 31.765, 35.215, 31.775, 35.225))
        assertEquals("City", RefractionRepository.cached(31.8200, 35.1800)?.horizonAreaName) // 5 km away, same city
        assertEquals("Neighbourhood", RefractionRepository.cached(31.7710, 35.2210)?.horizonAreaName) // inside both boxes
        assertNull(RefractionRepository.cached(31.9000, 35.2350)) // outside the city
    }

    @Test
    fun horizonFollowsTheAreasOutlineNotItsBox() {
        val horizon = JSONObject(javaClass.classLoader!!.getResource("horizon-jerusalem.json")!!.readText())
        fun store(lat: Double, lon: Double, name: String, bbox: List<Double>, ring: List<Double>?) {
            val area = JSONObject().put("name", name).put("bbox", JSONArray(bbox))
            if (ring != null) area.put("polygons", JSONArray().put(JSONArray().put(JSONArray(ring))))
            write(lat, lon, "static", JSONObject().put("horizon", JSONObject(horizon.toString()).put("area", area)))
            // what load() records of a horizon the server sent with outline=1
            val cells = JSONObject().put("hasHorizon", true).put("bbox", area.getJSONArray("bbox"))
            area.optJSONArray("polygons")?.let { cells.put("polygons", it) }
            write(lat, lon, "cells", JSONObject().put("horizon", cells))
        }
        store(31.7780, 35.2350, "City", listOf(31.70, 35.15, 31.85, 35.30), null)
        // an L: the whole west half of its box, and only the south half of the east
        store(31.7670, 35.2170, "Neighbourhood", listOf(31.765, 35.215, 31.775, 35.225),
            listOf(35.215, 31.765, 35.225, 31.765, 35.225, 31.770, 35.220, 31.770, 35.220, 31.775, 35.215, 31.775))
        assertEquals("Neighbourhood", RefractionRepository.cached(31.7670, 35.2230)?.horizonAreaName) // in the L's foot
        assertEquals("City", RefractionRepository.cached(31.7730, 35.2230)?.horizonAreaName) // in the box, outside the outline
    }

    @Test
    fun lightPollutionServesItsMapPixelOnly() {
        // the server's atlas rows run south from 85.0541669 N in 30" steps: 31.7780 and 31.7790 are row 6393, 31.7840 is row 6392
        write(31.7780, 35.2354, "sky", JSONObject().put("blpCdM2", 0.0012))
        val known = field("known").get(null)
        assertNotNull(known)
        RefractionRepository.cached(31.7780, 35.2354) // loads what is stored
        val places = RefractionRepository::class.java.getDeclaredMethod("places", Double::class.java, Double::class.java).apply { isAccessible = true }
        fun skyId(lat: Double, lon: Double): String {
            val p = places.invoke(RefractionRepository, lat, lon)!!
            val sky = p.javaClass.getDeclaredMethod("getSky").invoke(p)!!
            return sky.javaClass.getDeclaredMethod("getId").invoke(sky) as String
        }
        assertEquals("31.77800,35.23540", skyId(31.7790, 35.2360))
        assertEquals("31.78400,35.23540", skyId(31.7840, 35.2354))
    }
}
