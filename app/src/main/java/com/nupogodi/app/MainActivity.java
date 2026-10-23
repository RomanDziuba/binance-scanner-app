package com.nupogodi.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
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

        // Главный контейнер
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Верхняя панель с информацией (Счет и Жизни)
        LinearLayout topPanel = new LinearLayout(this);
        topPanel.setOrientation(LinearLayout.HORIZONTAL);
        topPanel.setGravity(Gravity.CENTER);
        topPanel.setPadding(20, 20, 20, 10);
        topPanel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        final TextView scoreText = new TextView(this);
        scoreText.setText("Счет: 0");
        scoreText.setTextColor(Color.parseColor("#10B981"));
        scoreText.setTextSize(18f);
        scoreText.setTypeface(Typeface.MONOSPACE);
        scoreText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        scoreText.setGravity(Gravity.START);

        final TextView livesText = new TextView(this);
        livesText.setText("❤️❤️❤️");
        livesText.setTextSize(18f);
        livesText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        livesText.setGravity(Gravity.END);

        topPanel.addView(scoreText);
        topPanel.addView(livesText);
        rootLayout.addView(topPanel);

        // Игровое поле (Canvas View)
        gameView = new GameView(this, scoreText, livesText);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        gameView.setLayoutParams(gameParams);
        rootLayout.addView(gameView);

        // Панель управления (4 кнопки: TL, BL, TR, BR)
        LinearLayout controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.HORIZONTAL);
        controlPanel.setGravity(Gravity.CENTER);
        controlPanel.setPadding(10, 10, 10, 20);
        controlPanel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, 130, 1f
        );
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

    // --- ВНУТРЕННИЙ КЛАСС ИГРОВОГО ПОЛЯ ---
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
        private long eggSpawnInterval = 2500; // мс между яйцами

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
                if (gameThread != null) {
                    gameThread.join();
                }
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
            eggSpawnInterval = 2500;
            updateUI();
        }

        @Override
        public void run() {
            while (playing) {
                updateGame();
                handler.post(this::invalidate);
                try {
                    Thread.sleep(30); // ~33 FPS
                } catch (InterruptedException ignored) {}
            }
        }

        private void updateGame() {
            if (isGameOver) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEggTime > eggSpawnInterval) {
                int lane = random.nextInt(4); // 0, 1, 2, 3
                eggs.add(new Egg(lane));
                lastEggTime = currentTime;
                // Ускорение со временем
                if (eggSpawnInterval > 900) {
                    eggSpawnInterval -= 40;
                }
            }

            Iterator<Egg> iterator = eggs.iterator();
            while (iterator.hasNext()) {
                Egg egg = iterator.next();
                egg.progress += 0.015f + (score * 0.0005f); // скорость движения яйца по желобу

                if (egg.progress >= 1.0f) {
                    // Яйцо докатилось до конца желоба
                    if (egg.lane == basketPos) {
                        score += 10;
                        // Звуковой эффект можно добавить через SoundPool если нужно
                    } else {
                        lives--;
                        if (lives <= 0) {
                            isGameOver = true;
                        }
                    }
                    iterator.remove();
                    updateUI();
                }
            }
        }

        private void updateUI() {
            handler.post(() -> {
                scoreTextView.setText("Счет: " + score);
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

            // 1. Рисуем фоновые желоба (4 линии от куриц к центру)
            paint.setStrokeWidth(4);
            paint.setColor(Color.parseColor("#334155"));

            // Левый верхний желоб
            canvas.drawLine(width * 0.25f, height * 0.15f, width * 0.4f, height * 0.45f, paint);
            // Левый нижний желоб
            canvas.drawLine(width * 0.15f, height * 0.35f, width * 0.4f, height * 0.55f, paint);
            // Правый верхний желоб
            canvas.drawLine(width * 0.75f, height * 0.15f, width * 0.6f, height * 0.45f, paint);
            // Правый нижний желоб
            canvas.drawLine(width * 0.85f, height * 0.35f, width * 0.6f, height * 0.55f, paint);

            // 2. Рисуем курочек по углам верха
            paint.setTextSize(36f);
            canvas.drawText("🐔", width * 0.22f, height * 0.13f, paint);
            canvas.drawText("🐔", width * 0.12f, height * 0.33f, paint);
            canvas.drawText("🐔", width * 0.72f, height * 0.13f, paint);
            canvas.drawText("🐔", width * 0.82f, height * 0.33f, paint);

            // 3. Рисуем персонажа (Романа) в центре с корзиной в текущей позиции
            drawCharacter(canvas, width, height, basketPos);

            // 4. Рисуем катящиеся яйца
            for (Egg egg : eggs) {
                float startX = 0, startY = 0, endX = 0, endY = 0;
                if (egg.lane == 0) { // Верх-Лево
                    startX = width * 0.25f; startY = height * 0.15f;
                    endX = width * 0.4f; endY = height * 0.45f;
                } else if (egg.lane == 1) { // Низ-Лево
                    startX = width * 0.15f; startY = height * 0.35f;
                    endX = width * 0.4f; endY = height * 0.55f;
                } else if (egg.lane == 2) { // Верх-Право
                    startX = width * 0.75f; startY = height * 0.15f;
                    endX = width * 0.6f; endY = height * 0.45f;
                } else if (egg.lane == 3) { // Низ-Право
                    startX = width * 0.85f; startY = height * 0.35f;
                    endX = width * 0.6f; endY = height * 0.55f;
                }

                float currX = startX + (endX - startX) * egg.progress;
                float currY = startY + (endY - startY) * egg.progress;

                paint.setColor(Color.WHITE);
                canvas.drawCircle(currX, currY, 14f, paint);
                paint.setColor(Color.parseColor("#F59E0B"));
                canvas.drawCircle(currX, currY, 10f, paint);
            }

            // 5. Экран окончания игры
            if (isGameOver) {
                paint.setColor(Color.parseColor("#CC030712"));
                canvas.drawRect(0, 0, width, height, paint);

                paint.setColor(Color.parseColor("#EF4444"));
                paint.setTextSize(48f);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("ИГРА ОКОНЧЕНА", width / 2f, height / 2f - 40f, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(20f);
                canvas.drawText("Нажмите любую кнопку управления", width / 2f, height / 2f + 20f, paint);
                paint.setTextAlign(Paint.Align.LEFT); // сброс выравнивания
            }
        }

        private void drawCharacter(Canvas canvas, int width, int height, int pos) {
            float centerX = width / 2f;
            float centerY = height * 0.52f;

            // Тело и голова персонажа (Роман)
            paint.setColor(Color.parseColor("#38BDF8"));
            canvas.drawCircle(centerX, centerY - 20f, 30f, paint); // Голова
            paint.setColor(Color.parseColor("#F59E0B"));
            canvas.drawRect(centerX - 25f, centerY, centerX + 25f, centerY + 80f, paint); // Торс

            // Координаты корзины в зависимости от выбранной позиции
            float basketX = centerX;
            float basketY = centerY + 20f;

            if (pos == 0) { // Верх-Лево
                basketX = width * 0.4f - 20f;
                basketY = height * 0.45f;
            } else if (pos == 1) { // Низ-Лево
                basketX = width * 0.4f - 20f;
                basketY = height * 0.55f;
            } else if (pos == 2) { // Верх-Право
                basketX = width * 0.6f + 20f;
                basketY = height * 0.45f;
            } else if (pos == 3) { // Низ-Право
                basketX = width * 0.6f + 20f;
                basketY = height * 0.55f;
            }

            // Рисуем руки к корзине
            paint.setStrokeWidth(6);
            paint.setColor(Color.parseColor("#CBD5E1"));
            canvas.drawLine(centerX, centerY + 20f, basketX, basketY, paint);

            // Сама корзина для ловли яиц
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#D97706"));
            canvas.drawRect(basketX - 25f, basketY - 15f, basketX + 25f, basketY + 15f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    // Класс для представления яйца
    private static class Egg {
        int lane; // 0, 1, 2, 3
        float progress = 0.0f; // от 0.0 до 1.0

        public Egg(int lane) {
            this.lane = lane;
        }
    }
}