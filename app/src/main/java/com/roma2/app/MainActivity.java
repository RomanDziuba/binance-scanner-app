package com.roma2.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
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
        title.setText("⚡ Roma2");
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
        date.setPadding(0, 16, 0, 0);
        card.addView(date);

        layout.addView(card);
        setContentView(layout);
    }
}
