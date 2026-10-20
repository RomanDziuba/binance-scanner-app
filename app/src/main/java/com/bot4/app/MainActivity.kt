package com.bot4.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    // ==================== НАСТРОЙКИ TELEGRAM ====================
    private val botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y"
    private val chatId = "1434233986"

    // ==================== НАСТРОЙКИ СКАНИРОВАНИЯ ====================
    private var minVolumeUsdt: Double = 6_000_000.0
    private var volumeSpikeMultiplier: Double = 2.0
    private var kamaPeriod: Int = 10
    private var minEfficiencyRatio: Double = 0.25

    // UI Элементы
    private lateinit var tvClock: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatsScanned: TextView
    private lateinit var tvStatsPassed: TextView
    private lateinit var tvStatsSignals: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvConsole: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var countdownSeconds = 300
    private val scanIntervalSeconds = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#030712"))
            setPadding(32, 32, 32, 32)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 1. Авторская подпись
        val authorView = TextView(this).apply {
            text = "✨ 𝒟𝓩𝐼𝒰𝐵𝒜  𝑅𝒪𝑀𝒜 ✨"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 20f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        // 2. Заголовок
        val titleView = TextView(this).apply {
            text = "🚀 Binance KAMA-Protected Scanner"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // 3. Блок Часов и Даты
        val clockCard = createCardLayout("#0F172A", "#1E293B")
        tvClock = TextView(this).apply {
            text = "00:00:00"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 38f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        tvDate = TextView(this).apply {
            text = "Загрузка даты..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
        clockCard.addView(tvClock)
        clockCard.addView(tvDate)
// 4. Панель Статистики и Таймера
        val statsCard = createCardLayout("#0F172A", "#1E293B")
        
        tvStatus = TextView(this).apply {
            text = "⚡ Статус: Ожидание запуска..."
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        }

        tvCountdown = TextView(this).apply {
            text = "⏳ До следующего скана: 05:00"
            setTextColor(Color.parseColor("#F59E0B"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val statsGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }
        tvStatsScanned = createStatItem(statsGrid, "Просканировано", "0")
        tvStatsPassed = createStatItem(statsGrid, "Прошли фильтр", "0")
        tvStatsSignals = createStatItem(statsGrid, "Сигналы", "0")

        statsCard.addView(tvStatus)
        statsCard.addView(tvCountdown)
        statsCard.addView(progressBar)
        statsCard.addView(statsGrid)

        // 5. Кнопки управления
        val btnScanNow = createStyledButton("🚀 Запустить сканирование сейчас", "#2563EB") {
            if (!isScanning) {
                runScannerCycle()
            } else {
                Toast.makeText(this, "Сканирование уже выполняется!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnSettings = createStyledButton("⚙️ Настройки параметров сканера", "#D97706") {
            showSettingsDialog()
        }

        // 6. Консоль логов
        val consoleCard = createCardLayout("#090D16", "#1E293B")
        val consoleTitle = TextView(this).apply {
            text = "📋 Консоль сигналов и логирования"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        tvConsole = TextView(this).apply {
            text = "[Система] Инициализация сканера KAMA-Protected...\n[Готовность] Нажмите запуск для сканирования Binance Futures."
            setTextColor(Color.parseColor("#34D399"))
            textSize = 12f
            setTypeface(Typeface.MONOSPACE)
        }
        consoleCard.addView(consoleTitle)
        consoleCard.addView(tvConsole)

        contentLayout.addView(authorView)
        contentLayout.addView(titleView)
        contentLayout.addView(clockCard)
        contentLayout.addView(statsCard)
        contentLayout.addView(btnScanNow)
        contentLayout.addView(btnSettings)
        contentLayout.addView(consoleCard)

        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        startRealTimeClock()
        startCountdownTimer()
    }

    private fun createCardLayout(bgColor: String, borderColor: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }

            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 16f
                setStroke(2, Color.parseColor(borderColor))
            }
            background = drawable
        }
    }