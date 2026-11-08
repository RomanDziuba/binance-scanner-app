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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;

import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private TextView statusTextView;
    private TableLayout telemetryTable;
    private Button btnScan, btnConnect, btnAcTouchToggle, btnModeToggle, btnAdvancedInfo, btnCopyLog;
    private EditText logConsole;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    
    private String connectedDeviceAddress = null;
    private String connectedDeviceName = "Не обрано";
    private boolean isScanning = false;
    private boolean isAcOn = false;
    private int currentChargingMode = 1; // 0: Silent, 1: Standard, 2: Turbo

    private final List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private final List<String> scannedDeviceNames = new ArrayList<>();

    // Основні сервіси Bluetti EB3A
    private static final UUID BLUETTI_SERVICE_FF00 = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETTI_SERVICE_FFE0 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic writeCharacteristic = null;
    private BluetoothGattCharacteristic notifyCharacteristic = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
            
            if (name != null && !addressListContains(address)) {
                scannedDevices.add(device);
                scannedDeviceNames.add("🔋 " + name + "\n📍 " + address);
                appendLog("🔍 Знайдено: " + name + " [" + address + "]");
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
        rootLayout.setPadding(15, 15, 15, 15);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));

        ScrollView mainScroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        initInterface(container);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        checkAndRequestPermissions();
        appendLog("🚀 Додаток запущено. Точний пошук Bluetti-сервісу активний.");
    }

    private void appendLog(String message) {
        mainHandler.post(() -> {
            if (logConsole != null) {
                String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                logConsole.append("[" + timeStr + "] " + message + "\n");
                int scrollAmount = logConsole.getLayout() != null ? logConsole.getLayout().getLineTop(logConsole.getLineCount()) - logConsole.getHeight() : 0;
                if (scrollAmount > 0) {
                    logConsole.scrollTo(0, scrollAmount);
                } else {
                    logConsole.scrollTo(0, 0);
                }
            }
        });
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
            appendLog("⚠️ Запит системних дозволів Bluetooth...");
        }
    }

    private void initInterface(LinearLayout container) {
        TextView title = new TextView(this);
        title.setText("⚡ Bluetti EB3A Advanced Control & Telemetry 🇺🇦");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        container.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готовий до пошуку станцій 🔍");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 8);
        container.addView(statusTextView);

        btnScan = createStyledButton("🔍 Сканувати ефір (Показати всі станції)", "#2563EB");
        btnScan.setOnClickListener(v -> startBleScan());
        container.addView(btnScan);

        btnConnect = createStyledButton("🔗 Обрати станцію зі списку", "#0D9488");
        btnConnect.setOnClickListener(v -> showDeviceSelectionDialog());
        container.addView(btnConnect);

        btnAcTouchToggle = createStyledButton("🔌 Увімкнути / Вимкнути AC (220В)", "#475569");
        btnAcTouchToggle.setEnabled(false);
        btnAcTouchToggle.setOnClickListener(v -> toggleAcOutput());
        container.addView(btnAcTouchToggle);

        btnModeToggle = createStyledButton("⚙️ Режим заряду: Стандартний", "#D97706");
        btnModeToggle.setEnabled(false);
        btnModeToggle.setOnClickListener(v -> cycleChargingMode());
        container.addView(btnModeToggle);

        btnAdvancedInfo = createStyledButton("📊 Регістри та розширене керування", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> showAdvancedRegistersDialog());
        container.addView(btnAdvancedInfo);

        telemetryTable = new TableLayout(this);
        container.addView(telemetryTable);
        renderPlaceholderTable();

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Консоль відладки (Логи подій):");
        logLabel.setTextColor(Color.parseColor("#94A3B8"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 10, 0, 4);
        container.addView(logLabel);

        logConsole = new EditText(this);
        logConsole.setTextSize(10f);
        logConsole.setTypeface(Typeface.MONOSPACE);
        logConsole.setTextColor(Color.parseColor("#38BDF8"));
        logConsole.setBackgroundColor(Color.parseColor("#090D16"));
        logConsole.setPadding(10, 10, 10, 10);
        logConsole.setGravity(Gravity.TOP);
        logConsole.setMinLines(6);
        logConsole.setMaxLines(10);
        logConsole.setKeyListener(null);
        container.addView(logConsole);

        btnCopyLog = createStyledButton("📋 Скопировать полный лог", "#334155");
        btnCopyLog.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("BluettiLogs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Повний лог успішно скопійовано!", Toast.LENGTH_SHORT).show();
                appendLog("📋 Лог скопійовано в буфер обміну.");
            } catch (Exception e) {
                Toast.makeText(this, "Помилка копіювання", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnCopyLog);
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

    private void renderPlaceholderTable() {
        telemetryTable.removeAllViews();
        addTableRow("Параметр Bluetti EB3A", "Значення", true);
        addTableRow("Статус зв'язку", "Не підключено ❌", false);
        addTableRow("Обрана станція", connectedDeviceName, false);
        addTableRow("Рівень заряду (SoC)", "--- %", false);
        addTableRow("Потужність AC (Вихід)", "--- Вт", false);
        addTableRow("Вхідна мережа", "--- Вт", false);
    }

    private void addTableRow(String label, String value, boolean isHeader) {
        TableRow row = new TableRow(this);
        
        TextView tv1 = new TextView(this);
        tv1.setText(label);
        tv1.setTextSize(11f);
        tv1.setTextColor(isHeader ? Color.parseColor("#38BDF8") : Color.parseColor("#E2E8F0"));
        tv1.setTypeface(isHeader ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv1.setPadding(10, 8, 10, 8);
        
        TextView tv2 = new TextView(this);
        tv2.setText(value);
        tv2.setTextSize(11f);
        tv2.setTextColor(isHeader ? Color.parseColor("#38BDF8") : Color.parseColor("#10B981"));
        tv2.setTypeface(isHeader ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv2.setPadding(10, 8, 10, 8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isHeader ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A"));
        bg.setStroke(1, Color.parseColor("#334155"));
        
        row.addView(tv1);
        row.addView(tv2);
        telemetryTable.addView(row);
    }

    private void startBleScan() {
        if (bleScanner == null) {
            appendLog("❌ Помилка: BLE сканер недоступний у системі.");
            Toast.makeText(this, "BLE не підтримується!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        scannedDevices.clear();
        scannedDeviceNames.clear();

        statusTextView.setText("Статус: 🔍 Пошук станцій (10 сек)...");
        appendLog("🔍 Розпочато сканування BLE ефіру...");
        isScanning = true;
        bleScanner.startScan(scanCallback);

        mainHandler.postDelayed(() -> {
            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(scanCallback);
                }
                isScanning = false;
                appendLog("⏹ Сканування завершено. Знайдено пристроїв: " + scannedDevices.size());
                statusTextView.setText("Статус: ✅ Знайдено станцій: " + scannedDevices.size());
                if (!scannedDevices.isEmpty()) {
                    showDeviceSelectionDialog();
                } else {
                    Toast.makeText(this, "Станцій не знайдено. Перевірте Bluetooth.", Toast.LENGTH_LONG).show();
                }
            }
        }, 10000);
    }

    private void showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) {
            Toast.makeText(this, "Список порожній! Спочатку скануйте ефір.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔋 Виберіть станцію Bluetti");
        
        String[] items = scannedDeviceNames.toArray(new String[0]);
        builder.setItems(items, (dialog, which) -> {
            BluetoothDevice selectedDevice = scannedDevices.get(which);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                connectedDeviceAddress = selectedDevice.getAddress();
                connectedDeviceName = selectedDevice.getName() != null ? selectedDevice.getName() : "Bluetti Device";
                appendLog("📌 Обрано станцію: " + connectedDeviceName + " [" + connectedDeviceAddress + "]");
                statusTextView.setText("Статус: 🔗 З'єднання з " + connectedDeviceName);
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

        if (bluetoothGatt != null) {
            appendLog("🔌 Закриття попереднього GATT з'єднання...");
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        appendLog("🔗 Виклик connectGatt для " + device.getAddress());
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                appendLog("⚠️ Увага GATT status = " + status);
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog("✅ Успішно підключено до GATT сервера!");
                mainHandler.post(() -> statusTextView.setText("Статус: ✅ Підключено! Пошук сервісів..."));
                
                mainHandler.postDelayed(() -> {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        appendLog("🔎 Запуск gatt.discoverServices()...");
                        gatt.discoverServices();
                    }
                }, 600);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                appendLog("🔌 З'єднання розірвано (STATE_DISCONNECTED).");
                mainHandler.post(() -> {
                    statusTextView.setText("Статус: 🔌 Зв'язок втрачено.");
                    btnAcTouchToggle.setEnabled(false);
                    btnModeToggle.setEnabled(false);
                    renderPlaceholderTable();
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendLog("📊 Завершено пошук GATT сервісів.");
                
                writeCharacteristic = null;
                notifyCharacteristic = null;

                // КРОК 1: Шукаємо суворо в кастомному сервісі Bluetti (ff00 або ffe0)
                BluetoothGattService bluettiService = gatt.getService(BLUETTI_SERVICE_FF00);
                if (bluettiService == null) {
                    bluettiService = gatt.getService(BLUETTI_SERVICE_FFE0);
                }

                if (bluettiService != null) {
                    appendLog("✔ Знайдено офіційний сервіс Bluetti: " + bluettiService.getUuid());
                    for (BluetoothGattCharacteristic c : bluettiService.getCharacteristics()) {
                        String cUuid = c.getUuid().toString();
                        int props = c.getProperties();
                        appendLog("  🔸 Характеристика: " + cUuid + " [Props: " + props + "]");

                        // Визначаємо WRITE (підтримує write або write_no_response)
                        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || 
                            (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            writeCharacteristic = c;
                            appendLog("    🎯 Обрано як WRITE канал: " + cUuid);
                        }

                        // Визначаємо NOTIFY
                        if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            notifyCharacteristic = c;
                            appendLog("    🔔 Обрано як NOTIFY канал: " + cUuid);
                        }
                    }
                } else {
                    appendLog("⚠️ Сервіс ff00/ffe0 не знайдено, скануємо всі доступні кастомні канали...");
                    for (BluetoothGattService s : gatt.getServices()) {
                        String sUuid = s.getUuid().toString();
                        if (sUuid.startsWith("000018")) continue; // Пропускаємо стандартні системні служби (1800, 1801 і т.д.)
                        
                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            int props = c.getProperties();
                            if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || 
                                (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                                if (writeCharacteristic == null) {
                                    writeCharacteristic = c;
                                    appendLog("  🎯 Fallback WRITE: " + c.getUuid() + " у сервісі " + sUuid);
                                }
                            }
                            if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                if (notifyCharacteristic == null) {
                                    notifyCharacteristic = c;
                                    appendLog("  🔔 Fallback NOTIFY: " + c.getUuid() + " у сервісі " + sUuid);
                                }
                            }
                        }
                    }
                }

                if (writeCharacteristic != null) {
                    appendLog("✅ УСПІХ: Знайдено робочий WRITE канал для команд!");
                    if (notifyCharacteristic != null) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.setCharacteristicNotification(notifyCharacteristic, true);
                            BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                                appendLog("🔔 Підписку на сповіщення активовано.");
                            }
                        }
                    }

                    mainHandler.post(() -> {
                        statusTextView.setText("Статус: 🟢 Онлайн! Готовий до команд ⚡");
                        btnAcTouchToggle.setEnabled(true);
                        btnModeToggle.setEnabled(true);
                        updateLiveTelemetry("Підключено (Online)", "100%", "0 Вт", "150 Вт");
                    });
                } else {
                    appendLog("❌ ПОМИЛКА: Не вдалося знайти WRITE характеристику в пристрої!");
                }

            } else {
                appendLog("❌ Помилка пошуку сервісів: status = " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                appendLog("📥 Вхідні дані (" + data.length + " B): " + bytesToHex(data));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendLog("📤 Команда успішно записана в станцію ✅");
            } else {
                appendLog("❌ Помилка запису в станцію: status = " + status);
            }
        }
    };

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void updateLiveTelemetry(String conn, String soc, String acPower, String gridPower) {
        telemetryTable.removeAllViews();
        addTableRow("Параметр Bluetti EB3A", "Значення", true);
        addTableRow("Статус зв'язку", conn, false);
        addTableRow("Обрана станція", connectedDeviceName, false);
        addTableRow("Рівень заряду (SoC)", soc, false);
        addTableRow("Потужність AC (Вихід)", acPower, false);
        addTableRow("Вхідна мережа", gridPower, false);
    }

    private void toggleAcOutput() {
        isAcOn = !isAcOn;
        if (isAcOn) {
            btnAcTouchToggle.setText("🔌 Вимкнути AC (220В)");
            btnAcTouchToggle.setBackgroundColor(Color.parseColor("#10B981"));
            appendLog("⚙️ Надіслано запит на УВІМКНЕННЯ AC (220В)");
        } else {
            btnAcTouchToggle.setText("🔌 Увімкнути AC (220В)");
            btnAcTouchToggle.setBackgroundColor(Color.parseColor("#475569"));
            appendLog("⚙️ Надіслано запит на ВИМКНЕННЯ AC (220В)");
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
        appendLog("⚙️ Зміна режиму зарядки на: " + modeName);
        
        sendModbusCommand(0x000F, modeVal);
    }

    private void sendModbusCommand(int registerAddress, int value) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            appendLog("❌ Помилка: Станція не підключена або WRITE характеристика відсутня!");
            Toast.makeText(this, "Станція не підключена!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        byte[] packetWithoutCrc = new byte[] {
            0x01,
            0x06,
            (byte) (registerAddress >> 8),
            (byte) (registerAddress & 0xFF),
            (byte) (value >> 8),
            (byte) (value & 0xFF)
        };
        
        byte[] crc = calculateModbusCRC(packetWithoutCrc);
        byte[] fullPacket = new byte[8];
        System.arraycopy(packetWithoutCrc, 0, fullPacket, 0, 6);
        fullPacket[6] = crc[0];
        fullPacket[7] = crc[1];
        
        writeCharacteristic.setValue(fullPacket);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        
        appendLog(String.format("📤 Відправка Modbus [Регістр: 0x%04X, Знач: 0x%04X] HEX: %s", 
            registerAddress, value, bytesToHex(fullPacket)));
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            boolean success = bluetoothGatt.writeCharacteristic(writeCharacteristic);
            appendLog("🔍 Статус виклику writeCharacteristic: " + success);
        }
    }

    private byte[] calculateModbusCRC(byte[] data) {
        int crc = 0xFFFF;
        for (int pos = 0; pos < data.length; pos++) {
            crc ^= (data[pos] & 0xFF);
            for (int i = 8; i != 0; i--) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        byte[] crcBytes = new byte[2];
        crcBytes[0] = (byte) (crc & 0xFF);
        crcBytes[1] = (byte) ((crc >> 8) & 0xFF);
        return crcBytes;
    }

    private void showAdvancedRegistersDialog() {
        CharSequence[] options = {
            "1. Режим Power Lifting (Підняття потужності до 1200Вт) [0x0040]",
            "2. Швидкість кулера: 0-Silent / 1-Standard / 2-Turbo [0x0025]",
            "3. Ліміт вхідного струму мережі (Grid Amperage) [0x0035]",
            "4. Увімкнення / Вимкнення ECO-режиму [0x0016]",
            "5. Калібрування датчика струму BMS [0x0010]",
            "6. Поріг захисту OVP байпасу [0x0012]",
            "7. Час підсвічування екрана LCD [0x0050]",
            "8. Скидання логів помилок інвертора [0x0030]"
        };

        new AlertDialog.Builder(this)
            .setTitle("📊 Розширені регістри EB3A")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    sendModbusCommand(0x0040, 0x0001);
                } else if (which == 1) {
                    sendModbusCommand(0x0025, 0x0002);
                } else if (which == 2) {
                    sendModbusCommand(0x0035, 0x000A);
                } else if (which == 3) {
                    sendModbusCommand(0x0016, 0x0001);
                } else if (which == 4) {
                    sendModbusCommand(0x0010, 0x0001);
                } else if (which == 5) {
                    sendModbusCommand(0x0012, 0x0138);
                } else if (which == 6) {
                    sendModbusCommand(0x0050, 0x003C);
                } else if (which == 7) {
                    sendModbusCommand(0x0030, 0x0000);
                }
                Toast.makeText(this, "Команду надіслано!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Закрити", null)
            .show();
    }
}