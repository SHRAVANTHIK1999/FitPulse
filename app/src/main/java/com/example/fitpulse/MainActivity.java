package com.example.fitpulse;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Home screen:
 * - Shows today's steps, calories, and active time.
 * - Listens for STEP_UPDATE broadcasts and animates the step counter.
 * - Reads today's persisted steps from Room on resume.
 */
public class MainActivity extends AppCompatActivity {

    private StepProgressView progressView;
    private TextView stepsText;
    private TextView caloriesText;
    private TextView durationText;
    private ImageView btnBackHome;

    /** Daily goal used by the circular progress view (loaded from prefs). */
    private int stepGoal = 10000;

    /** Receives live step updates from StepCounterManager and refreshes UI. */
    private final BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("STEP_UPDATE".equals(intent.getAction())) {
                int stepsToday = intent.getIntExtra("steps_today", 0);
                updateSteps(stepsToday);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        progressView  = findViewById(R.id.progress_view);
        stepsText     = findViewById(R.id.steps_text);
        caloriesText  = findViewById(R.id.calories_text);
        durationText  = findViewById(R.id.duration_text);
        btnBackHome   = findViewById(R.id.btn_back_home);

        // Load goal from Settings (FitPulsePrefs/step_goal)
        stepGoal = getSavedStepGoal();

        if (btnBackHome != null) btnBackHome.setOnClickListener(v -> {});

        // Bottom navigation wiring
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true; // already here
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(MainActivity.this, StatsActivity.class));
                return true;
            } else if (id == R.id.nav_monitor) {
                startActivity(new Intent(MainActivity.this, SensorMonitorActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    /** Reads the saved step goal (defaults to 10,000). */
    private int getSavedStepGoal() {
        SharedPreferences prefs = getSharedPreferences("FitPulsePrefs", MODE_PRIVATE);
        return prefs.getInt("step_goal", 10000);
    }

    /** Animator used to smoothly count numbers up/down in the step text. */
    private ValueAnimator stepAnimator;

    /** Updates UI for steps, progress, calories, and duration. */
    private void updateSteps(int steps) {
        // Parse the current number shown in the label (fallback to 0)
        int oldSteps;
        try {
            oldSteps = Integer.parseInt(stepsText.getText().toString().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            oldSteps = 0;
        }

        // Stop any ongoing animation before starting a new one
        if (stepAnimator != null && stepAnimator.isRunning()) {
            stepAnimator.cancel();
        }

        // Short animations for tiny changes; cap duration for large jumps
        int diff = Math.abs(steps - oldSteps);
        long duration = diff <= 3 ? 120 : Math.min(500, 20L * diff);

        stepAnimator = ValueAnimator.ofInt(oldSteps, steps);
        stepAnimator.setDuration(duration);
        stepAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            stepsText.setText(String.format(Locale.getDefault(), "%,d Steps", animatedValue));
        });
        stepAnimator.start();

        // Ring progress
        progressView.setSteps(steps, stepGoal);

        // Simple estimates
        float calories = steps * 0.04f;
        caloriesText.setText(String.format(Locale.getDefault(), "%.0f Cal", calories));

        int minutes = steps / 130;
        int seconds = (int) ((steps % 130) / 2.2);
        durationText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Begin listening to hardware step counter
        StepCounterManager.get(this).start();
    }

    @Override
    protected void onStop() {
        // Stop listening when screen not visible
        StepCounterManager.get(this).stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load today's persisted steps from Room (background thread)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            StepEntry todayEntry = db.stepDao().getStepsByDate(today);
            int steps = todayEntry != null ? todayEntry.steps : 0;
            runOnUiThread(() -> updateSteps(steps));
        }).start();

        // Listen for live updates while this Activity is in foreground
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(stepReceiver, new IntentFilter("STEP_UPDATE"));
    }

    @Override
    protected void onPause() {
        // Stop receiving broadcasts when paused
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver);
    }
}
