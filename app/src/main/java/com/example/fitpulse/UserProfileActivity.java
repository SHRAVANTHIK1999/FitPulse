package com.example.fitpulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * UserProfileActivity
 * - Shows user name/email from "user_data" prefs.
 * - Shows daily goal and today's steps with a progress bar.
 * - Updates live when STEP_UPDATE broadcasts arrive.
 * - Bottom nav is visible but no item is selected on this screen.
 */
public class UserProfileActivity extends AppCompatActivity {

    private TextView nameText, emailText;
    private TextView dailyGoalTv, reachedGoalTv, goalHelperTv;
    private ProgressBar goalProgress;
    private ImageView backButton;
    private BottomNavigationView bottomNav;

    // Preferences:
    // - user_data: name/email
    // - FitPulsePrefs: step_goal, today_steps
    private SharedPreferences userPrefs;
    private SharedPreferences fitPulsePrefs;

    /** Receives step updates and refreshes the goal section. */
    private final BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!"STEP_UPDATE".equals(intent.getAction())) return;
            int stepsToday = intent.getIntExtra("steps_today", 0);
            fitPulsePrefs.edit().putInt("today_steps", stepsToday).apply();
            updateGoalUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Bind views
        nameText      = findViewById(R.id.user_name);
        emailText     = findViewById(R.id.user_email);
        dailyGoalTv   = findViewById(R.id.daily_goal_value);
        reachedGoalTv = findViewById(R.id.reached_goal_value);
        goalHelperTv  = findViewById(R.id.goal_helper);
        goalProgress  = findViewById(R.id.goal_progress);
        backButton    = findViewById(R.id.btn_back_profile);
        bottomNav     = findViewById(R.id.bottom_navigation);

        backButton.setOnClickListener(v -> finish());

        // Open prefs
        userPrefs     = getSharedPreferences("user_data", MODE_PRIVATE);
        fitPulsePrefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);

        // Static user info
        setOrHide(nameText,  userPrefs.getString("name",  null));
        setOrHide(emailText, userPrefs.getString("email", null));

        // First render of goal/steps
        updateGoalUi();

        // Bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)    { startActivity(new Intent(this, MainActivity.class));           finish(); return true; }
            if (id == R.id.nav_stats)   { startActivity(new Intent(this, StatsActivity.class));         finish(); return true; }
            if (id == R.id.nav_monitor) { startActivity(new Intent(this, SensorMonitorActivity.class)); finish(); return true; }
            if (id == R.id.nav_settings){ startActivity(new Intent(this, SettingsActivity.class));      finish(); return true; }
            return false;
        });

        // Clear selection so it doesn't appear as if "Home" is active
        bottomNav.post(this::clearBottomSelection);
    }

    @Override protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(stepReceiver, new IntentFilter("STEP_UPDATE"));
    }

    @Override protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        updateGoalUi();
        clearBottomSelection();
    }

    /** Read goal/steps from prefs and update labels + progress bar. */
    private void updateGoalUi() {
        int dailyGoal    = safeGetInt(fitPulsePrefs, SettingsActivity.STEP_GOAL_KEY, 10000);
        int reachedToday = safeGetInt(fitPulsePrefs, "today_steps", 0);

        if (dailyGoal < 0) dailyGoal = 0;
        if (reachedToday < 0) reachedToday = 0;

        dailyGoalTv.setText(String.valueOf(dailyGoal));
        reachedGoalTv.setText(String.valueOf(reachedToday));

        int pct = (dailyGoal > 0)
                ? Math.min(100, (int) Math.round(reachedToday * 100.0 / dailyGoal))
                : 0;
        goalProgress.setMax(100);
        goalProgress.setProgress(pct);
        goalHelperTv.setText(pct + "% of daily goal");
    }

    /** Uncheck all items in the bottom nav for this screen. */
    private void clearBottomSelection() {
        if (bottomNav == null) return;
        Menu menu = bottomNav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }

    /** Set text or hide the TextView if value is empty. */
    private void setOrHide(TextView tv, String value) {
        if (TextUtils.isEmpty(value)) {
            tv.setVisibility(android.view.View.GONE);
        } else {
            tv.setVisibility(android.view.View.VISIBLE);
            tv.setText(value);
        }
    }

    /** Safely get an int from prefs, tolerating String values. */
    private int safeGetInt(SharedPreferences prefs, String key, int def) {
        try { return prefs.getInt(key, def); }
        catch (ClassCastException e) {
            try { return Integer.parseInt(prefs.getString(key, String.valueOf(def))); }
            catch (Exception ex) { return def; }
        }
    }
}
