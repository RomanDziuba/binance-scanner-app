package com.roma.nativeapp;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "AppConfig";
    private static final int PERMISSION_REQUEST_CODE = 200;

    private String githubToken = "";
    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";
    private String repoOwner = "RomanDziuba";
    private String repoName = "binance-scanner-app";
    
    private String appTitle = "WiFiWalkieTalkie";
    private String appVersion = "1.0";
    private String packageName = "com.roma.nativeapp";

    private int minSdk = 24;
    private int compileSdk = 34;

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
        loadSettings();

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
        statusView.setText("Статус: Готов к связи");
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

        // Обработка нажатия с запросом разрешений
        pttButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestMicPermission();
                        return true;
                    }

                    startTransmitting();
                    btnBg.setColor(Color.parseColor("#DC2626"));
                    pttButton.setText("🔴 ИДЕТ ПЕРЕДАЧА...");
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        stopTransmitting();
                    }
                    btnBg.setColor(Color.parseColor("#2563EB"));
                    pttButton.setText("🎙 ЗАЖМИ И ГОВОРИ");
                    return true;
            }
            return false;
        });
        card.addView(pttButton);

        layout.addView(card);
        setContentView(layout);

        // Запрос разрешения при старте приложения
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicPermission();
        } else {
            startWalkieService();
        }
    }

    private void requestMicPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            }, PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Разрешение получено!", Toast.LENGTH_SHORT).show();
                startWalkieService();
            } else {
                Toast.makeText(this, "❌ Без разрешения микрофон не работает", Toast.LENGTH_LONG).show();
            }
        }
    }

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

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        githubToken = sp.getString("githubToken", "");
        botToken = sp.getString("botToken", botToken);
        chatId = sp.getString("chatId", chatId);
        repoOwner = sp.getString("repoOwner", repoOwner);
        repoName = sp.getString("repoName", repoName);
        appTitle = sp.getString("appTitle", appTitle);
        appVersion = sp.getString("appVersion", appVersion);
        packageName = sp.getString("packageName", packageName);
    }

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

    private String getRootGradleContent() {
        return "buildscript {\n    repositories {\n        google()\n        mavenCentral()\n    }\n    dependencies {\n        classpath 'com.android.tools.build:gradle:8.2.2'\n    }\n}\nallprojects {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\n";
    }

    private String getSettingsGradleContent() {
        return "rootProject.name = \"" + appTitle + "\"\ninclude ':app'\n";
    }

    private String getWorkflowContent() {
        return "name: Build Native Android APK (Java)\n\non:\n  workflow_dispatch:\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - name: Checkout repository\n        uses: actions/checkout@v4\n      - name: Set up JDK 17\n        uses: actions/setup-java@v5\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - name: Setup Gradle 8.5\n        uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: '8.5'\n          cache-disabled: true\n      - name: Setup Android SDK & Accept Licenses\n        run: |\n          export ANDROID_HOME=/usr/local/lib/android/sdk\n          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n          yes | sdkmanager --licenses || true\n          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n      - name: Build Debug APK with Gradle (Clean Cache)\n        run: |\n          export ANDROID_HOME=/usr/local/lib/android/sdk\n          export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n          gradle clean assembleDebug --stacktrace --no-daemon\n      - name: Upload APK to Telegram Bot\n        if: success()\n        run: |\n          APK_PATH=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n          if [ -n \"$APK_PATH\" ]; then\n            APP_TITLE=\"" + appTitle + "\"\n            RENAMED_PATH=\"/tmp/$APP_TITLE.apk\"\n            cp \"$APK_PATH\" \"$RENAMED_PATH\"\n            curl -F chat_id=\"" + chatId + "\" -F document=@\"$RENAMED_PATH\" https://api.telegram.org/bot" + botToken + "/sendDocument\n          else\n            echo \"❌ Ошибка: APK файл не найден!\"\n            exit 1\n          fi\n";
    }

    // МАНИФЕСТ СО ВСЕМИ РАЗРЕШЕНИЯМИ
    private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MICROPHONE\" />\n" +
               "    <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />\n" +
               "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + appTitle + "\" android:supportsRtl=\"true\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
               "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
               "            <intent-filter>\n" +
               "                <action android:name=\"android.intent.action.MAIN\" />\n" +
               "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
               "            </intent-filter>\n" +
               "        </activity>\n" +
               "        <service android:name=\".MainActivity$WalkieService\" android:foregroundServiceType=\"microphone\" android:exported=\"false\" />\n" +
               "    </application>\n" +
               "</manifest>";
    }

    private String generateAppGradle() {
        return "plugins {\n    id 'com.android.application'\n}\n\n" +
               "android {\n    namespace '" + packageName + "'\n    compileSdk " + compileSdk + "\n" +
               "    defaultConfig {\n        applicationId \"" + packageName + "\"\n" +
               "        minSdk " + minSdk + "\n        targetSdk " + compileSdk + "\n        versionCode 1\n        versionName \"" + appVersion + "\"\n    }\n" +
               "    compileOptions {\n" +
               "        sourceCompatibility JavaVersion.VERSION_17\n" +
               "        targetCompatibility JavaVersion.VERSION_17\n" +
               "    }\n}\n\n" +
               "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.11.0'\n}\n";
    }
}