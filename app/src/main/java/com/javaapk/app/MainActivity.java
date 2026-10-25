package com.javaapk.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfig";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;
    private static final int PICK_APK_REQUEST = 1003;
    private static final int PICK_FILE_TO_FOLDER_REQUEST = 1004;

    private String githubToken = "";
    private String botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "RomaClockApp";
    private String appVersion = "1.0";
    private String packageName = "com.roma.nativeapp";
    private String currentJavaFileName = "Шаблон: Часы с фонариком (MainActivity.java)";
    private boolean iconCustomized = true;
    private String targetUploadFolder = "";

    private int minSdk = 24;
    private int compileSdk = 34;
    private String javaVersion = "17"; 
    private String activeCodeContent = "";

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor, btnFileManager;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;

    public static class RepoItem {
        public String path;
        public String type; // "tree" или "blob"
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
            activeCodeContent = generateMainActivity();
        }

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(20, 20, 20, 20);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Created by Dziuba Roman ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(16f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 10);
        container.addView(authorView);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(24f);
        clock.setTypeface(Typeface.MONOSPACE);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(0, 0, 0, 10);
        container.addView(clock);

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
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(this);
        titleView.setText("📱 Android CI/CD Controller (Pure Java)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 12);
        card.addView(titleView);

        infoTextView = new TextView(this);
        updateInfoText();
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(12f);
        infoTextView.setPadding(15, 12, 15, 12);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(14f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 12);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        btnBuild = createStyledButton(getBuildButtonText(), "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        btnStopBuild = createStyledButton("🛑 Экстренный стоп сборки", "#DC2626");
        btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
        card.addView(btnStopBuild);

        btnFileManager = createStyledButton("📁 Файловый менеджер репозитория", "#8B5CF6");
        btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
        card.addView(btnFileManager);

        btnCodeEditor = createStyledButton("📝 Редактор кода (С подсказками)", "#059669");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        btnUploadJava = createStyledButton("📂 Загрузить файл: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        btnDecompileApk = createStyledButton("📦 Декомпилировать APK в код", "#7C3AED");
        btnDecompileApk.setOnClickListener(v -> openApkFilePicker());
        card.addView(btnDecompileApk);

        btnReset = createStyledButton("🔄 Полный сброс (Очистка + Часы)", "#D97706");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        btnTitle = createStyledButton("🏷 Имя: " + appTitle + " (" + packageName + ")", "#475569");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        btnVersion = createStyledButton("📦 Версия приложения: " + appVersion, "#475569");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        btnIcon = createStyledButton(getIconBtnText(), "#7C3AED");
        updateIconButtonPreview();
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        btnSettings = createStyledButton("⚙️ Настройки API, SDK и Java", "#334155");
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        card.addView(btnSettings);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(13f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 12, 0, 8);
        card.addView(statusTextView);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Расширенный терминал / Логи:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(12f);
        logLabel.setPadding(0, 8, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь отображаются логи компиляции и ошибки GitHub Actions...");
        logEditText.setTextSize(11f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(6);
        logEditText.setMaxLines(14);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать логи в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("CI/CD Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи успешно скопированы!", Toast.LENGTH_SHORT).show();
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
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private String getBuildButtonText() {
        return "🚀 Скомпилировать APK (Java)\n📦 Пакет: " + packageName + " (v" + appVersion + ")";
    }

    private String getIconBtnText() {
        return iconCustomized ? "🖼 Иконка: 🕞 Часы (Шаблон)" : "🖼 Иконка: 🎨 Пользовательская";
    }

    private void updateIconButtonPreview() {
        if (btnIcon != null) {
            btnIcon.setText(getIconBtnText());
        }
    }

    private void updateInfoText() {
        String profileType = javaVersion.equals("8") ? "⚡ Слабый телефон" : "🚀 Мощный телефон";
        String info = "🏷 Имя: " + appTitle + "\n" +
                      "📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + "\n" +
                      "📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profileType + "]";
        if (infoTextView != null) {
            infoTextView.setText(info);
        }
        if (btnTitle != null) {
            btnTitle.setText("🏷 Имя: " + appTitle + " (" + packageName + ")");
        }
        if (btnVersion != null) {
            btnVersion.setText("📦 Версия приложения: " + appVersion);
        }
        if (btnBuild != null) {
            btnBuild.setText(getBuildButtonText());
        }
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
                        statusTextView.setText("Статус: ✅ Актуальные данные подтянуты с GitHub!");
                        logEditText.setText("Синхронизировано:\nПакет: " + packageName + "\nВерсия: " + appVersion);
                    });
                } else {
                    handler.post(() -> statusTextView.setText("Статус: Готов к работе"));
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: Готов к работе"));
            }
        }).start();
    }

    private void openRepositoryFileManager() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }
        statusTextView.setText("Статус: 🔄 Загрузка дерева репозитория...");
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
                        statusTextView.setText("Статус: Готов к работе");
                        showDirectoryBrowser("", items);
                    });
                } else {
                    handler.post(() -> {
                        statusTextView.setText("Статус: ❌ Ошибка загрузки дерева");
                        Toast.makeText(this, "Не удалось загрузить структуру репозитория", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    statusTextView.setText("Статус: ❌ Ошибка сети");
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showDirectoryBrowser(String currentDir, java.util.List<RepoItem> allItems) {
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(20, 20, 20, 20);
        dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

        TextView pathTitle = new TextView(this);
        pathTitle.setText("📂 Путь: /" + (currentDir.isEmpty() ? "(корень)" : currentDir));
        pathTitle.setTextColor(Color.parseColor("#38BDF8"));
        pathTitle.setTextSize(13f);
        pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pathTitle.setPadding(0, 0, 0, 10);
        dialogLayout.addView(pathTitle);

        java.util.Set<String> subFolders = new java.util.TreeSet<>();
        java.util.List<RepoItem> subFiles = new java.util.ArrayList<>();

        String prefix = currentDir.isEmpty() ? "" : currentDir + "/";
        int prefixLen = prefix.length();

        for (RepoItem item : allItems) {
            if (currentDir.isEmpty()) {
                if (item.path.equals(currentDir)) continue;
                int slashIdx = item.path.indexOf('/');
                if (slashIdx == -1) {
                    if ("tree".equals(item.type)) {
                        subFolders.add(item.path);
                    } else if ("blob".equals(item.type)) {
                        subFiles.add(item);
                    }
                } else {
                    String topFolder = item.path.substring(0, slashIdx);
                    subFolders.add(topFolder);
                }
            } else {
                if (item.path.startsWith(prefix) && item.path.length() > prefixLen) {
                    String subPath = item.path.substring(prefixLen);
                    int slashIdx = subPath.indexOf('/');
                    if (slashIdx == -1) {
                        if ("tree".equals(item.type)) {
                            subFolders.add(subPath);
                        } else if ("blob".equals(item.type)) {
                            subFiles.add(item);
                        }
                    } else {
                        String subFolder = subPath.substring(0, slashIdx);
                        subFolders.add(subFolder);
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
            displayItems.add("📄 Файл: " + fileItem.path.substring(prefixLen) + " (" + fileItem.size + " байт)");
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
                    tv.setTextSize(13f);
                    tv.setPadding(15, 12, 15, 12);
                }
                view.setBackgroundColor(Color.parseColor("#0F172A"));
                return view;
            }
        };
        listView.setAdapter(adapter);
        
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 600
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
                showFileOptionsDialog(selectedPath, allItems);
            }
        });

        dialogLayout.addView(listView);

        Button btnUploadHere = createStyledButton("➕ Загрузить файл в эту папку", "#0D9488");
        btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir));
        dialogLayout.addView(btnUploadHere);

        if (!currentDir.isEmpty()) {
            Button btnDeleteFolderContent = createStyledButton("🗑 Удалить ВСЕ содержимое этой папки", "#DC2626");
            btnDeleteFolderContent.setOnClickListener(v -> confirmDeleteFolderContents(currentDir, allItems));
            dialogLayout.addView(btnDeleteFolderContent);
        }

        new AlertDialog.Builder(this)
            .setTitle("📁 Файловый менеджер")
            .setView(dialogLayout)
            .setPositiveButton("Закрыть", null)
            .show();
    }

    private void showFileOptionsDialog(String filePath, java.util.List<RepoItem> allItems) {
        CharSequence[] options = {"📝 Редактировать файл", "🗑 Удалить файл"};
        new AlertDialog.Builder(this)
            .setTitle("Файл: " + filePath)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openFileEditorForPath(filePath);
                } else if (which == 1) {
                    confirmDeleteSingleFile(filePath);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void openFileEditorForPath(String filePath) {
        statusTextView.setText("Статус: 🔄 Загрузка файла " + filePath + "...");
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
                        statusTextView.setText("Статус: Готов к работе");
                        showCodeEditorDialogForPath(filePath, fileContent);
                    });
                } else {
                    handler.post(() -> {
                        statusTextView.setText("Статус: Готов к работе");
                        Toast.makeText(this, "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    statusTextView.setText("Статус: Готов к работе");
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showCodeEditorDialogForPath(String filePath, String initialContent) {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        TextView title = new TextView(this);
        title.setText("Редактирование: " + filePath);
        title.setTextColor(Color.parseColor("#F59E0B"));
        title.setTextSize(12f);
        editorLayout.addView(title);

        final EditText codeInput = new EditText(this);
        codeInput.setText(initialContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(15, 15, 15, 15);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setMinLines(15);
        codeInput.setMaxLines(22);
        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор файла")
            .setView(editorLayout)
            .setPositiveButton("Сохранить на GitHub", (dialog, which) -> {
                String newContent = cleanInvisibleCharacters(codeInput.getText().toString());
                statusTextView.setText("Статус: ⚙️ Сохранение файла " + filePath + "...");
                new Thread(() -> {
                    boolean success = pushFileToGitHub(filePath, newContent, "Update file via File Manager: " + filePath);
                    handler.post(() -> {
                        if (success) {
                            statusTextView.setText("Статус: ✅ Файл успешно сохранен!");
                            Toast.makeText(this, "Файл обновлен на GitHub", Toast.LENGTH_SHORT).show();
                        } else {
                            statusTextView.setText("Статус: ❌ Ошибка сохранения");
                        }
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmDeleteSingleFile(String filePath) {
        new AlertDialog.Builder(this)
            .setTitle("🗑 Удаление файла")
            .setMessage("Удалить файл " + filePath + "?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                statusTextView.setText("Статус: 🔄 Удаление файла...");
                new Thread(() -> {
                    deleteGitHubFile(filePath, "Delete file via File Manager: " + filePath);
                    handler.post(() -> {
                        statusTextView.setText("Статус: ✅ Файл удален!");
                        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
                        openRepositoryFileManager();
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmDeleteFolderContents(String dirPath, java.util.List<RepoItem> allItems) {
        if (dirPath.isEmpty()) return;
        new AlertDialog.Builder(this)
            .setTitle("🗑 Полное очищение папки")
            .setMessage("Вы уверены, что хотите полностью удалить все файлы внутри папки \"" + dirPath + "\"?")
            .setPositiveButton("Удалить всё содержимое", (dialog, which) -> {
                statusTextView.setText("Статус: 🔄 Очистка папки " + dirPath + "...");
                logEditText.setText("Удаление содержимого папки: " + dirPath);
                new Thread(() -> {
                    String prefix = dirPath + "/";
                    int deletedCount = 0;
                    for (RepoItem item : allItems) {
                        if (item.path.startsWith(prefix) && item.type.equals("blob")) {
                            deleteGitHubFile(item.path, "Delete folder contents in " + dirPath);
                            deletedCount++;
                        }
                    }
                    final int count = deletedCount;
                    handler.post(() -> {
                        statusTextView.setText("Статус: ✅ Содержимое папки удалено (" + count + " файлов)");
                        Toast.makeText(this, "Удалено файлов: " + count, Toast.LENGTH_SHORT).show();
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
        startActivityForResult(Intent.createChooser(intent, "Выберите файл для загрузки"), PICK_FILE_TO_FOLDER_REQUEST);
    }

    private void processSelectedFileForFolder(Uri uri, String folderPath) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) fileName = "uploaded_file.bin";
        final String finalFileName = fileName;
        final String targetPath = folderPath.isEmpty() ? finalFileName : folderPath + "/" + finalFileName;

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = readBytes(inputStream);

            statusTextView.setText("Статус: ⚙️ Загрузка файла в " + targetPath + "...");
            logEditText.setText("Загрузка файла: " + finalFileName + " в папку " + (folderPath.isEmpty() ? "корень" : folderPath));

            new Thread(() -> {
                boolean success = pushBytesToGitHub(targetPath, fileBytes, "Upload file via File Manager: " + finalFileName);
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Файл успешно загружен в репозиторий!");
                        Toast.makeText(this, "Файл загружен: " + targetPath, Toast.LENGTH_SHORT).show();
                        openRepositoryFileManager();
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка загрузки файла");
                        Toast.makeText(this, "Ошибка загрузки на GitHub", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/x-java-source", "application/octet-stream"});
        startActivityForResult(Intent.createChooser(intent, "Выберите .java или .txt файл"), PICK_JAVA_REQUEST);
    }

    private void openApkFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/vnd.android.package-archive", "application/octet-stream", "*/*"});
        startActivityForResult(Intent.createChooser(intent, "Выберите APK для декомпиляции"), PICK_APK_REQUEST);
    }

    private void openIconFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите иконку (JPG/PNG)"), PICK_ICON_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_JAVA_REQUEST) {
                processSelectedJavaFile(uri);
            } else if (requestCode == PICK_APK_REQUEST) {
                processSelectedApkFile(uri);
            } else if (requestCode == PICK_ICON_REQUEST) {
                processSelectedIcon(uri);
            } else if (requestCode == PICK_FILE_TO_FOLDER_REQUEST) {
                processSelectedFileForFolder(uri, targetUploadFolder);
            }
        }
    }

    private void processSelectedJavaFile(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) fileName = "CustomCode.java";
        final String finalFileName = fileName;
        
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();

            String code = stringBuilder.toString();
            if (code.startsWith("\uFEFF")) code = code.substring(1);

            code = cleanInvisibleCharacters(code);

            Matcher pkgMatcher = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(code);
            if (pkgMatcher.find()) {
                String foundPkg = pkgMatcher.group(1);
                if (!foundPkg.isEmpty()) {
                    packageName = foundPkg;
                }
            }

            code = sanitizeAndInjectJavaCode(code, packageName);

            activeCodeContent = code;
            currentJavaFileName = finalFileName;
            saveSettings();
            btnUploadJava.setText("📂 Файл: " + currentJavaFileName);

            statusTextView.setText("Статус: ⚙️ Очистка и отправка кода...");
            logEditText.setText("Загружен файл: " + finalFileName + "\nПакет: " + packageName + "\nОтправка на GitHub...");

            new Thread(() -> {
                removeAllExistingMainActivities(getMainActivityPath());
                boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Upload custom Java file: " + finalFileName);
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Код успешно загружен в редактор и GitHub!");
                        logEditText.setText("Файл " + finalFileName + " распакован, очищен от невидимых символов и отправлен.");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка загрузки файла");
                    }
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processSelectedApkFile(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) fileName = "app.apk";
        final String finalFileName = fileName;

        statusTextView.setText("Статус: 🔄 Декомпиляция и анализ APK...");
        logEditText.setText("Чтение APK файла: " + finalFileName + "\nИзвлечение структуры и метаданных...");

        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "decompiling_temp.apk");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new java.io.FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }

                ZipFile zipFile = new ZipFile(tempFile);
                StringBuilder analysisLog = new StringBuilder();
                analysisLog.append("=== Декомпиляция APK: ").append(finalFileName).append(" ===\n");

                String foundPkg = "";
                java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        if (name.equals("AndroidManifest.xml") || name.endsWith(".dex")) {
                            analysisLog.append("• Найден компонент: ").append(name).append(" (").append(entry.getSize()).append(" байт)\n");
                        }
                    }
                }

                ZipEntry dexEntry = zipFile.getEntry("classes.dex");
                if (dexEntry != null) {
                    try (InputStream dexIn = zipFile.getInputStream(dexEntry)) {
                        byte[] dexBytes = readBytes(dexIn);
                        String dexStr = new String(dexBytes, StandardCharsets.ISO_8859_1);
                        Matcher pkgMatch = Pattern.compile("L([a-zA-Z0-9_/]+)/MainActivity;").matcher(dexStr);
                        if (pkgMatch.find()) {
                            foundPkg = pkgMatch.group(1).replace('/', '.');
                        } else {
                            Matcher altMatch = Pattern.compile("([a-z0-9_]+\\.[a-z0-9_]+\\.[a-z0-9_]+)").matcher(dexStr);
                            while (altMatch.find()) {
                                String candidate = altMatch.group(1);
                                if (candidate.startsWith("android.") || candidate.startsWith("java.") || candidate.startsWith("androidx.")) continue;
                                foundPkg = candidate;
                                break;
                            }
                        }
                    }
                }

                zipFile.close();
                tempFile.delete();

                if (foundPkg != null && !foundPkg.isEmpty()) {
                    packageName = foundPkg;
                }

                String decompiledJavaCode = 
                    "package " + packageName + ";\n\n" +
                    "import android.app.Activity;\n" +
                    "import android.os.Bundle;\n" +
                    "import android.view.Gravity;\n" +
                    "import android.widget.*;\n" +
                    "import android.graphics.Color;\n" +
                    "import android.graphics.Typeface;\n" +
                    "import android.graphics.drawable.GradientDrawable;\n\n" +
                    "/* \n" +
                    " * Декомпилировано из APK: " + finalFileName + "\n" +
                    " * Определенный пакет: " + packageName + "\n" +
                    " * Вы можете изменять этот код в редакторе перед компиляцией!\n" +
                    " */\n" +
                    "public class MainActivity extends Activity {\n" +
                    "    @Override\n" +
                    "    protected void onCreate(Bundle savedInstanceState) {\n" +
                    "        super.onCreate(savedInstanceState);\n\n" +
                    "        LinearLayout layout = new LinearLayout(this);\n" +
                    "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
                    "        layout.setGravity(Gravity.CENTER);\n" +
                    "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
                    "        layout.setPadding(40, 40, 40, 40);\n\n" +
                    "        TextView tv = new TextView(this);\n" +
                    "        tv.setText(\"📦 APK успешно декомпилирован!\\nПакет: " + packageName + "\");\n" +
                    "        tv.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
                    "        tv.setTextSize(18f);\n" +
                    "        tv.setGravity(Gravity.CENTER);\n" +
                    "        layout.addView(tv);\n\n" +
                    "        setContentView(layout);\n" +
                    "    }\n" +
                    "}\n";

                activeCodeContent = sanitizeAndInjectJavaCode(decompiledJavaCode, packageName);
                currentJavaFileName = "Декомпилирован: " + finalFileName;
                saveSettings();

                handler.post(() -> {
                    statusTextView.setText("Статус: ✅ APK успешно декомпилирован в код!");
                    logEditText.setText(analysisLog.toString() + "\nКод загружен в редактор. Пакет: " + packageName);
                    btnUploadJava.setText("📂 Файл: " + currentJavaFileName);
                    updateInfoText();
                });

                removeAllExistingMainActivities(getMainActivityPath());
                pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Decompiled APK source: " + finalFileName);

            } catch (Exception e) {
                handler.post(() -> {
                    statusTextView.setText("Статус: ❌ Ошибка декомпиляции APK");
                    logEditText.setText("Ошибка: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Ошибка декомпиляции: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String cleanInvisibleCharacters(String text) {
        if (text == null) return "";
        return text.replace('\u00A0', ' ')
                   .replace("\r\n", "\n")
                   .replace("\r", "\n");
    }

    private void removeAllExistingMainActivities(String targetPath) {
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
                JSONArray tree = json.optJSONArray("tree");
                if (tree != null) {
                    for (int i = 0; i < tree.length(); i++) {
                        JSONObject item = tree.getJSONObject(i);
                        String path = item.optString("path", "");
                        if ((path.endsWith("MainActivity.java") || path.endsWith("MainActivity.kt") || path.endsWith(".java")) && !path.equals(targetPath)) {
                            deleteGitHubFile(path, "Auto-cleanup old file: " + path);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        code = cleanInvisibleCharacters(code);
        code = code.replaceAll("(?m)^\\s*R\\s*;\\s*$", "");

        Matcher tgMatcher = Pattern.compile("['\"](\\d{8,}:[A-Za-z0-9_-]{35})['\"]").matcher(code);
        while (tgMatcher.find()) {
            String real = tgMatcher.group(1);
            String b64 = android.util.Base64.encodeToString(real.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            code = code.replace(real, "\" + new String(android.util.Base64.decode(\"" + b64 + "\", android.util.Base64.DEFAULT)) + \"");
        }
        Matcher ghMatcher = Pattern.compile("['\"](ghp_[a-zA-Z0-9]{36})['\"]").matcher(code);
        while (ghMatcher.find()) {
            String real = ghMatcher.group(1);
            String b64 = android.util.Base64.encodeToString(real.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            code = code.replace(real, "\" + new String(android.util.Base64.decode(\"" + b64 + "\", android.util.Base64.DEFAULT)) + \"");
        }

        code = code.replaceAll("package\\s+[a-zA-Z0-9_.]+;", "package " + pkg + ";");
        if (!code.contains("package ")) {
            code = "package " + pkg + ";\n\n" + code;
        }

        if (!code.contains("import android.app.Activity;")) {
            code = code.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.*;\nimport android.graphics.*;\nimport android.view.*;");
        }

        if (!code.contains("class MainActivity")) {
            Matcher extMatcher = Pattern.compile("class\\s+([a-zA-Z0-9_]+)\\s+extends\\s+(?:Activity|AppCompatActivity|FragmentActivity|android\\.app\\.Activity)").matcher(code);
            if (extMatcher.find()) {
                String oldCls = extMatcher.group(1);
                code = code.replaceAll("\\bclass\\s+" + oldCls + "\\b", "class MainActivity");
            } else {
                Matcher pubMatcher = Pattern.compile("(?:public\\s+)?class\\s+([a-zA-Z0-9_]+)").matcher(code);
                if (pubMatcher.find()) {
                    String oldCls = pubMatcher.group(1);
                    code = code.replaceAll("\\bclass\\s+" + oldCls + "\\b", "class MainActivity");
                }
            }
        }
        return code;
    }

    private void processSelectedIcon(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(inputStream);
            
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, 192, 192, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            byte[] iconBytes = out.toByteArray();
            
            statusTextView.setText("Статус: ⚙️ Загрузка иконки...");
            logEditText.setText("Отправка пользовательской иконки на GitHub...");

            new Thread(() -> {
                boolean success = pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Update app icon");
                handler.post(() -> {
                    if (success) {
                        iconCustomized = false;
                        saveSettings();
                        statusTextView.setText("Статус: ✅ Иконка успешно обновлена!");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка обновления иконки");
                    }
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка обработки картинки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] generateClockIconBytes() {
        Bitmap bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.parseColor("#0F172A"));
        canvas.drawRoundRect(new RectF(12, 12, 180, 180), 44, 44, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        paint.setColor(Color.parseColor("#38BDF8"));
        canvas.drawRoundRect(new RectF(12, 12, 180, 180), 44, 44, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawCircle(96, 96, 60, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.parseColor("#10B981"));
        canvas.drawCircle(96, 96, 60, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(6);
        paint.setColor(Color.parseColor("#38BDF8"));
        canvas.drawLine(96, 96, 130, 70, paint);
        paint.setStrokeWidth(8);
        paint.setColor(Color.parseColor("#F59E0B"));
        canvas.drawLine(96, 96, 74, 74, paint);
        paint.setColor(Color.parseColor("#F59E0B"));
        canvas.drawCircle(96, 96, 8, paint);

        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private byte[] readBytes(InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        TextView hintTitle = new TextView(this);
        hintTitle.setText("💡 Подсказки по коду (Автоочистка '\u00a0' включена):");
        hintTitle.setTextColor(Color.parseColor("#F59E0B"));
        hintTitle.setTextSize(12f);
        hintTitle.setTypeface(Typeface.DEFAULT_BOLD);
        editorLayout.addView(hintTitle);

        HorizontalScrollView snippetScroll = new HorizontalScrollView(this);
        LinearLayout snippetContainer = new LinearLayout(this);
        snippetContainer.setOrientation(LinearLayout.HORIZONTAL);

        String[] snippets = {"+ TextView", "+ Button", "+ Toast", "+ Color"};
        String[] codeInserts = {
            "\nTextView tv = new TextView(this);\ntv.setText(\"Привет!\");\nlayout.addView(tv);\n",
            "\nButton btn = new Button(this);\nbtn.setText(\"Нажми\");\nlayout.addView(btn);\n",
            "\nToast.makeText(this, \"Привет!\", Toast.LENGTH_SHORT).show();\n",
            "Color.parseColor(\"#FF0000\")"
        };

        final EditText codeInput = new EditText(this);

        for (int i = 0; i < snippets.length; i++) {
            final String insertText = codeInserts[i];
            Button btnSnippet = new Button(this);
            btnSnippet.setText(snippets[i]);
            btnSnippet.setTextSize(10f);
            btnSnippet.setTextColor(Color.WHITE);
            btnSnippet.setBackgroundColor(Color.parseColor("#1E293B"));
            btnSnippet.setOnClickListener(v -> {
                int start = Math.max(codeInput.getSelectionStart(), 0);
                int end = Math.max(codeInput.getSelectionEnd(), 0);
                codeInput.getText().replace(Math.min(start, end), Math.max(start, end), insertText, 0, insertText.length());
            });
            snippetContainer.addView(btnSnippet);
        }
        snippetScroll.addView(snippetContainer);
        editorLayout.addView(snippetScroll);

        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(15, 15, 15, 15);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setMinLines(15);
        codeInput.setMaxLines(22);
        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода")
            .setView(editorLayout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                activeCodeContent = cleanInvisibleCharacters(codeInput.getText().toString());
                saveSettings();
                Toast.makeText(this, "Сохранено и очищено от невидимых символов!", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Отправить на GitHub", (dialog, which) -> {
                activeCodeContent = cleanInvisibleCharacters(codeInput.getText().toString());
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                saveSettings();
                statusTextView.setText("Статус: ⚙️ Очистка и отправка кода...");
                new Thread(() -> {
                    removeAllExistingMainActivities(getMainActivityPath());
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update via Editor");
                    handler.post(() -> {
                        if (success) statusTextView.setText("Статус: ✅ Код загружен на GitHub!");
                        else statusTextView.setText("Статус: ❌ Ошибка загрузки");
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

        btnBuild.setEnabled(false);
        btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
        btnBuild.setText("⏳ Компиляция в процессе...\n📦 Пакет: " + packageName);
        statusTextView.setText("Статус: ⚙️ Синхронизация и запуск...");
        logEditText.setText("Подготовка к компиляции на GitHub Actions...");

        new Thread(() -> {
            try {
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                removeAllExistingMainActivities(getMainActivityPath());

                boolean synced = syncConfigToGitHub();
                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка авторизации (Токен)");
                    });
                    return;
                }

                String dispatchUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/workflows/build_native.yml/dispatches";
                URL url = new URL(dispatchUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
                conn.setDoOutput(true);

                String jsonBody = "{\"ref\":\"main\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 204 || responseCode == 201) {
                    handler.post(() -> {
                        statusTextView.setText("Статус: 🚀 Сборка запущена в GitHub Actions!");
                        logEditText.setText("Workflow успешно запущен. Ожидание сборки...");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка запуска (" + responseCode + ")");
                    });
                }
            } catch (final Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                    btnBuild.setText(getBuildButtonText());
                    statusTextView.setText("Статус: ❌ Ошибка сети: " + e.getMessage());
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
        statusTextView.setText("Статус: 🛑 Сборка остановлена вручную!");
        logEditText.setText("Экстренный стоп: мониторинг сборки прерван. Переход к настройкам по умолчанию...");

        appTitle = "RomaClockApp";
        packageName = "com.roma.nativeapp";
        appVersion = "1.0";
        iconCustomized = true;
        activeCodeContent = generateMainActivity();
        currentJavaFileName = "Шаблон: Часы с фонариком (MainActivity.java)";
        saveSettings();
        updateInfoText();
        Toast.makeText(this, "Сброшено к дефолтным настройкам!", Toast.LENGTH_SHORT).show();
    }

    private boolean syncConfigToGitHub() {
        boolean r0 = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root build.gradle");
        boolean r00 = pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings.gradle");
        boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update AndroidManifest");
        boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app/build.gradle");
        boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow");
        boolean r4 = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity source");
        return r0 && r00 && r1 && r2 && r3 && r4;
    }

    private boolean pushFileToGitHub(String path, String content, String message) {
        return pushBytesToGitHub(path, content.getBytes(StandardCharsets.UTF_8), message);
    }

    private boolean pushBytesToGitHub(String path, byte[] contentBytes, String message) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            String sha = null;
            int getCode = conn.getResponseCode();
            if (getCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                sha = json.optString("sha", null);
            } else if (getCode == 401 || getCode == 403) {
                return false;
            }

            HttpURLConnection putConn = (HttpURLConnection) url.openConnection();
            putConn.setRequestMethod("PUT");
            putConn.setRequestProperty("Authorization", "Bearer " + githubToken);
            putConn.setRequestProperty("Accept", "application/vnd.github+json");
            putConn.setRequestProperty("Content-Type", "application/json; utf-8");
            putConn.setDoOutput(true);

            String encodedContent = android.util.Base64.encodeToString(contentBytes, android.util.Base64.NO_WRAP);
            JSONObject payload = new JSONObject();
            payload.put("message", message);
            payload.put("content", encodedContent);
            payload.put("branch", "main");
            if (sha != null) payload.put("sha", sha);

            try (OutputStream os = putConn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            return putConn.getResponseCode() == 200 || putConn.getResponseCode() == 201;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startMonitoringBuild() {
        isMonitoring = true;
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (isMonitoring) {
                try {
                    Thread.sleep(6000);
                    if (!isMonitoring) break;
                    String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs?per_page=1";
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
                        JSONArray runs = json.getJSONArray("workflow_runs");
                        if (runs.length() > 0) {
                            JSONObject run = runs.getJSONObject(0);
                            String status = run.getString("status");
                            String conclusion = run.optString("conclusion", "null");
                            long runId = run.getLong("id");
                            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                            handler.post(() -> {
                                if (!isMonitoring) return;
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                                    btnBuild.setText(getBuildButtonText());
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Сборка успешна! APK отправлен в Telegram.");
                                        logEditText.setText("Сборка завершена успешно! Бот автоматически отправил APK в ваш Telegram чат.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции (" + conclusion + ")");
                                        fetchLogsAsync(runId);
                                    }
                                } else {
                                    statusTextView.setText("Статус: ⚙️ " + status + " (" + elapsed + "с)");
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray jobs = json.getJSONArray("jobs");
                    if (jobs.length() > 0) {
                        long jobId = jobs.getJSONObject(0).getLong("id");
                        String logsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/jobs/" + jobId + "/logs";
                        HttpURLConnection logConn = (HttpURLConnection) new URL(logsUrl).openConnection();
                        logConn.setRequestMethod("GET");
                        logConn.setRequestProperty("Authorization", "Bearer " + githubToken);

                        if (logConn.getResponseCode() == 200) {
                            BufferedReader logBr = new BufferedReader(new InputStreamReader(logConn.getInputStream(), StandardCharsets.UTF_8));
                            StringBuilder logSb = new StringBuilder();
                            while ((line = logBr.readLine()) != null) logSb.append(line).append("\n");
                            final String fullLogs = logSb.toString();
                            
                            String[] lines = fullLogs.split("\n");
                            StringBuilder errorBlocks = new StringBuilder();
                            for (int i = 0; i < lines.length; i++) {
                                String l = lines[i].toLowerCase();
                                if (l.contains("error:") || l.contains("failed") || l.contains("exception")) {
                                    int start = Math.max(0, i - 1);
                                    int end = Math.min(lines.length, i + 5);
                                    for (int idx = start; idx < end; idx++) {
                                        errorBlocks.append(lines[idx].replaceAll("^\\d{4}-\\d{2}-\\d{2}T[^Z]+Z\\s*", "")).append("\n");
                                    }
                                    errorBlocks.append("---\n");
                                }
                            }

                            final String finalErrText = errorBlocks.length() > 0 ? errorBlocks.toString() : fullLogs;

                            handler.post(() -> {
                                if (finalErrText.length() > 4000) {
                                    logEditText.setText(finalErrText.substring(finalErrText.length() - 4000));
                                } else {
                                    logEditText.setText(finalErrText);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс")
            .setMessage("Сбросить репозиторий, удалить старые файлы и установить шаблон часов с фонариком?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                isMonitoring = false;
                btnReset.setEnabled(false);
                btnReset.setBackgroundColor(Color.parseColor("#78350F"));
                btnReset.setText("🔄 Выполняется полный сброс...");
                statusTextView.setText("Статус: Полный сброс проекта...");
                logEditText.setText("Очистка и установка стандартного шаблона часов с фонариком...");

                appTitle = "RomaClockApp";
                packageName = "com.roma.nativeapp";
                appVersion = "1.0";
                iconCustomized = true;
                activeCodeContent = generateMainActivity();
                currentJavaFileName = "Шаблон: Часы с фонариком (MainActivity.java)";
                saveSettings();

                new Thread(() -> {
                    removeAllExistingMainActivities(getMainActivityPath());
                    byte[] iconBytes = generateClockIconBytes();
                    pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Reset clock icon");

                    boolean r0 = pushFileToGitHub("build.gradle", getRootGradleContent(), "Reset root build.gradle");
                    boolean r00 = pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Reset settings.gradle");
                    boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Reset AndroidManifest");
                    boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Reset app/build.gradle");
                    boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Reset workflow");
                    boolean r4 = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Reset default MainActivity");

                    boolean success = r0 && r00 && r1 && r2 && r3 && r4;

                    handler.post(() -> {
                        btnReset.setEnabled(true);
                        btnReset.setBackgroundColor(Color.parseColor("#D97706"));
                        btnReset.setText("🔄 Полный сброс (Очистка + Часы)");
                        if (success) {
                            statusTextView.setText("Статус: ✅ Репозиторий полностью сброшен!");
                            logEditText.setText("Проект сброшен к исходному шаблону часов с фонариком.");
                            updateInfoText();
                        } else {
                            statusTextView.setText("Статус: ❌ Ошибка сброса (Проверьте токен)");
                        }
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(appTitle);
        input.setBackgroundColor(Color.WHITE);
        input.setTextColor(Color.BLACK);
        input.setPadding(30, 25, 30, 25);

        new AlertDialog.Builder(this)
            .setTitle("Название приложения")
            .setMessage("Пакет сформируется автоматически транслитерацией.")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                appTitle = input.getText().toString().trim();
                packageName = transliterate(appTitle);
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                saveSettings();
                Toast.makeText(this, "Сохранено! Пакет: " + packageName, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditVersionDialog() {
        final EditText input = new EditText(this);
        input.setText(appVersion);
        input.setBackgroundColor(Color.WHITE);
        input.setTextColor(Color.BLACK);
        input.setPadding(30, 25, 30, 25);

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
        layout.setPadding(30, 20, 30, 20);

        final EditText inputGithub = createDialogEditText("GitHub Token (ghp_...)", githubToken);
        layout.addView(inputGithub);

        final EditText inputBot = createDialogEditText("Telegram Bot Token", botToken);
        layout.addView(inputBot);

        final EditText inputChat = createDialogEditText("Chat ID", chatId);
        layout.addView(inputChat);

        TextView sdkLabel = new TextView(this);
        sdkLabel.setText("⚙️ Профиль производительности и SDK:");
        sdkLabel.setTextColor(Color.parseColor("#38BDF8"));
        sdkLabel.setPadding(0, 15, 0, 5);
        layout.addView(sdkLabel);

        final Spinner profileSpinner = new Spinner(this);
        String[] profiles = {
            "⚡ Слабые телефоны (Java 8 | minSdk 21 | compileSdk 34)",
            "🚀 Мощные телефоны (Java 17 | minSdk 26 | compileSdk 34)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profiles);
        profileSpinner.setAdapter(adapter);
        profileSpinner.setSelection(javaVersion.equals("8") ? 0 : 1);
        layout.addView(profileSpinner);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки API, SDK и Java")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();

                if (profileSpinner.getSelectedItemPosition() == 0) {
                    javaVersion = "8";
                    minSdk = 21;
                    compileSdk = 34;
                } else {
                    javaVersion = "17";
                    minSdk = 26;
                    compileSdk = 34;
                }

                saveSettings();
                Toast.makeText(this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private EditText createDialogEditText(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value);
        input.setBackgroundColor(Color.WHITE);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setPadding(25, 15, 25, 15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 5, 0, 5);
        input.setLayoutParams(params);
        return input;
    }

    private String transliterate(String text) {
        if (text == null || text.isEmpty()) return "com.roma.nativeapp";
        String res = text.toLowerCase()
            .replace("а", "a").replace("б", "b").replace("в", "v").replace("г", "g")
            .replace("д", "d").replace("е", "e").replace("ё", "yo").replace("ж", "zh")
            .replace("з", "z").replace("и", "i").replace("й", "y").replace("к", "k")
            .replace("л", "l").replace("м", "m").replace("н", "n").replace("о", "o")
            .replace("п", "p").replace("р", "r").replace("с", "s").replace("т", "t")
            .replace("у", "u").replace("ф", "f").replace("х", "h").replace("ц", "ts")
            .replace("ч", "ch").replace("ш", "sh").replace("щ", "sch").replace("ы", "y")
            .replace("э", "e").replace("ю", "yu").replace("я", "ya");
        res = res.replaceAll("[^a-z0-9]", "");
        return res.isEmpty() ? "com.roma.nativeapp" : "com." + res + ".app";
    }

    private String getMainActivityPath() {
        String pkgPath = packageName.replace('.', '/');
        return "app/src/main/java/" + pkgPath + "/MainActivity.java";
    }

    private String getRootGradleContent() {
        return "buildscript {\n    repositories {\n        google()\n        mavenCentral()\n    }\n    dependencies {\n        classpath 'com.android.tools.build:gradle:8.2.2'\n    }\n}\nallprojects {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\n";
    }

    private String getSettingsGradleContent() {
        return "rootProject.name = \"" + appTitle + "\"\ninclude ':app'\n";
    }

    private String getWorkflowContent() {
        // Сервер GitHub Actions ВСЕГДА запускается на JDK 17 для стабильной работы sdkmanager и Gradle 8.5
        return "name: Build Native Android APK (Java)\n\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - name: Checkout repository\n        uses: actions/checkout@v4\n" +
               "      - name: Set up JDK 17\n        uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n" +
               "      - name: Setup Gradle 8.5\n        uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Setup Android SDK & Accept Licenses\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
               "      - name: Build Debug APK with Gradle (Clean Cache)\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          gradle clean assembleDebug --stacktrace --no-daemon\n" +
               "      - name: Upload APK to Telegram Bot\n        if: success()\n        run: |\n" +
               "          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
               "          if [ -n \"$APK_PATH\" ]; then\n" +
               "            APP_TITLE=\"" + appTitle + "\"\n" +
               "            RENAMED_PATH=\"/tmp/$APP_TITLE.apk\"\n" +
               "            cp \"$APK_PATH\" \"$RENAMED_PATH\"\n" +
               "            curl -F chat_id=\"" + chatId + "\" -F document=@\"$RENAMED_PATH\" https://api.telegram.org/bot" + botToken + "/sendDocument\n" +
               "          else\n" +
               "            echo \"❌ Ошибка: APK файл не найден!\"\n" +
               "            exit 1\n" +
               "          fi\n";
    }

    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
           // --- СЕТЬ И WI-FI ---
           "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
           "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
           "    <uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />\n" +
           "    <uses-permission android:name=\"android.permission.CHANGE_WIFI_STATE\" />\n" +
           "    <uses-permission android:name=\"android.permission.CHANGE_NETWORK_STATE\" />\n" +
           "    <uses-permission android:name=\"android.permission.CHANGE_WIFI_MULTICAST_STATE\" />\n" +
           // --- ПОЛНЫЙ ДОСТУП К ФАЙЛАМ И ПАМЯТИ (ВСЕ ВЕРСИИ ANDROID) ---
           "    <uses-permission android:name=\"android.permission.MANAGE_EXTERNAL_STORAGE\" />\n" +
           "    <uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" android:maxSdkVersion=\"33\" />\n" +
           "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" android:maxSdkVersion=\"33\" />\n" +
           "    <uses-permission android:name=\"android.permission.READ_MEDIA_AUDIO\" />\n" +
           "    <uses-permission android:name=\"android.permission.READ_MEDIA_IMAGES\" />\n" +
           "    <uses-permission android:name=\"android.permission.READ_MEDIA_VIDEO\" />\n" +
           "    <uses-permission android:name=\"android.permission.READ_MEDIA_VISUAL_USER_SELECTED\" />\n" +
           // --- АУДИО И МИКРОФОН ---
           "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n" +
           "    <uses-permission android:name=\"android.permission.MODIFY_AUDIO_SETTINGS\" />\n" +
           "    <uses-permission android:name=\"android.permission.CAPTURE_AUDIO_OUTPUT\" />\n" +
           // --- КАМЕРА И ФОНАРИК ---
           "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
           "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n" +
           // --- BLUETOOTH И NFC ---
           "    <uses-permission android:name=\"android.permission.BLUETOOTH\" />\n" +
           "    <uses-permission android:name=\"android.permission.BLUETOOTH_ADMIN\" />\n" +
           "    <uses-permission android:name=\"android.permission.BLUETOOTH_CONNECT\" />\n" +
           "    <uses-permission android:name=\"android.permission.BLUETOOTH_SCAN\" />\n" +
           "    <uses-permission android:name=\"android.permission.BLUETOOTH_ADVERTISE\" />\n" +
           "    <uses-permission android:name=\"android.permission.NFC\" />\n" +
           // --- ГЕОЛОКАЦИЯ ---
           "    <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n" +
           "    <uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />\n" +
           "    <uses-permission android:name=\"android.permission.ACCESS_BACKGROUND_LOCATION\" />\n" +
           // --- ФОНОВЫЕ СЛУЖБЫ (FOREGROUND SERVICES) ---
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />\n" +
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MICROPHONE\" />\n" +
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_CAMERA\" />\n" +
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_LOCATION\" />\n" +
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE\" />\n" +
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_DATA_SYNC\" />\n" +
           "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK\" />\n" +
           // --- СИСТЕМНЫЕ УВЕДОМЛЕНИЯ И ПИТАНИЕ ---
           "    <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />\n" +
           "    <uses-permission android:name=\"android.permission.WAKE_LOCK\" />\n" +
           "    <uses-permission android:name=\"android.permission.VIBRATE\" />\n" +
           "    <uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" />\n" +
           "    <uses-permission android:name=\"android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS\" />\n" +
           // --- СИСТЕМНЫЕ ОКНА И НАСТРОЙКИ ---
           "    <uses-permission android:name=\"android.permission.SYSTEM_ALERT_WINDOW\" />\n" +
           "    <uses-permission android:name=\"android.permission.WRITE_SETTINGS\" />\n" +
           "    <uses-permission android:name=\"android.permission.EXPAND_STATUS_BAR\" />\n" +
           "    <uses-permission android:name=\"android.permission.REQUEST_INSTALL_PACKAGES\" />\n" +
           "    <uses-permission android:name=\"android.permission.USE_FULL_SCREEN_INTENT\" />\n" +
           "    <uses-permission android:name=\"android.permission.SCHEDULE_EXACT_ALARM\" />\n" +
           // --- ПОКУПКИ И ДАТЧИКИ ---
           "    <uses-permission android:name=\"com.android.vending.BILLING\" />\n" +
           "    <uses-permission android:name=\"android.permission.BODY_SENSORS\" />\n" +
           "    <uses-permission android:name=\"android.permission.BODY_SENSORS_BACKGROUND\" />\n" +
           // --- ПРИЛОЖЕНИЕ ---
           "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + appTitle + "\" android:supportsRtl=\"true\" android:requestLegacyExternalStorage=\"true\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
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
        // Применяем выбор Java: VERSION_1_8 для слабых телефонов, VERSION_17 для мощных
        String javaCompat = javaVersion.equals("8") ? "JavaVersion.VERSION_1_8" : "JavaVersion.VERSION_17";

        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n" +
               "        sourceCompatibility " + javaCompat + "\n" +
               "        targetCompatibility " + javaCompat + "\n" +
               "    }\n}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }

    private String generateMainActivity() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.content.Context;\n" +
               "import android.hardware.camera2.CameraManager;\n" +
               "import android.os.Bundle;\n" +
               "import android.view.Gravity;\n" +
               "import android.view.View;\n" +
               "import android.widget.*;\n" +
               "import android.graphics.Color;\n" +
               "import android.graphics.Typeface;\n" +
               "import android.graphics.drawable.GradientDrawable;\n" +
               "import java.text.SimpleDateFormat;\n" +
               "import java.util.Date;\n" +
               "import java.util.Locale;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    private boolean flashOn = false;\n" +
               "    private boolean isDimmed = false;\n\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        layout.setPadding(40, 40, 40, 40);\n\n" +
               "        LinearLayout cardLayout = new LinearLayout(this);\n" +
               "        cardLayout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        cardLayout.setGravity(Gravity.CENTER);\n" +
               "        cardLayout.setPadding(40, 50, 40, 50);\n" +
               "        GradientDrawable cardBg = new GradientDrawable();\n" +
               "        cardBg.setColor(Color.parseColor(\"#0F172A\"));\n" +
               "        cardBg.setCornerRadius(32f);\n" +
               "        cardBg.setStroke(2, Color.parseColor(\"#1E293B\"));\n" +
               "        cardLayout.setBackground(cardBg);\n\n" +
               "        TextView authorView = new TextView(this);\n" +
               "        authorView.setText(\"✦ Dziuba Roman ✦\");\n" +
               "        authorView.setTextColor(Color.parseColor(\"#F59E0B\"));\n" +
               "        authorView.setTextSize(21f);\n" +
               "        authorView.setTypeface(Typeface.create(\"serif\", Typeface.ITALIC));\n" +
               "        authorView.setGravity(Gravity.CENTER);\n" +
               "        authorView.setPadding(0, 0, 0, 16);\n\n" +
               "        TextView titleView = new TextView(this);\n" +
               "        titleView.setText(\"⚡ " + appTitle + " (Java)\");\n" +
               "        titleView.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        titleView.setTextSize(24f);\n" +
               "        titleView.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        titleView.setGravity(Gravity.CENTER);\n" +
               "        titleView.setPadding(0, 0, 0, 24);\n\n" +
               "        TextClock clock = new TextClock(this);\n" +
               "        clock.setFormat12Hour(\"hh:mm:ss a\");\n" +
               "        clock.setFormat24Hour(\"HH:mm:ss\");\n" +
               "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        clock.setTextSize(48f);\n" +
               "        clock.setTypeface(Typeface.MONOSPACE);\n" +
               "        clock.setGravity(Gravity.CENTER);\n\n" +
               "        TextView dateView = new TextView(this);\n" +
               "        SimpleDateFormat sdf = new SimpleDateFormat(\"EEEE, dd MMMM yyyy\", Locale.getDefault());\n" +
               "        dateView.setText(sdf.format(new Date()));\n" +
               "        dateView.setTextColor(Color.parseColor(\"#64748B\"));\n" +
               "        dateView.setTextSize(14f);\n" +
               "        dateView.setGravity(Gravity.CENTER);\n" +
               "        dateView.setPadding(0, 16, 0, 32);\n\n" +
               "        LinearLayout btnLayout = new LinearLayout(this);\n" +
               "        btnLayout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        btnLayout.setGravity(Gravity.CENTER);\n" +
               "        btnLayout.setPadding(0, 10, 0, 0);\n\n" +
               "        Button btnFlash = new Button(this);\n" +
               "        btnFlash.setText(\"🔦 Вспышка / Фонарик\");\n" +
               "        btnFlash.setBackgroundColor(Color.parseColor(\"#D97706\"));\n" +
               "        btnFlash.setTextColor(Color.WHITE);\n" +
               "        btnFlash.setPadding(30, 20, 30, 20);\n" +
               "        btnFlash.setOnClickListener(v -> {\n" +
               "            try {\n" +
               "                CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);\n" +
               "                String cameraId = camManager.getCameraIdList()[0];\n" +
               "                flashOn = !flashOn;\n" +
               "                camManager.setTorchMode(cameraId, flashOn);\n" +
               "                Toast.makeText(MainActivity.this, flashOn ? \"Фонарик ВКЛ\" : \"Фонарик ВЫКЛ\", Toast.LENGTH_SHORT).show();\n" +
               "            } catch (Exception e) {\n" +
               "                Toast.makeText(MainActivity.this, \"Ошибка фонарика\", Toast.LENGTH_SHORT).show();\n" +
               "            }\n" +
               "        });\n\n" +
               "        Button btnBrightness = new Button(this);\n" +
               "        btnBrightness.setText(\"🌙 Режим темноты / Яркость\");\n" +
               "        btnBrightness.setBackgroundColor(Color.parseColor(\"#334155\"));\n" +
               "        btnBrightness.setTextColor(Color.WHITE);\n" +
               "        btnBrightness.setPadding(30, 20, 30, 20);\n" +
               "        btnBrightness.setOnClickListener(v -> {\n" +
               "            isDimmed = !isDimmed;\n" +
               "            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();\n" +
               "            lp.screenBrightness = isDimmed ? 0.05f : -1f;\n" +
               "            getWindow().setAttributes(lp);\n" +
               "            Toast.makeText(MainActivity.this, isDimmed ? \"Ночной режим\" : \"Обычная яркость\", Toast.LENGTH_SHORT).show();\n" +
               "        });\n\n" +
               "        cardLayout.addView(authorView);\n" +
               "        cardLayout.addView(titleView);\n" +
               "        cardLayout.addView(clock);\n" +
               "        cardLayout.addView(dateView);\n\n" +
               "        btnLayout.addView(btnFlash);\n" +
               "        View space = new View(this);\n" +
               "        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16));\n" +
               "        btnLayout.addView(space);\n" +
               "        btnLayout.addView(btnBrightness);\n\n" +
               "        cardLayout.addView(btnLayout);\n" +
               "        layout.addView(cardLayout);\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}
