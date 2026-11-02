package com.roblox.version01;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
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

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private GameView gameView;
    private TextView hudTextView;
    private int currentMission = 1;
    private int totalCoins = 0;
    private boolean missionCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));

        gameView = new GameView(this, currentMission);
        rootLayout.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Верхняя панель HUD: Миссия, Монеты, Опасноть
        LinearLayout hudLayout = new LinearLayout(this);
        hudLayout.setOrientation(LinearLayout.HORIZONTAL);
        hudLayout.setPadding(30, 40, 30, 40);
        hudLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        hudTextView = new TextView(this);
        updateHudText();
        hudTextView.setTextColor(Color.parseColor("#F8FAFC"));
        hudTextView.setTextSize(14f);
        hudTextView.setTypeface(Typeface.DEFAULT_BOLD);
        hudTextView.setPadding(20, 10, 20, 10);
        
        GradientDrawable hudBg = new GradientDrawable();
        hudBg.setColor(Color.parseColor("#CC0F172A"));
        hudBg.setCornerRadius(16f);
        hudBg.setStroke(2, Color.parseColor("#EF4444")); // Красная рамка опасности
        hudTextView.setBackground(hudBg);

        hudLayout.addView(hudTextView);
        rootLayout.addView(hudLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        ));

        setContentView(rootLayout);
    }

    private void updateHudText() {
        hudTextView.setText("🔥 МИССИЯ " + currentMission + "/3 | 💎 Сбор: " + totalCoins);
    }

    public void onMissionComplete() {
        if (missionCompleted) return;
        missionCompleted = true;

        if (currentMission < 3) {
            currentMission++;
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "⚡ МИССИЯ ПРОЙДЕНА! Переход на уровень " + currentMission, android.widget.Toast.LENGTH_SHORT).show();
                FrameLayout root = (FrameLayout) gameView.getParent();
                if (root != null) {
                    root.removeView(gameView);
                    gameView = new GameView(this, currentMission);
                    root.addView(gameView, 0);
                }
                missionCompleted = false;
                updateHudText();
            });
        } else {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "🏆 ПОБЕДА! ВЫ ПРОШЛИ ВСЕ ОПАСНЫЕ МИССИИ РОБЛОКСА!", android.widget.Toast.LENGTH_LONG).show();
                currentMission = 1;
                totalCoins = 0;
                missionCompleted = false;
                updateHudText();
                // Перезапуск с первой миссии
                FrameLayout root = (FrameLayout) gameView.getParent();
                if (root != null) {
                    root.removeView(gameView);
                    gameView = new GameView(this, currentMission);
                    root.addView(gameView, 0);
                }
            });
        }
    }

    public void addCoin() {
        totalCoins++;
        runOnUiThread(this::updateHudText);
    }

    // --- ХАРДКОРНЫЙ ИГРОВОЙ ДВИЖОК ---
    private class GameView extends View {
        private Paint paint;
        private Handler handler = new Handler(Looper.getMainLooper());
        private Runnable updater;

        private float playerX = 100, playerY = 300;
        private float playerWidth = 55, playerHeight = 85;
        private float vx = 0, vy = 0;
        private boolean isGrounded = false;
        private final float gravity = 0.85f;
        private final float jumpForce = -16.5f;
        private final float moveSpeed = 5.5f;

        private float joystickCenterX = 200, joystickCenterY = 1400;
        private float joystickRadius = 120;
        private float stickX = 200, stickY = 1400;
        private boolean isJoystickActive = false;
        private int joystickTouchId = -1;

        private float jumpBtnX, jumpBtnY = 1350, jumpBtnRadius = 80;
        private boolean isJumpPressed = false;
        private int jumpTouchId = -1;

        private List<RectF> platforms = new ArrayList<>();
        private List<RectF> hazards = new ArrayList<>(); // Лазеры, шипы, кислота
        private List<Coin> coins = new ArrayList<>();
        private RectF finishPortal;
        private int missionId;

        public GameView(Context context, int mission) {
            super(context);
            this.missionId = mission;
            paint = new Paint();
            paint.setAntiAlias(true);

            loadMissionData(mission);

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

        private void loadMissionData(int mission) {
            platforms.clear();
            hazards.clear();
            coins.clear();

            if (mission == 1) {
                // МИССИЯ 1: Базовое Обби с лазерами
                platforms.add(new RectF(0, 1100, 1080, 1300)); // Земля
                platforms.add(new RectF(150, 920, 500, 960));
                platforms.add(new RectF(600, 780, 950, 820));
                platforms.add(new RectF(200, 620, 550, 660));
                platforms.add(new RectF(650, 480, 1000, 520)); // Верхняя платформа с финишем

                // Опасности (Красные лазерные барьеры смерти)
                hazards.add(new RectF(300, 1060, 360, 1100));
                hazards.add(new RectF(700, 740, 760, 780));
                hazards.add(new RectF(400, 580, 460, 620));

                coins.add(new Coin(300, 860));
                coins.add(new Coin(750, 720));
                coins.add(new Coin(350, 560));

                finishPortal = new RectF(820, 380, 920, 480);

            } else if (mission == 2) {
                // МИССИЯ 2: Узкие прыжки и много кислоты
                platforms.add(new RectF(0, 1100, 300, 1300));
                platforms.add(new RectF(450, 1000, 650, 1040));
                platforms.add(new RectF(150, 840, 350, 880));
                platforms.add(new RectF(500, 700, 750, 740));
                platforms.add(new RectF(200, 540, 450, 580));
                platforms.add(new RectF(650, 420, 980, 460));

                // Кислотное озеро внизу и шипы
                hazards.add(new RectF(300, 1200, 1080, 1300)); // Смертельная кислота
                hazards.add(new RectF(520, 960, 580, 1000));
                hazards.add(new RectF(220, 800, 280, 840));

                coins.add(new Coin(530, 930));
                coins.add(new Coin(220, 780));
                coins.add(new Coin(600, 640));
                coins.add(new Coin(300, 480));

                finishPortal = new RectF(800, 320, 900, 420);

            } else {
                // МИССИЯ 3: Экстремальный лабиринт смерти (Хардкор)
                platforms.add(new RectF(0, 1100, 250, 1300));
                platforms.add(new RectF(350, 980, 500, 1010));
                platforms.add(new RectF(600, 850, 750, 880));
                platforms.add(new RectF(350, 720, 500, 750));
                platforms.add(new RectF(100, 600, 250, 630));
                platforms.add(new RectF(400, 460, 600, 490));
                platforms.add(new RectF(700, 360, 950, 390));

                // Лазеры везде
                hazards.add(new RectF(250, 1100, 350, 1300)); // Провал со смертью
                hazards.add(new RectF(400, 940, 450, 980));
                hazards.add(new RectF(650, 810, 700, 850));
                hazards.add(new RectF(150, 560, 200, 600));
                hazards.add(new RectF(470, 420, 530, 460));

                coins.add(new Coin(400, 920));
                coins.add(new Coin(650, 790));
                coins.add(new Coin(150, 540));
                coins.add(new Coin(500, 400));
                coins.add(new Coin(800, 300));

                finishPortal = new RectF(800, 260, 900, 360);
            }
        }

        private void updatePhysics() {
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

            vy += gravity;
            playerX += vx;
            playerY += vy;

            if (playerX < 0) playerX = 0;
            if (playerX > getWidth() - playerWidth) playerX = getWidth() - playerWidth;

            isGrounded = false;
            RectF playerRect = new RectF(playerX, playerY, playerX + playerWidth, playerY + playerHeight);

            // Проверка коллизий с платформами
            for (RectF p : platforms) {
                if (playerRect.intersect(p) && vy > 0) {
                    if (playerY + playerHeight - vy <= p.top + 18) {
                        playerY = p.top - playerHeight;
                        vy = 0;
                        isGrounded = true;
                    }
                }
            }

            // Проверка смертельных опасностей (Лазеры, шипы, кислота)
            for (RectF h : hazards) {
                if (playerRect.intersect(h)) {
                    respawnPlayer();
                    return;
                }
            }

            // Сбор монет
            for (int i = coins.size() - 1; i >= 0; i--) {
                Coin c = coins.get(i);
                if (!c.collected && playerRect.intersects(c.x, c.y, c.x + c.size, c.y + c.size)) {
                    c.collected = true;
                    addCoin();
                }
            }

            // Проверка достижения финишного портала
            if (finishPortal != null && playerRect.intersect(finishPortal)) {
                onMissionComplete();
                return;
            }

            // Падение в пропасть вне экранов
            if (playerY > 1800) {
                respawnPlayer();
            }
        }

        private void respawnPlayer() {
            playerX = 80;
            playerY = 950;
            vy = 0;
            vx = 0;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (jumpBtnX == 0) {
                jumpBtnX = getWidth() - 220;
            }

            // 1. Отрисовка платформ (Стиль кибернетического Obby)
            paint.setColor(Color.parseColor("#1E293B"));
            for (RectF p : platforms) {
                canvas.drawRoundRect(p, 10, 10, paint);
                paint.setColor(Color.parseColor("#38BDF8"));
                canvas.drawRect(p.left, p.top, p.right, p.top + 6, paint); // Неоновый край
                paint.setColor(Color.parseColor("#1E293B"));
            }

            // 2. Отрисовка смертельных опасностей (Красные лазеры / Шипы)
            paint.setColor(Color.parseColor("#EF4444"));
            for (RectF h : hazards) {
                canvas.drawRoundRect(h, 8, 8, paint);
                // Эффект свечения лазера
                paint.setColor(Color.parseColor("#FCA5A5"));
                canvas.drawRect(h.left + 4, h.top + 4, h.right - 4, h.bottom - 4, paint);
                paint.setColor(Color.parseColor("#EF4444"));
            }

            // 3. Финишный портал (Яркое свечение победы)
            if (finishPortal != null) {
                paint.setColor(Color.parseColor("#10B981"));
                canvas.drawRoundRect(finishPortal, 20, 20, paint);
                paint.setColor(Color.parseColor("#A7F3D0"));
                canvas.drawCircle(finishPortal.centerX(), finishPortal.centerY(), 25, paint);
            }

            // 4. Отрисовка монет
            for (Coin c : coins) {
                if (!c.collected) {
                    paint.setColor(Color.parseColor("#F59E0B"));
                    canvas.drawCircle(c.x + c.size/2, c.y + c.size/2, c.size/2, paint);
                    paint.setColor(Color.parseColor("#FEF3C7"));
                    canvas.drawCircle(c.x + c.size/2, c.y + c.size/2, c.size/4, paint);
                }
            }

            // 5. Персонаж Роблокс (Стильный аватар с деталями)
            // Тело
            paint.setColor(Color.parseColor("#6366F1"));
            canvas.drawRect(playerX, playerY + 22, playerX + playerWidth, playerY + playerHeight - 18, paint);
            // Голова
            paint.setColor(Color.parseColor("#FBBF24"));
            canvas.drawRect(playerX + 8, playerY, playerX + playerWidth - 8, playerY + 26, paint);
            // Ноги
            paint.setColor(Color.parseColor("#0F172A"));
            canvas.drawRect(playerX + 4, playerY + playerHeight - 18, playerX + playerWidth/2 - 2, playerY + playerHeight, paint);
            canvas.drawRect(playerX + playerWidth/2 + 2, playerY + playerHeight - 18, playerX + playerWidth - 4, playerY + playerHeight, paint);

            // 6. UI Управления (Джойстик и Кнопка Прыжка)
            paint.setColor(Color.parseColor("#334155"));
            paint.setAlpha(120);
            canvas.drawCircle(joystickCenterX, joystickCenterY, joystickRadius, paint);
            paint.setColor(Color.parseColor("#38BDF8"));
            paint.setAlpha(200);
            canvas.drawCircle(stickX, stickY, 45, paint);

            paint.setColor(Color.parseColor("#DC2626"));
            paint.setAlpha(170);
            canvas.drawCircle(jumpBtnX, jumpBtnY, jumpBtnRadius, paint);
            
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            paint.setTextSize(34f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("JUMP", jumpBtnX - 48, jumpBtnY + 11, paint);
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
                    if (x < getWidth() / 2 && !isJoystickActive) {
                        isJoystickActive = true;
                        joystickTouchId = pointerId;
                        joystickCenterX = x;
                        joystickCenterY = y;
                        stickX = x;
                        stickY = y;
                    } else if (Math.hypot(x - jumpBtnX, y - jumpBtnY) < jumpBtnRadius + 50 && !isJumpPressed) {
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