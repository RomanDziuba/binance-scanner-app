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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText, songInfoText;
    private Button mainActionButton, btnClear, btnPaste, btnToggleLogs, btnShowHtml;
    private LinearLayout infoCard, downloadButtonsLayout;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static String cachedRc4Key = null;

    private enum State { IDLE, ANALYZING, READY_TO_DOWNLOAD, DOWNLOADED }
    private State currentState = State.IDLE;
    
    // Структура для хранения информации о найденных треках и партнерах
    public static class SmuleTrackInfo {
        String title;
        String artist;
        String mediaUrl;
        String pageUrl;
        SmuleTrackInfo(String title, String artist, String mediaUrl, String pageUrl) {
            this.title = title;
            this.artist = artist;
            this.mediaUrl = mediaUrl;
            this.pageUrl = pageUrl;
        }
    }

    private final Set<SmuleTrackInfo> foundTracks = new HashSet<>();
    private final Set<String> interceptedMediaUrls = new HashSet<>();
    private final StringBuilder debugLogBuilder = new StringBuilder();
    private String lastDownloadedHtml = ""; 

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
        authorView.setText("✦ Dziuba Roman — Smule Archive & Friends Extractor ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(12f);
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
        titleView.setText("🎵 Полный архив профиля Smule");
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
        urlInput.setHint("Например: https://www.smule.com/nowfearless");
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

        mainActionButton = createPillButton("💾 СОБРАТЬ ВСЕ ТРЕКИ И ДРУЗЕЙ", "#2563EB", 13f);
        mainActionButton.setOnClickListener(v -> startSmuleArchiveCrawler());
        card.addView(mainActionButton);

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

        TextView dlLabel = new TextView(this);
        dlLabel.setText("📥 Найденные записи и песни:");
        dlLabel.setTextColor(Color.parseColor("#38BDF8"));
        dlLabel.setTextSize(12f);
        dlLabel.setTypeface(Typeface.DEFAULT_BOLD);
        dlLabel.setPadding(0, 10, 0, 5);
        card.addView(dlLabel);

        downloadButtonsLayout = new LinearLayout(this);
        downloadButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        card.addView(downloadButtonsLayout);

        LinearLayout debugButtonsRow = new LinearLayout(this);
        debugButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        debugButtonsRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        btnToggleLogs = createPillButton("📋 ОТЧЕТ О ДРУЗЬЯХ И ТРЕКАХ", "#475569", 10f);
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
        logEditText.setHint("Здесь будет подробный отчет о соавторах и треках...");
        logEditText.setTextSize(9f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
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

    private void startSmuleArchiveCrawler() {
        String inputUrl = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(inputUrl)) {
            Toast.makeText(this, "Введите ссылку на профиль!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
            inputUrl = "https://" + inputUrl;
            urlInput.setText(inputUrl);
        }

        final String rootUrl = inputUrl;
        
        synchronized (foundTracks) {
            foundTracks.clear();
        }
        synchronized (interceptedMediaUrls) {
            interceptedMediaUrls.clear();
        }
        downloadButtonsLayout.removeAllViews();
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";

        webView.clearCache(true);
        webView.clearHistory();

        currentState = State.ANALYZING;
        mainActionButton.setEnabled(false);
        GradientDrawable bg = (GradientDrawable) mainActionButton.getBackground();
        bg.setColor(Color.parseColor("#475569"));
        mainActionButton.setText("⏳ СБОР АРХИВА И СОАВТОРОВ...");
        statusText.setText("СТАТУС: Сканирование профиля и подстраниц...");

        appendLog("=== СТАРТ СБОРА АРХИВА: " + rootUrl + " ===");

        // Фоновый рекурсивный сборщик записей и имен
        new Thread(() -> {
            Set<String> visited = new HashSet<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(rootUrl);
            int maxPages = 15;
            int scanned = 0;

            while (!queue.isEmpty() && scanned < maxPages) {
                String currentUrl = queue.poll();
                if (currentUrl == null || visited.contains(currentUrl)) continue;
                visited.add(currentUrl);
                scanned++;

                appendLog("\n🔍 Анализ страницы: " + currentUrl);
                String html = downloadString(currentUrl);
                if (html == null) continue;

                if (scanned == 1) {
                    lastDownloadedHtml = html;
                }

                // Извлекаем трек с этой страницы
                parseAndDecryptTrack(html, currentUrl);

                // Ищем ссылки на другие исполнения/песни на странице
                try {
                    Matcher linkMatch = Pattern.compile("href=\"(/[^\"#]+/p/[^\"]+)\"").matcher(html);
                    while (linkMatch.find()) {
                        String subPath = linkMatch.group(1);
                        String fullLink = "https://www.smule.com" + subPath;
                        if (!visited.contains(fullLink)) {
                            queue.add(fullLink);
                            appendLog("  ➕ Найдено выступление/дуэт: " + fullLink);
                        }
                    }
                } catch (Exception ignored) {}
            }

            appendLog("\n=== СБОР ЗАВЕРШЕН. Просмотрено страниц: " + scanned + ", найдено треков: " + foundTracks.size() + " ===");

            // Запускаем WebView сниффер для подстраховки
            handler.post(() -> startWebViewSniffer(rootUrl));

        }).start();
    }

    private void parseAndDecryptTrack(String html, String pageUrl) {
        try {
            String title = extractMeta(html, "\"title\"\\s*:\\s*\"([^\"]+)\"");
            String artist = extractMeta(html, "\"artist\"\\s*:\\s*\"([^\"]+)\"");
            if (title.isEmpty()) title = "Неизвестный трек";
            if (artist.isEmpty()) artist = "Неизвестный соавтор/исполнитель";

            appendLog("  🎵 Трек: \"" + title + "\" | Исполнитель/Соавтор: " + artist);

            String encryptedUrl = null;
            Matcher m1 = Pattern.compile("\"video_media_mp4_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
            if (m1.find()) {
                encryptedUrl = m1.group(1);
            } else {
                Matcher m2 = Pattern.compile("\"visualizer_media_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
                if (m2.find()) {
                    encryptedUrl = m2.group(1);
                } else {
                    Matcher m3 = Pattern.compile("\"media_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
                    if (m3.find()) encryptedUrl = m3.group(1);
                }
            }

            if (encryptedUrl != null && !encryptedUrl.isEmpty()) {
                if (cachedRc4Key == null) {
                    cachedRc4Key = fetchDynamicKey(html);
                }
                if (cachedRc4Key == null) {
                    cachedRc4Key = decodeBase64Custom("TT18WlV5TXVeLXFXYn1WTF5qSmR9TXYpOHklYlFXWGY+SUZCRGNKPiU0emcyQ2l8dGVsamBkVlpA");
                }

                String decrypted = rc4Decrypt(encryptedUrl, cachedRc4Key);
                if (decrypted != null && decrypted.startsWith("http")) {
                    synchronized (foundTracks) {
                        foundTracks.add(new SmuleTrackInfo(title, artist, decrypted, pageUrl));
                    }
                    appendLog("  🔓 Успешно расшифрована прямая ссылка на медиафайл!");
                }
            }
        } catch (Exception e) {
            appendLog("  ℹ️ Ошибка разбора страницы трека.");
        }
    }

    private void startWebViewSniffer(String targetUrl) {
        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                if (reqUrl.contains("mp4") || reqUrl.contains("m4a") || reqUrl.contains("mp3") || reqUrl.contains("m3u8") || reqUrl.contains("ts") || reqUrl.contains("videoplayback")) {
                    synchronized (interceptedMediaUrls) {
                        interceptedMediaUrls.add(reqUrl);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("🌐 WebView завершил работу. Построение кнопок...");

                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    buildArchiveButtons();
                }, 4000);
            }
        });
        webView.loadUrl(targetUrl);
    }

    private void buildArchiveButtons() {
        downloadButtonsLayout.removeAllViews();

        synchronized (foundTracks) {
            appendLog("\n=== ИТОГОВЫЙ СПИСОК АРХИВА ===");
            appendLog("Найдено полноценных треков с соавторами: " + foundTracks.size());

            if (foundTracks.isEmpty() && interceptedMediaUrls.isEmpty()) {
                statusText.setText("СТАТУС: Записи не найдены. Проверьте правильность ссылки профиля.");
                mainActionButton.setEnabled(true);
                GradientDrawable btnBg = (GradientDrawable) mainActionButton.getBackground();
                btnBg.setColor(Color.parseColor("#2563EB"));
                mainActionButton.setText("💾 ПОВТОРИТЬ СБОР");
                return;
            }

            currentState = State.READY_TO_DOWNLOAD;
            mainActionButton.setEnabled(true);
            GradientDrawable successBg = (GradientDrawable) mainActionButton.getBackground();
            successBg.setColor(Color.parseColor("#10B981"));
            mainActionButton.setText("💾 АРХИВ ГОТОВ К СКАЧИВАНИЮ");

            statusText.setText("СТАТУС: ✅ Найдено записей: " + (foundTracks.size() + interceptedMediaUrls.size()));
            songInfoText.setText(("🎵 СОБРАНО ТРЕКОВ И ДУЭТОВ: " + foundTracks.size()).toUpperCase());
            infoCard.setVisibility(View.VISIBLE);

            int index = 0;
            // Добавляем красиво оформленные кнопки для расшифрованных треков с именами
            for (SmuleTrackInfo track : foundTracks) {
                index++;
                final int currentIndex = index;
                final SmuleTrackInfo targetTrack = track;

                String btnTitle = "📥 " + track.artist + " - " + track.title;
                Button dlBtn = createPillButton(btnTitle, currentIndex % 2 == 0 ? "#059669" : "#2563EB", 11f);
                dlBtn.setOnClickListener(v -> downloadFileToPhone(targetTrack.mediaUrl, targetTrack.artist, targetTrack.title, currentIndex));
                
                downloadButtonsLayout.addView(dlBtn);
                appendLog("👉 [" + currentIndex + "] " + track.artist + " — " + track.title);
            }

            // Добавляем потоки, пойманные сниффером
            for (String netUrl : interceptedMediaUrls) {
                if (!isUrlAlreadyAdded(netUrl)) {
                    index++;
                    final int currentIndex = index;
                    final String targetUrl = netUrl;
                    Button dlBtn = createPillButton("📥 Скачать поток #" + currentIndex, "#7C3AED", 11f);
                    dlBtn.setOnClickListener(v -> downloadFileToPhone(targetUrl, "Smule_Artist", "Stream_Track", currentIndex));
                    downloadButtonsLayout.addView(dlBtn);
                }
            }

            logEditText.setText(debugLogBuilder.toString());
        }
    }

    private boolean isUrlAlreadyAdded(String url) {
        for (SmuleTrackInfo t : foundTracks) {
            if (t.mediaUrl.equals(url)) return true;
        }
        return false;
    }

    private void downloadFileToPhone(String url, String artist, String title, int index) {
        try {
            String ext = ".mp4";
            if (url.toLowerCase().contains(".m4a")) ext = ".m4a";
            if (url.toLowerCase().contains(".mp3")) ext = ".mp3";

            String cleanName = (artist + "_" + title).replaceAll("[^a-zA-Z0-9а-яА-Я]", "_");
            String fileName = "SmuleArchive_" + cleanName + "_" + index + ext;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(artist + " - " + title);
            request.setDescription("Скачивание архивного трека со Smule");
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
        currentState = State.IDLE;
        synchronized (foundTracks) {
            foundTracks.clear();
        }
        synchronized (interceptedMediaUrls) {
            interceptedMediaUrls.clear();
        }
        downloadButtonsLayout.removeAllViews();
        urlInput.setText("");
        infoCard.setVisibility(View.GONE);
        mainActionButton.setEnabled(true);
        GradientDrawable defaultBg = (GradientDrawable) mainActionButton.getBackground();
        defaultBg.setColor(Color.parseColor("#2563EB"));
        mainActionButton.setText("💾 СОБРАТЬ ВСЕ ТРЕКИ И ДРУЗЕЙ");
        statusText.setText("СТАТУС: Сброшено.");
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";
        logEditText.setText("");
        logEditText.setVisibility(View.GONE);

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

    String downloadString(String targetUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
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