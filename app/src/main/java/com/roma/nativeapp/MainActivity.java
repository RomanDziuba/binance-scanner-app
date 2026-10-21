package com.roma.nativeapp;

import android.view.*;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfig";
    private static final int PICK_ICON_REQUEST = 1001;
    private static final int PICK_JAVA_REQUEST = 1002;

    // Настройки, сохраняемые на телефоне
    private String githubToken = "";
    private String botToken = "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    private String appTitle = "RomaClockApp";
    private String appVersion = "1.0";
    private String packageName = "com.roma.nativeapp";
    private String currentJavaFileName = "Шаблон: Часы (MainActivity.java)";

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnUploadJava, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

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

        // 1. Авторская подпись
        TextView authorView = new TextView(this);
        authorView.setText("✦ Created by Dziuba Roman ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(16f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 10);
        container.addView(authorView);

        // 2. Живые часы
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
        titleView.setText("📱 Android CI/CD Controller");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(18f);
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

        // ЛОГИЧЕСКИЙ ПОРЯДОК ИНТЕРАКТИВНЫХ КНОПОК
        // 1. Кнопка компиляции (интерактивная с инфо о пакете)
        btnBuild = createStyledButton(getBuildButtonText(), "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        // 2. Кнопка загрузки Java файла (отображает имя загруженного файла)
        btnUploadJava = createStyledButton("📝 Java файл: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        // 3. Полный сброс репозитория
        btnReset = createStyledButton("🔄 Полный сброс (Шаблон Часы)", "#D97706");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        // 4. Изменить имя и пакет
        btnTitle = createStyledButton("🏷 Имя: " + appTitle + " (" + packageName + ")", "#475569");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        // 5. Изменить версию приложения
        btnVersion = createStyledButton("📦 Версия приложения: " + appVersion, "#475569");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        // 6. Иконка приложения (с миниатюрой)
        btnIcon = createStyledButton("🖼 Иконка приложения (Выбрать файл)", "#7C3AED");
        updateIconButtonPreview();
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        // 7. Настройки API (GitHub & Bot)
        btnSettings = createStyledButton("⚙️ Настройки API (GitHub & Bot)", "#334155");
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

        // РАСШИРЕННЫЙ ТЕРМИНАЛ / ЛОГИ
        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Расширенный терминал / Логи:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(12f);
        logLabel.setPadding(0, 8, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь отображаются логи компиляции, поиск ошибок и статусы...");
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
        return "🚀 Скомпилировать APK\n📦 Пакет: " + packageName + " (v" + appVersion + ")";
    }

    private void updateIconButtonPreview() {
        try {
            Bitmap iconBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(iconBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.parseColor("#7C3AED"));
            canvas.drawRoundRect(new RectF(0, 0, 48, 48), 12, 12, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(22f);
            canvas.drawText("🖼", 10, 33, paint);

            BitmapDrawable d = new BitmapDrawable(getResources(), iconBitmap);
            d.setBounds(0, 0, 50, 50);
            btnIcon.setCompoundDrawables(d, null, null, null);
            btnIcon.setCompoundDrawablePadding(15);
        } catch (Exception ignored) {}
    }

    private void updateInfoText() {
        String info = "🏷 Имя: " + appTitle + "\n" +
                      "📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + "\n" +
                      "📂 Репо: " + repoOwner + "/" + repoName;
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
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        githubToken = sp.getString("githubToken", "");
        botToken = sp.getString("botToken", "");
        chatId = sp.getString("chatId", chatId);
        repoOwner = sp.getString("repoOwner", repoOwner);
        repoName = sp.getString("repoName", repoName);
        appTitle = sp.getString("appTitle", appTitle);
        appVersion = sp.getString("appVersion", appVersion);
        packageName = sp.getString("packageName", packageName);
        currentJavaFileName = sp.getString("currentJavaFileName", "Шаблон: Часы (MainActivity.java)");
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
        editor.apply();
        updateInfoText();
    }

    // ==================== ВЫБОР ФАЙЛОВ ИЗ СИСТЕМЫ ====================

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/x-java-source", "application/octet-stream"});
        startActivityForResult(Intent.createChooser(intent, "Выберите .java файл"), PICK_JAVA_REQUEST);
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

    // ==================== МОЩНАЯ СИСТЕМА ПОИСКА И ПРОВЕРКИ ОШИБОК ====================

    private String validateAndFixJavaCode(String code, String targetPackage) {
        StringBuilder logReport = new StringBuilder("🔍 [АНАЛИЗАТОР ОШИБОК JAVA]\n");
        boolean hasError = false;

        // 1. Проверка на пустоту
        if (code == null || code.trim().isEmpty()) {
            return "ERROR: Исходный файл пуст!";
        }

        // 2. Валидация баланса скобок
        Deque<Character> stack = new ArrayDeque<>();
        int lineNumber = 1;
        int errorLine = -1;
        char expectedChar = ' ';

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '\n') lineNumber++;

            if (c == '{' || c == '(' || c == '[') {
                stack.push(c);
            } else if (c == '}' || c == ')' || c == ']') {
                if (stack.isEmpty()) {
                    hasError = true;
                    errorLine = lineNumber;
                    logReport.append("❌ Лишняя закрывающая скобка '").append(c).append("' на строке ").append(lineNumber).append("\n");
                    break;
                }
                char open = stack.pop();
                if ((c == '}' && open != '{') || (c == ')' && open != '(') || (c == ']' && open != '[')) {
                    hasError = true;
                    errorLine = lineNumber;
                    expectedChar = open == '{' ? '}' : (open == '(' ? ')' : ']');
                    logReport.append("❌ Несоответствие скобки на строке ").append(lineNumber)
                             .append(". Ожидалось '").append(expectedChar).append("', найдено '").append(c).append("'\n");
                    break;
                }
            }
        }
        if (!stack.isEmpty() && !hasError) {
            hasError = true;
            logReport.append("❌ Незакрытая скобка в конце файла! Количество незакрытых: ").append(stack.size()).append("\n");
        }

        // 3. Проверка структуры класса
        if (!code.contains("class ")) {
            logReport.append("⚠️ Предупреждение: В файле не обнаружено объявление 'class'.\n");
        }
        if (!code.contains("onCreate")) {
            logReport.append("⚠️ Предупреждение: Метод 'onCreate' отсутствует. Это может нарушить запуск Activity.\n");
        }

        // 4. Коррекция пакета (Package matching)
        Pattern pkgPattern = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE);
        Matcher matcher = pkgPattern.matcher(code);
        if (matcher.find()) {
            String existingPackage = matcher.group(1);
            if (!existingPackage.equals(targetPackage)) {
                logReport.append("🛠 Авто-фикс: Пакет '").append(existingPackage).append("' изменен на '").append(targetPackage).append("'\n");
                code = code.replaceFirst("package\\s+" + Pattern.quote(existingPackage) + "\\s*;", "package " + targetPackage + ";");
            } else {
                logReport.append("✅ Имя пакета корректно: ").append(targetPackage).append("\n");
            }
        } else {
            logReport.append("🛠 Авто-фикс: Добавлено объявление пакета: package ").append(targetPackage).append(";\n");
            code = "package " + targetPackage + ";\n\n" + code;
        }

        if (hasError) {
            logReport.append("⛔ Ошибка синтаксиса обнаружена до отправки! Сборка остановлена для предотвращения ошибки Gradle.\n");
            return "ERROR_LOG:" + logReport.toString();
        }

        logReport.append("✅ Синтаксический анализ пройден успешно! Ошибок скобок и структуры не обнаружено.\n");
        appendLog(logReport.toString());
        return code;
    }

    private void appendLog(String message) {
        handler.post(() -> {
            if (logEditText != null) {
                String current = logEditText.getText().toString();
                logEditText.setText(current + (current.isEmpty() ? "" : "\n") + message);
                logEditText.setSelection(logEditText.getText().length());
            }
        });
    }

    private void processSelectedJavaFile(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) {
            fileName = "CustomCode.java";
        }
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

            String rawCode = stringBuilder.toString();
            logEditText.setText(" Чтение и сканирование файла: " + finalFileName + "...\n");

            // Запуск усиленного анализатора
            String validatedCode = validateAndFixJavaCode(rawCode, packageName);

            if (validatedCode.startsWith("ERROR_LOG:")) {
                statusTextView.setText("Статус: ❌ Найдены ошибки в Java коде!");
                logEditText.setText(validatedCode.replace("ERROR_LOG:", ""));
                Toast.makeText(this, "⚠️ Обнаружена синтаксическая ошибка в Java коде!", Toast.LENGTH_LONG).show();
                return;
            }

            currentJavaFileName = finalFileName;
            saveSettings();
            btnUploadJava.setText("📝 Java файл: " + currentJavaFileName);

            statusTextView.setText("Статус: ⚙️ Отправка кастомного Java кода...");
            appendLog("Применение Anti-409 Shield и отправка в репозиторий...");

            new Thread(() -> {
                boolean success = pushFileToGitHub(getMainActivityPath(), validatedCode, "Upload custom Java file: " + finalFileName);
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Java файл успешно загружен!");
                        appendLog("Файл " + finalFileName + " адаптирован под пакет " + packageName + " и отправлен на GitHub.");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка загрузки файла");
                        appendLog("Не удалось отправить файл на GitHub. Проверьте токен и доступ.");
                    }
                });
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processSelectedIcon(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(inputStream);
            
            statusTextView.setText("Статус: ⚙️ Загрузка иконки...");
            logEditText.setText("Отправка пользовательской иконки на GitHub...\n");

            new Thread(() -> {
                boolean success = pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", bytes, "Update app icon");
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Иконка успешно обновлена!");
                        appendLog("Новая иконка приложения установлена в репозиторий.");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка обновления иконки");
                        appendLog("Не удалось загрузить иконку на GitHub.");
                    }
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка обработки картинки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readBytes(InputStream inputStream) throws java.io.IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // ==================== ANTI-409 GITHUB SHIELD ====================
    private String sanitizeAndEncodeCode(String code) {
        if (code == null) return "";
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
        return code;
    }

    // ==================== СЕТЕВЫЕ ОПЕРАЦИИ С GITHUB ====================

    private void triggerGitHubBuild() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub Token в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        // Блокируем кнопку компиляции, меняем цвет и текст
        btnBuild.setEnabled(false);
        btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
        btnBuild.setText("⏳ Компиляция в процессе...\n📦 Пакет: " + packageName);
        statusTextView.setText("Статус: ⚙️ Синхронизация файлов и запуск...");
        logEditText.setText("🚀 Запуск процесса CI/CD...\nПроверка конфигурационных файлов...");

        new Thread(() -> {
            try {
                boolean synced = syncConfigToGitHub();
                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка 401/403 (Проверьте токен)");
                        appendLog("❌ Ошибка авторизации на GitHub. Проверьте валидность токена и доступ к репозиторию.");
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
                        appendLog("Workflow успешно запущен. Ожидание сборки и генерации APK...");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка запуска (" + responseCode + ")");
                        appendLog("Не удалось запустить workflow. Код ответа сервера: " + responseCode);
                    });
                }
            } catch (final Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                    btnBuild.setText(getBuildButtonText());
                    statusTextView.setText("Статус: ❌ Ошибка сети");
                    appendLog("Исключение сети: " + e.getMessage());
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
        return r0 && r00 && r1 && r2 && r3;
    }

    private boolean pushFileToGitHub(String path, String content, String message) {
        return pushBytesToGitHub(path, sanitizeAndEncodeCode(content).getBytes(StandardCharsets.UTF_8), message);
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
                appendLog("❌ [GitHub API Error] Ошибка прав доступа (" + getCode + ") при получении SHA для " + path);
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
            int putCode = putConn.getResponseCode();
            boolean success = putCode == 200 || putCode == 201;
            if (!success) {
                appendLog("❌ [GitHub API Error] Не удалось обновить файл " + path + ". Код ответа: " + putCode);
            }
            return success;
        } catch (Exception e) {
            appendLog("❌ [Exception] Ошибка при передаче данных на GitHub (" + path + "): " + e.getMessage());
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
                                if ("completed".equals(status)) {
                                    isMonitoring = false;
                                    btnBuild.setEnabled(true);
                                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                                    btnBuild.setText(getBuildButtonText());
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Сборка успешна! APK отправлен в Telegram.");
                                        appendLog("🎉 Сборка завершена успешно! Бот автоматически отправил APK в ваш Telegram чат.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции (" + conclusion + ")");
                                        appendLog("❌ Сборка завершилась со статусом: " + conclusion + ". Получение логов ошибок...");
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
                            while ((line = logBr.readLine()) != null) {
                                logSb.append(line).append("\n");
                            }
                            final String fullLogs = logSb.toString();
                            handler.post(() -> {
                                appendLog("\n=== [ПОЛНЫЕ ЛОГИ ОШИБОК BUILD] ===");
                                if (fullLogs.length() > 4000) {
                                    appendLog(fullLogs.substring(fullLogs.length() - 4000));
                                } else {
                                    appendLog(fullLogs);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                appendLog("❌ Ошибка при скачивании логов: " + e.getMessage());
            }
        }).start();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс")
            .setMessage("Сбросить репозиторий и установить стандартный шаблон часов?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                btnReset.setEnabled(false);
                btnReset.setBackgroundColor(Color.parseColor("#78350F"));
                btnReset.setText("🔄 Выполняется полный сброс...");
                statusTextView.setText("Статус: Полный сброс проекта...");
                logEditText.setText("Очистка и установка стандартного шаблона часов (MainActivity.java)...\n");

                new Thread(() -> {
                    boolean r0 = pushFileToGitHub("build.gradle", getRootGradleContent(), "Reset root build.gradle");
                    boolean r00 = pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Reset settings.gradle");
                    boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Reset AndroidManifest");
                    boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Reset app/build.gradle");
                    boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Reset workflow");
                    boolean r4 = pushFileToGitHub(getMainActivityPath(), generateMainActivity(), "Reset default MainActivity");
                    
                    boolean success = r0 && r00 && r1 && r2 && r3 && r4;

                    handler.post(() -> {
                        btnReset.setEnabled(true);
                        btnReset.setBackgroundColor(Color.parseColor("#D97706"));
                        btnReset.setText("🔄 Полный сброс (Шаблон Часы)");
                        if (success) {
                            currentJavaFileName = "Шаблон: Часы (MainActivity.java)";
                            saveSettings();
                            btnUploadJava.setText("📝 Java файл: " + currentJavaFileName);
                            statusTextView.setText("Статус: ✅ Репозиторий сброшен к часам!");
                            appendLog("Проект успешно сброшен к исходному шаблону часов.");
                        } else {
                            statusTextView.setText("Статус: ❌ Ошибка сброса (Проверьте токен)");
                            appendLog("Не удалось сбросить проект. Проверьте правильность GitHub токена.");
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

        final EditText inputGithub = new EditText(this);
        inputGithub.setHint("GitHub Token (ghp_...)");
        inputGithub.setText(githubToken);
        inputGithub.setBackgroundColor(Color.WHITE);
        inputGithub.setTextColor(Color.BLACK);
        inputGithub.setHintTextColor(Color.GRAY);
        inputGithub.setPadding(30, 20, 30, 20);
        layout.addView(inputGithub);

        final EditText inputBot = new EditText(this);
        inputBot.setHint("Telegram Bot Token");
        inputBot.setText(botToken);
        inputBot.setBackgroundColor(Color.WHITE);
        inputBot.setTextColor(Color.BLACK);
        inputBot.setHintTextColor(Color.GRAY);
        inputBot.setPadding(30, 20, 30, 20);
        layout.addView(inputBot);

        final EditText inputChat = new EditText(this);
        inputChat.setHint("Chat ID");
        inputChat.setText(chatId);
        inputChat.setBackgroundColor(Color.WHITE);
        inputChat.setTextColor(Color.BLACK);
        inputChat.setHintTextColor(Color.GRAY);
        inputChat.setPadding(30, 20, 30, 20);
        layout.addView(inputChat);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки API и Токенов")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String tokenCandidate = inputGithub.getText().toString().trim();
                String botCandidate = inputBot.getText().toString().trim();
                String chatCandidate = inputChat.getText().toString().trim();

                // Проверка токена GitHub
                if (!tokenCandidate.isEmpty() && !tokenCandidate.startsWith("ghp_") && !tokenCandidate.startsWith("github_pat_")) {
                    Toast.makeText(this, "⚠️ Предупреждение: Формат GitHub Token не выглядит стандартным!", Toast.LENGTH_LONG).show();
                }

                githubToken = tokenCandidate;
                botToken = botCandidate;
                chatId = chatCandidate;
                saveSettings();
                Toast.makeText(this, "Настройки сохранены!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private String transliterate(String text) {
        if (text == null || text.isEmpty()) return "com.roma.nativeapp";
        String res = text.toLowerCase(Locale.ROOT)
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
        return "plugins {\n" +
               "    id 'com.android.application' version '8.1.1' apply false\n" +
               "}\n";
    }

    private String getSettingsGradleContent() {
        return "pluginManagement {\n" +
               "    repositories {\n" +
               "        google()\n" +
               "        mavenCentral()\n" +
               "        gradlePluginPortal()\n" +
               "    }\n" +
               "}\n" +
               "dependencyResolutionManagement {\n" +
               "    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n" +
               "    repositories {\n" +
               "        google()\n" +
               "        mavenCentral()\n" +
               "    }\n" +
               "}\n" +
               "rootProject.name = \"" + appTitle + "\"\n" +
               "include ':app'\n";
    }

    private String getWorkflowContent() {
        return "name: Build Native Android APK (Java)\n\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - uses: actions/checkout@v4\n" +
               "      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n" +
               "      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.0'\n          cache-disabled: true\n" +
               "      - name: Setup Android SDK & Accept Licenses\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-34\" \"build-tools;34.0.0\"\n" +
               "      - name: Generate Gradle Wrapper\n        run: |\n" +
               "          gradle wrapper --gradle-version 8.0\n" +
               "      - name: Build Debug APK with Gradle Wrapper\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          ./gradlew clean assembleDebug --stacktrace --no-daemon\n" +
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
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
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
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk 34\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n}\n";
    }

    private String generateMainActivity() {
        return "package " + packageName + ";\n\n" +
               "import android.app.Activity;\n" +
               "import android.os.Bundle;\n" +
               "import android.view.Gravity;\n" +
               "import android.widget.LinearLayout;\n" +
               "import android.widget.TextClock;\n" +
               "import android.widget.TextView;\n" +
               "import android.graphics.Color;\n" +
               "import android.graphics.Typeface;\n" +
               "import android.graphics.drawable.GradientDrawable;\n" +
               "import java.text.SimpleDateFormat;\n" +
               "import java.util.Date;\n" +
               "import java.util.Locale;\n\n" +
               "public class MainActivity extends Activity {\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        LinearLayout layout = new LinearLayout(this);\n" +
               "        layout.setOrientation(LinearLayout.VERTICAL);\n" +
               "        layout.setGravity(Gravity.CENTER);\n" +
               "        layout.setBackgroundColor(Color.parseColor(\"#030712\"));\n" +
               "        layout.setPadding(40, 40, 40, 40);\n\n" +
               "        LinearLayout card = new LinearLayout(this);\n" +
               "        card.setOrientation(LinearLayout.VERTICAL);\n" +
               "        card.setGravity(Gravity.CENTER);\n" +
               "        card.setPadding(40, 50, 40, 50);\n" +
               "        GradientDrawable bg = new GradientDrawable();\n" +
               "        bg.setColor(Color.parseColor(\"#0F172A\"));\n" +
               "        bg.setCornerRadius(32f);\n" +
               "        card.setBackground(bg);\n\n" +
               "        TextView title = new TextView(this);\n" +
               "        title.setText(\"⚡ " + appTitle + "\");\n" +
               "        title.setTextColor(Color.parseColor(\"#38BDF8\"));\n" +
               "        title.setTextSize(24f);\n" +
               "        title.setTypeface(Typeface.DEFAULT_BOLD);\n" +
               "        title.setGravity(Gravity.CENTER);\n" +
               "        title.setPadding(0, 0, 0, 24);\n" +
               "        card.addView(title);\n\n" +
               "        TextClock clock = new TextClock(this);\n" +
               "        clock.setTextColor(Color.parseColor(\"#10B981\"));\n" +
               "        clock.setTextSize(48f);\n" +
               "        clock.setTypeface(Typeface.MONOSPACE);\n" +
               "        clock.setGravity(Gravity.CENTER);\n" +
               "        card.addView(clock);\n\n" +
               "        TextView date = new TextView(this);\n" +
               "        SimpleDateFormat sdf = new SimpleDateFormat(\"EEEE, dd MMMM yyyy\", Locale.getDefault());\n" +
               "        date.setText(sdf.format(new Date()));\n" +
               "        date.setTextColor(Color.parseColor(\"#64748B\"));\n" +
               "        date.setTextSize(14f);\n" +
               "        date.setGravity(Gravity.CENTER);\n" +
               "        date.setPadding(0, 16, 0, 0);\n" +
               "        card.addView(date);\n\n" +
               "        layout.addView(card);\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}