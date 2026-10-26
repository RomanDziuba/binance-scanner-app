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

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    public static class FoundDevice {
        public BluetoothDevice device;
        public String discoveryMethod;
        public int rssi;

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
    private Button btnScan, btnDisconnect;
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
        authorView.setText("✦ Monster Watch Control Center v4.0 ✦");
        authorView.setTextColor(Color.parseColor("#38BDF8"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        // Информационная карточка
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
        infoTextView.setText("📦 Bluetooth: Активен\n⌚ Активное устройство: Нет");
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

        btnScan = createStyledButton("🚀 Запустить поиск всех устройств", "#2563EB");
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
        card.addView(btnScan);

        btnDisconnect = createStyledButton("🔌 Закрыть текущее GATT соединение", "#DC2626");
        btnDisconnect.setOnClickListener(v -> disconnectGatt());
        card.addView(btnDisconnect);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 8, 0, 4);
        card.addView(statusTextView);

        container.addView(card);

        // Блок со списком найденных устройств прямо на экране
        TextView listLabel = new TextView(this);
        listLabel.setText("📱 Панель управления найденными устройствами:");
        listLabel.setTextColor(Color.parseColor("#38BDF8"));
        listLabel.setTextSize(12f);
        listLabel.setTypeface(Typeface.DEFAULT_BOLD);
        listLabel.setPadding(0, 5, 0, 8);
        container.addView(listLabel);

        devicesContainer = new LinearLayout(this);
        devicesContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(devicesContainer);

        // Консоль логов внизу
        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Отладочный лог:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 10, 0, 4);
        container.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь будет отображаться вся системная информация...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(8);
        logEditText.setMaxLines(14);
        container.addView(logEditText);

        Button btnCopyLogs = createStyledButton("📋 Скопировать лог", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы!", Toast.LENGTH_SHORT).show();
        });
        container.addView(btnCopyLogs);

        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        appendLog("=== MONSTER ENGINE v4.0 ЗАПУЩЕН ===");
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
            appendLog("⚠️ Bluetooth выключен или недоступен.");
            statusTextView.setText("Статус: ⚠️ Включите Bluetooth");
        } else {
            appendLog("✅ Bluetooth модуль готов к работе.");
        }
    }

    private void checkPermissionsAndScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            
            appendLog("🔑 Запрос системных Bluetooth разрешений...");
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_PERMISSIONS);
            return;
        }
        startUltimateSearch();
    }

    private void startUltimateSearch() {
        discoveredDevices.clear();
        devicesContainer.removeAllViews();
        isScanning = true;

        appendLog("\n--- СТАРТ ПОИСКА ---");

        // 1. Сопряженные устройства (Redmi Watch 5 Active попадет сюда!)
        try {
            Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
            if (paired != null) {
                for (BluetoothDevice dev : paired) {
                    addDiscoveredDevice(dev, "Системное сопряжение", 0);
                }
            }
        } catch (Exception e) {
            appendLog("Ошибка сопряженных: " + e.getMessage());
        }

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

        // 3. Classic Discovery
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

        scanCountdown = 10;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isScanning && scanCountdown > 0) {
                    statusTextView.setText("Статус: 🔍 Поиск (" + scanCountdown + "с)... Найдено: " + discoveredDevices.size());
                    scanCountdown--;
                    handler.postDelayed(this, 1000);
                } else if (isScanning) {
                    stopAllScans();
                }
            }
        });
    }

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null && result.getDevice() != null) {
                addDiscoveredDevice(result.getDevice(), "BLE Scan", result.getRssi());
            }
        }
    };

    private synchronized void addDiscoveredDevice(BluetoothDevice device, String method, int rssi) {
        FoundDevice newItem = new FoundDevice(device, method, rssi);
        if (!discoveredDevices.contains(newItem)) {
            discoveredDevices.add(newItem);
            appendLog("✨ Найдено: " + (device.getName() != null ? device.getName() : "Без имени") + " [" + device.getAddress() + "] via " + method);
            handler.post(this::renderDeviceCards);
        }
    }

    private void stopAllScans() {
        isScanning = false;
        try {
            if (bleScanner != null) bleScanner.stopScan(bleScanCallback);
            if (bluetoothAdapter != null) bluetoothAdapter.cancelDiscovery();
            if (classicReceiver != null) {
                unregisterReceiver(classicReceiver);
                classicReceiver = null;
            }
        } catch (Exception ignored) {}

        statusTextView.setText("Статус: Поиск завершен ✅ (Объектов: " + discoveredDevices.size() + ")");
        appendLog("🏁 Поиск завершен. Отображено карточек: " + discoveredDevices.size());
    }

    // Рендеринг карточек с кнопками управления для каждого устройства прямо на экране
    private void renderDeviceCards() {
        devicesContainer.removeAllViews();
        for (FoundDevice fd : discoveredDevices) {
            BluetoothDevice dev = fd.device;
            String name = dev.getName() != null ? dev.getName() : "Неизвестное устройство";
            
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
            tvInfo.setText("⌚ " + name + "\n🔗 MAC: " + dev.getAddress() + "\n📍 Метод: " + fd.discoveryMethod + (fd.rssi != 0 ? " | RSSI: " + fd.rssi : ""));
            tvInfo.setTextColor(Color.parseColor("#E2E8F0"));
            tvInfo.setTextSize(11f);
            card.addView(tvInfo);

            // Кнопки управления для конкретного устройства
            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button btnConnect = createSmallButton("🔗 GATT Дамп", "#2563EB");
            btnConnect.setOnClickListener(v -> connectAndDumpGatt(dev));
            btnLayout.addView(btnConnect);

            Button btnBattery = createSmallButton("🔋 Батарея", "#0D9488");
            btnBattery.setOnClickListener(v -> readBattery(dev));
            btnLayout.addView(btnBattery);

            Button btnVibrate = createSmallButton("📳 Вибро", "#7C3AED");
            btnVibrate.setOnClickListener(v -> sendVibrationSignal(dev));
            btnLayout.addView(btnVibrate);

            card.addView(btnLayout);
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
        params.setMargins(2, 4, 2, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    private void connectAndDumpGatt(BluetoothDevice dev) {
        appendLog("🔗 Подключение к GATT: " + dev.getName() + " [" + dev.getAddress() + "]...");
        statusTextView.setText("Статус: Подключение к " + dev.getAddress());

        if (bluetoothGatt != null) {
            try { bluetoothGatt.close(); } catch (Exception ignored) {}
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 31) {
            bluetoothGatt = dev.connectGatt(this, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        appendLog("✅ Успешно подключено к " + dev.getName() + "! Запрос GATT сервисов...");
                        handler.post(() -> infoTextView.setText("📦 Устройство: " + dev.getName() + "\n🔗 MAC: " + dev.getAddress() + "\n⚙️ Статус: Подключено (GATT)"));
                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        appendLog("🔌 Соединение разорвано.");
                        handler.post(() -> infoTextView.setText("📦 Статус: Отключено"));
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> services = gatt.getServices();
                        appendLog("\n=== 🔥 НАЙДЕНО СЕРВИСОВ: " + services.size() + " ===");
                        for (BluetoothGattService s : services) {
                            appendLog("📂 UUID Сервиса: " + s.getUuid().toString());
                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                appendLog("   ├── ⚡ Характеристика: " + c.getUuid().toString());
                            }
                        }
                        appendLog("=== КОНЕЦ ДАМПА ===\n");
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE);
        }
    }

    private void readBattery(BluetoothDevice dev) {
        appendLog("🔋 Запрос батареи для " + dev.getAddress());
        connectAndDumpGatt(dev);
    }

    private void sendVibrationSignal(BluetoothDevice dev) {
        appendLog("📳 Попытка вызова вибрации/уведомления на " + dev.getAddress());
        connectAndDumpGatt(dev);
    }

    private void disconnectGatt() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
            appendLog("🔌 Соединение закрыто пользователем.");
            statusTextView.setText("Статус: Отключено");
            Toast.makeText(this, "Отключено", Toast.LENGTH_SHORT).show();
        }
    }
}