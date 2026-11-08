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

    // Зафіксований правильний канал запису
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
        appendLog("🚀 Додаток запущено. Канал ff02 зафіксовано як основний.");
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
        title.setText("⚡ Bluetti EB3A ff02 Protocol Master 🇺🇦");
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

        btnAdvancedInfo = createStyledButton("🛠 СКАНИР / ПОШУК ДОСТУПНИХ РЕГІСТРІВ (ff02)", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> showScanDialog());
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
                writeCharacteristic = null;
                notifyCharacteristic = null;

                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                        String cUuid = c.getUuid().toString();
                        if (cUuid.contains("ff02")) {
                            writeCharacteristic = c;
                            appendLog("🎯 Знайдено та закріплено WRITE канал: ff02");
                        } else if (cUuid.contains("ff01")) {
                            notifyCharacteristic = c;
                            appendLog("🔔 Знайдено NOTIFY канал: ff01");
                        }
                    }
                }

                if (writeCharacteristic != null && notifyCharacteristic != null) {
                    appendLog("✅ УСПІХ: Канал ff02 готовий до роботи!");
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
                        statusTextView.setText("Статус: 🟢 Онлайн через ff02 ⚡");
                        btnAcTouchToggle.setEnabled(true);
                        btnModeToggle.setEnabled(true);
                        updateLiveTelemetry("Підключено (ff02)", "100%", "0 Вт", "150 Вт");
                    });
                } else {
                    appendLog("❌ ПОМИЛКА: Не знайдено ff02 або ff01!");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                parseModbusResponse(data);
            }
        }
    };

    private void parseModbusResponse(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        
        if (data.length >= 3 && (data[1] & 0x80) != 0) {
            int errCode = data[2] & 0xFF;
            String errStr = (errCode == 0x02) ? "Illegal Data Address (Регістр заблокований)" : "Код помилки 0x" + Integer.toHexString(errCode);
            appendLog("📥 Відповідь [ПОМИЛКА]: " + sb.toString().trim() + " -> " + errStr);
        } else {
            appendLog("📥 Відповідь [УСПІХ/ДАНІ]: " + sb.toString().trim());
        }
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
        sendModbusWrite(0x000B, isAcOn ? 0x01 : 0x00);
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
        
        sendModbusWrite(0x000F, modeVal);
    }

    private void sendModbusWrite(int registerAddress, int value) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            appendLog("❌ Помилка: Станція не підключена або ff02 недоступний!");
            Toast.makeText(this, "Станція не підключена!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Функція 0x06 (Write Single Register) через ff02 без обгортки (як вимагає прошивка)
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
        
        StringBuilder hexStr = new StringBuilder();
        for (byte b : fullPacket) hexStr.append(String.format("%02X ", b));
        
        appendLog(String.format("📤 ff02 WRITE [Reg: 0x%04X, Val: 0x%04X] HEX: %s", 
            registerAddress, value, hexStr.toString().trim()));
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
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

    // --- СКАНИР / ПОШУК ДОСТУПНИХ РЕГІСТРІВ ЧЕРЕЗ ff02 ---
    private void showScanDialog() {
        CharSequence[] options = {
            "🔍 1. Сканувати базові регістри керування (0x0000 - 0x0020)",
            "🔍 2. Сканувати сервісні регістри (0x0A00 - 0x0A10)",
            "🔍 3. Сканувати системні блоки (0x1000 - 0x1010)"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Сканер регістрів через ff02")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    runRegisterScan(0x0000, 0x0020);
                } else if (which == 1) {
                    runRegisterScan(0x0A00, 0x0A10);
                } else if (which == 2) {
                    runRegisterScan(0x1000, 0x1010);
                }
            })
            .setNegativeButton("Закрити", null)
            .show();
    }

    private void runRegisterScan(int startReg, int endReg) {
        appendLog(String.format("🚀 Запуск сканування діапазону 0x%04X - 0x%04X через ff02...", startReg, endReg));
        Toast.makeText(this, "Сканування запущено. Дивіться лог.", Toast.LENGTH_SHORT).show();

        int delay = 0;
        for (int reg = startReg; reg <= endReg; reg++) {
            final int currentReg = reg;
            mainHandler.postDelayed(() -> {
                sendModbusWrite(currentReg, 0x0001);
            }, delay * 350L);
            delay++;
        }
    }
}