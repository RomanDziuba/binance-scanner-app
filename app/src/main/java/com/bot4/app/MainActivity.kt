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
import kotlin.math.*

class MainActivity : Activity() {

    // ==================== НАСТРОЙКИ TELEGRAM ====================
    private val botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y"
    private val chatId = "1434233986"

    // ==================== НАСТРОЙКИ СКАНИРОВАНИЯ ====================
    private var minVolumeUsdt: Double = 6_000_000.0
    private var volumeSpikeMultiplier: Double = 2.0
    private var minProfitPct: Double = 1.2
    private var maxProfitPct: Double = 3.0
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
    private var countdownSeconds = 300 // 5 минут до следующего скана
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
private fun createStyledButton(text: String, bgColor: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }

            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 12f
            }
            background = drawable
            setOnClickListener { onClick() }
        }
    }

    private fun createStatItem(parent: LinearLayout, label: String, initialVal: String): TextView {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvVal = TextView(this).apply {
            text = initialVal
            setTextColor(Color.parseColor("#34D399"))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val tvLbl = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#64748B"))
            textSize = 11f
            gravity = Gravity.CENTER
        }
        container.addView(tvVal)
        container.addView(tvLbl)
        parent.addView(container)
        return tvVal
    }

    private fun startRealTimeClock() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

        val clockRunnable = object : Runnable {
            override fun run() {
                val now = Date()
                tvClock.text = timeFormat.format(now)
                tvDate.text = dateFormat.format(now)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(clockRunnable)
    }

    private fun startCountdownTimer() {
        val timerRunnable = object : Runnable {
            override fun run() {
                if (!isScanning) {
                    if (countdownSeconds > 0) {
                        countdownSeconds--
                        val mins = countdownSeconds / 60
                        val secs = countdownSeconds % 60
                        val mStr = if (mins < 10) "0$mins" else "$mins"
                        val sStr = if (secs < 10) "0$secs" else "$secs"
                        tvCountdown.text = "⏳ До следующего скана: $mStr:$sStr"
                    } else {
                        countdownSeconds = scanIntervalSeconds
                        runScannerCycle()
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable)
    }

    private fun showSettingsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val title = TextView(this).apply {
            text = "⚙️ Настройки параметров сканера"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        val lblVol = TextView(this).apply {
            text = "Мин. объем USDT:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
        }
        layout.addView(lblVol)

        val inputVol = EditText(this).apply {
            setText(minVolumeUsdt.toInt().toString())
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(16, 16, 16, 16)
        }
        layout.addView(inputVol)
val lblMult = TextView(this).apply {
            text = "Множитель объема свечи:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, 16, 0, 0)
        }
        layout.addView(lblMult)

        val inputMult = EditText(this).apply {
            setText(volumeSpikeMultiplier.toString())
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(16, 16, 16, 16)
        }
        layout.addView(inputMult)

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                try {
                    val newVol = inputVol.text.toString().toDoubleOrNull()
                    val newMult = inputMult.text.toString().toDoubleOrNull()
                    if (newVol != null) minVolumeUsdt = newVol
                    if (newMult != null) volumeSpikeMultiplier = newMult
                    Toast.makeText(this, "✅ Настройки сохранены!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Ошибка в числах!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun sendTelegramMessage(textMessage: String) {
        Thread {
            try {
                val encodedText = java.net.URLEncoder.encode(textMessage, "UTF-8")
                val urlString = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedText&parse_mode=Markdown"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // Расчет индикатора KAMA (Kaufman's Adaptive Moving Average) и ATR
    private fun calculateKamaAndAtr(klines: JSONArray): Triple<Double, Double, Double> {
        val closes = DoubleArray(klines.length())
        val highs = DoubleArray(klines.length())
        val lows = DoubleArray(klines.length())

        for (i in 0 until klines.length()) {
            val candle = klines.getJSONArray(i)
            highs[i] = candle.getDouble(2)
            lows[i] = candle.getDouble(3)
            closes[i] = candle.getDouble(4)
        }

        val n = closes.size
        if (n < kamaPeriod + 1) return Triple(0.0, 0.0, 0.0)

        // Расчет KAMA
        var change = abs(closes[n - 1] - closes[n - 1 - kamaPeriod])
        var volatility = 0.0
        for (i in (n - kamaPeriod) until n) {
            volatility += abs(closes[i] - closes[i - 1])
        }

        val er = if (volatility == 0.0) 0.0 else change / volatility
        val fastSC = 2.0 / (2.0 + 1.0)
        val slowSC = 2.0 / (30.0 + 1.0)
        val sc = (er * (fastSC - slowSC) + slowSC).let { it * it }

        var kama = closes[n - 2] + sc * (closes[n - 1] - closes[n - 2])

        // Расчет ATR (14 периодов)
        var atr = 0.0
        val atrPeriod = min(14, n - 1)
        for (i in (n - atrPeriod) until n) {
            val tr = max(highs[i] - lows[i], max(abs(highs[i] - closes[i - 1]), abs(lows[i] - closes[i - 1])))
            atr += tr
        }
        atr /= atrPeriod

        val lastVolume = klines.getJSONArray(n - 1).getDouble(5)
        var avgVolume = 0.0
        for (i in (n - 20) until n) {
            avgVolume += klines.getJSONArray(i).getDouble(5)
        }
        avgVolume /= 20.0

        return Triple(er, atr, lastVolume / avgVolume)
    }

    private fun runScannerCycle() {
        if (isScanning) return
        isScanning = true
        tvStatus.text = "⚡ Статус: Сканирование Binance Futures..."
        progressBar.progress = 10
Thread {
            try {
                // 1. Загрузка 24ч тикеров
                val tickerUrl = URL("https://fapi.binance.com/fapi/v1/ticker/24hr")
                val connection = tickerUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseJson)

                val validPairs = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val symbol = item.getString("symbol")
                    if (symbol.endsWith("USDT")) {
                        val volumeUsdt = item.getDouble("quoteVolume")
                        if (volumeUsdt >= minVolumeUsdt) {
                            validPairs.add(symbol)
                        }
                    }
                }

                handler.post {
                    tvStatsScanned.text = validPairs.size.toString()
                    progressBar.progress = 30
                    tvStatus.text = "🔍 Найдено ликвидных пар: ${validPairs.size}. Анализ свечей..."
                }

                var passedCount = 0
                var activeSignalsCount = 0
                val logBuilder = StringBuilder()
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                logBuilder.append("[$timeStr] Сканирование завершено. Активных пар: ${validPairs.size}\n")

                // Анализируем топ-25 пар по объему для глубокой проверки KAMA & ATR
                val topPairs = validPairs.take(25)
                val step = 70 / max(1, topPairs.size)

                for ((idx, symbol) in topPairs.withIndex()) {
                    try {
                        // Загружаем часовые свечи (klines) для реального анализа
                        val klinesUrl = URL("https://fapi.binance.com/fapi/v1/klines?symbol=$symbol&interval=1h&limit=30")
                        val klinesConn = klinesUrl.openConnection() as HttpURLConnection
                        klinesConn.connectTimeout = 3000
                        klinesConn.readTimeout = 3000
                        val klinesJson = klinesConn.inputStream.bufferedReader().use { it.readText() }
                        val klines = JSONArray(klinesJson)

                        if (klines.length() >= 15) {
                            passedCount++
                            val (er, atr, volRatio) = calculateKamaAndAtr(klines)

                            // Условия торговой стратегии (KAMA Efficiency + Volume Spike)
                            if (er >= minEfficiencyRatio && volRatio >= volumeSpikeMultiplier) {
                                activeSignalsCount++
                                val lastCandle = klines.getJSONArray(klines.length() - 1)
                                val currentPrice = lastCandle.getDouble(4)
                                val sl = currentPrice - (atr * 1.5)
                                val tp = currentPrice + (atr * 2.5)
                                val profitPct = ((tp - currentPrice) / currentPrice) * 100

                                val signalMsg = """
                                    🚨 *BINANCE KAMA SIGNAL* 🚨
                                    • 🪙 Монета: $symbol
                                    • 📈 Вход (Entry): $currentPrice
                                    • 🛑 Стоп-Лосс (SL): ${String.format(Locale.US, "%.4f", sl)}
                                    • 🎯 Тейк-Профит (TP): ${String.format(Locale.US, "%.4f", tp)}
                                    • 💰 Ожидаемый профит: +${String.format(Locale.US, "%.2f", profitPct)}%
                                    • 📊 KAMA ER: ${String.format(Locale.US, "%.2f", er)}
                                    • ⚡ Объем (Spike): x${String.format(Locale.US, "%.1f", volRatio)}
                                """.trimIndent()
logBuilder.append("🎯 СИГНАЛ: $symbol | Entry: $currentPrice \vert{} TP: +${String.format(Locale.US, "%.2f", profitPct)}%\n")
                                sendTelegramMessage(signalMsg)
                            }
                        }
                    } catch (e: Exception) {
                        // Пропускаем проблемную пару при сбое сети
                    }

                    val currentProg = 30 + (idx + 1) * step
                    handler.post {
                        progressBar.progress = min(95, currentProg)
                    }
                    Thread.sleep(100) // Паауза во избежание Rate Limit от Binance API
                }

                handler.post {
                    tvStatsPassed.text = passedCount.toString()
                    tvStatsSignals.text = activeSignalsCount.toString()
                    progressBar.progress = 100
                    tvStatus.text = "✅ Сканирование и анализ завершены!"
                    tvConsole.text = logBuilder.toString()
                    isScanning = false
                    countdownSeconds = scanIntervalSeconds
                }

            } catch (e: Exception) {
                handler.post {
                    tvStatus.text = "❌ Ошибка соединения с Binance API"
                    tvConsole.text = "Ошибка: ${e.message}"
                    isScanning = false
                    countdownSeconds = scanIntervalSeconds
                }
            }
        }.start()
    }
}