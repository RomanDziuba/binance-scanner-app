package com.smuleloading.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDownload, btnCopyLogs;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static String cachedRc4Key = null;

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
        authorView.setText("✦ Dziuba Roman — Smule Downloader ✦");
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
        titleView.setText("📥 Загрузчик треков Smule");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setText("https://www.smule.com/c/2930392803_5270362763");
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 10);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        btnDownload = createStyledButton("🚀 Получить прямую ссылку на MP4", "#2563EB");
        btnDownload.setOnClickListener(v -> processSmuleUrl());
        card.addView(btnDownload);

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
        logLabel.setText("📋 Логи и результат:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится прямая ссылка на видео...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(10);
        logEditText.setMaxLines(15);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать ссылку", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Smule MP4 Link", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Скопировано в буфер!", Toast.LENGTH_SHORT).show();
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
        params.setMargins(0, 5, 0, 5);
        btn.setLayoutParams(params);
        return btn;
    }

    private void processSmuleUrl() {
        String urlStr = urlInput.getText().toString().trim();
        if (urlStr.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDownload.setEnabled(false);
        statusText.setText("Статус: ⚙️ Анализ и расшифровка...");
        logEditText.setText("=== ОБРАБОТКА ССЫЛКИ Smule ===\n");

        new Thread(() -> {
            StringBuilder logs = new StringBuilder();
            try {
                logs.append("1. Загрузка страницы трека...\n");
                String pageHtml = downloadString(urlStr);
                if (pageHtml == null) {
                    throw new Exception("Не удалось загрузить страницу трека.");
                }

                logs.append("2. Поиск зашифрованной ссылки...\n");
                Matcher mp4Matcher = Pattern.compile("\"video_media_mp4_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
                if (!mp4Matcher.find()) {
                    throw new Exception("video_media_mp4_url не найден в данных страницы.");
                }
                String encryptedUrl = mp4Matcher.group(1);
                logs.append("Найдено зашифрованное поле.\n\n");

                logs.append("3. Извлечение ключа дешифратора...\n");
                if (cachedRc4Key == null) {
                    cachedRc4Key = fetchDynamicKey(pageHtml);
                }
                if (cachedRc4Key == null) {
                    cachedRc4Key = decodeBase64Custom("TT18WlV5TXVeLXFXYn1WTF5qSmR9TXYpOHklYlFXWGY+SUZCRGNKPiU0emcyQ2l8dGVsamBkVlpA");
                }
                logs.append("Ключ готов.\n\n");

                logs.append("4. Расшифровка потока (RC4)...\n");
                String directMp4Url = rc4Decrypt(encryptedUrl, cachedRc4Key);

                if (directMp4Url != null && directMp4Url.startsWith("http")) {
                    logs.append("✅ УСПЕХ! Прямая ссылка получена:\n\n").append(directMp4Url);
                } else {
                    logs.append("❌ Ошибка расшифровки: получен некорректный результат -> ").append(directMp4Url);
                }

            } catch (Exception e) {
                logs.append("❌ ОШИБКА: ").append(e.getMessage());
            }

            final String finalLogs = logs.toString();
            handler.post(() -> {
                btnDownload.setEnabled(true);
                statusText.setText("Статус: Готово!");
                logEditText.setText(finalLogs);
            });
        }).start();
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
            return "Ошибка RC4: " + e.getMessage();
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