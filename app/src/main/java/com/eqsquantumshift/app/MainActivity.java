package com.eqsquantumshift.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private int currentLevel = 1;
    private int totalScore = 0;
    private int movesOnLevel = 0;
    private int maxAllowedMoves = 10;
    
    private int gridCols = 3;
    private int gridRows = 3;
    private int maxStates = 2;

    private TextView[][] cellViews;
    private int[][] states;
    
    private TextView levelTextView, scoreTextView, movesTextView, titleTextView, logoView, clockTextView, instructionView;
    private Button btnExit, btnHintSquare;
    private GridLayout gridLayout;
    
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;
    private ValueAnimator rgbAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#040711"));
        rootLayout.setPadding(14, 14, 14, 14);

        // --- ШАПКА ПРИЛОЖЕНИЯ ---
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 2, 0, 6);

        titleTextView = new TextView(this);
        titleTextView.setText("EQS: QUANTUM SHIFT");
        titleTextView.setTextColor(Color.parseColor("#38BDF8"));
        titleTextView.setTextSize(13);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleTextView.setLayoutParams(titleParams);

        logoView = new TextView(this);
        logoView.setText("DRV");
        logoView.setTextColor(Color.parseColor("#A855F7"));
        logoView.setTextSize(10);
        logoView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        logoView.setPadding(6, 2, 6, 2);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(Color.parseColor("#1E1035"));
        logoBg.setCornerRadius(8f);
        logoBg.setStroke(2, Color.parseColor("#7E22CE"));
        logoView.setBackground(logoBg);

        // RGB-переливающийся квадрат подсказки (-2 хода)
        btnHintSquare = new Button(this);
        btnHintSquare.setText("💡");
        btnHintSquare.setTextSize(12);
        btnHintSquare.setPadding(8, 2, 8, 2);
        GradientDrawable hintBg = new GradientDrawable();
        hintBg.setCornerRadius(10f);
        btnHintSquare.setBackground(hintBg);
        
        rgbAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), 
                Color.parseColor("#3B82F6"), Color.parseColor("#EC4899"), 
                Color.parseColor("#10B981"), Color.parseColor("#F59E0B"), Color.parseColor("#3B82F6"));
        rgbAnimator.setDuration(3000);
        rgbAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rgbAnimator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            hintBg.setColor(color);
            hintBg.setStroke(2, Color.WHITE);
        });
        rgbAnimator.start();

        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.setMargins(6, 0, 0, 0);
        btnHintSquare.setLayoutParams(hintParams);
        btnHintSquare.setOnClickListener(v -> useSmartHint());

        // Кнопка ВЫХОД
        btnExit = new Button(this);
        btnExit.setText("❌");
        btnExit.setTextSize(12);
        btnExit.setPadding(8, 2, 8, 2);
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.parseColor("#270F19"));
        exitBg.setCornerRadius(10f);
        exitBg.setStroke(2, Color.parseColor("#E11D48"));
        btnExit.setBackground(exitBg);
        LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        exitParams.setMargins(6, 0, 0, 0);
        btnExit.setLayoutParams(exitParams);
        btnExit.setOnClickListener(v -> finishAndRemoveTask());

        headerLayout.addView(titleTextView);
        headerLayout.addView(logoView);
        headerLayout.addView(btnHintSquare);
        headerLayout.addView(btnExit);
        rootLayout.addView(headerLayout);

        // --- КВАНТОВЫЕ ЧАСЫ ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(12);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(6, 4, 6, 4);

        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColor(Color.parseColor("#022C22"));
        clockBg.setCornerRadius(10f);
        clockBg.setStroke(2, Color.parseColor("#059669"));
        clockTextView.setBackground(clockBg);

        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 0, 0, 6);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);
        startClockAnimation();

        // --- ИНФО-ПАНЕЛЬ ---
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setPadding(6, 8, 6, 8);
        
        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setColor(Color.parseColor("#0B1120"));
        statsBg.setCornerRadius(12f);
        statsBg.setStroke(2, Color.parseColor("#1E293B"));
        statsLayout.setBackground(statsBg);

        levelTextView = createStatText("УР: 1/6", "#38BDF8");
        scoreTextView = createStatText("ОЧКИ: 0", "#F59E0B");
        movesTextView = createStatText("ХОДЫ: 0/0", "#10B981");

        statsLayout.addView(levelTextView);
        statsLayout.addView(scoreTextView);
        statsLayout.addView(movesTextView);
        rootLayout.addView(statsLayout);

        // --- ИГРОВАЯ СЕТКА ---
        gridLayout = new GridLayout(this);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        gridParams.setMargins(0, 8, 0, 0);
        gridLayout.setLayoutParams(gridParams);
        rootLayout.addView(gridLayout);

        // --- КРАТКАЯ ИНСТРУКЦИЯ ВНИЗУ ---
        instructionView = new TextView(this);
        instructionView.setText("📖 Цель: сделать ВСЕ клетки одинаковыми.\n💡 RGB-кнопка = точный шаг (-2 хода). Превысишь ходы — взрыв ядра!");
        instructionView.setTextColor(Color.parseColor("#64748B"));
        instructionView.setTextSize(10);
        instructionView.setGravity(Gravity.CENTER);
        instructionView.setPadding(4, 6, 4, 4);
        rootLayout.addView(instructionView);

        setContentView(rootLayout);
        loadLevel(currentLevel);
    }

    private void startClockAnimation() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String timeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("⚡ TIME: " + timeOnly + " ⚡");
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
        if (rgbAnimator != null) {
            rgbAnimator.cancel();
        }
    }

    private TextView createStatText(String text, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        return tv;
    }

    // 💡 ЛОГИЧЕСКАЯ ПОДСКАЗКА
    private void useSmartHint() {
        movesOnLevel += 2; // Штраф 2 хода за подсказку
        updateMovesDisplay();

        if (movesOnLevel > maxAllowedMoves) {
            triggerExplosionGameOver();
            return;
        }

        int bestR = 0, bestC = 0;
        int minMismatch = 999;

        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                int mismatch = countMismatchesAfterSimulatedClick(i, j);
                if (mismatch < minMismatch) {
                    minMismatch = mismatch;
                    bestR = i;
                    bestC = j;
                }
            }
        }

        Toast.makeText(this, "🤓 Подсказка: жми на [" + (bestR + 1) + ", " + (bestC + 1) + "] (-2 хода)", Toast.LENGTH_LONG).show();
        triggerQuantumWave(bestR, bestC);
        
        if (checkVictoryCondition()) {
            calculateScoreAndAdvance();
        }
    }

    private int countMismatchesAfterSimulatedClick(int r, int c) {
        int[][] tempStates = new int[gridRows][gridCols];
        for(int i=0; i<gridRows; i++) {
            System.arraycopy(states[i], 0, tempStates[i], 0, gridCols);
        }
        
        simulateWave(tempStates, r, c);

        int target = tempStates[0][0];
        int count = 0;
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                if (tempStates[i][j] != target) count++;
            }
        }
        return count;
    }

    private void simulateWave(int[][] s, int r, int c) {
        s[r][c] = (s[r][c] + 1) % maxStates;
        if (r > 0) s[r - 1][c] = (s[r - 1][c] + 1) % maxStates;
        if (r < gridRows - 1) s[r + 1][c] = (s[r + 1][c] + 1) % maxStates;
        if (c > 0) s[r][c - 1] = (s[r][c - 1] + 1) % maxStates;
        if (c < gridCols - 1) s[r][c + 1] = (s[r][c + 1] + 1) % maxStates;
        if (currentLevel >= 3) {
            if (r > 0 && c > 0) s[r - 1][c - 1] = (s[r - 1][c - 1] + 1) % maxStates;
            if (r > 0 && c < gridCols - 1) s[r - 1][c + 1] = (s[r - 1][c + 1] + 1) % maxStates;
            if (r < gridRows - 1 && c > 0) s[r + 1][c - 1] = (s[r + 1][c - 1] + 1) % maxStates;
            if (r < gridRows - 1 && c < gridCols - 1) s[r + 1][c + 1] = (s[r + 1][c + 1] + 1) % maxStates;
        }
    }

    private void loadLevel(int level) {
        movesOnLevel = 0;
        
        switch (level) {
            case 1: gridCols = 3; gridRows = 3; maxStates = 2; break;
            case 2: gridCols = 4; gridRows = 4; maxStates = 2; break;
            case 3: gridCols = 4; gridRows = 4; maxStates = 3; break;
            case 4: gridCols = 5; gridRows = 5; maxStates = 3; break;
            case 5: gridCols = 5; gridRows = 5; maxStates = 4; break;
            case 6: gridCols = 6; gridRows = 6; maxStates = 4; break;
        }

        maxAllowedMoves = (gridCols * gridRows * maxStates) + (level * 4);

        levelTextView.setText("УР: " + level + "/6");
        updateMovesDisplay();

        cellViews = new TextView[gridRows][gridCols];
        states = new int[gridRows][gridCols];
        
        initQuantumField();
        buildGridUI();
    }

    private void updateMovesDisplay() {
        movesTextView.setText("ХОДЫ: " + movesOnLevel + "/" + maxAllowedMoves);
    }

    private void initQuantumField() {
        Random random = new Random();
        do {
            for (int i = 0; i < gridRows; i++) {
                for (int j = 0; j < gridCols; j++) {
                    states[i][j] = random.nextInt(maxStates);
                }
            }
        } while (checkVictoryCondition());
    }

    private void buildGridUI() {
        gridLayout.removeAllViews();
        gridLayout.setRowCount(gridRows);
        gridLayout.setColumnCount(gridCols);

        int screenWidth = getResources().getDisplayMetrics().widthPixels - 50;
        int cellSize = screenWidth / gridCols - 8;

        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                final int row = i;
                final int col = j;

                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(4, 4, 4, 4);
                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(24);

                updateCellStyle(cell, states[i][j], currentLevel);
                startLivingCellAnimation(cell);

                cell.setOnClickListener(v -> {
                    triggerQuantumWave(row, col);
                    movesOnLevel++;
                    updateMovesDisplay();

                    if (movesOnLevel > maxAllowedMoves) {
                        triggerExplosionGameOver();
                        return;
                    }

                    if (checkVictoryCondition()) {
                        calculateScoreAndAdvance();
                    }
                });

                cellViews[i][j] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void startLivingCellAnimation(View cell) {
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(cell, "scaleX", 1.0f, 1.08f, 1.0f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(cell, "scaleY", 1.0f, 1.08f, 1.0f);
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        
        long duration = 1200 + new Random().nextInt(600);
        pulseX.setDuration(duration);
        pulseY.setDuration(duration);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(pulseX, pulseY);
        set.start();
    }

    private void triggerQuantumWave(int r, int c) {
        toggleState(r, c);
        if (r > 0) toggleState(r - 1, c);
        if (r < gridRows - 1) toggleState(r + 1, c);
        if (c > 0) toggleState(r, c - 1);
        if (c < gridCols - 1) toggleState(r, c + 1);

        if (currentLevel >= 3) {
            if (r > 0 && c > 0) toggleState(r - 1, c - 1);
            if (r > 0 && c < gridCols - 1) toggleState(r - 1, c + 1);
            if (r < gridRows - 1 && c > 0) toggleState(r + 1, c - 1);
            if (r < gridRows - 1 && c < gridCols - 1) toggleState(r + 1, c + 1);
        }
    }

    private void toggleState(int r, int c) {
        states[r][c] = (states[r][c] + 1) % maxStates;
        animateCellFancy(cellViews[r][c], states[r][c], currentLevel);
    }

    private void animateCellFancy(TextView cell, int newState, int level) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cell, "scaleX", 0.7f, 1.15f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cell, "scaleY", 0.7f, 1.15f, 1.0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(cell, "rotation", 0f, 360f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, rotation);
        animatorSet.setDuration(280);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();

        updateCellStyle(cell, newState, level);
    }

    private void updateCellStyle(TextView cell, int state, int level) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);

        String symbol = "⚛";
        
        switch (level) {
            case 1:
                if (state == 0) {
                    bg.setColors(new int[]{Color.parseColor("#0F172A"), Color.parseColor("#0284C7")});
                    bg.setStroke(3, Color.parseColor("#38BDF8"));
                    symbol = "🪐";
                } else {
                    bg.setColors(new int[]{Color.parseColor("#1E3A8A"), Color.parseColor("#2563EB")});
                    bg.setStroke(3, Color.parseColor("#60A5FA"));
                    symbol = "🌌";
                }
                break;
            case 2:
                if (state == 0) {
                    bg.setColors(new int[]{Color.parseColor("#2E1065"), Color.parseColor("#6D28D9")});
                    bg.setStroke(3, Color.parseColor("#A78BFA"));
                    symbol = "🔮";
                } else {
                    bg.setColors(new int[]{Color.parseColor("#4C1D95"), Color.parseColor("#9333EA")});
                    bg.setStroke(3, Color.parseColor("#C084FC"));
                    symbol = "💠";
                }
                break;
            case 3:
                if (state == 0) {
                    bg.setColors(new int[]{Color.parseColor("#022C22"), Color.parseColor("#047857")});
                    bg.setStroke(3, Color.parseColor("#34D399"));
                    symbol = "🧬";
                } else if (state == 1) {
                    bg.setColors(new int[]{Color.parseColor("#064E3B"), Color.parseColor("#059669")});
                    bg.setStroke(3, Color.parseColor("#6EE7B7"));
                    symbol = "🧪";
                } else {
                    bg.setColors(new int[]{Color.parseColor("#14532D"), Color.parseColor("#16A34A")});
                    bg.setStroke(3, Color.parseColor("#86EFAC"));
                    symbol = "⚡";
                }
                break;
            case 4:
                if (state == 0) {
                    bg.setColors(new int[]{Color.parseColor("#450A0A"), Color.parseColor("#B91C1C")});
                    bg.setStroke(3, Color.parseColor("#F87171"));
                    symbol = "🔥";
                } else if (state == 1) {
                    bg.setColors(new int[]{Color.parseColor("#7C2D12"), Color.parseColor("#C2410C")});
                    bg.setStroke(3, Color.parseColor("#FB923C"));
                    symbol = "💫";
                } else {
                    bg.setColors(new int[]{Color.parseColor("#78350F"), Color.parseColor("#D97706")});
                    bg.setStroke(3, Color.parseColor("#FBBF24"));
                    symbol = "✨";
                }
                break;
            case 5:
                if (state == 0) {
                    bg.setColors(new int[]{Color.parseColor("#312E81"), Color.parseColor("#4338CA")});
                    bg.setStroke(3, Color.parseColor("#818CF8"));
                    symbol = "💎";
                } else if (state == 1) {
                    bg.setColors(new int[]{Color.parseColor("#831843"), Color.parseColor("#BE185D")});
                    bg.setStroke(3, Color.parseColor("#F472B6"));
                    symbol = "🔮";
                } else if (state == 2) {
                    bg.setColors(new int[]{Color.parseColor("#134E4A"), Color.parseColor("#0F766E")});
                    bg.setStroke(3, Color.parseColor("#2DD4BF"));
                    symbol = "🌀";
                } else {
                    bg.setColors(new int[]{Color.parseColor("#3F3F46"), Color.parseColor("#71717A")});
                    bg.setStroke(3, Color.parseColor("#A1A1AA"));
                    symbol = "⚛";
                }
                break;
            case 6:
                if (state == 0) {
                    bg.setColors(new int[]{Color.parseColor("#581C87"), Color.parseColor("#7E22CE")});
                    bg.setStroke(3, Color.parseColor("#D8B4FE"));
                    symbol = "⭐";
                } else if (state == 1) {
                    bg.setColors(new int[]{Color.parseColor("#065F46"), Color.parseColor("#059669")});
                    bg.setStroke(3, Color.parseColor("#A7F3D0"));
                    symbol = "🌟";
                } else if (state == 2) {
                    bg.setColors(new int[]{Color.parseColor("#991B1B"), Color.parseColor("#DC2626")});
                    bg.setStroke(3, Color.parseColor("#FCA5A5"));
                    symbol = "💫";
                } else {
                    bg.setColors(new int[]{Color.parseColor("#1E3A8A"), Color.parseColor("#3B82F6")});
                    bg.setStroke(3, Color.parseColor("#BFDBFE"));
                    symbol = "✨";
                }
                break;
        }

        cell.setBackground(bg);
        cell.setText(symbol);
        cell.setTextColor(Color.WHITE);
    }

    private boolean checkVictoryCondition() {
        int firstState = states[0][0];
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                if (states[i][j] != firstState) return false;
            }
        }
        return true;
    }

    private void triggerExplosionGameOver() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("💥 КРАХ ЯДРА! ПЕРЕГРУЗКА!");
        builder.setMessage("Лимит ходов исчерпан. Квантовая матрица взорвалась!\n\nХочешь возобновить игру и попробовать снова?");
        builder.setPositiveButton("ВОЗОБНИВИТЬ ИГРУ 🔄", (dialog, which) -> {
            loadLevel(currentLevel);
        });
        builder.setNegativeButton("Выход ❌", (dialog, which) -> finishAndRemoveTask());
        builder.show();
    }

    private void calculateScoreAndAdvance() {
        int levelBonus = Math.max(150 - (movesOnLevel * 5), 40) * currentLevel;
        totalScore += levelBonus;
        scoreTextView.setText("ОЧКИ: " + totalScore);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (currentLevel < 6) {
            builder.setTitle("⚡ СЕКТОР " + currentLevel + " СТАБИЛИЗИРОВАН!");
            builder.setMessage("Уровень пройден за " + movesOnLevel + " ходов.\nНабрано очков: +" + levelBonus + "\n\nПереходим в следующий квантовый сектор?");
            builder.setPositiveButton("Следующий сектор 🚀", (dialog, which) -> {
                currentLevel++;
                loadLevel(currentLevel);
            });
        } else {
            builder.setTitle("🏆 ЯДРО ПОЛНОСТЬЮ СКОМПИЛИРОВАНО!");
            builder.setMessage("ЛЕГЕНДАРНО! Все 6 уровней пройдены.\nИтоговый счет: " + totalScore + " очков.\n\nВердикт Профессора: Мозг напарника работает на квантовой частоте! ☕ С него заслуженный кофе за шедевр!");
            builder.setPositiveButton("Пройти заново 🔄", (dialog, which) -> {
                currentLevel = 1;
                totalScore = 0;
                scoreTextView.setText("ОЧКИ: 0");
                loadLevel(currentLevel);
            });
            builder.setNegativeButton("Выход ❌", (dialog, which) -> finishAndRemoveTask());
        }
        builder.show();
    }
}