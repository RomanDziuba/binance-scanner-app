package com.peredachafaylov.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class MainActivity extends Activity {

    private TextView statusView, logView;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int port = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(35, 40, 35, 40);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(28f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        TextView title = new TextView(this);
        title.setText("🛜 Wi-Fi Файл-Сервер");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        card.addView(title);

        statusView = new TextView(this);
        statusView.setText("Статус: Запуск сервера...");
        statusView.setTextColor(Color.parseColor("#10B981"));
        statusView.setTextSize(14f);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 0, 0, 16);
        card.addView(statusView);

        Button btnToggle = new Button(this);
        btnToggle.setText("🔄 Перезапустить сервер");
        btnToggle.setBackgroundColor(Color.parseColor("#2563EB"));
        btnToggle.setTextColor(Color.WHITE);
        btnToggle.setPadding(20, 15, 20, 15);
        btnToggle.setOnClickListener(v -> restartServer());
        card.addView(btnToggle);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Логи подключений:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(12f);
        logLabel.setPadding(0, 20, 0, 4);
        card.addView(logLabel);

        logView = new TextView(this);
        logView.setText("Ожидание клиентов...");
        logView.setTextColor(Color.parseColor("#E2E8F0"));
        logView.setTextSize(11f);
        logView.setBackgroundColor(Color.parseColor("#030712"));
        logView.setPadding(15, 15, 15, 15);
        card.addView(logView);

        layout.addView(card);
        setContentView(layout);

        startServer();
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private void startServer() {
        if (isRunning) return;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                String ip = getLocalIpAddress();
                handler.post(() -> {
                    statusView.setText("🌐 Откройте в браузере ПК:\nhttp://" + ip + ":" + port);
                    appendLog("Сервер запущен на http://" + ip + ":" + port);
                });

                File storageDir = getExternalFilesDir(null);
                if (storageDir != null && !storageDir.exists()) {
                    storageDir.mkdirs();
                }

                while (isRunning) {
                    try {
                        Socket client = serverSocket.accept();
                        handleClient(client, storageDir);
                    } catch (IOException e) {
                        if (!isRunning) break;
                    }
                }
            } catch (IOException e) {
                handler.post(() -> statusView.setText("Ошибка запуска: " + e.getMessage()));
            }
        }).start();
    }

    private void restartServer() {
        stopServer();
        startServer();
        Toast.makeText(this, "Сервер перезапущен", Toast.LENGTH_SHORT).show();
    }

    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    private void appendLog(String text) {
        handler.post(() -> {
            String current = logView.getText().toString();
            logView.setText(text + "\n" + current);
        });
    }

    private void handleClient(Socket client, File storageDir) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String requestLine = reader.readLine();
                if (requestLine == null) return;

                appendLog("Запрос: " + requestLine);
                OutputStream out = client.getOutputStream();

                if (requestLine.startsWith("GET /download?file=")) {
                    String fileName = requestLine.substring(requestLine.indexOf("file=") + 5);
                    fileName = fileName.substring(0, fileName.indexOf(" "));
                    fileName = URLDecoder.decode(fileName, "UTF-8");

                    File file = new File(storageDir, fileName);
                    if (file.exists() && file.isFile()) {
                        String header = "HTTP/1.1 200 OK\r\n" +
                                "Content-Disposition: attachment; filename=\"" + file.getName() + "\"\r\n" +
                                "Content-Length: " + file.length() + "\r\n" +
                                "Content-Type: application/octet-stream\r\n\r\n";
                        out.write(header.getBytes(StandardCharsets.UTF_8));

                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = fis.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                        }
                        fis.close();
                        appendLog("Скачан файл: " + file.getName());
                    } else {
                        String response = "HTTP/1.1 404 Not Found\r\n\r\nFile Not Found";
                        out.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                } else if (requestLine.startsWith("POST /upload")) {
                    int contentLength = 0;
                    String line;
                    while (!(line = reader.readLine()).isEmpty()) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.substring(15).trim());
                        }
                    }

                    InputStream rawIn = client.getInputStream();
                    ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int totalRead = 0;
                    int read;
                    while (totalRead < contentLength && (read = rawIn.read(buf, 0, Math.min(buf.length, contentLength - totalRead))) != -1) {
                        bodyStream.write(buf, 0, read);
                        totalRead += read;
                    }
                    byte[] bodyBytes = bodyStream.toByteArray();

                    File outFile = new File(storageDir, "file_" + System.currentTimeMillis());
                    FileOutputStream fos = new FileOutputStream(outFile);
                    fos.write(bodyBytes);
                    fos.close();

                    appendLog("Файл успешно загружен с ПК!");

                    String htmlResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" +
                            "<h2>Файл успешно загружен в память телефона! <a href='/'>Назад</a></h2>";
                    out.write(htmlResponse.getBytes(StandardCharsets.UTF_8));
                } else {
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Wi-Fi Файл-Сервер</title>")
                        .append("<style>body{font-family:sans-serif;background:#f1f5f9;padding:20px;color:#1e293b}")
                        .append("h1{color:#2563eb}.card{background:white;padding:20px;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1);margin-bottom:20px}")
                        .append("ul{list-style:none;padding:0}li{padding:8px 0;border-bottom:1px solid #e2e8f0}a{color:#2563eb;text-decoration:none}")
                        .append("input[type=file]{margin:10px 0}button{background:#2563eb;color:white;border:none;padding:10px 20px;border-radius:6px;cursor:pointer}")
                        .append("</style></head><body>");

                    html.append("<h1>📱 Wi-Fi Файл-Обменник (Телефон ⇄ ПК)</h1>");

                    html.append("<div class='card'><h3>📤 Загрузить файл с ПК на телефон</h3>")
                        .append("<form action='/upload' method='POST' enctype='multipart/form-data'>")
                        .append("<input type='file' name='file'/><br>")
                        .append("<button type='submit'>Загрузить</button></form></div>");

                    html.append("<div class='card'><h3>📥 Файлы в памяти телефона</h3><ul>");
                    File[] files = storageDir.listFiles();
                    if (files != null && files.length > 0) {
                        for (File f : files) {
                            html.append("<li>📄 <a href='/download?file=").append(URLEncoder.encode(f.getName(), "UTF-8"))
                                .append("'>").append(f.getName()).append("</a> (").append(f.length() / 1024).append(" КБ)</li>");
                        }
                    } else {
                        html.append("<li>Папка пока пуста</li>");
                    }
                    html.append("</ul></div></body></html>");

                    String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" + html.toString();
                    out.write(response.getBytes(StandardCharsets.UTF_8));
                }

                client.close();
            } catch (Exception e) {
                appendLog("Ошибка клиента: " + e.getMessage());
            }
        }).start();
    }
}