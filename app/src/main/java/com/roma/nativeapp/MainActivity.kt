package com.roma.nativeapp

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import java.util.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(WormGameView(this))
    }

    class WormGameView(context: Context) : View(context) {
        private val paint = Paint()
        private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        private val snake = mutableListOf(PointF(300f, 300f), PointF(250f, 300f), PointF(200f, 300f))
        private var dx = 5f
        private var dy = 0f
        private val foodList = mutableListOf<RectF>()
        private var score = 0
        private val rnd = Random()

        init {
            setBackgroundColor(Color.parseColor("#0F172A"))
            spawnFood()
            Thread {
                while (true) {
                    Thread.sleep(30)
                    postInvalidate()
                }
            }.start()
        }

        private fun spawnFood() {
            foodList.clear()
            for (i in 0..5) {
                val x = rnd.nextInt(800 + 1).toFloat() + 50f
                val y = rnd.nextInt(1400 + 1).toFloat() + 100f
                foodList.add(RectF(x, y, x + 40f, y + 40f))
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            // Отрисовка цветных червячков-целей
            paint.color = Color.parseColor("#38BDF8")
            for (food in foodList) {
                canvas.drawOval(food, paint)
            }

            // Движение нашего червяка
            val head = PointF(snake[0].x + dx, snake[0].y + dy)
            if (head.x < 0) head.x = width.toFloat()
            if (head.x > width) head.x = 0f
            if (head.y < 0) head.y = height.toFloat()
            if (head.y > height) head.y = 0f

            snake.add(0, head)

            var eaten = false
            val iterator = foodList.iterator()
            while (iterator.hasNext()) {
                val f = iterator.next()
                if (RectF(head.x, head.y, head.x + 35f, head.y + 35f).intersect(f)) {
                    iterator.remove()
                    score += 15
                    eaten = true
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    break
                }
            }

            if (!eaten) {
                snake.removeAt(snake.size - 1)
            } else if (foodList.isEmpty()) {
                spawnFood()
            }

            for (i in snake.indices) {
                paint.color = if (i == 0) Color.parseColor("#22C55E") else Color.parseColor("#4ADE80")
                canvas.drawCircle(snake[i].x, snake[i].y, 22f, paint)
            }

            paint.color = Color.WHITE
            paint.textSize = 60f
            canvas.drawText("Счет: $score", 50f, 120f, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val head = snake[0]
                if (event.x < head.x && dx == 0f) { dx = -5f; dy = 0f }
                else if (event.x > head.x && dx == 0f) { dx = 5f; dy = 0f }
                else if (event.y < head.y && dy == 0f) { dx = 0f; dy = -5f }
                else if (event.y > head.y && dy == 0f) { dx = 0f; dy = 5f }
            }
            return true
        }
    }
}
