package com.example.fitpulse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Central manager for step counting using the hardware TYPE_STEP_COUNTER sensor.
 * - Maintains a per-day baseline to compute "today's steps".
 * - Persists latest count to Room.
 * - Broadcasts STEP_UPDATE (local) with "steps_today" for UI screens.
 * - Uses a ref-counted start/stop so multiple screens can safely share it.
 */
public class StepCounterManager implements SensorEventListener {

    /** Singleton instance (one per process). */
    private static StepCounterManager INSTANCE;

    /** App context and sensor objects. */
    private final Context appCtx;
    private final SensorManager sensorManager;
    private final Sensor stepCounter;

    /** Reference count for start()/stop() calls. */
    private int startCount = 0;

    /** Private SharedPreferences for daily baseline bookkeeping. */
    private static final String PREF_NAME = "step_prefs";

    // Throttle + de-dupe for broadcasts (prevents UI spam)
    private int lastSentSteps = -1;
    private long lastSentAtMs = 0L;
    private static final long MIN_BROADCAST_INTERVAL_MS = 200; // ~5 Hz max

    /** Private constructor; use get(Context) to obtain the singleton. */
    private StepCounterManager(Context ctx) {
        appCtx = ctx.getApplicationContext();
        sensorManager = (SensorManager) appCtx.getSystemService(Context.SENSOR_SERVICE);
        stepCounter = (sensorManager != null) ? sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) : null;
    }

    /**
     * Get/create the singleton instance.
     */
    public static synchronized StepCounterManager get(Context ctx) {
        if (INSTANCE == null) INSTANCE = new StepCounterManager(ctx);
        return INSTANCE;
    }

    /**
     * Register the step listener if not already active.
     * Multiple callers can invoke start(); the sensor is registered on the first start only.
     */
    public synchronized void start() {
        if (sensorManager == null || stepCounter == null) return;
        if (startCount++ > 0) return; // already active
        sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Unregister the listener when no more callers need it.
     * Sensor stops when the ref-count drops to zero.
     */
    public synchronized void stop() {
        if (sensorManager == null || stepCounter == null) return;
        if (startCount == 0) return;
        if (--startCount == 0) {
            sensorManager.unregisterListener(this);
        }
    }

    /**
     * Sensor callback: compute today's steps using a per-day baseline and notify listeners.
     */
    @Override
    public void onSensorChanged(android.hardware.SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        // Cumulative steps since boot
        int totalSteps = (int) event.values[0];
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Load/initialize today's baseline so: todaySteps = totalSinceBoot - baseline
        SharedPreferences prefs = appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastSavedDate = prefs.getString("last_date", null);

        if (lastSavedDate == null || !lastSavedDate.equals(today)) {
            prefs.edit()
                    .putString("last_date", today)
                    .putInt("base_steps_" + today, totalSteps)
                    .apply();
        }

        int baseSteps = prefs.getInt("base_steps_" + today, totalSteps);
        int todaySteps = Math.max(0, totalSteps - baseSteps);

        // Throttle: skip duplicates and too-frequent updates
        long now = System.currentTimeMillis();
        if (todaySteps == lastSentSteps && (now - lastSentAtMs) < MIN_BROADCAST_INTERVAL_MS) {
            return;
        }
        if ((now - lastSentAtMs) < MIN_BROADCAST_INTERVAL_MS) {
            return; // too soon since last send
        }
        lastSentSteps = todaySteps;
        lastSentAtMs = now;

        // Persist latest value for today to Room on a background thread
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(appCtx);
            db.stepDao().insert(new StepEntry(today, todaySteps));
        }).start();

        // Broadcast the update to interested screens (local within app)
        Intent stepIntent = new Intent("STEP_UPDATE");
        stepIntent.putExtra("steps_today", todaySteps);
        LocalBroadcastManager.getInstance(appCtx).sendBroadcast(stepIntent);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
