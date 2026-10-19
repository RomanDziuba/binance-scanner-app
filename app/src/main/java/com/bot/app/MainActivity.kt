package com.bot.app

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
            setBackgroundColor(Color.parseColor("#030712"))
            setPadding(40, 40, 40, 40)
        }

        // Контейнер-карточка с неоновой рамкой для антуража
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 50, 40, 50)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = 32f
                setStroke(2, Color.parseColor("#1E293B"))
            }
        }

        // Имя автора красивым курсивным прописным шрифтом с золотистым оттенком
        val authorView = TextView(this).apply {
            text = "✦ Dziuba Roman ✦"
            setTextColor(Color.parseColor("#F59E0B"))
            textSize = 21f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val titleView = TextView(this).apply {
            text = "⚡ Cyber Neon Clock"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val clock = TextClock(this).apply {
            format12Hour = "hh:mm:ss a"
            format24Hour = "HH:mm:ss"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 48f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }

        val dateView = TextView(this).apply {
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            text = sdf.format(Date())
            setTextColor(Color.parseColor("#64748B"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 32)
        }

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }

        // Кнопка фонарика
        val btnFlash = Button(this).apply {
            text = "🔦 Вспышка / Фонарик"
            setBackgroundColor(Color.parseColor("#D97706"))
            setTextColor(Color.WHITE)
            setPadding(30, 20, 30, 20)
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
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setPadding(30, 20, 30, 20)
            setOnClickListener {
                isDimmed = !isDimmed
                val lp = window.attributes
                lp.screenBrightness = if (isDimmed) 0.05f else -1f
                window.attributes = lp
                Toast.makeText(this@MainActivity, if (isDimmed) "Ночной режим" else "Обычная яркость", Toast.LENGTH_SHORT).show()
            }
        }

        cardLayout.addView(authorView)
        cardLayout.addView(titleView)
        cardLayout.addView(clock)
        cardLayout.addView(dateView)
        
        btnLayout.addView(btnFlash)
        val space = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16)
        }
        btnLayout.addView(space)
        btnLayout.addView(btnBrightness)
        
        cardLayout.addView(btnLayout)
        layout.addView(cardLayout)
        setContentView(layout)
    }
}
