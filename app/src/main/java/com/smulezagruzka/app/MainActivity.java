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

        // Главный контейнер
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

        // Подпись автора
        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — Smule Downloader ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(15f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 10, 0, 20);
        container.addView(authorView);

        // Карточка интерфейса
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

        // Поле ввода ссылки
        urlInput = new EditText(this);
        urlInput.setHint("Вставьте ссылку на трек Smule (https://www.smule.com/p/...)");
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

        // Кнопка скачивания
        btnDownload = createStyledButton("⬇️ Найти и скачать трек", "#2563EB");
        btnDownload.setOnClickListener(v -> startSmuleDownload());
        card.addView(btnDownload);

        // Кнопка воспроизведения
        btnPlay = createStyledButton("▶️ Прослушать трек", "#059669");
        btnPlay.setVisibility(View.GONE);
        btnPlay.setOnClickListener(v -> toggleAudioPlayback());
        card.addView(btnPlay);

        // Статус / Логи
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
        statusText.setText("Статус: 🔄 Подключение к Smule и парсинг страницы...");

        new Thread(() -> {
            try {
                // 1. Делаем GET-запрос на страницу трека Smule
                URL url = new URL(trackUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // Устанавливаем User-Agent браузера, чтобы сайт не блокировал запрос
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    handler.post(() -> {
                        btnDownload.setEnabled(true);
                        statusText.setText("Статус: ❌ Ошибка сервера Smule (Код: " + responseCode + ")");
                    });
                    return;
                }

                // 2. Читаем HTML-код страницы
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line).append("\n");
                }
                reader.close();

                String htmlContent = html.toString();

                // 3. Ищем прямую ссылку на медиафайл (.m4a или .mp3) через регулярные выражения
                // Smule обычно отдаёт ссылку в тегах <meta property="og:audio" content="..."> или в JSON-данных
                String mediaUrl = null;

                // Способ А: Ищем в OpenGraph тегах
                Matcher ogMatcher = Pattern.compile("<meta\\s+property=\"og:audio\"\\s+content=\"([^\"]+)\"").matcher(htmlContent);
                if (ogMatcher.find()) {
                    mediaUrl = ogMatcher.group(1);
                }

                // Способ Б: Если не нашли, ищем прямые ссылки на медиапоток в коде страницы
                if (mediaUrl == null || mediaUrl.isEmpty()) {
                    Matcher streamMatcher = Pattern.compile("https?://[^\"]+\\.(m4a|mp3)(\\?[^\"]*)?").matcher(htmlContent);
                    if (streamMatcher.find()) {
                        mediaUrl = streamMatcher.group(0);
                    }
                }

                if (mediaUrl == null || mediaUrl.isEmpty()) {
                    handler.post(() -> {
                        btnDownload.setEnabled(true);
                        statusText.setText("Статус: ❌ Не удалось найти аудиопоток в коде страницы.\nВозможно, трек удален или приватный.");
                    });
                    return;
                }

                // Очищаем HTML-сущности если они есть (например &amp; -> &)
                mediaUrl = mediaUrl.replace("&amp;", "&");

                handler.post(() -> statusText.setText("Статус: 📥 Найдена ссылка! Загрузка аудиофайла..."));

                // 4. Скачиваем сам аудиофайл
                URL audioUrlObj = new URL(mediaUrl);
                HttpURLConnection audioConn = (HttpURLConnection) audioUrlObj.openConnection();
                audioConn.setRequestMethod("GET");
                audioConn.setRequestProperty("User-Agent", "Mozilla/5.0");

                File outputDir = getExternalFilesDir(null);
                if (outputDir == null) outputDir = getCacheDir();
                
                String fileName = "smule_track_" + System.currentTimeMillis() + ".m4a";
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
                    statusText.setText("Статус: ✅ Трек успешно скачан!\nПуть: " + savedFile.getAbsolutePath());
                    Toast.makeText(MainActivity.this, "Песня скачана!", Toast.LENGTH_LONG).show();
                });

            } catch (final Exception e) {
                handler.post(() -> {
                    btnDownload.setEnabled(true);
                    statusText.setText("Статус: ❌ Ошибка скачивания: " + e.getMessage());
                });
            }
        }).start();
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