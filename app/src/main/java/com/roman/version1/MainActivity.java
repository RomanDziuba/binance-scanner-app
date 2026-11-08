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

    private BluetoothGattCharacteristic writeCharacteristic = null;
    private BluetoothGattCharacteristic notifyCharacteristic = null;
    
    // Додаткові знайдені характеристики для матриці перебору
    private BluetoothGattCharacteristic charFf02 = null;
    private BluetoothGattCharacteristic charFf03 = null;
    private BluetoothGattCharacteristic charFf04 = null;

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
        appendLog("🚀 Додаток запущено. Меню масового перебору та матриці тестів активне.");
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
        title.setText("⚡ Bluetti EB3A Master Matrix Brute-Force 🇺🇦");
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

        btnAdvancedInfo = createStyledButton("🛠 МЕНЮ МАСОВОГО ПЕРЕБОРУ ВАРІАНТІВ (MASTER MATRIX)", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> showMasterMatrixDialog());
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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog("✅ Успішно підключено до GATT сервера!");
                mainHandler.post(() -> statusTextView.setText("Статус: ✅ Підключено! Пошук сервісів..."));
                
                mainHandler.postDelayed(() -> {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices();
                    }
                }, 500);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                appendLog("🔌 З'єднання розірвано (Status: " + status + ")");
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
                writeCharacteristic = null;
                notifyCharacteristic = null;
                charFf02 = null;
                charFf03 = null;
                charFf04 = null;

                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                        String cUuid = c.getUuid().toString();
                        if (cUuid.contains("ff04")) {
                            charFf04 = c;
                            writeCharacteristic = c; // Основний за замовчуванням
                        } else if (cUuid.contains("ff03")) {
                            charFf03 = c;
                        } else if (cUuid.contains("ff02")) {
                            charFf02 = c;
                        } else if (cUuid.contains("ff01")) {
                            notifyCharacteristic = c;
                        }
                    }
                }

                if (writeCharacteristic != null && notifyCharacteristic != null) {
                    appendLog("✅ УСПІХ: Знайдено всі канали обміну!");
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            appendLog("🔔 Підписку на сповіщення активовано.");
                        }
                    }

                    mainHandler.post(() -> {
                        statusTextView.setText("Статус: 🟢 Готовий до масового перебору ⚡");
                        btnAcTouchToggle.setEnabled(true);
                        btnModeToggle.setEnabled(true);
                        updateLiveTelemetry("Підключено (Ready)", "100%", "0 Вт", "150 Вт");
                    });
                } else {
                    appendLog("❌ ПОМИЛКА: Не знайдено необхідні канали!");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                parseBluettiResponse(data);
            }
        }
    };

    private void parseBluettiResponse(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        appendLog("📥 Вхідний пакет (" + data.length + " B): " + sb.toString().trim());
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
            appendLog("⚙️ Запит: УВІМКНЕННЯ AC (220В)");
        } else {
            btnAcTouchToggle.setText("🔌 Увімкнути AC (220В)");
            btnAcTouchToggle.setBackgroundColor(Color.parseColor("#475569"));
            appendLog("⚙️ Запит: ВИМКНЕННЯ AC (220В)");
        }
        sendDirectModbus(0x000B, isAcOn ? 0x01 : 0x00);
    }

    private void cycleChargingMode() {
        currentChargingMode = (currentChargingMode + 1) % 3;
        int modeVal = currentChargingMode;
        
        String modeName = "Стандартний";
        if (modeVal == 0) modeName = "Тихий (200Вт)";
        else if (modeVal == 1) modeName = "Стандартний (350Вт)";
        else if (modeVal == 2) modeName = "Турбо (430Вт)";

        btnModeToggle.setText("⚙️ Режим заряду: " + modeName);
        appendLog("⚙️ Зміна режиму зарядки: " + modeName);
        
        sendDirectModbus(0x000F, modeVal);
    }

    // Стандартна відправка через поточний writeCharacteristic
    private void sendDirectModbus(int register, int value) {
        sendCustomPacket(writeCharacteristic, register, value, true, false);
    }

    // Низькорівневий метод відправки з можливістю вибору характеристик та обгортки 2A 2A
    private void sendCustomPacket(BluetoothGattCharacteristic targetChar, int register, int value, boolean use06, boolean wrap2A) {
        if (bluetoothGatt == null || targetChar == null) {
            appendLog("❌ Помилка: Станція або канал не готові!");
            return;
        }

        // 1. Формуємо Modbus payload (Функція 0x06 або 0x10)
        byte[] modbusBytes;
        if (use06) {
            modbusBytes = new byte[] {
                0x01, 0x06,
                (byte) (register >> 8), (byte) (register & 0xFF),
                (byte) (value >> 8), (byte) (value & 0xFF)
            };
        } else {
            modbusBytes = new byte[] {
                0x01, 0x10,
                (byte) (register >> 8), (byte) (register & 0xFF),
                0x00, 0x01, 0x02,
                (byte) (value >> 8), (byte) (value & 0xFF)
            };
        }

        byte[] crc = calculateModbusCRC(modbusBytes);
        byte[] fullModbus = new byte[modbusBytes.length + 2];
        System.arraycopy(modbusBytes, 0, fullModbus, 0, modbusBytes.length);
        fullModbus[fullModbus.length - 2] = crc[0];
        fullModbus[fullModbus.length - 1] = crc[1];

        byte[] finalPayload;
        if (wrap2A) {
            // Обгортка 2A 2A
            finalPayload = new byte[2 + fullModbus.length + 2];
            finalPayload[0] = 0x2A;
            finalPayload[1] = 0x2A;
            System.arraycopy(fullModbus, 0, finalPayload, 2, fullModbus.length);
            int sum = 0;
            for (int i = 2; i < finalPayload.length - 2; i++) sum += (finalPayload[i] & 0xFF);
            finalPayload[finalPayload.length - 2] = (byte) (sum & 0xFF);
            finalPayload[finalPayload.length - 1] = (byte) ((sum >> 8) & 0xFF);
        } else {
            finalPayload = fullModbus;
        }

        targetChar.setValue(finalPayload);
        targetChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        StringBuilder hexStr = new StringBuilder();
        for (byte b : finalPayload) hexStr.append(String.format("%02X ", b));

        String charName = "ff04";
        if (targetChar == charFf02) charName = "ff02";
        else if (targetChar == charFf03) charName = "ff03";

        appendLog(String.format("📤 [%s] Reg:0x%04X Val:0x%04X (Fn:%s, 2A:%s) HEX: %s",
                charName, register, value, use06 ? "06" : "10", wrap2A ? "YES" : "NO", hexStr.toString().trim()));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(targetChar);
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

    // --- ГАВНЕЙШЕЕ МЕНЮ МАСОВОГО ПЕРЕБОРУ (MASTER MATRIX) ---
    private void showMasterMatrixDialog() {
        CharSequence[] options = {
            "🔥 1. АВТОМАТИЧНИЙ МАСОВИЙ ПЕРЕБІР УСІХ ВАРІАНТІВ (Full Master Brute-Force)",
            "📋 2. Тест перебору регістрів режиму зарядки (0x000F, 0x0022, 0x0035)",
            "🔌 3. Тест перебору регістрів AC 220В (0x000B, 0x001B, 0x002B)",
            "🎯 4. Змінити активний канал запису вручну (ff02 / ff03 / ff04)"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Меню масового перебору (Master Matrix)")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    runFullMasterBruteForce();
                } else if (which == 1) {
                    runChargingRegistersBruteForce();
                } else if (which == 2) {
                    runAcRegistersBruteForce();
                } else if (which == 3) {
                    showCharacteristicSelector();
                }
            })
            .setNegativeButton("Закрити", null)
            .show();
    }

    // 1. Полный автоматический перебор ВСЕХ комбинаций (характеристики, функции, обертки, регистры)
    private void runFullMasterBruteForce() {
        appendLog("🔥🔥 ЗАПУСК ПОЛНОГО МААССОВОГО ПЕРЕБОРА (MASTER MATRIX)...");
        Toast.makeText(this, "Перебор запущен! Следите за логами.", Toast.LENGTH_LONG).show();

        BluetoothGattCharacteristic[] chars = { charFf04, charFf03, charFf02 };
        int[] registers = { 0x000B, 0x000F, 0x001B, 0x0022 };
        boolean[] functions = { true, false }; // true = 0x06, false = 0x10
        boolean[] wrappers = { false, true };  // false = чистый modbus, true = 2A 2A

        int delayIndex = 0;
        for (BluetoothGattCharacteristic ch : chars) {
            if (ch == null) continue;
            for (int reg : registers) {
                for (boolean fn06 : functions) {
                    for (boolean wrap : wrappers) {
                        final BluetoothGattCharacteristic target = ch;
                        final int r = reg;
                        final boolean f = fn06;
                        final boolean w = wrap;

                        mainHandler.postDelayed(() -> {
                            appendLog(String.format("🧪 [TEST] Reg=0x%04X | Fn=%s | 2A=%s", r, f ? "06" : "10", w ? "ON" : "OFF"));
                            sendCustomPacket(target, r, 0x0001, f, w);
                        }, delayIndex * 500L); // Пауза 500мс между тестами, чтобы не перегрузить BLE-стек (GATT 19)

                        delayIndex++;
                    }
                }
            }
        }
    }

    private void runChargingRegistersBruteForce() {
        appendLog("⚙️ Перебор регистров зарядки (0x000F, 0x0022, 0x0035, 0x0040)...");
        int[] regs = { 0x000F, 0x0022, 0x0035, 0x0040 };
        int idx = 0;
        for (int r : regs) {
            mainHandler.postDelayed(() -> {
                appendLog(String.format("⚙️ Тест регистра зарядки: 0x%04X", r));
                sendDirectModbus(r, 0x0002); // Турбо режим / значение 2
            }, idx * 400L);
            idx++;
        }
    }

    private void runAcRegistersBruteForce() {
        appendLog("🔌 Перебор регистров AC (0x000B, 0x001B, 0x002B, 0x100B)...");
        int[] regs = { 0x000B, 0x001B, 0x002B, 0x100B };
        int idx = 0;
        for (int r : regs) {
            mainHandler.postDelayed(() -> {
                appendLog(String.format("🔌 Тест регистра AC: 0x%04X", r));
                sendDirectModbus(r, 0x0001); // Включение AC
            }, idx * 400L);
            idx++;
        }
    }

    private void showCharacteristicSelector() {
        List<String> names = new ArrayList<>();
        List<BluetoothGattCharacteristic> list = new ArrayList<>();
        if (charFf04 != null) { names.add("ff04 (Основной)"); list.add(charFf04); }
        if (charFf03 != null) { names.add("ff03"); list.add(charFf03); }
        if (charFf02 != null) { names.add("ff02"); list.add(charFf02); }

        if (names.isEmpty()) {
            Toast.makeText(this, "Нет доступных характеристик", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Выберите WRITE характеристику")
            .setItems(names.toArray(new CharSequence[0]), (d, w) -> {
                writeCharacteristic = list.get(w);
                appendLog("🎯 Вручную выбрана характеристика: " + names.get(w));
                Toast.makeText(this, "Выбрано: " + names.get(w), Toast.LENGTH_SHORT).show();
            })
            .show();
    }
}