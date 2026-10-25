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
import android.webkit.JavascriptInterface;
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
            this.title = TextUtils.isEmpty(title) ? "Выступление Smule" : title;
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
        authorView.setText("✦ Dziuba Roman — Smule Deep API Extractor ✦");
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
        titleView.setText("🎙️ Умный архив треков и соавторов");
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

        mainActionButton = createPillButton("🚀 ЗАГРУЗИТЬ АРХИВ И ДРУЗЕЙ", "#2563EB", 13f);
        mainActionButton.setOnClickListener(v -> startArchiveExtraction());
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
        friendsLabel.setText("👥 Найденные друзья и соавторы:");
        friendsLabel.setTextColor(Color.parseColor("#38BDF8"));
        friendsLabel.setTextSize(12f);
        friendsLabel.setTypeface(Typeface.DEFAULT_BOLD);
        friendsLabel.setPadding(0, 10, 0, 5);
        card.addView(friendsLabel);

        friendsContainer = new LinearLayout(this);
        friendsContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(friendsContainer);

        TextView mediaLabel = new TextView(this);
        mediaLabel.setText("🎵 Ваши песни и дуэты:");
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

        // Регистрируем мост для приема данных из JavaScript
        webView.addJavascriptInterface(new SmuleJSBridge(), "SmuleBridge");

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

    // Мост для приема JSON-данных из браузера
    public class SmuleJSBridge {
        @JavascriptInterface
        public void onDataExtracted(String htmlContent, String pageTitle) {
            handler.post(() -> {
                lastDownloadedHtml = htmlContent;
                appendLog("📥 Получен HTML от движка страницы (" + htmlContent.length() + " байт)");
                parsePageData(htmlContent);
            });
        }
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
        mainActionButton.setText("⏳ СБОР ДАННЫХ...");
        statusText.setText("СТАТУС: Загрузка профиля и расшифровка записей...");

        appendLog("=== СТАРТ СБОРА: " + targetUrl + " ===");

        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("🌐 Страница загружена. Извлечение DOM и метаданных...");

                // Внедряем JavaScript для вытаскивания полного HTML и данных из памяти React
                webView.loadUrl("javascript:window.SmuleBridge.onDataExtracted(document.documentElement.innerHTML, document.title);");

                // Имитируем прокрутку страницы вниз, чтобы подгрузить все треки
                webView.loadUrl("javascript:" +
                    "window.scrollTo(0, document.body.scrollHeight);" +
                    "setTimeout(function() { window.scrollTo(0, 0); }, 1000);" +
                "");

                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    renderUI();
                }, 6000);
            }
        });

        webView.loadUrl(targetUrl);
    }

    private void parsePageData(String html) {
        try {
            // 1. Ищем названия песен и исполнителей через регулярные выражения в JSON данных страницы
            Matcher titleMatcher = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
            Matcher artistMatcher = Pattern.compile("\"artist\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
            Matcher mediaUrlMatcher = Pattern.compile("\"(video_media_mp4_url|visualizer_media_url|media_url)\"\\s*:\\s*\"([^\"]+)\"").matcher(html);

            // Также собираем друзей/соавторов по никам
            Matcher handleMatcher = Pattern.compile("\"handle\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
            while (handleMatcher.find()) {
                String handle = handleMatcher.group(1);
                if (!handle.equalsIgnoreCase(profileHandle) && handle.length() > 2) {
                    int count = friendsMap.containsKey(handle) ? friendsMap.get(handle) : 0;
                    friendsMap.put(handle, count + 1);
                }
            }

            // Извлекаем зашифрованные или прямые медиа ссылки
            while (mediaUrlMatcher.find()) {
                String encUrl = mediaUrlMatcher.group(2);
                String format = encUrl.contains("mp4") ? "mp4" : "m4a";
                
                // Пробуем расшифровать RC4 если это зашифрованный URL Smule
                String resolvedUrl = encUrl;
                if (encUrl.startsWith("e:")) {
                    if (cachedRc4Key == null) cachedRc4Key = fetchDynamicKey(html);
                    if (cachedRc4Key == null) cachedRc4Key = decodeBase64Custom("TT18WlV5TXVeLXFXYn1WTF5qSmR9TXYpOHklYlFXWGY+SUZCRGNKPiU0emcyQ2l8dGVsamBkVlpA");
                    resolvedUrl = rc4Decrypt(encUrl, cachedRc4Key);
                }

                if (resolvedUrl != null && resolvedUrl.startsWith("http")) {
                    synchronized (mediaItemsList) {
                        mediaItemsList.add(new MediaItem(resolvedUrl, "Трек Smule", profileHandle, format));
                    }
                }
            }

            appendLog("✅ Успешно распарсено: треков — " + mediaItemsList.size() + ", друзей — " + friendsMap.size());
        } catch (Exception e) {
            appendLog("⚠️ Ошибка парсинга: " + e.getMessage());
        }
    }

    private void renderUI() {
        friendsContainer.removeAllViews();
        mediaContainer.removeAllViews();

        profileHeaderTitle.setText("👤 ПРОФИЛЬ: @" + profileHandle.toUpperCase());
        profileStatsText.setText("Найдено соавторов: " + friendsMap.size() + " | Найдено треков: " + mediaItemsList.size());
        profileCard.setVisibility(View.VISIBLE);

        // Отрисовка друзей
        if (friendsMap.isEmpty()) {
            TextView noFriends = new TextView(this);
            noFriends.setText("ℹ️ Соавторы на этой странице не найдены.");
            noFriends.setTextColor(Color.parseColor("#94A3B8"));
            noFriends.setTextSize(11f);
            friendsContainer.addView(noFriends);
        } else {
            for (Map.Entry<String, Integer> entry : friendsMap.entrySet()) {
                String friendName = entry.getKey();
                int count = entry.getValue();

                Button friendBtn = createPillButton("🎤 @" + friendName + " (упоминаний: " + count + ")", "#334155", 11f);
                friendBtn.setOnClickListener(v -> {
                    String profileUrl = "https://www.smule.com/" + friendName;
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Friend", profileUrl));
                    Toast.makeText(this, "Ссылка на @" + friendName + " скопирована!", Toast.LENGTH_SHORT).show();
                });
                friendsContainer.addView(friendBtn);
            }
        }

        // Отрисовка медиафайлов с красивыми именами
        synchronized (mediaItemsList) {
            if (mediaItemsList.isEmpty()) {
                statusText.setText("СТАТУС: Треки не найдены. Проверьте 'ВЕСЬ HTML'.");
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

                String btnLabel = "📥 [" + item.format.toUpperCase() + "] " + item.artist + " — " + item.title + " #" + currentIndex;
                Button dlBtn = createPillButton(btnLabel, currentIndex % 2 == 0 ? "#059669" : "#2563EB", 11f);
                
                dlBtn.setOnClickListener(v -> downloadMediaFile(currentItem, currentIndex));
                mediaContainer.addView(dlBtn);
            }

            statusText.setText("СТАТУС: ✅ Найдено треков: " + mediaItemsList.size());
            mainActionButton.setEnabled(true);
            GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
            btnBg.setColor(Color.parseColor("#10B981"));
            mainActionButton.setText("💾 АРХИВ ГОТОВ К СКАЧИВАНИЮ");
        }
    }

    private void downloadMediaFile(MediaItem item, int index) {
        try {
            String fileName = "SmuleArchive_" + System.currentTimeMillis() + "_" + index + "." + item.format;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.url));
            request.setTitle(item.artist + " - " + item.title);
            request.setDescription("Скачивание трека со Smule...");
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
        mainActionButton.setText("🚀 ЗАГРУЗИТЬ АРХИВ И ДРУЗЕЙ");
        statusText.setText("СТАТУС: Сброшено.");
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";
        logEditText.setText("");
        logEditText.setVisibility(View.GONE);

        Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show();
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
            if (keyMatcher.find()) return decodeBase64Custom(keyMatcher.group(1));
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
            for (int k = 0; k < cipherBytes.length; k++) iArray[k] = cipherBytes[k] & 0xFF;

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
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
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