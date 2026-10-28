package com.tetris.version1;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    // --- ИГРОВЫЕ ПЕРЕМЕННЫЕ ---
    private static final int GRID_WIDTH = 10;
    private static final int GRID_HEIGHT = 20;
    private int[][] board = new int[GRID_HEIGHT][GRID_WIDTH];

    private int score = 0;
    private int level = 1;
    private int linesClearedTotal = 0;
    private boolean isGameOver = false;
    private boolean isPaused = false;

    // Настройки геймплея
    private int startLevel = 1;
    private boolean showGhostPiece = true;
    private int colorThemeIndex = 0; // 0: Киберпанк, 1: Матрица, 2: Пурпур

    // Настройки анимации
    private float globalGlowIntensity = 1.0f;
    private int gameLoopFpsMs = 33; // ~30 FPS для плавной отрисовки

    // Активная фигура
    private int currentX = 3, currentY = 0;
    private int currentShapeIndex = 0;
    private int currentRotation = 0;
    private int[][] currentShape;

    // Тетромино (I, J, L, O, S, T, Z)
    private final int[][][] TETROMINOES = {
        // I
        {{0,0,0,0}, {1,1,1,1}, {0,0,0,0}, {0,0,0,0}},
        // J
        {{1,0,0}, {1,1,1}, {0,0,0}},
        // L
        {{0,0,1}, {1,1,1}, {0,0,0}},
        // O
        {{1,1}, {1,1}},
        // S
        {{0,1,1}, {1,1,0}, {0,0,0}},
        // T
        {{0,1,0}, {1,1,1}, {0,0,0}},
        // Z
        {{1,1,0}, {0,1,1}, {0,0,0}}
    };

    // Цветовые палитры тем
    private int[][] activeThemeColors = {
        {Color.parseColor("#0F172A"), Color.parseColor("#38BDF8"), Color.parseColor("#818CF8"), Color.parseColor("#60A5FA"), Color.parseColor("#0284C7")},
        {Color.parseColor("#022C22"), Color.parseColor("#34D399"), Color.parseColor("#10B981"), Color.parseColor("#6EE7B7"), Color.parseColor("#059669")},
        {Color.parseColor("#2E1065"), Color.parseColor("#C084FC"), Color.parseColor("#A855F7"), Color.parseColor("#E879F9"), Color.parseColor("#7E22CE")}
    };

    // UI Элементы
    private TextView scoreTextView, levelTextView, linesTextView, clockTextView, titleTextView;
    private TetrisView tetrisView;
    private Button btnAnimSettings, btnGameSettings, btnPause;

    // Обработчики циклов
    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    private Handler gameLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable gameLoopRunnable;
    private long lastDropTime = 0;
    private float animPulseTimer = 0f; // Для плавной анимации сияния

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#040711"));
        rootLayout.setPadding(12, 12, 12, 12);

        // --- ШАПКА ---
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 2, 0, 4);

        titleTextView = new TextView(this);
        titleTextView.setText("EQS: QUANTUM TETRIS");
        titleTextView.setTextColor(Color.parseColor("#38BDF8"));
        titleTextView.setTextSize(11);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleTextView.setLayoutParams(titleParams);

        btnAnimSettings = createTopIconButton("🎨", v -> showAnimationSettingsDialog());
        btnGameSettings = createTopIconButton("⚙️", v -> showGameSettingsDialog());
        btnPause = createTopIconButton("⏸️", v -> togglePause());

        headerLayout.addView(titleTextView);
        headerLayout.addView(btnAnimSettings);
        headerLayout.addView(btnGameSettings);
        headerLayout.addView(btnPause);
        rootLayout.addView(headerLayout);

        // --- КВАНТОВЫЕ ЧАСЫ ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(13);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(8, 8, 8, 8);
        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColors(new int[]{Color.parseColor("#064E3B"), Color.parseColor("#022C22")});
        clockBg.setCornerRadius(10f);
        clockBg.setStroke(2, Color.parseColor("#10B981"));
        clockTextView.setBackground(clockBg);
        
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 2, 0, 4);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);
        startClockAnimation();

        // --- ИНФО-ПАНЕЛЬ СТАТИСТИКИ ---
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setPadding(6, 6, 6, 6);
        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setColor(Color.parseColor("#0B1120"));
        statsBg.setCornerRadius(10f);
        statsBg.setStroke(2, Color.parseColor("#1E293B"));
        statsLayout.setBackground(statsBg);

        levelTextView = createStatText("УР: 1", "#38BDF8");
        scoreTextView = createStatText("ОЧКИ: 0", "#F59E0B");
        linesTextView = createStatText("ЛИНИИ: 0", "#10B981");

        statsLayout.addView(levelTextView);
        statsLayout.addView(scoreTextView);
        statsLayout.addView(linesTextView);
        rootLayout.addView(statsLayout);

        // --- ИГРОВОЕ ПОЛЕ (Кастомный View с плавным рендерингом) ---
        tetrisView = new TetrisView(this);
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        viewParams.setMargins(0, 6, 0, 6);
        tetrisView.setLayoutParams(viewParams);
        rootLayout.addView(tetrisView);

        // --- ПАНЕЛЬ УПРАВЛЕНИЯ (Кнопки) ---
        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);
        controlsLayout.setPadding(0, 2, 0, 2);

        Button btnLeft = createControlButton("⬅️");
        Button btnRotate = createControlButton("🔄");
        Button btnRight = createControlButton("➡️");
        Button btnDrop = createControlButton("⬇️ СБРОС");

        btnLeft.setOnClickListener(v -> movePiece(-1, 0));
        btnRight.setOnClickListener(v -> movePiece(1, 0));
        btnRotate.setOnClickListener(v -> rotatePiece());
        btnDrop.setOnClickListener(v -> hardDrop());

        controlsLayout.addView(btnLeft);
        controlsLayout.addView(btnRotate);
        controlsLayout.addView(btnRight);
        controlsLayout.addView(btnDrop);
        rootLayout.addView(controlsLayout);

        setContentView(rootLayout);

        // Запуск игры
        initNewGame();
        startGameLoop();
    }

    private Button createTopIconButton(String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setPadding(6, 2, 6, 2);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E1035"));
        bg.setCornerRadius(8f);
        bg.setStroke(2, Color.parseColor("#7E22CE"));
        btn.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(4, 0, 0, 0);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);
        return btn;
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

    private Button createControlButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B"));
        bg.setCornerRadius(12f);
        bg.setStroke(2, Color.parseColor("#38BDF8"));
        btn.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(4, 2, 4, 2);
        btn.setLayoutParams(params);
        return btn;
    }

    // --- ИГРОВАЯ ЛОГИКА ТЕТРИСА ---
    private void initNewGame() {
        board = new int[GRID_HEIGHT][GRID_WIDTH];
        score = 0;
        level = startLevel;
        linesClearedTotal = 0;
        isGameOver = false;
        isPaused = false;
        spawnNewPiece();
        updateStatsUI();
    }

    private void spawnNewPiece() {
        Random random = new Random();
        currentShapeIndex = random.nextInt(TETROMINOES.length);
        currentShape = TETROMINOES[currentShapeIndex];
        currentRotation = 0;
        currentX = GRID_WIDTH / 2 - currentShape[0].length / 2;
        currentY = 0;

        if (!checkCollision(currentX, currentY, currentShape)) {
            isGameOver = true;
            showGameOverDialog();
        }
    }

    private boolean checkCollision(int posX, int posY, int[][] shape) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int boardX = posX + c;
                    int boardY = posY + r;
                    if (boardX < 0 || boardX >= GRID_WIDTH || boardY >= GRID_HEIGHT) {
                        return false;
                    }
                    if (boardY >= 0 && board[boardY][boardX] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void mergePieceToBoard() {
        for (int r = 0; r < currentShape.length; r++) {
            for (int c = 0; c < currentShape[r].length; c++) {
                if (currentShape[r][c] != 0) {
                    int boardY = currentY + r;
                    int boardX = currentX + c;
                    if (boardY >= 0 && boardY < GRID_HEIGHT && boardX >= 0 && boardX < GRID_WIDTH) {
                        board[boardY][boardX] = currentShapeIndex + 1;
                    }
                }
            }
        }
        clearFullLines();
        spawnNewPiece();
    }

    private void clearFullLines() {
        int linesClearedNow = 0;
        for (int r = GRID_HEIGHT - 1; r >= 0; r--) {
            boolean isFull = true;
            for (int c = 0; c < GRID_WIDTH; c++) {
                if (board[r][c] == 0) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                linesClearedNow++;
                for (int moveR = r; moveR > 0; moveR--) {
                    System.arraycopy(board[moveR - 1], 0, board[moveR], 0, GRID_WIDTH);
                }
                for (int c = 0; c < GRID_WIDTH; c++) {
                    board[0][c] = 0;
                }
                r++; // Проверить эту же строку снова после смещения
            }
        }

        if (linesClearedNow > 0) {
            linesClearedTotal += linesClearedNow;
            score += linesClearedNow * 100 * level;
            level = startLevel + linesClearedTotal / 5; // Каждые 5 линий — новый уровень
            updateStatsUI();
        }
    }

    private void updateStatsUI() {
        levelTextView.setText("УР: " + level);
        scoreTextView.setText("ОЧКИ: " + score);
        linesTextView.setText("ЛИНИИ: " + linesClearedTotal);
    }

    private void movePiece(int dx, int dy) {
        if (isGameOver || isPaused) return;
        if (checkCollision(currentX + dx, currentY + dy, currentShape)) {
            currentX += dx;
            currentY += dy;
        } else if (dy > 0) {
            mergePieceToBoard();
        }
    }

    private void rotatePiece() {
        if (isGameOver || isPaused) return;
        int rows = currentShape.length;
        int cols = currentShape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = currentShape[r][c];
            }
        }
        if (checkCollision(currentX, currentY, rotated)) {
            currentShape = rotated;
        }
    }

    private void hardDrop() {
        if (isGameOver || isPaused) return;
        while (checkCollision(currentX, currentY + 1, currentShape)) {
            currentY++;
        }
        mergePieceToBoard();
    }

    private void togglePause() {
        isPaused = !isPaused;
        btnPause.setText(isPaused ? "▶️" : "⏸️");
    }

    // --- НАДЕЖНЫЙ ИГРОВОЙ И АНИМАЦИОННЫЙ ЦИКЛ (HANDLER TICKER) ---
    private void startGameLoop() {
        gameLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameOver && !isPaused) {
                    animPulseTimer += 0.15f; // Плавное обновление анимации свечения

                    long currentTime = System.currentTimeMillis();
                    // Скорость падения зависит от уровня: чем выше уровень, тем меньше задержка
                    long dropInterval = Math.max(120, 700 - (level - 1) * 90);

                    if (currentTime - lastDropTime >= dropInterval) {
                        movePiece(0, 1);
                        lastDropTime = currentTime;
                    }

                    // Принудительная перерисовка кастомного View
                    if (tetrisView != null) {
                        tetrisView.invalidate();
                    }
                }
                gameLoopHandler.postDelayed(this, gameLoopFpsMs);
            }
        };
        gameLoopHandler.post(gameLoopRunnable);
    }

    private void startClockAnimation() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String timeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("💠 [ " + timeOnly + " ] 💠\n⚡ КВАНТОВЫЙ ТЕТРИС АКТИВЕН");
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null && clockRunnable != null) clockHandler.removeCallbacks(clockRunnable);
        if (gameLoopHandler != null && gameLoopRunnable != null) gameLoopHandler.removeCallbacks(gameLoopRunnable);
    }

    // --- КАСТОМНЫЙ ВИД ИГРОВОГО ПОЛЯ С НЕОНОВЫМ РЕНДЕРИНГОМ ---
    private class TetrisView extends View {
        private Paint paint = new Paint();

        public TetrisView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            int cellSize = Math.min(width / GRID_WIDTH, height / GRID_HEIGHT);

            int offsetX = (width - (cellSize * GRID_WIDTH)) / 2;
            int offsetY = (height - (cellSize * GRID_HEIGHT)) / 2;

            // Рисуем фон сетки
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#090D1A"));
            canvas.drawRect(offsetX, offsetY, offsetX + cellSize * GRID_WIDTH, offsetY + cellSize * GRID_HEIGHT, paint);

            // Тень и неоновое сияние статичных блоков
            for (int r = 0; r < GRID_HEIGHT; r++) {
                for (int c = 0; c < GRID_WIDTH; c++) {
                    if (board[r][c] != 0) {
                        drawCell(canvas, offsetX + c * cellSize, offsetY + r * cellSize, cellSize, board[r][c]);
                    }
                }
            }

            // Отрисовка призрачной фигуры (Ghost Piece)
            if (showGhostPiece && !isGameOver && !isPaused) {
                int ghostY = currentY;
                while (checkCollision(currentX, ghostY + 1, currentShape)) {
                    ghostY++;
                }
                if (ghostY > currentY) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2f);
                    paint.setColor(Color.argb(80, 255, 255, 255));
                    for (int r = 0; r < currentShape.length; r++) {
                        for (int c = 0; c < currentShape[r].length; c++) {
                            if (currentShape[r][c] != 0) {
                                int px = offsetX + (currentX + c) * cellSize;
                                int py = offsetY + (ghostY + r) * cellSize;
                                canvas.drawRoundRect(px + 2, py + 2, px + cellSize - 2, py + cellSize - 2, 8, 8, paint);
                            }
                        }
                    }
                }
            }

            // Отрисовка текущей падающей фигуры
            if (!isGameOver) {
                for (int r = 0; r < currentShape.length; r++) {
                    for (int c = 0; c < currentShape[r].length; c++) {
                        if (currentShape[r][c] != 0) {
                            drawCell(canvas, offsetX + (currentX + c) * cellSize, offsetY + (currentY + r) * cellSize, cellSize, currentShapeIndex + 1);
                        }
                    }
                }
            }

            // Рисуем сетку с плавным пульсирующим эффектом прозрачности
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            float alphaPulse = 30f + (float)(Math.sin(animPulseTimer) * 15f * globalGlowIntensity);
            paint.setColor(Color.argb((int)alphaPulse, 56, 189, 248));

            for (int r = 0; r <= GRID_HEIGHT; r++) {
                canvas.drawLine(offsetX, offsetY + r * cellSize, offsetX + GRID_WIDTH * cellSize, offsetY + r * cellSize, paint);
            }
            for (int c = 0; c <= GRID_WIDTH; c++) {
                canvas.drawLine(offsetX + c * cellSize, offsetY, offsetX + c * cellSize, offsetY + GRID_HEIGHT * cellSize, paint);
            }
        }

        private void drawCell(Canvas canvas, float x, float y, float size, int colorId) {
            int[] themeColors = activeThemeColors[colorThemeIndex];
            int blockColor = themeColors[colorId % themeColors.length];

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(blockColor);

            RectF rect = new RectF(x + 2, y + 2, x + size - 2, y + size - 2);
            canvas.drawRoundRect(rect, 8, 8, paint);

            // Внутренний неоновый блик
            paint.setColor(Color.argb(120, 255, 255, 255));
            RectF innerRect = new RectF(x + 5, y + 5, x + size - 5, y + size / 2 - 2);
            canvas.drawRoundRect(innerRect, 4, 4, paint);
        }
    }

    // --- ДИАЛОГИ НАСТРОЕК ---
    private void showAnimationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎨 Лаборатория анимаций и тем");

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(35, 20, 35, 20);

        Button btnTheme = new Button(this);
        updateThemeButtonText(btnTheme);
        btnTheme.setOnClickListener(v -> {
            colorThemeIndex = (colorThemeIndex + 1) % activeThemeColors.length;
            updateThemeButtonText(btnTheme);
            tetrisView.invalidate();
        });
        menu.addView(btnTheme);

        Button btnGlow = new Button(this);
        updateGlowButtonText(btnGlow);
        btnGlow.setOnClickListener(v -> {
            globalGlowIntensity += 0.5f;
            if (globalGlowIntensity > 2.0f) globalGlowIntensity = 0.5f;
            updateGlowButtonText(btnGlow);
        });
        menu.addView(btnGlow);

        builder.setView(menu);
        builder.setPositiveButton("Готово 🚀", null);
        builder.show();
    }

    private void updateThemeButtonText(Button btn) {
        String name = colorThemeIndex == 0 ? "Киберпанк Синий" : (colorThemeIndex == 1 ? "Зелёная Матрица" : "Квантовый Пурпур");
        btn.setText("🌟 Неоновая тема: " + name);
    }
    private void updateGlowButtonText(Button btn) {
        btn.setText("✨ Интенсивность сияния: " + globalGlowIntensity + "x");
    }

    private void showGameSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Настройки игрового процесса");

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(35, 20, 35, 20);

        Button btnLevel = new Button(this);
        btnLevel.setText("⭐ Стартовый уровень: " + startLevel);
        btnLevel.setOnClickListener(v -> {
            startLevel++;
            if (startLevel > 5) startLevel = 1;
            btnLevel.setText("⭐ Стартовый уровень: " + startLevel);
        });
        menu.addView(btnLevel);

        Button btnGhost = new Button(this);
        btnGhost.setText("👻 Тень падения: " + (showGhostPiece ? "ВКЛ" : "ВЫКЛ"));
        btnGhost.setOnClickListener(v -> {
            showGhostPiece = !showGhostPiece;
            btnGhost.setText("👻 Тень падения: " + (showGhostPiece ? "ВКЛ" : "ВЫКЛ"));
        });
        menu.addView(btnGhost);

        builder.setView(menu);
        builder.setPositiveButton("Перезапустить игру 🔄", (d, w) -> initNewGame());
        builder.show();
    }

    private void showGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("💥 КРАХ КВАНТОВОГО ЯДРА!");
        builder.setMessage("Игра окончена!\nИтоговый счёт: " + score + "\nДостигнутый уровень: " + level + "\n\nХотите попробовать снова?");
        builder.setPositiveButton("НОВАЯ ИГРА 🔄", (d, w) -> initNewGame());
        builder.setNegativeButton("Выход ❌", (d, w) -> finishAndRemoveTask());
        builder.show();
    }
}