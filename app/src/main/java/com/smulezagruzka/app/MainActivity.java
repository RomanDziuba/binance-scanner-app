package com.smulezagruzka.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

private void startSmuleDownload() {
    String trackUrl = urlInput.getText().toString().trim();
    if (trackUrl.isEmpty() || !trackUrl.contains("smule.com")) {
        Toast.makeText(this, "⚠️ Введите корректную ссылку Smule!", Toast.LENGTH_SHORT).show();
        return;
    }

    btnDownload.setEnabled(false);
    statusText.setText("Статус: 🔄 Подключение к Smule...");

    new Thread(() -> {
        try {
            // 1. Разворачиваем короткую ссылку (получаем финальный URL после всех редиректов)
            String finalUrlStr = getFinalRedirectUrl(trackUrl);
            
            // 2. Вытягиваем ключевой ID записи (recording key) из ссылки
            String recordingKey = extractRecordingKey(finalUrlStr);

            String mediaUrl = null;
            String trackTitle = "Smule_Track_" + System.currentTimeMillis();

            // 3. Вариант А: Запрос к официальному JSON API Smule (наиболее надежный способ в обход 403)
            if (recordingKey != null && !recordingKey.isEmpty()) {
                try {
                    String apiUrl = "https://www.smule.com/api/show/recording/" + recordingKey;
                    HttpURLConnection apiConn = (HttpURLConnection) new URL(apiUrl).openConnection();
                    setupBrowserHeaders(apiConn);

                    if (apiConn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(apiConn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder apiSb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) apiSb.append(line);
                        br.close();

                        JSONObject json = new JSONObject(apiSb.toString());
                        if (json.has("media_url")) {
                            mediaUrl = json.optString("media_url");
                        }
                        if (json.has("title")) {
                            trackTitle = json.optString("title").replaceAll("[^a-zA-Z0-9_-]", "_");
                        }
                    }
                } catch (Exception e) {
                    // Если API недоступно, переходим к парсингу HTML
                }
            }

            // 4. Вариант Б: Если API не отдало ссылку, парсим HTML с заголовками настоящего браузера
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                HttpURLConnection conn = (HttpURLConnection) new URL(finalUrlStr).openConnection();
                setupBrowserHeaders(conn);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    handler.post(() -> {
                        btnDownload.setEnabled(true);
                        statusText.setText("Статус: ❌ Защита Smule заблокировала запрос (Код: " + responseCode + ")");
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) html.append(line).append("\n");
                reader.close();

                String htmlContent = html.toString();

                Matcher ogMatcher = Pattern.compile("<meta\\s+property=\"og:audio\"\\s+content=\"([^\"]+)\"").matcher(htmlContent);
                if (ogMatcher.find()) {
                    mediaUrl = ogMatcher.group(1);
                }

                if (mediaUrl == null || mediaUrl.isEmpty()) {
                    Matcher streamMatcher = Pattern.compile("https?://[^\"]+\\.(m4a|mp3|mp4)(\\?[^\"]*)?").matcher(htmlContent);
                    if (streamMatcher.find()) {
                        mediaUrl = streamMatcher.group(0);
                    }
                }
            }

            if (mediaUrl == null || mediaUrl.isEmpty()) {
                handler.post(() -> {
                    btnDownload.setEnabled(true);
                    statusText.setText("Статус: ❌ Не удалось извлечь медиафайл.\nВозможно, запись приватная.");
                });
                return;
            }

            mediaUrl = mediaUrl.replace("&amp;", "&");
            final String downloadMediaUrl = mediaUrl;
            final String fileName = trackTitle + ".m4a";

            handler.post(() -> statusText.setText("Статус: 📥 Найдена прямая ссылка! Скачивание..."));

            // 5. Скачивание аудиофайла
            HttpURLConnection audioConn = (HttpURLConnection) new URL(downloadMediaUrl).openConnection();
            setupBrowserHeaders(audioConn);

            File outputDir = getExternalFilesDir(null);
            if (outputDir == null) outputDir = getCacheDir();
            downloadedFile = new File(outputDir, fileName);

            try (InputStream in = audioConn.getInputStream();
                 FileOutputStream out = new FileOutputStream(downloadedFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            final File savedFile = downloadedFile;
            handler.post(() -> {
                btnDownload.setEnabled(true);
                btnPlay.setVisibility(View.VISIBLE);
                statusText.setText("Статус: ✅ Трек скачан!\nФайл: " + fileName);
                Toast.makeText(MainActivity.this, "Песня успешно сохранена!", Toast.LENGTH_LONG).show();
            });

        } catch (final Exception e) {
            handler.post(() -> {
                btnDownload.setEnabled(true);
                statusText.setText("Статус: ❌ Ошибка: " + e.getMessage());
            });
        }
    }).start();
}

// Вспомогательный метод 1: Полный набор заголовков современного браузера
private void setupBrowserHeaders(HttpURLConnection conn) {
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
    conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
    conn.setRequestProperty("Sec-Fetch-Dest", "document");
    conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
    conn.setRequestProperty("Sec-Fetch-Site", "none");
    conn.setRequestProperty("Sec-Fetch-User", "?1");
    conn.setConnectTimeout(12000);
    conn.setReadTimeout(12000);
}

// Вспомогательный метод 2: Получение итогового URL после редиректов
private String getFinalRedirectUrl(String urlStr) {
    try {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setInstanceFollowRedirects(false);
        setupBrowserHeaders(conn);
        int status = conn.getResponseCode();

        if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
            status == HttpURLConnection.HTTP_MOVED_PERM || 
            status == HttpURLConnection.HTTP_SEE_OTHER) {
            String redirectUrl = conn.getHeaderField("Location");
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                return getFinalRedirectUrl(redirectUrl);
            }
        }
    } catch (Exception ignored) {}
    return urlStr;
}

// Вспомогательный метод 3: Извлечение ключа записи из URL
private String extractRecordingKey(String url) {
    Matcher matcher = Pattern.compile("(?:recording/|c/|p/)([a-zA-Z0-9_-]+)").matcher(url);
    if (matcher.find()) {
        return matcher.group(1);
    }
    return null;
}