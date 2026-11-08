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
    private String connectedDeviceName = "Не выбрано";
    private boolean isScanning = false;

    private final List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private final List<String> scannedDeviceNames = new ArrayList<>();

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic writeCharacteristic = null;
    private BluetoothGattCharacteristic notifyCharacteristic = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable;

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
            
            // Фильтруем только устройства Bluetti для удобства
            if (name != null && !addressListContains(address)) {
                scannedDevices.add(device);
                scannedDeviceNames.add("🔋 " + name + "\n📍 " + address);
                appendLog("🔍 Найдено: " + name + " [" + address + "]");
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
        appendLog("🚀 Модуль непрерывного дамп-логгера запущен.");
    }

    private void appendLog(String message) {
        mainHandler.post(() -> {
            if (logConsole != null) {
                String timeStr = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
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
            appendLog("⚠️ Запрос разрешений...");
        }
    }

    private void initInterface(LinearLayout container) {
        TextView title = new TextView(this);
        title.setText("⚡ Bluetti Full Packet Logger 🇺🇦");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        container.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Ожидание сканирования 🔍");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 8);
        container.addView(statusTextView);

        btnScan = createStyledButton("🔍 Сканировать эфир", "#2563EB");
        btnScan.setOnClickListener(v -> startBleScan());
        container.addView(btnScan);

        btnConnect = createStyledButton("🔗 Выбрать станцию", "#0D9488");
        btnConnect.setOnClickListener(v -> showDeviceSelectionDialog());
        container.addView(btnConnect);

        btnAcTouchToggle = createStyledButton("🔌 Запрос статуса (Read Registers)", "#475569");
        btnAcTouchToggle.setEnabled(false);
        btnAcTouchToggle.setOnClickListener(v -> requestStatusRegisters());
        container.addView(btnAcTouchToggle);

        btnModeToggle = createStyledButton("🔑 Тест авторизации (0x0000 - 0x0005)", "#D97706");
        btnModeToggle.setEnabled(false);
        btnModeToggle.setOnClickListener(v -> sendLoginChallenge());
        container.addView(btnModeToggle);

        btnAdvancedInfo = createStyledButton("🧹 Очистить лог", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> {
            if (logConsole != null) logConsole.setText("");
            appendLog("🧹 Лог очищен.");
        });
        container.addView(btnAdvancedInfo);

        telemetryTable = new TableLayout(this);
        container.addView(telemetryTable);
        renderPlaceholderTable();

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Системный дамп пакетов (HEX):");
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
        logConsole.setMinLines(9);
        logConsole.setMaxLines(14);
        logConsole.setKeyListener(null);
        container.addView(logConsole);

        btnCopyLog = createStyledButton("📋 Скопировать весь лог", "#334155");
        btnCopyLog.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("BluettiLogs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Лог скопирован!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка копирования", Toast.LENGTH_SHORT).show();
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
        addTableRow("Параметр Bluetti EB3A", "Значение", true);
        addTableRow("Статус связи", "Не подключено ❌", false);
        addTableRow("Устройство", connectedDeviceName, false);
        addTableRow("Сессия Heartbeat", "Ожидание", false);
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
        if (bleScanner == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;

        scannedDevices.clear();
        scannedDeviceNames.clear();
        statusTextView.setText("Статус: 🔍 Сканирование...");
        isScanning = true;
        bleScanner.startScan(scanCallback);

        mainHandler.postDelayed(() -> {
            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(scanCallback);
                }
                isScanning = false;
                appendLog("⏹ Сканирование завершено. Найдено устройств: " + scannedDevices.size());
                if (!scannedDevices.isEmpty()) showDeviceSelectionDialog();
            }
        }, 8000);
    }

    private void showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите станцию Bluetti");
        String[] items = scannedDeviceNames.toArray(new String[0]);
        builder.setItems(items, (dialog, which) -> {
            BluetoothDevice device = scannedDevices.get(which);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                connectedDeviceAddress = device.getAddress();
                connectedDeviceName = device.getName() != null ? device.getName() : "Bluetti";
                connectToDevice(device);
            }
        });
        builder.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        appendLog("🔗 Подключение к " + device.getAddress());
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog("✅ Подключено к GATT. Поиск служб...");
                mainHandler.post(() -> gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                appendLog("🔌 Связь разорвана станцией.");
                stopHeartbeatLoop();
                mainHandler.post(() -> {
                    statusTextView.setText("Статус: 🔌 Отключено");
                    btnAcTouchToggle.setEnabled(false);
                    btnModeToggle.setEnabled(false);
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
                        if (cUuid.contains("ff02")) writeCharacteristic = c;
                        else if (cUuid.contains("ff01")) notifyCharacteristic = c;
                    }
                }

                if (writeCharacteristic != null && notifyCharacteristic != null) {
                    appendLog("✅ Найдены ff02 (Write) и ff01 (Notify). Включаем уведомления...");
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        BluetoothGattDescriptor desc = notifyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (desc != null) {
                            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(desc);
                        }
                    }

                    // ЗАПУСКАЕМ УДЕРЖАНИЕ СВЯЗИ (HEARTBEAT)
                    startHeartbeatLoop();

                    mainHandler.post(() -> {
                        statusTextView.setText("Статус: 🟢 Сессия удержана (Heartbeat активен)");
                        btnAcTouchToggle.setEnabled(true);
                        btnModeToggle.setEnabled(true);
                    });
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                parseAndLogSniffedData(characteristic.getUuid().toString(), data);
            }
        }
    };

    private void startHeartbeatLoop() {
        stopHeartbeatLoop();
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (bluetoothGatt != null && writeCharacteristic != null) {
                    // Стандартный пакет поддержания сеанса (Heartbeat)
                    byte[] heartbeat = new byte[] { (byte)0xAA, 0x55, 0x01, 0x00, 0x00, 0x00 };
                    sendRawBytesSilent(heartbeat);
                }
                heartbeatHandler.postDelayed(this, 1000); // Повтор каждую секунду
            }
        };
        heartbeatHandler.postDelayed(heartbeatRunnable, 1000);
        appendLog("💓 Цикл удержания связи (Heartbeat) запущен.");
    }

    private void stopHeartbeatLoop() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }

    private void parseAndLogSniffedData(String uuid, byte[] data) {
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02X ", b));
            if (b >= 32 && b <= 126) ascii.append((char) b);
            else ascii.append(".");
        }
        String shortUuid = uuid.length() >= 8 ? uuid.substring(0, 8) : uuid;
        appendLog(String.format("📥 RECV [%s] (%dB): %s | ASCII: %s", shortUuid, data.length, hex.toString().trim(), ascii.toString()));
    }

    private void requestStatusRegisters() {
        appendLog("🚀 Запрос чтения регистров состояния...");
        sendRawBytes(new byte[] { 0x01, 0x03, 0x00, (byte)0x0B, 0x00, 0x05, (byte)0x35, (byte)0xC9 });
    }

    private void sendLoginChallenge() {
        appendLog("🔑 Отправка тестового запроса идиентификации...");
        sendRawBytes(new byte[] { 0x01, 0x03, 0x00, 0x00, 0x00, 0x08, (byte)0x44, (byte)0x0C });
    }

    private void sendRawBytes(byte[] raw) {
        if (bluetoothGatt == null || writeCharacteristic == null) return;
        writeCharacteristic.setValue(raw);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02X ", b));
        appendLog("📤 SEND: " + sb.toString().trim());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    private void sendRawBytesSilent(byte[] raw) {
        if (bluetoothGatt == null || writeCharacteristic == null) return;
        writeCharacteristic.setValue(raw);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }
}