package com.pult.version04;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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

    private static final String PREFS_NAME = "MonsterRemotePrefsV3";

    private Object irManager = null;
    private Method transmitMethod = null;
    private boolean isIrAvailable = false;

    private final int[][] irDatabase = generateMegaIrDatabase();
    private int currentScanIndex = 0;
    private boolean isScanning = false;
    private int scanPauseMs = 400;

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // 30 кастомных кнопок + 1 главная кнопка питания (btn_power)
    private Map<String, int[]> customButtonCodes = new HashMap<>();
    private Map<String, String> customButtonLabels = new HashMap<>();
    private Map<String, Button> remoteButtons = new HashMap<>();

    private String deviceTitle = "📺 Телевизор";
    private TextView lblDeviceTitle;

    private static StringBuilder debugLogs = new StringBuilder();
    private static TextView dialogLogView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initIrManagerSafe();
        loadSavedConfigs();

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(12, 12, 12, 12);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Верхняя панель: Название устройства + Кнопка Меню
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(14, 10, 14, 10);
        
        GradientDrawable topBarBg = new GradientDrawable();
        topBarBg.setColor(Color.parseColor("#0F172A"));
        topBarBg.setCornerRadius(18f);
        topBar.setBackground(topBarBg);
        
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.setMargins(0, 0, 0, 8);
        topBar.setLayoutParams(topParams);

        lblDeviceTitle = new TextView(this);
        lblDeviceTitle.setText(deviceTitle);
        lblDeviceTitle.setTextColor(Color.parseColor("#38BDF8"));
        lblDeviceTitle.setTextSize(14f);
        lblDeviceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lblDeviceTitle.setLayoutParams(titleParams);
        topBar.addView(lblDeviceTitle);

        Button btnCalibration = new Button(this);
        btnCalibration.setText("⚙️ МЕНЮ");
        btnCalibration.setTextColor(Color.parseColor("#F59E0B"));
        btnCalibration.setTextSize(10f);
        btnCalibration.setTypeface(Typeface.DEFAULT_BOLD);
        btnCalibration.setBackgroundColor(Color.parseColor("#1E293B"));
        btnCalibration.setPadding(12, 6, 12, 6);
        btnCalibration.setOnClickListener(v -> showCalibrationPanel());
        topBar.addView(btnCalibration);
        rootLayout.addView(topBar);

        // --- КОРПУС ПУЛЬТА НА ВЕСЬ ЭКРАН ---
        ScrollView remoteScroll = new ScrollView(this);
        remoteScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        LinearLayout remoteBody = new LinearLayout(this);
        remoteBody.setOrientation(LinearLayout.VERTICAL);
        remoteBody.setGravity(Gravity.CENTER_HORIZONTAL);
        remoteBody.setPadding(12, 16, 12, 16);

        GradientDrawable bodyBg = new GradientDrawable();
        bodyBg.setColor(Color.parseColor("#1E293B"));
        bodyBg.setCornerRadius(28f);
        bodyBg.setStroke(3, Color.parseColor("#334155"));
        remoteBody.setBackground(bodyBg);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        remoteBody.setLayoutParams(bodyParams);

        // 1. ОТДЕЛЬНАЯ БОЛЬШАЯ КНОПКА ВКЛЮЧЕНИЯ/ВЫКЛЮЧЕНИЯ ВВЕРХУ (без надписей по умолчанию)
        Button btnPower = createCustomButton("btn_power", "", 3.2f, 130);
        LinearLayout.LayoutParams powerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 130
        );
        powerParams.setMargins(6, 4, 6, 12);
        btnPower.setLayoutParams(powerParams);
        remoteBody.addView(btnPower);

        // 2. СЕТКА ИЗ 30 УНИВЕРСАЛЬНЫХ КНОПОК (10 рядов по 3 кнопки)
        for (int r = 1; r <= 10; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, 4, 0, 4);

            for (int c = 1; c <= 3; c++) {
                int btnIndex = (r - 1) * 3 + c;
                String keyId = "btn_" + btnIndex;
                Button btn = createCustomButton(keyId, "🔘 #" + btnIndex, 1.0f, 95);
                remoteBody.addView(btn);
            }
            remoteBody.addView(row);
        }

        remoteScroll.addView(remoteBody);
        rootLayout.addView(remoteScroll);

        // --- ЕДИНАЯ НИЖНЯЯ ПОДПИСЬ ---
        TextView authorView = new TextView(this);
        authorView.setText("✦ Універсальний IR • Dziuba Roman — 2026 ✦");
        authorView.setTextColor(Color.parseColor("#94A3B8"));
        authorView.setTextSize(11f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 8, 0, 4);
        rootLayout.addView(authorView);

        setContentView(rootLayout);
        appendLog("Пульт инициализирован. Всего кнопок: 31");
    }

    private Button createCustomButton(String keyId, String defaultFallback, float weight, int height) {
        Button btn = new Button(this);
        String label = customButtonLabels.containsKey(keyId) ? customButtonLabels.get(keyId) : defaultFallback;
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);

        updateButtonStyle(btn, customButtonCodes.containsKey(keyId));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, height, weight);
        btnParams.setMargins(5, 3, 5, 3);
        btn.setLayoutParams(btnParams);

        btn.setOnClickListener(v -> {
            int[] code = customButtonCodes.get(keyId);
            if (code != null) {
                transmitIr(code);
                Toast.makeText(this, "Команда: " + btn.getText(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Кнопка не настроена! Откройте '⚙️ МЕНЮ'", Toast.LENGTH_SHORT).show();
            }
        });

        remoteButtons.put(keyId, btn);
        return btn;
    }

    private void updateButtonStyle(Button btn, boolean isAssigned) {
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(20f); // Красивое сильное округление кнопок
        if (isAssigned) {
            // Подсветка активной (привязанной) кнопки другим цветом (бирюзово-зеленый оттенок)
            btnBg.setColor(Color.parseColor("#0D9488"));
            btnBg.setStroke(2, Color.parseColor("#2DD4BF"));
        } else {
            // Стандартный цвет нетронутой кнопки
            btnBg.setColor(Color.parseColor("#334155"));
            btnBg.setStroke(2, Color.parseColor("#475569"));
        }
        btn.setBackground(btnBg);
    }

    private void loadSavedConfigs() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        deviceTitle = sp.getString("deviceTitle", "📺 Телевизор");

        // Главная кнопка питания
        String powerLabel = sp.getString("btn_power_label", "");
        customButtonLabels.put("btn_power", powerLabel);
        String powerCodeStr = sp.getString("btn_power_code", "");
        if (!powerCodeStr.isEmpty()) parseAndStoreCode("btn_power", powerCodeStr);

        // 30 универсальных кнопок
        for (int i = 1; i <= 30; i++) {
            String keyId = "btn_" + i;
            String savedLabel = sp.getString(keyId + "_label", "🔘 #" + i);
            customButtonLabels.put(keyId, savedLabel);

            String savedCodeStr = sp.getString(keyId + "_code", "");
            if (!savedCodeStr.isEmpty()) parseAndStoreCode(keyId, savedCodeStr);
        }
    }

    private void parseAndStoreCode(String keyId, String codeStr) {
        String[] parts = codeStr.split(",");
        int[] code = new int[parts.length];
        try {
            for (int p = 0; p < parts.length; p++) code[p] = Integer.parseInt(parts[p]);
            customButtonCodes.put(keyId, code);
        } catch (Exception ignored) {}
    }

    private void saveButtonConfig(String keyId, String label, int[] code) {
        customButtonLabels.put(keyId, label);
        customButtonCodes.put(keyId, code);

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(keyId + "_label", label);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < code.length; i++) {
            sb.append(code[i]);
            if (i < code.length - 1) sb.append(",");
        }
        editor.putString(keyId + "_code", sb.toString());
        editor.apply();
    }

    private void resetButtonConfig(String keyId, String defaultLabel) {
        customButtonCodes.remove(keyId);
        customButtonLabels.put(keyId, defaultLabel);

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.remove(keyId + "_label");
        editor.remove(keyId + "_code");
        editor.apply();
    }

    private void initIrManagerSafe() {
        try {
            irManager = getSystemService("consumer_ir");
            if (irManager != null) {
                Method hasEmitter = irManager.getClass().getMethod("hasIrEmitter");
                isIrAvailable = (Boolean) hasEmitter.invoke(irManager);
                transmitMethod = irManager.getClass().getMethod("transmit", int.class, int[].class);
                appendLog("ИК-порт успешно активирован.");
            }
        } catch (Exception e) {
            isIrAvailable = false;
            appendLog("Ошибка ИК: " + e.toString());
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
     * МЕНЮ КАЛИБРОВКИ, НАСТРОЙКИ НАЗВАНИЯ И СБРОСА
     */
    private void showCalibrationPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(15, 15, 15, 15);
        panel.setBackgroundColor(Color.parseColor("#090D16"));

        TextView header = new TextView(this);
        header.setText("⚙️ Меню настройки устройства и кнопок");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(12f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, 8);
        panel.addView(header);

        // Настройка названия верхней панели (например, Телевизор / Кондиционер)
        LinearLayout titleEditLayout = new LinearLayout(this);
        titleEditLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleEditLayout.setGravity(Gravity.CENTER_VERTICAL);
        titleEditLayout.setPadding(0, 0, 0, 8);

        final EditText txtDeviceTitle = new EditText(this);
        txtDeviceTitle.setText(deviceTitle);
        txtDeviceTitle.setTextColor(Color.WHITE);
        txtDeviceTitle.setTextSize(11f);
        txtDeviceTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        titleEditLayout.addView(txtDeviceTitle);

        Button btnSaveTitle = createStyledButton("💾 Название", "#047857", 0.0f);
        titleEditLayout.addView(btnSaveTitle);
        panel.addView(titleEditLayout);

        final TextView lblPacketInfo = new TextView(this);
        lblPacketInfo.setText("Пакет поиска №: " + currentScanIndex + " / " + irDatabase.length);
        lblPacketInfo.setTextColor(Color.parseColor("#FBBF24"));
        lblPacketInfo.setTextSize(12f);
        lblPacketInfo.setPadding(0, 4, 0, 6);
        panel.addView(lblPacketInfo);

        // Пауза автоскана
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
        txtPause.setLayoutParams(new LinearLayout.LayoutParams(130, ViewGroup.LayoutParams.WRAP_CONTENT));
        pauseLayout.addView(txtPause);
        panel.addView(pauseLayout);

        // Кнопки управления поиском
        LinearLayout controlsRow1 = new LinearLayout(this);
        controlsRow1.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow1.setPadding(0, 6, 0, 4);

        Button btnStartAuto = createStyledButton("▶️ Авто-скан", "#047857", 1.0f);
        Button btnStopAuto = createStyledButton("⏹ Стоп", "#B91C1B", 1.0f);
        controlsRow1.addView(btnStartAuto);
        controlsRow1.addView(btnStopAuto);
        panel.addView(controlsRow1);

        LinearLayout controlsRow2 = new LinearLayout(this);
        controlsRow2.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow2.setPadding(0, 2, 0, 6);

        Button btnPrev = createStyledButton("⬅️ Назад", "#334155", 1.0f);
        Button btnNext = createStyledButton("➡️ Шаг", "#334155", 1.0f);
        controlsRow2.addView(btnPrev);
        controlsRow2.addView(btnNext);
        panel.addView(controlsRow2);

        Button btnTestCurrent = createStyledButton("🧪 Тест текущего пакета вручную", "#1D4ED8", 0.0f);
        panel.addView(btnTestCurrent);

        Button btnAssign = createStyledButton("🎯 Назначить пакет на кнопку пульта", "#7C3AED", 0.0f);
        panel.addView(btnAssign);

        // Терминал логов
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Логи:");
        logTitle.setTextColor(Color.parseColor("#64748B"));
        logTitle.setTextSize(10f);
        logTitle.setPadding(0, 6, 0, 2);
        panel.addView(logTitle);

        ScrollView logScroll = new ScrollView(this);
        logScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 160));
        
        dialogLogView = new TextView(this);
        dialogLogView.setText(debugLogs.toString());
        dialogLogView.setTextColor(Color.parseColor("#34D399"));
        dialogLogView.setTextSize(9f);
        dialogLogView.setTypeface(Typeface.MONOSPACE);
        logScroll.addView(dialogLogView);
        panel.addView(logScroll);

        AlertDialog calibDialog = new AlertDialog.Builder(this)
                .setTitle("⚙️ Меню настроек")
                .setView(panel)
                .setNegativeButton("Закрыть", (d, w) -> {
                    stopAutoScan();
                    dialogLogView = null;
                })
                .create();

        btnSaveTitle.setOnClickListener(v -> {
            deviceTitle = txtDeviceTitle.getText().toString().trim();
            if (deviceTitle.isEmpty()) deviceTitle = "📺 Устройство";
            lblDeviceTitle.setText(deviceTitle);
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString("deviceTitle", deviceTitle).apply();
            Toast.makeText(this, "Название обновлено!", Toast.LENGTH_SHORT).show();
        });

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
            appendLog("Тест пакета #" + currentScanIndex);
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
        final CharSequence[] buttonNames = new CharSequence[31];
        final CharSequence[] buttonKeys = new CharSequence[31];
        
        buttonKeys[0] = "btn_power";
        buttonNames[0] = "⚡ Кнопка Питания (Верхняя)";

        for (int i = 1; i <= 30; i++) {
            buttonKeys[i] = "btn_" + i;
            buttonNames[i] = "Кнопка #" + i + " (" + customButtonLabels.get("btn_" + i) + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите кнопку для пакета #" + packetIndex);
        builder.setItems(buttonNames, (dialog, which) -> {
            String selectedKey = (String) buttonKeys[which];

            LinearLayout editLayout = new LinearLayout(this);
            editLayout.setOrientation(LinearLayout.VERTICAL);
            editLayout.setPadding(20, 20, 20, 20);

            final EditText inputLabel = new EditText(this);
            inputLabel.setHint("Текст или смайл (например: 🔴 выкл)");
            inputLabel.setText(customButtonLabels.get(selectedKey));
            editLayout.addView(inputLabel);

            // Кнопка сброса привязки
            Button btnReset = createStyledButton("🗑 Сбросить (очистить кнопку)", "#991B1B", 0.0f);
            editLayout.addView(btnReset);

            AlertDialog assignDialog = new AlertDialog.Builder(this)
                .setTitle("Настройка кнопки")
                .setView(editLayout)
                .setPositiveButton("Привязать", (d2, w2) -> {
                    String newLabel = inputLabel.getText().toString().trim();
                    if (newLabel.isEmpty()) newLabel = selectedKey.equals("btn_power") ? "" : "🔘";

                    saveButtonConfig(selectedKey, newLabel, irDatabase[packetIndex]);

                    Button targetBtn = remoteButtons.get(selectedKey);
                    if (targetBtn != null) {
                        targetBtn.setText(newLabel);
                        updateButtonStyle(targetBtn, true);
                    }

                    appendLog("Пакет #" + packetIndex + " привязан к " + selectedKey);
                    Toast.makeText(this, "Кнопка настроена!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .create();

            btnReset.setOnClickListener(v -> {
                String defaultFallback = selectedKey.equals("btn_power") ? "" : "🔘 #" + selectedKey.replace("btn_", "");
                resetButtonConfig(selectedKey, defaultFallback);

                Button targetBtn = remoteButtons.get(selectedKey);
                if (targetBtn != null) {
                    targetBtn.setText(defaultFallback);
                    updateButtonStyle(targetBtn, false);
                }

                appendLog("Кнопка " + selectedKey + " сброшена в исходное состояние.");
                Toast.makeText(this, "Сброс выполнен!", Toast.LENGTH_SHORT).show();
                assignDialog.dismiss();
            });

            assignDialog.show();
        });
        builder.show();
    }

    private void transmitIr(int[] pattern) {
        try {
            if (irManager != null && transmitMethod != null) {
                transmitMethod.invoke(irManager, 38000, pattern);
                appendLog("OK: Отправлен пакет (элементов: " + pattern.length + ")");
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

    private int[][] generateMegaIrDatabase() {
        int[][] db = new int[200][];
        for (int i = 0; i < 200; i++) {
            int head1 = 7500 + (i * 20) % 2500;
            int head2 = 3800 - (i * 12) % 1200;
            int p1 = 350 + (i * 15);
            int p2 = 1100 + (i * 10);
            db[i] = new int[] {
                head1, head2,
                p1, 560, p1, 560, p1, p2,
                p1, 560, p1, p2, p1, p2, p1, 560,
                p1, p2, p1, 560, p1, 560, p1, p2,
                p1, 560, p1, 560, p1, p2, p1, p2,
                560, 40000
            };
        }
        return db;
    }
}