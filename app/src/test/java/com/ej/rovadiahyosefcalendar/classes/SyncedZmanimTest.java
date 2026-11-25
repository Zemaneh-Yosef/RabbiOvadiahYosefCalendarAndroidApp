package com.ej.rovadiahyosefcalendar.classes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.kosherjava.zmanim.util.GeoLocation;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

/** The table the phone sends the watch reads back as the times the phone computes. */
public class SyncedZmanimTest {
    private final GeoLocation jerusalem = new GeoLocation("Jerusalem", 31.778, 35.2354, 754, ZoneId.of("Asia/Jerusalem"));

    @Test
    public void tableReadsBackAsThePhoneTimes() {
        SyncedZmanim table = SyncedZmanim.parse(WatchZmanimSync.buildTable(jerusalem).toString());
        assertNotNull(table);
        assertTrue(table.matches(jerusalem));

        ROZmanimCalendar cal = new ROZmanimCalendar(jerusalem, null);
        cal.setAmudehHoraah(true);
        LocalDate date = LocalDate.now(jerusalem.getZoneId()).plusDays(10);
        cal.setLocalDate(date);
        SyncedZmanim.Day day = table.day(date);
        assertNotNull(day);
        assertEquals(cal.getSunrise(), day.sunrise);
        assertEquals(cal.getSeaLevelSunrise(), day.seaLevelSunrise);
        assertEquals(cal.getSunset(), day.sunset);
        assertEquals(cal.getSeaLevelSunset(), day.seaLevelSunset);
        assertEquals(cal.getDegreeTzetWithHaze(7.165), day.tzet7165);
        assertNull(day.haNetz); // no terrain horizon without data
        assertTrue(day.has85);
        assertEquals(cal.getSunriseOffsetByDegrees(98.5), day.sunriseAt85);
        assertEquals(cal.getSunsetOffsetByDegrees(98.5), day.sunsetAt85);
        assertEquals(cal.getEquinoxPercentOfShaahZmanis(16.04, false), table.equinox(date.getYear(), 16.04, false));

        assertNull(table.day(date.plusDays(60)));
    }

    @Test
    public void otherPlaceDoesNotMatch() {
        SyncedZmanim table = SyncedZmanim.parse(WatchZmanimSync.buildTable(jerusalem).toString());
        assertNotNull(table);
        assertFalse(table.matches(new GeoLocation("Tel Aviv", 32.0853, 34.7818, 754, ZoneId.of("Asia/Jerusalem"))));
        assertFalse(table.matches(new GeoLocation("Jerusalem", 31.778, 35.2354, 0, ZoneId.of("Asia/Jerusalem"))));
        assertNull(SyncedZmanim.parse("{\"v\":99}"));
        assertNull(SyncedZmanim.parse("not json"));
    }
}
