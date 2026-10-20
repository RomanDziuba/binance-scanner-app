package com.roma.nativeapp;

import android.app.Activity;
import android.content.Context;
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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magnetometer, accelerometer;
    private float[] lastGravity = new float[3];
    private float[] lastGeomagnetic = new float[3];
    private boolean haveGravity = false, haveGeomagnetic = false;
    private float currentDegree = 0f;

    private ImageView compassArrow;
    private TextView degreeTextView;
    private Button btnFlash;
    private boolean flashOn = false;
    private Camera camera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный фон (глубокий космос / премиум)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(30, 30, 30, 30);

        // Карточка-контейнер с закругленными краями и неоновой рамкой
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(40, 50, 40, 50);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(40f);
        cardBg.setStroke(3, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        // Гравировка ДРВ (Золотой эксклюзив)
        TextView engravingView = new TextView(this);
        engravingView.setText("✦ DRV ✦");
        engravingView.setTextColor(Color.parseColor("#F59E0B"));
        engravingView.setTextSize(26f);
        engravingView.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        engravingView.setGravity(Gravity.CENTER);
        engravingView.setPadding(0, 0, 0, 10);

        // Заголовок
        TextView titleView = new TextView(this);
        titleView.setText("🧭 PREMIUM COMPASS");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(18f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 30);

        // Визуальная стрелка компаса (текстовая кастомная иконка высокой четкости)
        compassArrow = new ImageView(this);
        compassArrow.setImageResource(android.R.drawable.ic_menu_compass);
        // Альтернативный вариант текстового указателя, если иконка не нужна, но здесь задействован вращающийся элемент
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(220, 220);
        arrowParams.gravity = Gravity.CENTER;
        compassArrow.setLayoutParams(arrowParams);
        compassArrow.setColorFilter(Color.parseColor("#10B981"));

        // Текст с точным градусом
        degreeTextView = new TextView(this);
        degreeTextView.setText("0° N");
        degreeTextView.setTextColor(Color.parseColor("#F8FAFC"));
        degreeTextView.setTextSize(36f);
        degreeTextView.setTypeface(Typeface.MONOSPACE);
        degreeTextView.setGravity(Gravity.CENTER);
        degreeTextView.setPadding(0, 30, 0, 30);

        // Кнопка фонарика
        btnFlash = new Button(this);
        btnFlash.setText("🔦 Фонарик ВЫКЛ");
        btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setPadding(40, 25, 40, 25);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#D97706"));
        btnBg.setCornerRadius(25f);
        btnFlash.setBackground(btnBg);

        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        // Сборка интерфейса
        card.addView(engravingView);
        card.addView(titleView);
        card.addView(compassArrow);
        card.addView(degreeTextView);
        card.addView(btnFlash);

        rootLayout.addView(card);
        setContentView(rootLayout);

        // Инициализация датчиков телефона
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        releaseCamera();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastGravity, 0, event.values.length);
            haveGravity = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastGeomagnetic, 0, event.values.length);
            haveGeomagnetic = true;
        }

        if (haveGravity && haveGeomagnetic) {
            float[] rMatrix = new float[9];
            float[] orientation = new float[3];
            if (SensorManager.getRotationMatrix(rMatrix, null, lastGravity, lastGeomagnetic)) {
                SensorManager.getOrientation(rMatrix, orientation);
                float azimuthInRadians = orientation[0];
                float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
                azimuthInDegrees = (azimuthInDegrees + 360) % 360;

                // Плавная анимация поворота стрелки компаса
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

                // Определение направления света (N, E, S, W)
                String direction = "N";
                if (azimuthInDegrees >= 22.5 && azimuthInDegrees < 67.5) direction = "NE";
                else if (azimuthInDegrees >= 67.5 && azimuthInDegrees < 112.5) direction = "E";
                else if (azimuthInDegrees >= 112.5 && azimuthInDegrees < 157.5) direction = "SE";
                else if (azimuthInDegrees >= 157.5 && azimuthInDegrees < 202.5) direction = "S";
                else if (azimuthInDegrees >= 202.5 && azimuthInDegrees < 247.5) direction = "SW";
                else if (azimuthInDegrees >= 247.5 && azimuthInDegrees < 292.5) direction = "W";
                else if (azimuthInDegrees >= 292.5 && azimuthInDegrees < 337.5) direction = "NW";

                degreeTextView.setText(String.format("%.0f° %s", azimuthInDegrees, direction));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void toggleFlashlight() {
        try {
            if (!flashOn) {
                camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
                camera.startPreview();
                flashOn = true;
                btnFlash.setText("🔦 Фонарик ВКЛ");
                btnFlash.setBackgroundColor(Color.parseColor("#10B981"));
            } else {
                releaseCamera();
                flashOn = false;
                btnFlash.setText("🔦 Фонарик ВЫКЛ");
                btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка фонарика (вспышка занята или отсутствует)", Toast.LENGTH_SHORT).show();
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