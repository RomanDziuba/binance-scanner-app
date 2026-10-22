package com.roma.nativeapp;

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

        TextView date = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        date.setText(sdf.format(new Date()));
        date.setTextColor(Color.parseColor("#64748B"));
        date.setTextSize(14f);
        date.setGravity(Gravity.CENTER);
        date.setPadding(0, 16, 0, 24);
        card.addView(date);

        Button btnFlash = new Button(this);
        btnFlash.setText("🔦 Фонарик");
        btnFlash.setBackgroundColor(Color.parseColor("#D97706"));
        btnFlash.setTextColor(Color.WHITE);
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String id = cm.getCameraIdList()[0];
                    flashOn = !flashOn;
                    cm.setTorchMode(id, flashOn);
                } catch(Exception e) {}
            }
        });
        card.addView(btnFlash);

        layout.addView(card);
        setContentView(layout);
    }
}
