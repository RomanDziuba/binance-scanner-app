package com.roma.nativeapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextClock
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color
import android.media.ToneGenerator
import android.media.AudioManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "🕒 Цифровые Часы"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val clock = TextClock(this).apply {
            format12Hour = "hh:mm:ss a"
            format24Hour = "HH:mm:ss"
            setTextColor(Color.WHITE)
            textSize = 50f
            gravity = Gravity.CENTER
        }

        val dateView = TextView(this).apply {
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            text = sdf.format(Date())
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 50)
        }

        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        val btnSound = Button(this).apply {
            text = "🔊 Проверить звук"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            }
        }

        layout.addView(title)
        layout.addView(clock)
        layout.addView(dateView)
        layout.addView(btnSound)
        setContentView(layout)
    }
}
