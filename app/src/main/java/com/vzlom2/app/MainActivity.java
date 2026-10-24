package com.vzlom2.app;

import android.app.Activity;
import android.app.AlertDialog;
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
    private Button btnDebug, btnBrowser, btnPasteUrl, btnCopyLogs;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private final Set<String> interceptedMediaUrls = new HashSet<>();

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
        authorView.setText("✦ Dziuba Roman — Smule Ultimate Downloader ✦");
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
        titleView.setText("🔥 Скачивание треков Smule");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        urlInput = new EditText(this);
        urlInput.setHint("Введите или вставьте ссылку на запись...");
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

        // Кнопка ручного входа / браузера
        btnBrowser = createStyledButton("🌐 Открыть сайт / Войти в аккаунт", "#7C3AED");
        btnBrowser.setOnClickListener(v -> showBrowserDialog());
        card.addView(btnBrowser);

        btnDebug = createStyledButton("🚀 Извлечь прямую ссылку для скачивания", "#2563EB");
        btnDebug.setOnClickListener(v -> startExtraction());
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
        logLabel.setText("📋 Прямые ссылки для скачивания (.mp4 / .m4a):");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появятся прямые ссылки на файл песни...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(12);
        logEditText.setMaxLines(20);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать прямую ссылку", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Direct Media Link", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Ссылка скопирована в буфер!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        // Инициализируем WebView для фоновой работы и перехвата
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36");
        
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

    // Открытие интерактивного браузера/входа в диалоговом окне
    private void showBrowserDialog() {
        LinearLayout browserLayout = new LinearLayout(this);
        browserLayout.setOrientation(LinearLayout.VERTICAL);
        browserLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1200
        ));

        WebView dialogWebView = new WebView(this);
        dialogWebView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));
        WebSettings ws = dialogWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36");
        
        dialogWebView.setWebViewClient(new WebViewClient());
        dialogWebView.loadUrl("https://www.smule.com/login");

        browserLayout.addView(dialogWebView);

        new AlertDialog.Builder(this)
            .setTitle("🌐 Браузер / Вход в аккаунт Smule")
            .setView(browserLayout)
            .setPositiveButton("Закрыть", (dialog, which) -> dialogWebView.destroy())
            .show();
    }

    private void startExtraction() {
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

        btnDebug.setEnabled(false);
        statusText.setText("Статус: 🌐 Загрузка и обход защиты...");
        logEditText.setText("Идет поиск прямой ссылки для скачивания...\nПожалуйста, подождите до 15 секунд...\n\n");

        webView.setVisibility(View.VISIBLE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                
                // Ловим медиафайлы .mp4, .m4a, .mp3
                if (reqUrl.contains(".m4a") || reqUrl.contains(".mp4") || reqUrl.contains(".mp3")) {
                    synchronized (interceptedMediaUrls) {
                        interceptedMediaUrls.add(reqUrl);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Пытаемся запустить автовоспроизведение медиа для вызова сетевого запроса
                webView.loadUrl("javascript:(function(){" +
                        "var playBtns = document.querySelectorAll('button, [class*=\"play\"], [class*=\"Play\"]');" +
                        "for(var i=0; i<playBtns.length; i++) { try { playBtns[i].click(); } catch(e){} }" +
                        "var audios = document.querySelectorAll('audio, video');" +
                        "for(var j=0; j<audios.length; j++) { try { audios[j].play(); } catch(e){} }" +
                        "})();");

                statusText.setText("Статус: ⏳ Ожидание перехвата медиафайла (12 сек)...");
                
                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    displayExtractedLinks();
                }, 12000);
            }
        });

        webView.loadUrl(targetUrl);
    }

    private void displayExtractedLinks() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 🎯 ПРЯМЫЕ ССЫЛКИ ДЛЯ СКАЧИВАНИЯ ===\n\n");

        synchronized (interceptedMediaUrls) {
            if (!interceptedMediaUrls.isEmpty()) {
                int count = 0;
                for (String link : interceptedMediaUrls) {
                    count++;
                    sb.append("Ссылка №").append(count).append(":\n").append(link).append("\n\n");
                }
            } else {
                sb.append("⚠️ Прямая ссылка не поймана в сети. Убедитесь, что ссылка ведет на конкретную запись песни, или войдите в аккаунт через кнопку браузера выше.");
            }
        }

        btnDebug.setEnabled(true);
        statusText.setText("Статус: ✅ Готово!");
        logEditText.setText(sb.toString());
    }
}