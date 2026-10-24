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
        authorView.setText("✦ Dziuba Roman — Canonical Frame Hunter ✦");
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
        titleView.setText("🛠️ Умный переход на канонический /frame/box");
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

        btnDebug = createStyledButton("🔍 Найти канонический фрейм и ссылки", "#2563EB");
        btnDebug.setOnClickListener(v -> runCanonicalScan());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Готов к умному поиску...");
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
        logLabel.setText("📋 Результаты анализа настоящего фрейма:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появятся данные из настоящего фрейма плеера...");
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
            ClipData clip = ClipData.newPlainText("Smule Canonical Debug", logEditText.getText().toString());
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

    private void runCanonicalScan() {
        String urlStr = urlInput.getText().toString().trim();
        if (urlStr.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDebug.setEnabled(false);
        statusText.setText("Статус: ⚙️ Получение канонического URL...");
        logEditText.setText("=== ПОИСК НАСТОЯЩЕГО ФРЕЙМА ===\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                // 1. Сначала качаем основную страницу
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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

                    String pageContent = html.toString().replace("\\/", "/");

                    // Вытягиваем метатег og:video
                    String realFrameUrl = null;
                    Matcher ogVideoMatcher = Pattern.compile("<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"").matcher(pageContent);
                    if (ogVideoMatcher.find()) {
                        realFrameUrl = ogVideoMatcher.group(1);
                    }

                    if (realFrameUrl == null) {
                        logs.append("Не удалось вытащить og:video из основной страницы.\n");
                        handler.post(() -> {
                            btnDebug.setEnabled(true);
                            logEditText.setText(logs.toString());
                        });
                        return;
                    }

                    logs.append("📌 Настоящий URL фрейма:\n").append(realFrameUrl).append("\n\n");

                    // 2. Делаем запрос к НАСТОЯЩЕМУ фрейму
                    HttpURLConnection frameConn = (HttpURLConnection) new URL(realFrameUrl).openConnection();
                    frameConn.setRequestMethod("GET");
                    setupBrowserHeaders(frameConn);

                    int frameCode = frameConn.getResponseCode();
                    logs.append("HTTP Код фрейма: ").append(frameCode).append("\n");

                    if (frameCode == 200) {
                        BufferedReader frameBr = new BufferedReader(new InputStreamReader(frameConn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder frameHtml = new StringBuilder();
                        while ((line = frameBr.readLine()) != null) {
                            frameHtml.append(line).append("\n");
                        }
                        frameBr.close();

                        String frameContent = frameHtml.toString().replace("\\/", "/");
                        logs.append("Размер фрейма: ").append(frameContent.length()).append(" байт\n\n");

                        // 3. Ищем ссылки на видео / медиа-файлы во фрейме
                        logs.append("--- Ищем ссылки на видео/аудио в коде фрейма ---\n");
                        Matcher streamMatcher = Pattern.compile("https?://[^\"]+?\\.(mp4|m4a|m3u8|mp3)(\\?[^\"]*)?").matcher(frameContent);
                        boolean foundStream = false;
                        while (streamMatcher.find()) {
                            foundStream = true;
                            logs.append("🎬 ").append(streamMatcher.group(0)).append("\n\n");
                        }
                        if (!foundStream) {
                            logs.append("Прямых медиа-ссылок с расширениями не найдено.\n\n");
                        }

                        // 4. Показываем первые 2000 символов кода фрейма (там весь плеер!)
                        logs.append("--- Первые 2000 символов кода фрейма ---\n");
                        logs.append(frameContent.length() > 2000 ? frameContent.substring(0, 2000) : frameContent);

                    } else {
                        logs.append("Ошибка получения фрейма. Код: ").append(frameCode);
                    }

                } else {
                    logs.append("Ошибка загрузки страницы. Код: ").append(conn.getResponseCode());
                }

            } catch (Exception e) {
                logs.append("❌ ОШИБКА: ").append(e.toString());
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Запрос фрейма завершен!");
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