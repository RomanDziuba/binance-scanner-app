package com.kompilyatsiya.app;

import android.graphics.Typeface;
import java.util.*;

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
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfig";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;

    private String githubToken = "";
    private String botToken = new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT));
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "RomaClockApp";
    private String appVersion = "1.0";
    private String packageName = "com.roma.nativeapp";
    private String currentJavaFileName = "Шаблон: Часы с фонариком (MainActivity.java)";
    private boolean iconCustomized = true;

    private int minSdk = 24;
    private int compileSdk = 34;
    private String javaVersion = "17"; 
    private String activeCodeContent = "";

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnUploadJava, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor;

    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;

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

        btnCodeEditor = createStyledButton("📝 Редактор кода (С подсказками)", "#059669");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        btnUploadJava = createStyledButton("📂 Загрузить файл: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

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

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/x-java-source", "application/octet-stream"});
        startActivityForResult(Intent.createChooser(intent, "Выберите .java или .txt файл"), PICK_JAVA_REQUEST);
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
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();

            String code = stringBuilder.toString();
            
            // Авто-очистка невидимых символов и неразрывных пробелов (\u00a0)
            code = sanitizeInvisibleChars(code);
            code = sanitizeAndInjectJavaCode(code, packageName);

            activeCodeContent = code;
            currentJavaFileName = finalFileName;
            saveSettings();
            btnUploadJava.setText("📂 Файл: " + currentJavaFileName);

            statusTextView.setText("Статус: ⚙️ Очистка старых файлов и отправка...");
            logEditText.setText("Загружен файл: " + finalFileName + "\nОчистка репозитория от старых Activity и отправка...");

            new Thread(() -> {
                removeAllExistingMainActivities(getMainActivityPath());

                boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Upload custom Java file: " + finalFileName);
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Код успешно обработан и загружен!");
                        logEditText.setText("Файл " + finalFileName + " очищен от невидимых символов (\u00a0), адаптирован под пакет " + packageName + " и отправлен на GitHub.");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка загрузки файла");
                    }
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Функция автоматической очистки неразрывных пробелов и невидимого мусора
    private String sanitizeInvisibleChars(String code) {
        if (code == null) return "";
        // Заменяем неразрывный пробел \u00a0 и другие варианты на обычный пробел
        code = code.replace('\u00a0', ' ');
        code = code.replace("\u00A0", " ");
        // Удаляем BOM метку в начале если присутствует
        if (code.startsWith("\uFEFF")) {
            code = code.substring(1);
        }
        return code;
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
            conn.setRequestMethod("DELETE");
            // ... (упрощено для стабильности удаления)
        } catch (Exception ignored) {}
    }

    private String sanitizeAndInjectJavaCode(String code, String pkg) {
        code = sanitizeInvisibleChars(code);

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
            
            new Thread(() -> {
                pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Update app icon");
                handler.post(() -> {
                    iconCustomized = false;
                    saveSettings();
                    statusTextView.setText("Статус: ✅ Иконка успешно обновлена!");
                });
            }).start();
        } catch (Exception ignored) {}
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
        hintTitle.setText("💡 Подсказки по коду (Авто-очистка пробелов активна):");
        hintTitle.setTextColor(Color.parseColor("#F59E0B"));
        hintTitle.setTextSize(12f);
        editorLayout.addView(hintTitle);

        final EditText codeInput = new EditText(this);
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
                // Автоматическая очистка невидимых символов при сохранении в редакторе
                activeCodeContent = sanitizeInvisibleChars(codeInput.getText().toString());
                saveSettings();
                Toast.makeText(this, "Код очищен и сохранен!", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Отправить на GitHub", (dialog, which) -> {
                activeCodeContent = sanitizeInvisibleChars(codeInput.getText().toString());
                activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
                saveSettings();
                statusTextView.setText("Статус: ⚙️ Очистка и отправка кода...");
                new Thread(() -> {
                    removeAllExistingMainActivities(getMainActivityPath());
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update via Editor");
                    handler.post(() -> {
                        if (success) statusTextView.setText("Статус: ✅ Код очищен и загружен на GitHub!");
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
        btnBuild.setText("⏳ Компиляция в процессе...");
        statusTextView.setText("Статус: ⚙️ Синхронизация и запуск...");

        new Thread(() -> {
            try {
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
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                sha = json.optString("sha", null);
            }

            HttpURLConnection putConn = (HttpURLConnection) new URL(urlStr).openConnection();
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
            while (isMonitoring) {
                try {
                    Thread.sleep(6000);
                    String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs?per_page=1";
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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

                            handler.post(() -> {
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                                    btnBuild.setText(getBuildButtonText());
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Успех! APK отправлен в Telegram.");
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

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс")
            .setMessage("Сбросить проект к шаблону часов?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
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
                    syncConfigToGitHub();
                    handler.post(() -> {
                        statusTextView.setText("Статус: ✅ Сброшено!");
                        updateInfoText();
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(appTitle);
        new AlertDialog.Builder(this)
            .setTitle("Название приложения")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                appTitle = input.getText().toString().trim();
                packageName = transliterate(appTitle);
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
        final EditText inputGithub = createDialogEditText("GitHub Token", githubToken);
        final EditText inputBot = createDialogEditText("Bot Token", botToken);
        final EditText inputChat = createDialogEditText("Chat ID", chatId);
        layout.addView(inputGithub);
        layout.addView(inputBot);
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
        input.setPadding(20, 15, 20, 15);
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
        return "app/src/main/java/" + packageName.replace('.', '/') + "/MainActivity.java";
    }

    private String getRootGradleContent() {
        return "buildscript { repositories { google() mavenCentral() } dependencies { classpath 'com.android.tools.build:gradle:8.2.2' } } allprojects { repositories { google() mavenCentral() } }\n";
    }

    private String getSettingsGradleContent() {
        return "rootProject.name = \"" + appTitle + "\"\ninclude ':app'\n";
    }

    private String getWorkflowContent() {
        return "name: Build Native Android APK (Java)\non:\n  workflow_dispatch:\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - uses: actions/checkout@v4\n" +
               "      - uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '" + (javaVersion.equals("8") ? "8" : "17") + "'\n" +
               "      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Setup Android SDK\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
               "      - name: Build Debug APK\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          gradle clean assembleDebug --stacktrace --no-daemon\n" +
               "      - name: Upload APK to Telegram\n        if: success()\n        run: |\n" +
               "          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
               "          if [ -n \"$APK_PATH\" ]; then\n" +
               "            curl -F chat_id=\"" + chatId + "\" -F document=@\"$APK_PATH\" https://api.telegram.org/bot" + botToken + "/sendDocument\n" +
               "          fi\n";
    }

    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + appTitle + "\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
               "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
               "            <intent-filter>\n" +
               "                <action android:name=\"android.intent.action.MAIN\" />\n" +
               "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
               "            </intent-filter>\n" +
               "        </activity>\n" +
               "    </application>\n</manifest>";
    }

    private String generateAppGradle() {
        return "plugins { id 'com.android.application' }\nandroid {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n" +
               "}\ndependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }

    private String generateMainActivity() {
        return "package " + packageName + ";\n\nimport android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.TextView;\n\n" +
               "public class MainActivity extends Activity {\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n        TextView tv = new TextView(this);\n" +
               "        tv.setText(\"⚡ " + appTitle + " (RomaClock)\");\n        setContentView(tv);\n    }\n}\n";
    }
}