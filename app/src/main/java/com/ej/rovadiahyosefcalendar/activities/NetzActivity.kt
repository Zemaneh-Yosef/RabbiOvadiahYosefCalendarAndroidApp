package com.ej.rovadiahyosefcalendar.activities

import android.util.Log
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ej.rovadiahyosefcalendar.R
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar
import com.ej.rovadiahyosefcalendar.classes.ZmanimNames
import com.ej.rovadiahyosefcalendar.classes.netz.Panorama
import com.ej.rovadiahyosefcalendar.classes.netz.PanoramaRenderer
import com.ej.rovadiahyosefcalendar.classes.netz.PanoramaView
import com.ej.rovadiahyosefcalendar.classes.netz.SkyColors
import com.ej.rovadiahyosefcalendar.classes.netz.TerrainTiles
import com.ej.rovadiahyosefcalendar.classes.royzmanim.HorizonSet
import com.ej.rovadiahyosefcalendar.classes.royzmanim.HorizonSetPoint
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionRepository
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionStatus
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionSupport
import com.ej.rovadiahyosefcalendar.classes.royzmanim.SolarEvent
import com.ej.rovadiahyosefcalendar.classes.royzmanim.StarNightfall
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * The sunrise countdown: the place's own horizon toward the sunrise (terrain from the refraction server or AWS, draped with
 * Sentinel-2 imagery), under a sky as bright as the Sun's depression makes it, with the time left in three big columns.
 * Tapping the ground picks Any Peak or Full Reveal. After sunrise it counts up (dimmed) until chatzot, then counts down to
 * the next day's sunrise.
 */
class NetzActivity : AppCompatActivity() {

    /** Which sunrise is counted down to. */
    enum class SunriseKind(val pref: String) { ANY_PEAK("anyPeak"), FULL_REVEAL("fullReveal") }

    private lateinit var prefs: SharedPreferences
    private var calendar by mutableStateOf<ROZmanimCalendar?>(null)

    /**
     * New weather, terrain or haze for this place (a refresh, or the visible sunrise finishing in the background): rebuild the
     * calendar with it, so the countdown is never left counting to an older or fallback sunrise.
     */
    private val refractionListener: (Double, Double) -> Unit = { _, _ ->
        runOnUiThread { if (!isFinishing && !isDestroyed) calendar = newCalendar() }
    }

    /** Whether the weather, terrain or visible sunrise is still loading: the counted sunrise may still move (a banner says so). */
    private var refractionUpdating by mutableStateOf(false)
    /** The update is downloading (else it is calculating the visible sunrise): what the banner says. */
    private var refractionDownloading by mutableStateOf(false)
    private val statusHandler = Handler(Looper.getMainLooper())
    private val showUpdating = Runnable { refractionUpdating = true }
    /** The user tapped the banner closed: it stays closed while this screen is open. */
    private var refractionDismissed = false
    private val statusListener = Runnable { runOnUiThread { updateRefractionStatus() } }

    /** The failure message is showing instead (the download or the computation failed, or it took too long). */
    private var refractionFailed by mutableStateOf(false)
    private val hideFailed = Runnable { refractionFailed = false }
    /** The failed download or computation (its time) already told about. */
    private var failureShown: Long? = null
    /** The update took too long: the banner said so and stays down until the update ends. */
    private var gaveUp = false
    private var watchdogPosted = false
    private val watchdog = Runnable {
        watchdogPosted = false
        gaveUp = true
        updateRefractionStatus()
    }

    /**
     * Shows the banner after a short delay (loads from the saved data never flash it) while today's or tomorrow's counted sunrise is
     * still going to change, hides it when it won't, and says so when the update failed or took too long.
     */
    private fun updateRefractionStatus() {
        val geo = calendar?.geoLocation ?: return
        val today = LocalDate.now(geo.zoneId)
        val fullDisk = prefs.getString(PREF_KIND, null) == SunriseKind.FULL_REVEAL.pref// the kind counted down to
        val updating = ROZmanimCalendar.isUpdating(geo, today, fullDisk) || ROZmanimCalendar.isUpdating(geo, today.plusDays(1), fullDisk)
        statusHandler.removeCallbacks(showUpdating)
        when {
            refractionDismissed -> refractionUpdating = false
            updating && !gaveUp -> {
                refractionDownloading = RefractionStatus.isDownloading(geo.latitude, geo.longitude) && RefractionSupport.weather// the terrain only: the visible sunrise
                statusHandler.removeCallbacks(hideFailed)
                refractionFailed = false
                if (!watchdogPosted) {// so the banner always goes away, whatever gets stuck
                    watchdogPosted = true
                    statusHandler.postDelayed(watchdog, MAX_UPDATING_MS)
                }
                if (!refractionUpdating) statusHandler.postDelayed(showUpdating, 500)
            }
            updating -> if (refractionUpdating) {// it has taken too long: say so once, then the banner goes away
                refractionUpdating = false
                showFailed()
            }
            else -> {// done
                statusHandler.removeCallbacks(watchdog)
                watchdogPosted = false
                val said = gaveUp
                gaveUp = false
                val failure = RefractionStatus.lastFailure(geo.latitude, geo.longitude)
                val newFailure = failure != null && failure != failureShown && System.currentTimeMillis() - failure < 60_000
                if (newFailure) failureShown = failure
                val wasShowing = refractionUpdating
                refractionUpdating = false
                if (newFailure && !said && (wasShowing || !refractionFailed)) showFailed()
            }
        }
    }

    private fun showFailed() {
        refractionFailed = true
        statusHandler.removeCallbacks(hideFailed)
        statusHandler.postDelayed(hideFailed, 4000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        prefs = getSharedPreferences(MainFragmentManagerActivity.SHARED_PREF, MODE_PRIVATE)
        calendar = newCalendar()
        setContent { NetzScreen() }
    }

    override fun onStart() {
        super.onStart()
        RefractionRepository.addListener(refractionListener)
        RefractionStatus.addListener(statusListener)
        RefractionRepository.ensureFresh(MainFragmentManagerActivity.sLatitude, MainFragmentManagerActivity.sLongitude)
        updateRefractionStatus()
    }

    override fun onStop() {
        RefractionRepository.removeListener(refractionListener)
        RefractionStatus.removeListener(statusListener)
        statusHandler.removeCallbacks(showUpdating)
        statusHandler.removeCallbacks(watchdog)
        watchdogPosted = false
        super.onStop()
    }

    private fun newCalendar() = ROZmanimCalendar(
        GeoLocation(
            MainFragmentManagerActivity.sCurrentLocationName,
            MainFragmentManagerActivity.sLatitude,
            MainFragmentManagerActivity.sLongitude,
            0.0, // elevation doesn't matter here
            ZoneId.of(MainFragmentManagerActivity.sCurrentTimeZoneID),
        )
    )

    private fun zmanimNames(): ZmanimNames {
        val hebrew = prefs.getBoolean("isZmanimInHebrew", false)
        val translated = !hebrew && prefs.getBoolean("isZmanimEnglishTranslated", false)
        val americanized = !hebrew && !translated && prefs.getBoolean("isZmanimAmericanized", false)
        return ZmanimNames(hebrew, translated, americanized)
    }

    // ------------------------------------------------------------------------------------------------ the day's moments

    /** One day's sunrise of the chosen kind, the chatzot after it and the hatzot halayla before it. */
    private class Day(val date: LocalDate, val sunrise: Instant?, val fromTerrain: Boolean, val chatzot: Instant?, val hatzotHalayla: Instant?)

    /** What is shown at an instant: counting down to (or up from, after it) a day's sunrise. */
    private class Phase(val day: Day, val countingUp: Boolean)

    private fun dayOf(cal: ROZmanimCalendar, date: LocalDate, kind: SunriseKind): Day {
        cal.localDate = date
        val visibleSunrise = cal.haNetz// null without a terrain horizon: then both kinds are from sea level (mishor)
        val sunrise = if (kind == SunriseKind.FULL_REVEAL) cal.sunriseFullDisk else visibleSunrise ?: cal.seaLevelSunrise
        return Day(date, sunrise, visibleSunrise != null, cal.chatzot, cal.previousSolarMidnight)
    }

    private fun phaseAt(today: Day, tomorrow: Day, now: Instant): Phase {
        val rise = today.sunrise
        if (rise != null && now.isBefore(rise)) return Phase(today, false)
        if (rise != null && today.chatzot != null && now.isBefore(today.chatzot)) return Phase(today, true)
        return Phase(tomorrow, false)
    }

    /** The vantage point of the horizon set that wins the sunrise in this direction (as the website picks it). */
    private fun vantageFor(set: HorizonSet?, azDeg: Double): HorizonSetPoint? {
        if (set == null || set.points.isEmpty()) return null
        for (p in set.points) for (w in p.wins[SolarEvent.SUNRISE] ?: emptyList()) {
            val inside = if (w[0] <= w[1]) azDeg >= w[0] && azDeg <= w[1] else azDeg >= w[0] || azDeg <= w[1]
            if (inside) return p
        }
        return set.points.firstOrNull { it.sunrise != null } ?: set.points[0]
    }

    // ------------------------------------------------------------------------------------------------ the screen

    @OptIn(ExperimentalLayoutApi::class)// navigationBarsIgnoringVisibility
    @Composable
    private fun NetzScreen() {
        val cal = calendar ?: return
        val lat = MainFragmentManagerActivity.sLatitude
        val lon = MainFragmentManagerActivity.sLongitude
        val zone = remember(cal) { ZoneId.of(MainFragmentManagerActivity.sCurrentTimeZoneID) }

        var kind by remember {
            mutableStateOf(if (prefs.getString(PREF_KIND, null) == SunriseKind.FULL_REVEAL.pref) SunriseKind.FULL_REVEAL else SunriseKind.ANY_PEAK)
        }
        var showPicker by remember { mutableStateOf(false) }

        // ticks on the second
        var now by remember { mutableStateOf(Instant.now()) }
        LaunchedEffect(Unit) {
            while (isActive) {
                now = Instant.now()
                delay(1000 - now.toEpochMilli() % 1000)
            }
        }

        val today = now.atZone(zone).toLocalDate()
        // Today's and tomorrow's moments, calculated on a background thread: with new weather data the calculator's caches are
        // empty and each sunrise is traced through the air. Kept with the kind and date they are for: new data for the same ones
        // leave the old times up until the new ones are ready, while another kind or date shows "calculating" until then.
        var computed by remember { mutableStateOf<Pair<Pair<SunriseKind, LocalDate>, Pair<Day, Day>>?>(null) }
        LaunchedEffect(cal, kind, today) {
            val copy = cal.copy// a copy of its own (the date is set on it), sharing the calculator and its caches
            val d = withContext(Dispatchers.Default) { Pair(dayOf(copy, today, kind), dayOf(copy, today.plusDays(1), kind)) }
            computed = Pair(Pair(kind, today), d)
        }
        val days = computed?.takeIf { it.first == Pair(kind, today) }?.second
        val phase = days?.let { phaseAt(it.first, it.second, now) }
        val target = phase?.day?.sunrise

        // The sky follows the Sun's depression, but never from earlier than the hatzot halayla before the counted sunrise:
        // before it, the sky is hatzot halayla's.
        val hatzot = phase?.day?.hatzotHalayla
        val skyTime = if (hatzot != null && now.isBefore(hatzot)) hatzot else now
        val sky = SkyColors.at(-StarNightfall.sunAltAz(skyTime.toEpochMilli().toDouble(), lat, lon)[0])

        // the direction and height of the counted sunrise (the camera aims there)
        val riseAltAz = target?.let { StarNightfall.sunAltAz(it.toEpochMilli().toDouble(), lat, lon) }
        val sunNow = StarNightfall.sunAltAz(now.toEpochMilli().toDouble(), lat, lon)

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = constraints.maxWidth
            val hPx = constraints.maxHeight

            // the panorama, rendered smaller than the screen and stretched (the terrain is soft at this scale anyway)
            val horizonSet = cal.refractionData?.horizon
            val vantage = vantageFor(horizonSet, riseAltAz?.get(1) ?: 90.0)
            val renderW = min(wPx, MAX_RENDER_WIDTH).coerceAtLeast(1)
            val renderH = (hPx.toLong() * renderW / max(1, wPx)).toInt().coerceAtLeast(1)
            val aspect = renderW.toDouble() / renderH
            val view = if (riseAltAz == null) null else PanoramaView(
                lat = vantage?.lat ?: lat,
                lon = vantage?.lon ?: lon,
                eyeM = vantage?.height,
                headingDeg = (riseAltAz[1] * 10).roundToInt() / 10.0,
                centreAltDeg = (SkyColors.apparentAltitude(riseAltAz[0]) * 20).roundToInt() / 20.0,
                horizonFraction = HORIZON_FRACTION,
                hFovDeg = (BASE_FOV_DEG * aspect).coerceIn(MIN_FOV_DEG, MAX_FOV_DEG),
                width = renderW,
                height = renderH,
                horizon = vantage?.sunrise,
                terrainKey = horizonSet?.terrainKey,
            )
            var panorama by remember { mutableStateOf<Panorama?>(null) }
            var image by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(view) {
                if (view == null) { panorama = null; image = null; return@LaunchedEffect }
                if (panorama?.view != view) { panorama = null; image = null }
                // offline with nothing cached: no terrain or Sun, and try again now and then. A picture drawn partly from AWS
                // heights (the server slow or unreachable) is shown, then redrawn a few times so it becomes the server's terrain.
                // a fresh TerrainTiles per attempt: it gives up on a host for its lifetime once it looks unreachable
                suspend fun renderOnce(preview: Boolean): Panorama? = try {
                    withContext(Dispatchers.IO) {
                        PanoramaRenderer.render(TerrainTiles(cacheDir, terrainKey = view.terrainKey), view, preview) { !isActive }
                    }
                } catch (e: CancellationException) {
                    throw e// the screen was left: not a failure
                } catch (e: Exception) {
                    Log.e("NetzActivity", "Rendering the panorama failed", e)
                    null// the countdown is shown without the terrain, and the render is tried again
                } catch (e: OutOfMemoryError) {
                    Log.e("NetzActivity", "Not enough memory for the panorama", e)
                    null
                }
                var redraws = 0
                var first = panorama == null
                while (isActive) {
                    // the first time, when the picture isn't ready within PREVIEW_AFTER_MS (tiles to download), a rough one
                    // (a few coarse tiles, no imagery) is shown meanwhile
                    val p = if (!first) renderOnce(false) else coroutineScope {
                        val full = async { renderOnce(false) }
                        if (withTimeoutOrNull(PREVIEW_AFTER_MS) { full.join() } == null) {
                            val rough = renderOnce(true)
                            if (rough != null && !full.isCompleted) {
                                image = rough.bitmap.asImageBitmap()
                                panorama = rough
                            }
                        }
                        full.await()
                    }
                    first = false
                    if (p != null) {
                        image = p.bitmap.asImageBitmap()
                        panorama = p
                        if (p.complete || ++redraws > MAX_REDRAWS) break
                    }
                    delay(OFFLINE_RETRY_MS)
                }
            }

            val pano = panorama
            val scale = wPx.toFloat() / renderW
            // The countdown's sunrise comes from the server's horizon and the traced air, the picture from the tiles and a
            // standard refraction, so they can disagree by a little: shift the drawn Sun by the difference, so that at the
            // counted moment its top (Any Peak) or bottom (Full Reveal) is exactly on the drawn ridge.
            val sunOffset = remember(pano, riseAltAz, kind, phase?.day?.fromTerrain) {
                val ridge = if (phase?.day?.fromTerrain == true && riseAltAz != null) pano?.skylineAltAt(riseAltAz[1]) else null
                if (ridge == null || riseAltAz == null) 0.0 else {
                    val centre = if (kind == SunriseKind.FULL_REVEAL) ridge + SUN_RADIUS_DEG else ridge - SUN_RADIUS_DEG
                    (centre - SkyColors.apparentAltitude(riseAltAz[0])).coerceIn(-MAX_SUN_OFFSET_DEG, MAX_SUN_OFFSET_DEG)
                }
            }
            val horizonY = pano?.project(pano.view.headingDeg, 0.0)?.y?.times(scale) ?: (hPx * HORIZON_FRACTION)
            val groundTopAt = { x: Float -> pano?.skylineAt(x / scale)?.times(scale) ?: (hPx * HORIZON_FRACTION) }
            val darken = remember(sky.dark) {
                val b = 1f - sky.dark
                ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(b, b, b, 1f) })
            }
            val sunPx = with(density) { 8.dp.toPx() }
            // The system bars are hidden here, so safeDrawing has nothing at the bottom: keep clear of where the navigation bar
            // (or the gesture handle) would be, and of a display cutout
            val bottomEdge = WindowInsets.navigationBarsIgnoringVisibility.union(WindowInsets.displayCutout)

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(pano) {
                        detectTapGestures { o: Offset -> if (o.y >= groundTopAt(o.x)) showPicker = true }
                    }
            ) {
                // sky: the zenith colour down to 55% of the screen above the horizon, blending to the horizon colour there
                drawRect(Color(sky.horizon))
                drawRect(
                    Brush.verticalGradient(
                        0f to Color(sky.zenith), 1f to Color(sky.horizon),
                        startY = horizonY - size.height * 0.55f, endY = horizonY, tileMode = TileMode.Clamp,
                    )
                )
                if (pano != null) {
                    val img = image
                    val f = (pano.view.width / 2.0) / tan(Math.toRadians(pano.view.hFovDeg / 2))
                    // the Sun now, behind the terrain (drawn first, so the ridge hides what is below it), raised or lowered by
                    // sunOffset so it clears the drawn ridge when the countdown ends
                    val app = SkyColors.apparentAltitude(sunNow[0]) + sunOffset
                    if (app > -1.5) pano.project(sunNow[1], app)?.let { c ->
                        val r = max(5.0, f * tan(Math.toRadians(SUN_RADIUS_DEG))).toFloat() * scale
                        val centre = Offset(c.x * scale, c.y * scale)
                        drawCircle(
                            Brush.radialGradient(listOf(Color(0x99FFC861), Color(0x00FFC861)), centre, r * 4),
                            radius = r * 4, center = centre,
                        )
                        drawCircle(Color(0xFFFFE6A3), radius = r, center = centre)
                    }
                    if (img != null) drawImage(
                        img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        colorFilter = darken, filterQuality = FilterQuality.Medium,
                    )
                    // where the counted sunrise comes up over the ridge
                    if (riseAltAz != null) pano.project(riseAltAz[1], SkyColors.apparentAltitude(riseAltAz[0]))?.let { p ->
                        val x = p.x * scale
                        if (x in 0f..size.width) {
                            val y = pano.skylineAt(p.x) * scale
                            drawCircle(Color(0xFFF5B400), radius = sunPx, center = Offset(x, y))
                            drawCircle(Color(0xFF7C2D12), radius = sunPx, center = Offset(x, y), style = Stroke(2.dp.toPx()))
                        }
                    }
                }
            }

            if (phase != null) Countdown(
                phase = phase, now = now, kind = kind,
                heightPx = horizonY.coerceIn(hPx * 0.35f, hPx.toFloat()),
                widthPx = wPx.toFloat(),
            ) else Column(
                // the sunrise is still being calculated
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = with(density) { (horizonY.coerceIn(hPx * 0.35f, hPx.toFloat()) * 0.4f).toDp() }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(Modifier.size(36.dp), color = Color.White, strokeWidth = 3.dp)
                Text(
                    stringResource(R.string.netz_calculating),
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            Text(
                "✕",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 22.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.quit)) { finish() }
                    .padding(16.dp),
            )

            if (refractionUpdating || refractionFailed) {
                // the same slowly pulsing green as the banner on the zemanim screen (still while it shows the failure)
                val pulse by rememberInfiniteTransition(label = "refractionPulse").animateColor(
                    colorResource(R.color.refraction_banner), colorResource(R.color.refraction_banner_pulse),
                    infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "refractionPulseColor",
                )
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(bottomEdge)
                        .padding(bottom = if (pano != null) 56.dp else 24.dp)// above the imagery credit when it shows
                        .background(pulse, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.dismiss)) {
                            refractionDismissed = true
                            statusHandler.removeCallbacks(showUpdating)
                            refractionUpdating = false
                            refractionFailed = false
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (refractionUpdating) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Text(
                            stringResource(if (refractionDownloading) R.string.refraction_updating else R.string.refraction_calculating_sunrise),
                            color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp),
                        )
                    } else {
                        Text(stringResource(R.string.refraction_update_failed), color = Color.White, fontSize = 14.sp)
                    }
                    Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }

            if (pano != null) Text(
                stringResource(R.string.netz_imagery_credit),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(bottomEdge)
                    // centred, away from the screen's rounded corners
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
        }

        if (showPicker) SunrisePicker(
            selected = kind,
            onPick = {
                kind = it
                prefs.edit().putString(PREF_KIND, it.pref).apply()
                showPicker = false
                updateRefractionStatus()// another kind of sunrise may still be computing
            },
            onDismiss = { showPicker = false },
        )
    }

    /** The label, the kind of sunrise, and the time in three columns, in the sky above the horizon. */
    @Composable
    private fun Countdown(phase: Phase, now: Instant, kind: SunriseKind, heightPx: Float, widthPx: Float) {
        val density = LocalDensity.current
        val target = phase.day.sunrise
        val seconds = if (target == null) null else abs(target.epochSecond - now.epochSecond).let {
            // counting down shows the whole seconds left (ceil), so it reaches 00 at the moment itself
            if (!phase.countingUp && target.toEpochMilli() - now.toEpochMilli() > it * 1000) it + 1 else it
        }
        // after sunrise the numbers dim: it has passed
        val textColor = if (phase.countingUp) Color.White.copy(alpha = 0.55f) else Color.White
        val shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 8f)

        val heightDp = with(density) { heightPx.toDp() }
        val widthDp = with(density) { widthPx.toDp() }
        val labelSize = (min(widthDp.value, heightDp.value * 1.6f) * 0.065f).coerceIn(20f, 44f)
        // two tabular digits plus a small unit in a third of the width; and not taller than the space under the label
        val digitDp = min(widthDp.value / 3f / 1.9f, heightDp.value * 0.42f)
        val digitSize = with(density) { digitDp.dp.toSp() }
        val unitSize = digitSize * 0.38f

        Column(
            Modifier
                .fillMaxWidth()
                .height(heightDp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (phase.countingUp) R.string.netz_since_sunrise else R.string.netz_sunrise_countdown),
                color = Color.White, fontSize = labelSize.sp, fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = shadow), textAlign = TextAlign.Center,
            )
            val kindName = stringResource(if (kind == SunriseKind.FULL_REVEAL) R.string.netz_full_reveal else R.string.netz_any_peak)
            Text(
                if (phase.day.fromTerrain) kindName else "$kindName (${zmanimNames().mishorString})",
                color = Color.White.copy(alpha = 0.8f), fontSize = (labelSize * 0.45f).sp,
                style = TextStyle(shadow = shadow), textAlign = TextAlign.Center,
            )
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val parts = if (seconds == null) listOf("--", "--", "--")
                    else listOf(seconds / 3600, seconds % 3600 / 60, seconds % 60).map { "%02d".format(it) }
                    val units = listOf(R.string.netz_unit_hours, R.string.netz_unit_minutes, R.string.netz_unit_seconds)
                    for (i in 0..2) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                        Text(
                            parts[i], color = textColor, fontSize = digitSize, fontWeight = FontWeight.Bold, maxLines = 1,
                            style = TextStyle(fontFeatureSettings = "tnum", shadow = shadow, lineHeight = digitSize),
                        )
                        Text(
                            stringResource(units[i]), color = textColor, fontSize = unitSize, fontWeight = FontWeight.Bold, maxLines = 1,
                            style = TextStyle(shadow = shadow), modifier = Modifier.padding(start = 2.dp, bottom = with(density) { (digitSize * 0.12f).toDp() }),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SunrisePicker(selected: SunriseKind, onPick: (SunriseKind) -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.netz_pick_sunrise)) },
            text = {
                Column {
                    for ((k, label) in listOf(SunriseKind.ANY_PEAK to R.string.netz_any_peak, SunriseKind.FULL_REVEAL to R.string.netz_full_reveal)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = k == selected, role = Role.RadioButton) { onPick(k) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = k == selected, onClick = null)
                            Text(stringResource(label), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        )
    }

    companion object {
        private const val PREF_KIND = "netzCountdownSunriseKind"
        /** Where the counted sunrise's height sits on the screen, from the top. */
        private const val HORIZON_FRACTION = 0.82f
        private const val MAX_RENDER_WIDTH = 720
        /**
         * Horizontal field of view per unit of aspect ratio: about 14 degrees top to bottom on any screen, so a ridge a few
         * tenths of a degree high still reads, and the 18% below the horizon reaches about 2.5 degrees down (the near ground,
         * which the imagery can't detail at this scale, kept to a strip).
         */
        private const val BASE_FOV_DEG = 14.0
        private const val MIN_FOV_DEG = 5.0
        private const val MAX_FOV_DEG = 60.0
        private const val SUN_RADIUS_DEG = 0.2666
        private const val OFFLINE_RETRY_MS = 60_000L
        /** How many times a picture drawn partly from AWS heights is redrawn, OFFLINE_RETRY_MS apart. */
        private const val MAX_REDRAWS = 3
        /** How long the picture may take before a rough preview is drawn in the meantime. */
        private const val PREVIEW_AFTER_MS = 1500L
        /** The most the drawn Sun is moved to meet the drawn ridge; more means the picture is not of that horizon. */
        private const val MAX_SUN_OFFSET_DEG = 1.0
        /** The longest the "updating" banner stays up: downloads time out well before this, so reaching it means something got stuck. */
        private const val MAX_UPDATING_MS = 120_000L
    }
}
