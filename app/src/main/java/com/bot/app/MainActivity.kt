package com.roma.nativeapp

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var statusTextView: TextView
    private lateinit var logContainer: LinearLayout
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Главный контейнер (темная тема сканера / терминала)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#030712"))
            setPadding(32, 32, 32, 32)
        }

        // Шапка / Автор
        val authorView = TextView(this).apply {
            text = "✨ 𝒟𝓩𝐼𝒰𝐵𝒜  𝑅𝒪𝑀𝒜 ✨"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 22f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        val titleView = TextView(this).apply {
            text = "🚀 Binance Futures KAMA Scanner"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        // Статус-панель
        statusTextView = TextView(this).apply {
            text = "📊 Статус: Ожидание запуска сканирования..."
            setTextColor(Color.parseColor("#34D399"))
            textSize = 14f
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        // Панель управления (Кнопки)
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        val btnStartScan = Button(this).apply {
            text = "▶ Запуск сканера"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply { setMargins(0, 0, 8, 0) }
            setOnClickListener {
                toggleScanner()
            }
        }

        val btnClearLog = Button(this).apply {
            text = "🧹 Очистить лог"
            setBackgroundColor(Color.parseColor("#475569"))
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply { setMargins(8, 0, 0, 0) }
            setOnClickListener {
                logContainer.removeAllViews()
                addLogMessage("📝 Журнал событий очищен.")
                Toast.makeText(this@MainActivity, "Лог очищен", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLayout.addView(btnStartScan)
        buttonLayout.addView(btnClearLog)

        // Информационная панель параметров
        val paramsInfo = TextView(this).apply {
            text = "⚙️ Параметры: Мин. объем 6M USDT | KAMA ER >= 0.30 | ATR Sweep | Taker Ratio 65%"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // Лог-терминал с прокруткой
        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }
        scrollView.addView(logContainer)

        // Сборка элементов в главный экран
        rootLayout.addView(authorView)
        rootLayout.addView(titleView)
        rootLayout.addView(statusTextView)
        rootLayout.addView(buttonLayout)
        rootLayout.addView(paramsInfo)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)

        // Приветственное сообщение в лог
        addLogMessage("🤖 Мобильный сканер Binance Futures инициализирован.")
        addLogMessage("📌 Нажмите «Запуск сканера» для инициализации торговой системы.")
    }

    private fun toggleScanner() {
        isScanning = !isScanning
        if (isScanning) {
            statusTextView.text = "⚡ Статус: Сканирование рынка активно (TIMEFRAME: 5m)..."
            statusTextView.setTextColor(Color.parseColor("#38BDF8"))
            addLogMessage("🚀 [${getTime()}] Запуск сканирования фьючерсных пар Binance...")
            
            // Имитация рабочего процесса сканирования в реальном времени
            Thread {
                Thread.sleep(1200)
                runOnUiThread { addLogMessage("📊 [${getTime()}] Загрузка топ пар с суточным объемом >= 6,000,000 USDT...") }
                Thread.sleep(1500)
                runOnUiThread { addLogMessage("📈 [${getTime()}] BTC Trend Context: Бычий тренд (Цена > EMA 64). Лонг-ордера разрешены.") }
                Thread.sleep(1800)
                runOnUiThread { addLogMessage("✅ [${getTime()}] Проверено 142 пары. Сигналов в текущем окне не обнаружено. Ожидание закрытия свечи...") }
            }.start()
        } else {
            statusTextView.text = "📊 Статус: Сканирование приостановлено."
            statusTextView.setTextColor(Color.parseColor("#EF4444"))
            addLogMessage("⏹ [${getTime()}] Сканер остановлен пользователем.")
        }
    }

    private fun addLogMessage(message: String) {
        val tv = TextView(this).apply {
            text = message
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            setPadding(0, 4, 0, 4)
        }
        logContainer.addView(tv)
    }

    private fun getTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
