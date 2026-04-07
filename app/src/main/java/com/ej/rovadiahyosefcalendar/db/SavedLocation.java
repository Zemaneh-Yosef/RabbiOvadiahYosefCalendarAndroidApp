package com.ej.rovadiahyosefcalendar.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a user-saved location stored in the Room database.
 * Replaces the old SharedPreferences keys: location1..5, location1Lat..5Lat,
 * location1Long..5Long, location1Timezone..5Timezone.
 *
 * The `name` column has a unique index so duplicate location names are rejected
 * at the database level, mirroring the old HashSet de-duplication logic.
 */
@Entity(
        tableName = "saved_locations",
        indices = {@Index(value = "name", unique = true)}
)
public class SavedLocation {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Human-readable display name (e.g. "Brooklyn, New York (11201)"). */
    public String name;

    public double latitude;
    public double longitude;

    /** IANA timezone ID string, e.g. "America/New_York". */
    public String timezoneId;

    /**
     * Epoch millis of when this location was last selected.
     * Used to order the list most-recent-first and to evict the oldest
     * entry when the list grows beyond MAX_SAVED_LOCATIONS.
     */
    public long lastUsedAt;

    // -----------------------------------------------------------------------
    // Convenience constructor
    // -----------------------------------------------------------------------

    public SavedLocation(String name, double latitude, double longitude,
                         String timezoneId, long lastUsedAt) {
        this.name       = name;
        this.latitude   = latitude;
        this.longitude  = longitude;
        this.timezoneId = timezoneId;
        this.lastUsedAt = lastUsedAt;
    }
}
