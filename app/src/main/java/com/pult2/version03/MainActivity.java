package com.pult2.version03;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private Object irManager = null;
    private Method transmitMethod = null;
    private boolean isIrAvailable = false;

    private final int[][] irDatabase = generateIrDatabase();
    private int currentScanIndex = 0;
    private boolean isScanning = false;
    private int scanPauseMs = 500;

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // Мапа привязки ID кнопок к ИК-кодам
    private Map<String, int[]> customButtonCodes = new HashMap<>();
    private Map<String, Button> remoteButtons = new HashMap<>();

    private static StringBuilder debugLogs = new StringBuilder();
    private static TextView dialogLogView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initIrManagerSafe();

        // Главный фон на весь экран (темный премиальный стиль)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(16, 16, 16, 16);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Верхняя панель (Заголовок + Кнопка калибровки)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(10, 10, 10, 10);
        
        GradientDrawable topBarBg = new GradientDrawable();
        topBarBg.setColor(Color.parseColor("#0F172A"));
        topBarBg.setCornerRadius(16f);
        topBar.setBackground(topBarBg);
        
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.setMargins(0, 0, 0, 12);
        topBar.setLayoutParams(topParams);

        TextView appTitle = new TextView(this);
        appTitle.setText("⚡ Monster Smart Remote");
        appTitle.setTextColor(Color.parseColor("#38BDF8"));
        appTitle.setTextSize(14f);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        appTitle.setLayoutParams(titleParams);
        topBar.addView(appTitle);

        Button btnCalibration = new Button(this);
        btnCalibration.setText("⚙️ Калибровка");
        btnCalibration.setTextColor(Color.parseColor("#F59E0B"));
        btnCalibration.setTextSize(10f);
        btnCalibration.setTypeface(Typeface.DEFAULT_BOLD);
        btnCalibration.setBackgroundColor(Color.parseColor("#1E293B"));
        btnCalibration.setPadding(12, 8, 12, 8);
        btnCalibration.setOnClickListener(v -> showCalibrationPanel());
        topBar.addView(btnCalibration);
        rootLayout.addView(topBar);

        // --- КОРПУС ПУЛЬТА НА ВЕСЬ ЭКРАН (Стилизация под скриншот) ---
        ScrollView remoteScroll = new ScrollView(this);
        remoteScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        LinearLayout remoteBody = new LinearLayout(this);
        remoteBody.setOrientation(LinearLayout.VERTICAL);
        remoteBody.setGravity(Gravity.CENTER_HORIZONTAL);
        remoteBody.setPadding(20, 30, 20, 20);

        // Текстура корпуса пульта (серый скругленный матовый пластик с темной рамкой)
        GradientDrawable bodyBg = new GradientDrawable();
        bodyBg.setColor(Color.parseColor("#334155"));
        bodyBg.setCornerRadius(36f);
        bodyBg.setStroke(4, Color.parseColor("#475569"));
        remoteBody.setBackground(bodyBg);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
            850, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        remoteBody.setLayoutParams(bodyParams);

        // 1. Верхний ряд: две круглые кнопки (Power слева, Инфо/Меню справа)
        LinearLayout topButtonsRow = new LinearLayout(this);
        topButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        topButtonsRow.setGravity(Gravity.CENTER);
        topButtonsRow.setPadding(0, 0, 0, 25);

        Button btnPower = createRoundButton("btn_power", "⏻", "#DC2626"); // Красная круглая
        Button btnSettings = createRoundButton("btn_settings", "⚙", "#0D9488"); // Бирюзовая круглая
        
        LinearLayout.LayoutParams roundParams = new LinearLayout.LayoutParams(110, 110);
        roundParams.setMargins(40, 0, 40, 0);
        btnPower.setLayoutParams(roundParams);
        btnSettings.setLayoutParams(roundParams);

        topButtonsRow.addView(btnPower);
        topButtonsRow.addView(btnSettings);
        remoteBody.addView(topButtonsRow);

        // 2. Сетка прямоугольных кнопок (5 рядов по 4 кнопки, как на образце)
        String[][] gridKeys = {
            {"btn_1", "btn_2", "btn_3", "btn_mute"},
            {"btn_4", "btn_5", "btn_6", "btn_vol_up"},
            {"btn_7", "btn_8", "btn_9", "btn_vol_down"},
            {"btn_star", "btn_0", "btn_hash", "btn_ch_up"},
            {"btn_back", "btn_home", "btn_menu", "btn_ch_down"}
        };
        
        String[][] gridLabels = {
            {"1", "2", "3", "Mute"},
            {"4", "5", "6", "Vol +"},
            {"7", "8", "9", "Vol -"},
            {"*", "0", "#", "Ch +"},
            {"Back", "Home", "Menu", "Ch -"}
        };

        for (int r = 0; r < gridKeys.length; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, 6, 0, 6);

            for (int c = 0; c < gridKeys[r].length; c++) {
                String keyId = gridKeys[r][c];
                String label = gridLabels[r][c];
                
                Button btn = new Button(this);
                btn.setText(label);
                btn.setTextColor(Color.WHITE);
                btn.setTextSize(11f);
                btn.setTypeface(Typeface.DEFAULT_BOLD);
                
                // Цвет кнопок: функциональные чуть темнее, цифровые стандартные
                String btnColor = (c == 3) ? "#1E293B" : "#475569";
                GradientDrawable btnBg = new GradientDrawable();
                btnBg.setColor(Color.parseColor(btnColor));
                btnBg.setCornerRadius(12f);
                btn.setBackground(btnBg);

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(155, 75);
                btnParams.setMargins(6, 4, 6, 4);
                btn.setLayoutParams(btnParams);

                btn.setOnClickListener(v -> executeButtonAction(keyId, label));
                remoteButtons.put(keyId, btn);
                row.addView(btn);
            }
            remoteBody.addView(row);
        }

        // 3. Нижний блок: Навигационный круг (Крестовина со скруглениями и центральной кнопкой OK)
        LinearLayout navContainer = new LinearLayout(this);
        navContainer.setOrientation(LinearLayout.VERTICAL);
        navContainer.setGravity(Gravity.CENTER);
        navContainer.setPadding(0, 30, 0, 10);

        // Внешнее кольцо навигации
        FrameLayout dPadLayout = new FrameLayout(this);
        dPadLayout.setLayoutParams(new LinearLayout.LayoutParams(340, 340));

        GradientDrawable ringBg = new GradientDrawable();
        ringBg.setShape(GradientDrawable.OVAL);
        ringBg.setColor(Color.parseColor("#1E293B"));
        ringBg.setStroke(3, Color.parseColor("#0D9488"));
        dPadLayout.setBackground(ringBg);

        // Кнопка ВВЕРХ
        Button btnUp = createNavButton("btn_up", "▲", 110, 70, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 10, 0, 0);
        // Кнопка ВНИЗ
        Button btnDown = createNavButton("btn_down", "▼", 110, 70, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0, 0, 10);
        // Кнопка ВЛЕВО
        Button btnLeft = createNavButton("btn_left", "◄", 70, 110, Gravity.LEFT | Gravity.CENTER_VERTICAL, 10, 0, 0, 0);
        // Кнопка ВПРАВО
        Button btnRight = createNavButton("btn_right", "►", 70, 110, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 10, 0);
        // Центральная кнопка OK (красная круглая)
        Button btnOk = createRoundButton("btn_ok", "OK", "#DC2626");
        FrameLayout.LayoutParams okParams = new FrameLayout.LayoutParams(110, 110);
        okParams.gravity = Gravity.CENTER;
        btnOk.setLayoutParams(okParams);
        btnOk.setTextSize(12f);
        btnOk.setOnClickListener(v -> executeButtonAction("btn_ok", "OK"));
        remoteButtons.put("btn_ok", btnOk);

        dPadLayout.addView(btnUp);
        dPadLayout.addView(btnDown);
        dPadLayout.addView(btnLeft);
        dPadLayout.addView(btnRight);
        dPadLayout.addView(btnOk);

        navContainer.addView(dPadLayout);
        remoteBody.addView(navContainer);

        remoteScroll.addView(remoteBody);
        rootLayout.addView(remoteScroll);

        // --- БЛОК АВТОРСТВА (Внизу экрана) ---
        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — 2026 ✦");
        authorView.setTextColor(Color.parseColor("#94A3B8"));
        authorView.setTextSize(11f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 10, 0, 5);
        rootLayout.addView(authorView);

        setContentView(rootLayout);
        appendLog("Remote initialized successfully. Ready.");
    }

    private Button createRoundButton(String keyId, String label, String hexColor) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(hexColor));
        btn.setBackground(bg);

        btn.setOnClickListener(v -> executeButtonAction(keyId, label));
        remoteButtons.put(keyId, btn);
        return btn;
    }

    private Button createNavButton(String keyId, String label, int w, int h, int gravity, int l, int t, int r, int b) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(Color.parseColor("#38BDF8"));
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.TRANSPARENT);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(w, h);
        params.gravity = gravity;
        params.setMargins(l, t, r, b);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> executeButtonAction(keyId, label));
        remoteButtons.put(keyId, btn);
        return btn;
    }

    private void executeButtonAction(String keyId, String defaultLabel) {
        int[] code = customButtonCodes.get(keyId);
        if (code != null) {
            transmitIr(code);
            Toast.makeText(this, "Команда: " + remoteButtons.get(keyId).getText(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Кнопка '" + defaultLabel + "' не откалибрована! Нажмите 'Калибровка'", Toast.LENGTH_SHORT).show();
        }
    }

    private void initIrManagerSafe() {
        try {
            irManager = getSystemService("consumer_ir");
            if (irManager != null) {
                Method hasEmitter = irManager.getClass().getMethod("hasIrEmitter");
                isIrAvailable = (Boolean) hasEmitter.invoke(irManager);
                transmitMethod = irManager.getClass().getMethod("transmit", int.class, int[].class);
                appendLog("IR Manager готов.");
            }
        } catch (Exception e) {
            isIrAvailable = false;
            appendLog("Ошибка init IR: " + e.toString());
        }
    }

    private void appendLog(String text) {
        String msg = "• " + text + "\n";
        debugLogs.append(msg);
        if (dialogLogView != null) {
            dialogLogView.post(() -> dialogLogView.setText(debugLogs.toString()));
        }
    }

    /**
     * СТУДИЯ КАЛИБРОВКИ И ЛОГОВ
     */
    private void showCalibrationPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(15, 15, 15, 15);
        panel.setBackgroundColor(Color.parseColor("#090D16"));

        TextView header = new TextView(this);
        header.setText("🔬 Калибровка ИК-пультов (Dziuba Roman 2026)");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(12f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, 10);
        panel.addView(header);

        final TextView lblPacketInfo = new TextView(this);
        lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
        lblPacketInfo.setTextColor(Color.parseColor("#FBBF24"));
        lblPacketInfo.setTextSize(13f);
        lblPacketInfo.setPadding(0, 0, 0, 8);
        panel.addView(lblPacketInfo);

        // Настройка паузы
        LinearLayout pauseLayout = new LinearLayout(this);
        pauseLayout.setOrientation(LinearLayout.HORIZONTAL);
        pauseLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView lblPause = new TextView(this);
        lblPause.setText("Пауза (мс): ");
        lblPause.setTextColor(Color.parseColor("#94A3B8"));
        lblPause.setTextSize(11f);
        pauseLayout.addView(lblPause);

        final EditText txtPause = new EditText(this);
        txtPause.setText(String.valueOf(scanPauseMs));
        txtPause.setTextColor(Color.WHITE);
        txtPause.setTextSize(11f);
        txtPause.setLayoutParams(new LinearLayout.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
        pauseLayout.addView(txtPause);
        panel.addView(pauseLayout);

        // Кнопки управления автосканом
        LinearLayout controlsRow1 = new LinearLayout(this);
        controlsRow1.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow1.setPadding(0, 8, 0, 4);

        Button btnStartAuto = createStyledButton("▶️ Авто-скан", "#047857", 1.0f);
        Button btnStopAuto = createStyledButton("⏹ Стоп", "#B91C1B", 1.0f);
        controlsRow1.addView(btnStartAuto);
        controlsRow1.addView(btnStopAuto);
        panel.addView(controlsRow1);

        LinearLayout controlsRow2 = new LinearLayout(this);
        controlsRow2.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow2.setPadding(0, 4, 0, 8);

        Button btnPrev = createStyledButton("⬅️ Назад", "#334155", 1.0f);
        Button btnNext = createStyledButton("➡️ Шаг", "#334155", 1.0f);
        controlsRow2.addView(btnPrev);
        controlsRow2.addView(btnNext);
        panel.addView(controlsRow2);

        Button btnTestCurrent = createStyledButton("🧪 Проверить текущий пакет вручную", "#1D4ED8", 0.0f);
        panel.addView(btnTestCurrent);

        Button btnAssign = createStyledButton("🎯 Привязать этот пакет к кнопке пульта", "#7C3AED", 0.0f);
        panel.addView(btnAssign);

        // Терминал логов
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Отладочные логи ИК-порта:");
        logTitle.setTextColor(Color.parseColor("#64748B"));
        logTitle.setTextSize(10f);
        logTitle.setPadding(0, 10, 0, 2);
        panel.addView(logTitle);

        ScrollView logScroll = new ScrollView(this);
        logScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));
        
        dialogLogView = new TextView(this);
        dialogLogView.setText(debugLogs.toString());
        dialogLogView.setTextColor(Color.parseColor("#34D399"));
        dialogLogView.setTextSize(9f);
        dialogLogView.setTypeface(Typeface.MONOSPACE);
        logScroll.addView(dialogLogView);
        panel.addView(logScroll);

        AlertDialog calibDialog = new AlertDialog.Builder(this)
                .setTitle("⚙️ Меню калибровки")
                .setView(panel)
                .setNegativeButton("Закрыть", (d, w) -> {
                    stopAutoScan();
                    dialogLogView = null;
                })
                .create();

        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScanning) return;
                
                if (currentScanIndex < irDatabase.length) {
                    transmitIr(irDatabase[currentScanIndex]);
                    lblPacketInfo.setText("⚡ Перебор: Пакет № " + currentScanIndex + " / " + irDatabase.length);
                    currentScanIndex++;
                    scanHandler.postDelayed(this, scanPauseMs);
                } else {
                    stopAutoScan();
                    lblPacketInfo.setText("✅ Перебор завершен!");
                }
            }
        };

        btnStartAuto.setOnClickListener(v -> {
            try {
                scanPauseMs = Integer.parseInt(txtPause.getText().toString().trim());
            } catch (Exception ignored) {}
            
            isScanning = true;
            appendLog("Старт автоскана с паузой " + scanPauseMs + "мс");
            scanHandler.post(scanRunnable);
        });

        btnStopAuto.setOnClickListener(v -> {
            stopAutoScan();
            appendLog("Остановлено на пакете #" + currentScanIndex);
            lblPacketInfo.setText("⏹ Остановлено на пакете № " + currentScanIndex);
        });

        btnPrev.setOnClickListener(v -> {
            if (currentScanIndex > 0) currentScanIndex--;
            lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
            transmitIr(irDatabase[currentScanIndex]);
        });

        btnNext.setOnClickListener(v -> {
            if (currentScanIndex < irDatabase.length - 1) currentScanIndex++;
            lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
            transmitIr(irDatabase[currentScanIndex]);
        });

        btnTestCurrent.setOnClickListener(v -> {
            appendLog("Ручной тест пакета #" + currentScanIndex);
            transmitIr(irDatabase[currentScanIndex]);
        });

        btnAssign.setOnClickListener(v -> {
            stopAutoScan();
            showButtonAssignmentDialog(currentScanIndex);
        });

        calibDialog.show();
    }

    private void stopAutoScan() {
        isScanning = false;
        scanHandler.removeCallbacks(scanRunnable);
    }

    private void showButtonAssignmentDialog(final int packetIndex) {
        final CharSequence[] buttonKeys = {
            "btn_power", "btn_settings", "btn_ok", "btn_up", "btn_down", "btn_left", "btn_right",
            "btn_1", "btn_2", "btn_3", "btn_mute", "btn_vol_up", "btn_vol_down", "btn_ch_up", "btn_ch_down"
        };
        final CharSequence[] buttonNames = {
            "POWER (Красная)", "Настройки (Бирюзовая)", "OK (Центр)", "Вверх ▲", "Вниз ▼", "Влево ◄", "Вправо ►",
            "Кнопка 1", "Кнопка 2", "Кнопка 3", "Mute", "Vol +", "Vol -", "Ch +", "Ch -"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Привязать пакет #" + packetIndex + " к кнопке:");
        builder.setItems(buttonNames, (dialog, which) -> {
            String selectedKey = (String) buttonKeys[which];
            Button targetBtn = remoteButtons.get(selectedKey);

            final EditText inputName = new EditText(this);
            inputName.setText(targetBtn != null ? targetBtn.getText().toString() : "Команда");

            new AlertDialog.Builder(this)
                .setTitle("Название для кнопки")
                .setView(inputName)
                .setPositiveButton("Сохранить", (d2, w2) -> {
                    String newName = inputName.getText().toString();
                    customButtonCodes.put(selectedKey, irDatabase[packetIndex]);
                    if (targetBtn != null) targetBtn.setText(newName);
                    appendLog("Пакет #" + packetIndex + " привязан к '" + newName + "'");
                    Toast.makeText(this, "Успешно привязано!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
        });
        builder.show();
    }

    private void transmitIr(int[] pattern) {
        try {
            if (irManager != null && transmitMethod != null) {
                transmitMethod.invoke(irManager, 38000, pattern);
                appendLog("OK: Отправлено (элементов: " + pattern.length + ")");
            } else {
                appendLog("ERROR: ИК-порт недоступен");
            }
        } catch (Exception e) {
            String err = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            appendLog("FAIL: " + err);
            Toast.makeText(this, "Ошибка: " + err, Toast.LENGTH_SHORT).show();
        }
    }

    private Button createStyledButton(String text, String colorHex, float weight) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        if (weight > 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            params.setMargins(4, 4, 4, 4);
            btn.setLayoutParams(params);
        } else {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 4, 4, 4);
            btn.setLayoutParams(params);
        }
        return btn;
    }

    private int[][] generateIrDatabase() {
        int[][] db = new int[50][];
        for (int i = 0; i < 50; i++) {
            int baseLen = 400 + (i * 25);
            db[i] = new int[] {
                9000 - (i * 10), 4500 + (i * 5),
                baseLen, 560, baseLen, 560, baseLen, 1690,
                560, 560, 560, 560, 560, 560, 560, 560,
                560, 1690, 560, 1690, 560, 560, 560
            };
        }
        return db;
    }
}