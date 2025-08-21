package com.example.fitpulse;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences; // ← added
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;

/**
 * Shows live accel/gyro, today's steps, BMI calculator, and posts a notification
 * when the step goal is reached.
 */
public class SensorMonitorActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensor, gyroSensor; // step counter is handled by StepCounterManager

    private TextView stepDataText, accelDataText, gyroDataText;

    // BMI UI
    private TextInputEditText etWeight, etHeight;
    private TextView bmiValueText, bmiStatusText;
    private MaterialButton btnCalcBmi;

    private static final String CHANNEL_ID = "step_goal_channel";
    private static final int STEP_GOAL = 10000; // default (actual goal read from prefs)

    /** Receives today's step updates and refreshes UI / checks goal. */
    private final BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!"STEP_UPDATE".equals(intent.getAction())) return;
            int todaySteps = intent.getIntExtra("steps_today", 0);

            if (stepDataText != null) {
                stepDataText.setText(String.format(Locale.getDefault(), "%d", todaySteps));
            }

            // Use goal from Settings; fire when steps >= goal
            int goal = getStepGoalFromPrefs();
            if (todaySteps >= goal) {
                sendStepGoalNotification(todaySteps);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_monitor);

        // Bind views
        stepDataText  = findViewById(R.id.step_data);
        //accelDataText = findViewById(R.id.accel_data); // bind only if present in XML
        gyroDataText  = findViewById(R.id.gyro_data);

        // BMI views
        etWeight      = findViewById(R.id.et_weight);
        etHeight      = findViewById(R.id.et_height);
        bmiValueText  = findViewById(R.id.bmi_value_text);
        bmiStatusText = findViewById(R.id.bmi_status_text);
        btnCalcBmi    = findViewById(R.id.btn_calc_bmi);

        // Bold input when text is present
        attachBoldOnInput(etWeight);
        attachBoldOnInput(etHeight);

        if (btnCalcBmi != null) {
            btnCalcBmi.setOnClickListener(v -> {
                String ws = (etWeight != null && etWeight.getText() != null) ? etWeight.getText().toString() : "";
                String hs = (etHeight != null && etHeight.getText() != null) ? etHeight.getText().toString() : "";

                ws = ws.trim().replace(',', '.');
                hs = hs.trim().replace(',', '.');

                if (ws.isEmpty() || hs.isEmpty()) {
                    setBmiResult("—", "Enter weight & height");
                    return;
                }
                try {
                    float w = Float.parseFloat(ws);     // kg
                    float hcm = Float.parseFloat(hs);   // cm
                    float hm = hcm / 100f;              // m
                    if (w <= 0 || hm <= 0) {
                        setBmiResult("—", "Invalid values");
                        return;
                    }
                    float bmi = w / (hm * hm);
                    setBmiResult(String.format(Locale.getDefault(), "%.1f", bmi), bmiCategory(bmi));
                } catch (NumberFormatException e) {
                    setBmiResult("—", "Invalid number");
                }
            });
        }

        // Sensors used on this screen
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Activity recognition permission (Android 10+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    2001
            );
        }

        createNotificationChannel();

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_monitor);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)    { startActivity(new Intent(this, MainActivity.class)); return true; }
            if (id == R.id.nav_stats)   { startActivity(new Intent(this, StatsActivity.class)); return true; }
            if (id == R.id.nav_monitor) { return true; }
            if (id == R.id.nav_settings){ startActivity(new Intent(this, SettingsActivity.class)); return true; }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ensure global step listener is running
        StepCounterManager.get(this).start();
    }

    @Override
    protected void onStop() {
        // Stop global step listener when not visible
        StepCounterManager.get(this).stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show today's steps from DB immediately
        new Thread(() -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            StepEntry todayEntry = db.stepDao().getStepsByDate(today);
            int steps = (todayEntry != null) ? todayEntry.steps : 0;
            runOnUiThread(() -> {
                if (stepDataText != null) {
                    stepDataText.setText(String.format(Locale.getDefault(), "%d", steps));
                }
                // Also check goal in case it was reached while paused
                int goal = getStepGoalFromPrefs();
                if (steps >= goal) {
                    sendStepGoalNotification(steps);
                }
            });
        }).start();

        // Register sensors for live accel/gyro values
        if (accelSensor != null)
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
        if (gyroSensor != null)
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);

        // Listen for live step broadcasts
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(stepReceiver, new IntentFilter("STEP_UPDATE"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            if (accelDataText != null) {
                accelDataText.setText(String.format(Locale.getDefault(),
                        "X: %.2f\nY: %.2f\nZ: %.2f", x, y, z));
            }

        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            if (gyroDataText != null) {
                gyroDataText.setText(String.format(Locale.getDefault(),
                        "X: %.2f\nY: %.2f\nZ: %.2f", x, y, z));
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /** Current step goal from Settings (FitPulsePrefs/step_goal). */
    private int getStepGoalFromPrefs() {
        SharedPreferences p = getSharedPreferences("FitPulsePrefs", MODE_PRIVATE);
        return p.getInt("step_goal", STEP_GOAL);
    }

    /** Create notification channel (Android O+). */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Step Goal Channel";
            String description = "Notifications for reaching step goal";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /** Post a "goal reached" notification. */
    private void sendStepGoalNotification(int steps) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎉 Step Goal Reached!")
                .setContentText("You walked " + steps + " steps today. Great job!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationManager.notify(1001, builder.build());
        }
    }

    // ----- BMI helpers -----
    private void setBmiResult(String value, String status) {
        if (bmiValueText != null)  bmiValueText.setText(value);
        if (bmiStatusText != null) bmiStatusText.setText(status);
    }

    private String bmiCategory(float bmi) {
        if (bmi < 18.5f) return "Underweight";
        if (bmi < 25f)   return "Normal";
        if (bmi < 30f)   return "Overweight";
        return "Obese";
    }

    /** Makes the input bold when it contains text (keeps hint normal). */
    private void attachBoldOnInput(TextInputEditText et) {
        if (et == null) return;
        et.setTypeface(et.getTypeface(),
                (et.getText() != null && et.getText().length() > 0) ? Typeface.BOLD : Typeface.NORMAL);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                et.setTypeface(et.getTypeface(), s.length() > 0 ? Typeface.BOLD : Typeface.NORMAL);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}
