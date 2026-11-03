package com.hentay.version002;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {
    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private String cameraId;
    private OrtEnvironment aiEnv;
    private OrtSession aiSession;
    private TextView aiStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("⚡ Monster Clock, Flash & AI");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        layout.addView(title);

        aiStatusText = new TextView(this);
        aiStatusText.setText("⏳ Инициализация оффлайн ИИ...");
        aiStatusText.setTextColor(Color.parseColor("#10B981"));
        aiStatusText.setTextSize(11f);
        aiStatusText.setGravity(Gravity.CENTER);
        aiStatusText.setPadding(0, 0, 0, 20);
        layout.addView(aiStatusText);

        new Thread(this::initOfflineAiModel).start();

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(48f);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(0, 0, 0, 30);
        layout.addView(clock);

        Button flashBtn = new Button(this);
        flashBtn.setText("🔦 Включить фонарик");
        flashBtn.setTextColor(Color.WHITE);
        flashBtn.setTextSize(14f);
        flashBtn.setBackgroundColor(Color.parseColor("#2563EB"));
        flashBtn.setPadding(20, 20, 20, 20);
        layout.addView(flashBtn);

        TextView brightLabel = new TextView(this);
        brightLabel.setText("☀️ Яркость экрана");
        brightLabel.setTextColor(Color.parseColor("#94A3B8"));
        brightLabel.setTextSize(12f);
        brightLabel.setPadding(0, 25, 0, 8);
        layout.addView(brightLabel);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(50);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                try {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = Math.max(0.01f, progress / 100f);
                    getWindow().setAttributes(lp);
                } catch (Exception ignored) {}
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        layout.addView(seekBar);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception ignored) {}

        flashBtn.setOnClickListener(v -> {
            try {
                if (cameraManager != null && cameraId != null) {
                    isFlashOn = !isFlashOn;
                    cameraManager.setTorchMode(cameraId, isFlashOn);
                    flashBtn.setText(isFlashOn ? "🔦 Выключить фонарик" : "🔦 Включить фонарик");
                    flashBtn.setBackgroundColor(Color.parseColor(isFlashOn ? "#DC2626" : "#2563EB"));
                }
            } catch (Exception e) {}
        });

        setContentView(layout);
    }

    private void initOfflineAiModel() {
        try {
            File modelFile = new File(getFilesDir(), "model.onnx");
            if (!modelFile.exists() || modelFile.length() == 0) {
                InputStream in = getAssets().open("model.onnx");
                FileOutputStream out = new FileOutputStream(modelFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
            aiEnv = OrtEnvironment.getEnvironment();
            aiSession = aiEnv.createSession(modelFile.getAbsolutePath());
            runOnUiThread(() -> aiStatusText.setText("✅ Оффлайн ИИ модель активна!"));
        } catch (Exception e) {
            runOnUiThread(() -> aiStatusText.setText(
                "ℹ️ Режим стандартного приложения (ИИ модель не найдена в assets)"
            ));
        }
    }
}