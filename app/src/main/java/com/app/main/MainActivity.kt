package com.app.main

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val textView = TextView(this).apply {
            text = "✅ Тест сборки успешен!"
            setTextColor(Color.GREEN)
            textSize = 24f
            gravity = Gravity.CENTER
        }

        layout.addView(textView)
        setContentView(layout)
    }
}
