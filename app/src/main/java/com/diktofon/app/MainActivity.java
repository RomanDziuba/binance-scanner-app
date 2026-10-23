package com.diktofon.app;

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
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private int score = 0;
    private int misses = 0;
    private final int MAX_MISSES = 3;
    private int basketPos = 1; // 0=UL, 1=UR, 2=LL, 3=LR
    private boolean isGameOver = false;
    private List<Egg> eggs = new ArrayList<>();
    private Random random = new Random();
    private float eggSpeed = 1.0f;
    private long spawnRate = 2000;
    private long lastSpawnTime = 0;

    private GameView gameView;
    private Handler gameLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable gameLoopRunnable;

    private static class Egg {
        int pos;
        float progress = 0;
        boolean isActive = true;

        Egg(int pos) {
            this.pos = pos;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#111827"));
        rootLayout.setPadding(10, 10, 10, 10);

        TextView titleView = new TextView(this);
        titleView.setText("ШИВА-ЯЙЦЕЛОБ (НУ ПОГОДИ!)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 10, 0, 10);
        rootLayout.addView(titleView);

        gameView = new GameView(this);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        );
        gameParams.setMargins(0, 10, 0, 10);
        gameView.setLayoutParams(gameParams);
        
        GradientDrawable gameBg = new GradientDrawable();
        gameBg.setColor(Color.parseColor("#0F172A"));
        gameBg.setCornerRadius(20f);
        gameBg.setStroke(4, Color.parseColor("#334155"));
        gameView.setBackground(gameBg);
        
        rootLayout.addView(gameView);

        LinearLayout scoreLayout = new LinearLayout(this);
        scoreLayout.setOrientation(LinearLayout.HORIZONTAL);
        scoreLayout.setGravity(Gravity.CENTER);
        scoreLayout.setPadding(0, 10, 0, 10);

        TextView scoreTitle = new TextView(this);
        scoreTitle.setText("СЧЕТ: ");
        scoreTitle.setTextColor(Color.parseColor("#94A3B8"));
        scoreTitle.setTextSize(16f);
        scoreLayout.addView(scoreTitle);

        final TextView scoreValue = new TextView(this);
        scoreValue.setText("0");
        scoreValue.setTextColor(Color.parseColor("#10B981"));
        scoreValue.setTextSize(24f);
        scoreValue.setTypeface(Typeface.MONOSPACE);
        scoreValue.setPadding(0, 0, 20, 0);
        scoreLayout.addView(scoreValue);
        
        Button btnReset = new Button(this);
        btnReset.setText("🔄 Заново");
        btnReset.setTextColor(Color.WHITE);
        btnReset.setBackgroundColor(Color.parseColor("#7C3AED"));
        btnReset.setPadding(15, 10, 15, 10);
        btnReset.setOnClickListener(v -> resetGame());
        scoreLayout.addView(btnReset);

        rootLayout.addView(scoreLayout);
        setContentView(rootLayout);

        gameLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameOver) {
                    updateGame();
                    scoreValue.setText(String.valueOf(score));
                    gameView.invalidate();
                    gameLoopHandler.postDelayed(this, 33);
                }
            }
        };
        startGame();
    }

    private void startGame() {
        isGameOver = false;
        score = 0;
        misses = 0;
        eggSpeed = 1.0f;
        spawnRate = 2000;
        eggs.clear();
        basketPos = 1;
        gameLoopHandler.post(gameLoopRunnable);
    }

    private void resetGame() {
        gameLoopHandler.removeCallbacks(gameLoopRunnable);
        startGame();
    }

    private void gameOver() {
        isGameOver = true;
        gameLoopHandler.removeCallbacks(gameLoopRunnable);
        Toast.makeText(this, "КОНЕЦ ИГРЫ! СЧЕТ: " + score, Toast.LENGTH_LONG).show();
        gameView.invalidate();
    }

    private void updateGame() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastSpawnTime > spawnRate) {
            eggs.add(new Egg(random.nextInt(4)));
            lastSpawnTime = currentTime;
            
            eggSpeed += 0.01f;
            if (spawnRate > 500) spawnRate -= 10;
        }

        List<Egg> toRemove = new ArrayList<>();
        
        for (Egg egg : eggs) {
            if (!egg.isActive) {
                toRemove.add(egg);
                continue;
            }
            
            egg.progress += eggSpeed;
            
            if (egg.progress >= 95.0f) {
                if (egg.pos == basketPos) {
                    score++;
                    egg.isActive = false;
                } else if (egg.progress >= 100.0f) {
                    misses++;
                    egg.isActive = false;
                    if (misses >= MAX_MISSES) {
                        gameOver();
                        return;
                    }
                }
            }
        }
        eggs.removeAll(toRemove);
    }

    private class GameView extends View {
        private Paint charPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint rampPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint chickenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint eggPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint uiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path charPath = new Path();

        public GameView(Context context) {
            super(context);
            rampPaint.setColor(Color.parseColor("#334155"));
            rampPaint.setStrokeWidth(6f);
            rampPaint.setStyle(Paint.Style.STROKE);
            
            chickenPaint.setColor(Color.parseColor("#F1F5F9"));
            chickenPaint.setStyle(Paint.Style.FILL);
            
            eggPaint.setColor(Color.parseColor("#FDE68A"));
            eggPaint.setStyle(Paint.Style.FILL);
            
            uiPaint.setColor(Color.parseColor("#EF4444"));
            uiPaint.setTextSize(14f);
            uiPaint.setTypeface(Typeface.MONOSPACE);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int centerX = w / 2;
            int centerY = h / 2;
            int inset = w / 10;

            // 1. Желоба
            canvas.drawLine(inset, inset, centerX - inset, centerY - inset, rampPaint);
            canvas.drawLine(w - inset, inset, centerX + inset, centerY - inset, rampPaint);
            canvas.drawLine(inset, h - inset, centerX - inset, centerY + inset, rampPaint);
            canvas.drawLine(w - inset, h - inset, centerX + inset, centerY + inset, rampPaint);

            // 2. Куры
            canvas.drawCircle(inset / 2, inset / 2, inset / 2, chickenPaint);
            canvas.drawCircle(w - inset / 2, inset / 2, inset / 2, chickenPaint);
            canvas.drawCircle(inset / 2, h - inset / 2, inset / 2, chickenPaint);
            canvas.drawCircle(w - inset / 2, h - inset / 2, inset / 2, chickenPaint);

            // 3. ПЕРСОНАЖ И КОРЗИНЫ
            canvas.save();
            canvas.translate(centerX, centerY);
            
            charPaint.setStyle(Paint.Style.FILL);
            charPaint.setColor(Color.parseColor("#E5CFC1"));
            canvas.drawCircle(0, -centerY / 4, centerX / 6, charPaint);
            
            charPaint.setColor(Color.parseColor("#4B5563"));
            canvas.drawRect(-centerX / 12, -centerY / 4 + centerX / 12, centerX / 12, -centerY / 4 + centerX / 6, charPaint);

            charPaint.setColor(Color.parseColor("#D2B48C"));
            canvas.drawRect(-centerX / 6, -centerY / 4 + centerX / 6, centerX / 6, centerY / 6, charPaint);
            
            charPaint.setColor(Color.parseColor("#1D4ED8"));
            canvas.drawRect(-centerX / 5, centerY / 6, centerX / 5, centerY / 3, charPaint);
            
            rampPaint.setColor(Color.WHITE);
            rampPaint.setStrokeWidth(3f);
            canvas.drawLine(-centerX / 20, centerY / 6 + centerX / 20, centerX / 20, centerY / 6 + centerX / 20, rampPaint);
            rampPaint.setColor(Color.parseColor("#334155"));
            rampPaint.setStrokeWidth(6f);
            
            canvas.restore();

            int armEndX, armEndY;
            charPaint.setColor(Color.parseColor("#D2B48C"));
            charPaint.setStyle(Paint.Style.STROKE);
            charPaint.setStrokeWidth(8f);

            charPath.reset();
            charPath.moveTo(centerX, centerY - centerY / 12);
            
            switch (basketPos) {
                case 0:
                    charPath.lineTo(centerX - inset, centerY - inset);
                    armEndX = centerX - inset;
                    armEndY = centerY - inset;
                    break;
                case 1:
                    charPath.lineTo(centerX + inset, centerY - inset);
                    armEndX = centerX + inset;
                    armEndY = centerY - inset;
                    break;
                case 2:
                    charPath.lineTo(centerX - inset, centerY + inset);
                    armEndX = centerX - inset;
                    armEndY = centerY + inset;
                    break;
                case 3:
                    charPath.lineTo(centerX + inset, centerY + inset);
                    armEndX = centerX + inset;
                    armEndY = centerY + inset;
                    break;
                default:
                    armEndX = centerX;
                    armEndY = centerY;
            }
            canvas.drawPath(charPath, charPaint);

            charPaint.setColor(Color.parseColor("#854D0E"));
            charPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(armEndX - inset / 4, armEndY, armEndX + inset / 4, armEndY + inset / 4, charPaint);

            // 4. ЯЙЦА
            for (Egg egg : eggs) {
                if (egg.isActive) {
                    float eggW = inset / 3;
                    float startX, startY, endX, endY, currentX, currentY;
                    
                    switch (egg.pos) {
                        case 0:
                            startX = inset; startY = inset;
                            endX = centerX - inset; endY = centerY - inset;
                            break;
                        case 1:
                            startX = w - inset; startY = inset;
                            endX = centerX + inset; endY = centerY - inset;
                            break;
                        case 2:
                            startX = inset; startY = h - inset;
                            endX = centerX - inset; endY = centerY + inset;
                            break;
                        case 3:
                            startX = w - inset; startY = h - inset;
                            endX = centerX + inset; endY = centerY + inset;
                            break;
                        default: continue;
                    }
                    
                    currentX = startX + (endX - startX) * (egg.progress / 100.0f);
                    currentY = startY + (endY - startY) * (egg.progress / 100.0f);
                    canvas.drawOval(currentX - eggW / 2, currentY - eggW / 2, currentX + eggW / 2, currentY + eggW / 2, eggPaint);
                }
            }

            // 5. ПРОМАХИ
            StringBuilder missesText = new StringBuilder("ПРОМАХИ: ");
            for (int i = 0; i < MAX_MISSES; i++) {
                if (i < misses) missesText.append("● ");
                else missesText.append("○ ");
            }
            canvas.drawText(missesText.toString(), centerX, centerY * 1.8f, uiPaint);
            
            if (isGameOver) {
                uiPaint.setTextSize(30f);
                uiPaint.setColor(Color.parseColor("#DC2626"));
                canvas.drawText("GAME OVER!", centerX, centerY, uiPaint);
                uiPaint.setTextSize(14f);
                uiPaint.setColor(Color.parseColor("#EF4444"));
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isGameOver && event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();
                int w = getWidth();
                int h = getHeight();

                if (x < w / 2 && y < h / 2) basketPos = 0;
                else if (x > w / 2 && y < h / 2) basketPos = 1;
                else if (x < w / 2 && y > h / 2) basketPos = 2;
                else if (x > w / 2 && y > h / 2) basketPos = 3;
                
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}