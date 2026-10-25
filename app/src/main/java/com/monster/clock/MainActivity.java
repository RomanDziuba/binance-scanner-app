package com.monster.clock;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int currentLevel = 1;
    private int totalScore = 0;
    private int movesOnLevel = 0;
    
    private int gridCols = 3;
    private int gridRows = 3;
    private int maxStates = 2;

    private TextView[][] cellViews;
    private int[][] states;
    
    private TextView levelTextView, scoreTextView, movesTextView, titleTextView, logoView;
    private GridLayout gridLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#060913"));
        rootLayout.setPadding(32, 32, 32, 32);

        // --- ШАПКА ПРИЛОЖЕНИЯ С ЛОГОТИПОМ DRV_2026 ---
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        titleTextView = new TextView(this);
        titleTextView.setText("EQS: QUANTUM SHIFT");
        titleTextView.setTextColor(Color.parseColor("#38BDF8"));
        titleTextView.setTextSize(18);
        titleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleTextView.setLayoutParams(titleParams);

        logoView = new TextView(this);
        logoView.setText("DRV_2026");
        logoView.setTextColor(Color.parseColor("#64748B"));
        logoView.setTextSize(12);
        logoView.setTypeface(null, android.graphics.Typeface.BOLD);
        logoView.setPadding(12, 6, 12, 6);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(Color.parseColor("#1E293B"));
        logoBg.setCornerRadius(12f);
        logoView.setBackground(logoBg);

        headerLayout.addView(titleTextView);
        headerLayout.addView(logoView);
        rootLayout.addView(headerLayout);

        // --- ИНФОРМАЦИОННАЯ ПАНЕЛЬ ---
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setPadding(0, 24, 0, 24);
        statsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        levelTextView = createStatText("Уровень: 1/4");
        scoreTextView = createStatText("Очки: 0");
        movesTextView = createStatText("Ходы: 0");

        statsLayout.addView(levelTextView);
        statsLayout.addView(scoreTextView);
        statsLayout.addView(movesTextView);
        rootLayout.addView(statsLayout);

        // --- ИГРОВАЯ СЕТКА ---
        gridLayout = new GridLayout(this);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        gridLayout.setLayoutParams(gridParams);
        rootLayout.addView(gridLayout);

        setContentView(rootLayout);

        loadLevel(currentLevel);
    }

    private TextView createStatText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(14);
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

        levelTextView.setText("Уровень: " + level + "/4");
        movesTextView.setText("Ходы: 0");

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

        int screenWidth = getResources().getDisplayMetrics().widthPixels - 64;
        int cellSize = screenWidth / gridCols - 16;

        for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
                final int row = i;
                final int col = j;

                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(8, 8, 8, 8);
                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(16);
                cell.setTypeface(null, android.graphics.Typeface.BOLD);

                updateCellStyle(cell, states[i][j]);

                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        triggerQuantumWave(row, col);
                        movesOnLevel++;
                        movesTextView.setText("Ходы: " + movesOnLevel);

                        if (checkVictoryCondition()) {
                            calculateScoreAndAdvance();
                        }
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
        animateCell(cellViews[r][c], states[r][c]);
    }

    private void animateCell(final TextView cell, int newState) {
        ValueAnimator anim = ValueAnimator.ofFloat(1f, 1.12f, 1f);
        anim.setDuration(200);
        
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = (Float) animation.getAnimatedValue();
                cell.setScaleX(scale);
                cell.setScaleY(scale);
            }
        });
        
        anim.start();
        updateCellStyle(cell, newState);
    }

    private void updateCellStyle(TextView cell, int state) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);

        if (state == 0) {
            bg.setColor(Color.parseColor("#0F172A"));
            bg.setStroke(2, Color.parseColor("#334155"));
            cell.setTextColor(Color.parseColor("#64748B"));
            cell.setText("0");
        } else if (state == 1) {
            bg.setColor(Color.parseColor("#1E1B4B"));
            bg.setStroke(2, Color.parseColor("#6366F1"));
            cell.setTextColor(Color.parseColor("#818CF8"));
            cell.setText("1");
        } else {
            bg.setColor(Color.parseColor("#064E3B"));
            bg.setStroke(2, Color.parseColor("#10B981"));
            cell.setTextColor(Color.parseColor("#34D399"));
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
        int levelBonus = Math.max(100 - (movesOnLevel * 5), 20) * currentLevel;
        totalScore += levelBonus;
        scoreTextView.setText("Очки: " + totalScore);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (currentLevel < 4) {
            builder.setTitle("⚡ СЕКТОР СТАБИЛИЗИРОВАН!");
            builder.setMessage("Уровень " + currentLevel + " пройден за " + movesOnLevel + " ходов.\nНабрано очков: +" + levelBonus + "\n\nГотов к переходу на следующий уровень сложности?");
            builder.setPositiveButton("Следующий сектор", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    currentLevel++;
                    loadLevel(currentLevel);
                }
            });
        } else {
            builder.setTitle("🏆 ЯДРО ПОЛНОСТЬЮ СКОМПИЛИРОВАНО!");
            builder.setMessage("Невероятно! Все 4 уровня пройдены.\nИтоговый счет: " + totalScore + " очков.\n\nВердикт системы: Интеллект напарника подтвержден на 100%. Но кофе за этот шедевр всё равно с него! 😉☕");
            builder.setPositiveButton("Пройти заново", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    currentLevel = 1;
                    totalScore = 0;
                    scoreTextView.setText("Очки: 0");
                    loadLevel(currentLevel);
                }
            });
        }
        builder.show();
    }
}