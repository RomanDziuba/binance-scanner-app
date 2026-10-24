package com.rom.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnPasteUrl, btnCopyLogs;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Продвинутый HTTP-клиент с поддержкой HTTP/2 и современных TLS-шифров
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false) // Ручное отслеживание редиректов для глубокого анализа
            .followSslRedirects(false)
            .build();

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
        authorView.setText("✦ Dziuba Roman — Advanced Core Hunter (OkHttp) ✦");
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
        titleView.setText("🛠️ Обход защиты и глубокий анализ");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setHint("Введите или вставьте URL...");
        urlInput.setHintTextColor(Color.parseColor("#475569"));
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 8);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        btnPasteUrl = createStyledButton("📋 Вставить из буфера", "#334155");
        btnPasteUrl.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPasteUrl);

        btnDebug = createStyledButton("🚀 Запустить защищенный пробой", "#2563EB");
        btnDebug.setOnClickListener(v -> startAdvancedAnalysis());
        card.addView(btnDebug);

        statusText = new TextView(this);
        statusText.setText("Статус: Готов к запуску...");
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
        logLabel.setText("📋 Отчет о проходе защиты:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится детальный лог обхода и содержимое...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(14);
        logEditText.setMaxLines(20);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать отчет", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Advanced Report", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Отчет успешно скопирован!", Toast.LENGTH_SHORT).show();
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
        params.setMargins(0, 4, 0, 4);
        btn.setLayoutParams(params);
        return btn;
    }

    private void pasteFromClipboard() {
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
        Toast.makeText(this, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
    }

    private void startAdvancedAnalysis() {
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
        btnDebug.setEnabled(false);
        statusText.setText("Статус: [Защищенный этап] Обход редиректов...");
        logEditText.setText("=== 🛡️ ADVANCED CORE HUNTER REPORT ===\nЦель: " + targetUrl + "\n\n");

        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("=== 🛡️ ADVANCED CORE HUNTER REPORT ===\n");
            report.append("Цель: ").append(targetUrl).append("\n\n");

            try {
                String currentUrl = targetUrl;
                int redirectCount = 0;
                Response response = null;

                // 1. Продвинутый обход редиректов с OkHttp
                report.append("--- ЭТАП 1: Трассировка редиректов ---\n");
                while (redirectCount < 5) {
                    Request request = new Request.Builder()
                            .url(currentUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                            .header("Sec-Ch-Ua", "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                            .header("Sec-Ch-Ua-Mobile", "?0")
                            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                            .header("Upgrade-Insecure-Requests", "1")
                            .build();

                    if (response != null) {
                        response.close();
                    }

                    response = httpClient.newCall(request).execute();
                    int code = response.code();

                    report.append("• Шаг [").append(redirectCount + 1).append("] URL: ").append(currentUrl).append("\n");
                    report.append("  Код ответа: ").append(code).append("\n");

                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        String redirectUrl = response.header("Location");
                        report.append("  ↪️ Редирект на: ").append(redirectUrl).append("\n\n");

                        if (redirectUrl == null) break;

                        if (redirectUrl.startsWith("/")) {
                            okhttp3.HttpUrl base = response.request().url();
                            redirectUrl = base.scheme() + "://" + base.host() + redirectUrl;
                        }

                        currentUrl = redirectUrl;
                        redirectCount++;
                    } else {
                        break;
                    }
                }

                report.append("• Финальный целевой URL: ").append(currentUrl).append("\n");

                if (response == null || !response.isSuccessful()) {
                    report.append("❌ Ошибка: Сервер не отдал успешный код (HTTP ").append(response != null ? response.code() : "null").append(").\n");
                    finishAnalysis(report.toString());
                    if (response != null) response.close();
                    return;
                }

                String pageContent = response.body() != null ? response.body().string() : "";
                response.close();

                report.append("• Размер данных страницы: ").append(pageContent.length()).append(" символов\n");

                Matcher titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(pageContent);
                if (titleMatcher.find()) {
                    report.append("• Заголовок (Title): ").append(titleMatcher.group(1).trim()).append("\n");
                }
                report.append("✅ Этап 1 успешно пройден без Connection reset!\n\n");

                // 2. Сканирование API и JSON-состояний
                updateStatus("Статус: [Этап 2] Анализ структуры и API...");
                report.append("--- ЭТАП 2: Поиск API и скрытых данных ---\n");

                Matcher stateMatcher = Pattern.compile("(window\\.__[a-zA-Z0-9_]+__\\s*=\\s*\\{.+?\\});", Pattern.DOTALL).matcher(pageContent);
                int statesFound = 0;
                while (stateMatcher.find() && statesFound < 3) {
                    statesFound++;
                    String snippet = stateMatcher.group(1);
                    if (snippet.length() > 300) snippet = snippet.substring(0, 300) + "... [обрезано]";
                    report.append("🎯 Найдено состояние №").append(statesFound).append(":\n").append(snippet).append("\n\n");
                }
                if (statesFound == 0) report.append("ℹ️ Крупных глобальных JSON-структур не найдено.\n");

                Matcher apiMatcher = Pattern.compile("[\"']((?:https?://[^\"']+|/[a-zA-Z0-9_/\\-]+)?/api/v[0-9]+/[^\"']+)[\"']").matcher(pageContent);
                int apiCount = 0;
                while (apiMatcher.find()) {
                    apiCount++;
                    report.append("  [API #").append(apiCount).append("] ").append(apiMatcher.group(1)).append("\n");
                    if (apiCount >= 5) break;
                }
                report.append("\n✅ Этап 2 завершен.\n\n");

                // 3. Глубокий анализ JS-файлов
                updateStatus("Статус: [Этап 3] Скачивание и глубокий анализ скриптов...");
                report.append("--- ЭТАП 3: Анализ JS-скриптов ---\n");

                List<String> jsLinks = extractJsLinks(pageContent, currentUrl);
                report.append("• Найдено внешних JS: ").append(jsLinks.size()).append("\n");

                int analyzed = 0;
                for (String jsUrl : jsLinks) {
                    if (analyzed >= 3) break;
                    analyzed++;
                    report.append("\n[Скрипт #").append(analyzed).append("] ").append(jsUrl).append("\n");

                    try {
                        Request jsReq = new Request.Builder()
                                .url(jsUrl)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                .header("Referer", currentUrl)
                                .build();
                        Response jsResp = httpClient.newCall(jsReq).execute();
                        if (jsResp.isSuccessful() && jsResp.body() != null) {
                            String jsText = jsResp.body().string();
                            jsResp.close();
                            report.append("  Размер: ").append(jsText.length()).append(" символов\n");

                            Matcher codeMatcher = Pattern.compile(".{0,80}(?:\"e:\"|'e:'|media_url|video_media_url|decodeUrl|decryptUrl|authorization|bearer|token|signature|AES|atob).{0,80}").matcher(jsText);
                            int matches = 0;
                            while (codeMatcher.find()) {
                                matches++;
                                report.append("  🎯 Совпадение [").append(matches).append("]: ").append(codeMatcher.group(0).trim()).append("\n");
                                if (matches >= 5) break;
                            }
                            if (matches == 0) report.append("  (искомых сигнатур не найдено)\n");
                        } else {
                            if (jsResp != null) jsResp.close();
                            report.append("  ❌ Ошибка загрузки скрипта\n");
                        }
                    } catch (Exception e) {
                        report.append("  ❌ Ошибка: ").append(e.getMessage()).append("\n");
                    }
                }

                report.append("\n🎉 Разведка успешно завершена!");

            } catch (Exception e) {
                report.append("\n❌ ОШИБКА ПРОБОЯ ЗАЩИТЫ: ").append(e.toString());
            }

            finishAnalysis(report.toString());
        }).start();
    }

    private void updateStatus(String status) {
        handler.post(() -> statusText.setText(status));
    }

    private void finishAnalysis(String finalReport) {
        handler.post(() -> {
            btnDebug.setEnabled(true);
            statusText.setText("Статус: ✅ Анализ завершен!");
            logEditText.setText(finalReport);
        });
    }

    private List<String> extractJsLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        Matcher scriptMatcher = Pattern.compile("<script[^>]+src=[\"']([^\"']+\\.js[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
        while (scriptMatcher.find()) {
            String src = scriptMatcher.group(1);
            if (src.startsWith("//")) {
                src = "https:" + src;
            } else if (src.startsWith("/")) {
                try {
                    okhttp3.HttpUrl base = okhttp3.HttpUrl.parse(baseUrl);
                    if (base != null) {
                        src = base.scheme() + "://" + base.host() + src;
                    }
                } catch (Exception ignored) {}
            }
            if (src.startsWith("http")) {
                links.add(src);
            }
        }
        return links;
    }
}