package com.ej.rovadiahyosefcalendar.classes.royzmanim

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/**
 * Terrain horizons for visible sunrise / sunset, from the refraction server's /v1/horizon (port of horizon-client.js).
 * The server computes a set once per place and caches it; keep it with the location. A place's very first set can take the
 * server minutes: the request then fails with [HorizonPendingException], whose retryAfterS says when to ask again.
 */
object HorizonClient {

    /** A place's first horizon, still being computed on the server: ask again after retryAfterS seconds. */
    class HorizonPendingException(message: String, val retryAfterS: Int, val etaS: Int?) : java.io.IOException(message)

    /** A horizon's terrain, as /v1/terrain-version gives it. */
    data class TerrainVersion(val fine: String?, val layers: String?, val algo: Int)

    /** A horizon set as the server sent it: the raw JSON (to store) and the parsed set (for the calculator). */
    class ServerHorizon(val json: JSONObject) {
        val set: HorizonSet = parseSet(json)
        val areaName: String? = json.optJSONObject("area")?.optString("name")?.takeIf { it.isNotEmpty() }
        val terrain: TerrainVersion get() = terrainOf(json)
    }

    private fun query(lat: Double, lon: Double, area: Boolean, radiusKm: Double?, heightM: Double?, moon: Boolean, version: String?, outline: Boolean = false): String {
        val q = ArrayList<String>()
        q.add("lat=" + String.format(Locale.ROOT, "%.6f", lat))
        q.add("lon=" + String.format(Locale.ROOT, "%.6f", lon))
        if (area) q.add("area=auto")
        if (area && outline) q.add("outline=1")
        if (radiusKm != null && radiusKm != 0.0) q.add("radius_km=$radiusKm")
        if (heightM != null) q.add("height=$heightM")
        if (moon) q.add("moon=1")
        if (version != null) q.add("v=" + URLEncoder.encode(version, "UTF-8"))
        return q.joinToString("&")
    }

    private fun get(url: String, what: String): JSONObject {
        try {
            return WeatherHttp.getJson(url, 60000, what)
        } catch (e: WeatherHttpException) {
            val body = e.body
            if (e.status == 503 && body != null && body.optBoolean("computing", false)) {
                val retry = body.optInt("retryAfterS", 0).takeIf { it > 0 } ?: 15
                throw HorizonPendingException("$what: ${body.optString("error", "computing")}", retry, body.optInt("etaS", 0).takeIf { it > 0 })
            }
            throw e
        }
    }

    /** The horizon set for a point (radiusKm 0, the eye heightM above sea level) or for vantage points within radiusKm. */
    @JvmStatic
    @JvmOverloads
    fun fetchHorizon(baseUrl: String, lat: Double, lon: Double, radiusKm: Double? = null, heightM: Double? = null, version: String? = null): ServerHorizon =
        ServerHorizon(get("${baseUrl.trimEnd('/')}/v1/horizon?${query(lat, lon, false, radiusKm, heightM, false, version)}", "horizon"))

    /**
     * Vantage search over the official area (neighbourhood, village, city) containing the point; the server picks the area. The
     * answer's `area` carries its outline (`polygons`), so a stored horizon can be reused exactly inside it.
     */
    @JvmStatic
    @JvmOverloads
    fun fetchHorizonForArea(baseUrl: String, lat: Double, lon: Double, moon: Boolean = false, version: String? = null): ServerHorizon =
        ServerHorizon(get("${baseUrl.trimEnd('/')}/v1/horizon?${query(lat, lon, true, null, null, moon, version, outline = true)}", "horizon"))

    /** What the server would build the area's horizon from, without computing it (a few bytes). */
    @JvmStatic
    fun fetchTerrainVersion(baseUrl: String, lat: Double, lon: Double, area: Boolean = true, moon: Boolean = false): TerrainVersion =
        terrainOf(get("${baseUrl.trimEnd('/')}/v1/terrain-version?${query(lat, lon, area, null, null, moon, null)}", "terrain version"), versionAnswer = true)

    /** The terrain a horizon set was built from (or a /v1/terrain-version answer). */
    @JvmStatic
    fun terrainOf(j: JSONObject, versionAnswer: Boolean = false): TerrainVersion {
        val t = if (versionAnswer) j else j.optJSONObject("terrain") ?: JSONObject()
        val fineAny = t.opt("fine")
        val fine = when (fineAny) {
            is JSONObject -> fineAny.optString("version").ifEmpty { fineAny.optString("signature") }.ifEmpty { null }
            is String -> fineAny.ifEmpty { null }
            else -> null
        }
        return TerrainVersion(fine, t.optString("layers").takeIf { it.isNotEmpty() && !t.isNull("layers") }, t.optInt("algo", 1))
    }

    @JvmStatic
    fun sameTerrain(a: TerrainVersion, b: TerrainVersion) = a.fine == b.fine && a.layers == b.layers && a.algo == b.algo

    private fun parseProfile(a: JSONArray?): HorizonProfile? {
        if (a == null || a.length() == 0) return null
        val pts = (0 until a.length()).map { i ->
            val p = a.getJSONObject(i)
            if (p.has("elevationDeg") && !p.isNull("elevationDeg")) ObservedHorizonPoint(p.getDouble("azimuthDeg"), p.getDouble("elevationDeg"))
            else {
                val alts = p.optJSONArray("alternates")
                DemHorizonPoint(
                    p.getDouble("azimuthDeg"), p.getDouble("distanceKm"), p.getDouble("heightM"),
                    if (alts == null) emptyList() else (0 until alts.length()).map { k -> alts.getJSONArray(k).let { doubleArrayOf(it.getDouble(0), it.getDouble(1)) } },
                    WeatherHttp.numOrNull(p, "floorM"),
                )
            }
        }
        return HorizonProfile(pts)
    }

    /** A HorizonSet from the server's JSON. */
    @JvmStatic
    fun parseSet(j: JSONObject): HorizonSet {
        val points = j.optJSONArray("points") ?: JSONArray()
        return HorizonSet(j.optString("mode", "point"), (0 until points.length()).map { i ->
            val p = points.getJSONObject(i)
            val wins = HashMap<SolarEvent, List<DoubleArray>>()
            p.optJSONObject("wins")?.let { w ->
                for (ev in SolarEvent.entries) {
                    val a = w.optJSONArray(ev.name.lowercase()) ?: continue
                    wins[ev] = (0 until a.length()).map { k -> a.getJSONArray(k).let { doubleArrayOf(it.getDouble(0), it.getDouble(1)) } }
                }
            }
            HorizonSetPoint(p.getDouble("lat"), p.getDouble("lon"), p.optDouble("ground", 0.0), p.optDouble("height", 0.0),
                parseProfile(p.optJSONArray("sunrise")), parseProfile(p.optJSONArray("sunset")), wins)
        }, j.optJSONObject("terrain")?.optInt("algo", 1))
    }
}
