package com.roma.nativeapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfig";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;
    private static final int PICK_APK_REQUEST = 1003;

    private String githubToken = "";
    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "RomaWalkieTalkie";
    private String appVersion = "1.0";
    private String packageName = "com.roma.walkie";
    private String currentJavaFileName = "Wi-Fi Рация (MainActivity.java)";
    private boolean iconCustomized = true;

    private int minSdk = 24;
    private int compileSdk = 34;
    private String javaVersion = "17"; 
    private String activeCodeContent = "";

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        if (activeCodeContent.isEmpty()) {
            activeCodeContent = generateWalkieTalkieCode();
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
        authorView.setText("✦ Made in Kamyanske • Roman Dziuba ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 10);
        container.addView(authorView);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(22f);
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
        titleView.setText("📡 Wi-Fi Рация Builder (Pure Java)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
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

        btnCodeEditor = createStyledButton("📝 Редактор кода рации", "#059669");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        btnUploadJava = createStyledButton("📂 Загрузить файл: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        btnDecompileApk = createStyledButton("📦 Декомпилировать APK в код", "#7C3AED");
        btnDecompileApk.setOnClickListener(v -> openApkFilePicker());
        card.addView(btnDecompileApk);

        btnReset = createStyledButton("🔄 Сброс к шаблону Рации", "#D97706");
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
        logEditText.setHint("Здесь отображаются логи компиляции...");
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
        return "🚀 Скомпилировать APK Рации\n📦 Пакет: " + packageName + " (v" + appVersion + ")";
    }

    private String getIconBtnText() {
        return iconCustomized ? "🖼 Иконка: 📡 Рация (Шаблон)" : "🖼 Иконка: 🎨 Пользовательская";
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
        if (infoTextView != null) infoTextView.setText(info);
        if (btnTitle != null) btnTitle.setText("🏷 Имя: " + appTitle + " (" + packageName + ")");
        if (btnVersion != null) btnVersion.setText("📦 Версия приложения: " + appVersion);
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
                        statusTextView.setText("Статус: ✅ Актуальные данные подтянуты с GitHub!");
                    });
                } else {
                    handler.post(() -> statusTextView.setText("Статус: Готов к работе"));
                }
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: Готов к работе"));
            }
        }).start();
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
            while ((line = reader.readLine()) != null) stringBuilder.append(line).append("\n");
            reader.close();

            String code = stringBuilder.toString();
            if (code.startsWith("\uFEFF")) code = code.substring(1);
            code = cleanInvisibleCharacters(code);

            Matcher pkgMatcher = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);").matcher(code);
            if (pkgMatcher.find()) {
                String foundPkg = pkgMatcher.group(1);
                if (!foundPkg.isEmpty()) packageName = foundPkg;
            }

            code = sanitizeAndInjectJavaCode(code, packageName);
            activeCodeContent = code;
            currentJavaFileName = finalFileName;
            saveSettings();
            btnUploadJava.setText("📂 Файл: " + currentJavaFileName);

            statusTextView.setText("Статус: ⚙️ Загрузка кода...");
            new Thread(() -> {
                removeAllExistingMainActivities(getMainActivityPath());
                boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Upload file: " + finalFileName);
                handler.post(() -> {
                    if (success) statusTextView.setText("Статус: ✅ Код загружен на GitHub!");
                    else statusTextView.setText("Статус: ❌ Ошибка загрузки");
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processSelectedApkFile(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) fileName = "app.apk";
        final String finalFileName = fileName;

        statusTextView.setText("Статус: 🔄 Декомпиляция APK...");
        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "decompiling_temp.apk");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new java.io.FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                }
                ZipFile zipFile = new ZipFile(tempFile);
                String foundPkg = "";
                ZipEntry dexEntry = zipFile.getEntry("classes.dex");
                if (dexEntry != null) {
                    try (InputStream dexIn = zipFile.getInputStream(dexEntry)) {
                        byte[] dexBytes = readBytes(dexIn);
                        String dexStr = new String(dexBytes, StandardCharsets.ISO_8859_1);
                        Matcher pkgMatch = Pattern.compile("L([a-zA-Z0-9_/]+)/MainActivity;").matcher(dexStr);
                        if (pkgMatch.find()) {
                            foundPkg = pkgMatch.group(1).replace('/', '.');
                        }
                    }
                }
                zipFile.close();
                tempFile.delete();

                if (foundPkg != null && !foundPkg.isEmpty()) packageName = foundPkg;

                activeCodeContent = sanitizeAndInjectJavaCode(generateWalkieTalkieCode(), packageName);
                currentJavaFileName = "Декомпилирован: " + finalFileName;
                saveSettings();

                handler.post(() -> {
                    statusTextView.setText("Статус: ✅ APK декомпилирован!");
                    btnUploadJava.setText("📂 Файл: " + currentJavaFileName);
                    updateInfoText();
                });

                removeAllExistingMainActivities(getMainActivityPath());
                pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Decompiled APK source");
            } catch (Exception e) {
                handler.post(() -> statusTextView.setText("Статус: ❌ Ошибка декомпиляции"));
            }
        }).start();
    }

    private String cleanInvisibleCharacters(String text) {
        if (text == null) return "";
        return text.replace('\u00A0', ' ').replace("\r\n", "\n").replace("\r", "\n");
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
                        if ((path.endsWith("MainActivity.java") || path.endsWith(".java")) && !path.equals(targetPath)) {
                            deleteGitHubFile(path, "Cleanup");
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
        code = code.replaceAll("package\\s+[a-zA-Z0-9_.]+;", "package " + pkg + ";");
        if (!code.contains("package ")) code = "package " + pkg + ";\n\n" + code;
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

            new Thread(() -> {
                pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Update icon");
                handler.post(() -> Toast.makeText(this, "Иконка обновлена!", Toast.LENGTH_SHORT).show());
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка иконки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readBytes(InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
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

        final EditText codeInput = new EditText(this);
        codeInput.setText(activeCodeContent);
        codeInput.setTextSize(11f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#E2E8F0"));
        codeInput.setBackgroundColor(Color.parseColor("#030712"));
        codeInput.setPadding(15, 15, 15, 15);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setMinLines(18);
        codeInput.setMaxLines(25);
        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода Рации")
            .setView(editorLayout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                activeCodeContent = cleanInvisibleCharacters(codeInput.getText().toString());
                saveSettings();
                Toast.makeText(this, "Сохранено локально!", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Отправить на GitHub", (dialog, which) -> {
                activeCodeContent = cleanInvisibleCharacters(codeInput.getText().toString());
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                saveSettings();
                statusTextView.setText("Статус: ⚙️ Отправка кода...");
                new Thread(() -> {
                    removeAllExistingMainActivities(getMainActivityPath());
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update via Editor");
                    handler.post(() -> {
                        if (success) statusTextView.setText("Статус: ✅ Код на GitHub!");
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
        btnBuild.setText("⏳ Сборка APK Рации...");
        statusTextView.setText("Статус: ⚙️ Синхронизация файлов...");

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
                conn.setDoOutput(true);

                String jsonBody = "{\"ref\":\"main\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 204 || responseCode == 201) {
                    handler.post(() -> {
                        statusTextView.setText("Статус: 🚀 Сборка запущена в GitHub Actions!");
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
                    statusTextView.setText("Статус: ❌ Ошибка сети");
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
        statusTextView.setText("Статус: 🛑 Остановлено вручную");
    }

    private boolean syncConfigToGitHub() {
        boolean r0 = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle");
        boolean r00 = pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings");
        boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update Manifest");
        boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle");
        boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow");
        boolean r4 = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");
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
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                sha = json.optString("sha", null);
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
                                        statusTextView.setText("Статус: ✅ APK отправлен в Telegram!");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка сборки");
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
                            handler.post(() -> {
                                if (fullLogs.length() > 4000) {
                                    logEditText.setText(fullLogs.substring(fullLogs.length() - 4000));
                                } else {
                                    logEditText.setText(fullLogs);
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
            .setTitle("Сброс к шаблону Рации")
            .setMessage("Установить исходный код Wi-Fi рации?")
            .setPositiveButton("Установить", (dialog, which) -> {
                appTitle = "RomaWalkieTalkie";
                packageName = "com.roma.walkie";
                appVersion = "1.0";
                iconCustomized = true;
                activeCodeContent = generateWalkieTalkieCode();
                currentJavaFileName = "Wi-Fi Рация (MainActivity.java)";
                saveSettings();
                statusTextView.setText("Статус: ✅ Шаблон рации установлен!");
                updateInfoText();
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
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                appTitle = input.getText().toString().trim();
                packageName = transliterate(appTitle);
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                saveSettings();
                Toast.makeText(this, "Пакет: " + packageName, Toast.LENGTH_SHORT).show();
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
            .setTitle("Версия")
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

        final EditText inputGithub = createDialogEditText("GitHub Token", githubToken);
        layout.addView(inputGithub);
        final EditText inputBot = createDialogEditText("Bot Token", botToken);
        layout.addView(inputBot);
        final EditText inputChat = createDialogEditText("Chat ID", chatId);
        layout.addView(inputChat);

        new AlertDialog.Builder(this)
            .setTitle("Настройки API")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();
                saveSettings();
                Toast.makeText(this, "Сохранено!", Toast.LENGTH_SHORT).show();
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
        input.setPadding(25, 15, 25, 15);
        return input;
    }

    private String transliterate(String text) {
        if (text == null || text.isEmpty()) return "com.roma.walkie";
        String res = text.toLowerCase()
            .replace("а", "a").replace("б", "b").replace("в", "v").replace("г", "g")
            .replace("д", "d").replace("е", "e").replace("ж", "zh").replace("з", "z")
            .replace("и", "i").replace("й", "y").replace("к", "k").replace("л", "l")
            .replace("м", "m").replace("н", "n").replace("о", "o").replace("п", "p")
            .replace("р", "r").replace("с", "s").replace("т", "t").replace("у", "u")
            .replace("ф", "f").replace("х", "h").replace("ц", "ts").replace("ч", "ch")
            .replace("ш", "sh").replace("щ", "sch").replace("ы", "y").replace("э", "e")
            .replace("ю", "yu").replace("я", "ya");
        res = res.replaceAll("[^a-z0-9]", "");
        return res.isEmpty() ? "com.roma.walkie" : "com." + res + ".app";
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
        return "name: Build Walkie APK\n\non:\n  workflow_dispatch:\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n      - run: |\n          export ANDROID_HOME=/usr/local/lib/android/sdk\n          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n          yes | sdkmanager --licenses || true\n          sdkmanager \"platform-tools\" \"platforms;android-34\" \"build-tools;34.0.0\"\n          gradle clean assembleDebug --stacktrace --no-daemon\n      - run: |\n          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n          curl -F chat_id=\"" + chatId + "\" -F document=@\"$APK_PATH\" https://api.telegram.org/bot" + botToken + "/sendDocument\n";
    }

    // [ОЧЕНЬ ВАЖНО]: Добавлено разрешение RECORD_AUDIO в Манифест!
    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n" +
               "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + appTitle + "\" android:supportsRtl=\"true\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
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
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk 34\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n        sourceCompatibility JavaVersion.VERSION_17\n        targetCompatibility JavaVersion.VERSION_17\n    }\n}\n\ndependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }

    // Исходный код самой Рации, который пойдет в скомпилированное приложение
    private String generateWalkieTalkieCode() {
        return "package " + packageName + ";\n\n" +
               "import android.Manifest;\n" +
               "import android.app.Activity;\n" +
               "import android.content.Context;\n" +
               "import android.content.pm.PackageManager;\n" +
               "import android.graphics.Color;\n" +
               "import android.graphics.Typeface;\n" +
               "import android.graphics.drawable.GradientDrawable;\n" +
               "import android.media.AudioAttributes;\n" +
               "import android.media.AudioFormat;\n" +
               "import android.media.AudioRecord;\n" +
               "import android.media.AudioTrack;\n" +
               "import android.media.MediaRecorder;\n" +
               "import android.os.Bundle;\n" +
               "import android.os.Handler;\n" +
               "import android.os.Looper;\n" +
               "import android.view.Gravity;\n" +
               "import android.view.MotionEvent;\n" +
               "import android.widget.*;\n" +
               "import java.net.DatagramPacket;\n" +
               "import java.net.DatagramSocket;\n" +
               "import java.net.InetAddress;\n" +
               "import java.net.NetworkInterface;\n" +
               "import java.util.Enumeration;\n" +
               "import java.util.Locale;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    private EditText ipInput;\n" +
               "    private Button pttButton;\n" +
               "    private TextView statusView, localIpView;\n" +
               "    private boolean isTransmitting = false;\n" +
               "    private boolean isListening = true;\n" +
               "    private static final int PORT = 50005;\n" +
               "    private static final int SAMPLE_RATE = 8000;\n" +
               "    private AudioRecord audioRecord;\n" +
               "    private AudioTrack audioTrack;\n" +
               "    private Handler handler = new Handler(Looper.getMainLooper());\n" +
               "    private Thread receiveThread;\n\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {\n" +
               "            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);\n" +
               "        }\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        layout.setPadding(30, 30, 30, 30);\n" +
               "        LinearLayout card = new LinearLayout(this);\n" +
               "        card.setOrientation(LinearLayout.VERTICAL);\n" +
               "        card.setGravity(Gravity.CENTER);\n" +
               "        card.setPadding(35, 45, 35, 45);\n" +
               "        GradientDrawable cardBg = new GradientDrawable();\n" +
               "        cardBg.setColor(Color.parseColor(\"#0F172A\"));\n" +
               "        cardBg.setCornerRadius(36f);\n" +
               "        cardBg.setStroke(2, Color.parseColor(\"#1E293B\"));\n" +
               "        card.setBackground(cardBg);\n" +
               "        TextView authorView = new TextView(this);\n" +
               "        authorView.setText(\"✦ Made in Kamyanske • Roman Dziuba ✦\");\n" +
               "        authorView.setTextColor(Color.parseColor(\"#F59E0B\"));\n" +
               "        authorView.setTextSize(13f);\n" +
               "        authorView.setTypeface(Typeface.create(\"serif\", Typeface.ITALIC));\n" +
               "        authorView.setGravity(Gravity.CENTER);\n" +
               "        authorView.setPadding(0, 0, 0, 16);\n" +
               "        card.addView(authorView);\n" +
               "        TextView title = new TextView(this);\n" +
               "        title.setText(\"📡 Wi-Fi Локальная Рация\");\n" +
               "        title.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        title.setTextSize(20f);\n" +
               "        title.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        title.setGravity(Gravity.CENTER);\n" +
               "        title.setPadding(0, 0, 0, 15);\n" +
               "        card.addView(title);\n" +
               "        localIpView = new TextView(this);\n" +
               "        localIpView.setText(\"Мой IP в сети: \" + getLocalIpAddress());\n" +
               "        localIpView.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        localIpView.setTextSize(13f);\n" +
               "        localIpView.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        localIpView.setGravity(Gravity.CENTER);\n" +
               "        localIpView.setPadding(0, 0, 0, 20);\n" +
               "        card.addView(localIpView);\n" +
               "        ipInput = new EditText(this);\n" +
               "        ipInput.setHint(\"IP собеседника (напр. 192.168.0.5)\");\n" +
               "        ipInput.setTextColor(Color.WHITE);\n" +
               "        ipInput.setHintTextColor(Color.parseColor(\"#64748B\"));\n" +
               "        ipInput.setTextSize(13f);\n" +
               "        ipInput.setBackgroundColor(Color.parseColor(\"#1E293B\"));\n" +
               "        ipInput.setPadding(20, 20, 20, 20);\n" +
               "        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);\n" +
               "        inputParams.setMargins(0, 0, 0, 25);\n" +
               "        ipInput.setLayoutParams(inputParams);\n" +
               "        card.addView(ipInput);\n" +
               "        statusView = new TextView(this);\n" +
               "        statusView.setText(\"Статус: Готов к связи (Слушаю эфир)\");\n" +
               "        statusView.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        statusView.setTextSize(14f);\n" +
               "        statusView.setGravity(Gravity.CENTER);\n" +
               "        statusView.setPadding(0, 0, 0, 25);\n" +
               "        card.addView(statusView);\n" +
               "        pttButton = new Button(this);\n" +
               "        pttButton.setText(\"🎙 ЗАЖМИ И ГОВОРИ\");\n" +
               "        pttButton.setTextColor(Color.WHITE);\n" +
               "        pttButton.setTextSize(16f);\n" +
               "        pttButton.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        GradientDrawable btnBg = new GradientDrawable();\n" +
               "        btnBg.setColor(Color.parseColor(\"#2563EB\"));\n" +
               "        btnBg.setCornerRadius(30f);\n" +
               "        pttButton.setBackground(btnBg);\n" +
               "        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 130);\n" +
               "        pttButton.setLayoutParams(btnParams);\n" +
               "        pttButton.setOnTouchListener((v, event) -> {\n" +
               "            switch (event.getAction()) {\n" +
               "                case MotionEvent.ACTION_DOWN:\n" +
               "                    startTransmitting();\n" +
               "                    btnBg.setColor(Color.parseColor(\"#DC2626\"));\n" +
               "                    pttButton.setText(\"🔴 ИДЕТ ПЕРЕДАЧА ГОЛОСА...\");\n" +
               "                    return true;\n" +
               "                case MotionEvent.ACTION_UP:\n" +
               "                case MotionEvent.ACTION_CANCEL:\n" +
               "                    stopTransmitting();\n" +
               "                    btnBg.setColor(Color.parseColor(\"#2563EB\"));\n" +
               "                    pttButton.setText(\"🎙 ЗАЖМИ И ГОВОРИ\");\n" +
               "                    return true;\n" +
               "            }\n" +
               "            return false;\n" +
               "        });\n" +
               "        card.addView(pttButton);\n" +
               "        layout.addView(card);\n" +
               "        setContentView(layout);\n" +
               "        startListening();\n" +
               "    }\n" +
               "    private String getLocalIpAddress() {\n" +
               "        try {\n" +
               "            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();\n" +
               "            while (interfaces.hasMoreElements()) {\n" +
               "                NetworkInterface intf = interfaces.nextElement();\n" +
               "                Enumeration<InetAddress> addrs = intf.getInetAddresses();\n" +
               "                while (addrs.hasMoreElements()) {\n" +
               "                    InetAddress addr = addrs.nextElement();\n" +
               "                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {\n" +
               "                        return addr.getHostAddress();\n" +
               "                    }\n" +
               "                }\n" +
               "            }\n" +
               "        } catch (Exception e) {}\n" +
               "        return \"Не удалось определить IP\";\n" +
               "    }\n" +
               "    private void startTransmitting() {\n" +
               "        final String targetIp = ipInput.getText().toString().trim();\n" +
               "        if (targetIp.isEmpty()) {\n" +
               "            Toast.makeText(this, \"Введите IP собеседника!\", Toast.LENGTH_SHORT).show();\n" +
               "            return;\n" +
               "        }\n" +
               "        isTransmitting = true;\n" +
               "        statusView.setText(\"Статус: 🔴 Передача голоса...\");\n" +
               "        new Thread(() -> {\n" +
               "            try {\n" +
               "                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {\n" +
               "                    handler.post(() -> statusView.setText(\"Ошибка: Нет разрешения на микрофон!\"));\n" +
               "                    isTransmitting = false;\n" +
               "                    return;\n" +
               "                }\n" +
               "                DatagramSocket socket = new DatagramSocket();\n" +
               "                InetAddress address = InetAddress.getByName(targetIp);\n" +
               "                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);\n" +
               "                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);\n" +
               "                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {\n" +
               "                    handler.post(() -> statusView.setText(\"Ошибка: Микрофон заблокирован\"));\n" +
               "                    isTransmitting = false;\n" +
               "                    return;\n" +
               "                }\n" +
               "                audioRecord.startRecording();\n" +
               "                byte[] buffer = new byte[bufferSize];\n" +
               "                while (isTransmitting) {\n" +
               "                    int read = audioRecord.read(buffer, 0, buffer.length);\n" +
               "                    if (read > 0) {\n" +
               "                        DatagramPacket packet = new DatagramPacket(buffer, read, address, PORT);\n" +
               "                        socket.send(packet);\n" +
               "                    }\n" +
               "                }\n" +
               "                audioRecord.stop();\n" +
               "                audioRecord.release();\n" +
               "                socket.close();\n" +
               "            } catch (Exception e) {\n" +
               "                handler.post(() -> statusView.setText(\"Ошибка передачи: \" + e.getMessage()));\n" +
               "            }\n" +
               "        }).start();\n" +
               "    }\n" +
               "    private void stopTransmitting() {\n" +
               "        isTransmitting = false;\n" +
               "        statusView.setText(\"Статус: Готов к связи (Слушаю эфир)\");\n" +
               "    }\n" +
               "    private void startListening() {\n" +
               "        receiveThread = new Thread(() -> {\n" +
               "            try {\n" +
               "                DatagramSocket socket = new DatagramSocket(PORT);\n" +
               "                int playBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);\n" +
               "                audioTrack = new AudioTrack(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(), new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(), playBufferSize, AudioTrack.MODE_STREAM, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE);\n" +
               "                audioTrack.play();\n" +
               "                byte[] buffer = new byte[playBufferSize];\n" +
               "                while (isListening) {\n" +
               "                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);\n" +
               "                    socket.receive(packet);\n" +
               "                    audioTrack.write(packet.getData(), 0, packet.getLength());\n" +
               "                }\n" +
               "                socket.close();\n" +
               "            } catch (Exception e) {}\n" +
               "        });\n" +
               "        receiveThread.start();\n" +
               "    }\n" +
               "    @Override\n" +
               "    protected void onDestroy() {\n" +
               "        super.onDestroy();\n" +
               "        isListening = false;\n" +
               "        isTransmitting = false;\n" +
               "        if (receiveThread != null) receiveThread.interrupt();\n" +
               "    }\n" +
               "}\n";
    }
}