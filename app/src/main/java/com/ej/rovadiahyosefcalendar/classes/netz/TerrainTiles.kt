package com.ej.rovadiahyosefcalendar.classes.netz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionRepository
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Terrain (Terrarium DEM) and satellite (Sentinel-2 cloudless) web-Mercator tiles, with an on-disk
 * cache (so a second visit works offline). Decoded tiles are packed in 16 bits (128 KB each, half of
 * a float / ARGB tile): a render holds a few hundred of them at once. See [heightAt] and [colourAt].
 * Every method returns null instead of throwing when a tile cannot be had.
 */
class TerrainTiles @JvmOverloads constructor(
    cacheDir: File,
    private val server: String = RefractionRepository.REFRACTION_SERVER,
    /**
     * The terrain the horizon being drawn was built from ([com.ej.rovadiahyosefcalendar.classes.royzmanim.HorizonSet.terrainKey]).
     * The server's tiles are then asked for and kept under that version, so the ridge drawn is the one the sunrise was computed
     * over, and a new terrain release replaces them; null (no horizon): the latest tiles, unversioned.
     */
    private val terrainKey: String? = null,
) {

    /**
     * Tiles fetched by [prefetch], keyed by (z, x, y). Missing tiles are simply absent. [complete]: every height in the server's
     * zooms came from the server's terrain (none from AWS because the server was slow or unreachable).
     */
    class Fetched(
        val heights: Map<Triple<Int, Int, Int>, ShortArray>,
        val imagery: Map<Triple<Int, Int, Int>, ShortArray>,
        val complete: Boolean = true,
    )

    private val root = File(cacheDir, "netz-tiles")
    private val trimmed = AtomicBoolean(false)
    private val pruned = AtomicBoolean(false)
    private val demServer = DEM_SERVER + "/" + (terrainKey?.let { "v-$it" } ?: LATEST)

    /** Consecutive network (not HTTP-status) failures; after a few we stop waiting on that host. */
    private val serverFails = AtomicInteger()
    private val imageryFails = AtomicInteger()

    /** Set once a height tile the server should have had came from AWS instead. */
    private val substituted = AtomicBoolean(false)

    /** 256x256 heights, row-major, rows north to south, packed (see [heightAt]). */
    fun heights(z: Int, x: Int, y: Int): ShortArray? = loadDem(z, x, y)

    /** 256x256 pixels of the Sentinel-2 cloudless mosaic (z <= 13), packed RGB565 (see [colourAt]). */
    fun imagery(z: Int, x: Int, y: Int): ShortArray? = if (z > MAX_IMAGERY_ZOOM) null else loadImagery(z, x, y)

    /**
     * Downloads (or reads from disk) all the given tiles on a pool of [POOL] threads.
     * Returns null if [isCancelled] became true.
     */
    fun prefetch(
        demKeys: Collection<Triple<Int, Int, Int>>,
        imageryKeys: Collection<Triple<Int, Int, Int>>,
        isCancelled: () -> Boolean = { false },
    ): Fetched? {
        val pool = Executors.newFixedThreadPool(POOL) { r -> Thread(r, "netz-tiles").apply { isDaemon = true } }
        try {
            val dem = demKeys.map { k ->
                k to pool.submit(Callable { if (isCancelled()) null else heights(k.first, k.second, k.third) })
            }
            val img = imageryKeys.map { k ->
                k to pool.submit(Callable { if (isCancelled()) null else imagery(k.first, k.second, k.third) })
            }
            val h = HashMap<Triple<Int, Int, Int>, ShortArray>()
            val p = HashMap<Triple<Int, Int, Int>, ShortArray>()
            for ((k, fut) in dem) {
                if (isCancelled()) return null
                await(fut)?.let { h[k] = it }
            }
            for ((k, fut) in img) {
                if (isCancelled()) return null
                await(fut)?.let { p[k] = it }
            }
            return if (isCancelled()) null else Fetched(h, p, !substituted.get())
        } finally {
            pool.shutdownNow()
        }
    }

    private fun <T> await(f: Future<T?>): T? = try { f.get() } catch (e: Exception) { null }

    /**
     * The ground height (m above sea level, the sea at 0) at a point, from the refraction server's terrain tiles (the heights its
     * horizons are computed from), else the AWS Terrain Tiles: one zoom-14 tile (~10 m pixels at 30 deg), kept on disk. Null when
     * neither answers. Blocking (port of the website's ground-height.js).
     */
    fun groundHeight(lat: Double, lon: Double): Double? {
        val z = GROUND_ZOOM
        val n = 1 shl z
        val l = Math.toRadians(lat.coerceIn(-85.05, 85.05))
        val px = (lon + 180) / 360 * n * TILE
        val py = (1 - kotlin.math.asinh(kotlin.math.tan(l)) / Math.PI) / 2 * n * TILE
        val tx = Math.floorMod(kotlin.math.floor(px / TILE).toInt(), n)
        val ty = kotlin.math.floor(py / TILE).toInt().coerceIn(0, n - 1)
        val i = kotlin.math.floor(py - ty * TILE).toInt().coerceIn(0, TILE - 1) * TILE +
            kotlin.math.floor(px - kotlin.math.floor(px / TILE) * TILE).toInt().coerceIn(0, TILE - 1)
        val tile = heights(z, tx, ty) ?: return null
        return heightAt(tile, i).toDouble()
    }

    // ---- DEM ----

    /**
     * The server's tile of this terrain version (cached, else downloaded); a tile the server will never have (a 404 without
     * "pending", remembered on disk for [NONE_KEEP_MS]) or any zoom it doesn't serve comes from AWS. A "pending" 404 (its data
     * is not on the server yet) is used from AWS this time and asked for again next time. When the server is slow or unreachable, AWS stands in for this render
     * only: its copy isn't preferred next time, and [Fetched.complete] says the picture should be redrawn.
     */
    private fun loadDem(z: Int, x: Int, y: Int): ShortArray? {
        pruneOldVersions()
        val serves = z in SERVER_ZOOMS
        if (serves) {
            readDem(file(demServer, z, x, y, "png"), false)?.let { return it }
            val none = file(demServer, z, x, y, "none")
            val never = none.isFile && System.currentTimeMillis() - none.lastModified() < NONE_KEEP_MS
            if (!never && serverFails.get() < MAX_FAILS) {
                val v = terrainKey?.let { "?v=$it" } ?: ""
                val r = fetch("$server/v1/terrain-tiles/$z/$x/$y.png$v", 5000, SERVER_READ_TIMEOUT_MS, serverFails)
                val h = r.bytes?.let { decodeDem(it, false) }
                if (h != null) {
                    write(file(demServer, z, x, y, "png"), r.bytes)
                    return h
                }
                if (r.status == 404 && !r.pending) write(none, ByteArray(0))
                else substituted.set(true)
            } else if (!never) substituted.set(true)
        }
        readDem(file(DEM_AWS, z, x, y, "png"), true)?.let { return it }
        val bytes = fetch("https://s3.amazonaws.com/elevation-tiles-prod/terrarium/$z/$x/$y.png", 5000, 5000, null).bytes
        val h = bytes?.let { decodeDem(it, true) } ?: return null
        write(file(DEM_AWS, z, x, y, "png"), bytes)
        return h
    }

    private fun readDem(f: File, aws: Boolean): ShortArray? {
        if (!f.isFile) return null
        val h = try { decodeDem(f.readBytes(), aws) } catch (e: Exception) { null }
        if (h == null) f.delete()
        return h
    }

    /** Once per instance, when the version is known: drop the server tiles of other terrain versions (and the unversioned ones). */
    private fun pruneOldVersions() {
        if (terrainKey == null || !pruned.compareAndSet(false, true)) return
        try {
            val keep = File(root, demServer).name
            File(root, DEM_SERVER).listFiles()?.forEach { if (it.name != keep) it.deleteRecursively() }
        } catch (e: Exception) {
            // best-effort
        }
    }

    /** Terrarium: h = R*256 + G + B/256 - 32768. [clampSea] flattens AWS's bathymetry to sea level. */
    private fun decodeDem(bytes: ByteArray, clampSea: Boolean): ShortArray? {
        val px = decodePixels(bytes, premultiplied = false) ?: return null
        val out = ShortArray(TILE * TILE)
        for (i in out.indices) {
            val c = px[i]
            val h = ((c shr 16) and 0xFF) * 256f + ((c shr 8) and 0xFF) + (c and 0xFF) / 256f - 32768f
            out[i] = packHeight(if (clampSea && h < 0f) 0f else h)
        }
        return out
    }

    // ---- imagery ----

    private fun loadImagery(z: Int, x: Int, y: Int): ShortArray? {
        val f = file("img", z, x, y, "jpg")
        if (f.isFile) {
            val p = try { decodePixels(f.readBytes(), premultiplied = true)?.let(::packColours) } catch (e: Exception) { null }
            if (p != null) return p
            f.delete()
        }
        if (imageryFails.get() >= MAX_FAILS) return null
        val bytes = fetch(
            "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless_3857/default/g/$z/$y/$x.jpg", 8000, 8000, imageryFails
        ).bytes ?: return null
        val p = decodePixels(bytes, premultiplied = true)?.let(::packColours) ?: return null
        write(f, bytes)
        return p
    }

    // ---- shared plumbing ----

    private fun decodePixels(bytes: ByteArray, premultiplied: Boolean): IntArray? {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = premultiplied
        }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        try {
            if (bmp.width != TILE || bmp.height != TILE) return null
            val px = IntArray(TILE * TILE)
            bmp.getPixels(px, 0, TILE, 0, 0, TILE, TILE)
            return px
        } finally {
            bmp.recycle()
        }
    }

    /** A download's body (on a 200), HTTP status (0 when none came back), and an error answer's "pending" (will be there later). */
    private class Got(val bytes: ByteArray?, val status: Int, val pending: Boolean = false)

    /**
     * [fails] counts consecutive failures to reach the host (not HTTP statuses, nor a reached host that was slow to answer, as
     * the server is while it first builds a tile), so a dead host is skipped quickly. The connection is left to the pool
     * (no disconnect) so the next tile from the same host skips the TLS handshake.
     */
    private fun fetch(url: String, connectMs: Int, readMs: Int, fails: AtomicInteger?): Got {
        var conn: HttpURLConnection? = null
        var reached = false
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = connectMs
                readTimeout = readMs
                setRequestProperty("User-Agent", "ZemanehYosef-Android")
            }
            conn.connect()
            reached = true
            val status = conn.responseCode
            if (status != 200) {
                val err = try { conn.errorStream?.use { it.readBytes() } } catch (e: IOException) { conn.disconnect(); null } // drained: reusable
                val pending = err != null && try { org.json.JSONObject(String(err)).optBoolean("pending", false) } catch (e: Exception) { false }
                fails?.set(0)
                return Got(null, status, pending)
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            fails?.set(0)
            Got(bytes, status)
        } catch (e: IOException) {
            if (!reached) fails?.incrementAndGet()
            conn?.disconnect()
            Got(null, 0)
        } catch (e: Exception) {
            conn?.disconnect()
            Got(null, 0)
        }
    }

    private fun file(kind: String, z: Int, x: Int, y: Int, ext: String) = File(root, "$kind/$z/$x/$y.$ext")

    /** Atomic (tmp + rename) so a half-written tile is never read back. */
    private fun write(dest: File, bytes: ByteArray) {
        try {
            dest.parentFile?.mkdirs()
            val tmp = File(dest.parentFile, "${dest.name}.${Thread.currentThread().id}.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(dest)) tmp.delete()
            if (trimmed.compareAndSet(false, true)) trim()
        } catch (e: Exception) {
            // cache is best-effort
        }
    }

    /** Once per instance: drop the oldest tiles beyond [CACHE_CAP_BYTES]. */
    private fun trim() {
        try {
            val files = root.walkTopDown().filter { it.isFile }.toMutableList()
            var total = files.sumOf { it.length() }
            if (total <= CACHE_CAP_BYTES) return
            files.sortBy { it.lastModified() }
            for (f in files) {
                if (total <= CACHE_CAP_BYTES * 9 / 10) break
                val len = f.length()
                if (f.delete()) total -= len
            }
        } catch (e: Exception) {
            // best-effort
        }
    }


    private fun packColours(argb: IntArray) = ShortArray(argb.size) { i ->
        val c = argb[i]
        val v = ((c shr 8) and 0xF800) or ((c shr 5) and 0x07E0) or ((c shr 3) and 0x001F)
        (if (v == 0) 1 else v).toShort() // 0 stays free to mean "no imagery"
    }

    companion object {
        const val TILE = 256

        /** Heights in quarter metres above -500 m, unsigned: -500 m to 15,883 m. */
        @JvmStatic
        fun packHeight(m: Float): Short = Math.round((m + 500f) * 4f).coerceIn(0, 0xFFFF).toShort()

        @JvmStatic
        fun heightAt(tile: ShortArray, i: Int): Float = (tile[i].toInt() and 0xFFFF) * 0.25f - 500f

        /** Opaque ARGB of a packed RGB565 pixel, or 0 where there is none. */
        @JvmStatic
        fun colourAt(tile: ShortArray, i: Int): Int {
            val v = tile[i].toInt() and 0xFFFF
            if (v == 0) return 0
            val r = (v shr 11) and 0x1F
            val g = (v shr 5) and 0x3F
            val b = v and 0x1F
            return (0xFF shl 24) or (((r shl 3) or (r shr 2)) shl 16) or (((g shl 2) or (g shr 4)) shl 8) or ((b shl 3) or (b shr 2))
        }
        const val MAX_IMAGERY_ZOOM = 13
        /** Zoom of the tile [groundHeight] reads. */
        private const val GROUND_ZOOM = 14
        private const val POOL = 6
        private const val MAX_FAILS = 3
        private const val CACHE_CAP_BYTES = 150L * 1024 * 1024
        private const val DEM_SERVER = "dem-server"
        private const val DEM_AWS = "dem-aws"
        private const val LATEST = "latest"
        /** How long a tile the server said it will never have is taken from AWS without asking it again. */
        private const val NONE_KEEP_MS = 7L * 24 * 3600 * 1000
        /** The zooms the server's /v1/terrain-tiles serves. */
        private val SERVER_ZOOMS = 8..14
        /** A tile the server hasn't built yet can take it well over the connect timeout to answer. */
        private const val SERVER_READ_TIMEOUT_MS = 20_000
    }
}
