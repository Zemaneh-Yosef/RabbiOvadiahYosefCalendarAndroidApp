package com.ej.rovadiahyosefcalendar.classes.royzmanim

import com.ej.rovadiahyosefcalendar.classes.royzmanim.AtmosphereGuard.Interval
import com.ej.rovadiahyosefcalendar.classes.royzmanim.AtmosphereGuard.SeriesKind
import com.ej.rovadiahyosefcalendar.classes.royzmanim.WeatherHttp.approximateEventTime
import com.ej.rovadiahyosefcalendar.classes.royzmanim.WeatherHttp.hourlySeries
import com.ej.rovadiahyosefcalendar.classes.royzmanim.WeatherHttp.num
import com.ej.rovadiahyosefcalendar.classes.royzmanim.WeatherHttp.valueAt
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * The weather sources of the website's refraction plumbing (ports of open-meteo-atmosphere.js, open-meteo-path.js,
 * nws-atmosphere.js, path-atmosphere.js and sunset-air.js). All fetching is blocking: call off the main thread.
 * Terms: Open-Meteo's free API is for non-commercial use (credit "Weather data by Open-Meteo.com", CC BY 4.0).
 */

/** A source that was fetched up front and answers synchronously; refresh() fetches it again. */
interface FetchedProvider : AtmosphereProvider {
    fun refresh()
}

private fun f4(x: Double) = String.format(Locale.ROOT, "%.4f", x)

// =============================================================================================== sunset air (MOS)

/**
 * A statistical correction of the lowest 200 m of weather-model air at sunset (Model Output Statistics), from radiosondes
 * released within an hour of sunset against the same model's forecast (see the website's sunset-air.js for the fits).
 */
object SunsetAir {
    val HEIGHTS_M = doubleArrayOf(25.0, 50.0, 100.0, 200.0)
    /** a (K), b per height, by model */
    val COEFFICIENTS: Map<String, Array<DoubleArray>> = mapOf(
        "gfs" to arrayOf(doubleArrayOf(-0.0912, 0.2659), doubleArrayOf(-0.2151, 0.2745), doubleArrayOf(-0.4609, 0.2952), doubleArrayOf(-0.7808, 0.4320)),
        "hrrr" to arrayOf(doubleArrayOf(-0.0852, 0.1820), doubleArrayOf(-0.1953, 0.2460), doubleArrayOf(-0.4746, 0.2834), doubleArrayOf(-0.9834, 0.3164)),
        "gem" to arrayOf(doubleArrayOf(0.1108, 0.2327), doubleArrayOf(0.0916, 0.3088), doubleArrayOf(0.0216, 0.3605), doubleArrayOf(0.0511, 0.5056)),
        "europe" to arrayOf(doubleArrayOf(-0.0254, 0.4030), doubleArrayOf(-0.0859, 0.4995), doubleArrayOf(-0.2510, 0.5751), doubleArrayOf(-0.3897, 0.7766)),
        "icon" to arrayOf(doubleArrayOf(0.0029, 0.4891), doubleArrayOf(-0.0435, 0.6260), doubleArrayOf(-0.1806, 0.7552), doubleArrayOf(0.0, 1.0)),
    )
    private const val G0 = 9.80665
    private const val R_DRY = 287.05

    private fun interp(zs: DoubleArray, ts: DoubleArray, z: Double): Double {
        if (z <= zs[0]) return ts[0]
        for (i in 0 until zs.size - 1) if (z <= zs[i + 1]) return ts[i] + (ts[i + 1] - ts[i]) * (z - zs[i]) / (zs[i + 1] - zs[i])
        return Double.NaN
    }

    /** One column corrected; unchanged when the model has no coefficients, it is over water, or it is shorter than 200 m. */
    @JvmStatic
    fun correctColumn(prof: PathProfile, model: String): PathProfile {
        val coef = COEFFICIENTS[model] ?: return prof
        if (prof.water) return prof
        val levels = prof.levels.sortedBy { it.h }
        if (levels.size < 2) return prof
        val l0 = levels[0]
        val ground = l0.h - 2
        val zs = DoubleArray(levels.size) { levels[it].h - ground }
        val ts = DoubleArray(levels.size) { levels[it].t }
        val top = HEIGHTS_M[HEIGHTS_M.size - 1]
        if (zs[zs.size - 1] < top + 2) return prof
        val out = ArrayList<ProfileLevel>()
        out.add(l0)
        HEIGHTS_M.forEachIndexed { k, h ->
            val rise = interp(zs, ts, h + 2) - l0.t
            val t = l0.t + coef[k][0] + coef[k][1] * rise
            val z = ground + 2 + h
            val prev = out[out.size - 1]
            val p = prev.p * exp(-G0 * (z - prev.h) / (R_DRY * ((t + prev.t) / 2 + 273.15)))
            out.add(ProfileLevel(z, t, p))
        }
        for (l in levels) if (l.h - ground > top + 3) out.add(l)
        return prof.copy(levels = out)
    }

    @JvmStatic
    fun correctPath(path: List<PathProfile>, model: String) = path.map { correctColumn(it, model) }
}

// =============================================================================================== Open-Meteo, one temperature

/** Monthly normals at the times of sunrise and sunset (minC = sunrise, meanC = sunset, as monthlyClimate reads them). */
class EventNormals(
    val sunriseC: DoubleArray, val sunsetC: DoubleArray, val heightM: Double, val years: Pair<Int, Int>, val source: String,
    val sunriseDewC: DoubleArray? = null, val sunsetDewC: DoubleArray? = null,
    /** The centre of the reanalysis grid cell Open-Meteo answered from (lat, lon), when it said. */
    val cell: DoubleArray? = null,
) {
    fun toMonthly() = Refraction.MonthlyNormals(sunriseC, sunsetC, null, heightM, sunriseDewC, sunsetDewC)
    fun provider(): ClimatologyProvider = Refraction.monthlyClimate(toMonthly())

    fun toJson(): JSONObject = JSONObject().apply {
        put("sunriseC", JSONArray(sunriseC.toList())); put("sunsetC", JSONArray(sunsetC.toList()))
        put("heightM", heightM); put("years", JSONArray(listOf(years.first, years.second))); put("source", source)
        sunriseDewC?.let { put("sunriseDewC", JSONArray(it.toList())) }
        sunsetDewC?.let { put("sunsetDewC", JSONArray(it.toList())) }
        cell?.let { put("cell", JSONArray(it.toList())) }
    }

    companion object {
        private fun arr(a: JSONArray?) = a?.let { j -> DoubleArray(j.length()) { j.optDouble(it, Double.NaN) } }

        @JvmStatic
        fun fromJson(j: JSONObject?): EventNormals? {
            if (j == null) return null
            val sr = arr(j.optJSONArray("sunriseC") ?: j.optJSONArray("minC")) ?: return null
            val ss = arr(j.optJSONArray("sunsetC") ?: j.optJSONArray("meanC")) ?: return null
            if (sr.size != 12 || ss.size != 12) return null
            val y = j.optJSONArray("years")
            return EventNormals(sr, ss, j.optDouble("heightM", 0.0), Pair(y?.optInt(0) ?: 0, y?.optInt(1) ?: 0), j.optString("source"),
                arr(j.optJSONArray("sunriseDewC")), arr(j.optJSONArray("sunsetDewC")), arr(j.optJSONArray("cell"))?.takeIf { it.size == 2 })
        }
    }
}

object OpenMeteo {
    const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    const val ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive"
    const val MARINE_URL = "https://marine-api.open-meteo.com/v1/marine"

    /**
     * Forecast-backed provider: hourly 2 m temperature and surface pressure for 16 days (plus yesterday), interpolated to the
     * approximate time of each sunrise / sunset, tagged with the forecast point's elevation. fallback: for dates outside it.
     */
    @JvmStatic
    @JvmOverloads
    fun createAtmosphere(latitude: Double, longitude: Double, fallback: AtmosphereProvider? = null, humidity: Boolean = false, timeoutMs: Int = 20000): FetchedProvider {
        val url = "$FORECAST_URL?latitude=${f4(latitude)}&longitude=${f4(longitude)}&hourly=temperature_2m,surface_pressure${if (humidity) ",dew_point_2m" else ""}" +
            "&past_days=1&forecast_days=16&timeformat=unixtime&timezone=GMT"
        return object : FetchedProvider {
            @Volatile var temps: List<Interval> = emptyList()
            @Volatile var press: List<Interval> = emptyList()
            @Volatile var dews: List<Interval> = emptyList()
            @Volatile var elevationM: Double? = null

            override fun refresh() {
                val j = WeatherHttp.getOpenMeteo(url, timeoutMs = timeoutMs) as JSONObject
                val h = j.optJSONObject("hourly") ?: throw WeatherHttpException("Open-Meteo returned no hourly data")
                val time = h.optJSONArray("time")
                if (time == null || time.length() == 0) throw WeatherHttpException("Open-Meteo returned no hourly data")
                val unit = j.optJSONObject("hourly_units")?.optString("temperature_2m")
                if (!unit.isNullOrEmpty() && !Regex("°C|C$").containsMatchIn(unit)) throw WeatherHttpException("unexpected temperature unit $unit")
                val t = AtmosphereGuard.cleanSeries(hourlySeries(time, h.optJSONArray("temperature_2m")), SeriesKind.TEMPERATURE, "Open-Meteo forecast")
                press = AtmosphereGuard.cleanSeries(hourlySeries(time, h.optJSONArray("surface_pressure")), SeriesKind.PRESSURE, "Open-Meteo forecast")
                dews = if (humidity) AtmosphereGuard.cleanSeries(hourlySeries(time, h.optJSONArray("dew_point_2m")), SeriesKind.DEW, "Open-Meteo forecast") else emptyList()
                elevationM = j.optDouble("elevation", Double.NaN).takeIf { it.isFinite() }
                if (t.isEmpty()) throw WeatherHttpException("Open-Meteo returned no temperatures")
                temps = t
            }

            override fun airFor(date: LocalDate, event: SolarEvent, geo: com.kosherjava.zmanim.util.GeoLocation): AtmosphereSpec? {
                val t = approximateEventTime(date, geo.latitude, geo.longitude, event)
                val temperatureC = valueAt(temps, t)
                if (!temperatureC.isFinite()) return fallback?.airFor(date, event, geo)
                val td = if (dews.isNotEmpty()) valueAt(dews, t).takeIf { it.isFinite() } else null
                val el = elevationM
                val p = if (el != null) valueAt(press, t).takeIf { it.isFinite() } else null
                return SurfaceAir(temperatureC, pressureMb = p, heightM = el, dewPointC = td, source = "forecast")
            }
        }.also { it.refresh() }
    }

    /**
     * Monthly temperature normals at the times of sunrise and sunset, from ERA5 via Open-Meteo's archive (the last `years`
     * full years). One large request: fetch once per place and store the result.
     */
    @JvmStatic
    @JvmOverloads
    fun fetchNormals(latitude: Double, longitude: Double, years: Int = 5, humidity: Boolean = false, timeoutMs: Int = 60000): EventNormals {
        val endYear = LocalDate.now(ZoneOffset.UTC).year - 1
        val startYear = endYear - years + 1
        val url = "$ARCHIVE_URL?latitude=${f4(latitude)}&longitude=${f4(longitude)}&start_date=$startYear-01-01&end_date=$endYear-12-31" +
            "&hourly=temperature_2m${if (humidity) ",dew_point_2m" else ""}&timeformat=unixtime&timezone=GMT"
        val j = WeatherHttp.getOpenMeteo(url, timeoutMs = timeoutMs) as JSONObject
        val h = j.optJSONObject("hourly")
        val series = AtmosphereGuard.cleanSeries(hourlySeries(h?.optJSONArray("time"), h?.optJSONArray("temperature_2m")), SeriesKind.TEMPERATURE, "Open-Meteo archive")
        if (series.size < 24 * 300) throw WeatherHttpException("Open-Meteo archive returned too little data")
        fun monthly(s: List<Interval>): Pair<DoubleArray, DoubleArray> {
            val sum = Array(2) { DoubleArray(12) }
            val n = Array(2) { IntArray(12) }
            // a walking index: the series is sorted, so each lookup starts where the last ended
            for (y in startYear..endYear) for (m in 1..12) for (d in 1..YearMonth.of(y, m).lengthOfMonth()) {
                val date = LocalDate.of(y, m, d)
                for ((k, ev) in listOf(SolarEvent.SUNRISE, SolarEvent.SUNSET).withIndex()) {
                    val v = sortedValueAt(s, approximateEventTime(date, latitude, longitude, ev))
                    if (v.isFinite()) { sum[k][m - 1] += v; n[k][m - 1]++ }
                }
            }
            fun avg(k: Int) = DoubleArray(12) { if (n[k][it] > 0) Math.round(sum[k][it] / n[k][it] * 100) / 100.0 else Double.NaN }
            return Pair(avg(0), avg(1))
        }
        val (sunriseC, sunsetC) = monthly(series)
        if ((sunriseC + sunsetC).any { !it.isFinite() }) throw WeatherHttpException("Open-Meteo archive has gaps for some months")
        var sunriseDew: DoubleArray? = null
        var sunsetDew: DoubleArray? = null
        if (humidity) {
            val dew = monthly(AtmosphereGuard.cleanSeries(hourlySeries(h?.optJSONArray("time"), h?.optJSONArray("dew_point_2m")), SeriesKind.DEW, "Open-Meteo archive"))
            if ((dew.first + dew.second).all { it.isFinite() }) { sunriseDew = dew.first; sunsetDew = dew.second }
        }
        return EventNormals(sunriseC, sunsetC, j.optDouble("elevation", 0.0).takeIf { it.isFinite() } ?: 0.0, Pair(startYear, endYear),
            "Open-Meteo archive (ECMWF ERA5)", sunriseDew, sunsetDew, WeatherHttp.reportedCell(j))
    }

    /** valueAt on hourly intervals by index arithmetic (the archive is ~40,000 hours: a linear scan per day is too slow). */
    private fun sortedValueAt(s: List<Interval>, t: Double): Double {
        if (s.isEmpty() || t < s[0].start || t > s[s.size - 1].end) return Double.NaN
        var lo = 0
        var hi = s.size - 1
        while (hi - lo > 1) { val m = (lo + hi) ushr 1; if ((s[m].start + s[m].end) / 2.0 <= t) lo = m else hi = m }
        val a = (s[lo].start + s[lo].end) / 2.0
        val b = (s[hi].start + s[hi].end) / 2.0
        if (t <= a) return s[lo].value
        if (t >= b) return s[hi].value
        return s[lo].value + (s[hi].value - s[lo].value) * (t - a) / (b - a)
    }
}

// =============================================================================================== NWS (US) and NCEI normals

object Nws {
    private const val NWS_BASE = "https://api.weather.gov"
    private const val NCEI_DATA = "https://www.ncei.noaa.gov/access/services/data/v1"
    private const val NCEI_SEARCH = "https://www.ncei.noaa.gov/access/services/search/v1/data"

    private fun durationMs(d: String): Long {
        val m = Regex("^P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?$").find(d) ?: throw IllegalArgumentException("Unrecognised ISO 8601 duration: $d")
        fun g(i: Int) = m.groupValues[i].ifEmpty { "0" }.toLong()
        return (((g(1) * 24 + g(2)) * 60 + g(3)) * 60 + g(4)) * 1000
    }

    /** An NWS grid layer as intervals in C. */
    @JvmStatic
    fun parseTemperatureLayer(layer: JSONObject?): List<Interval> {
        val values = layer?.optJSONArray("values") ?: return emptyList()
        val f = Regex("degF", RegexOption.IGNORE_CASE).containsMatchIn(layer.optString("uom"))
        val out = ArrayList<Interval>()
        for (i in 0 until values.length()) {
            val v = values.getJSONObject(i)
            if (v.isNull("value")) continue
            val parts = v.getString("validTime").split("/")
            val start = try { java.time.OffsetDateTime.parse(parts[0]).toInstant().toEpochMilli() } catch (e: Exception) { continue }
            val end = start + durationMs(parts[1])
            val value = v.getDouble("value")
            if (end > start) out.add(Interval(start, end, if (f) (value - 32) * 5 / 9 else value))
        }
        out.sortBy { it.start }
        return AtmosphereGuard.cleanSeries(out, SeriesKind.TEMPERATURE, "NWS forecast")
    }

    /** The NWS forecast grid for a US location as a provider (about 7 days ahead). */
    @JvmStatic
    fun createAtmosphere(latitude: Double, longitude: Double, fallback: AtmosphereProvider? = null, timeoutMs: Int = 20000): FetchedProvider {
        val lat = f4(latitude)
        val lon = f4(longitude)
        return object : FetchedProvider {
            @Volatile var series: List<Interval> = emptyList()
            var gridUrl = ""
            @Volatile var gridElevationM: Double? = null

            private fun get(url: String): JSONObject {
                val (status, text) = WeatherHttp.get(url, timeoutMs, "application/geo+json")
                if (status !in 200..299) throw WeatherHttpException("NWS request failed ($status) for $url", status)
                return JSONObject(text)
            }

            override fun refresh() {
                if (gridUrl.isEmpty()) {
                    gridUrl = get("$NWS_BASE/points/$lat,$lon").optJSONObject("properties")?.optString("forecastGridData").orEmpty()
                    if (gridUrl.isEmpty()) throw WeatherHttpException("NWS has no forecast grid for this location (US only)")
                }
                val props = get(gridUrl).optJSONObject("properties")
                val s = parseTemperatureLayer(props?.optJSONObject("temperature"))
                if (s.isEmpty()) throw WeatherHttpException("NWS grid returned no temperature data")
                series = s
                val elev = props?.optJSONObject("elevation")
                val v = elev?.optDouble("value", Double.NaN) ?: Double.NaN
                gridElevationM = if (v.isFinite()) (if (Regex("ft", RegexOption.IGNORE_CASE).containsMatchIn(elev?.optString("unitCode").orEmpty())) v * 0.3048 else v) else null
            }

            override fun airFor(date: LocalDate, event: SolarEvent, geo: com.kosherjava.zmanim.util.GeoLocation): AtmosphereSpec? {
                val t = approximateEventTime(date, geo.latitude, geo.longitude, event)
                val temperatureC = valueAt(series, t)
                if (temperatureC.isFinite()) return SurfaceAir(temperatureC, heightM = gridElevationM, source = "forecast")
                return fallback?.airFor(date, event, geo)
            }
        }.also { it.refresh() }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = PI / 180
        val a = sin((lat2 - lat1) * r / 2).let { it * it } + cos(lat1 * r) * cos(lat2 * r) * sin((lon2 - lon1) * r / 2).let { it * it }
        return 12742 * asin(kotlin.math.sqrt(a))
    }

    /** NOAA 1991-2020 normals from the nearest station that has temperature normals (US only; a last resort). */
    @JvmStatic
    fun fetchNearestNormals(latitude: Double, longitude: Double, searchRadiusDeg: Double = 0.5, maxCandidates: Int = 8): EventNormals {
        val d = searchRadiusDeg
        val bbox = listOf(latitude + d, longitude - d, latitude - d, longitude + d).joinToString(",") { f4(it) }
        val (status, text) = WeatherHttp.get("$NCEI_SEARCH?dataset=normals-monthly-1991-2020&bbox=$bbox&limit=1000")
        if (status !in 200..299) throw WeatherHttpException("NCEI station search failed ($status)", status)
        val results = JSONObject(text).optJSONArray("results") ?: JSONArray()
        val cands = (0 until results.length()).mapNotNull { i ->
            val x = results.getJSONObject(i)
            val c = x.optJSONArray("centroid")
            val id = x.optString("name").replace(Regex("\\.csv$", RegexOption.IGNORE_CASE), "")
            if (id.isEmpty() || c == null) null else Triple(id, c.optDouble(1), c.optDouble(0))
        }.sortedBy { distanceKm(latitude, longitude, it.second, it.third) }.take(maxCandidates)
        for ((id, _, _) in cands) {
            val (st, body) = WeatherHttp.get("$NCEI_DATA?dataset=normals-monthly-1991-2020&stations=$id&dataTypes=MLY-TMIN-NORMAL,MLY-TAVG-NORMAL&format=json&includeStationName=true&includeStationLocation=1")
            if (st !in 200..299) continue
            val rows = try { JSONArray(body) } catch (e: Exception) { continue }
            val minC = DoubleArray(12) { Double.NaN }
            val meanC = DoubleArray(12) { Double.NaN }
            fun f2c(s: String?) = if (s.isNullOrBlank()) Double.NaN else (s.trim().toDouble() - 32) * 5 / 9
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val m = row.optString("DATE").trim().toIntOrNull()?.minus(1) ?: continue
                if (m in 0..11) { minC[m] = f2c(row.optString("MLY-TMIN-NORMAL")); meanC[m] = f2c(row.optString("MLY-TAVG-NORMAL")) }
            }
            if (rows.length() == 0 || (minC + meanC).any { !it.isFinite() }) continue
            return EventNormals(minC, meanC, rows.getJSONObject(0).optString("ELEVATION").toDoubleOrNull() ?: 0.0, Pair(1991, 2020), "NOAA normals $id")
        }
        throw WeatherHttpException("No station with temperature normals nearby")
    }
}

// =============================================================================================== Open-Meteo path profiles

/**
 * The air along the whole line of sight: for each sunrise and sunset in the next 16 days, model temperature profiles (2 m
 * plus pressure levels up to 700 mb) at 0-400 km along the Sun's direction, with the sea-surface temperature where the path
 * crosses the sea. Every profile of an event from one model (chosen by place; the fallback GFS where it ends).
 */
object OpenMeteoPath {
    val DEFAULT_DISTANCES_KM = doubleArrayOf(0.0, 10.0, 25.0, 50.0, 100.0, 150.0, 200.0, 300.0, 400.0)
    val DEFAULT_LEVELS_MB = intArrayOf(1000, 975, 950, 925, 900, 850, 800, 700)
    val GEM_LEVELS_MB = intArrayOf(1015, 1000, 985, 970, 950, 925, 900, 875, 850, 800, 700)
    const val FALLBACK_MODEL = "gfs_seamless"
    private const val DEG = PI / 180
    private const val R_EARTH = 6371008.8

    private val CANADA = arrayOf(
        doubleArrayOf(-141.0, 84.0), doubleArrayOf(-141.0, 60.3), doubleArrayOf(-139.0, 60.0), doubleArrayOf(-137.5, 59.0), doubleArrayOf(-135.5, 59.8), doubleArrayOf(-133.4, 58.4), doubleArrayOf(-131.0, 56.0),
        doubleArrayOf(-130.0, 55.3), doubleArrayOf(-130.6, 54.7), doubleArrayOf(-128.0, 52.0), doubleArrayOf(-125.0, 48.4), doubleArrayOf(-123.25, 48.25), doubleArrayOf(-123.2, 48.75),
        doubleArrayOf(-123.0, 49.0), doubleArrayOf(-95.15, 49.0), doubleArrayOf(-95.15, 49.38), doubleArrayOf(-94.6, 48.7), doubleArrayOf(-93.0, 48.6), doubleArrayOf(-90.8, 48.2), doubleArrayOf(-89.4, 48.0),
        doubleArrayOf(-88.3, 48.3), doubleArrayOf(-84.8, 46.9), doubleArrayOf(-84.4, 46.5), doubleArrayOf(-84.0, 46.2), doubleArrayOf(-83.5, 46.0), doubleArrayOf(-82.4, 45.3), doubleArrayOf(-82.42, 43.0),
        doubleArrayOf(-82.5, 42.6), doubleArrayOf(-82.9, 42.35), doubleArrayOf(-83.06, 42.31), doubleArrayOf(-83.13, 42.2), doubleArrayOf(-83.15, 42.05), doubleArrayOf(-82.5, 41.7), doubleArrayOf(-81.0, 42.25), doubleArrayOf(-79.8, 42.5), doubleArrayOf(-78.9, 42.9), doubleArrayOf(-79.05, 43.2),
        doubleArrayOf(-79.2, 43.45), doubleArrayOf(-78.0, 43.6), doubleArrayOf(-76.8, 43.6), doubleArrayOf(-76.3, 44.2), doubleArrayOf(-75.4, 44.8), doubleArrayOf(-74.7, 45.0), doubleArrayOf(-71.5, 45.0),
        doubleArrayOf(-71.1, 45.3), doubleArrayOf(-70.6, 45.5), doubleArrayOf(-70.2, 46.2), doubleArrayOf(-70.0, 46.7), doubleArrayOf(-69.2, 47.45), doubleArrayOf(-68.3, 47.35),
        doubleArrayOf(-67.8, 47.07), doubleArrayOf(-67.8, 45.7), doubleArrayOf(-67.4, 45.6), doubleArrayOf(-67.0, 44.9), doubleArrayOf(-66.5, 44.0), doubleArrayOf(-60.0, 41.0), doubleArrayOf(-50.0, 45.0),
        doubleArrayOf(-52.0, 60.0), doubleArrayOf(-64.0, 60.5), doubleArrayOf(-73.0, 76.0), doubleArrayOf(-60.0, 82.5), doubleArrayOf(-60.0, 84.0),
    )

    private fun inPolygon(lat: Double, lon: Double, poly: Array<DoubleArray>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val xi = poly[i][0]; val yi = poly[i][1]; val xj = poly[j][0]; val yj = poly[j][1]
            if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }

    /** The Open-Meteo model to use for an observer: ICON near Israel, GEM in Canada, best_match in Europe, else GFS. */
    @JvmStatic
    fun chooseModel(lat: Double, lon: Double): String = when {
        lat in 29.0..34.8 && lon in 33.0..36.8 -> "icon_seamless"
        inPolygon(lat, lon, CANADA) -> "gem_seamless"
        lat in 29.5..70.5 && lon in -23.5..62.5 -> "best_match"
        else -> FALLBACK_MODEL
    }

    /** The sunset-air coefficients for one event's air, or null. */
    @JvmStatic
    fun sunsetAirModel(name: String, event: SolarEvent, lat: Double, lon: Double, t: Double, fetchedAt: Long): String? {
        if (event != SolarEvent.SUNSET) return null
        return when (name) {
            "gfs_global", "gfs025" -> "gfs"
            "icon_seamless" -> "icon"
            "gem_seamless" -> "gem"
            "best_match" -> if (chooseModel(lat, lon) == "best_match") "europe" else null
            "gfs_seamless" -> if (lat in 21.0..53.0 && lon in -135.0..-60.0 && t < fetchedAt + 48 * 3600000.0) "hrrr" else "gfs"
            else -> null
        }
    }

    private fun destination(lat: Double, lon: Double, azDeg: Double, dM: Double): DoubleArray {
        val d = dM / R_EARTH; val p1 = lat * DEG; val l1 = lon * DEG; val a = azDeg * DEG
        val p2 = asin(sin(p1) * cos(d) + cos(p1) * sin(d) * cos(a))
        val l2 = l1 + atan2(sin(a) * sin(d) * cos(p1), cos(d) - sin(p1) * sin(p2))
        return doubleArrayOf(p2 / DEG, ((l2 / DEG + 540) % 360) - 180)
    }

    private fun declination(ms: Double): Double {
        val n = ms / 86400000 - 10957.5
        val g = (357.529 + 0.98560028 * n) * DEG
        val q = 280.459 + 0.98564736 * n
        val L = (q + 1.915 * sin(g) + 0.020 * sin(2 * g)) * DEG
        return asin(sin(23.439 * DEG) * sin(L)) / DEG
    }

    /** Azimuth of sunrise / sunset (deg east of north), to ~0.5 deg. */
    @JvmStatic
    fun eventAzimuth(lat: Double, ms: Double, event: SolarEvent): Double {
        val c = max(-1.0, min(1.0, sin(declination(ms) * DEG) / cos(lat * DEG)))
        val a = acos(c) / DEG
        return if (event == SolarEvent.SUNRISE) a else 360 - a
    }

    /** Rough sunrise / sunset time (epoch ms); NaN if the Sun doesn't rise or set. */
    @JvmStatic
    fun eventTime(date: LocalDate, lat: Double, lon: Double, event: SolarEvent): Double {
        val noon = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli() - lon / 15 * 3600000
        val dec = declination(noon) * DEG
        val cosH = (sin(-0.833 * DEG) - sin(lat * DEG) * sin(dec)) / (cos(lat * DEG) * cos(dec))
        if (cosH < -1 || cosH > 1) return Double.NaN
        val H = acos(cosH) / DEG
        return noon + (if (event == SolarEvent.SUNRISE) -H else H) / 15 * 3600000
    }

    private class Series(val times: LongArray, val cols: JSONObject, val elevation: Double, val levels: List<Int>, val cell: DoubleArray? = null) {
        fun at(name: String, t: Double): Double {
            val col = cols.optJSONArray(name) ?: return Double.NaN
            if (times.isEmpty()) return Double.NaN
            val x = (t / 1000 - times[0]) / 3600
            if (x < 0 || x > times.size - 1) return Double.NaN
            val i = min(floor(x).toInt(), times.size - 2)
            val f = x - i
            val a = num(col, i)
            val b = num(col, i + 1)
            if (a.isNaN() || b.isNaN()) return if (f < 0.5) a else b
            return a + (b - a) * f
        }
    }

    private fun toSeries(json: Any, levels: IntArray = IntArray(0)): List<Series> {
        val arr = if (json is JSONArray) (0 until json.length()).map { json.getJSONObject(it) } else listOf(json as JSONObject)
        return arr.map { j ->
            val cols = j.optJSONObject("hourly") ?: JSONObject()
            fun has(k: String): Boolean { val a = cols.optJSONArray(k) ?: return false; return (0 until a.length()).any { !a.isNull(it) } }
            val time = cols.optJSONArray("time")
            Series(LongArray(time?.length() ?: 0) { time!!.getLong(it) }, cols, j.optDouble("elevation", Double.NaN),
                levels.filter { p -> has("temperature_${p}hPa") && has("geopotential_height_${p}hPa") }, WeatherHttp.reportedCell(j))
        }
    }

    /** What the path provider holds; refresh() fetches it again. */
    class PathAtmosphere internal constructor(
        private val latitude: Double, private val longitude: Double, private val days: Int, private val humidity: Boolean,
        val model: String, val fallbackModel: String?, private val sunsetAirCorrection: Boolean, private val timeoutMs: Int,
    ) : FetchedProvider {
        private class Fan(val event: SolarEvent, val az: Double, val idx: IntArray)
        @Volatile private var fans: List<Fan> = emptyList()
        @Volatile private var fc: List<Series> = emptyList()
        @Volatile private var fb: List<Series> = emptyList()
        @Volatile private var marine: Map<Int, Series> = emptyMap()
        private val built = HashMap<String, PathAir?>()
        private var stamp = ""
        private var fetchedAt = 0L
        @Volatile var coverage: LongArray? = null
        @Volatile var lastError: Exception? = null
        /** The centre of the model's grid cell around the observer (lat, lon), as Open-Meteo reported it. */
        @Volatile var cell: DoubleArray? = null
        private val dists = DEFAULT_DISTANCES_KM

        private fun levelsFor(m: String) = if (Regex("^(cmc_)?gem").containsMatchIn(m)) GEM_LEVELS_MB else DEFAULT_LEVELS_MB

        override fun refresh() {
            val now = System.currentTimeMillis()
            val span = (days + 1) * 86400000.0
            val t0 = now - 86400000.0
            val pts = ArrayList<DoubleArray>()
            pts.add(doubleArrayOf(latitude, longitude))
            val newFans = ArrayList<Fan>()
            for (event in SolarEvent.entries) for (frac in doubleArrayOf(0.25, 0.75)) {
                val az = eventAzimuth(latitude, t0 + span * frac, event)
                val idx = IntArray(dists.size) { k ->
                    if (dists[k] == 0.0) 0 else { pts.add(destination(latitude, longitude, az, dists[k] * 1000)); pts.size - 1 }
                }
                newFans.add(Fan(event, az, idx))
            }
            val lats = pts.joinToString(",") { f4(it[0]) }
            val lons = pts.joinToString(",") { f4(it[1]) }
            fun fetchModel(m: String): List<Series> {
                val lv = levelsFor(m)
                val hourly = listOf("temperature_2m", "surface_pressure") + lv.map { "temperature_${it}hPa" } + lv.map { "geopotential_height_${it}hPa" } +
                    (if (humidity) listOf("dew_point_2m") + lv.map { "relative_humidity_${it}hPa" } else emptyList())
                val url = "${OpenMeteo.FORECAST_URL}?latitude=$lats&longitude=$lons&hourly=${hourly.joinToString(",")}&past_days=1&forecast_days=$days" +
                    "&timeformat=unixtime&timezone=GMT&cell_selection=nearest" + (if (m == "best_match") "" else "&models=$m")
                val s = toSeries(WeatherHttp.getOpenMeteo(url, timeoutMs = timeoutMs), lv)
                if (s.size != pts.size) throw WeatherHttpException("Open-Meteo returned ${s.size} locations, expected ${pts.size}")
                if (s[0].times.isEmpty()) throw WeatherHttpException("Open-Meteo returned no hourly data for $m")
                return s
            }
            var errA: Exception? = null
            var errB: Exception? = null
            // both at once; either alone is still usable
            val fbThread = fallbackModel?.let { fbm ->
                val holder = arrayOfNulls<List<Series>>(1)
                val th = Thread { try { holder[0] = fetchModel(fbm) } catch (e: Exception) { errB = e } }
                th.start()
                Pair(th, holder)
            }
            val newFc = try { fetchModel(model) } catch (e: Exception) { errA = e; emptyList() }
            fbThread?.first?.join()
            val newFb = fbThread?.second?.get(0) ?: emptyList()
            if (errA != null && (errB != null || fallbackModel == null)) throw errA!!
            var err: Exception? = errA ?: errB
            val any = newFc.ifEmpty { newFb }
            val sea = pts.indices.filter { any[it].elevation == 0.0 }
            val newMarine = HashMap<Int, Series>()
            if (sea.isNotEmpty()) {
                try {
                    val murl = "${OpenMeteo.MARINE_URL}?latitude=${sea.joinToString(",") { f4(pts[it][0]) }}&longitude=${sea.joinToString(",") { f4(pts[it][1]) }}" +
                        "&hourly=sea_surface_temperature&past_days=1&forecast_days=${min(days, 8)}&timeformat=unixtime&timezone=GMT"
                    val ms = toSeries(WeatherHttp.getOpenMeteo(murl, timeoutMs = timeoutMs))
                    sea.forEachIndexed { k, i -> if (k < ms.size && ms[k].times.isNotEmpty()) newMarine[i] = ms[k] }
                } catch (e: Exception) { err = e }
            }
            val ends = listOf(newFc, newFb).filter { it.isNotEmpty() }.map { it[0].times }
            synchronized(built) {
                fans = newFans; fc = newFc; fb = newFb; marine = newMarine; built.clear()
                cell = any[0].cell
                fetchedAt = now
                coverage = longArrayOf(ends.minOf { it[0] } * 1000, ends.maxOf { it[it.size - 1] } * 1000)
                stamp = ends.joinToString(".") { it[0].toString() }
                lastError = err
            }
        }

        private fun sstAt(s: Series, t: Double): Double {
            val v = s.at("sea_surface_temperature", t)
            if (v.isFinite()) return v
            val col = s.cols.optJSONArray("sea_surface_temperature") ?: return Double.NaN
            for (i in col.length() - 1 downTo 0) if (!col.isNull(i) && t > s.times[i] * 1000) return col.getDouble(i)
            return Double.NaN
        }

        private fun profileFrom(s: Series?, i: Int, t: Double, distanceKm: Double): PathProfile? {
            if (s == null) return null
            val t2 = s.at("temperature_2m", t)
            val ps = s.at("surface_pressure", t)
            if (!t2.isFinite() || !ps.isFinite() || !s.elevation.isFinite()) return null
            val h0 = s.elevation + 2
            val lv = ArrayList<ProfileLevel>()
            lv.add(ProfileLevel(h0, t2, ps, td = if (humidity) s.at("dew_point_2m", t).takeIf { it.isFinite() } else null))
            for (p in s.levels) {
                if (p >= ps) continue
                val z = s.at("geopotential_height_${p}hPa", t)
                val tp = s.at("temperature_${p}hPa", t)
                if (!z.isFinite() || !tp.isFinite()) return null
                if (z > h0 + 1) lv.add(ProfileLevel(z, tp, p.toDouble(), rh = if (humidity) s.at("relative_humidity_${p}hPa", t).takeIf { it.isFinite() } else null))
            }
            val m = marine[i]
            if (m != null) {
                val sst = sstAt(m, t)
                if (sst.isFinite()) return PathProfile(distanceKm, true, sst, lv)
            }
            return PathProfile(distanceKm, levels = lv)
        }

        override fun airFor(date: LocalDate, event: SolarEvent, geo: com.kosherjava.zmanim.util.GeoLocation): AtmosphereSpec? {
            val k = "$date|$event"
            synchronized(built) { if (built.containsKey(k)) return built[k] }
            var res: PathAir? = null
            val t = eventTime(date, geo.latitude, geo.longitude, event)
            val cov = coverage
            if (cov != null && t.isFinite() && t >= cov[0] && t <= cov[1]) {
                val az = eventAzimuth(latitude, t, event)
                val fan = fans.filter { it.event == event }.minByOrNull { abs(it.az - az) }!!
                for ((series, name) in listOf(Pair(fc, model), Pair(fb, fallbackModel ?: ""))) {
                    if (series.isEmpty()) continue
                    val path = fan.idx.mapIndexed { n, i -> profileFrom(series[i], i, t, dists[n]) }
                    if (path.all { it != null }) {
                        val air = if (sunsetAirCorrection) sunsetAirModel(name, event, latitude, longitude, t, fetchedAt) else null
                        @Suppress("UNCHECKED_CAST") val p = path as List<PathProfile>
                        res = PathAir(if (air != null) SunsetAir.correctPath(p, air) else p,
                            "om|$name|$stamp|$k${if (humidity) "|h" else ""}${if (air != null) "|air-${air}1" else ""}", "forecast")
                        break
                    }
                }
            }
            synchronized(built) { built[k] = res }
            return res
        }
    }

    /** Path profiles for the place, fetched now (blocking; Open-Meteo can take 30-50 s for a model not asked for in a while). */
    @JvmStatic
    @JvmOverloads
    fun create(latitude: Double, longitude: Double, days: Int = 16, models: String = "auto", humidity: Boolean = false,
               sunsetAirCorrection: Boolean = true, timeoutMs: Int = 60000): PathAtmosphere {
        val d = max(1, min(days, 16))
        val model = if (models == "auto") chooseModel(latitude, longitude) else models
        val fb = if (FALLBACK_MODEL != model && model != "best_match") FALLBACK_MODEL else null
        return PathAtmosphere(latitude, longitude, d, humidity, model, fb, sunsetAirCorrection, timeoutMs).also { it.refresh() }
    }
}

// =============================================================================================== the refraction server's profiles

/**
 * Temperature profiles along the sunrise / sunset direction from the refraction server, for any date: the latest GFS run
 * ("forecast", ~7 days), else typical profiles ("climatology-gfs", "climatology-ncep"). Dates must be loaded first.
 */
class ServerPathAtmosphere(
    baseUrl: String, private val latitude: Double, private val longitude: Double, days: Int = 31,
    typical: Boolean = false, private val sunsetAirCorrection: Boolean = true, private val timeoutMs: Int = 20000,
) : FetchedProvider {
    private val root = "${baseUrl.trimEnd('/')}/v1/path-profiles?lat=${f4(latitude)}&lon=${f4(longitude)}" + (if (typical) "&typical=1" else "")
    private val byKey = HashMap<String, Pair<PathAir, String>>()
    private val ranges = ArrayList<Pair<LocalDate, Int>>()
    @Volatile var cycle: String? = null
    @Volatile var lastError: Exception? = null
    val eventCount get() = synchronized(byKey) { byKey.size }

    init {
        val yesterday = Instant.ofEpochMilli(System.currentTimeMillis() - 86400000 + (longitude / 15 * 3600000).toLong()).atZone(ZoneOffset.UTC).toLocalDate()
        ranges.add(Pair(yesterday, days))
        loadRange(yesterday, days)
    }

    private fun hashProfiles(s: String): String {
        var h = 5381
        for (c in s) h = (h shl 5) + h + c.code
        return (h.toLong() and 0xffffffffL).toString(36)
    }

    private fun load(from: LocalDate, days: Int) {
        try {
            val json = WeatherHttp.getJson("$root&from=${from.format(DateTimeFormatter.ISO_LOCAL_DATE)}&days=$days", timeoutMs, "refraction server")
            val events = json.optJSONArray("events") ?: JSONArray()
            val jCycle = json.optString("cycle").takeIf { it.isNotEmpty() }
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                val profiles = e.optJSONArray("profiles")
                if (profiles == null || profiles.length() == 0) continue
                val event = if (e.optString("event") == "sunrise") SolarEvent.SUNRISE else SolarEvent.SUNSET
                val source = e.optString("source")
                val date = e.optString("date")
                val tag = if (source == "forecast") jCycle else "$source#${hashProfiles(profiles.toString())}"
                val m = if (sunsetAirCorrection && event == SolarEvent.SUNSET && (source == "forecast" || source == "climatology-gfs")) "gfs" else null
                val raw = parseProfiles(profiles)
                val path = if (m != null) SunsetAir.correctPath(raw, m) else raw
                val air = PathAir(path, "srv|$tag|$latitude,$longitude|$date|${event.name.lowercase()}${if (m != null) "|air-${m}1" else ""}", source)
                val problem = AtmosphereGuard.pathProblem(air)
                synchronized(byKey) {
                    if (problem != null) {
                        AtmosphereGuard.report("refraction server, $date $event ($source): $problem")
                        byKey.remove("$date|$event")
                    } else byKey["$date|$event"] = Pair(air, source)
                }
            }
            if (jCycle != null) cycle = jCycle
            lastError = null
        } catch (err: Exception) {
            lastError = err
        }
    }

    private fun loadRange(from: LocalDate, days: Int) {
        var k = 0
        while (k < days) { load(from.plusDays(k.toLong()), min(120, days - k)); k += 120 }
    }

    /** Load a date range (e.g. a whole year before printing). */
    fun prefetch(from: LocalDate, days: Int) { synchronized(ranges) { ranges.add(Pair(from, days)) }; loadRange(from, days) }

    override fun refresh() { for (r in synchronized(ranges) { ranges.toList() }) loadRange(r.first, r.second) }

    fun sourceOf(date: LocalDate, event: SolarEvent) = synchronized(byKey) { byKey["$date|$event"]?.second }

    override fun airFor(date: LocalDate, event: SolarEvent, geo: com.kosherjava.zmanim.util.GeoLocation): AtmosphereSpec? =
        synchronized(byKey) { byKey["$date|$event"]?.first }

    companion object {
        @JvmStatic
        fun parseProfiles(a: JSONArray): List<PathProfile> = (0 until a.length()).map { i ->
            val p = a.getJSONObject(i)
            val lv = p.optJSONArray("levels") ?: JSONArray()
            PathProfile(
                p.optDouble("distanceKm"), p.optBoolean("water", false), WeatherHttp.numOrNull(p, "skinC"),
                (0 until lv.length()).map { k ->
                    val l = lv.getJSONObject(k)
                    ProfileLevel(l.optDouble("h"), l.optDouble("t"), l.optDouble("p"), WeatherHttp.numOrNull(l, "td"), WeatherHttp.numOrNull(l, "rh"))
                },
            )
        }
    }
}
