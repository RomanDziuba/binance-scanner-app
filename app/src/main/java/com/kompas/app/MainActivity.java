package com.kompas.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
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

        // Инициализация менеджера фонарика
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception ignored) {}

        // Главный фон — глубокий ретро-стиль
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#0A0908"));
        rootLayout.setPadding(20, 20, 20, 20);

        // Верхний заголовок
        TextView titleTop = new TextView(this);
        titleTop.setText("ANTIQUE CHRONOMETER");
        titleTop.setTextColor(Color.parseColor("#92704E"));
        titleTop.setTextSize(14f);
        titleTop.setTypeface(Typeface.MONOSPACE);
        titleTop.setGravity(Gravity.CENTER);
        titleTop.setPadding(0, 0, 0, 10);

        // Текстовое поле градусов
        degreeTextView = new TextView(this);
        degreeTextView.setText("0°");
        degreeTextView.setTextColor(Color.parseColor("#D4AF37"));
        degreeTextView.setTextSize(38f);
        degreeTextView.setTypeface(Typeface.DEFAULT_BOLD);
        degreeTextView.setGravity(Gravity.CENTER);

        // Направление (Север, Юг...)
        directionTextView = new TextView(this);
        directionTextView.setText("СЕВЕР");
        directionTextView.setTextColor(Color.parseColor("#8DA4C4"));
        directionTextView.setTextSize(16f);
        directionTextView.setTypeface(Typeface.MONOSPACE);
        directionTextView.setGravity(Gravity.CENTER);
        directionTextView.setPadding(0, 0, 0, 15);

        // === ВИНТАЖНЫЙ КОМПАС КАК НА СКРИНШОТЕ ===
        FrameLayout compassContainer = new FrameLayout(this);
        int compassSize = 880; // Большой размер
        FrameLayout.LayoutParams compParams = new FrameLayout.LayoutParams(compassSize, compassSize);
        compParams.gravity = Gravity.CENTER;
        compassContainer.setLayoutParams(compParams);

        // Циферблат в старинном стиле
        Bitmap dialBitmap = createVintageCompassDialBitmap(compassSize);
        ImageView dialImageView = new ImageView(this);
        dialImageView.setImageBitmap(dialBitmap);
        dialImageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Вращающаяся черная старинная стрелка
        Bitmap needleBitmap = createVintageNeedleBitmap(compassSize);
        compassDialView = new ImageView(this);
        compassDialView.setImageBitmap(needleBitmap);
        compassDialView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        compassContainer.addView(dialImageView);
        compassContainer.addView(compassDialView);

        // Кнопка Фонарика
        btnFlash = new Button(this);
        btnFlash.setText("🔦 ФОНАРИК");
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setTextSize(16f);
        btnFlash.setTypeface(Typeface.DEFAULT_BOLD);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 130);
        btnParams.setMargins(40, 25, 40, 10);
        btnFlash.setLayoutParams(btnParams);
        
        updateFlashButtonUI(false);

        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        // Сборка экрана
        rootLayout.addView(titleTop);
        rootLayout.addView(degreeTextView);
        rootLayout.addView(directionTextView);
        rootLayout.addView(compassContainer);
        rootLayout.addView(btnFlash);

        setContentView(rootLayout);
    }

    // Рисуем винтажный циферблат точь-в-точь как на фото + гравировка ДРВ прописью
    private Bitmap createVintageCompassDialBitmap(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int cx = size / 2;
        int cy = size / 2;
        int radius = size / 2 - 25;

        // 1. Металлический состаренный бронзовый ободок
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2A1E14"));
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(14);
        paint.setColor(Color.parseColor("#7A522C"));
        canvas.drawCircle(cx, cy, radius - 7, paint);

        // Зубчики по ободку (как на оригинале)
        paint.setStrokeWidth(3);
        paint.setColor(Color.parseColor("#4A321A"));
        for (int i = 0; i < 360; i += 3) {
            float angle = (float) Math.toRadians(i);
            float innerR = radius - 16;
            float outerR = radius - 2;
            canvas.drawLine(
                cx + (float)(innerR * Math.sin(angle)), cy - (float)(innerR * Math.cos(angle)),
                cx + (float)(outerR * Math.sin(angle)), cy - (float)(outerR * Math.cos(angle)), paint
            );
        }

        // 2. Внутреннее пергаментное поле циферблата (кремовый/слоновая кость)
        int innerRadius = radius - 22;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#F4EEDC"));
        canvas.drawCircle(cx, cy, innerRadius, paint);

        // Тонкие черные кольца внутри
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.parseColor("#2C221E"));
        canvas.drawCircle(cx, cy, innerRadius, paint);
        canvas.drawCircle(cx, cy, innerRadius - 35, paint);
        canvas.drawCircle(cx, cy, innerRadius - 130, paint);

        // 3. Градусная шкала и цифры (0, 20, 40... 340)
        paint.setTextSize(26);
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD));
        paint.setColor(Color.parseColor("#2C221E"));
        paint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < 360; i += 10) {
            float angle = (float) Math.toRadians(i);
            float tickInner = (i % 20 == 0) ? innerRadius - 22 : innerRadius - 12;
            float tickOuter = innerRadius - 4;

            canvas.drawLine(
                cx + (float)(tickInner * Math.sin(angle)), cy - (float)(tickInner * Math.cos(angle)),
                cx + (float)(tickOuter * Math.sin(angle)), cy - (float)(tickOuter * Math.cos(angle)), paint
            );

            // Цифры градусов каждые 20 градусов
            if (i > 0 && i % 20 == 0) {
                float textR = innerRadius - 55;
                float tx = cx + (float)(textR * Math.sin(angle));
                float ty = cy - (float)(textR * Math.cos(angle));
                
                canvas.save();
                canvas.translate(tx, ty);
                canvas.rotate(i);
                canvas.drawText(String.valueOf(i), 0, 8, paint);
                canvas.restore();
            }
        }

        // 4. Сложная винтажная многолучевая звезда (Компасная роза)
        drawVintageCompassRose(canvas, cx, cy, innerRadius - 135);

        // 5. Буквы сторон света (N, E, S, W, N.E, N.W, S.E, S.W)
        paint.setTextSize(42);
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        paint.setColor(Color.parseColor("#1A1412"));

        // Север сверху
        canvas.drawText("N", cx, cy - innerRadius + 95, paint);
        // Восток справа
        canvas.drawText("E", cx + innerRadius - 70, cy + 14, paint);
        // Юг снизу
        canvas.drawText("S", cx, cy + innerRadius - 55, paint);
        // Запад слева
        canvas.drawText("W", cx - innerRadius + 70, cy + 14, paint);

        // Промежуточные (N.E, N.W, S.E, S.W)
        paint.setTextSize(24);
        paint.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        canvas.drawText("N.E", cx + 180, cy - 180, paint);
        canvas.drawText("N.W", cx - 180, cy - 180, paint);
        canvas.drawText("S.E", cx + 180, cy + 200, paint);
        canvas.drawText("S.W", cx - 180, cy + 200, paint);

        // 6. ИЗЯЩНАЯ ПРОПИСНАЯ ГРАВИРОВКА «Д Р В» ПОД СТАРИНУ ВНУТРИ КОМПАСА
        paint.setTextSize(36);
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        paint.setColor(Color.parseColor("#8C6239")); // Благородный бронзово-золотой оттенок
        canvas.drawText("Д · Р · В", cx, cy + 75, paint);

        return bitmap;
    }

    // Отрисовка старинного узора звезды компаса
    private void drawVintageCompassRose(Canvas canvas, int cx, int cy, int maxR) {
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);

        // Рисуем лучи звезды
        for (int i = 0; i < 8; i++) {
            float angle = (float) Math.toRadians(i * 45);
            Path rayPath = new Path();
            
            float length = (i % 2 == 0) ? maxR : maxR * 0.7f;
            float width = (i % 2 == 0) ? 18f : 10f;

            float tipX = cx + (float)(length * Math.sin(angle));
            float tipY = cy - (float)(length * Math.cos(angle));

            float leftX = cx + (float)(width * Math.cos(angle));
            float leftY = cy + (float)(width * Math.sin(angle));
            
            float rightX = cx - (float)(width * Math.cos(angle));
            float rightY = cy - (float)(width * Math.sin(angle));

            rayPath.moveTo(tipX, tipY);
            rayPath.lineTo(leftX, leftY);
            rayPath.lineTo(cx, cy);
            rayPath.close();

            starPaint.setColor(i % 2 == 0 ? Color.parseColor("#1C1613") : Color.parseColor("#6E5B4D"));
            canvas.drawPath(rayPath, starPaint);

            Path rayPath2 = new Path();
            rayPath2.moveTo(tipX, tipY);
            rayPath2.lineTo(rightX, rightY);
            rayPath2.lineTo(cx, cy);
            rayPath2.close();
            
            starPaint.setColor(i % 2 == 0 ? Color.parseColor("#4A3B32") : Color.parseColor("#A89584"));
            canvas.drawPath(rayPath2, starPaint);
        }
    }

    // Рисуем оригинальную черную старинную стрелку с буквами N и S
    private Bitmap createVintageNeedleBitmap(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int cx = size / 2;
        int cy = size / 2;
        int length = size / 2 - 105;

        // Сегменты стрелки (черный антикварный металл)
        Path northNeedle = new Path();
        northNeedle.moveTo(cx, cy - length);
        northNeedle.lineTo(cx - 24, cy - 40);
        northNeedle.lineTo(cx - 10, cy);
        northNeedle.lineTo(cx + 10, cy);
        northNeedle.lineTo(cx + 24, cy - 40);
        northNeedle.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1A1412"));
        canvas.drawPath(northNeedle, paint);

        Path southNeedle = new Path();
        southNeedle.moveTo(cx, cy + length);
        southNeedle.lineTo(cx - 24, cy + 40);
        southNeedle.lineTo(cx - 10, cy);
        southNeedle.lineTo(cx + 10, cy);
        southNeedle.lineTo(cx + 24, cy + 40);
        southNeedle.close();

        paint.setColor(Color.parseColor("#2C221E"));
        canvas.drawPath(southNeedle, paint);

        // Буквы N и S на стрелке
        paint.setTextSize(20);
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD));
        paint.setColor(Color.parseColor("#D4AF37")); // Золотистые буквы на стрелке
        paint.setTextAlign(Paint.Align.CENTER);
        
        canvas.drawText("N", cx, cy - length + 55, paint);
        canvas.drawText("S", cx, cy + length - 40, paint);

        // Центральная латунная закрепка (как на фото с винтиком)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#8C6239"));
        canvas.drawCircle(cx, cy, 22, paint);
        paint.setColor(Color.parseColor("#D4AF37"));
        canvas.drawCircle(cx, cy, 14, paint);
        paint.setColor(Color.parseColor("#1A1412"));
        canvas.drawCircle(cx, cy, 5, paint);

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

                // Плавное вращение стрелки компаса
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
            btnBg.setColor(Color.parseColor("#059669")); // Зеленый
        } else {
            btnFlash.setText("🔦 ВКЛЮЧИТЬ ФОНАРИК");
            btnBg.setColor(Color.parseColor("#7A522C")); // Бронзово-коричневый винтаж
        }
        btnFlash.setBackground(btnBg);
    }
}