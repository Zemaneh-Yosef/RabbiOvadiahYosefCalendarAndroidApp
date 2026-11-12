package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.ej.rovadiahyosefcalendar.classes.royzmanim.StarNightfall.HazeSeries
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.round

/**
 * Haze (aerosol optical depth at 550 nm) for nightfall by the stars, from Open-Meteo's air-quality API (CAMS global), with
 * NOAA's GEFS-Aerosols as the forecast's fallback; plus the refraction server's AERONET calibration and light pollution
 * (ports of open-meteo-haze.js, light-pollution-client.js and refraction-data.js's haze calibration).
 */
object HazeClient {
    private const val AQ_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
    const val GEFS_AEROSOL_URL = "https://oceanwatch.aoml.noaa.gov/erddap/griddap/GEFS_Aerosols_bfd8_ee31_ed8b.json"
    const val HAZE_HISTORY_START = "2022-08-01"

    private fun f4(x: Double) = String.format(Locale.ROOT, "%.4f", x)

    private fun guardAod(vals: DoubleArray, source: String): DoubleArray {
        var bad = 0
        val out = DoubleArray(vals.size) { i ->
            val v = vals[i]
            if (v.isNaN()) Double.NaN
            else if (AtmosphereGuard.plausibleAod(v) == null) { bad++; Double.NaN }
            else round(v * 1000) / 1000
        }
        if (bad > 0) AtmosphereGuard.report("$source: $bad haze values out of range")
        return out
    }

    private fun seriesOf(j: JSONObject, lat: Double, lon: Double): HazeSeries {
        val h = j.optJSONObject("hourly")
        val times = h?.optJSONArray("time")
        val vals = h?.optJSONArray("aerosol_optical_depth")
        if (times == null || times.length() < 2) throw WeatherHttpException("Open-Meteo returned no haze")
        val raw = DoubleArray(times.length()) { WeatherHttp.num(vals, it) }
        return HazeSeries(lat, lon, times.getLong(0).toDouble(), (times.getLong(1) - times.getLong(0)).toDouble(), guardAod(raw, "CAMS via Open-Meteo"))
    }

    /** cell: the centre of the CAMS grid cell Open-Meteo answered from (lat, lon), when it said. */
    class HazeNormals(val normals: HazeSeries, val normalsRange: Pair<String, String>, val source: String, val cell: DoubleArray? = null)

    /** CAMS's archive at a place, hourly (August 2022 to the end of last month), for the averages. */
    @JvmStatic
    fun fetchHazeNormals(latitude: Double, longitude: Double): HazeNormals {
        val now = LocalDate.now(ZoneOffset.UTC)
        val end = now.withDayOfMonth(1).minusDays(1).toString()
        val url = "$AQ_URL?latitude=${f4(latitude)}&longitude=${f4(longitude)}&hourly=aerosol_optical_depth&start_date=$HAZE_HISTORY_START&end_date=$end&timeformat=unixtime&timezone=GMT"
        val j = WeatherHttp.getOpenMeteo(url, timeoutMs = 60000) as JSONObject
        return HazeNormals(seriesOf(j, latitude, longitude), Pair(HAZE_HISTORY_START, end), "CAMS global via Open-Meteo air-quality API", WeatherHttp.reportedCell(j))
    }

    /** CAMS's forecast haze, hourly, yesterday to `days` ahead. */
    @JvmStatic
    @JvmOverloads
    fun fetchHazeForecast(latitude: Double, longitude: Double, days: Int = 7): HazeSeries {
        val url = "$AQ_URL?latitude=${f4(latitude)}&longitude=${f4(longitude)}&hourly=aerosol_optical_depth&past_days=1&forecast_days=${days.coerceIn(1, 7)}&timeformat=unixtime&timezone=GMT"
        return seriesOf(WeatherHttp.getOpenMeteo(url) as JSONObject, latitude, longitude)
    }

    /** Forecast haze from NOAA's GEFS-Aerosols (the fallback), onto an hourly grid. */
    @JvmStatic
    fun fetchGefsHazeForecast(latitude: Double, longitude: Double, retries: Int = 1): HazeSeries {
        val lat = Math.round(latitude * 4) / 4.0
        val lon = ((Math.round(longitude * 4) / 4.0) % 360 + 360) % 360
        val url = "$GEFS_AEROSOL_URL?AOTK_entireatmosphere%5B0:1:last%5D%5B($lat)%5D%5B($lon)%5D"
        var j: JSONObject? = null
        var attempt = 0
        while (j == null) {
            try {
                j = WeatherHttp.getJson(url, 30000, "GEFS-Aerosols request")
            } catch (e: Exception) {
                if (attempt >= retries) throw e
                Thread.sleep(2000L * (attempt + 1))
                attempt++
            }
        }
        val table = j.optJSONObject("table")
        val cols = table?.optJSONArray("columnNames") ?: JSONArray()
        val rows = table?.optJSONArray("rows") ?: JSONArray()
        val names = (0 until cols.length()).map { cols.getString(it) }
        val it = names.indexOf("time")
        val iv = names.indexOf("AOTK_entireatmosphere")
        if (it < 0 || iv < 0 || rows.length() < 2) throw WeatherHttpException("GEFS-Aerosols returned no data")
        val pts = (0 until rows.length()).mapNotNull { k ->
            val r = rows.getJSONArray(k)
            val t = try { OffsetDateTime.parse(r.getString(it)).toEpochSecond().toDouble() } catch (e: Exception) { return@mapNotNull null }
            doubleArrayOf(t, if (r.isNull(iv)) Double.NaN else r.optDouble(iv, Double.NaN))
        }.sortedBy { it[0] }
        val t0 = pts[0][0]
        val t1 = pts[pts.size - 1][0]
        val vals = ArrayList<Double>()
        var t = t0
        var k = 0
        while (t <= t1) {
            while (k < pts.size - 2 && pts[k + 1][0] <= t) k++
            val (ta, va) = pts[k][0] to pts[k][1]
            val (tb, vb) = pts[k + 1][0] to pts[k + 1][1]
            vals.add(if (va.isFinite() && vb.isFinite()) va + (vb - va) * (t - ta) / (tb - ta) else Double.NaN)
            t += 3600
        }
        return HazeSeries(latitude, longitude, t0, 3600.0, guardAod(vals.toDoubleArray(), "GEFS-Aerosols"))
    }

    /** The refraction server's AERONET calibration of CAMS at a place (1 where no station is near). */
    @JvmStatic
    fun fetchHazeCalibration(serverUrl: String, lat: Double, lon: Double): Double {
        val j = WeatherHttp.getJson("${serverUrl.trimEnd('/')}/v1/haze-calibration?lat=${f4(lat)}&lon=${f4(lon)}", 20000, "haze calibration")
        val f = j.optDouble("factor", Double.NaN)
        if (!f.isFinite() || f <= 0) throw WeatherHttpException("no factor in the answer")
        return f
    }

    /**
     * Light pollution at the spot (Blp, pi x cd/m^2, as calc_time.py's app passes it), from the server's World Atlas lookup.
     * No answer is not a dark sky: do not substitute 0 on failure (a darker sky gives an earlier nightfall).
     */
    @JvmStatic
    fun fetchLightPollution(serverUrl: String, lat: Double, lon: Double): Double {
        val j = WeatherHttp.getJson("${serverUrl.trimEnd('/')}/v1/light-pollution?lat=${String.format(Locale.ROOT, "%.5f", lat)}&lon=${String.format(Locale.ROOT, "%.5f", lon)}", 20000, "light-pollution request")
        val v = j.optDouble("blpCdM2", Double.NaN)
        if (!v.isFinite()) throw WeatherHttpException("no blpCdM2 in the answer")
        return v
    }

    // ----------------------------------------------------------------------------------------- JSON (for storage)

    @JvmStatic
    fun seriesToJson(s: HazeSeries): JSONObject = JSONObject().apply {
        put("lat", s.lat); put("lon", s.lon); put("t0", s.t0); put("step", s.step)
        put("aod", JSONArray().also { a -> s.aod.forEach { v -> a.put(if (v.isNaN()) JSONObject.NULL else v) } })
    }

    @JvmStatic
    fun seriesFromJson(j: JSONObject?): HazeSeries? {
        if (j == null) return null
        val a = j.optJSONArray("aod") ?: return null
        return HazeSeries(j.getDouble("lat"), j.getDouble("lon"), j.getDouble("t0"), j.getDouble("step"), DoubleArray(a.length()) { WeatherHttp.num(a, it) })
    }
}
