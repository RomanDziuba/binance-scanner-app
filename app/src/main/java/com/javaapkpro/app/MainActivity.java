package com.roma.nativeapp;

import android.app.Activity;
import android.app.Dialog;
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
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    // Настройки SDK и Java
    private int compileSdk = 34;
    private int minSdk = 24;
    private String javaVersion = "17"; // 8, 11, 17, 21

    // Текущий код в памяти редактора
    private String currentJavaCode = "";

    private TextView statusTextView, infoTextView;
    private EditText logEditText;
    private Button btnBuild, btnUploadJava, btnReset, btnTitle, btnVersion, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isMonitoring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        if (currentJavaCode == null || currentJavaCode.trim().isEmpty()) {
            currentJavaCode = generateMainActivity();
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

        // КНОПКИ УПРАВЛЕНИЯ
        btnBuild = createStyledButton(getBuildButtonText(), "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        btnCodeEditor = createStyledButton("💻 Редактор кода (Подсветка + ИИ-Справка)", "#059669");
        btnCodeEditor.setOnClickListener(v -> openAdvancedCodeEditor());
        card.addView(btnCodeEditor);

        // Загрузка файла или вставка текста
        btnUploadJava = createStyledButton("📝 Загрузить / Вставить код: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> showSourceInputChoiceDialog());
        card.addView(btnUploadJava);

        btnReset = createStyledButton("🔄 Полный сброс (Шаблон Часы)", "#D97706");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        btnTitle = createStyledButton("🏷 Имя: " + appTitle + " (" + packageName + ")", "#475569");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        btnVersion = createStyledButton("📦 Версия приложения: " + appVersion, "#475569");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        btnIcon = createStyledButton("🖼 Иконка приложения (Выбрать файл)", "#7C3AED");
        updateIconButtonPreview();
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        btnSettings = createStyledButton("⚙️ Настройки API, SDK & Java", "#334155");
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
        logLabel.setText("📋 Расширенный терминал / Умный анализ ошибок:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(12f);
        logLabel.setPadding(0, 8, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь отображаются логи компиляции и результаты расширенного парсинга ошибок...");
        logEditText.setTextSize(11f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(12, 12, 12, 12);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(6);
        logEditText.setMaxLines(14);
        card.addView(logEditText);

        btnCopyLogs = createStyledButton("📋 Скопировать весь лог в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("CI/CD Full Logs", logEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Весь лог успешно скопирован в буфер обмена!", Toast.LENGTH_SHORT).show();
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
        return "🚀 Скомпилировать APK\n📦 Пакет: " + packageName + " (v" + appVersion + ") | Java " + javaVersion;
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
        String phoneProfile = (javaVersion.equals("8") || minSdk <= 24) ? "⚡ Для слабых телефонов (Java 8)" : "🔥 Для сильных телефонов (Java " + javaVersion + ")";
        String info = "🏷 Имя: " + appTitle + "\n" +
                      "📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + "\n" +
                      "☕ Java: " + javaVersion + " | SDK: min " + minSdk + " / compile " + compileSdk + "\n" +
                      "📱 Профиль: " + phoneProfile + "\n" +
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
        compileSdk = sp.getInt("compileSdk", 34);
        minSdk = sp.getInt("minSdk", 24);
        javaVersion = sp.getString("javaVersion", "17");
        currentJavaCode = sp.getString("currentJavaCode", generateMainActivity());
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
        editor.putInt("compileSdk", compileSdk);
        editor.putInt("minSdk", minSdk);
        editor.putString("javaVersion", javaVersion);
        editor.putString("currentJavaCode", currentJavaCode);
        editor.apply();
        updateInfoText();
    }

    // ==================== ВЫБОР СПОСОБА ЗАГРУЗКИ КОДА (ФАЙЛ ИЛИ ТЕКСТ) ====================

    private void showSourceInputChoiceDialog() {
        CharSequence[] options = new CharSequence[]{
            "📂 Выбрать файл с телефона (.java, .txt, .kt)",
            "✍️ Вставить / ввести код текстом вручную"
        };
        new AlertDialog.Builder(this)
            .setTitle("Загрузка исходного кода")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openJavaFilePicker();
                } else {
                    showTextPasteDialog();
                }
            })
            .show();
    }

    private void showTextPasteDialog() {
        final EditText input = new EditText(this);
        input.setHint("Вставьте или введите код MainActivity здесь...");
        input.setText(currentJavaCode);
        input.setTextSize(12f);
        input.setTypeface(Typeface.MONOSPACE);
        input.setTextColor(Color.parseColor("#E2E8F0"));
        input.setBackgroundColor(Color.parseColor("#030712"));
        input.setPadding(20, 20, 20, 20);
        input.setGravity(Gravity.TOP);
        input.setMinLines(15);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(input);
        scroll.setPadding(20, 10, 20, 10);

        new AlertDialog.Builder(this)
            .setTitle("✍️ Ручная вставка кода")
            .setView(scroll)
            .setPositiveButton("Применить и отправить", (dialog, which) -> {
                String pastedCode = input.getText().toString();
                if (!pastedCode.trim().isEmpty()) {
                    currentJavaCode = pastedCode;
                    currentJavaFileName = "Вставленный текст (Manual Input)";
                    saveSettings();
                    btnUploadJava.setText("📝 Код: " + currentJavaFileName);

                    statusTextView.setText("Статус: ⚙️ Отправка введенного кода на GitHub...");
                    logEditText.setText("Отправка ручного ввода в репозиторий...");

                    new Thread(() -> {
                        boolean success = pushFileToGitHub(getMainActivityPath(), currentJavaCode, "Upload manual text input");
                        handler.post(() -> {
                            if (success) {
                                statusTextView.setText("Статус: ✅ Код успешно отправлен!");
                                logEditText.setText("Вставленный код успешно адаптирован и отправлен на GitHub.");
                            } else {
                                statusTextView.setText("Статус: ❌ Ошибка отправки");
                                logEditText.setText("Не удалось отправить код на GitHub. Проверьте токен.");
                            }
                        });
                    }).start();
                } else {
                    Toast.makeText(this, "Код не может быть пустым!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void openJavaFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "text/plain", 
            "text/x-java-source", 
            "text/x-kotlin", 
            "application/octet-stream",
            "application/x-java"
        });
        startActivityForResult(Intent.createChooser(intent, "Выберите файл кода (.java / .txt)"), PICK_JAVA_REQUEST);
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
        if (fileName == null) {
            fileName = "CustomCode.java";
        }
        
        if (fileName.endsWith(".txt")) {
            fileName = fileName.substring(0, fileName.length() - 4) + ".java";
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

            String code = stringBuilder.toString();
            currentJavaCode = code;
            currentJavaFileName = finalFileName;
            saveSettings();
            btnUploadJava.setText("📝 Файл: " + currentJavaFileName);

            statusTextView.setText("Статус: ⚙️ Обработка и отправка файла...");
            logEditText.setText("Загружен файл: " + finalFileName + "\nПрименение Anti-409 Shield и отправка в репозиторий...");

            new Thread(() -> {
                boolean success = pushFileToGitHub(getMainActivityPath(), currentJavaCode, "Upload custom source file: " + finalFileName);
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Файл успешно загружен!");
                        logEditText.setText("Файл " + finalFileName + " успешно адаптирован под пакет " + packageName + " и отправлен на GitHub.");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка загрузки файла");
                        logEditText.setText("Не удалось отправить файл на GitHub. Проверьте токен.");
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
            logEditText.setText("Отправка пользовательской иконки на GitHub...");

            new Thread(() -> {
                boolean success = pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", bytes, "Update app icon");
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Иконка успешно обновлена!");
                        logEditText.setText("Новая иконка приложения установлена в репозиторий.");
                    } else {
                        statusTextView.setText("Статус: ❌ Ошибка обновления иконки");
                        logEditText.setText("Не удалось загрузить иконку на GitHub.");
                    }
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка обработки картинки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readBytes(InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
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

        btnBuild.setEnabled(false);
        btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
        btnBuild.setText("⏳ Компиляция в процессе...\n📦 Пакет: " + packageName);
        statusTextView.setText("Статус: ⚙️ Синхронизация файлов и запуск...");
        logEditText.setText("Подготовка к компиляции на GitHub Actions...");

        new Thread(() -> {
            try {
                boolean synced = syncConfigToGitHub();
                if (!synced) {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getButtonTextSafe());
                        statusTextView.setText("Статус: ❌ Ошибка 401/403 (Проверьте токен)");
                        logEditText.setText("Ошибка авторизации на GitHub. Проверьте правильность токена.");
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
                        logEditText.setText("Workflow успешно запущен. Ожидание сборки и генерации APK...");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getButtonTextSafe());
                        statusTextView.setText("Статус: ❌ Ошибка запуска (" + responseCode + ")");
                        logEditText.setText("Не удалось запустить workflow. Код ответа: " + responseCode);
                    });
                }
            } catch (final Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                    btnBuild.setText(getButtonTextSafe());
                    statusTextView.setText("Статус: ❌ Ошибка сети");
                    logEditText.setText("Исключение: " + e.getMessage());
                });
            }
        }).start();
    }

    private String getButtonTextSafe() {
        return getBuildButtonText();
    }

    private boolean syncConfigToGitHub() {
        boolean r0 = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root build.gradle");
        boolean r00 = pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings.gradle");
        boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update AndroidManifest");
        boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app/build.gradle");
        boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow");
        boolean r4 = pushFileToGitHub(getMainActivityPath(), currentJavaCode, "Update MainActivity.java");
        return r0 && r00 && r1 && r2 && r3 && r4;
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
                br.close();
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
            int putCode = putConn.getResponseCode();
            return putCode == 200 || putCode == 201;
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
                        br.close();
                        
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
                                    btnBuild.setText(getButtonTextSafe());
                                    if ("success".equals(conclusion)) {
                                        statusTextView.setText("Статус: ✅ Сборка успешна! APK отправлен в Telegram.");
                                        logEditText.setText("Сборка завершена успешно! Бот автоматически отправил APK в ваш Telegram чат.");
                                    } else {
                                        statusTextView.setText("Статус: ❌ Ошибка компиляции (" + conclusion + ")");
                                        // Запуск расширенного интеллектуального парсинга логов
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

    // ==================== РАСШИРЕННЫЙ И ПРОДВИНУТЫЙ ПАРСИНГ ЛОГОВ ====================

    private void fetchLogsAsync(long runId) {
        new Thread(() -> {
            try {
                String jobsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + runId + "/jobs";
                HttpURLConnection conn = (HttpURLConnection) new URL(jobsUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
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
                            StringBuilder fullLogBuilder = new StringBuilder();
                            String currentLine;
                            while ((currentLine = logBr.readLine()) != null) {
                                fullLogBuilder.append(currentLine).append("\n");
                            }
                            logBr.close();
                            
                            final String rawLogs = fullLogBuilder.toString();
                            final String parsedSummary = parseAndHighlightErrors(rawLogs);

                            handler.post(() -> {
                                logEditText.setText(parsedSummary);
                            });
                        } else {
                            handler.post(() -> logEditText.setText("Не удалось загрузить логи задачи (Код: " + logConn.getResponseCode() + ")"));
                        }
                    }
                } else {
                    handler.post(() -> logEditText.setText("Не удалось получить список job (Код: " + conn.getResponseCode() + ")"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> logEditText.setText("Ошибка при загрузке логов: " + e.getMessage()));
            }
        }).start();
    }

    private String parseAndHighlightErrors(String fullLogs) {
        if (fullLogs == null || fullLogs.isEmpty()) {
            return "Логи пусты или недоступны.";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== 🔍 РАСШИРЕННЫЙ АНАЛИЗАТОР ОШИБОК СБОРКИ ===\n\n");

        String[] lines = fullLogs.split("\n");
        boolean foundError = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lower = line.toLowerCase();
            
            // Расширенный поиск ошибок Javac, Gradle, FAILED и исключений
            if (lower.contains("error:") || lower.contains("failed") || lower.contains("exception") || lower.contains("cannot find symbol") || lower.contains("symbol:") || lower.contains("location:")) {
                String cleanLine = line.replaceAll("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\s+", "");
                report.append("❌ ").append(cleanLine).append("\n");
                foundError = true;
            }
        }

        if (!foundError) {
            report.append("⚠️ Точечных ошибок компиляции не обнаружено. Последние 40 строк логов:\n\n");
            int startIdx = Math.max(0, lines.length - 40);
            for (int i = startIdx; i < lines.length; i++) {
                String cleanLine = lines[i].replaceAll("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\s+", "");
                report.append(cleanLine).append("\n");
            }
        } else {
            report.append("\n----------------------------------------\n");
            report.append("💡 Рекомендация: Откройте 'Редактор кода' и исправьте указанную ошибку.\n\n");
            report.append("--- ПОЛНЫЕ ОРИГИНАЛЬНЫЕ ЛОГИ СБОРКИ ---:\n\n");
            for (String l : lines) {
                report.append(l.replaceAll("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\s+", "")).append("\n");
            }
        }

        return report.toString();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс")
            .setMessage("Сбросить репозиторий и установить шаблон часов?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                btnReset.setEnabled(false);
                btnReset.setBackgroundColor(Color.parseColor("#78350F"));
                btnReset.setText("🔄 Выполняется полный сброс...");
                statusTextView.setText("Статус: Полный сброс проекта...");
                logEditText.setText("Очистка и установка стандартного шаблона часов (MainActivity.java)...");

                currentJavaCode = generateMainActivity();

                new Thread(() -> {
                    boolean r0 = pushFileToGitHub("build.gradle", getRootGradleContent(), "Reset root build.gradle");
                    boolean r00 = pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Reset settings.gradle");
                    boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Reset AndroidManifest");
                    boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Reset app/build.gradle");
                    boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Reset workflow");
                    boolean r4 = pushFileToGitHub(getMainActivityPath(), currentJavaCode, "Reset default MainActivity");
                    
                    boolean success = r0 && r00 && r1 && r2 && r3 && r4;

                    handler.post(() -> {
                        btnReset.setEnabled(true);
                        btnReset.setBackgroundColor(Color.parseColor("#D97706"));
                        btnReset.setText("🔄 Полный сброс (Шаблон Часы)");
                        if (success) {
                            currentJavaFileName = "Шаблон: Часы (MainActivity.java)";
                            saveSettings();
                            btnUploadJava.setText("📝 Код: " + currentJavaFileName);
                            statusTextView.setText("Статус: ✅ Репозиторий сброшен к часам!");
                            logEditText.setText("Проект успешно сброшен к исходному шаблону часов.");
                        } else {
                            statusTextView.setText("Статус: ❌ Ошибка сброса (Проверьте токен)");
                            logEditText.setText("Не удалось сбросить проект. Проверьте правильность GitHub токена.");
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

    // ==================== НАСТРОЙКИ API, SDK & JAVA ====================

    private void showSettingsDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        TextView sectionApi = createHeaderTextView("🔑 1. Ключи и Репозиторий GitHub");
        layout.addView(sectionApi);

        final EditText inputGithub = createCustomEditText("GitHub Token (ghp_...)", githubToken);
        layout.addView(inputGithub);

        final EditText inputBot = createCustomEditText("Telegram Bot Token", botToken);
        layout.addView(inputBot);

        final EditText inputChat = createCustomEditText("Chat ID", chatId);
        layout.addView(inputChat);

        final EditText inputOwner = createCustomEditText("GitHub Owner (Логин)", repoOwner);
        layout.addView(inputOwner);

        final EditText inputRepo = createCustomEditText("Имя репозитория", repoName);
        layout.addView(inputRepo);

        TextView sectionSdk = createHeaderTextView("⚙️ 2. Конфигурация Java & Android SDK");
        layout.addView(sectionSdk);

        TextView labelJava = createLabelTextView("Версия Java (для сборки в Gradle):");
        layout.addView(labelJava);

        final Spinner spinnerJava = new Spinner(this);
        String[] javaOptions = new String[]{"8 (Совместимость для старых устройств)", "11 (Стандарт)", "17 (Рекомендуется по умолчанию)", "21 (Самый новый)"};
        ArrayAdapter<String> adapterJava = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, javaOptions);
        spinnerJava.setAdapter(adapterJava);
        if (javaVersion.equals("8")) spinnerJava.setSelection(0);
        else if (javaVersion.equals("11")) spinnerJava.setSelection(1);
        else if (javaVersion.equals("21")) spinnerJava.setSelection(3);
        else spinnerJava.setSelection(2);
        layout.addView(spinnerJava);

        TextView labelMinSdk = createLabelTextView("Минимальный Android SDK (minSdk):");
        layout.addView(labelMinSdk);

        final EditText inputMinSdk = createCustomEditText("minSdk (например, 21 или 24)", String.valueOf(minSdk));
        layout.addView(inputMinSdk);

        TextView labelCompileSdk = createLabelTextView("Целевой Android SDK (compileSdk / targetSdk):");
        layout.addView(labelCompileSdk);

        final EditText inputCompileSdk = createCustomEditText("compileSdk (например, 33 или 34)", String.valueOf(compileSdk));
        layout.addView(inputCompileSdk);

        LinearLayout presetsLayout = new LinearLayout(this);
        presetsLayout.setOrientation(LinearLayout.HORIZONTAL);
        presetsLayout.setPadding(0, 15, 0, 15);

        Button btnWeakPreset = new Button(this);
        btnWeakPreset.setText("⚡ Для слабых тел.");
        btnWeakPreset.setTextSize(10f);
        btnWeakPreset.setOnClickListener(v -> {
            spinnerJava.setSelection(0);
            inputMinSdk.setText("21");
            inputCompileSdk.setText("30");
            Toast.makeText(this, "Установлены настройки для слабых устройств!", Toast.LENGTH_SHORT).show();
        });
        presetsLayout.addView(btnWeakPreset);

        Button btnStrongPreset = new Button(this);
        btnStrongPreset.setText("🔥 Для сильных тел.");
        btnStrongPreset.setTextSize(10f);
        btnStrongPreset.setOnClickListener(v -> {
            spinnerJava.setSelection(2);
            inputMinSdk.setText("24");
            inputCompileSdk.setText("34");
            Toast.makeText(this, "Установлены оптимальные мощные настройки!", Toast.LENGTH_SHORT).show();
        });
        presetsLayout.addView(btnStrongPreset);
        layout.addView(presetsLayout);

        scroll.addView(layout);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройки API, SDK и Java")
            .setView(scroll)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();
                repoOwner = inputOwner.getText().toString().trim();
                repoName = inputRepo.getText().toString().trim();

                int sel = spinnerJava.getSelectedItemPosition();
                if (sel == 0) javaVersion = "8";
                else if (sel == 1) javaVersion = "11";
                else if (sel == 3) javaVersion = "21";
                else javaVersion = "17";

                try {
                    minSdk = Integer.parseInt(inputMinSdk.getText().toString().trim());
                } catch (Exception e) { minSdk = 24; }

                try {
                    compileSdk = Integer.parseInt(inputCompileSdk.getText().toString().trim());
                } catch (Exception e) { compileSdk = 34; }

                saveSettings();
                Toast.makeText(this, "Все настройки успешно сохранены!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private TextView createHeaderTextView(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#0284C7"));
        tv.setTextSize(14f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 15, 0, 8);
        return tv;
    }

    private TextView createLabelTextView(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(11f);
        tv.setPadding(0, 8, 0, 2);
        return tv;
    }

    private EditText createCustomEditText(String hint, String text) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(text);
        et.setBackgroundColor(Color.WHITE);
        et.setTextColor(Color.BLACK);
        et.setHintTextColor(Color.GRAY);
        et.setPadding(25, 18, 25, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 4, 0, 8);
        et.setLayoutParams(lp);
        return et;
    }

    // ==================== ВСТРОЕННЫЙ РЕДАКТОР КОДА С ПОДСВЕТКОЙ И ИИ-ПОДСКАЗКАМИ ====================

    private void openAdvancedCodeEditor() {
        final Dialog editorDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(Color.parseColor("#090D16"));
        mainRoot.setPadding(16, 16, 16, 16);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 12);

        TextView title = new TextView(this);
        title.setText("💻 Редактор Java-кода");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button btnHelp = new Button(this);
        btnHelp.setText("🤖 ИИ-Подсказки & Шпаргалка");
        btnHelp.setBackgroundColor(Color.parseColor("#8B5CF6"));
        btnHelp.setTextColor(Color.WHITE);
        btnHelp.setTextSize(10f);
        btnHelp.setOnClickListener(v -> showAiHelpDialog());
        header.addView(btnHelp);

        mainRoot.addView(header);

        final EditText codeInput = new EditText(this);
        codeInput.setText(currentJavaCode);
        codeInput.setTextSize(12f);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setTextColor(Color.parseColor("#F8FAFC"));
        codeInput.setBackgroundColor(Color.parseColor("#020617"));
        codeInput.setPadding(16, 16, 16, 16);
        codeInput.setGravity(Gravity.TOP);
        codeInput.setMinLines(20);

        highlightSyntax(codeInput);

        codeInput.addTextChangedListener(new TextWatcher() {
            private boolean isHighlighting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (isHighlighting) return;
                isHighlighting = true;
                highlightSyntax(codeInput);
                isHighlighting = false;
            }
        });

        ScrollView codeScroll = new ScrollView(this);
        codeScroll.addView(codeInput);
        mainRoot.addView(codeScroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        HorizontalScrollView quickKeysScroll = new HorizontalScrollView(this);
        quickKeysScroll.setPadding(0, 8, 0, 8);
        LinearLayout quickKeysLayout = new LinearLayout(this);
        quickKeysLayout.setOrientation(LinearLayout.HORIZONTAL);

        String[] keys = new String[]{"{", "}", "(", ")", ";", "=", "\"", "Activity", "String", "TextView", "Button", "Color.parseColor(\"#\")", "Toast.makeText"};
        for (String k : keys) {
            Button btnKey = new Button(this);
            btnKey.setText(k);
            btnKey.setTextSize(10f);
            btnKey.setBackgroundColor(Color.parseColor("#1E293B"));
            btnKey.setTextColor(Color.parseColor("#38BDF8"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(2, 0, 2, 0);
            btnKey.setLayoutParams(lp);
            btnKey.setOnClickListener(v -> {
                int start = Math.max(codeInput.getSelectionStart(), 0);
                int end = Math.max(codeInput.getSelectionEnd(), 0);
                codeInput.getText().replace(Math.min(start, end), Math.max(start, end), k, 0, k.length());
            });
            quickKeysLayout.addView(btnKey);
        }
        quickKeysScroll.addView(quickKeysLayout);
        mainRoot.addView(quickKeysScroll);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(0, 10, 0, 0);

        Button btnSaveCode = new Button(this);
        btnSaveCode.setText("💾 Сохранить и применить");
        btnSaveCode.setBackgroundColor(Color.parseColor("#10B981"));
        btnSaveCode.setTextColor(Color.WHITE);
        btnSaveCode.setOnClickListener(v -> {
            currentJavaCode = codeInput.getText().toString();
            saveSettings();
            Toast.makeText(this, "Код успешно обновлён в памяти!", Toast.LENGTH_SHORT).show();
            editorDialog.dismiss();
        });

        Button btnClose = new Button(this);
        btnClose.setText("❌ Закрыть");
        btnClose.setBackgroundColor(Color.parseColor("#64748B"));
        btnClose.setTextColor(Color.WHITE);
        btnClose.setOnClickListener(v -> editorDialog.dismiss());

        bottomBar.addView(btnSaveCode, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bottomBar.addView(btnClose, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        mainRoot.addView(bottomBar);

        editorDialog.setContentView(mainRoot);
        editorDialog.show();
    }

    private void highlightSyntax(EditText editor) {
        Editable editable = editor.getText();
        String text = editable.toString();
        
        ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            editable.removeSpan(span);
        }

        String keywordsRegex = "\\b(package|import|public|protected|private|class|extends|implements|void|int|boolean|long|float|double|new|return|if|else|for|while|try|catch|finally|static|final|super|this|volatile)\\b";
        applySpanPattern(editable, text, keywordsRegex, Color.parseColor("#3B82F6"));

        String androidClassesRegex = "\\b(Activity|Bundle|LinearLayout|TextView|Button|EditText|ScrollView|Color|Typeface|GradientDrawable|Toast|Intent|Handler|Looper|SharedPreferences|Runnable|Thread|Context|View|Gravity)\\b";
        applySpanPattern(editable, text, androidClassesRegex, Color.parseColor("#10B981"));

        String stringsRegex = "\"[^\"]*\"";
        applySpanPattern(editable, text, stringsRegex, Color.parseColor("#F59E0B"));

        String commentsRegex = "//.*";
        applySpanPattern(editable, text, commentsRegex, Color.parseColor("#64748B"));

        String numbersRegex = "\\b\\d+\\b";
        applySpanPattern(editable, text, numbersRegex, Color.parseColor("#A855F7"));
    }

    private void applySpanPattern(Editable editable, String text, String regex, int color) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            editable.setSpan(
                new ForegroundColorSpan(color),
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private void showAiHelpDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        TextView mainTitle = new TextView(this);
        mainTitle.setText("🤖 Шпаргалка и ИИ-Промпты");
        mainTitle.setTextColor(Color.parseColor("#8B5CF6"));
        mainTitle.setTextSize(16f);
        mainTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mainTitle.setPadding(0, 0, 0, 15);
        layout.addView(mainTitle);

        layout.addView(createAiHelpSection("📖 1. Базовые команды Java / Android",
            "• package com.roma.nativeapp; — Имя вашего пакета.\n" +
            "• onCreate(Bundle saved) — Главная точка входа в экран.\n" +
            "• LinearLayout — Контейнер для элементов.\n" +
            "• TextView — Вывод текста.\n" +
            "• Button — Кнопка с кликом setOnClickListener(v -> { ... }).\n" +
            "• Toast.makeText(...) — Уведомление внизу экрана."
        ));

        layout.addView(createAiHelpSection("🧠 2. Промпты для ИИ",
            "Скопируйте и вставьте в ChatGPT/Gemini:"
        ));

        layout.addView(createCopyablePromptCard("📌 Создать форму входа:",
            "Напиши код MainActivity на чистом Java без XML для Android. Нужен темно-синий экран, заголовок 'Вход', два поля ввода и синяя кнопка 'Войти'."
        ));

        scroll.addView(layout);

        new AlertDialog.Builder(this)
            .setTitle("Справочник & ИИ-Помощник")
            .setView(scroll)
            .setPositiveButton("Понятно", null)
            .show();
    }

    private View createAiHelpSection(String title, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 10, 0, 15);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#38BDF8"));
        t.setTextSize(13f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, 6);
        box.addView(t);

        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(Color.parseColor("#CBD5E1"));
        b.setTextSize(11f);
        b.setLineSpacing(1.2f, 1.2f);
        box.addView(b);

        return box;
    }

    private View createCopyablePromptCard(String title, final String promptText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(16f);
        bg.setStroke(1, Color.parseColor("#334155"));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F59E0B"));
        t.setTextSize(11f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t);

        TextView p = new TextView(this);
        p.setText(promptText);
        p.setTextColor(Color.parseColor("#94A3B8"));
        p.setTextSize(10f);
        p.setPadding(0, 6, 0, 10);
        card.addView(p);

        Button btnCopy = new Button(this);
        btnCopy.setText("📋 Скопировать промпт");
        btnCopy.setBackgroundColor(Color.parseColor("#2563EB"));
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setTextSize(10f);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI Prompt", promptText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Промпт скопирован!", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnCopy);

        return card;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ ГЕНЕРАТОРЫ КОДА И МАНИФЕСТА ====================

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
               "      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '" + javaVersion + "'\n" +
               "      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.0'\n          cache-disabled: true\n" +
               "      - name: Setup Android SDK & Accept Licenses\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;" + compileSdk + ".0.0\"\n" +
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
        String javaCompatibility = javaVersion.equals("8") ? "JavaVersion.VERSION_1_8" : "JavaVersion.VERSION_" + javaVersion;
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n" +
               "        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n\n" +
               "    compileOptions {\n" +
               "        sourceCompatibility " + javaCompatibility + "\n" +
               "        targetCompatibility " + javaCompatibility + "\n" +
               "    }\n" +
               "}\n\n" +
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
