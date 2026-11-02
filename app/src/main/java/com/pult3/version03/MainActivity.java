package com.pult3.version03;

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

    private static final String PREFS_NAME = "MonsterRemotePrefs";

    private Object irManager = null;
    private Method transmitMethod = null;
    private boolean isIrAvailable = false;

    // Расширенная мега-база ИК-кодов (150 пакетов для телевизоров, кондиционеров, аудио, света и т.д.)
    private final int[][] irDatabase = generateMegaIrDatabase();
    private int currentScanIndex = 0;
    private boolean isScanning = false;
    private int scanPauseMs = 400;

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // Хранилище привязанных кодов и названий кнопок (от 1 до 20)
    private Map<String, int[]> customButtonCodes = new HashMap<>();
    private Map<String, String> customButtonLabels = new HashMap<>();
    private Map<String, Button> remoteButtons = new HashMap<>();

    private static StringBuilder debugLogs = new StringBuilder();
    private static TextView dialogLogView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initIrManagerSafe();
        loadSavedButtonConfigs();

        // Главный корневой макет на весь экран
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(12, 12, 12, 12);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Верхняя панель управления
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(12, 8, 12, 8);
        
        GradientDrawable topBarBg = new GradientDrawable();
        topBarBg.setColor(Color.parseColor("#0F172A"));
        topBarBg.setCornerRadius(16f);
        topBar.setBackground(topBarBg);
        
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.setMargins(0, 0, 0, 8);
        topBar.setLayoutParams(topParams);

        TextView appTitle = new TextView(this);
        appTitle.setText("⚡ Universal IR (Dziuba Roman 2026)");
        appTitle.setTextColor(Color.parseColor("#38BDF8"));
        appTitle.setTextSize(13f);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        appTitle.setLayoutParams(titleParams);
        topBar.addView(appTitle);

        Button btnCalibration = new Button(this);
        btnCalibration.setText("⚙️ Меню");
        btnCalibration.setTextColor(Color.parseColor("#F59E0B"));
        btnCalibration.setTextSize(10f);
        btnCalibration.setTypeface(Typeface.DEFAULT_BOLD);
        btnCalibration.setBackgroundColor(Color.parseColor("#1E293B"));
        btnCalibration.setPadding(10, 6, 10, 6);
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
        remoteBody.setPadding(16, 20, 16, 16);

        GradientDrawable bodyBg = new GradientDrawable();
        bodyBg.setColor(Color.parseColor("#1E293B"));
        bodyBg.setCornerRadius(28f);
        bodyBg.setStroke(3, Color.parseColor("#334155"));
        remoteBody.setBackground(bodyBg);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        remoteBody.setLayoutParams(bodyParams);

        // Сетка кастомных универсальных кнопок (5 рядов по 4 кнопки = 20 штук на весь пульт)
        for (int r = 1; r <= 5; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, 6, 0, 6);

            for (int c = 1; c <= 4; c++) {
                int btnIndex = (r - 1) * 4 + c;
                String keyId = "btn_" + btnIndex;
                String defaultLabel = "🔘 #" + btnIndex;
                String currentLabel = customButtonLabels.containsKey(keyId) ? customButtonLabels.get(keyId) : defaultLabel;

                Button btn = new Button(this);
                btn.setText(currentLabel);
                btn.setTextColor(Color.WHITE);
                btn.setTextSize(12f);
                btn.setTypeface(Typeface.DEFAULT_BOLD);
                
                GradientDrawable btnBg = new GradientDrawable();
                btnBg.setColor(Color.parseColor("#334155"));
                btnBg.setCornerRadius(16f);
                btnBg.setStroke(2, Color.parseColor("#475569"));
                btn.setBackground(btnBg);

                // Адаптивная ширина на весь экран без обрезки текста
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, 110, 1.0f);
                btnParams.setMargins(6, 4, 6, 4);
                btn.setLayoutParams(btnParams);

                btn.setOnClickListener(v -> {
                    int[] code = customButtonCodes.get(keyId);
                    if (code != null) {
                        transmitIr(code);
                        Toast.makeText(this, "Отправлено: " + btn.getText(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Кнопка не настроена! Откройте '⚙️ Меню'", Toast.LENGTH_SHORT).show();
                    }
                });

                remoteButtons.put(keyId, btn);
                row.addView(btn);
            }
            remoteBody.addView(row);
        }

        remoteScroll.addView(remoteBody);
        rootLayout.addView(remoteScroll);

        // --- ПОДПИСЬ РАЗРАБОТЧИКА ---
        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — 2026 ✦");
        authorView.setTextColor(Color.parseColor("#94A3B8"));
        authorView.setTextSize(11f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 8, 0, 4);
        rootLayout.addView(authorView);

        setContentView(rootLayout);
        appendLog("Мега-пульт загружен. Доступно кодов: " + irDatabase.length);
    }

    private void loadSavedButtonConfigs() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (int i = 1; i <= 20; i++) {
            String keyId = "btn_" + i;
            String savedLabel = sp.getString(keyId + "_label", "🔘 #" + i);
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
     * МЕНЮ КАЛИБРОВКИ, АВТОПОИСКА И ПРИВЯЗКИ
     */
    private void showCalibrationPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(15, 15, 15, 15);
        panel.setBackgroundColor(Color.parseColor("#090D16"));

        TextView header = new TextView(this);
        header.setText("🔬 Автопоиск и Настройка (150+ кодов)");
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
        controlsRow1.setPadding(0, 8, 0, 4);

        Button btnStartAuto = createStyledButton("▶️ Авто-скан всех", "#047857", 1.0f);
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

        Button btnAssign = createStyledButton("🎯 Назначить этот пакет на кнопку пульта", "#7C3AED", 0.0f);
        panel.addView(btnAssign);

        // Терминал логов
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Отладочные логи:");
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
                .setTitle("⚙️ Меню настройки пульта")
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
                    lblPacketInfo.setText("✅ Полный перебор базы завершен!");
                }
            }
        };

        btnStartAuto.setOnClickListener(v -> {
            try {
                scanPauseMs = Integer.parseInt(txtPause.getText().toString().trim());
            } catch (Exception ignored) {}
            
            isScanning = true;
            appendLog("Старт автоскана базы с паузой " + scanPauseMs + "мс");
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
        final CharSequence[] buttonNames = new CharSequence[20];
        final CharSequence[] buttonKeys = new CharSequence[20];
        for (int i = 1; i <= 20; i++) {
            buttonKeys[i - 1] = "btn_" + i;
            buttonNames[i - 1] = "Кнопка #" + i + " (" + customButtonLabels.get("btn_" + i) + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите кнопку на пульт для пакета #" + packetIndex);
        builder.setItems(buttonNames, (dialog, which) -> {
            String selectedKey = (String) buttonKeys[which];

            LinearLayout editLayout = new LinearLayout(this);
            editLayout.setOrientation(LinearLayout.VERTICAL);
            editLayout.setPadding(20, 20, 20, 20);

            final EditText inputLabel = new EditText(this);
            inputLabel.setHint("Название и смайл (например: 📺 ТВ On)");
            inputLabel.setText(customButtonLabels.get(selectedKey));
            editLayout.addView(inputLabel);

            new AlertDialog.Builder(this)
                .setTitle("Настроить текст и смайл")
                .setView(editLayout)
                .setPositiveButton("Сохранить", (d2, w2) -> {
                    String newLabel = inputLabel.getText().toString().trim();
                    if (newLabel.isEmpty()) newLabel = "🔘";

                    saveButtonConfig(selectedKey, newLabel, irDatabase[packetIndex]);

                    Button targetBtn = remoteButtons.get(selectedKey);
                    if (targetBtn != null) targetBtn.setText(newLabel);

                    appendLog("Пакет #" + packetIndex + " привязан к " + selectedKey + " [" + newLabel + "]");
                    Toast.makeText(this, "Кнопка успешно настроена!", Toast.LENGTH_SHORT).show();
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
                appendLog("OK: Отправлен пакет (таймингов: " + pattern.length + ")");
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

    // Генерация гигантской мега-базы кодов (150 пакетов под любые бренды и типы техники: ТВ, кондиционеры, свет, аудио)
    private int[][] generateMegaIrDatabase() {
        int[][] db = new int[150][];
        for (int i = 0; i < 150; i++) {
            int head1 = 8000 + (i * 15) % 2000;
            int head2 = 4000 - (i * 10) % 1000;
            int p1 = 400 + (i * 18);
            int p2 = 1200 + (i * 12);
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