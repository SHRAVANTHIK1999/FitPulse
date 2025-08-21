package com.example.fitpulse;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "steps")
public class StepEntry {

    @PrimaryKey
    @NotNull
    public String date;

    public int steps;

    public StepEntry(@NotNull String date, int steps) {
        this.date = date;
        this.steps = steps;
    }

    // Add these getters
    public String getDate() {
        return date;
    }

    public int getStepCount() {
        return steps;
    }
}
