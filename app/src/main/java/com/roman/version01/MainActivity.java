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

import java.io.BufferedReader;
import java.io.FileReader;
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
        layout.setPadding(20, 20, 20, 20);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("🚀 Super-Monster Camera Scanner v3.0");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);
        layout.addView(title);

        // Статус
        statusText = new TextView(this);
        statusText.setText("Статус: Готов к супер-поиску (Все методы)");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(11f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 12);
        layout.addView(statusText);

        // Панель кнопок (Поиск + Консоль)
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);

        Button btnScanAll = createStyledButton("⚡ Найти ВСЕМИ путями", "#2563EB", 1.0f);
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
                startAllDiscoveryMethods();
            } else {
                Toast.makeText(this, "Сканирование уже выполняется...", Toast.LENGTH_SHORT).show();
            }
        });

        btnConsole.setOnClickListener(v -> showDebugConsoleDialog());

        // Нажатие на элемент списка
        cameraListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = foundDevices.get(position);
            if (selectedItem.startsWith("⚠️")) return;
            String addressPart = selectedItem.split(" ")[0];
            showMainMenuForCamera(addressPart);
        });

        setContentView(layout);
        logDebug("Приложение запущено. Все модули (ONVIF, ARP, Subnet, Web) активны.");
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
            .setTitle("💻 Консоль отладки")
            .setView(consoleLayout)
            .setPositiveButton("Закрыть", null)
            .show();
    }

    // ЗАПУСК ВСЕХ МЕТОДОВ В ПАРАЛЛЕЛИ
    private void startAllDiscoveryMethods() {
        foundDevices.clear();
        adapter.notifyDataSetChanged();
        isScanning = true;
        statusText.setText("Статус: 🔄 Работают все методы поиска...");
        logDebug("=== СТАРТ ПОЛНОГО КОМПЛЕКСНОГО ПОИСКА ===");

        // 1. Мгновенно читаем ARP кэш
        readSystemArpTable();

        // 2. В отдельном потоке запускаем ONVIF и Сканирование подсетей
        new Thread(() -> {
            startOnvifDiscovery();
            startSubnetPortScan();

            runOnUiThread(() -> {
                isScanning = false;
                statusText.setText("Статус: ✅ Все варианты поиска завершены.");
                logDebug("=== ВСЕ МЕТОДЫ ЗАВЕРШЕНЫ. Найдено объектов: " + foundDevices.size() + " ===");
                if (foundDevices.isEmpty()) {
                    foundDevices.add("⚠️ Устройства не найдены ни одним из методов.");
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    // МЕТОД 1: Чтение ARP-кэша Android
    private void readSystemArpTable() {
        logDebug("📖 [ARP-Кэш] Чтение системной таблицы железа...");
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String ip = parts[0];
                    String mac = parts[3];
                    if (!ip.equals("0.0.0.0") && !mac.equals("00:00:00:00:00:00")) {
                        final String arpEntry = ip + ":80 (Устройство из ARP-кэша)";
                        logDebug("📌 [ARP Найдено]: " + ip + " [MAC: " + mac + "]");
                        
                        runOnUiThread(() -> {
                            if (!foundDevices.contains(arpEntry)) {
                                foundDevices.add(arpEntry);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            logDebug("❌ [ARP Ошибка]: " + e.getMessage());
        }
    }

    // МЕТОД 2: ONVIF Broadcast (UDP 3702)
    private void startOnvifDiscovery() {
        logDebug("📡 [ONVIF] Запуск аппаратного широковещания (UDP 3702)...");
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

            socket.setSoTimeout(2500);
            byte[] recvBuf = new byte[4096];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);

            while (isScanning) {
                try {
                    socket.receive(recvPacket);
                    String senderIp = recvPacket.getAddress().getHostAddress();
                    logDebug("🎯 [ONVIF Ответ от железа!]: " + senderIp);
                    final String cameraResult = senderIp + ":554 (ONVIF Камера / Домофон)";

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
            logDebug("✅ [ONVIF] Завершено.");
        } catch (Exception e) {
            logDebug("❌ [ONVIF Ошибка]: " + e.getMessage());
        }
    }

    // МЕТОД 3: Сканирование портов и подсетей
    private void startSubnetPortScan() {
        logDebug("🔎 [PortScan] Сканирование диапазонов и портов...");
        List<String> targetSubnets = new ArrayList<>();
        String currentSubnet = getLocalSubnet();
        if (currentSubnet != null) {
            targetSubnets.add(currentSubnet);
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

            for (int i = 1; i < 255; i++) {
                if (!isScanning) break;
                final String host = subnet + i;

                for (int port : targetPorts) {
                    if (!isScanning) break;

                    if (isPortOpen(host, port, 35)) {
                        String protoName = getProtocolName(port);
                        final String resultStr = host + ":" + port + " (" + protoName + ")";
                        logDebug("✅ [PortScan НАЙДЕНО]: " + resultStr);

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
            case 80: return "HTTP / Web";
            case 8080: return "HTTP Alt";
            case 554: return "RTSP Поток";
            case 8000: return "Hikvision";
            case 37777: return "Dahua";
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
        } catch (Exception ignored) {}
        return null;
    }

    // МЕНЮ УПРАВЛЕНИЯ
    private void showMainMenuForCamera(String ipAndPort) {
        CharSequence[] options = {
            "🌐 Открыть Веб-панель (Исправлена клавиатура и масштаб)",
            "📹 Подменю RTSP потоков",
            "🔍 Проверить отклик (Пинг)",
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
                    Toast.makeText(this, "IP:Порт скопирован!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }

    private void showRtspSubmenu(String ipAndPort) {
        String pureIp = ipAndPort.split(":")[0];
        CharSequence[] rtspOptions = {
            "📹 Основной поток (stream1)",
            "📹 Субпоток / Low quality (stream2)",
            "📹 Поток live.sdp",
            "📹 H264 Превью",
            "📋 Скопировать стандартную ссылку"
        };

        new AlertDialog.Builder(this)
            .setTitle("📦 RTSP ссылки: " + pureIp)
            .setItems(rtspOptions, (dialog, which) -> {
                String url = "rtsp://" + pureIp + ":554/stream1";
                if (which == 1) url = "rtsp://" + pureIp + ":554/stream2";
                else if (which == 2) url = "rtsp://" + pureIp + ":554/live.sdp";
                else if (which == 3) url = "rtsp://" + pureIp + ":554/h264Preview_01_main";

                copyToClipboard(url);
                Toast.makeText(this, "Скопировано! Вставьте в плеер (например, VLC)", Toast.LENGTH_LONG).show();
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

    // УЛУЧШЕННЫЙ ВЕБ-БРАУЗЕР С КРУПНЫМ ШРИФТОМ И КЛАВИАТУРОЙ
    private void openWebStream(String url) {
        LinearLayout webLayout = new LinearLayout(this);
        webLayout.setOrientation(LinearLayout.VERTICAL);
        webLayout.setBackgroundColor(Color.parseColor("#030712"));

        WebView webView = new WebView(this);
        
        // Включаем фокус для всплывающей клавиатуры
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // КРУПНЫЙ ШРИФТ И МАСШТАБИРОВАНИЕ
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false); // Прячем уродливые кнопки зума
        settings.setTextZoom(130); // Увеличиваем размер текста на 130%

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);

        webLayout.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1000
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("🌐 Открыто: " + url)
            .setView(webLayout)
            .setPositiveButton("Закрыть", (d, w) -> webView.destroy())
            .create();

        // Позволяем всплывать клавиатуре поверх диалогового окна
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