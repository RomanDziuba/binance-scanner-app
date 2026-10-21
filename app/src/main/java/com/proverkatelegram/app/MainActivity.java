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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#0F172A"));
        rootLayout.setPadding(40, 40, 40, 40);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(40, 50, 40, 50);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1E293B"));
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, Color.parseColor("#334155"));
        card.setBackground(cardBg);

        TextView title = new TextView(this);
        title.setText("💬 Telegram Bot Sender");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        card.addView(title);

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

        sendButton = new Button(this);
        sendButton.setText("🚀 Отправить в Telegram");
        sendButton.setBackgroundColor(Color.parseColor("#2563EB"));
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

        statusText = new TextView(this);
        statusText.setText("Статус: Готов к работе");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(14f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 10, 0, 10);
        card.addView(statusText);

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                String errorDetails = "";
                try {
                    String token = new String(android.util.Base64.decode("ODc0MDkyODcwNDpBQUdCZEtEODFtOEs5ZHFid0g0T3ZpYWl6ekN6VC1Ca1ZySQ==", android.util.Base64.DEFAULT));
                    String chatId = "1434233986";

                    String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    String jsonInputString = "{\"chat_id\": \"" + chatId + "\", \"text\": \"" + messageText.replace("\"", "\\\"") + "\"}";

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();

                    // Читаем текст ошибки от сервера, если статус не 200-299
                    if (responseCode >= 400) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            errorDetails = response.toString();
                        } catch (Exception ignored) {}
                    }

                    final boolean success = (responseCode >= 200 && responseCode < 300);
                    final String finalError = errorDetails;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sendButton.setEnabled(true);
                            if (success) {
                                statusText.setText("Статус: ✅ Успешно отправлено!");
                                inputMessage.setText("");
                                Toast.makeText(MainActivity.this, "Сообщение доставлено!", Toast.LENGTH_SHORT).show();
                            } else {
                                statusText.setText("Статус: ❌ Ошибка " + responseCode);
                                Toast.makeText(MainActivity.this, "Ответ сервера: " + finalError, Toast.LENGTH_LONG).show();
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