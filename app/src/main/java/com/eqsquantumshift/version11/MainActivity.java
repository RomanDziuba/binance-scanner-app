package com.eqsquantumshift.version11;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private AnimatedCharacterView characterView;
    private TextView statusTextView, clockTextView;
    
    // Надежный Handler-цикл для постоянного покадрового дыхания персонажа (~30 FPS)
    private Handler animHandler = new Handler(Looper.getMainLooper());
    private Runnable animRunnable;
    private float globalTime = 0f;

    // Генератор системных звуков (чтобы не нужны были внешние аудиофайлы)
    private ToneGenerator toneGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализируем звуковой генератор
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Корневой макет
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#040711"));
        rootLayout.setPadding(20, 20, 20, 20);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // --- ШАПКА ---
        TextView titleView = new TextView(this);
        titleView.setText("🤖 ЖИВОЙ ПЕРСОНАЖ: ТЫКНИ В МЕНЯ!");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(14);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 15);
        rootLayout.addView(titleView);

        // --- КВАНТОВЫЕ ЧАСЫ (для атмосферы) ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(12);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(10, 8, 10, 8);
        
        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColors(new int[]{Color.parseColor("#064E3B"), Color.parseColor("#022C22")});
        clockBg.setCornerRadius(12f);
        clockBg.setStroke(2, Color.parseColor("#10B981"));
        clockTextView.setBackground(clockBg);
        
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 0, 0, 20);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);
        startClockAnimation();

        // --- АНИМИРОВАННЫЙ ПЕРСОНАЖ ---
        characterView = new AnimatedCharacterView(this);
        LinearLayout.LayoutParams charParams = new LinearLayout.LayoutParams(700, 700);
        charParams.setMargins(0, 40, 0, 40);
        characterView.setLayoutParams(charParams);
        
        // Обработка нажатия на персонажа
        characterView.setOnClickListener(v -> {
            characterView.triggerOuchReaction();
            playOuchSound();
        });
        
        rootLayout.addView(characterView);

        // --- ИНСТРУКЦИЯ СТАТУСА ---
        statusTextView = new TextView(this);
        statusTextView.setText("💬 «Привет! Я твой виртуальный друг. Нажми на меня!»");
        statusTextView.setTextColor(Color.parseColor("#F59E0B"));
        statusTextView.setTextSize(13);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(10, 15, 10, 15);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#1E1B4B"));
        statusBg.setCornerRadius(14f);
        statusBg.setStroke(2, Color.parseColor("#6366F1"));
        statusTextView.setBackground(statusBg);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusTextView.setLayoutParams(statusParams);
        rootLayout.addView(statusTextView);

        setContentView(rootLayout);

        // Запускаем глобальный игровой цикл анимации (~30 FPS)
        startGlobalAnimationLoop();
    }

    // --- ЗВУКОВОЙ ЭФФЕКТ "ОЙ!" ---
    private void playOuchSound() {
        if (toneGen != null) {
            // Воспроизводим короткий двухтональный писк-удивление (как "Ой!")
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        }
    }

    // --- ГЛОБАЛЬНЫЙ ЦИКЛ АНИМАЦИИ (HANDLER) ---
    private void startGlobalAnimationLoop() {
        animRunnable = new Runnable() {
            @Override
            public void run() {
                globalTime += 0.1f;
                if (characterView != null) {
                    characterView.updateBreathing(globalTime);
                }
                animHandler.postDelayed(this, 33); // ~30 кадров в секунду
            }
        };
        animHandler.post(animRunnable);
    }

    private void startClockAnimation() {
        Handler clockHandler = new Handler(Looper.getMainLooper());
        Runnable clockRunnable = new Runnable() {
            @Override
            public void run() {
                String timeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("💠 [ " + timeOnly + " ] 💠 АКТИВЕН");
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animHandler != null && animRunnable != null) {
            animHandler.removeCallbacks(animRunnable);
        }
        if (toneGen != null) {
            toneGen.release();
        }
    }

    // ==========================================
    // КАССТОМНЫЙ ВИД ПЕРСОНАЖА (РИСОВАНИЕ ХОЛСТА)
    // ==========================================
    private class AnimatedCharacterView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean isOuchState = false;
        private float breathOffset = 0f;
        private float jumpOffset = 0f;

        public AnimatedCharacterView(Context context) {
            super(context);
        }

        public void updateBreathing(float time) {
            // Эффект плавного покачивания/дыхания
            breathOffset = (float) Math.sin(time * 2.0f) * 8f;
            invalidate();
        }

        // Реакция на клик ("Ой!")
        public void triggerOuchReaction() {
            isOuchState = true;
            statusTextView.setText("💥 «Ой! Зачем ты толкаешься?! 😆»");
            statusTextView.setTextColor(Color.parseColor("#EF4444"));

            // Плавный прыжок вверх-вниз через ValueAnimator
            ValueAnimator jumpAnim = ValueAnimator.ofFloat(0f, -40f, 0f);
            jumpAnim.setDuration(300);
            jumpAnim.addUpdateListener(anim -> {
                jumpOffset = (float) anim.getAnimatedValue();
                invalidate();
            });
            jumpAnim.start();

            // Возвращаем обычное лицо через 1 секунду
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isOuchState = false;
                statusTextView.setText("💬 «Хе-хе, мне щекотно! 😊»");
                statusTextView.setTextColor(Color.parseColor("#34D399"));
            }, 1000);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f + breathOffset + jumpOffset;

            // 1. Неоновое свечение под персонажем
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(40, 56, 189, 248));
            canvas.drawCircle(cx, cy + 120, 160, paint);

            // 2. Тело / Костюм робота-друга
            paint.setColor(Color.parseColor("#1E293B"));
            canvas.drawRoundRect(new RectF(cx - 110, cy - 20, cx + 110, cy + 140), 30, 30, paint);
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.parseColor("#38BDF8"));
            canvas.drawRoundRect(new RectF(cx - 110, cy - 20, cx + 110, cy + 140), 30, 30, paint);

            // 3. Голова персонажа (круглый интерактивный шлем/лицо)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#0F172A"));
            canvas.drawCircle(cx, cy - 50, 110, paint);
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            paint.setColor(Color.parseColor("#818CF8"));
            canvas.drawCircle(cx, cy - 50, 110, paint);

            // 4. Глаза (моргающие / удивленные)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#34D399"));
            if (isOuchState) {
                // Крестики вместо глаз при клике (>_<)
                paint.setStrokeWidth(6f);
                canvas.drawLine(cx - 45, cy - 75, cx - 25, cy - 55, paint);
                canvas.drawLine(cx - 25, cy - 75, cx - 45, cy - 55, paint);
                canvas.drawLine(cx + 25, cy - 75, cx + 45, cy - 55, paint);
                canvas.drawLine(cx + 45, cy - 75, cx + 25, cy - 55, paint);
            } else {
                // Обычные добрые светящиеся глаза-кружочки
                canvas.drawCircle(cx - 35, cy - 65, 16, paint);
                canvas.drawCircle(cx + 35, cy - 65, 16, paint);
                // Блики в глазах
                paint.setColor(Color.WHITE);
                canvas.drawCircle(cx - 40, cy - 70, 5, paint);
                canvas.drawCircle(cx + 30, cy - 70, 5, paint);
            }

            // 5. Рот (открывается при нажатии в виде буквы "О" или улыбки)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#F43F5E"));
            if (isOuchState) {
                // Широко открытый рот при клике ("Ой!")
                canvas.drawOval(new RectF(cx - 20, cy - 35, cx + 20, cy + 5), paint);
            } else {
                // Веселая улыбка в спокойном состоянии
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5f);
                RectF smileRect = new RectF(cx - 30, cy - 45, cx + 30, cy - 10);
                canvas.drawArc(smileRect, 20, 140, false, paint);
            }
        }
    }
}