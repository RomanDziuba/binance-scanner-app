package com.roma.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String BOT_TOKEN = "REDACTED_TELEGRAM_TOKEN";
    private static final String MY_CHAT_ID = "1434233986";
    private static final String GITHUB_TOKEN = "REDACTED_GITHUB_TOKEN";
    private static final String REPO_OWNER = "RomanDziuba";
    private static final String REPO_NAME = "binance-scanner-app";
    
    private TextView statusLog;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем графический интерфейс для Android-устройства
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ Android Java Compiler Bot");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(22f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 30);

        statusLog = new TextView(this);
        statusLog.setText("📱 Бот-компилятор готов к работе.\nУправление ведется через Telegram-чат.");
        statusLog.setTextColor(Color.parseColor("#10B981"));
        statusLog.setTextSize(14f);
        statusLog.setGravity(Gravity.CENTER);
        statusLog.setPadding(20, 20, 20, 20);

        Button btnStartService = new Button(this);
        btnStartService.setText("🚀 Проверить связь с GitHub");
        btnStartService.setBackgroundColor(Color.parseColor("#D97706"));
        btnStartService.setTextColor(Color.WHITE);
        btnStartService.setOnClickListener(v -> {
            statusLog.setText("⏳ Проверка связи с репозиторием...");
            executor.execute(this::testGitHubConnection);
        });

        layout.addView(titleView);
        layout.addView(statusLog);
        layout.addView(btnStartService);
        
        setContentView(layout);
    }

    private void testGitHubConnection() {
        try {
            String urlStr = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
            conn.setConnectTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            runOnUiThread(() -> {
                if (responseCode == 200) {
                    statusLog.setText("✅ Связь с репозиторием GitHub успешна!\nРепозиторий готов к сборке APK.");
                } else {
                    statusLog.setText("❌ Ошибка подключения к GitHub. Код: " + responseCode);
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> statusLog.setText("❌ Ошибка сети: " + e.getMessage()));
        }
    }
}