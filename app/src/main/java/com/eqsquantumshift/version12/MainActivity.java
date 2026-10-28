package com.eqsquantumshift.version12;

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
import android.view.animation.LinearInterpolator;
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
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

public class MainActivity extends Activity {

    // --- ИГРОВЫЕ ПЕРЕМЕННЫЕ ---
    private int currentLevel = 1;
    private int totalScore = 0;
    private int movesOnLevel = 0;
    private int maxAllowedMoves = 10;
    private double difficultyMultiplier = 2.0;
    
    private int gridCols = 3;
    private int gridRows = 3;
    private int maxStates = 2;

    private TextView[][] cellViews;
    private int[][] states;
    
    // --- ПЕРЕМЕННЫЕ НАСТРОЙКИ АНИМАЦИЙ ---
    private float animRotationMax = 15f;      
    private int animRotateDuration = 1200;    
    private float animBreathMin = 0.90f;      
    private float animBreathMax = 1.10f;      
    private int animBreathDuration = 900;     
    private int rgbAnimDuration = 3000;       
    private float explosionIntensity = 1.0f;  

    // --- UI КОМПОНЕНТЫ ---
    private TextView levelTextView, scoreTextView, movesTextView, titleTextView, logoView, clockTextView, instructionView;
    private Button btnExit, btnHintBottom, btnAnimSettings;
    private GridLayout gridLayout;
    private LinearLayout rootLayout;
    
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;
    private ValueAnimator rgbAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new LinearLayout(this);
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
        titleTextView.setTextSize(12);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleTextView.setLayoutParams(titleParams);

        // Кнопка открытия МЕНЮ НАСТРОЙКИ АНИМАЦИЙ (🎨)
        btnAnimSettings = new Button(this);
        btnAnimSettings.setText("🎨");
        btnAnimSettings.setTextSize(12);
        btnAnimSettings.setPadding(8, 2, 8, 2);
        GradientDrawable animBg = new GradientDrawable();
        animBg.setColor(Color.parseColor("#0F282A"));
        animBg.setCornerRadius(10f);
        animBg.setStroke(2, Color.parseColor("#14B8A6"));
        btnAnimSettings.setBackground(animBg);
        btnAnimSettings.setOnClickListener(v -> showAnimationSettingsDialog());

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

        btnExit = new Button(this);
        btnExit.setText("❌");
        btnExit.setTextSize(12);
        btnExit.setPadding(8, 2, 8, 2);
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.parseColor("#270F19"));
        exitBg.setCornerRadius(10f);
        exitBg.setStroke(2, Color.parseColor("#E11D48"));
        btnExit.setBackground(exitBg);

        LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        marginParams.setMargins(6, 0, 0, 0);
        
        btnAnimSettings.setLayoutParams(marginParams);
        logoView.setLayoutParams(marginParams);
        btnExit.setLayoutParams(marginParams);
        btnExit.setOnClickListener(v -> finishAndRemoveTask());

        headerLayout.addView(titleTextView);
        headerLayout.addView(btnAnimSettings);
        headerLayout.addView(logoView);
        headerLayout.addView(btnExit);
        rootLayout.addView(headerLayout);

        // --- КВАНТОВЫЕ ЧАСЫ ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(14);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(12, 10, 12, 10);

        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        clockBg.setColors(new int[]{Color.parseColor("#064E3B"), Color.parseColor("#022C22"), Color.parseColor("#064E3B")});
        clockBg.setCornerRadius(14f);
        clockBg.setStroke(3, Color.parseColor("#10B981"));
        clockTextView.setBackground(clockBg);

        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 4, 0, 8);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);
        startClockAnimation();
        
        clockTextView.setOnClickListener(v -> showSettingsMenuDialog());

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

        // --- НИЖНЯЯ ПАНЕЛЬ ---
        LinearLayout bottomLayout = new LinearLayout(this);
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        bottomLayout.setGravity(Gravity.CENTER);
        bottomLayout.setPadding(0, 6, 0, 0);

        btnHintBottom = new Button(this);
        btnHintBottom.setText("💡 РАСЧЕТ ИДЕАЛЬНОГО ХОДА...");
        btnHintBottom.setTextColor(Color.WHITE);
        btnHintBottom.setTextSize(12);
        btnHintBottom.setTypeface(Typeface.DEFAULT_BOLD);
        btnHintBottom.setPadding(16, 10, 16, 10);
        
        GradientDrawable hintBtnBg = new GradientDrawable();
        hintBtnBg.setCornerRadius(14f);
        btnHintBottom.setBackground(hintBtnBg);

        initRgbAnimator(hintBtnBg);

        LinearLayout.LayoutParams hintBtnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintBtnParams.setMargins(0, 0, 0, 6);
        btnHintBottom.setLayoutParams(hintBtnParams);
        btnHintBottom.setOnClickListener(v -> useMathematicalHintInstant());
        bottomLayout.addView(btnHintBottom);

        instructionView = new TextView(this);
        instructionView.setText("📖 Нажми 🎨 сверху для настройки анимаций, а часы — для лимита ходов!");
        instructionView.setTextColor(Color.parseColor("#64748B"));
        instructionView.setTextSize(9);
        instructionView.setGravity(Gravity.CENTER);
        bottomLayout.addView(instructionView);

        rootLayout.addView(bottomLayout);

        setContentView(rootLayout);
        loadLevel(currentLevel);
    }

    private void initRgbAnimator(GradientDrawable hintBtnBg) {
        if (rgbAnimator != null) {
            rgbAnimator.cancel();
        }
        rgbAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), 
                Color.parseColor("#2563EB"), Color.parseColor("#9333EA"), 
                Color.parseColor("#059669"), Color.parseColor("#D97706"), Color.parseColor("#2563EB"));
        rgbAnimator.setDuration(rgbAnimDuration); 
        rgbAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rgbAnimator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            hintBtnBg.setColor(color);
            hintBtnBg.setStroke(2, Color.WHITE);
        });
        rgbAnimator.start();
    }

    private void showAnimationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎨 Лаборатория анимаций и эффектов");

        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(30, 20, 30, 20);

        TextView descText = new TextView(this);
        descText.setText("Экспериментируйте со скоростью и амплитудой живых клеток и эффектов в реальном времени!");
        descText.setTextSize(12);
        descText.setTextColor(Color.parseColor("#94A3B8"));
        descText.setPadding(0, 0, 0, 20);
        menuLayout.addView(descText);

        Button btnRot = new Button(this);
        updateRotText(btnRot);
        btnRot.setOnClickListener(v -> {
            animRotationMax += 5f;
            if (animRotationMax > 35f) animRotationMax = 0f; 
            updateRotText(btnRot);
            buildGridUI(); 
        });
        menuLayout.addView(btnRot);

        Button btnSpeed = new Button(this);
        updateSpeedText(btnSpeed);
        btnSpeed.setOnClickListener(v -> {
            if (animRotateDuration > 500) {
                animRotateDuration -= 300;
                animBreathDuration -= 200;
            } else {
                animRotateDuration = 2000;
                animBreathDuration = 1500;
            }
            updateSpeedText(btnSpeed);
            buildGridUI();
        });
        menuLayout.addView(btnSpeed);

        Button btnRgb = new Button(this);
        updateRgbText(btnRgb);
        btnRgb.setOnClickListener(v -> {
            rgbAnimDuration -= 1000;
            if (rgbAnimDuration < 1000) rgbAnimDuration = 5000;
            updateRgbText(btnRgb);
            GradientDrawable hintBtnBg = (GradientDrawable) btnHintBottom.getBackground();
            initRgbAnimator(hintBtnBg);
        });
        menuLayout.addView(btnRgb);

        Button btnExp = new Button(this);
        updateExpText(btnExp);
        btnExp.setOnClickListener(v -> {
            explosionIntensity += 0.5f;
            if (explosionIntensity > 3.0f) explosionIntensity = 0.5f;
            updateExpText(btnExp);
        });
        menuLayout.addView(btnExp);

        builder.setView(menuLayout);
        builder.setPositiveButton("Применить и закрыть 🚀", (dialog, which) -> {
            Toast.makeText(this, "Анимации обновлены!", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void updateRotText(Button btn) {
        btn.setText("🌀 Угол поворота клеток: ±" + (int)animRotationMax + "°");
    }
    private void updateSpeedText(Button btn) {
        btn.setText("⚡ Скорость анимации: " + animRotateDuration + " мс");
    }
    private void updateRgbText(Button btn) {
        btn.setText("🌈 Скорость RGB кнопки: " + (rgbAnimDuration/1000.0) + " сек");
    }
    private void updateExpText(Button btn) {
        btn.setText("💥 Сила тряски взрыва: " + explosionIntensity + "x");
    }

    private void showSettingsMenuDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Настройка лимита ходов");
        
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(40, 20, 40, 20);

        TextView infoText = new TextView(this);
        infoText.setText("Коэффициент лимита ходов: " + difficultyMultiplier + "x\nФормула: Мин. ходы комбинации * Коэффициент");
        infoText.setTextSize(13);
        infoText.setPadding(0, 0, 0, 30);
        menuLayout.addView(infoText);

        LinearLayout rowButtons = new LinearLayout(this);
        rowButtons.setOrientation(LinearLayout.HORIZONTAL);
        rowButtons.setGravity(Gravity.CENTER);

        Button btnMinus = new Button(this);
        btnMinus.setText("➖ Проще");
        btnMinus.setOnClickListener(v -> {
            if (difficultyMultiplier > 1.0) {
                difficultyMultiplier = Math.round((difficultyMultiplier - 0.2) * 10.0) / 10.0;
                infoText.setText("Коэффициент лимита ходов: " + difficultyMultiplier + "x\nФормула: Мин. ходы комбинации * Коэффициент");
                recalculateMaxMovesFromCurrentState();
            }
        });

        Button btnPlus = new Button(this);
        btnPlus.setText("➕ Сложнее");
        btnPlus.setOnClickListener(v -> {
            if (difficultyMultiplier < 5.0) {
                difficultyMultiplier = Math.round((difficultyMultiplier + 0.2) * 10.0) / 10.0;
                infoText.setText("Коэффициент лимита ходов: " + difficultyMultiplier + "x\nФормула: Мин. ходы комбинации * Коэффициент");
                recalculateMaxMovesFromCurrentState();
            }
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnMinus.setLayoutParams(btnParams);
        btnPlus.setLayoutParams(btnParams);
        rowButtons.addView(btnMinus);
        rowButtons.addView(btnPlus);
        menuLayout.addView(rowButtons);

        builder.setView(menuLayout);
        builder.setPositiveButton("Готово 🔐", null);
        builder.show();
    }

    private void startClockAnimation() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String timeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("💠 [ " + timeOnly + " ] 💠\n⚙️ НАСТРОЙКА ХОДОВ");
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

    private void startLivingCellAnimation(final View cell) {
        if (animRotationMax > 0f) {
            ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(cell, "rotation", -animRotationMax, animRotationMax);
            rotateAnim.setDuration(animRotateDuration + new Random().nextInt(300));
            rotateAnim.setRepeatMode(ValueAnimator.REVERSE);
            rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnim.setInterpolator(new LinearInterpolator());
            rotateAnim.start();
        }

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cell, "scaleX", animBreathMin, animBreathMax);
        scaleX.setDuration(animBreathDuration + new Random().nextInt(200));
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cell, "scaleY", animBreathMin, animBreathMax);
        scaleY.setDuration(animBreathDuration + new Random().nextInt(200));
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet breathingSet = new AnimatorSet();
        breathingSet.playTogether(scaleX, scaleY);
        breathingSet.start();
    }

    private void recalculateMaxMovesFromCurrentState() {
        int exactMinMoves = countRemainingMovesToVictory();
        if (exactMinMoves <= 0) exactMinMoves = 2;
        maxAllowedMoves = (int) Math.ceil(exactMinMoves * difficultyMultiplier);
        if (maxAllowedMoves < exactMinMoves) maxAllowedMoves = exactMinMoves + 1;
        updateMovesDisplay();
    }

    private void useMathematicalHintInstant() {
        movesOnLevel += 2;
        updateMovesDisplay();

        if (movesOnLevel > maxAllowedMoves) {
            triggerVisualExplosion();
            return;
        }

        int[] bestMove = calculateOptimalHintMove();
        if (bestMove != null) {
            triggerQuantumWave(bestMove[0], bestMove[1]);
            updateHintButtonText();
            
            if (checkVictoryCondition()) {
                calculateScoreAndAdvance();
            }
        }
    }

    private int[] calculateOptimalHintMove() {
        class Node {
            int[][] board;
            int firstR, firstC;
            int stepsFromStart;
            Node(int[][] b, int r, int c, int steps) {
                board = cloneBoard(b);
                firstR = r;
                firstC = c;
                stepsFromStart = steps;
            }
        }

        if (isUniform(states)) return null;

        Queue<Node> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int[][] nextBoard = cloneBoard(states);
                applyWaveToBoard(nextBoard, r, c);
                if (isUniform(nextBoard)) {
                    return new int[]{r, c};
                }
                queue.add(new Node(nextBoard, r, c, 1));
                visited.add(boardToString(nextBoard));
            }
        }

        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            int depthLimit = (gridRows >= 5) ? 3 : 5;
            if (curr.stepsFromStart >= depthLimit) continue;

            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    int[][] nextBoard = cloneBoard(curr.board);
                    applyWaveToBoard(nextBoard, r, c);

                    if (isUniform(nextBoard)) {
                        return new int[]{curr.firstR, curr.firstC};
                    }

                    String key = boardToString(nextBoard);
                    if (!visited.contains(key) && visited.size() < 3000) {
                        visited.add(key);
                        queue.add(new Node(nextBoard, curr.firstR, curr.firstC, curr.stepsFromStart + 1));
                    }
                }
            }
        }

        int bestR = 0, bestC = 0, minMismatch = 999;
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int mismatch = countMismatchesAfterSimulatedClick(r, c);
                if (mismatch < minMismatch) {
                    minMismatch = mismatch;
                    bestR = r;
                    bestC = c;
                }
            }
        }
        return new int[]{bestR, bestC};
    }

    private int countRemainingMovesToVictory() {
        class SearchNode {
            int[][] board;
            int steps;
            SearchNode(int[][] b, int s) {
                board = cloneBoard(b);
                steps = s;
            }
        }
        if (isUniform(states)) return 0;

        Queue<SearchNode> q = new LinkedList<>();
        Set<String> seen = new HashSet<>();

        q.add(new SearchNode(states, 0));
        seen.add(boardToString(states));

        while (!q.isEmpty()) {
            SearchNode curr = q.poll();
            int depthLimit = (gridRows >= 5) ? 4 : 6;
            if (curr.steps >= depthLimit) break; 

            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    int[][] nextB = cloneBoard(curr.board);
                    applyWaveToBoard(nextB, r, c);
                    if (isUniform(nextB)) {
                        return curr.steps + 1;
                    }
                    String key = boardToString(nextB);
                    if (!seen.contains(key) && seen.size() < 2000) {
                        seen.add(key);
                        q.add(new SearchNode(nextB, curr.steps + 1));
                    }
                }
            }
        }
        return (gridCols == 3) ? 3 : 5;
    }

    private void updateHintButtonText() {
        int rem = countRemainingMovesToVictory();
        if (rem > 0) {
            btnHintBottom.setText("💡 ХОД (ДО ПОБЕДЫ: " + rem + " ШАГ.)");
        } else {
            btnHintBottom.setText("💡 СДЕЛАТЬ ОПТИМАЛЬНЫЙ КВАНТОВЫЙ ХОД");
        }
    }

    private boolean isUniform(int[][] b) {
        int first = b[0][0];
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                if (b[i][j] != first) return false;
            }
        }
        return true;
    }

    private int[][] cloneBoard(int[][] b) {
        int[][] copy = new int[gridRows][gridCols];
        for (int i = 0; i < gridRows; i++) {
            System.arraycopy(b[i], 0, copy[i], 0, gridCols);
        }
        return copy;
    }

    private String boardToString(int[][] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                sb.append(b[i][j]);
            }
        }
        return sb.toString();
    }

    private void applyWaveToBoard(int[][] s, int r, int c) {
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

    private int countMismatchesAfterSimulatedClick(int r, int c) {
        int[][] tempStates = cloneBoard(states);
        applyWaveToBoard(tempStates, r, c);
        int target = tempStates[0][0];
        int count = 0;
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                if (tempStates[i][j] != target) count++;
            }
        }
        return count;
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

        cellViews = new TextView[gridRows][gridCols];
        states = new int[gridRows][gridCols];

        initQuantumField();

        int exactMinMoves = countRemainingMovesToVictory();
        if (exactMinMoves <= 0) exactMinMoves = 3;
        
        maxAllowedMoves = (int) Math.ceil(exactMinMoves * difficultyMultiplier);

        levelTextView.setText("УР: " + level + "/6");
        updateMovesDisplay();

        buildGridUI();
        updateHintButtonText();
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
                cell.setTextSize(22);

                updateCellStyle(cell, states[i][j], currentLevel);
                startLivingCellAnimation(cell); 

                cell.setOnClickListener(v -> {
                    triggerQuantumWave(row, col);
                    movesOnLevel++;
                    
                    recalculateMaxMovesFromCurrentState();
                    updateHintButtonText();

                    if (movesOnLevel > maxAllowedMoves) {
                        triggerVisualExplosion();
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
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cell, "scaleX", 0.85f, 1.15f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cell, "scaleY", 0.85f, 1.15f, 1.0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(300); 
        animatorSet.setInterpolator(new OvershootInterpolator(1.3f));
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

    private void triggerVisualExplosion() {
        float shakeDist = 40f * explosionIntensity;
        ObjectAnimator shake = ObjectAnimator.ofFloat(gridLayout, "translationX", 0f, shakeDist, -shakeDist, shakeDist*0.75f, -shakeDist*0.75f, shakeDist*0.4f, -shakeDist*0.4f, 0f);
        shake.setDuration(1000);

        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), 
                Color.parseColor("#040711"), Color.parseColor("#991B1B"), Color.parseColor("#450A0A"), Color.parseColor("#040711"));
        colorAnim.setDuration(1000);
        colorAnim.addUpdateListener(anim -> rootLayout.setBackgroundColor((int) anim.getAnimatedValue()));

        AnimatorSet explosionSet = new AnimatorSet();
        explosionSet.playTogether(shake, colorAnim);
        explosionSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                showExplosionGameOverDialog();
            }
        });
        explosionSet.start();
    }

    private void showExplosionGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("💥 КРАХ ЯДРА! ПЕРЕГРУЗКА!");
        builder.setMessage("Лимит ходов исчерпан. Квантовая матрица взорвалась!\n\nХочешь возобновить игру и попробовать снова?");
        builder.setPositiveButton("ВОЗОБНОВИТЬ ИГРУ 🔄", (dialog, which) -> loadLevel(currentLevel));
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
            builder.setMessage("ЛЕГЕНДАРНО! Все 6 уровней пройдены.\nИтоговый счет: " + totalScore + " очков.");
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

    private boolean checkVictoryCondition() {
        int firstState = states[0][0];
        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                if (states[i][j] != firstState) return false;
            }
        }
        return true;
    }
}