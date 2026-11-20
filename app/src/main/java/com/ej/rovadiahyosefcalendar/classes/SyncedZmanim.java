package com.ej.rovadiahyosefcalendar.classes;

import com.kosherjava.zmanim.util.GeoLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The weather-dependent times the phone computed for a place, as sent to the watch (path "zmanim/"). The watch has no weather
 * data of its own, so it shows these instead of computing them; the phone shows the same table while a watch that has it is out
 * of reach (see WatchZmanimSync), so both always show the same times. The same class is in the wearos module.
 * <p>
 * Format: {"v":1, "lat", "lon", "elev", "tz", "days": {"yyyy-MM-dd": {"srS", "sr", "ssS", "ss", "netz", "tz7165"}},
 * "equinox": {"year|degrees|sunset": percent}}. Times are ISO-8601 instants (exact, as computed), null where there is none.
 */
public final class SyncedZmanim {
    public static final int VERSION = 1;

    /** One date's times; any of them may be null (no such time that day). */
    public static final class Day {
        public final Instant seaLevelSunrise, sunrise, seaLevelSunset, sunset, haNetz, tzet7165;

        Day(JSONObject j) {
            seaLevelSunrise = instant(j, "srS");
            sunrise = instant(j, "sr");
            seaLevelSunset = instant(j, "ssS");
            sunset = instant(j, "ss");
            haNetz = instant(j, "netz");
            tzet7165 = instant(j, "tz7165");
        }

        private static Instant instant(JSONObject j, String key) {
            return j.isNull(key) ? null : Instant.parse(j.optString(key));
        }
    }

    private final double lat, lon, elev;
    private final String zone;
    private final Map<String, Day> days = new HashMap<>();
    private final Map<String, Double> equinox = new HashMap<>();

    private SyncedZmanim(JSONObject j) throws JSONException {
        lat = j.getDouble("lat");
        lon = j.getDouble("lon");
        elev = j.getDouble("elev");
        zone = j.getString("tz");
        JSONObject d = j.getJSONObject("days");
        for (Iterator<String> it = d.keys(); it.hasNext(); ) {
            String k = it.next();
            days.put(k, new Day(d.getJSONObject(k)));
        }
        JSONObject e = j.optJSONObject("equinox");
        if (e != null) {
            for (Iterator<String> it = e.keys(); it.hasNext(); ) {
                String k = it.next();
                equinox.put(k, e.getDouble(k));
            }
        }
    }

    /** The table in a message, or null when it is missing, unreadable or of another version. */
    public static SyncedZmanim parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject j = new JSONObject(json);
            return j.optInt("v") == VERSION ? new SyncedZmanim(j) : null;
        } catch (JSONException e) {
            return null;
        }
    }

    /** Whether the table was computed for this place (the same coordinates, elevation and time zone). */
    public boolean matches(GeoLocation g) {
        return g != null && Math.abs(g.getLatitude() - lat) < 1e-6 && Math.abs(g.getLongitude() - lon) < 1e-6
                && Math.abs(g.getElevation() - elev) < 0.5 && g.getZoneId() != null && zone.equals(g.getZoneId().getId());
    }

    /** The date's times, or null when the table doesn't cover it. */
    public Day day(LocalDate date) {
        return date == null ? null : days.get(date.toString());
    }

    /** The equinox percentage of a seasonal hour for a dip (see ROZmanimCalendar), or null when the table doesn't have it. */
    public Double equinox(int year, double degrees, boolean sunset) {
        return equinox.get(equinoxKey(year, degrees, sunset));
    }

    public static String equinoxKey(int year, double degrees, boolean sunset) {
        return year + "|" + degrees + "|" + sunset;
    }
}
