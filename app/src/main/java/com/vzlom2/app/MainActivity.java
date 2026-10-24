package com.vzlom2.app;

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
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnPasteUrl, btnCopyLogs;
    private LinearLayout downloadButtonsLayout;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private final Set<String> interceptedMediaUrls = new HashSet<>();
    private final StringBuilder debugLogBuilder = new StringBuilder();

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
        authorView.setText("✦ Dziuba Roman — Universal Media Extractor ✦");
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
        titleView.setText("🔥 Всеядный парсер контента & Кнопки загрузки");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setHint("Вставьте ссылку на страницу...");
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

        btnDebug = createStyledButton("🚀 Запустить глубокий парсинг и дешифровку", "#2563EB");
        btnDebug.setOnClickListener(v -> startFullExtraction());
        card.addView(btnDebug);

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
        logLabel.setText("📋 Консоль отладки этапов дешифрования:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь будет полная техническая картина отладки...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(10);
        logEditText.setMaxLines(16);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог отладки", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Debug Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы в буфер!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        // Контейнер для динамических кнопок скачивания
        TextView dlLabel = new TextView(this);
        dlLabel.setText("📥 Кнопки для скачивания файлов в телефон:");
        dlLabel.setTextColor(Color.parseColor("#38BDF8"));
        dlLabel.setTextSize(12f);
        dlLabel.setTypeface(Typeface.DEFAULT_BOLD);
        dlLabel.setPadding(0, 15, 0, 5);
        card.addView(dlLabel);

        downloadButtonsLayout = new LinearLayout(this);
        downloadButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        card.addView(downloadButtonsLayout);

        // Фоновый WebView
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        
        container.addView(webView);
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

    private void startFullExtraction() {
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
        interceptedMediaUrls.clear();
        downloadButtonsLayout.removeAllViews();
        debugLogBuilder.setLength(0);

        appendLog("=== [ЭТАП 1] ИНИЦИАЦИЯ ПАРСИНГА ===");
        appendLog("Цель: " + targetUrl);
        appendLog("Настройка скрытого WebView и обход защит...\n");

        btnDebug.setEnabled(false);
        statusText.setText("Статус: [Этап 2] Загрузка и обход Cloudflare...");

        webView.setVisibility(View.VISIBLE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                
                // Всеядный перехватчик: видео, звуки, изображения любых форматов
                if (reqUrl.matches(".*\\.(mp4|m4a|mp3|wav|aac|ogg|flac|jpg|jpeg|png|webp|gif|svg|txt)(\\?.*)?$") ||
                    reqUrl.contains("performance") || reqUrl.contains("renvideo") || reqUrl.contains("cdn")) {
                    
                    synchronized (interceptedMediaUrls) {
                        if (reqUrl.matches(".*\\.(mp4|m4a|mp3|wav|aac|ogg|flac|jpg|jpeg|png|webp|gif|svg|txt)(\\?.*)?$")) {
                            interceptedMediaUrls.add(reqUrl);
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                appendLog("=== [ЭТАП 3] СТРАНИЦА ЗАГРУЖЕНА ===");
                appendLog("Финальный URL: " + url);
                appendLog("Запуск эмуляции плеера для инициации сетевых потоков...\n");

                statusText.setText("Статус: [Этап 4] Сбор медиа и текстов...");

                // Эмулируем запуск аудио/видео
                webView.loadUrl("javascript:(function(){" +
                        "var playBtns = document.querySelectorAll('button, [class*=\"play\"], [class*=\"Play\"]');" +
                        "for(var i=0; i<playBtns.length; i++) { try { playBtns[i].click(); } catch(e){} }" +
                        "var audios = document.querySelectorAll('audio, video');" +
                        "for(var j=0; j<audios.length; j++) { try { audios[j].play(); } catch(e){} }" +
                        "})();");

                // Даем время на сбор потоков и парсим текст (без кракозябров)
                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    
                    webView.loadUrl("javascript:window.HTMLOUT.processContent(" +
                            "document.title," +
                            "document.documentElement.innerText" +
                            ");");
                }, 12000);
            }
        });

        webView.addJavascriptInterface(new ContentJavaScriptInterface(), "HTMLOUT");
        webView.loadUrl(targetUrl);
    }

    class ContentJavaScriptInterface {
        @android.webkit.JavascriptInterface
        public void processContent(String title, String cleanText) {
            appendLog("=== [ЭТАП 5] ДЕШИФРОВКА И АНАЛИЗ КОНТЕНТА ===");
            appendLog("Заголовок страницы: " + title);
            
            // Чистый текст без кракозябров (фильтруем служебный мусор)
            if (!TextUtils.isEmpty(cleanText)) {
                String normalizedText = cleanText.replaceAll("[\\t\\r]+", " ").replaceAll("\\n\\s*\n", "\n").trim();
                if (normalizedText.length > 500) normalizedText = normalizedText.substring(0, 500) + "... [обрезано]";
                appendLog("\n📝 [Извлеченный текст/описание]:\n" + normalizedText + "\n");
            }

            handler.post(() -> {
                buildDownloadButtons();
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Успешно завершено!");
                logEditText.setText(debugLogBuilder.toString());
            });
        }
    }

    private void appendLog(String message) {
        debugLogBuilder.append(message).append("\n");
    }

    private void buildDownloadButtons() {
        downloadButtonsLayout.removeAllViews();

        synchronized (interceptedMediaUrls) {
            appendLog("=== [ЭТАП 6] ФОРМИРОВАНИЕ КНОПОК СКАЧИВАНИЯ ===");
            appendLog("Найдено уникальных файлов: " + interceptedMediaUrls.size() + "\n");

            if (interceptedMediaUrls.isEmpty()) {
                TextView noFiles = new TextView(this);
                noFiles.setText("⚠️ Медиафайлы не перехвачены. Убедитесь, что ссылка ведет на конкретную запись.");
                noFiles.setTextColor(Color.parseColor("#94A3B8"));
                noFiles.setTextSize(11f);
                downloadButtonsLayout.addView(noFiles);
                return;
            }

            int index = 0;
            for (String fileUrl : interceptedMediaUrls) {
                index++;
                String fileType = getFileType(fileUrl);
                String buttonTitle = "📥 Скачать " + fileType + " #" + index;

                // Создаем интерактивную кнопку для каждого файла
                Button dlBtn = createStyledButton(buttonTitle, index % 2 == 0 ? "#059669" : "#2563EB");
                
                final String targetUrl = fileUrl;
                dlBtn.setOnClickListener(v -> downloadFileToPhone(targetUrl, fileType, index));
                
                downloadButtonsLayout.addView(dlBtn);
                appendLog("👉 Создана кнопка для: " + fileUrl);
            }
        }
    }

    private String getFileType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".mp4") || lower.contains("renvideo")) return "ВИДЕО (.mp4)";
        if (lower.contains(".m4a")) return "АУДИО (.m4a)";
        if (lower.contains(".mp3")) return "АУДИО (.mp3)";
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return "ФОТО (.jpg)";
        if (lower.contains(".png")) return "ФОТО (.png)";
        if (lower.contains(".webp")) return "ФОТО (.webp)";
        return "ФАЙЛ";
    }

    private void downloadFileToPhone(String url, String fileType, int index) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Загрузка Smule " + fileType);
            request.setDescription("Скачивание файла на телефон...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            String extension = fileType.contains("mp4") ? ".mp4" : (fileType.contains("m4a") ? ".m4a" : ".jpg");
            String fileName = "SmuleMedia_" + System.currentTimeMillis() + "_" + index + extension;
            
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Загрузка началась! Проверьте шторку уведомлений.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка скачивания: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}