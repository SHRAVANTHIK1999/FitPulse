package com.example.fitpulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * StatsActivity
 * - Loads step history from Room and renders a weekly bar chart.
 * - Lets user switch between current and previous week.
 * - Shows quick “Today / Yesterday / 2 days ago” cards.
 */
public class StatsActivity extends AppCompatActivity {

    private BarChart barChart;
    private LinearLayout stepHistoryContainer;
    private Spinner spinnerWeek;
    private ImageView btnBackHome;

    private AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private static final DateTimeFormatter DB_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    private boolean isCurrentWeek = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Bind views
        barChart = findViewById(R.id.bar_chart);
        stepHistoryContainer = findViewById(R.id.step_history_container);
        spinnerWeek = findViewById(R.id.spinner_week);
        btnBackHome = findViewById(R.id.btn_back_home);

        db = AppDatabase.getInstance(getApplicationContext());

        // Week selector: 0 = current, 1 = previous
        spinnerWeek.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                isCurrentWeek = (position == 0);
                if (isCurrentWeek) {
                    loadAndRenderWeek(LocalDate.now());
                } else {
                    loadAndRenderWeek(LocalDate.now().minusDays(7));
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Back to home
        btnBackHome.setOnClickListener(v -> {
            startActivity(new Intent(StatsActivity.this, MainActivity.class));
            finish();
        });

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_stats);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); return true; }
            if (id == R.id.nav_stats) { return true; } // already here
            if (id == R.id.nav_monitor) { startActivity(new Intent(this, SensorMonitorActivity.class)); return true; }
            if (id == R.id.nav_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
            return false;
        });

        // Initial load: current week
        loadAndRenderWeek(LocalDate.now());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Keep step counter active while viewing stats
        StepCounterManager.get(this).start();
    }

    @Override
    protected void onStop() {
        StepCounterManager.get(this).stop();
        super.onStop();
    }

    /**
     * Query DB on a background thread and render the selected week.
     * @param anyDateInTargetWeek any date within the week to render
     */
    private void loadAndRenderWeek(LocalDate anyDateInTargetWeek) {
        io.execute(() -> {
            LocalDate weekStart = anyDateInTargetWeek.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd   = weekStart.plusDays(6);

            // Load all steps once; map by date for quick lookup
            List<StepEntry> allSteps = db.stepDao().getAllSteps();
            Map<String, Integer> byDate = new HashMap<>();
            for (StepEntry e : allSteps) byDate.put(e.date, e.steps);

            // Build chart entries and x-axis labels (Mon..Sun)
            List<BarEntry> barEntries = new ArrayList<>(7);
            List<String> xLabels = new ArrayList<>(7);
            int x = 0;
            for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                int steps = byDate.getOrDefault(d.format(DB_FMT), 0);
                barEntries.add(new BarEntry(x, steps));
                xLabels.add(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
                x++;
            }

            runOnUiThread(() -> {
                setupChart(barEntries, xLabels);
                populateCardsTodayYesterday(byDate);
            });
        });
    }

    /** Configure MPAndroidChart with entries and labels. */
    private void setupChart(List<BarEntry> entries, List<String> xLabels) {
        BarDataSet dataSet = new BarDataSet(entries, "Steps");
        dataSet.setValueTextSize(10f);
        dataSet.setColor(getResources().getColor(R.color.purple_500, getTheme()));
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black, getTheme()));

        // Hide value labels for zero bars
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                float v = barEntry.getY();
                return (v <= 0f) ? "" : String.format(Locale.getDefault(), "%,.0f", v);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChart.setData(data);

        // X axis labels (Mon..Sun)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

        // Y axes & legend
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setTextColor(getResources().getColor(android.R.color.black, getTheme()));

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

        // No description text
        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);

        barChart.setFitBars(true);
        barChart.animateY(800);
        barChart.invalidate();
    }

    /** Fill the small cards for Today / Yesterday / Two days ago. */
    private void populateCardsTodayYesterday(Map<String, Integer> byDate) {
        LinearLayout stepHistoryContainer = findViewById(R.id.step_history_container);
        stepHistoryContainer.removeAllViews();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        int todaySteps = byDate.getOrDefault(today.format(DB_FMT), 0);
        int yestSteps  = byDate.getOrDefault(yesterday.format(DB_FMT), 0);
        int twoDaysAgoSteps = byDate.getOrDefault(twoDaysAgo.format(DB_FMT), 0);

        addStepRow(stepHistoryContainer, "Today", todaySteps);
        addStepRow(stepHistoryContainer, "Yesterday", yestSteps);
        addStepRow(stepHistoryContainer,
                twoDaysAgo.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                twoDaysAgoSteps);
    }

    /** Inflate a row layout and append it to the history container. */
    private void addStepRow(LinearLayout container, String label, int steps) {
        View row = getLayoutInflater().inflate(R.layout.item_step_row, container, false);
        TextView dayLabel  = row.findViewById(R.id.label_text);
        TextView stepsText = row.findViewById(R.id.steps_text);

        dayLabel.setText(label);
        stepsText.setText(String.format(Locale.getDefault(), "%,d steps", steps));

        container.addView(row);
    }
}
