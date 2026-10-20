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
for ((idx, symbol) in topPairs.withIndex()) {
                    try {
                        val klinesUrl = URL("https://fapi.binance.com/fapi/v1/klines?symbol=$symbol&interval=1h&limit=25")
                        val klinesConn = klinesUrl.openConnection() as HttpURLConnection
                        klinesConn.connectTimeout = 3000
                        klinesConn.readTimeout = 3000
                        val klinesJson = klinesConn.inputStream.bufferedReader().use { it.readText() }
                        val klines = JSONArray(klinesJson)

                        if (klines.length() >= 10) {
                            passedCount++
                            
                            val len = klines.length()
                            val lastCandle = klines.getJSONArray(len - 1)
                            val currentPrice = lastCandle.getString(4).toDouble()
                            val prevPrice = klines.getJSONArray(len - 2).getString(4).toDouble()
                            
                            val lastVol = lastCandle.getString(5).toDouble()
                            var avgVol = 0.0
                            for (k in 0 until len) {
                                avgVol += klines.getJSONArray(k).getString(5).toDouble()
                            }
                            avgVol /= len.toDouble()
                            val volRatio = if (avgVol == 0.0) 1.0 else lastVol / avgVol

                            if (currentPrice > prevPrice && volRatio >= volumeSpikeMultiplier) {
                                activeSignalsCount++
                                val atr = currentPrice * 0.012
                                val sl = currentPrice - (atr * 1.5)
                                val tp = currentPrice + (atr * 2.5)
                                val profitPct = ((tp - currentPrice) / currentPrice) * 100

                                val slStr = java.lang.String.format(Locale.US, "%.4f", sl)
                                val tpStr = java.lang.String.format(Locale.US, "%.4f", tp)
                                val profitStr = java.lang.String.format(Locale.US, "%.2f", profitPct)
                                val volStr = java.lang.String.format(Locale.US, "%.1f", volRatio)

                                val signalMsg = """
                                    🚨 *BINANCE KAMA SIGNAL* 🚨
                                    • 🪙 Монета: $symbol
                                    • 📈 Вход (Entry): $currentPrice
                                    • 🛑 Стоп-Лосс (SL): $slStr
                                    • 🎯 Тейк-Профит (TP): $tpStr
                                    • 💰 Ожидаемый профит: +$profitStr%
                                    • ⚡ Объем (Spike): x$volStr
                                """.trimIndent()

                                logBuilder.append("🎯 СИГНАЛ: $symbol | Entry: $currentPrice \vert{} TP: +$profitStr%\n")
                                sendTelegramMessage(signalMsg)
                            }
                        }
                    } catch (e: Exception) {
                        // Пропуск при сетевой ошибке
                    }

                    val currentProg = 30 + (idx + 1) * step
                    handler.post {
                        progressBar.progress = Math.min(95, currentProg)
                    }
                    Thread.sleep(60)
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