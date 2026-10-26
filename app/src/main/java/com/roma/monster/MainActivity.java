package com.roma.monster;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

private BroadcastReceiver classicReceiver;

private void checkPermissionsAndScan() {
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
        (android.os.Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
        requestPermissions(new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN
        }, REQUEST_PERMISSIONS);
        return;
    }
    startExhaustiveSearch();
}

private void startExhaustiveSearch() {
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
        Toast.initSafeToast(this, "Включите Bluetooth!"); // или Toast.makeText
        Toast.makeText(this, "Включите Bluetooth!", Toast.LENGTH_LONG).show();
        return;
    }

    discoveredDevices.clear();
    deviceDisplayNames.clear();

    statusTextView.setText("Статус: ⚡ Глубокий поиск (BLE + Paired + Classic)...");
    logEditText.setText("=== 🔍 ТОТАЛЬНОЕ СКАНИРОВАНИЕ ЭФИРА ===\n");

    // 1. Проверяем уже сопряженные устройства в системе (Самый надежный способ для часов Xiaomi!)
    try {
        java.util.Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            logEditText.append("📂 Найдено сопряженных в системе: " + pairedDevices.size() + "\n");
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName() != null ? device.getName() : "Unknown Paired";
                String entry = "🔗 [Сопряжено] " + name + " [" + device.getAddress() + "]";
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    deviceDisplayNames.add(entry);
                    logEditText.append("  • " + entry + "\n");
                }
            }
        }
    } catch (Exception e) {
        logEditText.append("⚠️ Ошибка чтения сопряженных: " + e.getMessage() + "\n");
    }

    // 2. Запуск агрессивного BLE сканирования
    if (bleScanner != null) {
        isScanning = true;
        
        android.bluetooth.le.ScanSettings settings = new android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY) // Максимальная мощность
            .setCallbackType(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build();

        bleScanner.startScan(null, settings, scanCallback);
        logEditText.append("📡 BLE Сканер запущен в режиме LOW_LATENCY...\n");
    }

    // 3. Запуск классического Bluetooth поиска
    try {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        logEditText.append("🔎 Классический BT discovery запущен...\n");

        if (classicReceiver == null) {
            classicReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            String name = device.getName() != null ? device.getName() : "Classic BT Device";
                            String entry = "📻 [Classic] " + name + " [" + device.getAddress() + "]";
                            if (!discoveredDevices.contains(device)) {
                                discoveredDevices.add(device);
                                deviceDisplayNames.add(entry);
                                handler.post(() -> logEditText.append("  • " + entry + "\n"));
                            }
                        }
                    }
                }
            };
            registerReceiver(classicReceiver, new android.content.IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    } catch (Exception ignored) {}

    // Авто-остановка через 12 секунд и показ результатов
    handler.postDelayed(() -> {
        stopAllScans();
        showDeviceSelectionDialog();
    }, 12000);
}

private void stopAllScans() {
    isScanning = false;
    if (bleScanner != null && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
        try {
            bleScanner.stopScan(scanCallback);
        } catch (Exception ignored) {}
    }
    try {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (classicReceiver != null) {
            unregisterReceiver(classicReceiver);
            classicReceiver = null;
        }
    } catch (Exception ignored) {}

    handler.post(() -> statusTextView.setText("Статус: Поиск завершен. Найдено: " + discoveredDevices.size()));
}

private ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        BluetoothDevice device = result.getDevice();
        String name = device.getName();
        if (name == null || name.isEmpty()) {
            name = "BLE Beacon / Unnamed";
        }
        
        int rssi = result.getRssi();
        String entry = "📡 [BLE] " + name + " [" + device.getAddress() + "] RSSI: " + rssi;
        
        if (!discoveredDevices.contains(device)) {
            discoveredDevices.add(device);
            deviceDisplayNames.add(entry);
            handler.post(() -> logEditText.append("  • " + entry + "\n"));
        }
    }
};