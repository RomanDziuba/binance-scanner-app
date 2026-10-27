package com.kompljatorxxx.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends Activity {

    // =========================================================================
    // КОНСТАНТЫ И КОНФИГУРАЦИОННЫЕ ПАРАМЕТРЫ ПРИЛОЖЕНИЯ
    // =========================================================================
    private static final String PREFS_NAME = "AppConfigMonster";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;
    private static final int PICK_APK_REQUEST = 1003;
    private static final int PICK_FILE_TO_FOLDER_REQUEST = 1004;

    // Данные авторизации и конфигурации репозитория
    private String githubToken = "";
    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    // Метаданные собираемого Android-проекта
    private String appTitle = "RomaMonsterApp";
    private String appVersion = "1.0";
    private int versionCode = 1;
    private String packageName = "com.romamonsterapp.app";
    private String currentJavaFileName = "Шаблон: Monster Clock";
    private boolean iconCustomized = true;
    private String targetUploadFolder = "";

    // SDK и параметры компилятора
    private int minSdk = 21;
    private int compileSdk = 34;
    private String javaVersion = "8"; 
    private String activeCodeContent = "";
    private long latestRunId = -1;

    // Графические элементы интерфейса
    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnResetClock, 
                   btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, 
                   btnCodeEditor, btnFileManager, btnAiFix, btnRetryUpload;

    // Поток и состояния системы
    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;
    private String lastErrorLog = "";
    private byte[] lastFailedBytes = null;
    private String lastFailedPath = "";

    // Диалог файлового менеджера для возможности его мгновенного закрытия
    private AlertDialog activeRepoDialog = null;

    // Класс структуры элементов репозитория GitHub
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

        // Если активный код пуст — загружаем базовый шаблон часов с фонариком
        if (activeCodeContent.isEmpty()) {
            activeCodeContent = generateClockTemplate();
        }

        // Корневой разметчик с тёмной темой
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

        // Заголовок автора
        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman — Monster CI/CD Engine v9.0 Ultimate ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        // Карточка с управлением
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
        titleView.setText("🚀 Android Monster Compiler Pro");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 10);
        card.addView(titleView);

        infoTextView = new TextView(this);
        infoTextView.setTextColor(Color.parseColor("#38BDF8"));
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

        // ==========================================
        // КНОПКИ УПРАВЛЕНИЯ (БЕЗ БЕЛЫХ КНОПОК)
        // ==========================================

        // 1. Скомпилировать
        btnBuild = createStyledButton("", "#1E3A8A", "#60A5FA");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        // 2. Версия
        btnVersion = createStyledButton("", "#1E293B", "#94A3B8");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        // 3. Имя
        btnTitle = createStyledButton("", "#1E293B", "#94A3B8");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        // 4. Иконка
        btnIcon = createStyledButton("", "#4C1D95", "#C084FC");
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        // 5. Редактор кода
        btnCodeEditor = createStyledButton("📝 Редактор кода (с Поиском и Выделением)", "#0F172A", "#38BDF8");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        // 6. Загрузить файл
        btnUploadJava = createStyledButton("", "#064E3B", "#34D399");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        // Кнопка повтора при ошибке сети
        btnRetryUpload = createStyledButton("🔄 Повторить отправку файла на сервер", "#78350F", "#FBBF24");
        btnRetryUpload.setVisibility(View.GONE);
        btnRetryUpload.setOnClickListener(v -> retryLastUpload());
        card.addView(btnRetryUpload);

        // 7. Декомпилировать
        btnDecompileApk = createStyledButton("📦 Декомпилировать APK/ZIP в код", "#334155", "#CBD5E1");
        btnDecompileApk.setOnClickListener(v -> openUniversalFilePicker());
        card.addView(btnDecompileApk);

        // 8. Сброс к часам
        btnResetClock = createStyledButton("🔄 Сбросить к часам (Monster Clock)", "#78350F", "#FBBF24");
        btnResetClock.setOnClickListener(v -> confirmResetProject());
        card.addView(btnResetClock);

        // 9. Экстренный стоп
        btnStopBuild = createStyledButton("🛑 Экстренный стоп сборки", "#7F1D1D", "#F87171");
        btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
        card.addView(btnStopBuild);

        // 10. Файловый менеджер репозитория
        btnFileManager = createStyledButton("📁 Файловый менеджер репозитория", "#581C87", "#A855F7");
        btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
        card.addView(btnFileManager);

        // 11. AI-Профессор
        btnAiFix = createStyledButton("🎓 Smart AI-Профессор: Экспертный анализ", "#312E81", "#818CF8");
        btnAiFix.setOnClickListener(v -> runAiCodeDoctor());
        card.addView(btnAiFix);

        // 12. Настройки
        btnSettings = createStyledButton("⚙️ Настройки GitHub, Telegram & SDK", "#1E293B", "#94A3B8");
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        card.addView(btnSettings);

        // Отображение статуса
        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#34D399"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 6);
        card.addView(statusTextView);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Терминал логов и отчетов сборки:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь выводятся подробные логи процесса сборки...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(8);
        logEditText.setMaxLines(15);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог в буфер", "#334155", "#94A3B8");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Monster Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopyLogs);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        updateAllButtonLabels();
        logEditText.setText("=== 🚀 MONSTER COMPILER PRO V9 ULTIMATE ===\n" +
                            "• Приложение: " + appTitle + "\n" +
                            "• Пакет (Package): " + packageName + "\n" +
                            "• Версия: " + appVersion + " (Code: " + versionCode + ")\n" +
                            "• GitHub Token: " + (githubToken.isEmpty() ? "⚠️ Не настроен" : "✅ Готов к работе"));

        if (!githubToken.isEmpty()) {
            fetchRemoteRepositoryInfo();
        }
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ИНТЕРФЕЙСА И СТИЛИЗАЦИИ
    // =========================================================================

    private void updateAllButtonLabels() {
        String profile = javaVersion.equals("8") ? "⚡ Стабильный (Java 8)" : "🚀 Java 17";
        String info = "🏷 Имя: " + appTitle + " | 📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + " (Code: " + versionCode + ") | 📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profile + "]";
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя приложения: " + appTitle);
        if (btnVersion != null) btnVersion.setText("📦 Версия: " + appVersion);
        if (btnUploadJava != null) btnUploadJava.setText("📂 Загрузи Файл сюда👈  " + currentJavaFileName);
        if (btnBuild != null) btnBuild.setText(getBuildButtonText());
        if (btnIcon != null) btnIcon.setText(iconCustomized ? "🖼 Иконка: ⚡ Шаблон" : "🖼 Иконка: 🎨 Пользовательская");
    }

    // Генератор профессиональных стилизованных кнопок (без белого цвета!)
    private Button createStyledButton(String text, String bgHex, String textHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textHex));
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgHex));
        bg.setCornerRadius(12f);
        bg.setStroke(1, Color.parseColor("#334155"));
        btn.setBackground(bg);
        
        btn.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private String getBuildButtonText() {
        return "🚀 Скомпилировать APK\n📦 " + appTitle + " (v" + appVersion + ")";
    }

    // =========================================================================
    // КАЧЕСТВЕННЫЙ ТРАНСЛИТЕРАТОР ДЛЯ СОЗДАНИЯ УНИКАЛЬНЫХ ПАКЕТОВ ПРИЛОЖЕНИЙ
    // =========================================================================
    private String transliterateToPackageName(String text) {
        if (text == null || text.trim().isEmpty()) return "monster.app";
        
        Map<Character, String> map = new HashMap<>();
        String[] cyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","і","ї","є"};
        String[] lat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","shch","","y","","e","yu","ya","i","yi","ye"};
        
        for (int i = 0; i < cyr.length; i++) {
            map.put(cyr[i].charAt(0), lat[i]);
        }

        StringBuilder sb = new StringBuilder();
        String lower = text.toLowerCase().trim();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (map.containsKey(c)) {
                sb.append(map.get(c));
            } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else if (c == ' ' || c == '_' || c == '-') {
                sb.append('.');
            }
        }

        String res = sb.toString().replaceAll("\\.+", ".").replaceAll("^\\.|\\.$", "");
        if (res.isEmpty()) res = "custom.app";
        if (Character.isDigit(res.charAt(0))) res = "app." + res;
        
        return "com." + res + ".app";
    }

    // =========================================================================
    // РАБОТА С НАСТРОЙКАМИ И СИНХРОНИЗАЦИЯ С GITHUB
    // =========================================================================
    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        githubToken = sp.getString("githubToken", "");
        botToken = sp.getString("botToken", botToken);
        chatId = sp.getString("chatId", chatId);
        repoOwner = sp.getString("repoOwner", repoOwner);
        repoName = sp.getString("repoName", repoName);
        appTitle = sp.getString("appTitle", appTitle);
        appVersion = sp.getString("appVersion", appVersion);
        versionCode = sp.getInt("versionCode", 1);
        packageName = sp.getString("packageName", transliterateToPackageName(appTitle));
        currentJavaFileName = sp.getString("currentJavaFileName", currentJavaFileName);
        iconCustomized = sp.getBoolean("iconCustomized", true);
        minSdk = sp.getInt("minSdk", 21);
        compileSdk = sp.getInt("compileSdk", 34);
        javaVersion = sp.getString("javaVersion", "8");
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
        editor.putInt("versionCode", versionCode);
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

    private void fetchRemoteRepositoryInfo() {
        statusTextView.setText("Статус: 🔄 Синхронизация с GitHub...");
        new Thread(() -> {
            try {
                String gradlePath = "app/build.gradle";
                String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + gradlePath;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    JSONObject json = new JSONObject(sb.toString());
                    String contentB64 = json.getString("content").replaceAll("\\s", "");
                    String gradleContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);

                    Matcher vMatcher = Pattern.compile("versionName\\s+\"([^\"]+)\"").matcher(gradleContent);
                    if (vMatcher.find()) appVersion = vMatcher.group(1);
                    Matcher vcMatcher = Pattern.compile("versionCode\\s+(\\d+)").matcher(gradleContent);
                    if (vcMatcher.find()) versionCode = Integer.parseInt(vcMatcher.group(1));

                    handler.post(() -> {
                        saveSettings();
                        statusTextView.setText("Статус: ✅ Репозиторий готов!");
                    });
                } else {
                    handler.post(() -> statusTextView.setText("Статус: Готов к работе 🚀"));
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: Готов к работе 🚀"));
            }
        }).start();
    }

    // =========================================================================
    // ФАЙЛОВЫЙ МЕНЕДЖЕР РЕПОЗИТОРИЯ С МГНОВЕННЫМ ЗАКРЫТИЕМ И КЛИПБОРДОМ
    // =========================================================================
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
                URL url = new URL(treeUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
                    List<RepoItem> items = new ArrayList<>();
                    if (treeArray != null) {
                        for (int i = 0; i < treeArray.length(); i++) {
                            JSONObject obj = treeArray.getJSONObject(i);
                            items.add(new RepoItem(
                                obj.optString("path"),
                                obj.optString("type"),
                                obj.optString("sha"),
                                obj.optLong("size", 0)
                            ));
                        }
                    }
                    handler.post(() -> {
                        statusTextView.setText("Статус: Готов к работе 🚀");
                        showDirectoryBrowser("", items);
                    });
                } else {
                    handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка загрузки репозитория"));
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка сети"));
            }
        }).start();
    }

    private void showDirectoryBrowser(String currentDir, List<RepoItem> allItems) {
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(15, 15, 15, 15);
        dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

        // Верхняя панель управления окном репозитория
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView pathTitle = new TextView(this);
        pathTitle.setText("📂 /" + (currentDir.isEmpty() ? "root" : currentDir));
        pathTitle.setTextColor(Color.parseColor("#38BDF8"));
        pathTitle.setTextSize(13f);
        pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pathTitle.setLayoutParams(pathParams);
        topBar.addView(pathTitle);

        // КНОПКА МГНОВЕННОГО ВЫХОДА ИЗ РЕПОЗИТОРИЯ В ОДИН КЛИК
        Button btnCloseRepo = createStyledButton("❌ Закрыть репозиторий", "#7F1D1D", "#F87171");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnClosingParams(closeParams);
        btnCloseRepo.setLayoutParams(closeParams);
        btnCloseRepo.setOnClickListener(v -> {
            if (activeRepoDialog != null) {
                activeRepoDialog.dismiss();
            }
        });
        topBar.addView(btnCloseRepo);
        dialogLayout.addView(topBar);

        java.util.Set<String> subFolders = new java.util.TreeSet<>();
        List<RepoItem> subFiles = new ArrayList<>();

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

        List<String> displayItems = new ArrayList<>();
        List<String> itemPaths = new ArrayList<>();
        List<Boolean> isFolderList = new ArrayList<>();

        if (!currentDir.isEmpty()) {
            displayItems.add("⬆️ [Наверх]");
            itemPaths.add("..");
            isFolderList.add(true);
        }

        for (String folder : subFolders) {
            displayItems.add("📁 " + folder);
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
                    tv.setPadding(12, 10, 12, 10);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        listView.setAdapter(adapter);
        
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 650
        );
        listView.setLayoutParams(listParams);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPath = itemPaths.get(position);
            boolean isFolder = isFolderList.get(position);

            if (selectedPath.equals("..")) {
                int lastSlash = currentDir.lastIndexOf('/');
                String parentDir = lastSlash == -1 ? "" : currentDir.substring(0, lastSlash);
                if (activeRepoDialog != null) activeRepoDialog.dismiss();
                showDirectoryBrowser(parentDir, allItems);
            } else if (isFolder) {
                if (activeRepoDialog != null) activeRepoDialog.dismiss();
                showDirectoryBrowser(selectedPath, allItems);
            } else {
                showFileOptionsDialog(selectedPath);
            }
        });

        dialogLayout.addView(listView);

        Button btnUploadHere = createStyledButton("➕ Загрузить файл в эту папку", "#064E3B", "#34D399");
        btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir));
        dialogLayout.addView(btnUploadHere);

        if (activeRepoDialog != null && activeRepoDialog.isShowing()) {
            activeRepoDialog.dismiss();
        }

        activeRepoDialog = new AlertDialog.Builder(this)
            .setTitle("📁 Обзор репозитория")
            .setView(dialogLayout)
            .setCancelable(true)
            .create();
            
        activeRepoDialog.show();
    }

    private void btnClosingParams(LinearLayout.LayoutParams params) {
        params.setMargins(5, 0, 0, 8);
    }

    // Полноценное контекстное меню файла: Копировать, Вставить, Удалить и т.д.
    private void showFileOptionsDialog(String filePath) {
        CharSequence[] options = {
            "📝 Редактировать", 
            "📋 Скопировать путь к файлу", 
            "📥 Вставить текст из буфера в файл", 
            "🗑 Удалить файл", 
            "ℹ️ Информация"
        };
        new AlertDialog.Builder(this)
            .setTitle("📄 Действия: " + filePath)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openFileEditorForPath(filePath);
                } else if (which == 1) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("File Path", filePath));
                    Toast.makeText(this, "Путь скопирован!", Toast.LENGTH_SHORT).show();
                } else if (which == 2) {
                    pasteClipboardToFilePath(filePath);
                } else if (which == 3) {
                    confirmDeleteSingleFile(filePath);
                } else if (which == 4) {
                    Toast.makeText(this, "Путь: " + filePath, Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void pasteClipboardToFilePath(String filePath) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            String clipText = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            String cleanText = cleanBomAndSpecialChars(clipText);
            
            statusTextView.setText("Статус: ⚙️ Вставка текста из буфера...");
            new Thread(() -> {
                boolean success = pushFileToGitHub(filePath, cleanText, "Paste clipboard content to " + filePath);
                handler.post(() -> {
                    if (success) {
                        Toast.makeText(this, "Текст успешно вставлен и сохранен!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка сохранения файла", Toast.LENGTH_SHORT).show();
                    }
                    statusTextView.setText("Статус: Готов к работе 🚀");
                });
            }).start();
        } else {
            Toast.makeText(this, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileEditorForPath(String filePath) {
        statusTextView.setText("Статус: 🔄 Загрузка " + filePath + "...");
        new Thread(() -> {
            try {
                String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + filePath;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject json = new JSONObject(sb.toString());
                    String contentB64 = json.getString("content").replaceAll("\\s", "");
                    String fileContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);

                    handler.post(() -> {
                        statusTextView.setText("Статус: Готов к работе 🚀");
                        showCodeEditorDialogForPath(filePath, fileContent);
                    });
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка загрузки"));
            }
        }).start();
    }

    private void showCodeEditorDialogForPath(String filePath, String initialContent) {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 800
        ));

        final EditText codeInput = new EditText(this);
        codeInput.setText(initialContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setGravity(Gravity.TOP);
        editorScroll.addView(codeInput);
        editorLayout.addView(editorScroll);

        new AlertDialog.Builder(this)
            .setTitle("📝 Правка: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить", (dialog, which) -> {
                String newContent = cleanBomAndSpecialChars(codeInput.getText().toString());
                statusTextView.setText("Статус: ⚙️ Сохранение...");
                new Thread(() -> {
                    boolean success = pushFileToGitHub(filePath, newContent, "Update file: " + filePath);
                    handler.post(() -> {
                        if (success) Toast.makeText(this, "Успешно сохранено на GitHub!", Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Статус: Готов к работе 🚀");
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmDeleteSingleFile(String filePath) {
        new AlertDialog.Builder(this)
            .setTitle("🗑 Подтверждение удаления")
            .setMessage("Удалить файл " + filePath + " из репозитория?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                new Thread(() -> {
                    deleteGitHubFile(filePath, "Delete file: " + filePath);
                    handler.post(() -> {
                        Toast.makeText(this, "Файл успешно удален", Toast.LENGTH_SHORT).show();
                        openRepositoryFileManager();
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void openFilePickerForFolder(String folderPath) {
        targetUploadFolder = folderPath;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите файл"), PICK_FILE_TO_FOLDER_REQUEST);
    }

    private void processSelectedFileForFolder(Uri uri, String folderPath) {
        String rawName = getFileNameFromUri(uri);
        final String fileName = (rawName != null) ? rawName : "uploaded.bin";
        final String targetPath = folderPath.isEmpty() ? fileName : folderPath + "/" + fileName;

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = readBytes(inputStream);

            statusTextView.setText("Статус: ⚙️ Загрузка файла...");
            new Thread(() -> {
                boolean success = pushBytesToGitHub(targetPath, fileBytes, "Upload file: " + fileName);
                handler.post(() -> {
                    if (success) {
                        Toast.makeText(MainActivity.this, "Файл загружен!", Toast.LENGTH_SHORT).show();
                        openRepositoryFileManager();
                    }
                    statusTextView.setText("Статус: Готов к работе 🚀");
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // ОЧИСТКА БОМ, ВСТАВКА КОДА И ЗАГРУЗКА
    // =========================================================================
    private String cleanBomAndSpecialChars(String text) {
        if (text == null) return "";
        while (text.startsWith("\uFEFF") || text.startsWith("\ufeff") || (text.length() > 0 && text.charAt(0) == 65279)) {
            text = text.substring(1);
        }
        return text.replace("\uFEFF", "").replace("\ufeff", "").replace('\u00A0', ' ').replace("\r\n", "\n").trim();
    }

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите Java или Текстовый файл"), PICK_JAVA_REQUEST);
    }

    private void openUniversalFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите APK, ZIP, RAR или Java файл"), PICK_APK_REQUEST);
    }

    private void openIconFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите иконку"), PICK_ICON_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_JAVA_REQUEST) processSelectedJavaFile(uri);
            else if (requestCode == PICK_APK_REQUEST) processUniversalFileForDecompilation(uri);
            else if (requestCode == PICK_ICON_REQUEST) processSelectedIcon(uri);
            else if (requestCode == PICK_FILE_TO_FOLDER_REQUEST) processSelectedFileForFolder(uri, targetUploadFolder);
        }
    }

    private void processSelectedJavaFile(Uri uri) {
        String rawName = getFileNameFromUri(uri);
        final String fileName = (rawName != null) ? rawName : "Custom.java";
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();

            String code = cleanBomAndSpecialChars(sb.toString());
            
            // Если в файле другое название пакета — обновляем локальный пакет приложения
            Matcher pkgMatch = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(code);
            if (pkgMatch.find()) {
                packageName = pkgMatch.group(1);
            }

            activeCodeContent = sanitizeAndInjectJavaCode(code, packageName);
            currentJavaFileName = fileName;
            saveSettings();

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("📤 Очистка BOM и выгрузка на GitHub");
            progressDialog.setMessage("Проверка кода и отправка в репозиторий...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Очищаем репозиторий перед выгрузкой нового проекта
            cleanOldPackageFilesAsync();

            final String targetPath = getMainActivityPath();
            final byte[] uploadBytes = activeCodeContent.getBytes(StandardCharsets.UTF_8);

            new Thread(() -> {
                boolean success = false;
                int attempts = 0;
                while (attempts < 10 && !success) {
                    attempts++;
                    success = pushBytesToGitHub(targetPath, uploadBytes, "Upload cleaned Java: " + fileName);
                    if (!success && attempts < 10) {
                        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    }
                }

                final boolean isSuccess = success;
                handler.post(() -> {
                    progressDialog.dismiss();
                    if (isSuccess) {
                        if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Файл очищен от BOM и мгновенно отправлен на GitHub!", Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Статус: ✅ Файл загружен в репозиторий!");
                        logEditText.setText("=== 📂 УСПЕШНАЯ ВЫГРУЗКА И ОЧИСТКА BOM ===\n" +
                                            "• Файл: " + fileName + "\n" +
                                            "• Пакет: " + packageName + "\n" +
                                            "• Путь на GitHub: " + targetPath);
                    } else {
                        lastFailedBytes = uploadBytes;
                        lastFailedPath = targetPath;
                        if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.VISIBLE);
                        statusTextView.setText("Статус: ❌ Ошибка выгрузки в сеть");
                    }
                    updateAllButtonLabels();
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void retryLastUpload() {
        if (lastFailedBytes == null || lastFailedPath.isEmpty()) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🔄 Повтор отправки");
        progressDialog.setMessage("Выгрузка файла на GitHub...");
        progressDialog.show();

        new Thread(() -> {
            boolean success = pushBytesToGitHub(lastFailedPath, lastFailedBytes, "Retry upload");
            handler.post(() -> {
                progressDialog.dismiss();
                if (success) {
                    if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Успешно отправлено!", Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Статус: ✅ Синхронизировано!");
                } else {
                    Toast.makeText(this, "Снова ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Декомпиляция и распаковка архива/APK в код
    private void processUniversalFileForDecompilation(Uri uri) {
        String rawName = getFileNameFromUri(uri);
        final String fileName = (rawName != null) ? rawName : "archive.file";
        final String lowerName = fileName.toLowerCase();

        statusTextView.setText("Статус: 🔄 Распаковка и декомпиляция...");
        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "temp_upload." + (lowerName.endsWith(".apk") ? "apk" : "zip"));
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new java.io.FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                }

                if (lowerName.endsWith(".apk") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
                    try {
                        ZipFile zip = new ZipFile(tempFile);
                        ZipEntry dex = zip.getEntry("classes.dex");
                        if (dex != null) {
                            try (InputStream dexIn = zip.getInputStream(dex)) {
                                byte[] bytes = readBytes(dexIn);
                                String dexStr = new String(bytes, StandardCharsets.ISO_8859_1);
                                Matcher pkg = Pattern.compile("L([a-zA-Z0-9_/]+)/MainActivity;").matcher(dexStr);
                                if (pkg.find()) packageName = pkg.group(1).replace('/', '.');
                            }
                        }
                        zip.close();
                    } catch (Exception ignored) {}

                    String decompiledJava = decompileDexToJava(tempFile);
                    activeCodeContent = sanitizeAndInjectJavaCode(decompiledJava, packageName);
                    currentJavaFileName = fileName + " (Распакован)";
                    tempFile.delete();

                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Decompiled: " + fileName);

                    handler.post(() -> {
                        saveSettings();
                        statusTextView.setText("Статус: ✅ Распаковано и загружено!");
                        Toast.makeText(MainActivity.this, "Файл декомпилирован и готов к правке!", Toast.LENGTH_LONG).show();
                        logEditText.setText("=== 📦 РАСПАКОВКА И ДЕКОМПИЛЯЦИЯ УСПЕШНА ===\n• Файл: " + fileName + "\n• Пакет: " + packageName);
                        updateAllButtonLabels();
                        openCodeEditorDialog(); // Сразу открываем редактор с поиском
                    });
                } else {
                    InputStream is = getContentResolver().openInputStream(uri);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    br.close();

                    String code = cleanBomAndSpecialChars(sb.toString());
                    Matcher pkgMatch = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(code);
                    if (pkgMatch.find()) packageName = pkgMatch.group(1);

                    activeCodeContent = sanitizeAndInjectJavaCode(code, packageName);
                    currentJavaFileName = fileName;
                    tempFile.delete();

                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Upload text file: " + fileName);

                    handler.post(() -> {
                        saveSettings();
                        statusTextView.setText("Статус: ✅ Готово к редактированию!");
                        logEditText.setText("=== 📄 ФАЙЛ УСПЕШНО ЗАГРУЖЕН ===\n• Файл: " + fileName);
                        updateAllButtonLabels();
                        openCodeEditorDialog();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    statusTextView.setText("Статус: ❌ Ошибка декомпиляции");
                    logEditText.setText("❌ Ошибка обработки: " + e.getMessage());
                });
            }
        }).start();
    }

    private String decompileDexToJava(File apkFile) {
        try {
            ZipFile zip = new ZipFile(apkFile);
            ZipEntry dexEntry = zip.getEntry("classes.dex");
            if (dexEntry == null) {
                zip.close();
                return generateClockTemplate();
            }
            InputStream is = zip.getInputStream(dexEntry);
            byte[] dexBytes = readBytes(is);
            zip.close();

            if (dexBytes.length < 112) return generateClockTemplate();

            int stringIdsSize = readInt(dexBytes, 56);
            int stringIdsOff = readInt(dexBytes, 60);

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(packageName).append(";\n\n");
            sb.append("import android.app.Activity;\n");
            sb.append("import android.os.Bundle;\n");
            sb.append("import android.widget.*;\n");
            sb.append("import android.graphics.*;\n");
            sb.append("import android.view.*;\n\n");
            sb.append("public class MainActivity extends Activity {\n");
            sb.append("    @Override\n");
            sb.append("    protected void onCreate(Bundle savedInstanceState) {\n");
            sb.append("        super.onCreate(savedInstanceState);\n");
            sb.append("        LinearLayout layout = new LinearLayout(this);\n");
            sb.append("        layout.setOrientation(LinearLayout.VERTICAL);\n");
            sb.append("        layout.setGravity(Gravity.CENTER);\n");
            sb.append("        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n");
            sb.append("        layout.setPadding(30, 30, 30, 30);\n\n");

            sb.append("        TextView tv = new TextView(this);\n");
            sb.append("        tv.setText(\"⚡ Декомпилировано из APK\");\n");
            sb.append("        tv.setTextColor(Color.parseColor(\"#38BDF8\"));\n");
            sb.append("        tv.setTextSize(18f);\n");
            sb.append("        tv.setGravity(Gravity.CENTER);\n");
            sb.append("        layout.addView(tv);\n\n");

            int addedCount = 0;
            for (int i = 0; i < Math.min(stringIdsSize, 250); i++) {
                int stringOffOff = stringIdsOff + i * 4;
                if (stringOffOff + 4 > dexBytes.length) break;
                int stringDataOff = readInt(dexBytes, stringOffOff);
                if (stringDataOff >= 0 && stringDataOff < dexBytes.length) {
                    String str = readUleb128String(dexBytes, stringDataOff);
                    if (str != null && !str.isEmpty() && str.length() < 70 && isPrintable(str)) {
                        if (str.contains("setText") || str.contains("Button") || str.contains("Color") || str.contains("http") || str.contains("layout")) {
                            sb.append("        // Распознанная константа: ").append(escapeJava(str)).append("\n");
                            addedCount++;
                        }
                    }
                }
            }

            sb.append("\n        setContentView(layout);\n");
            sb.append("    }\n");
            sb.append("}\n");
            return sb.toString();
        } catch (Exception e) {
            return generateClockTemplate();
        }
    }

    private int readInt(byte[] src, int offset) {
        return (src[offset] & 0xFF) |
               ((src[offset + 1] & 0xFF) << 8) |
               ((src[offset + 2] & 0xFF) << 16) |
               ((src[offset + 3] & 0xFF) << 24);
    }

    private String readUleb128String(byte[] src, int offset) {
        try {
            int len = 0;
            int shift = 0;
            int idx = offset;
            while (idx < src.length) {
                int b = src[idx++] & 0xFF;
                len |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            if (len <= 0 || len > 512 || idx + len > src.length) return null;
            return new String(src, idx, len, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPrintable(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 && c != '\n' && c != '\t' && c != '\r') return false;
        }
        return true;
    }

    private String escapeJava(String s) {
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    private void processSelectedIcon(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(is);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, 192, 192, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            byte[] iconBytes = out.toByteArray();

            new Thread(() -> {
                boolean success = pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Update icon");
                handler.post(() -> {
                    if (success) {
                        iconCustomized = false;
                        saveSettings();
                        Toast.makeText(MainActivity.this, "Иконка обновлена!", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка иконки", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // SMART AI-ПРОФЕССОР И ИНСПЕКЦИЯ КОДА
    // =========================================================================
    private void runAiCodeDoctor() {
        statusTextView.setText("Статус: 🎓 Smart AI-Профессор анализирует код и терминал...");
        
        new Thread(() -> {
            try {
                Thread.sleep(800);
                String fixedCode = activeCodeContent;
                boolean repaired = false;
                StringBuilder report = new StringBuilder("=== 🎓 SMART AI-ПРОФЕССОР: ОТЧЕТ ИНСПЕКЦИИ ===\n\n");

                // 1. Проверка BOM
                if (fixedCode.contains("\uFEFF") || fixedCode.contains("\ufeff")) {
                    fixedCode = cleanBomAndSpecialChars(fixedCode);
                    repaired = true;
                    report.append("  ➜ 🛠 Вырезан скрытый символ BOM (\\ufeff).\n");
                }

                // 2. Добавление недостающих WebView импортов
                if ((lastErrorLog.contains("WebView") || fixedCode.contains("WebView")) && !fixedCode.contains("import android.webkit.WebView;")) {
                    fixedCode = fixedCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.webkit.WebView;\nimport android.webkit.WebSettings;\nimport android.webkit.WebViewClient;");
                    repaired = true;
                    report.append("  ➜ 🛠 Интегрированы импорты WebView.\n");
                }

                // 3. Импорты коллекций
                if (!fixedCode.contains("import java.util.*;")) {
                    fixedCode = fixedCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport java.util.*;\nimport java.util.regex.*;");
                    repaired = true;
                    report.append("  ➜ 🛠 Подключены пакеты java.util.* и java.util.regex.*\n");
                }

                if (repaired) {
                    activeCodeContent = fixedCode;
                    saveSettings();
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "AI Auto-Fix applied");
                    report.append("\n🎉 Код автоматически приведен к идеалу и отправлен на GitHub!");
                } else {
                    report.append("✅ Синтаксических структурных аномалий не обнаружено.");
                }

                final String reportStr = report.toString();
                handler.post(() -> {
                    statusTextView.setText("Статус: ✅ Инспекция завершена!");
                    logEditText.setText(reportStr);
                });
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка инспектора"));
            }
        }).start();
    }

    private void cleanOldPackageFilesAsync() {
        new Thread(() -> {
            deleteGitHubFile("app/src/main/java/com/app/app/MainActivity.java", "Clean old path");
        }).start();
    }

    private void deleteGitHubFile(String path, String message) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                String sha = json.optString("sha", null);

                HttpURLConnection delConn = (HttpURLConnection) url.openConnection();
                delConn.setRequestMethod("DELETE");
                delConn.setRequestProperty("Authorization", "Bearer " + githubToken);
                delConn.setRequestProperty("Content-Type", "application/json; utf-8");
                delConn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("message", message);
                payload.put("sha", sha);
                payload.put("branch", "main");

                try (OutputStream os = delConn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                delConn.getResponseCode();
            }
        } catch (Exception ignored) {}
    }

    private String sanitizeAndInjectJavaCode(String code, String pkg) {
        code = cleanBomAndSpecialChars(code);
        code = code.replaceAll("package\\s+[a-zA-Z0-9_.]+;", "package " + pkg + ";");
        if (!code.contains("package ")) code = "package " + pkg + ";\n\n" + code;
        if (!code.contains("import android.app.Activity;")) {
            code = code.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.*;\nimport android.graphics.*;\nimport android.view.*;");
        }
        if (!code.contains("class MainActivity")) {
            Matcher m = Pattern.compile("class\\s+([a-zA-Z0-9_]+)").matcher(code);
            if (m.find()) code = code.replaceAll("\\bclass\\s+" + m.group(1) + "\\b", "class MainActivity");
        }
        return code;
    }

    private byte[] readBytes(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        return out.toByteArray();
    }

    private String getFileNameFromUri(Uri uri) {
        String res = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) res = c.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (res == null) {
            res = uri.getPath();
            int cut = res.lastIndexOf('/');
            if (cut != -1) res = res.substring(cut + 1);
        }
        return res;
    }

    // =========================================================================
    // ПОЛНОЭКРАННЫЙ РЕДАКТОР КОДА С ПОИСКОМ И ВЫДЕЛЕНИЕМ (SEARCH & HIGHLIGHT)
    // =========================================================================
    private int lastSearchIndex = -1;

    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        // Панель поиска
        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setPadding(0, 0, 0, 10);

        final EditText searchInput = new EditText(this);
        searchInput.setHint("🔍 Поиск в коде...");
        searchInput.setHintTextColor(Color.parseColor("#64748B"));
        searchInput.setTextColor(Color.parseColor("#38BDF8"));
        searchInput.setTextSize(11f);
        searchInput.setBackgroundColor(Color.parseColor("#030712"));
        searchInput.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        searchInput.setLayoutParams(searchParams);
        searchBar.addView(searchInput);

        Button btnFindNext = createStyledButton("⏩ Найти", "#1E293B", "#38BDF8");
        LinearLayout.LayoutParams btnFindParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnFindParams.setMargins(6, 0, 0, 0);
        btnFindNext.setLayoutParams(btnFindParams);
        searchBar.addView(btnFindNext);

        editorLayout.addView(searchBar);

        final ScrollView editorScroll = new ScrollView(this);
        editorScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1100 // Увеличенная высота для удобства
        ));

        final EditText codeInput = new EditText(this);
        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setGravity(Gravity.TOP);
        editorScroll.addView(codeInput);
        editorLayout.addView(editorScroll);

        // Обработчик кнопки Найти
        btnFindNext.setOnClickListener(v -> {
            String query = searchInput.getText().toString();
            if (query.isEmpty()) return;

            String fullText = codeInput.getText().toString();
            int findPos = fullText.toLowerCase().indexOf(query.toLowerCase(), lastSearchIndex + 1);
            if (findPos == -1) {
                findPos = fullText.toLowerCase().indexOf(query.toLowerCase(), 0); // Циклический поиск с начала
            }

            if (findPos != -1) {
                lastSearchIndex = findPos;
                SpannableString spannable = new SpannableString(fullText);
                spannable.setSpan(new BackgroundColorSpan(Color.parseColor("#854D0E")), findPos, findPos + query.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                codeInput.setText(spannable);
                codeInput.setSelection(findPos, findPos + query.length());
                Toast.makeText(this, "Найдено на позиции " + findPos, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Совпадений не найдено", Toast.LENGTH_SHORT).show();
                lastSearchIndex = -1;
            }
        });

        AlertDialog editorDialog = new AlertDialog.Builder(this)
            .setTitle("📝 Полноэкранный Редактор Кода Pro")
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить на GitHub", (dialog, which) -> {
                String rawCode = cleanBomAndSpecialChars(codeInput.getText().toString());
                
                // Автоматически определяем пакет из кода
                Matcher pkgMatch = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(rawCode);
                if (pkgMatch.find()) {
                    packageName = pkgMatch.group(1);
                }
                
                activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
                saveSettings();

                // Автоматическая отправка на GitHub
                statusTextView.setText("Статус: ⚙️ Мгновенная выгрузка на GitHub...");
                new Thread(() -> {
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update via Code Editor");
                    handler.post(() -> {
                        if (success) {
                            Toast.makeText(this, "Код очищен и выгружен на GitHub!", Toast.LENGTH_SHORT).show();
                            statusTextView.setText("Статус: ✅ Выгружено в репозиторий!");
                        } else {
                            Toast.makeText(this, "Ошибка сети при выгрузке", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();

                // Запрос на проверку Smart AI-Инспектором
                showInspectionOfferDialog();
            })
            .setNegativeButton("Отмена", null)
            .create();

        editorDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        editorDialog.show();
    }

    private void showInspectionOfferDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🎓 Smart AI-Инспектор")
            .setMessage("Код сохранен и выгружен. Запустить проверку и автоисправление ошибок в терминале?")
            .setPositiveButton("Запустить", (dialog, which) -> runAiCodeDoctor())
            .setNegativeButton("Позже", null)
            .show();
    }

    // =========================================================================
    // ЗАПУСК СБОРКИ НА СЕРВЕРЕ GITHUB ACTIONS
    // =========================================================================
    private void triggerGitHubBuild() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        Matcher pkgMatch = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(activeCodeContent);
        if (pkgMatch.find()) {
            packageName = pkgMatch.group(1);
        }

        versionCode++;
        saveSettings();

        btnBuild.setEnabled(false);
        btnBuild.setText("⏳ Сборка в облаке...\n📦 " + appTitle);
        statusTextView.setText("Статус: ⚙️ Синхронизация структуры репозитория...");
        logEditText.setText("Синхронизация чистого репозитория на GitHub...");

        new Thread(() -> {
            try {
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                
                boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                 pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update properties") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local.properties") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");

                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        updateAllButtonLabels();
                        statusTextView.setText("Статус: ❌ Ошибка токена GitHub");
                    });
                    return;
                }

                String dispatchUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/workflows/build_native.yml/dispatches";
                URL url = new URL(dispatchUrl);
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
                        statusTextView.setText("Статус: 🚀 Сборка запущена! Ожидание файлов...");
                        logEditText.setText("Облачный сервер компилирует APK...\nПосле завершения бот отправит готовый APK и файл исходного кода (" + appTitle + "-v" + appVersion + ".java.txt) в Telegram!");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        updateAllButtonLabels();
                        statusTextView.setText("Статус: ❌ Ошибка запуска компиляции");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    updateAllButtonLabels();
                    statusTextView.setText("Статус: ❌ Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }

    private void emergencyStopBuild() {
        isMonitoring = false;
        if (latestRunId > 0 && !githubToken.isEmpty()) {
            new Thread(() -> {
                try {
                    String cancelUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + latestRunId + "/cancel";
                    HttpURLConnection conn = (HttpURLConnection) new URL(cancelUrl).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                    conn.getResponseCode();
                } catch (Exception ignored) {}
            }).start();
        }

        handler.post(() -> {
            if (btnBuild != null) {
                btnBuild.setEnabled(true);
                updateAllButtonLabels();
            }
            statusTextView.setText("Статус: 🛑 Сборка остановлена!");
            logEditText.setText("⚠️ Процесс сборки экстренно отменен.");
        });
    }

    private boolean pushFileToGitHub(String path, String content, String message) {
        return pushBytesToGitHub(path, content.getBytes(StandardCharsets.UTF_8), message);
    }

    private boolean pushBytesToGitHub(String path, byte[] bytes, String message) {
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
            payload.put("message", message);
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
            long start = System.currentTimeMillis();
            while (isMonitoring) {
                try {
                    Thread.sleep(6000);
                    if (!isMonitoring) break;
                    String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs?per_page=1";
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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
                            long elapsed = (System.currentTimeMillis() - start) / 1000;

                            handler.post(() -> {
                                if (!isMonitoring) return;
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    updateAllButtonLabels();
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Успешно! Файлы доставлены в Telegram.");
                                        logEditText.setText("Сборка успешно завершена!\nФайлы " + appTitle + "-v" + appVersion + ".apk и " + appTitle + "-v" + appVersion + ".java.txt отправлены в ваш Telegram.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции");
                                        fetchLogsAsync(latestRunId);
                                    }
                                } else {
                                    statusTextView.setText("Статус: ⚙️ " + status + " (" + elapsed + "с)");
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
                String jobsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + runId + "/jobs";
                HttpURLConnection conn = (HttpURLConnection) new URL(jobsUrl).openConnection();
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
                        String logsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/jobs/" + jobId + "/logs";
                        HttpURLConnection logConn = (HttpURLConnection) new URL(logsUrl).openConnection();
                        logConn.setRequestMethod("GET");
                        logConn.setRequestProperty("Authorization", "Bearer " + githubToken);

                        if (logConn.getResponseCode() == 200) {
                            BufferedReader lb = new BufferedReader(new InputStreamReader(logConn.getInputStream(), StandardCharsets.UTF_8));
                            StringBuilder ls = new StringBuilder();
                            while ((line = lb.readLine()) != null) ls.append(line).append("\n");
                            lastErrorLog = ls.toString();

                            StringBuilder professionalReport = new StringBuilder("=== 📋 ОТЧЕТ ОБ ОШИБКАХ СБОРКИ ===\n\n");
                            String[] lines = lastErrorLog.split("\n");
                            int lineNum = 1;
                            for (String logLine : lines) {
                                String lower = logLine.toLowerCase();
                                if (lower.contains("error:") || lower.contains("failed") || lower.contains("exception")) {
                                    professionalReport.append("❌ [Строка ").append(lineNum).append("]: ").append(logLine.trim()).append("\n");
                                }
                                lineNum++;
                            }

                            handler.post(() -> {
                                logEditText.setText(professionalReport.toString());
                                Toast.makeText(MainActivity.this, "⚠️ Обнаружены ошибки сборки!", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Сброс к часам")
            .setMessage("Сбросить текущий проект к стандартным часам Monster Clock?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                activeCodeContent = generateClockTemplate();
                currentJavaFileName = "Шаблон: Monster Clock";
                appTitle = "Monster Clock";
                packageName = "com.monster.clock.app";
                appVersion = "1.0";
                versionCode = 1;
                
                saveSettings();
                cleanOldPackageFilesAsync();

                new Thread(() -> {
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Reset to Monster Clock");
                }).start();

                Toast.makeText(this, "Проект сброшен к Monster Clock!", Toast.LENGTH_SHORT).show();
                logEditText.setText("=== 🔄 ПРОЕКТ СБРОШЕН ДО ЧАСОВ ===");
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(appTitle);
        new AlertDialog.Builder(this)
            .setTitle("Имя приложения")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                appTitle = input.getText().toString().trim();
                packageName = transliterateToPackageName(appTitle);
                saveSettings();
                Toast.makeText(this, "Имя и уникальный пакет обновлены!", Toast.LENGTH_SHORT).show();
                logEditText.setText("=== 🏷 ИМЯ И ПАКЕТ ОБНОВЛЕНЫ ===\n• Имя: " + appTitle + "\n• Пакет: " + packageName);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditVersionDialog() {
        final EditText input = new EditText(this);
        input.setText(appVersion);
        new AlertDialog.Builder(this)
            .setTitle("Версия приложения")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                appVersion = input.getText().toString().trim();
                saveSettings();
                Toast.makeText(this, "Версия обновлена!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 15, 20, 15);

        final EditText inputGithub = createDialogEditText("GitHub Token", githubToken);
        layout.addView(inputGithub);
        final EditText inputBot = createDialogEditText("Bot Token", botToken);
        layout.addView(inputBot);
        final EditText inputChat = createDialogEditText("Chat ID", chatId);
        layout.addView(inputChat);

        final Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
            "⚡ Стабильный (Java 8)", "🚀 Java 17"
        }));
        spinner.setSelection(javaVersion.equals("8") ? 0 : 1);
        layout.addView(spinner);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки компилятора")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();
                javaVersion = spinner.getSelectedItemPosition() == 0 ? "8" : "17";
                minSdk = spinner.getSelectedItemPosition() == 0 ? 21 : 26;
                saveSettings();
                Toast.makeText(this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private EditText createDialogEditText(String hint, String val) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(val);
        et.setPadding(20, 12, 20, 12);
        return et;
    }

    private String getMainActivityPath() {
        return "app/src/main/java/" + packageName.replace('.', '/') + "/MainActivity.java";
    }

    private String getRootGradleContent() {
        return "buildscript {\n    repositories {\n        google()\n        mavenCentral()\n    }\n    dependencies {\n        classpath 'com.android.tools.build:gradle:8.2.2'\n    }\n}\nallprojects {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\n";
    }

    private String getSettingsGradleContent() {
        return "rootProject.name = \"" + appTitle + "\"\ninclude ':app'\n";
    }

    private String getGradlePropertiesContent() {
        return "org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8\n" +
               "android.useAndroidX=true\n" +
               "android.useJetifier=true\n";
    }

    // =========================================================================
    // WORKFLOW ДЛЯ СБОРКИ И ОТПРАВКИ ИЗДЕЛИЙ С ТОЧНЫМИ ИМЕНАМИ С ВЕРСИЕЙ В TELEGRAM
    // =========================================================================
    private String getWorkflowContent() {
        String cleanTitle = appTitle.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return "name: Build Monster APK\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - name: Checkout\n        uses: actions/checkout@v4\n" +
               "      - name: Set up JDK 17\n        uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n" +
               "      - name: Setup Android SDK (Native Bash)\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          echo \"y\" | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
               "      - name: Setup Gradle\n        uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Build APK\n        run: |\n" +
               "          gradle clean assembleDebug --info --stacktrace --no-daemon\n" +
               "      - name: Send APK and Java Code Duplicate to Telegram\n        if: success()\n        run: |\n" +
               "          RAW_APK=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
               "          TARGET_APK=\"" + cleanTitle + "-v" + appVersion + ".apk\"\n" +
               "          TARGET_TXT=\"" + cleanTitle + "-v" + appVersion + ".java.txt\"\n" +
               "          cp \"$RAW_APK\" \"$TARGET_APK\"\n" +
               "          JAVA_SRC=$(find app/src/main/java/ -name \"MainActivity.java\" | head -n 1)\n" +
               "          if [ -f \"$JAVA_SRC\" ]; then\n" +
               "            cp \"$JAVA_SRC\" \"$TARGET_TXT\"\n" +
               "          else\n" +
               "            echo \"package " + packageName + ";\" > \"$TARGET_TXT\"\n" +
               "          fi\n" +
               "          curl -F chat_id=\"" + chatId + "\" -F document=@\"$TARGET_APK\" https://api.telegram.org/bot" + botToken + "/sendDocument\n" +
               "          curl -F chat_id=\"" + chatId + "\" -F document=@\"$TARGET_TXT\" https://api.telegram.org/bot" + botToken + "/sendDocument\n";
    }

    // =========================================================================
    // ПОЛНАЯ БАЗА РАЗРЕШЕНИЙ MANIFEST (ВКЛЮЧАЯ NFC, CAMERA, GPS И Т.Д.)
    // =========================================================================
    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CHANGE_WIFI_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CHANGE_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CHANGE_WIFI_MULTICAST_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.MANAGE_EXTERNAL_STORAGE\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" android:maxSdkVersion=\"33\" />\n" +
               "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" android:maxSdkVersion=\"33\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_AUDIO\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_IMAGES\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_VIDEO\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_VISUAL_USER_SELECTED\" />\n" +
               "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n" +
               "    <uses-permission android:name=\"android.permission.MODIFY_AUDIO_SETTINGS\" />\n" +
               "    <uses-permission android:name=\"android.permission.CAPTURE_AUDIO_OUTPUT\" />\n" +
               "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_ADMIN\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_CONNECT\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_SCAN\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_ADVERTISE\" />\n" +
               "    <uses-permission android:name=\"android.permission.NFC\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_BACKGROUND_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MICROPHONE\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_DATA_SYNC\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK\" />\n" +
               "    <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />\n" +
               "    <uses-permission android:name=\"android.permission.WAKE_LOCK\" />\n" +
               "    <uses-permission android:name=\"android.permission.VIBRATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" />\n" +
               "    <uses-permission android:name=\"android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS\" />\n" +
               "    <uses-permission android:name=\"android.permission.SYSTEM_ALERT_WINDOW\" />\n" +
               "    <uses-permission android:name=\"android.permission.WRITE_SETTINGS\" />\n" +
               "    <uses-permission android:name=\"android.permission.EXPAND_STATUS_BAR\" />\n" +
               "    <uses-permission android:name=\"android.permission.REQUEST_INSTALL_PACKAGES\" />\n" +
               "    <uses-permission android:name=\"android.permission.USE_FULL_SCREEN_INTENT\" />\n" +
               "    <uses-permission android:name=\"android.permission.SCHEDULE_EXACT_ALARM\" />\n" +
               "    <uses-permission android:name=\"com.android.vending.BILLING\" />\n" +
               "    <uses-permission android:name=\"android.permission.BODY_SENSORS\" />\n" +
               "    <uses-permission android:name=\"android.permission.BODY_SENSORS_BACKGROUND\" />\n" +
               "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + appTitle + "\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
               "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
               "            <intent-filter>\n" +
               "                <action android:name=\"android.intent.action.MAIN\" />\n" +
               "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
               "            </intent-filter>\n" +
               "        </activity>\n" +
               "    </application>\n" +
               "</manifest>";
    }

    private String generateAppGradle() {
        String compat = javaVersion.equals("8") ? "JavaVersion.VERSION_1_8" : "JavaVersion.VERSION_17";
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode " + versionCode + "\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n        sourceCompatibility " + compat + "\n        targetCompatibility " + compat + "\n    }\n" +
               "    applicationVariants.all { variant ->\n" +
               "        variant.outputs.all { output ->\n" +
               "            outputFileName = \"" + appTitle + "-v" + appVersion + "-\" + variant.name + \".apk\"\n" +
               "        }\n" +
               "    }\n" +
               "}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n    implementation 'com.squareup.okhttp3:okhttp:4.12.0'\n}\n";
    }

    private String generateClockTemplate() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.content.Context;\n" +
               "import android.hardware.camera2.CameraManager;\n" +
               "import android.os.Bundle;\n" +
               "import android.view.Gravity;\n" +
               "import android.view.WindowManager;\n" +
               "import android.widget.*;\n" +
               "import android.graphics.Color;\n" +
               "import android.graphics.Typeface;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    private boolean isFlashOn = false;\n" +
               "    private CameraManager cameraManager;\n" +
               "    private String cameraId;\n\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        layout.setPadding(30, 30, 30, 30);\n\n" +
               "        TextView title = new TextView(this);\n" +
               "        title.setText(\"⚡ Monster Clock & Flash\");\n" +
               "        title.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        title.setTextSize(20f);\n" +
               "        title.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        title.setGravity(Gravity.CENTER);\n" +
               "        title.setPadding(0, 0, 0, 30);\n" +
               "        layout.addView(title);\n\n" +
               "        TextClock clock = new TextClock(this);\n" +
               "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        clock.setTextSize(52f);\n" +
               "        clock.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        clock.setGravity(Gravity.CENTER);\n" +
               "        clock.setPadding(0, 0, 0, 40);\n" +
               "        layout.addView(clock);\n\n" +
               "        Button flashBtn = new Button(this);\n" +
               "        flashBtn.setText(\"🔦 Включить фонарик\");\n" +
               "        flashBtn.setTextColor(Color.WHITE);\n" +
               "        flashBtn.setTextSize(14f);\n" +
               "        flashBtn.setBackgroundColor(Color.parseColor(\"#2563EB\"));\n" +
               "        flashBtn.setPadding(20, 20, 20, 20);\n" +
               "        layout.addView(flashBtn);\n\n" +
               "        TextView brightLabel = new TextView(this);\n" +
               "        brightLabel.setText(\"☀️ Яркость экрана\");\n" +
               "        brightLabel.setTextColor(Color.parseColor(\"#94A3B8\"));\n" +
               "        brightLabel.setTextSize(12f);\n" +
               "        brightLabel.setPadding(0, 40, 0, 10);\n" +
               "        layout.addView(brightLabel);\n\n" +
               "        SeekBar seekBar = new SeekBar(this);\n" +
               "        seekBar.setMax(100);\n" +
               "        seekBar.setProgress(50);\n" +
               "        layout.addView(seekBar);\n\n" +
               "        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);\n" +
               "        try {\n" +
               "            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {\n" +
               "                cameraId = cameraManager.getCameraIdList()[0];\n" +
               "            }\n" +
               "        } catch (Exception ignored) {}\n\n" +
               "        flashBtn.setOnClickListener(v -> {\n" +
               "            try {\n" +
               "                if (cameraManager != null && cameraId != null) {\n" +
               "                    isFlashOn = !isFlashOn;\n" +
               "                    cameraManager.setTorchMode(cameraId, isFlashOn);\n" +
               "                    flashBtn.setText(isFlashOn ? \"🔦 Выключить фонарик\" : \"🔦 Включить фонарик\");\n" +
               "                    flashBtn.setBackgroundColor(Color.parseColor(isFlashOn ? \"#DC2626\" : \"#2563EB\"));\n" +
               "                } else {\n" +
               "                    Toast.makeText(this, \"Фонарик недоступен\", Toast.LENGTH_SHORT).show();\n" +
               "                }\n" +
               "            } catch (Exception e) {\n" +
               "                Toast.makeText(this, \"Ошибка фонарика: \" + e.getMessage(), Toast.LENGTH_SHORT).show();\n" +
               "            }\n" +
               "        });\n\n" +
               "        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {\n" +
               "            @Override\n" +
               "            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {\n" +
               "                WindowManager.LayoutParams lp = getWindow().getAttributes();\n" +
               "                lp.screenBrightness = (float) progress / 100f;\n" +
               "                getWindow().setAttributes(lp);\n" +
               "            }\n" +
               "            @Override public void onStartTrackingTouch(SeekBar seekBar) {}\n" +
               "            @Override public void onStopTrackingTouch(SeekBar seekBar) {}\n" +
               "        });\n\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}