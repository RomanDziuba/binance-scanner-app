package com.roma.nativeapp;

﻿package com.roma.nativeapp;

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
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
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
    private int minSdk = 24;
    private int compileSdk = 34;
    private String javaVersion = "17"; // "8" или "17"
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

        // 1. Кнопка компиляции
        btnBuild = createStyledButton(getBuildButtonText(), "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        // 2. Встроенный редактор кода
        btnCodeEditor = createStyledButton("📝 Редактор кода (С подсказками)", "#059669");
        btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
        card.addView(btnCodeEditor);

        // 3. Кнопка загрузки Java / Текстового файла
        btnUploadJava = createStyledButton("📂 Загрузить файл: " + currentJavaFileName, "#0D9488");
        btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
        card.addView(btnUploadJava);

        // 4. Полный сброс репозитория
        btnReset = createStyledButton("🔄 Полный сброс (Шаблон Часы)", "#D97706");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        // 5. Изменить имя и пакет
        btnTitle = createStyledButton("🏷 Имя: " + appTitle + " (" + packageName + ")", "#475569");
        btnTitle.setOnClickListener(v -> showEditTitleDialog());
        card.addView(btnTitle);

        // 6. Изменить версию приложения
        btnVersion = createStyledButton("📦 Версия приложения: " + appVersion, "#475569");
        btnVersion.setOnClickListener(v -> showEditVersionDialog());
        card.addView(btnVersion);

        // 7. Иконка приложения
        btnIcon = createStyledButton("🖼 Иконка приложения (Выбрать файл)", "#7C3AED");
        updateIconButtonPreview();
        btnIcon.setOnClickListener(v -> openIconFilePicker());
        card.addView(btnIcon);

        // 8. Настройки API, SDK и Java
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

        // ЛОГИ
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
        String profileType = javaVersion.equals("8") ? "⚡ Для слабых тел." : "🚀 Для мощных тел.";
        String info = "🏷 Имя: " + appTitle + "\n" +
                      "📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + "\n" +
                      "📂 Репо: " + repoOwner + "/" + repoName + "\n" +
                      "☕ Java: " + javaVersion + " | SDK Target: " + compileSdk + " (minSdk: " + minSdk + ") [" + profileType + "]";
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
        editor.putInt("minSdk", minSdk);
        editor.putInt("compileSdk", compileSdk);
        editor.putString("javaVersion", javaVersion);
        editor.putString("activeCodeContent", activeCodeContent);
        editor.apply();
        updateInfoText();
    }

    // ==================== ВЫБОР ФАЙЛОВ ИЗ СИСТЕМЫ ====================

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

            String code = stringBuilder.toString();
            activeCodeContent = code;
            currentJavaFileName = finalFileName;
            saveSettings();
            btnUploadJava.setText("📂 Файл: " + currentJavaFileName);

            statusTextView.setText("Статус: ⚙️ Отправка кастомного кода...");
            logEditText.setText("Загружен файл: " + finalFileName + "\nОтправка в репозиторий...");

            new Thread(() -> {
                boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Upload code file: " + finalFileName);
                handler.post(() -> {
                    if (success) {
                        statusTextView.setText("Статус: ✅ Файл успешно загружен!");
                        logEditText.setText("Файл " + finalFileName + " отправлен на GitHub.");
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

    // ==================== ПРОДВИНУТЫЙ РЕДАКТОР КОДА ====================

    private void openCodeEditorDialog() {
        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(15, 15, 15, 15);
        editorLayout.setBackgroundColor(Color.parseColor("#090D16"));

        TextView hintTitle = new TextView(this);
        hintTitle.setText("💡 Обучающие подсказки для чайников:");
        hintTitle.setTextColor(Color.parseColor("#F59E0B"));
        hintTitle.setTextSize(12f);
        hintTitle.setTypeface(Typeface.DEFAULT_BOLD);
        editorLayout.addView(hintTitle);

        TextView hintDesc = new TextView(this);
        hintDesc.setText(
            "• package — уникальный адрес программы.\n" +
            "• import — подключение инструментов Android (кнопки, цвета, экраны).\n" +
            "• onCreate() — точка старта вашего экрана.\n" +
            "• setContentView() — вывод интерфейса на экран."
        );
        hintDesc.setTextColor(Color.parseColor("#94A3B8"));
        hintDesc.setTextSize(10f);
        hintDesc.setPadding(0, 4, 0, 10);
        editorLayout.addView(hintDesc);

        HorizontalScrollView snippetScroll = new HorizontalScrollView(this);
        LinearLayout snippetContainer = new LinearLayout(this);
        snippetContainer.setOrientation(LinearLayout.HORIZONTAL);

        String[] snippets = {"+ TextView (Текст)", "+ Button (Кнопка)", "+ Toast (Всплывающее)", "+ Color (Цвет)"};
        String[] codeInserts = {
            "\nTextView tv = new TextView(this);\ntv.setText(\"Привет!\");\nlayout.addView(tv);\n",
            "\nButton btn = new Button(this);\nbtn.setText(\"Нажми меня\");\nlayout.addView(btn);\n",
            "\nToast.makeText(this, \"Сообщение!\", Toast.LENGTH_SHORT).show();\n",
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

        applySyntaxHighlighting(codeInput.getText());
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                applySyntaxHighlighting(s);
            }
        });

        editorLayout.addView(codeInput);

        new AlertDialog.Builder(this)
            .setTitle("📝 Редактор кода")
            .setView(editorLayout)
            .setPositiveButton("Применить и Сохранить", (dialog, which) -> {
                activeCodeContent = codeInput.getText().toString();
                saveSettings();
                Toast.makeText(this, "Код успешно сохранён!", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Отправить на GitHub", (dialog, which) -> {
                activeCodeContent = codeInput.getText().toString();
                saveSettings();
                statusTextView.setText("Статус: ⚙️ Отправка отредактированного кода...");
                new Thread(() -> {
                    boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update Java code via Editor");
                    handler.post(() -> {
                        if (success) {
                            statusTextView.setText("Статус: ✅ Код из редактора загружен на GitHub!");
                            logEditText.setText("Код обновлён в репозитории через редактор.");
                        } else {
                            statusTextView.setText("Статус: ❌ Ошибка загрузки кода");
                        }
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void applySyntaxHighlighting(Editable editable) {
        String text = editable.toString();
        // Очищаем старые подцветки
        ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            editable.removeSpan(span);
        }

        // Подсветка ключевых слов
        Pattern keywords = Pattern.compile("\\b(package|import|public|class|extends|protected|void|override|new|return|if|else|boolean|int|String)\\b");
        Matcher matcher = keywords.matcher(text);
        while (matcher.find()) {
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#F43F5E")), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Подсветка строк
        Pattern strings = Pattern.compile("\".*?\"");
        Matcher strMatcher = strings.matcher(text);
        while (strMatcher.find()) {
            editable.setSpan(new ForegroundColorSpan(Color.parseColor("#10B981")), strMatcher.start(), strMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
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
                        btnBuild.setText(getBuildButtonText());
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
                        logEditText.setText("Workflow успешно запущен. Ожидание сборки...");
                        startMonitoringBuild();
                    });
                } else {
                    handler.post(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                        btnBuild.setText(getBuildButtonText());
                        statusTextView.setText("Статус: ❌ Ошибка запуска (" + responseCode + ")");
                        logEditText.setText("Не удалось запустить workflow. Код ответа: " + responseCode);
                    });
                }
            } catch (final Exception e) {
                handler.post(() -> {
                    btnBuild.setEnabled(true);
                    btnBuild.setBackgroundColor(Color.parseColor("#2563EB"));
                    btnBuild.setText(getBuildButtonText());
                    statusTextView.setText("Статус: ❌ Ошибка сети");
                    logEditText.setText("Исключение: " + e.getMessage());
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
                            while ((line = logBr.readLine()) != null) {
                                logSb.append(line).append("\n");
                            }
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
            .setTitle("Полный сброс")
            .setMessage("Сбросить репозиторий и установить шаблон часов?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                btnReset.setEnabled(false);
                btnReset.setBackgroundColor(Color.parseColor("#78350F"));
                btnReset.setText("🔄 Выполняется полный сброс...");
                statusTextView.setText("Статус: Полный сброс проекта...");
                logEditText.setText("Очистка и установка стандартного шаблона часов...");

                activeCodeContent = generateMainActivity();

                new Thread(() -> {
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
                        btnReset.setText("🔄 Полный сброс (Шаблон Часы)");
                        if (success) {
                            currentJavaFileName = "Шаблон: Часы (MainActivity.java)";
                            saveSettings();
                            btnUploadJava.setText("📂 Файл: " + currentJavaFileName);
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

        // Настройки SDK и версий Java
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
        String javaSetupVersion = javaVersion.equals("8") ? "8" : "17";
        return "name: Build Native Android APK (Java)\n\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - uses: actions/checkout@v4\n" +
               "      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '" + javaSetupVersion + "'\n" +
               "      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.0'\n          cache-disabled: true\n" +
               "      - name: Setup Android SDK & Accept Licenses\n        run: |\n" +
               "          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
               "          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
               "          yes | sdkmanager --licenses || true\n" +
               "          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
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
        String javaCompatibility = javaVersion.equals("8") ? "JavaVersion.VERSION_1_8" : "JavaVersion.VERSION_17";
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n" +
               "        sourceCompatibility " + javaCompatibility + "\n" +
               "        targetCompatibility " + javaCompatibility + "\n" +
               "    }\n}\n\n" +
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
