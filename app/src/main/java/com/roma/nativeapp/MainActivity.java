package com.roma.nativeapp;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText ipInput;
    private Button pttButton;
    private TextView statusView, localIpView;
    
    private static boolean isTransmitting = false;
    private static final int PORT = 50005;
    private static final int SAMPLE_RATE = 8000;
    
    private static AudioRecord audioRecord;
    private static Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Запрос разрешений (микрофон и уведомления для Android 13+)
        String[] perms = {
            Manifest.permission.RECORD_AUDIO,
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE"
        };
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(perms, 100);
        }

        // Интерфейс приложения
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(35, 45, 35, 45);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(36f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Made in Kamyanske • Roman Dziuba ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 0, 0, 16);
        card.addView(authorView);

        TextView title = new TextView(this);
        title.setText("📡 Wi-Fi Рация (Фоновый режим)");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        card.addView(title);

        localIpView = new TextView(this);
        localIpView.setText("Мой IP в сети: " + getLocalIpAddress());
        localIpView.setTextColor(Color.parseColor("#10B981"));
        localIpView.setTextSize(14f);
        localIpView.setTypeface(Typeface.MONOSPACE);
        localIpView.setGravity(Gravity.CENTER);
        localIpView.setPadding(0, 0, 0, 20);
        card.addView(localIpView);

        ipInput = new EditText(this);
        ipInput.setHint("IP собеседника (например, 192.168.1.50)");
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

        statusView = new TextView(this);
        statusView.setText("Статус: Фоновая служба активна");
        statusView.setTextColor(Color.parseColor("#94A3B8"));
        statusView.setTextSize(13f);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 0, 0, 25);
        card.addView(statusView);

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

        pttButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTransmitting();
                    btnBg.setColor(Color.parseColor("#DC2626"));
                    pttButton.setText("🔴 ИДЕТ ПЕРЕДАЧА...");
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopTransmitting();
                    btnBg.setColor(Color.parseColor("#2563EB"));
                    pttButton.setText("🎙 ЗАЖМИ И ГОВОРИ");
                    return true;
            }
            return false;
        });
        card.addView(pttButton);

        layout.addView(card);
        setContentView(layout);

        // Запуск фоновой службы при старте приложения
        startWalkieService();
    }

    // Универсальный метод поиска IP-адреса устройства в локальной сети
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> enNet = NetworkInterface.getNetworkInterfaces(); enNet.hasMoreElements(); ) {
                NetworkInterface intf = enNet.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Не найден (Проверьте Wi-Fi)";
    }

    private void startWalkieService() {
        Intent serviceIntent = new Intent(this, WalkieService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void startTransmitting() {
        final String targetIp = ipInput.getText().toString().trim();
        if (targetIp.isEmpty()) {
            Toast.makeText(this, "Введите IP собеседника!", Toast.LENGTH_SHORT).show();
            return;
        }

        isTransmitting = true;
        statusView.setText("Статус: 🔴 Передача голоса...");

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

    private void stopTransmitting() {
        isTransmitting = false;
        statusView.setText("Статус: Фоновая служба активна (Слушаю)");
    }

    // Фоновая служба для непрерывной работы сети и звука в фоне
    public static class WalkieService extends Service {
        private boolean isRunning = false;
        private Thread listenThread;

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = new Notification.Builder(this, "walkie_channel")
                    .setContentTitle("Wi-Fi Рация активна")
                    .setContentText("Прием и передача голоса в фоновом режиме")
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .build();
            } else {
                notification = new Notification.Builder(this)
                    .setContentTitle("Wi-Fi Рация активна")
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .build();
            }
            startForeground(1, notification);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (!isRunning) {
                isRunning = true;
                startReceiverServer();
            }
            return START_STICKY;
        }

        private void startReceiverServer() {
            listenThread = new Thread(() -> {
                try {
                    DatagramSocket socket = new DatagramSocket(PORT);
                    int playBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, 
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    
                    AudioTrack audioTrack = new AudioTrack(
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

                    while (isRunning) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        audioTrack.write(packet.getData(), 0, packet.getLength());
                    }
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listenThread.start();
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    "walkie_channel", "Walkie Talkie Background", NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) manager.createNotificationChannel(channel);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            isRunning = false;
            if (listenThread != null) listenThread.interrupt();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}