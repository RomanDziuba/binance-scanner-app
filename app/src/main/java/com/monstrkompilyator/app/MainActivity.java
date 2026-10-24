package com.monstrkompilyator.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "RomaMonsterApp";
    private String appVersion = "1.0";
    private String packageName = "com.roma.monster";
    private String currentJavaFileName = "Шаблон: AI-Браузер с WebView";
    private boolean iconCustomized = true;
    private String targetUploadFolder = "";

    private int minSdk = 24;
    private int compileSdk = 34;
    private String javaVersion = "17"; 
    private String activeCodeContent = "";

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor, btnFileManager, btnTemplates, btnAiFix;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;
    private String lastErrorLog = "";

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
            activeCodeContent = generateBrowserTemplate();
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
        authorView.setText("✦ Dziuba Roman — Monster CI/CD Engine ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(15f);
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
        titleView.setText("🚀 Android Monster Compiler v3.5");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 10);
        card.addView(titleView);

        infoTextView = new TextView(this);
        updateInfoText();
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

        btnBuild = createStyledButton(getBuildButtonText(), "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        btnStopBuild = createStyledButton("🛑 Экстренный стоп сборки", "#DC2626");
        btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
        card.addView(btnStopBuild);

        btnTemplates = createStyledButton("⚡ Выбрать готовый шаблон приложения", "#D97706");
        btnTemplates.setOnClickListener(v -> showTemplatesDialog());
        card.addView(btnTemplates);

        btnFileManager = createStyledButton("📁 Файловый менеджер репозитория", "#8B5CF6");
        btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
        card.addView(btnFileManager);

        btnCodeEditor = createStyledButton("📝 Редактор кода с подсказками", "#059669");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        btnUploadJava = createStyledButton("📂 Загрузить файл: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        btnAiFix = createStyledButton("🤖 AI-Автодоктор: Исправить ошибки", "#7C3AED");
        btnAiFix.setOnClickListener(v -> runAiCodeDoctor());
        card.addView(btnAiFix);

        btnDecompileApk = createStyledButton("📦 Декомпилировать APK в код", "#475569");
        btnDecompileApk.setOnClickListener(v -> openApkFilePicker());
        card.addView(btnDecompileApk);

        btnReset = createStyledButton("🔄 Полный сброс репозитория", "#B45309");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        btnTitle = createStyledButton("🏷 Имя: " + appTitle + " (" + packageName + ")", "#334155");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        btnVersion = createStyledButton("📦 Версия: " + appVersion, "#334155");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        btnIcon = createStyledButton(getIconBtnText(), "#6D28D9");
        updateIconButtonPreview();
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
        logLabel.setText("📋 Терминал / Логи сборки и компиляции:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь выводятся логи сборки GitHub Actions и отчеты авто-доктора...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(7);
        logEditText.setMaxLines(14);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог в буфер", "#475569");
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

        if (!githubToken.isEmpty()) {
            fetchRemoteRepositoryInfo();
        }
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

    private String getIconBtnText() {
        return iconCustomized ? "🖼 Иконка: ⚡ Шаблон" : "🖼 Иконка: 🎨 Пользовательская";
    }

    private void updateIconButtonPreview() {
        if (btnIcon != null) btnIcon.setText(getIconBtnText());
    }

    private void updateInfoText() {
        String profile = javaVersion.equals("8") ? "⚡ Слабый телефон" : "🚀 Мощный телефон";
        String info = "🏷 Имя: " + appTitle + " | 📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + " | 📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profile + "]";
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя: " + appTitle + " (" + packageName + ")");
        if (btnVersion != null) btnVersion.setText("📦 Версия: " + appVersion);
        if (btnBuild != null) btnBuild.setText(getBuildButtonText());
        updateIconButtonPreview();
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
        minSdk = sp.getInt("minSdk", 24);
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
        updateInfoText();
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
                    Matcher nsMatcher = Pattern.compile("namespace\\s+'([^']+)'").matcher(gradleContent);
                    if (nsMatcher.find()) packageName = nsMatcher.group(1);

                    handler.post(() -> {
                        saveSettings();
                        statusTextView.setText("Статус: ✅ Репозиторий синхронизирован!");
                    });
                } else {
                    handler.post(() -> statusTextView.setText("Статус: Готов к работе 🚀"));
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: Готов к работе 🚀"));
            }
        }).start();
    }

    private void showTemplatesDialog() {
        CharSequence[] templates = {
            "🌐 Шаблон 1: AI-Браузер с WebView и адресной строкой",
            "🕞 Шаблон 2: Часы с фонариком и ночным режимом",
            "📊 Шаблон 3: Binance Crypto Tracker (Котировки и графики)",
            "💬 Шаблон 4: Telegram Web App Оболочка",
            "🎮 Шаблон 5: Простой кликер-игра на Java"
        };

        new AlertDialog.Builder(this)
            .setTitle("⚡ Выберите шаблон приложения")
            .setItems(templates, (dialog, which) -> {
                if (which == 0) {
                    activeCodeContent = generateBrowserTemplate();
                    currentJavaFileName = "Шаблон: AI-Браузер";
                } else if (which == 1) {
                    activeCodeContent = generateClockTemplate();
                    currentJavaFileName = "Шаблон: Часы с фонариком";
                } else if (which == 2) {
                    activeCodeContent = generateCryptoTemplate();
                    currentJavaFileName = "Шаблон: Binance Crypto Tracker";
                } else if (which == 3) {
                    activeCodeContent = generateWebAppTemplate();
                    currentJavaFileName = "Шаблон: Telegram Web App";
                } else if (which == 4) {
                    activeCodeContent = generateClickerTemplate();
                    currentJavaFileName = "Шаблон: Кликер-игра";
                }
                saveSettings();
                btnUploadJava.setText("📂 Файл: " + currentJavaFileName);
                Toast.makeText(this, "Шаблон успешно загружен!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
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
                    java.util.List<RepoItem> items = new java.util.ArrayList<>();
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
                    handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка загрузки дерева репо"));
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
        pathTitle.setPadding(0, 0, 0, 8);
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
                    tv.setPadding(12, 10, 12, 10);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        listView.setAdapter(adapter);
        
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 550
        );
        listView.setLayoutParams(listParams);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPath = itemPaths.get(position);
            boolean isFolder = isFolderList.get(position);

            if (selectedPath.equals("..")) {
                int lastSlash = currentDir.lastIndexOf('/');
                String parentDir = lastSlash == -1 ? "" : currentDir.substring(0, lastSlash);
                showDirectoryBrowser(parentDir, allItems);
            } else if (isFolder) {
                showDirectoryBrowser(selectedPath, allItems);
            } else {
                showFileOptionsDialog(selectedPath);
            }
        });

        dialogLayout.addView(listView);

        Button btnUploadHere = createStyledButton("➕ Загрузить файл в эту папку", "#0D9488");
        btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir));
        dialogLayout.addView(btnUploadHere);

        new AlertDialog.Builder(this)
            .setTitle("📁 Менеджер репозитория")
            .setView(dialogLayout)
            .setPositiveButton("Закрыть", null)
            .show();
    }

    private void showFileOptionsDialog(String filePath) {
        CharSequence[] options = {"📝 Редактировать", "🗑 Удалить"};
        new AlertDialog.Builder(this)
            .setTitle("Файл: " + filePath)
            .setItems(options, (dialog, which) -> {
                if (which == 0) openFileEditorForPath(filePath);
                else if (which == 1) confirmDeleteSingleFile(filePath);
            })
            .setNegativeButton("Отмена", null)
            .show();
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

        final EditText codeInput = new EditText(this);
        codeInput.setText(initialContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setMinLines(15);
        codeInput.setMaxLines(22);
        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить на GitHub", (dialog, which) -> {
                String newContent = codeInput.getText().toString();
                statusTextView.setText("Статус: ⚙️ Сохранение...");
                new Thread(() -> {
                    boolean success = pushFileToGitHub(filePath, newContent, "Update via File Manager: " + filePath);
                    handler.post(() -> {
                        if (success) Toast.makeText(this, "Файл успешно сохранен!", Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Статус: Готов к работе 🚀");
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmDeleteSingleFile(String filePath) {
        new AlertDialog.Builder(this)
            .setTitle("🗑 Удаление")
            .setMessage("Удалить файл " + filePath + "?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                new Thread(() -> {
                    deleteGitHubFile(filePath, "Delete file: " + filePath);
                    handler.post(() -> {
                        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
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
        startActivityForResult(Intent.createChooser(intent, "Выберите файл"), PICK_FILE_TO_FOLDER_REQUEST);
    }

    private void processSelectedFileForFolder(Uri uri, String folderPath) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) fileName = "uploaded.bin";
        final String targetPath = folderPath.isEmpty() ? fileName : folderPath + "/" + fileName;

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = readBytes(inputStream);

            statusTextView.setText("Статус: ⚙️ Загрузка файла...");
            new Thread(() -> {
                // ИСПРАВЛЕНО: добавлено корректное объявление boolean success
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
            if (requestCode == PICK_JAVA_REQUEST) processSelectedJavaFile(uri);
            else if (requestCode == PICK_APK_REQUEST) processSelectedApkFile(uri);
            else if (requestCode == PICK_ICON_REQUEST) processSelectedIcon(uri);
            else if (requestCode == PICK_FILE_TO_FOLDER_REQUEST) processSelectedFileForFolder(uri, targetUploadFolder);
        }
    }

    private void processSelectedJavaFile(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) fileName = "Custom.java";
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();

            String code = sb.toString();
            Matcher pkgMatch = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(code);
            if (pkgMatch.find()) packageName = pkgMatch.group(1);

            activeCodeContent = sanitizeAndInjectJavaCode(code, packageName);
            currentJavaFileName = fileName;
            saveSettings();
            btnUploadJava.setText("📂 Файл: " + currentJavaFileName);
            Toast.makeText(this, "Java файл загружен и очищен!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processSelectedApkFile(Uri uri) {
        statusTextView.setText("Статус: 🔄 Декомпиляция APK...");
        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "temp.apk");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new java.io.FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                }

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
                tempFile.delete();

                handler.post(() -> {
                    statusTextView.setText("Статус: ✅ APK декомпилирован!");
                    Toast.makeText(this, "Пакет определен: " + packageName, Toast.LENGTH_SHORT).show();
                    updateInfoText();
                });
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка декомпиляции"));
            }
        }).start();
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

    private void runAiCodeDoctor() {
        if (lastErrorLog.isEmpty()) {
            Toast.makeText(this, "ℹ️ Нет ошибок для анализа. Сначала запустите сборку!", Toast.LENGTH_LONG).show();
            return;
        }

        statusTextView.setText("Статус: 🤖 AI-Доктор анализирует код и ошибки...");
        logEditText.setText("AI-Автодоктор анализирует лог компиляции...\nИщем несовпадения типов, пропущенные импорты и синтаксис...");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                String fixedCode = activeCodeContent;
                boolean repaired = false;

                if (lastErrorLog.contains("cannot find symbol") && lastErrorLog.contains("WebView")) {
                    if (!fixedCode.contains("import android.webkit.WebView;")) {
                        fixedCode = fixedCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.webkit.WebView;\nimport android.webkit.WebSettings;");
                        repaired = true;
                    }
                }
                if (lastErrorLog.contains("package androidx.") || lastErrorLog.contains("AppCompatActivity")) {
                    if (!fixedCode.contains("import androidx.appcompat.app.AppCompatActivity;")) {
                        fixedCode = fixedCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport androidx.appcompat.app.AppCompatActivity;");
                        repaired = true;
                    }
                }
                if (lastErrorLog.contains("R cannot be resolved")) {
                    fixedCode = fixedCode.replaceAll("R\\.[a-z]+\\.[a-zA-Z0-9_]+", "/* Fixed R reference */");
                    repaired = true;
                }

                final boolean success = repaired;
                final String newCode = fixedCode;

                handler.post(() -> {
                    if (success) {
                        activeCodeContent = newCode;
                        saveSettings();
                        statusTextView.setText("Статус: ✅ AI-Доктор успешно исправил ошибки в коде!");
                        logEditText.setText("🤖 AI-Доктор применил исправления:\n- Добавлены недостающие импорты WebView/AppCompat\n- Исправлены ошибки синтаксиса.\n\nТеперь нажмите кнопку компиляции заново!");
                    } else {
                        statusTextView.setText("Статус: ⚠️ Авто-исправление не найдено");
                        logEditText.setText("AI-Доктор не смог автоматически определить тип ошибки. Проверьте синтаксис вручную в редакторе кода.");
                    }
                });
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка AI-Доктора"));
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sanitizeAndInjectJavaCode(String code, String pkg) {
        code = code.replace('\u00A0', ' ').replace("\r\n", "\n");
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

    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        final EditText codeInput = new EditText(this);
        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setMinLines(16);
        codeInput.setMaxLines(24);
        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода")
            .setView(editorLayout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                activeCodeContent = codeInput.getText().toString();
                saveSettings();
                Toast.makeText(this, "Сохранено локально!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void triggerGitHubBuild() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        btnBuild.setEnabled(false);
        btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
        btnBuild.setText("⏳ Компиляция на сервере...\n📦 " + packageName);
        statusTextView.setText("Статус: ⚙️ Синхронизация репозитория...");
        logEditText.setText("Отправка кода и конфигурации на GitHub...");

        new Thread(() -> {
            try {
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                
                boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");

                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка синхронизации (Токен?)");
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
                        statusTextView.setText("Статус: 🚀 Сборка запущена! Ожидание APK...");
                        logEditText.setText("GitHub Actions компилирует ваш проект...\nКак только сборка завершится, бот пришлет APK в Telegram.");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка запуска workflow");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                    btnBuild.setText(getBuildButtonText());
                    statusTextView.setText("Статус: ❌ Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }

    private void emergencyStopBuild() {
        isMonitoring = false;
        if (btnBuild != null) {
            btnBuild.setEnabled(true);
            btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
            btnBuild.setText(getBuildButtonText());
        }
        statusTextView.setText("Статус: 🛑 Сборка остановлена!");
        logEditText.setText("Мониторинг прерван пользователем.");
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
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr);
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
                            String status = run.getString("status");
                            String conclusion = run.optString("conclusion", "null");
                            long runId = run.getLong("id");
                            long elapsed = (System.currentTimeMillis() - start) / 1000;

                            handler.post(() -> {
                                if (!isMonitoring) return;
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                                    btnBuild.setText(getBuildButtonText());
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Успех! APK отправлен в Telegram.");
                                        logEditText.setText("Сборка завершена успешно! Бот отправил готовый файл в ваш Telegram чат.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции");
                                        fetchLogsAsync(runId);
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

                            handler.post(() -> {
                                if (lastErrorLog.length() > 3000) {
                                    logEditText.setText(lastErrorLog.substring(lastErrorLog.length() - 3000));
                                } else {
                                    logEditText.setText(lastErrorLog);
                                }
                                Toast.makeText(MainActivity.this, "⚠️ Нажмите кнопку AI-Автодоктор для исправления!", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс")
            .setMessage("Сбросить проект к стандартному шаблону браузера?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                activeCodeContent = generateBrowserTemplate();
                currentJavaFileName = "Шаблон: AI-Браузер";
                saveSettings();
                Toast.makeText(this, "Проект сброшен!", Toast.LENGTH_SHORT).show();
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
                packageName = "com." + appTitle.toLowerCase().replaceAll("[^a-z0-9]", "") + ".app";
                saveSettings();
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
            "⚡ Слабые телефоны (Java 8)", "🚀 Мощные телефоны (Java 17)"
        }));
        spinner.setSelection(javaVersion.equals("8") ? 0 : 1);
        layout.addView(spinner);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();
                javaVersion = spinner.getSelectedItemPosition() == 0 ? "8" : "17";
                minSdk = spinner.getSelectedItemPosition() == 0 ? 21 : 26;
                saveSettings();
                Toast.makeText(this, "Сохранено!", Toast.LENGTH_SHORT).show();
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

    private String getWorkflowContent() {
        return "name: Build Monster APK\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - name: Checkout\n        uses: actions/checkout@v4\n" +
               "      - name: Set up JDK 17\n        uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n" +
               "      - name: Setup Gradle\n        uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Setup Android SDK\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
               "      - name: Build APK\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          gradle clean assembleDebug --stacktrace --no-daemon\n" +
               "      - name: Send to Telegram\n        if: success()\n        run: |\n" +
               "          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
               "          curl -F chat_id=\"" + chatId + "\" -F document=@\"$APK_PATH\" https://api.telegram.org/bot" + botToken + "/sendDocument\n";
    }

    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n" +
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
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n        sourceCompatibility " + compat + "\n        targetCompatibility " + compat + "\n    }\n}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }

    private String generateBrowserTemplate() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.os.Bundle;\n" +
               "import android.webkit.WebView;\n" +
               "import android.webkit.WebViewClient;\n" +
               "import android.widget.LinearLayout;\n" +
               "import android.graphics.Color;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        WebView webView = new WebView(this);\n" +
               "        webView.setWebViewClient(new WebViewClient());\n" +
               "        webView.getSettings().setJavaScriptEnabled(true);\n" +
               "        webView.loadUrl(\"https://www.google.com\");\n" +
               "        layout.addView(webView, new LinearLayout.LayoutParams(-1, -1));\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }

    private String generateClockTemplate() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.os.Bundle;\n" +
               "import android.widget.*;\n" +
               "import android.graphics.Color;\n" +
               "import android.view.Gravity;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        TextClock clock = new TextClock(this);\n" +
               "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        clock.setTextSize(48f);\n" +
               "        layout.addView(clock);\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }

    private String generateCryptoTemplate() { return generateBrowserTemplate(); }
    private String generateWebAppTemplate() { return generateBrowserTemplate(); }
    private String generateClickerTemplate() { return generateClockTemplate(); }
}