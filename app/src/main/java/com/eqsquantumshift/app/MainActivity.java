package com.eqsquantumshift.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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
    private Button btnExit, btnHint;
    private GridLayout gridLayout;
    
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#040711"));
        rootLayout.setPadding(16, 16, 16, 16);

        // --- ШАПКА ПРИЛОЖЕНИЯ ---
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 4, 0, 8);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        titleTextView = new TextView(this);
        titleTextView.setText("EQS: QUANTUM SHIFT");
        titleTextView.setTextColor(Color.parseColor("#38BDF8"));
        titleTextView.setTextSize(14);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleTextView.setLayoutParams(titleParams);

        logoView = new TextView(this);
        logoView.setText("DRV");
        logoView.setTextColor(Color.parseColor("#A855F7"));
        logoView.setTextSize(10);
        logoView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        logoView.setPadding(8, 4, 8, 4);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(Color.parseColor("#1E1035"));
        logoBg.setCornerRadius(8f);
        logoBg.setStroke(2, Color.parseColor("#7E22CE"));
        logoView.setBackground(logoBg);

        // Кнопка ПОДСКАЗКА (🤓)
        btnHint = new Button(this);
        btnHint.setText("🤓");
        btnHint.setTextSize(14);
        btnHint.setPadding(10, 2, 10, 2);
        GradientDrawable hintBg = new GradientDrawable();
        hintBg.setColor(Color.parseColor("#172554"));
        hintBg.setCornerRadius(10f);
        hintBg.setStroke(2, Color.parseColor("#3B82F6"));
        btnHint.setBackground(hintBg);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.setMargins(8, 0, 0, 0);
        btnHint.setLayoutParams(hintParams);
        btnHint.setOnClickListener(v -> showProfessorHint());

        // Кнопка ВЫХОД
        btnExit = new Button(this);
        btnExit.setText("❌");
        btnExit.setTextSize(14);
        btnExit.setPadding(10, 2, 10, 2);
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.parseColor("#270F19"));
        exitBg.setCornerRadius(10f);
        exitBg.setStroke(2, Color.parseColor("#E11D48"));
        btnExit.setBackground(exitBg);
        LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        exitParams.setMargins(8, 0, 0, 0);
        btnExit.setLayoutParams(exitParams);
        btnExit.setOnClickListener(v -> finishAndRemoveTask());

        headerLayout.addView(titleTextView);
        headerLayout.addView(logoView);
        headerLayout.addView(btnHint);
        headerLayout.addView(btnExit);
        rootLayout.addView(headerLayout);

        // --- КВАНТОВЫЕ ЧАСЫ ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(13);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(8, 6, 8, 6);

        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColor(Color.parseColor("#022C22"));
        clockBg.setCornerRadius(12f);
        clockBg.setStroke(2, Color.parseColor("#059669"));
        clockTextView.setBackground(clockBg);

        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 0, 0, 8);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);

        startClockAnimation();

        // --- ИНФО-ПАНЕЛЬ ---
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setPadding(8, 10, 8, 10);
        
        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setColor(Color.parseColor("#0B1120"));
        statsBg.setCornerRadius(14f);
        statsBg.setStroke(2, Color.parseColor("#1E293B"));
        statsLayout.setBackground(statsBg);

        levelTextView = createStatText("УР: 1/6", "#38BDF8");
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
        gridParams.setMargins(0, 12, 0, 0);
        gridLayout.setLayoutParams(gridParams);
        rootLayout.addView(gridLayout);

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

    private void showProfessorHint() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🤓 Совет Профессора ДРВ");
        builder.setMessage("Сектор " + currentLevel + " требует расчёта волнового резонанса!\n\n" +
                (currentLevel <= 2 ? "• Совет: На этом уровне волна распространяется по кресту (вверх, вниз, влево, вправо). Фокусируйтесь на углах!" :
                 currentLevel <= 4 ? "• Совет: Внимание! Здесь активирован диагональный резонанс. Каждое нажатие меняет угловые соседи." :
                 "• Совет: Ультимативный уровень! 4 квантовых состояния требуют строгой симметрии. Ищите центральные узлы сингулярности!"));
        builder.setPositiveButton("Понял 🚀", null);
        builder.show();
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

        levelTextView.setText("УР: " + level + "/6");
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
                cell.setTextSize(26); // Увеличенный размер смайлов

                updateCellStyle(cell, states[i][j], currentLevel);

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

        // Диагональный резонанс с уровня 3
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

    // 🎨 УНИКАЛЬНЫЕ ЦВЕТА И СМАЙЛЫ ДЛЯ КАЖДОГО УРОВНЯ
    private void updateCellStyle(TextView cell, int state, int level) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);

        String symbol = "⚛";
        
        switch (level) {
            case 1: // Космос (Синий / Голубой)
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
            case 2: // Пурпурный кибер
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
            case 3: // Изумрудный био-резонанс (3 состояния)
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
            case 4: // Плазменный огонь (3 состояния)
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
            case 5: // Кристальная матрица (4 состояния)
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
            case 6: // Ультимативное ядро (4 состояния)
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