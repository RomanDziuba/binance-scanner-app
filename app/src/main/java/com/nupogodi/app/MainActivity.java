package com.nupogodi.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Верхняя панель (Счет и Жизни)
        LinearLayout topPanel = new LinearLayout(this);
        topPanel.setOrientation(LinearLayout.HORIZONTAL);
        topPanel.setGravity(Gravity.CENTER);
        topPanel.setPadding(20, 20, 20, 10);
        topPanel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        final TextView scoreText = new TextView(this);
        scoreText.setText("СЧЕТ: 0");
        scoreText.setTextColor(Color.parseColor("#10B981"));
        scoreText.setTextSize(18f);
        scoreText.setTypeface(Typeface.MONOSPACE);
        scoreText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView livesText = new TextView(this);
        livesText.setText("❤️❤️❤️");
        livesText.setTextSize(18f);
        livesText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        livesText.setGravity(Gravity.END);

        topPanel.addView(scoreText);
        topPanel.addView(livesText);
        rootLayout.addView(topPanel);

        // Игровой холст
        gameView = new GameView(this, scoreText, livesText);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        gameView.setLayoutParams(gameParams);
        rootLayout.addView(gameView);

        // Панель управления (4 позиции)
        LinearLayout controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.HORIZONTAL);
        controlPanel.setGravity(Gravity.CENTER);
        controlPanel.setPadding(10, 10, 10, 20);

        Button btnTL = createControlBtn("↖ Верх Л");
        Button btnBL = createControlBtn("↙ Низ Л");
        Button btnTR = createControlBtn("↗ Верх П");
        Button btnBR = createControlBtn("↘ Низ П");

        btnTL.setOnClickListener(v -> gameView.setBasketPosition(0));
        btnBL.setOnClickListener(v -> gameView.setBasketPosition(1));
        btnTR.setOnClickListener(v -> gameView.setBasketPosition(2));
        btnBR.setOnClickListener(v -> gameView.setBasketPosition(3));

        controlPanel.addView(btnTL);
        controlPanel.addView(btnBL);
        controlPanel.addView(btnTR);
        controlPanel.addView(btnBR);
        rootLayout.addView(controlPanel);

        setContentView(rootLayout);
    }

    private Button createControlBtn(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor("#1E293B"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 130, 1f);
        params.setMargins(5, 0, 5, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeGame();
    }

    // Игровой класс с прорисовкой
    private static class GameView extends View implements Runnable {
        private Thread gameThread = null;
        private volatile boolean playing = false;
        private Paint paint;
        private Handler handler;

        private TextView scoreTextView;
        private TextView livesTextView;

        private int score = 0;
        private int lives = 3;
        private int basketPos = 1; // 0: Верх-Лево, 1: Низ-Лево, 2: Верх-Право, 3: Низ-Право

        private ArrayList<Egg> eggs;
        private Random random;
        private long lastEggTime = 0;
        private long eggSpawnInterval = 2300;

        private boolean isGameOver = false;

        public GameView(Context context, TextView scoreView, TextView livesView) {
            super(context);
            this.scoreTextView = scoreView;
            this.livesTextView = livesView;
            paint = new Paint();
            paint.setAntiAlias(true);
            handler = new Handler(Looper.getMainLooper());
            eggs = new ArrayList<>();
            random = new Random();
            setBackgroundColor(Color.parseColor("#0F172A"));
        }

        public void setBasketPosition(int pos) {
            if (!isGameOver) {
                basketPos = pos;
            } else {
                restartGame();
            }
            invalidate();
        }

        public void pauseGame() {
            playing = false;
            try {
                if (gameThread != null) gameThread.join();
            } catch (InterruptedException ignored) {}
        }

        public void resumeGame() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        private void restartGame() {
            score = 0;
            lives = 3;
            eggs.clear();
            isGameOver = false;
            eggSpawnInterval = 2300;
            updateUI();
        }

        @Override
        public void run() {
            while (playing) {
                updateGame();
                handler.post(this::invalidate);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {}
            }
        }

        private void updateGame() {
            if (isGameOver) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEggTime > eggSpawnInterval) {
                eggs.add(new Egg(random.nextInt(4)));
                lastEggTime = currentTime;
                if (eggSpawnInterval > 800) eggSpawnInterval -= 35;
            }

            Iterator<Egg> iterator = eggs.iterator();
            while (iterator.hasNext()) {
                Egg egg = iterator.next();
                egg.progress += 0.016f + (score * 0.0004f);

                if (egg.progress >= 1.0f) {
                    if (egg.lane == basketPos) {
                        score += 10;
                    } else {
                        lives--;
                        if (lives <= 0) isGameOver = true;
                    }
                    iterator.remove();
                    updateUI();
                }
            }
        }

        private void updateUI() {
            handler.post(() -> {
                scoreTextView.setText("СЧЕТ: " + score);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lives; i++) sb.append("❤️");
                for (int i = lives; i < 3; i++) sb.append("🖤");
                livesTextView.setText(sb.toString());
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            if (width == 0 || height == 0) return;

            // 1. Отрисовка желобов
            paint.setStrokeWidth(6);
            paint.setColor(Color.parseColor("#334155"));
            canvas.drawLine(width * 0.22f, height * 0.18f, width * 0.40f, height * 0.42f, paint); // ВЛ
            canvas.drawLine(width * 0.12f, height * 0.38f, width * 0.40f, height * 0.58f, paint); // НЛ
            canvas.drawLine(width * 0.78f, height * 0.18f, width * 0.60f, height * 0.42f, paint); // ВП
            canvas.drawLine(width * 0.88f, height * 0.38f, width * 0.60f, height * 0.58f, paint); // НП

            // 2. Отрисовка куриц
            paint.setTextSize(40f);
            canvas.drawText("🐔", width * 0.18f, height * 0.16f, paint);
            canvas.drawText("🐔", width * 0.08f, height * 0.36f, paint);
            canvas.drawText("🐔", width * 0.78f, height * 0.16f, paint);
            canvas.drawText("🐔", width * 0.88f, height * 0.36f, paint);

            // 3. Отрисовка персонажа Романа (с портретными деталями)
            drawRomanCharacter(canvas, width, height, basketPos);

            // 4. Отрисовка яиц
            for (Egg egg : eggs) {
                float startX = 0, startY = 0, endX = 0, endY = 0;
                if (egg.lane == 0) {
                    startX = width * 0.22f; startY = height * 0.18f; endX = width * 0.40f; endY = height * 0.42f;
                } else if (egg.lane == 1) {
                    startX = width * 0.12f; startY = height * 0.38f; endX = width * 0.40f; endY = height * 0.58f;
                } else if (egg.lane == 2) {
                    startX = width * 0.78f; startY = height * 0.18f; endX = width * 0.60f; endY = height * 0.42f;
                } else if (egg.lane == 3) {
                    startX = width * 0.88f; startY = height * 0.38f; endX = width * 0.60f; endY = height * 0.58f;
                }

                float currX = startX + (endX - startX) * egg.progress;
                float currY = startY + (endY - startY) * egg.progress;

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(currX, currY, 13f, paint);
                paint.setColor(Color.parseColor("#F59E0B"));
                canvas.drawCircle(currX, currY, 9f, paint);
            }

            // 5. Game Over экран
            if (isGameOver) {
                paint.setColor(Color.parseColor("#E6030712"));
                canvas.drawRect(0, 0, width, height, paint);

                paint.setColor(Color.parseColor("#EF4444"));
                paint.setTextSize(46f);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("ИГРА ОКОНЧЕНА", width / 2f, height / 2f - 30f, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(20f);
                canvas.drawText("Нажмите любую кнопку для старта", width / 2f, height / 2f + 30f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }

        // Отрисовка детализированного атлетичного персонажа Романа
        private void drawRomanCharacter(Canvas canvas, int width, int height, int pos) {
            float cx = width / 2f;
            float cy = height * 0.50f;

            boolean isLeft = (pos == 0 || pos == 1);
            boolean isTop = (pos == 0 || pos == 2);

            // Координаты корзины
            float basketX = isLeft ? (width * 0.40f - 15f) : (width * 0.60f + 15f);
            float basketY = isTop ? (height * 0.42f) : (height * 0.58f);

            paint.setStyle(Paint.Style.FILL);

            // 1. Шорты (темно-синие, спортивные с белыми шнурками)
            paint.setColor(Color.parseColor("#1E3A8A")); // Синий
            RectF shorts = new RectF(cx - 30f, cy + 50f, cx + 30f, cy + 120f);
            canvas.drawRoundRect(shorts, 10f, 10f, paint);

            // Белая полоска шнурка
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(4);
            canvas.drawLine(cx - 5f, cy + 55f, cx + 5f, cy + 55f, paint);
            canvas.drawLine(cx, cy + 55f, cx - 4f, cy + 70f, paint);
            canvas.drawLine(cx, cy + 55f, cx + 4f, cy + 70f, paint);

            // 2. Атлетичный торс (кожа / загар)
            paint.setColor(Color.parseColor("#E0A96D")); // Оттенок кожи
            Path torso = new Path();
            torso.moveTo(cx - 38f, cy - 20f); // Плечи
            torso.lineTo(cx + 38f, cy - 20f);
            torso.lineTo(cx + 26f, cy + 50f); // Талия
            torso.lineTo(cx - 26f, cy + 50f);
            torso.close();
            canvas.drawPath(torso, paint);

            // Рельеф пресса / груди (контурные линии)
            paint.setColor(Color.parseColor("#C68B45"));
            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(cx - 20f, cy, cx + 20f, cy, paint); // Грудь
            canvas.drawLine(cx, cy - 10f, cx, cy + 45f, paint); // Пресс (вертикаль)
            canvas.drawLine(cx - 15f, cy + 15f, cx + 15f, cy + 15f, paint);
            canvas.drawLine(cx - 12f, cy + 30f, cx + 12f, cy + 30f, paint);

            // 3. Голова (Форма + Стрижка под ноль / Лысина)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#E0A96D"));
            canvas.drawCircle(cx, cy - 50f, 24f, paint); // Голова

            // Лицо (глаза, легкая щетина)
            float faceLookX = isLeft ? -5f : 5f;
            paint.setColor(Color.parseColor("#27272A"));
            canvas.drawCircle(cx + faceLookX - 6f, cy - 54f, 3f, paint); // Левый глаз
            canvas.drawCircle(cx + faceLookX + 6f, cy - 54f, 3f, paint); // Правый глаз

            // Легкая аккуратная бородка / щетина
            paint.setColor(Color.parseColor("#B48148"));
            RectF beard = new RectF(cx - 14f, cy - 48f, cx + 14f, cy - 32f);
            canvas.drawArc(beard, 0, 180, true, paint);

            // 4. Руки и Ловля (вытянуты к корзине)
            paint.setColor(Color.parseColor("#E0A96D"));
            paint.setStyle(Paint.Style.FILL);

            // Плечи/Мускулы
            float shoulderX = isLeft ? (cx - 30f) : (cx + 30f);
            canvas.drawCircle(shoulderX, cy - 15f, 12f, paint); // Дельтаплан / Бицепс

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(12);
            paint.setStrokeCap(Paint.Cap.ROUND);

            // Рука, ведущая к корзине
            canvas.drawLine(shoulderX, cy - 15f, basketX, basketY, paint);

            // 5. Сама Корзина
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#D97706")); // Дерево
            RectF basketRect = new RectF(basketX - 28f, basketY - 12f, basketX + 28f, basketY + 12f);
            canvas.drawRoundRect(basketRect, 6f, 6f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.parseColor("#78350F"));
            paint.setStrokeWidth(3);
            canvas.drawRoundRect(basketRect, 6f, 6f, paint);
        }
    }

    private static class Egg {
        int lane;
        float progress = 0.0f;

        public Egg(int lane) {
            this.lane = lane;
        }
    }
}