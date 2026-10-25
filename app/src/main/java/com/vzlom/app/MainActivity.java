package com.vzlom.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

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

    public class MainActivity {
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
                        extractedMediaList.add(new MediaItem(videoUrl, currentPageTitle, "ВИДЕО (Player)", "mp4"));
                        appendLog("🌐 [JS DOM] Найден видеофайл: " + videoUrl);
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
        statusText.setText("СТАТУС: Загрузка страницы и сбор данных...");

        appendLog("=== СТАРТ АНАЛИЗА: " + targetUrl + " ===");

        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString().toLowerCase();
                
                if (reqUrl.contains("fbcdn.net") || reqUrl.contains("akamaized.net") || reqUrl.contains("smule.com") ||
                    reqUrl.matches(".*\\.(mp4|m4a|mp3|wav|aac|ogg|flac|m3u8|ts|webm|mov)(\\?.*)?$") || 
                    reqUrl.contains("videoplayback") || reqUrl.contains("video-ams")) {
                    
                    if (reqUrl.contains(".mp4") || reqUrl.contains(".m4a") || reqUrl.contains("video") || reqUrl.contains("fbcdn") || reqUrl.contains("smule")) {
                        synchronized (extractedMediaList) {
                            String fmt = reqUrl.contains("m4a") || reqUrl.contains("mp3") ? "m4a" : "mp4";
                            String type = fmt.equals("m4a") ? "АУДИО" : "ВИДЕО";
                            extractedMediaList.add(new MediaItem(request.getUrl().toString(), currentPageTitle, type + " (CDN)", fmt));
                            appendLog("🌐 [Sniffer] Перехвачен файл: " + request.getUrl().toString());
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appendLog("🌐 Страница загружена. Запуск JS-распознавателя...");

                webView.loadUrl("javascript:window.UniversalBridge.onPageReady(document.documentElement.innerHTML, document.title);");
                webView.loadUrl("javascript:(function(){" +
                    "var vids = document.querySelectorAll('video');" +
                    "for(var i=0; i<vids.length; i++) {" +
                    "  if(vids[i].src) window.UniversalBridge.onVideoUrlFound(vids[i].src);" +
                    "  var sources = vids[i].querySelectorAll('source');" +
                    "  for(var j=0; j<sources.length; j++) { if(sources[j].src) window.UniversalBridge.onVideoUrlFound(sources[j].src); }" +
                    "}" +
                    "var playBtns = document.querySelectorAll('[aria-label*=\"Play\"], [aria-label*=\"Воспроизвести\"], div[role=\"button\"]');" +
                    "for(var k=0; k<playBtns.length; k++) { try { playBtns[k].click(); } catch(e){} }" +
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
                    extractedMediaList.add(new MediaItem(ogVideo.group(1), currentPageTitle, "ВИДЕО (OG)", "mp4"));
                }
            }

            Matcher jsonVideoMatcher = Pattern.compile("\"(playable_url|browser_native_hd_url|browser_native_sd_url|video_url|hd_src|sd_src|video_media_mp4_url|media_url)\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
            while (jsonVideoMatcher.find()) {
                String rawUrl = jsonVideoMatcher.group(2).replace("\\/", "/");
                if (rawUrl.startsWith("http")) {
                    String fmt = rawUrl.contains("m4a") ? "m4a" : "mp4";
                    String type = fmt.equals("m4a") ? "АУДИО" : "ВИДЕО";
                    synchronized (extractedMediaList) {
                        extractedMediaList.add(new MediaItem(rawUrl, currentPageTitle, type + " (JSON)", fmt));
                    }
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

            appendLog("✅ Успешно распарсено объектов: " + extractedMediaList.size());
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
                statusText.setText("СТАТУС: Файлы не найдены. Нажмите Play на странице.");
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
            String fileName = "Media_" + cleanTitle + "_" + System.currentTimeMillis() + "_" + index + "." + item.format;
            
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
        mainActionButton.setText("🚀 ЗАПУСТИТЬ АНАЛИЗ");
        statusText.setText("СТАТУС: Сброшено.");
        debugLogBuilder.setLength(0);
        lastDownloadedHtml = "";
        logEditText.setText("");
        logEditText.setVisibility(View.GONE);

        Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show();
    }
}