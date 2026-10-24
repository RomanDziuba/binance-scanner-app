package com.smuledrv.app;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        authorView.setText("✦ Dziuba Roman — CDN Link Builder Test ✦");
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
        titleView.setText("🛠️ Тест сборки прямой CDN-ссылки");
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

        btnDebug = createStyledButton("🔍 Собрать и проверить CDN ссылку", "#2563EB");
        btnDebug.setOnClickListener(v -> runCdnTest());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Ожидание теста...");
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
        logLabel.setText("📋 Результат проверки CDN:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится результат проверки собранной ссылки...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(12);
        logEditText.setMaxLines(18);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Smule CDN Test", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Отчет скопирован!", Toast.LENGTH_SHORT).show();
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

    private void runCdnTest() {
        String urlStr = urlInput.getText().toString().trim();
        if (urlStr.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDebug.setEnabled(false);
        statusText.setText("Статус: ⚙️ Парсинг страницы и сборка CDN-ссылки...");
        logEditText.setText("=== ТЕСТ СБОРКИ CDN ССЫЛКИ ===\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                setupBrowserHeaders(conn);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder html = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        html.append(line).append("\n");
                    }
                    br.close();

                    String pageContent = html.toString().replace("\\/", "/");

                    // Ищем cover_url в тексте страницы
                    Matcher matcher = Pattern.compile("\"cover_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageContent);
                    if (matcher.find()) {
                        String coverUrl = matcher.group(1);
                        logs.append("Найден cover_url:\n").append(coverUrl).append("\n\n");

                        // Собираем видео-ссылку (меняем cover -> video, .jpg -> .mp4)
                        String videoUrl = coverUrl.replace("/cover/", "/video/").replace(".jpg", ".mp4");
                        logs.append("🔗 Сконструированная Video URL:\n").append(videoUrl).append("\n\n");

                        // Проверяем доступность видео-ссылки HEAD-запросом
                        logs.append("Проверка доступности сервера (HEAD запрос)...\n");
                        HttpURLConnection testConn = (HttpURLConnection) new URL(videoUrl).openConnection();
                        testConn.setRequestMethod("HEAD");
                        setupBrowserHeaders(testConn);
                        int testCode = testConn.getResponseCode();
                        logs.append("📡 HTTP Код видео-файла: ").append(testCode).append("\n");
                        
                        if (testCode == 200 || testCode == 206) {
                            logs.append("\n✅ УСПЕХ! Ссылка рабочая, файл доступен для скачивания!");
                        } else {
                            // Попробуем альтернативный вариант пути (без слова video, а прямо в performance)
                            String altUrl = coverUrl.replace("/cover/", "/").replace(".jpg", ".mp4");
                            logs.append("\nПроверяем альтернативный путь:\n").append(altUrl).append("\n");
                            HttpURLConnection altConn = (HttpURLConnection) new URL(altUrl).openConnection();
                            altConn.setRequestMethod("HEAD");
                            setupBrowserHeaders(altConn);
                            int altCode = altConn.getResponseCode();
                            logs.append("📡 HTTP Код альт. файла: ").append(altCode).append("\n");
                            if (altCode == 200 || altCode == 206) {
                                logs.append("\n✅ УСПЕХ по альтернативному пути!");
                            }
                        }

                    } else {
                        logs.append("Не удалось найти cover_url в коде страницы.");
                    }

                } else {
                    logs.append("Ошибка HTTP страницы: ").append(responseCode);
                }

            } catch (Exception e) {
                logs.append("❌ ОШИБКА: ").append(e.toString());
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Тест CDN завершен!");
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
}