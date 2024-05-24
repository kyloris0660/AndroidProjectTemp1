package com.example.steptracking;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private TextView stepCountText;
    private TextView debugInfoText;
    private TextView sensorDataText;
    private int stepCount = 0;
    private static final float STEP_THRESHOLD = 2.0f;
    private static final int MIN_TIME_BETWEEN_STEPS_MS = 300;
    private float[] gravity = new float[3];
    private long lastStepTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepCountText = findViewById(R.id.stepCountText);
        debugInfoText = findViewById(R.id.debugInfoText);
        sensorDataText = findViewById(R.id.sensorDataText);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor == null) {
                debugInfoText.setText("No Accelerometer Sensor found!");
            } else {
                debugInfoText.setText("Accelerometer Sensor found!");
            }
        } else {
            debugInfoText.setText("Sensor Manager is null!");
        }

        registerStepCounter();
    }

    private void registerStepCounter() {
        if (accelerometerSensor != null && sensorManager != null) {
            boolean registered = sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            debugInfoText.setText("Accelerometer registered: " + registered);
        } else {
            debugInfoText.setText("Failed to register accelerometer.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometerSensor != null && sensorManager != null) {
            boolean registered = sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            debugInfoText.setText("Accelerometer registered on resume: " + registered);
        } else {
            debugInfoText.setText("Accelerometer not available on resume.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            debugInfoText.setText("Accelerometer unregistered.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            float linear_acceleration_x = event.values[0] - gravity[0];
            float linear_acceleration_y = event.values[1] - gravity[1];
            float linear_acceleration_z = event.values[2] - gravity[2];

            // Calculate the magnitude of the acceleration vector.
            float magnitude = (float) Math.sqrt(linear_acceleration_x * linear_acceleration_x +
                    linear_acceleration_y * linear_acceleration_y +
                    linear_acceleration_z * linear_acceleration_z);

            sensorDataText.setText("X: " + linear_acceleration_x + " Y: " + linear_acceleration_y + " Z: " + linear_acceleration_z);

            long currentTime = System.currentTimeMillis();
            if (magnitude > STEP_THRESHOLD && (currentTime - lastStepTime) > MIN_TIME_BETWEEN_STEPS_MS) {
                stepCount++;
                stepCountText.setText("Steps: " + stepCount);
                lastStepTime = currentTime;
            }

            debugInfoText.setText("Magnitude: " + magnitude + "\nLast step time: " + lastStepTime);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        debugInfoText.setText("Sensor accuracy changed: " + accuracy);
    }
}
