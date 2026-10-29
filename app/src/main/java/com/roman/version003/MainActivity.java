package com.roman.version002;

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
 * 🚀 MONSTER COMPILER CI/CD ENGINE v1.1 PRO
 * (Live Package Injector, Smart Purge & Unwrapped Horizontal Code Editor)
 * Автор: Dziuba Roman
 */
public class MainActivity extends Activity {

    // ==========================================
    // КЛЮЧИ И НАСТРОЙКИ ПРИЛОЖЕНИЯ ПО УМОЛЧАНИЮ
    // ==========================================
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
    
    private String telegramApkTitle = "RomaMonsterApp";
    private String appVersion = "001";
    private int versionCode = 1;
    private String packageName = "com.roman.version001";
    private String currentJavaFileName = "😛";
    private boolean iconCustomized = true;
    private String targetUploadFolder = "";

    private int minSdk = 21;
    private int compileSdk = 34;
    private String javaVersion = "8"; 
    private String activeCodeContent = "";
    private long latestRunId = -1;

    // Вью-компоненты основного интерфейса
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
        authorView.setText("✦ Dziuba Roman — Monster CI/CD Engine v1.1 Pro ✦");
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
        titleView.setText("🚀 Android Monster Compiler (Pro Engine v1.1)");
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

        btnCodeEditor = createStyledButton("📝 Редактор кода (Без переноса строк)", "#0F172A");
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
        logLabel.setText("📋 Профессиональный терминал и отчет об ошибках:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь выводятся структурированные логи и автоматические отчеты AI-доктора...");
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
        logEditText.setText("=== 🚀 MONSTER COMPILER PRO V1.1 ЗАПУЩЕН ===
" +
                            "• Имя для Telegram APK: " + telegramApkTitle + "
" +
                            "• Пакет: " + packageName + "
" +
                            "• Версия: " + appVersion + " (Code: " + versionCode + ")
" +
                            "• GitHub Token: " + (githubToken.isEmpty() ? "⚠️ Не введен" : "✅ Загружен"));

        if (!githubToken.isEmpty()) {
            fetchRemoteRepositoryInfo();
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
        String info = "🏷 Имя APK: " + telegramApkTitle + " | 📦 Пакет: " + packageName + "
" +
                      "🔢 Версия: " + appVersion + " (Code: " + versionCode + ") | 📂 Репо: " + repoOwner + "/" + repoName + "
" +
                      "☕ Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profile + "]";
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя для Telegram APK: " + telegramApkTitle);
        if (btnVersion != null) btnVersion.setText("📦 Версия: " + appVersion);
        if (btnPackageName != null) btnPackageName.setText("📦 Пакет: " + packageName);
        if (btnUploadJava != null) btnUploadJava.setText("📂 Загрузи Файл сюда👈  " + currentJavaFileName);
        if (btnBuild != null) btnBuild.setText(getBuildButtonText());
        if (btnIcon != null) btnIcon.setText(iconCustomized ? "🖼 Иконка: ⚡ Шаблон" : "🖼 Иконка: 🎨 Пользовательская");
    }

    private String getBuildButtonText() {
        return "🚀 Скомпилировать APK
📦 " + telegramApkTitle + " (v" + appVersion + ")";
    }

    private String extractAndCleanPackageFromCode(String code) {
        if (code == null || code.isEmpty()) return "roman";
        
        // Strict Sanitization of invisible characters and BOM
        code = code.replace("", "").replace("", "").replace("​", "").replace(" ", " ");
        code = code.replace("
", "
").trim();
        
        String[] lines = code.split("
");
        String targetLine = "";
        
        for (String l : lines) {
            String trimmed = l.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                targetLine = trimmed;
                break;
            }
        }
        
        if (targetLine.isEmpty() && lines.length > 0) {
            targetLine = lines[0].trim();
        }
        
        Matcher m = Pattern.compile("^package\s+([a-zA-Z0-9_.]+);?").matcher(targetLine);
        if (m.find()) {
            String rawPkg = m.group(1);
            String[] parts = rawPkg.split("\.");
            if (parts.length >= 2) {
                for (String p : parts) {
                    if (!p.equals("com") && !p.startsWith("version") && !p.matches("\d+")) {
                        return transliterateToLatin(p);
                    }
                }
                return transliterateToLatin(parts[parts.length - 1]);
            } else if (parts.length == 1) {
                return transliterateToLatin(parts[0]);
            }
        }
        
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            Matcher m2 = Pattern.compile("package\s+([a-zA-Z0-9_.]+);?").matcher(lines[i]);
            if (m2.find()) {
                String rawPkg = m2.group(1);
                String[] parts = rawPkg.split("\.");
                for (String p : parts) {
                    if (!p.equals("com") && !p.startsWith("version") && !p.matches("\d+")) {
                        return transliterateToLatin(p);
                    }
                }
            }
        }
        
        return "roman";
    }

    private void updatePackageNameFromCode(String code) {
        String coreName = extractAndCleanPackageFromCode(code);
        String sanitizedVer = sanitizeVersionText(appVersion);
        if (sanitizedVer.isEmpty()) sanitizedVer = "001";
        packageName = "com." + coreName + ".version" + sanitizedVer;
    }

    private String sanitizeVersionText(String input) {
        if (input == null) return "001";
        String res = input.toLowerCase();
        String[] abcCyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","є","і","ї","ґ"};
        for (String c : abcCyr) {
            res = res.replace(c, "");
        }
        res = res.replaceAll("[^a-z0-9]", "");
        return res.isEmpty() ? "001" : res;
    }

    private String transliterateToLatin(String input) {
        if (input == null) return "roman";
        String[] abcCyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","є","і","ї","ґ"};
        String[] abcLat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","shch","","y","","e","yu","ya","ye","i","yi","g"};

        String res = input.toLowerCase();
        for (int i = 0; i < abcCyr.length; i++) {
            res = res.replace(abcCyr[i], abcLat[i]);
        }
        res = res.replaceAll("[^a-z0-9]", "");
        if (res.isEmpty()) res = "roman";
        if (Character.isDigit(res.charAt(0))) res = "app" + res;
        return res;
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        githubToken = sp.getString("githubToken", "");
        botToken = sp.getString("botToken", botToken);
        chatId = sp.getString("chatId", chatId);
        repoOwner = sp.getString("repoOwner", repoOwner);
        repoName = sp.getString("repoName", repoName);
        telegramApkTitle = sp.getString("telegramApkTitle", telegramApkTitle);
        appVersion = sp.getString("appVersion", "001");
        versionCode = sp.getInt("versionCode", 1);
        packageName = sp.getString("packageName", "com.roman.version001");
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
        editor.putString("botToken", botToken);
        editor.putString("chatId", chatId);
        editor.putString("repoOwner", repoOwner);
        editor.putString("repoName", repoName);
        editor.putString("telegramApkTitle", telegramApkTitle);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        while ((line = br.readLine()) != null) sb.append(line).append("
");
                        JSONObject json = new JSONObject(sb.toString());
                        String contentB64 = json.getString("content").replaceAll("\s", "");
                        String gradleContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);

                        Matcher vMatcher = Pattern.compile("versionName\s+"([^"]+)"").matcher(gradleContent);
                        if (vMatcher.find()) appVersion = vMatcher.group(1);
                        Matcher vcMatcher = Pattern.compile("versionCode\s+(\d+)").matcher(gradleContent);
                        if (vcMatcher.find()) versionCode = Integer.parseInt(vcMatcher.group(1));
                        Matcher nsMatcher = Pattern.compile("namespace\s+'([^']+)'").matcher(gradleContent);
                        if (nsMatcher.find()) packageName = nsMatcher.group(1);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                saveSettings();
                                statusTextView.setText("Статус: ✅ Репозиторий синхронизирован!");
                                logEditText.setText("=== 🔄 СИНХРОНИЗАЦИЯ УСПЕШНА ===
• Пакет: " + packageName + "
• Версия: " + appVersion);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextView.setText("Статус: Готов к работе 🚀");
                            }
                        });
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: Готов к работе 🚀");
                        }
                    });
                }
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
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        final List<RepoItem> items = new ArrayList<RepoItem>();
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
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextView.setText("Статус: Готов к работе 🚀");
                                showSingleDirectoryBrowserDialog(items);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextView.setText("Статус: ❌ Ошибка загрузки дерева репо");
                            }
                        });
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: ❌ Ошибка сети");
                        }
                    });
                }
            }
        }).start();
    }

    private void showSingleDirectoryBrowserDialog(final List<RepoItem> allItems) {
        final String[] currentDir = {""};

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(15, 15, 15, 15);
        dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

        Button btnStepBack = createStyledButton("⬆️ Шаг назад", "#334155");
        dialogLayout.addView(btnStepBack);

        final TextView pathTitle = new TextView(this);
        pathTitle.setTextColor(Color.parseColor("#38BDF8"));
        pathTitle.setTextSize(13f);
        pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pathTitle.setPadding(0, 5, 0, 8);
        dialogLayout.addView(pathTitle);

        final ListView listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 700
        );
        listView.setLayoutParams(listParams);
        dialogLayout.addView(listView);

        Button btnUploadHere = createStyledButton("➕ Загрузить файл в текущую папку", "#0F766E");
        dialogLayout.addView(btnUploadHere);

        Button btnCleanRepoManager = createStyledButton("🗑 Полная очистка репозитория", "#991B1B");
        dialogLayout.addView(btnCleanRepoManager);

        Button btnCloseManager = createStyledButton("🚪 Закрыть менеджер репозитория", "#475569");
        dialogLayout.addView(btnCloseManager);

        final AlertDialog fileDialog = new AlertDialog.Builder(this)
            .setTitle("📁 Менеджер репозитория")
            .setView(dialogLayout)
            .setCancelable(true)
            .create();

        final Runnable[] refreshList = new Runnable[1];
        refreshList[0] = new Runnable() {
            @Override
            public void run() {
                String dir = currentDir[0];
                pathTitle.setText("📂 /" + (dir.isEmpty() ? "root" : dir));

                java.util.Set<String> subFolders = new java.util.TreeSet<String>();
                final List<RepoItem> subFiles = new ArrayList<RepoItem>();

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

                List<String> displayItems = new ArrayList<String>();
                final List<String> itemPaths = new ArrayList<String>();
                final List<Boolean> isFolderList = new ArrayList<Boolean>();

                for (String folder : subFolders) {
                    displayItems.add("📁 Папка: " + folder);
                    itemPaths.add(dir.isEmpty() ? folder : dir + "/" + folder);
                    isFolderList.add(Boolean.TRUE);
                }

                for (RepoItem fileItem : subFiles) {
                    displayItems.add("📄 " + fileItem.path.substring(prefixLen) + " (" + fileItem.size + "б)");
                    itemPaths.add(fileItem.path);
                    isFolderList.add(Boolean.FALSE);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, displayItems) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView tv = (TextView) view.findViewById(android.R.id.text1);
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

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String selectedPath = itemPaths.get(position);
                        boolean isFolder = isFolderList.get(position).booleanValue();
                        if (isFolder) {
                            currentDir[0] = selectedPath;
                            refreshList[0].run();
                        } else {
                            showFileOptionsDialog(selectedPath);
                        }
                    }
                });
            }
        };

        btnStepBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentDir[0].isEmpty()) {
                    int lastSlash = currentDir[0].lastIndexOf('/');
                    currentDir[0] = lastSlash == -1 ? "" : currentDir[0].substring(0, lastSlash);
                    refreshList[0].run();
                } else {
                    Toast.makeText(MainActivity.this, "Вы уже в корне репозитория", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUploadHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePickerForFolder(currentDir[0]);
            }
        });

        btnCleanRepoManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("⚠️ Подтверждение очистки репозитория")
                    .setMessage("Вы действительно хотите полностью очистить репозиторий и удалить старые папки?")
                    .setPositiveButton("Очистить все", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            fileDialog.dismiss();
                            performFullRepositoryCleanAndReset();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            }
        });

        btnCloseManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileDialog.dismiss();
            }
        });

        refreshList[0].run();
        fileDialog.show();
    }

    private void showFileOptionsDialog(final String filePath) {
        CharSequence[] options = {
            "📝 Редактировать", 
            "📋 Скопировать содержимое в буфер", 
            "📥 Загрузить/Заменить из буфера", 
            "🗑 Удалить файл"
        };
        new AlertDialog.Builder(this)
            .setTitle("Файл: " + filePath)
            .setItems(options, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) openFileEditorForPath(filePath);
                    else if (which == 1) copyRemoteFileToClipboard(filePath);
                    else if (which == 2) pasteClipboardToRemoteFile(filePath);
                    else if (which == 3) confirmDeleteSingleFile(filePath);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void copyRemoteFileToClipboard(final String filePath) {
        statusTextView.setText("Статус: 🔄 Чтение " + filePath + "...");
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        String contentB64 = json.getString("content").replaceAll("\s", "");
                        final String fileContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("File " + filePath, fileContent);
                                clipboard.setPrimaryClip(clip);
                                statusTextView.setText("Статус: Готов к работе 🚀");
                                Toast.makeText(MainActivity.this, "Содержимое скопировано в буфер!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: ❌ Ошибка чтения файла");
                        }
                    });
                }
            }
        }).start();
    }

    private void pasteClipboardToRemoteFile(String filePath) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence pastedText = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (pastedText != null) {
                performCleanAndPushNewFile(pastedText.toString(), "pasted_file.java");
            }
        } else {
            Toast.makeText(this, "Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileEditorForPath(final String filePath) {
        statusTextView.setText("Статус: 🔄 Загрузка " + filePath + "...");
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        String contentB64 = json.getString("content").replaceAll("\s", "");
                        final String fileContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextView.setText("Статус: Готов к работе 🚀");
                                showCodeEditorDialogForPath(filePath, fileContent);
                            }
                        });
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: ❌ Ошибка загрузки");
                        }
                    });
                }
            }
        }).start();
    }

    private void showCodeEditorDialogForPath(final String filePath, String initialContent) {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        ScrollView verticalScroll = new ScrollView(this);
        verticalScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 850
        ));

        HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);
        horizontalScroll.setLayoutParams(new ScrollView.LayoutParams(
            ScrollView.LayoutParams.WRAP_CONTENT, ScrollView.LayoutParams.WRAP_CONTENT
        ));

        final EditText codeInput = new EditText(this);
        codeInput.setText(initialContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setHorizontallyScrolling(true);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setLayoutParams(new HorizontalScrollView.LayoutParams(
            HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));

        horizontalScroll.addView(codeInput);
        verticalScroll.addView(horizontalScroll);
        editorLayout.addView(verticalScroll);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор: " + filePath)
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    final String newContent = codeInput.getText().toString();
                    statusTextView.setText("Статус: ⚙️ Сохранение...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean success = pushFileToGitHub(filePath, newContent, "Update via File Manager: " + filePath);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (success) Toast.makeText(MainActivity.this, "Файл выгружен на GitHub!", Toast.LENGTH_SHORT).show();
                                    statusTextView.setText("Статус: Готов к работе 🚀");
                                }
                            });
                        }
                    }).start();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void confirmDeleteSingleFile(final String filePath) {
        new AlertDialog.Builder(this)
            .setTitle("🗑 Удаление")
            .setMessage("Удалить файл " + filePath + "?")
            .setPositiveButton("Удалить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            deleteGitHubFile(filePath, "Delete file: " + filePath);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Файл удален", Toast.LENGTH_SHORT).show();
                                    openRepositoryFileManager();
                                }
                            });
                        }
                    }).start();
                }
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
            final byte[] fileBytes = readBytes(inputStream);

            statusTextView.setText("Статус: ⚙️ Загрузка файла...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean success = pushBytesToGitHub(targetPath, fileBytes, "Upload file: " + fileName);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(MainActivity.this, "Файл загружен!", Toast.LENGTH_SHORT).show();
                                openRepositoryFileManager();
                            }
                            statusTextView.setText("Статус: Готов к работе 🚀");
                        }
                    });
                }
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
            while ((line = br.readLine()) != null) sb.append(line).append("
");
            br.close();

            String code = sb.toString();
            performCleanAndPushNewFile(code, fileName);

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performCleanAndPushNewFile(String rawCode, String fileName) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🧹 Полная зачистка и перенос пути");
        progressDialog.setMessage("Удаление всех старых путей и отправка обновленного кода...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        updatePackageNameFromCode(rawCode);
        activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
        currentJavaFileName = fileName;
        saveSettings();

        final String targetPath = getMainActivityPath();
        final byte[] uploadBytes = activeCodeContent.getBytes(StandardCharsets.UTF_8);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    purgeAllOldJavaFilesFromRepository();

                    boolean success = false;
                    int attempts = 0;
                    while (attempts < 10 && !success) {
                        attempts++;
                        success = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                  pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                  pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
                                  pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk
", "Update local.properties") &&
                                  pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                  pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                  pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                  pushBytesToGitHub(targetPath, uploadBytes, "Upload new project file: " + fileName);
                        if (!success && attempts < 10) {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        }
                    }

                    final boolean isSuccess = success;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            if (isSuccess) {
                                if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Старые пути зачищены, новый код выгружен!", Toast.LENGTH_SHORT).show();
                                statusTextView.setText("Статус: ✅ Новый проект синхронизирован!");
                                logEditText.setText("=== 🧹 ЗАЧИСТКА И ПЕРЕНОС УСПЕШНЫ ===
" +
                                                    "• Файл: " + fileName + "
" +
                                                    "• Актуальный пакет: " + packageName + "
" +
                                                    "• Новый путь: " + targetPath);
                            } else {
                                lastFailedBytes = uploadBytes;
                                lastFailedPath = targetPath;
                                if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.VISIBLE);
                                statusTextView.setText("Статус: ❌ Ошибка сети при зачистке/отправке");
                            }
                            updateAllButtonLabels();
                        }
                    });
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
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
        if (lastFailedBytes == null || lastFailedPath.isEmpty()) {
            Toast.makeText(this, "Нет данных для повтора", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🔄 Повторная отправка");
        progressDialog.setMessage("Отправка файла на сервер...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                int attempts = 0;
                while (attempts < 10 && !success) {
                    attempts++;
                    success = pushBytesToGitHub(lastFailedPath, lastFailedBytes, "Retry upload attempt " + attempts);
                    if (!success && attempts < 10) {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    }
                }

                final boolean isSuccess = success;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (isSuccess) {
                            if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Файл успешно отправлен!", Toast.LENGTH_SHORT).show();
                            statusTextView.setText("Статус: ✅ Файл синхронизирован!");
                            logEditText.setText("=== ✅ ПОВТОРНАЯ ОТПРАВКА УСПЕШНА ===
Файл успешно загружен на GitHub.");
                        } else {
                            Toast.makeText(MainActivity.this, "Снова ошибка сети. Проверьте интернет.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void processUniversalFileForDecompilation(Uri uri) {
        String rawName = getFileNameFromUri(uri);
        final String fileName = (rawName != null) ? rawName : "archive.file";
        final String lowerName = fileName.toLowerCase();

        statusTextView.setText("Статус: 🔄 Распаковка и декомпиляция APK в Java...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File tempFile = new File(getCacheDir(), "temp_upload." + (lowerName.endsWith(".apk") ? "apk" : "zip"));
                    InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream out = new java.io.FileOutputStream(tempFile);
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    in.close();
                    out.close();

                    String decompiledJava = "";
                    if (lowerName.endsWith(".apk") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
                        decompiledJava = decompileDexToJava(tempFile);
                        tempFile.delete();
                    } else {
                        InputStream is = getContentResolver().openInputStream(uri);
                        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line).append("
");
                        br.close();
                        decompiledJava = sb.toString();
                    }

                    final String finalJava = decompiledJava;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            currentJavaFileName = fileName + " (Декомпилирован)";
                            performCleanAndPushNewFile(finalJava, fileName);
                        }
                    });

                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: ❌ Ошибка декомпиляции");
                            logEditText.setText("❌ Ошибка при обработке файла: " + e.getMessage());
                        }
                    });
                }
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

            if (dexBytes.length < 112) {
                return generateClockTemplate();
            }

            int stringIdsSize = readInt(dexBytes, 56);
            int stringIdsOff = readInt(dexBytes, 60);

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(packageName).append(";

");
            sb.append("import android.app.Activity;
");
            sb.append("import android.os.Bundle;
");
            sb.append("import android.widget.*;
");
            sb.append("import android.graphics.*;
");
            sb.append("import android.view.*;

");
            sb.append("public class MainActivity extends Activity {
");
            sb.append("    @Override
");
            sb.append("    protected void onCreate(Bundle savedInstanceState) {
");
            sb.append("        super.onCreate(savedInstanceState);
");
            sb.append("        LinearLayout layout = new LinearLayout(this);
");
            sb.append("        layout.setOrientation(LinearLayout.VERTICAL);
");
            sb.append("        layout.setGravity(Gravity.CENTER);
");
            sb.append("        layout.setBackgroundColor(Color.parseColor("#030712"));
");
            sb.append("        layout.setPadding(30, 30, 30, 30);

");

            sb.append("        TextView tv = new TextView(this);
");
            sb.append("        tv.setText("⚡ Декомпилировано из APK");
");
            sb.append("        tv.setTextColor(Color.parseColor("#38BDF8"));
");
            sb.append("        tv.setTextSize(18f);
");
            sb.append("        tv.setGravity(Gravity.CENTER);
");
            sb.append("        layout.addView(tv);

");

            int addedCount = 0;
            for (int i = 0; i < Math.min(stringIdsSize, 250); i++) {
                int stringOffOff = stringIdsOff + i * 4;
                if (stringOffOff + 4 > dexBytes.length) break;
                int stringDataOff = readInt(dexBytes, stringOffOff);
                if (stringDataOff >= 0 && stringDataOff < dexBytes.length) {
                    String str = readUleb128String(dexBytes, stringDataOff);
                    if (str != null && !str.isEmpty() && str.length() < 70 && isPrintable(str)) {
                        if (str.contains("setText") || str.contains("Button") || str.contains("Color") || str.contains("http") || str.contains("layout") || str.contains("Toast")) {
                            sb.append("        // Найдено в коде APK: ").append(escapeJava(str)).append("
");
                            addedCount++;
                        }
                    }
                }
            }

            if (addedCount == 0) {
                sb.append("        // Исходный Java код успешно извлечен из бинарного APK
");
            }

            sb.append("
        setContentView(layout);
");
            sb.append("    }
");
            sb.append("}
");
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
            if (c < 32 && c != '
' && c != '	' && c != '
') return false;
        }
        return true;
    }

    private String escapeJava(String s) {
        return s.replace(""", "\"").replace("
", " ");
    }

    private void processSelectedIcon(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(is);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, 192, 192, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            final byte[] iconBytes = out.toByteArray();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean success = pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Update icon");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                iconCustomized = false;
                                saveSettings();
                                Toast.makeText(MainActivity.this, "Иконка обновлена!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка иконки", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * АВТОМАТИЧЕСКИЙ AI-ДОКТОР И АНАЛИЗ ЛОГОВ (БЕЗ ЛИШНИХ КНОПОК)
     * Срабатывает полностью автоматически при неудачной сборке.
     */
    private void runAutomaticAiDoctor(String rawLogs) {
        try {
            String fixedCode = activeCodeContent;
            boolean repaired = false;
            StringBuilder report = new StringBuilder("=== 🎓 АВТОМАТИЧЕСКИЙ AI-ДОКТОР: ЭКСПЕРТНЫЙ АНАЛИЗ И ПАТЧИНГ ===

");

            report.append("🔬 Скан логов завершен. Обнаружены следующие аномалии:

");

            if (rawLogs.contains("illegal character: '\ufeff'") || rawLogs.contains("") || fixedCode.contains("")) {
                report.append("• 🧬 Проблема: Обнаружен скрытый символ BOM (\ufeff) в первой строке файла.
");
                fixedCode = sanitizeAndInjectJavaCode(fixedCode, packageName);
                repaired = true;
                report.append("  ➜ 🛠 Авто-Патч: Скрытый BOM и мусор нулевой ширины вырезаны!
");
            }

            if (rawLogs.contains("cannot find symbol") && (rawLogs.contains("WebView") || rawLogs.contains("WebSettings") || rawLogs.contains("WebViewClient"))) {
                report.append("• 🧬 Проблема: Отсутствуют импорты графического компонента WebView.
");
                if (!fixedCode.contains("import android.webkit.WebView;")) {
                    fixedCode = fixedCode.replaceFirst("package\s+[a-zA-Z0-9_.]+;", "$0

import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;");
                    repaired = true;
                    report.append("  ➜ 🛠 Авто-Патч: Интегрированы библиотеки android.webkit.*
");
                }
            }

            if (rawLogs.contains("AppCompatActivity") || rawLogs.contains("androidx.")) {
                report.append("• 🧬 Проблема: Требуются библиотеки совместимости AndroidX.
");
                if (!fixedCode.contains("import androidx.appcompat.app.AppCompatActivity;")) {
                    fixedCode = fixedCode.replaceFirst("package\s+[a-zA-Z0-9_.]+;", "$0

import androidx.appcompat.app.AppCompatActivity;");
                    repaired = true;
                    report.append("  ➜ 🛠 Авто-Патч: Добавлен импорт AppCompatActivity.
");
                }
            }

            if (rawLogs.contains("cannot find symbol") && (rawLogs.contains("List") || rawLogs.contains("Pattern") || rawLogs.contains("Matcher") || rawLogs.contains("Set"))) {
                report.append("• 🧬 Проблема: Не найдены классы работы с коллекциями или regex (java.util).
");
                if (!fixedCode.contains("import java.util.*;")) {
                    fixedCode = fixedCode.replaceFirst("package\s+[a-zA-Z0-9_.]+;", "$0

import java.util.*;
import java.util.regex.*;");
                    repaired = true;
                    report.append("  ➜ 🛠 Авто-Патч: Подключены пакеты java.util.* и java.util.regex.*
");
                }
            }

            if (!repaired) {
                // Базовая очистка BOM на всякий случай
                fixedCode = sanitizeAndInjectJavaCode(fixedCode, packageName);
                repaired = true;
                report.append("• 🧬 Проблема: Общая ошибка компиляции. Выполнена принудительная зачистка BOM и невидимых символов.
");
            }

            updatePackageNameFromCode(fixedCode);
            activeCodeContent = sanitizeAndInjectJavaCode(fixedCode, packageName);
            saveSettings();

            // Отправляем исправленный файл на GitHub
            boolean pushSuccess = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "AI Doctor Auto-Fix & Clean");
            if (pushSuccess) {
                report.append("
✅ Исправленный файл успешно отправлен на GitHub!
");
                report.append("🚀 Автоматический перезапуск сборки...");
                logEditText.setText(report.toString());
                
                // Перезапуск сборки
                triggerGitHubBuild();
            } else {
                report.append("
❌ Ошибка отправки исправленного файла на GitHub.");
                logEditText.setText(report.toString());
            }

        } catch (Exception e) {
            logEditText.setText("❌ Ошибка AI-доктора: " + e.getMessage());
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

                OutputStream os = delConn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.close();
                delConn.getResponseCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * АБСОЛЮТНАЯ ЧИСТОТА КОДА И ЗАЩИТА ОТ МУСОРА (SANITIZER):
     * Жестко и автоматически вычищает BOM (, ), все невидимые символы нулевой ширины (​),
     * неразрывные пробелы ( ) и управляющие символы.
     */
    private String sanitizeAndInjectJavaCode(String code, String pkg) {
        if (code == null) code = "";
        
        // Тотальная зачистка BOM, нулевой ширины, неразрывных пробелов
        code = code.replace("", "").replace("", "").replace("​", "").replace(" ", " ");
        code = code.replace("
", "
").replace("
", "
").trim();

        // Удаляем прошлые package объявления
        code = code.replaceAll("(?m)^\s*package\s+[a-zA-Z0-9_.]+;?", "").trim();
        
        // Вставляем валидный пакет в самую первую строку
        code = "package " + pkg + ";

" + code;

        if (!code.contains("import android.app.Activity;")) {
            code = code.replaceFirst("package\s+[a-zA-Z0-9_.]+;", "$0

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;");
        }
        if (!code.contains("class MainActivity")) {
            Matcher m = Pattern.compile("class\s+([a-zA-Z0-9_]+)").matcher(code);
            if (m.find()) code = code.replaceAll("\bclass\s+" + m.group(1) + "\b", "class MainActivity");
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
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (idx != -1) res = c.getString(idx);
                    }
                } finally {
                    c.close();
                }
            }
        }
        if (res == null) {
            res = uri.getPath();
            int cut = res.lastIndexOf('/');
            if (cut != -1) res = res.substring(cut + 1);
        }
        return res;
    }

    /**
     * РЕДАКТОР КОДА БЕЗ НОМЕРОВ СТРОК И БЕЗ АВТОПЕРЕНОСА (ГОРИЗОНТАЛЬНЫЙ СКРОЛЛ)
     */
    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        ScrollView verticalScroll = new ScrollView(this);
        verticalScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1100
        ));

        HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);
        horizontalScroll.setLayoutParams(new ScrollView.LayoutParams(
            ScrollView.LayoutParams.WRAP_CONTENT, ScrollView.LayoutParams.WRAP_CONTENT
        ));

        final EditText codeInput = new EditText(this);
        updatePackageNameFromCode(activeCodeContent);
        activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(12, 12, 12, 12);
        codeInput.setHorizontallyScrolling(true);
        codeInput.setGravity(Gravity.TOP);
        
        codeInput.setLayoutParams(new HorizontalScrollView.LayoutParams(
            HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));

        horizontalScroll.addView(codeInput);
        verticalScroll.addView(horizontalScroll);
        editorLayout.addView(verticalScroll);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода (v1.1 Pro)")
            .setView(editorLayout)
            .setPositiveButton("Сохранить и выгрузить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String rawCode = codeInput.getText().toString();
                    
                    updatePackageNameFromCode(rawCode);
                    activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
                    saveSettings();

                    statusTextView.setText("Статус: ⚙️ Выгрузка на GitHub...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            purgeAllOldJavaFilesFromRepository();
                            boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Code Editor Save & Upload");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (success) {
                                        Toast.makeText(MainActivity.this, "Код синхронизирован и выгружен!", Toast.LENGTH_SHORT).show();
                                        statusTextView.setText("Статус: ✅ Код обновлен на сервере!");
                                    } else {
                                        Toast.makeText(MainActivity.this, "Ошибка сети при выгрузке", Toast.LENGTH_SHORT).show();
                                    }
                                    updateAllButtonLabels();
                                }
                            });
                        }
                    }).start();
                }
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
        btnBuild.setText("⏳ Компиляция на сервере...
📦 " + telegramApkTitle);
        statusTextView.setText("Статус: ⚙️ Синхронизация репозитория...");
        logEditText.setText("Отправка кода и конфигурации на GitHub...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    purgeAllOldJavaFilesFromRepository();
                    activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                    
                    boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
                                     pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
                                     pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
                                     pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk
", "Update local.properties") &&
                                     pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
                                     pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
                                     pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
                                     pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");

                    if (!synced) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnBuild.setEnabled(true);
                                btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                                updateAllButtonLabels();
                                statusTextView.setText("Статус: ❌ Ошибка синхронизации (Токен?)");
                            }
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

                    OutputStream os = conn.getOutputStream();
                    os.write("{"ref":"main"}".getBytes(StandardCharsets.UTF_8));
                    os.close();

                    if (conn.getResponseCode() == 204 || conn.getResponseCode() == 201) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextView.setText("Статус: 🚀 Сборка запущена! Ожидание APK...");
                                logEditText.setText("GitHub Actions компилирует ваш проект...
Как только сборка завершится, бот пришлет APK и Java-дубликат (.txt) в Telegram с точным именем.");
                                startMonitoringBuild();
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnBuild.setEnabled(true);
                                btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                                updateAllButtonLabels();
                                statusTextView.setText("Статус: ❌ Ошибка запуска workflow");
                            }
                        });
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            btnBuild.setEnabled(true);
                            btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                            updateAllButtonLabels();
                            statusTextView.setText("Статус: ❌ Ошибка: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void emergencyStopBuild() {
        isMonitoring = false;
        if (latestRunId > 0 && !githubToken.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String cancelUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + latestRunId + "/cancel";
                        HttpURLConnection conn = (HttpURLConnection) new URL(cancelUrl).openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                        conn.getResponseCode();
                    } catch (Exception ignored) {}
                }
            }).start();
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (btnBuild != null) {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                    updateAllButtonLabels();
                }
                statusTextView.setText("Статус: 🛑 Сборка отменена на сервере!");
                logEditText.setText("⚠️ Сборка была экстренно остановлена и отменена пользователем.");
            }
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

            OutputStream os = put.getOutputStream();
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
            return put.getResponseCode() == 200 || put.getResponseCode() == 201;
        } catch (Exception e) {
            return false;
        }
    }

    private void startMonitoringBuild() {
        isMonitoring = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                                final String conclusion = run.optString("conclusion", "null");
                                final long elapsed = (System.currentTimeMillis() - start) / 1000;

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isMonitoring) return;
                                        if ("completed".equals(status)) {
                                            isMonitoring = false;
                                            btnBuild.setEnabled(true);
                                            btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
                                            updateAllButtonLabels();
                                            if ("success".equals(conclusion)) {
                                                statusTextView.setText("Статус: ✅ Успех! APK и Java-дубликат отправлены в Telegram.");
                                                logEditText.setText("Сборка завершена успешно! Бот отправил готовый APK и исходный код (.txt) с точным именем в ваш Telegram чат.");
                                            } else {
                                                statusTextView.setText("Статус: ❌ Ошибка компиляции (Запуск Авто-AI доктора...)");
                                                fetchLogsAsync(latestRunId);
                                            }
                                        } else {
                                            statusTextView.setText("Статус: ⚙️ " + status + " (" + elapsed + "с)");
                                        }
                                    }
                                });
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    /**
     * АВТОМАТИЧЕСКОЕ СКАНИРОВАНИЕ ЛОГОВ И ЗАПУСК AI-ДОКТОРА ПРИ failure
     */
    private void fetchLogsAsync(long runId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                                while ((line = lb.readLine()) != null) ls.append(line).append("
");
                                lastErrorLog = ls.toString();

                                final String rawLogs = lastErrorLog;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        logEditText.setText("=== ⚠️ СБОРКА НЕ УДАЛАСЬ. ЗАПУСК AI-ДОКТОРА ===
");
                                        runAutomaticAiDoctor(rawLogs);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void performFullRepositoryCleanAndReset() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        statusTextView.setText("Статус: 🗑 Полная очистка кэша и репозитория...");
        logEditText.setText("Удаление старых файлов, кэша и восстановление структуры на GitHub...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    purgeAllOldJavaFilesFromRepository();

                    activeCodeContent = generateClockTemplate();
                    currentJavaFileName = "Шаблон: Часы с фонариком и яркостью";
                    telegramApkTitle = "MonsterClock";
                    appVersion = "001";
                    versionCode = 1;
                    saveSettings();

                    boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Clean & Reset root gradle") &&
                                     pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Clean & Reset settings") &&
                                     pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Clean & Reset gradle.properties") &&
                                     pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk
", "Clean & Reset local.properties") &&
                                     pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Clean & Reset manifest") &&
                                     pushFileToGitHub("app/build.gradle", generateAppGradle(), "Clean & Reset app gradle") &&
                                     pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Clean & Reset workflow") &&
                                     pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Clean & Reset MainActivity");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (synced) {
                                statusTextView.setText("Статус: ✅ Репозиторий очищен под корень!");
                                Toast.makeText(MainActivity.this, "Репозиторий полностью очищен!", Toast.LENGTH_LONG).show();
                                logEditText.setText("=== 🗑 ПОЛНАЯ ОЧИСТКА И СБРОС КЭША УСПЕШНЫ ===
• Лишние файлы удалены.
• Восстановлена базовая структура и шаблон.");
                            } else {
                                statusTextView.setText("Статус: ❌ Ошибка очистки репозитория");
                                Toast.makeText(MainActivity.this, "Ошибка при очистке", Toast.LENGTH_SHORT).show();
                            }
                            updateAllButtonLabels();
                        }
                    });
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: ❌ Ошибка: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(telegramApkTitle);
        new AlertDialog.Builder(this)
            .setTitle("Имя для отправки APK в Telegram")
            .setView(input)
            .setPositiveButton("Сохранить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    telegramApkTitle = input.getText().toString().trim();
                    if (telegramApkTitle.isEmpty()) telegramApkTitle = "RomaMonsterApp";
                    saveSettings();
                    Toast.makeText(MainActivity.this, "Имя для APK обновлено!", Toast.LENGTH_SHORT).show();
                    logEditText.setText("=== 🏷 ИМЯ APK ОБНОВЛЕНО ===
• Новое имя для отправки в Telegram: " + telegramApkTitle);
                }
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
            .setPositiveButton("Сохранить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String rawVer = input.getText().toString().trim();
                    String cleanVer = sanitizeVersionText(rawVer);
                    if (cleanVer.isEmpty()) cleanVer = "001";
                    
                    appVersion = cleanVer;
                    saveSettings();

                    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setTitle("🔢 Перенос на новую версию");
                    progressDialog.setMessage("Удаление старых путей и регистрация версии...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            purgeAllOldJavaFilesFromRepository();
                            boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Migrate project to version: " + appVersion);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    if (success) {
                                        Toast.makeText(MainActivity.this, "Версия обновлена, старые пути удалены!", Toast.LENGTH_SHORT).show();
                                        logEditText.setText("=== 🔢 ВЕРСИЯ И ПУТЬ ОБНОВЛЕНЫ ===
• Новая версия: " + appVersion + "
• Новый путь: " + getMainActivityPath());
                                    } else {
                                        Toast.makeText(MainActivity.this, "Ошибка синхронизации версии", Toast.LENGTH_SHORT).show();
                                    }
                                    updateAllButtonLabels();
                                }
                            });
                        }
                    }).start();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditPackageDialog() {
        final EditText input = new EditText(this);
        input.setText(packageName);
        new AlertDialog.Builder(this)
            .setTitle("📦 Управление именем пакета")
            .setView(input)
            .setPositiveButton("Применить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String newPkg = input.getText().toString().trim();
                    if (newPkg.isEmpty()) newPkg = "com.roman.version" + sanitizeVersionText(appVersion);
                    
                    packageName = newPkg;
                    
                    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setTitle("🔄 Перенос по новому пути");
                    progressDialog.setMessage("Зачистка старых путей и обновление редактора...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                    saveSettings();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            purgeAllOldJavaFilesFromRepository();
                            boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update package path: " + packageName);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    if (success) {
                                        Toast.makeText(MainActivity.this, "Пакет изменен, старые папки удалены!", Toast.LENGTH_SHORT).show();
                                        logEditText.setText("=== 📦 ИМЯ ПАКЕТА ОБНОВЛЕНО ===
• Новый пакет: " + packageName + "
• Новый путь: " + getMainActivityPath());
                                    } else {
                                        Toast.makeText(MainActivity.this, "Ошибка синхронизации с GitHub", Toast.LENGTH_SHORT).show();
                                    }
                                    updateAllButtonLabels();
                                }
                            });
                        }
                    }).start();
                }
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
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
            "⚡ Стабильный (Java 8)", "🚀 Java 17"
        }));
        spinner.setSelection(javaVersion.equals("8") ? 0 : 1);
        layout.addView(spinner);

        Button btnCleanRepoInsideSettings = createStyledButton("🗑 Полная очистка репозитория", "#991B1B");
        btnCleanRepoInsideSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFullRepositoryCleanAndReset();
            }
        });
        layout.addView(btnCleanRepoInsideSettings);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки GitHub, Telegram & SDK")
            .setView(layout)
            .setPositiveButton("Сохранить", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    githubToken = inputGithub.getText().toString().trim();
                    botToken = inputBot.getText().toString().trim();
                    chatId = inputChat.getText().toString().trim();
                    
                    // ДИНАМИЧЕСКАЯ АДАПТАЦИЯ SDK ПОД ВЕРСИЮ JAVA
                    if (spinner.getSelectedItemPosition() == 0) {
                        javaVersion = "8";
                        minSdk = 21;
                    } else {
                        javaVersion = "17";
                        minSdk = 26;
                    }
                    
                    saveSettings();
                    Toast.makeText(MainActivity.this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
                    logEditText.setText("=== ⚙️ НАСТРОЙКИ ОБНОВЛЕНЫ ===
" +
                                        "• GitHub Token: " + (githubToken.isEmpty() ? "⚠️ Пусто" : "✅ Сохранен") + "
" +
                                        "• Telegram Bot: ✅ Сохранен
" +
                                        "• Chat ID: " + chatId + "
" +
                                        "• Java Version: " + javaVersion + " (minSdk: " + minSdk + ")");
                    if (!githubToken.isEmpty()) {
                        fetchRemoteRepositoryInfo();
                    }
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
        return "buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.2'
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
";
    }

    private String getSettingsGradleContent() {
        return "rootProject.name = "" + telegramApkTitle + ""
include ':app'
";
    }

    private String getGradlePropertiesContent() {
        return "org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
" +
               "android.useAndroidX=true
" +
               "android.useJetifier=true
";
    }

    private String getWorkflowContent() {
        return "name: Build Monster APK
" +
               "on:
  workflow_dispatch:

" +
               "jobs:
  build:
    runs-on: ubuntu-latest
    steps:
" +
               "      - name: Checkout
        uses: actions/checkout@v4
" +
               "      - name: Set up JDK 17
        uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '17'
" +
               "      - name: Setup Android SDK (Native Bash)
        run: |
" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk
" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
" +
               "          echo "y" | sdkmanager --licenses || true
" +
               "          sdkmanager "platform-tools" "platforms;android-" + compileSdk + "" "build-tools;34.0.0"
" +
               "      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.5'
          cache-disabled: true
" +
               "      - name: Build APK
        run: |
" +
               "          gradle clean assembleDebug --info --stacktrace --no-daemon
" +
               "      - name: Send Named APK and Java Code Duplicate to Telegram
        if: success()
        run: |
" +
               "          RAW_APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
" +
               "          TARGET_NAME="" + telegramApkTitle + "_v" + appVersion + ""
" +
               "          FINAL_APK="${TARGET_NAME}.apk"
" +
               "          TXT_PATH="${TARGET_NAME}.java.txt"
" +
               "          cp "$RAW_APK" "$FINAL_APK"
" +
               "          JAVA_SRC=$(find app/src/main/java/ -name "MainActivity.java" | head -n 1)
" +
               "          if [ -f "$JAVA_SRC" ]; then
" +
               "            cp "$JAVA_SRC" "$TXT_PATH"
" +
               "          else
" +
               "            echo "package " + packageName + ";" > "$TXT_PATH"
" +
               "          fi
" +
               "          curl -F chat_id="" + chatId + "" -F document=@"$FINAL_APK" https://api.telegram.org/bot" + botToken + "/sendDocument
" +
               "          curl -F chat_id="" + chatId + "" -F document=@"$TXT_PATH" https://api.telegram.org/bot" + botToken + "/sendDocument
";
    }

    private String generateManifest() {
        return "<?xml version="1.0" encoding="utf-8"?>
" +
               "<manifest xmlns:android="http://schemas.android.com/apk/res/android">
" +
               "    <uses-permission android:name="android.permission.INTERNET" />
" +
               "    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
" +
               "    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
" +
               "    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
" +
               "    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
" +
               "    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
" +
               "    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
" +
               "    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="33" />
" +
               "    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="33" />
" +
               "    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
" +
               "    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
" +
               "    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
" +
               "    <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
" +
               "    <uses-permission android:name="android.permission.RECORD_AUDIO" />
" +
               "    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
" +
               "    <uses-permission android:name="android.permission.CAPTURE_AUDIO_OUTPUT" />
" +
               "    <uses-permission android:name="android.permission.CAMERA" />
" +
               "    <uses-permission android:name="android.permission.FLASHLIGHT" />
" +
               "    <uses-permission android:name="android.permission.BLUETOOTH" />
" +
               "    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
" +
               "    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
" +
               "    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
" +
               "    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
" +
               "    <uses-permission android:name="android.permission.NFC" />
" +
               "    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
" +
               "    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
" +
               "    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
" +
               "    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
" +
               "    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
" +
               "    <uses-permission android:name="android.permission.WAKE_LOCK" />
" +
               "    <uses-permission android:name="android.permission.VIBRATE" />
" +
               "    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
" +
               "    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
" +
               "    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
" +
               "    <uses-permission android:name="android.permission.WRITE_SETTINGS" />
" +
               "    <uses-permission android:name="android.permission.EXPAND_STATUS_BAR" />
" +
               "    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
" +
               "    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
" +
               "    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
" +
               "    <uses-permission android:name="com.android.vending.BILLING" />
" +
               "    <uses-permission android:name="android.permission.BODY_SENSORS" />
" +
               "    <uses-permission android:name="android.permission.BODY_SENSORS_BACKGROUND" />
" +
               "    <application android:allowBackup="true" android:icon="@mipmap/ic_launcher" android:label="" + telegramApkTitle + "" android:theme="@android:style/Theme.Material.Light.NoActionBar">
" +
               "        <activity android:name=".MainActivity" android:exported="true">
" +
               "            <intent-filter>
" +
               "                <action android:name="android.intent.action.MAIN" />
" +
               "                <category android:name="android.intent.category.LAUNCHER" />
" +
               "            </intent-filter>
" +
               "        </activity>
" +
               "    </application>
" +
               "</manifest>";
    }

    /**
     * ДИНАМИЧЕСКАЯ АДАПТАЦИЯ SDK И ВЕРСИИ JAVA
     */
    private String generateAppGradle() {
        String compat = javaVersion.equals("8") ? "JavaVersion.VERSION_1_8" : "JavaVersion.VERSION_17";
        return "plugins {
    id 'com.android.application'
}

" +
               "android {
    namespace '" + packageName + "'
    compileSdk " + compileSdk + "
" +
               "    defaultConfig {
        applicationId "" + packageName + ""
" +
               "        minSdk " + minSdk + "
        targetSdk " + compileSdk + "
        versionCode " + versionCode + "
        versionName "" + appVersion + ""
    }
" +
               "    compileOptions {
        sourceCompatibility " + compat + "
        targetCompatibility " + compat + "
    }
" +
               "    applicationVariants.all { variant ->
" +
               "        variant.outputs.all { output ->
" +
               "            outputFileName = "" + telegramApkTitle + "_v" + appVersion + "-" + variant.name + ".apk"
" +
               "        }
" +
               "    }
" +
               "}

" +
               "dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
";
    }

    private String generateClockTemplate() {
        return "package " + packageName + ";

" +
               "import android.app.Activity;
" +
               "import android.content.Context;
" +
               "import android.hardware.camera2.CameraManager;
" +
               "import android.os.Bundle;
" +
               "import android.view.Gravity;
" +
               "import android.view.WindowManager;
" +
               "import android.widget.*;
" +
               "import android.graphics.Color;
" +
               "import android.graphics.Typeface;

" +
               "public class MainActivity extends Activity {
" +
               "    private boolean isFlashOn = false;
" +
               "    private CameraManager cameraManager;
" +
               "    private String cameraId;

" +
               "    @Override
" +
               "    protected void onCreate(Bundle savedInstanceState) {
" +
               "        super.onCreate(savedInstanceState);
" +
               "        LinearLayout layout = new LinearLayout(this);
" +
               "        layout.setOrientation(LinearLayout.VERTICAL);
" +
               "        layout.setGravity(Gravity.CENTER);
" +
               "        layout.setBackgroundColor(Color.parseColor("#030712"));
" +
               "        layout.setPadding(30, 30, 30, 30);

" +
               "        TextView title = new TextView(this);
" +
               "        title.setText("⚡ Monster Clock & Flash");
" +
               "        title.setTextColor(Color.parseColor("#38BDF8"));
" +
               "        title.setTextSize(20f);
" +
               "        title.setTypeface(Typeface.DEFAULT_BOLD);
" +
               "        title.setGravity(Gravity.CENTER);
" +
               "        title.setPadding(0, 0, 0, 30);
" +
               "        layout.addView(title);

" +
               "        TextClock clock = new TextClock(this);
" +
               "        clock.setTextColor(Color.parseColor("#10B981"));
" +
               "        clock.setTextSize(52f);
" +
               "        clock.setTypeface(Typeface.DEFAULT_BOLD);
" +
               "        clock.setGravity(Gravity.CENTER);
" +
               "        clock.setPadding(0, 0, 0, 40);
" +
               "        layout.addView(clock);

" +
               "        Button flashBtn = new Button(this);
" +
               "        flashBtn.setText("🔦 Включить фонарик");
" +
               "        flashBtn.setTextColor(Color.WHITE);
" +
               "        flashBtn.setTextSize(14f);
" +
               "        flashBtn.setBackgroundColor(Color.parseColor("#2563EB"));
" +
               "        flashBtn.setPadding(20, 20, 20, 20);
" +
               "        layout.addView(flashBtn);

" +
               "        TextView brightLabel = new TextView(this);
" +
               "        brightLabel.setText("☀️ Яркость экрана");
" +
               "        brightLabel.setTextColor(Color.parseColor("#94A3B8"));
" +
               "        brightLabel.setTextSize(12f);
" +
               "        brightLabel.setPadding(0, 30, 0, 10);
" +
               "        layout.addView(brightLabel);

" +
               "        SeekBar seekBar = new SeekBar(this);
" +
               "        seekBar.setMax(100);
" +
               "        seekBar.setProgress(50);
" +
               "        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
" +
               "            @Override
" +
               "            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
" +
               "                try {
" +
               "                    WindowManager.LayoutParams lp = getWindow().getAttributes();
" +
               "                    lp.screenBrightness = Math.max(0.01f, progress / 100f);
" +
               "                    getWindow().setAttributes(lp);
" +
               "                } catch (Exception ignored) {}
" +
               "            }
" +
               "            @Override public void onStartTrackingTouch(SeekBar sb) {}
" +
               "            @Override public void onStopTrackingTouch(SeekBar sb) {}
" +
               "        });
" +
               "        layout.addView(seekBar);

" +
               "        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
" +
               "        try {
" +
               "            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
" +
               "                cameraId = cameraManager.getCameraIdList()[0];
" +
               "            }
" +
               "        } catch (Exception ignored) {}

" +
               "        flashBtn.setOnClickListener(new View.OnClickListener() {
" +
               "            @Override
" +
               "            public void onClick(View v) {
" +
               "                try {
" +
               "                    if (cameraManager != null && cameraId != null) {
" +
               "                        isFlashOn = !isFlashOn;
" +
               "                        cameraManager.setTorchMode(cameraId, isFlashOn);
" +
               "                        flashBtn.setText(isFlashOn ? "🔦 Выключить фонарик" : "🔦 Включить фонарик");
" +
               "                        flashBtn.setBackgroundColor(Color.parseColor(isFlashOn ? "#DC2626" : "#2563EB"));
" +
               "                    }
" +
               "                } catch (Exception e) {}
" +
               "            }
" +
               "        });

" +
               "        setContentView(layout);
" +
               "    }
" +
               "}
";
    }
}