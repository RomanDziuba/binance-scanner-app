package com.bot2.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    private var score = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(40, 40, 40, 40)
        }

        val titleView = TextView(this).apply {
            text = "🎮 Мини-Кликкер на Kotlin"
            setTextColor(Color.CYAN)
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val scoreView = TextView(this).apply {
            text = "Очки: 0"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        val btn = Button(this).apply {
            text = "Кликни меня!"
            setBackgroundColor(Color.parseColor("#FF6200EE"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                score++
                scoreView.text = "Очки: $score"
            }
        }

        layout.addView(titleView)
        layout.addView(scoreView)
        layout.addView(btn)
        setContentView(layout)
    }
}
