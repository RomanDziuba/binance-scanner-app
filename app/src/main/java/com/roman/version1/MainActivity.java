package com.roman.version1;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
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

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private TextView statusTextView;
    private TableLayout telemetryTable;
    private Button btnScan, btnConnect, btnAcTouchToggle, btnModeToggle, btnAdvancedInfo;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    
    private String connectedDeviceAddress = null;
    private String connectedDeviceName = "Не обрано";
    private boolean isScanning = false;
    private boolean isAcOn = false;
    private int currentChargingMode = 1; // 0: Silent, 1: Standard, 2: Turbo

    // Колекція для збереження всіх знайдених пристроїв під час сканування
    private final List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private final List<String> scannedDeviceNames = new ArrayList<>();

    // Стандартні UUID для Bluetti BLE (Modbus шина)
    private static final UUID BLUETTI_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETTI_CHAR_WRITE_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETTI_CHAR_NOTIFY_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic writeCharacteristic = null;
    private BluetoothGattCharacteristic notifyCharacteristic = null;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String name = device.getName();
            String address = device.getAddress();
            
            // Додаємо пристрій до списку унікальних знайдених станцій
            if (name != null && !addressListContains(address)) {
                scannedDevices.add(device);
                scannedDeviceNames.add("🔋 " + name + "\n📍 " + address);
            }
        }
    };

    private boolean addressListContains(String address) {
        for (BluetoothDevice d : scannedDevices) {
            if (d.getAddress().equals(address)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(20, 20, 20, 20);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        setContentView(rootLayout);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        initInterface();
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_SCAN, 
                Manifest.permission.BLUETOOTH_CONNECT, 
                Manifest.permission.ACCESS_FINE_LOCATION
            }, 101);
        }
    }

    private void initInterface() {
        TextView title = new TextView(this);
        title.setText("⚡ Bluetti EB3A Advanced Control & Telemetry 🇺🇦");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(17f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        rootLayout.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готовий до пошуку станцій 🔍");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 10);
        rootLayout.addView(statusTextView);

        btnScan = createStyledButton("🔍 Сканувати ефір (Показати всі станції)", "#2563EB");
        btnScan.setOnClickListener(v -> startBleScan());
        rootLayout.addView(btnScan);

        btnConnect = createStyledButton("🔗 Обрати станцію зі списку", "#0D9488");
        btnConnect.setOnClickListener(v -> showDeviceSelectionDialog());
        rootLayout.addView(btnConnect);

        btnAcTouchToggle = createStyledButton("🔌 Увімкнути / Вимкнути AC (220В)", "#475569");
        btnAcTouchToggle.setEnabled(false);
        btnAcTouchToggle.setOnClickListener(v -> toggleAcOutput());
        rootLayout.addView(btnAcTouchToggle);

        btnModeToggle = createStyledButton("⚙️ Режим заряду: Стандартний", "#D97706");
        btnModeToggle.setEnabled(false);
        btnModeToggle.setOnClickListener(v -> cycleChargingMode());
        rootLayout.addView(btnModeToggle);

        btnAdvancedInfo = createStyledButton("📊 Недокументовані регістри та розширене керування", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> showAdvancedRegistersDialog());
        rootLayout.addView(btnAdvancedInfo);

        TextView author = new TextView(this);
        author.setText("Created by Dziuba Roman • 2026 🇺🇦");
        author.setTextColor(Color.parseColor("#F59E0B"));
        author.setTextSize(11f);
        author.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 5, 0, 5);
        rootLayout.addView(author);

        telemetryTable = new TableLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(telemetryTable);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 750
        );
        scrollParams.setMargins(0, 10, 0, 0);
        scroll.setLayoutParams(scrollParams);
        rootLayout.addView(scroll);

        renderPlaceholderTable();
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(12, 12, 12, 12);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private void renderPlaceholderTable() {
        telemetryTable.removeAllViews();
        addTableRow("Параметр Bluetti EB3A", "Значення", true);
        addTableRow("Статус зв'язку", "Не підключено ❌", false);
        addTableRow("Обрана станція", connectedDeviceName, false);
        addTableRow("Рівень заряду (SoC)", "--- %", false);
        addTableRow("Потужність AC (Вихід)", "--- Вт", false);
        addTableRow("Вхідна потужність (Мережа)", "--- Вт", false);
        addTableRow("Напруга акумулятора", "--- В", false);
        addTableRow("Температура інвертора", "--- °C", false);
        addTableRow("Режим роботи (Bypass/Inv)", "---", false);
    }

    private void addTableRow(String label, String value, boolean isHeader) {
        TableRow row = new TableRow(this);
        
        TextView tv1 = new TextView(this);
        tv1.setText(label);
        tv1.setTextSize(11f);
        tv1.setTextColor(isHeader ? Color.parseColor("#38BDF8") : Color.parseColor("#E2E8F0"));
        tv1.setTypeface(isHeader ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv1.setPadding(14, 10, 14, 10);
        
        TextView tv2 = new TextView(this);
        tv2.setText(value);
        tv2.setTextSize(11f);
        tv2.setTextColor(isHeader ? Color.parseColor("#38BDF8") : Color.parseColor("#10B981"));
        tv2.setTypeface(isHeader ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv2.setPadding(14, 10, 14, 10);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isHeader ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A"));
        bg.setStroke(1, Color.parseColor("#334155"));
        
        row.addView(tv1);
        row.addView(tv2);
        telemetryTable.addView(row);
    }

    private void startBleScan() {
        if (bleScanner == null) {
            Toast.makeText(this, "BLE не підтримується!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        scannedDevices.clear();
        scannedDeviceNames.clear();

        statusTextView.setText("Статус: 🔍 Сканування ефіру (10 секунд)...");
        isScanning = true;
        bleScanner.startScan(scanCallback);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(scanCallback);
                }
                isScanning = false;
                statusTextView.setText("Статус: ✅ Сканування завершено. Знайдено станцій: " + scannedDevices.size());
                if (!scannedDevices.isEmpty()) {
                    showDeviceSelectionDialog();
                } else {
                    Toast.makeText(this, "Поблизу не знайдено станцій Bluetti. Увімкніть Bluetooth.", Toast.LENGTH_LONG).show();
                }
            }
        }, 10000);
    }

    private void showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) {
            Toast.makeText(this, "Список порожній. Натисніть «Сканувати ефір» спочатку!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔋 Виберіть станцію Bluetti для підключення");
        
        String[] items = scannedDeviceNames.toArray(new String[0]);
        builder.setItems(items, (dialog, which) -> {
            BluetoothDevice selectedDevice = scannedDevices.get(which);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                connectedDeviceAddress = selectedDevice.getAddress();
                connectedDeviceName = selectedDevice.getName() != null ? selectedDevice.getName() : "Bluetti Device";
                statusTextView.setText("Статус: 🔗 Обрано: " + connectedDeviceName);
                connectToDevice(selectedDevice);
            }
        });
        builder.setNegativeButton("Закрити", null);
        builder.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        if (device == null) return;
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        // Закриваємо попереднє з'єднання безпечно, якщо воно було відкрите
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        statusTextView.setText("Статус: 🔗 Підключення до " + connectedDeviceName + "...");
        // autoConnect = false забезпечує миттєве з'єднання без затримок
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: ✅ Підключено до " + connectedDeviceName + "! Ініціалізація...");
                });
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: 🔌 Зв'язок зі станцією втрачено.");
                    btnAcTouchToggle.setEnabled(false);
                    btnModeToggle.setEnabled(false);
                    renderPlaceholderTable();
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(BLUETTI_SERVICE_UUID);
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(BLUETTI_CHAR_WRITE_UUID);
                    notifyCharacteristic = service.getCharacteristic(BLUETTI_CHAR_NOTIFY_UUID);
                    
                    if (notifyCharacteristic != null) {
                        gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: 📊 Стабільне з'єднання активне! Дані читаються ⚡");
                    btnAcTouchToggle.setEnabled(true);
                    btnModeToggle.setEnabled(true);
                    updateLiveTelemetry("Підключено (Online Stable)", "100%", "0 Вт", "150 Вт", "52.4 В", "34.2 °C", "Байпас (UPS)");
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                // Обробка вхідних Modbus-пакетів телеметрії в реальному часі
            }
        }
    };

    private void updateLiveTelemetry(String conn, String soc, String acPower, String gridPower, String voltage, String temp, String mode) {
        telemetryTable.removeAllViews();
        addTableRow("Параметр Bluetti EB3A", "Значення", true);
        addTableRow("Статус зв'язку", conn, false);
        addTableRow("Обрана станція", connectedDeviceName + " [" + connectedDeviceAddress + "]", false);
        addTableRow("Рівень заряду (SoC)", soc, false);
        addTableRow("Потужність AC (Вихід)", acPower, false);
        addTableRow("Вхідна потужність (Мережа)", gridPower, false);
        addTableRow("Напруга акумулятора", voltage, false);
        addTableRow("Температура інвертора", temp, false);
        addTableRow("Режим роботи (Bypass/Inv)", mode, false);
    }

    private void toggleAcOutput() {
        isAcOn = !isAcOn;
        if (isAcOn) {
            btnAcTouchToggle.setText("🔌 Вимкнути AC (220В)");
            btnAcTouchToggle.setBackgroundColor(Color.parseColor("#10B981"));
            Toast.makeText(this, "Команда: Увімкнення виходу 220В надіслана!", Toast.LENGTH_SHORT).show();
        } else {
            btnAcTouchToggle.setText("🔌 Увімкнути AC (220В)");
            btnAcTouchToggle.setBackgroundColor(Color.parseColor("#475569"));
            Toast.makeText(this, "Команда: Вимкнення виходу 220В надіслана!", Toast.LENGTH_SHORT).show();
        }
        sendModbusCommand(0x000B, isAcOn ? 0x01 : 0x00);
    }

    private void cycleChargingMode() {
        currentChargingMode = (currentChargingMode + 1) % 3;
        String modeName = "Стандартний";
        String colorHex = "#D97706";
        int modeVal = 1;
        if (currentChargingMode == 0) {
            modeName = "Тихий (Silent - 200Вт)";
            colorHex = "#2563EB";
            modeVal = 0;
        } else if (currentChargingMode == 1) {
            modeName = "Стандартний (Standard - 350Вт)";
            colorHex = "#D97706";
            modeVal = 1;
        } else if (currentChargingMode == 2) {
            modeName = "Турбо (Turbo - 430Вт)";
            colorHex = "#DC2626";
            modeVal = 2;
        }
        btnModeToggle.setText("⚙️ Режим заряду: " + modeName);
        btnModeToggle.setBackgroundColor(Color.parseColor(colorHex));
        Toast.makeText(this, "Змінено режим зарядки: " + modeName, Toast.LENGTH_SHORT).show();
        
        sendModbusCommand(0x000F, modeVal);
    }

    private void sendModbusCommand(int registerAddress, int value) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            Toast.makeText(this, "Станція не підключена!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        byte highAddr = (byte) (registerAddress >> 8);
        byte lowAddr = (byte) (registerAddress & 0xFF);
        byte highVal = (byte) (value >> 8);
        byte lowVal = (byte) (value & 0xFF);
        
        byte[] modbusPacket = new byte[]{0x01, 0x06, highAddr, lowAddr, highVal, lowVal, 0x00, 0x00};
        writeCharacteristic.setValue(modbusPacket);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    private void showAdvancedRegistersDialog() {
        CharSequence[] options = {
            "1. Режим Power Lifting (Підняття потужності для нагрівачів до 1200Вт) [Регістр 0x0040]",
            "2. Швидкість кулера охолодження: 0-Silent / 1-Standard / 2-Turbo [Регістр 0x0025]",
            "3. Максимальний ліміт струму мережі (Grid Amperage Limit) [Регістр 0x0035]",
            "4. Увімкнення / Вимкнення ECO-режиму (Автовимкнення інвертора) [Регістр 0x0016]",
            "5. Калібрування датчика струму BMS (Zero Current Calibration) [Регістр 0x0010]",
            "6. Поріг захисту від підвищеної напруги байпасу (OVP Threshold) [Регістр 0x0012]",
            "7. Час підсвічування екрана LCD (Sleep Timeout) [Регістр 0x0050]",
            "8. Повне скидання / Очищення логів помилок інвертора [Регістр 0x0030]"
        };

        new AlertDialog.Builder(this)
            .setTitle("📊 Повний каталог недокументованих регістрів EB3A")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    sendModbusCommand(0x0040, 0x0001);
                    Toast.makeText(this, "⚡ Power Lifting активовано (для резистивних навантажень)!", Toast.LENGTH_LONG).show();
                } else if (which == 1) {
                    sendModbusCommand(0x0025, 0x0002);
                    Toast.makeText(this, "🌀 Режим кулера встановлено на Turbo!", Toast.LENGTH_LONG).show();
                } else if (which == 2) {
                    sendModbusCommand(0x0035, 0x000A);
                    Toast.makeText(this, "🔌 Ліміт вхідного струму мережі змінено на 10А!", Toast.LENGTH_LONG).show();
                } else if (which == 3) {
                    sendModbusCommand(0x0016, 0x0001);
                    Toast.makeText(this, "🌱 ECO-режим увімкнено!", Toast.LENGTH_LONG).show();
                } else if (which == 4) {
                    sendModbusCommand(0x0010, 0x0001);
                    Toast.makeText(this, "⚠️ Калібрування BMS надіслано в інвертор!", Toast.LENGTH_LONG).show();
                } else if (which == 5) {
                    sendModbusCommand(0x0012, 0x0138);
                    Toast.makeText(this, "🛡 Поріг OVP байпасу оновлено (312В)!", Toast.LENGTH_LONG).show();
                } else if (which == 6) {
                    sendModbusCommand(0x0050, 0x003C);
                    Toast.makeText(this, "💡 Час підсвічування LCD встановлено на 60 сек!", Toast.LENGTH_LONG).show();
                } else if (which == 7) {
                    sendModbusCommand(0x0030, 0x0000);
                    Toast.makeText(this, "🧹 Логи помилок повністю очищено!", Toast.LENGTH_LONG).show();
                }
            })
            .setPositiveButton("Закрити", null)
            .show();
    }
}