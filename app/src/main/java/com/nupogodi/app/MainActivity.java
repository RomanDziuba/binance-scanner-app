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

        // Главный контейнер в стиле корпуса консоли "Электроника"
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#94A3B8")); // Цвет серебристого корпуса
        rootLayout.setPadding(15, 15, 15, 15);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Верхняя надпись "ЭЛЕКТРОНИКА" / Логотип
        TextView brandView = new TextView(this);
        brandView.setText("•ИМ•  Э Л Е К Т Р О Н И К А");
        brandView.setTextColor(Color.parseColor("#0F172A"));
        brandView.setTextSize(14f);
        brandView.setTypeface(Typeface.DEFAULT_BOLD);
        brandView.setGravity(Gravity.CENTER);
        brandView.setPadding(0, 5, 0, 10);
        rootLayout.addView(brandView);

        // Игровой ЖК-экран с характерным болотно-зеленым фоном
        gameView = new GameView(this);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        gameView.setLayoutParams(gameParams);
        rootLayout.addView(gameView);

        // Панель управления (4 кнопки по углам в стиле консоли)
        LinearLayout controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.HORIZONTAL);
        controlPanel.setGravity(Gravity.CENTER);
        controlPanel.setPadding(5, 15, 5, 10);
        controlPanel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button btnTL = createConsoleButton("↖ ВЕРХ ЛЕВ");
        Button btnBL = createConsoleButton("↙ НИЗ ЛЕВ");
        Button btnTR = createConsoleButton("↗ ВЕРХ ПРАВ");
        Button btnBR = createConsoleButton("↘ НИЗ ПРАВ");

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

    private Button createConsoleButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#0F172A"));
        btn.setTextSize(10f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 110, 1f);
        params.setMargins(4, 0, 4, 0);
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

    // --- ИГРОВОЙ ЭКРАН И ЛОГИКА ---
    private static class GameView extends View implements Runnable {
        private Thread gameThread = null;
        private volatile boolean playing = false;
        private Paint paint;
        private Handler handler;

        private int score = 0;
        private int lives = 3;
        private int basketPos = 1; // 0: ВЛ, 1: НЛ, 2: ВП, 3: НП

        private ArrayList<Egg> eggs;
        private Random random;
        private long lastEggTime = 0;
        private long eggSpawnInterval = 2400;
        private boolean isGameOver = false;

        public GameView(Context context) {
            super(context);
            paint = new Paint();
            paint.setAntiAlias(true);
            handler = new Handler(Looper.getMainLooper());
            eggs = new ArrayList<>();
            random = new Random();
            setBackgroundColor(Color.parseColor("#C2CDB5")); // Классический LCD цвет фона
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
            eggSpawnInterval = 2400;
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
                if (eggSpawnInterval > 850) eggSpawnInterval -= 40;
            }

            Iterator<Egg> iterator = eggs.iterator();
            while (iterator.hasNext()) {
                Egg egg = iterator.next();
                egg.progress += 0.015f + (score * 0.0004f);

                if (egg.progress >= 1.0f) {
                    if (egg.lane == basketPos) {
                        score += 1;
                    } else {
                        lives--;
                        if (lives <= 0) isGameOver = true;
                    }
                    iterator.remove();
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            if (width == 0 || height == 0) return;

            // Стилизация под ЖК-экран (темно-зеленые элементы интерфейса)
            paint.setColor(Color.parseColor("#2C3E28"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawRect(10, 10, width - 10, height - 10, paint);

            // 1. Рисуем 4 желоба (как в оригинале "Ну, погоди!")
            paint.setStrokeWidth(4);
            // Верх-Лево
            canvas.drawLine(width * 0.22f, height * 0.20f, width * 0.42f, height * 0.44f, paint);
            // Низ-Лево
            canvas.drawLine(width * 0.12f, height * 0.40f, width * 0.42f, height * 0.60f, paint);
            // Верх-Право
            canvas.drawLine(width * 0.78f, height * 0.20f, width * 0.58f, height * 0.44f, paint);
            // Низ-Право
            canvas.drawLine(width * 0.88f, height * 0.40f, width * 0.58f, height * 0.60f, paint);

            // 2. Домики и Курочки на верхушках желобов
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(32f);
            canvas.drawText("🐔", width * 0.18f, height * 0.18f, paint);
            canvas.drawText("🐔", width * 0.08f, height * 0.38f, paint);
            canvas.drawText("🐔", width * 0.78f, height * 0.18f, paint);
            canvas.drawText("🐔", width * 0.88f, height * 0.38f, paint);

            // 3. Табло счета (вверху по центру, в стиле ЖК-цифр)
            paint.setTextSize(40f);
            paint.setTypeface(Typeface.MONOSPACE);
            String scoreStr = String.format("%02d", score);
            canvas.drawText(scoreStr, width / 2f - 25f, height * 0.15f, paint);

            // Индикатор жизней (шлемы/сердечки справа вверху)
            StringBuilder livesStr = new StringBuilder();
            for (int i = 0; i < lives; i++) livesStr.append("❤️");
            paint.setTextSize(16f);
            canvas.drawText(livesStr.toString(), width * 0.70f, height * 0.12f, paint);

            // 4. ОТРИСОВКА ГЛАВНОГО ГЕРОЯ (ТОЧНЫЙ ОБРАЗ С ФОТОГРАФИИ)
            drawRomanCharacter(canvas, width, height, basketPos);

            // 5. Катящиеся яйца
            for (Egg egg : eggs) {
                float startX = 0, startY = 0, endX = 0, endY = 0;
                if (egg.lane == 0) {
                    startX = width * 0.22f; startY = height * 0.20f; endX = width * 0.42f; endY = height * 0.44f;
                } else if (egg.lane == 1) {
                    startX = width * 0.12f; startY = height * 0.40f; endX = width * 0.42f; endY = height * 0.60f;
                } else if (egg.lane == 2) {
                    startX = width * 0.78f; startY = height * 0.20f; endX = width * 0.58f; endY = height * 0.44f;
                } else if (egg.lane == 3) {
                    startX = width * 0.88f; startY = height * 0.40f; endX = width * 0.58f; endY = height * 0.60f;
                }

                float currX = startX + (endX - startX) * egg.progress;
                float currY = startY + (endY - startY) * egg.progress;

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#1E293B"));
                canvas.drawCircle(currX, currY, 12f, paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(currX, currY, 6f, paint);
            }

            // Экран Game Over
            if (isGameOver) {
                paint.setColor(Color.parseColor("#CC2C3E28"));
                canvas.drawRect(0, 0, width, height, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(36f);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("ИГРА ОКОНЧЕНА", width / 2f, height / 2f - 20f, paint);

                paint.setTextSize(18f);
                canvas.drawText("Нажмите любую кнопку", width / 2f, height / 2f + 25f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }

        // Прорисовка персонажа по деталям вашей фотографии:
        // Атлетичное телосложение, лысина/стрижка под ноль, бородка, темные шорты со шнурком
        private void drawRomanCharacter(Canvas canvas, int width, int height, int pos) {
            float cx = width / 2f;
            float cy = height * 0.52f;

            boolean isLeft = (pos == 0 || pos == 1);
            boolean isTop = (pos == 0 || pos == 2);

            float basketX = isLeft ? (width * 0.42f - 15f) : (width * 0.58f + 15f);
            float basketY = isTop ? (height * 0.44f) : (height * 0.60f);

            paint.setStyle(Paint.Style.FILL);

            // 1. Темные спортивные шорты со шнурком (как на фото)
            paint.setColor(Color.parseColor("#1E3A8A"));
            RectF shorts = new RectF(cx - 28f, cy + 45f, cx + 28f, cy + 115f);
            canvas.drawRoundRect(shorts, 8f, 8f, paint);

            // Белый шнурок на шортах
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(3);
            canvas.drawLine(cx, cy + 48f, cx - 4f, cy + 62f, paint);
            canvas.drawLine(cx, cy + 48f, cx + 4f, cy + 62f, paint);

            // 2. Атлетичный торс (загорелая кожа, рельефная грудь и пресс)
            paint.setColor(Color.parseColor("#D49A5E"));
            Path torso = new Path();
            torso.moveTo(cx - 36f, cy - 18f);
            torso.lineTo(cx + 36f, cy - 18f);
            torso.lineTo(cx + 24f, cy + 45f);
            torso.lineTo(cx - 24f, cy + 45f);
            torso.close();
            canvas.drawPath(torso, paint);

            // Рельеф пресса
            paint.setColor(Color.parseColor("#B87D43"));
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(cx - 16f, cy + 5f, cx + 16f, cy + 5f, paint);
            canvas.drawLine(cx - 14f, cy + 20f, cx + 14f, cy + 20f, paint);
            canvas.drawLine(cx, cy - 8f, cx, cy + 40f, paint);

            // 3. Голова (лысина, правильная форма черепа, бородка)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#D49A5E"));
            canvas.drawCircle(cx, cy - 48f, 22f, paint);

            // Глаза и борода
            paint.setColor(Color.parseColor("#1E293B"));
            canvas.drawCircle(cx - 5f, cy - 52f, 2.5f, paint);
            canvas.drawCircle(cx + 5f, cy - 52f, 2.5f, paint);

            paint.setColor(Color.parseColor("#9A6330")); // Борода
            RectF beard = new RectF(cx - 12f, cy - 46f, cx + 12f, cy - 32f);
            canvas.drawArc(beard, 0, 180, true, paint);

            // 4. Руки, тянущиеся к корзине
            paint.setColor(Color.parseColor("#D49A5E"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setStrokeCap(Paint.Cap.ROUND);
            float shoulderX = isLeft ? (cx - 28f) : (cx + 28f);
            canvas.drawLine(shoulderX, cy - 12f, basketX, basketY, paint);

            // 5. Корзина для ловли яиц
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#B45309")); // Плеттеная корзина
            RectF basketRect = new RectF(basketX - 25f, basketY - 10f, basketX + 25f, basketY + 10f);
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