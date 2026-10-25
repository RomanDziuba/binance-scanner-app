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
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText, pageInfoTitle, pageInfoDetails;
    private Button mainActionButton, btnClear, btnPaste, btnToggleLogs, btnShowHtml;
    private LinearLayout infoCard, mediaContainer;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());

    private final Set<MediaItem> extractedMediaList = new HashSet<>();
    private final StringBuilder debugLogBuilder = new StringBuilder();
    private String lastDownloadedHtml = ""; 
    private String currentPageTitle = "Универсальный ресурс";

    private static class MediaItem {
        String url;
        String title;
        String type; // ВИДЕО, АУДИО, ПОТОК, ФОТО
        String format;

        MediaItem(String url, String title, String type, String format) {
            this.url = url;
            this.title = TextUtils.isEmpty(title) ? "Медиафайл" : title;
            this.type = type;
            this.format = format;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MediaItem item = (MediaItem) o;
            return url.equals(item.url);
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }
    }

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
        authorView.setText("✦ Dziuba Roman — Universal Data & Media Extractor ✦");
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
        titleView.setText("🔥 Универсальный экстрактор (Facebook / Web)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(15f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        btnPaste = createPillButton("📋 ВСТАВИТЬ ССЫЛКУ ИЗ БУФЕРА", "#334155", 12f);
        btnPaste.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPaste);

        urlInput = new EditText(this);
        urlInput.setHint("Вставьте ссылку (Facebook, YouTube, сайты)...");
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

        mainActionButton = createPillButton("🚀 ЗАПУСТИТЬ УНИВЕРСАЛЬНЫЙ АНАЛИЗ", "#2563EB", 13f);
        mainActionButton.setOnClickListener(v -> startUniversalExtraction());
        card.addView(mainActionButton);

        btnClear = createPillButton("❌ СБРОСИТЬ", "#7F1D1D", 11f);
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

        infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(15, 15, 15, 15);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#111827"));
        infoBg.setCornerRadius(16f);
        infoCard.setBackground(infoBg);
        infoCard.setVisibility(View.GONE);

        pageInfoTitle = new TextView(this);
        pageInfoTitle.setTextColor(Color.parseColor("#F59E0B"));
        pageInfoTitle.setTextSize(13f);
        pageInfoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        infoCard.addView(pageInfoTitle);

        pageInfoDetails = new TextView(this);
        pageInfoDetails.setTextColor(Color.parseColor("#9CA3AF"));
        pageInfoDetails.setTextSize(11f);
        infoCard.addView(pageInfoDetails);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 5, 0, 12);
        infoCard.setLayoutParams(infoParams);
        card.addView(infoCard);

        TextView mediaLabel = new TextView(this);
        mediaLabel.setText("📥 Найденные медиафайлы и потоки:");
        mediaLabel.setTextColor(Color.parseColor("#38BDF8"));
        mediaLabel.setTextSize(12f);
        mediaLabel.setTypeface(Typeface.DEFAULT_BOLD);
        mediaLabel.setPadding(0, 10, 0, 5);
        card.addView(mediaLabel);

        mediaContainer = new LinearLayout(this);
        mediaContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(mediaContainer);

        LinearLayout debugButtonsRow = new LinearLayout(this);
        debugButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        
        btnToggleLogs = createPillButton("📋 ВСЕ ЛОГИ", "#475569", 10f);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMargins(2, 4, 2, 4);
        btnToggleLogs.setLayoutParams(p1);
        btnToggleLogs.setOnClickListener(v -> {
            logEditText.setVisibility(logEditText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            logEditText.setText(debugLogBuilder.toString());
        });
        debugButtonsRow.addView(btnToggleLogs);

        btnShowHtml = createPillButton("🌐 ВЕСЬ HTML", "#0D9488", 10f);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p2.setMargins(2, 4, 2, 4);
        btnShowHtml.setLayoutParams(p2);
        btnShowHtml.setOnClickListener(v -> {
            logEditText.setVisibility(logEditText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            logEditText.setText(TextUtils.isEmpty(lastDownloadedHtml) ? "ℹ️ HTML код пуст." : lastDownloadedHtml);
        });
        debugButtonsRow.addView(btnShowHtml);

        card.addView(debugButtonsRow);

        logEditText = new EditText(this);
        logEditText.setHint("Логи отладки...");
        logEditText.setTextSize(9f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setVisibility(View.GONE);
        card.addView(logEditText);

        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        // Используем User-Agent мобильного Safari (iPhone), чтобы Facebook отдавал простую мобильную верстку с видеотегами
        ws.setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1");

        webView.addJavascriptInterface(new UniversalJSBridge(), "UniversalBridge");

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
                        Toast.makeText(this, "Ссылка вставлена!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            Toast.makeText(this, "Буфер пуст!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }

    public class UniversalJSBridge {
        @JavascriptInterface
        public void onPageReady(String htmlContent, String pageTitle) {
            handler.post(() -> {
                lastDownloadedHtml = htmlContent;
                currentPageTitle = TextUtils.isEmpty(pageTitle) ? "Веб-страница" : pageTitle;
                appendLog("📥 Загружен DOM: " + currentPageTitle + " (" + htmlContent.length() + " байт)");
                parseUniversalContent(htmlContent);
            });
        }

        @JavascriptInterface
        public void onVideoUrlFound(String videoUrl) {
            handler.post(() -> {
                if (!TextUtils.isEmpty(videoUrl) && videoUrl.startsWith("http")) {
                    synchronized (extractedMediaList) {
                        extractedMediaList.add(new MediaItem(videoUrl, currentPageTitle, "ВИДЕО (DOM Player)", "mp4"));
                        appendLog("🌐 [JS DOM] Найден видеоплеер: " + videoUrl);
                    }
                }
            });
        }
    }

    private void startUniversalExtraction() {
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
        
        synchronized (extractedMediaList) {
            extractedMediaList.clear();
        }
        mediaContainer.removeAllViews();
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";
        infoCard.setVisibility(View.GONE);

        webView.clearCache(true);
        webView.clearHistory();

        mainActionButton.setEnabled(false);
        GradientDrawable bg = (GradientDrawable) mainActionButton.getBackground();
        bg.setColor(Color.parseColor("#475569"));
        mainActionButton.setText("⏳ АНАЛИЗ РЕСУРСА...");
        statusText.setText("СТАТУС: Загрузка страницы и обход редиректов...");

        appendLog("=== СТАРТ УНИВЕРСАЛЬНОГО АНАЛИЗА: " + targetUrl + " ===");

        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString().toLowerCase();
                
                // Перехват медиапотоков (mp4, m3u8, ts, m4a, mp3, video playback)
                if (reqUrl.matches(".*\\.(mp4|m4a|mp3|wav|aac|ogg|flac|m3u8|ts|webm|mov)(\\?.*)?$") || 
                    reqUrl.contains("videoplayback") || reqUrl.contains("video-ams") || reqUrl.contains("fbcdn.net") && reqUrl.contains(".mp4")) {
                    synchronized (extractedMediaList) {
                        String type = "ВИДЕО";
                        String fmt = "mp4";
                        if (reqUrl.contains("mp3") || reqUrl.contains("m4a")) { type = "АУДИО"; fmt = "mp3"; }
                        
                        extractedMediaList.add(new MediaItem(request.getUrl().toString(), "Поток Facebook/Web", type, fmt));
                        appendLog("🌐 [Sniffer] Пойман поток: " + request.getUrl().toString());
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("🌐 Страница прогружена. Сканирование плееров...");

                // Передаем HTML и запускаем скрипт поиска тегов <video> и source на странице (актуально для Facebook)
                webView.loadUrl("javascript:window.UniversalBridge.onPageReady(document.documentElement.innerHTML, document.title);");
                webView.loadUrl("javascript:(function(){" +
                    "var vids = document.querySelectorAll('video');" +
                    "for(var i=0; i<vids.length; i++) {" +
                    "  if(vids[i].src) window.UniversalBridge.onVideoUrlFound(vids[i].src);" +
                    "  var sources = vids[i].querySelectorAll('source');" +
                    "  for(var j=0; j<sources.length; j++) { if(sources[j].src) window.UniversalBridge.onVideoUrlFound(sources[j].src); }" +
                    "}" +
                    "var ogV = document.querySelector('meta[property=\"og:video\"], meta[property=\"og:video:secure_url\"]');" +
                    "if(ogV && ogV.content) window.UniversalBridge.onVideoUrlFound(ogV.content);" +
                    "})();");

                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    renderUI();
                }, 8000);
            }
        });

        webView.loadUrl(targetUrl);
    }

    private void parseUniversalContent(String html) {
        try {
            Matcher ogTitle = Pattern.compile("<meta[^>]*property=\"og:title\"[^>]*content=\"([^\"]+)\"").matcher(html);
            if (ogTitle.find()) {
                currentPageTitle = ogTitle.group(1);
            }

            Matcher ogVideo = Pattern.compile("<meta[^>]*property=\"og:video(?:\\:secure_url)?\"[^>]*content=\"([^\"]+)\"").matcher(html);
            if (ogVideo.find()) {
                synchronized (extractedMediaList) {
                    extractedMediaList.add(new MediaItem(ogVideo.group(1), currentPageTitle, "ВИДЕО (Facebook OpenGraph)", "mp4"));
                }
            }

            Matcher directLinks = Pattern.compile("https?://[^\\s\"'<>]+?\\.(mp4|m4a|mp3|m3u8|webm|ts)(\\?[^\\s\"'<>]*)?").matcher(html);
            while (directLinks.find()) {
                String fileUrl = directLinks.group(0);
                String ext = directLinks.group(1).toLowerCase();
                String type = ext.equals("mp4") ? "ВИДЕО" : "АУДИО";

                synchronized (extractedMediaList) {
                    extractedMediaList.add(new MediaItem(fileUrl, currentPageTitle, type, ext));
                }
            }

            appendLog("✅ Распарсено объектов: " + extractedMediaList.size());
        } catch (Exception e) {
            appendLog("⚠️ Ошибка парсинга: " + e.getMessage());
        }
    }

    private void renderUI() {
        mediaContainer.removeAllViews();

        pageInfoTitle.setText("📄 " + currentPageTitle);
        pageInfoDetails.setText("Найдено медиафайлов: " + extractedMediaList.size());
        infoCard.setVisibility(View.VISIBLE);

        synchronized (extractedMediaList) {
            if (extractedMediaList.isEmpty()) {
                statusText.setText("СТАТУС: Видео не найдено. Нажмите Play в браузере.");
                mainActionButton.setEnabled(true);
                GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
                btnBg.setColor(Color.parseColor("#2563EB"));
                mainActionButton.setText("🚀 ПОВТОРИТЬ АНАЛИЗ");
                return;
            }

            int index = 0;
            for (MediaItem item : extractedMediaList) {
                index++;
                final int currentIndex = index;
                final MediaItem currentItem = item;

                String btnLabel = "📥 [" + item.type + "] " + item.title + " #" + currentIndex;
                Button dlBtn = createPillButton(btnLabel, currentIndex % 2 == 0 ? "#059669" : "#2563EB", 11f);
                
                dlBtn.setOnClickListener(v -> downloadMediaFile(currentItem, currentIndex));
                mediaContainer.addView(dlBtn);
            }

            statusText.setText("СТАТУС: ✅ Найдено файлов: " + extractedMediaList.size());
            mainActionButton.setEnabled(true);
            GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
            btnBg.setColor(Color.parseColor("#10B981"));
            mainActionButton.setText("💾 ДАННЫЕ ГОТОВЫ К ЗАГРУЗКЕ");
        }
    }

    private void downloadMediaFile(MediaItem item, int index) {
        try {
            String cleanTitle = item.title.replaceAll("[^a-zA-Z0-9а-яА-Я]", "_");
            if (cleanTitle.length() > 30) cleanTitle = cleanTitle.substring(0, 30);
            String fileName = "FB_Media_" + cleanTitle + "_" + System.currentTimeMillis() + "_" + index + "." + item.format;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.url));
            request.setTitle(item.title);
            request.setDescription("Скачивание медиафайла...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Загрузка началась!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void appendLog(String message) {
        debugLogBuilder.append(message).append("\n");
    }

    private void resetState() {
        synchronized (extractedMediaList) {
            extractedMediaList.clear();
        }
        mediaContainer.removeAllViews();
        infoCard.setVisibility(View.GONE);
        urlInput.setText("");
        mainActionButton.setEnabled(true);
        GradientDrawable defaultBg = (GradientDrawable) mainActionButton.getBackground();
        defaultBg.setColor(Color.parseColor("#2563EB"));
        mainActionButton.setText("🚀 ЗАПУСТИТЬ УНИВЕРСАЛЬНЫЙ АНАЛИЗ");
        statusText.setText("СТАТУС: Сброшено.");
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";
        logEditText.setText("");
        logEditText.setVisibility(View.GONE);

        Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show();
    }
}