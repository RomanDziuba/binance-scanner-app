package com.vzlom.app;

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
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText;
    private Button btnDebug, btnPasteUrl, btnCopyLogs;
    private WebView hiddenWebView;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private final Set<String> interceptedMediaUrls = new HashSet<>();
    private final List<String> networkLogs = new ArrayList<>();

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
        authorView.setText("✦ Dziuba Roman — Smule Full Debugger v4.1 ✦");
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
        titleView.setText("🔍 Обход Cloudflare & Перехват треков");
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

        btnDebug = createStyledButton("🚀 Запустить отладку и перехват", "#2563EB");
        btnDebug.setOnClickListener(v -> startFullDebugExtraction());
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
        logLabel.setText("📋 Полная консоль отладки и лог:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится вся детальная картина и перехваченные ссылки...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(16);
        logEditText.setMaxLines(24);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать полный лог", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Full Debug Log", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Лог отладки скопирован!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        hiddenWebView = new WebView(this);
        hiddenWebView.setVisibility(View.GONE);
        WebSettings webSettings = hiddenWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36");
        
        hiddenWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        container.addView(hiddenWebView);

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

    private void startFullDebugExtraction() {
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
        networkLogs.clear();

        btnDebug.setEnabled(false);
        statusText.setText("Статус: 🌐 Обход Cloudflare и загрузка страницы...");
        logEditText.setText("=== 🔍 FULL DEBUG CONSOLE ===\nЦель: " + targetUrl + "\nЖдем прохождение защиты и запуск плеера...\n\n");

        hiddenWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                
                // Перехватываем все медиа и api запросы Smule
                if (reqUrl.contains(".m4a") || reqUrl.contains(".mp4") || reqUrl.contains(".mp3") || reqUrl.contains("performance") || reqUrl.contains("recording")) {
                    synchronized (interceptedMediaUrls) {
                        if (reqUrl.contains(".m4a") || reqUrl.contains(".mp4") || reqUrl.contains(".mp3")) {
                            interceptedMediaUrls.add(reqUrl);
                        }
                    }
                    synchronized (networkLogs) {
                        if (networkLogs.size() < 30) {
                            networkLogs.add("🔥 [Media/API Intercept]: " + reqUrl);
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Автоматически кликаем по кнопкам play и запускаем аудио/видео элементы
                hiddenWebView.loadUrl("javascript:(function(){" +
                        "console.log('Auto-clicking play...');" +
                        "var playBtns = document.querySelectorAll('button, [class*=\"play\"], [class*=\"Play\"], [aria-label*=\"play\"], [aria-label*=\"Play\"]');" +
                        "for(var i=0; i<playBtns.length; i++) { try { playBtns[i].click(); } catch(e){} }" +
                        "var audios = document.querySelectorAll('audio, video');" +
                        "for(var j=0; j<audios.length; j++) { try { audios[j].play(); } catch(e){} }" +
                        "})();");

                statusText.setText("Статус: ⏳ Ожидание Cloudflare и воспроизведения (9 сек)...");
                
                // Увеличили задержку до 9 секунд, чтобы дать время пройти Cloudflare и подгрузить медиа
                handler.postDelayed(() -> {
                    hiddenWebView.loadUrl("javascript:window.HTMLOUT.processHTML(" +
                            "document.getElementsByTagName('html')[0].innerHTML," +
                            "document.title" +
                            ");");
                }, 9000);
            }
        });

        hiddenWebView.loadUrl(targetUrl);
    }

    class MyJavaScriptInterface {
        @JavascriptInterface
        public void processHTML(String html, String title) {
            StringBuilder report = new StringBuilder();
            report.append("=== 🔍 FULL DEBUG CONSOLE REPORT ===\n");
            report.append("📌 Заголовок: ").append(title).append("\n");
            report.append("• Размер HTML: ").append(html.length()).append(" символов\n\n");

            report.append("--- 📡 1. ПЕРЕХВАТЕННЫЕ СЕТЕВЫЕ ЗАПРОСЫ (Network Intercept) ---\n");
            synchronized (interceptedMediaUrls) {
                if (!interceptedMediaUrls.isEmpty()) {
                    int n = 0;
                    for (String mediaUrl : interceptedMediaUrls) {
                        n++;
                        report.append("🎯 [Сетевой медиа-поток #").append(n).append("]:\n").append(mediaUrl).append("\n\n");
                    }
                } else {
                    report.append("ℹ️ Сетевым перехватчиком файлы .m4a/.mp4 еще не пойманы (возможно, требуется больше времени).\n\n");
                }
            }

            report.append("--- 🏷️ 2. META ТЕГИ СТРАНИЦЫ ---\n");
            Matcher metaMatcher = Pattern.compile("<meta\\s+(?:name|property)=[\"']([^\"']+)[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
            int metaCount = 0;
            while (metaMatcher.find()) {
                String prop = metaMatcher.group(1);
                String val = metaMatcher.group(2);
                if (prop.contains("og:") || prop.contains("twitter:") || prop.contains("video") || prop.contains("audio")) {
                    metaCount++;
                    report.append("• ").append(prop).append(" = ").append(val).append("\n");
                }
            }
            if (metaCount == 0) report.append("ℹ️ Специфических og/twitter meta-тегов не найдено.\n");
            report.append("\n");

            report.append("--- 📦 3. JSON СОСТОЯНИЯ (window.__NEXT_DATA__ / Performance) ---\n");
            Matcher nextData = Pattern.compile("<script id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>", Pattern.DOTALL).matcher(html);
            if (nextData.find()) {
                String json = nextData.group(1);
                report.append("✅ Найден __NEXT_DATA__! Длина: ").append(json.length()).append(" символов.\n");
                
                Matcher mUrl = Pattern.compile("[\"'](https?://[^\"']+\\.(?:m4a|mp4|mp3)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(json);
                int count = 0;
                while (mUrl.find()) {
                    count++;
                    report.append("🔥 [NEXT_DATA Media #").append(count).append("]: ").append(mUrl.group(1).replace("\\/", "/")).append("\n");
                }
                if (count == 0) report.append("ℹ️ Внутри __NEXT_DATA__ прямых ссылок .m4a/.mp4 не обнаружено.\n");
            } else {
                report.append("ℹ️ Тег __NEXT_DATA__ на странице отсутствует.\n");
            }
            report.append("\n");

            report.append("--- 🌐 4. ЛОГ СЕТЕВЫХ ОБРАЩЕНИЙ WIDGET/CDN ---\n");
            synchronized (networkLogs) {
                if (!networkLogs.isEmpty()) {
                    for (String log : networkLogs) {
                        report.append(log).append("\n");
                    }
                } else {
                    report.append("ℹ️ Сетевых вызовов к CDN Smule не зафиксировано.\n");
                }
            }

            handler.post(() -> {
                btnDebug.setEnabled(true);
                statusText.setText("Статус: ✅ Полный отладочный лог собран!");
                logEditText.setText(report.toString());
            });
        }
    }
}