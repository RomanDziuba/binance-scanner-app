package com.mainactivity.version93;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("⚡ Monster Clock & Flash");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(52f);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(0, 0, 0, 40);
        layout.addView(clock);

        Button flashBtn = new Button(this);
        flashBtn.setText("🔦 Включить фонарик");
        flashBtn.setTextColor(Color.WHITE);
        flashBtn.setTextSize(14f);
        flashBtn.setBackgroundColor(Color.parseColor("#2563EB"));
        flashBtn.setPadding(20, 20, 20, 20);
        layout.addView(flashBtn);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(50);
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
}