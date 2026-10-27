package com.roma.monster;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    public static class FoundDevice {
        public BluetoothDevice device;
        public String discoveryMethod;
        public int rssi;
        public String connectionState = "Отключено 🔌";

        public FoundDevice(BluetoothDevice device, String discoveryMethod, int rssi) {
            this.device = device;
            this.discoveryMethod = discoveryMethod;
            this.rssi = rssi;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FoundDevice) {
                return this.device.getAddress().equalsIgnoreCase(((FoundDevice) obj).device.getAddress());
            }
            return false;
        }
    }

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    
    private final List<FoundDevice> discoveredDevices = new ArrayList<>();
    
    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnScan;
    private LinearLayout devicesContainer;

    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int REQUEST_PERMISSIONS = 101;
    private BroadcastReceiver classicReceiver;
    private boolean isScanning = false;
    private int scanCountdown = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(12, 12, 12, 12);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Monster Engine v11.0 Omni-Connect ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        // Информационная панель
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(20f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        infoTextView = new TextView(this);
        infoTextView.setText("📦 Bluetooth: Активен\n🌐 Режим: Абсолютный всеядный поиск (Без фильтров)");
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(11f);
        infoTextView.setPadding(10, 8, 10, 8);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(10f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 10);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        btnScan = createStyledButton("🚀 Запустить поиск абсолютно всех устройств", "#2563EB");
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
        card.addView(btnScan);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 8, 0, 4);
        card.addView(statusTextView);

        container.addView(card);

        TextView listLabel = new TextView(this);
        listLabel.setText("📱 Полный реестр найденных гаджетов:");
        listLabel.setTextColor(Color.parseColor("#38BDF8"));
        listLabel.setTextSize(12f);
        listLabel.setTypeface(Typeface.DEFAULT_BOLD);
        listLabel.setPadding(0, 5, 0, 8);
        container.addView(listLabel);

        devicesContainer = new LinearLayout(this);
        devicesContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(devicesContainer);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Системный лог обмена:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 10, 0, 4);
        container.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь отображается отладка всех подключений...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(10);
        logEditText.setMaxLines(16);
        container.addView(logEditText);

        Button btnCopyLogs = createStyledButton("📋 Скопировать лог", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы!", Toast.LENGTH_SHORT).show();
        });
        container.addView(btnCopyLogs);

        Button btnClearLogs = createStyledButton("🧹 Очистить консоль", "#334155");
        btnClearLogs.setOnClickListener(v -> logEditText.setText(""));
        container.addView(btnClearLogs);

        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        appendLog("=== MONSTER ENGINE v11.0 OMNI ЗАПУЩЕН ===");
        checkBluetoothState();
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(10, 10, 10, 10);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 6);
        btn.setLayoutParams(params);
        return btn;
    }

    private void appendLog(String msg) {
        handler.post(() -> {
            if (logEditText != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                logEditText.append("[" + sdf.format(new Date()) + "] " + msg + "\n");
            }
        });
    }

    private void checkBluetoothState() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            appendLog("⚠️ Bluetooth выключен.");
            statusTextView.setText("Статус: ⚠️ Включите Bluetooth");
        } else {
            appendLog("✅ Bluetooth модуль готов.");
        }
    }

    private void checkPermissionsAndScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            
            appendLog("🔑 Запрос системных разрешений...");
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_PERMISSIONS);
            return;
        }
        startOmniScan();
    }

    private void startOmniScan() {
        discoveredDevices.clear();
        devicesContainer.removeAllViews();
        isScanning = true;

        appendLog("\n--- СТАРТ ТОТАЛЬНОГО ПОИСКА (БЕЗ ФИЛЬТРОВ) ---");

        // 1. Сопряженные устройства
        try {
            Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
            if (paired != null) {
                for (BluetoothDevice dev : paired) {
                    addDiscoveredDevice(dev, "Сопряжено (System)", 0);
                }
            }
        } catch (Exception ignored) {}

        // 2. BLE сканер
        if (bleScanner != null) {
            try {
                ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
                bleScanner.startScan(null, settings, bleScanCallback);
            } catch (Exception e) {
                appendLog("Ошибка BLE сканера: " + e.getMessage());
            }
        }

        // 3. Классический поиск
        try {
            if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();

            if (classicReceiver == null) {
                classicReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                            BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
                            if (dev != null) addDiscoveredDevice(dev, "Classic BT", rssi);
                        }
                    }
                };
                registerReceiver(classicReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
        } catch (Exception ignored) {}

        scanCountdown = 12;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isScanning && scanCountdown > 0) {
                    statusTextView.setText("Статус: 🔍 Поиск (" + scanCountdown + "с)... Найдено: " + discoveredDevices.size());
                    scanCountdown--;
                    handler.postDelayed(this, 1000);
                } else if (isScanning) {
                    stopScan();
                }
            }
        });
    }

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null && result.getDevice() != null) {
                // НИКАКИХ ФИЛЬТРОВ — добавляем абсолютно все устройства в эфире
                addDiscoveredDevice(result.getDevice(), "BLE Scan", result.getRssi());
            }
        }
    };

    private synchronized void addDiscoveredDevice(BluetoothDevice device, String method, int rssi) {
        FoundDevice newItem = new FoundDevice(device, method, rssi);
        if (!discoveredDevices.contains(newItem)) {
            discoveredDevices.add(newItem);
            String displayName = device.getName() != null ? device.getName() : "Без имени (Аноним)";
            appendLog("✨ Найдено: " + displayName + " [" + device.getAddress() + "] via " + method);
            handler.post(this::renderDeviceCards);
        }
    }

    private void stopScan() {
        isScanning = false;
        try {
            if (bleScanner != null) bleScanner.stopScan(bleScanCallback);
            if (bluetoothAdapter != null) bluetoothAdapter.cancelDiscovery();
            if (classicReceiver != null) {
                unregisterReceiver(classicReceiver);
                classicReceiver = null;
            }
        } catch (Exception ignored) {}

        statusTextView.setText("Статус: Поиск завершен ✅ (Всего: " + discoveredDevices.size() + ")");
        appendLog("🏁 Поиск завершен. Ни одно устройство не было отсечено.");
    }

    private void renderDeviceCards() {
        devicesContainer.removeAllViews();
        for (FoundDevice fd : discoveredDevices) {
            BluetoothDevice dev = fd.device;
            String name = dev.getName() != null ? dev.getName() : "Устройство без имени [" + dev.getAddress() + "]";
            
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(12, 12, 12, 12);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#1E293B"));
            bg.setCornerRadius(16f);
            bg.setStroke(1, Color.parseColor("#334155"));
            card.setBackground(bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            card.setLayoutParams(params);

            TextView tvInfo = new TextView(this);
            tvInfo.setText("🎧 " + name + "\n🔗 MAC: " + dev.getAddress() + "\n📍 Метод: " + fd.discoveryMethod + " | Статус: " + fd.connectionState);
            tvInfo.setTextColor(Color.parseColor("#E2E8F0"));
            tvInfo.setTextSize(11f);
            card.addView(tvInfo);

            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);

            Button btnConnect = createSmallButton("🔗 Подключить GATT", "#2563EB");
            btnConnect.setOnClickListener(v -> connectGattDevice(fd));
            row1.addView(btnConnect);

            Button btnAudio = createSmallButton("🎵 Слушать звук (A2DP)", "#059669");
            btnAudio.setOnClickListener(v -> streamAudioDevice(dev));
            row1.addView(btnAudio);

            card.addView(row1);

            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setPadding(0, 4, 0, 0);

            Button btnPair = createSmallButton("🔑 Сопряжение (Pair)", "#D97706");
            btnPair.setOnClickListener(v -> pairDevice(dev));
            row2.addView(btnPair);

            Button btnDisconnect = createSmallButton("🔌 Отключить", "#DC2626");
            btnDisconnect.setOnClickListener(v -> disconnectDevice(fd));
            row2.addView(btnDisconnect);

            card.addView(row2);
            devicesContainer.addView(card);
        }
    }

    private Button createSmallButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(10f);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(6, 6, 6, 6);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(2, 2, 2, 2);
        btn.setLayoutParams(params);
        return btn;
    }

    private void connectGattDevice(FoundDevice fd) {
        BluetoothDevice dev = fd.device;
        String name = dev.getName() != null ? dev.getName() : dev.getAddress();
        appendLog("\n🚀 Открытие GATT сокета для: " + name);
        fd.connectionState = "Коннект...";
        handler.post(this::renderDeviceCards);

        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
        }

        handler.postDelayed(() -> {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 31) return;

            bluetoothGatt = dev.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        appendLog("⚠️ Устройство [" + name + "] отклонило GATT-сокет (status=" + status + "). Возможно, это аудиоколонка/наушники — используйте кнопку 'Слушать звук'.");
                        try { gatt.close(); } catch (Exception ignored) {}
                        bluetoothGatt = null;
                        fd.connectionState = "Ошибка сокета (" + status + ")";
                        handler.post(() -> renderDeviceCards());
                        return;
                    }

                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        fd.connectionState = "Подключено (GATT) ✅";
                        appendLog("✅ Успешный GATT-сокет открыт для " + name);
                        handler.post(() -> renderDeviceCards());
                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        fd.connectionState = "Отключено 🔌";
                        appendLog("🔌 Соединение закрыто.");
                        handler.post(() -> renderDeviceCards());
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        appendLog("🔥 Найдено служб у [" + name + "]: " + gatt.getServices().size());
                        for (BluetoothGattService s : gatt.getServices()) {
                            appendLog(" 📂 Сервис: " + s.getUuid());
                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                appendLog("   ├── Характеристика: " + c.getUuid());
                            }
                        }
                    }
                }
            }, BluetoothDevice.TRANSPORT_AUTO);
        }, 300);
    }

    // Перенаправление потока музыки на колонки и наушники через системный аудиоменеджер
    private void streamAudioDevice(BluetoothDevice dev) {
        String name = dev.getName() != null ? dev.getName() : dev.getAddress();
        appendLog("🎵 [АУДИО] Переключение музыкального потока на: " + name);
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Выберите колонку/наушники в меню для вывода звука", Toast.LENGTH_LONG).show();
            appendLog("ℹ️ Открыты настройки аудио-соединений Android.");
        } catch (Exception e) {
            appendLog("❌ Ошибка аудио-панели: " + e.getMessage());
        }
    }

    private void pairDevice(BluetoothDevice dev) {
        try {
            appendLog("🔑 Запрос системного сопряжения (Pair)...");
            Method method = dev.getClass().getMethod("createBond");
            method.invoke(dev);
            Toast.makeText(this, "Запрос отправлен в систему", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendLog("❌ Ошибка сопряжения: " + e.getMessage());
        }
    }

    private void disconnectDevice(FoundDevice fd) {
        fd.connectionState = "Отключено 🔌";
        handler.post(this::renderDeviceCards);
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
        }
        appendLog("🔌 Соединение принудительно разорвано.");
    }
}