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
    private int currentChargingMode = 1;

    private final List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private final List<String> scannedDeviceNames = new ArrayList<>();

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Зберігаємо колекцію всіх знайдених характеристик запису для тестів
    private final List<BluetoothGattCharacteristic> availableWriteChars = new ArrayList<>();
    private BluetoothGattCharacteristic activeWriteCharacteristic = null;
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
        appendLog("🚀 Додаток запущено. Режим матричного авто-брутфорсу готовий.");
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
            appendLog("⚠️ Запит системних дозволів...");
        }
    }

    private void initInterface(LinearLayout container) {
        TextView title = new TextView(this);
        title.setText("⚡ Bluetti EB3A Matrix Brute-Force 🇺🇦");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(15f);
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

        btnScan = createStyledButton("🔍 Сканувати ефір (Показати станції)", "#2563EB");
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

        btnAdvancedInfo = createStyledButton("🛠 МЕНЮ АВТО-БРУТФОРСУ (Усі варіанти)", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> showSettingsHubDialog());
        container.addView(btnAdvancedInfo);

        telemetryTable = new TableLayout(this);
        container.addView(telemetryTable);
        renderPlaceholderTable();

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Консоль відладки:");
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
        logConsole.setMinLines(7);
        logConsole.setMaxLines(12);
        logConsole.setKeyListener(null);
        container.addView(logConsole);

        btnCopyLog = createStyledButton("📋 Скопіювати повний лог", "#334155");
        btnCopyLog.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("BluettiLogs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Повний лог скопійовано!", Toast.LENGTH_SHORT).show();
                appendLog("📋 Лог успішно скопійовано.");
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
            appendLog("❌ BLE сканер недоступний.");
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
        appendLog("🔍 Сканування BLE ефіру...");
        isScanning = true;
        bleScanner.startScan(scanCallback);

        mainHandler.postDelayed(() -> {
            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(scanCallback);
                }
                isScanning = false;
                appendLog("⏹ Сканування завершено. Знайдено: " + scannedDevices.size());
                statusTextView.setText("Статус: ✅ Знайдено станцій: " + scannedDevices.size());
                if (!scannedDevices.isEmpty()) {
                    showDeviceSelectionDialog();
                } else {
                    Toast.makeText(this, "Станцій не знайдено.", Toast.LENGTH_LONG).show();
                }
            }
        }, 10000);
    }

    private void showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) {
            Toast.makeText(this, "Список порожній!", Toast.LENGTH_SHORT).show();
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
            appendLog("🔌 Закриття попереднього з'єднання...");
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        appendLog("🔗 Підключення до " + device.getAddress());
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                appendLog("⚠️ GATT status = " + status);
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog("✅ Успішно підключено до GATT сервера!");
                mainHandler.post(() -> statusTextView.setText("Статус: ✅ Підключено! Пошук сервісів..."));
                
                mainHandler.postDelayed(() -> {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices();
                    }
                }, 400);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                appendLog("🔌 З'єднання розірвано.");
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
                availableWriteChars.clear();
                notifyCharacteristic = null;

                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                        String cUuid = c.getUuid().toString();
                        int props = c.getProperties();
                        
                        if ((props & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                            availableWriteChars.add(c);
                            appendLog("🎯 Знайдено WRITE канал: " + cUuid);
                        }
                        if (cUuid.contains("ff01")) {
                            notifyCharacteristic = c;
                            appendLog("🔔 Знайдено NOTIFY канал: " + cUuid);
                        }
                    }
                }

                if (!availableWriteChars.isEmpty()) {
                    activeWriteCharacteristic = availableWriteChars.get(0); // За замовчуванням перший
                }

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
                    statusTextView.setText("Статус: 🟢 Готовий до тестування ⚡");
                    btnAcTouchToggle.setEnabled(true);
                    btnModeToggle.setEnabled(true);
                    updateLiveTelemetry("Підключено", "100%", "0 Вт", "150 Вт");
                });
            }
        }

        @Override
        v.override // just standard callback formatting
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (byte b : data) {
                    sb.append(String.format("%02X ", b));
                }
                appendLog("📥 ВХІДНИЙ ПАКЕТ (" + data.length + " B): " + sb.toString().trim());
            }
        }
    };

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
        appendLog(isAcOn ? "⚙️ Запит: УВІМКНЕННЯ AC (220В)" : "⚙️ Запит: ВИМКНЕННЯ AC (220В)");
        sendUniversalCommand(0x000B, isAcOn ? 0x01 : 0x00);
    }

    private void cycleChargingMode() {
        currentChargingMode = (currentChargingMode + 1) % 3;
        int modeVal = currentChargingMode;
        appendLog("⚙️ Зміна режиму зарядки на індекс: " + modeVal);
        sendUniversalCommand(0x000F, modeVal);
    }

    // Універсальна відправка з урахуванням активного каналу
    private void sendUniversalCommand(int registerAddress, int value) {
        if (bluetoothGatt == null || activeWriteCharacteristic == null) {
            appendLog("❌ Помилка: Немає підключення або WRITE каналу!");
            return;
        }

        // Формуємо чистий Modbus (Функція 0x06) + CRC
        byte[] modbusPayload = new byte[] {
            0x01, 0x06,
            (byte) (registerAddress >> 8),
            (byte) (registerAddress & 0xFF),
            (byte) (value >> 8),
            (byte) (value & 0xFF)
        };
        byte[] crc = calculateModbusCRC(modbusPayload);
        byte[] packet = new byte[8];
        System.arraycopy(modbusPayload, 0, packet, 0, 6);
        packet[6] = crc[0];
        packet[7] = crc[1];

        activeWriteCharacteristic.setValue(packet);
        activeWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        StringBuilder sb = new StringBuilder();
        for (byte b : packet) sb.append(String.format("%02X ", b));
        
        appendLog(String.format("📤 Відправка [Reg: 0x%04X, Val: 0x%04X] HEX: %s", registerAddress, value, sb.toString().trim()));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(activeWriteCharacteristic);
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
        return new byte[] { (byte) (crc & 0xFF), (byte) ((crc >> 8) & 0xFF) };
    }

    // --- МЕНЮ АВТО-БРУТФОРСУ (УСІ ВАРІАНТИ ОДНИМ НАТИСКАНИМ) ---
    private void showSettingsHubDialog() {
        CharSequence[] options = {
            "🚀 1. АВТО-МАТРИЦЯ: Перебрати усі WRITE канали та формати пакетів під час зв'язку",
            "🔄 2. Змінити поточний WRITE канал (Перемикати по черзі)",
            "📋 3. Надіслати тестовий пакет читання моделі (Регістр 0x000C)"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Меню повного брутфорсу")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    startFullMatrixBruteForce();
                } else if (which == 1) {
                    cycleWriteCharacteristic();
                } else if (which == 2) {
                    sendTestReadCommand();
                }
            })
            .setNegativeButton("Закрити", null)
            .show();
    }

    private void cycleWriteCharacteristic() {
        if (availableWriteChars.isEmpty()) {
            Toast.norm(this, "Немає доступних каналів", Toast.LENGTH_SHORT);
            return;
        }
        int currentIndex = availableWriteChars.indexOf(activeWriteCharacteristic);
        int nextIndex = (currentIndex + 1) % availableWriteChars.size();
        activeWriteCharacteristic = availableWriteChars.get(nextIndex);
        appendLog("🔄 Ручне перемикання WRITE каналу на: " + activeWriteCharacteristic.getUuid().toString());
        Toast.makeText(this, "Канал змінено!", Toast.LENGTH_SHORT).show();
    }

    private void sendTestReadCommand() {
        if (bluetoothGatt == null || activeWriteCharacteristic == null) return;
        // Читання регістра 0x000C (модель) за допомогою функції 0x03
        byte[] payload = new byte[] { 0x01, 0x03, 0x00, 0x0C, 0x00, 0x02 };
        byte[] crc = calculateModbusCRC(payload);
        byte[] packet = new byte[8];
        System.arraycopy(payload, 0, packet, 0, 6);
        packet[6] = crc[0]; packet[7] = crc[1];

        activeWriteCharacteristic.setValue(packet);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(activeWriteCharacteristic);
            appendLog("📤 Тест читання (0x03) надіслано.");
        }
    }

    // ГОЛОВНИЙ АВТОМАТИЧНИЙ БРУТФОРС УКЛАДЕНИХ ВАРІАНТІВ
    private void startFullMatrixBruteForce() {
        appendLog("🚀 ЗАПУСК АВТО-МАТРИЦІ БРУТФОРСУ (Перебір каналів і форматів)...");
        Toast.makeText(this, "Авто-брутфорс пішов, дивіться логи!", Toast.LENGTH_LONG).show();

        if (availableWriteChars.isEmpty()) {
            appendLog("❌ Помилка: Немає доступних характеристик запису.");
            return;
        }

        // Послідовність тестів з інтервалами, щоб не крашити Bluetooth-стек станції
        int delayStep = 1000; // 1 секунда між тестами
        int counter = 0;

        for (int c = 0; c < availableWriteChars.size(); c++) {
            final BluetoothGattCharacteristic charToTest = availableWriteChars.get(c);
            
            // Тест 1: Чистий Modbus 0x06 (Запит увімкнення AC)
            mainHandler.postDelayed(() -> {
                activeWriteCharacteristic = charToTest;
                appendLog(String.format("🧪 [Тест %d] Канал: %s | Функція: 0x06 (AC On)", counter, charToTest.getUuid().toString()));
                sendUniversalCommand(0x000B, 0x0001);
            }, counter * delayStep);
            counter++;

            // Тест 2: Пакет із заголовком 2A 2A
            mainHandler.postDelayed(() -> {
                activeWriteCharacteristic = charToTest;
                appendLog(String.format("🧪 [Тест %d] Канал: %s | Формат: 2A 2A обгортка", counter, charToTest.getUuid().toString()));
                sendBluettiWrappedCommand(0x000B, 0x0001);
            }, counter * delayStep);
            counter++;
        }
    }

    private void sendBluettiWrappedCommand(int reg, int val) {
        if (bluetoothGatt == null || activeWriteCharacteristic == null) return;
        byte[] modbus = new byte[] { 0x01, 0x06, (byte)(reg >> 8), (byte)(reg & 0xFF), (byte)(val >> 8), (byte)(val & 0xFF) };
        byte[] crc = calculateModbusCRC(modbus);
        
        byte[] wrapped = new byte[2 + 8 + 2];
        wrapped[0] = 0x2A; wrapped[1] = 0x2A;
        System.arraycopy(modbus, 0, wrapped, 2, 6);
        wrapped[8] = crc[0]; wrapped[9] = crc[1];
        // контрольний суматор хвоста
        wrapped[10] = 0x00; wrapped[11] = 0x01;

        activeWriteCharacteristic.setValue(wrapped);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(activeWriteCharacteristic);
        }
    }
}