package com.app.main

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import kotlin.concurrent.thread
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var gameView: TetrisView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = TetrisView(this)
        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }
}

// ==========================================
// 🔊 Синтезатор Звуковых Эффектов (SoundFX)
// ==========================================
class SoundSynth {
    private val sampleRate = 22050

    private fun playTone(freqStart: Float, freqEnd: Float, durationMs: Int, volume: Float = 0.5f) {
        thread {
            try {
                val numSamples = (durationMs * sampleRate / 1000)
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val currentFreq = freqStart + (freqEnd - freqStart) * t
                    val angle = 2.0 * Math.PI * i * currentFreq / sampleRate
                    val sample = (sin(angle) * Short.MAX_VALUE * volume * (1f - t)).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    fun playMove() = playTone(300f, 200f, 40, 0.2f)
    fun playRotate() = playTone(400f, 700f, 60, 0.3f)
    fun playDrop() = playTone(150f, 80f, 80, 0.5f)
    fun playClearLine() {
        playTone(523.25f, 1046.5f, 150, 0.6f)
    }
    fun playGameOver() {
        playTone(400f, 100f, 400, 0.6f)
    }
}

// ==========================================
// 🎮 Графический Движок Игры (TetrisView)
// ==========================================
class TetrisView(context: Context) : View(context), Runnable {
    private var isRunning = false
    private var gameThread: Thread? = null
    private val sound = SoundSynth()

    // Размеры сетки
    private val cols = 10
    private val rows = 20
    private var board = Array(rows) { IntArray(cols) { 0 } }

    // Цвета фигур (Неоновая палитра)
    private val colors = intArrayOf(
        0,
        Color.parseColor("#00F0FF"), // I - Голубой
        Color.parseColor("#FF007F"), // T - Пурпурный
        Color.parseColor("#FFDD00"), // O - Желтый
        Color.parseColor("#00FF66"), // S - Зеленый
        Color.parseColor("#FF3300"), // Z - Красный
        Color.parseColor("#0044FF"), // J - Синий
        Color.parseColor("#FF9900")  // L - Оранжевый
    )