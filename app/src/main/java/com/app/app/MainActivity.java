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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText, profileHeaderTitle, profileStatsText;
    private Button mainActionButton, btnClear, btnPaste, btnToggleLogs, btnShowHtml;
    private LinearLayout profileCard, mediaContainer, friendsContainer;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static String cachedRc4Key = null;

    private final Set<MediaItem> mediaItemsList = new HashSet<>();
    private final Map<String, Integer> friendsMap = new HashMap<>();
    private final StringBuilder debugLogBuilder = new StringBuilder();
    private String lastDownloadedHtml = ""; 
    
    private String profileHandle = "nowfearless";

    private static class MediaItem {
        String url;
        String title;
        String artist;
        String format;

        MediaItem(String url, String title, String artist, String format) {
            this.url = url;
            this.title = TextUtils.isEmpty(title) ? "Запись Smule" : title;
            this.artist = TextUtils.isEmpty(artist) ? "nowfearless" : artist;
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
        authorView.setText("✦ Dziuba Roman — Smule Archive & Media Center ✦");
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
        titleView.setText("🎙️ Центр восстановления архива и друзей");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(15f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        btnPaste = createPillButton("📋 ВСТАВИТЬ ССЫЛКУ ПРОФИЛЯ", "#334155", 12f);
        btnPaste.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPaste);

        urlInput = new EditText(this);
        urlInput.setText("https://www.smule.com/nowfearless");
        urlInput.setHint("Ссылка на профиль Smule...");
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

        mainActionButton = createPillButton("🚀 ИЗВЛЕЧЬ ВСЕ ТРЕКИ И ДРУЗЕЙ", "#2563EB", 13f);
        mainActionButton.setOnClickListener(v -> startArchiveExtraction());
        card.addView(mainActionButton);

        btnClear = createPillButton("❌ СБРОСИТЬ", "#7F1D1D", 11f);
        btnClear.setOnClickListener(v -> resetState());
        card.addView(btnClear);

        statusText = new TextView(this);
        statusText.setText("СТАТУС: ГОТОВ К СБОРУ ДАННЫХ...");
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

        profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.VERTICAL);
        profileCard.setPadding(15, 15, 15, 15);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#111827"));
        infoBg.setCornerRadius(16f);
        profileCard.setBackground(infoBg);
        profileCard.setVisibility(View.GONE);

        profileHeaderTitle = new TextView(this);
        profileHeaderTitle.setTextColor(Color.parseColor("#F59E0B"));
        profileHeaderTitle.setTextSize(13f);
        profileHeaderTitle.setTypeface(Typeface.DEFAULT_BOLD);
        profileCard.addView(profileHeaderTitle);

        profileStatsText = new TextView(this);
        profileStatsText.setTextColor(Color.parseColor("#9CA3AF"));
        profileStatsText.setTextSize(11f);
        profileCard.addView(profileStatsText);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 5, 0, 12);
        profileCard.setLayoutParams(infoParams);
        card.addView(profileCard);

        TextView friendsLabel = new TextView(this);
        friendsLabel.setText("👥 Найденные соавторы и друзья:");
        friendsLabel.setTextColor(Color.parseColor("#38BDF8"));
        friendsLabel.setTextSize(12f);
        friendsLabel.setTypeface(Typeface.DEFAULT_BOLD);
        friendsLabel.setPadding(0, 10, 0, 5);
        card.addView(friendsLabel);

        friendsContainer = new LinearLayout(this);
        friendsContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(friendsContainer);

        TextView mediaLabel = new TextView(this);
        mediaLabel.setText("🎵 Найденные записи и песни:");
        mediaLabel.setTextColor(Color.parseColor("#10B981"));
        mediaLabel.setTextSize(12f);
        mediaLabel.setTypeface(Typeface.DEFAULT_BOLD);
        mediaLabel.setPadding(0, 15, 0, 5);
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
        logEditText.setHint("Логи...");
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
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        
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

    private void startArchiveExtraction() {
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
        
        synchronized (mediaItemsList) {
            mediaItemsList.clear();
        }
        friendsMap.clear();
        mediaContainer.removeAllViews();
        friendsContainer.removeAllViews();
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";

        webView.clearCache(true);
        webView.clearHistory();

        mainActionButton.setEnabled(false);
        GradientDrawable bg = (GradientDrawable) mainActionButton.getBackground();
        bg.setColor(Color.parseColor("#475569"));
        mainActionButton.setText("⏳ ИЗВЛЕЧЕНИЕ ЧЕРЕЗ WEBVIEW...");
        statusText.setText("СТАТУС: Загрузка страницы в браузере и сниффинг...");

        appendLog("=== СТАРТ СНИФФЕРА: " + targetUrl + " ===");

        // Запускаем мощный WebView сниффер, который перехватывает трафик «на лету»
        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                
                // Перехватываем видео, аудио и потоки
                if (reqUrl.contains("mp4") || reqUrl.contains("m4a") || reqUrl.contains("mp3") || reqUrl.contains("m3u8") || reqUrl.contains("ts") || reqUrl.contains("videoplayback")) {
                    synchronized (mediaItemsList) {
                        String fmt = reqUrl.contains("mp4") ? "mp4" : "m4a";
                        if (reqUrl.contains("mp3")) fmt = "mp3";
                        mediaItemsList.add(new MediaItem(reqUrl, "Потоковая запись Smule", profileHandle, fmt));
                        appendLog("🌐 [Sniffer] Пойман медиафайл: " + reqUrl);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("🌐 WebView страница прогружена. Сбор данных...");

                // Автоматически запускаем плееры для инициации загрузки потоков в сети
                webView.loadUrl("javascript:(function(){" +
                        "var vids = document.querySelectorAll('video, audio');" +
                        "for(var i=0; i<vids.length; i++) { try { vids[i].play(); } catch(e){} }" +
                        "})();");

                // Даем время на сбор данных и отрисовываем интерфейс
                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    renderUI();
                }, 7000);
            }
        });

        webView.loadUrl(targetUrl);
    }

    private void renderUI() {
        friendsContainer.removeAllViews();
        mediaContainer.removeAllViews();

        profileHeaderTitle.setText("👤 ПРОФИЛЬ: @" + profileHandle.toUpperCase());
        profileStatsText.setText("Найдено файлов/потоков: " + mediaItemsList.size());
        profileCard.setVisibility(View.VISIBLE);

        synchronized (mediaItemsList) {
            if (mediaItemsList.isEmpty()) {
                statusText.setText("СТАТУС: Потоки не пойманы. Прокрутите страницу в браузере.");
                mainActionButton.setEnabled(true);
                GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
                btnBg.setColor(Color.parseColor("#2563EB"));
                mainActionButton.setText("🚀 ПОВТОРИТЬ");
                return;
            }

            int index = 0;
            for (MediaItem item : mediaItemsList) {
                index++;
                final int currentIndex = index;
                final MediaItem currentItem = item;

                String btnLabel = "📥 [" + item.format.toUpperCase() + "] " + item.title + " #" + currentIndex;
                Button dlBtn = createPillButton(btnLabel, currentIndex % 2 == 0 ? "#059669" : "#2563EB", 11f);
                
                dlBtn.setOnClickListener(v -> downloadMediaFile(currentItem, currentIndex));
                mediaContainer.addView(dlBtn);
                appendLog("👉 Кнопка создана для: " + item.url);
            }

            statusText.setText("СТАТУС: ✅ Успешно найдено файлов: " + mediaItemsList.size());
            mainActionButton.setEnabled(true);
            GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
            btnBg.setColor(Color.parseColor("#10B981"));
            mainActionButton.setText("💾 ФАЙЛЫ ГОТОВЫ К СКАЧИВАНИЮ");
        }
    }

    private void downloadMediaFile(MediaItem item, int index) {
        try {
            String fileName = "SmuleArchive_" + System.currentTimeMillis() + "_" + index + "." + item.format;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.url));
            request.setTitle("Smule Track #" + index);
            request.setDescription("Скачивание медиа из архива...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Загрузка началась!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void appendLog(String message) {
        debugLogBuilder.append(message).append("\n");
    }

    private void resetState() {
        synchronized (mediaItemsList) {
            mediaItemsList.clear();
        }
        friendsMap.clear();
        friendsContainer.removeAllViews();
        mediaContainer.removeAllViews();
        profileCard.setVisibility(View.GONE);
        urlInput.setText("https://www.smule.com/nowfearless");
        mainActionButton.setEnabled(true);
        GradientDrawable defaultBg = (GradientDrawable) mainActionButton.getBackground();
        defaultBg.setColor(Color.parseColor("#2563EB"));
        mainActionButton.setText("🚀 ИЗВЛЕЧЬ ВСЕ ТРЕКИ И ДРУЗЕЙ");
        statusText.setText("СТАТУС: Сброшено.");
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";
        logEditText.setText("");
        logEditText.setVisibility(View.GONE);

        Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show();
    }
}