package com.pro9.version09;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 🚀 MONSTER COMPILER CI/CD ENGINE v9.0 ULTRA PRO (NO TELEGRAM - LOCAL DOWNLOADS)
 * Автор: Dziuba Roman
 */
public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfigMonsterV9";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;
    private static final int PICK_APK_REQUEST = 1003;
    private static final int PICK_FILE_TO_FOLDER_REQUEST = 1004;

    private String githubToken = "";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String apkTitle = "RomaMonsterApp";
    private String appVersion = "009";
    private int versionCode = 9;
    private String packageName = "com.roman.version009";
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
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnTitle, btnVersion, btnPackageName, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor, btnFileManager, btnRetryUpload;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;
    private byte[] lastFailedBytes = null;
    private String lastFailedPath = "";

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

        updatePackageNameFromCode(activeCodeContent);
        activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);

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
        authorView.setText("✦ Dziuba Roman — Monster CI/CD Engine v9.0 Local ✦");
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
        titleView.setText("🚀 Android Monster Compiler (v9.0 Local Downloads)");
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

        btnBuild = createStyledButton("", "#1D4ED8");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        btnVersion = createStyledButton("", "#334155");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        btnTitle = createStyledButton("", "#334155");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        btnPackageName = createStyledButton("", "#0F766E");
        btnPackageName.setOnClickListener(v -> showEditPackageDialog());
        card.addView(btnPackageName);

        btnIcon = createStyledButton("", "#5B21B6");
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        btnCodeEditor = createStyledButton("📝 Редактор кода (Поиск + Санитизация)", "#0F172A");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        btnUploadJava = createStyledButton("", "#115E59");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        btnRetryUpload = createStyledButton("🔄 Повторить отправку файла на сервер", "#B45309");
        btnRetryUpload.setVisibility(View.GONE);
        btnRetryUpload.setOnClickListener(v -> retryLastUpload());
        card.addView(btnRetryUpload);

        btnDecompileApk = createStyledButton("📦 Декомпилировать файл/APK в код", "#475569");
        btnDecompileApk.setOnClickListener(v -> openUniversalFilePicker());
        card.addView(btnDecompileApk);

        btnStopBuild = createStyledButton("🛑 Экстренный стоп сборки", "#B91C1C");
        btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
        card.addView(btnStopBuild);

        btnFileManager = createStyledButton("📁 Файловый менеджер репозитория", "#6D28D9");
        btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
        card.addView(btnFileManager);

        btnSettings = createStyledButton("⚙️ Настройки GitHub & SDK", "#1E293B");
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
        logLabel.setText("📋 Профессиональный терминал и AI-доктор:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь выводятся логи и автоматические отчеты AI-доктора...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(8);
        logEditText.setMaxLines(15);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать лог в буфер", "#334155");
        btnCopyLogs.setOnClickListener(v -> safeCopyToClipboard(logEditText != null && logEditText.getText() != null ? logEditText.getText().toString() : ""));
        card.addView(btnCopyLogs);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        updateAllButtonLabels();
        logEditText.setText("=== 🚀 MONSTER COMPILER PRO V9.0 LOCAL ЗАПУЩЕН ===\n" +
                            "• Имя APK: " + apkTitle + "\n" +
                            "• Пакет: " + packageName + "\n" +
                            "• Версия: " + appVersion + " (Code: " + versionCode + ")\n" +
                            "• Сохранение: Папка 'Загрузки' (Downloads) в телефоне\n" +
                            "• GitHub Token: " + (githubToken.isEmpty() ? "⚠️ Не введен" : "✅ Загружен"));

        if (!githubToken.isEmpty()) {
            fetchRemoteRepositoryInfo();
        }
    }

    private void safeCopyToClipboard(String textToCopy) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            if (textToCopy == null || textToCopy.trim().isEmpty()) return;
            if (textToCopy.length() > 50000) {
                textToCopy = textToCopy.substring(textToCopy.length() - 50000);
            }
            ClipData clip = ClipData.newPlainText("MonsterLogs", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Логи скопированы в буфер!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка копирования", Toast.LENGTH_SHORT).show();
        }
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
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

    private void updateAllButtonLabels() {
        String profile = javaVersion.equals("8") ? "⚡ Стабильный (Java 8)" : "🚀 Java 17";
        String info = "🏷 Имя APK: " + apkTitle + " | 📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + " (Code: " + versionCode + ") | 📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | minSdk: " + minSdk + " [" + profile + "]";
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя APK: " + apkTitle);
        if (btnVersion != null) btnVersion.setText("📦 Версия: " + appVersion);
        if (btnPackageName != null) btnPackageName.setText("📦 Пакет: " + packageName);
        if (btnUploadJava != null) btnUploadJava.setText("📂 Загрузи Файл сюда👈  " + currentJavaFileName);
        if (btnBuild != null) btnBuild.setText(getBuildButtonText());
        if (btnIcon != null) btnIcon.setText(iconCustomized ? "🖼 Иконка: ⚡ Шаблон" : "🖼 Иконка: 🎨 Пользовательская");
    }

    private String getBuildButtonText() {
        return "🚀 Скомпилировать и сохранить в Загрузки\n📦 " + apkTitle + " (v" + appVersion + ")";
    }

    private String extractAndCleanPackageFromCode(String code) {
        if (code == null || code.isEmpty()) return transliterateToLatin(apkTitle);
        
        String[] lines = code.split("\n");
        String targetLine = "";
        
        for (String l : lines) {
            String trimmed = l.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                targetLine = trimmed;
                break;
            }
        }
        
        if (targetLine.isEmpty() && lines.length > 0) targetLine = lines[0].trim();
        
        Matcher m = Pattern.compile("^package\\s+([a-zA-Z0-9_.]+);?").matcher(targetLine);
        if (m.find()) {
            String rawPkg = m.group(1);
            String[] parts = rawPkg.split("\\.");
            if (parts.length >= 2) {
                for (String p : parts) {
                    if (!p.equals("com") && !p.startsWith("version") && !p.matches("\\d+")) {
                        return transliterateToLatin(p);
                    }
                }
                return transliterateToLatin(parts[parts.length - 1]);
            } else if (parts.length == 1) {
                return transliterateToLatin(parts[0]);
            }
        }
        return transliterateToLatin(apkTitle);
    }

    private void updatePackageNameFromCode(String code) {
        String coreName = extractAndCleanPackageFromCode(code);
        String sanitizedVer = sanitizeVersionText(appVersion);
        if (sanitizedVer.isEmpty()) sanitizedVer = "009";
        packageName = "com." + coreName + ".version" + sanitizedVer;
    }

    private String sanitizeVersionText(String input) {
        if (input == null) return "009";
        String res = input.toLowerCase();
        String[] abcCyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","є","і","ї","ґ"};
        for (String c : abcCyr) res = res.replace(c, "");
        res = res.replaceAll("[^a-z0-9]", "");
        return res.isEmpty() ? "009" : res;
    }

    private String transliterateToLatin(String input) {
        if (input == null || input.isEmpty()) return "romanapp";
        String[] abcCyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","є","і","ї","ґ"};
        String[] abcLat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","shch","","y","","e","yu","ya","ye","i","yi","g"};

        String res = input.toLowerCase();
        for (int i = 0; i < abcCyr.length; i++) res = res.replace(abcCyr[i], abcLat[i]);
        res = res.replaceAll("[^a-z0-9]", "");
        if (res.isEmpty()) res = "romanapp";
        if (Character.isDigit(res.charAt(0))) res = "app" + res;
        return res;
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        githubToken = sp.getString("githubToken", "");
        repoOwner = sp.getString("repoOwner", repoOwner);
        repoName = sp.getString("repoName", repoName);
        apkTitle = sp.getString("apkTitle", apkTitle);
        appVersion = sp.getString("appVersion", "009");
        versionCode = sp.getInt("versionCode", 9);
        packageName = sp.getString("packageName", "com.roman.version009");
        currentJavaFileName = sp.getString("currentJavaFileName", currentJavaFileName);
        iconCustomized = sp.getBoolean("iconCustomized", true);
        minSdk = sp.getInt("minSdk", 21);
        compileSdk = sp.getInt("compileSdk", 34);
        javaVersion = sp.getString("javaVersion", "8");
        activeCodeContent = sp.getString("activeCodeContent", "");
    }

    private void saveSettings() {
        updatePackageNameFromCode(activeCodeContent);
        activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);

        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("githubToken", githubToken);
        editor.putString("repoOwner", repoOwner);
        editor.putString("repoName", repoName);
        editor.putString("apkTitle", apkTitle);
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
                        showSingleDirectoryBrowserDialog(items);
                    });
                } else {
                    handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка загрузки дерева репо"));
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка сети"));
            }
        }).start();
    }

    private void showSingleDirectoryBrowserDialog(List<RepoItem> allItems) {
        final String[] currentDir = {""};

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(15, 15, 15, 15);
        dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

        Button btnStepBack = createStyledButton("⬆️ Шаг назад", "#334155");
        dialogLayout.addView(btnStepBack);

        TextView pathTitle = new TextView(this);
        pathTitle.setTextColor(Color.parseColor("#38BDF8"));
        pathTitle.setTextSize(13f);
        pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pathTitle.setPadding(0, 5, 0, 8);
        dialogLayout.addView(pathTitle);

        ListView listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 650
        );
        listView.setLayoutParams(listParams);
        dialogLayout.addView(listView);

        Button btnUploadHere = createStyledButton("➕ Загрузить файл в текущую папку", "#0F766E");
        dialogLayout.addView(btnUploadHere);

        Button btnCreateFolder = createStyledButton("📁 Создать папку", "#047857");
        dialogLayout.addView(btnCreateFolder);

        Button btnCreateFile = createStyledButton("📄 Создать файл", "#0E7490");
        dialogLayout.addView(btnCreateFile);

        Button btnCleanRepoManager = createStyledButton("🗑 Полная очистка репозитория", "#991B1B");
        dialogLayout.addView(btnCleanRepoManager);

        Button btnCloseManager = createStyledButton("🚪 Закрыть менеджер", "#475569");
        dialogLayout.addView(btnCloseManager);

        AlertDialog fileDialog = new AlertDialog.Builder(this)
            .setTitle("📁 Менеджер репозитория")
            .setView(dialogLayout)
            .setCancelable(true)
            .create();

        final Runnable[] refreshList = new Runnable[1];
        refreshList[0] = () -> {
            String dir = currentDir[0];
            pathTitle.setText("📂 /" + (dir.isEmpty() ? "root" : dir));

            java.util.Set<String> subFolders = new java.util.TreeSet<>();
            List<RepoItem> subFiles = new ArrayList<>();

            String prefix = dir.isEmpty() ? "" : dir + "/";
            int prefixLen = prefix.length();

            for (RepoItem item : allItems) {
                if (dir.isEmpty()) {
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

            for (String folder : subFolders) {
                displayItems.add("📁 Папка: " + folder);
                itemPaths.add(dir.isEmpty() ? folder : dir + "/" + folder);
                isFolderList.add(true);
            }

            for (RepoItem fileItem : subFiles) {
                displayItems.add("📄 " + fileItem.path.substring(prefixLen) + " (" + fileItem.size + "б)");
                itemPaths.add(fileItem.path);
                isFolderList.add(false);
            }

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

            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedPath = itemPaths.get(position);
                boolean isFolder = isFolderList.get(position);
                if (isFolder) {
                    currentDir[0] = selectedPath;
                    refreshList[0].run();
                } else {
                    showFileOptionsDialog(selectedPath);
                }
            });
        };

        btnStepBack.setOnClickListener(v -> {
            if (!currentDir[0].isEmpty()) {
                int lastSlash = currentDir[0].lastIndexOf('/');
                currentDir[0] = lastSlash == -1 ? "" : currentDir[0].substring(0, lastSlash);
                refreshList[0].run();
            } else {
                Toast.makeText(this, "Вы уже в корне репозитория", Toast.LENGTH_SHORT).show();
            }
        });

        btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir[0]));

        btnCreateFolder.setOnClickListener(v -> {
            final EditText folderInput = new EditText(this);
            folderInput.setHint("Имя новой папки (например: utils)");
            new AlertDialog.Builder(this)
                .setTitle("📁 Создать новую папку")
                .setView(folderInput)
                .setPositiveButton("Создать", (d, w) -> {
                    String folderName = folderInput.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        String newPath = currentDir[0].isEmpty() ? folderName + "/.gitkeep" : currentDir[0] + "/" + folderName + "/.gitkeep";
                        statusTextView.setText("Статус: ⚙️ Создание папки...");
                        new Thread(() -> {
                            boolean ok = pushBytesToGitHub(newPath, new byte[0], "Create folder " + folderName);
                            handler.post(() -> {
                                if (ok) {
                                    Toast.makeText(this, "Папка создана!", Toast.LENGTH_SHORT).show();
                                    fileDialog.dismiss();
                                    openRepositoryFileManager();
                                } else {
                                    Toast.makeText(this, "Ошибка создания папки", Toast.LENGTH_SHORT).show();
                                }
                                statusTextView.setText("Статус: Готов к работе 🚀");
                            });
                        }).start();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        btnCreateFile.setOnClickListener(v -> {
            LinearLayout createLayout = new LinearLayout(this);
            createLayout.setOrientation(LinearLayout.VERTICAL);
            createLayout.setPadding(15, 15, 15, 15);

            final EditText nameInput = new EditText(this);
            nameInput.setHint("Имя файла (например: Config.java)");
            createLayout.addView(nameInput);

            final EditText contentInput = new EditText(this);
            contentInput.setHint("Содержимое файла...");
            contentInput.setMinLines(5);
            contentInput.setGravity(Gravity.TOP);
            createLayout.addView(contentInput);

            new AlertDialog.Builder(this)
                .setTitle("📄 Создать новый файл")
                .setView(createLayout)
                .setPositiveButton("Создать и выгрузить", (d, w) -> {
                    String fileName = nameInput.getText().toString().trim();
                    String fileContent = contentInput.getText().toString();
                    if (!fileName.isEmpty()) {
                        String newPath = currentDir[0].isEmpty() ? fileName : currentDir[0] + "/" + fileName;
                        statusTextView.setText("Статус: ⚙️ Создание файла...");
                        new Thread(() -> {
                            boolean ok = pushBytesToGitHub(newPath, fileContent.getBytes(StandardCharsets.UTF_8), "Create file " + fileName);
                            handler.post(() -> {
                                if (ok) {
                                    Toast.makeText(this, "Файл создан!", Toast.LENGTH_SHORT).show();
                                    fileDialog.dismiss();
                                    openRepositoryFileManager();
                                } else {
                                    Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show();
                                }
                                statusTextView.setText("Статус: Готов к работе 🚀");
                            });
                        }).start();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        btnCleanRepoManager.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Подтверждение очистки репозитория")
                .setMessage("Вы действительно хотите полностью очистить репозиторий?")
                .setPositiveButton("Очистить все", (d, w) -> {
                    fileDialog.dismiss();
                    performFullRepositoryCleanAndReset();
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        btnCloseManager.setOnClickListener(v -> fileDialog.dismiss());

        refreshList[0].run();
        fileDialog.show();
    }

    private void showFileOptionsDialog(String filePath) {
        CharSequence[] options = {
            "📝 Редактировать", 
            "📋 Скопировать содержимое в буфер", 
            "📥 Загрузить/Заменить из буфера", 
            "🗑 Удалить файл"
        };
        new AlertDialog.Builder(this)
            .setTitle("Файл: " + filePath)
            .setItems(options, (dialog, which) -> {
                if (which == 0) openFileEditorForPath(filePath);
                else if (which == 1) copyRemoteFileToClipboard(filePath);
                else if (which == 2) pasteClipboardToRemoteFile(filePath);
                else if (which == 3) confirmDeleteSingleFile(filePath);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void copyRemoteFileToClipboard(String filePath) {
        statusTextView.setText("Статус: 🔄 Чтение " + filePath + "...");
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

                    handler.post(() -> {
                        safeCopyToClipboard(fileContent);
                        statusTextView.setText("Статус: Готов к работе 🚀");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка чтения файла"));
            }
        }).start();
    }

    private void pasteClipboardToRemoteFile(String filePath) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence pastedText = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (pastedText != null) {
                    statusTextView.setText("Статус: ⚙️ Запись в " + filePath + "...");
                    new Thread(() -> {
                        boolean ok = pushFileToGitHub(filePath, pastedText.toString(), "Update file from clipboard: " + filePath);
                        handler.post(() -> {
                            if (ok) Toast.makeText(this, "Файл успешно обновлен!", Toast.LENGTH_SHORT).show();
                            statusTextView.setText("Статус: Готов к работе 🚀");
                        });
                    }).start();
                    return;
                }
            }
            Toast.makeText(this, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка доступа к буферу", Toast.LENGTH_SHORT).show();
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

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(0, 0, 0, 10);

        final EditText searchInput = new EditText(this);
        searchInput.setHint("🔍 Поиск по файлу...");
        searchInput.setHintTextColor(Color.parseColor("#64748B"));
        searchInput.setTextColor(Color.parseColor("#F8FAFC"));
        searchInput.setBackgroundColor(Color.parseColor("#1E293B"));
        searchInput.setTextSize(11f);
        searchInput.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        searchInput.setLayoutParams(searchParams);
        searchBar.addView(searchInput);

        final TextView searchCount = new TextView(this);
        searchCount.setText(" 0 ");
        searchCount.setTextColor(Color.parseColor("#38BDF8"));
        searchCount.setTextSize(10f);
        searchCount.setPadding(10, 0, 10, 0);
        searchBar.addView(searchCount);

        Button btnSearchUp = createStyledButton("⬆", "#334155");
        searchBar.addView(btnSearchUp);
        Button btnSearchDown = createStyledButton("⬇", "#334155");
        searchBar.addView(btnSearchDown);

        editorLayout.addView(searchBar);

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

        final List<Integer> matchIndices = new ArrayList<>();
        final int[] activeMatchPointer = {0};

        Runnable executeSearch = () -> {
            matchIndices.clear();
            activeMatchPointer[0] = 0;
            String query = searchInput.getText().toString();
            String fullCode = codeInput.getText().toString();

            if (query.isEmpty() || fullCode.isEmpty()) {
                searchCount.setText(" 0 ");
                codeInput.setText(fullCode);
                searchInput.requestFocus(); 
                return;
            }

            String lowerFull = fullCode.toLowerCase();
            String lowerQuery = query.toLowerCase();

            int idx = lowerFull.indexOf(lowerQuery);
            while (idx >= 0) {
                matchIndices.add(idx);
                idx = lowerFull.indexOf(lowerQuery, idx + lowerQuery.length());
            }

            if (!matchIndices.isEmpty()) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(fullCode);
                for (int matchIdx : matchIndices) {
                    ssb.setSpan(new BackgroundColorSpan(Color.parseColor("#FBBF24")), matchIdx, matchIdx + query.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#000000")), matchIdx, matchIdx + query.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                codeInput.setText(ssb);
                scrollToPos(editorScroll, codeInput, matchIndices.get(0));
                searchCount.setText(" 1 / " + matchIndices.size() + " ");
            } else {
                searchCount.setText(" 0 ");
            }
            searchInput.requestFocus();
        };

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                executeSearch.run();
                searchInput.requestFocus();
            }
        });

        btnSearchDown.setOnClickListener(v -> {
            if (!matchIndices.isEmpty()) {
                activeMatchPointer[0] = (activeMatchPointer[0] + 1) % matchIndices.size();
                scrollToPos(editorScroll, codeInput, matchIndices.get(activeMatchPointer[0]));
                searchCount.setText(" " + (activeMatchPointer[0] + 1) + " / " + matchIndices.size() + " ");
                searchInput.requestFocus();
            }
        });

        btnSearchUp.setOnClickListener(v -> {
            if (!matchIndices.isEmpty()) {
                activeMatchPointer[0] = (activeMatchPointer[0] - 1 + matchIndices.size()) % matchIndices.size();
                scrollToPos(editorScroll, codeInput, matchIndices.get(activeMatchPointer[0]));
                searchCount.setText(" " + (activeMatchPointer[0] + 1) + " / " + matchIndices.size() + " ");
                searchInput.requestFocus();
            }
        });

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить", (dialog, which) -> {
                String newContent = codeInput.getText().toString();
                statusTextView.setText("Статус: ⚙️ Сохранение...");
                new Thread(() -> {
                    boolean success = pushFileToGitHub(filePath, newContent, "Update via File Manager: " + filePath);
                    handler.post(() -> {
                        if (success) Toast.makeText(this, "Файл выгружен на GitHub!", Toast.LENGTH_SHORT).show();
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

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите Java, APK или файл"), PICK_JAVA_REQUEST);
    }

    private void openUniversalFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите файл"), PICK_APK_REQUEST);
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
            performCleanAndPushNewFile(sb.toString(), fileName);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения", Toast.LENGTH_LONG).show();
        }
    }

    private void performCleanAndPushNewFile(String rawCode, String fileName) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🧹 Зачистка и перенос");
        progressDialog.setMessage("Отправка обновленного кода...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        updatePackageNameFromCode(rawCode);
        activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
        currentJavaFileName = fileName;
        saveSettings();

        final String targetPath = getMainActivityPath();
        final byte[] uploadBytes = activeCodeContent.getBytes(StandardCharsets.UTF_8);

        new Thread(() -> {
            try {
                purgeAllOldJavaFilesFromRepository();
                boolean success = false;
                int attempts = 0;
                while (attempts < 10 && !success) {
                    attempts++;
                    success = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update gradle") &&
                              pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                              pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
                              pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local") &&
                              pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                              pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                              pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                              pushBytesToGitHub(targetPath, uploadBytes, "Upload new file: " + fileName);
                    if (!success && attempts < 10) {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    }
                }

                final boolean isSuccess = success;
                handler.post(() -> {
                    progressDialog.dismiss();
                    if (isSuccess) {
                        if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Новый код выгружен!", Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Статус: ✅ Новый проект синхронизирован!");
                    } else {
                        lastFailedBytes = uploadBytes;
                        lastFailedPath = targetPath;
                        if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.VISIBLE);
                        statusTextView.setText("Статус: ❌ Ошибка сети");
                    }
                    updateAllButtonLabels();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void purgeAllOldJavaFilesFromRepository() {
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
                if (treeArray != null) {
                    for (int i = 0; i < treeArray.length(); i++) {
                        JSONObject obj = treeArray.getJSONObject(i);
                        String path = obj.optString("path", "");
                        if (path.startsWith("app/src/main/java/") && path.endsWith("MainActivity.java")) {
                            if (!path.equals(getMainActivityPath())) {
                                deleteGitHubFile(path, "Purge old path: " + path);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void retryLastUpload() {
        if (lastFailedBytes == null || lastFailedPath.isEmpty()) return;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🔄 Повторная отправка");
        progressDialog.setMessage("Отправка файла...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            boolean success = pushBytesToGitHub(lastFailedPath, lastFailedBytes, "Retry upload");
            handler.post(() -> {
                progressDialog.dismiss();
                if (success) {
                    if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Файл успешно отправлен!", Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Статус: ✅ Файл синхронизирован!");
                } else {
                    Toast.makeText(this, "Снова ошибка сети", Toast.LENGTH_LONG).show();
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

                String decompiledJava = "";
                if (lowerName.endsWith(".apk") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
                    decompiledJava = decompileDexToJava(tempFile);
                    tempFile.delete();
                } else {
                    InputStream is = getContentResolver().openInputStream(uri);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    br.close();
                    decompiledJava = sb.toString();
                }

                final String finalJava = decompiledJava;
                handler.post(() -> {
                    currentJavaFileName = fileName + " (Декомпилирован)";
                    performCleanAndPushNewFile(finalJava, fileName);
                });
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

            if (dexBytes.length < 112) return generateClockTemplate();

            int stringIdsSize = readInt(dexBytes, 56);
            int stringIdsOff = readInt(dexBytes, 60);

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(packageName).append(";\n\n");
            sb.append("import android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.*;\nimport android.graphics.*;\nimport android.view.*;\n\n");
            sb.append("public class MainActivity extends Activity {\n");
            sb.append("    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n");
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

            for (int i = 0; i < Math.min(stringIdsSize, 250); i++) {
                int stringOffOff = stringIdsOff + i * 4;
                if (stringOffOff + 4 > dexBytes.length) break;
                int stringDataOff = readInt(dexBytes, stringOffOff);
                if (stringDataOff >= 0 && stringDataOff < dexBytes.length) {
                    String str = readUleb128String(dexBytes, stringDataOff);
                    if (str != null && !str.isEmpty() && str.length() < 70 && isPrintable(str)) {
                        if (str.contains("setText") || str.contains("Button") || str.contains("Color") || str.contains("http")) {
                            sb.append("        // ").append(escapeJava(str)).append("\n");
                        }
                    }
                }
            }

            sb.append("\n        setContentView(layout);\n    }\n}\n");
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
            int len = 0, shift = 0, idx = offset;
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
        if (code == null) code = "";
        while (code.startsWith("\uFEFF") || code.startsWith("\ufeff")) code = code.substring(1);
        code = code.replace('\u00A0', ' ').replace("\u200b", "").replace("\uFEFF", "").replace("\ufeff", "");
        code = code.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        code = code.replace("\\r\\n", "\n").trim();
        code = code.replaceAll("(?m)^\\s*package\\s+[a-zA-Z0-9_.]+;?", "").trim();
        code = "package " + pkg + ";\n\n" + code;

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

    private void scrollToPos(ScrollView scroll, EditText edit, int pos) {
        if (edit == null || scroll == null || pos < 0) return;
        edit.post(() -> {
            try {
                if (edit.getLayout() != null) {
                    int line = edit.getLayout().getLineForOffset(pos);
                    int y = edit.getLayout().getLineTop(line);
                    scroll.smoothScrollTo(0, y);
                }
            } catch (Exception ignored) {}
        });
    }

    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(0, 0, 0, 10);

        final EditText searchInput = new EditText(this);
        searchInput.setHint("🔍 Поиск по коду...");
        searchInput.setHintTextColor(Color.parseColor("#64748B"));
        searchInput.setTextColor(Color.parseColor("#F8FAFC"));
        searchInput.setBackgroundColor(Color.parseColor("#1E293B"));
        searchInput.setTextSize(11f);
        searchInput.setPadding(12, 10, 12, 10);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        searchBar.addView(searchInput);

        final TextView searchCount = new TextView(this);
        searchCount.setText(" 0 / 0 ");
        searchCount.setTextColor(Color.parseColor("#38BDF8"));
        searchCount.setTextSize(11f);
        searchBar.addView(searchCount);

        Button btnPrevMatch = createStyledButton("⬆️", "#334155");
        searchBar.addView(btnPrevMatch);
        Button btnNextMatch = createStyledButton("⬇️", "#334155");
        searchBar.addView(btnNextMatch);

        editorLayout.addView(searchBar);

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1100));

        final EditText codeInput = new EditText(this);
        updatePackageNameFromCode(activeCodeContent);
        activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setGravity(Gravity.TOP);
        editorScroll.addView(codeInput);
        editorLayout.addView(editorScroll);

        final List<Integer> matchIndices = new ArrayList<>();
        final int[] activeMatchPointer = {0};

        Runnable executeSearch = () -> {
            matchIndices.clear();
            activeMatchPointer[0] = 0;
            String query = searchInput.getText().toString();
            String fullCode = codeInput.getText().toString();

            if (query.isEmpty() || fullCode.isEmpty()) {
                searchCount.setText(" 0 / 0 ");
                codeInput.setText(fullCode);
                searchInput.requestFocus();
                return;
            }

            String lowerFull = fullCode.toLowerCase();
            String lowerQuery = query.toLowerCase();

            int idx = lowerFull.indexOf(lowerQuery);
            while (idx >= 0) {
                matchIndices.add(idx);
                idx = lowerFull.indexOf(lowerQuery, idx + lowerQuery.length());
            }

            if (!matchIndices.isEmpty()) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(fullCode);
                for (int matchIdx : matchIndices) {
                    ssb.setSpan(new BackgroundColorSpan(Color.parseColor("#FBBF24")), matchIdx, matchIdx + query.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#000000")), matchIdx, matchIdx + query.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                codeInput.setText(ssb);
                scrollToPos(editorScroll, codeInput, matchIndices.get(0));
                searchCount.setText(" 1 / " + matchIndices.size() + " ");
            } else {
                searchCount.setText(" 0 / 0 ");
            }
            searchInput.requestFocus();
        };

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                executeSearch.run();
                searchInput.requestFocus();
            }
        });

        btnNextMatch.setOnClickListener(v -> {
            if (!matchIndices.isEmpty()) {
                activeMatchPointer[0] = (activeMatchPointer[0] + 1) % matchIndices.size();
                scrollToPos(editorScroll, codeInput, matchIndices.get(activeMatchPointer[0]));
                searchCount.setText(" " + (activeMatchPointer[0] + 1) + " / " + matchIndices.size() + " ");
                searchInput.requestFocus();
            }
        });

        btnPrevMatch.setOnClickListener(v -> {
            if (!matchIndices.isEmpty()) {
                activeMatchPointer[0] = (activeMatchPointer[0] - 1 + matchIndices.size()) % matchIndices.size();
                scrollToPos(editorScroll, codeInput, matchIndices.get(activeMatchPointer[0]));
                searchCount.setText(" " + (activeMatchPointer[0] + 1) + " / " + matchIndices.size() + " ");
                searchInput.requestFocus();
            }
        });

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода")
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить", (dialog, which) -> {
                String rawCode = codeInput.getText().toString();
                updatePackageNameFromCode(rawCode);
                activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
                saveSettings();

                statusTextView.setText("Статус: ⚙️ Выгрузка на GitHub...");
                new Thread(() -> {
                    purgeAllOldJavaFilesFromRepository();
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Code Editor Save");
                    handler.post(() -> {
                        if (success) {
                            Toast.makeText(this, "Код синхронизирован!", Toast.LENGTH_SHORT).show();
                            statusTextView.setText("Статус: ✅ Код обновлен!");
                        } else {
                            Toast.makeText(this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                        updateAllButtonLabels();
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
        btnBuild.setText("⏳ Компиляция на сервере...\n📦 " + apkTitle);
        statusTextView.setText("Статус: ⚙️ Синхронизация репозитория...");
        logEditText.setText("Отправка кода на GitHub и запуск сборки...");

        new Thread(() -> {
            try {
                purgeAllOldJavaFilesFromRepository();
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                
                Boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                 pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local.properties") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                 pushFileToGitHub("app/src/main/assets/.gitkeep", "", "Create assets folder") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");

                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
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
                        statusTextView.setText("Статус: 🚀 Сборка запущена! Ожидание APK...");
                        logEditText.setText("GitHub Actions компилирует проект...\nОжидание завершения сборки для сохранения APK в папку 'Загрузки'...");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                        updateAllButtonLabels();
                        statusTextView.setText("Статус: ❌ Ошибка запуска workflow");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
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
                btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                updateAllButtonLabels();
            }
            statusTextView.setText("Статус: 🛑 Сборка отменена!");
            logEditText.setText("⚠️ Сборка была остановлена пользователем.");
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
                                    btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                                    updateAllButtonLabels();
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Сборка успешна! Загрузка APK в папку 'Загрузки'...");
                                        downloadArtifactsAndSaveToDownloads(latestRunId);
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции. Анализ логов...");
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

    private void downloadArtifactsAndSaveToDownloads(long runId) {
        new Thread(() -> {
            try {
                String artifactsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + runId + "/artifacts";
                HttpURLConnection conn = (HttpURLConnection) new URL(artifactsUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONArray artifacts = new JSONObject(sb.toString()).getJSONArray("artifacts");
                    
                    if (artifacts.length() > 0) {
                        JSONObject art = artifacts.getJSONObject(0);
                        String downloadUrl = art.getString("archive_download_url");
                        
                        HttpURLConnection dlConn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                        dlConn.setRequestMethod("GET");
                        dlConn.setRequestProperty("Authorization", "Bearer " + githubToken);

                        if (dlConn.getResponseCode() == 200) {
                            File zipFile = new File(getCacheDir(), "build_artifact.zip");
                            try (InputStream in = dlConn.getInputStream();
                                 OutputStream out = new java.io.FileOutputStream(zipFile)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                            }

                            // Распаковываем ZIP и находим APK
                            ZipFile zip = new ZipFile(zipFile);
                            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                            while (entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement();
                                if (!entry.isDirectory() && entry.getName().endsWith(".apk")) {
                                    InputStream is = zip.getInputStream(entry);
                                    byte[] apkBytes = readBytes(is);
                                    is.close();
                                    
                                    // Сохраняем в папку Загрузки телефона!
                                    saveBytesToDownloads(apkTitle + "_v" + appVersion + ".apk", apkBytes, "application/vnd.android.package-archive");
                                    break;
                                }
                            }
                            zip.close();
                            zipFile.delete();

                            handler.post(() -> {
                                statusTextView.setText("Статус: ✅ APK успешно сохранен в папку 'Загрузки'!");
                                logEditText.setText("=== 📥 ГОТОВЫЙ APK СКАЧАН И СОХРАНЕН ===\n• Файл сохранен в системную папку 'Загрузки' телефона.");
                                Toast.makeText(MainActivity.this, "APK сохранен в Загрузки!", Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                    }
                }
                handler.post(() -> statusTextView.setText("Статус: ⚠️ Артефакт сборки не найден"));
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка сохранения APK: " + e.getMessage()));
            }
        }).start();
    }

    private void saveBytesToDownloads(String fileName, byte[] data, String mimeType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        out.write(data);
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File destFile = new File(downloadsDir, fileName);
                try (OutputStream out = new java.io.FileOutputStream(destFile)) {
                    out.write(data);
                }
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(destFile));
                sendBroadcast(intent);
            }
        } catch (Exception ignored) {}
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
                            String errorLog = ls.toString();

                            String fixedCode = activeCodeContent;
                            boolean repaired = false;
                            StringBuilder report = new StringBuilder("=== 🎓 AI-ПРОФЕССОР: ДИАГНОСТИЧЕСКИЙ ОТЧЕТ ===\n\n");

                            if (errorLog.contains("text blocks are not supported") || errorLog.contains("feature is not supported in -source")) {
                                javaVersion = "17";
                                minSdk = 26;
                                saveSettings();
                                repaired = true;
                                report.append("  ➜ 🛠 Авто-Патч: Версия проекта переключена на Java 17 / SDK 26!\n");
                            }

                            if (errorLog.contains("illegal character") || errorLog.contains("\ufeff") || errorLog.contains("\uFEFF")) {
                                fixedCode = sanitizeAndInjectJavaCode(fixedCode, packageName);
                                repaired = true;
                                report.append("  ➜ 🛠 Авто-Патч: Код очищен от BOM и скрытых символов!\n");
                            }

                            if (errorLog.contains("cannot find symbol") && errorLog.contains("WebView")) {
                                if (!fixedCode.contains("import android.webkit.WebView;")) {
                                    fixedCode = fixedCode.replaceFirst("package\\s+[a-zA-Z0-9_.]+;", "$0\n\nimport android.webkit.WebView;\nimport android.webkit.WebSettings;");
                                    repaired = true;
                                    report.append("  ➜ 🛠 Авто-Патч: Подключен WebView.\n");
                                }
                            }

                            final boolean isRepaired = repaired;
                            final String newFixedCode = fixedCode;

                            handler.post(() -> {
                                logEditText.setText(report.toString());
                                showAIDoctorDiagnosticsDialog(report.toString(), newFixedCode, isRepaired);
                            });
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void showAIDoctorDiagnosticsDialog(String reportText, String fixedCode, boolean isRepaired) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setBackgroundColor(Color.parseColor("#090D16"));

        final EditText reportArea = new EditText(this);
        reportArea.setText(reportText);
        reportArea.setTextSize(10f);
        reportArea.setTypeface(Typeface.MONOSPACE);
        reportArea.setTextColor(Color.parseColor("#E2E8F0"));
        reportArea.setBackgroundColor(Color.parseColor("#030712"));
        reportArea.setPadding(12, 12, 12, 12);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 700));
        scroll.addView(reportArea);
        layout.addView(scroll);

        Button btnApplyDoctorFix = createStyledButton("🩺 Авто-исправить и перезапустить", "#0F766E");
        layout.addView(btnApplyDoctorFix);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("🔬 Диагностика GitHub Actions")
            .setView(layout)
            .setNegativeButton("Закрыть", null)
            .create();

        btnApplyDoctorFix.setOnClickListener(v -> {
            dialog.dismiss();
            activeCodeContent = fixedCode;
            saveSettings();
            statusTextView.setText("Статус: 🚀 Отправка исправления и перезапуск...");
            new Thread(() -> {
                purgeAllOldJavaFilesFromRepository();
                boolean pushed = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "AI Auto-Fix");
                if (pushed) handler.post(() -> triggerGitHubBuild());
            }).start();
        });

        dialog.show();
    }

    private void performFullRepositoryCleanAndReset() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        statusTextView.setText("Статус: 🗑 Полная очистка репозитория...");
        logEditText.setText("Удаление старых файлов и сброс репозитория...");

        new Thread(() -> {
            try {
                purgeAllOldJavaFilesFromRepository();
                activeCodeContent = generateClockTemplate();
                currentJavaFileName = "Шаблон: Часы с фонариком и яркостью";
                apkTitle = "MonsterClock";
                appVersion = "009";
                versionCode = 9;
                saveSettings();

                Boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Settings") &&
                                 pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Props") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Local") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "App Gradle") &&
                                 pushFileToGitHub("app/src/main/assets/README.txt", "Put your .onnx AI models here", "Assets") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "MainActivity");

                handler.post(() -> {
                    if (synced) {
                        statusTextView.setText("Статус: ✅ Репозиторий очищен!");
                        Toast.makeText(MainActivity.this, "Репозиторий очищен!", Toast.LENGTH_LONG).show();
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
        input.setText(apkTitle);
        new AlertDialog.Builder(this)
            .setTitle("Имя APK файла")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                apkTitle = input.getText().toString().trim();
                if (apkTitle.isEmpty()) apkTitle = "RomaMonsterApp";
                saveSettings();
                Toast.makeText(this, "Имя APK обновлено!", Toast.LENGTH_SHORT).show();
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
                appVersion = sanitizeVersionText(input.getText().toString().trim());
                saveSettings();
                ProgressDialog pd = ProgressDialog.show(this, "Перенос", "Обновление версии...");
                new Thread(() -> {
                    purgeAllOldJavaFilesFromRepository();
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Version update");
                    handler.post(() -> {
                        pd.dismiss();
                        updateAllButtonLabels();
                        Toast.makeText(this, "Версия обновлена!", Toast.LENGTH_SHORT).show();
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditPackageDialog() {
        final EditText input = new EditText(this);
        input.setText(packageName);
        new AlertDialog.Builder(this)
            .setTitle("📦 Имя пакета")
            .setView(input)
            .setPositiveButton("Применить", (dialog, which) -> {
                String newPkg = input.getText().toString().trim();
                packageName = newPkg.startsWith("com.") ? newPkg : "com." + transliterateToLatin(newPkg) + ".version" + sanitizeVersionText(appVersion);
                
                ProgressDialog pd = ProgressDialog.show(this, "Перенос", "Обновление пути пакета...");
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                saveSettings();

                new Thread(() -> {
                    purgeAllOldJavaFilesFromRepository();
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Package update");
                    handler.post(() -> {
                        pd.dismiss();
                        updateAllButtonLabels();
                        Toast.makeText(this, "Пакет изменен!", Toast.LENGTH_SHORT).show();
                    });
                }).start();
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

        final Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
            "⚡ Стабильный (Java 8)", "🚀 Java 17"
        }));
        spinner.setSelection(javaVersion.equals("8") ? 0 : 1);
        layout.addView(spinner);

        Button btnCleanRepoInsideSettings = createStyledButton("🗑 Полная очистка репозитория", "#991B1B");
        btnCleanRepoInsideSettings.setOnClickListener(v -> performFullRepositoryCleanAndReset());
        layout.addView(btnCleanRepoInsideSettings);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки GitHub & SDK")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                javaVersion = spinner.getSelectedItemPosition() == 0 ? "8" : "17";
                minSdk = spinner.getSelectedItemPosition() == 0 ? 21 : 26;
                saveSettings();
                Toast.makeText(this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
                if (!githubToken.isEmpty()) fetchRemoteRepositoryInfo();
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
        return "rootProject.name = \"" + apkTitle + "\"\ninclude ':app'\n";
    }

    private String getGradlePropertiesContent() {
        return "org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\nandroid.useJetifier=true\n";
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
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
               "      - name: Setup Gradle\n        uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Build APK\n        run: |\n" +
               "          gradle clean assembleDebug --info --stacktrace --no-daemon\n" +
               "      - name: Upload APK Artifact\n        uses: actions/upload-artifact@v4\n        with:\n          name: " + apkTitle + "_v" + appVersion + "\n          path: app/build/outputs/apk/debug/*.apk\n";
    }

    private String extractThemeFromCode(String code) {
        if (code == null) return "@android:style/Theme.Material.Light.NoActionBar";
        Matcher m = Pattern.compile("//\\s*THEME:\\s*([@a-zA-Z0-9_./:]+)").matcher(code);
        if (m.find()) return m.group(1);
        return "@android:style/Theme.Material.Light.NoActionBar";
    }

    private String generateManifest() {
        String appTheme = extractThemeFromCode(activeCodeContent);
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.TRANSMIT_IR\" />\n" +
               "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n" +
               "    <application android:allowBackup=\"true\" android:usesCleartextTraffic=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + apkTitle + "\" android:theme=\"" + appTheme + "\">\n" +
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
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode " + versionCode + "\n        versionName \"" + appVersion + "\"\n        multiDexEnabled true\n    }\n" +
               "    compileOptions {\n        sourceCompatibility " + compat + "\n        targetCompatibility " + compat + "\n    }\n" +
               "    applicationVariants.all { variant ->\n" +
               "        variant.outputs.all { output ->\n" +
               "            outputFileName = \"" + apkTitle + "_v" + appVersion + "-\" + variant.name + \".apk\"\n" +
               "        }\n" +
               "    }\n" +
               "}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n    implementation 'com.squareup.okhttp3:okhttp:4.12.0'\n    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.0'\n    implementation 'androidx.multidex:multidex:2.0.1'\n}\n";
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
               "// THEME: @android:style/Theme.Material.Light.NoActionBar\n" +
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
               "        title.setText(\"⚡ Monster Clock & Flash (Local)\");\n" +
               "        title.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        title.setTextSize(20f);\n" +
               "        title.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        title.setGravity(Gravity.CENTER);\n" +
               "        title.setPadding(0, 0, 0, 15);\n" +
               "        layout.addView(title);\n\n" +
               "        TextClock clock = new TextClock(this);\n" +
               "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        clock.setTextSize(48f);\n" +
               "        clock.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        clock.setGravity(Gravity.CENTER);\n" +
               "        clock.setPadding(0, 0, 0, 30);\n" +
               "        layout.addView(clock);\n\n" +
               "        Button flashBtn = new Button(this);\n" +
               "        flashBtn.setText(\"🔦 Включить фонарик\");\n" +
               "        flashBtn.setTextColor(Color.WHITE);\n" +
               "        flashBtn.setTextSize(14f);\n" +
               "        flashBtn.setBackgroundColor(Color.parseColor(\"#2563EB\"));\n" +
               "        flashBtn.setPadding(20, 20, 20, 20);\n" +
               "        layout.addView(flashBtn);\n\n" +
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
               "                }\n" +
               "            } catch (Exception e) {}\n" +
               "        });\n\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}