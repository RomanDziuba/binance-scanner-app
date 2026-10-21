package com.kompas.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.*;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private float currentDegree = 0f;

    private ImageView compassArrow;
    private TextView degreeTextView;
    private TextView directionTextView;
    private Button btnFlash;
    
    private boolean flashOn = false;
    private Camera camera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация датчиков
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        // Главный контейнер (Космический фон)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(30, 30, 30, 30);

        // Премиальная карточка компаса
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(40, 40, 40, 40);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(40f);
        cardBg.setStroke(3, Color.parseColor("#38BDF8"));
        cardLayout.setBackground(cardBg);

        // Гравировка ДРВ (Стильный золотой акцент)
        TextView drvView = new TextView(this);
        drvView.setText("✦ Д Р В ✦");
        drvView.setTextColor(Color.parseColor("#F59E0B"));
        drvView.setTextSize(26f);
        drvView.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        drvView.setGravity(Gravity.CENTER);
        drvView.setPadding(0, 0, 0, 10);

        // Подзаголовок
        TextView titleView = new TextView(this);
        titleView.setText("🧭 SMART COMPASS");
        titleView.setTextColor(Color.parseColor("#94A3B8"));
        titleView.setTextSize(14f);
        titleView.setTypeface(Typeface.MONOSPACE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 30);

        // Текстовое поле градусов
        degreeTextView = new TextView(this);
        degreeTextView.setText("0° С");
        degreeTextView.setTextColor(Color.parseColor("#10B981"));
        degreeTextView.setTextSize(36f);
        degreeTextView.setTypeface(Typeface.DEFAULT_BOLD);
        degreeTextView.setGravity(Gravity.CENTER);

        // Направление (Север, Юг и т.д.)
        directionTextView = new TextView(this);
        directionTextView.setText("СЕВЕР");
        directionTextView.setTextColor(Color.parseColor("#38BDF8"));
        directionTextView.setTextSize(18f);
        directionTextView.setGravity(Gravity.CENTER);
        directionTextView.setPadding(0, 0, 0, 20);

        // Визуальная стрелка компаса (текстовая / индикаторная)
        compassArrow = new ImageView(this);
        compassArrow.setImageResource(android.R.drawable.ic_menu_compass);
        // Делаем стрелку крупной и яркой с помощью фильтра или текста
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(220, 220);
        arrowParams.setMargins(0, 20, 0, 30);
        compassArrow.setLayoutParams(arrowParams);

        // Кнопка Фонарика
        btnFlash = new Button(this);
        btnFlash.setText("🔦 ВКЛЮЧИТЬ ФОНАРИК");
        btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setTextSize(16f);
        btnFlash.setTypeface(Typeface.DEFAULT_BOLD);
        btnFlash.setPadding(40, 25, 40, 25);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#B45309"));
        btnBg.setCornerRadius(24f);
        btnFlash.setBackground(btnBg);

        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        // Сборка интерфейса
        cardLayout.addView(drvView);
        cardLayout.addView(titleView);
        cardLayout.addView(degreeTextView);
        cardLayout.addView(directionTextView);
        cardLayout.addView(compassArrow);
        cardLayout.addView(btnFlash);

        rootLayout.addView(cardLayout);
        setContentView(rootLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        releaseCamera();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            float[] rMatrix = new float[9];
            float[] orientation = new float[3];
            
            boolean success = SensorManager.getRotationMatrix(rMatrix, null, lastAccelerometer, lastMagnetometer);
            if (success) {
                SensorManager.getOrientation(rMatrix, orientation);
                float azimuthInRadians = orientation[0];
                float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
                azimuthInDegrees = (azimuthInDegrees + 360) % 360;

                // Плавный поворот стрелки компаса
                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -azimuthInDegrees,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                ra.setDuration(250);
                ra.setFillAfter(true);
                compassArrow.startAnimation(ra);
                currentDegree = -azimuthInDegrees;

                // Обновление текста градусов и сторон света
                int degInt = Math.round(azimuthInDegrees);
                degreeTextView.setText(degInt + "°");
                directionTextView.setText(getDirectionString(degInt));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private String getDirectionString(int deg) {
        if (deg >= 338 || deg < 23) return "СЕВЕР (N)";
        if (deg >= 23 && deg < 68) return "СЕВЕРО-ВОСТОК (NE)";
        if (deg >= 68 && deg < 113) return "ВОСТОК (E)";
        if (deg >= 113 && deg < 158) return "ЮГО-ВОСТОК (SE)";
        if (deg >= 158 && deg < 203) return "ЮГ (S)";
        if (deg >= 203 && deg < 248) return "ЮГО-ЗАПАД (SW)";
        if (deg >= 248 && deg < 293) return "ЗАПАД (W)";
        if (deg >= 293 && deg < 338) return "СЕВЕРО-ЗАПАД (NW)";
        return "СЕВЕР (N)";
    }

    private void toggleFlashlight() {
        try {
            if (!flashOn) {
                camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
                camera.startPreview();
                flashOn = true;
                btnFlash.setText("🔦 ФОНАРИК ВКЛЮЧЕН");
                btnFlash.setBackgroundColor(Color.parseColor("#10B981"));
            } else {
                releaseCamera();
                flashOn = false;
                btnFlash.setText("🔦 ВКЛЮЧИТЬ ФОНАРИК");
                btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка фонарика (нет вспышки)", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(p);
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {}
            camera = null;
        }
    }
}