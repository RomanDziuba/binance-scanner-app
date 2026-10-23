package com.peredachanakomp.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Collections;

public class MainActivity extends Activity {

    private TextView statusView, linkView, infoView;
    private Button btnToggleServer, btnPermissions;
    private boolean isServerRunning = false;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private static final int PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(30, 30, 30, 30);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView title = new TextView(this);
        title.setText("📡 Wi-Fi Сервер (Память + SD-карты)");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 20);
        root.addView(title);

        infoView = new TextView(this);
        infoView.setText("Инструкция:\n1. Включите точку доступа Wi-Fi на телефоне.\n2. Подключите к ней компьютер.\n3. Запустите сервер и откройте ссылку в браузере ПК.");
        infoView.setTextColor(Color.parseColor("#94A3B8"));
        infoView.setTextSize(13f);
        infoView.setPadding(20, 20, 20, 20);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#0F172A"));
        infoBg.setCornerRadius(16f);
        infoBg.setStroke(2, Color.parseColor("#1E293B"));
        infoView.setBackground(infoBg);
        root.addView(infoView);

        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
        root.addView(space);

        btnToggleServer = new Button(this);
        btnToggleServer.setText("🚀 Запустить сервер");
        btnToggleServer.setTextColor(Color.WHITE);
        btnToggleServer.setTextSize(14f);
        btnToggleServer.setTypeface(Typeface.DEFAULT_BOLD);
        btnToggleServer.setBackgroundColor(Color.parseColor("#2563EB"));
        btnToggleServer.setPadding(20, 25, 20, 25);
        btnToggleServer.setOnClickListener(v -> {
            if (!isServerRunning) {
                startFileServer();
            } else {
                stopFileServer();
            }
        });
        root.addView(btnToggleServer);

        View space2 = new View(this);
        space2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 15));
        root.addView(space2);

        linkView = new TextView(this);
        linkView.setText("Ссылка для ПК: не активна");
        linkView.setTextColor(Color.parseColor("#10B981"));
        linkView.setTextSize(14f);
        linkView.setTypeface(Typeface.MONOSPACE);
        linkView.setGravity(Gravity.CENTER);
        linkView.setPadding(10, 15, 10, 15);
        root.addView(linkView);

        statusView = new TextView(this);
        statusView.setText("Статус: Сервер остановлен");
        statusView.setTextColor(Color.parseColor("#64748B"));
        statusView.setTextSize(13f);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 10, 0, 10);
        root.addView(statusView);

        btnPermissions = new Button(this);
        btnPermissions.setText("⚙️ Разрешить доступ ко ВСЕМ файлам");
        btnPermissions.setTextColor(Color.WHITE);
        btnPermissions.setTextSize(12f);
        btnPermissions.setBackgroundColor(Color.parseColor("#D97706"));
        btnPermissions.setPadding(15, 15, 15, 15);
        btnPermissions.setOnClickListener(v -> requestAllFilesPermission());
        root.addView(btnPermissions);

        setContentView(root);
        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                btnPermissions.setVisibility(View.VISIBLE);
            } else {
                btnPermissions.setVisibility(View.GONE);
            }
        }
    }

    private void requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception ignored) {
                    Toast.makeText(this, "Откройте Настройки -> Приложения -> Доступ к файлам", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void startFileServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            isServerRunning = true;
            btnToggleServer.setText("🛑 Остановить сервер");
            btnToggleServer.setBackgroundColor(Color.parseColor("#DC2626"));
            
            String ip = getLocalIpAddress();
            String url = "http://" + ip + ":" + PORT;
            linkView.setText(url);
            statusView.setText("Статус: Сервер работает! Откройте ссылку на ПК");

            serverThread = new Thread(() -> {
                while (isServerRunning) {
                    try {
                        Socket client = serverSocket.accept();
                        new Thread(() -> handleClient(client)).start();
                    } catch (Exception e) {
                        break;
                    }
                }
            });
            serverThread.start();

        } catch (Exception e) {
            Toast.makeText(this, "Не удалось запустить сервер: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopFileServer() {
        isServerRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}

        btnToggleServer.setText("🚀 Запустить сервер");
        btnToggleServer.setBackgroundColor(Color.parseColor("#2563EB"));
        linkView.setText("Ссылка для ПК: не активна");
        statusView.setText("Статус: Сервер остановлен");
    }

    // ТОЧНЫЙ МЕТОД ПОИСКА IP ИЗ САМОГО ПЕРВОГО РАБОЧЕГО СКРИПТА
    private String getLocalIpAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.43.1";
    }

    private void handleClient(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String requestLine = reader.readLine();
            if (requestLine == null) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String rawUri = URLDecoder.decode(parts[1], "UTF-8");

            OutputStream out = client.getOutputStream();

            // Главная страница (Меню выбора: Внутренняя память или Все диски)
            if (rawUri.equals("/") || rawUri.equals("/index.html")) {
                StringBuilder html = new StringBuilder();
                html.append("<html><head><meta charset='utf-8'><title>Phone File Explorer</title>");
                html.append("<style>body{font-family:sans-serif;background:#030712;color:#E2E8F0;padding:30px;}");
                html.append("h2{color:#38BDF8;border-bottom:2px solid #1E293B;padding-bottom:10px;}");
                html.append(".card{background:#0F172A;border:1px solid #1E293B;padding:20px;margin:15px 0;border-radius:12px;font-size:18px;}");
                html.append("a{color:#38BDF8;text-decoration:none;display:block;}");
                html.append("a:hover{color:#10B981;}</style></head><body>");
                html.append("<h2>📂 Выберите хранилище для просмотра:</h2>");
                html.append("<div class='card'><a href='/internal/'>📁 Внутренняя память телефона (Основная)</a></div>");
                html.append("<div class='card'><a href='/storage/'>📁 Все диски и SD-карты (/storage/)</a></div>");
                html.append("</body></html>");

                String httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" + html.toString();
                out.write(httpResponse.getBytes("UTF-8"));
                out.flush();
                client.close();
                return;
            }

            File rootDir;
            String subPath = "";

            if (rawUri.startsWith("/internal")) {
                rootDir = Environment.getExternalStorageDirectory();
                subPath = rawUri.substring("/internal".length());
            } else if (rawUri.startsWith("/storage")) {
                rootDir = new File("/storage/");
                subPath = rawUri.substring("/storage".length());
            } else {
                String response = "HTTP/1.1 404 Not Found\r\n\r\nNot Found";
                out.write(response.getBytes());
                out.flush();
                client.close();
                return;
            }

            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }

            File targetFile = new File(rootDir, subPath);

            if (!targetFile.exists()) {
                String response = "HTTP/1.1 404 Not Found\r\n\r\nFile Not Found: " + targetFile.getAbsolutePath();
                out.write(response.getBytes());
                out.flush();
                client.close();
                return;
            }

            if (targetFile.isDirectory()) {
                StringBuilder html = new StringBuilder();
                html.append("<html><head><meta charset='utf-8'><title>Storage Explorer</title>");
                html.append("<style>body{font-family:sans-serif;background:#030712;color:#E2E8F0;padding:20px;}");
                html.append("a{color:#38BDF8;text-decoration:none;display:block;padding:8px 0;border-bottom:1px solid #1E293B;}");
                html.append("a:hover{color:#10B981;}</style></head><body>");
                html.append("<h2>📂 Путь: ").append(targetFile.getAbsolutePath()).append("</h2>");

                String prefix = rawUri.startsWith("/internal") ? "/internal" : "/storage";

                // Кнопка наверх или в главное меню
                if (targetFile.equals(rootDir)) {
                    html.append("<a href='/'>🏠 [Вернуться в главное меню]</a>");
                } else {
                    File parent = targetFile.getParentFile();
                    if (parent != null && parent.exists() && parent.getAbsolutePath().startsWith(rootDir.getAbsolutePath())) {
                        String parentRel = parent.getAbsolutePath().replace(rootDir.getAbsolutePath(), "");
                        if (!parentRel.startsWith("/")) parentRel = "/" + parentRel;
                        html.append("<a href='").append(prefix).append(parentRel).append("'>⬅️ [Наверх]</a>");
                    } else {
                        html.append("<a href='/'>🏠 [В главное меню]</a>");
                    }
                }

                File[] files = targetFile.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String fRel = f.getAbsolutePath().replace(rootDir.getAbsolutePath(), "");
                        if (!fRel.startsWith("/")) fRel = "/" + fRel;

                        if (f.isDirectory()) {
                            html.append("<a href='").append(prefix).append(fRel).append("'>📁 <b>").append(f.getName()).append("</b></a>");
                        } else {
                            html.append("<a href='").append(prefix).append(fRel).append("'>📄 ").append(f.getName()).append(" (").append(f.length() / 1024).append(" KB)</a>");
                        }
                    }
                } else {
                    html.append("<p style='color:#EF4444;'>⚠️ Не удалось прочитать папку (проверьте разрешения доступа)</p>");
                }
                html.append("</body></html>");

                String httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" + html.toString();
                out.write(httpResponse.getBytes("UTF-8"));
            } else {
                String mime = "application/octet-stream";
                String header = "HTTP/1.1 200 OK\r\nContent-Type: " + mime + "\r\nContent-Length: " + targetFile.length() + "\r\nContent-Disposition: attachment; filename=\"" + targetFile.getName() + "\"\r\n\r\n";
                out.write(header.getBytes());

                FileInputStream fis = new FileInputStream(targetFile);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    out.write(buffer, 0, count);
                }
                fis.close();
            }
            out.flush();
            client.close();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFileServer();
    }
}