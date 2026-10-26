package com.app.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    private Button btnScan, btnConnectLast, btnVibrate, btnReadBattery, btnDisconnect;

    private Handler handler = new Handler(Looper.getMainLocate() != null ? Looper.getMainLooper() : Looper.getMainLooper());
    private static final int REQUEST_PERMISSIONS = 100;

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
        authorView.setText("✦ Xiaomi Watch BLE Commander v1.0 ✦");
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
        infoTextView.setText("📦 Статус Bluetooth: Проверка...\n⌚ Часы Xiaomi: Не подключены");
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
        btnScan = createStyledButton("🔍 Найти Xiaomi часы (BLE Scan)", "#2563EB");
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
                Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_PERMISSIONS);
            return;
        }
        startBleScan();
    }

    private void startBleScan() {
        if (bleScanner == null) {
            Toast.makeText(this, "BLE Scanner недоступен", Toast.LENGTH_SHORT).show();
            return;
        }

        discoveredDevices.clear();
        deviceDisplayNames.clear();
        
        statusTextView.setText("Статус: 🔍 Сканирование эфира (10 сек)...");
        logEditText.setText("=== ПОИСК BLUETOOTH УСТРОЙСТВ ===\n");

        isScanning = true;
        bleScanner.startScan(scanCallback);

        // Остановить сканирование через 10 секунд
        handler.postDelayed(() -> {
            if (isScanning) {
                bleScanner.stopScan(scanCallback);
                isScanning = false;
                statusTextView.setText("Статус: Сканирование завершено");
                showDeviceSelectionDialog();
            }
        }, 10000);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                name = "Unknown BLE Device";
            }
            
            // Фильтруем или выводим все, но обращаем внимание на Xiaomi/Mi/Smart Watch
            String entry = name + " [" + device.getAddress() + "]" + (result.getRssi() != 0 ? " RSSI: " + result.getRssi() : "");
            if (!discoveredDevices.contains(device)) {
                discoveredDevices.add(device);
                deviceDisplayNames.add(entry);
                
                logEditText.append("• Найдено: " + entry + "\n");
            }
        }
    };

    private void showDeviceSelectionDialog() {
        if (deviceDisplayNames.isEmpty()) {
            Toast.makeText(this, "Устройства не найдены. Попробуйте ближе поднести часы.", Toast.LENGTH_LONG).show();
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
        logEditText.setText("Подключение к GATT серверу: " + device.getAddress() + "\n");

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
                    infoTextView.setText("📦 Устройство: " + gatt.getDevice().getName() + "\n🔗 MAC: " + gatt.getDevice().getAddress() + "\n⚙️ Состояние: Подключено (GATT)");
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
                        logEditText.append("  - UUID: " + service.getUuid().toString().substring(4, 8) + "\n");
                    }
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                handler.post(() -> {
                    logEditText.append("📥 Данные характеристик прочитаны: " + (data != null ? data.length + " байт" : "пусто") + "\n");
                });
            }
        }
    };

    private void sendVibrationCommand() {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "⚠️ Сначала подключитесь к часам!", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Команда вибрации отправлена в лог", Toast.LENGTH_SHORT).show();
        logEditText.append("📳 Попытка отправки сигнала вибрации на браслет/часы...\n");
        // Примечание: У Xiaomi для вибрации используется запись в специфическую характеристику Alert Notification Service (UUID 0x1811 или аналог Mi Band)
    }

    private void readDeviceBattery() {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "⚠️ Сначала подключитесь к часам!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Поиск стандартного сервиса батареи (Battery Service UUID: 0x180F)
        boolean found = false;
        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            if (service.getUuid().toString().toLowerCase().contains("180f")) {
                BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));
                if (charac != null) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 31) {
                        bluetoothGatt.readCharacteristic(charac);
                        found = true;
                        logEditText.append("🔋 Запрос уровня заряда батареи отправлен...\n");
                    }
                }
            }
        }
        if (!found) {
            Toast.makeText(this, "Сервис батареи не найден на этом устройстве", Toast.LENGTH_SHORT).show();
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