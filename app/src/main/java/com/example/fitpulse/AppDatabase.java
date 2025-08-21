package com.example.fitpulse;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

/**
 * Central Room database for the app.
 *
 * - Declares the list of @Entity classes managed by Room (here: StepEntry).
 * - Specifies the schema version (version = 1).
 * - Room generates the concrete implementation of this abstract class.
 */
@Database(entities = {StepEntry.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    /** Singleton instance to ensure only one DB object exists per process. */
    private static AppDatabase instance;

    /**
     * Data Access Object (DAO) accessor for StepEntry operations.
     * Room will generate the implementation at compile time.
     */
    public abstract StepDao stepDao();

    /**
     * Thread-safe (synchronized) getter for the singleton DB instance.
     *
     * @param context any Context; applicationContext is used to avoid leaking an Activity.
     * @return the single AppDatabase instance.
     *
     * Implementation details:
     *  - Uses Room.databaseBuilder to create/return the DB.
     *  - .fallbackToDestructiveMigration() is convenient during development:
     *      if the schema version changes without a defined Migration, Room will
     *      DROP and RECREATE the database (⚠ this wipes existing data).
     *    Replace with proper Migration(s) before production if you need to preserve data.
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(), // use app context to avoid memory leaks
                            AppDatabase.class,               // RoomDatabase subclass
                            "step_db"                        // on-disk database filename
                    )
                    .fallbackToDestructiveMigration()       // dev convenience: resets DB on schema mismatch
                    .build();                               // build the DB instance
        }
        return instance;
    }
}
