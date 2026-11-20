package com.ej.rovadiahyosefcalendar.classes.netz

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** What to look at. eyeM: eye height above sea level, or null = ground height under (lat, lon) + 1.7 m. */
data class PanoramaView(
    val lat: Double, val lon: Double, val eyeM: Double?,
    val headingDeg: Double,     // azimuth at the image's horizontal centre (deg east of north)
    val centreAltDeg: Double,   // apparent altitude placed at y = horizonFraction * height
    val horizonFraction: Float, // e.g. 0.70f
    val hFovDeg: Double,
    val width: Int, val height: Int,
)

/**
 * A rendered terrain skyline plus the pinhole projection it was drawn with, so the caller can place
 * the sun (apparent altitude) in exactly the same frame.
 */
class Panorama(
    val view: PanoramaView,
    val bitmap: Bitmap,          // width x height, ARGB_8888, sky pixels fully transparent
    val skylineY: FloatArray,    // per column: y (px) of the top of the terrain; `height` where no terrain was hit
    val eyeM: Double,            // the eye height actually used
) {
    private val f = (view.width / 2.0) / tan(Math.toRadians(view.hFovDeg) / 2)

    /** Apparent az/alt (degrees) to pixel; null if behind the camera. */
    fun project(azDeg: Double, altDeg: Double): PointF? {
        var daz = (azDeg - view.headingDeg) % 360.0
        if (daz > 180) daz -= 360 else if (daz <= -180) daz += 360
        if (abs(daz) >= 89.0) return null
        val dr = Math.toRadians(daz)
        val x = view.width / 2.0 + f * tan(dr)
        val cy = view.horizonFraction * view.height + f * tan(Math.toRadians(view.centreAltDeg))
        val y = cy - f * tan(Math.toRadians(altDeg)) / cos(dr)
        if (!x.isFinite() || !y.isFinite()) return null
        return PointF(x.toFloat(), y.toFloat())
    }

    /** Azimuth (deg east of north) seen at pixel column [x]. */
    fun azimuthAt(x: Float): Double = view.headingDeg + Math.toDegrees(atan((x - view.width / 2.0) / f))

    /** Interpolated terrain top at [x], clamped to the image. */
    fun skylineAt(x: Float): Float {
        val t = (x - 0.5f).coerceIn(0f, (skylineY.size - 1).toFloat())
        val i = t.toInt()
        val j = min(i + 1, skylineY.size - 1)
        val a = t - i
        return skylineY[i] * (1 - a) + skylineY[j] * a
    }
}

/**
 * Voxel-space style renderer: per image column, march a ray over the heightmap from near to far and
 * paint each newly visible span with the satellite colour. Angles are *apparent* (standard refraction
 * k = 0.13 baked in) to match how the sun is drawn.
 */
object PanoramaRenderer {
    private const val R = 6371008.8
    private const val K = 0.13
    private const val MIN_D = 5.0
    private const val MAX_D = 150_000.0
    private const val EQ = 40075016.686
    private const val HAZE_SCALE_M = 45_000.0
    private const val HAZE_MAX = 0.75
    private const val EARTH_R = 0xa8
    private const val EARTH_G = 0x9a
    private const val EARTH_B = 0x80
    private const val HAZE_R = 0xc4
    private const val HAZE_G = 0xcc
    private const val HAZE_B = 0xd6
    private const val LN2 = 0.6931471805599453
    private const val TWO_PI = 2 * Math.PI

    /** Null when no terrain could be had (offline, nothing cached) or when cancelled. */
    fun render(tiles: TerrainTiles, view: PanoramaView, isCancelled: () -> Boolean = { false }): Panorama? {
        val w = view.width
        val h = view.height
        if (w <= 0 || h <= 0) return null

        val lat0 = Math.toRadians(view.lat)
        val lon0 = Math.toRadians(view.lon)
        val sinLat = sin(lat0)
        val cosLat = cos(lat0)
        val f = (w / 2.0) / tan(Math.toRadians(view.hFovDeg) / 2)
        val hFovRad = Math.toRadians(view.hFovDeg)
        val headingR = Math.toRadians(view.headingDeg)
        val cy = view.horizonFraction * h + f * tan(Math.toRadians(view.centreAltDeg))

        // Sample distances are the same for every column, so precompute everything that depends only on d.
        var n = 0
        run { var d = MIN_D; while (d <= MAX_D) { n++; d = d * 1.006 + 2.0 } }
        val dist = DoubleArray(n)
        val sinD = DoubleArray(n)
        val cosD = DoubleArray(n)
        val curv = DoubleArray(n)
        val hazeI = IntArray(n)
        val zH = IntArray(n)
        val zI = IntArray(n)
        run {
            var d = MIN_D
            for (i in 0 until n) {
                dist[i] = d
                val dl = d / R
                sinD[i] = sin(dl)
                cosD[i] = cos(dl)
                curv[i] = d * d * (1 - K) / (2 * R)
                hazeI[i] = (min(HAZE_MAX, 1 - exp(-d / HAZE_SCALE_M)) * 256).toInt()
                val footprint = d * hFovRad / w
                val z = floor(ln(EQ * cosLat / (256 * footprint)) / LN2 + 0.5).toInt()
                zH[i] = z.coerceIn(8, 14)
                zI[i] = z.coerceIn(8, TerrainTiles.MAX_IMAGERY_ZOOM)
                d = d * 1.006 + 2.0
            }
        }

        val ray = Ray(sinLat, cosLat, lon0)

        // Pass 1: geometry only, to learn which tiles the rays touch.
        val demSet = HashSet<Long>()
        val imgSet = HashSet<Long>()
        if (view.eyeM == null) {
            ray.atObserver()
            val m = 1 shl 14
            demSet.add(tk(14, (ray.xf * m).toInt(), (ray.yf * m).toInt()))
        }
        for (x in 0 until w) {
            if (isCancelled()) return null
            val u = (x + 0.5 - w / 2.0) / f
            val az = headingR + atan(u)
            val sa = sin(az)
            val ca = cos(az)
            var lastH = Long.MIN_VALUE
            var lastI = Long.MIN_VALUE
            for (i in 0 until n) {
                ray.step(sa, ca, sinD[i], cosD[i])
                val mh = 1 shl zH[i]
                val pxh = ray.xf * mh * 256.0
                val pyh = ray.yf * mh * 256.0
                val txLo = (pxh - 1).toInt() shr 8
                val txHi = (pxh + 1).toInt() shr 8
                val tyLo = (pyh - 1).toInt() shr 8
                val tyHi = (pyh + 1).toInt() shr 8
                val edge = txLo != txHi || tyLo != tyHi
                val sigH = tk(zH[i], txLo, tyLo)
                if (edge || sigH != lastH) {
                    lastH = sigH
                    demSet.add(sigH)
                    if (edge) {
                        demSet.add(tk(zH[i], txHi, tyLo)); demSet.add(tk(zH[i], txLo, tyHi)); demSet.add(tk(zH[i], txHi, tyHi))
                    }
                }
                val mi = 1 shl zI[i]
                val sigI = tk(zI[i], (ray.xf * mi).toInt(), (ray.yf * mi).toInt())
                if (sigI != lastI) {
                    lastI = sigI
                    imgSet.add(sigI)
                }
            }
        }

        val fetched = tiles.prefetch(demSet.map(::untk), imgSet.map(::untk), isCancelled) ?: return null
        if (fetched.heights.isEmpty()) return null
        val field = Field(
            fetched.heights.mapKeysTo(HashMap()) { tk(it.key.first, it.key.second, it.key.third) },
            fetched.imagery.mapKeysTo(HashMap()) { tk(it.key.first, it.key.second, it.key.third) },
        )

        val eye = view.eyeM ?: run {
            ray.atObserver()
            field.height(14, ray.xf, ray.yf) + 1.7
        }

        // Pass 2: march front to back with a y-buffer.
        val pix = IntArray(w * h)
        val skyline = FloatArray(w)
        for (x in 0 until w) {
            if (isCancelled()) return null
            val u = (x + 0.5 - w / 2.0) / f
            val az = headingR + atan(u)
            val sa = sin(az)
            val ca = cos(az)
            val fs = f * sqrt(1 + u * u) // f / cos(daz)
            var yTopF = h.toFloat()
            var yTopI = h
            var below = 0 // colour of the last sample drawn (the bottom of the next span); 0 = none yet
            for (i in 0 until n) {
                ray.step(sa, ca, sinD[i], cosD[i])
                val hgt = field.height(zH[i], ray.xf, ray.yf)
                val y = cy - fs * ((hgt - eye - curv[i]) / dist[i])
                if (y < yTopF) {
                    val top = if (y <= 0.0) 0 else ceil(y).toInt()
                    if (top < yTopI) {
                        val c = colour(field, ray.xf, ray.yf, zH[i], zI[i], hazeI[i], cosLat)
                        // blend from this sample at the top to the one below, so near ground isn't flat bands
                        val span = yTopI - top
                        if (below == 0 || span < 3) for (r in top until yTopI) pix[r * w + x] = c
                        else for (r in top until yTopI) pix[r * w + x] = lerp(c, below, ((r - top) * 256) / span)
                        below = c
                        yTopI = top
                    }
                    yTopF = y.toFloat()
                    if (yTopI <= 0) break
                }
            }
            skyline[x] = max(0f, yTopF)
        }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pix, 0, w, 0, 0, w, h)
        return Panorama(view, bmp, skyline, eye)
    }

    /** Satellite pixel (or a hillshaded earth tone when the imagery tile is missing) with haze mixed in. */
    private fun colour(field: Field, xf: Double, yf: Double, zh: Int, zi: Int, hazeI: Int, cosLat: Double): Int {
        var c = field.image(zi, xf, yf)
        var r: Int
        var g: Int
        var b: Int
        // relief from the heights: light from the NW at 45 degrees, (east, north, up) = (-0.5, 0.5, 0.7071)
        val m = 256 shl zh
        val ix = floor(xf * m).toInt()
        val iy = floor(yf * m).toInt()
        val mpp = EQ * cosLat / m
        val dzdx = (field.pixel(zh, ix + 1, iy) - field.pixel(zh, ix - 1, iy)) / (2 * mpp)
        val dzdnorth = (field.pixel(zh, ix, iy - 1) - field.pixel(zh, ix, iy + 1)) / (2 * mpp)
        val dot = ((dzdx * 0.5 - dzdnorth * 0.5 + 0.7071) / sqrt(dzdx * dzdx + dzdnorth * dzdnorth + 1.0)).coerceIn(0.0, 1.0)
        if (c != 0) {
            // the imagery has its own shadows; a mild extra shade keeps slopes readable once the night darkens it
            val k = 0.8 + 0.3 * dot
            r = min(255, (((c shr 16) and 0xFF) * k).toInt()); g = min(255, (((c shr 8) and 0xFF) * k).toInt()); b = min(255, ((c and 0xFF) * k).toInt())
        } else {
            val k = 0.35 + 0.75 * dot
            r = min(255, (EARTH_R * k).toInt()); g = min(255, (EARTH_G * k).toInt()); b = min(255, (EARTH_B * k).toInt())
        }
        r += ((HAZE_R - r) * hazeI) shr 8
        g += ((HAZE_G - g) * hazeI) shr 8
        b += ((HAZE_B - b) * hazeI) shr 8
        c = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        return c
    }

    /** a + (b - a) * f / 256 per channel, opaque. */
    private fun lerp(a: Int, b: Int, f: Int): Int {
        fun ch(s: Int): Int {
            val x = (a shr s) and 0xFF
            return (x + ((((b shr s) and 0xFF) - x) * f shr 8)) shl s
        }
        return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
    }

    private fun tk(z: Int, x: Int, y: Int): Long {
        val n = 1 shl z
        return (z.toLong() shl 48) or ((x and (n - 1)).toLong() shl 24) or y.coerceIn(0, n - 1).toLong()
    }

    private fun untk(k: Long) = Triple((k ushr 48).toInt(), ((k ushr 24) and 0xFFFFFF).toInt(), (k and 0xFFFFFF).toInt())

    /** Walks a great circle from the observer, yielding normalized Mercator coordinates (0..1) of each sample. */
    private class Ray(val sinLat: Double, val cosLat: Double, val lon0: Double) {
        var xf = 0.0
        var yf = 0.0

        fun step(sinAz: Double, cosAz: Double, sinD: Double, cosD: Double) {
            val s = sinLat * cosD + cosLat * sinD * cosAz
            set(atan2(sinAz * sinD * cosLat, cosD - sinLat * s), s)
        }

        fun atObserver() = set(0.0, sinLat)

        /** [dLon] east of the observer; [s] = sine of the point's latitude. */
        private fun set(dLon: Double, s: Double) {
            val x = (lon0 + dLon) / TWO_PI + 0.5
            xf = x - floor(x)
            val c = s.coerceIn(-0.9999, 0.9999)
            yf = (0.5 - ln((1 + c) / (1 - c)) / (4 * Math.PI)).coerceIn(0.0, 1.0)
        }
    }

    /** Sampler over the tiles gathered for this render; a missing DEM tile reads as sea level. */
    private class Field(val dem: Map<Long, ShortArray>, val img: Map<Long, ShortArray>) {
        private var lastKey = -1L
        private var lastTile: ShortArray? = null

        /** Height at global pixel (ix, iy) of zoom z. */
        fun pixel(z: Int, ix: Int, iy: Int): Float {
            val m = 256 shl z
            val x = ix and (m - 1)
            val y = iy.coerceIn(0, m - 1)
            val key = (z.toLong() shl 48) or ((x shr 8).toLong() shl 24) or (y shr 8).toLong()
            if (key != lastKey) {
                lastKey = key
                lastTile = dem[key]
            }
            val t = lastTile ?: return 0f
            return TerrainTiles.heightAt(t, (y and 255) * 256 + (x and 255))
        }

        fun height(z: Int, xf: Double, yf: Double): Float {
            val m = 256 shl z
            val px = xf * m - 0.5
            val py = yf * m - 0.5
            val ix = floor(px).toInt()
            val iy = floor(py).toInt()
            val fx = (px - ix).toFloat()
            val fy = (py - iy).toFloat()
            val a = pixel(z, ix, iy)
            val b = pixel(z, ix + 1, iy)
            val c = pixel(z, ix, iy + 1)
            val d = pixel(z, ix + 1, iy + 1)
            return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy
        }

        private fun imgPixel(z: Int, m: Int, ix0: Int, iy0: Int): Int {
            val ix = ix0 and (m - 1)
            val iy = iy0.coerceIn(0, m - 1)
            val t = img[(z.toLong() shl 48) or ((ix shr 8).toLong() shl 24) or (iy shr 8).toLong()] ?: return 0
            return TerrainTiles.colourAt(t, (iy and 255) * 256 + (ix and 255))
        }

        /** Satellite colour, bilinear (so close ground isn't blocky), or 0 when that imagery tile is missing. */
        fun image(z: Int, xf: Double, yf: Double): Int {
            val m = 256 shl z
            val px = xf * m - 0.5
            val py = yf * m - 0.5
            val ix = floor(px).toInt()
            val iy = floor(py).toInt()
            val a = imgPixel(z, m, ix, iy)
            if (a == 0) return 0
            // a neighbour across a missing tile's edge: fall back to the pixel itself
            val b = imgPixel(z, m, ix + 1, iy).takeIf { it != 0 } ?: a
            val c = imgPixel(z, m, ix, iy + 1).takeIf { it != 0 } ?: a
            val d = imgPixel(z, m, ix + 1, iy + 1).takeIf { it != 0 } ?: a
            val fx = ((px - ix) * 256).toInt()
            val fy = ((py - iy) * 256).toInt()
            fun ch(s: Int): Int {
                val top = ((a shr s) and 0xFF) * (256 - fx) + ((b shr s) and 0xFF) * fx
                val bot = ((c shr s) and 0xFF) * (256 - fx) + ((d shr s) and 0xFF) * fx
                return ((top * (256 - fy) + bot * fy) shr 16) shl s
            }
            return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
        }
    }
}
