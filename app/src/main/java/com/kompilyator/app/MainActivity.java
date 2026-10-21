package com.kompilyator.app;

import android.view.*;
import java.util.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
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

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfig";
    private static final int PICK_FILE_REQUEST = 1001;

    private String githubToken = "";
    private String botToken = "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    private String appTitle = "RomaClockApp";
    private String appVersion = "1.0";
    private String packageName = "com.roma.nativeapp";

    private TextView infoTextView, clockTextView, logTextView;
    private Button btnBuild, btnStatus, btnReset, btnCodeInput, btnSettings, btnCopyLogs;
    private ImageView iconPreview;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private StringBuilder logBuffer = new StringBuilder();
    private volatile boolean isOperationRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        // Главный контейнер
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(25, 25, 25, 25);

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

        // 2. Живые часы с текущим временем
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#10B981"));
        clockTextView.setTextSize(18f);
        clockTextView.setTypeface(Typeface.MONOSPACE);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(0, 0, 0, 15);
        startClockTicker();
        container.addView(clockTextView);

        // Карточка панели управления
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 35, 30, 35);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(28f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        // Заголовок и мини-иконка
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 0, 0, 15);

        iconPreview = new ImageView(this);
        iconPreview.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        iconPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        headerLayout.addView(iconPreview);

        TextView titleView = new TextView(this);
        titleView.setText("  Android CI/CD Controller");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(18f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        headerLayout.addView(titleView);
        card.addView(headerLayout);

        // Инфо-блок о проекте
        infoTextView = new TextView(this);
        updateInfoText();
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(13f);
        infoTextView.setPadding(20, 15, 20, 15);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(14f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 15);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        // Кнопки управления
        btnBuild = createStyledButton("🚀 Скомпилировать APK (GitHub)", "#2563EB");
        btnBuild.setOnClickListener(v -> triggerGitHubBuild());
        card.addView(btnBuild);

        btnStatus = createStyledButton("📊 Проверить статус и файлы", "#334155");
        btnStatus.setOnClickListener(v -> checkRepoAndStatus());
        card.addView(btnStatus);

        btnReset = createStyledButton("🔄 Полный сброс (Часы + Фонарик)", "#D97706");
        btnReset.setOnClickListener(v -> confirmResetProject());
        card.addView(btnReset);

        btnCodeInput = createStyledButton("📝 Загрузить файл или вставить Java код", "#0284C7");
        btnCodeInput.setOnClickListener(v -> showCodeInputDialog());
        card.addView(btnCodeInput);

        btnSettings = createStyledButton("⚙️ Настройки API и токенов", "#0F766E");
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        card.addView(btnSettings);

        // Терминал логов
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Терминал логов и ошибок:");
        logTitle.setTextColor(Color.parseColor("#CBD5E1"));
        logTitle.setTextSize(13f);
        logTitle.setTypeface(Typeface.DEFAULT_BOLD);
        logTitle.setPadding(0, 15, 0, 5);
        card.addView(logTitle);

        logTextView = new TextView(this);
        logTextView.setText("Система готова к работе.\n");
        logTextView.setTextColor(Color.parseColor("#34D399"));
        logTextView.setTextSize(11f);
        logTextView.setTypeface(Typeface.MONOSPACE);
        logTextView.setPadding(15, 15, 15, 15);

        GradientDrawable logBg = new GradientDrawable();
        logBg.setColor(Color.parseColor("#020617"));
        logBg.setCornerRadius(12f);
        logBg.setStroke(1, Color.parseColor("#334155"));
        logTextView.setBackground(logBg);

        LinearLayout.LayoutParams logParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 200
        );
        logParams.setMargins(0, 0, 0, 10);
        logTextView.setLayoutParams(logParams);
        
        ScrollView logScroll = new ScrollView(this);
        logScroll.setLayoutParams(logParams);
        logScroll.addView(logTextView);
        card.addView(logScroll);

        btnCopyLogs = createStyledButton("📋 Скопировать логи в буфер", "#475569");
        btnCopyLogs.setOnClickListener(v -> copyLogsToClipboard());
        card.addView(btnCopyLogs);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        appendLog("Приложение запущено. Проверьте настройки GitHub токена.");
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(20, 15, 20, 15);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        btn.setLayoutParams(params);
        return btn;
    }

    private void setButtonsEnabled(boolean enabled) {
        isOperationRunning = !enabled;
        btnBuild.setEnabled(enabled);
        btnStatus.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnCodeInput.setEnabled(enabled);
        btnSettings.setEnabled(enabled);
        
        String dimColor = enabled ? "#2563EB" : "#475569";
        btnBuild.setBackgroundColor(Color.parseColor(dimColor));
    }

    private void appendLog(String text) {
        handler.post(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String line = "[" + time + "] " + text + "\n";
            logBuffer.append(line);
            logTextView.setText(logBuffer.toString());
        });
    }

    private void copyLogsToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("CI/CD Logs", logBuffer.toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Логи скопированы в буфер обмена!", Toast.LENGTH_SHORT).show();
    }

    private void startClockTicker() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String timeStr = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("🕒 " + timeStr);
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void updateInfoText() {
        String info = "🏷 Имя: " + appTitle + "\n" +
                      "📦 Пакет: " + packageName + "\n" +
                      "🔢 Версия: " + appVersion + "\n" +
                      "📂 Репо: " + repoOwner + "/" + repoName;
        if (infoTextView != null) {
            infoTextView.setText(info);
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
        editor.apply();
        updateInfoText();
    }

    private String getMainActivityPath() {
        return "app/src/main/java/" + packageName.replace('.', '/') + "/MainActivity.java";
    }

    // --- СЕТЕВЫЕ ОПЕРАЦИИ ГИТХАБ ---

    private void triggerGitHubBuild() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Сначала введите GitHub токен в настройках!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        setButtonsEnabled(false);
        appendLog("🚀 Запуск процесса синхронизации и компиляции...");

        new Thread(() -> {
            try {
                appendLog("📤 Синхронизация файлов конфигурации с GitHub...");
                boolean synced = syncConfigToGitHub();
                if (!synced) {
                    appendLog("❌ Ошибка синхронизации (Проверьте GitHub токен и права repo/workflow)");
                    setButtonsEnabled(true);
                    return;
                }

                appendLog("⚡ Запуск Workflow (dispatches)...");
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
                    appendLog("✅ Сборка успешно поставлена в очередь GitHub Actions!");
                    startMonitoringBuild();
                } else {
                    appendLog("❌ Ошибка запуска сборки. Код ответа: " + responseCode);
                    setButtonsEnabled(true);
                }
            } catch (Exception e) {
                appendLog("❌ Исключение при сборке: " + e.getMessage());
                setButtonsEnabled(true);
            }
        }).start();
    }

    private boolean syncConfigToGitHub() {
        boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update AndroidManifest");
        boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app/build.gradle");
        boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow");
        boolean r4 = pushFileToGitHub(getMainActivityPath(), generateClockFlashlightMainActivity(), "Update MainActivity clock+flashlight");
        return r1 && r2 && r3 && r4;
    }

    private boolean pushFileToGitHub(String path, String content, String message) {
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
            }

            HttpURLConnection putConn = (HttpURLConnection) url.openConnection();
            putConn.setRequestMethod("PUT");
            putConn.setRequestProperty("Authorization", "Bearer " + githubToken);
            putConn.setRequestProperty("Accept", "application/vnd.github+json");
            putConn.setRequestProperty("Content-Type", "application/json; utf-8");
            putConn.setDoOutput(true);

            String encodedContent = android.util.Base64.encodeToString(content.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            JSONObject payload = new JSONObject();
            payload.put("message", message);
            payload.put("content", encodedContent);
            payload.put("branch", "main");
            if (sha != null) payload.put("sha", sha);

            try (OutputStream os = putConn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int putCode = putConn.getResponseCode();
            appendLog("📁 Файл " + path + " -> Статус: " + putCode);
            return putCode == 200 || putCode == 201;
        } catch (Exception e) {
            appendLog("❌ Ошибка отправки файла " + path + ": " + e.getMessage());
            return false;
        }
    }

    private void startMonitoringBuild() {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            boolean monitoring = true;
            while (monitoring) {
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
                            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                            if ("completed".equals(status)) {
                                monitoring = false;
                                handler.post(() -> setButtonsEnabled(true));
                                if ("success".equals(conclusion)) {
                                    appendLog("✅ Сборка успешно завершена! APK отправлен в Telegram.");
                                } else {
                                    appendLog("❌ Сборка завершилась с ошибкой: " + conclusion);
                                }
                            } else {
                                appendLog("⚙️ Статус сборки: " + status + " (" + elapsed + "с)");
                            }
                        }
                    }
                } catch (Exception e) {
                    appendLog("⚠️ Ошибка мониторинга: " + e.getMessage());
                }
            }
        }).start();
    }

    private void checkRepoAndStatus() {
        if (githubToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите GitHub токен!", Toast.LENGTH_SHORT).show();
            return;
        }
        appendLog("📊 Проверка соединения с репозиторием и статуса...");
        new Thread(() -> {
            try {
                String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
                
                int code = conn.getResponseCode();
                if (code == 200) {
                    appendLog("✅ Репозиторий доступен (" + repoOwner + "/" + repoName + ")");
                } else {
                    appendLog("❌ Ошибка доступа к репозиторию. Код: " + code);
                }
            } catch (Exception e) {
                appendLog("❌ Ошибка сети: " + e.getMessage());
            }
        }).start();
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс")
            .setMessage("Сбросить репозиторий к шаблону (Часы + Фонарик)?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                setButtonsEnabled(false);
                appendLog("🔄 Выполняется полный сброс репозитория...");
                new Thread(() -> {
                    boolean success = syncConfigToGitHub();
                    handler.post(() -> {
                        setButtonsEnabled(true);
                        if (success) {
                            appendLog("✅ Проект успешно сброшен к часам и фонарику!");
                        } else {
                            appendLog("❌ Ошибка сброса проекта.");
                        }
                    });
                }).start();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    // --- ВВОД И ОБРАБОТКА СВОЕГО JAVA КОДА ---

    private void showCodeInputDialog() {
        CharSequence[] options = {"📁 Выбрать файл .java", "✍️ Вставить текст кода вручную"};
        new AlertDialog.Builder(this)
            .setTitle("Загрузка Java кода")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, PICK_FILE_REQUEST);
                } else {
                    showPasteCodeDialog();
                }
            })
            .show();
    }

    private void showPasteCodeDialog() {
        final EditText input = new EditText(this);
        input.setHint("Вставьте ваш код MainActivity здесь...");
        input.setBackgroundColor(Color.WHITE);
        input.setTextColor(Color.BLACK);
        input.setPadding(25, 20, 25, 20);

        new AlertDialog.Builder(this)
            .setTitle("Вставка Java кода")
            .setView(input)
            .setPositiveButton("Загрузить", (dialog, which) -> {
                String code = input.getText().toString();
                processAndPushCustomCode(code, "pasted_MainActivity.java");
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                processAndPushCustomCode(sb.toString(), "uploaded_file.java");
            } catch (Exception e) {
                appendLog("❌ Ошибка чтения файла: " + e.getMessage());
            }
        }
    }

    private void processAndPushCustomCode(String codeStr, String fileName) {
        // Предкомпиляционная проверка синтаксиса
        if (!codeStr.contains("class MainActivity") && !codeStr.contains("class ")) {
            appendLog("⚠️ Внимание: В коде не найден класс MainActivity. Возможна ошибка компиляции!");
        }
        if (!codeStr.contains("package ")) {
            codeStr = "package " + packageName + ";\n\n" + codeStr;
            appendLog("ℹ️ Автоматически добавлен пакет: " + packageName);
        }

        setButtonsEnabled(false);
        appendLog("📝 Обработка и отправка файла " + fileName + "...");

        final String finalCode = codeStr;
        new Thread(() -> {
            boolean success = pushFileToGitHub(getMainActivityPath(), finalCode, "Update MainActivity with custom code");
            handler.post(() -> {
                setButtonsEnabled(true);
                if (success) {
                    appendLog("✅ Пользовательский Java код успешно отправлен на GitHub!");
                } else {
                    appendLog("❌ Ошибка отправки кода на GitHub.");
                }
            });
        }).start();
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
        inputGithub.setPadding(25, 15, 25, 15);
        layout.addView(inputGithub);

        final EditText inputBot = new EditText(this);
        inputBot.setHint("Telegram Bot Token");
        inputBot.setText(botToken);
        inputBot.setBackgroundColor(Color.WHITE);
        inputBot.setTextColor(Color.BLACK);
        inputBot.setPadding(25, 15, 25, 15);
        layout.addView(inputBot);

        final EditText inputChat = new EditText(this);
        inputChat.setHint("Telegram Chat ID");
        inputChat.setText(chatId);
        inputChat.setBackgroundColor(Color.WHITE);
        inputChat.setTextColor(Color.BLACK);
        inputChat.setPadding(25, 15, 25, 15);
        layout.addView(inputChat);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Имя приложения (App Title)");
        inputTitle.setText(appTitle);
        inputTitle.setBackgroundColor(Color.WHITE);
        inputTitle.setTextColor(Color.BLACK);
        inputTitle.setPadding(25, 15, 25, 15);
        layout.addView(inputTitle);

        new AlertDialog.Builder(this)
            .setTitle("Настройки API и проекта")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                githubToken = inputGithub.getText().toString().trim();
                botToken = inputBot.getText().toString().trim();
                chatId = inputChat.getText().toString().trim();
                appTitle = inputTitle.getText().toString().trim();
                packageName = transliterate(appTitle);
                saveSettings();
                appendLog("✅ Настройки успешно сохранены!");
            })
            .setNegativeButton("Отмена", null)
            .show();
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

    // --- ШАБЛОНЫ ФАЙЛОВ ДЛЯ ГИТХАБА ---

    private String getWorkflowContent() {
        return "name: Build Native Android APK (Java)\n\n" +
               "on:\n  workflow_dispatch:\n\n" +
               "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
               "      - uses: actions/checkout@v4\n" +
               "      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n" +
               "      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n" +
               "      - name: Build APK\n        run: gradle clean assembleDebug --no-daemon\n" +
               "      - name: Upload APK to Telegram\n        run: |\n" +
               "          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
               "          curl -F chat_id=\"" + chatId + "\" -F document=@\"$APK_PATH\" https://api.telegram.org/bot" + botToken + "/sendDocument\n";
    }

    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n" +
               "    <application android:allowBackup=\"true\" android:label=\"" + appTitle + "\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
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
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n" +
               "    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }

    private String generateClockFlashlightMainActivity() {
        return "package " + packageName + ";\n\n" +
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
               "        Button btnFlash = new Button(this);\n" +
               "        btnFlash.setText(\"🔦 Вспышка / Фонарик\");\n" +
               "        btnFlash.setBackgroundColor(Color.parseColor(\"#D97706\"));\n" +
               "        btnFlash.setTextColor(Color.WHITE);\n" +
               "        btnFlash.setPadding(30, 20, 30, 20);\n" +
               "        btnFlash.setOnClickListener(new View.OnClickListener() {\n" +
               "            @Override\n" +
               "            public void onClick(View v) {\n" +
               "                try {\n" +
               "                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);\n" +
               "                    String camId = cm.getCameraIdList()[0];\n" +
               "                    flashOn = !flashOn;\n" +
               "                    cm.setTorchMode(camId, flashOn);\n" +
               "                    Toast.makeText(MainActivity.this, flashOn ? \"Фонарик ВКЛ\" : \"Фонарик ВЫКЛ\", Toast.LENGTH_SHORT).show();\n" +
               "                } catch (Exception e) {\n" +
               "                    Toast.makeText(MainActivity.this, \"Ошибка фонарика\", Toast.LENGTH_SHORT).show();\n" +
               "                }\n" +
               "            }\n" +
               "        });\n" +
               "        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);\n" +
               "        lp.setMargins(0, 30, 0, 0);\n" +
               "        btnFlash.setLayoutParams(lp);\n" +
               "        card.addView(btnFlash);\n\n" +
               "        layout.addView(card);\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}