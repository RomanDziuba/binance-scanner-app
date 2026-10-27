package com.COMPILER;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;

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
        tv.setText("✨ Чистый проект Monster Pro\nРепозиторий полностью очищен и готов");
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(18f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);

        setContentView(layout);
    }
}
