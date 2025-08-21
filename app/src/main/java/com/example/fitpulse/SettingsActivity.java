package com.example.fitpulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * SettingsActivity
 * - Lets the user view/update the Daily Step Goal (stored in SharedPreferences "FitPulsePrefs").
 * - Provides "View Profile" and "Logout" actions.
 * - Wires bottom navigation to other screens.
 * - Starts/stops the global StepCounterManager while this screen is visible.
 */
public class SettingsActivity extends AppCompatActivity {

    // UI references
    EditText editGoal;
    Button btnSaveGoal, btnLogout, btnViewProfile;
    ImageView btnBack;

    // SharedPreferences file and key for the step goal (used across the app)
    public static final String PREFS_NAME = "FitPulsePrefs";
    public static final String STEP_GOAL_KEY = "step_goal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Bind views
        editGoal       = findViewById(R.id.edit_goal);
        btnSaveGoal    = findViewById(R.id.btn_save_goal);
        btnBack        = findViewById(R.id.btn_back_home);
        btnLogout      = findViewById(R.id.btn_logout);
        btnViewProfile = findViewById(R.id.btn_view_profile);

        // Load saved goal into the input (default 10,000 if none saved yet)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedGoal = prefs.getInt(STEP_GOAL_KEY, 10000);
        editGoal.setText(String.valueOf(savedGoal));

        // Save goal button: validate number, persist to FitPulsePrefs, show toast
        btnSaveGoal.setOnClickListener(v -> {
            try {
                int goal = Integer.parseInt(editGoal.getText().toString());
                prefs.edit().putInt(STEP_GOAL_KEY, goal).apply();
                Toast.makeText(this, "Goal Saved!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button: return to Home
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                finish();
            });
        }

        // Logout: clear login state and navigate to Login (clear back stack)
        btnLogout.setOnClickListener(v -> {
            SharedPreferences userPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
            userPrefs.edit().putBoolean("logged_in", false).apply();

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // View Profile screen
        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });

        // Bottom navigation: mark Settings selected and handle navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_settings);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home)    { startActivity(new Intent(this, MainActivity.class)); return true; }
                if (id == R.id.nav_stats)   { startActivity(new Intent(this, StatsActivity.class)); return true; }
                if (id == R.id.nav_monitor) { startActivity(new Intent(this, SensorMonitorActivity.class)); return true; }
                if (id == R.id.nav_settings){ return true; } // already here
                return false;
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start global step listener while this screen is visible
        StepCounterManager.get(this).start();
    }

    @Override
    protected void onStop() {
        // Stop global step listener when leaving this screen
        StepCounterManager.get(this).stop();
        super.onStop();
    }
}
