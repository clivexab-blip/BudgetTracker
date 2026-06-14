package com.example.budgettracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // Unified model tracking both income and expense points for the same month label
    class Bar(val label: String, val incomeAmount: Float, val expenseAmount: Float)

    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // Premium themed Green for Income
        style = Paint.Style.FILL
    }

    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EB2718") // Themed Red for Expenses
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.parseColor("#666666")
        textSize  = 24f
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var chartData = listOf<Bar>()
    private var maxVal = 1.0f

    fun setBars(data: List<Bar>) {
        chartData = data

        // Dynamic clearance scale based on the highest recorded single element metric
        val maxIncome = data.maxOfOrNull { it.incomeAmount } ?: 0f
        val maxExpense = data.maxOfOrNull { it.expenseAmount } ?: 0f
        val highestValue = maxOf(maxIncome, maxExpense)

        maxVal = if (highestValue > 0f) highestValue * 1.15f else 1.0f // 15% clear headroom buffer
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (chartData.isEmpty()) {
            textPaint.textSize = 30f
            canvas.drawText("No structural tracking data", width / 2f, height / 2f, textPaint)
            return
        }

        // Reserve 60dp at the baseline bottom layer for labels
        val usableWidth  = width.toFloat()
        val usableHeight = height.toFloat() - 60f
        val groupCount   = chartData.size

        val sectionWidth = usableWidth / groupCount
        val individualBarWidth = sectionWidth * 0.28f
        val barSpacing = 6f // Space between twin layout blocks

        chartData.forEachIndexed { idx, barItem ->
            val sectionCenter = (idx * sectionWidth) + (sectionWidth / 2f)

            // ── 1. Draw Income Column (Left Side) ──
            val incomeRatio = barItem.incomeAmount / maxVal
            val incomeTop   = usableHeight - (usableHeight * incomeRatio)
            val incomeLeft  = sectionCenter - individualBarWidth - (barSpacing / 2f)
            val incomeRight = sectionCenter - (barSpacing / 2f)

            if (barItem.incomeAmount > 0f) {
                val incomeRect = RectF(incomeLeft, incomeTop, incomeRight, usableHeight)
                canvas.drawRoundRect(incomeRect, 8f, 8f, incomePaint)
            }

            // ── 2. Draw Expense Column (Right Side) ──
            val expenseRatio = barItem.expenseAmount / maxVal
            val expenseTop   = usableHeight - (usableHeight * expenseRatio)
            val expenseLeft  = sectionCenter + (barSpacing / 2f)
            val expenseRight = sectionCenter + individualBarWidth + (barSpacing / 2f)

            if (barItem.expenseAmount > 0f) {
                val expenseRect = RectF(expenseLeft, expenseTop, expenseRight, usableHeight)
                canvas.drawRoundRect(expenseRect, 8f, 8f, expensePaint)
            }

            // ── 3. Draw Month Identifier Label String ──
            textPaint.textSize = 24f
            canvas.drawText(barItem.label, sectionCenter, height.toFloat() - 15f, textPaint)
        }
    }
}