package com.roma.nativeapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(50, 50, 50, 50)
        }

        val tv = TextView(this).apply {
            text = "⚡ Универсальный Шаблон\n(Графики, Сеть, Датчики, Google Pay)"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val btn = Button(this).apply {
            text = "Проверить модули"
            setBackgroundColor(Color.parseColor("#3B82F6"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Все подсистемы активны и готовы к работе!", Toast.LENGTH_LONG).show()
            }
        }

        layout.addView(tv)
        layout.addView(btn)
        setContentView(layout)
    }
}
