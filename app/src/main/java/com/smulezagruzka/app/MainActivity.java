package com.smulezagruzka.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnCopyLogs;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — Smule Debug Mode ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 15);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(25, 25, 25, 25);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(28f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(this);
        titleView.setText("🛠️ Отладочный режим Smule Parser");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setText("https://www.smule.com/c/2930392803_5270362763");
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 10);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        btnDebug = createStyledButton("🔍 Запустить диагностику и сбор данных", "#2563EB");
        btnDebug.setOnClickListener(v -> runDiagnostic());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Ожидание запуска диагностики...");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(12f);
        statusText.setPadding(12, 10, 12, 10);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#1E293B"));
        statusBg.setCornerRadius(10f);
        statusText.setBackground(statusBg);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, 10, 0, 10);
        statusText.setLayoutParams(statusParams);
        card.addView(statusText);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Логи и сырые данные (для копирования):");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появятся результаты запроса, заголовки и HTML/JSON...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(10);
        logEditText.setMaxLines(16);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Smule Debug Log", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Лог скопирован в буфер обмена!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        container.addView(card);
        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 5, 0, 5);
        btn.setLayoutParams(params);
        return btn;
    }

    private void runDiagnostic() {
        String urlStr = urlInput.getText().toString().trim();
        if (urlStr.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDebug.setEnabled(false);
        statusText.setText("Статус: ⚙️ Выполнение запроса...");
        logEditText.setText("=== НАЧАЛО ДИАГНОСТИКИ ===\nURL: " + urlStr + "\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                // 1. Проверяем редиректы и заголовки
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(false);
                setupBrowserHeaders(conn);

                int responseCode = conn.getResponseCode();
                logs.append("1. Ответ сервера: ").append(responseCode).append("\n");

                Map<String, List<String>> headers = conn.getHeaderFields();
                logs.append("--- Заголовки ответа ---\n");
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    logs.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }

                String finalUrl = urlStr;
                if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                    finalUrl = conn.getHeaderField("Location");
                    logs.append("\nРедирект на Location: ").append(finalUrl).append("\n");
                }

                // 2. Делаем запрос на итоговый URL
                logs.append("\n2. Запрос к итоговой странице: ").append(finalUrl).append("\n");
                HttpURLConnection connFinal = (HttpURLConnection) new URL(finalUrl).openConnection();
                connFinal.setRequestMethod("GET");
                setupBrowserHeaders(connFinal);

                int codeFinal = connFinal.getResponseCode();
                logs.append("Код ответа страницы: ").append(codeFinal).append("\n");

                if (codeFinal == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connFinal.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder html = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        html.append(line).append("\n");
                    }
                    br.close();

                    String pageContent = html.toString();
                    logs.append("Длина HTML страницы: ").append(pageContent.length()).append(" символов\n\n");

                    logs.append("--- Первые 1500 символов HTML ---\n");
                    logs.append(pageContent.length() > 1500 ? pageContent.substring(0, 1500) : pageContent);
                    logs.append("\n...\n");
                }

                // 3. Пробуем дернуть API по ключу из ссылки
                String recordingKey = extractRecordingKey(urlStr);
                if (recordingKey != null) {
                    logs.append("\n3. Извлеченный Recording Key: ").append(recordingKey).append("\n");
                    String apiUrl = "https://www.smule.com/api/show/recording/" + recordingKey;
                    logs.append("Запрос к API: ").append(apiUrl).append("\n");

                    HttpURLConnection apiConn = (HttpURLConnection) new URL(apiUrl).openConnection();
                    apiConn.setRequestMethod("GET");
                    setupBrowserHeaders(apiConn);

                    int apiCode = apiConn.getResponseCode();
                    logs.append("API Код ответа: ").append(apiCode).append("\n");
                    if (apiCode == 200) {
                        BufferedReader apiBr = new BufferedReader(new InputStreamReader(apiConn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder apiSb = new StringBuilder();
                        String line;
                        while ((line = apiBr.readLine()) != null) {
                            apiSb.append(line).append("\n");
                        }
                        apiBr.close();
                        logs.append("--- Ответ API (JSON) ---\n");
                        logs.append(apiSb.toString());
                    }
                } else {
                    logs.append("\n3. Не удалось извлечь Recording Key из URL.\n");
                }

            } catch (Exception e) {
                logs.append("\n❌ ОШИБКА ДИАГНОСТИКИ: ").append(e.toString()).append("\n");
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Диагностика завершена. Скопируйте лог!");
                logEditText.setText(finalLogs);
            });
        }).start();
    }

    private void setupBrowserHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
    }

    private String extractRecordingKey(String url) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:recording/|c/|p/)([a-zA-Z0-9_-]+)").matcher(url);
        if (matcher.find()) {
            String fullKey = matcher.group(1);
            if (fullKey.contains("_")) {
                String[] parts = fullKey.split("_");
                return parts[parts.length - 1];
            }
            return fullKey;
        }
        return null;
    }
}