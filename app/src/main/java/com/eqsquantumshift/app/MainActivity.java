package com.eqsquantumshift.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private int currentLevel = 1;
    private int totalScore = 0;
    private int movesOnLevel = 0;
    
    private int gridCols = 3;
    private int gridRows = 3;
    private int maxStates = 2;

    private TextView[][] cellViews;
    private int[][] states;
    
    private TextView levelTextView, scoreTextView, movesTextView, titleTextView, logoView, clockTextView;
    private Button btnExit;
    private GridLayout gridLayout;
    
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер (Космический черный фон)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#040711"));
        rootLayout.setPadding(20, 20, 20, 20);

        // --- ШАПКА ПРИЛОЖЕНИЯ (Заголовок, DRV_2026 и Кнопка ВЫХОД) ---
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 4, 0, 10);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        titleTextView = new TextView(this);
        titleTextView.setText("ENTROPY\nQUANTUM SHIFT");
        titleTextView.setTextColor(Color.parseColor("#38BDF8"));
        titleTextView.setTextSize(15);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        titleTextView.setLineSpacing(0f, 0.9f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleTextView.setLayoutParams(titleParams);

        logoView = new TextView(this);
        logoView.setText("DRV_2026");
        logoView.setTextColor(Color.parseColor("#A855F7"));
        logoView.setTextSize(11);
        logoView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        logoView.setPadding(10, 4, 10, 4);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(Color.parseColor("#1E1035"));
        logoBg.setCornerRadius(10f);
        logoBg.setStroke(2, Color.parseColor("#7E22CE"));
        logoView.setBackground(logoBg);

        btnExit = new Button(this);
        btnExit.setText("❌ ВЫХОД");
        btnExit.setTextColor(Color.parseColor("#F43F5E"));
        btnExit.setTextSize(11);
        btnExit.setTypeface(Typeface.DEFAULT_BOLD);
        btnExit.setPadding(14, 4, 14, 4);
        
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.parseColor("#270F19"));
        exitBg.setCornerRadius(12f);
        exitBg.setStroke(2, Color.parseColor("#E11D48"));
        btnExit.setBackground(exitBg);
        
        LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        exitParams.setMargins(10, 0, 0, 0);
        btnExit.setLayoutParams(exitParams);

        btnExit.setOnClickListener(v -> finishAndRemoveTask());

        headerLayout.addView(titleTextView);
        headerLayout.addView(logoView);
        headerLayout.addView(btnExit);
        rootLayout.addView(headerLayout);

        // --- 🕒 СТИЛЬНЫЕ АНИМИРОВАННЫЕ КВАНТОВЫЕ ЧАСЫ (CHRONOMETER) ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(15);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(12, 10, 12, 10);

        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColor(Color.parseColor("#022C22"));
        clockBg.setCornerRadius(14f);
        clockBg.setStroke(2, Color.parseColor("#059669"));
        clockTextView.setBackground(clockBg);

        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 0, 0, 12);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);

        startClockAnimation();

        // --- ИНФОРМАЦИОННАЯ ПАНЕЛЬ (Уровень, Очки, Ходы) ---
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setPadding(10, 12, 10, 12);
        
        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setColor(Color.parseColor("#0B1120"));
        statsBg.setCornerRadius(16f);
        statsBg.setStroke(2, Color.parseColor("#1E293B"));
        statsLayout.setBackground(statsBg);

        levelTextView = createStatText("УР: 1/4", "#38BDF8");
        scoreTextView = createStatText("ОЧКИ: 0", "#F59E0B");
        movesTextView = createStatText("ХОДЫ: 0", "#10B981");

        statsLayout.addView(levelTextView);
        statsLayout.addView(scoreTextView);
        statsLayout.addView(movesTextView);
        rootLayout.addView(statsLayout);

        // --- ИГРОВАЯ СЕТКА ---
        gridLayout = new GridLayout(this);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        gridParams.setMargins(0, 16, 0, 0);
        gridLayout.setLayoutParams(gridParams);
        rootLayout.addView(gridLayout);

        setContentView(rootLayout);
        loadLevel(currentLevel);
    }

    private void startClockAnimation() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String timeOnly = new SimpleDateFormat("HH : mm : ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("⚡ TIME: " + timeOnly + " ⚡");

                ObjectAnimator pulseX = ObjectAnimator.ofFloat(clockTextView, "scaleX", 1.0f, 1.02f, 1.0f);
                ObjectAnimator pulseY = ObjectAnimator.ofFloat(clockTextView, "scaleY", 1.0f, 1.02f, 1.0f);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(pulseX, pulseY);
                set.setDuration(600);
                set.start();

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
    }

    private TextView createStatText(String text, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        return tv;
    }

    private void loadLevel(int level) {
        movesOnLevel = 0;
        
        switch (level) {
            case 1:
                gridCols = 3; gridRows = 3; maxStates = 2; break;
            case 2:
                gridCols = 4; gridRows = 4; maxStates = 2; break;
            case 3:
                gridCols = 4; gridRows = 4; maxStates = 3; break;
            case 4:
                gridCols = 5; gridRows = 5; maxStates = 3; break;
        }

        levelTextView.setText("УР: " + level + "/4");
        movesTextView.setText("ХОДЫ: 0");

        cellViews = new TextView[gridRows][gridCols];
        states = new int[gridRows][gridCols];
        
        initQuantumField();
        buildGridUI();
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

        int screenWidth = getResources().getDisplayMetrics().widthPixels - 60;
        int cellSize = screenWidth / gridCols - 10;

        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                final int row = i;
                final int col = j;

                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(5, 5, 5, 5);
                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(22);
                cell.setTypeface(Typeface.DEFAULT_BOLD);

                updateCellStyle(cell, states[i][j]);

                cell.setOnClickListener(v -> {
                    triggerQuantumWave(row, col);
                    movesOnLevel++;
                    movesTextView.setText("ХОДЫ: " + movesOnLevel);

                    if (checkVictoryCondition()) {
                        calculateScoreAndAdvance();
                    }
                });

                cellViews[i][j] = cell;
                gridLayout.addView(cell);
            }
        }
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
        animateCellFancy(cellViews[r][c], states[r][c]);
    }

    private void animateCellFancy(TextView cell, int newState) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cell, "scaleX", 0.7f, 1.15f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cell, "scaleY", 0.7f, 1.15f, 1.0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(cell, "rotation", 0f, 45f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, rotation);
        animatorSet.setDuration(280);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();

        updateCellStyle(cell, newState);
    }

    private void updateCellStyle(TextView cell, int state) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(18f);

        if (state == 0) {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{Color.parseColor("#0F172A"), Color.parseColor("#0284C7")});
            bg.setStroke(3, Color.parseColor("#38BDF8"));
            cell.setTextColor(Color.parseColor("#E0F2FE"));
            cell.setText("⚛");
        } else if (state == 1) {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{Color.parseColor("#2E1065"), Color.parseColor("#7E22CE")});
            bg.setStroke(3, Color.parseColor("#C084FC"));
            cell.setTextColor(Color.parseColor("#F3E8FF"));
            cell.setText("🌀");
        } else {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{Color.parseColor("#064E3B"), Color.parseColor("#059669")});
            bg.setStroke(3, Color.parseColor("#34D399"));
            cell.setTextColor(Color.parseColor("#ECFDF5"));
            cell.setText("⚡");
        }
        cell.setBackground(bg);
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

    private void calculateScoreAndAdvance() {
        int levelBonus = Math.max(120 - (movesOnLevel * 6), 30) * currentLevel;
        totalScore += levelBonus;
        scoreTextView.setText("ОЧКИ: " + totalScore);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (currentLevel < 4) {
            builder.setTitle("⚡ СЕКТОР СТАБИЛИЗИРОВАН!");
            builder.setMessage("Сектор " + currentLevel + " пройден за " + movesOnLevel + " ходов.\nНабрано очков: +" + levelBonus + "\n\nПерейти к следующей матрице?");
            builder.setPositiveButton("Дальше 🚀", (dialog, which) -> {
                currentLevel++;
                loadLevel(currentLevel);
            });
        } else {
            builder.setTitle("🏆 КВАНТОВОЕ ЯДРО СКОМПИЛИРОВАНО!");
            builder.setMessage("Потрясающе! Все 4 сектора стабилизированы.\nИтоговый результат: " + totalScore + " очков.\n\nВердикт: Мозг напарника выдержал перегрузку! ☕ С него кофе за успешную пусконаладку!");
            builder.setPositiveButton("Заново 🔄", (dialog, which) -> {
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