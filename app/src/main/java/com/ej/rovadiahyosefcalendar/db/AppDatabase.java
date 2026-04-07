package com.ej.rovadiahyosefcalendar.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The single Room database for the app.
 *
 * Access it via {@link #getInstance(Context)} — never construct it directly.
 * The singleton pattern here is thread-safe via double-checked locking.
 *
 * Schema version history
 * ──────────────────────
 * 1 → initial schema (saved_locations table)
 */
@Database(entities = {SavedLocation.class}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase sInstance;

    /** The maximum number of locations to keep. Oldest is evicted beyond this. */
    public static final int MAX_SAVED_LOCATIONS = 10; // no longer arbitrarily capped at 5

    public abstract SavedLocationDao savedLocationDao();

    public static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "rovadiah_yosef_calendar.db"
                            )
                            // If you need to migrate from an older schema in the future,
                            // add .addMigrations(MIGRATION_1_2, ...) here.
                            // For the initial release just use destructive to stay safe:
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return sInstance;
    }
}
