package com.roma.nativeapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#030712"))
            setPadding(40, 40, 40, 40)
        }

        val titleView = TextView(this).apply {
            text = "✨ Dziuba Roman — Binance Scanner ✨"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val statusView = TextView(this).apply {
            text = "🚀 Система готова к работе!"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 16f
            gravity = Gravity.CENTER
        }

        layout.addView(titleView)
        layout.addView(statusView)
        setContentView(layout)
    }
}