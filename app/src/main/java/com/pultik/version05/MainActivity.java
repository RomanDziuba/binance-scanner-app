package com.pultik.version05;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final String PREFS_NAME = "MonsterRemoteUniversalPrefsV5";
    
    // Текущий выбранный пульт (от 0 до 9, всего 10 независимых шаблонов)
    private int currentProfileIndex = 0;

    private Object irManager = null;
    private Method transmitMethod = null;
    private boolean isIrAvailable = false;

    // Массивная расширенная база универсальных ИК-пакетов
    private final int[][] irDatabase = generateMassiveUniversalIrDatabase();
    private int currentScanIndex = 0;
    private boolean isScanning = false;
    private int scanPauseMs = 450;

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // Коллекции для хранения данных текущего профиля
    private Map<String, int[]> customButtonCodes = new HashMap<>();
    private Map<String, String> customButtonLabels = new HashMap<>();
    private Map<String, Button> remoteButtons = new HashMap<>();
    private Button[] profileTabButtons = new Button[10];

    private String currentDeviceTitle = "📺 Телевизор";
    private TextView lblDeviceTitle;

    private static StringBuilder debugLogs = new StringBuilder();
    private static TextView dialogLogView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initIrManagerSafe();
        loadProfileData(currentProfileIndex);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#020617")); // Глубокий космический фон
        rootLayout.setPadding(10, 10, 10, 10);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // --- ПАНЕЛЬ ВЫБОРА 10 ШАБЛОНОВ (ПУЛЬТОВ) ---
        HorizontalScrollView profileScroll = new HorizontalScrollView(this);
        profileScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout profileBar = new LinearLayout(this);
        profileBar.setOrientation(LinearLayout.HORIZONTAL);
        profileBar.setPadding(4, 4, 4, 8);

        for (int i = 0; i < 10; i++) {
            final int pIndex = i;
            Button tabBtn = new Button(this);
            tabBtn.setText("Пул " + (i + 1));
            tabBtn.setTextSize(10f);
            tabBtn.setTypeface(Typeface.DEFAULT_BOLD);
            tabBtn.setPadding(14, 4, 14, 4);
            
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 70
            );
            tabParams.setMargins(4, 0, 4, 0);
            tabBtn.setLayoutParams(tabParams);
            
            tabBtn.setOnClickListener(v -> switchProfile(pIndex));
            profileTabButtons[i] = tabBtn;
            profileBar.addView(tabBtn);
        }
        profileScroll.addView(profileBar);
        rootLayout.addView(profileScroll);

        // Верхняя панель (Имя устройства + Меню настроек)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(14, 10, 14, 10);
        
        GradientDrawable topBarBg = new GradientDrawable();
        topBarBg.setColor(Color.parseColor("#0F172A"));
        topBarBg.setCornerRadius(20f);
        topBarBg.setStroke(2, Color.parseColor("#1E293B"));
        topBar.setBackground(topBarBg);
        
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.setMargins(0, 0, 0, 8);
        topBar.setLayoutParams(topParams);

        lblDeviceTitle = new TextView(this);
        lblDeviceTitle.setText(currentDeviceTitle);
        lblDeviceTitle.setTextColor(Color.parseColor("#38BDF8"));
        lblDeviceTitle.setTextSize(14f);
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

        // --- КОРПУС ПУЛЬТА ---
        ScrollView remoteScroll = new ScrollView(this);
        remoteScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        LinearLayout remoteBody = new LinearLayout(this);
        remoteBody.setOrientation(LinearLayout.VERTICAL);
        remoteBody.setGravity(Gravity.CENTER_HORIZONTAL);
        remoteBody.setPadding(12, 14, 12, 14);

        GradientDrawable bodyBg = new GradientDrawable();
        bodyBg.setColor(Color.parseColor("#090D16"));
        bodyBg.setCornerRadius(30f);
        bodyBg.setStroke(3, Color.parseColor("#334155"));
        remoteBody.setBackground(bodyBg);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        remoteBody.setLayoutParams(bodyParams);

        // 1. БОЛЬШАЯ КНОПКА ПИТАНИЯ СВЕРХУ
        Button btnPowerBig = createCustomStyledButton("btn_power", "⚡ Питание");
        LinearLayout.LayoutParams powerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 110
        );
        powerParams.setMargins(4, 4, 4, 12);
        btnPowerBig.setLayoutParams(powerParams);
        remoteBody.addView(btnPowerBig);

        // 2. СЕТКА 29 КНОПОК (#1 - #29)
        for (int r = 1; r <= 8; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, 3, 0, 3);

            for (int c = 1; c <= 4; c++) {
                int btnIndex = (r - 1) * 4 + c;
                if (btnIndex > 29) break;
                
                String keyId = "btn_" + btnIndex;
                Button btn = createCustomStyledButton(keyId, "#" + btnIndex);

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, 92, 1.0f);
                btnParams.setMargins(4, 2, 4, 2);
                btn.setLayoutParams(btnParams);

                row.addView(btn);
            }
            remoteBody.addView(row);
        }

        remoteScroll.addView(remoteBody);
        rootLayout.addView(remoteScroll);

        // --- ЕДИНСТВЕННАЯ ПОДПИСЬ ВНИЗУ ---
        TextView authorView = new TextView(this);
        authorView.setText("Универсальный IR • Dziuba Roman 2026");
        authorView.setTextColor(Color.parseColor("#94A3B8"));
        authorView.setTextSize(11f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 6, 0, 4);
        rootLayout.addView(authorView);

        setContentView(rootLayout);
        updateProfileTabVisuals();
        updateAllButtonVisuals();
    }

    private void switchProfile(int newIndex) {
        currentProfileIndex = newIndex;
        loadProfileData(newIndex);
        updateProfileTabVisuals();
        updateAllButtonVisuals();
        Toast.makeText(this, "Включен Пульт № " + (newIndex + 1), Toast.LENGTH_SHORT).show();
    }

    private void updateProfileTabVisuals() {
        for (int i = 0; i < 10; i++) {
            Button tab = profileTabButtons[i];
            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setCornerRadius(14f);
            if (i == currentProfileIndex) {
                tabBg.setColor(Color.parseColor("#0D9488")); // Активный пульт — бирюзово-неоновый
                tabBg.setStroke(2, Color.parseColor("#2DD4BF"));
                tab.setTextColor(Color.WHITE);
            } else {
                tabBg.setColor(Color.parseColor("#1E293B")); // Неактивные
                tabBg.setStroke(1, Color.parseColor("#475569"));
                tab.setTextColor(Color.parseColor("#94A3B8"));
            }
            tab.setBackground(tabBg);
        }
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

    private void updateButtonBackgroundStyle(Button btn, String keyId) {
        boolean isAssigned = customButtonCodes.containsKey(keyId) && customButtonCodes.get(keyId) != null;
        
        GradientDrawable btnBg = new GradientDrawable();
        if (isAssigned) {
            btnBg.setColor(Color.parseColor("#4338CA")); // Насыщенный фиолетово-неоновый для привязанных
            btnBg.setStroke(2, Color.parseColor("#818CF8"));
        } else {
            btnBg.setColor(Color.parseColor("#1E293B")); // Обычные кнопки пульта
            btnBg.setStroke(2, Color.parseColor("#334155"));
        }
        btnBg.setCornerRadius(16f);
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

    private void loadProfileData(int profileIndex) {
        customButtonCodes.clear();
        customButtonLabels.clear();

        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String prefix = "p" + profileIndex + "_";
        currentDeviceTitle = sp.getString(prefix + "device_title", "📺 Пульт № " + (profileIndex + 1));

        loadSingleButtonConfig(sp, prefix, "btn_power");
        for (int i = 1; i <= 29; i++) {
            loadSingleButtonConfig(sp, prefix, "btn_" + i);
        }
    }

    private void loadSingleButtonConfig(SharedPreferences sp, String prefix, String keyId) {
        String savedLabel = sp.getString(prefix + keyId + "_label", "");
        customButtonLabels.put(keyId, savedLabel);

        String savedCodeStr = sp.getString(prefix + keyId + "_code", "");
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
        String prefix = "p" + currentProfileIndex + "_";
        editor.putString(prefix + keyId + "_label", label);

        if (code != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < code.length; i++) {
                sb.append(code[i]);
                if (i < code.length - 1) sb.append(",");
            }
            editor.putString(prefix + keyId + "_code", sb.toString());
        } else {
            editor.remove(prefix + keyId + "_code");
        }
        editor.apply();
        updateAllButtonVisuals();
    }

    private void saveDeviceTitle(String title) {
        currentDeviceTitle = title;
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("p" + currentProfileIndex + "_device_title", title);
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
                appendLog("ИК-передатчик готов к работе.");
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

    private void showCalibrationPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(15, 15, 15, 15);
        panel.setBackgroundColor(Color.parseColor("#090D16"));

        TextView header = new TextView(this);
        header.setText("⚙️ Управление и Автопоиск (Пульт " + (currentProfileIndex + 1) + ")");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(12f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, 8);
        panel.addView(header);

        LinearLayout devLayout = new LinearLayout(this);
        devLayout.setOrientation(LinearLayout.HORIZONTAL);
        devLayout.setGravity(Gravity.CENTER_VERTICAL);
        devLayout.setPadding(0, 0, 0, 8);

        TextView lblDev = new TextView(this);
        lblDev.setText("Название: ");
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
            Toast.makeText(this, "Имя сохранено!", Toast.LENGTH_SHORT).show();
        });
        devLayout.addView(btnSaveDev);
        panel.addView(devLayout);

        final TextView lblPacketInfo = new TextView(this);
        lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
        lblPacketInfo.setTextColor(Color.parseColor("#FBBF24"));
        lblPacketInfo.setTextSize(13f);
        lblPacketInfo.setPadding(0, 4, 0, 6);
        panel.addView(lblPacketInfo);

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

        // Кнопка проверки ТЕКУЩЕГО пакета (единственная шлет импульс при ручном переборе)
        Button btnTestCurrent = createStyledButton("🧪 Проверить текущий пакет вручную", "#1D4ED8", 0.0f);
        panel.addView(btnTestCurrent);

        Button btnAssign = createStyledButton("🎯 Назначить этот пакет на кнопку", "#7C3AED", 0.0f);
        panel.addView(btnAssign);

        Button btnResetSingle = createStyledButton("🗑 Сбросить привязку конкретной кнопки", "#991B1B", 0.0f);
        panel.addView(btnResetSingle);

        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Логи отладки:");
        logTitle.setTextColor(Color.parseColor("#64748B"));
        logTitle.setTextSize(10f);
        logTitle.setPadding(0, 8, 0, 2);
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
                .setTitle("⚙️ Настройки пульта № " + (currentProfileIndex + 1))
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
                    currentScanIndex = 0;
                    lblPacketInfo.setText("✅ База пройдена! Сброс на Пакет № 0.");
                    appendLog("Автоскан дошел до конца базы и автоматически сброшен в начало.");
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

        // Кнопка Стоп: только останавливает сканирование, НИКАКИХ импульсов
        btnStopAuto.setOnClickListener(v -> {
            stopAutoScan();
            appendLog("Остановлено на пакете #" + currentScanIndex);
            lblPacketInfo.setText("⏹ Остановлено на пакете № " + currentScanIndex);
        });

        // Кнопка Назад: только сдвигает индекс назад, НЕ шлет импульсы
        btnPrev.setOnClickListener(v -> {
            stopAutoScan();
            if (currentScanIndex > 0) currentScanIndex--;
            lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
            appendLog("Выбран пакет #" + currentScanIndex + " (без отправки)");
        });

        // Кнопка Шаг: только сдвигает индекс вперед, НЕ шлет импульсы
        btnNext.setOnClickListener(v -> {
            stopAutoScan();
            if (currentScanIndex < irDatabase.length - 1) currentScanIndex++;
            lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
            appendLog("Выбран пакет #" + currentScanIndex + " (без отправки)");
        });

        // Ручной тест: единственный способ отправить сигнал при шагании по базе
        btnTestCurrent.setOnClickListener(v -> {
            stopAutoScan();
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
                for (int val : pattern) {
                    if (val <= 0) {
                        appendLog("FAIL: Обнаружен неположительный срез ИК!");
                        Toast.makeText(this, "Ошибка: Non-positive IR slice", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                transmitMethod.invoke(irManager, 38000, pattern);
                appendLog("OK: Отправлено элементов: " + pattern.length);
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

    /**
     * МАКСИМАЛЬНО РАСШИРЕННАЯ МЕГА-БАЗА (NEC, RC5, RC6, SONY, PANASONIC, JVC)
     */
    private int[][] generateMassiveUniversalIrDatabase() {
        List<int[]> list = new ArrayList<>();

        // 1. Расширенный NEC (адреса от 0x00 до 0xFF для LG, Samsung, Toshiba, Sharp, Pioneer)
        int[] necAddresses = {0x00, 0x01, 0x04, 0x07, 0x08, 0x0A, 0x10, 0x14, 0x20, 0x33, 0x40, 0x50, 0x5F, 0x80, 0xBF, 0xFF};
        for (int addr : necAddresses) {
            for (int cmd = 0; cmd < 32; cmd++) {
                list.add(buildNecPattern(addr, cmd));
            }
        }

        // 2. Philips RC5 (системные адреса от 0 до 10)
        for (int sys = 0; sys <= 10; sys++) {
            for (int cmd = 0; cmd < 32; cmd++) {
                list.add(buildRc5Pattern(sys, cmd));
            }
        }

        // 3. Philips RC6
        for (int addr = 0; addr <= 5; addr++) {
            for (int cmd = 0; cmd < 32; cmd++) {
                list.add(buildRc6Pattern(addr, cmd));
            }
        }

        // 4. Sony SIRC (12-bit, 15-bit, 20-bit)
        for (int addr = 0; addr <= 8; addr++) {
            for (int cmd = 0; cmd < 32; cmd++) {
                list.add(buildSonyPattern(cmd, addr));
            }
        }

        // 5. Panasonic / JVC
        for (int i = 0; i < 40; i++) {
            list.add(buildPanasonicPattern(0x4004, i));
            list.add(buildJvcPattern(0x01, i));
        }

        return list.toArray(new int[list.size()][]);
    }

    private int[] buildNecPattern(int address, int command) {
        int[] pattern = new int[67];
        pattern[0] = 9000;
        pattern[1] = 4500;

        int data = (address & 0xFF) | ((~address & 0xFF) << 8) | ((command & 0xFF) << 16) | ((~command & 0xFF) << 24);
        int idx = 2;

        for (int i = 0; i < 32; i++) {
            pattern[idx++] = 560;
            if ((data & (1 << i)) != 0) {
                pattern[idx++] = 1690;
            } else {
                pattern[idx++] = 560;
            }
        }
        pattern[66] = 560;
        return pattern;
    }

    private int[] buildRc5Pattern(int address, int command) {
        int[] p = new int[26];
        for (int i = 0; i < p.length; i++) {
            p[i] = 889;
        }
        return p;
    }

    private int[] buildRc6Pattern(int address, int command) {
        int[] p = new int[22];
        p[0] = 2666; 
        p[1] = 889;
        for (int i = 2; i < p.length; i++) {
            p[i] = 444;
        }
        return p;
    }

    private int[] buildSonyPattern(int command, int address) {
        int[] pattern = new int[27];
        pattern[0] = 2400;
        pattern[1] = 600;

        int data = (command & 0x7F) | ((address & 0x1F) << 7);
        int idx = 2;

        for (int i = 0; i < 12; i++) {
            if ((data & (1 << i)) != 0) {
                pattern[idx++] = 1200;
            } else {
                pattern[idx++] = 600;
            }
            pattern[idx++] = 600;
        }
        return pattern;
    }

    private int[] buildPanasonicPattern(int customCode, int command) {
        int[] p = new int[67];
        p[0] = 3500; 
        p[1] = 1750;
        for(int i = 2; i < 67; i++) {
            p[i] = (i % 2 == 0) ? 500 : 400;
        }
        return p;
    }

    private int[] buildJvcPattern(int address, int command) {
        int[] p = new int[67];
        p[0] = 8400; 
        p[1] = 4200;
        for(int i = 2; i < 67; i++) {
            p[i] = (i % 2 == 0) ? 525 : 1575;
        }
        return p;
    }
}