package com.kamera.version01;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {
    private TextView statusText;
    private ListView cameraListView;
    private List<String> foundDevices = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private volatile boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(24, 24, 24, 24);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("🚀 Multi-Path Monster Camera Scanner");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        // Статус
        statusText = new TextView(this);
        statusText.setText("Статус: Готов к многопутевому сканированию");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 16);
        layout.addView(statusText);

        // Кнопка запуска всех путей
        Button btnScanAll = new Button(this);
        btnScanAll.setText("🔍 Запустить все пути поиска (Подсети + Порты)");
        btnScanAll.setTextColor(Color.parseColor("#F8FAFC"));
        btnScanAll.setTextSize(12f);
        btnScanAll.setTypeface(Typeface.DEFAULT_BOLD);
        btnScanAll.setBackgroundColor(Color.parseColor("#2563EB"));
        btnScanAll.setPadding(16, 16, 16, 16);
        layout.addView(btnScanAll);

        // Список найденных устройств
        cameraListView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 800
        );
        listParams.setMargins(0, 20, 0, 20);
        cameraListView.setLayoutParams(listParams);
        layout.addView(cameraListView);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, foundDevices) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                if (tv != null) {
                    tv.setTextColor(Color.parseColor("#E2E8F0"));
                    tv.setTextSize(13f);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        cameraListView.setAdapter(adapter);

        // Обработка нажатия на элемент списка
        cameraListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = foundDevices.get(position);
            if (selectedItem.startsWith("⚠️")) return;
            String addressPart = selectedItem.split(" ")[0]; // Извлекаем IP:Порт
            showConnectionOptions(addressPart);
        });

        btnScanAll.setOnClickListener(v -> {
            if (!isScanning) {
                startMultiPathScan();
            } else {
                Toast.makeText(this, "Сканирование уже выполняется...", Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(layout);
    }

    private void startMultiPathScan() {
        foundDevices.clear();
        adapter.notifyDataSetChanged();
        isScanning = true;
        statusText.setText("Статус: 🔄 Сканирование всех подсетей и портов...");

        new Thread(() -> {
            // Формируем список подсетей (текущая + альтернативные шлюзы)
            List<String> targetSubnets = new ArrayList<>();
            String currentSubnet = getLocalSubnet();
            if (currentSubnet != null && !targetSubnets.contains(currentSubnet)) {
                targetSubnets.add(currentSubnet);
            }
            targetSubnets.add("192.168.1.");
            targetSubnets.add("192.168.0.");
            targetSubnets.add("192.168.8.");
            targetSubnets.add("192.168.100.");
            targetSubnets.add("10.0.0.");
            targetSubnets.add("172.16.0.");

            // Целевые порты (HTTP, RTSP, DVR)
            int[] targetPorts = {80, 8080, 554, 8000, 37777};

            for (String subnet : targetSubnets) {
                if (!isScanning) break;
                for (int i = 1; i < 255; i++) {
                    if (!isScanning) break;
                    final String host = subnet + i;

                    for (int port : targetPorts) {
                        if (!isScanning) break;
                        // Быстрый таймаут в 50мс для ускорения поиска по множеству сетей
                        if (isPortOpen(host, port, 50)) {
                            String protoName = getProtocolName(port);
                            final String resultStr = host + ":" + port + " (" + protoName + ")";
                            
                            runOnUiThread(() -> {
                                if (!foundDevices.contains(resultStr)) {
                                    foundDevices.add(resultStr);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                            break; 
                        }
                    }
                }
            }

            runOnUiThread(() -> {
                isScanning = false;
                statusText.setText("Статус: ✅ Поиск завершен.");
                if (foundDevices.isEmpty()) {
                    foundDevices.add("⚠️ Устройства не найдены ни в одной подсети.");
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private String getProtocolName(int port) {
        switch (port) {
            case 80: return "HTTP / Веб-панель";
            case 8080: return "HTTP (Alt Web)";
            case 554: return "RTSP Поток";
            case 8000: return "Hikvision SDK";
            case 37777: return "Dahua DVR";
            default: return "Порт " + port;
        }
    }

    private boolean isPortOpen(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getLocalSubnet() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            if (ipAddress == 0) return null;

            String ipString = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

            int lastDot = ipString.lastIndexOf('.');
            if (lastDot != -1) {
                return ipString.substring(0, lastDot + 1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void showConnectionOptions(String ipAndPort) {
        CharSequence[] options = {
            "🌐 Открыть Web-интерфейс",
            "📹 Скопировать RTSP ссылку потока",
            "📋 Скопировать IP:Порт"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Выберите действие для: " + ipAndPort)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openWebStream("http://" + ipAndPort);
                } else if (which == 1) {
                    String rtspUrl = "rtsp://" + ipAndPort.split(":")[0] + ":554/stream1";
                    copyToClipboard(rtspUrl);
                    Toast.makeText(this, "RTSP ссылка скопирована!", Toast.LENGTH_SHORT).show();
                } else if (which == 2) {
                    copyToClipboard(ipAndPort);
                    Toast.makeText(this, "IP:Порт скопирован!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void openWebStream(String url) {
        LinearLayout webLayout = new LinearLayout(this);
        webLayout.setOrientation(LinearLayout.VERTICAL);
        webLayout.setBackgroundColor(Color.parseColor("#030712"));

        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.loadUrl(url);

        webLayout.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 900
        ));

        new AlertDialog.Builder(this)
            .setTitle("🎥 Просмотр: " + url)
            .setView(webLayout)
            .setPositiveButton("Закрыть", (dialog, which) -> webView.destroy())
            .show();
    }

    private void copyToClipboard(String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("CameraLink", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception ignored) {}
    }
}