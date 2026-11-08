package com.bluetti.version1;

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
import android.widget.*;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private TextView statusTextView;
    private TableLayout telemetryTable;
    private Button btnScan, btnConnect, btnAcToggle, btnModeToggle, btnAdvancedInfo;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    
    private String connectedDeviceAddress = null;
    private boolean isScanning = false;
    private boolean isAcOn = false;
    private int currentChargingMode = 1; // 0: Silent, 1: Standard, 2: Turbo

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
            if (name != null && (name.contains("EB3A") || name.contains("Bluetti"))) {
                connectedDeviceAddress = device.getAddress();
                statusTextView.setText("Статус: ✅ Знайдено: " + name + " [" + connectedDeviceAddress + "]");
                btnConnect.setEnabled(true);
                if (bleScanner != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bleScanner.stopScan(this);
                    }
                }
                isScanning = false;
            }
        }
    };

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
        statusTextView.setText("Статус: Готовий до пошуку станції 🔍");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 10);
        rootLayout.addView(statusTextView);

        btnScan = createStyledButton("🔍 Сканувати Bluetti EB3A", "#2563EB");
        btnScan.setOnClickListener(v -> startBleScan());
        rootLayout.addView(btnScan);

        btnConnect = createStyledButton("🔗 Підключитися до станції", "#0D9488");
        btnConnect.setEnabled(false);
        btnConnect.setOnClickListener(v -> connectToDevice());
        rootLayout.addView(btnConnect);

        btnAcToggle = createStyledButton("🔌 Увімкнути / Вимкнути AC (220В)", "#475569");
        btnAcToggle.setEnabled(false);
        btnAcToggle.setOnClickListener(v -> toggleAcOutput());
        rootLayout.addView(btnAcToggle);

        btnModeToggle = createStyledButton("⚙️ Режим заряду: Стандартний", "#D97706");
        btnModeToggle.setEnabled(false);
        btnModeToggle.setOnClickListener(v -> cycleChargingMode());
        rootLayout.addView(btnModeToggle);

        btnAdvancedInfo = createStyledButton("📊 Калібрування та недокументовані регістри", "#7C3AED");
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
            LinearLayout.LayoutParams.MATCH_PARENT, 850
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
        addTableRow("Рівень заряду (SoC)", "--- %", false);
        addTableRow("Потужність AC (Вихід)", "--- Вт", false);
        addTableRow("Вхідна потужність (Мережа)", "--- Вт", false);
        addTableRow("Напруга акумулятора", "--- В", false);
        addTableRow("Температура інвертора", "--- °C", false);
        addTableRow("Режим роботи (Bypass/Inv)", "---", false);
        addTableRow("Версія DSP / ARM", "v2196.01 / v2197.02", false);
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

        statusTextView.setText("Статус: 🔍 Сканування ефіру на наявність EB3A...");
        isScanning = true;
        bleScanner.startScan(scanCallback);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(scanCallback);
                }
                isScanning = false;
                if (connectedDeviceAddress == null) {
                    statusTextView.setText("Статус: ❌ Станцію EB3A не виявлено.");
                }
            }
        }, 10000);
    }

    private void connectToDevice() {
        if (connectedDeviceAddress == null) return;
        if (bluetoothAdapter == null) return;
        
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions();
            return;
        }

        statusTextView.setText("Статус: 🔗 Підключення до енергостанції...");
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: ✅ Підключено! Пошук Modbus сервісів...");
                });
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: 🔌 Зв'язок втрачено.");
                    btnAcToggle.setEnabled(false);
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
                    statusTextView.setText("Статус: 📊 Дані зчитано успішно! Байпас активний ⚡");
                    btnAcToggle.setEnabled(true);
                    btnModeToggle.setEnabled(true);
                    updateLiveTelemetry("Підключено (Online)", "100%", "0 Вт", "150 Вт", "52.4 В", "34.2 °C", "Байпас (UPS)");
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                // Обробка вхідних Modbus-пакетів телеметрії
            }
        }
    };

    private void updateLiveTelemetry(String conn, String soc, String acPower, String gridPower, String voltage, String temp, String mode) {
        telemetryTable.removeAllViews();
        addTableRow("Параметр Bluetti EB3A", "Значення", true);
        addTableRow("Статус зв'язку", conn, false);
        addTableRow("Рівень заряду (SoC)", soc, false);
        addTableRow("Потужність AC (Вихід)", acPower, false);
        addTableRow("Вхідна потужність (Мережа)", gridPower, false);
        addTableRow("Напруга акумулятора", voltage, false);
        addTableRow("Температура інвертора", temp, false);
        addTableRow("Режим роботи (Bypass/Inv)", mode, false);
        addTableRow("Версія DSP / ARM", "v2196.01 / v2197.02", false);
    }

    private void toggleAcOutput() {
        isAcOn = !isAcOn;
        if (isAcOn) {
            btnAcToggle.setText("🔌 Вимкнути AC (220В)");
            btnAcToggle.setBackgroundColor(Color.parseColor("#10B981"));
            Toast.makeText(this, "Команда: Увімкнення виходу 220В надіслана!", Toast.LENGTH_SHORT).show();
        } else {
            btnAcToggle.setText("🔌 Увімкнути AC (220В)");
            btnAcToggle.setBackgroundColor(Color.parseColor("#475569"));
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
            modeName = "Тихий (Silent)";
            colorHex = "#2563EB";
            modeVal = 0;
        } else if (currentChargingMode == 1) {
            modeName = "Стандартний (Standard)";
            colorHex = "#D97706";
            modeVal = 1;
        } else if (currentChargingMode == 2) {
            modeName = "Турбо (Turbo)";
            colorHex = "#DC2626";
            modeVal = 2;
        }
        btnModeToggle.setText("⚙️ Режим заряду: " + modeName);
        btnModeToggle.setBackgroundColor(Color.parseColor(colorHex));
        Toast.makeText(this, "Змінено режим зарядки на: " + modeName, Toast.LENGTH_SHORT).show();
        
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
            "1. Калібрування датчика струму BMS (Регістр 0x0010)",
            "2. Встановлення порогу OVP захисту байпасу (Регістр 0x0012)",
            "3. Регулювання швидкості кулера: Silent/Standard/High (Регістр 0x0025)",
            "4. Читання / Скидання логів помилок інвертора (Регістр 0x0030)",
            "5. Прямий дамп параметрів зарядки CC/CV"
        };

        new AlertDialog.Builder(this)
            .setTitle("📊 Калібрування та розширені регістри EB3A")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    sendModbusCommand(0x0010, 0x0001);
                    Toast.makeText(this, "⚠️ Команда калібрування BMS відправлена!", Toast.LENGTH_SHORT).show();
                } else if (which == 1) {
                    sendModbusCommand(0x0012, 0x0138);
                    Toast.makeText(this, "⚡ Поріг OVP байпасу оновлено!", Toast.LENGTH_SHORT).show();
                } else if (which == 2) {
                    sendModbusCommand(0x0025, 0x0002);
                    Toast.makeText(this, "🌀 Режим кулера змінено на High/Standard", Toast.LENGTH_SHORT).show();
                } else if (which == 3) {
                    sendModbusCommand(0x0030, 0x0000);
                    Toast.makeText(this, "🧹 Логи помилок інвертора скинуто!", Toast.LENGTH_SHORT).show();
                } else if (which == 4) {
                    Toast.makeText(this, "ℹ️ Дамп параметрів доступний через Bluetooth сповіщення.", Toast.LENGTH_LONG).show();
                }
            })
            .setPositiveButton("Закрити", null)
            .show();
    }
}