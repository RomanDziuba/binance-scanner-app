package ua.quick.radar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/* 
 * Декомпилировано из APK: base.apk
 * Определенный пакет: ua.quick.radar
 * Вы можете изменять этот код в редакторе перед компиляцией!
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
       
TextView tv = new TextView(this);

Toast.makeText(this, "Привет!", Toast.LENGTH_SHORT).show();
tv.setText("Привет!");
layout.addView(tv);

TextView tv = new TextView(this);
tv.setText("Привет Ромка!");
layout.addView(tv);

Button btn = new Button(this);
btn.setText("Нажми шо нибудь");
layout.addView(btn);
 layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        TextView tv = new TextView(this);
        tv.setText("📦 APK успешно декомпилирован!\nПакет: ua.quick.radar");
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(18f);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);

        setContentView(layout);
    }
}
