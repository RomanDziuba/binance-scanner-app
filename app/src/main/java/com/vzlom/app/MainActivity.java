package com.vzlom.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnPasteUrl, btnCopyLogs;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Продвинутый OkHttpClient с имитацией реального браузера
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true) // Разрешаем автоматические редиректы под защитой браузерных заголовков
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build();

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
        authorView.setText("✦ Dziuba Roman — Smule Ultimate Extractor ✦");
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
        titleView.setText("🔥 Абсолютный поиск медиа Smule");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setHint("Введите или вставьте URL...");
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

        btnPasteUrl = createStyledButton("📋 Вставить из буфера", "#334155");
        btnPasteUrl.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPasteUrl);

        btnDebug = createStyledButton("🚀 Выкачать все ссылки и треки", "#2563EB");
        btnDebug.setOnClickListener(v -> startSmuleExtraction());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Готов к работе...");
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
        logLabel.setText("📋 Найденные ссылки и отчет:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появятся все прямые ссылки на аудио/видео (.m4a, .mp4)...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(14);
        logEditText.setMaxLines(22);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать всё", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Smule All Links", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Все ссылки скопированы в буфер!", Toast.LENGTH_SHORT).show();
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

    private void startSmuleExtraction() {
        String inputUrl = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(inputUrl)) {
            Toast.makeText(this, "Введите ссылку!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
            inputUrl = "https://" + inputUrl;
            urlInput.setText(inputUrl);
        }

        final String targetUrl = inputUrl;
        btnDebug.setEnabled(false);
        statusText.setText("Статус: 🔄 Обход защиты и загрузка страницы...");
        logEditText.setText("=== 🚀 SMULE ULTIMATE EXTRACTOR ===\nЦель: " + targetUrl + "\n\n");

        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("=== 🚀 SMULE ULTIMATE EXTRACTOR ===\n");
            report.append("Цель: ").append(targetUrl).append("\n\n");

            try {
                // Имитируем запросы полноценного десктопного Chrome для пробоя защиты 403
                Request request = new Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                        .header("Sec-Ch-Ua-Mobile", "?0")
                        .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .header("Upgrade-Insecure-Requests", "1")
                        .build();

                Response response = httpClient.newCall(request).execute();
                int code = response.code();
                String finalUrl = response.request().url().toString();

                report.append("• Финальный URL: ").append(finalUrl).append("\n");
                report.append("• Код ответа сервера: ").append(code).append("\n");

                if (!response.isSuccessful() || response.body() == null) {
                    report.append("❌ Ошибка: Сервер вернул код ").append(code).append("\n");
                    response.close();
                    finishExtraction(report.toString());
                    return;
                }

                String html = response.body().string();
                response.close();

                report.append("• Размер полученного HTML: ").append(html.length()).append(" символов\n\n");

                // Извлекаем заголовок
                Matcher titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html);
                if (titleMatcher.find()) {
                    report.append("🎵 Название трека / Страницы:\n👉 ").append(titleMatcher.group(1).trim()).append("\n\n");
                }

                report.append("--- ПОИСК ПРЯМЫХ МЕДИА-ССЫЛОК (.mp4 / .m4a) ---\n");
                Set<String> foundLinks = new HashSet<>();

                // Шаблон 1: OpenGraph мета-теги
                Matcher ogVideo = Pattern.compile("<meta\\s+property=[\"']og:video(?::secure_url)?[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
                while (ogVideo.find()) foundLinks.add(ogVideo.group(1));

                Matcher ogAudio = Pattern.compile("<meta\\s+property=[\"']og:audio(?::secure_url)?[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
                while (ogAudio.find()) foundLinks.add(ogAudio.group(1));

                // Шаблон 2: Поиск всех URL, оканчивающихся или содержащих расширения media в HTML
                Matcher urlPattern = Pattern.compile("https?://[^\\s\"']+\\.(?:mp4|m4a|mp3|webm)(?:\\?[^\\s\"']*)?", Pattern.CASE_INSENSITIVE).matcher(html);
                while (urlPattern.find()) {
                    foundLinks.add(urlPattern.group(0));
                }

                // Шаблон 3: Поиск JSON-ключей с media_url / performance в коде страницы
                Matcher jsonKeyPattern = Pattern.compile("[\"'](?:media_url|audio_url|video_url|secure_url|url)[\"']\\s*:\\s*[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
                while (jsonKeyPattern.find()) {
                    foundLinks.add(jsonKeyPattern.group(1));
                }

                int index = 0;
                if (!foundLinks.isEmpty()) {
                    for (String link : foundLinks) {
                        index++;
                        // Декодируем слэши если они экранированы в JSON (\/)
                        String cleanLink = link.replace("\\/", "/");
                        report.append("\n🎯 Ссылка №").append(index).append(":\n").append(cleanLink).append("\n");
                    }
                } else {
                    report.append("ℹ️ В HTML прямой ссылки не оказалось. Сканируем JS-скрипты страницы...\n");
                }

                // Шаблон 4: Сканируем скрипты JS, подключенные к странице
                List<String> jsLinks = extractJsLinks(html, finalUrl);
                report.append("\n--- СКАНИРОВАНИЕ JS СКРИПТОВ (").append(jsLinks.size()).append(" шт.) ---\n");

                int jsChecked = 0;
                for (String jsUrl : jsLinks) {
                    if (jsChecked >= 5) break; // Ограничим первыми 5 скриптами для скорости
                    jsChecked++;
                    
                    try {
                        Request jsReq = new Request.Builder()
                                .url(jsUrl)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                                .header("Referer", finalUrl)
                                .header("Origin", "https://www.smule.com")
                                .build();
                        Response jsResp = httpClient.newCall(jsReq).execute();
                        if (jsResp.isSuccessful() && jsResp.body() != null) {
                            String jsContent = jsResp.body().string();
                            jsResp.close();
                            
                            Matcher jsMediaMatcher = Pattern.compile("https?://[^\\s\"'\\\\]+\\.(?:mp4|m4a|mp3)", Pattern.CASE_INSENSITIVE).matcher(jsContent);
                            while (jsMediaMatcher.find()) {
                                String found = jsMediaMatcher.group(0).replace("\\/", "/");
                                if (found.contains("smule") || found.contains("cdn")) {
                                    foundLinks.add(found);
                                    report.append("🔥 Найдено в JS: ").append(found).append("\n");
                                }
                            }
                        } else {
                            if (jsResp != null) jsResp.close();
                        }
                    } catch (Exception ignored) {}
                }

                if (foundLinks.size() > index) {
                    report.append("\n✅ Дополнительные ссылки из скриптов успешно добавлены в список выше!");
                }

                report.append("\n\n🎉 Анализ завершен! Скопируйте нужную ссылку и скачайте её в браузере или через любой загрузчик.");

            } catch (Exception e) {
                report.append("\n❌ ОШИБКА ОБХОДА: ").append(e.toString());
            }

            finishExtraction(report.toString());
        }).start();
    }

    private void finishExtraction(String report) {
        handler.post(() -> {
            btnDebug.setEnabled(true);
            statusText.setText("Статус: ✅ Поиск завершен!");
            logEditText.setText(report);
        });
    }

    private List<String> extractJsLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        Matcher scriptMatcher = Pattern.compile("<script[^>]+src=[\"']([^\"']+\\.js[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
        while (scriptMatcher.find()) {
            String src = scriptMatcher.group(1);
            if (src.startsWith("//")) {
                src = "https:" + src;
            } else if (src.startsWith("/")) {
                try {
                    okhttp3.HttpUrl base = okhttp3.HttpUrl.parse(baseUrl);
                    if (base != null) {
                        src = base.scheme() + "://" + base.host() + src;
                    }
                } catch (Exception ignored) {}
            }
            if (src.startsWith("http")) {
                links.add(src);
            }
        }
        return links;
    }
}