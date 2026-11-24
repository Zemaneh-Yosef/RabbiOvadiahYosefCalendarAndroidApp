package com.ej.rovadiahyosefcalendar.classes

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionRepository
import com.ej.rovadiahyosefcalendar.classes.royzmanim.RefractionSupport
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.kosherjava.zmanim.ZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Keeps the watch showing the same times as the phone. The watch has no weather, terrain or haze data, so the phone computes the
 * times those data move (sunrise and sunset, visible sunrise, nightfall by the stars, the Amudei Horaah equinox ratios) and sends
 * them ([SyncedZmanim], path "zmanim/") for the next [DAYS_AFTER] days, plus a year of visible sunrises in the ChaiTables format
 * (path "chaiTable/") for the days after that.
 *
 * The phone always shows the times from its newest data. A watch that is out of reach keeps its last table until it is back in
 * reach and gets the new one.
 */
object WatchZmanimSync {
    private const val TAG = "WatchZmanimSync"
    const val PATH_ZMANIM = "zmanim/"
    const val PATH_CHAI_TABLE = "chaiTable/"
    /** Advertised by watch apps that show the "zmanim/" table (wearos res/values/wear.xml); older ones only take "chaiTable/". */
    private const val CAPABILITY = "synced_zmanim"
    private const val PREFS = "watchZmanimSync"
    /** The last table every watch in reach received, so an unchanged table isn't sent again. */
    private const val KEY_DELIVERED = "delivered"
    private const val DAYS_BEFORE = 1L
    private const val DAYS_AFTER = 30L
    private const val DEBOUNCE_MS = 3000L
    /** The dips the Amudei Horaah zmanim take an equinox ratio for: (degrees, after sunset). */
    private val EQUINOX_DIPS = listOf(16.04 to false, 3.7 to true, 5.075 to true, 16.04 to true)

    private var prefs: SharedPreferences? = null
    private val executor by lazy { Executors.newSingleThreadExecutor { r -> Thread(r, "watch-sync").apply { isDaemon = true } } }
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private var force = false
    /** The year of visible sunrises last built: (place|Jewish year|day built) -> table, so it's built at most once a day. */
    private var chaiCache: Pair<String, String>? = null

    @JvmStatic
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /**
     * Send the watch the current times soon (several requests in a row make one sync).
     * @param forceSend send even when the table is the one the watch already has (e.g. the watch app may have been reinstalled)
     */
    @JvmStatic
    fun requestSync(context: Context, forceSend: Boolean) {
        val app = context.applicationContext
        main.post {
            force = force || forceSend
            main.removeCallbacksAndMessages(this)
            main.postAtTime({
                val f = force
                force = false
                val geo = MainFragmentManagerActivity.sROZmanimCalendar?.geoLocation
                val name = MainFragmentManagerActivity.sCurrentLocationName
                if (geo != null && !name.isNullOrEmpty()) executor.execute { sync(app, geo.clone() as GeoLocation, name, f) }
            }, this, android.os.SystemClock.uptimeMillis() + DEBOUNCE_MS)
        }
    }

    private fun sync(context: Context, geo: GeoLocation, locationName: String, forceSend: Boolean) {
        val p = prefs ?: return
        // every watch in reach takes the year of visible sunrises; only the ones with this watch app's capability take the table
        val connected: List<Node> = try {
            Tasks.await(Wearable.getNodeClient(context).connectedNodes, 10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "No watch connection: $e")
            emptyList()
        }
        val capable: Collection<Node> = if (connected.isEmpty()) emptyList() else try {
            Tasks.await(Wearable.getCapabilityClient(context).getCapability(CAPABILITY, CapabilityClient.FILTER_REACHABLE), 10, TimeUnit.SECONDS).nodes
        } catch (e: Exception) {
            Log.w(TAG, "Watch capability lookup failed: $e")
            emptyList()
        }
        // after the "prefs/" message (the watch files it under the location name that message carries)
        if (forceSend && connected.isNotEmpty()) {
            val chai = try { chaiTable(geo, locationName) } catch (e: Exception) { Log.e(TAG, "Building the visible sunrise table failed", e); null }
            if (chai != null) connected.forEach { send(context, it, PATH_CHAI_TABLE, chai) }
        }
        val delivered = p.getString(KEY_DELIVERED, null)
        if (capable.isEmpty()) {
            return// no watch in reach: it gets the newest table when it's back (the next sync)
        }
        val table = try { buildTable(geo).toString() } catch (e: Exception) { Log.e(TAG, "Building the table failed", e); return }
        if (table == delivered && !forceSend) {
            return
        }
        val allReceived = capable.all { send(context, it, PATH_ZMANIM, table) }
        if (allReceived) p.edit().putString(KEY_DELIVERED, table).apply()
    }

    private fun send(context: Context, node: Node, path: String, text: String): Boolean = try {
        Tasks.await(Wearable.getMessageClient(context).sendMessage(node.id, path, text.toByteArray(StandardCharsets.UTF_8)), 10, TimeUnit.SECONDS)
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send $path to ${node.displayName}: $e")
        false
    }

    /** The table for the place from the current data. */
    @JvmStatic
    fun buildTable(geo: GeoLocation): JSONObject {
        val cal = ROZmanimCalendar(geo, RefractionSupport.dataFor(geo))
        val today = LocalDate.now(geo.zoneId)
        val days = JSONObject()
        for (i in -DAYS_BEFORE..DAYS_AFTER) {
            val date = today.plusDays(i)
            cal.localDate = date
            days.put(date.toString(), JSONObject()
                .put("srS", iso(cal.seaLevelSunrise))
                .put("sr", iso(cal.sunrise))
                .put("ssS", iso(cal.seaLevelSunset))
                .put("ss", iso(cal.sunset))
                .put("netz", iso(cal.getHaNetz(true)))
                .put("tz7165", iso(cal.getDegreeTzetWithHaze(7.165)))
                // the 8.5 degree limits of the Amudei Horaah dawn and nightfall, from the phone's calculator
                .put("r85", iso(cal.getSunriseOffsetByDegrees(ZmanimCalendar.GEOMETRIC_ZENITH + 8.5)))
                .put("s85", iso(cal.getSunsetOffsetByDegrees(ZmanimCalendar.GEOMETRIC_ZENITH + 8.5))))
        }
        val equinox = JSONObject()
        for (year in setOf(today.minusDays(DAYS_BEFORE).year, today.plusDays(DAYS_AFTER).year)) {
            cal.localDate = LocalDate.of(year, 6, 1)
            for ((degrees, sunset) in EQUINOX_DIPS) {
                cal.getEquinoxPercentOfShaahZmanis(degrees, sunset)?.let { equinox.put(SyncedZmanim.equinoxKey(year, degrees, sunset), it) }
            }
        }
        return JSONObject()
            .put("v", SyncedZmanim.VERSION)
            .put("lat", geo.latitude)
            .put("lon", geo.longitude)
            .put("elev", geo.elevation)
            .put("tz", geo.zoneId.id)
            .put("days", days)
            .put("equinox", equinox)
    }

    private fun iso(i: Instant?): Any = i?.toString() ?: JSONObject.NULL

    /** This Jewish year's visible sunrises (epoch seconds, ':'-separated, as ChaiTables were sent); empty without a terrain horizon. */
    private fun chaiTable(geo: GeoLocation, locationName: String): String {
        val today = LocalDate.now(geo.zoneId)
        val year = JewishCalendar(today).jewishYear
        val key = "${geo.latitude}|${geo.longitude}|${geo.elevation}|$locationName|$year|$today"
        chaiCache?.takeIf { it.first == key }?.let { return it.second }
        val data = RefractionSupport.dataFor(geo)
        val out = StringBuilder()
        if (data?.horizon != null) {
            val cal = ROZmanimCalendar(geo, data)
            var date = JewishCalendar(year, JewishCalendar.TISHREI, 1).localDate
            val end = JewishCalendar(year + 1, JewishCalendar.TISHREI, 1).localDate
            while (date.isBefore(end)) {
                cal.localDate = date
                cal.getHaNetz(true)?.let { out.append(it.epochSecond).append(':') }
                date = date.plusDays(1)
            }
        }
        return out.toString().also { chaiCache = key to it }
    }
}
