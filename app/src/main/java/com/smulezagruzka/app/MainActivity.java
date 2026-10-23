package com.smulezagruzka.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput;
    private TextView statusText;
    private Button btnDownload, btnPlay;
    private Handler handler = new Handler(Looper.getMainLooper());
    private File downloadedFile = null;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(30, 30, 30, 30);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — Smule Downloader ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(15f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 10, 0, 20);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 30, 30, 30);
        
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
        titleView.setText("🎤 Скачивание песен со Smule");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(18f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 16);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setHint("Вставьте ссылку на трек Smule (https://www.smule.com/c/...)");
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setHintTextColor(Color.parseColor("#475569"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 15);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        btnDownload = createStyledButton("⬇️ Найти и скачать трек", "#2563EB");
        btnDownload.setOnClickListener(v -> startSmuleDownload());
        card.addView(btnDownload);

        btnPlay = createStyledButton("▶️ Прослушать трек", "#059669");
        btnPlay.setVisibility(View.GONE);
        btnPlay.setOnClickListener(v -> toggleAudioPlayback());
        card.addView(btnPlay);

        statusText = new TextView(this);
        statusText.setText("Статус: Готов к работе\nВведите ссылку на песню Smule выше.");
        statusText.setTextColor(Color.parseColor("#10B981"));
        statusText.setTextSize(12f);
        statusText.setPadding(20, 20, 20, 20);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#1E293B"));
        statusBg.setCornerRadius(14f);
        statusText.setBackground(statusBg);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, 15, 0, 0);
        statusText.setLayoutParams(statusParams);
        card.addView(statusText);

        container.addView(card);
        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        btn.setLayoutParams(params);
        return btn;
    }

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
                String finalUrlStr = getFinalRedirectUrl(trackUrl);
                String recordingKey = extractRecordingKey(finalUrlStr);

                String mediaUrl = null;
                String trackTitle = "Smule_Track_" + System.currentTimeMillis();

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
                    } catch (Exception ignored) {}
                }

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

    private String extractRecordingKey(String url) {
        Matcher matcher = Pattern.compile("(?:recording/|c/|p/)([a-zA-Z0-9_-]+)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void toggleAudioPlayback() {
        if (downloadedFile == null || !downloadedFile.exists()) {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (isPlaying) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                isPlaying = false;
                btnPlay.setText("▶️ Прослушать трек");
                statusText.setText("Статус: ⏹ Воспроизведение остановлено");
            } else {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(downloadedFile.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlaying = true;
                btnPlay.setText("⏹ Остановить воспроизведение");
                statusText.setText("Статус: 🎵 Воспроизведение песни...");

                mediaPlayer.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    btnPlay.setText("▶️ Прослушать трек");
                    statusText.setText("Статус: ✔️ Воспроизведение завершено");
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка воспроизведения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}