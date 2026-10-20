package com.roma.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    // ==================== НАСТРОЙКИ ====================
    private static final String BOT_TOKEN = "REDACTED_TELEGRAM_TOKEN";
    private static final String MY_CHAT_ID = "1434233986";

    private static final String GITHUB_TOKEN = "REDACTED_GITHUB_TOKEN";
    private static final String REPO_OWNER = "RomanDziuba";
    private static final String REPO_NAME = "binance-scanner-app";
    private static final String WORKFLOW_FILE = "build_native.yml";
    private static final String WORKFLOW_PATH = ".github/workflows/" + WORKFLOW_FILE;

    private static final String MANIFEST_PATH = "app/src/main/AndroidManifest.xml";
    private static final String BUILD_GRADLE_APP_PATH = "app/build.gradle";
    private static final String BUILD_GRADLE_ROOT_PATH = "build.gradle";
    private static final String SETTINGS_GRADLE_PATH = "settings.gradle";
    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";
    // ====================================================

    private static final Map<String, Object> projectConfig = new HashMap<>();
    private static final ConcurrentHashMap<String, String> userStates = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    private TextView logView;
    private static MainActivity instance;

    static {
        projectConfig.put("app_title", "RomaClockApp");
        projectConfig.put("package_name", "com.roma.nativeapp");
        projectConfig.put("version", "1.0");
        projectConfig.put("app_mode", "default_clock");
        projectConfig.put("app_language", "java");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        // Создаем графический интерфейс с логами на экране телефона
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ Android Java Compiler Bot");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 20);

        logView = new TextView(this);
        logView.setText("🚀 Инициализация компилятора...");
        logView.setTextColor(Color.parseColor("#10B981"));
        logView.setTextSize(13f);
        logView.setTypeface(Typeface.MONOSPACE);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(logView);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        layout.addView(titleView);
        layout.addView(scroll);
        setContentView(layout);

        // Запуск бота в фоновом потоке, чтобы не вешать интерфейс Android
        threadPool.submit(this::startCompilerBot);
    }

    public static void appendLog(String text) {
        if (instance != null && instance.logView != null) {
            instance.runOnUiThread(() -> {
                String current = instance.logView.getText().toString();
                instance.logView.setText(current + "\n" + text);
            });
        }
    }

    private void startCompilerBot() {
        appendLog("🔍 Проверка репозитория GitHub...");
        ensureRepositoryIntegrity(false);
        appendLog("🤖 Telegram-бот запущен и слушает чат...");

        long offset = 0;
        while (true) {
            try {
                String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + offset + "&timeout=5";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AndroidCompilerApp");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject res = new JSONObject(sb.toString());
                    if (res.optBoolean("ok")) {
                        JSONArray results = res.optJSONArray("result");
                        if (results != null) {
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject update = results.getJSONObject(i);
                                offset = update.getLong("update_id") + 1;
                                threadPool.submit(() -> handleUpdate(update));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ==================== ЛОГИКА РАБОТЫ С GITHUB ====================
    private static boolean pushContentToGithub(String path, byte[] contentBytes, String commitMsg) {
        int attempts = 0;
        while (attempts < 5) {
            attempts++;
            try {
                String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + path;
                HttpURLConnection getConn = (HttpURLConnection) new URL(urlStr).openConnection();
                getConn.setRequestMethod("GET");
                getConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
                getConn.setRequestProperty("User-Agent", "AndroidCompilerApp");
                getConn.setRequestProperty("Accept", "application/vnd.github+json");

                String sha = null;
                if (getConn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(getConn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject json = new JSONObject(sb.toString());
                    sha = json.optString("sha", null);
                }

                JSONObject payload = new JSONObject();
                payload.put("message", commitMsg);
                payload.put("content", Base64.getEncoder().encodeToString(contentBytes));
                payload.put("branch", "main");
                if (sha != null) payload.put("sha", sha);

                HttpURLConnection putConn = (HttpURLConnection) new URL(urlStr).openConnection();
                putConn.setRequestMethod("PUT");
                putConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
                putConn.setRequestProperty("User-Agent", "AndroidCompilerApp");
                putConn.setRequestProperty("Content-Type", "application/json");
                putConn.setDoOutput(true);

                try (OutputStream os = putConn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                if (putConn.getResponseCode() == 200 || putConn.getResponseCode() == 201) {
                    return true;
                }
            } catch (Exception e) {
                appendLog("⚠️ Ошибка GitHub (" + attempts + "): " + e.getMessage());
            }
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    private static void deleteGithubFile(String path, String commitMsg) {
        try {
            String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + path;
            HttpURLConnection getConn = (HttpURLConnection) new URL(urlStr).openConnection();
            getConn.setRequestMethod("GET");
            getConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            getConn.setRequestProperty("User-Agent", "AndroidCompilerApp");

            if (getConn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(getConn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                String sha = json.optString("sha", null);

                JSONObject payload = new JSONObject();
                payload.put("message", commitMsg);
                payload.put("sha", sha);
                payload.put("branch", "main");

                HttpURLConnection delConn = (HttpURLConnection) new URL(urlStr).openConnection();
                delConn.setRequestMethod("DELETE");
                delConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
                delConn.setRequestProperty("User-Agent", "AndroidCompilerApp");
                delConn.setRequestProperty("Content-Type", "application/json");
                delConn.setDoOutput(true);

                try (OutputStream os = delConn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                delConn.getResponseCode();
            }
        } catch (Exception ignored) {}
    }

    private static void removeAllExistingMainActivities() {
        try {
            String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/git/trees/main?recursive=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            conn.setRequestProperty("User-Agent", "AndroidCompilerApp");

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
                        String mainPath = "app/src/main/java/" + ((String)projectConfig.get("package_name")).replace('.', '/') + "/MainActivity.java";
                        if ((path.endsWith("MainActivity.java") || path.endsWith("MainActivity.kt")) && !path.equals(mainPath)) {
                            deleteGithubFile(path, "Cleanup old file: " + path);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean checkRepositoryInitialized() {
        try {
            String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/gradle.properties";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            conn.setRequestProperty("User-Agent", "AndroidCompilerApp");
            return conn.getResponseCode() == 200;
        } catch (Exception ignored) {}
        return false;
    }

    private static void syncConfigToGithub() {
        String workflow = "name: Build Native Android APK (Java)\non:\n  workflow_dispatch:\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n      - run: |\n          export ANDROID_HOME=/usr/local/lib/android/sdk\n          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n          yes | sdkmanager --licenses || true\n          sdkmanager \"platform-tools\" \"platforms;android-34\" \"build-tools;34.0.0\"\n          gradle clean assembleDebug --stacktrace --no-daemon\n      - if: success()\n        run: |\n          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n          if [ -n \"$APK_PATH\" ]; then\n            curl -F chat_id=\"" + MY_CHAT_ID + "\" -F document=@\"$APK_PATH\" https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument\n          fi\n";

        String manifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <uses-permission android:name=\"android.permission.INTERNET\" />\n    <application android:allowBackup=\"true\" android:label=\"" + projectConfig.get("app_title") + "\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n        <activity android:name=\".MainActivity\" android:exported=\"true\">\n            <intent-filter>\n                <action android:name=\"android.intent.action.MAIN\" />\n                <category android:name=\"android.intent.category.LAUNCHER\" />\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>\n";

        String appGradle = "plugins { id 'com.android.application' }\nandroid {\n    namespace '" + projectConfig.get("package_name") + "'\n    compileSdk 34\n    defaultConfig {\n        applicationId \"" + projectConfig.get("package_name") + "\"\n        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName \"" + projectConfig.get("version") + "\"\n    }\n}\ndependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n}\n";

        pushContentToGithub(MANIFEST_PATH, manifest.getBytes(StandardCharsets.UTF_8), "Update Manifest");
        pushContentToGithub(BUILD_GRADLE_APP_PATH, appGradle.getBytes(StandardCharsets.UTF_8), "Update app/build.gradle");
        pushContentToGithub(WORKFLOW_PATH, workflow.getBytes(StandardCharsets.UTF_8), "Update workflow");
    }

    private static void ensureRepositoryIntegrity(boolean force) {
        if (!force && checkRepositoryInitialized()) return;
        deleteGithubFile(".github/workflows/build_godot.yml", "Delete old");
        removeAllExistingMainActivities();
        syncConfigToGithub();
    }

    // ==================== ОБРАБОТКА ТЕЛЕГРАМ ====================
    private static void sendMessage(String chatId, String text) {
        try {
            String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
            JSONObject json = new JSONObject();
            json.put("chat_id", chatId);
            json.put("text", text);
            json.put("parse_mode", "Markdown");

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } catch (Exception ignored) {}
    }

    private static void handleUpdate(JSONObject update) {
        try {
            if (update.has("message")) {
                JSONObject msg = update.getJSONObject("message");
                String chatId = String.valueOf(msg.getJSONObject("chat").getLong("id"));
                if (!chatId.equals(MY_CHAT_ID)) return;

                if (msg.has("document")) {
                    JSONObject doc = msg.getJSONObject("document");
                    String fileName = doc.optString("file_name", "").toLowerCase();
                    String fileId = doc.getString("file_id");
                    
                    if (fileName.endsWith(".java")) {
                        appendLog("📥 Получен файл: " + fileName);
                        processCustomJavaFile(chatId, fileId, fileName);
                    }
                } else {
                    String text = msg.optString("text", "");
                    if ("/start".equals(text)) {
                        sendMessage(chatId, "🤖 **Android Компилятор Активен!**\nОтправьте мне любой `.java` файл документом.");
                    }
                }
            }
        } catch (Exception e) {
            appendLog("⚠️ Ошибка обработки апдейта: " + e.getMessage());
        }
    }

    private static void processCustomJavaFile(String chatId, String fileId, String fileName) {
        try {
            sendMessage(chatId, "⚙️ Скачивание и обработка файла `" + fileName + "`...");
            
            String getFileUrl = "https://api.telegram.org/bot" + BOT_TOKEN + "/getFile?file_id=" + fileId;
            HttpURLConnection conn = (HttpURLConnection) new URL(getFileUrl);
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject res = new JSONObject(sb.toString());
                
                if (res.optBoolean("ok")) {
                    String filePath = res.getJSONObject("result").getString("file_path");
                    String downloadUrl = "https://api.telegram.org/file/bot" + BOT_TOKEN + "/" + filePath;
                    
                    InputStream in = new URL(downloadUrl).openStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    String codeStr = out.toString("UTF-8");
                    
                    // Санитизация токенов для безопасности GitHub
                    codeStr = codeStr.replaceAll("ghp_[a-zA-Z0-9]{36}", "REDACTED_TOKEN");
                    
                    String pkg = (String) projectConfig.get("package_name");
                    codeStr = codeStr.replaceAll("(?m)^\\s*package\\s+[a-zA-Z0-9_.]+;", "package " + pkg + ";");
                    if (!codeStr.contains("package " + pkg + ";")) {
                        codeStr = "package " + pkg + ";\n\n" + codeStr;
                    }
                    
                    Matcher m = Pattern.compile("class\\s+([a-zA-Z0-9_]+)").matcher(codeStr);
                    if (m.find()) {
                        codeStr = codeStr.replace("class " + m.group(1), "class MainActivity");
                    }

                    removeAllExistingMainActivities();
                    syncConfigToGithub();
                    
                    String mainPath = "app/src/main/java/" + pkg.replace('.', '/') + "/MainActivity.java";
                    boolean success = pushContentToGithub(mainPath, codeStr.getBytes(StandardCharsets.UTF_8), "Update MainActivity");
                    
                    if (success) {
                        appendLog("✅ Код пропатчен и отправлен в GitHub. Запуск сборки APK...");
                        sendMessage(chatId, "✅ **Код успешно загружен!** Запускаю компиляцию на GitHub...");
                        triggerGithubBuild(chatId);
                    } else {
                        sendMessage(chatId, "❌ Ошибка отправки в GitHub репозиторий.");
                    }
                }
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка сборщика: " + e.getMessage());
            sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private static void triggerGithubBuild(String chatId) {
        try {
            String dispatchUrl = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/actions/workflows/" + WORKFLOW_FILE + "/dispatches";
            HttpURLConnection conn = (HttpURLConnection) new URL(dispatchUrl);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            conn.setRequestProperty("User-Agent", "AndroidCompilerApp");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject();
            payload.put("ref", "main");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 204 || conn.getResponseCode() == 201) {
                appendLog("🚀 Сборка APK успешно запущена в GitHub Actions!");
                sendMessage(chatId, "⚙️ **Компиляция на GitHub запущена!** Ожидайте готовый APK файл в этом чате.");
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка триггера сборки: " + e.getMessage());
        }
    }
}