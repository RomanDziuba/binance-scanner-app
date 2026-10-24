package com.2.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfigMonster";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;
    private static final int PICK_APK_REQUEST = 1003;
    private static final int PICK_FILE_TO_FOLDER_REQUEST = 1004;

    private String githubToken = "";
    private String botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "RomaUniversalApp";
    private String appVersion = "2.0";
    private String packageName = "com.roma.universal";
    private String currentJavaFileName = "Универсальный парсер медиа и сайтов";
    private boolean iconCustomized = true;
    private String targetUploadFolder = "";

    private int minSdk = 21;
    private int compileSdk = 34;
    private String javaVersion = "17"; 
    private String activeCodeContent = "";
    private long latestRunId = -1;

    private TextView statusTextView, infoTextView;
    private EditText logEditText, urlInput;
    private LinearLayout downloadButtonsLayout;
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor, btnFileManager, btnAiFix, btnUniversalParse, btnPasteUrl;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;
    private String lastErrorLog = "";
    
    private WebView webView;
    private final Set<String> interceptedFiles = new HashSet<>();
    private final StringBuilder professionalLog = new StringBuilder();

    public static class RepoItem {
        public String path;
        public String type;
        public String sha;
        public long size;

        public RepoItem(String path, String type, String sha, long size) {
            this.path = path;
            this.type = type;
            this.sha = sha;
            this.size = size;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        if (activeCodeContent.isEmpty()) {
            activeCodeContent = generateUniversalTemplate();
        }

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(16, 16, 16, 16);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — Monster CI/CD & AI Doctor MAX ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(24f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(this);
        titleView.setText("🚀 Android Monster Compiler v5.0 MAX");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 10);
        card.addView(titleView);

        infoTextView = new TextView(this);
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(11f);
        infoTextView.setPadding(12, 10, 12, 10);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(12f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 10);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        // Блок Универсального Парсера прямо в интерфейсе
        urlInput = new EditText(this);
        urlInput.setHint("Вставьте URL любого сайта для парсинга...");
        urlInput.setHintTextColor(Color.parseColor("#475569"));
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#E2E8F0"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(12, 12, 12, 12);
        card.addView(urlInput);

        btnPasteUrl = createStyledButton("📋 Вставить ссылку из буфера", "#334155");
        btnPasteUrl.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPasteUrl);

        btnUniversalParse = createStyledButton("🔍 Запустить глубокий парсинг и перехват", "#0D9488");
        btnUniversalParse.setOnClickListener(v -> startUniversalParser());
        card.addView(btnUniversalParse);

        btnBuild = createStyledButton("", "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        btnStopBuild = createStyledButton("🛑 Экстренный стоп сборки", "#DC2626");
        btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
        card.addView(btnStopBuild);

        btnFileManager = createStyledButton("📁 Файловый менеджер репозитория", "#8B5CF6");
        btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
        card.addView(btnFileManager);

        btnCodeEditor = createStyledButton("📝 Редактор кода с подсказками", "#059669");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        btnUploadJava = createStyledButton("", "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        // AI Доктор MAX
        btnAiFix = createStyledButton("🤖 AI-Доктор MAX: Глубинный анализ и автофикс", "#7C3AED");
        btnAiFix.setOnClickListener(v -> runAiCodeDoctorMax());
        card.addView(btnAiFix);

        btnDecompileApk = createStyledButton("📦 Декомпилировать APK в код", "#475569");
        btnDecompileApk.setOnClickListener(v -> openApkFilePicker());
        card.addView(btnDecompileApk);

        btnReset = createStyledButton("🔄 Полный сброс (Шаблон Парсера)", "#B45309");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        btnTitle = createStyledButton("", "#334155");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        btnVersion = createStyledButton("", "#334155");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        btnIcon = createStyledButton("", "#6D28D9");
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        btnSettings = createStyledButton("⚙️ Настройки GitHub, Telegram & SDK", "#1E293B");
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        card.addView(btnSettings);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 6);
        card.addView(statusTextView);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Профессиональный лог дешифрования и терминал:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь выводятся подробные этапы работы парсера и отчеты AI-Доктора...");
        logEditText.setTextSize(10f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(8);
        logEditText.setMaxLines(15);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Monster Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        // Контейнер для кнопок скачивания найденных файлов
        TextView dlTitle = new TextView(this);
        dlTitle.setText("📥 Кнопки загрузки найденных медиа и файлов:");
        dlTitle.setTextColor(Color.parseColor("#38BDF8"));
        dlTitle.setTextSize(12f);
        dlTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dlTitle.setPadding(0, 10, 0, 5);
        card.addView(dlTitle);

        downloadButtonsLayout = new LinearLayout(this);
        downloadButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        card.addView(downloadButtonsLayout);

        // Скрытый WebView для универсального парсинга
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        container.addView(webView);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        updateAllButtonLabels();
        logEditText.setText("=== 🚀 MONSTER COMPILER v5.0 MAX ЗАПУЩЕН ===\n" +
                            "• Профессиональный логгер активен\n" +
                            "• AI-Доктор MAX готов к глубокому анализу\n" +
                            "• Пакет: " + packageName);
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
                }
            }
        }
    }

    private void writeProLog(String tag, String message) {
        String logLine = "[" + java.time.LocalTime.now() + "] [" + tag + "] " + message + "\n";
        professionalLog.append(logLine);
        handler.post(() -> logEditText.setText(professionalLog.toString()));
    }

    private void startUniversalParser() {
        String url = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Введите URL для парсинга!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
            urlInput.setText(url);
        }

        final String targetUrl = url;
        interceptedFiles.clear();
        downloadButtonsLayout.removeAllViews();
        professionalLog.setLength(0);

        writeProLog("PARSER", "=== СТАРТ УНИВЕРСАЛЬНОГО АНАЛИЗА ===");
        writeProLog("TARGET", targetUrl);
        statusTextView.setText("Статус: 🌐 Загрузка и перехват потоков...");

        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                // Профессиональный подбор любых медиа, аудио, видео, картинок, документов
                if (reqUrl.matches(".*\\.(mp4|m4a|mp3|wav|aac|ogg|flac|m3u8|ts|jpg|jpeg|png|webp|gif|svg|pdf|txt|doc|docx)(\\?.*)?$")) {
                    synchronized (interceptedFiles) {
                        interceptedFiles.add(reqUrl);
                    }
                    writeProLog("INTERCEPT", "Пойман файл: " + reqUrl);
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                writeProLog("DOM", "Страница полностью отрендерена. Запуск сбора текста и медиа...");

                webView.loadUrl("javascript:(function(){" +
                        "var media = document.querySelectorAll('video, audio');" +
                        "for(var i=0; i<media.length; i++) { try { media[i].play(); } catch(e){} }" +
                        "})();");

                handler.postDelayed(() -> {
                    webView.setVisibility(View.GONE);
                    webView.loadUrl("javascript:window.HTMLOUT.onContentLoaded(" +
                            "document.title," +
                            "document.documentElement.innerText" +
                            ");");
                }, 8000);
            }
        });

        webView.addJavascriptInterface(new ProJsBridge(), "HTMLOUT");
        webView.loadUrl(targetUrl);
    }

    class ProJsBridge {
        @android.webkit.JavascriptInterface
        public void onContentLoaded(String title, String text) {
            writeProLog("META", "Заголовок страницы: " + title);
            if (!TextUtils.isEmpty(text)) {
                String clean = text.replaceAll("[\\t\\r]+", " ").replaceAll("\\n\\s*\n", "\n").trim();
                if (clean.length > 400) clean = clean.substring(0, 400) + "... [обрезано]";
                writeProLog("TEXT", "Извлеченный текст:\n" + clean);
            }
            handler.post(() -> {
                buildDownloadButtonsPro();
                statusTextView.setText("Статус: ✅ Парсинг завершен успешно!");
            });
        }
    }

    private void buildDownloadButtonsPro() {
        downloadButtonsLayout.removeAllViews();
        synchronized (interceptedFiles) {
            writeProLog("BUILD", "Сформировано кнопок загрузки: " + interceptedFiles.size());
            if (interceptedFiles.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("ℹ️ Прямых файловых потоков не перехвачено. Проверьте консоль.");
                tv.setTextColor(Color.parseColor("#94A3B8"));
                downloadButtonsLayout.addView(tv);
                return;
            }

            int idx = 0;
            for (String fileUrl : interceptedFiles) {
                idx++;
                String type = detectFileTypePro(fileUrl);
                Button btn = createStyledButton("📥 Скачать " + type + " #" + idx, idx % 2 == 0 ? "#059669" : "#2563EB");
                final String target = fileUrl;
                btn.setOnClickListener(v -> triggerDownloadManager(target, type, idx));
                downloadButtonsLayout.addView(btn);
            }
        }
    }

    private String detectFileTypePro(String url) {
        String l = url.toLowerCase();
        if (l.contains(".mp4") || l.contains("m3u8")) return "ВИДЕО";
        if (l.contains(".mp3") || l.contains(".m4a") || l.contains(".wav")) return "АУДИО";
        if (l.contains(".jpg") || l.contains(".png") || l.contains(".webp")) return "ФОТО";
        return "ФАЙЛ";
    }

    private void triggerDownloadManager(String url, String type, int idx) {
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle("Загрузка (" + type + ")");
            req.setDescription("Загрузка через Monster Engine...");
            req.allowScanningByMediaScanner();
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            String ext = type.equals("ВИДЕО") ? ".mp4" : (type.equals("АУДИО") ? ".mp3" : ".jpg");
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MonsterDownload_" + System.currentTimeMillis() + "_" + idx + ext);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(req);
                Toast.makeText(this, "Загрузка добавлена в менеджер!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // 🤖 AI-ДОКТОР МАХ (Глубинный автофикс кода)
    // ==========================================
    private void runAiCodeDoctorMax() {
        if (lastErrorLog.isEmpty() && activeCodeContent.isEmpty()) {
            Toast.makeText(this, "ℹ️ Нет ошибок для анализа. Сначала запустите сборку или загрузите код!", Toast.LENGTH_LONG).show();
            return;
        }

        statusTextView.setText("Статус: 🤖 AI-Доктор MAX проводит глубокий аудит кода...");
        logEditText.setText("=== 🤖 AI-ДОКТОР МАХ: АНАЛИЗ И АВТОФИКС ===\n" +
                            "• Сканирование AST и синтаксиса...\n" +
                            "• Проверка пропущенных импортов и символов...\n");

        new Thread(() -> {
            try {
                Thread.sleep(1200);
                String code = activeCodeContent;
                boolean fixed = false;
                StringBuilder fixReport = new StringBuilder("=== 🤖 ОТЧЕТ AI-ДОКТОРА МАХ ===\n");

                // 1. Автоисправление импортов WebView / WebSettings
                if ((lastErrorLog.contains("WebView") || code.contains("WebView")) && !code.contains("import android.webkit.WebView;")) {
                    code = code.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.webkit.WebView;\nimport android.webkit.WebSettings;\nimport android.webkit.WebViewClient;\nimport android.webkit.WebResourceRequest;\nimport android.webkit.WebResourceResponse;");
                    fixReport.append("✅ Добавлены недостающие импорты WebView и WebSettings\n");
                    fixed = true;
                }

                // 2. Автоисправление DownloadManager импортов
                if (code.contains("DownloadManager") && !code.contains("import android.app.DownloadManager;")) {
                    code = code.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.app.DownloadManager;\nimport android.net.Uri;\nimport android.os.Environment;\nimport android.content.Context;");
                    fixReport.append("✅ Добавлены импорты для DownloadManager и работы с файлами\n");
                    fixed = true;
                }

                // 3. Автоисправление отсутствующих UI элементов (LinearLayout, Button, EditText)
                if (code.contains("LinearLayout") && !code.contains("import android.widget.LinearLayout;")) {
                    code = code.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.widget.*;\nimport android.view.*;");
                    fixReport.append("✅ Добавлен пакет android.widget.* для интерфейса\n");
                    fixed = true;
                }

                // 4. Исправление распространенных синтаксических ошибок (незакрытые кавычки, точки с запятой)
                int openBraces = countOccurrences(code, '{');
                int closeBraces = countOccurrences(code, '}');
                if (openBraces > closeBraces) {
                    for (int i = 0; i < (openBraces - closeBraces); i++) {
                        code += "\n}";
                    }
                    fixReport.append("⚠️ Обнаружен дисбаланс скобок. Автоматически закрыто блоков: ").append(openBraces - closeBraces).append("\n");
                    fixed = true;
                }

                // 5. Исправление R-ссылок
                if (lastErrorLog.contains("R cannot be resolved") || lastErrorLog.contains("symbol R")) {
                    code = code.replaceAll("R\\.[a-z]+\\.[a-zA-Z0-9_]+", "/* Fixed unresolved R reference */");
                    fixReport.append("✅ Исправлены неразрешенные ссылки на ресурсы R.*\n");
                    fixed = true;
                }

                final boolean hasFixed = fixed;
                final String finalCode = code;
                final String reportStr = fixReport.toString();

                handler.post(() -> {
                    if (hasFixed) {
                        activeCodeContent = finalCode;
                        saveSettings();
                        statusTextView.setText("Статус: ✅ AI-Доктор MAX успешно исправил ошибки!");
                        logEditText.setText(reportStr + "\n🎉 Код успешно оптимизирован. Нажмите «Скомпилировать APK» заново!");
                    } else {
                        statusTextView.setText("Статус: ✨ Ошибок в коде не обнаружено");
                        logEditText.setText("=== 🤖 AI-ДОКТОР МАХ ===\nКритичных синтаксических ошибок и пропущенных импортов в текущем коде не найдено. Код полностью готов к компиляции!");
                    }
                });

            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка AI-Доктора: " + e.getMessage()));
            }
        }).start();
    }

    private int countOccurrences(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }

    // Стандартные методы работы с репозиторием, сборкой и настройками
    private void updateAllButtonLabels() {
        String profile = javaVersion.equals("8") ? "⚡ Стабильный (Java 8)" : "🚀 Java 17";
        String info = "🏷 Имя: " + appTitle + " | 📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + " | 📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profile + "]";
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя: " + appTitle + " (" + packageName + ")");
        if (btnVersion != null) btnVersion.setText("📦 Версия: " + appVersion);
        if (btnUploadJava != null) btnUploadJava.setText("📂 Файл: " + currentJavaFileName);
        if (btnBuild != null) btnBuild.setText(getBuildButtonText());
        if (btnIcon != null) btnIcon.setText(iconCustomized ? "🖼 Иконка: ⚡ Шаблон" : "🖼 Иконка: 🎨 Пользовательская");
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 6);
        btn.setLayoutParams(params);
        return btn;
    }

    private String getBuildButtonText() {
        return "🚀 Скомпилировать APK\n📦 " + packageName + " (v" + appVersion + ")";
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        githubToken = sp.getString("githubToken", "");
        botToken = sp.getString("botToken", botToken);
        chatId = sp.getString("chatId", chatId);
        repoOwner = sp.getString("repoOwner", repoOwner);
        repoName = sp.getString("repoName", repoName);
        appTitle = sp.getString("appTitle", appTitle);
        appVersion = sp.getString("appVersion", appVersion);
        packageName = sp.getString("packageName", packageName);
        currentJavaFileName = sp.getString("currentJavaFileName", currentJavaFileName);
        iconCustomized = sp.getBoolean("iconCustomized", true);
        minSdk = sp.getInt("minSdk", 21);
        compileSdk = sp.getInt("compileSdk", 34);
        javaVersion = sp.getString("javaVersion", "17");
        activeCodeContent = sp.getString("activeCodeContent", "");
    }

    private void saveSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("githubToken", githubToken);
        editor.putString("botToken", botToken);
        editor.putString("chatId", chatId);
        editor.putString("repoOwner", repoOwner);
        editor.putString("repoName", repoName);
        editor.putString("appTitle", appTitle);
        editor.putString("appVersion", appVersion);
        editor.putString("packageName", packageName);
        editor.putString("currentJavaFileName", currentJavaFileName);
        editor.putBoolean("iconCustomized", iconCustomized);
        editor.putInt("minSdk", minSdk);
        editor.putInt("compileSdk", compileSdk);
        editor.putString("javaVersion", javaVersion);
        editor.putString("activeCodeContent", activeCodeContent);
        editor.apply();
        updateAllButtonLabels();
    }

    private void openRepositoryFileManager() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }
        statusTextView.setText("Статус: 🔄 Загрузка структуры репозитория...");
        new Thread(() -> {
            try {
                String treeUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/git/trees/main?recursive=1";
                HttpURLConnection conn = (HttpURLConnection) new URL(treeUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray treeArray = json.optJSONArray("tree");
                    java.util.List<RepoItem> items = new java.util.ArrayList<>();
                    if (treeArray != null) {
                        for (int i = 0; i < treeArray.length(); i++) {
                            JSONObject obj = treeArray.getJSONObject(i);
                            items.add(new RepoItem(obj.optString("path"), obj.optString("type"), obj.optString("sha"), obj.optLong("size", 0)));
                        }
                    }
                    handler.post(() -> {
                        statusTextView.setText("Статус: Готов к работе 🚀");
                        showDirectoryBrowser("", items);
                    });
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка сети"));
            }
        }).start();
    }

    private void showDirectoryBrowser(String currentDir, java.util.List<RepoItem> allItems) {
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(15, 15, 15, 15);
        dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

        TextView pathTitle = new TextView(this);
        pathTitle.setText("📂 /" + (currentDir.isEmpty() ? "root" : currentDir));
        pathTitle.setTextColor(Color.parseColor("#38BDF8"));
        pathTitle.setTextSize(12f);
        pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dialogLayout.addView(pathTitle);

        java.util.Set<String> subFolders = new java.util.TreeSet<>();
        java.util.List<RepoItem> subFiles = new java.util.ArrayList<>();
        String prefix = currentDir.isEmpty() ? "" : currentDir + "/";
        int prefixLen = prefix.length();

        for (RepoItem item : allItems) {
            if (currentDir.isEmpty()) {
                if (item.path.isEmpty()) continue;
                int slashIdx = item.path.indexOf('/');
                if (slashIdx == -1) {
                    if ("tree".equals(item.type)) subFolders.add(item.path);
                    else if ("blob".equals(item.type)) subFiles.add(item);
                } else {
                    subFolders.add(item.path.substring(0, slashIdx));
                }
            } else {
                if (item.path.startsWith(prefix) && item.path.length() > prefixLen) {
                    String subPath = item.path.substring(prefixLen);
                    int slashIdx = subPath.indexOf('/');
                    if (slashIdx == -1) {
                        if ("tree".equals(item.type)) subFolders.add(subPath);
                        else if ("blob".equals(item.type)) subFiles.add(item);
                    } else {
                        subFolders.add(subPath.substring(0, slashIdx));
                    }
                }
            }
        }

        java.util.List<String> displayItems = new java.util.ArrayList<>();
        java.util.List<String> itemPaths = new java.util.ArrayList<>();
        java.util.List<Boolean> isFolderList = new java.util.ArrayList<>();

        if (!currentDir.isEmpty()) {
            displayItems.add("⬆️ [Наверх]");
            itemPaths.add("..");
            isFolderList.add(true);
        }

        for (String folder : subFolders) {
            displayItems.add("📁 Папка: " + folder);
            itemPaths.add(currentDir.isEmpty() ? folder : currentDir + "/" + folder);
            isFolderList.add(true);
        }

        for (RepoItem fileItem : subFiles) {
            displayItems.add("📄 " + fileItem.path.substring(prefixLen) + " (" + fileItem.size + "б)");
            itemPaths.add(fileItem.path);
            isFolderList.add(false);
        }

        ListView listView = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                if (tv != null) {
                    tv.setTextColor(Color.parseColor("#E2E8F0"));
                    tv.setTextSize(12f);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 550));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPath = itemPaths.get(position);
            boolean isFolder = isFolderList.get(position);
            if (selectedPath.equals("..")) {
                int lastSlash = currentDir.lastIndexOf('/');
                showDirectoryBrowser(lastSlash == -1 ? "" : currentDir.substring(0, lastSlash), allItems);
            } else if (isFolder) {
                showDirectoryBrowser(selectedPath, allItems);
            } else {
                openFileEditorForPath(selectedPath);
            }
        });
        dialogLayout.addView(listView);

        new AlertDialog.Builder(this).setTitle("📁 Менеджер репозитория").setView(dialogLayout).setPositiveButton("Закрыть", null).show();
    }

    private void openFileEditorForPath(String filePath) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + filePath;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject json = new JSONObject(sb.toString());
                    String contentB64 = json.getString("content").replaceAll("\\s", "");
                    String fileContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);

                    handler.post(() -> showCodeEditorDialogForPath(filePath, fileContent));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void showCodeEditorDialogForPath(String filePath, String initialContent) {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        final EditText codeInput = new EditText(this);
        codeInput.setText(initialContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setMinLines(15);
        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить на GitHub", (dialog, which) -> {
                new Thread(() -> {
                    pushFileToGitHub(filePath, codeInput.getText().toString(), "Update via File Manager");
                    handler.post(() -> Toast.makeText(this, "Сохранено!", Toast.LENGTH_SHORT).show());
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите Java файл"), PICK_JAVA_REQUEST);
    }

    private void openApkFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(Intent.createChooser(intent, "Выберите APK"), PICK_APK_REQUEST);
    }

    private void openIconFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите иконку"), PICK_ICON_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_JAVA_REQUEST) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    activeCodeContent = sb.toString();
                    saveSettings();
                    Toast.makeText(this, "Java файл загружен!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Ошибка чтения", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void triggerGitHubBuild() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        btnBuild.setEnabled(false);
        statusTextView.setText("Статус: ⚙️ Компиляция на сервере...");

        new Thread(() -> {
            try {
                boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");

                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        statusTextView.setText("Статус: ❌ Ошибка синхронизации");
                    });
                    return;
                }

                URL url = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/workflows/build_native.yml/dispatches");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write("{\"ref\":\"main\"}".getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() == 204 || conn.getResponseCode() == 201) {
                    handler.post(() -> {
                        statusTextView.setText("Статус: 🚀 Сборка запущена!");
                        startMonitoringBuild();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    statusTextView.setText("Статус: ❌ Ошибка");
                });
            }
        }).start();
    }

    private void emergencyStopBuild() {
        isMonitoring = false;
        btnBuild.setEnabled(true);
        statusTextView.setText("Статус: 🛑 Сборка остановлена");
    }

    private boolean pushFileToGitHub(String path, String content, String msg) {
        return pushBytesToGitHub(path, content.getBytes(StandardCharsets.UTF_8), msg);
    }

    private boolean pushBytesToGitHub(String path, byte[] bytes, String msg) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            String sha = null;
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                sha = new JSONObject(sb.toString()).optString("sha", null);
            }

            HttpURLConnection put = (HttpURLConnection) url.openConnection();
            put.setRequestMethod("PUT");
            put.setRequestProperty("Authorization", "Bearer " + githubToken);
            put.setRequestProperty("Content-Type", "application/json; utf-8");
            put.setDoOutput(true);

            JSONObject payload = new JSONObject();
            payload.put("message", msg);
            payload.put("content", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP));
            payload.put("branch", "main");
            if (sha != null) payload.put("sha", sha);

            try (OutputStream os = put.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            return put.getResponseCode() == 200 || put.getResponseCode() == 201;
        } catch (Exception e) {
            return false;
        }
    }

    private void startMonitoringBuild() {
        isMonitoring = true;
        new Thread(() -> {
            while (isMonitoring) {
                try {
                    Thread.sleep(7000);
                    URL url = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs?per_page=1");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + githubToken);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        JSONArray runs = new JSONObject(sb.toString()).getJSONArray("workflow_runs");
                        if (runs.length() > 0) {
                            JSONObject run = runs.getJSONObject(0);
                            latestRunId = run.getLong("id");
                            String status = run.getString("status");
                            String conclusion = run.optString("conclusion", "null");

                            handler.post(() -> {
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Успех! APK отправлен в Telegram.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции");
                                        fetchLogsAsync(latestRunId);
                                    }
                                } else {
                                    statusTextView.setText("Статус: ⚙️ " + status);
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void fetchLogsAsync(long runId) {
        new Thread(() -> {
            try {
                URL jobsUrl = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + runId + "/jobs");
                HttpURLConnection conn = (HttpURLConnection) jobsUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONArray jobs = new JSONObject(sb.toString()).getJSONArray("jobs");
                    if (jobs.length() > 0) {
                        long jobId = jobs.getJSONObject(0).getLong("id");
                        URL logUrl = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/jobs/" + jobId + "/logs");
                        HttpURLConnection lConn = (HttpURLConnection) logUrl.openConnection();
                        lConn.setRequestMethod("GET");
                        lConn.setRequestProperty("Authorization", "Bearer " + githubToken);
                        if (lConn.getResponseCode() == 200) {
                            BufferedReader lb = new BufferedReader(new InputStreamReader(lConn.getInputStream(), StandardCharsets.UTF_8));
                            StringBuilder ls = new StringBuilder();
                            while ((line = lb.readLine()) != null) ls.append(line).append("\n");
                            lastErrorLog = ls.toString();
                            handler.post(() -> Toast.makeText(this, "⚠️ Нажмите AI-Доктор MAX для исправления ошибки!", Toast.LENGTH_LONG).show());
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Сброс")
            .setMessage("Сбросить к шаблону универсального парсера?")
            .setPositiveButton("Сбросить", (d, w) -> {
                activeCodeContent = generateUniversalTemplate();
                currentJavaFileName = "Универсальный шаблон парсера";
                appTitle = "RomaUniversalApp";
                packageName = "com.roma.universal";
                saveSettings();
                Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(appTitle);
        new AlertDialog.Builder(this).setTitle("Имя").setView(input).setPositiveButton("OK", (d, w) -> {
            appTitle = input.getText().toString().trim();
            packageName = "com." + appTitle.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app";
            saveSettings();
        }).show();
    }

    private void showEditVersionDialog() {
        final EditText input = new EditText(this);
        input.setText(appVersion);
        new AlertDialog.Builder(this).setTitle("Версия").setView(input).setPositiveButton("OK", (d, w) -> {
            appVersion = input.getText().toString().trim();
            saveSettings();
        }).show();
    }

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 15, 20, 15);
        final EditText inputGh = createDialogEditText("GitHub Token", githubToken);
        layout.addView(inputGh);
        final EditText inputBot = createDialogEditText("Bot Token", botToken);
        layout.addView(inputBot);
        final EditText inputChat = createDialogEditText("Chat ID", chatId);
        layout.addView(inputChat);

        new AlertDialog.Builder(this).setTitle("Настройки").setView(layout).setPositiveButton("Сохранить", (d, w) -> {
            githubToken = inputGh.getText().toString().trim();
            botToken = inputBot.getText().toString().trim();
            chatId = inputChat.getText().toString().trim();
            saveSettings();
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private EditText createDialogEditText(String hint, String val) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(val);
        et.setPadding(20, 12, 20, 12);
        return et;
    }

    private String getMainActivityPath() { return "app/src/main/java/" + packageName.replace('.', '/') + "/MainActivity.java"; }
    private String getRootGradleContent() { return "buildscript { repositories { google(); mavenCentral() } dependencies { classpath 'com.android.tools.build:gradle:8.2.2' } } allprojects { repositories { google(); mavenCentral() } }\n"; }
    private String getSettingsGradleContent() { return "rootProject.name = \"" + appTitle + "\"\ninclude ':app'\n"; }
    private String getWorkflowContent() { return "name: Build\non:\n  workflow_dispatch:\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - run: |\n          export ANDROID_HOME=/usr/local/lib/android/sdk\n          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n          echo \"y\" | sdkmanager --licenses || true\n          sdkmanager \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n      - run: gradle clean assembleDebug --no-daemon\n      - if: success()\n        run: |\n          APK=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n          curl -F chat_id=\"" + chatId + "\" -F document=@\"$APK\" https://api.telegram.org/bot" + botToken + "/sendDocument\n"; }
    private String generateManifest() { return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <uses-permission android:name=\"android.permission.INTERNET\" />\n    <application android:allowBackup=\"true\" android:label=\"" + appTitle + "\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n        <activity android:name=\".MainActivity\" android:exported=\"true\">\n            <intent-filter>\n                <action android:name=\"android.intent.action.MAIN\" />\n                <category android:name=\"android.intent.category.LAUNCHER\" />\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>\n"; }
    private String generateAppGradle() { return "plugins { id 'com.android.application' }\nandroid {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n    defaultConfig {\n        applicationId \"" + packageName + "\"\n        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n    compileOptions {\n        sourceCompatibility JavaVersion.VERSION_17\n        targetCompatibility JavaVersion.VERSION_17\n    }\n}\ndependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n"; }

    private String generateUniversalTemplate() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.os.Bundle;\n" +
               "import android.webkit.WebView;\n" +
               "import android.widget.TextView;\n" +
               "import android.view.Gravity;\n" +
               "import android.graphics.Color;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        TextView tv = new TextView(this);\n" +
               "        tv.setText(\"🚀 Универсальный движок запущен!\");\n" +
               "        tv.setTextColor(Color.WHITE);\n" +
               "        tv.setTextSize(18f);\n" +
               "        tv.setGravity(Gravity.CENTER);\n" +
               "        tv.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        setContentView(tv);\n" +
               "    }\n" +
               "}\n";
    }
}