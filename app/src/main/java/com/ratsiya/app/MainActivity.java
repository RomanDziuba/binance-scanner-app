package com.ratsiya.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText ipInput;
    private Button pttButton;
    private TextView statusView, localIpView;
    
    private boolean isTransmitting = false;
    private boolean isListening = true;
    
    private static final int PORT = 50005;
    private static final int SAMPLE_RATE = 8000; // 8kHz оптимально для голоса по Wi-Fi
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Thread receiveThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Запрос разрешения на запись звука (микрофон)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        // Основной контейнер
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        // Карточка интерфейса
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(35, 45, 35, 45);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(36f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        // Подпись автора (Made in Kamyanske • Roman Dziuba)
        TextView authorView = new TextView(this);
        authorView.setText("✦ Made in Kamyanske • Roman Dziuba ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 0, 0, 16);
        card.addView(authorView);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("📡 Wi-Fi Локальная Рация");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        card.addView(title);

        // Индикатор IP текущего телефона
        localIpView = new TextView(this);
        localIpView.setText("Мой IP в сети: " + getLocalIpAddress());
        localIpView.setTextColor(Color.parseColor("#94A3B8"));
        localIpView.setTextSize(12f);
        localIpView.setGravity(Gravity.CENTER);
        localIpView.setPadding(0, 0, 0, 20);
        card.addView(localIpView);

        // Поле ввода IP-адреса собеседника
        ipInput = new EditText(this);
        ipInput.setHint("Введите IP собеседника (например, 192.168.1.50)");
        ipInput.setTextColor(Color.WHITE);
        ipInput.setHintTextColor(Color.parseColor("#64748B"));
        ipInput.setTextSize(13f);
        ipInput.setBackgroundColor(Color.parseColor("#1E293B"));
        ipInput.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 25);
        ipInput.setLayoutParams(inputParams);
        card.addView(ipInput);

        // СТАТУС
        statusView = new TextView(this);
        statusView.setText("Статус: Готов к связи (Слушаю эфир)");
        statusView.setTextColor(Color.parseColor("#10B981"));
        statusView.setTextSize(14f);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 0, 0, 25);
        card.addView(statusView);

        // БОЛЬШАЯ КРУТАЯ КНОПКА РАЦИИ (PTT)
        pttButton = new Button(this);
        pttButton.setText("🎙 ЗАЖМИ И ГОВОРИ");
        pttButton.setTextColor(Color.WHITE);
        pttButton.setTextSize(16f);
        pttButton.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#2563EB"));
        btnBg.setCornerRadius(30f);
        pttButton.setBackground(btnBg);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 130
        );
        pttButton.setLayoutParams(btnParams);

        // Обработка зажатия кнопки (TouchListener)
        pttButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTransmitting();
                    btnBg.setColor(Color.parseColor("#DC2626")); // Красный при передаче
                    pttButton.setText("🔴 ИДЕТ ПЕРЕДАЧА ГОЛОСА...");
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopTransmitting();
                    btnBg.setColor(Color.parseColor("#2563EB")); // Синий в режиме ожидания
                    pttButton.setText("🎙 ЗАЖМИ И ГОВОРИ");
                    return true;
            }
            return false;
        });
        card.addView(pttButton);

        layout.addView(card);
        setContentView(layout);

        // Запуск фонового приема звука по сети
        startListening();
    }

    // Получение IP-адреса устройства в Wi-Fi сети
    private String getLocalIpAddress() {
        try {
            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        } catch (Exception e) {
            return "Неизвестно (Включите Wi-Fi)";
        }
    }

    // Начало передачи голоса (нажали кнопку)
    private void startTransmitting() {
        final String targetIp = ipInput.getText().toString().trim();
        if (targetIp.isEmpty()) {
            Toast.makeText(this, "Введите IP-адрес второго телефона!", Toast.LENGTH_SHORT).show();
            return;
        }

        isTransmitting = true;
        statusView.setText("Статус: 🔴 Передача голоса в эфир...");

        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress address = InetAddress.getByName(targetIp);
                
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, 
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                
                audioRecord.startRecording();
                byte[] buffer = new byte[bufferSize];

                while (isTransmitting) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, read, address, PORT);
                        socket.send(packet);
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                socket.close();
            } catch (Exception e) {
                handler.post(() -> statusView.setText("Ошибка передачи: " + e.getMessage()));
            }
        }).start();
    }

    // Остановка передачи (отпустили кнопку)
    private void stopTransmitting() {
        isTransmitting = false;
        statusView.setText("Статус: Готов к связи (Слушаю эфир)");
    }

    // Фоновый поток для приема звука от собеседника
    private void startListening() {
        receiveThread = new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(PORT);
                int playBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, 
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                
                audioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                    new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    playBufferSize,
                    AudioTrack.MODE_STREAM,
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
                );
                
                audioTrack.play();
                byte[] buffer = new byte[playBufferSize];

                while (isListening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    audioTrack.write(packet.getData(), 0, packet.getLength());
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isListening = false;
        isTransmitting = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
    }
}