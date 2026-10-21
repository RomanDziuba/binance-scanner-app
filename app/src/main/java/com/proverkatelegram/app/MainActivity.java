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
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private EditText inputMessage;
    private TextView statusText;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер (Глубокий темный фон)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#0F172A")); // Slate 900
        rootLayout.setPadding(40, 40, 40, 40);

        // Карточка интерфейса
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(40, 50, 40, 50);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1E293B")); // Slate 800
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, Color.parseColor("#334155"));
        card.setBackground(cardBg);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("💬 Telegram Bot Sender");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        card.addView(title);

        // Поле ввода сообщения
        inputMessage = new EditText(this);
        inputMessage.setHint("Введите сообщение для отправки...");
        inputMessage.setHintTextColor(Color.parseColor("#64748B"));
        inputMessage.setTextColor(Color.WHITE);
        inputMessage.setTextSize(16f);
        inputMessage.setBackgroundColor(Color.parseColor("#0F172A"));
        inputMessage.setPadding(30, 30, 30, 30);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 300
        );
        inputParams.setMargins(0, 0, 0, 24);
        inputMessage.setLayoutParams(inputParams);
        inputMessage.setGravity(Gravity.TOP | Gravity.START);
        card.addView(inputMessage);

        // Кнопка отправки
        sendButton = new Button(this);
        sendButton.setText("🚀 Отправить в Telegram");
        sendButton.setBackgroundColor(Color.parseColor("#2563EB")); // Blue 600
        sendButton.setTextColor(Color.WHITE);
        sendButton.setTextSize(16f);
        sendButton.setTypeface(Typeface.DEFAULT_BOLD);
        sendButton.setPadding(30, 25, 30, 25);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 0, 0, 20);
        sendButton.setLayoutParams(btnParams);
        
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = inputMessage.getText().toString().trim();
                if (msg.isEmpty()) {
                    Toast.makeText(MainActivity.this, "⚠️ Введите текст сообщения!", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendTelegramMessage(msg);
            }
        });
        card.addView(sendButton);

        // Текст статуса
        statusText = new TextView(this);
        statusText.setText("Статус: Готов к работе");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(14f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 10, 0, 10);
        card.addView(statusText);

        // Подпись автора
        TextView author = new TextView(this);
        author.setText("App engineering by Dziuba R.V");
        author.setTextColor(Color.parseColor("#475569"));
        author.setTextSize(11f);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, 20, 0, 0);
        card.addView(author);

        rootLayout.addView(card);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));
        
        setContentView(rootLayout);
    }

    private void sendTelegramMessage(final String messageText) {
        statusText.setText("Статус: Отправка...");
        sendButton.setEnabled(false);

        // Сетевые запросы должны выполняться в отдельном потоке (Thread)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Ваши реальные данные (бот автоматически зашифрует токен в Base64 для GitHub)
                    String token = "REDACTED_TELEGRAM_TOKEN";
                    String chatId = "1434233986";

                    String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    // Формируем JSON тело запроса
                    String jsonInputString = "{\"chat_id\": \"" + chatId + "\", \"text\": \"" + messageText.replace("\"", "\\\"") + "\"}";

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    final boolean success = (responseCode >= 200 && responseCode < 300);

                    // Возвращаемся в главный поток для обновления интерфейса
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sendButton.setEnabled(true);
                            if (success) {
                                statusText.setText("Статус: ✅ Успешно отправлено!");
                                inputMessage.setText("");
                                Toast.makeText(MainActivity.this, "Сообщение доставлено!", Toast.LENGTH_SHORT).show();
                            } else {
                                statusText.setText("Статус: ❌ Ошибка сервера (" + responseCode + ")");
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sendButton.setEnabled(true);
                            statusText.setText("Статус: ❌ Ошибка сети");
                            Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}