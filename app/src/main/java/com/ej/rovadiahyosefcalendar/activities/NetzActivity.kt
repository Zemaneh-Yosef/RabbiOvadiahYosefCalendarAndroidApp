package com.ej.rovadiahyosefcalendar.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
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
import com.ej.rovadiahyosefcalendar.classes.royzmanim.SolarEvent
import com.ej.rovadiahyosefcalendar.classes.royzmanim.StarNightfall
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.coroutines.Dispatchers
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
        RefractionRepository.ensureFresh(MainFragmentManagerActivity.sLatitude, MainFragmentManagerActivity.sLongitude)
    }

    override fun onStop() {
        RefractionRepository.removeListener(refractionListener)
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
        val sunrise: Instant?
        val terrain: Boolean
        if (kind == SunriseKind.FULL_REVEAL) {
            sunrise = cal.sunriseFullDisk
            terrain = cal.isSunriseFullDiskFromTerrain
        } else {
            sunrise = cal.haNetz ?: cal.seaLevelSunrise
            terrain = cal.isHaNetzFromTerrain
        }
        return Day(date, sunrise, terrain, cal.chatzot, cal.previousSolarMidnight)
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
        val days = remember(cal, kind, today) { Pair(dayOf(cal, today, kind), dayOf(cal, today.plusDays(1), kind)) }
        val phase = phaseAt(days.first, days.second, now)
        val target = phase.day.sunrise

        // The sky follows the Sun's depression, but never from earlier than the hatzot halayla before the counted sunrise:
        // before it, the sky is hatzot halayla's.
        val hatzot = phase.day.hatzotHalayla
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
            val vantage = vantageFor(cal.refractionData?.horizon, riseAltAz?.get(1) ?: 90.0)
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
            )
            var panorama by remember { mutableStateOf<Panorama?>(null) }
            var image by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(view) {
                if (view == null) { panorama = null; image = null; return@LaunchedEffect }
                if (panorama?.view != view) { panorama = null; image = null }
                // offline with nothing cached: no terrain or Sun, and try again now and then
                while (isActive) {
                    // a fresh TerrainTiles per attempt: it gives up on a host for its lifetime once it looks unreachable
                    val p = withContext(Dispatchers.IO) { PanoramaRenderer.render(TerrainTiles(cacheDir), view) { !isActive } }
                    if (p != null) {
                        image = p.bitmap.asImageBitmap()
                        panorama = p
                        break
                    }
                    delay(OFFLINE_RETRY_MS)
                }
            }

            val pano = panorama
            val scale = wPx.toFloat() / renderW
            val horizonY = pano?.project(pano.view.headingDeg, 0.0)?.y?.times(scale) ?: (hPx * HORIZON_FRACTION)
            val groundTopAt = { x: Float -> pano?.skylineAt(x / scale)?.times(scale) ?: (hPx * HORIZON_FRACTION) }
            val darken = remember(sky.dark) {
                val b = 1f - sky.dark
                ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(b, b, b, 1f) })
            }
            val sunPx = with(density) { 8.dp.toPx() }

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
                    // the Sun now, behind the terrain (drawn first, so the ridge hides what is below it)
                    val app = SkyColors.apparentAltitude(sunNow[0])
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

            Countdown(
                phase = phase, now = now, kind = kind,
                heightPx = horizonY.coerceIn(hPx * 0.35f, hPx.toFloat()),
                widthPx = wPx.toFloat(),
            )

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

            if (pano != null) Text(
                stringResource(R.string.netz_imagery_credit),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        if (showPicker) SunrisePicker(
            selected = kind,
            onPick = {
                kind = it
                prefs.edit().putString(PREF_KIND, it.pref).apply()
                showPicker = false
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
    }
}
