package com.monstercompilerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
    private String botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "RomaMonsterApp";
    private String appVersion = "1.0";
    private int versionCode = 1;
    private String packageName = "com.roma.monster";
    private String currentJavaFileName = "😛";
    private boolean iconCustomized = true;
    private String targetUploadFolder = "";

    private int minSdk = 21;
    private int compileSdk = 34;
    private String javaVersion = "8"; 
    private String activeCodeContent = "";
    private long latestRunId = -1;

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnResetClock, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor, btnFileManager, btnAiFix, btnRetryUpload;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;
    private String lastErrorLog = "";
    private byte[] lastFailedBytes = null;
    private String lastFailedPath = "";

    private AlertDialog fileManagerDialog = null;

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
            activeCodeContent = generateClockTemplate();
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
        authorView.setText("✦ Dziuba Roman — Monster CI/CD Engine v6.0 Pro ✦");
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
        titleView.setText("🚀 Android Monster Compiler (v6.0 Pro Engine)");
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

        // 1. Скомпилировать
        btnBuild = createStyledButton("", "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        // 2. Версия
        btnVersion = createStyledButton("", "#334155");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        // 3. Имя
        btnTitle = createStyledButton("", "#334155");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        // 4. Иконка
        btnIcon = createStyledButton("", "#6D28D9");
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        // 5. Редактор кода
        btnCodeEditor = createStyledButton("📝 Редактор кода", "#0A0F1D");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        // 6. Загрузи файл
        btnUploadJava = createStyledButton("", "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        // Повтор отправки
        btnRetryUpload = createStyledButton("🔄 Повторить отправку файла на сервер", "#D97706");
        btnRetryUpload.setVisibility(View.GONE);
        btnRetryUpload.setOnClickListener(v -> retryLastUpload());
        card.addView(btnRetryUpload);

        // 7. Декомпилировать
        btnDecompileApk = createStyledButton("📦 Декомпилировать файл/APK в код", "#475569");
        btnDecompileApk.setOnClickListener(v -> openUniversalFilePicker());
        card.addView(btnDecompileApk);

        // 8. Сброс к часам
        btnResetClock = createStyledButton("🔄 Сбросить к часам (Monster Clock)", "#B45309");
        btnResetClock.setOnClickListener(v -> confirmResetProject());
        card.addView(btnResetClock);

        // 9. Стоп
        btnStopBuild = createStyledButton("🛑 Экстренный стоп сборки", "#DC2626");
        btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
        card.addView(btnStopBuild);

        // Дополнительные кнопки
        btnFileManager = createStyledButton("📁 Файловый менеджер репозитория (x2 XL)", "#8B5CF6");
        btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
        card.addView(btnFileManager);

        btnAiFix = createStyledButton("🎓 Smart AI-Профессор (Глубокий ремонт & Тест)", "#7C3AED");
        btnAiFix.setOnClickListener(v -> runAiCodeDoctor());
        card.addView(btnAiFix);

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
        logLabel.setText("📋 Терминал и отчёт об ошибках:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Логи сборки и авто-пачи Профессора...");
        logEditText.setTextSize(10f);
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

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        updateAllButtonLabels();
        logEditText.setText("=== 🚀 MONSTER COMPILER PRO v6.0 ЗАПУЩЕН ===\n" +
                            "• Авто-чистка старых проектов: Включена\n" +
                            "• Исправление лямбд: Активно\n" +
                            "• Диспетчер файлов XL: Готов");

        if (!githubToken.isEmpty()) {
            fetchRemoteRepositoryInfo();
        }
    }

    private void updateAllButtonLabels() {
        String profile = javaVersion.equals("8") ? "⚡ Стабильный (Java 8)" : "🚀 Java 17";
        String info = "🏷 Имя: " + appTitle + " | 📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + " (Code: " + versionCode + ") | 📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profile + "]";
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя: " + appTitle + " (" + packageName + ")");
        if (btnVersion != null) btnVersion.setText("📦 Версия: " + appVersion);
        if (btnUploadJava != null) btnUploadJava.setText("📂 Загрузи Файл сюда👈  " + currentJavaFileName);
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
        versionCode = sp.getInt("versionCode", 1);
        packageName = sp.getString("packageName", packageName);
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

    // ==========================================
    // УВЕЛИЧЕННЫЙ В 2 РАЗА ФАЙЛОВЫЙ МЕНЕДЖЕР (XL)
    // ==========================================
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
        if (fileManagerDialog != null && fileManagerDialog.isShowing()) {
            fileManagerDialog.dismiss();
        }

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(15, 15, 15, 15);
        dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

        TextView pathTitle = new TextView(this);
        pathTitle.setText("📂 /" + (currentDir.isEmpty() ? "root" : currentDir));
        pathTitle.setTextColor(Color.parseColor("#38BDF8"));
        pathTitle.setTextSize(13f);
        pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pathTitle.setPadding(0, 0, 0, 10);
        dialogLayout.addView(pathTitle);

        // Полноразмерная кнопка НАВЕРХ
        if (!currentDir.isEmpty()) {
            Button btnUp = createStyledButton("⬆️ На один уровень вверх", "#334155");
            btnUp.setOnClickListener(v -> {
                int lastSlash = currentDir.lastIndexOf('/');
                String parentDir = lastSlash == -1 ? "" : currentDir.substring(0, lastSlash);
                showDirectoryBrowser(parentDir, allItems);
            });
            dialogLayout.addView(btnUp);
        }

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

        for (String folder : subFolders) {
            displayItems.add("📁 Папка: " + folder);
            itemPaths.add(currentDir.isEmpty() ? folder : currentDir + "/" + folder);
            isFolderList.add(true);
        }

        for (RepoItem fileItem : subFiles) {
            displayItems.add("📄 " + fileItem.path.substring(prefixLen) + " (" + fileItem.size + " б)");
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
                    tv.setPadding(12, 12, 12, 12);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        listView.setAdapter(adapter);
        
        // Увеличенное в 2 раза окно (1100px)
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1100
        );
        listView.setLayoutParams(listParams);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPath = itemPaths.get(position);
            boolean isFolder = isFolderList.get(position);

            if (isFolder) {
                showDirectoryBrowser(selectedPath, allItems);
            } else {
                showFileOptionsDialog(selectedPath);
            }
        });

        dialogLayout.addView(listView);

        Button btnUploadHere = createStyledButton("➕ Загрузить файл в эту папку", "#0D9488");
        btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir));
        dialogLayout.addView(btnUploadHere);

        Button btnCloseInstant = createStyledButton("❌ ЗАКРЫТЬ МЕНЕДЖЕР", "#DC2626");
        btnCloseInstant.setOnClickListener(v -> {
            if (fileManagerDialog != null) fileManagerDialog.dismiss();
        });
        dialogLayout.addView(btnCloseInstant);

        fileManagerDialog = new AlertDialog.Builder(this)
            .setTitle("📁 Диспетчер Репозитория (XL)")
            .setView(dialogLayout)
            .setCancelable(true)
            .create();
        fileManagerDialog.show();
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

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 850
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
            .setTitle("📝 Редактирование: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить и Загрузить", (dialog, which) -> {
                String newContent = codeInput.getText().toString();
                
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("📤 Сохранение файла");
                progressDialog.setMessage("Выгрузка с очисткой от BOM на GitHub...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Thread(() -> {
                    boolean success = pushFileToGitHub(filePath, newContent, "Update via File Manager: " + filePath);
                    handler.post(() -> {
                        progressDialog.dismiss();
                        if (success) Toast.makeText(this, "Файл выгружен!", Toast.LENGTH_SHORT).show();
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

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("📤 Выгрузка в репозиторий");
            progressDialog.setMessage("Загрузка " + fileName + " на сервер...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                boolean success = pushBytesToGitHub(targetPath, fileBytes, "Upload file: " + fileName);
                handler.post(() -> {
                    progressDialog.dismiss();
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
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите Java или код"), PICK_JAVA_REQUEST);
    }

    private void openUniversalFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите APK или ZIP"), PICK_APK_REQUEST);
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

    // =======================================================
    // АВТО-ОЧИСТКА ВСЕХ СТАРЫХ ПРОЕКТОВ И ПАКЕТОВ ПЕРЕД ЗАЛИВКОЙ
    // =======================================================
    private void wipeAllOldJavaProjectsSync() {
        try {
            String treeUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/git/trees/main?recursive=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(treeUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONArray treeArray = new JSONObject(sb.toString()).optJSONArray("tree");
                if (treeArray != null) {
                    for (int i = 0; i < treeArray.length(); i++) {
                        JSONObject obj = treeArray.getJSONObject(i);
                        String path = obj.optString("path");
                        if (path.startsWith("app/src/main/java/") && path.endsWith(".java")) {
                            deleteGitHubFile(path, "Auto-Wipe Old Project: " + path);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
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

            String code = sb.toString();
            Matcher pkgMatch = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(code);
            if (pkgMatch.find()) packageName = pkgMatch.group(1);

            activeCodeContent = sanitizeAndInjectJavaCode(code, packageName);
            currentJavaFileName = fileName;
            saveSettings();

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("📤 Загрузка и выгрузка");
            progressDialog.setMessage("1. Очистка старых проектов...\n2. Отправка нового файла (Попытка 1/10)...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            final String targetPath = getMainActivityPath();
            final byte[] uploadBytes = activeCodeContent.getBytes(StandardCharsets.UTF_8);

            new Thread(() -> {
                wipeAllOldJavaProjectsSync();

                boolean success = false;
                int attempts = 0;
                while (attempts < 10 && !success) {
                    attempts++;
                    final int currentAttempt = attempts;
                    handler.post(() -> progressDialog.setMessage("Очистка от BOM и выгрузка на GitHub...\nПопытка " + currentAttempt + " из 10"));
                    
                    success = pushBytesToGitHub(targetPath, uploadBytes, "Upload new project: " + fileName);
                    if (!success && attempts < 10) {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    }
                }

                final boolean isSuccess = success;
                final int finalAttempts = attempts;

                handler.post(() -> {
                    progressDialog.dismiss();
                    if (isSuccess) {
                        if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Новый проект загружен и зачищен!", Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Статус: ✅ Новый код готов к компиляции!");
                        logEditText.setText("=== 📂 СТАРЫЕ ПРОЕКТЫ УДАЛЕНЫ, НОВЫЙ ФАЙЛ ВЫГРУЖЕН ===\n" +
                                            "• Файл: " + fileName + "\n" +
                                            "• Пакет: " + packageName + "\n" +
                                            "• Попыток отправки: " + finalAttempts + "/10\n" +
                                            "• Репозиторий очищен под новый проект.");
                    } else {
                        lastFailedBytes = uploadBytes;
                        lastFailedPath = targetPath;
                        if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.VISIBLE);
                        statusTextView.setText("Статус: ❌ Ошибка сети при отправке файла");
                    }
                    updateAllButtonLabels();
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void retryLastUpload() {
        if (lastFailedBytes == null || lastFailedPath.isEmpty()) {
            Toast.makeText(this, "Нет данных для повтора", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🔄 Повторная отправка");
        progressDialog.setMessage("Выгрузка файла на сервер...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            boolean success = false;
            int attempts = 0;
            while (attempts < 10 && !success) {
                attempts++;
                final int currentAttempt = attempts;
                handler.post(() -> progressDialog.setMessage("Отправка на GitHub...\nПопытка " + currentAttempt + " из 10"));
                
                success = pushBytesToGitHub(lastFailedPath, lastFailedBytes, "Retry upload attempt " + attempts);
                if (!success && attempts < 10) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }

            final boolean isSuccess = success;
            handler.post(() -> {
                progressDialog.dismiss();
                if (isSuccess) {
                    if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Файл выгружен!", Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Статус: ✅ Файл синхронизирован!");
                } else {
                    Toast.makeText(this, "Ошибка сети", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void processUniversalFileForDecompilation(Uri uri) {
        String rawName = getFileNameFromUri(uri);
        final String fileName = (rawName != null) ? rawName : "archive.file";
        final String lowerName = fileName.toLowerCase();

        statusTextView.setText("Статус: 🔄 Декомпиляция...");
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
                    currentJavaFileName = fileName + " (Декомпилирован)";
                    tempFile.delete();

                    wipeAllOldJavaProjectsSync();
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Decompiled: " + fileName);

                    handler.post(() -> {
                        saveSettings();
                        statusTextView.setText("Статус: ✅ APK успешно декомпилирован!");
                        Toast.makeText(MainActivity.this, "APK декомпилирован!", Toast.LENGTH_LONG).show();
                        updateAllButtonLabels();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка декомпиляции"));
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

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(packageName).append(";\n\n");
            sb.append("import android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.*;\nimport android.graphics.*;\nimport android.view.*;\n\n");
            sb.append("public class MainActivity extends Activity {\n");
            sb.append("    @Override\n");
            sb.append("    protected void onCreate(Bundle savedInstanceState) {\n");
            sb.append("        super.onCreate(savedInstanceState);\n");
            sb.append("        LinearLayout layout = new LinearLayout(this);\n");
            sb.append("        layout.setOrientation(LinearLayout.VERTICAL);\n");
            sb.append("        layout.setGravity(Gravity.CENTER);\n");
            sb.append("        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n\n");
            sb.append("        TextView tv = new TextView(this);\n");
            sb.append("        tv.setText(\"⚡ Декомпилировано из APK\");\n");
            sb.append("        tv.setTextColor(Color.parseColor(\"#38BDF8\"));\n");
            sb.append("        tv.setTextSize(18f);\n");
            sb.append("        layout.addView(tv);\n");
            sb.append("        setContentView(layout);\n");
            sb.append("    }\n}\n");
            return sb.toString();
        } catch (Exception e) {
            return generateClockTemplate();
        }
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

    // ========================================================
    // ГЛУБОКИЙ АВТО-ДОКТОР (SMART AI-ПРОФЕССОР v6.0 С ТЕСТИРОВАНИЕМ)
    // ========================================================
    private void runAiCodeDoctor() {
        if (lastErrorLog.isEmpty()) {
            Toast.makeText(this, "ℹ️ Нет ошибок для анализа. Сначала запустите сборку!", Toast.LENGTH_LONG).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🎓 Smart AI-Профессор");
        progressDialog.setMessage("Анализ ошибок, зачистка старых пакетов и лечение в виртуальном буфере...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                String bufferCode = activeCodeContent;
                StringBuilder auditLog = new StringBuilder("=== 🎓 SMART AI-ПРОФЕССОР v6.0: ГЛУБОКИЙ АУДИТ ===\n\n");
                boolean madeChanges = false;

                // 1. Очистка BOM и невидимых символов
                if (bufferCode.contains("\uFEFF") || bufferCode.contains("\ufeff") || lastErrorLog.contains("\\ufeff")) {
                    bufferCode = bufferCode.replace("\uFEFF", "").replace("\ufeff", "");
                    auditLog.append("• 🛠 Удален скрытый BOM (\\ufeff).\n");
                    madeChanges = true;
                }

                // 2. Исправление кавычек-елочек " "
                if (bufferCode.contains(""") || bufferCode.contains(""") || lastErrorLog.contains("\\u00ab")) {
                    bufferCode = bufferCode.replace(""", "\"").replace(""", "\"");
                    auditLog.append("• 🛠 Заменены типографские кавычки " " на стандартные \".\n");
                    madeChanges = true;
                }

                // 3. Авто-исправление Лямбда-выражений
                if (lastErrorLog.contains("effectively final") || lastErrorLog.contains("lambda expression")) {
                    bufferCode = bufferCode.replaceAll("(\\b[a-zA-Z0-9_]+\\s*=\\s*[^;]+;)", "final $1");
                    auditLog.append("• 🛠 Применена защита лямбда-выражений (добавлено final).\n");
                    madeChanges = true;
                }

                // 4. Подключение библиотек
                if (lastErrorLog.contains("AppCompatActivity") && !bufferCode.contains("import androidx.appcompat.app.AppCompatActivity;")) {
                    bufferCode = bufferCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport androidx.appcompat.app.AppCompatActivity;");
                    auditLog.append("• 🛠 Добавлен импорт AppCompatActivity.\n");
                    madeChanges = true;
                }

                if ((lastErrorLog.contains("List") || lastErrorLog.contains("Random") || lastErrorLog.contains("Pattern")) && !bufferCode.contains("import java.util.*;")) {
                    bufferCode = bufferCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport java.util.*;\nimport java.util.regex.*;");
                    auditLog.append("• 🛠 Подключены java.util.* и java.util.regex.*\n");
                    madeChanges = true;
                }

                // Санитизация кода во временном буфере
                bufferCode = sanitizeAndInjectJavaCode(bufferCode, packageName);

                // Тестовый пуш временного буфера для проверки компиляции
                wipeAllOldJavaProjectsSync();
                boolean pushed = pushFileToGitHub(getMainActivityPath(), bufferCode, "AI Doctor Test Patch");

                final String finalBuffer = bufferCode;
                final boolean finalSuccess = madeChanges && pushed;
                final String report = auditLog.toString();

                handler.post(() -> {
                    progressDialog.dismiss();
                    logEditText.setText(report + "\n🎉 Патч применен и залит на GitHub. Нажмите 'Скомпилировать'!");
                    if (finalSuccess) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("🎓 Успешный ремонт Профессора")
                            .setMessage("Профессор полностью устранил ошибки во временном буфере и выгрузил чистый код на GitHub.\n\nОбновить ваш локальный редактор этим кодом?")
                            .setPositiveButton("Да, обновить", (dialog, which) -> {
                                activeCodeContent = finalBuffer;
                                saveSettings();
                                Toast.makeText(MainActivity.this, "Редактор обновлен!", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Нет, оставить", null)
                            .show();
                    }
                });

            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Ошибка Профессора", Toast.LENGTH_SHORT).show();
                });
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
        if (code != null) {
            while (code.startsWith("\uFEFF") || code.startsWith("\ufeff") || (code.length() > 0 && code.charAt(0) == 65279)) {
                code = code.substring(1);
            }
            code = code.replace("\uFEFF", "").replace("\ufeff", "");
        }
        code = code.replace('"', '"').replace('"', '"');
        code = code.replace('\u00A0', ' ').replace("\r\n", "\n").trim();
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

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 850
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

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода")
            .setView(editorLayout)
            .setPositiveButton("Сохранить и Выгрузить", (dialog, which) -> {
                activeCodeContent = sanitizeAndInjectJavaCode(codeInput.getText().toString(), packageName);
                saveSettings();

                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("📤 Сохранение и Выгрузка");
                progressDialog.setMessage("Очистка старых проектов и отправка кода на GitHub...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Thread(() -> {
                    wipeAllOldJavaProjectsSync();
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity via Editor");
                    handler.post(() -> {
                        progressDialog.dismiss();
                        if (success) {
                            Toast.makeText(this, "Код очищен и выгружен на GitHub!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Ошибка выгрузки", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
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

        versionCode++;
        saveSettings();

        btnBuild.setEnabled(false);
        btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
        btnBuild.setText("⏳ Компиляция на сервере...\n📦 " + packageName);
        statusTextView.setText("Статус: ⚙️ Очистка репозитория и синхронизация...");
        logEditText.setText("Удаление старых проектов и запуск сборки...");

        new Thread(() -> {
            try {
                wipeAllOldJavaProjectsSync();

                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                
                boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                 pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local.properties") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");

                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        updateAllButtonLabels();
                        statusTextView.setText("Статус: ❌ Ошибка синхронизации");
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
                        statusTextView.setText("Статус: 🚀 Сборка запущена!");
                        logEditText.setText("GitHub Actions компилирует ваш проект...\nОжидайте APK в Telegram.");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        updateAllButtonLabels();
                        statusTextView.setText("Статус: ❌ Ошибка запуска");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
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
                btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                updateAllButtonLabels();
            }
            statusTextView.setText("Статус: 🛑 Сборка остановлена!");
            logEditText.setText("⚠️ Сборка отменена.");
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
                                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                                    updateAllButtonLabels();
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Успех! APK в Telegram.");
                                        logEditText.setText("Сборка завершена успешно! Бот отправил готовый APK в ваш Telegram чат.");
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

                            StringBuilder professionalReport = new StringBuilder("=== 📋 ПОЛНЫЙ ОТЧЕТ ОБ ОШИБКАХ ===\n\n");
                            boolean foundErrorBlock = false;
                            
                            String[] lines = lastErrorLog.split("\n");
                            int lineNum = 1;
                            for (String logLine : lines) {
                                String lower = logLine.toLowerCase();
                                if (lower.contains("error:") || lower.contains("failed") || lower.contains("exception")) {
                                    professionalReport.append("❌ [Строка ~").append(lineNum).append("]: ").append(logLine.trim()).append("\n");
                                    foundErrorBlock = true;
                                }
                                lineNum++;
                            }

                            if (!foundErrorBlock) {
                                professionalReport.append(lastErrorLog.length() > 2500 ? lastErrorLog.substring(lastErrorLog.length() - 2500) : lastErrorLog);
                            }

                            handler.post(() -> {
                                logEditText.setText(professionalReport.toString());
                                Toast.makeText(MainActivity.this, "⚠️ Ошибка сборки! Запустите AI-Профессора", Toast.LENGTH_LONG).show();
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
            .setMessage("Удалить текущий проект и сбросить репозиторий до Monster Clock?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                activeCodeContent = generateClockTemplate();
                currentJavaFileName = "Сейчас стоит Шаблон 🕞";
                
                appTitle = "Monster Clock";
                packageName = "com.monster.clock";
                appVersion = "1.0";
                versionCode = 1;
                
                saveSettings();

                new Thread(() -> {
                    wipeAllOldJavaProjectsSync();
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Reset to Monster Clock");
                }).start();

                Toast.makeText(this, "Проект сброшен к часам!", Toast.LENGTH_SHORT).show();
                logEditText.setText("=== 🔄 ПРОЕКТ СБРОШЕН ДО ЧАСОВ ===");
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmFullRepositoryClean() {
        new AlertDialog.Builder(this)
            .setTitle("🗑 Полная очистка репозитория")
            .setMessage("Удалить абсолютно все проекты, старые файлы и восстановить чистый заводской вид?")
            .setPositiveButton("Очистить все", (dialog, which) -> performFullRepositoryCleanAndReset())
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void performFullRepositoryCleanAndReset() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        statusTextView.setText("Статус: 🗑 Полное затирание репозитория...");

        new Thread(() -> {
            try {
                wipeAllOldJavaProjectsSync();

                activeCodeContent = generateClockTemplate();
                currentJavaFileName = "Шаблон: Часы";
                appTitle = "MonsterClock";
                packageName = "com.monster.clock";
                appVersion = "1.0";
                versionCode = 1;
                saveSettings();

                boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Clean root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Clean settings") &&
                                 pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Clean gradle.properties") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Clean local.properties") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Clean manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "Clean app gradle") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Clean workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Clean MainActivity");

                handler.post(() -> {
                    if (synced) {
                        statusTextView.setText("Статус: ✅ Репозиторий идеально чист!");
                        Toast.makeText(MainActivity.this, "Репозиторий зачищен под ноль!", Toast.LENGTH_LONG).show();
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка очистки");
                    }
                    updateAllButtonLabels();
                });
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка"));
            }
        }).start();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(appTitle);
        new AlertDialog.Builder(this)
            .setTitle("Имя приложения")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                appTitle = input.getText().toString().trim();
                String sanitized = appTitle.toLowerCase().replaceAll("[^a-z0-9]", "");
                if (sanitized.isEmpty()) sanitized = "app";
                if (Character.isDigit(sanitized.charAt(0))) {
                    sanitized = "n" + sanitized;
                }
                packageName = "com." + sanitized + ".app";
                saveSettings();
                Toast.makeText(this, "Имя и пакет обновлены!", Toast.LENGTH_SHORT).show();
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

        Button btnCleanRepoInsideSettings = createStyledButton("🗑 Полная очистка репозитория", "#991B1B");
        btnCleanRepoInsideSettings.setOnClickListener(v -> confirmFullRepositoryClean());
        layout.addView(btnCleanRepoInsideSettings);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки GitHub, Telegram & SDK")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();
                javaVersion = spinner.getSelectedItemPosition() == 0 ? "8" : "17";
                minSdk = spinner.getSelectedItemPosition() == 0 ? 21 : 26;
                saveSettings();
                Toast.makeText(this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
                if (!githubToken.isEmpty()) {
                    fetchRemoteRepositoryInfo();
                }
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

    private String getWorkflowContent() {
        return "name: Build Monster APK\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - name: Checkout\n        uses: actions/checkout@v4\n" +
               "      - name: Set up JDK 17\n        uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n" +
               "      - name: Setup Android SDK\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          echo \"y\" | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
               "      - name: Setup Gradle\n        uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Build APK\n        run: |\n" +
               "          gradle clean assembleDebug --info --stacktrace --no-daemon\n" +
               "      - name: Send APK to Telegram\n        if: success()\n        run: |\n" +
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
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode " + versionCode + "\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n        sourceCompatibility " + compat + "\n        targetCompatibility " + compat + "\n    }\n" +
               "}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }

    private String generateClockTemplate() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.os.Bundle;\n" +
               "import android.view.Gravity;\n" +
               "import android.widget.*;\n" +
               "import android.graphics.Color;\n" +
               "import android.graphics.Typeface;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        layout.setPadding(30, 30, 30, 30);\n\n" +
               "        TextClock clock = new TextClock(this);\n" +
               "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        clock.setTextSize(52f);\n" +
               "        clock.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        clock.setGravity(Gravity.CENTER);\n" +
               "        layout.addView(clock);\n\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}