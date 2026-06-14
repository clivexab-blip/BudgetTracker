package com.example.budgettracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class Slice(val value: Float, val colorInt: Int)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.parseColor("#1E442F")
        textAlign = Paint.Align.CENTER
        textSize  = 32f
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val percentagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize  = 24f
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var slices = listOf<Slice>()
    private var totalWeight = 0f
    private var centerLabel: String = "ZAR"

    fun setSlices(newSlices: List<Slice>) {
        slices = newSlices
        totalWeight = newSlices.sumOf { it.value.toDouble() }.toFloat()
        invalidate()
    }

    fun setCenterLabel(label: String) {
        centerLabel = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size   = minOf(width, height).toFloat()
        val cx     = width / 2f
        val cy     = height / 2f
        val radius = size / 2f - 20f

        if (slices.isEmpty() || totalWeight == 0f) {
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawCircle(cx, cy, radius, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(cx, cy, radius * 0.55f, paint)
            canvas.drawText(centerLabel, cx, cy + 10f, textPaint)
            return
        }

        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        var startAngle = -90f

        // Render colored wheel segments
        slices.forEach { slice ->
            if (slice.value > 0f) {
                val sweep = (slice.value / totalWeight) * 360f
                paint.style = Paint.Style.FILL
                paint.color = slice.colorInt
                canvas.drawArc(oval, startAngle, sweep, true, paint)
                startAngle += sweep
            }
        }

        // Overlay precise single-point decimal strings mathematically
        startAngle = -90f
        slices.forEach { slice ->
            if (slice.value > 0f) {
                val sweep = (slice.value / totalWeight) * 360f
                val percentageValue = (slice.value / totalWeight) * 100f

                if (percentageValue >= 5f) {
                    val middleAngleRad = Math.toRadians((startAngle + sweep / 2f).toDouble())

                    val labelRadius = radius * 0.75f
                    val labelX = cx + (labelRadius * cos(middleAngleRad)).toFloat()
                    val labelY = cy + (labelRadius * sin(middleAngleRad)).toFloat()

                    val textCorrectedY = labelY - ((percentagePaint.descent() + percentagePaint.ascent()) / 2f)
                    canvas.drawText("%.1f%%".format(percentageValue), labelX, textCorrectedY, percentagePaint)
                }
                startAngle += sweep
            }
        }

        // Center donut hole mask layout dropped down to 0.55f to widen colored layout regions
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, radius * 0.55f, paint)
        canvas.drawText(centerLabel, cx, cy + 10f, textPaint)
    }
}