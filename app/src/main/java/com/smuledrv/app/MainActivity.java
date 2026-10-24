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
        authorView.setText("✦ Dziuba Roman — Frame/Box Hunter ✦");
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
        titleView.setText("🛠️ Анализ страницы /frame/box");
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

        btnDebug = createStyledButton("🔍 Запросить /frame/box и искать видео", "#2563EB");
        btnDebug.setOnClickListener(v -> runFrameBoxScan());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Ожидание сканирования фрейма...");
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
        logLabel.setText("📋 Результаты анализа /frame/box:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появятся данные из фрейма плеера...");
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
            ClipData clip = ClipData.newPlainText("Smule FrameBox Debug", logEditText.getText().toString());
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

    private void runFrameBoxScan() {
        String urlStr = urlInput.getText().toString().trim();
        if (urlStr.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDebug.setEnabled(false);
        statusText.setText("Статус: ⚙️ Получение страницы и переход на /frame/box...");
        logEditText.setText("=== АНАЛИЗ /frame/box ===\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                // 1. Сначала загружаем основную страницу, чтобы узнать точный URL записи или перенаправление
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                setupBrowserHeaders(conn);

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder html = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        html.append(line).append("\n");
                    }
                    br.close();

                    String pageContent = html.toString();

                    // Ищем каноническую ссылку или собираем /frame/box из og:video
                    String frameBoxUrl = null;
                    Matcher ogVideoMatcher = Pattern.compile("<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"").matcher(pageContent);
                    if (ogVideoMatcher.find()) {
                        frameBoxUrl = ogVideoMatcher.group(1);
                    }

                    if (frameBoxUrl == null) {
                        frameBoxUrl = urlStr + "/frame/box";
                    }

                    logs.append("Целевой URL фрейма:\n").append(frameBoxUrl).append("\n\n");

                    // 2. Делаем запрос на страницу /frame/box
                    HttpURLConnection frameConn = (HttpURLConnection) new URL(frameBoxUrl).openConnection();
                    frameConn.setRequestMethod("GET");
                    setupBrowserHeaders(frameConn);

                    int frameCode = frameConn.getResponseCode();
                    logs.append("HTTP Код фрейма: ").append(frameCode).append("\n\n");

                    if (frameCode == 200) {
                        BufferedReader frameBr = new BufferedReader(new InputStreamReader(frameConn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder frameHtml = new StringBuilder();
                        while ((line = frameBr.readLine()) != null) {
                            frameHtml.append(line).append("\n");
                        }
                        frameBr.close();

                        String frameContent = frameHtml.toString().replace("\\/", "/");
                        logs.append("Размер фрейма: ").append(frameContent.length()).append(" байт\n\n");

                        // Ищем теги video/source во фрейме
                        logs.append("--- Теги video / source во фрейме ---\n");
                        Matcher vMatcher = Pattern.compile("<(video|source)[^>]*>", Pattern.CASE_INSENSITIVE).matcher(frameContent);
                        boolean found = false;
                        while (vMatcher.find()) {
                            found = true;
                            logs.append("🎥 ").append(vMatcher.group(0)).append("\n\n");
                        }
                        if (!found) {
                            logs.append("Теги video/source не найдены во фрейме.\n\n");
                        }

                        // Ищем любые прямые ссылки на .mp4 или потоки внутри фрейма
                        logs.append("--- Прямые ссылки во фрейме ---\n");
                        Matcher linkMatcher = Pattern.compile("https?://[^\"]+?\\.mp4[^\"]*").matcher(frameContent);
                        boolean foundLink = false;
                        while (linkMatcher.find()) {
                            foundLink = true;
                            logs.append("🔗 ").append(linkMatcher.group(0)).append("\n\n");
                        }
                        if (!foundLink) {
                            logs.append("Прямых ссылок на .mp4 во фрейме не найдено.\n");
                        }

                    } else {
                        logs.append("Не удалось загрузить фрейм. Код: ").append(frameCode);
                    }

                } else {
                    logs.append("Ошибка HTTP основной страницы: ").append(conn.getResponseCode());
                }

            } catch (Exception e) {
                logs.append("❌ ОШИБКА: ").append(e.toString());
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Анализ фрейма завершен!");
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