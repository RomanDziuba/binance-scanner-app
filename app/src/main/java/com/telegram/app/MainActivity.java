package com.telegram.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    // Ваши данные для теста
    private static final String BOT_TOKEN = "" + new String(android.util.Base64.decode("ODc0MDkyODcwNDpBQUdCZEtEODFtOEs5ZHFid0g0T3ZpYWl6ekN6VC1Ca1ZySQ==", android.util.Base64.DEFAULT)) + "";
    private static final String CHAT_ID = "1434233986";

    private EditText inputMessage;
    private Button btnSend;
    private TextView statusLog;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем красивый темный интерфейс
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(30, 40, 30, 40);

        // Карточка контейнера
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 30, 30, 30);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(28f);
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("🤖 Telegram Bot Test");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        card.addView(title);

        // Строка ввода (EditText)
        inputMessage = new EditText(this);
        inputMessage.setHint("Введите сообщение для отправки...");
        inputMessage.setHintTextColor(Color.parseColor("#475569"));
        inputMessage.setTextColor(Color.WHITE);
        inputMessage.setTextSize(14f);
        inputMessage.setBackgroundColor(Color.parseColor("#1E293B"));
        inputMessage.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 15);
        inputMessage.setLayoutParams(inputParams);
        card.addView(inputMessage);

        // Кнопка отправки
        btnSend = new Button(this);
        btnSend.setText("🚀 Отправить в Telegram");
        btnSend.setTextColor(Color.WHITE);
        btnSend.setTextSize(14f);
        btnSend.setTypeface(Typeface.DEFAULT_BOLD);
        btnSend.setBackgroundColor(Color.parseColor("#2563EB"));
        btnSend.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 0, 0, 15);
        btnSend.setLayoutParams(btnParams);
        
        btnSend.setOnClickListener(v -> {
            String msg = inputMessage.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Введите текст сообщения!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendTelegramMessage(msg);
        });
        card.addView(btnSend);

        // Статус / Лог
        statusLog = new TextView(this);
        statusLog.setText("Статус: Ожидание ввода...");
        statusLog.setTextColor(Color.parseColor("#10B981"));
        statusLog.setTextSize(13f);
        statusLog.setGravity(Gravity.CENTER);
        statusLog.setPadding(0, 10, 0, 10);
        card.addView(statusLog);

        rootLayout.addView(card);
        setContentView(rootLayout);
    }

    private void sendTelegramMessage(final String messageText) {
        btnSend.setEnabled(false);
        statusLog.setText("Статус: ⏳ Отправка запроса...");

        new Thread(() -> {
            try {
                // Формируем URL для Telegram Bot API
                String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Тело запроса JSON (chat_id и текст)
                String jsonBody = "{\"chat_id\":\"" + CHAT_ID + "\", \"text\":\"" + messageText + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                handler.post(() -> {
                    btnSend.setEnabled(true);
                    if (responseCode == 200 || responseCode == 201) {
                        statusLog.setText("Статус: ✅ Сообщение успешно доставлено!");
                        Toast.makeText(this, "Отправлено в Telegram!", Toast.LENGTH_SHORT).show();
                        inputMessage.setText("");
                    } else {
                        statusLog.setText("Статус: ❌ Ошибка API (Код: " + responseCode + ")");
                    }
                });

            } catch (final Exception e) {
                handler.post(() -> {
                    btnSend.setEnabled(true);
                    statusLog.setText("Статус: ❌ Ошибка сети");
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}