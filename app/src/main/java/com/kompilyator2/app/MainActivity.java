package com.kompilyator2.app;

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
import android.hardware.camera2.CameraManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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

    private String githubToken = "";
    private String botToken = "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    private String appTitle = "RomaClockApp";
    private String appVersion = "1.0";
    private String packageName = "com.roma.nativeapp";

    private TextView statusTextView, infoTextView;
    private Button btnBuild, btnReset, btnSettings, btnTitle, btnVersion, btnCodeInput, btnLogs;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isOperationActive = false;
    private String lastErrorLog = "Ошибок пока нет. Система работает стабильно.";
    
    private boolean flashOn = false;
    private boolean isDimmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(24, 24, 24, 24);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(10, 10, 10, 10);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Created by Dziuba Roman ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(16f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 15);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 35, 30, 35);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(28f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ Android CI/CD Studio");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 16);
        card.addView(titleView);

        infoTextView = new TextView(this);
        updateInfoText();
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(13f);
        infoTextView.setPadding(20, 16, 20, 16);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(16f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 16);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        btnBuild = createStyledButton("🚀 Скомпилировать APK (GitHub)", "#2563EB");
        btnBuild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOperationActive) triggerGitHubBuild();
            }
        });
        card.addView(btnBuild);

        btnCodeInput = createStyledButton("📝 Вставить / Загрузить Java код", "#475569");
        btnCodeInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCodeInputDialog();
            }
        });
        card.addView(btnCodeInput);

        btnReset = createStyledButton("🔄 Полный сброс (Шаблон: Часы)", "#D97706");
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOperationActive) confirmResetProject();
            }
        });
        card.addView(btnReset);

        btnTitle = createStyledButton("🏷 Изменить имя приложения", "#334155");
        btnTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTitleDialog();
            }
        });
        card.addView(btnTitle);

        btnVersion = createStyledButton("📦 Изменить версию", "#334155");
        btnVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditVersionDialog();
            }
        });
        card.addView(btnVersion);

        btnSettings = createStyledButton("⚙️ Конфигурация API и Токенов", "#0F766E");
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
        card.addView(btnSettings);

        btnLogs = createStyledButton("📋 Просмотр и копирование логов", "#7C3AED");
        btnLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogsDialog();
            }
        });
        card.addView(btnLogs);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(13f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 16, 0, 5);
        card.addView(statusTextView);

        container.addView(card);

        LinearLayout clockCard = new LinearLayout(this);
        clockCard.setOrientation(LinearLayout.VERTICAL);
        clockCard.setGravity(Gravity.CENTER);
        clockCard.setPadding(30, 30, 30, 30);
        
        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColor(Color.parseColor("#0F172A"));
        clockBg.setCornerRadius(28f);
        clockBg.setStroke(2, Color.parseColor("#1E293B"));
        clockCard.setBackground(clockBg);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(36f);
        clock.setTypeface(Typeface.MONOSPACE);
        clock.setGravity(Gravity.CENTER);
        clockCard.addView(clock);

        TextView dateView = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        dateView.setText(sdf.format(new Date()));
        dateView.setTextColor(Color.parseColor("#64748B"));
        dateView.setTextSize(13f);
        dateView.setGravity(Gravity.CENTER);
        dateView.setPadding(0, 8, 0, 20);
        clockCard.addView(dateView);

        LinearLayout funLayout = new LinearLayout(this);
        funLayout.setOrientation(LinearLayout.HORIZONTAL);
        funLayout.setGravity(Gravity.CENTER);

        Button btnFlash = new Button(this);
        btnFlash.setText("🔦 Фонарик");
        btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setTextSize(12f);
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String cameraId = camManager.getCameraIdList()[0];
                    flashOn = !flashOn;
                    camManager.setTorchMode(cameraId, flashOn);
                    Toast.makeText(MainActivity.this, flashOn ? "Фонарик ВКЛ" : "Фонарик ВЫКЛ", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Вспышка недоступна", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnBright = new Button(this);
        btnBright.setText("🌙 Яркость");
        btnBright.setBackgroundColor(Color.parseColor("#334155"));
        btnBright.setTextColor(Color.WHITE);
        btnBright.setTextSize(12f);
        btnBright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDimmed = !isDimmed;
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = isDimmed ? 0.05f : -1f;
                getWindow().setAttributes(lp);
                Toast.makeText(MainActivity.this, isDimmed ? "Ночной режим" : "Обычная яркость", Toast.LENGTH_SHORT).show();
            }
        });

        funLayout.addView(btnFlash);
        View space = new View(MainActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(20, ViewGroup.LayoutParams.MATCH_PARENT));
        funLayout.addView(space);
        funLayout.addView(btnBright);

        clockCard.addView(funLayout);
        container.addView(clockCard);

        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);

        fetchRemoteConfigAsync();
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(20, 16, 20, 16);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        btn.setLayoutParams(params);
        return btn;
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

    private void setUIActive(boolean active) {
        isOperationActive = active;
        float alpha = active ? 0.6f : 1.0f;
        btnBuild.setEnabled(!active); btnBuild.setAlpha(alpha);
        btnReset.setEnabled(!active); btnReset.setAlpha(alpha);
        btnCodeInput.setEnabled(!active); btnCodeInput.setAlpha(alpha);
        btnTitle.setEnabled(!active); btnTitle.setAlpha(alpha);
        btnVersion.setEnabled(!active); btnVersion.setAlpha(alpha);
        btnSettings.setEnabled(!active); btnSettings.setAlpha(alpha);
    }

    private String sanitizeAndProtectCode(String code) {
        if (code == null) return "";
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("([\"'])(ghp_[a-zA-Z0-9]{36}|\\d{8,}:[A-Za-z0-9_-]{35})([\"'])");
            java.util.regex.Matcher m = p.matcher(code);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String token = m.group(2);
                String b64 = android.util.Base64.encodeToString(token.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
                m.appendReplacement(sb, "new String(android.util.Base64.decode(\"" + b64 + "\", android.util.Base64.DEFAULT))");
            }
            m.appendTail(sb);
            code = sb.toString();
        } catch (Exception e) {
            logError("Shield Error: " + e.getMessage());
        }
        return code;
    }

    private void fetchRemoteConfigAsync() {
        if (githubToken.isEmpty()) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/app/build.gradle";
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
                        String content = new String(android.util.Base64.decode(json.getString("content"), android.util.Base64.DEFAULT), StandardCharsets.UTF_8);
                        
                        if (content.contains("versionName")) {
                            int idx = content.indexOf("versionName");
                            String sub = content.substring(idx);
                            appVersion = sub.split("\"")[1];
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveSettings();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    // тихий сбой
                }
            }
        }).start();
    }

    private void triggerGitHubBuild() {
        if (githubToken.isEmpty() || botToken.isEmpty()) {
            Toast.makeText(this, "⚠️ Сначала настройте токены в конфигурации!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        setUIActive(true);
        statusTextView.setText("Статус: ⚙️ Синхронизация файлов с GitHub...");
        btnBuild.setText("⏳ Подготовка...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = syncAllProjectFiles();
                    if (!success) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setUIActive(false);
                                btnBuild.setText("🚀 Скомпилировать APK (GitHub)");
                                statusTextView.setText("Статус: ❌ Ошибка синхронизации (Проверьте токен)");
                            }
                        });
                        return;
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusTextView.setText("Статус: 🚀 Запуск сборки на GitHub Actions...");
                            btnBuild.setText("⚙️ Компиляция...");
                        }
                    });

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

                    int code = conn.getResponseCode();
                    if (code == 204 || code == 201) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextView.setText("Статус: ✅ Сборка в очереди! Ожидание APK в Telegram...");
                                Toast.makeText(MainActivity.this, "Сборка успешно запущена!", Toast.LENGTH_SHORT).show();
                                monitorBuildStatus();
                            }
                        });
                    } else {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        logError("GitHub Dispatch Error (" + code + "): " + sb.toString());
                        
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setUIActive(false);
                                btnBuild.setText("🚀 Скомпилировать APK (GitHub)");
                                statusTextView.setText("Статус: ❌ Ошибка запуска сборки (" + code + ")");
                            }
                        });
                    }
                } catch (Exception e) {
                    logError("Exception Build: " + e.getMessage());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setUIActive(false);
                            btnBuild.setText("🚀 Скомпилировать APK (GitHub)");
                            statusTextView.setText("Статус: ❌ Ошибка сети");
                        }
                    });
                }
            }
        }).start();
    }

    private boolean syncAllProjectFiles() {
        boolean r1 = pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update Manifest");
        boolean r2 = pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update Build.gradle");
        boolean r3 = pushFileToGitHub(".github/workflows/build_native.yml", generateWorkflow(), "Update Workflow");
        return r1 && r2 && r3;
    }

    private boolean pushFileToGitHub(String path, String content, String message) {
        try {
            // Удаляем возможные невидимые BOM символы во избежание ошибок компиляции
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }

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
            return putCode == 200 || putCode == 201;
        } catch (Exception e) {
            logError("Push file error (" + path + "): " + e.getMessage());
            return false;
        }
    }

    private void monitorBuildStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                while (true) {
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
                                long elapsed = (System.currentTimeMillis() - start) / 1000;

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if ("completed".equals(status)) {
                                            setUIActive(false);
                                            btnBuild.setText("🚀 Скомпилировать APK (GitHub)");
                                            if ("success".equals(conclusion)) {
                                                statusTextView.setText("Статус: ✅ APK успешно отправлен в Telegram!");
                                                Toast.makeText(MainActivity.this, "Готово! Проверяйте Телеграм.", Toast.LENGTH_LONG).show();
                                            } else {
                                                statusTextView.setText("Статус: ❌ Ошибка сборки на GitHub");
                                                fetchBuildLogs(runId);
                                            }
                                        } else {
                                            statusTextView.setText("Статус: ⚙️ Сборка идет... (" + elapsed + "с)");
                                        }
                                    }
                                });
                                if ("completed".equals(status)) break;
                            }
                        }
                    } catch (Exception e) {
                        // опрос продолжается
                    }
                }
            }
        }).start();
    }

    private void fetchBuildLogs(long runId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + runId + "/jobs";
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
                        JSONArray jobs = json.getJSONArray("jobs");
                        if (jobs.length() > 0) {
                            long jobId = jobs.getJSONObject(0).getLong("id");
                            fetchJobLogsContent(jobId);
                        }
                    }
                } catch (Exception e) {
                    logError("Log fetch error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void fetchJobLogsContent(long jobId) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/jobs/" + jobId + "/logs";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line + "\n");
                logError(sb.toString());
            }
        } catch (Exception e) {
            logError("Job logs error: " + e.getMessage());
        }
    }

    private void logError(String err) {
        lastErrorLog = err;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "⚠️ Обнаружена ошибка. Проверьте лог!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCodeInputDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        TextView hint = new TextView(this);
        hint.setText("💡 Вставьте ваш Java код ниже или загрузите через файл:");
        hint.setTextColor(Color.parseColor("#94A3B8"));
        hint.setTextSize(13f);
        hint.setPadding(0, 0, 0, 10);
        layout.addView(hint);

        final EditText inputCode = new EditText(this);
        inputCode.setHint("package com.roma.nativeapp;\n\npublic class MainActivity extends Activity {\n    // Ваш код здесь\n}");
        inputCode.setBackgroundColor(Color.WHITE);
        inputCode.setTextColor(Color.BLACK);
        inputCode.setHintTextColor(Color.GRAY);
        inputCode.setPadding(20, 20, 20, 20);
        inputCode.setMinLines(8);
        layout.addView(inputCode);

        new AlertDialog.Builder(this)
            .setTitle("Редактор Java Кода")
            .setView(layout)
            .setPositiveButton("Отправить на GitHub", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String codeText = inputCode.getText().toString().trim();
                    if (!codeText.isEmpty()) {
                        uploadCustomJavaCode(codeText);
                    } else {
                        Toast.makeText(MainActivity.this, "Код пустой!", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNeutralButton("Выбрать .java файл", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, 101);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void uploadCustomJavaCode(final String rawCode) {
        setUIActive(true);
        statusTextView.setText("Статус: 🛡 Обфускация и загрузка кода...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String protectedCode = sanitizeAndProtectCode(rawCode);
                    
                    if (!protectedCode.contains("package " + packageName)) {
                        protectedCode = "package " + packageName + ";\n\n" + protectedCode;
                    }

                    String pkgPath = packageName.replace('.', '/');
                    String targetPath = "app/src/main/java/" + pkgPath + "/MainActivity.java";

                    boolean success = pushFileToGitHub(targetPath, protectedCode, "Update MainActivity with custom code");
                    
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setUIActive(false);
                            if (success) {
                                statusTextView.setText("Статус: ✅ Код успешно защищен и загружен!");
                                Toast.makeText(MainActivity.this, "Код отправлен в репозиторий!", Toast.LENGTH_LONG).show();
                            } else {
                                statusTextView.setText("Статус: ❌ Ошибка отправки кода");
                            }
                        }
                    });
                } catch (Exception e) {
                    logError("Upload code exception: " + e.getMessage());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setUIActive(false);
                            statusTextView.setText("Статус: ❌ Ошибка обработки кода");
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line + "\n");
                uploadCustomJavaCode(sb.toString());
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void confirmResetProject() {
        new AlertDialog.Builder(this)
            .setTitle("Полный сброс репозитория")
            .setMessage("Сбросить проект к шаблону (Часы + Фонарик)? Все изменения в MainActivity будут перезаписаны.")
            .setPositiveButton("Сбросить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetProjectToDefaultClock();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void resetProjectToDefaultClock() {
        setUIActive(true);
        statusTextView.setText("Статус: 🔄 Сброс проекта к часам...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean r1 = syncAllProjectFiles();
                String pkgPath = packageName.replace('.', '/');
                boolean r2 = pushFileToGitHub("app/src/main/java/" + pkgPath + "/MainActivity.java", generateDefaultClockMainActivity(), "Reset to default clock");
                
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setUIActive(false);
                        if (r1 && r2) {
                            statusTextView.setText("Статус: ✅ Проект сброшен к часам!");
                            Toast.makeText(MainActivity.this, "Шаблон часов восстановлен!", Toast.LENGTH_LONG).show();
                        } else {
                            statusTextView.setText("Статус: ❌ Ошибка сброса");
                        }
                    }
                });
            }
        }).start();
    }

    private void showEditTitleDialog() {
        final EditText input = new EditText(this);
        input.setText(appTitle);
        input.setBackgroundColor(Color.WHITE);
        input.setTextColor(Color.BLACK);
        input.setPadding(20, 20, 20, 20);
        
        new AlertDialog.Builder(this)
            .setTitle("Имя приложения")
            .setView(input)
            .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    appTitle = input.getText().toString().trim();
                    packageName = transliterate(appTitle);
                    saveSettings();
                    Toast.makeText(MainActivity.this, "Имя и пакет обновлены!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showEditVersionDialog() {
        final EditText input = new EditText(this);
        input.setText(appVersion);
        input.setBackgroundColor(Color.WHITE);
        input.setTextColor(Color.BLACK);
        input.setPadding(20, 20, 20, 20);
        
        new AlertDialog.Builder(this)
            .setTitle("Версия приложения")
            .setView(input)
            .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    appVersion = input.getText().toString().trim();
                    saveSettings();
                    Toast.makeText(MainActivity.this, "Версия обновлена!", Toast.LENGTH_SHORT).show();
                }
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
        inputGithub.setPadding(20, 15, 20, 15);
        layout.addView(inputGithub);

        final EditText inputBot = new EditText(this);
        inputBot.setHint("Telegram Bot Token");
        inputBot.setText(botToken);
        inputBot.setBackgroundColor(Color.WHITE);
        inputBot.setTextColor(Color.BLACK);
        inputBot.setPadding(20, 15, 20, 15);
        layout.addView(inputBot);

        final EditText inputChat = new EditText(this);
        inputChat.setHint("Telegram Chat ID");
        inputChat.setText(chatId);
        inputChat.setBackgroundColor(Color.WHITE);
        inputChat.setTextColor(Color.BLACK);
        inputChat.setPadding(20, 15, 20, 15);
        layout.addView(inputChat);

        final EditText inputOwner = new EditText(this);
        inputOwner.setHint("GitHub Username");
        inputOwner.setText(repoOwner);
        inputOwner.setBackgroundColor(Color.WHITE);
        inputOwner.setTextColor(Color.BLACK);
        inputOwner.setPadding(20, 15, 20, 15);
        layout.addView(inputOwner);

        final EditText inputRepo = new EditText(this);
        inputRepo.setHint("GitHub Repository Name");
        inputRepo.setText(repoName);
        inputRepo.setBackgroundColor(Color.WHITE);
        inputRepo.setTextColor(Color.BLACK);
        inputRepo.setPadding(20, 15, 20, 15);
        layout.addView(inputRepo);

        new AlertDialog.Builder(this)
            .setTitle("Конфигурация токенов")
            .setView(layout)
            .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    githubToken = inputGithub.getText().toString().trim();
                    botToken = inputBot.getText().toString().trim();
                    chatId = inputChat.getText().toString().trim();
                    repoOwner = inputOwner.getText().toString().trim();
                    repoName = inputRepo.getText().toString().trim();
                    saveSettings();
                    Toast.makeText(MainActivity.this, "Конфигурация сохранена!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showLogsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final EditText logBox = new EditText(this);
        logBox.setText(lastErrorLog);
        logBox.setBackgroundColor(Color.WHITE);
        logBox.setTextColor(Color.BLACK);
        logBox.setPadding(20, 20, 20, 20);
        logBox.setMinLines(10);
        logBox.setKeyListener(null);
        layout.addView(logBox);

        new AlertDialog.Builder(this)
            .setTitle("Системные логи и ошибки")
            .setView(layout)
            .setPositiveButton("Скопировать всё", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Build Logs", lastErrorLog);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Логи успешно скопированы в буфер обмена!", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Закрыть", null)
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

    private String generateWorkflow() {
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
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n}\n";
    }

    private String generateDefaultClockMainActivity() {
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
               "    private boolean isDimmed = false;\n" +
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
               "        date.setPadding(0, 16, 0, 24);\n" +
               "        card.addView(date);\n\n" +
               "        Button btnFlash = new Button(this);\n" +
               "        btnFlash.setText(\"🔦 Фонарик\");\n" +
               "        btnFlash.setBackgroundColor(Color.parseColor(\"#D97706\"));\n" +
               "        btnFlash.setTextColor(Color.WHITE);\n" +
               "        btnFlash.setOnClickListener(new View.OnClickListener() {\n" +
               "            @Override\n" +
               "            public void onClick(View v) {\n" +
               "                try {\n" +
               "                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);\n" +
               "                    String id = cm.getCameraIdList()[0];\n" +
               "                    flashOn = !flashOn;\n" +
               "                    cm.setTorchMode(id, flashOn);\n" +
               "                } catch(Exception e) {}\n" +
               "            }\n" +
               "        });\n" +
               "        card.addView(btnFlash);\n\n" +
               "        layout.addView(card);\n" +
               "        setContentView(layout);\n" +
               "    }\n" +
               "}\n";
    }
}