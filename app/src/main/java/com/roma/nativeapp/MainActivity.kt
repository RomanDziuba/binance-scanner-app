package com.roma.nativeapp

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private var flashOn = false
    private var isDimmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#050811"))
            setPadding(40, 40, 40, 40)
        }

        val authorView = TextView(this).apply {
            text = "✨ Roma Dziuba ✨"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        val titleView = TextView(this).apply {
            text = "🕒 Smart Multi-Clock"
            setTextColor(Color.WHITE)
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val clock = TextClock(this).apply {
            format12Hour = "hh:mm:ss a"
            format24Hour = "HH:mm:ss"
            setTextColor(Color.parseColor("#4ADE80"))
            textSize = 52f
            gravity = Gravity.CENTER
        }

        val dateView = TextView(this).apply {
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            text = sdf.format(Date())
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }

        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

        // Кнопка будильника
        val btnAlarm = Button(this).apply {
            text = "⏰ Установить будильник (+1 мин)"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                Toast.makeText(this@MainActivity, "Будильник заведен на 1 минуту!", Toast.LENGTH_LONG).show()
            }
        }

        // Кнопка фонарика
        val btnFlash = Button(this).apply {
            text = "🔦 Фонарик / Вспышка"
            setBackgroundColor(Color.parseColor("#D97706"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                try {
                    val camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = camManager.cameraIdList[0]
                    flashOn = !flashOn
                    camManager.setTorchMode(cameraId, flashOn)
                    Toast.makeText(this@MainActivity, if (flashOn) "Фонарик ВКЛ" else "Фонарик ВЫКЛ", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Ошибка фонарика (нет вспышки)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Кнопка ночной яркости
        val btnBrightness = Button(this).apply {
            text = "🌙 Режим темноты / Яркость"
            setBackgroundColor(Color.parseColor("#475569"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                isDimmed = !isDimmed
                val lp = window.attributes
                lp.screenBrightness = if (isDimmed) 0.05f else -1f
                window.attributes = lp
                Toast.makeText(this@MainActivity, if (isDimmed) "Ночной режим" else "Обычная яркость", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(authorView)
        layout.addView(titleView)
        layout.addView(clock)
        layout.addView(dateView)
        layout.addView(btnAlarm)
        layout.addView(btnFlash)
        layout.addView(btnBrightness)
        setContentView(layout)
    }
}
