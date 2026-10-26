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
        public int workingTransport = BluetoothDevice.TRANSPORT_AUTO;
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
        authorView.setText("✦ Monster Watch & Audio Hub v8.0 Pro ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        // Верхняя панель управления
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

        btnScan = createStyledButton("🚀 Найти все часы, колонки и наушники", "#2563EB");
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

        // Список устройств
        TextView listLabel = new TextView(this);
        listLabel.setText("📱 Обнаруженные устройства (с понятным управлением):");
        listLabel.setTextColor(Color.parseColor("#38BDF8"));
        listLabel.setTextSize(12f);
        listLabel.setTypeface(Typeface.DEFAULT_BOLD);
        listLabel.setPadding(0, 5, 0, 8);
        container.addView(listLabel);

        devicesContainer = new LinearLayout(this);
        devicesContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(devicesContainer);

        // Консоль логов
        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Отладочный лог:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 10, 0, 4);
        container.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь будет отображаться вся информация и потоки датчиков...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(9);
        logEditText.setMaxLines(15);
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

        appendLog("=== MONSTER ENGINE v8.0 PRO ЗАПУЩЕН ===");
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
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
            }, REQUEST_PERMISSIONS);
            return;
        }
        startUltimateSearch();
    }

    private void startUltimateSearch() {
        discoveredDevices.clear();
        devicesContainer.removeAllViews();
        isScanning = true;

        appendLog("\n--- СТАРТ СУПЕР-ПОИСКА ---");

        // 1. Системные сопряжения
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

        // 2. Агрессивный BLE сканер
        if (bleScanner != null) {
            try {
                ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
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
                    statusTextView.setText("Статус: 🔍 Сканирование (" + scanCountdown + "с)... Найдено: " + discoveredDevices.size());
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

        statusTextView.setText("Статус: Поиск завершен ✅ (Устройств: " + discoveredDevices.size() + ")");
        appendLog("🏁 Поиск завершен. Карточек создано: " + discoveredDevices.size());
    }

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
            tvInfo.setText("⌚ " + name + "\n🔗 MAC: " + dev.getAddress() + "\n📍 Источник: " + fd.discoveryMethod + " | Статус: " + fd.connectionState);
            tvInfo.setTextColor(Color.parseColor("#E2E8F0"));
            tvInfo.setTextSize(11f);
            card.addView(tvInfo);

            // Ряд 1 кнопок (Основные команды часов)
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);

            Button btnConnect = createSmallButton("🔗 Подключить", "#2563EB");
            btnConnect.setOnClickListener(v -> startSmartConnection(fd, "DUMP"));
            row1.addView(btnConnect);

            Button btnBattery = createSmallButton("🔋 Заряд батареи", "#0D9488");
            btnBattery.setOnClickListener(v -> startSmartConnection(fd, "BATTERY"));
            row1.addView(btnBattery);

            Button btnVibe = createSmallButton("📳 Поиск / Вибро", "#7C3AED");
            btnVibe.setOnClickListener(v -> startSmartConnection(fd, "VIBE"));
            row1.addView(btnVibe);

            card.addView(row1);

            // Ряд 2 кнопок (Стриминг, Аудио и Управление)
            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setPadding(0, 4, 0, 0);

            Button btnNotify = createSmallButton("📡 Слушать поток", "#D97706");
            btnNotify.setOnClickListener(v -> startSmartConnection(fd, "NOTIFY"));
            row2.addView(btnNotify);

            Button btnAudio = createSmallButton("🎵 Поток музыки", "#C026D3");
            btnAudio.setOnClickListener(v -> streamAudioToPhone(dev));
            row2.addView(btnAudio);

            Button btnDisconnectDev = createSmallButton("🔌 Отключить", "#DC2626");
            btnDisconnectDev.setOnClickListener(v -> disconnectSpecificDevice(fd));
            row2.addView(btnDisconnectDev);

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

    // Умное подключение с авто-перебором транспортов для устранения ошибок status 4 / 255
    private void startSmartConnection(FoundDevice fd, String actionType) {
        BluetoothDevice dev = fd.device;
        appendLog("\n========================================");
        appendLog("🚀 ПОДКЛЮЧЕНИЕ К: " + dev.getName() + " [" + dev.getAddress() + "]");
        appendLog("========================================");
        
        fd.connectionState = "Подключение...";
        handler.post(this::renderDeviceCards);

        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
        }

        handler.postDelayed(() -> tryConnectWithTransportFallback(fd, actionType, BluetoothDevice.TRANSPORT_AUTO), 300);
    }

    private void tryConnectWithTransportFallback(FoundDevice fd, String actionType, int transport) {
        BluetoothDevice dev = fd.device;
        String tName = (transport == BluetoothDevice.TRANSPORT_LE) ? "BLE (LE)" : (transport == BluetoothDevice.TRANSPORT_BREDR ? "Classic (BREDR)" : "AUTO");
        appendLog("🔄 Попытка сокета с каналом: " + tName);

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 31) {
            appendLog("❌ ОШИБКА: Нет разрешений коннекта!");
            return;
        }

        bluetoothGatt = dev.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    appendLog("⚠️ Ошибка сокета (status=" + status + ") на канале " + tName);
                    try { gatt.close(); } catch (Exception ignored) {}
                    bluetoothGatt = null;

                    // Авто-перебор на резервные каналы, если AUTO не прошел
                    if (transport == BluetoothDevice.TRANSPORT_AUTO) {
                        appendLog("🔄 Пробуем запасной канал: TRANSPORT_LE...");
                        handler.postDelayed(() -> tryConnectWithTransportFallback(fd, actionType, BluetoothDevice.TRANSPORT_LE), 400);
                    } else if (transport == BluetoothDevice.TRANSPORT_LE) {
                        appendLog("🔄 Пробуем классический канал: TRANSPORT_BREDR...");
                        handler.postDelayed(() -> tryConnectWithTransportFallback(fd, actionType, BluetoothDevice.TRANSPORT_BREDR), 400);
                    } else {
                        fd.connectionState = "Ошибка (Код " + status + ")";
                        handler.post(() -> {
                            renderDeviceCards();
                            statusTextView.setText("Статус: ❌ Ошибка подключения (" + status + ")");
                        });
                    }
                    return;
                }

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    fd.connectionState = "Подключено ✅";
                    appendLog("✅ УСПЕШНО! Соединение установлено (" + tName + ")");
                    handler.post(() -> {
                        renderDeviceCards();
                        infoTextView.setText("📦 Устройство: " + dev.getName() + "\n🔗 MAC: " + dev.getAddress() + "\n⚙️ Канал: " + tName);
                        statusTextView.setText("Статус: ✅ Успешно подключено!");
                    });

                    handler.postDelayed(() -> gatt.discoverServices(), 500);

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    fd.connectionState = "Отключено 🔌";
                    appendLog("🔌 Соединение разорвано.");
                    handler.post(() -> {
                        renderDeviceCards();
                        infoTextView.setText("📦 Статус: Отключено");
                    });
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    List<BluetoothGattService> services = gatt.getServices();
                    appendLog("\n=== 🔥 СЕРВИСОВ ОБНАРУЖЕНО: " + services.size() + " ===");
                    
                    for (BluetoothGattService s : services) {
                        String sUuid = s.getUuid().toString();
                        appendLog("📂 Сервис: " + sUuid);
                        
                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            String cUuid = c.getUuid().toString();
                            int props = c.getProperties();
                            appendLog("   ├── ⚡ Характеристика: " + cUuid + " [Права: " + getPropsName(props) + "]");

                            if (actionType.equals("NOTIFY") && ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 || (props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)) {
                                gatt.setCharacteristicNotification(c, true);
                                BluetoothGattDescriptor desc = c.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                                if (desc != null) {
                                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    gatt.writeDescriptor(desc);
                                    appendLog("   └── 🔔 Активирован прием потока от " + cUuid);
                                }
                            }

                            if (actionType.equals("BATTERY") && cUuid.contains("2a19")) {
                                appendLog("🔋 Чтение уровня батареи...");
                                gatt.readCharacteristic(c);
                            }
                        }
                    }

                    if (actionType.equals("VIBE")) {
                        appendLog("📳 Попытка отправить сигнал поиска/вибрации...");
                        for (BluetoothGattService s : services) {
                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                int p = c.getProperties();
                                if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || (p & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                                    c.setValue(new byte[]{0x01, 0x03});
                                    gatt.writeCharacteristic(c);
                                    appendLog("📤 Команда отправлена в " + c.getUuid().toString());
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    appendLog("❌ Ошибка сбора служб: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        int val = data[0] & 0xFF;
                        appendLog("🔋 ЗАРЯД БАТАРЕИ: " + val + "%");
                        handler.post(() -> Toast.makeText(MainActivity.this, "Заряд: " + val + "%", Toast.LENGTH_LONG).show());
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                if (data != null) {
                    StringBuilder hex = new StringBuilder();
                    for (byte b : data) hex.append(String.format("%02X ", b));
                    appendLog("📥 [ПОТОК ДАННЫХ]: " + characteristic.getUuid().toString() + " -> [HEX: " + hex.toString().trim() + "]");
                }
            }
        }, transport);
    }

    // Специальная функция для аудио-устройств (колонок и наушников)
    private void streamAudioToPhone(BluetoothDevice dev) {
        appendLog("🎵 [АУДИО] Проверка музыкального профиля для " + dev.getName() + " [" + dev.getAddress() + "]");
        try {
            // Запуск системных настроек Bluetooth или вызов профиля гарнитуры для перехвата аудио
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Выберите устройство в настройках для потока аудио", Toast.LENGTH_LONG).show();
            appendLog("ℹ️ Открыты настройки Bluetooth для управления аудио-потоком (A2DP).");
        } catch (Exception e) {
            appendLog("❌ Ошибка открытия аудио-панели: " + e.getMessage());
        }
    }

    private void disconnectSpecificDevice(FoundDevice fd) {
        fd.connectionState = "Отключено 🔌";
        handler.post(this::renderDeviceCards);
        
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
        }
        appendLog("🔌 Связь с устройством " + fd.device.getAddress() + " принудительно разорвана.");
        statusTextView.setText("Статус: Соединение закрыто");
        Toast.makeText(this, "Устройство отключено", Toast.LENGTH_SHORT).show();
    }

    private String getPropsName(int p) {
        StringBuilder sb = new StringBuilder();
        if ((p & BluetoothGattCharacteristic.PROPERTY_READ) != 0) sb.append("Чтение ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) sb.append("Запись ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) sb.append("Уведомление ");
        return sb.toString().trim();
    }
}