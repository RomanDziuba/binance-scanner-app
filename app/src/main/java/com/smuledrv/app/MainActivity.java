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
        authorView.setText("✦ Dziuba Roman — JS Player Source Hunter ✦");
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
        titleView.setText("🛠️ Скачивание и анализ плеера Smule JS");
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

        btnDebug = createStyledButton("🔍 Скачать player_frame.js и искать декодер", "#2563EB");
        btnDebug.setOnClickListener(v -> downloadAndAnalyzePlayerJs());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Готов к загрузке JS плеера...");
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
        statusText.setTextSize(12f);
        statusText.setLayoutParams(statusParams);
        card.addView(statusText);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Результаты поиска в JS-коде плеера:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появятся фрагменты кода декодера из JS...");
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
            ClipData clip = ClipData.newPlainText("Smule JS Analysis", logEditText.getText().toString());
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

    private void downloadAndAnalyzePlayerJs() {
        btnDebug.setEnabled(false);
        statusText.setText("Статус: ⚙️ Скачивание JS файла плеера...");
        logEditText.setText("=== АНАЛИЗ player_frame.js ===\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                // Ссылка на найденный JS плеер
                String jsUrl = "https://w1-fa.cdn.smule.com/assets/community/player_frame-9b1955254fd1f0d9007c7112263408c9.js";
                logs.append("Запрос к: ").append(jsUrl).append("\n\n");

                HttpURLConnection conn = (HttpURLConnection) new URL(jsUrl).openConnection();
                conn.setRequestMethod("GET");
                setupBrowserHeaders(conn);

                int code = conn.getResponseCode();
                logs.append("HTTP Код JS файла: ").append(code).append("\n");

                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder jsContent = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        jsContent.append(line).append("\n");
                    }
                    br.close();

                    String jsText = jsContent.toString();
                    logs.append("Размер JS файла: ").append(jsText.length()).append(" символов\n\n");

                    // Ищем функции расшифровки или работы со строкой 'e:'
                    logs.append("--- Строки с функциями расшифровки / обработки URL ---\n");
                    String[] rows = jsText.split("[;\\n]");
                    int foundCount = 0;
                    for (String r : rows) {
                        if ((r.contains("atob") || r.contains("charCodeAt") || r.contains("media_url") || r.contains("decrypt") || r.contains(".substring(2)")) && r.length() < 300) {
                            logs.append("🔍 ").append(r.trim()).append("\n\n");
                            foundCount++;
                            if (foundCount >= 20) {
                                logs.append("... (показано первые 20 совпадений)\n");
                                break;
                            }
                        }
                    }

                    if (foundCount == 0) {
                        logs.append("Прямых совпадений по ключевым словам не найдено. Выводим фрагмент кода:\n");
                        logs.append(jsText.length() > 1000 ? jsText.substring(0, 1000) : jsText);
                    }

                } else {
                    logs.append("Ошибка скачивания JS. Код: ").append(code);
                }

            } catch (Exception e) {
                logs.append("❌ ОШИБКА: ").append(e.toString());
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Анализ JS завершен!");
                logEditText.setText(finalLogs);
            });
        }).start();
    }

    private void setupBrowserHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Referer", "https://www.smule.com/");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
    }
}