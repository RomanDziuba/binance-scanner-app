package com.roman.version01;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {
    private TextView statusText;
    private ListView cameraListView;
    private List<String> foundDevices = new ArrayList<>();
    private List<String> debugLogs = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private volatile boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(20, 20, 20, 20);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("🚀 Ultimate Monster Camera Scanner & Console");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);
        layout.addView(title);

        // Статус
        statusText = new TextView(this);
        statusText.setText("Статус: Готов к сканированию");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(11f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 12);
        layout.addView(statusText);

        // Панель кнопок (Поиск + Консоль)
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);

        Button btnScanAll = createStyledButton("🔍 Сканировать всё", "#2563EB", 1.0f);
        Button btnConsole = createStyledButton("💻 Консоль логов", "#475569", 1.0f);

        btnLayout.addView(btnScanAll);
        
        ViewGroup.MarginLayoutParams consoleParams = (ViewGroup.MarginLayoutParams) btnConsole.getLayoutParams();
        if (consoleParams != null) {
            consoleParams.setMargins(10, 0, 0, 0);
        }
        btnLayout.addView(btnConsole);

        layout.addView(btnLayout);

        // Список найденных устройств
        cameraListView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 850
        );
        listParams.setMargins(0, 15, 0, 15);
        cameraListView.setLayoutParams(listParams);
        layout.addView(cameraListView);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, foundDevices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                if (tv != null) {
                    tv.setTextColor(Color.parseColor("#E2E8F0"));
                    tv.setTextSize(12f);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        cameraListView.setAdapter(adapter);

        // События кнопок
        btnScanAll.setOnClickListener(v -> {
            if (!isScanning) {
                startMultiPathScan();
            } else {
                Toast.makeText(this, "Сканирование уже выполняется...", Toast.LENGTH_SHORT).show();
            }
        });

        btnConsole.setOnClickListener(v -> showDebugConsoleDialog());

        // Нажатие на найденную камеру -> Главное Меню
        cameraListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = foundDevices.get(position);
            if (selectedItem.startsWith("⚠️")) return;
            String addressPart = selectedItem.split(" ")[0];
            showMainMenuForCamera(addressPart);
        });

        setContentView(layout);
        logDebug("Приложение запущено. Интерфейс инициализирован.");
    }

    private Button createStyledButton(String text, String colorHex, float weight) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(10, 12, 10, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, weight
        );
        btn.setLayoutParams(params);
        return btn;
    }

    private void logDebug(String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timeStamp + "] " + message;
        debugLogs.add(0, logEntry); // Новые логи сверху
        if (debugLogs.size() > 250) {
            debugLogs.remove(debugLogs.size() - 1);
        }
    }

    // ОТЛАДОЧНАЯ КОНСОЛЬ
    private void showDebugConsoleDialog() {
        LinearLayout consoleLayout = new LinearLayout(this);
        consoleLayout.setOrientation(LinearLayout.VERTICAL);
        consoleLayout.setPadding(15, 15, 15, 15);
        consoleLayout.setBackgroundColor(Color.parseColor("#090D16"));

        final EditText logEditText = new EditText(this);
        StringBuilder sb = new StringBuilder();
        for (String log : debugLogs) {
            sb.append(log).append("\n");
        }
        logEditText.setText(sb.toString());
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#10B981"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setEnabled(false); // Только для чтения

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 800
        ));
        scroll.addView(logEditText);
        consoleLayout.addView(scroll);

        Button btnClear = createStyledButton("🗑 Очистить консоль", "#991B1B", 1.0f);
        btnClear.setOnClickListener(v -> {
            debugLogs.clear();
            logEditText.setText("");
            Toast.makeText(this, "Консоль очищена", Toast.LENGTH_SHORT).show();
        });
        consoleLayout.addView(btnClear);

        new AlertDialog.Builder(this)
            .setTitle("💻 Отладочная консоль (Поиск и Подключение)")
            .setView(consoleLayout)
            .setPositiveButton("Закрыть", null)
            .show();
    }

    private void startMultiPathScan() {
        foundDevices.clear();
        adapter.notifyDataSetChanged();
        isScanning = true;
        statusText.setText("Статус: 🔄 Сканирование сетей и портов...");
        logDebug("=== СТАРТ МНОГОПУТЕВОГО СКАНИРОВАНИЯ ===");

        new Thread(() -> {
            List<String> targetSubnets = new ArrayList<>();
            String currentSubnet = getLocalSubnet();
            if (currentSubnet != null) {
                targetSubnets.add(currentSubnet);
                logDebug("Определена текущая подсеть Wi-Fi: " + currentSubnet + "x");
            } else {
                logDebug("⚠️ Локальная подсеть Wi-Fi не определена. Сканируем стандартные шлюзы.");
            }

            targetSubnets.add("192.168.1.");
            targetSubnets.add("192.168.0.");
            targetSubnets.add("192.168.8.");
            targetSubnets.add("192.168.100.");
            targetSubnets.add("10.0.0.");
            targetSubnets.add("172.16.0.");

            int[] targetPorts = {80, 8080, 554, 8000, 37777};

            for (String subnet : targetSubnets) {
                if (!isScanning) break;
                logDebug("Сканирование подсети: " + subnet + "1-254");

                for (int i = 1; i < 255; i++) {
                    if (!isScanning) break;
                    final String host = subnet + i;

                    for (int port : targetPorts) {
                        if (!isScanning) break;
                        
                        if (isPortOpen(host, port, 40)) {
                            String protoName = getProtocolName(port);
                            final String resultStr = host + ":" + port + " (" + protoName + ")";
                            logDebug("✅ НАЙДЕНО: " + resultStr);

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
                logDebug("=== СКАНИРОВАНИЕ ЗАВЕРШЕНО. Найдено: " + foundDevices.size() + " ===");
                if (foundDevices.isEmpty()) {
                    foundDevices.add("⚠️ Устройства не найдены ни в одной подсети.");
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private boolean isPortOpen(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getProtocolName(int port) {
        switch (port) {
            case 80: return "HTTP / Веб-панель";
            case 8080: return "HTTP (Alt Web)";
            case 554: return "RTSP Поток";
            case 8000: return "Hikvision DVR";
            case 37777: return "Dahua DVR";
            default: return "Порт " + port;
        }
    }

    private String getLocalSubnet() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                if (ipAddress != 0) {
                    String ipString = String.format("%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));

                    int lastDot = ipString.lastIndexOf('.');
                    if (lastDot != -1) {
                        return ipString.substring(0, lastDot + 1);
                    }
                }
            }
        } catch (Exception e) {
            logDebug("❌ Ошибка получения локального IP: " + e.getMessage());
        }
        return null;
    }

    // ГЛАВНОЕ МЕНЮ ДЛЯ ВЫБРАННОЙ КАМЕРЫ
    private void showMainMenuForCamera(String ipAndPort) {
        logDebug("Открытие главного меню для устройства: " + ipAndPort);
        CharSequence[] options = {
            "🌐 Открыть Web-интерфейс в WebView",
            "📹 Меню RTSP потоков (Подменю)",
            "🔍 Проверить пинг / Статус порта",
            "📋 Скопировать IP:Порт"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Меню управления: " + ipAndPort)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    logDebug("Действие: Открытие Web UI для http://" + ipAndPort);
                    openWebStream("http://" + ipAndPort);
                } else if (which == 1) {
                    logDebug("Переход в подменю RTSP для " + ipAndPort);
                    showRtspSubmenu(ipAndPort);
                } else if (which == 2) {
                    logDebug("Запуск проверки связи для " + ipAndPort);
                    testConnectionStatus(ipAndPort);
                } else if (which == 3) {
                    copyToClipboard(ipAndPort);
                    logDebug("Скопировано в буфер: " + ipAndPort);
                    Toast.makeText(this, "IP:Порт скопирован!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }

    // ПОДМЕНЮ RTSP ПОТОКОВ
    private void showRtspSubmenu(String ipAndPort) {
        String pureIp = ipAndPort.split(":")[0];
        CharSequence[] rtspOptions = {
            "📹 Основной поток (stream1)",
            "📹 Субпоток / Низкое качество (stream2)",
            "📹 Поток через live.sdp",
            "📹 Превью H264 (Hikvision/Dahua)",
            "📋 Скопировать стандартную ссылку"
        };

        new AlertDialog.Builder(this)
            .setTitle("📦 Подменю RTSP: " + pureIp)
            .setItems(rtspOptions, (dialog, which) -> {
                String url = "";
                if (which == 0) {
                    url = "rtsp://" + pureIp + ":554/stream1";
                } else if (which == 1) {
                    url = "rtsp://" + pureIp + ":554/stream2";
                } else if (which == 2) {
                    url = "rtsp://" + pureIp + ":554/live.sdp";
                } else if (which == 3) {
                    url = "rtsp://" + pureIp + ":554/h264Preview_01_main";
                } else if (which == 4) {
                    url = "rtsp://" + pureIp + ":554/ch0_0.h264";
                }
                
                logDebug("Сформирована и скопирована RTSP ссылка: " + url);
                copyToClipboard(url);
                Toast.makeText(this, "RTSP ссылка скопирована в буфер!", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Назад", (d, w) -> showMainMenuForCamera(ipAndPort))
            .show();
    }

    // ДЕТАЛЬНАЯ ПРОВЕРКА СВЯЗИ (ПИНГ)
    private void testConnectionStatus(String ipAndPort) {
        String[] parts = ipAndPort.split(":");
        final String ip = parts[0];
        final int port = Integer.parseInt(parts[1]);

        statusText.setText("Статус: 🔄 Проверка связи с " + ip + "...");
        logDebug("Начало проверки отклика для " + ip + ":" + port);

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            boolean reachable = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 1500);
                reachable = true;
            } catch (Exception e) {
                logDebug("❌ Ошибка пинга " + ip + ":" + port + " -> " + e.getMessage());
            }
            long duration = System.currentTimeMillis() - startTime;
            final boolean success = reachable;
            final long timeMs = duration;

            runOnUiThread(() -> {
                statusText.setText("Статус: Проверка завершена");
                String msg = success ? "✅ Устройство онлайн! Отклик: " + timeMs + " мс" : "❌ Устройство не отвечает или порт закрыт.";
                logDebug("Результат проверки " + ip + ":" + port + " -> " + msg);
                
                new AlertDialog.Builder(this)
                    .setTitle("📊 Статус соединения")
                    .setMessage("Устройство: " + ipAndPort + "\n\n" + msg)
                    .setPositiveButton("ОК", null)
                    .show();
            });
        }).start();
    }

    private void openWebStream(String url) {
        LinearLayout webLayout = new LinearLayout(this);
        webLayout.setOrientation(LinearLayout.VERTICAL);
        webLayout.setBackgroundColor(Color.parseColor("#030712"));

        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                logDebug("⚠️ Ошибка WebView (" + errorCode + ") для " + failingUrl + ": " + description);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
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
            .setPositiveButton("Закрыть", (dialog, which) -> {
                webView.destroy();
                logDebug("Закрыт WebView для " + url);
            })
            .show();
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("CameraLink", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            logDebug("❌ Ошибка копирования: " + e.getMessage());
        }
    }
}