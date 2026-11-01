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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
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
        layout.setPadding(16, 16, 16, 16);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("🚀 Ultimate Monster Scanner v4.0 Pro");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        layout.addView(title);

        // Статус
        statusText = new TextView(this);
        statusText.setText("Статус: Готов к глубокому поиску");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(11f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 10);
        layout.addView(statusText);

        // Панель кнопок (Поиск + Консоль + Cloud API)
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);

        Button btnScanAll = createStyledButton("⚡ Бренд-Скан", "#2563EB", 1.0f);
        Button btnCloudApi = createStyledButton("☁️ Cloud API", "#0D9488", 1.0f);
        Button btnConsole = createStyledButton("💻 Логи", "#475569", 1.0f);

        btnLayout.addView(btnScanAll);
        
        ViewGroup.MarginLayoutParams p1 = (ViewGroup.MarginLayoutParams) btnCloudApi.getLayoutParams();
        if (p1 != null) p1.setMargins(8, 0, 0, 0);
        btnLayout.addView(btnCloudApi);

        ViewGroup.MarginLayoutParams p2 = (ViewGroup.MarginLayoutParams) btnConsole.getLayoutParams();
        if (p2 != null) p2.setMargins(8, 0, 0, 0);
        btnLayout.addView(btnConsole);

        layout.addView(btnLayout);

        // Список найденных устройств
        cameraListView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 820
        );
        listParams.setMargins(0, 12, 0, 12);
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
                startComprehensiveBrandScan();
            } else {
                Toast.makeText(this, "Сканирование уже выполняется...", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloudApi.setOnClickListener(v -> showCloudApiDatabaseDialog());
        btnConsole.setOnClickListener(v -> showDebugConsoleDialog());

        // Нажатие на элемент списка
        cameraListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = foundDevices.get(position);
            if (selectedItem.startsWith("⚠️")) return;
            String addressPart = selectedItem.split(" ")[0];
            showMainMenuForCamera(addressPart);
        });

        setContentView(layout);
        logDebug("Приложение v4.0 запущенно. База брендов и Cloud API загружена.");
    }

    private Button createStyledButton(String text, String colorHex, float weight) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(10f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(6, 10, 6, 10);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, weight
        );
        btn.setLayoutParams(params);
        return btn;
    }

    private void logDebug(String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timeStamp + "] " + message;
        debugLogs.add(0, logEntry);
        if (debugLogs.size() > 300) {
            debugLogs.remove(debugLogs.size() - 1);
        }
    }

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
        logEditText.setEnabled(false);

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
            .setTitle("💻 Отладочная консоль")
            .setView(consoleLayout)
            .setPositiveButton("Закрыть", null)
            .show();
    }

    // БАЗА ДАННЫХ CLOUD API И ШЛЮЗОВ ОФИЦИАЛЬНЫХ ПРИЛОЖЕНИЙ
    private void showCloudApiDatabaseDialog() {
        CharSequence[] cloudProviders = {
            "🌐 Tuya / Smart Life (Шлюзы и Облако)",
            "📹 EZVIZ / Hik-Connect (Токены и Потоки)",
            "🏠 Ростелеком / Дом.ру / Уфанет (Домофония)",
            "📱 Xiaomi Mi Home / Yi Home (Cloud Links)",
            "☁️ Ivideon / Trassir Cloud Web API"
        };

        new AlertDialog.Builder(this)
            .setTitle("☁️ База Cloud API и шлюзов")
            .setItems(cloudProviders, (dialog, which) -> {
                String title = "";
                String info = "";
                if (which == 0) {
                    title = "Tuya / Smart Life API";
                    info = "• Официальное приложение использует OpenAPI (api.tuya.com).\n" +
                           "• Локальный RTSP для шлюзов:\n  rtsp://[IP]:8554/live/ch0\n" +
                           "• Требует токен авторизации через Tuya Developer Platform.";
                } else if (which == 1) {
                    title = "EZVIZ / Hik-Connect Cloud";
                    info = "• Облачные камеры отдают поток по протоколу HLS/RTSP через сессионный ключ.\n" +
                           "• Ссылка потока:\n  rtsp://[IP]:554/h264/ch1/main/av_stream\n" +
                           "• Проверка через EZVIZ Open API.";
                } else if (which == 2) {
                    title = "Провайдерские домофоны (Ростелеком/Дом.ру)";
                    info = "• Видеопоток завязан на закрытые VLAN.\n" +
                           "• Ссылки обычно имеют вид:\n  https://[город].domru.ru/video/[id]/index.m3u8\n" +
                           "• Доступен через сниффинг трафика официального приложения.";
                } else if (which == 3) {
                    title = "Xiaomi / Yi Home";
                    info = "• Камеры прошиваются кастомными RTSP-серверами (Yi-Hack).\n" +
                           "• Стандартный локальный поток:\n  rtsp://[IP]:554/ch0_0.h264";
                } else {
                    title = "Ivideon / Trassir Cloud";
                    info = "• Облачный сервис с поддержкойпрямых API запросов.\n" +
                           "• Ссылка интеграции:\n  rtsp://ivideon:[password]@[IP]:10554/v1/u";
                }

                new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(info)
                    .setPositiveButton("Скопировать шаблон", (d, w) -> {
                        copyToClipboard(info);
                        Toast.makeText(this, "Информация скопирована!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Назад", (d, w) -> showCloudApiDatabaseDialog())
                    .show();
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }

    // КОМПЛЕКСНЫЙ СКАНЕР С УЧЕТОМ СПЕЦИФИЧЕСКИХ ПОРТОВ БРЕНДОВ
    private void startComprehensiveBrandScan() {
        foundDevices.clear();
        adapter.notifyDataSetChanged();
        isScanning = true;
        statusText.setText("Статус: 🔄 Сканирование портов брендов и ONVIF...");
        logDebug("=== СТАРТ РАСШИРЕННОГО СКАНЕРА БРЕНДОВ ===");

        new Thread(() -> {
            // 1. Запуск ONVIF широковещания
            startOnvifDiscovery();

            // 2. Сканирование подсетей с расширенными портами брендов
            List<String> targetSubnets = new ArrayList<>();
            String currentSubnet = getLocalSubnet();
            if (currentSubnet != null) targetSubnets.add(currentSubnet);

            targetSubnets.add("192.168.1.");
            targetSubnets.add("192.168.0.");
            targetSubnets.add("192.168.8.");
            targetSubnets.add("192.168.100.");
            targetSubnets.add("10.0.0.");

            // РАСШИРЕННЫЙ СПИСОК СПЕЦИФИЧЕСКИХ ПОРТОВ ПРОИЗВОДИТЕЛЕЙ
            int[] brandPorts = {
                80,    // Стандартный Web
                443,   // Защищенный Web (SSL)
                554,   // RTSP Поток
                8080,  // Альтернативный Web
                8554,  // Tuya / VLC RTSP
                8000,  // Hikvision SDK / Service
                37020, // Hikvision SADP
                37777, // Dahua DVR / NVR
                37778, // Dahua Service
                88,    // Foscam Web
                9000,  // Reolink Media
                2020   // TP-Link Tapo / VIGI
            };

            for (String subnet : targetSubnets) {
                if (!isScanning) break;
                logDebug("Сканирование диапазона: " + subnet + "1-254");

                for (int i = 1; i < 255; i++) {
                    if (!isScanning) break;
                    final String host = subnet + i;

                    for (int port : brandPorts) {
                        if (!isScanning) break;

                        if (isPortOpen(host, port, 30)) {
                            String brandName = getBrandNameByPort(port);
                            final String resultStr = host + ":" + port + " (" + brandName + ")";
                            logDebug("✅ НАЙДЕНО [Бренд/Порт]: " + resultStr);

                            runOnUiThread(() -> {
                                if (!foundDevices.contains(resultStr)) {
                                    foundDevices.add(resultStr);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                            // Не прерываем сразу по IP, так как у камеры может быть открыто несколько портов (например, 80 и 554)
                        }
                    }
                }
            }

            runOnUiThread(() -> {
                isScanning = false;
                statusText.setText("Статус: ✅ Поиск брендов завершен.");
                logDebug("=== СКАНИРОВАНИЕ ЗАВЕРШЕНО. Найдено: " + foundDevices.size() + " ===");
                if (foundDevices.isEmpty()) {
                    foundDevices.add("⚠️ Устройства брендов не найдены.");
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

    private String getBrandNameByPort(int port) {
        switch (port) {
            case 80: return "HTTP / Универсальный Web";
            case 443: return "HTTPS Защищенный Web";
            case 554: return "RTSP Стандартный поток";
            case 8080: return "HTTP Альтернативный";
            case 8554: return "Tuya / Smart Life RTSP";
            case 8000: return "Hikvision (SDK / DVR)";
            case 37020: return "Hikvision SADP";
            case 37777: return "Dahua (DVR / IPC)";
            case 37778: return "Dahua Service";
            case 88: return "Foscam Camera";
            case 9000: return "Reolink Media Port";
            case 2020: return "TP-Link Tapo / VIGI";
            default: return "Порт " + port;
        }
    }

    private void startOnvifDiscovery() {
        logDebug("📡 [ONVIF] Запуск широковещательного поиска (UDP 3702)...");
        try {
            String probeMessage = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" " +
                "xmlns:w=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" " +
                "xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\">\n" +
                "<e:Header><w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>" +
                "<w:To>urn:schemas-xmlsoap-org:ws:2005/04/discovery</w:To></e:Header>" +
                "<e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body>" +
                "</e:Envelope>";

            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] buffer = probeMessage.getBytes(StandardCharsets.UTF_8);

            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 3702);
            socket.send(packet);

            socket.setSoTimeout(2000);
            byte[] recvBuf = new byte[4096];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);

            while (isScanning) {
                try {
                    socket.receive(recvPacket);
                    String senderIp = recvPacket.getAddress().getHostAddress();
                    logDebug("🎯 [ONVIF Ответ от железа!]: " + senderIp);
                    final String cameraResult = senderIp + ":554 (ONVIF / IP-Камера)";

                    runOnUiThread(() -> {
                        if (!foundDevices.contains(cameraResult)) {
                            foundDevices.add(cameraResult);
                            adapter.notifyDataSetChanged();
                        }
                    });
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            socket.close();
            logDebug("✅ [ONVIF] Поиск завершен.");
        } catch (Exception e) {
            logDebug("❌ [ONVIF Ошибка]: " + e.getMessage());
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
        } catch (Exception ignored) {}
        return null;
    }

    // МЕНЮ УПРАВЛЕНИЯ УСТРОЙСТВОМ
    private void showMainMenuForCamera(String ipAndPort) {
        CharSequence[] options = {
            "🌐 Открыть Веб-интерфейс (С крупным шрифтом)",
            "📹 Подменю RTSP потоков брендов",
            "🔍 Проверить пинг / Статус",
            "📋 Скопировать IP:Порт"
        };

        new AlertDialog.Builder(this)
            .setTitle("🛠 Меню: " + ipAndPort)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openWebStream("http://" + ipAndPort);
                } else if (which == 1) {
                    showRtspSubmenu(ipAndPort);
                } else if (which == 2) {
                    testConnectionStatus(ipAndPort);
                } else if (which == 3) {
                    copyToClipboard(ipAndPort);
                    Toast.makeText(this, "Скопировано в буфер!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }

    private void showRtspSubmenu(String ipAndPort) {
        String pureIp = ipAndPort.split(":")[0];
        CharSequence[] rtspOptions = {
            "📹 Стандартный / Hikvision (stream1)",
            "📹 Доп. поток / Substream (stream2)",
            "📹 Dahua / Uniview (cam/realmonitor)",
            "📹 Tuya / Smart Life (live/ch0)",
            "📋 Скопировать универсальную ссылку"
        };

        new AlertDialog.Builder(this)
            .setTitle("📦 RTSP ссылки брендов: " + pureIp)
            .setItems(rtspOptions, (dialog, which) -> {
                String url = "rtsp://" + pureIp + ":554/stream1";
                if (which == 1) url = "rtsp://" + pureIp + ":554/stream2";
                else if (which == 2) url = "rtsp://" + pureIp + ":554/cam/realmonitor?channel=1&subtype=0";
                else if (which == 3) url = "rtsp://" + pureIp + ":8554/live/ch0";

                copyToClipboard(url);
                Toast.makeText(this, "RTSP скопирована! Вставьте в VLC Player", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Назад", (d, w) -> showMainMenuForCamera(ipAndPort))
            .show();
    }

    private void testConnectionStatus(String ipAndPort) {
        String[] parts = ipAndPort.split(":");
        final String ip = parts[0];
        final int port = Integer.parseInt(parts[1]);

        logDebug("Пинг " + ip + ":" + port + "...");
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            boolean reachable = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 1500);
                reachable = true;
            } catch (Exception ignored) {}
            long duration = System.currentTimeMillis() - startTime;
            final boolean success = reachable;
            final long timeMs = duration;

            runOnUiThread(() -> {
                String msg = success ? "✅ Онлайн! Отклик: " + timeMs + " мс" : "❌ Офлайн / Порт закрыт.";
                logDebug("Результат пинга: " + msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    // УЛУЧШЕННЫЙ ВЕБ-БРАУЗЕР (КРУПНЫЙ ШРИФТ + КЛАВИАТУРА)
    private void openWebStream(String url) {
        LinearLayout webLayout = new LinearLayout(this);
        webLayout.setOrientation(LinearLayout.VERTICAL);
        webLayout.setBackgroundColor(Color.parseColor("#030712"));

        WebView webView = new WebView(this);
        
        // Фокус для клавиатуры
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // МАСШТАБ И КРУПНЫЙ ШРИФТ
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(130); // Увеличение текста на 30%

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.proceed(); // Игнорируем SSL ошибки камер
            }
        });

        webView.loadUrl(url);

        webLayout.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1000
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("🌐 Веб-панель: " + url)
            .setView(webLayout)
            .setPositiveButton("Закрыть", (d, w) -> webView.destroy())
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.show();
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("CameraLink", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception ignored) {}
    }
}