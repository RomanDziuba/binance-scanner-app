package com.roma.monster;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private WifiManager wifiManager;
    private EditText logEditText;
    private TextView statusTextView;
    private BroadcastReceiver wifiScanReceiver;
    private Button btnScan, btnCopy;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(16, 16, 16, 16);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("⚡ DEEP ROUTER INSPECTOR PRO v2.0 ⚡");
        authorView.setTextColor(Color.parseColor("#38BDF8"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.DEFAULT_BOLD);
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 12);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(20f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        cardBg.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        btnScan = createStyledButton("📡 Запустить глубокий аудит роутеров", "#1D4ED8");
        btnScan.setOnClickListener(v -> startDeepWifiScan());
        card.addView(btnScan);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к глубокому анализу...");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(11f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 10);
        card.addView(statusTextView);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится продвинутая информация о роутерах и модемах...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(14);
        logEditText.setMaxLines(22);
        card.addView(logEditText);

        btnCopy = createStyledButton("📋 Скопировать весь отчет в буфер", "#334155");
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("RouterInspectorLogs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Отчет успешно скопирован!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopy);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                parseAndDisplayAdvancedResults();
            }
        };
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(11f);
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

    private void startDeepWifiScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        if (wifiManager == null) {
            Toast.makeText(this, "Wi-Fi модуль не найден!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        statusTextView.setText("Статус: 🔄 Сканирование эфира и пробиз точек...");
        logEditText.setText("Инициализация глубокого аудита оборудования...\nЗапрос таблиц BSSID и Capabilities...");

        boolean success = wifiManager.startScan();
        if (!success) {
            parseAndDisplayAdvancedResults();
        }
    }

    private void parseAndDisplayAdvancedResults() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            statusTextView.setText("Статус: ⚠️ Точки не найдены");
            logEditText.setText("⚠️ Список пуст. Убедитесь, что геопозиция включена.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 🛡️ ADVANCED ROUTER & MODEM AUDIT REPORT ===\n");
        sb.append("• Всего обнаружено точек: ").append(results.size()).append("\n");
        sb.append("• Timestamp: ").append(System.currentTimeMillis()).append("\n\n");

        int index = 1;
        for (ScanResult net : results) {
            String ssid = (net.SSID == null || net.SSID.isEmpty()) ? "[HIDDEN / СКРЫТАЯ СЕТЬ]" : net.SSID;
            String bssid = net.BSSID;
            int rssi = net.level;
            int quality = WifiManager.calculateSignalLevel(rssi, 100);
            int freq = net.frequency;
            int channel = getChannelFromFrequency(freq);
            String band = getBandFromFrequency(freq);
            
            // Расчет примерного расстояния до роутера (в метрах) по формуле свободного пространства
            double distanceMeters = calculateDistance(rssi, freq);

            // Определение вендора по MAC-адресу (OUI)
            String vendor = getVendorByMac(bssid);

            // Ширина канала
            String channelWidth = "20 MHz (или Legacy)";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                switch (net.channelWidth) {
                    case ScanResult.CHANNEL_WIDTH_40MHZ: channelWidth = "40 MHz (Широкий канал)"; break;
                    case ScanResult.CHANNEL_WIDTH_80MHZ: channelWidth = "80 MHz (High Speed)"; break;
                    case ScanResult.CHANNEL_WIDTH_160MHZ: channelWidth = "160 MHz (Ultra Speed)"; break;
                    case ScanResult.CHANNEL_WIDTH_320MHZ: channelWidth = "320 MHz (Wi-Fi 7)"; break;
                    case ScanResult.CHANNEL_WIDTH_20MHZ: channelWidth = "20 MHz"; break;
                }
            }

            // Анализ безопасности и уязвимостей по Capabilities
            String caps = net.capabilities;
            boolean hasWps = caps.contains("WPS");
            boolean hasPmf = caps.contains("MFPR") || caps.contains("MFPC"); // Protected Management Frames
            boolean hasWpa3 = caps.contains("WPA3") || caps.contains("SAE");
            boolean hasWep = caps.contains("WEP");
            boolean hasWpsLocked = caps.contains("WPS-"); // Косвенные признаки

            sb.append(String.format(Locale.US, "[%02d] SSID: %s\n", index++, ssid));
            sb.append(" ┣ 🏢 Производитель (Vendor): ").append(vendor).append("\n");
            sb.append(" ┣ 🆔 MAC (BSSID): ").append(bssid).append("\n");
            sb.append(" ┣ 📶 Сигнал: ").append(rssi).append(" dBm | Качество: ").append(quality).append("%\n");
            sb.append(" ┣ 📍 Расстояние (approx): ~").append(String.format(Locale.US, "%.1f", distanceMeters)).append(" м\n");
            sb.append(" ┣ 📻 Частота: ").append(freq).append(" MHz (").append(band)., Канал: ").append(channel).append(")\n");
            sb.append(" ┣ 📐 Ширина: ").append(channelWidth).append("\n");
            sb.append(" ┣ 🛡️ Аудит Безопасности:\n");
            sb.append(" ┃   ┣ Шифрование: ").append(parseDeepSecurity(caps)).append("\n");
            sb.append(" ┃   ┣ WPA3 / SAE: ").append(hasWpa3 ? "✅ Поддерживается (Защищен)" : "❌ Отсутствует").append("\n");
            sb.append(" ┃   ┣ PMF (Защита от деауха): ").append(hasPmf ? "✅ Включена" : "⚠️ Отключена (Уязвим к deauth)").append("\n");
            sb.append(" ┃   ┣ WPS (Быстрое подключение): ").append(haswpsText(hasWps)).append("\n");
            sb.append(" ┃   ┗ WEP / Устаревшее: ").append(hasWep ? "🚨 ВНИМАНИЕ: Уязвимый WEP!" : "Безопасно").append("\n");
            sb.append(" ┗ 🏷 Сырые Capabilities: ").append(caps).append("\n\n");
        }

        statusTextView.setText("Статус: ✅ Глубокий аудит роутеров завершен!");
        logEditText.setText(sb.toString());
    }

    private String getVendorByMac(String mac) {
        if (mac == null || mac.length() < 8) return "Неизвестный вендор";
        String oui = mac.substring(0, 8).toUpperCase(Locale.US);
        // База основных производителей сетевого оборудования
        if (oui.startsWith("E4:FA:C4") || oui.startsWith("14:EB:B6") || oui.startsWith("50:3F:58") || oui.startsWith("AC:84:C6")) return "TP-Link Technologies";
        if (oui.startsWith("CC:2D:21")) return "TP-Link / Shenzhen Bilian";
        if (oui.startsWith("C0:7B:BC") || oui.startsWith("00:11:32")) return "Synology / Router";
        if (oui.startsWith("10:7B:44") || oui.startsWith("28:EE:52") || oui.startsWith("AC:F3:09")) return "Xiaomi Communications";
        if (oui.startsWith("B4:FB:E4") || oui.startsWith("38:2C:4A") || oui.startsWith("68:FF:7B")) return "ASUSTeK Computer";
        if (oui.startsWith("64:D1:54") || oui.startsWith("74:DA:DA")) return "Ubiquiti Networks";
        if (oui.startsWith("4C:5E:0C") || oui.startsWith("64:64:44")) return "MikroTik";
        if (oui.startsWith("00:14:D1") || oui.startsWith("00:18:E7")) return "Cisco / Linksys";
        if (oui.startsWith("24:69:68") || oui.startsWith("70:8A:0C")) return "Huawei Technologies";
        return "Частный / Другой вендор (OUI: " + oui + ")";
    }

    private double calculateDistance(int rssi, int freqMhz) {
        // Эмпирический расчет расстояния по затуханию сигнала в свободном пространстве (FSPL)
        // d = 10 ^ ((27.55 - (20 * log10(freqMhz)) + Math.abs(rssi)) / 20)
        try {
            double exp = (27.55 - (20 * Math.log10(freqMhz)) + Math.abs(rssi)) / 20.0;
            return Math.pow(10.0, exp);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String haswpsText(boolean wps) {
        if (!wps) return "Отключен / Недоступен";
        return "⚠️ Включен (Возможен перебор PIN)";
    }

    private int getChannelFromFrequency(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            if (freq == 2484) return 14;
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else if (freq >= 5955 && freq <= 7115) {
            return (freq - 5950) / 5;
        }
        return 0;
    }

    private String getBandFromFrequency(int freq) {
        if (freq >= 2400 && freq < 2500) return "2.4 GHz";
        if (freq >= 5000 && freq < 5900) return "5 GHz";
        if (freq >= 5900 && freq <= 7200) return "6 GHz (Wi-Fi 6E/7)";
        return "Диапазон не определен";
    }

    private String parseDeepSecurity(String caps) {
        if (caps.contains("WPA3") && caps.contains("WPA2")) return "WPA2/WPA3 Mixed (Гибрид)";
        if (caps.contains("WPA3") || caps.contains("SAE")) return "WPA3-SAE (Максимальная защита)";
        if (caps.contains("WPA2")) return "WPA2-PSK (AES/CCMP)";
        if (caps.contains("WPA")) return "WPA-PSK (Устаревший)";
        if (caps.contains("WEP")) return "WEP (Критически уязвим)";
        return "Открытая сеть (Без пароля)";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (wifiScanReceiver != null) {
                unregisterReceiver(wifiScanReceiver);
            }
        } catch (Exception ignored) {}
    }
}