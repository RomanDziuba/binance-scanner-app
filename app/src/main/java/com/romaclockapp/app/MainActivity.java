package com.romaclockapp.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private boolean flashOn = false;
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
        card.setGravity(Gravity.CENTER);
        card.setPadding(40, 50, 40, 50);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(32f);
        card.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("⚡ RomaClockApp");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(24f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        card.addView(title);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(48f);
        clock.setTypeface(Typeface.MONOSPACE);
        clock.setGravity(Gravity.CENTER);
        card.addView(clock);

        Button btnFlash = new Button(this);
        btnFlash.setText("🔦 Вспышка / Фонарик");
        btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setPadding(30, 20, 30, 20);
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String camId = cm.getCameraIdList()[0];
                    flashOn = !flashOn;
                    cm.setTorchMode(camId, flashOn);
                    Toast.makeText(MainActivity.this, flashOn ? "Фонарик ВКЛ" : "Фонарик ВЫКЛ", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Ошибка фонарика", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 30, 0, 0);
        btnFlash.setLayoutParams(lp);
        card.addView(btnFlash);

        layout.addView(card);
        setContentView(layout);
    }
}
