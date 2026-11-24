package com.ej.rovadiahyosefcalendar.classes.royzmanim

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A request that failed in a way worth reporting (HTTP status, a non-JSON body). */
open class WeatherHttpException(message: String, val status: Int = 0, val body: JSONObject? = null) : IOException(message)

/** Open-Meteo answered 429 (over a limit): ask again after [retryAfterS] seconds. */
class OpenMeteoRateLimitException(message: String, val retryAfterS: Int) : WeatherHttpException(message, 429)

/** Blocking JSON GETs for the weather and refraction-server clients (call off the main thread). */
object WeatherHttp {
    /** Sent to every service; NWS asks every app to identify itself. */
    const val USER_AGENT = "RabbiOvadiahYosefCalendar (Android; zmanim refraction)"

    /** GET a URL; returns the status and the body text. */
    @JvmStatic
    fun get(url: String, timeoutMs: Int = 20000, accept: String = "application/json"): Pair<Int, String> =
        getWithHeaders(url, timeoutMs, accept).let { Pair(it.first, it.second) }

    /** GET a URL; returns the status, the body text and the response headers (names in lower case). */
    @JvmStatic
    fun getWithHeaders(url: String, timeoutMs: Int = 20000, accept: String = "application/json"): Triple<Int, String, Map<String, String>> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = min(timeoutMs, 15000)
            c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.setRequestProperty("Accept", accept)
            c.setRequestProperty("Accept-Encoding", "gzip")
            val status = c.responseCode
            val headers = HashMap<String, String>()
            for ((k, v) in c.headerFields) if (k != null && !v.isNullOrEmpty()) headers[k.lowercase()] = v[0]
            val stream = (if (status in 200..299) c.inputStream else c.errorStream) ?: return Triple(status, "", headers)
            val input = if ("gzip".equals(c.contentEncoding, true)) java.util.zip.GZIPInputStream(stream) else stream
            return Triple(status, input.bufferedReader().use { it.readText() }, headers)
        } finally {
            c.disconnect()
        }
    }

    /** When Open-Meteo's last 429 said to ask again (epoch ms); 0 = no wait. */
    @Volatile private var openMeteoBlockedAt = 0L

    /** When Open-Meteo's last 429 said to ask again (epoch ms), or 0 once that has passed. */
    @JvmStatic
    fun openMeteoBlockedUntil(): Long = openMeteoBlockedAt.takeIf { it > System.currentTimeMillis() } ?: 0L

    /** A Retry-After at most this long is waited out within the request; a longer one fails it. */
    private const val MAX_INLINE_WAIT_S = 30

    /** A JSON object from a server, with its error message on failure (no retries). */
    @JvmStatic
    fun getJson(url: String, timeoutMs: Int = 20000, what: String = "request"): JSONObject {
        val (status, text) = get(url, timeoutMs)
        val body = try { JSONObject(text) } catch (e: Exception) { null }
        if (status !in 200..299) throw WeatherHttpException("$what failed: ${body?.optString("error")?.takeIf { it.isNotEmpty() } ?: "HTTP $status"}", status, body)
        return body ?: throw WeatherHttpException("$what failed: not JSON", status)
    }

    /**
     * GET a JSON response from Open-Meteo, with retries for passing trouble: a 200 with a plain-text body ("timeoutReached"),
     * a timeout or network error and HTTP 5xx are retried `retries` times; HTTP 4xx is not. HTTP 429 (a limit reached) is
     * retried after its Retry-After when that is short; a longer one throws [OpenMeteoRateLimitException], and until it has
     * passed every request fails at once with the same error instead of spending more of the quota. The body may be one object
     * or an array (several locations).
     */
    @JvmStatic
    fun getOpenMeteo(url: String, retries: Int = 1, timeoutMs: Int = 20000, retryDelayMs: Long = 2000): Any {
        var attempt = 0
        while (true) {
            val blocked = openMeteoBlockedUntil()
            if (blocked > 0) throw OpenMeteoRateLimitException("Open-Meteo limit reached (waiting for its Retry-After)",
                ((blocked - System.currentTimeMillis() + 999) / 1000).toInt())
            var retryable = true
            var wait = retryDelayMs * (attempt + 1)
            try {
                val (status, text, headers) = getWithHeaders(url, timeoutMs)
                if (status == 429) {
                    val after = headers["retry-after"]?.trim()?.toIntOrNull()
                    val s = if (after != null && after > 0) after else 60
                    if (s > MAX_INLINE_WAIT_S || attempt >= retries) {
                        openMeteoBlockedAt = System.currentTimeMillis() + s * 1000L
                        throw OpenMeteoRateLimitException("Open-Meteo limit reached (HTTP 429; ask again in $s s)", s)
                    }
                    wait = s * 1000L
                    throw WeatherHttpException("Open-Meteo limit reached (HTTP 429)", 429)
                }
                val ok = status in 200..299
                val j: Any = try {
                    val t = text.trimStart()
                    if (t.startsWith("[")) JSONArray(t) else JSONObject(t)
                } catch (e: Exception) {
                    retryable = ok || status >= 500
                    throw WeatherHttpException("Open-Meteo request failed (HTTP $status: ${text.trim().take(120).ifEmpty { "empty response" }})", status)
                }
                val error = (j as? JSONObject)?.optBoolean("error", false) ?: false
                if (!ok || error) {
                    retryable = status >= 500
                    val reason = (j as? JSONObject)?.optString("reason")?.takeIf { it.isNotEmpty() }
                    throw WeatherHttpException("Open-Meteo request failed (HTTP $status${if (reason != null) ": $reason" else ""})", status)
                }
                return j
            } catch (e: IOException) {
                if (e is OpenMeteoRateLimitException || !retryable || attempt >= retries) throw e
                Thread.sleep(wait)
                attempt++
            }
        }
    }

    /** The number at index i of a JSON array, or NaN for null / missing. */
    @JvmStatic
    fun num(a: JSONArray?, i: Int): Double = if (a == null || i >= a.length() || a.isNull(i)) Double.NaN else a.optDouble(i, Double.NaN)

    /** The grid cell an Open-Meteo answer came from: its "latitude" / "longitude" are the cell's centre, not the point asked. */
    @JvmStatic
    fun reportedCell(o: JSONObject): DoubleArray? {
        val lat = numOrNull(o, "latitude") ?: return null
        val lon = numOrNull(o, "longitude") ?: return null
        return doubleArrayOf(lat, lon)
    }

    /** A JSON number, or null when absent / null. */
    @JvmStatic
    fun numOrNull(o: JSONObject, key: String): Double? = if (!o.has(key) || o.isNull(key)) null else o.optDouble(key, Double.NaN).takeIf { it.isFinite() }

    // ------------------------------------------------------------------------------------------- shared helpers

    /** Hourly values (unix seconds, value at that instant) as intervals centred on each hour. */
    @JvmStatic
    fun hourlySeries(times: JSONArray?, values: JSONArray?): List<AtmosphereGuard.Interval> {
        val out = ArrayList<AtmosphereGuard.Interval>()
        if (times == null) return out
        for (i in 0 until times.length()) {
            val v = num(values, i)
            if (!v.isFinite()) continue
            val t = times.getLong(i) * 1000
            out.add(AtmosphereGuard.Interval(t - 1800000, t + 1800000, v))
        }
        return out
    }

    /** Value at time t, linear between interval midpoints; NaN outside the series. */
    @JvmStatic
    fun valueAt(series: List<AtmosphereGuard.Interval>, t: Double): Double {
        if (series.isEmpty() || t < series[0].start || t > series[series.size - 1].end) return Double.NaN
        fun mid(i: AtmosphereGuard.Interval) = (i.start + i.end) / 2.0
        if (t <= mid(series[0])) return series[0].value
        for (i in 0 until series.size - 1) {
            val a = mid(series[i])
            val b = mid(series[i + 1])
            if (t in a..b) return series[i].value + (series[i + 1].value - series[i].value) * (t - a) / (b - a)
        }
        return series[series.size - 1].value
    }

    /** Rough time of sunrise / sunset (epoch ms), good to well under an hour: enough to pick the hourly value. */
    @JvmStatic
    fun approximateEventTime(date: LocalDate, lat: Double, lon: Double, event: SolarEvent): Double {
        val deg = PI / 180
        val day0 = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli().toDouble()
        val j2000 = Instant.parse("2000-01-01T12:00:00Z").toEpochMilli().toDouble()
        val n = (day0 - j2000) / 86400000 + 0.0008 - lon / 360 + 0.5
        val M = (357.5291 + 0.98560028 * n) % 360
        val C = 1.9148 * sin(M * deg) + 0.02 * sin(2 * M * deg)
        val L = (M + C + 180 + 102.9372) % 360
        val noonJ = 2451545 + n + 0.0053 * sin(M * deg) - 0.0069 * sin(2 * L * deg)
        val dec = asin(sin(L * deg) * sin(23.44 * deg))
        val cosH = (sin(-0.833 * deg) - sin(lat * deg) * sin(dec)) / (kotlin.math.cos(lat * deg) * kotlin.math.cos(dec))
        val H = acos(max(-1.0, min(1.0, cosH))) / deg
        val J = noonJ + (if (event == SolarEvent.SUNRISE) -H else H) / 360
        return (J - 2440587.5) * 86400000
    }
}
