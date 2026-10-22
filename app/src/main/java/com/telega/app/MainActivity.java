package com.telega.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private EditText messageInput;
    private Button sendButton;
    private TextView statusView;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Ваши данные Telegram API
    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 50, 40, 50);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        TextView title = new TextView(this);
        title.setText("📤 Тест Telegram API");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        card.addView(title);

        messageInput = new EditText(this);
        messageInput.setHint("Введите текст сообщения для Telegram...");
        messageInput.setTextColor(Color.WHITE);
        messageInput.setHintTextColor(Color.parseColor("#64748B"));
        messageInput.setBackgroundColor(Color.parseColor("#1E293B"));
        messageInput.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 20);
        messageInput.setLayoutParams(inputParams);
        card.addView(messageInput);

        sendButton = new Button(this);
        sendButton.setText("🚀 Отправить сообщение");
        sendButton.setBackgroundColor(Color.parseColor("#2563EB"));
        sendButton.setTextColor(Color.WHITE);
        sendButton.setPadding(20, 20, 20, 20);
        sendButton.setOnClickListener(v -> sendTelegramMessage());
        card.addView(sendButton);

        statusView = new TextView(this);
        statusView.setText("Статус: Готов к отправке");
        statusView.setTextColor(Color.parseColor("#10B981"));
        statusView.setTextSize(14f);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 20, 0, 0);
        card.addView(statusView);

        layout.addView(card);
        setContentView(layout);
    }

    private void sendTelegramMessage() {
        final String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Введите текст сообщения!", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);
        statusView.setText("Статус: ⏳ Отправка запроса...");

        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Безопасное экранирование JSON текста
                String escapedText = text.replace("\\", "\\\\")
                                         .replace("\"", "\\\"")
                                         .replace("\n", "\\n")
                                         .replace("\r", "\\r");

                String jsonBody = "{\"chat_id\":\"" + chatId + "\", \"text\":\"" + escapedText + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                handler.post(() -> {
                    sendButton.setEnabled(true);
                    if (responseCode == 200) {
                        statusView.setText("Статус: ✅ Успешно отправлено в Telegram!");
                        Toast.makeText(MainActivity.this, "Сообщение отправлено!", Toast.LENGTH_SHORT).show();
                        messageInput.setText("");
                    } else {
                        statusView.setText("Статус: ❌ Ошибка сервера (Код: " + responseCode + ")");
                    }
                });

            } catch (final Exception e) {
                handler.post(() -> {
                    sendButton.setEnabled(true);
                    statusView.setText("Статус: ❌ Ошибка сети: " + e.getMessage());
                });
            }
        }).start();
    }
}