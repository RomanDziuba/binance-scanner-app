package com.kompas.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
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

    private ImageView compassDialView;
    private TextView degreeTextView;
    private TextView directionTextView;
    private Button btnFlash;
    
    private boolean flashOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация датчиков
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        // Инициализация современного менеджера камеры для фонарика
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception ignored) {}

        // Главный фон — глубокий космос / темный металл
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#050B14"));
        rootLayout.setPadding(20, 20, 20, 20);

        // Верхняя гравировка «Д Р В»
        TextView drvView = new TextView(this);
        drvView.setText("✦  Д  Р  В  ✦");
        drvView.setTextColor(Color.parseColor("#F59E0B"));
        drvView.setTextSize(22f);
        drvView.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        drvView.setGravity(Gravity.CENTER);
        drvView.setPadding(0, 0, 0, 15);

        // Текстовое поле градусов (крупное)
        degreeTextView = new TextView(this);
        degreeTextView.setText("0°");
        degreeTextView.setTextColor(Color.parseColor("#34D399"));
        degreeTextView.setTextSize(42f);
        degreeTextView.setTypeface(Typeface.DEFAULT_BOLD);
        degreeTextView.setGravity(Gravity.CENTER);

        // Направление (Север, Юг...)
        directionTextView = new TextView(this);
        directionTextView.setText("СЕВЕР");
        directionTextView.setTextColor(Color.parseColor("#38BDF8"));
        directionTextView.setTextSize(18f);
        directionTextView.setTypeface(Typeface.MONOSPACE);
        directionTextView.setGravity(Gravity.CENTER);
        directionTextView.setPadding(0, 0, 0, 20);

        // === ОГРОМНЫЙ КРУГЛЫЙ КОМПАС (Генерируется программно в высоком качестве) ===
        FrameLayout compassContainer = new FrameLayout(this);
        int compassSize = 850; // Большой размер компаса на экране
        FrameLayout.LayoutParams compParams = new FrameLayout.LayoutParams(compassSize, compassSize);
        compParams.gravity = Gravity.CENTER;
        compassContainer.setLayoutParams(compParams);

        // Создаем циферблат с делениями и буквами N, E, S, W
        Bitmap dialBitmap = createCompassDialBitmap(compassSize);
        ImageView dialImageView = new ImageView(this);
        dialImageView.setImageBitmap(dialBitmap);
        dialImageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Создаем огромную красивую вращающуюся стрелку (Красный север / Синий юг)
        Bitmap needleBitmap = createCompassNeedleBitmap(compassSize);
        compassDialView = new ImageView(this);
        compassDialView.setImageBitmap(needleBitmap);
        compassDialView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        compassContainer.addView(dialImageView);
        compassContainer.addView(compassDialView);

        // Кнопка Фонарика внизу
        btnFlash = new Button(this);
        btnFlash.setText("🔦 ФОНАРИК");
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setTextSize(16f);
        btnFlash.setTypeface(Typeface.DEFAULT_BOLD);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140);
        btnParams.setMargins(40, 30, 40, 10);
        btnFlash.setLayoutParams(btnParams);
        
        updateFlashButtonUI(false);

        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        // Сборка экрана
        rootLayout.addView(drvView);
        rootLayout.addView(degreeTextView);
        rootLayout.addView(directionTextView);
        rootLayout.addView(compassContainer);
        rootLayout.addView(btnFlash);

        setContentView(rootLayout);
    }

    // Рисуем шикарный циферблат с делениями и сторонами света
    private Bitmap createCompassDialBitmap(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int cx = size / 2;
        int cy = size / 2;
        int radius = size / 2 - 20;

        // Внешний золотой ободок хронометра
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setColor(Color.parseColor("#F59E0B")); // Золотой контур ДРВ
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStrokeWidth(3);
        paint.setColor(Color.parseColor("#334155"));
        canvas.drawCircle(cx, cy, radius - 30, paint);

        // Штрихи градусов и сторон света
        paint.setTextSize(36);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        
        for (int i = 0; i < 360; i += 10) {
            float angle = (float) Math.toRadians(i);
            float innerR = (i % 30 == 0) ? radius - 60 : radius - 45;
            float outerR = radius - 35;

            float startX = cx + (float) (innerR * Math.sin(angle));
            float startY = cy - (float) (innerR * Math.cos(angle));
            float endX = cx + (float) (outerR * Math.sin(angle));
            float endY = cy - (float) (outerR * Math.cos(angle));

            paint.setStrokeWidth((i % 30 == 0) ? 5 : 2);
            paint.setColor((i == 0) ? Color.parseColor("#EF4444") : Color.parseColor("#94A3B8"));
            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        // Буквы N, E, S, W
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        
        // Север (Красный)
        paint.setColor(Color.parseColor("#EF4444"));
        paint.setTextSize(48);
        canvas.drawText("N", cx, cy - radius + 90, paint);

        // Восток, Юг, Запад
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawText("E", cx + radius - 90, cy + 18, paint);
        canvas.drawText("S", cx, cy + radius - 50, paint);
        canvas.drawText("W", cx - radius + 90, cy + 18, paint);

        // Гравировка по центру циферблата
        paint.setTextSize(22);
        paint.setColor(Color.parseColor("#64748B"));
        canvas.drawText("CHRONOMETER", cx, cy - 60, paint);
        
        paint.setTextSize(26);
        paint.setColor(Color.parseColor("#F59E0B"));
        canvas.drawText("Д Р В", cx, cy + 70, paint);

        return bitmap;
    }

    // Рисуем большую, массивную двухцветную стрелку компаса
    private Bitmap createCompassNeedleBitmap(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int cx = size / 2;
        int cy = size / 2;
        int length = size / 2 - 110;

        Path northPath = new Path();
        northPath.moveTo(cx, cy - length);
        northPath.lineTo(cx - 22, cy);
        northPath.lineTo(cx + 22, cy);
        northPath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#EF4444")); // Северная стрелка — ярко-красная
        canvas.drawPath(northPath, paint);

        Path southPath = new Path();
        southPath.moveTo(cx, cy + length);
        southPath.lineTo(cx - 22, cy);
        southPath.lineTo(cx + 22, cy);
        southPath.close();

        paint.setColor(Color.parseColor("#38BDF8")); // Южная стрелка — синяя/неоновая
        canvas.drawPath(southPath, paint);

        // Центральный золотой крепеж стрелки
        paint.setColor(Color.parseColor("#F59E0B"));
        canvas.drawCircle(cx, cy, 18, paint);
        paint.setColor(Color.parseColor("#0F172A"));
        canvas.drawCircle(cx, cy, 8, paint);

        return bitmap;
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
        turnOffFlashlight();
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

                // Плавное вращение большой стрелки
                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -azimuthInDegrees,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                ra.setDuration(200);
                ra.setFillAfter(true);
                compassDialView.startAnimation(ra);
                currentDegree = -azimuthInDegrees;

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
        if (cameraManager == null || cameraId == null) {
            Toast.makeText(this, "Вспышка не обнаружена на устройстве", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            flashOn = !flashOn;
            cameraManager.setTorchMode(cameraId, flashOn);
            updateFlashButtonUI(flashOn);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Ошибка доступа к камере", Toast.LENGTH_SHORT).show();
            flashOn = false;
            updateFlashButtonUI(false);
        }
    }

    private void turnOffFlashlight() {
        if (cameraManager != null && cameraId != null && flashOn) {
            try {
                cameraManager.setTorchMode(cameraId, false);
                flashOn = false;
            } catch (Exception ignored) {}
        }
    }

    private void updateFlashButtonUI(boolean isOn) {
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(30f);
        if (isOn) {
            btnFlash.setText("🔦 ФОНАРИК ВКЛЮЧЕН");
            btnBg.setColor(Color.parseColor("#059669")); // Зеленый активный
        } else {
            btnFlash.setText("🔦 ВКЛЮЧИТЬ ФОНАРИК");
            btnBg.setColor(Color.parseColor("#D97706")); // Золотой/Оранжевый
        }
        btnFlash.setBackground(btnBg);
    }
}