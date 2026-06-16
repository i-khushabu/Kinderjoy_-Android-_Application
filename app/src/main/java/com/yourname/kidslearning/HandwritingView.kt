package com.yourname.kidslearning

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.Ink

class HandwritingView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val path = Path()
    private val paths = mutableListOf<Path>()
    private var inkBuilder = Ink.builder()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paths.forEach { canvas.drawPath(it, paint) }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                inkBuilder = Ink.builder() // Reset ink when a new stroke starts
                path.reset()
                path.moveTo(event.x, event.y)

                val strokeBuilder = Ink.Stroke.builder()
                strokeBuilder.addPoint(Ink.Point.create(event.x, event.y))
                inkBuilder.addStroke(strokeBuilder.build())

                paths.add(Path(path)) // Save the path
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(event.x, event.y)

                val lastInk = inkBuilder.build()
                val lastStroke = lastInk.strokes.lastOrNull()?.let {
                    Ink.Stroke.builder().apply { it.points.forEach { p -> addPoint(p) } }
                }

                lastStroke?.build()?.let { updatedStroke ->
                    val newInk = Ink.builder().apply {
                        lastInk.strokes.dropLast(1).forEach { addStroke(it) }
                        addStroke(updatedStroke)
                    }.build()

                    inkBuilder = Ink.builder().apply {
                        newInk.strokes.forEach { addStroke(it) }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                paths.add(Path(path)) // Save the final stroke
                path.reset()
            }
        }
        invalidate() // Redraw the canvas
        return true
    }

    fun clearCanvas() {
        Log.d("HandwritingView", "Canvas Cleared")
        inkBuilder = Ink.builder() // ✅ Reset ink data
        paths.clear()
        path.reset()
        invalidate()
    }

    fun getInk(): Ink {
        return inkBuilder.build() // ✅ Return ink for recognition
    }
}
