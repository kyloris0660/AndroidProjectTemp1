package com.example.steptracking;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private TextView stepCountText;
    private TextView debugInfoText;
    private TextView sensorDataText;
    private TextView accelXText;
    private TextView accelYText;
    private TextView accelZText;
    private Button startButton;
    private CheckBox debugCheckBox;
    private TableLayout stepTable;
    private LineChart lineChart;
    private int stepCount = 0;
    private static final float STEP_THRESHOLD = 2.0f;
    private static final int MIN_TIME_BETWEEN_STEPS_MS = 300;
    private float[] gravity = new float[3];
    private long lastStepTime = 0L;
    private boolean isRecording = false;
    private Handler handler = new Handler();
    private List<Entry> entries = new ArrayList<>();
    private LineDataSet dataSet;
    private LineData lineData;
    private int intervalCount = 0;
    private int stepsInInterval = 0;
    private float maxYValue = 10f; // y轴最大值

    private EditText goalInput;
    private Button setGoalButton;
    private ProgressBar progressBar;
    private TextView goalText;
    private int stepGoal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepCountText = findViewById(R.id.stepCountText);
        debugInfoText = findViewById(R.id.debugInfoText);
        sensorDataText = findViewById(R.id.sensorDataText);
        accelXText = findViewById(R.id.accelXText);
        accelYText = findViewById(R.id.accelYText);
        accelZText = findViewById(R.id.accelZText);
        startButton = findViewById(R.id.startButton);
        debugCheckBox = findViewById(R.id.debugCheckBox);
        stepTable = findViewById(R.id.stepTable);
        lineChart = findViewById(R.id.lineChart);

        goalInput = findViewById(R.id.goalInput);
        setGoalButton = findViewById(R.id.setGoalButton);
        progressBar = findViewById(R.id.progressBar);
        goalText = findViewById(R.id.goalText);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometerSensor == null) {
            debugInfoText.setText("No Accelerometer Sensor found!");
        } else {
            debugInfoText.setText("Accelerometer Sensor found!");
        }

        registerStepCounter();
        setupChart();
        setupStartButton();
        setupGoalButton();
        setupDebugCheckBox();
    }

    private void setupGoalButton() {
        setGoalButton.setOnClickListener(v -> {
            String goalTextStr = goalInput.getText().toString();
            if (!goalTextStr.isEmpty()) {
                stepGoal = Integer.parseInt(goalTextStr);
                goalText.setText("Goal: " + stepGoal + " steps");
                progressBar.setProgress(0);
                progressBar.setMax(stepGoal);
            }
        });
    }

    private void setupDebugCheckBox() {
        debugCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int visibility = isChecked ? View.VISIBLE : View.GONE;
            debugInfoText.setVisibility(visibility);
            sensorDataText.setVisibility(visibility);
            accelXText.setVisibility(visibility);
            accelYText.setVisibility(visibility);
            accelZText.setVisibility(visibility);
        });
    }

    private void registerStepCounter() {
        if (accelerometerSensor != null) {
            debugInfoText.setText("Accelerometer registered.");
        } else {
            debugInfoText.setText("Failed to register accelerometer.");
        }
    }

    private void setupStartButton() {
        startButton.setOnClickListener(v -> {
            if (!isRecording) {
                isRecording = true;
                intervalCount = 0;
                stepsInInterval = 0;
                entries.clear();
                stepTable.removeViews(1, stepTable.getChildCount() - 1);
                startButton.setText("Stop Recording");
                handler.post(recordingRunnable);
            } else {
                isRecording = false;
                startButton.setText("Start Recording");
                handler.removeCallbacks(recordingRunnable);
            }
        });
    }

    private void setupChart() {
        dataSet = new LineDataSet(entries, "Steps per Interval");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(true);
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f); // Always start from 0
        leftAxis.setAxisMaximum(maxYValue); // Set initial max value

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        lineChart.invalidate();
    }

    private final Runnable recordingRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                intervalCount++;
                updateTableAndChart();
                stepsInInterval = 0;
                handler.postDelayed(this, 5000); // 每5秒执行一次
            }
        }
    };

    private void updateTableAndChart() {
        TableRow row = new TableRow(this);
        TextView intervalText = new TextView(this);
        intervalText.setText(String.valueOf(intervalCount));
        intervalText.setGravity(Gravity.CENTER);
        intervalText.setTextSize(16); // 调整字体大小
        row.addView(intervalText);

        TextView stepsText = new TextView(this);
        stepsText.setText(String.valueOf(stepsInInterval));
        stepsText.setGravity(Gravity.CENTER);
        stepsText.setTextSize(16); // 调整字体大小
        row.addView(stepsText);

        stepTable.addView(row);

        entries.add(new Entry(intervalCount, stepsInInterval));
        dataSet.notifyDataSetChanged();
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();

        adjustYAxis();
        lineChart.invalidate();
    }

    private void adjustYAxis() {
        float maxInDataSet = 0f;
        for (Entry entry : entries) {
            if (entry.getY() > maxInDataSet) {
                maxInDataSet = entry.getY();
            }
        }
        // 如果顶到表格外面就重新绘制y轴
        if (maxInDataSet > maxYValue) {
            maxYValue = maxInDataSet + 5;
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setAxisMaximum(maxYValue);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            float x = event.values[0] - gravity[0];
            float y = event.values[1] - gravity[1];
            float z = event.values[2] - gravity[2];

            accelXText.setText("Accel X: " + x);
            accelYText.setText("Accel Y: " + y);
            accelZText.setText("Accel Z: " + z);

            sensorDataText.setText("Sensor Data: X=" + x + " Y=" + y + " Z=" + z);

            if (isStepDetected(x, y, z)) {
                stepCount++;
                stepsInInterval++;
                stepCountText.setText("Steps: " + stepCount);

                if (stepGoal > 0) {
                    int progress = Math.min(stepCount, stepGoal);
                    progressBar.setProgress(progress);
                }
            }
        }
    }

    private boolean isStepDetected(float x, float y, float z) {
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
        long currentTime = System.currentTimeMillis();

        if (magnitude > STEP_THRESHOLD) {
            if (currentTime - lastStepTime > MIN_TIME_BETWEEN_STEPS_MS) {
                lastStepTime = currentTime;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerStepCounter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
