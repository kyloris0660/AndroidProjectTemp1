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
    private static final float STEP_THRESHOLD = 10f;
    private float lastX, lastY, lastZ;
    private long lastTime;

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
        debugInfoText.setText("Sensor event triggered.");

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - lastTime;

            sensorDataText.setText("X: " + event.values[0] + " Y: " + event.values[1] + " Z: " + event.values[2]);

            if (timeDifference > 100) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float deltaX = x - lastX;
                float deltaY = y - lastY;
                float deltaZ = z - lastZ;

                float magnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                debugInfoText.append("\nMagnitude: " + magnitude);

                if (magnitude > STEP_THRESHOLD) {
                    stepCount++;
                    stepCountText.setText("Steps: " + stepCount);
                }

                lastX = x;
                lastY = y;
                lastZ = z;
                lastTime = currentTime;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        debugInfoText.setText("Sensor accuracy changed: " + accuracy);
    }
}
