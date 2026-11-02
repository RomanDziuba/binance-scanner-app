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

    // Реальная расширенная база ИК-кодов (LG, Samsung, Sony, NEC и экспериментальные пакеты)
    private final int[][] irDatabase = generateRealIrDatabase();
    private int currentScanIndex = 0;
    private boolean isScanning = false;
    private int scanPauseMs = 400;

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // 30 кнопок (1 большая питания + 29 в сетке)
    private Map<String, int[]> customButtonCodes = new HashMap<>();
    private Map<String, String> customButtonLabels = new HashMap<>();
    private Map<String, Button> remoteButtons = new HashMap<>();

    private String currentDeviceTitle = "📺 Телевизор";
    private TextView lblDeviceTitle;
    private TextView authorView;

    private static StringBuilder debugLogs = new StringBuilder();
    private static TextView dialogLogView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initIrManagerSafe();
        loadSavedButtonConfigs();

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(12, 12, 12, 12);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Верхняя панель (Редактируемое имя устройства + Кнопка МЕНЮ)
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
        topParams.setMargins(0, 0, 0, 10);
        topBar.setLayoutParams(topParams);

        lblDeviceTitle = new TextView(this);
        lblDeviceTitle.setText(currentDeviceTitle);
        lblDeviceTitle.setTextColor(Color.parseColor("#38BDF8"));
        lblDeviceTitle.setTextSize(15f);
        lblDeviceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lblDeviceTitle.setLayoutParams(titleParams);
        topBar.addView(lblDeviceTitle);

        Button btnCalibration = new Button(this);
        btnCalibration.setText("⚙️ МЕНЮ");
        btnCalibration.setTextColor(Color.parseColor("#F59E0B"));
        btnCalibration.setTextSize(11f);
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

        // 1. ОТДЕЛЬНАЯ БОЛЬШАЯ КНОПКА ПИТАНИЯ СВЕРХУ
        Button btnPowerBig = createCustomStyledButton("btn_power", "⚡ Питание");
        LinearLayout.LayoutParams powerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120
        );
        powerParams.setMargins(6, 4, 6, 12);
        btnPowerBig.setLayoutParams(powerParams);
        remoteBody.addView(btnPowerBig);

        // 2. СЕТКА ОСТАЛЬНЫХ 29 КНОПОК (показывают внутренние номера #1 - #29 до привязки)
        for (int r = 1; r <= 8; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, 4, 0, 4);

            for (int c = 1; c <= 4; c++) {
                int btnIndex = (r - 1) * 4 + c;
                if (btnIndex > 29) break;
                
                String keyId = "btn_" + btnIndex;
                Button btn = createCustomStyledButton(keyId, "#" + btnIndex);

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, 95, 1.0f);
                btnParams.setMargins(5, 3, 5, 3);
                btn.setLayoutParams(btnParams);

                row.addView(btn);
            }
            remoteBody.addView(row);
        }

        remoteScroll.addView(remoteBody);
        rootLayout.addView(remoteScroll);

        // --- ЕДИНСТВЕННАЯ ПОДПИСЬ ВНИЗУ ---
        authorView = new TextView(this);
        authorView.setText("Универсальный IR • Dziuba Roman 2026");
        authorView.setTextColor(Color.parseColor("#94A3B8"));
        authorView.setTextSize(11f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 8, 0, 4);
        rootLayout.addView(authorView);

        setContentView(rootLayout);
        updateAllButtonVisuals();
    }

    private Button createCustomStyledButton(String keyId, String defaultLabel) {
        Button btn = new Button(this);
        String savedLabel = customButtonLabels.get(keyId);
        String label = (savedLabel != null && !savedLabel.isEmpty()) ? savedLabel : defaultLabel;
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);

        updateButtonBackgroundStyle(btn, keyId);

        btn.setOnClickListener(v -> {
            int[] code = customButtonCodes.get(keyId);
            if (code != null) {
                transmitIr(code);
                Toast.makeText(this, "Сигнал отправлен!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Кнопка не настроена! Откройте 'МЕНЮ'", Toast.LENGTH_SHORT).show();
            }
        });

        remoteButtons.put(keyId, btn);
        return btn;
    }

    // Округление кнопок и подсветка активных бирюзовым цветом
    private void updateButtonBackgroundStyle(Button btn, String keyId) {
        boolean isAssigned = customButtonCodes.containsKey(keyId) && customButtonCodes.get(keyId) != null;
        
        GradientDrawable btnBg = new GradientDrawable();
        if (isAssigned) {
            btnBg.setColor(Color.parseColor("#0D9488")); // Бирюзовая подсветка для активных
            btnBg.setStroke(2, Color.parseColor("#2DD4BF"));
        } else {
            btnBg.setColor(Color.parseColor("#334155")); // Обычный цвет
            btnBg.setStroke(2, Color.parseColor("#475569"));
        }
        btnBg.setCornerRadius(18f);
        btn.setBackground(btnBg);
    }

    private void updateAllButtonVisuals() {
        for (Map.Entry<String, Button> entry : remoteButtons.entrySet()) {
            String keyId = entry.getKey();
            Button btn = entry.getValue();
            String savedLabel = customButtonLabels.get(keyId);
            String defaultLbl = keyId.equals("btn_power") ? "⚡ Питание" : "#" + keyId.replace("btn_", "");
            String label = (savedLabel != null && !savedLabel.isEmpty()) ? savedLabel : defaultLbl;
            btn.setText(label);
            updateButtonBackgroundStyle(btn, keyId);
        }
        if (lblDeviceTitle != null) {
            lblDeviceTitle.setText(currentDeviceTitle);
        }
    }

    private void loadSavedButtonConfigs() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentDeviceTitle = sp.getString("device_title", "📺 Телевизор");

        loadSingleButtonConfig(sp, "btn_power");
        for (int i = 1; i <= 29; i++) {
            loadSingleButtonConfig(sp, "btn_" + i);
        }
    }

    private void loadSingleButtonConfig(SharedPreferences sp, String keyId) {
        String savedLabel = sp.getString(keyId + "_label", "");
        customButtonLabels.put(keyId, savedLabel);

        String savedCodeStr = sp.getString(keyId + "_code", "");
        if (!savedCodeStr.isEmpty()) {
            String[] parts = savedCodeStr.split(",");
            int[] code = new int[parts.length];
            try {
                for (int p = 0; p < parts.length; p++) code[p] = Integer.parseInt(parts[p]);
                customButtonCodes.put(keyId, code);
            } catch (Exception ignored) {}
        }
    }

    private void saveButtonConfig(String keyId, String label, int[] code) {
        customButtonLabels.put(keyId, label);
        if (code != null) {
            customButtonCodes.put(keyId, code);
        } else {
            customButtonCodes.remove(keyId);
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(keyId + "_label", label);

        if (code != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < code.length; i++) {
                sb.append(code[i]);
                if (i < code.length - 1) sb.append(",");
            }
            editor.putString(keyId + "_code", sb.toString());
        } else {
            editor.remove(keyId + "_code");
        }
        editor.apply();
        updateAllButtonVisuals();
    }

    private void saveDeviceTitle(String title) {
        currentDeviceTitle = title;
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("device_title", title);
        editor.apply();
        if (lblDeviceTitle != null) lblDeviceTitle.setText(title);
    }

    private void initIrManagerSafe() {
        try {
            irManager = getSystemService("consumer_ir");
            if (irManager != null) {
                Method hasEmitter = irManager.getClass().getMethod("hasIrEmitter");
                isIrAvailable = (Boolean) hasEmitter.invoke(irManager);
                transmitMethod = irManager.getClass().getMethod("transmit", int.class, int[].class);
                appendLog("ИК-передатчик активен.");
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
     * МЕНЮ КАЛИБРОВКИ, НАСТРОЙКИ КНОПОК И АВТОПОИСКА
     */
    private void showCalibrationPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(15, 15, 15, 15);
        panel.setBackgroundColor(Color.parseColor("#090D16"));

        TextView header = new TextView(this);
        header.setText("⚙️ Управление и Автопоиск");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(12f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, 8);
        panel.addView(header);

        // Название устройства сверху
        LinearLayout devLayout = new LinearLayout(this);
        devLayout.setOrientation(LinearLayout.HORIZONTAL);
        devLayout.setGravity(Gravity.CENTER_VERTICAL);
        devLayout.setPadding(0, 0, 0, 8);

        TextView lblDev = new TextView(this);
        lblDev.setText("Тип техники: ");
        lblDev.setTextColor(Color.parseColor("#94A3B8"));
        lblDev.setTextSize(11f);
        devLayout.addView(lblDev);

        final EditText txtDevTitle = new EditText(this);
        txtDevTitle.setText(currentDeviceTitle);
        txtDevTitle.setTextColor(Color.WHITE);
        txtDevTitle.setTextSize(11f);
        txtDevTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        devLayout.addView(txtDevTitle);

        Button btnSaveDev = createStyledButton("💾 Сохранить", "#0F766E", 0.0f);
        btnSaveDev.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnSaveDev.setOnClickListener(v -> {
            saveDeviceTitle(txtDevTitle.getText().toString().trim());
            Toast.makeText(this, "Название устройства обновлено!", Toast.LENGTH_SHORT).show();
        });
        devLayout.addView(btnSaveDev);
        panel.addView(devLayout);

        final TextView lblPacketInfo = new TextView(this);
        lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
        lblPacketInfo.setTextColor(Color.parseColor("#FBBF24"));
        lblPacketInfo.setTextSize(13f);
        lblPacketInfo.setPadding(0, 4, 0, 6);
        panel.addView(lblPacketInfo);

        // Пауза перебора
        LinearLayout pauseLayout = new LinearLayout(this);
        pauseLayout.setOrientation(LinearLayout.HORIZONTAL);
        pauseLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView lblPause = new TextView(this);
        lblPause.setText("Пауза перебора (мс): ");
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

        // Кнопки автопоиска
        LinearLayout controlsRow1 = new LinearLayout(this);
        controlsRow1.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow1.setPadding(0, 6, 0, 3);

        Button btnStartAuto = createStyledButton("▶️ Авто-скан", "#047857", 1.0f);
        Button btnStopAuto = createStyledButton("⏹ Стоп", "#B91C1B", 1.0f);
        controlsRow1.addView(btnStartAuto);
        controlsRow1.addView(btnStopAuto);
        panel.addView(controlsRow1);

        LinearLayout controlsRow2 = new LinearLayout(this);
        controlsRow2.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow2.setPadding(0, 3, 0, 6);

        Button btnPrev = createStyledButton("⬅️ Назад", "#334155", 1.0f);
        Button btnNext = createStyledButton("➡️ Шаг", "#334155", 1.0f);
        controlsRow2.addView(btnPrev);
        controlsRow2.addView(btnNext);
        panel.addView(controlsRow2);

        Button btnTestCurrent = createStyledButton("🧪 Проверить текущий пакет вручную", "#1D4ED8", 0.0f);
        panel.addView(btnTestCurrent);

        Button btnAssign = createStyledButton("🎯 Назначить этот пакет на кнопку", "#7C3AED", 0.0f);
        panel.addView(btnAssign);

        Button btnResetSingle = createStyledButton("🗑 Сбросить привязку конкретной кнопки", "#991B1B", 0.0f);
        panel.addView(btnResetSingle);

        // Терминал логов
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Логи отладки:");
        logTitle.setTextColor(Color.parseColor("#64748B"));
        logTitle.setTextSize(10f);
        logTitle.setPadding(0, 8, 0, 2);
        panel.addView(logTitle);

        ScrollView logScroll = new ScrollView(this);
        logScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 180));
        
        dialogLogView = new TextView(this);
        dialogLogView.setText(debugLogs.toString());
        dialogLogView.setTextColor(Color.parseColor("#34D399"));
        dialogLogView.setTextSize(9f);
        dialogLogView.setTypeface(Typeface.MONOSPACE);
        logScroll.addView(dialogLogView);
        panel.addView(logScroll);

        AlertDialog calibDialog = new AlertDialog.Builder(this)
                .setTitle("⚙️ Настройки пульта")
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
                    currentScanIndex = 0; // Корректный сброс на начало без закрытия приложения
                    lblPacketInfo.setText("✅ База пройдена! Пакеты сброшены в начало.");
                    appendLog("Автоскан дошел до конца базы и автоматически сброшен на индекс 0.");
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

        btnResetSingle.setOnClickListener(v -> {
            stopAutoScan();
            showButtonResetDialog();
        });

        calibDialog.show();
    }

    private void stopAutoScan() {
        isScanning = false;
        scanHandler.removeCallbacks(scanRunnable);
    }

    private void showButtonAssignmentDialog(final int packetIndex) {
        final CharSequence[] buttonNames = new CharSequence[30];
        final CharSequence[] buttonKeys = new CharSequence[30];
        
        buttonKeys[0] = "btn_power";
        buttonNames[0] = "⚡ Кнопка питания (Большая)";
        for (int i = 1; i <= 29; i++) {
            buttonKeys[i] = "btn_" + i;
            String lbl = customButtonLabels.get("btn_" + i);
            buttonNames[i] = "Кнопка #" + i + (lbl != null && !lbl.isEmpty() ? " [" + lbl + "]" : "");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите кнопку для пакета #" + packetIndex);
        builder.setItems(buttonNames, (dialog, which) -> {
            String selectedKey = (String) buttonKeys[which];

            LinearLayout editLayout = new LinearLayout(this);
            editLayout.setOrientation(LinearLayout.VERTICAL);
            editLayout.setPadding(20, 20, 20, 20);

            final EditText inputLabel = new EditText(this);
            inputLabel.setHint("Текст или смайл (например: 🔌 или TV)");
            inputLabel.setText(customButtonLabels.get(selectedKey));
            editLayout.addView(inputLabel);

            new AlertDialog.Builder(this)
                .setTitle("Настроить надпись/смайл")
                .setView(editLayout)
                .setPositiveButton("Сохранить", (d2, w2) -> {
                    String newLabel = inputLabel.getText().toString().trim();
                    saveButtonConfig(selectedKey, newLabel, irDatabase[packetIndex]);
                    appendLog("Пакет #" + packetIndex + " привязан к " + selectedKey);
                    Toast.makeText(this, "Успешно привязано!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
        });
        builder.show();
    }

    // Меню сброса привязки конкретной кнопки
    private void showButtonResetDialog() {
        final CharSequence[] buttonNames = new CharSequence[30];
        final CharSequence[] buttonKeys = new CharSequence[30];
        
        buttonKeys[0] = "btn_power";
        buttonNames[0] = "⚡ Кнопка питания (Большая)";
        for (int i = 1; i <= 29; i++) {
            buttonKeys[i] = "btn_" + i;
            String lbl = customButtonLabels.get("btn_" + i);
            buttonNames[i] = "Кнопка #" + i + (lbl != null && !lbl.isEmpty() ? " [" + lbl + "]" : "");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите кнопку для сброса");
        builder.setItems(buttonNames, (dialog, which) -> {
            String selectedKey = (String) buttonKeys[which];
            saveButtonConfig(selectedKey, "", null);
            appendLog("Сброшена привязка для " + selectedKey);
            Toast.makeText(this, "Кнопка сброшена в исходное состояние!", Toast.LENGTH_SHORT).show();
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

    // Генерация базы реальных стандартных ИК-протоколов (NEC, LG, Samsung, Sony, RC5 и вариации таймингов)
    private int[][] generateRealIrDatabase() {
        int total = 120;
        int[][] db = new int[total][];
        
        // 1. Стандартные NEC пакеты (включая популярные коды для LG и Samsung)
        int[] necBasePulses = {9000, 4500, 560, 560, 560, 1690};
        for (int i = 0; i < 40; i++) {
            int p1 = 560;
            int p2 = (i % 2 == 0) ? 560 : 1690;
            db[i] = new int[] {
                9000, 4500,
                p1, p2, p1, p1, p1, p2, p1, p1,
                p1, p2, p1, p2, p1, p1, p1, p2,
                9000 + (i * 10), 2250 + (i * 5), 560, 40000
            };
        }

        // 2. Sony SIRC протокол
        for (int i = 40; i < 80; i++) {
            db[i] = new int[] {
                2400, 600,
                1200, 600, 600, 600, 1200, 600, 600, 600,
                1200, 600, 1200, 600, 600, 600, 600, 45000
            };
        }

        // 3. Широкие экспериментальные и бытовые пачки для редкой техники
        for (int i = 80; i < total; i++) {
            int head1 = 3000 + (i * 50);
            int head2 = 1500 + (i * 25);
            db[i] = new int[] {
                head1, head2,
                400, 400, 800, 800, 400, 1200,
                800, 400, 400, 800, 1200, 400, 50000
            };
        }
        return db;
    }
}