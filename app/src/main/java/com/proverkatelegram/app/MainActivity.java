package com.proverkatelegram.app;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    // Ваши реальные данные
    private static final String BOT_TOKEN = "REDACTED_TELEGRAM_TOKEN";
    private static final String CHAT_ID = "1434233986";

    private EditText messageInput;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер (Темная тема Cyber Slate)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#0F172A")); // Slate 900
        rootLayout.setPadding(40, 40, 40, 40);

        // Карточка-контейнер
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(40, 50, 40, 50);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1E293B")); // Slate 800
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, Color.parseColor("#334155"));
        cardLayout.setBackground(cardBg);

        // Авторство
        TextView authorView = new TextView(this);
        authorView.setText("App engineering by Dziuba R.V");
        authorView.setTextColor(Color.parseColor("#64748B"));
        authorView.setTextSize(12f);
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 0, 0, 16);

        // Заголовок
        TextView titleView = new TextView(this);
        titleView.setText("💬 Telegram Bot Sender");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 24);

        // Поле ввода сообщения
        messageInput = new EditText(this);
        messageInput.setHint("Введите сообщение для отправки...");
        messageInput.setHintTextColor(Color.parseColor("#64748B"));
        messageInput.setTextColor(Color.WHITE);
        messageInput.setBackgroundColor(Color.parseColor("#0F172A"));
        messageInput.setPadding(30, 30, 30, 30);
        messageInput.setTextSize(14f);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 24);
        messageInput.setLayoutParams(inputParams);

        // Кнопка отправки
        sendButton = new Button(this);
        sendButton.setText("🚀 Отправить в Telegram");
        sendButton.setBackgroundColor(Color.parseColor("#2563EB")); // Blue 600
        sendButton.setTextColor(Color.WHITE);
        sendButton.setTextSize(14f);
        sendButton.setTypeface(Typeface.DEFAULT_BOLD);
        sendButton.setPadding(30, 25, 30, 25);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = messageInput.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(MainActivity.this, "⚠️ Введите текст сообщения!", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendTelegramMessage(text);
            }
        });

        // Сборка интерфейса
        cardLayout.addView(authorView);
        cardLayout.addView(titleView);
        cardLayout.addView(messageInput);
        cardLayout.addView(sendButton);

        rootLayout.addView(cardLayout);
        setContentView(rootLayout);
    }

    // Метод отправки запроса к Telegram API в фоновом потоке
    private void sendTelegramMessage(final String messageText) {
        sendButton.setEnabled(false);
        sendButton.setText("⏳ Отправка...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setDoOutput(true);

                    // Формируем JSON-тело запроса с экранированием кавычек
                    String jsonInputString = "{\"chat_id\": \"" + CHAT_ID + "\", \"text\": \"" + messageText.replace("\"", "\\\"") + "\"}";

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    final int responseCode = conn.getResponseCode();

                    // Возвращаемся в главный поток для показа Toast
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sendButton.setEnabled(true);
                            sendButton.setText("🚀 Отправить в Telegram");
                            
                            if (responseCode == 200) {
                                Toast.makeText(MainActivity.this, "✅ Сообщение успешно отправлено!", Toast.LENGTH_LONG).show();
                                messageInput.setText(""); // Очищаем поле
                            } else {
                                Toast.makeText(MainActivity.this, "❌ Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sendButton.setEnabled(true);
                            sendButton.setText("🚀 Отправить в Telegram");
                            Toast.makeText(MainActivity.this, "❌ Ошибка сети: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
