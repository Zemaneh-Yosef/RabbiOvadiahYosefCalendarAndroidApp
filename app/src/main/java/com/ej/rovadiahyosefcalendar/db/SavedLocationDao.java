package com.ej.rovadiahyosefcalendar.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for {@link SavedLocation}.
 *
 * All methods are synchronous and intended to be called from a background
 * thread (e.g. via Executors.newSingleThreadExecutor()). Do NOT call these
 * on the main thread — Room will throw if you do.
 */
@Dao
public interface SavedLocationDao {

    // -----------------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------------

    @Query("SELECT * FROM saved_locations ORDER BY lastUsedAt DESC")
    List<SavedLocation> getAllOrderedByRecent();

    @Query("SELECT * FROM saved_locations ORDER BY lastUsedAt DESC LIMIT :limit")
    List<SavedLocation> getRecentLocations(int limit);

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    SavedLocation findById(int id);

    @Query("SELECT * FROM saved_locations WHERE name = :name LIMIT 1")
    SavedLocation findByName(String name);

    @Query("SELECT COUNT(*) FROM saved_locations")
    int count();

    @Query("SELECT * FROM saved_locations ORDER BY lastUsedAt ASC LIMIT 1")
    SavedLocation getOldest();

    // -----------------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(SavedLocation location);

    @Update
    void update(SavedLocation location);

    @Query("DELETE FROM saved_locations WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM saved_locations WHERE id = " +
        "(SELECT id FROM saved_locations ORDER BY lastUsedAt ASC LIMIT 1)")
    void deleteOldest();

    @Query("DELETE FROM saved_locations")
    void deleteAll();

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Query("UPDATE saved_locations SET lastUsedAt = :timestamp WHERE name = :name")
    void updateLastUsed(String name, long timestamp);

    /**
     * NEW: Updates ALL fields of an existing location.
     * This is required so timezone, lat, lon, and lastUsedAt stay correct.
     */
    @Query("UPDATE saved_locations " +
        "SET latitude = :lat, longitude = :lon, timezoneId = :tz, lastUsedAt = :ts " +
        "WHERE name = :name")
    void updateLocation(String name, double lat, double lon, String tz, long ts);
}
