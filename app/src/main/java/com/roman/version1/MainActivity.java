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
        appendLog("🚀 Модуль реверсу авторизації запущено. Очікування підключення...");
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
        title.setText("⚡ Bluetti Auth & Handshake Sniffer 🇺🇦");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        container.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Очікування сканування 🔍");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 8);
        container.addView(statusTextView);

        btnScan = createStyledButton("🔍 Сканувати ефір", "#2563EB");
        btnScan.setOnClickListener(v -> startBleScan());
        container.addView(btnScan);

        btnConnect = createStyledButton("🔗 Обрати станцію", "#0D9488");
        btnConnect.setOnClickListener(v -> showDeviceSelectionDialog());
        container.addView(btnConnect);

        btnAcTouchToggle = createStyledButton("🔌 Тест AC із запитом доступу", "#475569");
        btnAcTouchToggle.setEnabled(false);
        btnAcTouchToggle.setOnClickListener(v -> sendHandshakeAndToggleAc());
        container.addView(btnAcTouchToggle);

        btnModeToggle = createStyledButton("⚙️ Надіслати сервісний ключ логіна", "#D97706");
        btnModeToggle.setEnabled(false);
        btnModeToggle.setOnClickListener(v -> sendExplicitLoginPacket());
        container.addView(btnModeToggle);

        btnAdvancedInfo = createStyledButton("🛠 Меню діагностики", "#7C3AED");
        btnAdvancedInfo.setOnClickListener(v -> showAuthHubDialog());
        container.addView(btnAdvancedInfo);

        telemetryTable = new TableLayout(this);
        container.addView(telemetryTable);
        renderPlaceholderTable();

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Системний лог обміну даними:");
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

        btnCopyLog = createStyledButton("📋 Скопіювати лог", "#334155");
        btnCopyLog.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("BluettiLogs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Лог скопійовано!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Помилка", Toast.LENGTH_SHORT).show();
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
        addTableRow("Режим аутентифікації", "Очікування дампа", false);
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
        statusTextView.setText("Статус: 🔍 Сканування...");
        isScanning = true;
        bleScanner.startScan(scanCallback);

        mainHandler.postDelayed(() -> {
            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(scanCallback);
                }
                isScanning = false;
                appendLog("⏹ Сканування завершено. Знайдено: " + scannedDevices.size());
                if (!scannedDevices.isEmpty()) showDeviceSelectionDialog();
            }
        }, 10000);
    }

    private void showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Виберіть станцію");
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
        appendLog("🔗 Підключення до " + device.getAddress());
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog("✅ Підключено до GATT. Запуск служб...");
                mainHandler.post(() -> gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                appendLog("🔌 З'єднання розірвано.");
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
                    appendLog("✅ Знайдено ff02 (Write) та ff01 (Notify). Активація сповіщень...");
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        BluetoothGattDescriptor desc = notifyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (desc != null) {
                            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(desc);
                        }
                    }

                    mainHandler.post(() -> {
                        statusTextView.setText("Статус: 🟢 Онлайн, збираємо дамп хендшейку");
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
                parseAndLogSniffedData(data);
            }
        }
    };

    private void parseAndLogSniffedData(byte[] data) {
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02X ", b));
            if (b >= 32 && b <= 126) ascii.append((char) b);
            else ascii.append(".");
        }
        appendLog(String.format("📥 SNIFF [%d B] HEX: %s | ASCII: %s", data.length, hex.toString().trim(), ascii.toString()));
    }

    private void sendHandshakeAndToggleAc() {
        appendLog("🚀 Спроба ініціалізації сесії перед увімкненням AC...");
        sendRawBytes(new byte[] { 0x01, 0x03, 0x00, 0x00, 0x00, 0x05, (byte)0x85, (byte)0xC9 });
        
        mainHandler.postDelayed(() -> {
            sendModbusWrite(0x000B, 0x0001);
        }, 500);
    }

    private void sendExplicitLoginPacket() {
        appendLog("🔑 Відправка тестового масиву авторизації...");
        sendModbusWrite(0x00F0, 0x55AA);
    }

    private void sendModbusWrite(int reg, int val) {
        if (bluetoothGatt == null || writeCharacteristic == null) return;
        byte[] payload = new byte[] { 0x01, 0x06, (byte)(reg >> 8), (byte)(reg & 0xFF), (byte)(val >> 8), (byte)(val & 0xFF) };
        byte[] crc = calculateCRC(payload);
        byte[] packet = new byte[8];
        System.arraycopy(payload, 0, packet, 0, 6);
        packet[6] = crc[0];
        packet[7] = crc[1];

        writeCharacteristic.setValue(packet);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : packet) sb.append(String.format("%02X ", b));
        appendLog("📤 WRITE ff02: " + sb.toString().trim());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    private void sendRawBytes(byte[] raw) {
        if (bluetoothGatt == null || writeCharacteristic == null) return;
        writeCharacteristic.setValue(raw);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
        appendLog("📤 RAW SEND: " + raw.length + " bytes");
    }

    private byte[] calculateCRC(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 8; i != 0; i--) {
                if ((crc & 0x0001) != 0) { crc >>= 1; crc ^= 0xA001; }
                else { crc >>= 1; }
            }
        }
        return new byte[] { (byte)(crc & 0xFF), (byte)((crc >> 8) & 0xFF) };
    }

    private void showAuthHubDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🛠 Аналізатор хендшейку")
            .setItems(new CharSequence[]{"ℹ️ Перевіряйте консоль при підключенні на наявність вхідних байтів"}, (d, w) -> {})
            .setNegativeButton("Закрити", null)
            .show();
    }
}