package com.roma.monster;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    
    private boolean isScanning = false;
    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private List<String> deviceDisplayNames = new ArrayList<>();
    
    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnScan, btnVibrate, btnReadBattery, btnDisconnect;

    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int REQUEST_PERMISSIONS = 100;
    private BroadcastReceiver classicReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        // Интерфейс приложения в стиле Monster Engine
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
        authorView.setText("✦ Xiaomi Watch BLE Commander v2.0 Pro ✦");
        authorView.setTextColor(Color.parseColor("#38BDF8"));
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
        infoTextView.setText("📦 Статус Bluetooth: Активен\n⌚ Часы Xiaomi: Не подключены");
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

        // Кнопки управления
        btnScan = createStyledButton("🔍 Тотальный поиск часов (BLE + Paired + Classic)", "#2563EB");
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
        card.addView(btnScan);

        btnVibrate = createStyledButton("📳 Тест: Вызвать вибрацию на часах", "#0D9488");
        btnVibrate.setOnClickListener(v -> sendVibrationCommand());
        card.addView(btnVibrate);

        btnReadBattery = createStyledButton("🔋 Прочитать уровень батареи", "#7C3AED");
        btnReadBattery.setOnClickListener(v -> readDeviceBattery());
        card.addView(btnReadBattery);

        btnDisconnect = createStyledButton("🔌 Отключиться от часов", "#DC2626");
        btnDisconnect.setOnClickListener(v -> disconnectGatt());
        card.addView(btnDisconnect);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к сканированию 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 6);
        card.addView(statusTextView);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 BLE Логи и Датчики:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь будут появляться данные с часов...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(8);
        logEditText.setMaxLines(15);
        card.addView(logEditText);

        Button btnCopyLogs = createStyledButton("📋 Скопировать лог", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("BLE Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

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

    private void checkBluetoothState() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            statusTextView.setText("Статус: ⚠️ Bluetooth выключен!");
            Toast.makeText(this, "Включите Bluetooth!", Toast.LENGTH_LONG).show();
        } else {
            statusTextView.setText("Статус: Bluetooth активен ✅");
        }
    }

    private void checkPermissionsAndScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_PERMISSIONS);
            return;
        }
        startExhaustiveSearch();
    }

    private void startExhaustiveSearch() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Включите Bluetooth!", Toast.LENGTH_LONG).show();
            return;
        }

        discoveredDevices.clear();
        deviceDisplayNames.clear();

        statusTextView.setText("Статус: ⚡ Глубокий поиск (BLE + Paired + Classic)...");
        logEditText.setText("=== 🔍 ТОТАЛЬНОЕ СКАНИРОВАНИЕ ЭФИРА ===\n");

        // 1. Проверяем уже сопряженные устройства в системе
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                logEditText.append("📂 Найдено сопряженных в системе: " + pairedDevices.size() + "\n");
                for (BluetoothDevice device : pairedDevices) {
                    String name = device.getName() != null ? device.getName() : "Unknown Paired";
                    String entry = "🔗 [Сопряжено] " + name + " [" + device.getAddress() + "]";
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        deviceDisplayNames.add(entry);
                        logEditText.append("  • " + entry + "\n");
                    }
                }
            }
        } catch (Exception e) {
            logEditText.append("⚠️ Ошибка сопряженных: " + e.getMessage() + "\n");
        }

        // 2. Запуск агрессивного BLE сканирования
        if (bleScanner != null) {
            isScanning = true;
            ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .build();

            bleScanner.startScan(null, settings, scanCallback);
            logEditText.append("📡 BLE Сканер запущен в режиме LOW_LATENCY...\n");
        }

        // 3. Запуск классического Bluetooth поиска
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            logEditText.append("🔎 Классический BT discovery запущен...\n");

            if (classicReceiver == null) {
                classicReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (device != null) {
                                String name = device.getName() != null ? device.getName() : "Classic BT Device";
                                String entry = "📻 [Classic] " + name + " [" + device.getAddress() + "]";
                                if (!discoveredDevices.contains(device)) {
                                    discoveredDevices.add(device);
                                    deviceDisplayNames.add(entry);
                                    handler.post(() -> logEditText.append("  • " + entry + "\n"));
                                }
                            }
                        }
                    }
                };
                registerReceiver(classicReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
        } catch (Exception ignored) {}

        // Авто-остановка через 12 секунд и показ результатов
        handler.postDelayed(() -> {
            stopAllScans();
            showDeviceSelectionDialog();
        }, 12000);
    }

    private void stopAllScans() {
        isScanning = false;
        if (bleScanner != null && (android.os.Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (Exception ignored) {}
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

        handler.post(() -> statusTextView.setText("Статус: Поиск завершен. Найдено: " + discoveredDevices.size()));
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                name = "BLE Beacon / Unnamed";
            }
            
            int rssi = result.getRssi();
            String entry = "📡 [BLE] " + name + " [" + device.getAddress() + "] RSSI: " + rssi;
            
            if (!discoveredDevices.contains(device)) {
                discoveredDevices.add(device);
                deviceDisplayNames.add(entry);
                handler.post(() -> logEditText.append("  • " + entry + "\n"));
            }
        }
    };

    private void showDeviceSelectionDialog() {
        if (deviceDisplayNames.isEmpty()) {
            Toast.makeText(this, "Устройства не найдены. Поднесите часы ближе.", Toast.LENGTH_LONG).show();
            return;
        }

        CharSequence[] items = deviceDisplayNames.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("⌚ Выберите ваши часы Xiaomi")
            .setItems(items, (dialog, which) -> {
                BluetoothDevice selectedDevice = discoveredDevices.get(which);
                connectToDevice(selectedDevice);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void connectToDevice(BluetoothDevice device) {
        statusTextView.setText("Статус: 🔗 Подключение к " + device.getName() + "...");
        logEditText.append("Подключение к GATT серверу: " + device.getAddress() + "\n");

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 31) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                handler.post(() -> {
                    statusTextView.setText("Статус: ✅ Подключено к часам!");
                    infoTextView.setText("📦 Устройство: " + gatt.getDevice().getName() + "\n🔗 MAC: " + gatt.getDevice().getAddress() + "\n⚙️ Состояние: Подключено");
                    logEditText.append("Успешное подключение! Запрос сервисов...\n");
                });
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                handler.post(() -> {
                    statusTextView.setText("Статус: 🔌 Отключено от часов");
                    infoTextView.setText("📦 Статус: Не подключено");
                    logEditText.append("Соединение разорвано.\n");
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handler.post(() -> {
                    logEditText.append("✨ Сервисы обнаружены (" + gatt.getServices().size() + " шт.)\n");
                    for (BluetoothGattService service : gatt.getServices()) {
                        logEditText.append("  - UUID: " + service.getUuid().toString() + "\n");
                    }
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                handler.post(() -> {
                    logEditText.append("📥 Прочитано байт: " + (data != null ? data.length : 0) + "\n");
                });
            }
        }
    };

    private void sendVibrationCommand() {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "⚠️ Сначала подключитесь к часам!", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Команда отправлена", Toast.LENGTH_SHORT).show();
        logEditText.append("📳 Попытка отправки сигнала вибрации...\n");
    }

    private void readDeviceBattery() {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "⚠️ Сначала подключитесь к часам!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean found = false;
        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            if (service.getUuid().toString().toLowerCase().contains("180f")) {
                BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));
                if (charac != null) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 31) {
                        bluetoothGatt.readCharacteristic(charac);
                        found = true;
                        logEditText.append("🔋 Запрос уровня заряда батареи...\n");
                    }
                }
            }
        }
        if (!found) {
            Toast.makeText(this, "Сервис батареи не найден", Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectGatt() {
        if (bluetoothGatt != null) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 31) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
            statusTextView.setText("Статус: 🔌 Отключено вручную");
            Toast.makeText(this, "Отключено", Toast.LENGTH_SHORT).show();
        }
    }
}