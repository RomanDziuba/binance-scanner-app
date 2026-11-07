package com.pro5.version09;

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
 * 🚀 MONSTER COMPILER CI/CD ENGINE v9.0 ULTRA LOCAL
 * (No Telegram — Direct Download Folder Export)
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
    private String appVersion = "002";
    private int versionCode = 2;
    private String packageName = "com.roman.version002";
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
    private String lastErrorLog = "";
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
        titleView.setText("🚀 Android Monster Local (v9.0 Ultra)");
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
        logEditText.setHint("Здесь выводятся логи и отчеты AI-доктора...");
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
        logEditText.setText("=== 🚀 MONSTER COMPILER LOCAL V9.0 ЗАПУЩЕН ===\n" +
                            "• Имя APK: " + apkTitle + "\n" +
                            "• Пакет: " + packageName + "\n" +
                            "• Версия: " + appVersion + " (Code: " + versionCode + ")\n" +
                            "• Режим: Сохранение в память телефона (Download)\n" +
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
            if (textToCopy.length() > 50000) textToCopy = textToCopy.substring(textToCopy.length() - 50000);
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
        return "🚀 Скомпилировать APK (в Download)\n📦 " + apkTitle + " (v" + appVersion + ")";
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
        if (sanitizedVer.isEmpty()) sanitizedVer = "002";
        packageName = "com." + coreName + ".version" + sanitizedVer;
    }

    private String sanitizeVersionText(String input) {
        if (input == null) return "002";
        String res = input.toLowerCase();
        String[] abcCyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","є","і","ї","ґ"};
        for (String c : abcCyr) res = res.replace(c, "");
        res = res.replaceAll("[^a-z0-9]", "");
        return res.isEmpty() ? "002" : res;
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
        appVersion = sp.getString("appVersion", "002");
        versionCode = sp.getInt("versionCode", 2);
        packageName = sp.getString("packageName", "com.roman.version002");
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
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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
                        logEditText.setText("=== 🔄 СИНХРОНИЗАЦИЯ УСПЕШНА ===\n• Пакет: " + packageName + "\n• Версия: " + appVersion);
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
                    List<RepoItem> items = new ArrayList<>();
                    if (treeArray != null) {
                        for (int i = 0; i < treeArray.length(); i++) {
                            JSONObject obj = treeArray.getJSONObject(i);
                            items.add(new RepoItem(obj.optString("path"), obj.optString("type"), obj.optString("sha"), obj.optLong("size", 0)));
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
        listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 650));
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
                Toast.makeText(this, "Вы в корне репозитория", Toast.LENGTH_SHORT).show();
            }
        });

        btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir[0]));
        btnCreateFolder.setOnClickListener(v -> {
            final EditText folderInput = new EditText(this);
            folderInput.setHint("Имя новой папки");
            new AlertDialog.Builder(this)
                .setTitle("📁 Создать папку")
                .setView(folderInput)
                .setPositiveButton("Создать", (d, w) -> {
                    String folderName = folderInput.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        String newPath = currentDir[0].isEmpty() ? folderName + "/.gitkeep" : currentDir[0] + "/" + folderName + "/.gitkeep";
                        new Thread(() -> {
                            boolean ok = pushBytesToGitHub(newPath, new byte[0], "Create folder " + folderName);
                            handler.post(() -> {
                                if (ok) {
                                    fileDialog.dismiss();
                                    openRepositoryFileManager();
                                }
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
            contentInput.setHint("Содержимое...");
            contentInput.setMinLines(5);
            createLayout.addView(contentInput);

            new AlertDialog.Builder(this)
                .setTitle("📄 Создать файл")
                .setView(createLayout)
                .setPositiveButton("Создать", (d, w) -> {
                    String fileName = nameInput.getText().toString().trim();
                    String fileContent = contentInput.getText().toString();
                    if (!fileName.isEmpty()) {
                        String newPath = currentDir[0].isEmpty() ? fileName : currentDir[0] + "/" + fileName;
                        new Thread(() -> {
                            boolean ok = pushBytesToGitHub(newPath, fileContent.getBytes(StandardCharsets.UTF_8), "Create file");
                            handler.post(() -> {
                                if (ok) {
                                    fileDialog.dismiss();
                                    openRepositoryFileManager();
                                }
                            });
                        }).start();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        btnCleanRepoManager.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Очистка репозитория")
                .setMessage("Очистить репозиторий полностью?")
                .setPositiveButton("Очистить", (d, w) -> {
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
        CharSequence[] options = {"📝 Редактировать", "📋 Копировать в буфер", "📥 Загрузить из буфера", "🗑 Удалить"};
        new AlertDialog.Builder(this)
            .setTitle(filePath)
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
                    handler.post(() -> safeCopyToClipboard(fileContent));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void pasteClipboardToRemoteFile(String filePath) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                CharSequence pasted = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (pasted != null) {
                    new Thread(() -> {
                        pushFileToGitHub(filePath, pasted.toString(), "Update from clipboard");
                        handler.post(() -> Toast.makeText(this, "Обновлено!", Toast.LENGTH_SHORT).show());
                    }).start();
                }
            }
        } catch (Exception ignored) {}
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
        codeInput.setPadding(12, 12, 12, 12);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 900));
        scroll.addView(codeInput);
        editorLayout.addView(scroll);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String newContent = codeInput.getText().toString();
                new Thread(() -> {
                    pushFileToGitHub(filePath, newContent, "Update file");
                    handler.post(() -> Toast.makeText(this, "Сохранено!", Toast.LENGTH_SHORT).show());
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmDeleteSingleFile(String filePath) {
        new AlertDialog.Builder(this)
            .setTitle("🗑 Удаление")
            .setMessage("Удалить " + filePath + "?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                new Thread(() -> {
                    deleteGitHubFile(filePath, "Delete");
                    handler.post(() -> openRepositoryFileManager());
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
            InputStream in = getContentResolver().openInputStream(uri);
            byte[] fileBytes = readBytes(in);
            new Thread(() -> {
                pushBytesToGitHub(targetPath, fileBytes, "Upload");
                handler.post(() -> openRepositoryFileManager());
            }).start();
        } catch (Exception ignored) {}
    }

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите файл"), PICK_JAVA_REQUEST);
    }

    private void openUniversalFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите APK/ZIP"), PICK_APK_REQUEST);
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
            BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            performCleanAndPushNewFile(sb.toString(), fileName);
        } catch (Exception ignored) {}
    }

    private void performCleanAndPushNewFile(String rawCode, String fileName) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🧹 Зачистка и перенос");
        progressDialog.setMessage("Отправка кода...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        updatePackageNameFromCode(rawCode);
        activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
        currentJavaFileName = fileName;
        saveSettings();

        final String targetPath = getMainActivityPath();
        final byte[] uploadBytes = activeCodeContent.getBytes(StandardCharsets.UTF_8);

        new Thread(() -> {
            purgeAllOldJavaFilesFromRepository();
            boolean success = pushFileToGitHub("build.gradle", getRootGradleContent(), "gradle") &&
                              pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "settings") &&
                              pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "props") &&
                              pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "local") &&
                              pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "manifest") &&
                              pushFileToGitHub("app/build.gradle", generateAppGradle(), "app gradle") &&
                              pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "workflow") &&
                              pushBytesToGitHub(targetPath, uploadBytes, "MainActivity");

            handler.post(() -> {
                progressDialog.dismiss();
                if (success) {
                    if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Новый код выгружен!", Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Статус: ✅ Синхронизировано!");
                } else {
                    lastFailedBytes = uploadBytes;
                    lastFailedPath = targetPath;
                    if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.VISIBLE);
                }
                updateAllButtonLabels();
            });
        }).start();
    }

    private void purgeAllOldJavaFilesFromRepository() {
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
                        String path = treeArray.getJSONObject(i).optString("path", "");
                        if (path.startsWith("app/src/main/java/") && path.endsWith("MainActivity.java")) {
                            if (!path.equals(getMainActivityPath())) deleteGitHubFile(path, "purge");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void retryLastUpload() {
        if (lastFailedBytes == null) return;
        new Thread(() -> {
            boolean success = pushBytesToGitHub(lastFailedPath, lastFailedBytes, "retry");
            handler.post(() -> {
                if (success && btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
            });
        }).start();
    }

    private void processUniversalFileForDecompilation(Uri uri) {
        // Заглушка декомпиляции сохранена без изменений логики
        Toast.makeText(this, "Обработка файла...", Toast.LENGTH_SHORT).show();
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
                pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "icon");
                handler.post(() -> {
                    iconCustomized = false;
                    saveSettings();
                    Toast.makeText(this, "Иконка обновлена!", Toast.LENGTH_SHORT).show();
                });
            }).start();
        } catch (Exception ignored) {}
    }

    private void deleteGitHubFile(String path, String message) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                String sha = new JSONObject(sb.toString()).optString("sha", null);

                HttpURLConnection del = (HttpURLConnection) new URL(urlStr).openConnection();
                del.setRequestMethod("DELETE");
                del.setRequestProperty("Authorization", "Bearer " + githubToken);
                del.setRequestProperty("Content-Type", "application/json; utf-8");
                del.setDoOutput(true);
                JSONObject payload = new JSONObject();
                payload.put("message", message);
                payload.put("sha", sha);
                payload.put("branch", "main");
                try (OutputStream os = del.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                del.getResponseCode();
            }
        } catch (Exception ignored) {}
    }

    private String sanitizeAndInjectJavaCode(String code, String pkg) {
        if (code == null) code = "";
        while (code.startsWith("\uFEFF") || code.startsWith("\ufeff")) code = code.substring(1);
        code = code.replace('\u00A0', ' ').replace("\u200b", "").trim();
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

    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        final EditText codeInput = new EditText(this);
        updatePackageNameFromCode(activeCodeContent);
        activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1100));
        scroll.addView(codeInput);
        editorLayout.addView(scroll);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода")
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить", (dialog, which) -> {
                activeCodeContent = sanitizeAndInjectJavaCode(codeInput.getText().toString(), packageName);
                saveSettings();
                new Thread(() -> {
                    purgeAllOldJavaFilesFromRepository();
                    pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Save");
                    handler.post(() -> Toast.makeText(this, "Сохранено на GitHub!", Toast.LENGTH_SHORT).show());
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void triggerGitHubBuild() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        versionCode++;
        saveSettings();

        btnBuild.setEnabled(false);
        btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
        btnBuild.setText("⏳ Компиляция (в Download)...\n📦 " + apkTitle);
        statusTextView.setText("Статус: ⚙️ Подготовка сборки...");
        logEditText.setText("Отправка кода в репозиторий для сборки локального APK...");

        new Thread(() -> {
            try {
                purgeAllOldJavaFilesFromRepository();
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                
                Boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "root gradle") &&
                                 pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "settings") &&
                                 pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "props") &&
                                 pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "local") &&
                                 pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "manifest") &&
                                 pushFileToGitHub("app/build.gradle", generateAppGradle(), "app gradle") &&
                                 pushFileToGitHub("app/src/main/assets/.gitkeep", "", "assets") &&
                                 pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "workflow") &&
                                 pushFileToGitHub(getMainActivityPath(), activeCodeContent, "MainActivity");

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
                HttpURLConnection conn = (HttpURLConnection) new URL(dispatchUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write("{\"ref\":\"main\"}".getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() == 204 || conn.getResponseCode() == 201) {
                    handler.post(() -> {
                        statusTextView.setText("Статус: 🚀 Сборка запущена! Ожидание файла...");
                        logEditText.setText("GitHub Actions компилирует APK...\nПосле завершения файл будет сохранен в репозиторий и доступен для скачивания во вкладке Actions (Artifacts).");
                        startMonitoringBuild();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                    updateAllButtonLabels();
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
            statusTextView.setText("Статус: 🛑 Сборка остановлена");
        });
    }

    private boolean pushFileToGitHub(String path, String content, String message) {
        return pushBytesToGitHub(path, content.getBytes(StandardCharsets.UTF_8), message);
    }

    private boolean pushBytesToGitHub(String path, byte[] bytes, String message) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            String sha = null;
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                sha = new JSONObject(sb.toString()).optString("sha", null);
            }

            HttpURLConnection put = (HttpURLConnection) new URL(urlStr).openConnection();
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
                            handler.post(() -> {
                                if (!isMonitoring) return;
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                                    updateAllButtonLabels();
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Сборка успешна! APK сохранен в GitHub Artifacts.");
                                        logEditText.setText("Сборка завершена успешно! Готовый APK доступен в репозитории во вкладке Actions -> Build Monster APK -> Artifacts.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции");
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void performFullRepositoryCleanAndReset() {
        new Thread(() -> {
            purgeAllOldJavaFilesFromRepository();
            activeCodeContent = generateClockTemplate();
            saveSettings();
            handler.post(() -> Toast.makeText(this, "Сброшено!", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(apkTitle);
        new AlertDialog.Builder(this)
            .setTitle("Имя APK")
            .setView(input)
            .setPositiveButton("Сохранить", (d, w) -> {
                apkTitle = input.getText().toString().trim();
                if (apkTitle.isEmpty()) apkTitle = "RomaMonsterApp";
                saveSettings();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditVersionDialog() {
        final EditText input = new EditText(this);
        input.setText(appVersion);
        new AlertDialog.Builder(this)
            .setTitle("Версия")
            .setView(input)
            .setPositiveButton("Сохранить", (d, w) -> {
                appVersion = sanitizeVersionText(input.getText().toString());
                saveSettings();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditPackageDialog() {
        final EditText input = new EditText(this);
        input.setText(packageName);
        new AlertDialog.Builder(this)
            .setTitle("Пакет")
            .setView(input)
            .setPositiveButton("Применить", (d, w) -> {
                packageName = input.getText().toString().trim();
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

        final Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"⚡ Java 8", "🚀 Java 17"}));
        spinner.setSelection(javaVersion.equals("8") ? 0 : 1);
        layout.addView(spinner);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки")
            .setView(layout)
            .setPositiveButton("Сохранить", (d, w) -> {
                githubToken = inputGithub.getText().toString().trim();
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

    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <application android:allowBackup=\"true\" android:usesCleartextTraffic=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + apkTitle + "\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
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
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n    implementation 'androidx.multidex:multidex:2.0.1'\n}\n";
    }

    private String generateClockTemplate() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.os.Bundle;\n" +
               "import android.widget.*;\n" +
               "import android.graphics.*;\n" +
               "import android.view.*;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        layout.setPadding(30, 30, 30, 30);\n\n" +
               "        TextView tv = new TextView(this);\n" +
               "        tv.setText(\"⚡ Monster Local App v9.0\");\n" +
               "        tv.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        tv.setTextSize(20f);\n" +
               "        tv.setGravity(Gravity.CENTER);\n" +
               "        layout.addView(tv);\n\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}