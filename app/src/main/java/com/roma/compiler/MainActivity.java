package com.roma.compiler;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ Roma Compiler App");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(24f);
        titleView.setGravity(Gravity.CENTER);

        TextView descView = new TextView(this);
        descView.setText("Отправьте .java файл в Telegram-бот для сборки APK.");
        descView.setTextColor(Color.parseColor("#10B981"));
        descView.setTextSize(16f);
        descView.setGravity(Gravity.CENTER);
        descView.setPadding(0, 30, 0, 0);

        layout.addView(titleView);
        layout.addView(descView);
        setContentView(layout);
    }
}
