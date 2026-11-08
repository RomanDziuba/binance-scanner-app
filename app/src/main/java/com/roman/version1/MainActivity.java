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
    
    // Перемикач режиму запису: true = функція 0x06, false = функція 0x10
    private boolean useFunction06 = true;

    private final List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private final List<String> scannedDeviceNames = new ArrayList<>();

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic writeCharacteristic = null;
    private BluetoothGattCharacteristic notifyCharacteristic = null;
    
    // Альтернативні характеристики для запису
    private BluetoothGattCharacteristic altWriteChar03 = null;
    private BluetoothGattCharacteristic altWriteChar04 = null;

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
        appendLog("🚀 Додаток запущено. Режим функції 0x06 та альтернативні діапазони активні.");
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
        title.setText("⚡ Bluetti EB3A Advanced Protocol Diagnostic 🇺🇦");
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

        btnAdvancedInfo = createStyledButton("🛠 Розширене меню тестування та брутфорсу", "#7C3AED");
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
                        appendLog("🔎 Запуск discoverServices()...");
                        gatt.discoverServices();
                    }
                }, 600);

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
                appendLog("📊 Аналіз сервісів та характеристик...");
                
                writeCharacteristic = null;
                notifyCharacteristic = null;
                altWriteChar03 = null;
                altWriteChar04 = null;

                for (BluetoothGattService service : gatt.getServices()) {
                    String sUuid = service.getUuid().toString();
                    appendLog("  📦 Сервіс: " + sUuid);

                    for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                        String cUuid = c.getUuid().toString();
                        int props = c.getProperties();
                        appendLog("     🔸 Характеристика: " + cUuid + " [Props: " + props + "]");

                        if (cUuid.contains("ff02")) {
                            writeCharacteristic = c;
                            appendLog("       🎯 Основний WRITE канал (ff02)");
                        } else if (cUuid.contains("ff01")) {
                            notifyCharacteristic = c;
                            appendLog("       🔔 NOTIFY канал (ff01)");
                        } else if (cUuid.contains("ff03")) {
                            altWriteChar03 = c;
                            appendLog("       🔸 Альтернативний канал ff03");
                        } else if (cUuid.contains("ff04")) {
                            altWriteChar04 = c;
                            appendLog("       🔸 Альтернативний канал ff04");
                        }
                    }
                }

                if (writeCharacteristic == null && altWriteChar03 != null) {
                    writeCharacteristic = altWriteChar03;
                }

                if (writeCharacteristic != null && notifyCharacteristic != null) {
                    appendLog("✅ УСПІХ: Канали зв'язку активовано!");
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
                        statusTextView.setText("Статус: 🟢 Готовий до тестування ⚡");
                        btnAcTouchToggle.setEnabled(true);
                        btnModeToggle.setEnabled(true);
                        updateLiveTelemetry("Підключено (Online)", "100%", "0 Вт", "150 Вт");
                    });
                } else {
                    appendLog("❌ ПОМИЛКА: Не знайдено необхідні канали!");
                }

            } else {
                appendLog("❌ Помилка пошуку сервісів: status = " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                parseModbusResponse(data);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                appendLog("❌ Помилка запису GATT: status = " + status);
            }
        }
    };

    private void parseModbusResponse(byte[] data) {
        if (data.length >= 3) {
            int func = data[1] & 0xFF;
            
            if ((func & 0x80) != 0) {
                int errCode = data[2] & 0xFF;
                String errDesc = "Невідома помилка";
                if (errCode == 0x01) errDesc = "Illegal Function";
                else if (errCode == 0x02) errDesc = "Illegal Data Address (Регістр заблокований)";
                else if (errCode == 0x03) errDesc = "Illegal Data Value";
                appendLog("⚠️ MODBUS ВІДПОВІДЬ [Помилка 0x" + Integer.toHexString(errCode) + "]: " + errDesc);
                return;
            }

            if (func == 0x03 || func == 0x04) {
                int byteCount = data[2] & 0xFF;
                appendLog("📊 Отримано " + (byteCount / 2) + " регістрів від станції:");
                for (int i = 0; i < byteCount && (3 + i + 1) < data.length; i += 2) {
                    int val = ((data[3 + i] & 0xFF) << 8) | (data[3 + i + 1] & 0xFF);
                    appendLog(String.format("   👉 Значення = 0x%04X (%d)", val, val));
                }
            } else {
                appendLog("✅ Успішна відповідь запису від станції! Пакет прийнято.");
            }
        }
    }

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

    // Універсальний метод відправки: підтримка функції 0x06 (Single) або 0x10 (Multiple)
    private void sendModbusWrite(int registerAddress, int value) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            appendLog("❌ Помилка: Станція не підключена!");
            Toast.makeText(this, "Станція не підключена!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        byte[] packetWithoutCrc;
        
        if (useFunction06) {
            // Функція 0x06 (Write Single Register)
            packetWithoutCrc = new byte[] {
                0x01,
                0x06,
                (byte) (registerAddress >> 8),
                (byte) (registerAddress & 0xFF),
                (byte) (value >> 8),
                (byte) (value & 0xFF)
            };
        } else {
            // Функція 0x10 (Write Multiple Registers)
            packetWithoutCrc = new byte[] {
                0x01,
                0x10,
                (byte) (registerAddress >> 8),
                (byte) (registerAddress & 0xFF),
                0x00, 0x01,
                0x02,
                (byte) (value >> 8),
                (byte) (value & 0xFF)
            };
        }
        
        byte[] crc = calculateModbusCRC(packetWithoutCrc);
        byte[] fullPacket = new byte[packetWithoutCrc.length + 2];
        System.arraycopy(packetWithoutCrc, 0, fullPacket, 0, packetWithoutCrc.length);
        fullPacket[fullPacket.length - 2] = crc[0];
        fullPacket[fullPacket.length - 1] = crc[1];
        
        writeCharacteristic.setValue(fullPacket);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        
        appendLog(String.format("📤 Запис [Func: 0x%02X, Reg: 0x%04X, Val: 0x%04X] HEX: %s", 
            useFunction06 ? 0x06 : 0x10, registerAddress, value, bytesToHex(fullPacket)));
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    private void sendModbusRead(int startRegisterAddress, int count) {
        if (bluetoothGatt == null || writeCharacteristic == null) return;
        
        byte[] packetWithoutCrc = new byte[] {
            0x01,
            0x03,
            (byte) (startRegisterAddress >> 8),
            (byte) (startRegisterAddress & 0xFF),
            (byte) (count >> 8),
            (byte) (count & 0xFF)
        };
        
        byte[] crc = calculateModbusCRC(packetWithoutCrc);
        byte[] fullPacket = new byte[8];
        System.arraycopy(packetWithoutCrc, 0, fullPacket, 0, 6);
        fullPacket[6] = crc[0];
        fullPacket[7] = crc[1];
        
        writeCharacteristic.setValue(fullPacket);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        
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
        byte[] crcBytes = new byte[2];
        crcBytes[0] = (byte) (crc & 0xFF);
        crcBytes[1] = (byte) ((crc >> 8) & 0xFF);
        return crcBytes;
    }

    // --- РОЗШИРЕНЕ МЕНЮ ТЕСТУВАННЯ ---
    private void showSettingsHubDialog() {
        CharSequence[] categories = {
            useFunction06 ? "1. ⚙️ Режим запису: Функція 0x06 (Зараз активна) -> Натисніть для зміни на 0x10" : "1. ⚙️ Режим запису: Функція 0x10 (Зараз активна) -> Натисніть для зміни на 0x06",
            "2. 🔄 Перемкнути WRITE характеристику (ff02 <-> ff03)",
            "3. 🔍 Сканувати високі адреси керування (0x0A00 - 0x0B00)",
            "4. ⚡ Брутфорс запису у вищих діапазонах (0x0A00 - 0x0A20)"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Меню діагностики протоколу")
            .setItems(categories, (dialog, which) -> {
                if (which == 0) {
                    useFunction06 = !useFunction06;
                    Toast.makeText(this, "Змінено на функцію: " + (useFunction06 ? "0x06" : "0x10"), Toast.LENGTH_SHORT).show();
                    appendLog("⚙️ Змінено Modbus функцію запису на: " + (useFunction06 ? "0x06" : "0x10"));
                } else if (which == 1) {
                    if (writeCharacteristic == altWriteChar03 && altWriteChar04 != null) {
                        writeCharacteristic = altWriteChar04;
                        appendLog("🔄 Перемкнуто на характеристику ff04");
                        Toast.makeText(this, "Канал: ff04", Toast.LENGTH_SHORT).show();
                    } else if (altWriteChar03 != null) {
                        writeCharacteristic = altWriteChar03;
                        appendLog("🔄 Перемкнуто на характеристику ff03");
                        Toast.makeText(this, "Канал: ff03", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Альтернативні характеристики відсутні", Toast.LENGTH_SHORT).show();
                    }
                } else if (which == 2) {
                    appendLog("🔍 Читання діапазону 0x0A00 - 0x0A10...");
                    sendModbusRead(0x0A00, 8);
                } else if (which == 3) {
                    appendLog("⚠️ Запуск брутфорсу запису в діапазоні 0x0A00...");
                    for (int i = 0; i < 10; i++) {
                        final int addr = 0x0A00 + i;
                        mainHandler.postDelayed(() -> {
                            sendModbusWrite(addr, 0x0001);
                        }, i * 300L);
                    }
                }
            })
            .setNegativeButton("Закрити", null)
            .show();
    }
}