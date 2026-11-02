package com.roman.version04;

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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private GameView gameView;
    private TextView scoreTextView;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер (эмулятор экрана телефона)
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));

        // Игровой движок (Canvas View)
        gameView = new GameView(this);
        rootLayout.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Верхняя панель интерфейса (HUD в стиле Roblox)
        LinearLayout hudLayout = new LinearLayout(this);
        hudLayout.setOrientation(LinearLayout.HORIZONTAL);
        hudLayout.setPadding(40, 40, 40, 40);
        hudLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreTextView = new TextView(this);
        scoreTextView.setText("💎 Монеты: 0");
        scoreTextView.setTextColor(Color.parseColor("#F59E0B"));
        scoreTextView.setTextSize(18f);
        scoreTextView.setTypeface(Typeface.DEFAULT_BOLD);
        scoreTextView.setPadding(20, 10, 20, 10);
        
        // Полупрозрачный фон для панели счёта
        GradientDrawable hudBg = new GradientDrawable();
        hudBg.setColor(Color.parseColor("#990F172A"));
        hudBg.setCornerRadius(16f);
        hudBg.setStroke(2, Color.parseColor("#38BDF8"));
        scoreTextView.setBackground(hudBg);

        hudLayout.addView(scoreTextView);
        rootLayout.addView(hudLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        ));

        setContentView(rootLayout);
    }

    public void updateScore(int newScore) {
        score = newScore;
        runOnUiThread(() -> scoreTextView.setText("💎 Монеты: " + score));
    }

    // --- ИГРОВОЙ ДВИЖОК И КОНТРОЛЛЕРЫ (РОБЛОКС СТИЛЬ) ---
    private class GameView extends View {
        private Paint paint;
        private Handler handler = new Handler(Looper.getMainLooper());
        private Runnable updater;

        // Игрок (Кубический персонаж Roblox)
        private float playerX = 100, playerY = 400;
        private float playerWidth = 60, playerHeight = 90;
        private float vx = 0, vy = 0;
        private boolean isGrounded = false;
        private final float gravity = 0.8f;
        private final float jumpForce = -15f;
        private final float moveSpeed = 5f;

        // Джойстик (Левый стик управления)
        private float joystickCenterX = 200, joystickCenterY = 1400;
        private float joystickRadius = 120;
        private float stickX = 200, stickY = 1400;
        private boolean isJoystickActive = false;
        private int joystickTouchId = -1;

        // Кнопка прыжка (Правая сторона)
        private float jumpBtnX, jumpBtnY = 1350, jumpBtnRadius = 80;
        private boolean isJumpPressed = false;
        private int jumpTouchId = -1;

        // Мир (Платформы и монеты)
        private List<RectF> platforms = new ArrayList<>();
        private List<Coin> coins = new ArrayList<>();
        private Random random = new Random();

        public GameView(Context context) {
            super(context);
            paint = new Paint();
            paint.setAntiAlias(true);

            // Создаем мир (карту)
            platforms.add(new RectF(0, 1100, 1080, 1300)); // Земля
            platforms.add(new RectF(200, 900, 600, 940));  // Платформа 1
            platforms.add(new RectF(500, 700, 900, 740));  // Платформа 2
            platforms.add(new RectF(100, 500, 500, 540));  // Платформа 3

            // Спавним монеты
            coins.add(new Coin(350, 830));
            coins.add(new Coin(700, 630));
            coins.add(new Coin(300, 430));

            // Игровой цикл (~60 FPS)
            updater = new Runnable() {
                @Override
                public void run() {
                    updatePhysics();
                    invalidate();
                    handler.postDelayed(this, 16);
                }
            };
            handler.post(updater);
        }

        private void updatePhysics() {
            // Обработка движения от джойстика
            if (isJoystickActive) {
                float dx = stickX - joystickCenterX;
                if (Math.abs(dx) > 10) {
                    vx = (dx / joystickRadius) * moveSpeed;
                } else {
                    vx = 0;
                }
            } else {
                vx = 0;
            }

            // Гравитация
            vy += gravity;
            playerX += vx;
            playerY += vy;

            // Ограничения экрана
            if (playerX < 0) playerX = 0;
            if (playerX > getWidth() - playerWidth) playerX = getWidth() - playerWidth;

            // Коллизии с платформами
            isGrounded = false;
            RectF playerRect = new RectF(playerX, playerY, playerX + playerWidth, playerY + playerHeight);

            for (RectF p : platforms) {
                // Проверяем приземление сверху
                if (playerRect.intersect(p) && vy > 0) {
                    // Если падали сверху платформы
                    if (playerY + playerHeight - vy <= p.top + 15) {
                        playerY = p.top - playerHeight;
                        vy = 0;
                        isGrounded = true;
                    }
                }
            }

            // Проверка сбора монет
            for (int i = coins.size() - 1; i >= 0; i--) {
                Coin c = coins.get(i);
                if (!c.collected && playerRect.intersects(c.x, c.y, c.x + c.size, c.y + c.size)) {
                    c.collected = true;
                    score++;
                    updateScore(score);
                }
            }

            // Авто-респаун при падении ниже экрана
            if (playerY > 1800) {
                playerX = 100;
                playerY = 300;
                vy = 0;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Инициализация координат кнопки прыжка при первом рендере
            if (jumpBtnX == 0) {
                jumpBtnX = getWidth() - 220;
            }

            // 1. Отрисовка платформ (Стиль Roblox Obby)
            paint.setColor(Color.parseColor("#1E293B"));
            for (RectF p : platforms) {
                canvas.drawRoundRect(p, 12, 12, paint);
                // Неоновая подсветка грани платформы
                paint.setColor(Color.parseColor("#38BDF8"));
                canvas.drawRect(p.left, p.top, p.right, p.top + 6, paint);
                paint.setColor(Color.parseColor("#1E293B"));
            }

            // 2. Отрисовка монеток
            for (Coin c : coins) {
                if (!c.collected) {
                    paint.setColor(Color.parseColor("#F59E0B"));
                    canvas.drawCircle(c.x + c.size/2, c.y + c.size/2, c.size/2, paint);
                    paint.setColor(Color.parseColor("#FEF3C7"));
                    canvas.drawCircle(c.x + c.size/2, c.y + c.size/2, c.size/4, paint);
                }
            }

            // 3. Отрисовка игрока (Блочный персонаж Roblox)
            // Тело (Синяя футболка)
            paint.setColor(Color.parseColor("#2563EB"));
            canvas.drawRect(playerX, playerY + 25, playerX + playerWidth, playerY + playerHeight - 20, paint);
            // Голова (Желтая классическая голова)
            paint.setColor(Color.parseColor("#FACC15"));
            canvas.drawRect(playerX + 10, playerY, playerX + playerWidth - 10, playerY + 30, paint);
            // Ноги (Черные штаны)
            paint.setColor(Color.parseColor("#0F172A"));
            canvas.drawRect(playerX + 5, playerY + playerHeight - 20, playerX + playerWidth/2 - 2, playerY + playerHeight, paint);
            canvas.drawRect(playerX + playerWidth/2 + 2, playerY + playerHeight - 20, playerX + playerWidth - 5, playerY + playerHeight, paint);

            // 4. Отрисовка элементов управления (UI Joystick & Jump Button в стиле Roblox Mobile)
            // База джойстика
            paint.setColor(Color.parseColor("#334155"));
            paint.setAlpha(120);
            canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint);
            // Стикло (ручка джойстика)
            paint.setColor(Color.parseColor("#38BDF8"));
            paint.setAlpha(200);
            canvas.drawCircle(stickX, stickY, 50, paint);

            // Кнопка прыжка
            paint.setColor(Color.parseColor("#DC2626"));
            paint.setAlpha(160);
            canvas.drawCircle(jumpBtnX, jumpBtnY, jumpBtnRadius, paint);
            
            // Текст внутри кнопки прыжка
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            paint.setTextSize(36f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("JUMP", jumpBtnX - 50, jumpBtnY + 12, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);

            float x = event.getX(pointerIndex);
            float y = event.getY(pointerIndex);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    // Проверяем нажатие на джойстик (левая половина экрана)
                    if (x < getWidth() / 2 && !isJoystickActive) {
                        isJoystickActive = true;
                        joystickTouchId = pointerId;
                        joystickCenterX = x;
                        joystickCenterY = y;
                        stickX = x;
                        stickY = y;
                    }
                    // Проверяем нажатие на кнопку прыжка (правая нижняя область)
                    else if (Math.hypot(x - jumpBtnX, y - jumpBtnY) < jumpBtnRadius + 50 && !isJumpPressed) {
                        isJumpPressed = true;
                        jumpTouchId = pointerId;
                        if (isGrounded) {
                            vy = jumpForce;
                            isGrounded = false;
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        int id = event.getPointerId(i);
                        float px = event.getX(i);
                        float py = event.getY(i);

                        if (id == joystickTouchId) {
                            float dx = px - joystickCenterX;
                            float dy = py - joystickCenterY;
                            float dist = (float) Math.hypot(dx, dy);

                            if (dist < joystickRadius) {
                                stickX = px;
                                stickY = py;
                            } else {
                                stickX = joystickCenterX + (dx / dist) * joystickRadius;
                                stickY = joystickCenterY + (dy / dist) * joystickRadius;
                            }
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (pointerId == joystickTouchId) {
                        isJoystickActive = false;
                        joystickTouchId = -1;
                        // Возвращаем центр джойстика на исходную позицию
                        joystickCenterX = 200;
                        joystickCenterY = 1400;
                        stickX = 200;
                        stickY = 1400;
                    }
                    if (pointerId == jumpTouchId) {
                        isJumpPressed = false;
                        jumpTouchId = -1;
                    }
                    break;
            }
            return true;
        }
    }

    // Класс для объектов монет
    private static class Coin {
        float x, y;
        float size = 40;
        boolean collected = false;

        public Coin(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}