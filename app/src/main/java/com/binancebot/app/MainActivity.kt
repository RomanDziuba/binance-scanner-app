package com.binancebot.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    // ==================== НАСТРОЙКИ СТРАТЕГИИ ====================
    private var minVolumeUsdt = 6_000_000.0
    private var filterLowVolume = true
    private var volumeSpikeMultiplier = 2.0
    private var filterRiskReward = true
    private var maxSlTpRatio = 1.0
    private var filterMinProfit = true
    private var minProfitPct = 1.2
    private var filterMaxProfit = true
    private var maxProfitPct = 3.0
    private var filterBtcTrend = true
    private var filterLiquiditySweep = true
    private var sweepMinAtrRatio = 0.2
    private var sweepMaxAtrRatio = 1.5
    private var filterTakerRatio = true
    private var minTakerBuyRatio = 0.65
    private var filterAtrSpike = true
    private var maxAtrMultiplier = 2.5
    private var filterFundingRate = true
    private var maxAbsoluteFundingRate = 0.0005
    private var filterKamaEfficiency = true
    private var kamaErPeriod = 10
    private var minEfficiencyRatio = 0.30
    private var filterKamaTrendGuard = true

    // Телеграм настройки
    private val botToken = "8740928704:AAGBdKD81m8K9dqbwH4OviaizzCzT-BkVrI"
    private val chatId = "1434233986"
    private val sendDetailedInfo = true

    private val timeframeMain = "15m"
    private val timeframeConf = "30m"
    private val baseLookback = 64
    private val emaPeriod = 64
    private val limit = 200

    // UI элементы
    private lateinit var tvStatus: TextView
    private lateinit var tvPairsCount: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvConsole: TextView
    private lateinit var btnScanNow: Button

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(10)
    private val lastAlertTime = mutableMapOf<String, Long>()
    
    private var btcBullish = true
    private var btcBearish = false
    private val fundingRateCache = mutableMapOf<String, Double>()
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#030712"))
            setPadding(30, 30, 30, 30)
        }

        val scrollContainer = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Заголовок и автор
        val authorView = TextView(this).apply {
            text = "✨ 𝒟𝓩𝐼𝒰𝐵𝒜  𝑅𝒪𝑀𝒜 ✨"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 20f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        val titleView = TextView(this).apply {
            text = "🚀 Binance Futures KAMA Scanner"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
// Часы реального времени
        val clockView = TextClock(this).apply {
            format12Hour = "hh:mm:ss a"
            format24Hour = "HH:mm:ss"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // Карточка статуса
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(24, 24, 24, 24)
        }

        tvStatus = TextView(this).apply {
            text = "🟢 Статус: Готов к работе"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        tvPairsCount = TextView(this).apply {
            text = "📊 Просканировано пар: 0"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        tvCountdown = TextView(this).apply {
            text = "⏳ До след. скана: 05:00"
            setTextColor(Color.parseColor("#FBBF24"))
            textSize = 14f
            setPadding(0, 0, 0, 12)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16)
        }

        cardLayout.addView(tvStatus)
        cardLayout.addView(tvPairsCount)
        cardLayout.addView(tvCountdown)
        cardLayout.addView(progressBar)

        // Кнопки управления
        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        btnScanNow = Button(this).apply {
            text = "⚡ Скан сейчас"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (!isScanning) {
                    startScanCycle()
                } else {
                    Toast.TestCase("Сканирование уже выполняется!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnSettings = Button(this).apply {
            text = "⚙️ Настройки"
            setBackgroundColor(Color.parseColor("#475569"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                showSettingsDialog()
            }
        }

        val btnParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(8, 0, 8, 0)
        }
        btnScanNow.layoutParams = btnParams
        btnSettings.layoutParams = btnParams

        btnLayout.addView(btnScanNow)
        btnLayout.addView(btnSettings)

        // Терминал / Консоль
        val terminalLabel = TextView(this).apply {
            text = "📋 Терминал сигналов:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, 10, 0, 6)
        }

        tvConsole = TextView(this).apply {
            text = "[System] Инициализация сканера KAMA...\n[System] Ожидание запуска цикла."
            setTextColor(Color.parseColor("#34D399"))
            textSize = 12f
            setBackgroundColor(Color.parseColor("#020617"))
            setPadding(16, 16, 16, 16)
            typeface = Typeface.MONOSPACE
        }

        contentLayout.addView(authorView)
        contentLayout.addView(titleView)
        contentLayout.addView(clockView)
        contentLayout.addView(cardLayout)
        contentLayout.addView(btnLayout)
        contentLayout.addView(terminalLabel)
        contentLayout.addView(tvConsole)

        scrollContainer.addView(contentLayout)
        rootLayout.addView(scrollContainer)
        setContentView(rootLayout)

        // Автозапуск сканера в фоне
        startBackgroundLoop()
    }
private fun logToConsole(msg: String) {
        handler.post {
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val currentText = tvConsole.text.toString()
            val newText = "[$timeStr] $msg\n$currentText"
            val lines = newText.split("\n")
            if (lines.size > 50) {
                tvConsole.text = lines.take(50).joinToString("\n")
            } else {
                tvConsole.text = newText
            }
        }
    }

    private fun showSettingsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }

        val inputVol = EditText(this).apply {
            setText(minVolumeUsdt.toInt().toString())
            hint = "Мин. объем USDT"
        }
        val inputMinProfit = EditText(this).apply {
            setText(minProfitPct.toString())
            hint = "Мин. профит (%)"
        }
        val inputMaxProfit = EditText(this).apply {
            setText(maxProfitPct.toString())
            hint = "Макс. профит (%)"
        }

        layout.addView(TextView(this).apply { text = "Мин. объем пары (USDT):" })
        layout.addView(inputVol)
        layout.addView(TextView(this).apply { text = "Минимальный профит (%):" })
        layout.addView(inputMinProfit)
        layout.addView(TextView(this).apply { text = "Максимальный профит (%):" })
        layout.addView(inputMaxProfit)

        AlertDialog.Builder(this)
            .setTitle("⚙️ Параметры сканера")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                try {
                    minVolumeUsdt = inputVol.text.toString().toDouble()
                    minProfitPct = inputMinProfit.text.toString().toDouble()
                    maxProfitPct = inputMaxProfit.text.toString().toDouble()
                    Toast.makeText(this, "✅ Настройки обновлены!", Toast.LENGTH_SHORT).show()
                    logToConsole("Настройки обновлены пользователем.")
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Ошибка ввода данных!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun startBackgroundLoop() {
        Thread {
            while (true) {
                try {
                    runScanCycleInternal()
                    // Ожидание 5 минут (300 секунд)
                    for (i in 300 downTo 1) {
                        val mins = i / 60
                        val secs = i % 60
                        handler.post {
                            tvCountdown.text = String.format("⏳ До след. скана: %02d:%02d", mins, secs)
                        }
                        Thread.sleep(1000)
                    }
                } catch (e: Exception) {
                    logToConsole("Ошибка в фоновом цикле: ${e.message}")
                    Thread.sleep(10000)
                }
            }
        }.start()
    }

    private fun startScanCycle() {
        executor.execute {
            runScanCycleInternal()
        }
    }

    private fun runScanCycleInternal() {
        if (isScanning) return
        isScanning = true
        handler.post {
            tvStatus.text = "🔄 Статус: Сканирование рынка..."
            progressBar.isIndeterminate = true
        }

        logToConsole("Запуск сканирования Binance Futures...")

        try {
            updateMarketContext()
            val pairs = getActiveFuturesPairs()
            logToConsole("Найдено ликвидных пар (объем >= ${minVolumeUsdt.toInt()}): ${pairs.size}")

            handler.post {
                tvPairsCount.text = "📊 Просканировано пар: ${pairs.size}"
            }

            for (symbol in pairs) {
                processPair(symbol)
            }
logToConsole("✅ Сканирование успешно завершено.")
        } catch (e: Exception) {
            logToConsole("❌ Ошибка при сканировании: ${e.message}")
        } finally {
            isScanning = false
            handler.post {
                tvStatus.text = "🟢 Статус: Ожидание таймера"
                progressBar.isIndeterminate = false
                progressBar.progress = 100
            }
        }
    }

    private fun getActiveFuturesPairs(): List<String> {
        val urlStr = "https://fapi.binance.com/fapi/v1/ticker/24hr"
        val validPairs = mutableListOf<String>()
        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val jsonArray = JSONArray(response.toString())
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val symbol = item.getString("symbol")
                if (symbol.endsWith("USDT")) {
                    val volUsdt = item.getDouble("quoteVolume")
                    if (volUsdt >= minVolumeUsdt) {
                        validPairs.add(symbol)
                    }
                }
            }
        } catch (e: Exception) {
            logToConsole("Ошибка получения списка пар: ${e.message}")
        }
        return validPairs
    }

    private fun updateMarketContext() {
        try {
            // Тренд BTC
            val btcKlines = fetchKlines("BTCUSDT", timeframeConf, 50)
            if (btcKlines != null && btcKlines.length() > emaPeriod) {
                val closes = mutableListOf<Double>()
                for (i in 0 until btcKlines.length()) {
                    closes.add(btcKlines.getJSONArray(i).getString(4).toDouble())
                }
                val emaBtc = calculateEma(closes, emaPeriod)
                val currBtc = closes[closes.size - 2]
                val prevEma = emaBtc[emaBtc.size - 2]
                btcBullish = currBtc > prevEma
                btcBearish = currBtc < prevEma
            }

            // Ставки финансирования
            if (filterFundingRate) {
                val conn = URL("https://fapi.binance.com/fapi/v1/premiumIndex").openConnection() as HttpURLConnection
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val resp = StringBuilder()
                var l: String?
                while (reader.readLine().also { l = it } != null) resp.append(l)
                reader.close()

                val arr = JSONArray(resp.toString())
                synchronized(fundingRateCache) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        fundingRateCache[obj.getString("symbol")] = obj.optDouble("lastFundingRate", 0.0)
                    }
                }
            }
        } catch (e: Exception) {
            // Игнорируем сетевые сбои контекста
        }
    }

    private fun fetchKlines(symbol: String, timeframe: String, lim: Int): JSONArray? {
        try {
            val urlStr = "https://fapi.binance.com/fapi/v1/klines?symbol=$symbol&interval=$timeframe&limit=$lim"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val resp = StringBuilder()
            var l: String?
            while (reader.readLine().also { l = it } != null) resp.append(l)
            reader.close()
            return JSONArray(resp.toString())
        } catch (e: Exception) {
            return null
        }
    }
private fun calculateEma(data: List<Double>, period: Int): List<Double> {
        val emaList = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1)
        var prevEma = data[0]
        emaList.add(prevEma)
        for (i in 1 until data.size) {
            val curr = (data[i] - prevEma) * multiplier + prevEma
            emaList.add(curr)
            prevEma = curr
        }
        return emaList
    }

    private fun processPair(symbol: String) {
        val klines15 = fetchKlines(symbol, timeframeMain, limit) ?: return
        val klines30 = fetchKlines(symbol, timeframeConf, limit) ?: return
        if (klines15.length() < limit || klines30.length() < limit) return

        // Парсинг свечей 15m
        val dfClose15 = mutableListOf<Double>()
        val dfHigh15 = mutableListOf<Double>()
        val dfLow15 = mutableListOf<Double>()
        val dfOpen15 = mutableListOf<Double>()
        val dfVol15 = mutableListOf<Double>()
        val dfTbBase15 = mutableListOf<Double>()

        for (i in 0 until klines15.length()) {
            val row = klines15.getJSONArray(i)
            dfOpen15.add(row.getString(1).toDouble())
            dfHigh15.add(row.getString(2).toDouble())
            dfLow15.add(row.getString(3).toDouble())
            dfClose15.add(row.getString(4).toDouble())
            dfVol15.add(row.getString(5).toDouble())
            dfTbBase15.add(row.getString(9).toDouble())
        }

        val currPrice = dfClose15.last()
        val cIdx = dfClose15.size - 2
        val cOpen = dfOpen15[cIdx]
        val cHigh = dfHigh15[cIdx]
        val cLow = dfLow15[cIdx]
        val cClose = dfClose15[cIdx]
        val cVol = dfVol15[cIdx]
        val cTbBase = dfTbBase15[cIdx]

        // Расчет ATR и Мюррея
        val m15 = calcMurreyMath(dfHigh15, dfLow15, dfClose15, baseLookblock = baseLookback)
        val step15 = m15["step"] ?: 1.0
        val atr15 = m15["atr"] ?: 0.0

        val m30Data = parseKlinesSimple(klines30)
        val m30 = calcMurreyMath(m30Data.highs, m30Data.lows, m30Data.closes, baseLookblock = baseLookback)

        // Проверка фильтров
        val avgVol15 = dfVol15.takeLast(21).dropLast(1).average()
        val isHighVol15 = cVol > (avgVol15 * volumeSpikeMultiplier)

        if (filterLowVolume && !isHighVol15) return

        val takerRatio = if (cVol > 0) cTbBase / cVol else 0.5
        if (filterTakerRatio && takerRatio < minTakerBuyRatio && takerRatio > (1.0 - minTakerBuyRatio)) return

        // Проверка уровней Мюррея и сигналов
        var isSignal = false
        var direction = "" // "bullish" or "bearish"
        var tpMin = 0.0
        var tpMax = 0.0
        var slLevel = 0.0

        val level5_8 = m15["5_8"] ?: 0.0
        val level3_8 = m15["3_8"] ?: 0.0
        val level0_8 = m15["0_8"] ?: 0.0
        val level8_8 = m15["8_8"] ?: 0.0

        if (currPrice in level3_8..level5_8) {
            if (checkSweep(cLow, cHigh, cClose, cOpen, level5_8, true, atr15)) {
                isSignal = true
                direction = "bullish"
                tpMin = m15["7_8"] ?: (currPrice * 1.01)
                tpMax = m15["8_8"] ?: (currPrice * 1.02)
                slLevel = m15["_-2_8"] ?: (currPrice * 0.98)
            } else if (checkSweep(cLow, cHigh, cClose, cOpen, level3_8, false, atr15)) {
                isSignal = true
                direction = "bearish"
                tpMin = m15["0_8"] ?: (currPrice * 0.99)
                tpMax = m15["1_8"] ?: (currPrice * 0.98)
                slLevel = m15["_+2_8"] ?: (currPrice * 1.02)
            }
        }

        if (!isSignal) return

        // Проверка потенциала профита
        val tpMinPct = abs(tpMin - currPrice) / currPrice * 100.0
        val slPct = abs(slLevel - currPrice) / currPrice * 100.0

        if (filterMinProfit && tpMinPct < minProfitPct) return
        if (filterMaxProfit && tpMinPct > maxProfitPct) return
        if (filterRiskReward && slPct > (tpMinPct * maxSlTpRatio)) return
if (filterBtcTrend && symbol != "BTCUSDT") {
            if (direction == "bullish" && !btcBullish) return
            if (direction == "bearish" && !btcBearish) return
        }

        // Отправка сигнала в Telegram
        val closedTime = klines15.getJSONArray(cIdx).getLong(0)
        synchronized(lastAlertTime) {
            if (lastAlertTime[symbol] == closedTime) return
            lastAlertTime[symbol] = closedTime
        }

        val dirStr = if (direction == "bullish") "📈 ЛОНГ" else "📉 ШОРТ"
        val msg = "🔔 СИГНАЛ KAMA SCANNER 🔔\n" +
                "$symbol **** $dirStr\n" +
                "💰 ВХОД: %.4f\n".format(currPrice) +
                "🎯 Тейк-Профит: %.4f - %.4f (+%.2f%%)\n".format(tpMin, tpMax, tpMinPct) +
                "🛡️ Стоп-Лосс: %.4f (-%.2f%%)".format(slLevel, slPct)

        sendTelegramAlert(symbol, msg)
        logToConsole("🎯 СИГНАЛ ОБНАРУЖЕН: $symbol ($dirStr) | Вход: $currPrice")
    }

    private fun checkSweep(low: Double, high: Double, close: Double, open: Double, level: Double, isBullish: Boolean, atr: Double): Boolean {
        if (!filterLiquiditySweep) {
            return if (isBullish) close > level && open <= level else close < level && open >= level
        }
        val effAtr = if (atr > 0) atr else (high - low)
        if (isBullish) {
            if (low < level && close > level) {
                val ratio = (level - low) / effAtr
                if (ratio in sweepMinAtrRatio..sweepMaxAtrRatio) return true
            }
        } else {
            if (high > level && close < level) {
                val ratio = (high - level) / effAtr
                if (ratio in sweepMinAtrRatio..sweepMaxAtrRatio) return true
            }
        }
        return false
    }

    private class SimpleKlines(val highs: List<Double>, val lows: List<Double>, val closes: List<Double>)

    private fun parseKlinesSimple(klines: JSONArray): SimpleKlines {
        val hs = mutableListOf<Double>()
        val ls = mutableListOf<Double>()
        val cs = mutableListOf<Double>()
        for (i in 0 until klines.length()) {
            val r = klines.getJSONArray(i)
            hs.add(r.getString(2).toDouble())
            ls.add(r.getString(3).toDouble())
            cs.add(r.getString(4).toDouble())
        }
        return SimpleKlines(hs, ls, cs)
    }

    private fun calcMurreyMath(highs: List<Double>, lows: List<Double>, closes: List<Double>, baseLookblock: Int): Map<String, Double> {
        val trList = mutableListOf<Double>()
        for (i in 1 until highs.size) {
            val hl = highs[i] - lows[i]
            val hc = abs(highs[i] - closes[i - 1])
            val lc = abs(lows[i] - closes[i - 1])
            trList.add(max(hl, max(hc, lc)))
        }
        val atr = if (trList.isNotEmpty()) trList.takeLast(14).average() else 1.0
        val hh = highs.takeLast(baseLookblock).maxOrNull() ?: 1.0
        val ll = lows.takeLast(baseLookblock).minOrNull() ?: 0.0
        val range = hh - ll
        val step = range / 8.0

        return mapOf(
            "step" to step,
            "atr" to atr,
            "0_8" to ll,
            "1_8" to (ll + step),
            "3_8" to (ll + 3 * step),
            "5_8" to (ll + 5 * step),
            "7_8" to (ll + 7 * step),
            "8_8" to hh,
            "_+2_8" to (hh + 2 * step),
            "_-2_8" to (ll - 2 * step)
        )
    }

    private fun sendTelegramAlert(symbol: String, message: String) {
        Thread {
            try {
                val urlStr = "https://api.telegram.org/bot$botToken/sendMessage"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", message)
                    put("parse_mode", "HTML")
                }
val os = conn.outputStream
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                conn.inputStream.close()
                logToConsole("✅ Сигнал для $symbol отправлен в Telegram.")
            } catch (e: Exception) {
                logToConsole("❌ Ошибка отправки в Telegram: ${e.message}")
            }
        }.start()
    }
}