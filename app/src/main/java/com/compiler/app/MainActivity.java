package com.compiler.app;

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
    private boolean isDimmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(40, 50, 40, 50);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        cardLayout.setBackground(cardBg);

        TextView authorView = new TextView(this);
        authorView.setText("✦ Dziuba Roman ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(21f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 0, 0, 16);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ RomaClockApp (Java)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(24f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 24);

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("hh:mm:ss a");
        clock.setFormat24Hour("HH:mm:ss");
        clock.setTextColor(Color.parseColor("#10B981"));
        clock.setTextSize(48f);
        clock.setTypeface(Typeface.MONOSPACE);
        clock.setGravity(Gravity.CENTER);

        TextView dateView = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        dateView.setText(sdf.format(new Date()));
        dateView.setTextColor(Color.parseColor("#64748B"));
        dateView.setTextSize(14f);
        dateView.setGravity(Gravity.CENTER);
        dateView.setPadding(0, 16, 0, 32);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setPadding(0, 10, 0, 0);

        Button btnFlash = new Button(this);
        btnFlash.setText("🔦 Вспышка / Фонарик");
        btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setPadding(30, 20, 30, 20);
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String cameraId = camManager.getCameraIdList()[0];
                    flashOn = !flashOn;
                    camManager.setTorchMode(cameraId, flashOn);
                    Toast.makeText(MainActivity.this, flashOn ? "Фонарик ВКЛ" : "Фонарик ВЫКЛ", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Ошибка фонарика (нет вспышки)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnBrightness = new Button(this);
        btnBrightness.setText("🌙 Режим темноты / Яркость");
        btnBrightness.setBackgroundColor(Color.parseColor("#334155"));
        btnBrightness.setTextColor(Color.WHITE);
        btnBrightness.setPadding(30, 20, 30, 20);
        btnBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDimmed = !isDimmed;
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = isDimmed ? 0.05f : -1f;
                getWindow().setAttributes(lp);
                Toast.makeText(MainActivity.this, isDimmed ? "Ночной режим" : "Обычная яркость", Toast.LENGTH_SHORT).show();
            }
        });

        cardLayout.addView(authorView);
        cardLayout.addView(titleView);
        cardLayout.addView(clock);
        cardLayout.addView(dateView);
        
        btnLayout.addView(btnFlash);
        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16));
        btnLayout.addView(space);
        btnLayout.addView(btnBrightness);
        
        cardLayout.addView(btnLayout);
        layout.addView(cardLayout);
        setContentView(layout);
    }
}
