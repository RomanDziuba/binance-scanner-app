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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnPasteUrl, btnCopyLogs;
    private Handler handler = new Handler(Looper.getMainLooper());

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
        authorView.setText("✦ Dziuba Roman — Multi-Stage Core Hunter ✦");
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
        titleView.setText("🛠️ Поэтапный глубокий анализ целей");
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

        btnDebug = createStyledButton("🚀 Запустить поэтапный пробой", "#2563EB");
        btnDebug.setOnClickListener(v -> startSequentialAnalysis());
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
        logLabel.setText("📋 Итоговый структурированный отчет:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится подробный отчет о проходе всех этапов...");
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
            ClipData clip = ClipData.newPlainText("Deep Analysis Report", logEditText.getText().toString());
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

    private void startSequentialAnalysis() {
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
        statusText.setText("Статус: [Этап 1/4] Разведка и обход редиректов...");
        logEditText.setText("=== 🕵️ МНОГОЭТАПНЫЙ ОТЧЕТ АНАЛИЗА ===\nИсходная цель: " + targetUrl + "\n\n");

        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("=== 🕵️ МНОГОЭТАПНЫЙ ОТЧЕТ АНАЛИЗА ===\n");
            report.append("Исходная цель: ").append(targetUrl).append("\n\n");

            try {
                // ==================== ЭТАП 1: Разведка и обработка редиректов ====================
                updateStatus("Статус: [Этап 1/4] Обработка редиректов и HTTP...");
                report.append("--- ЭТАП 1: Разведка и редиректы ---\n");
                
                String currentUrl = targetUrl;
                HttpURLConnection conn = null;
                int redirectCount = 0;
                int responseCode = -1;
                
                while (redirectCount < 5) {
                    try {
                        conn = (HttpURLConnection) new URL(currentUrl).openConnection();
                        conn.setRequestMethod("GET");
                        setupBrowserHeaders(conn);
                        conn.setInstanceFollowRedirects(false);
                        
                        responseCode = conn.getResponseCode();
                        report.append("• Шаг [").append(redirectCount + 1).append("] URL: ").append(currentUrl).append("\n");
                        report.append("  Код ответа: ").append(responseCode).append("\n");

                        if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                            String redirectUrl = conn.getHeaderField("Location");
                            report.append("  ↪️ Редирект на: ").append(redirectUrl).append("\n\n");
                            
                            if (redirectUrl == null) break;
                            
                            if (redirectUrl.startsWith("/")) {
                                URL base = new URL(currentUrl);
                                redirectUrl = base.getProtocol() + "://" + base.getHost() + redirectUrl;
                            } else if (!redirectUrl.startsWith("http")) {
                                URL base = new URL(currentUrl);
                                redirectUrl = new URL(base, redirectUrl).toString();
                            }
                            
                            currentUrl = redirectUrl;
                            redirectCount++;
                            conn.disconnect();
                        } else {
                            break;
                        }
                    } catch (Exception e) {
                        report.append("  ⚠️ Ошибка соединения на шаге: ").append(e.getMessage()).append("\n");
                        break;
                    }
                }

                report.append("• Финальный целевой URL: ").append(currentUrl).append("\n");
                
                // Пробуем получить данные с конечного сайта
                try {
                    conn = (HttpURLConnection) new URL(currentUrl).openConnection();
                    conn.setRequestMethod("GET");
                    setupBrowserHeaders(conn);
                    responseCode = conn.getResponseCode();
                    
                    report.append("• HTTP Статус финала: ").append(responseCode).append("\n");
                    report.append("• Content-Type: ").append(conn.getContentType()).append("\n");
                    report.append("• Сервер: ").append(conn.getHeaderField("Server")).append("\n\n");

                    if (responseCode != 200) {
                        report.append("❌ Сервер вернул код отличный от 200. Анализ страницы невозможен.\n");
                        finishAnalysis(report.toString());
                        return;
                    }

                    String pageContent = readStream(conn.getInputStream());
                    report.append("• Размер полученных данных: ").append(pageContent.length()).append(" символов\n");

                    Matcher titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(pageContent);
                    if (titleMatcher.find()) {
                        report.append("• Заголовок страницы (Title): ").append(titleMatcher.group(1).trim()).append("\n");
                    }
                    report.append("✅ Этап 1 завершен успешно.\n\n");

                    // ==================== ЭТАП 2: Поиск скрытых состояний и API ====================
                    updateStatus("Статус: [Этап 2/4] Сканирование API и скрытых JSON...");
                    report.append("--- ЭТАП 2: Поиск скрытых данных и API ---\n");

                    Matcher stateMatcher = Pattern.compile("(window\\.__[a-zA-Z0-9_]+__\\s*=\\s*\\{.+?\\});", Pattern.DOTALL).matcher(pageContent);
                    int statesFound = 0;
                    while (stateMatcher.find() && statesFound < 3) {
                        statesFound++;
                        String snippet = stateMatcher.group(1);
                        if (snippet.length() > 300) snippet = snippet.substring(0, 300) + "... [обрезано]";
                        report.append("🎯 Найдено состояние №").append(statesFound).append(":\n").append(snippet).append("\n\n");
                    }
                    if (statesFound == 0) {
                        report.append("ℹ️ Крупных JSON-состояний в HTML не обнаружено.\n");
                    }

                    Matcher apiMatcher = Pattern.compile("[\"']((?:https?://[^\"']+|/[a-zA-Z0-9_/\\-]+)?/api/v[0-9]+/[^\"']+)[\"']").matcher(pageContent);
                    int apiCount = 0;
                    report.append("Найденные API маршруты:\n");
                    while (apiMatcher.find()) {
                        apiCount++;
                        report.append("  [").append(apiCount).append("] ").append(apiMatcher.group(1)).append("\n");
                        if (apiCount >= 5) break;
                    }
                    if (apiCount == 0) report.append("  (явных API путей не найдено)\n");
                    report.append("\n✅ Этап 2 завершен.\n\n");

                    // ==================== ЭТАП 3: Погружение в скрипты (JS Deep Dive) ====================
                    updateStatus("Статус: [Этап 3/4] Скачивание и глубокий анализ .JS файлов...");
                    report.append("--- ЭТАП 3: Глубокий анализ JavaScript-файлов ---\n");

                    List<String> jsLinks = extractJsLinks(pageContent, currentUrl);
                    report.append("• Обнаружено внешних JS скриптов: ").append(jsLinks.size()).append("\n");

                    int analyzedJsCount = 0;
                    for (String jsUrl : jsLinks) {
                        if (analyzedJsCount >= 3) {
                            report.append("⚠️ Лимит анализа: проверены первые 3 JS файла.\n");
                            break;
                        }
                        analyzedJsCount++;
                        report.append("\n[Скрипт #").append(analyzedJsCount).append("] URL: ").append(jsUrl).append("\n");

                        try {
                            String jsText = fetchContentDirect(jsUrl);
                            report.append("  Размер JS: ").append(jsText.length()).append(" символов\n");

                            Matcher codeMatcher = Pattern.compile(".{0,80}(?:\"e:\"|'e:'|media_url|video_media_url|decodeUrl|decryptUrl|authorization|bearer|token|signature|AES|atob).{0,80}").matcher(jsText);
                            
                            int matchInJs = 0;
                            while (codeMatcher.find()) {
                                matchInJs++;
                                report.append("  🎯 Совпадение [").append(matchInJs).append("]: ").append(codeMatcher.group(0).trim()).append("\n");
                                if (matchInJs >= 5) {
                                    report.append("  ... (показано первые 5 совпадений в этом скрипте)\n");
                                    break;
                                }
                            }
                            if (matchInJs == 0) {
                                report.append("  (искомых сигнатур декодирования/токенов не обнаружено)\n");
                            }

                        } catch (Exception e) {
                            report.append("  ❌ Ошибка загрузки JS: ").append(e.getMessage()).append("\n");
                        }
                    }
                    report.append("\n✅ Этап 3 завершен.\n\n");

                    // ==================== ЭТАП 4: Финальный отчет ====================
                    updateStatus("Статус: [Этап 4/4] Синтез отчета...");
                    report.append("--- ЭТАП 4: Итоговый вердикт ---\n");
                    report.append("🎉 Полный цикл глубокой разведки успешно завершен!\n");

                } catch (Exception e) {
                    report.append("\n🛡️ ЗАЩИТА СЕРВЕРА (Connection reset / Blocked):\n")
                          .append("Конечный ресурс (например, mil.ru) принудительно сбросил TCP-соединение из-за систем защиты от ботов/парсер-фильтров.\n")
                          .append("Техническая ошибка: ").append(e.getMessage()).append("\n");
                }

            } catch (Exception e) {
                report.append("\n❌ КРИТИЧЕСКАЯ ОШИБКА НА КОНВЕЙЕРЕ: ").append(e.toString());
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

    private String readStream(java.io.InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private String fetchContentDirect(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        setupBrowserHeaders(conn);
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP Код: " + conn.getResponseCode());
        }
        return readStream(conn.getInputStream());
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
                    URL base = new URL(baseUrl);
                    src = base.getProtocol() + "://" + base.getHost() + src;
                } catch (Exception ignored) {}
            }
            if (src.startsWith("http")) {
                links.add(src);
            }
        }
        return links;
    }

    private void setupBrowserHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
    }
}