package com.app.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText, songInfoText;
    private Button mainActionButton, btnClear, btnPaste, btnToggleLogs;
    private LinearLayout infoCard, downloadButtonsLayout;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static String cachedRc4Key = null;

    private enum State { IDLE, ANALYZING, READY_TO_DOWNLOAD, DOWNLOADED }
    private State currentState = State.IDLE;
    
    private final Set<String> interceptedMediaUrls = new HashSet<>();
    private final StringBuilder debugLogBuilder = new StringBuilder();
    
    private String mediaExtension = "mp4";
    private String mediaTitle = "Media_File";
    private String mediaArtist = "Universal_Extractor";

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

        // Авторский заголовок
        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — Universal Deep Media Extractor ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(13f);
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
        titleView.setText("🔥 Глубокий парсер (Smule, YouTube, Web)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(15f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        // Кнопка вставки из буфера
        btnPaste = createPillButton("📋 ВСТАВИТЬ ИЗ БУФЕРА", "#334155", 12f);
        btnPaste.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPaste);

        // Поле ввода ссылки
        urlInput = new EditText(this);
        urlInput.setHint("Вставьте ссылку на трек или сайт...");
        urlInput.setHintTextColor(Color.parseColor("#475569"));
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(18, 18, 18, 18);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 8, 0, 12);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(15, 15, 15, 15);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#111827"));
        infoBg.setCornerRadius(16f);
        infoCard.setBackground(infoBg);
        infoCard.setVisibility(LinearLayout.GONE);

        songInfoText = new TextView(this);
        songInfoText.setTextColor(Color.parseColor("#34D399"));
        songInfoText.setTextSize(12f);
        songInfoText.setTypeface(Typeface.DEFAULT_BOLD);
        infoCard.addView(songInfoText);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 10);
        infoCard.setLayoutParams(infoParams);
        card.addView(infoCard);

        // Главная кнопка действия
        mainActionButton = createPillButton("🚀 ЗАПУСТИТЬ ГЛУБОКИЙ АНАЛИЗ", "#2563EB", 13f);
        mainActionButton.setOnClickListener(v -> startDeepExtraction());
        card.addView(mainActionButton);

        // Кнопка сброса
        btnClear = createPillButton("❌ СБРОСИТЬ / ОЧИСТИТЬ", "#7F1D1D", 12f);
        btnClear.setOnClickListener(v -> resetState());
        card.addView(btnClear);

        statusText = new TextView(this);
        statusText.setText("СТАТУС: ГОТОВ К РАБОТЕ...");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(11f);
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

        // Контейнер для динамических кнопок скачивания всех найденных медиафайлов
        TextView dlLabel = new TextView(this);
        dlLabel.setText("📥 Найденные медиапотоки и файлы:");
        dlLabel.setTextColor(Color.parseColor("#38BDF8"));
        dlLabel.setTextSize(12f);
        dlLabel.setTypeface(Typeface.DEFAULT_BOLD);
        dlLabel.setPadding(0, 10, 0, 5);
        card.addView(dlLabel);

        downloadButtonsLayout = new LinearLayout(this);
        downloadButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        card.addView(downloadButtonsLayout);

        // Лог отладки (скрытый по умолчанию, с возможностью просмотра)
        btnToggleLogs = createPillButton("📋 ПОКАЗАТЬ / СКРЫТЬ ЛОГИ", "#475569", 11f);
        btnToggleLogs.setOnClickListener(v -> {
            if (logEditText.getVisibility() == View.VISIBLE) {
                logEditText.setVisibility(View.GONE);
            } else {
                logEditText.setVisibility(View.VISIBLE);
            }
        });
        card.addView(btnToggleLogs);

        logEditText = new EditText(this);
        logEditText.setHint("Технические логи отладки...");
        logEditText.setTextSize(9f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setVisibility(View.GONE);
        card.addView(logEditText);

        // Скрытый мощный WebView для сниффинга сетевых запросов и выполнения JS
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        
        container.addView(webView);
        container.addView(card);
        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private Button createPillButton(String text, String colorHex, float textSizeSp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(textSizeSp);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable pillShape = new GradientDrawable();
        pillShape.setColor(Color.parseColor(colorHex));
        pillShape.setCornerRadius(100f);
        btn.setBackground(pillShape);
        
        btn.setPadding(15, 18, 15, 18);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 6, 0, 6);
        btn.setLayoutParams(params);
        return btn;
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (!TextUtils.isEmpty(text)) {
                        urlInput.setText(text.toString().trim());
                        Toast.makeText(this, "Ссылка успешно вставлена!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            Toast.makeText(this, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка вставки из буфера", Toast.LENGTH_SHORT).show();
        }
    }

    private void startDeepExtraction() {
        String inputUrl = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(inputUrl)) {
            Toast.makeText(this, "Введите или вставьте ссылку!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
            inputUrl = "https://" + inputUrl;
            urlInput.setText(inputUrl);
        }

        final String targetUrl = inputUrl;
        interceptedMediaUrls.clear();
        downloadButtonsLayout.removeAllViews();
        debugLogBuilder.setLength(0);

        currentState = State.ANALYZING;
        mainActionButton.setEnabled(false);
        GradientDrawable bg = (GradientDrawable) mainActionButton.getBackground();
        bg.setColor(Color.parseColor("#475569"));
        mainActionButton.setText("⏳ ИДЕТ ГЛУБОКИЙ АНАЛИЗ...");
        statusText.setText("СТАТУС: Сканирование исходного кода и перехват трафика...");

        appendLog("=== СТАРТ АНАЛИЗА: " + targetUrl + " ===");

        // Шаг 1: Поток для глубокого парсинга (Smule, OpenGraph, JSON-структуры)
        new Thread(() -> {
            try {
                String pageHtml = downloadString(targetUrl);
                if (pageHtml != null) {
                    appendLog("✅ HTML-код страницы успешно загружен (" + pageHtml.length() + " байт).");

                    // Проверяем Smule-специфичные паттерны и дешифруем через RC4 если есть
                    extractSmuleMedia(pageHtml);

                    // Проверяем универсальные теги (OpenGraph видео/аудио, прямые mp4/m3u8/mp3 в тексте)
                    extractGenericMedia(pageHtml);
                }
            } catch (Exception e) {
                appendLog("⚠️ Ошибка фонового парсинга: " + e.getMessage());
            }
        }).start();

        // Шаг 2: Параллельно запускаем WebView для сниффинга динамических сетевых запросов (видео, аудио, стримы)
        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                
                // Перехватываем любые медиаформаты и потоки (включая m3u8, ts, mp4, m4a, mp3, webm, ogg)
                if (reqUrl.matches(".*\\.(mp4|m4a|mp3|wav|aac|ogg|flac|m3u8|ts|webm|mov)(\\?.*)?$") || reqUrl.contains("videoplayback")) {
                    synchronized (interceptedMediaUrls) {
                        interceptedMediaUrls.add(reqUrl);
                        appendLog("🌐 [Sniffer] Перехвачен медиапоток: " + reqUrl);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("🌐 Страница отрендерена в WebView. Сбор динамических элементов...");

                // Автоматически эмулируем клик по плеерам/кнопкам воспроизведения для инициации загрузки потоков
                webView.loadUrl("javascript:(function(){" +
                        "var playElements = document.querySelectorAll('video, audio, [class*=\"play\"], [class*=\"Play\"], button');" +
                        "for(var i=0; i<playElements.length; i++) { try { playElements[i].play(); playElements[i].click(); } catch(e){} }" +
                        "})();");

                // Даем время на сбор всех асинхронных запросов и формируем результат
                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    buildDownloadButtons();
                }, 8000);
            }
        });

        webView.loadUrl(targetUrl);
    }

    private void extractSmuleMedia(String pageHtml) {
        try {
            mediaTitle = extractMeta(pageHtml, "\"title\"\\s*:\\s*\"([^\"]+)\"");
            mediaArtist = extractMeta(pageHtml, "\"artist\"\\s*:\\s*\"([^\"]+)\"");
            if (mediaTitle.isEmpty()) mediaTitle = "Smule_Track";
            if (mediaArtist.isEmpty()) mediaArtist = "Artist";

            String encryptedUrl = null;
            Matcher m1 = Pattern.compile("\"video_media_mp4_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
            if (m1.find()) {
                encryptedUrl = m1.group(1);
                mediaExtension = "mp4";
            } else {
                Matcher m2 = Pattern.compile("\"visualizer_media_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
                if (m2.find()) {
                    encryptedUrl = m2.group(1);
                    mediaExtension = "mp4";
                } else {
                    Matcher m3 = Pattern.compile("\"media_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
                    if (m3.find()) {
                        encryptedUrl = m3.group(1);
                        mediaExtension = "m4a";
                    }
                }
            }

            if (encryptedUrl != null && !encryptedUrl.isEmpty()) {
                appendLog("🔒 Найден зашифрованный Smule-поток. Расшифровка...");
                if (cachedRc4Key == null) {
                    cachedRc4Key = fetchDynamicKey(pageHtml);
                }
                if (cachedRc4Key == null) {
                    cachedRc4Key = decodeBase64Custom("TT18WlV5TXVeLXFXYn1WTF5qSmR9TXYpOHklYlFXWGY+SUZCRGNKPiU0emcyQ2l8dGVsamBkVlpA");
                }

                String decrypted = rc4Decrypt(encryptedUrl, cachedRc4Key);
                if (decrypted != null && decrypted.startsWith("http")) {
                    synchronized (interceptedMediaUrls) {
                        interceptedMediaUrls.add(decrypted);
                    }
                    appendLog("🔓 Smule успешно расшифрован: " + decrypted);
                }
            }
        } catch (Exception e) {
            appendLog("ℹ️ Smule-парсер: не целевая страница или формат изменен.");
        }
    }

    private void extractGenericMedia(String pageHtml) {
        try {
            // Поиск OpenGraph тегов видео/аудио
            Matcher ogVideo = Pattern.compile("<meta[^>]*property=\"og:video\"[^>]*content=\"([^\"]+)\"").matcher(pageHtml);
            if (ogVideo.find()) {
                synchronized (interceptedMediaUrls) {
                    interceptedMediaUrls.add(ogVideo.group(1));
                }
            }
            Matcher ogAudio = Pattern.compile("<meta[^>]*property=\"og:audio\"[^>]*content=\"([^\"]+)\"").matcher(pageHtml);
            if (ogAudio.find()) {
                synchronized (interceptedMediaUrls) {
                    interceptedMediaUrls.add(ogAudio.group(1));
                }
            }

            // Поиск любых прямых ссылок на mp4/webm/m3u8 в исходном коде
            Matcher directLinks = Pattern.compile("https?://[^\\s\"']+\\.(mp4|m4a|mp3|m3u8|webm|ts)(\\?[^\\s\"']*)?").matcher(pageHtml);
            while (directLinks.find()) {
                synchronized (interceptedMediaUrls) {
                    interceptedMediaUrls.add(directLinks.group(0));
                }
            }
        } catch (Exception ignored) {}
    }

    private void buildDownloadButtons() {
        downloadButtonsLayout.removeAllViews();

        synchronized (interceptedMediaUrls) {
            appendLog("=== ИТОГ АНАЛИЗА ===");
            appendLog("Найдено уникальных медиассылок: " + interceptedMediaUrls.size());

            if (interceptedMediaUrls.isEmpty()) {
                statusText.setText("СТАТУС: Прямые файлы не найдены. Попробуйте воспроизвести видео на странице.");
                mainActionButton.setEnabled(true);
                GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
                btnBg.setColor(Color.parseColor("#2563EB"));
                mainActionButton.setText("🚀 ПОВТОРИТЬ АНАЛИЗ");
                return;
            }

            currentState = State.READY_TO_DOWNLOAD;
            mainActionButton.setEnabled(true);
            GradientDrawable successBg = (GradientDrawable) mainActionButton.getBackground();
            successBg.setColor(Color.parseColor("#10B981"));
            mainActionButton.setText("💾 ВСЕ ПОТОКИ ГОТОВЫ");

            statusText.setText("СТАТУС: ✅ Успешно найдено файлов: " + interceptedMediaUrls.size());
            
            songInfoText.setText(("🎵 НАЙДЕНО МЕДИАФАЙЛОВ: " + interceptedMediaUrls.size()).toUpperCase());
            infoCard.setVisibility(View.VISIBLE);

            int index = 0;
            for (String fileUrl : interceptedMediaUrls) {
                index++;
                final int currentIndex = index;
                final String targetFileUrl = fileUrl;
                
                String fileType = "ФАЙЛ";
                String lower = fileUrl.toLowerCase();
                if (lower.contains("mp4") || lower.contains("mov") || lower.contains("webm")) fileType = "ВИДЕО";
                else if (lower.contains("mp3") || lower.contains("m4a") || lower.contains("aac") || lower.contains("audio")) fileType = "АУДИО";
                else if (lower.contains("m3u8") || lower.contains("ts")) fileType = "ПОТОК (STREAM)";

                final String currentType = fileType;
                Button dlBtn = createPillButton("📥 СКАЧАТЬ " + fileType + " #" + currentIndex, currentIndex % 2 == 0 ? "#059669" : "#2563EB", 12f);
                dlBtn.setOnClickListener(v -> downloadFileToPhone(targetFileUrl, currentType, currentIndex));
                
                downloadButtonsLayout.addView(dlBtn);
                appendLog("👉 Кнопка скачивания #${currentIndex}: ${fileUrl}");
            }

            logEditText.setText(debugLogBuilder.toString());
        }
    }

    private void downloadFileToPhone(String url, String fileType, int index) {
        try {
            String ext = fileType.contains("ВИДЕО") ? ".mp4" : (fileType.contains("АУДИО") ? ".mp3" : ".mp4");
            if (url.toLowerCase().contains(".m4a")) ext = ".m4a";
            if (url.toLowerCase().contains(".webm")) ext = ".webm";
            if (url.toLowerCase().contains(".m3u8")) ext = ".ts";

            String cleanTitle = mediaTitle.replaceAll("[^a-zA-Z0-9а-яА-Я]", "_");
            String fileName = "Extracted_" + cleanTitle + "_" + System.currentTimeMillis() + "_" + index + ext;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(mediaArtist + " - " + mediaTitle);
            request.setDescription("Скачивание медиафайла...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Загрузка началась! Проверьте шторку уведомлений.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void appendLog(String message) {
        debugLogBuilder.append(message).append("\n");
    }

    private void resetState() {
        currentState = State.IDLE;
        interceptedMediaUrls.clear();
        downloadButtonsLayout.removeAllViews();
        urlInput.setText("");
        infoCard.setVisibility(View.GONE);
        mainActionButton.setEnabled(true);
        GradientDrawable defaultBg = (GradientDrawable) mainActionButton.getBackground();
        defaultBg.setColor(Color.parseColor("#2563EB"));
        mainActionButton.setText("🚀 ЗАПУСТИТЬ ГЛУБОКИЙ АНАЛИЗ");
        statusText.setText("СТАТУС: Сброшено. Готов к работе...");
        debugLogBuilder.setLength(0);
        logEditText.setText("");

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
        } catch (Exception ignored) {}

        Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show();
    }

    private String extractMeta(String html, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(html);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return "";
    }

    private String fetchDynamicKey(String pageHtml) {
        try {
            Matcher coreMatcher = Pattern.compile("src=\"([^\"]*community_core-[a-f0-9]+\\.js)\"").matcher(pageHtml);
            String coreUrl;
            if (coreMatcher.find()) {
                coreUrl = coreMatcher.group(1);
                if (coreUrl.startsWith("//")) coreUrl = "https:" + coreUrl;
            } else {
                coreUrl = "https://w1-fa.cdn.smule.com/assets/community_core-f1e2b046e16d487330152631ceb1fdee.js";
            }

            String coreJsText = downloadString(coreUrl);
            if (coreJsText == null) return null;

            Matcher keyMatcher = Pattern.compile("var\\s+v\\s*=\\s*p\\(\"([^\"]+)\"\\)").matcher(coreJsText);
            if (keyMatcher.find()) {
                return decodeBase64Custom(keyMatcher.group(1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String decodeBase64Custom(String b64) {
        try {
            String fixed = b64.replace('-', '+').replace('_', '/');
            byte[] decodedBytes = Base64.decode(fixed, Base64.DEFAULT);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return b64;
        }
    }

    private String rc4Decrypt(String encUrl, String key) {
        if (encUrl == null || !encUrl.startsWith("e:")) return encUrl;
        try {
            String b64 = encUrl.substring(2);
            byte[] cipherBytes = Base64.decode(b64, Base64.DEFAULT);
            int[] iArray = new int[cipherBytes.length];
            for (int k = 0; k < cipherBytes.length; k++) {
                iArray[k] = cipherBytes[k] & 0xFF;
            }

            int[] r = new int[256];
            for (int a = 0; a < 256; a++) r[a] = a;

            int s = 0;
            for (int a = 0; a < 256; a++) {
                s = (s + r[a] + key.charAt(a % key.length())) % 256;
                int temp = r[a]; r[a] = r[s]; r[s] = temp;
            }

            int a = 0; s = 0;
            StringBuilder decrypted = new StringBuilder();
            for (int l = 0; l < iArray.length; l++) {
                a = (a + 1) % 256;
                s = (s + r[a]) % 256;
                int temp = r[a]; r[a] = r[s]; r[s] = temp;
                int keyStreamByte = r[(r[a] + r[s]) % 256];
                decrypted.append((char) (iArray[l] ^ keyStreamByte));
            }

            return decrypted.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String downloadString(String targetUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}