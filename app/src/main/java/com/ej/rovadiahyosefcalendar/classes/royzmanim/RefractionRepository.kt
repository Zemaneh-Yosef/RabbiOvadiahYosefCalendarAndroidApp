package com.ej.rovadiahyosefcalendar.classes.royzmanim

import android.content.Context
import android.util.Log
import com.ej.rovadiahyosefcalendar.classes.royzmanim.StarNightfall.HazeData
import com.kosherjava.zmanim.util.GeoLocation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Everything the refraction-corrected zmanim need for one place, as the website's refraction-data.js /
 * refraction-snapshot.js / auto-atmosphere.js load it:
 *
 *  - provider:  the air per sunrise / sunset, best first: Open-Meteo path profiles (16 days; else a one-temperature forecast,
 *               NWS in the US or Open-Meteo), the refraction server's profiles (its forecast and climatology), the monthly
 *               normals at the spot (ERA5 via Open-Meteo, else NOAA's station normals), else the calculator's default model;
 *  - horizon:   the terrain horizon set of the official area around the point, for the visible sunrise;
 *  - haze:      CAMS's haze (averages and forecast), light pollution and the AERONET calibration, for Tzet by the stars.
 *
 * Stored per place in the app's files (the last [MAX_LOCATIONS] places): the horizon and normals until the server's terrain
 * changes, the forecast as a snapshot of the provider's answers (used with no network while younger than
 * [FORECAST_TTL_MS], shown while a new one loads up to [FORECAST_MAX_AGE_MS]), the haze. [cached] reads it synchronously, for
 * the first render; [load] fetches what is stale (blocking: call it off the main thread, or use [loadAsync]).
 */
object RefractionRepository {
    private const val TAG = "RefractionRepository"
    const val REFRACTION_SERVER = "https://hanetz.royzmanim.com/selfhost"
    const val FORECAST_TTL_MS = 3 * 60 * 60 * 1000L
    const val FORECAST_MAX_AGE_MS = 48 * 60 * 60 * 1000L
    private const val FORECAST_DAYS = 33
    const val HAZE_TTL_MS = 6 * 60 * 60 * 1000L
    private const val HAZE_CALIBRATION_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    private const val HAZE_NORMALS_MAX_AGE_MS = 365L * 24 * 60 * 60 * 1000
    private const val MAX_LOCATIONS = 30
    private const val CACHE_VERSION = 2
    /** Bump when stored horizons must be fetched again everywhere (as the website's TERRAIN_VERSION). */
    const val TERRAIN_VERSION = "bare-earth-2"
    private const val TERRAIN_CHECK_MS = 24 * 60 * 60 * 1000L

    /** What a calculator is built with for a place; every field may be missing (offline, first run). */
    class RefractionData(
        val provider: AtmosphereProvider?,
        val horizon: HorizonSet?,
        val horizonAreaName: String?,
        val normals: EventNormals?,
        val haze: HazeData?,
        val forecastFetchedAt: Long?,
        val fromCache: Boolean,
        val notes: List<String>,
        val horizonRetryS: Int? = null,
    )

    @Volatile private var dir: File? = null
    private val memory = ConcurrentHashMap<String, RefractionData>()
    private val referenceHaze = ConcurrentHashMap<String, HazeData>()
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "refraction-loader").apply { isDaemon = true } }
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(Double, Double) -> Unit>()

    /** Call once with any context (the files live in the app's private storage). */
    @JvmStatic
    fun init(context: Context) {
        if (dir != null) return
        val files = context.applicationContext.filesDir
        // earlier layouts are not read: drop them
        for (v in 1 until CACHE_VERSION) File(files, "refraction-v$v").takeIf { it.exists() }?.deleteRecursively()
        dir = File(files, "refraction-v$CACHE_VERSION").apply { mkdirs() }
    }

    /** Called (on the loader thread) when better data for a place arrives: recompute and redraw there. */
    @JvmStatic
    fun addListener(l: (Double, Double) -> Unit) { listeners.add(l) }

    @JvmStatic
    fun removeListener(l: (Double, Double) -> Unit) { listeners.remove(l) }

    /** Tell the listeners that something for a place (data, a visible sunrise) is now better. */
    @JvmStatic
    fun notifyListeners(lat: Double, lon: Double) = listeners.forEach { try { it(lat, lon) } catch (e: Exception) { Log.e(TAG, "listener failed", e) } }

    private fun placeId(lat: Double, lon: Double) = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon)

    // ------------------------------------------------------------------------------------- which stored place serves a location
    //
    // A new GPS fix (every pull to refresh) lands metres from the last one. Each kind of data is reused from a stored place only
    // where its own source would answer with the same data - the same cell of the same grid - so neighbouring cells never share:
    //
    //  - horizon:   the server builds it for the official area (neighbourhood, village, city) containing the point, so it serves
    //               every location inside that area's outline (`outline=1`; just its box from a server without outlines - the
    //               smallest box where they overlap). A point in no known area gets a horizon for the point itself, which serves
    //               only within GPS noise.
    //  - forecast:  the weather model's cell around the observer, as Open-Meteo reported it. Several models are on projected
    //               grids (and Europe's mixes them), so a location must lie within half a cell of the reported centre: the
    //               circle inside the cell ([forecastCellRadiusM]).
    //  - normals:   the reanalysis cell Open-Meteo reported (ERA5-Land, 0.1 degrees; a coarser ERA5 cell holds that square too).
    //  - haze:      the CAMS cell Open-Meteo reported (0.4 degrees); a location within 0.125 degrees of its centre is surely in it.
    //  - sky:       the light-pollution atlas pixel the refraction server reads (30 arc-seconds from 85.0542 N, 180 W - as its
    //               light_pollution.py locates them), computed directly.
    //
    // Data whose source reported no cell (NWS or NOAA station fallbacks, a horizon for a point in no known area) serve only within
    // GPS noise.

    private const val GPS_NOISE_M = 25.0
    private const val NORMALS_HALF_CELL_DEG = 0.05
    private const val HAZE_HALF_CELL_DEG = 0.125
    // the World Atlas grid as the refraction server reads it (zemaneh-shamayim light_pollution.py: TOP, LEFT, PX)
    private const val SKY_TOP = 85.0541668645001
    private const val SKY_LEFT = -180.00000000000003
    private const val SKY_PX = 0.008333330000000002
    private fun skyPixel(lat: Double, lon: Double) = Pair(Math.floor((SKY_TOP - lat) / SKY_PX), Math.floor((lon - SKY_LEFT) / SKY_PX))

    /** Half the cell of the finest model behind a place's forecast (see [OpenMeteoPath.chooseModel]): the circle inside it. */
    private fun forecastCellRadiusM(lat: Double, lon: Double): Double {
        val lonM = 111320.0 * kotlin.math.cos(Math.toRadians(lat))
        return when (OpenMeteoPath.chooseModel(lat, lon)) {
            "best_match" -> 650.0                     // Europe: AROME 1.3 km
            "gem_seamless" -> 1_250.0                 // Canada: HRDPS 2.5 km
            "icon_seamless" -> 0.03125 * lonM         // Israel: ICON-EU 0.0625 degrees
            else -> if (lat in 24.0..50.0 && lon in -125.0..-66.0) 1_500.0 // contiguous US: HRRR 3 km
                    else 0.055 * lonM                 // elsewhere: GFS 0.11 degrees
        }
    }

    private enum class Kind(val file: String) { HORIZON("static"), FORECAST("forecast"), NORMALS("normals"), HAZE("haze"), SKY("sky") }

    private class Spot(val lat: Double, val lon: Double) { val id = placeId(lat, lon) }

    /** The stored place each kind of data for a location comes from. */
    private class Places(val raw: Spot, val horizon: Spot, val forecast: Spot, val normals: Spot, val haze: Spot, val sky: Spot) {
        val key = "${horizon.id}|${forecast.id}|${normals.id}|${haze.id}|${sky.id}"
        val ids get() = setOf(horizon.id, forecast.id, normals.id, haze.id, sky.id)
    }

    private val known = ConcurrentHashMap<Kind, java.util.concurrent.CopyOnWriteArrayList<Spot>>()
    /**
     * Per place, what its stored data cover (the small "<place>.cells.json"): "horizon" {hasHorizon, bbox [s, w, n, e],
     * polygons (the area's outline as the server's areas.py keeps it: polygons of rings, outer first, each [lon, lat, ...])},
     * "forecast" / "normals" / "haze" [lat, lon] (the cell centre the source reported).
     */
    private val cells = ConcurrentHashMap<String, JSONObject>()
    @Volatile private var knownLoaded = false

    private fun spotOf(fileName: String, suffix: String): Spot? {
        // "<lat>_<lon>.<kind>.json"
        val parts = fileName.removeSuffix(suffix).split('_')
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lon = parts.getOrNull(1)?.toDoubleOrNull()
        return if (parts.size == 2 && lat != null && lon != null) Spot(lat, lon) else null
    }

    @Synchronized
    private fun loadKnown() {
        if (knownLoaded) return
        val d = dir ?: return
        knownLoaded = true
        d.listFiles { f -> f.name.endsWith(".cells.json") }?.forEach { f ->
            val s = spotOf(f.name, ".cells.json") ?: return@forEach
            readJson(f)?.let { cells[s.id] = it }
        }
        for (kind in Kind.entries) {
            val suffix = ".${kind.file}.json"
            d.listFiles { f -> f.name.endsWith(suffix) && !f.name.startsWith("reference_") }?.forEach { f ->
                val s = spotOf(f.name, suffix) ?: return@forEach
                if (kind != Kind.HORIZON || cells[s.id]?.optJSONObject("horizon")?.optBoolean("hasHorizon") == true) remember(kind, s)
            }
        }
    }

    private fun remember(kind: Kind, s: Spot) {
        val list = known.getOrPut(kind) { java.util.concurrent.CopyOnWriteArrayList() }
        if (list.none { it.id == s.id }) list.add(s)
    }

    /** Record what a place's data of one kind cover. */
    @Synchronized
    private fun setCell(id: String, kind: String, value: Any) {
        val j = JSONObject((cells[id] ?: JSONObject()).toString()).put(kind, value)
        cells[id] = j
        writeJson(null, fileFor(id, "cells"), j)
    }

    private fun cellOf(lat: Double, lon: Double) = JSONArray().put(lat).put(lon)

    /** What is kept of a horizon to find it again: whether there is one, and its area's box. */
    private fun areaJson(horizon: JSONObject?) = JSONObject().apply {
        put("hasHorizon", horizon != null)
        val area = horizon?.optJSONObject("area")
        area?.optJSONArray("bbox")?.takeIf { it.length() == 4 }?.let { put("bbox", it) }
        area?.optJSONArray("polygons")?.takeIf { it.length() > 0 }?.let { put("polygons", it) }
    }

    /** Even-odd over every ring of every polygon, as the server's areas.area_contains (rings flat [lon, lat, lon, lat, ...]). */
    private fun outlineContains(polygons: JSONArray, lat: Double, lon: Double): Boolean {
        for (p in 0 until polygons.length()) {
            val rings = polygons.optJSONArray(p) ?: continue
            var inside = false
            for (k in 0 until rings.length()) {
                val r = rings.optJSONArray(k) ?: continue
                var c = false
                var j = r.length() - 2
                var i = 0
                while (i + 1 < r.length()) {
                    val xi = r.optDouble(i); val yi = r.optDouble(i + 1); val xj = r.optDouble(j); val yj = r.optDouble(j + 1)
                    if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) c = !c
                    j = i
                    i += 2
                }
                if (c) inside = !inside
            }
            if (inside) return true
        }
        return false
    }

    private fun metres(aLat: Double, aLon: Double, lat: Double, lon: Double): Double {
        val dy = (aLat - lat) * 111320.0
        val dx = (aLon - lon) * 111320.0 * kotlin.math.cos(Math.toRadians(lat))
        return kotlin.math.hypot(dx, dy)
    }

    /** Whether a stored place's data of this kind are the data its source would give at (lat, lon). */
    private fun serves(kind: Kind, s: Spot, lat: Double, lon: Double): Boolean {
        val c = cells[s.id]
        fun centre(name: String) = c?.optJSONArray(name)?.takeIf { it.length() == 2 }?.let { doubleArrayOf(it.optDouble(0), it.optDouble(1)) }
        fun lonDiff(a: Double, b: Double) = Math.abs(((a - b + 540.0) % 360.0) - 180.0)
        return when (kind) {
            Kind.HORIZON -> {
                val h = c?.optJSONObject("horizon")
                val b = h?.optJSONArray("bbox")
                if (b != null) lat in b.optDouble(0)..b.optDouble(2) && lon in b.optDouble(1)..b.optDouble(3)
                    && (h.optJSONArray("polygons")?.let { outlineContains(it, lat, lon) } ?: true)
                else metres(s.lat, s.lon, lat, lon) <= GPS_NOISE_M
            }
            Kind.FORECAST -> centre("forecast")?.let { metres(it[0], it[1], lat, lon) <= forecastCellRadiusM(lat, lon) }
                ?: (metres(s.lat, s.lon, lat, lon) <= GPS_NOISE_M)
            Kind.NORMALS -> centre("normals")?.let { Math.abs(it[0] - lat) <= NORMALS_HALF_CELL_DEG && lonDiff(it[1], lon) <= NORMALS_HALF_CELL_DEG }
                ?: (metres(s.lat, s.lon, lat, lon) <= GPS_NOISE_M)
            Kind.HAZE -> centre("haze")?.let { Math.abs(it[0] - lat) <= HAZE_HALF_CELL_DEG && lonDiff(it[1], lon) <= HAZE_HALF_CELL_DEG }
                ?: (metres(s.lat, s.lon, lat, lon) <= GPS_NOISE_M)
            Kind.SKY -> skyPixel(s.lat, s.lon) == skyPixel(lat, lon)
        }
    }

    /** The stored place whose data of this kind serve (lat, lon): for the horizon the smallest area, else the nearest. */
    private fun storedFor(kind: Kind, lat: Double, lon: Double): Spot? {
        val hits = known[kind]?.filter { serves(kind, it, lat, lon) } ?: return null
        if (kind == Kind.HORIZON) {
            hits.mapNotNull { s -> cells[s.id]?.optJSONObject("horizon")?.optJSONArray("bbox")?.let { b -> s to (b.optDouble(2) - b.optDouble(0)) * (b.optDouble(3) - b.optDouble(1)) } }
                .minByOrNull { it.second }?.let { return it.first }
        }
        return hits.minByOrNull { metres(it.lat, it.lon, lat, lon) }
    }

    private fun places(lat: Double, lon: Double): Places {
        loadKnown()
        val raw = Spot(lat, lon)
        return Places(
            raw,
            storedFor(Kind.HORIZON, lat, lon) ?: raw,
            storedFor(Kind.FORECAST, lat, lon) ?: raw,
            storedFor(Kind.NORMALS, lat, lon) ?: raw,
            storedFor(Kind.HAZE, lat, lon) ?: raw,
            storedFor(Kind.SKY, lat, lon) ?: raw,
        )
    }

    private fun readNormals(id: String): EventNormals? = EventNormals.fromJson(readJson(fileFor(id, Kind.NORMALS.file)))

    /** Light pollution (cd/m2). */
    private fun readSky(id: String): Double? = readJson(fileFor(id, Kind.SKY.file))?.let { WeatherHttp.numOrNull(it, "blpCdM2") }
    private fun fileFor(id: String, kind: String) = dir?.let { File(it, "${id.replace(',', '_')}.$kind.json") }

    private fun readJson(f: File?): JSONObject? = try { if (f != null && f.isFile) JSONObject(f.readText()) else null } catch (e: Exception) { null }

    private fun writeJson(id: String?, f: File?, j: JSONObject) {
        if (f == null) return
        try {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(j.toString())
            tmp.renameTo(f)
            if (id != null) prune(id)
        } catch (e: Exception) {
            Log.w(TAG, "could not store ${f.name}", e)
        }
    }

    /** The places the location being loaded draws on (each kind may come from a different one): never pruned. */
    @Volatile private var inUse: Set<String> = emptySet()

    /** Keep the files of the last MAX_LOCATIONS places (by last write). */
    private fun prune(keepId: String) {
        val d = dir ?: return
        val files = d.listFiles { f -> f.name.endsWith(".json") && !f.name.startsWith("reference_") } ?: return
        val byPlace = files.groupBy { it.name.substringBefore('.') }
        if (byPlace.size <= MAX_LOCATIONS) return
        val keep = (inUse + keepId).map { it.replace(',', '_') }.toSet()
        byPlace.entries.filter { it.key !in keep }.sortedBy { e -> e.value.maxOf { it.lastModified() } }
            .take(byPlace.size - MAX_LOCATIONS).forEach { e ->
                e.value.forEach { it.delete() }
                val id = e.key.replace('_', ',')
                known.values.forEach { l -> l.removeAll { it.id == id } }
                cells.remove(id)
            }
    }

    // ------------------------------------------------------------------------------------- snapshot (stored forecast)

    private fun specToJson(spec: AtmosphereSpec): JSONObject = when (spec) {
        is SurfaceAir -> JSONObject().apply {
            put("temperatureC", spec.temperatureC)
            spec.pressureMb?.let { put("pressureMb", it) }; spec.heightM?.let { put("heightM", it) }
            spec.dewPointC?.let { put("dewPointC", it) }; spec.relativeHumidity?.let { put("relativeHumidity", it) }
            spec.source?.let { put("source", it) }; spec.key?.let { put("key", it) }
        }
        is PathAir -> JSONObject().apply {
            put("key", spec.identityKey); spec.source?.let { put("source", it) }
            put("path", JSONArray().also { a ->
                for (p in spec.path) a.put(JSONObject().apply {
                    put("distanceKm", p.distanceKm); if (p.water) put("water", true); p.skinC?.let { put("skinC", it) }
                    put("levels", JSONArray().also { la ->
                        for (l in p.levels) la.put(JSONObject().apply {
                            put("h", l.h); put("t", l.t); put("p", l.p); l.td?.let { put("td", it) }; l.rh?.let { put("rh", it) }
                        })
                    })
                })
            })
        }
    }

    private fun specFromJson(j: JSONObject): AtmosphereSpec? {
        val path = j.optJSONArray("path")
        if (path != null) return PathAir(ServerPathAtmosphere.parseProfiles(path), j.optString("key").ifEmpty { null }, j.optString("source").ifEmpty { null })
        if (!j.has("temperatureC")) return null
        return SurfaceAir(j.getDouble("temperatureC"), WeatherHttp.numOrNull(j, "pressureMb"), WeatherHttp.numOrNull(j, "heightM"),
            WeatherHttp.numOrNull(j, "dewPointC"), WeatherHttp.numOrNull(j, "relativeHumidity"), j.optString("source").ifEmpty { null }, j.optString("key").ifEmpty { null })
    }

    /** The provider's answers for every sunrise / sunset in a range (what is stored of a forecast). */
    private fun snapshotProvider(provider: AtmosphereProvider, geo: GeoLocation, from: LocalDate, days: Int): JSONObject {
        val table = JSONObject()
        for (i in 0 until days) {
            val date = from.plusDays(i.toLong())
            for (ev in SolarEvent.entries) {
                val spec = try { provider.airFor(date, ev, geo) } catch (e: Exception) { null } ?: continue
                table.put("$date|${ev.name.lowercase()}", specToJson(spec))
            }
        }
        return table
    }

    private fun providerFromSnapshot(table: JSONObject): AtmosphereProvider {
        val parsed = HashMap<String, AtmosphereSpec?>()
        return AtmosphereProvider { date, event, _ ->
            val k = "$date|${event.name.lowercase()}"
            synchronized(parsed) {
                if (!parsed.containsKey(k)) parsed[k] = table.optJSONObject(k)?.let { specFromJson(it) }
                parsed[k]
            }
        }
    }

    private fun yesterdayAt(lon: Double): LocalDate =
        Instant.ofEpochMilli(System.currentTimeMillis() + (lon / 15 * 3600000).toLong()).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)

    // ------------------------------------------------------------------------------------- haze

    private fun hazeFromStored(st: JSONObject?, blpCdM2: Double? = null): HazeData? {
        if (st == null) return null
        val normals = HazeClient.seriesFromJson(st.optJSONObject("normals")) ?: return null
        if (normals.aod.isEmpty()) return null
        val fetchedAt = if (st.isNull("forecastFetchedAt")) null else st.optLong("forecastFetchedAt", 0).takeIf { it > 0 }
        val fresh = fetchedAt != null && System.currentTimeMillis() - fetchedAt < FORECAST_MAX_AGE_MS
        val range = st.optJSONArray("normalsRange")
        return HazeData(
            normals = normals,
            forecast = if (fresh) HazeClient.seriesFromJson(st.optJSONObject("forecast")) else null,
            blpCdM2 = blpCdM2,
            forecastFetchedAt = if (fresh) fetchedAt else null,
            normalsRange = Pair(range?.optString(0).orEmpty(), range?.optString(1).orEmpty()),
            source = st.optString("source"),
            forecastSource = if (fresh) st.optString("forecastSource").ifEmpty { null } else null,
            aodScale = WeatherHttp.numOrNull(st, "aodScale"),
        )
    }

    /**
     * Haze for a place: stored parts reused while fresh, the rest fetched (reference: normals and calibration only). The light
     * pollution comes from `sky`, which may be a different stored place (see [SKY_M]).
     */
    private fun loadHaze(lat: Double, lon: Double, sky: Spot?, notes: MutableList<String>, force: Boolean, reference: Boolean): HazeData? {
        val id = placeId(lat, lon)
        val file = if (reference) dir?.let { File(it, "reference_${id.replace(',', '_')}.haze.json") } else fileFor(id, "haze")
        val st = readJson(file)
        val now = System.currentTimeMillis()
        val normalsOk = st != null && st.optJSONObject("normals") != null && now - st.optLong("normalsFetchedAt", 0) < HAZE_NORMALS_MAX_AGE_MS
        val forecastOk = reference || (!force && st != null && !st.isNull("forecastFetchedAt") && st.has("forecastFetchedAt") && now - st.optLong("forecastFetchedAt", 0) < HAZE_TTL_MS)
        val calibrationOk = st != null && st.has("calibratedAt") && now - st.optLong("calibratedAt", 0) < HAZE_CALIBRATION_MAX_AGE_MS
        val normals = if (normalsOk) null else try { HazeClient.fetchHazeNormals(lat, lon) } catch (e: Exception) { notes.add("haze normals: ${e.message}"); null }
        var fcSrc: String? = null
        val fc = if (forecastOk) null else try {
            HazeClient.fetchHazeForecast(lat, lon).also { fcSrc = "cams" }
        } catch (e: Exception) {
            notes.add("haze forecast (Open-Meteo): ${e.message}")
            try { HazeClient.fetchGefsHazeForecast(lat, lon).also { fcSrc = "gefs" } } catch (e2: Exception) { notes.add("haze forecast (NOAA GEFS-Aerosols): ${e2.message}"); null }
        }
        val storedBlp = sky?.let { readSky(it.id) }
        val blp = if (reference || sky == null || storedBlp != null) null else try { HazeClient.fetchLightPollution(REFRACTION_SERVER, sky.lat, sky.lon) } catch (e: Exception) { notes.add("light pollution: ${e.message}"); null }
        if (blp != null && sky != null) { writeJson(sky.id, fileFor(sky.id, Kind.SKY.file), JSONObject().put("blpCdM2", blp)); remember(Kind.SKY, sky) }
        val scale = if (calibrationOk) null else try { HazeClient.fetchHazeCalibration(REFRACTION_SERVER, lat, lon) } catch (e: Exception) { notes.add("haze calibration: ${e.message}"); null }
        val baseNormals = normals?.let { HazeClient.seriesToJson(it.normals) } ?: st?.optJSONObject("normals") ?: return null
        val out = JSONObject().apply {
            put("normals", baseNormals)
            put("normalsRange", normals?.let { JSONArray(listOf(it.normalsRange.first, it.normalsRange.second)) } ?: st?.optJSONArray("normalsRange") ?: JSONArray())
            put("source", normals?.source ?: st?.optString("source").orEmpty())
            put("normalsFetchedAt", if (normals != null) now else st!!.optLong("normalsFetchedAt"))
            put("forecast", fc?.let { HazeClient.seriesToJson(it) } ?: st?.optJSONObject("forecast") ?: JSONObject.NULL)
            put("forecastFetchedAt", if (fc != null) now else if (st != null && st.has("forecastFetchedAt") && !st.isNull("forecastFetchedAt")) st.getLong("forecastFetchedAt") else JSONObject.NULL)
            put("forecastSource", fcSrc ?: st?.optString("forecastSource").orEmpty())
            put("aodScale", scale ?: st?.let { WeatherHttp.numOrNull(it, "aodScale") } ?: JSONObject.NULL)
            if (scale != null) put("calibratedAt", now) else if (st != null && st.has("calibratedAt")) put("calibratedAt", st.getLong("calibratedAt"))
        }
        if (normals != null || fc != null || scale != null) writeJson(if (reference) null else id, file, out)
        if (!reference) {
            normals?.cell?.let { setCell(id, "haze", cellOf(it[0], it[1])) }
            remember(Kind.HAZE, Spot(lat, lon))
        }
        return hazeFromStored(out, blp ?: storedBlp)
    }

    /** The average haze of a reference place (e.g. Jerusalem's): normals and calibration only, stored apart. Blocking. */
    @JvmStatic
    fun loadReferenceHaze(lat: Double, lon: Double): HazeData? {
        val notes = ArrayList<String>()
        val h = loadHaze(lat, lon, null, notes, force = false, reference = true)
        if (notes.isNotEmpty()) Log.i(TAG, "Reference haze: $notes")
        h?.let { referenceHaze[placeId(lat, lon)] = it }
        return h
    }

    /** The stored average haze of a reference place (null until loadReferenceHaze stored it). */
    @JvmStatic
    fun readCachedReferenceHaze(lat: Double, lon: Double): HazeData? {
        val id = placeId(lat, lon)
        referenceHaze[id]?.let { return it }
        return hazeFromStored(readJson(dir?.let { File(it, "reference_${id.replace(',', '_')}.haze.json") }))?.also { referenceHaze[id] = it }
    }

    // ------------------------------------------------------------------------------------- reading what is stored

    /** What earlier runs stored for this spot (memory first), for the first render; null when nothing is. Synchronous. */
    @JvmStatic
    fun cached(rawLat: Double, rawLon: Double): RefractionData? {
        if (dir == null) return null
        val p = places(rawLat, rawLon)
        memory[p.key]?.let { return it }
        val stat = readJson(fileFor(p.horizon.id, Kind.HORIZON.file))
        val fc = readJson(fileFor(p.forecast.id, Kind.FORECAST.file))?.takeIf { System.currentTimeMillis() - it.optLong("fetchedAt") <= FORECAST_MAX_AGE_MS }
        val haze = hazeFromStored(readJson(fileFor(p.haze.id, Kind.HAZE.file)), readSky(p.sky.id))
        val normals = readNormals(p.normals.id)
        if (stat == null && fc == null && haze == null && normals == null) return null
        val horizon = stat?.optJSONObject("horizon")?.let { try { HorizonClient.ServerHorizon(it) } catch (e: Exception) { null } }
        val provider = Refraction.chainProviders(fc?.optJSONObject("table")?.let { providerFromSnapshot(it) }, normals?.provider())
        val data = RefractionData(provider, horizon?.set, horizon?.areaName, normals, haze, fc?.optLong("fetchedAt"), true, emptyList())
        memory[p.key] = data
        return data
    }

    // ------------------------------------------------------------------------------------- loading

    /** Load in the background; listeners (and onDone) are told (with these coordinates) when it lands. */
    @JvmStatic
    @JvmOverloads
    fun loadAsync(lat: Double, lon: Double, force: Boolean = false, onDone: ((RefractionData) -> Unit)? = null) {
        executor.execute {
            try {
                val before = if (dir == null) null else memory[places(lat, lon).key]
                val data = load(lat, lon, force)
                onDone?.invoke(data)
                if (data !== before) listeners.forEach { try { it(lat, lon) } catch (e: Exception) { Log.e(TAG, "listener failed", e) } }
            } catch (e: Exception) {
                Log.e(TAG, "refraction data failed", e)
            }
        }
    }

    /**
     * Load everything for one spot (blocking). Never throws for network failures: missing pieces come back null and are
     * listed in `notes`. With a fresh stored forecast no forecast request is made at all. Each kind of data is read from, and
     * fetched for, the stored place that serves this spot for it (see [places]).
     */
    @JvmStatic
    @JvmOverloads
    fun load(rawLat: Double, rawLon: Double, force: Boolean = false): RefractionData {
        val p = places(rawLat, rawLon)
        inUse = p.ids
        val notes = ArrayList<String>()
        val now = System.currentTimeMillis()
        // reading a place counts as using it, for the pruning by age
        for (s in p.ids) for (k in Kind.entries) fileFor(s, k.file)?.takeIf { it.isFile }?.setLastModified(now)

        val stat = readJson(fileFor(p.horizon.id, Kind.HORIZON.file))
        val storedForecast = readJson(fileFor(p.forecast.id, Kind.FORECAST.file))
        val fresh = !force && storedForecast != null && now - storedForecast.optLong("fetchedAt") < FORECAST_TTL_MS
        val fcLat = p.forecast.lat
        val fcLon = p.forecast.lon
        val geo = GeoLocation(p.forecast.id, fcLat, fcLon, 0.0, java.time.ZoneOffset.UTC)

        val haze = loadHaze(p.haze.lat, p.haze.lon, p.sky, notes, force, reference = false)

        // the stored horizon, checked about once a day against the server's terrain, downloaded again only where that changed
        val hLat = p.horizon.lat
        val hLon = p.horizon.lon
        val storedHorizon = stat?.optJSONObject("horizon")
        var terrainCheckedAt = stat?.optLong("terrainCheckedAt", 0)?.takeIf { it > 0 }
        var horizonRetryS: Int? = null
        val horizonJson: JSONObject? = try {
            when {
                storedHorizon == null -> HorizonClient.fetchHorizonForArea(REFRACTION_SERVER, hLat, hLon, version = TERRAIN_VERSION).json.also { terrainCheckedAt = now }
                !force && terrainCheckedAt != null && now - terrainCheckedAt!! < TERRAIN_CHECK_MS -> storedHorizon
                else -> {
                    val current = HorizonClient.fetchTerrainVersion(REFRACTION_SERVER, hLat, hLon)
                    if (HorizonClient.sameTerrain(current, HorizonClient.terrainOf(storedHorizon))) { terrainCheckedAt = now; storedHorizon }
                    else {
                        notes.add("terrain changed here: horizon downloaded again")
                        HorizonClient.fetchHorizonForArea(REFRACTION_SERVER, hLat, hLon, version = "$TERRAIN_VERSION.${current.layers ?: "-"}.${current.fine ?: "-"}").json.also { terrainCheckedAt = now }
                    }
                }
            }
        } catch (e: Exception) {
            notes.add("horizon: ${e.message}")
            if (e is HorizonClient.HorizonPendingException) horizonRetryS = e.retryAfterS
            storedHorizon
        }
        val horizon = horizonJson?.let { try { HorizonClient.ServerHorizon(it) } catch (e: Exception) { notes.add("horizon: ${e.message}"); null } }
        if (horizonJson != null && (horizonJson !== storedHorizon || terrainCheckedAt != stat?.optLong("terrainCheckedAt"))) {
            writeJson(p.horizon.id, fileFor(p.horizon.id, Kind.HORIZON.file), JSONObject().apply {
                put("horizon", horizonJson)
                terrainCheckedAt?.let { put("terrainCheckedAt", it) }
            })
            setCell(p.horizon.id, "horizon", areaJson(horizonJson))
            remember(Kind.HORIZON, p.horizon)
        }

        var normals = readNormals(p.normals.id)
        if (normals == null) {
            val nLat = p.normals.lat
            val nLon = p.normals.lon
            normals = try { OpenMeteo.fetchNormals(nLat, nLon) } catch (e: Exception) { notes.add("Open-Meteo normals: ${e.message}"); null }
            if (normals == null) normals = try { Nws.fetchNearestNormals(nLat, nLon) } catch (e: Exception) { null }
            if (normals != null) {
                writeJson(p.normals.id, fileFor(p.normals.id, Kind.NORMALS.file), normals.toJson())
                normals.cell?.let { setCell(p.normals.id, "normals", cellOf(it[0], it[1])) }
                remember(Kind.NORMALS, p.normals)
            }
        }

        val result: RefractionData
        if (fresh) {
            // ── fresh forecast in storage: no Open-Meteo request ──
            notes.add("forecast from storage (${(now - storedForecast!!.optLong("fetchedAt")) / 60000} min old)")
            result = RefractionData(
                Refraction.chainProviders(providerFromSnapshot(storedForecast.getJSONObject("table")), normals?.provider()),
                horizon?.set, horizon?.areaName, normals, haze, storedForecast.optLong("fetchedAt"), true, notes, horizonRetryS,
            )
        } else {
            // ── fetch: path profiles (else one temperature), the server's profiles ──
            val path = try {
                OpenMeteoPath.create(fcLat, fcLon).also { o -> o.lastError?.let { notes.add("Open-Meteo path profiles (partly): ${it.message}") } }
            } catch (e: Exception) { notes.add("Open-Meteo path profiles: ${e.message}"); null }
            val single: FetchedProvider? = if (path != null) null else {
                try { Nws.createAtmosphere(fcLat, fcLon) } catch (e: Exception) {
                    if (!(e.message ?: "").let { it.contains("(404)") || it.contains("US only") }) notes.add("NWS: ${e.message}")
                    try { OpenMeteo.createAtmosphere(fcLat, fcLon) } catch (e2: Exception) { notes.add("Open-Meteo forecast: ${e2.message}"); null }
                }
            }
            val serverPaths = try {
                ServerPathAtmosphere(REFRACTION_SERVER, fcLat, fcLon).also { o ->
                    o.lastError?.let { notes.add("refraction server: ${it.message}") } ?: run { if (o.eventCount == 0) notes.add("refraction server: no data for this place") }
                }
            } catch (e: Exception) { notes.add("refraction server: ${e.message}"); null }
            val forecast: AtmosphereProvider? = path ?: single
            val gotForecast = forecast != null || (serverPaths?.eventCount ?: 0) > 0
            val provider = Refraction.chainProviders(forecast, serverPaths, normals?.provider())
            if (gotForecast) {
                writeJson(p.forecast.id, fileFor(p.forecast.id, Kind.FORECAST.file), JSONObject().apply {
                    put("fetchedAt", now)
                    put("table", snapshotProvider(provider, geo, yesterdayAt(fcLon), FORECAST_DAYS))
                })
                path?.cell?.let { setCell(p.forecast.id, "forecast", cellOf(it[0], it[1])) }
                remember(Kind.FORECAST, p.forecast)
                result = RefractionData(provider, horizon?.set, horizon?.areaName, normals, haze, now, false, notes, horizonRetryS)
            } else if (storedForecast != null && now - storedForecast.optLong("fetchedAt") <= FORECAST_MAX_AGE_MS) {
                // offline / every source failed: a stored forecast (even past its TTL) beats the normals
                notes.add("forecast download failed; using the stored one")
                result = RefractionData(Refraction.chainProviders(providerFromSnapshot(storedForecast.getJSONObject("table")), normals?.provider()),
                    horizon?.set, horizon?.areaName, normals, haze, storedForecast.optLong("fetchedAt"), true, notes, horizonRetryS)
            } else {
                result = RefractionData(normals?.provider(), horizon?.set, horizon?.areaName, normals, haze, null, false, notes, horizonRetryS)
            }
        }
        if (notes.isNotEmpty()) Log.i(TAG, "refraction data for ${p.key}: $notes")
        // what was just stored may now serve this spot from other places: key the result both ways
        memory[p.key] = result
        memory[places(rawLat, rawLon).key] = result
        return result
    }

    private val lastAttempt = ConcurrentHashMap<String, Long>()
    private const val RETRY_MS = 10 * 60 * 1000L

    /**
     * Load in the background when the place's forecast is stale (or there is none), at most every 10 minutes per place - so a
     * screen that rebuilds its calendar whenever new data arrive never loops while offline. Listeners are told when it lands.
     */
    @JvmStatic
    fun ensureFresh(rawLat: Double, rawLon: Double) {
        lastAsked = doubleArrayOf(rawLat, rawLon)
        if (dir == null || !RefractionSupport.enabled) return
        val key = places(rawLat, rawLon).key
        if (memory[key]?.fromCache == false && forecastStaleIn(rawLat, rawLon) > 0) return
        if (cached(rawLat, rawLon) != null && forecastStaleIn(rawLat, rawLon) > 0 && memory[key]?.horizon != null) return
        val now = System.currentTimeMillis()
        val last = lastAttempt[key]
        if (last != null && now - last < RETRY_MS) return
        lastAttempt[key] = now
        loadAsync(rawLat, rawLon)// listeners are told with the caller's coordinates
    }

    /** The place a screen last asked to keep fresh. */
    @Volatile private var lastAsked: DoubleArray? = null

    /**
     * For while the app is on screen: refresh the place last shown when its forecast has gone stale (so a screen left open
     * gets the new forecast every [FORECAST_TTL_MS], as on opening it), and say how long until it is worth checking again.
     * Listeners are told when new data land.
     */
    @JvmStatic
    fun refreshLastPlace(): Long {
        val p = lastAsked ?: return RETRY_MS
        ensureFresh(p[0], p[1])
        val staleIn = if (RefractionSupport.enabled) forecastStaleIn(p[0], p[1]) else 0
        // stale now (a load is under way, or failed offline): look again after the retry interval
        return if (staleIn > 0) staleIn + 1_000 else RETRY_MS
    }

    /** Milliseconds until a place's stored forecast should be fetched again (0 = now / none). */
    @JvmStatic
    fun forecastStaleIn(rawLat: Double, rawLon: Double): Long {
        val at = cached(rawLat, rawLon)?.forecastFetchedAt ?: return 0
        return maxOf(0, at + FORECAST_TTL_MS - System.currentTimeMillis())
    }
}
