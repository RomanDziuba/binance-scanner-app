package com.roma.nativeapp;

﻿import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class TelegramGitHubBot {

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
    private static final String ICON_PATH = "app/src/main/res/mipmap-xxhdpi/ic_launcher.png";
    // ====================================================

    private static final Map<String, Object> projectConfig = new HashMap<>();
    private static final Map<String, String> userStates = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    static {
        projectConfig.put("app_title", "RomaClockApp");
        projectConfig.put("package_name", "com.roma.nativeapp");
        projectConfig.put("version", "1.0");
        projectConfig.put("app_mode", "default_clock");
        projectConfig.put("app_language", "java");
        projectConfig.put("icon_customized", true);
        projectConfig.put("last_menu_message_id", null);
    }

    private static final String DEFAULT_WORKFLOW_TEMPLATE = 
            "name: Build Native Android APK (Java)\n\n" +
            "on:\n  workflow_dispatch:\n\n" +
            "jobs:\n  build:\n    runs-on: ubuntu-latest\n\n" +
            "    steps:\n" +
            "      - name: Checkout repository\n        uses: actions/checkout@v4\n\n" +
            "      - name: Set up JDK 17\n        uses: actions/setup-java@v4\n        with:\n" +
            "          distribution: 'temurin'\n          java-version: '17'\n\n" +
            "      - name: Setup Gradle 8.5\n        uses: gradle/actions/setup-gradle@v3\n        with:\n" +
            "          gradle-version: '8.5'\n\n" +
            "      - name: Setup Android SDK & Accept Licenses\n        run: |\n" +
            "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
            "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
            "          yes | sdkmanager --licenses || true\n" +
            "          sdkmanager \"platform-tools\" \"platforms;android-34\" \"build-tools;34.0.0\"\n\n" +
            "      - name: Build Debug APK with Gradle 8.5\n        run: |\n" +
            "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
            "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
            "          gradle assembleDebug --stacktrace --no-daemon\n\n" +
            "      - name: Upload APK to Telegram Bot\n        if: success()\n        run: |\n" +
            "          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
            "          if [ -n \"$APK_PATH\" ]; then\n" +
            "            APP_TITLE=\"{APP_TITLE}\"\n" +
            "            RENAMED_PATH=\"/tmp/$APP_TITLE.apk\"\n" +
            "            cp \"$APK_PATH\" \"$RENAMED_PATH\"\n" +
            "            curl -F chat_id=\"{MY_CHAT_ID}\" -F document=@\"$RENAMED_PATH\" https://api.telegram.org/bot{BOT_TOKEN}/sendDocument\n" +
            "          else\n" +
            "            echo \"❌ Ошибка: APK файл не найден!\"\n" +
            "            exit 1\n" +
            "          fi\n";

    private static String getWorkflowContent() {
        return DEFAULT_WORKFLOW_TEMPLATE
                .replace("{MY_CHAT_ID}", MY_CHAT_ID)
                .replace("{BOT_TOKEN}", BOT_TOKEN)
                .replace("{APP_TITLE}", (String) projectConfig.get("app_title"));
    }

    private static final String DEFAULT_GRADLE_PROPERTIES = 
            "android.useAndroidX=true\n" +
            "android.enableJetifier=true\n" +
            "android.nonTransitiveRClass=true\n" +
            "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n";

    private static String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n\n" +
                "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
                "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
                "    <uses-permission android:name=\"android.permission.VIBRATE\" />\n" +
                "    <uses-permission android:name=\"android.permission.WAKE_LOCK\" />\n" +
                "    <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />\n" +
                "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
                "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n\n" +
                "    <application\n" +
                "        android:allowBackup=\"true\"\n" +
                "        android:icon=\"@mipmap/ic_launcher\"\n" +
                "        android:label=\"" + projectConfig.get("app_title") + "\"\n" +
                "        android:supportsRtl=\"true\"\n" +
                "        android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n\n" +
                "        <activity\n" +
                "            android:name=\".MainActivity\"\n" +
                "            android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.MAIN\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "    </application>\n" +
                "</manifest>\n";
    }

    private static String generateMainActivity() {
        String pkg = (String) projectConfig.get("package_name");
        return "package " + pkg + ";\n\n" +
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
                "        super.onCreate(savedInstanceState);\n" +
                "        LinearLayout layout = new LinearLayout(this);\n" +
                "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
                "        layout.setGravity(Gravity.CENTER);\n" +
                "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
                "        layout.setPadding(40, 40, 40, 40);\n\n" +
                "        TextView titleView = new TextView(this);\n" +
                "        titleView.setText(\"⚡ RomaClockApp (Java)\");\n" +
                "        titleView.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
                "        titleView.setTextSize(24f);\n" +
                "        titleView.setGravity(Gravity.CENTER);\n\n" +
                "        TextClock clock = new TextClock(this);\n" +
                "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
                "        clock.setTextSize(48f);\n" +
                "        clock.setGravity(Gravity.CENTER);\n\n" +
                "        layout.addView(titleView);\n" +
                "        layout.addView(clock);\n" +
                "        setContentView(layout);\n" +
                "    }\n" +
                "}\n";
    }

    private static String generateAppGradle() {
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
                "android {\n" +
                "    namespace '" + projectConfig.get("package_name") + "'\n" +
                "    compileSdk 34\n" +
                "    defaultConfig {\n" +
                "        applicationId \"" + projectConfig.get("package_name") + "\"\n" +
                "        minSdk 24\n" +
                "        targetSdk 34\n" +
                "        versionCode 1\n" +
                "        versionName \"" + projectConfig.get("version") + "\"\n" +
                "    }\n" +
                "    compileOptions {\n" +
                "        sourceCompatibility JavaVersion.VERSION_17\n" +
                "        targetCompatibility JavaVersion.VERSION_17\n" +
                "    }\n" +
                "}\n\n" +
                "dependencies {\n" +
                "    implementation 'androidx.appcompat:appcompat:1.6.1'\n" +
                "    implementation 'com.google.android.material:material:1.11.0'\n" +
                "}\n";
    }

    private static final String DEFAULT_ROOT_GRADLE = 
            "buildscript {\n    repositories { google(); mavenCentral() }\n" +
            "    dependencies { classpath 'com.android.tools.build:gradle:8.2.2' }\n}\n" +
            "allprojects { repositories { google(); mavenCentral() } }\n";

    private static final String DEFAULT_SETTINGS_GRADLE = 
            "rootProject.name = \"RomaClockApp\"\ninclude ':app'\n";

    private static String getMainActivityPath() {
        String pkgPath = ((String) projectConfig.get("package_name")).replace('.', '/');
        return "app/src/main/java/" + pkgPath + "/MainActivity.java";
    }

    // ==================== GITHUB API ====================
    private static boolean pushContentToGithub(String path, byte[] contentBytes, String commitMsg) {
        while (true) {
            try {
                String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + path;
                HttpURLConnection getConn = (HttpURLConnection) new URL(urlStr).openConnection();
                getConn.setRequestMethod("GET");
                getConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
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
                putConn.setRequestProperty("Accept", "application/vnd.github+json");
                putConn.setRequestProperty("Content-Type", "application/json");
                putConn.setDoOutput(true);

                try (OutputStream os = putConn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = putConn.getResponseCode();
                return code == 200 || code == 201;
            } catch (Exception e) {
                System.out.println("⚠️ Ошибка сети при отправке в GitHub (" + path + "), ждем... " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void deleteGithubFile(String path, String commitMsg) {
        try {
            String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + path;
            HttpURLConnection getConn = (HttpURLConnection) new URL(urlStr).openConnection();
            getConn.setRequestMethod("GET");
            getConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            getConn.setRequestProperty("Accept", "application/vnd.github+json");

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
                delConn.setRequestProperty("Accept", "application/vnd.github+json");
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
                        if (path.endsWith("MainActivity.java") || path.endsWith("MainActivity.kt")) {
                            deleteGithubFile(path, "Cleanup old file: " + path);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean checkRepositoryInitialized() {
        try {
            String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + GRADLE_PROPERTIES_PATH;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            if (conn.getResponseCode() != 200) return false;

            String mainUrl = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + getMainActivityPath();
            HttpURLConnection mainConn = (HttpURLConnection) new URL(mainUrl).openConnection();
            mainConn.setRequestMethod("GET");
            mainConn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            return mainConn.getResponseCode() == 200;
        } catch (Exception e) {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    private static void syncConfigToGithub() {
        pushContentToGithub(MANIFEST_PATH, generateManifest().getBytes(StandardCharsets.UTF_8), "Update AndroidManifest.xml");
        pushContentToGithub(BUILD_GRADLE_APP_PATH, generateAppGradle().getBytes(StandardCharsets.UTF_8), "Update app/build.gradle");
        pushContentToGithub(WORKFLOW_PATH, getWorkflowContent().getBytes(StandardCharsets.UTF_8), "Update workflow");
    }

    private static void ensureRepositoryIntegrity(boolean force) {
        if (!force && checkRepositoryInitialized()) {
            System.out.println("⚡ [Старт] Репозиторий проверен.");
            return;
        }
        System.out.println("🧹 [Старт] Полный сброс и очистка проекта...");
        deleteGithubFile(".github/workflows/build_godot.yml", "Delete old");
        deleteGithubFile(".github/workflows/main.yml", "Delete old");
        removeAllExistingMainActivities();

        pushContentToGithub(WORKFLOW_PATH, getWorkflowContent().getBytes(StandardCharsets.UTF_8), "Reset workflow");
        pushContentToGithub(BUILD_GRADLE_ROOT_PATH, DEFAULT_ROOT_GRADLE.getBytes(StandardCharsets.UTF_8), "Reset root gradle");
        pushContentToGithub(SETTINGS_GRADLE_PATH, DEFAULT_SETTINGS_GRADLE.getBytes(StandardCharsets.UTF_8), "Reset settings");
        pushContentToGithub(GRADLE_PROPERTIES_PATH, DEFAULT_GRADLE_PROPERTIES.getBytes(StandardCharsets.UTF_8), "Add properties");
        syncConfigToGithub();

        if ("default_clock".equals(projectConfig.get("app_mode"))) {
            pushContentToGithub(getMainActivityPath(), generateMainActivity().getBytes(StandardCharsets.UTF_8), "Reset MainActivity");
        }
        System.out.println("✅ Сброс завершен!");
    }

    // ==================== TELEGRAM API ====================
    private static void sendMessage(String chatId, String text, String replyMarkup) {
        try {
            String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
            JSONObject json = new JSONObject();
            json.put("chat_id", chatId);
            json.put("text", text);
            json.put("parse_mode", "Markdown");
            if (replyMarkup != null) json.put("reply_markup", new JSONObject(replyMarkup));

            sendPostRequest(urlStr, json.toString());
        } catch (Exception ignored) {}
    }

    private static void editMessage(String chatId, long messageId, String text, String replyMarkup) {
        try {
            String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/editMessageText";
            JSONObject json = new JSONObject();
            json.put("chat_id", chatId);
            json.put("message_id", messageId);
            json.put("text", text);
            json.put("parse_mode", "Markdown");
            if (replyMarkup != null) json.put("reply_markup", new JSONObject(replyMarkup));

            sendPostRequest(urlStr, json.toString());
        } catch (Exception ignored) {}
    }

    private static void answerCallback(String callbackId, String text) {
        try {
            String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/answerCallbackQuery";
            JSONObject json = new JSONObject();
            json.put("callback_query_id", callbackId);
            json.put("text", text);
            sendPostRequest(urlStr, json.toString());
        } catch (Exception ignored) {}
    }

    private static String sendPostRequest(String urlStr, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private static String getMenuText() {
        return "📱 **Панель управления Android (Java Bot)**\n\n" +
                "🏷 **Имя:** `" + projectConfig.get("app_title") + "`\n" +
                "📦 **Пакет:** `" + projectConfig.get("package_name") + "`\n" +
                "🔢 **Версия:** `" + projectConfig.get("version") + "`\n";
    }

    private static String getKeyboard(String buildStatus, String resetStatus) {
        JSONObject kb = new JSONObject();
        JSONArray rows = new JSONArray();

        JSONArray r1 = new JSONArray();
        r1.put(new JSONObject().put("text", buildStatus).put("callback_data", "build_apk"));
        rows.put(r1);

        JSONArray r2 = new JSONArray();
        r2.put(new JSONObject().put("text", resetStatus).put("callback_data", "reset_project"));
        rows.put(r2);

        kb.put("inline_keyboard", rows);
        return kb.toString();
    }

    private static void sendMainMenu(String chatId, Long messageId) {
        String text = getMenuText();
        String kb = getKeyboard("🚀 Скомпилировать APK (Java)", "🔄 Полный сброс (Очистка)");
        if (messageId != null) {
            editMessage(chatId, messageId, text, kb);
        } else {
            sendMessage(chatId, text, kb);
        }
    }

    // ==================== MAIN LOOP ====================
    public static void main(String[] args) {
        System.out.println("🤖 Запуск Java Telegram-бота...");
        ensureRepositoryIntegrity(false);

        long offset = 0;
        while (true) {
            try {
                String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + offset + "&timeout=5";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
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

    private static void handleUpdate(JSONObject update) {
        try {
            if (update.has("callback_query")) {
                JSONObject cb = update.getJSONObject("callback_query");
                String chatId = String.valueOf(cb.getJSONObject("message").getJSONObject("chat").getLong("id"));
                if (!chatId.equals(MY_CHAT_ID)) return;

                String callbackId = cb.getString("id");
                String data = cb.getString("data");
                long msgId = cb.getJSONObject("message").getLong("message_id");
                answerCallback(callbackId, "Принято!");

                if ("reset_project".equals(data)) {
                    editMessage(chatId, msgId, getMenuText(), getKeyboard("🚀 Скомпилировать APK (Java)", "🧹 Очистка..."));
                    projectConfig.put("app_mode", "default_clock");
                    projectConfig.put("app_title", "RomaClockApp");
                    projectConfig.put("package_name", "com.roma.nativeapp");
                    ensureRepositoryIntegrity(true);
                    editMessage(chatId, msgId, getMenuText(), getKeyboard("🚀 Скомпилировать APK (Java)", "✅ Сброшено!"));
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    sendMainMenu(chatId, msgId);
                } else if ("build_apk".equals(data)) {
                    editMessage(chatId, msgId, getMenuText(), getKeyboard("⚙️ Запуск сборки...", "🔄 Полный сброс"));
                    // Запуск билда в отдельном потоке
                    threadPool.submit(() -> triggerGithubBuild(chatId, msgId));
                }
            } else if (update.has("message")) {
                JSONObject msg = update.getJSONObject("message");
                String chatId = String.valueOf(msg.getJSONObject("chat").getLong("id"));
                if (!chatId.equals(MY_CHAT_ID)) return;

                String text = msg.optString("text", "");
                if ("/start".equals(text) || "/menu".equals(text)) {
                    sendMainMenu(chatId, null);
                }
            }
        } catch (Exception e) {
            System.out.println("Error handling update: " + e.getMessage());
        }
    }

    private static void triggerGithubBuild(String chatId, long messageId) {
        try {
            syncConfigToGithub();
            String dispatchUrl = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/actions/workflows/" + WORKFLOW_FILE + "/dispatches";
            HttpURLConnection conn = (HttpURLConnection) new URL(dispatchUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject();
            payload.put("ref", "main");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 204 || conn.getResponseCode() == 201) {
                editMessage(chatId, messageId, "⚙️ Сборка запущена на GitHub...", getKeyboard("⏳ Компиляция...", "🔄 Полный сброс"));
            } else {
                editMessage(chatId, messageId, "❌ Ошибка запуска компиляции.", getKeyboard("🚀 Скомпилировать APK (Java)", "🔄 Полный сброс"));
            }
        } catch (Exception e) {
            editMessage(chatId, messageId, "❌ Ошибка: " + e.getMessage(), getKeyboard("🚀 Скомпилировать APK (Java)", "🔄 Полный сброс"));
        }
    }
}