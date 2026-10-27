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

        // Главный макет (темная тема компилятора)
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

        // Заголовок
        TextView authorView = new TextView(this);
        authorView.setText("⚡ DEEP WI-FI SCANNER PRO v1.0 ⚡");
        authorView.setTextColor(Color.parseColor("#38BDF8"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.DEFAULT_BOLD);
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 12);
        container.addView(authorView);

        // Карточка интерфейса
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(20f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        // Кнопка сканирования
        btnScan = createStyledButton("📡 Запустить глубокий скан эфира", "#1D4ED8");
        btnScan.setOnClickListener(v -> startDeepWifiScan());
        card.addView(btnScan);

        // Статус
        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Ожидание запуска сканирования...");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(11f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 10);
        card.addView(statusTextView);

        // Консоль (вывод результатов)
        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится полная информация обо всех найденных Wi-Fi сетях...");
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

        // Кнопка копирования консоли
        btnCopy = createStyledButton("📋 Скопировать всю консоль в буфер", "#334155");
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("WifiScanLogs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Консоль успешно скопирована!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopy);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        // Регистрация ресивера для результатов сканирования
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                parseAndDisplayWifiResults(success);
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
            Toast.makeText(this, "Wi-Fi модуль не обнаружен!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Включаем модуль Wi-Fi...", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        statusTextView.setText("Статус: 🔄 Сканирование эфира...");
        logEditText.setText("Инициация поиска точек доступа...\nАнализ радиочастотного спектра...");

        boolean success = wifiManager.startScan();
        if (!success) {
            parseAndDisplayWifiResults(false);
        }
    }

    private void parseAndDisplayWifiResults(boolean scanTriggeredSuccess) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            statusTextView.setText("Статус: ⚠️ Сети не найдены или скан заблокирован ОС");
            logEditText.setText("⚠️ Список сетей пуст. Убедитесь, что геолокация (GPS) включена в настройках телефона.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 📊 ОТЧЕТ ГЛУБОКОГО СКАНИРОВАНИЯ WI-FI ===\n");
        sb.append("• Найдено точек доступа: ").append(results.size()).append("\n");
        sb.append("• Время сканирования: ").append(System.currentTimeMillis()).append("\n\n");

        int index = 1;
        for (ScanResult net : results) {
            String ssid = (net.SSID == null || net.SSID.isEmpty()) ? "[СКРЫТАЯ СЕТЬ / HIDDEN]" : net.SSID;
            String bssid = net.BSSID;
            int rssi = net.level;
            int quality = WifiManager.calculateSignalLevel(rssi, 100);
            int frequency = net.frequency;
            int channel = getChannelFromFrequency(frequency);
            String band = getBandFromFrequency(frequency);
            
            // Ширина канала
            String channelWidthStr = "Неизвестно";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                switch (net.channelWidth) {
                    case ScanResult.CHANNEL_WIDTH_20MHZ: channelWidthStr = "20 MHz"; break;
                    case ScanResult.CHANNEL_WIDTH_40MHZ: channelWidthStr = "40 MHz"; break;
                    case ScanResult.CHANNEL_WIDTH_80MHZ: channelWidthStr = "80 MHz"; break;
                    case ScanResult.CHANNEL_WIDTH_160MHZ: channelWidthStr = "160 MHz"; break;
                    case ScanResult.CHANNEL_WIDTH_320MHZ: channelWidthStr = "320 MHz"; break;
                    default: channelWidthStr = "Ширина: " + net.channelWidth; break;
                }
            }

            // Безопасное определение поколения Wi-Fi через рефлексию (исключает ошибки компилятора)
            String wifiStandardStr = "Legacy / 802.11";
            try {
                Field f = ScanResult.class.getField("wifiStandard");
                int standard = f.getInt(net);
                if (standard == 1) wifiStandardStr = "802.11a/b/g";
                else if (standard == 4) wifiStandardStr = "Wi-Fi 4 (802.11n)";
                else if (standard == 5) wifiStandardStr = "Wi-Fi 5 (802.11ac)";
                else if (standard == 6) wifiStandardStr = "Wi-Fi 6 / 6E (802.11ax)";
                else if (standard == 7) wifiStandardStr = "Wi-Fi 7 (802.11be)";
                else wifiStandardStr = "Стандарт ID: " + standard;
            } catch (Exception ignored) {}

            String caps = net.capabilities;
            String securityType = parseSecurityType(caps);

            sb.append(String.format(Locale.US, "[%02d] SSID: %s\n", index++, ssid));
            sb.append(" ┣ 🆔 BSSID (MAC): ").append(bssid).append("\n");
            sb.append(" ┣ 📶 Сигнал: ").append(rssi).append(" dBm (Качество: ").append(quality).append("%)\n");
            sb.append(" ┣ 📻 Частота / Диап: ").append(frequency).append(" MHz (").append(band).append(", Канал: ").append(channel).append(")\n");
            sb.append(" ┣ 📐 Ширина канала: ").append(channelWidthStr).append("\n");
            sb.append(" ┣ 🚀 Поколение Wi-Fi: ").append(wifiStandardStr).append("\n");
            sb.append(" ┣ 🔒 Безопасность: ").append(securityType).append("\n");
            sb.append(" ┗ 🏷 сырые Capabilities: ").append(caps).append("\n\n");
        }

        statusTextView.setText("Статус: ✅ Сканирование завершено успешно!");
        logEditText.setText(sb.toString());
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
        return "Другой диапазон";
    }

    private String parseSecurityType(String caps) {
        if (caps.contains("WPA3")) return "WPA3-Personal / Enterprise";
        if (caps.contains("WPA2") && caps.contains("WPA")) return "WPA/WPA2 Mixed";
        if (caps.contains("WPA2")) return "WPA2-PSK";
        if (caps.contains("WPA")) return "WPA-PSK";
        if (caps.contains("WEP")) return "WEP (Уязвимый!)";
        return "Открытая сеть (None)";
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