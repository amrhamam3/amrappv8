package com.amr3d.preview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class WireframeBackgroundView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private data class Node(var x: Float, var y: Float, val vx: Float, val vy: Float)
    private val nodes = mutableListOf<Node>()
    private var alpha = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var running = true

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            update()
            invalidate()
            handler.postDelayed(this, 33)
        }
    }

    init {
        for (i in 0 until 30) {
            nodes.add(Node(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.0008f,
                vy = (Random.nextFloat() - 0.5f) * 0.0008f
            ))
        }
        handler.post(ticker)
    }

    fun fadeIn() {
        val start = System.currentTimeMillis()
        val dur = 1500L
        val h = Handler(Looper.getMainLooper())
        val r = object : Runnable {
            override fun run() {
                val t = ((System.currentTimeMillis() - start).toFloat() / dur).coerceIn(0f, 1f)
                alpha = t * 0.35f
                if (t < 1f) h.postDelayed(this, 16)
            }
        }
        h.post(r)
    }

    private fun update() {
        val w = width.toFloat().takeIf { it > 0 } ?: return
        val h = height.toFloat().takeIf { it > 0 } ?: return
        for (n in nodes) {
            n.x += n.vx
            n.y += n.vy
            if (n.x < 0f) n.x = 1f
            if (n.x > 1f) n.x = 0f
            if (n.y < 0f) n.y = 1f
            if (n.y > 1f) n.y = 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat().takeIf { it > 0 } ?: return
        val h = height.toFloat().takeIf { it > 0 } ?: return
        if (alpha <= 0f) return

        val threshold = 0.28f
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val dx = nodes[i].x - nodes[j].x
                val dy = nodes[i].y - nodes[j].y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < threshold) {
                    val lineAlpha = ((1f - dist / threshold) * alpha * 255).toInt().coerceIn(0, 255)
                    paint.color = android.graphics.Color.argb(lineAlpha, 255, 138, 30)
                    canvas.drawLine(
                        nodes[i].x * w, nodes[i].y * h,
                        nodes[j].x * w, nodes[j].y * h,
                        paint
                    )
                }
            }
        }
        for (n in nodes) {
            paint.color = android.graphics.Color.argb((alpha * 200).toInt(), 255, 138, 30)
            canvas.drawCircle(n.x * w, n.y * h, 2f, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        running = false
        handler.removeCallbacks(ticker)
    }
}
