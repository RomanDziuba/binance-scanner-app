package com.roma.nativeapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(40, 40, 40, 40)
        }
        val tv = TextView(this).apply {
            text = "✦ Dziuba Roman — Пользовательский код ✦"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
        }
        layout.addView(tv)
        setContentView(layout)
    }
}
