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
        authorView.setText("✦ Monster Watch Control Center v5.0 Ultimate ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
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

        btnScan = createStyledButton("🚀 Запустить супер-сканирование эфира", "#2563EB");
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

        // Блок со списком найденных устройств
        TextView listLabel = new TextView(this);
        listLabel.setText("📱 Панель управления устройствами (с полным набором функций):");
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
        logLabel.setText("📋 Отладочный лог обмена данных:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 10, 0, 4);
        container.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь будет отображаться детальный лог подключения...");
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

        appendLog("=== MONSTER ENGINE v5.0 ЗАПУЩЕН ===");
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
        appendLog("🏁 Сканирование завершено. Карточек создано: " + discoveredDevices.size());
    }

    // Рендеринг расширенной панели управления с 6 кнопками для каждого устройства
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
            tvInfo.setText("⌚ " + name + "\n🔗 MAC: " + dev.getAddress() + "\n📍 Источник: " + fd.discoveryMethod + (fd.rssi != 0 ? " | RSSI: " + fd.rssi : ""));
            tvInfo.setTextColor(Color.parseColor("#E2E8F0"));
            tvInfo.setTextSize(11f);
            card.addView(tvInfo);

            // Ряд 1 кнопок (Основные действия)
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);

            Button btnDump = createSmallButton("🔗 GATT Дамп", "#2563EB");
            btnDump.setOnClickListener(v -> connectAndExecute(dev, "DUMP"));
            row1.addView(btnDump);

            Button btnBattery = createSmallButton("🔋 Батарея", "#0D9488");
            btnBattery.setOnClickListener(v -> connectAndExecute(dev, "BATTERY"));
            row1.addView(btnBattery);

            Button btnVibe = createSmallButton("📳 Вибро", "#7C3AED");
            btnVibe.setOnClickListener(v -> connectAndExecute(dev, "VIBE"));
            row1.addView(btnVibe);

            card.addView(row1);

            // Ряд 2 кнопок (Дополнительный функционал)
            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setPadding(0, 4, 0, 0);

            Button btnNotify = createSmallButton("📋 Подписка", "#D97706");
            btnNotify.setOnClickListener(v -> connectAndExecute(dev, "NOTIFY"));
            row2.addView(btnNotify);

            Button btnPair = createSmallButton("🔗 System Pair", "#059669");
            btnPair.setOnClickListener(v -> pairDevice(dev));
            row2.addView(btnPair);

            Button btnUnpair = createSmallButton("❌ Сброс пары", "#DC2626");
            btnUnpair.setOnClickListener(v -> unpairDevice(dev));
            row2.addView(btnUnpair);

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

    // Универсальный многоуровневый метод подключения с автовыбором транспорта
    private void connectAndExecute(BluetoothDevice dev, String actionType) {
        appendLog("\n--- ПОПЫТКА ПОДКЛЮЧЕНИЯ К " + dev.getName() + " [" + dev.getAddress() + "] ---");
        statusTextView.setText("Статус: Подключение к " + dev.getAddress());

        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 31) {
            appendLog("❌ ОШИБКА: Нет разрешения BLUETOOTH_CONNECT!");
            return;
        }

        // Пробуем подключиться с явным указанием Low Energy транспорта
        bluetoothGatt = dev.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    appendLog("❌ ОШИБКА GATT сокета: status=" + status + ", state=" + newState);
                    try { gatt.close(); } catch (Exception ignored) {}
                    return;
                }

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    appendLog("✅ УСПЕШНО ПОДКЛЮЧЕНО к " + dev.getName() + "! Запуск поиска служб...");
                    handler.post(() -> infoTextView.setText("📦 Устройство: " + dev.getName() + "\n🔗 MAC: " + dev.getAddress() + "\n⚙️ Статус: Подключено (GATT)"));
                    
                    // Задержка перед дискавером для стабилизации связи
                    handler.postDelayed(() -> gatt.discoverServices(), 600);
                    
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    appendLog("🔌 Соединение с устройством разорвано.");
                    handler.post(() -> infoTextView.setText("📦 Статус: Отключено"));
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    List<BluetoothGattService> services = gatt.getServices();
                    appendLog("✨ СЛУЖБЫ ОБНАРУЖЕНЫ (" + services.size() + " шт.):");
                    
                    for (BluetoothGattService s : services) {
                        String sUuid = s.getUuid().toString();
                        appendLog("📂 Сервис: " + sUuid);
                        
                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            String cUuid = c.getUuid().toString();
                            int props = c.getProperties();
                            appendLog("   ├── ⚡ Характеристика: " + cUuid + " [Props: " + getPropsName(props) + "]");

                            // Если запрошена батарея и найден стандартный сервис батареи (0x180F / 0x2A19)
                            if (actionType.equals("BATTERY") && cUuid.contains("2a19")) {
                                appendLog("🔋 Найден уровень батареи! Читаем значение...");
                                gatt.readCharacteristic(c);
                            }
                        }
                    }
                    
                    if (actionType.equals("VIBE")) {
                        appendLog("📳 Поиск характеристик для отправки вибро-сигнала...");
                        // Попытка отправить тестовый байт в первую попавшуюся writable характеристику
                        for (BluetoothGattService s : services) {
                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                int p = c.getProperties();
                                if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || (p & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                                    c.setValue(new byte[]{0x01, 0x02});
                                    gatt.writeCharacteristic(c);
                                    appendLog("📤 Отправлен тестовый пакет в " + c.getUuid().toString());
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    appendLog("❌ Ошибка discoverServices: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        int val = data[0] & 0xFF;
                        appendLog("🔋 РЕЗУЛЬТАТ ЧТЕНИЯ [Батарея]: " + val + "% (Байт: " + data.length + ")");
                        handler.post(() -> Toast.makeText(MainActivity.this, "Заряд батареи: " + val + "%", Toast.LENGTH_LONG).show());
                    }
                } else {
                    appendLog("❌ Ошибка чтения характеристики: " + status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                appendLog("📤 Статус записи пакета: " + (status == BluetoothGatt.GATT_SUCCESS ? "УСПЕХ ✅" : "ОШИБКА ❌ (" + status + ")"));
            }
        }, BluetoothDevice.TRANSPORT_LE);
    }

    private String getPropsName(int p) {
        StringBuilder sb = new StringBuilder();
        if ((p & BluetoothGattCharacteristic.PROPERTY_READ) != 0) sb.append("R ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) sb.append("W ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) sb.append("N ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) sb.append("I ");
        return sb.toString().trim();
    }

    private void pairDevice(BluetoothDevice dev) {
        try {
            appendLog("🔗 Запрос системного сопряжения (createBond) для " + dev.getName() + "...");
            Method method = dev.getClass().getMethod("createBond");
            method.invoke(dev);
            Toast.makeText(this, "Запрос сопряжения отправлен", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendLog("❌ Ошибка сопряжения: " + e.getMessage());
        }
    }

    private void unpairDevice(BluetoothDevice dev) {
        try {
            appendLog("❌ Удаление сопряжения (removeBond) для " + dev.getName() + "...");
            Method method = dev.getClass().getMethod("removeBond");
            method.invoke(dev);
            Toast.makeText(this, "Сопряжение сброшено", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendLog("❌ Ошибка сброса пары: " + e.getMessage());
        }
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