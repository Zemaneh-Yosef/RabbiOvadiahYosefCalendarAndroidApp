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
class TerrainTiles(cacheDir: File, private val server: String = RefractionRepository.REFRACTION_SERVER) {

    /** Tiles fetched by [prefetch], keyed by (z, x, y). Missing tiles are simply absent. */
    class Fetched(
        val heights: Map<Triple<Int, Int, Int>, ShortArray>,
        val imagery: Map<Triple<Int, Int, Int>, ShortArray>,
    )

    private val root = File(cacheDir, "netz-tiles")
    private val trimmed = AtomicBoolean(false)

    /** Consecutive network (not HTTP-status) failures; after a few we stop waiting on that host. */
    private val serverFails = AtomicInteger()
    private val imageryFails = AtomicInteger()

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
            return if (isCancelled()) null else Fetched(h, p)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun <T> await(f: Future<T?>): T? = try { f.get() } catch (e: Exception) { null }

    // ---- DEM ----

    private fun loadDem(z: Int, x: Int, y: Int): ShortArray? {
        for (dir in arrayOf(DEM_SERVER, DEM_AWS)) {
            val f = file(dir, z, x, y, "png")
            if (!f.isFile) continue
            val h = try { decodeDem(f.readBytes(), dir == DEM_AWS) } catch (e: Exception) { null }
            if (h != null) return h
            f.delete()
        }
        if (z in 8..14 && serverFails.get() < MAX_FAILS) {
            val bytes = download("$server/v1/terrain-tiles/$z/$x/$y.png", 5000, serverFails)
            val h = bytes?.let { decodeDem(it, false) }
            if (h != null) {
                write(file(DEM_SERVER, z, x, y, "png"), bytes)
                return h
            }
        }
        val bytes = download("https://s3.amazonaws.com/elevation-tiles-prod/terrarium/$z/$x/$y.png", 5000, null)
        val h = bytes?.let { decodeDem(it, true) } ?: return null
        write(file(DEM_AWS, z, x, y, "png"), bytes)
        return h
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
        val bytes = download(
            "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless_3857/default/g/$z/$y/$x.jpg", 8000, imageryFails
        ) ?: return null
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

    /** [fails] counts consecutive network errors (not HTTP statuses) so a dead host is skipped quickly. */
    private fun download(url: String, timeoutMs: Int, fails: AtomicInteger?): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", "ZemanehYosef-Android")
            }
            if (conn.responseCode != 200) return null
            val bytes = conn.inputStream.use { it.readBytes() }
            fails?.set(0)
            bytes
        } catch (e: IOException) {
            fails?.incrementAndGet()
            null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
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
        private const val POOL = 6
        private const val MAX_FAILS = 3
        private const val CACHE_CAP_BYTES = 150L * 1024 * 1024
        private const val DEM_SERVER = "dem-server"
        private const val DEM_AWS = "dem-aws"
    }
}
