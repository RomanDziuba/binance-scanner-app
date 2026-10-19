package com.app.main

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
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "🎮 Мини-Игра Кликер"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        }

        val scoreView = TextView(this).apply {
            text = "Счет: 0"
            setTextColor(Color.GREEN)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val button = Button(this).apply {
            text = "Нажми меня!"
            setBackgroundColor(Color.parseColor("#3F51B5"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                score++
                scoreView.text = "Счет: $score"
            }
        }

        layout.addView(title)
        layout.addView(scoreView)
        layout.addView(button)
        setContentView(layout)
    }
}
