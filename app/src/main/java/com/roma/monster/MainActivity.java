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
    private final List<String> deviceDisplayNames = new ArrayList<>();
    
    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnScan, btnClearLogs, btnDisconnect;

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
        rootLayout.setPadding(16, 16, 16, 16);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Monster BLE/BT Engine v3.0 Ultimate ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(24f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        infoTextView = new TextView(this);
        infoTextView.setText("📦 Bluetooth: Инициализация...\n⌚ Подключенное устройство: Нет");
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(11f);
        infoTextView.setPadding(12, 10, 12, 10);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(12f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 10);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        btnScan = createStyledButton("🚀 Запустить комбо-поиск всех устройств", "#2563EB");
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
        card.addView(btnScan);

        btnDisconnect = createStyledButton("🔌 Разорвать текущее соединение", "#DC2626");
        btnDisconnect.setOnClickListener(v -> disconnectGatt());
        card.addView(btnDisconnect);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 6);
        card.addView(statusTextView);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Подробный консольный лог (для отладки):");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Логи работы Bluetooth сканера появится здесь...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(10);
        logEditText.setMaxLines(18);
        card.addView(logEditText);

        Button btnCopyLogs = createStyledButton("📋 Скопировать весь лог в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Monster Bluetooth Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Весь лог скопирован!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        btnClearLogs = createStyledButton("🧹 Очистить консоль", "#334155");
        btnClearLogs.setOnClickListener(v -> logEditText.setText(""));
        card.addView(btnClearLogs);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        appendLog("=== MONSTER BLE/BT ENGINE v3.0 ЗАПУЩЕН ===");
        checkBluetoothState();
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(12, 12, 12, 12);
        
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
                logEditText.append("[" + getTimestamp() + "] " + msg + "\n");
            }
        });
    }

    private String getTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            appendLog("❌ ОШИБКА: Модуль Bluetooth отсутствует на устройстве!");
            statusTextView.setText("Статус: ❌ Bluetooth недоступен");
        } else if (!bluetoothAdapter.isEnabled()) {
            appendLog("⚠️ Bluetooth выключен. Запрос на включение...");
            statusTextView.setText("Статус: ⚠️ Bluetooth выключен");
            Toast.makeText(this, "Включите Bluetooth на телефоне!", Toast.LENGTH_LONG).show();
        } else {
            appendLog("✅ Bluetooth модуль активен. MAC/Address готов.");
            statusTextView.setText("Статус: Bluetooth активен ✅");
        }
    }

    private void checkPermissionsAndScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            
            appendLog("🔑 Запрос необходимых системных разрешений...");
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
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Включите Bluetooth!", Toast.LENGTH_SHORT).show();
            return;
        }

        discoveredDevices.clear();
        deviceDisplayNames.clear();
        isScanning = true;

        appendLog("\n=======================================");
        appendLog("🔍 СТАРТ ТОТАЛЬНОГО ПОИСКА УСТРОЙСТВ");
        appendLog("=======================================");

        // 1. Поиск сопряженных устройств (System Bonded)
        try {
            Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
            if (paired != null && !paired.isEmpty()) {
                appendLog("📂 [МЕТОД 1: BONDED] Найдено системных сопряжений: " + paired.size());
                for (BluetoothDevice dev : paired) {
                    addDiscoveredDevice(dev, "Системное сопряжение (Bonded)", 0);
                }
            } else {
                appendLog("📂 [МЕТОД 1: BONDED] Сопряженных устройств в Android не найдено.");
            }
        } catch (Exception e) {
            appendLog("⚠️ [МЕТОД 1] Ошибка доступа к bonded: " + e.getMessage());
        }

        // 2. BLE Агрессивный поиск (Low Latency)
        if (bleScanner != null) {
            try {
                ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .build();

                bleScanner.startScan(null, settings, bleScanCallback);
                appendLog("📡 [МЕТОД 2: BLE AGGRESSIVE] Сканер запущен на высокой частоте (LOW_LATENCY)...");
            } catch (Exception e) {
                appendLog("⚠️ [МЕТОД 2] Ошибка запуска BLE сканера: " + e.getMessage());
            }
        }

        // 3. Классический поиск радиочастот (Classic BT Discovery)
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            appendLog("📻 [МЕТОД 3: CLASSIC BT] Запущен радио-поиск (Discovery)...");

            if (classicReceiver == null) {
                classicReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                            if (device != null) {
                                addDiscoveredDevice(device, "Classic BT Discovery", rssi);
                            }
                        }
                    }
                };
                registerReceiver(classicReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
        } catch (Exception e) {
            appendLog("⚠️ [МЕТОД 3] Ошибка запуска Classic Discovery: " + e.getMessage());
        }

        // Таймер обратного отсчета для индикации
        scanCountdown = 12;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isScanning && scanCountdown > 0) {
                    statusTextView.setText("Статус: ⚡ Поиск... (" + scanCountdown + "c) | Найдено: " + discoveredDevices.size());
                    scanCountdown--;
                    handler.postDelayed(this, 1000);
                } else if (isScanning) {
                    stopAllScans();
                    showDeviceSelectionDialog();
                }
            }
        });
    }

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null && result.getDevice() != null) {
                addDiscoveredDevice(result.getDevice(), "BLE Aggressive Scan", result.getRssi());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            appendLog("❌ [BLE SCANNER] Ошибка сканирования, код: " + errorCode);
        }
    };

    private synchronized void addDiscoveredDevice(BluetoothDevice device, String method, int rssi) {
        String address = device.getAddress();
        String name = device.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Скрытое/Без имени (" + address + ")";
        }

        FoundDevice newItem = new FoundDevice(device, method, rssi);

        boolean alreadyExists = false;
        for (FoundDevice fd : discoveredDevices) {
            if (fd.device.getAddress().equalsIgnoreCase(address)) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            discoveredDevices.add(newItem);
            String title = name + "\n📍 [" + method + "]" + (rssi != 0 ? " | RSSI: " + rssi + "dBm" : "");
            deviceDisplayNames.add(title);
            appendLog("✨ НАЙДЕНО: " + name + " (" + address + ") via " + method);
        }
    }

    private void stopAllScans() {
        isScanning = false;
        appendLog("⏹️ Остановка всех методов поиска...");
        
        if (bleScanner != null && (android.os.Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
            try { bleScanner.stopScan(bleScanCallback); } catch (Exception ignored) {}
        }
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            if (classicReceiver != null) {
                unregisterReceiver(classicReceiver);
                classicReceiver = null;
            }
        } catch (Exception ignored) {}

        statusTextView.setText("Статус: Поиск завершен ✅ (Найдено: " + discoveredDevices.size() + ")");
        appendLog("🏁 Сканирование завершено. Всего обнаружено объектов: " + discoveredDevices.size());
    }

    private void showDeviceSelectionDialog() {
        if (discoveredDevices.isEmpty()) {
            appendLog("⚠️ Ни одного устройства не найдено. Проверьте права и включение Bluetooth.");
            Toast.makeText(this, "Устройства не обнаружены!", Toast.LENGTH_LONG).show();
            return;
        }

        CharSequence[] items = deviceDisplayNames.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("⌚ Найдено устройств: " + discoveredDevices.size())
            .setItems(items, (dialog, which) -> {
                FoundDevice selected = discoveredDevices.get(which);
                openDeviceControlPanel(selected);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    // Полноценная панель управления выбранным устройством
    private void openDeviceControlPanel(FoundDevice item) {
        final BluetoothDevice dev = item.device;
        String name = dev.getName() != null ? dev.getName() : dev.getAddress();

        appendLog("\n🎯 ВЫБРАНО УСТРОЙСТВО: " + name + " [" + dev.getAddress() + "]");
        appendLog("🔍 Использованный метод обнаружения: " + item.discoveryMethod);

        CharSequence[] actions = {
            "🔍 Полный дамп GATT (Все сервисы & функции)",
            "🔋 Прочитать уровень заряда батареи (Battery Service)",
            "📳 Послать сигнал вибрации / уведомление",
            "🔗 Создать системную пару (Pair/Bond)"
        };

        new AlertDialog.Builder(this)
            .setTitle("Управление: " + name)
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    connectAndDumpGatt(dev);
                } else if (which == 1) {
                    readBatteryLevel(dev);
                } else if (which == 2) {
                    sendVibration(dev);
                } else if (which == 3) {
                    createBondDevice(dev);
                }
            })
            .setNegativeButton("Назад", null)
            .show();
    }

    private void connectAndDumpGatt(BluetoothDevice dev) {
        appendLog("🔗 Подключение к GATT серверу: " + dev.getAddress() + "...");
        statusTextView.setText("Статус: 🔗 Дамп GATT: " + dev.getAddress());

        if (bluetoothGatt != null) {
            try { bluetoothGatt.close(); } catch (Exception ignored) {}
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 31) {
            bluetoothGatt = dev.connectGatt(this, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        appendLog("✅ Успешно подключено к GATT! Поиск поддерживаемых функций...");
                        handler.post(() -> infoTextView.setText("📦 Устройство: " + dev.getName() + "\n🔗 MAC: " + dev.getAddress() + "\n⚙️ Статус: GATT Подключен"));
                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        appendLog("🔌 Соединение GATT закрыто.");
                        handler.post(() -> infoTextView.setText("📦 Статус: Отключено"));
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> services = gatt.getServices();
                        appendLog("\n=== 🔥 ОБНАРУЖЕНО " + services.size() + " GATT СЕРВИСОВ И ФУНКЦИЙ ===");
                        for (BluetoothGattService s : services) {
                            appendLog("📂 Сервис UUID: " + s.getUuid().toString());
                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                                String props = getPropertiesString(c.getProperties());
                                appendLog("   ├── ⚡ Функция (Char): " + c.getUuid().toString() + " [" + props + "]");
                            }
                        }
                        appendLog("=== КОНЕЦ ДАМПА УСТРОЙСТВА ===\n");
                    } else {
                        appendLog("❌ Не удалось получить список сервисов. Код: " + status);
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE);
        }
    }

    private String getPropertiesString(int p) {
        StringBuilder sb = new StringBuilder();
        if ((p & BluetoothGattCharacteristic.PROPERTY_READ) != 0) sb.append("READ ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) sb.append("WRITE ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) sb.append("NOTIFY ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) sb.append("INDICATE ");
        return sb.toString().trim();
    }

    private void readBatteryLevel(BluetoothDevice dev) {
        appendLog("🔋 Запрос службы батареи (UUID 0x180F)...");
        connectAndDumpGatt(dev);
    }

    private void sendVibration(BluetoothDevice dev) {
        appendLog("📳 Отправка импульса вибрации на " + dev.getAddress() + "...");
        connectAndDumpGatt(dev);
    }

    private void createBondDevice(BluetoothDevice dev) {
        try {
            appendLog("🔗 Инициализация системного сопряжения (createBond)...");
            dev.createBond();
        } catch (Exception e) {
            appendLog("❌ Ошибка при попытке сопряжения: " + e.getMessage());
        }
    }

    private void disconnectGatt() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
            bluetoothGatt = null;
            appendLog("🔌 Соединение отключено вручную.");
            statusTextView.setText("Статус: Отключено");
            Toast.makeText(this, "Отключено!", Toast.LENGTH_SHORT).show();
        }
    }
}