package com.monstercompiler.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        TextView tv = new TextView(this);
        tv.setText("⚡ Чистый проект Monster Pro");
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(20f);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);

        setContentView(layout);
    }
}
