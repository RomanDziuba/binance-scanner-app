package ua.quick.radar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        TextView tv = new TextView(this);
        tv.setText("⚡ Моё приложение работает!");
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(20f);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);

        setContentView(layout);
    }
}