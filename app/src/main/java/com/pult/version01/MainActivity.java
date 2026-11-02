package com.pult.version01;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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

    // База данных кодов для перебора (симуляция таймингов)
    private final int[][] irDatabase = generateIrDatabase();
    private int currentScanIndex = 0;
    private boolean isScanning = false;
    private int scanPauseMs = 500; // Пауза по умолчанию 0.5 сек

    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    // Хранилище привязанных команд к кнопкам пульта
    private Map<String, int[]> customButtonCodes = new HashMap<>();
    private Map<String, Button> remoteButtons = new HashMap<>();

    private TextView statusIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Безопасная инициализация ИК-передатчика через рефлексию (чтобы сервер GitHub Actions не ругался на отсутствие пакета)
        initIrManagerSafe();

        // Главный корневой макет
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(20, 20, 20, 20);

        // Верхняя панель управления и кнопка калибровки
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, 20);

        TextView appTitle = new TextView(this);
        appTitle.setText("⚡ Monster Smart Remote");
        appTitle.setTextColor(Color.parseColor("#38BDF8"));
        appTitle.setTextSize(16f);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        appTitle.setLayoutParams(titleParams);
        topBar.addView(appTitle);

        // Маленькая кнопка калибровки в углу пульта
        Button btnCalibration = new Button(this);
        btnCalibration.setText("⚙️ Калибровка");
        btnCalibration.setTextColor(Color.parseColor("#F59E0B"));
        btnCalibration.setTextSize(10f);
        btnCalibration.setBackgroundColor(Color.parseColor("#1E293B"));
        btnCalibration.setPadding(10, 5, 10, 5);
        btnCalibration.setOnClickListener(v -> showCalibrationPanel());
        topBar.addView(btnCalibration);

        rootLayout.addView(topBar);

        // Индикатор статуса ИК
        statusIndicator = new TextView(this);
        statusIndicator.setText(isIrAvailable ? "✅ ИК-порт готов к работе" : "⚠️ ИК-порт (эмуляция / недоступен)");
        statusIndicator.setTextColor(isIrAvailable ? Color.parseColor("#10B981") : Color.parseColor("#FBBF24"));
        statusIndicator.setTextSize(11f);
        statusIndicator.setPadding(0, 0, 0, 15);
        rootLayout.addView(statusIndicator);

        // Сетка кнопок пульта
        ScrollView remoteScroll = new ScrollView(this);
        LinearLayout remoteGrid = new LinearLayout(this);
        remoteGrid.setOrientation(LinearLayout.VERTICAL);
        remoteGrid.setGravity(Gravity.CENTER_HORIZONTAL);

        remoteGrid.addView(createRemoteRow("btn_power", "🔴 POWER", "#DC2626", 400, 120));
        remoteGrid.addView(createRemoteRow("btn_vol_up", "🔊 Громкость +", "#2563EB", 300, 100));
        remoteGrid.addView(createRemoteRow("btn_vol_down", "🔉 Громкость -", "#2563EB", 300, 100));
        remoteGrid.addView(createRemoteRow("btn_ch_up", "📺 Канал +", "#0D9488", 300, 100));
        remoteGrid.addView(createRemoteRow("btn_ch_down", "📉 Канал -", "#0D9488", 300, 100));
        remoteGrid.addView(createRemoteRow("btn_mute", "🔇 Mute", "#7C3AED", 250, 90));
        remoteGrid.addView(createRemoteRow("btn_custom1", "⭐ Избранное 1", "#475569", 350, 90));
        remoteGrid.addView(createRemoteRow("btn_custom2", "⭐ Избранное 2", "#475569", 350, 90));

        remoteScroll.addView(remoteGrid);
        rootLayout.addView(remoteScroll);

        setContentView(rootLayout);
    }

    private void initIrManagerSafe() {
        try {
            irManager = getSystemService("consumer_ir");
            if (irManager != null) {
                Method hasEmitter = irManager.getClass().getMethod("hasIrEmitter");
                isIrAvailable = (Boolean) hasEmitter.invoke(irManager);
                transmitMethod = irManager.getClass().getMethod("transmit", int.class, int[].class);
            }
        } catch (Exception e) {
            isIrAvailable = false;
        }
    }

    private LinearLayout createRemoteRow(String keyId, String defaultLabel, String colorHex, int width, int height) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 6, 0, 6);

        Button btn = new Button(this);
        btn.setText(defaultLabel);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            int[] code = customButtonCodes.get(keyId);
            if (code != null) {
                transmitIr(code);
                Toast.makeText(this, "Отправлена команда: " + btn.getText(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Кнопка '" + btn.getText() + "' не откалибрована! Нажмите 'Калибровка'", Toast.LENGTH_SHORT).show();
            }
        });

        remoteButtons.put(keyId, btn);
        row.addView(btn);
        return row;
    }

    /**
     * ПАНЕЛЬ КАЛИБРОВКИ И АВТОПОИСКА
     */
    private void showCalibrationPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(20, 20, 20, 20);
        panel.setBackgroundColor(Color.parseColor("#090D16"));

        TextView header = new TextView(this);
        header.setText("🔬 Студия калибровки и авто-поиска ИК");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(13f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, 15);
        panel.addView(header);

        final TextView lblPacketInfo = new TextView(this);
        lblPacketInfo.setText("Пакет №: " + currentScanIndex + " / " + irDatabase.length);
        lblPacketInfo.setTextColor(Color.parseColor("#FBBF24"));
        lblPacketInfo.setTextSize(14f);
        lblPacketInfo.setPadding(0, 0, 0, 10);
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

        // Кнопки управления автопоиском
        LinearLayout controlsRow1 = new LinearLayout(this);
        controlsRow1.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow1.setPadding(0, 10, 0, 5);

        Button btnStartAuto = createStyledButton("▶️ Авто-скан", "#047857", 1.0f);
        Button btnStopAuto = createStyledButton("⏹ Стоп", "#B91C1B", 1.0f);
        controlsRow1.addView(btnStartAuto);
        controlsRow1.addView(btnStopAuto);
        panel.addView(controlsRow1);

        LinearLayout controlsRow2 = new LinearLayout(this);
        controlsRow2.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow2.setPadding(0, 5, 0, 10);

        Button btnPrev = createStyledButton("⬅️ Назад", "#334155", 1.0f);
        Button btnNext = createStyledButton("➡️ Шаг", "#334155", 1.0f);
        controlsRow2.addView(btnPrev);
        controlsRow2.addView(btnNext);
        panel.addView(controlsRow2);

        Button btnTestCurrent = createStyledButton("🧪 Проверить текущий пакет вручную", "#1D4ED8", 0.0f);
        panel.addView(btnTestCurrent);

        Button btnAssign = createStyledButton("🎯 Привязать этот пакет к кнопке пульта", "#7C3AED", 0.0f);
        panel.addView(btnAssign);

        AlertDialog calibDialog = new AlertDialog.Builder(this)
                .setTitle("⚙️ Меню калибровки пульта")
                .setView(panel)
                .setNegativeButton("Закрыть", (d, w) -> stopAutoScan())
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
            Toast.makeText(this, "Автосканирование запущенно...", Toast.LENGTH_SHORT).show();
            scanHandler.post(scanRunnable);
        });

        btnStopAuto.setOnClickListener(v -> {
            stopAutoScan();
            Toast.makeText(this, "Сканирование остановлено на пакете #" + currentScanIndex, Toast.LENGTH_SHORT).show();
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
            transmitIr(irDatabase[currentScanIndex]);
            Toast.makeText(this, "Тест пакета #" + currentScanIndex, Toast.LENGTH_SHORT).show();
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
        final CharSequence[] buttonKeys = {"btn_power", "btn_vol_up", "btn_vol_down", "btn_ch_up", "btn_ch_down", "btn_mute", "btn_custom1", "btn_custom2"};
        final CharSequence[] buttonNames = {"POWER", "Громкость +", "Громкость -", "Канал +", "Канал -", "Mute", "Избранное 1", "Избранное 2"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите кнопку для пакета #" + packetIndex);
        builder.setItems(buttonNames, (dialog, which) -> {
            String selectedKey = (String) buttonKeys[which];
            Button targetBtn = remoteButtons.get(selectedKey);

            final EditText inputName = new EditText(this);
            inputName.setText(targetBtn != null ? targetBtn.getText().toString() : "Команда");

            new AlertDialog.Builder(this)
                .setTitle("Введите имя для кнопки")
                .setView(inputName)
                .setPositiveButton("Сохранить", (d2, w2) -> {
                    String newName = inputName.getText().toString();
                    customButtonCodes.put(selectedKey, irDatabase[packetIndex]);
                    if (targetBtn != null) targetBtn.setText(newName);
                    Toast.makeText(this, "Пакет #" + packetIndex + " привязан к кнопке '" + newName + "'!", Toast.LENGTH_LONG).show();
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
            } else {
                Toast.makeText(this, "Симуляция ИК (порт неактивен)", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка отправки ИК", Toast.LENGTH_SHORT).show();
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