package com.example.linefit.renderer

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.example.linefit.data.ActivitySummary
import com.example.linefit.ui.theme.BackgroundDark
import com.example.linefit.ui.theme.CardBorder
import com.example.linefit.ui.theme.MutedOlive
import com.example.linefit.ui.theme.SurfaceCard
import com.example.linefit.ui.theme.TextPrimary
import com.example.linefit.ui.theme.TextSecondary
import com.example.linefit.ui.theme.VoltLime
import java.util.Locale

object NeonRouteRenderer {

    fun renderOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        summary: ActivitySummary,
        projectedPoints: List<Offset>,
        config: RouteStyleConfig,
        isTransparentBg: Boolean = false
    ) {
        // 1. Optional Dark Background (for in-app preview)
        if (!isTransparentBg) {
            val bgPaint = Paint().apply {
                color = BackgroundDark.toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width, height, bgPaint)
        }

        // 2. Optional Mini Route Card Border
        if (config.showMiniRouteCard) {
            val cardPadding = width * 0.04f
            val cardRect = RectF(
                cardPadding,
                height * 0.18f,
                width - cardPadding,
                if (config.showSplitsChart) height * 0.72f else height * 0.88f
            )

            if (!isTransparentBg) {
                val cardBgPaint = Paint().apply {
                    color = SurfaceCard.toArgb()
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(cardRect, 32f, 32f, cardBgPaint)
            }

            val borderPaint = Paint().apply {
                color = CardBorder.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)
        }

        // 3. Render Neon Route Line (Dual Layer)
        if (projectedPoints.size >= 2) {
            val path = Path()
            path.moveTo(projectedPoints[0].x, projectedPoints[0].y)
            for (i in 1 until projectedPoints.size) {
                path.lineTo(projectedPoints[i].x, projectedPoints[i].y)
            }

            // Layer A: Outer Neon Glow (Blur)
            val glowPaint = Paint().apply {
                color = config.glowTheme.glowColor.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = config.strokeWidthPx * 2.8f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
                if (config.glowRadiusPx > 0f) {
                    maskFilter = BlurMaskFilter(config.glowRadiusPx, BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawPath(path, glowPaint)

            // Layer B: Core Vibrant Line
            val corePaint = Paint().apply {
                color = config.glowTheme.coreColor.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = config.strokeWidthPx
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            canvas.drawPath(path, corePaint)

            // Draw Start & Finish Point Markers
            val startPoint = projectedPoints.first()
            val endPoint = projectedPoints.last()

            // Start Dot (Volt Lime)
            val startDotPaint = Paint().apply {
                color = VoltLime.toArgb()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(startPoint.x, startPoint.y, config.strokeWidthPx * 0.9f, startDotPaint)

            // Finish Dot (Glow Accent)
            val finishDotPaint = Paint().apply {
                color = config.glowTheme.glowColor.toArgb()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(endPoint.x, endPoint.y, config.strokeWidthPx * 1.1f, finishDotPaint)
        }

        // 4. Render Top Stats Header Overlay
        if (config.showStatsOverlay) {
            renderHeaderStats(canvas, width, height, summary)
        }

        // 5. Render Bottom Splits Bar Chart Overlay
        if (config.showSplitsChart && summary.splits.isNotEmpty()) {
            renderSplitsChart(canvas, width, height, summary)
        }
    }

    private fun renderHeaderStats(canvas: Canvas, width: Float, height: Float, summary: ActivitySummary) {
        val scaleFactor = width / 1080f

        // Main Distance Value
        val distText = String.format(Locale.US, "%.2f", summary.totalDistanceKm)
        val distValPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = 120f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(distText, width * 0.08f, height * 0.08f, distValPaint)

        // Sub Label "KILOMETERS COMPLETED"
        val distLabelPaint = Paint().apply {
            color = VoltLime.toArgb()
            textSize = 28f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        canvas.drawText("KILOMETERS COMPLETED", width * 0.08f, height * 0.11f, distLabelPaint)

        // Time & Avg Pace Metrics (Right Aligned)
        val metricValPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = 46f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val metricLabelPaint = Paint().apply {
            color = TextSecondary.toArgb()
            textSize = 22f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            letterSpacing = 0.12f
        }

        val rightMargin = width * 0.92f
        canvas.drawText("TIME", rightMargin, height * 0.06f, metricLabelPaint)
        canvas.drawText(summary.durationFormatted, rightMargin, height * 0.09f, metricValPaint)

        canvas.drawText("AVG PACE", rightMargin, height * 0.12f, metricLabelPaint)
        canvas.drawText(summary.avgPaceFormatted, rightMargin, height * 0.15f, metricValPaint)
    }

    private fun renderSplitsChart(canvas: Canvas, width: Float, height: Float, summary: ActivitySummary) {
        val scaleFactor = width / 1080f
        val chartTop = height * 0.76f
        val chartBottom = height * 0.90f
        val maxBarHeight = chartBottom - chartTop - (30f * scaleFactor)

        val splits = summary.splits
        if (splits.isEmpty()) return

        val titlePaint = Paint().apply {
            color = TextSecondary.toArgb()
            textSize = 24f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        canvas.drawText("SPLIT COMPARISON (PER KM)", width * 0.08f, chartTop, titlePaint)

        val leftX = width * 0.08f
        val rightX = width * 0.92f
        val totalChartWidth = rightX - leftX
        val barCount = splits.size
        val barWidth = (totalChartWidth / (barCount * 1.6f)).coerceIn(12f * scaleFactor, 44f * scaleFactor)
        val barGap = (totalChartWidth - (barWidth * barCount)) / (barCount + 1)

        val maxPace = splits.maxOfOrNull { it.avgPaceSecPerKm } ?: 300.0
        val minPace = splits.minOfOrNull { it.avgPaceSecPerKm } ?: 200.0

        splits.forEachIndexed { i, split ->
            val x = leftX + barGap + i * (barWidth + barGap)
            val paceRange = (maxPace - minPace).coerceAtLeast(1.0)
            // Faster pace (smaller seconds) -> taller bar
            val normalizedRatio = 0.35f + 0.65f * ((maxPace - split.avgPaceSecPerKm) / paceRange).toFloat()
            val barHeight = maxBarHeight * normalizedRatio
            val barTop = chartBottom - barHeight

            val barRect = RectF(x, barTop, x + barWidth, chartBottom)

            // Bar Color: Volt Lime if fastest, Muted Olive otherwise
            val barPaint = Paint().apply {
                color = if (split.isFastest) VoltLime.toArgb() else MutedOlive.toArgb()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(barRect, barWidth / 2f, barWidth / 2f, barPaint)

            // Km Index Label below bar
            val labelPaint = Paint().apply {
                color = if (split.isFastest) VoltLime.toArgb() else TextSecondary.toArgb()
                textSize = 20f * scaleFactor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${split.kmIndex}", x + barWidth / 2f, chartBottom + (24f * scaleFactor), labelPaint)
        }
    }
}
