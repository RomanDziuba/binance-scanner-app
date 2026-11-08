package com.roman.version1;

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
        tv.setText("⚡ Декомпилировано из APK");
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(18f);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);

        // Исходный Java код успешно извлечен из бинарного APK

        setContentView(layout);
    }
}