package com.example.fitpulse;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StepEntry stepEntry);

    // Last 7 days steps
    @Query("SELECT * FROM steps ORDER BY date DESC LIMIT 7")
    List<StepEntry> getLast7Days();

    // Get all steps sorted by date ASC
    @Query("SELECT * FROM steps ORDER BY date ASC")
    List<StepEntry> getAll();

    // Get specific date
    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    StepEntry getStepsByDate(String date);

    // All steps (no limit)
    @Query("SELECT * FROM steps ORDER BY date DESC")
    List<StepEntry> getAllSteps();
}
