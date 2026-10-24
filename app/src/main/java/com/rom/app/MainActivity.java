package com.rom.app;

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
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnPasteUrl, btnCopyLogs;
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
        authorView.setText("✦ Dziuba Roman — Universal Web JS Hunter ✦");
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
        titleView.setText("🛠️ Универсальный анализ JS-скриптов сайта");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setHint("Введите или вставьте любую URL-ссылку...");
        urlInput.setHintTextColor(Color.parseColor("#475569"));
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 8);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        // КНОПКА «ВСТАВИТЬ ИЗ БУФЕРА»
        btnPasteUrl = createStyledButton("📋 Вставить из буфера", "#334155");
        btnPasteUrl.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPasteUrl);

        btnDebug = createStyledButton("🔍 Скачать и искать декодеры / JS", "#2563EB");
        btnDebug.setOnClickListener(v -> analyzeTargetUrl());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Введите ссылку для начала работы...");
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
        logLabel.setText("📋 Результаты анализа:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится лог скачивания и найденные алгоритмы...");
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
            ClipData clip = ClipData.newPlainText("Analysis Log", logEditText.getText().toString());
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
        params.setMargins(0, 4, 0, 4);
        btn.setLayoutParams(params);
        return btn;
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (!TextUtils.isEmpty(text)) {
                    urlInput.setText(text.toString().trim());
                    Toast.makeText(this, "Ссылка вставлена!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        Toast.makeText(this, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
    }

    private void analyzeTargetUrl() {
        String inputUrl = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(inputUrl)) {
            Toast.makeText(this, "Пожалуйста, введите или вставьте ссылку!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
            inputUrl = "https://" + inputUrl;
            urlInput.setText(inputUrl);
        }

        final String targetUrlStr = inputUrl;
        btnDebug.setEnabled(false);
        statusText.setText("Статус: ⚙️ Анализ URL...");
        logEditText.setText("=== УНИВЕРСАЛЬНЫЙ АНАЛИЗ ===\nЦель: " + targetUrlStr + "\n\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                // Если указан прямой файл JS
                if (targetUrlStr.endsWith(".js")) {
                    logs.append("Скачиваем прямой JS-файл...\n");
                    fetchAndAnalyzeJs(targetUrlStr, logs);
                } else {
                    // Иначе скачиваем HTML и ищем связанные JS скрипты
                    logs.append("Скачивание HTML-страницы...\n");
                    String htmlContent = fetchContent(targetUrlStr);
                    logs.append("Размер HTML: ").append(htmlContent.length()).append(" символов\n\n");

                    // 1. Ищем скрипты в самом HTML
                    searchPatterns(htmlContent, logs, "Inline HTML/JS");

                    // 2. Ищем подключенные внешние .js файлы в тегах <script src="...">
                    List<String> jsLinks = extractJsLinks(htmlContent, targetUrlStr);
                    logs.append("\nНайдено внешних JS-скриптов: ").append(jsLinks.size()).append("\n");

                    int downloadedCount = 0;
                    for (String jsUrl : jsLinks) {
                        if (downloadedCount >= 5) {
                            logs.append("⚠️ Пропущены остальные JS (лимит 5 файлов)\n");
                            break;
                        }
                        logs.append("\n--- Анализ файла: ").append(jsUrl).append(" ---\n");
                        try {
                            fetchAndAnalyzeJs(jsUrl, logs);
                            downloadedCount++;
                        } catch (Exception e) {
                            logs.append(" Ошибка скачивания скрипта: ").append(e.getMessage()).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                logs.append("\n❌ КРИТИЧЕСКАЯ ОШИБКА: ").append(e.toString());
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Анализ завершен!");
                logEditText.setText(finalLogs);
            });
        }).start();
    }

    private void fetchAndAnalyzeJs(String jsUrl, StringBuilder logs) throws Exception {
        String jsContent = fetchContent(jsUrl);
        logs.append("Размер JS: ").append(jsContent.length()).append(" символов\n");
        searchPatterns(jsContent, logs, "JS Content");
    }

    private String fetchContent(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        setupBrowserHeaders(conn);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new Exception("HTTP Код: " + code);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private void searchPatterns(String content, StringBuilder logs, String sourceName) {
        // Регулярное выражение для поиска расшифровки/декодирования или ключей
        Matcher matcher = Pattern.compile(".{0,100}(?:\"e:\"|'e:'|media_url|video_media_url|decodeUrl|decryptUrl|atob|btoa|AES|crypto|substring|charCodeAt).{0,100}")
                .matcher(content);
        
        boolean found = false;
        int count = 0;
        while (matcher.find()) {
            found = true;
            logs.append("🎯 [").append(sourceName).append("]: ").append(matcher.group(0).trim()).append("\n\n");
            count++;
            if (count >= 10) {
                logs.append("... (показано первые 10 совпадений из ").append(sourceName).append(")\n");
                break;
            }
        }
        if (!found) {
            logs.append("Результат: Совпадений алгоритмов в ").append(sourceName).append(" не найдено.\n");
        }
    }

    private List<String> extractJsLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        Matcher scriptMatcher = Pattern.compile("<script[^>]+src=[\"']([^\"']+\\.js[^\"']*)[\"']").matcher(html);
        while (scriptMatcher.find()) {
            String src = scriptMatcher.group(1);
            if (src.startsWith("//")) {
                src = "https:" + src;
            } else if (src.startsWith("/")) {
                try {
                    URL base = new URL(baseUrl);
                    src = base.getProtocol() + "://" + base.getHost() + src;
                } catch (Exception ignored) {}
            }
            if (src.startsWith("http")) {
                links.add(src);
            }
        }
        return links;
    }

    private void setupBrowserHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", "*/*");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
    }
}