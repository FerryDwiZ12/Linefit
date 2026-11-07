package com.example.linefit.renderer

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
import kotlin.math.cos
import kotlin.math.sin

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
        // Dispatch to appropriate preset theme renderer
        when (config.presetTheme) {
            PresetTheme.THE_VOLT_PULSE -> renderVoltPulse(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.MINIMALIST_GLASS_PILL -> renderMinimalistGlassPill(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.PACE_HEATMAP_RIBBON -> renderPaceHeatmapRibbon(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.TELEMETRY_HUD -> renderTelemetryHud(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.MAGAZINE_EDITORIAL -> renderMagazineEditorial(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.SPLIT_ANALYTICAL -> renderSplitAnalytical(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.TRAIL_HORIZON -> renderTrailHorizon(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.VIEWFINDER_FRAME -> renderViewfinderFrame(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.DUAL_RING_ACTIVITY -> renderDualRingActivity(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.BRUTALIST_TICKET -> renderBrutalistTicket(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.DOODLE_NOTEBOOK -> renderDoodleNotebook(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.CYBERPUNK_GRID -> renderCyberpunkGrid(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.CHALKBOARD_SKETCH -> renderChalkboardSketch(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.RETRO_GAMING_PIXEL -> renderRetroGamingPixel(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.MINIMAL_TOPOGRAPHY -> renderMinimalTopography(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.GOLDEN_CHAMPION -> renderGoldenChampion(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.STEALTH_CARBON -> renderStealthCarbon(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.STRAVA_SUNSET -> renderStravaSunset(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.NEON_CYBERDREAM -> renderNeonCyberdream(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
            PresetTheme.VINTAGE_POSTCARD -> renderVintagePostcard(canvas, width, height, summary, projectedPoints, config, isTransparentBg)
        }
    }

    // Helper to draw standard dual-layer neon route path
    private fun drawNeonRoutePath(
        canvas: Canvas,
        points: List<Offset>,
        glowColor: Int,
        coreColor: Int,
        strokeWidth: Float,
        glowRadius: Float
    ) {
        if (points.size < 2) return
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }

        // Outer Glow
        if (glowRadius > 0f) {
            val glowPaint = Paint().apply {
                color = glowColor
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth * 2.8f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
                maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(path, glowPaint)
        }

        // Core Line
        val corePaint = Paint().apply {
            color = coreColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        canvas.drawPath(path, corePaint)

        // Start & End Dots
        val dotPaint = Paint().apply {
            color = coreColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(points.first().x, points.first().y, strokeWidth * 0.9f, dotPaint)
        canvas.drawCircle(points.last().x, points.last().y, strokeWidth * 1.1f, dotPaint)
    }

    // 1. THE VOLT PULSE
    private fun renderVoltPulse(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(BackgroundDark.toArgb())
        val cardPadding = w * 0.04f
        val cardRect = RectF(cardPadding, h * 0.18f, w - cardPadding, if (cfg.showSplitsChart) h * 0.72f else h * 0.88f)

        if (!isTrans) {
            val cardBgPaint = Paint().apply { color = SurfaceCard.toArgb(); style = Paint.Style.FILL }
            canvas.drawRoundRect(cardRect, 32f, 32f, cardBgPaint)
        }
        if (cfg.showMiniRouteCard) {
            val borderPaint = Paint().apply { color = CardBorder.toArgb(); style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
            canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)
        }

        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx, cfg.glowRadiusPx)

        if (cfg.showStatsOverlay) renderHeaderStats(canvas, w, h, sum, cfg.customLocation)
        if (cfg.showSplitsChart && sum.splits.isNotEmpty()) renderSplitsChart(canvas, w, h, sum)
    }

    // 2. MINIMALIST GLASS PILL
    private fun renderMinimalistGlassPill(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(BackgroundDark.toArgb())
        val pillRect = RectF(w * 0.05f, h * 0.40f, w * 0.95f, h * 0.60f)
        val pillPaint = Paint().apply { color = AndroidColor.parseColor("#E0262A32"); style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(pillRect, (pillRect.height() / 2f), (pillRect.height() / 2f), pillPaint)

        val borderPaint = Paint().apply { color = AndroidColor.parseColor("#40FFFFFF"); style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
        canvas.drawRoundRect(pillRect, (pillRect.height() / 2f), (pillRect.height() / 2f), borderPaint)

        // Mini route inside left side of pill
        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx * 0.7f, cfg.glowRadiusPx * 0.5f)

        val textPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = w * 0.038f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val locText = if (cfg.customLocation.isNotBlank()) " | ${cfg.customLocation}" else ""
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM | ${sum.avgPaceFormatted} | ${sum.avgHeartRate ?: 154} BPM$locText", w * 0.36f, h * 0.515f, textPaint)
    }

    // 3. PACE HEATMAP RIBBON
    private fun renderPaceHeatmapRibbon(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#121418"))
        val cardRect = RectF(w * 0.05f, h * 0.32f, w * 0.95f, h * 0.68f)
        val bgPaint = Paint().apply { color = AndroidColor.parseColor("#1C2026"); style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(cardRect, 28f, 28f, bgPaint)

        // Heatmap Gradient Ribbon
        if (pts.size >= 2) {
            val path = Path()
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)

            val grad = LinearGradient(w * 0.1f, 0f, w * 0.9f, 0f, intArrayOf(AndroidColor.GREEN, AndroidColor.YELLOW, AndroidColor.RED), null, Shader.TileMode.CLAMP)
            val ribbonPaint = Paint().apply {
                shader = grad
                style = Paint.Style.STROKE
                strokeWidth = cfg.strokeWidthPx * 1.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            canvas.drawPath(path, ribbonPaint)
        }

        val textPaint = Paint().apply { color = VoltLime.toArgb(); textSize = w * 0.035f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("PACE HEATMAP RIBBON  •  ${cfg.customLocation}", w * 0.09f, h * 0.37f, textPaint)

        val subTextPaint = Paint().apply { color = TextSecondary.toArgb(); textSize = w * 0.03f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("FAST PACE 3:15 /KM", w * 0.09f, h * 0.64f, subTextPaint)
        canvas.drawText("MAX HR 178 BPM", w * 0.65f, h * 0.64f, subTextPaint)
    }

    // 4. TELEMETRY HUD
    private fun renderTelemetryHud(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#090B0E"))

        // Grid lines background
        val gridPaint = Paint().apply { color = AndroidColor.parseColor("#1F2633"); strokeWidth = 2f }
        var y = 0f
        while (y < h) { canvas.drawLine(0f, y, w, y, gridPaint); y += h * 0.08f }
        var x = 0f
        while (x < w) { canvas.drawLine(x, 0f, x, h, gridPaint); x += w * 0.12f }

        // Dash Route
        if (pts.size >= 2) {
            val path = Path()
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)

            val dashPaint = Paint().apply {
                color = VoltLime.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = cfg.strokeWidthPx
                pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
                isAntiAlias = true
            }
            canvas.drawPath(path, dashPaint)
        }

        // HR Zone Arc Gauge
        val arcRect = RectF(w * 0.08f, h * 0.70f, w * 0.42f, h * 0.90f)
        val arcPaint = Paint().apply { color = AndroidColor.GREEN; style = Paint.Style.STROKE; strokeWidth = 14f; isAntiAlias = true }
        canvas.drawArc(arcRect, 180f, 140f, false, arcPaint)

        val labelPaint = Paint().apply { color = TextPrimary.toArgb(); textSize = w * 0.038f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("HR ZONE: 154 BPM", w * 0.10f, h * 0.88f, labelPaint)
        canvas.drawText("${cfg.customLocation}  [${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM]", w * 0.50f, h * 0.88f, labelPaint)
    }

    // 5. MAGAZINE EDITORIAL
    private fun renderMagazineEditorial(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#0C0E12"))

        // Giant Editorial Distance Text
        val distPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = w * 0.26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(String.format(Locale.US, "%.1f", sum.totalDistanceKm), w * 0.06f, h * 0.18f, distPaint)

        val kmPaint = Paint().apply {
            color = VoltLime.toArgb()
            textSize = w * 0.18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("KM", w * 0.65f, h * 0.18f, kmPaint)

        // Location Tag Subheading
        val subPaint = Paint().apply {
            color = TextSecondary.toArgb()
            textSize = w * 0.04f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
            isAntiAlias = true
        }
        canvas.drawText(cfg.customLocation.uppercase(Locale.ROOT), w * 0.07f, h * 0.23f, subPaint)

        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx, cfg.glowRadiusPx)

        // Editorial Footer Metrics
        val footerPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = w * 0.048f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("PACE: ${sum.avgPaceFormatted}", w * 0.92f, h * 0.86f, footerPaint)
        canvas.drawText("TIME: ${sum.durationFormatted}", w * 0.92f, h * 0.91f, footerPaint)
    }

    // 6. SPLIT ANALYTICAL
    private fun renderSplitAnalytical(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        renderVoltPulse(canvas, w, h, sum, pts, cfg, isTrans)

        // Overlaid Heart Rate Line Chart
        if (sum.trackPoints.size >= 2) {
            val hrPath = Path()
            val left = w * 0.08f
            val right = w * 0.92f
            val bottom = h * 0.88f
            val top = h * 0.78f

            val count = sum.trackPoints.size
            for (i in 0 until count) {
                val x = left + (i.toFloat() / count) * (right - left)
                val hr = sum.trackPoints[i].heartRate ?: 150
                val normHr = ((hr - 120) / 60f).coerceIn(0f, 1f)
                val y = bottom - normHr * (bottom - top)
                if (i == 0) hrPath.moveTo(x, y) else hrPath.lineTo(x, y)
            }

            val hrPaint = Paint().apply {
                color = AndroidColor.RED
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }
            canvas.drawPath(hrPath, hrPaint)
        }
    }

    // 7. TRAIL HORIZON
    private fun renderTrailHorizon(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#0F1318"))

        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx, cfg.glowRadiusPx)

        // Elevation Profile Horizon
        val horizonPath = Path()
        val left = w * 0.08f
        val right = w * 0.92f
        val bottom = h * 0.92f
        val top = h * 0.78f

        horizonPath.moveTo(left, bottom)
        for (i in pts.indices) {
            val x = left + (i.toFloat() / pts.size) * (right - left)
            val ele = sum.trackPoints.getOrNull(i)?.ele ?: 140.0
            val normEle = ((ele - 130.0) / 30.0).coerceIn(0.1, 0.9).toFloat()
            val y = bottom - normEle * (bottom - top)
            horizonPath.lineTo(x, y)
        }
        horizonPath.lineTo(right, bottom)
        horizonPath.close()

        val elePaint = Paint().apply {
            color = AndroidColor.parseColor("#404DFF4D")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(horizonPath, elePaint)

        val textPaint = Paint().apply { color = VoltLime.toArgb(); textSize = w * 0.038f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("ELEVATION GAIN: ${sum.elevationGainMeters.toInt()}m  •  ${cfg.customLocation}", w * 0.08f, h * 0.74f, textPaint)
    }

    // 8. VIEWFINDER FRAME
    private fun renderViewfinderFrame(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#08090C"))

        val bracketPaint = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }
        val bLen = w * 0.08f
        val margin = w * 0.06f

        // Top Left Bracket
        canvas.drawLine(margin, margin, margin + bLen, margin, bracketPaint)
        canvas.drawLine(margin, margin, margin, margin + bLen, bracketPaint)
        // Top Right Bracket
        canvas.drawLine(w - margin, margin, w - margin - bLen, margin, bracketPaint)
        canvas.drawLine(w - margin, margin, w - margin, margin + bLen, bracketPaint)
        // Bottom Left
        canvas.drawLine(margin, h - margin, margin + bLen, h - margin, bracketPaint)
        canvas.drawLine(margin, h - margin, margin, h - margin - bLen, bracketPaint)
        // Bottom Right
        canvas.drawLine(w - margin, h - margin, w - margin - bLen, h - margin, bracketPaint)
        canvas.drawLine(w - margin, h - margin, w - margin, h - margin - bLen, bracketPaint)

        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx, cfg.glowRadiusPx)

        val textPaint = Paint().apply { color = AndroidColor.WHITE; textSize = w * 0.036f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
        canvas.drawText("[REC]  ${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM", margin + 10f, margin + 40f, textPaint)
        canvas.drawText(cfg.customLocation, margin + 10f, h - margin - 20f, textPaint)
    }

    // 9. DUAL RING ACTIVITY
    private fun renderDualRingActivity(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#0C0E12"))

        // Concentric Activity Rings
        val center = Offset(w / 2f, h * 0.45f)
        val outerRadius = w * 0.38f
        val innerRadius = w * 0.30f

        val ringOuterPaint = Paint().apply { color = VoltLime.toArgb(); style = Paint.Style.STROKE; strokeWidth = 24f; isAntiAlias = true }
        val ringInnerPaint = Paint().apply { color = AndroidColor.parseColor("#FFFF6D00"); style = Paint.Style.STROKE; strokeWidth = 24f; isAntiAlias = true }

        val rectOuter = RectF(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
        val rectInner = RectF(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius)

        canvas.drawArc(rectOuter, -90f, 310f, false, ringOuterPaint)
        canvas.drawArc(rectInner, -90f, 260f, false, ringInnerPaint)

        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx * 0.8f, cfg.glowRadiusPx * 0.5f)

        val textPaint = Paint().apply { color = TextPrimary.toArgb(); textSize = w * 0.045f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM  •  ${sum.durationFormatted}", w / 2f, h * 0.84f, textPaint)
        canvas.drawText(cfg.customLocation, w / 2f, h * 0.90f, textPaint)
    }

    // 10. BRUTALIST TICKET
    private fun renderBrutalistTicket(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        val ticketRect = RectF(w * 0.08f, h * 0.12f, w * 0.92f, h * 0.88f)
        val ticketBg = Paint().apply { color = AndroidColor.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRect(ticketRect, ticketBg)

        val borderPaint = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true }
        canvas.drawRect(ticketRect, borderPaint)

        val headerPaint = Paint().apply { color = AndroidColor.WHITE; textSize = w * 0.055f; typeface = Typeface.MONOSPACE; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("BRUTALIST TICKET", w / 2f, h * 0.20f, headerPaint)
        canvas.drawText(cfg.customLocation.uppercase(Locale.ROOT), w / 2f, h * 0.25f, headerPaint)

        drawNeonRoutePath(canvas, pts, AndroidColor.WHITE, AndroidColor.WHITE, cfg.strokeWidthPx * 0.8f, 0f)

        val bodyPaint = Paint().apply { color = AndroidColor.WHITE; textSize = w * 0.038f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
        canvas.drawText("DIST: ${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM", w * 0.14f, h * 0.78f, bodyPaint)
        canvas.drawText("TIME: ${sum.durationFormatted}", w * 0.14f, h * 0.82f, bodyPaint)
        canvas.drawText("PACE: ${sum.avgPaceFormatted}", w * 0.55f, h * 0.78f, bodyPaint)
        canvas.drawText("HR: ${sum.avgHeartRate ?: 154} BPM", w * 0.55f, h * 0.82f, bodyPaint)
    }

    // 11. DOODLE NOTEBOOK
    private fun renderDoodleNotebook(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        val paperRect = RectF(w * 0.06f, h * 0.08f, w * 0.94f, h * 0.92f)
        val paperPaint = Paint().apply { color = AndroidColor.parseColor("#FFFDF5"); style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(paperRect, 16f, 16f, paperPaint)

        // Yellow Highlighter Stripe
        val hlPaint = Paint().apply { color = AndroidColor.parseColor("#80FFE082"); style = Paint.Style.FILL }
        canvas.drawRect(w * 0.12f, h * 0.22f, w * 0.88f, h * 0.27f, hlPaint)

        val titlePaint = Paint().apply { color = AndroidColor.BLACK; textSize = w * 0.065f; typeface = Typeface.SERIF; isAntiAlias = true }
        canvas.drawText("Morning Run", w * 0.14f, h * 0.18f, titlePaint)
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM", w * 0.14f, h * 0.26f, titlePaint)
        canvas.drawText(cfg.customLocation, w * 0.14f, h * 0.88f, titlePaint)

        // Sketch Route Line
        drawNeonRoutePath(canvas, pts, AndroidColor.BLACK, AndroidColor.BLACK, cfg.strokeWidthPx * 0.9f, 0f)
    }

    // 12. CYBERPUNK GRID
    private fun renderCyberpunkGrid(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#0C0519"))

        // Pink Grid
        val gridPaint = Paint().apply { color = AndroidColor.parseColor("#38FF0077"); strokeWidth = 2f }
        var y = 0f
        while (y < h) { canvas.drawLine(0f, y, w, y, gridPaint); y += h * 0.05f }

        drawNeonRoutePath(canvas, pts, AndroidColor.parseColor("#FF0077"), AndroidColor.parseColor("#00E5FF"), cfg.strokeWidthPx, 32f)

        val textPaint = Paint().apply { color = AndroidColor.parseColor("#00E5FF"); textSize = w * 0.048f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
        canvas.drawText("CYBERPUNK GRID", w * 0.08f, h * 0.12f, textPaint)
        canvas.drawText("${cfg.customLocation} [${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM]", w * 0.08f, h * 0.90f, textPaint)
    }

    // 13. CHALKBOARD SKETCH
    private fun renderChalkboardSketch(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#1B2421"))

        drawNeonRoutePath(canvas, pts, AndroidColor.WHITE, AndroidColor.WHITE, cfg.strokeWidthPx * 0.8f, 12f)

        val chalkPaint = Paint().apply { color = AndroidColor.parseColor("#E0FFFFFF"); textSize = w * 0.052f; typeface = Typeface.SERIF; isAntiAlias = true }
        canvas.drawText("Chalkboard Sketch", w * 0.08f, h * 0.14f, chalkPaint)
        canvas.drawText("Distance: ${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} km", w * 0.08f, h * 0.84f, chalkPaint)
        canvas.drawText("Location: ${cfg.customLocation}", w * 0.08f, h * 0.89f, chalkPaint)
    }

    // 14. RETRO GAMING PIXEL
    private fun renderRetroGamingPixel(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.BLACK)

        drawNeonRoutePath(canvas, pts, AndroidColor.GREEN, AndroidColor.GREEN, cfg.strokeWidthPx, 0f)

        val pixelPaint = Paint().apply { color = AndroidColor.GREEN; textSize = w * 0.042f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
        canvas.drawText("STAGE 1 CLEARED!", w * 0.08f, h * 0.12f, pixelPaint)
        canvas.drawText("SCORE: ${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM", w * 0.08f, h * 0.88f, pixelPaint)
        canvas.drawText(cfg.customLocation, w * 0.08f, h * 0.93f, pixelPaint)
    }

    // 15. MINIMAL TOPOGRAPHY
    private fun renderMinimalTopography(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#11151C"))

        // Topography Circles
        val topoPaint = Paint().apply { color = AndroidColor.parseColor("#1Affffff"); style = Paint.Style.STROKE; strokeWidth = 2f }
        var r = w * 0.1f
        while (r < w * 0.8f) { canvas.drawCircle(w / 2f, h / 2f, r, topoPaint); r += w * 0.1f }

        drawNeonRoutePath(canvas, pts, cfg.glowTheme.glowColor.toArgb(), cfg.glowTheme.coreColor.toArgb(), cfg.strokeWidthPx, cfg.glowRadiusPx)

        val textPaint = Paint().apply { color = TextPrimary.toArgb(); textSize = w * 0.038f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM  •  ${cfg.customLocation}", w * 0.08f, h * 0.90f, textPaint)
    }

    // 16. GOLDEN CHAMPION
    private fun renderGoldenChampion(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#0D0B05"))

        val goldColor = AndroidColor.parseColor("#FFD700")
        val borderPaint = Paint().apply { color = goldColor; style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }
        canvas.drawRect(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.95f, borderPaint)

        drawNeonRoutePath(canvas, pts, goldColor, goldColor, cfg.strokeWidthPx, 20f)

        val goldTextPaint = Paint().apply { color = goldColor; textSize = w * 0.048f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("🏆 GOLDEN CHAMPION 🏆", w / 2f, h * 0.12f, goldTextPaint)
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM  •  ${cfg.customLocation}", w / 2f, h * 0.90f, goldTextPaint)
    }

    // 17. STEALTH CARBON
    private fun renderStealthCarbon(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#121212"))

        drawNeonRoutePath(canvas, pts, AndroidColor.parseColor("#FF1744"), AndroidColor.parseColor("#FF5252"), cfg.strokeWidthPx, 24f)

        val stealthPaint = Paint().apply { color = AndroidColor.parseColor("#FF1744"); textSize = w * 0.042f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("STEALTH TELEMETRY  •  ${cfg.customLocation}", w * 0.08f, h * 0.12f, stealthPaint)
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM  |  ${sum.avgPaceFormatted}", w * 0.08f, h * 0.90f, stealthPaint)
    }

    // 18. STRAVA SUNSET
    private fun renderStravaSunset(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#150B05"))

        val orangeColor = AndroidColor.parseColor("#FC4C02")
        drawNeonRoutePath(canvas, pts, orangeColor, orangeColor, cfg.strokeWidthPx, 28f)

        val orangePaint = Paint().apply { color = orangeColor; textSize = w * 0.052f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("SUNSET ATHLETIC", w * 0.08f, h * 0.12f, orangePaint)
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM  •  ${cfg.customLocation}", w * 0.08f, h * 0.90f, orangePaint)
    }

    // 19. NEON CYBERDREAM
    private fun renderNeonCyberdream(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        if (!isTrans) canvas.drawColor(AndroidColor.parseColor("#190426"))

        drawNeonRoutePath(canvas, pts, AndroidColor.parseColor("#FF00D4"), AndroidColor.parseColor("#9D00FF"), cfg.strokeWidthPx, 36f)

        val dreamPaint = Paint().apply { color = AndroidColor.parseColor("#FF00D4"); textSize = w * 0.048f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("CYBERDREAM ✨", w * 0.08f, h * 0.12f, dreamPaint)
        canvas.drawText("${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM  •  ${cfg.customLocation}", w * 0.08f, h * 0.90f, dreamPaint)
    }

    // 20. VINTAGE POSTCARD
    private fun renderVintagePostcard(
        canvas: Canvas, w: Float, h: Float, sum: ActivitySummary, pts: List<Offset>, cfg: RouteStyleConfig, isTrans: Boolean
    ) {
        val cardRect = RectF(w * 0.06f, h * 0.08f, w * 0.94f, h * 0.92f)
        val cardBg = Paint().apply { color = AndroidColor.parseColor("#F4EAD5"); style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(cardRect, 20f, 20f, cardBg)

        // Stamp box
        val stampRect = RectF(w * 0.68f, h * 0.12f, w * 0.88f, h * 0.24f)
        val stampBorder = Paint().apply { color = AndroidColor.parseColor("#8C2D19"); style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) }
        canvas.drawRect(stampRect, stampBorder)

        drawNeonRoutePath(canvas, pts, AndroidColor.parseColor("#2B2D42"), AndroidColor.parseColor("#2B2D42"), cfg.strokeWidthPx * 0.8f, 0f)

        val textPaint = Paint().apply { color = AndroidColor.parseColor("#2B2D42"); textSize = w * 0.042f; typeface = Typeface.SERIF; isAntiAlias = true }
        canvas.drawText("GREETINGS FROM", w * 0.10f, h * 0.16f, textPaint)
        canvas.drawText(cfg.customLocation.uppercase(Locale.ROOT), w * 0.10f, h * 0.21f, textPaint)
        canvas.drawText("DISTANCE: ${String.format(Locale.US, "%.2f", sum.totalDistanceKm)} KM", w * 0.10f, h * 0.88f, textPaint)
    }

    // Common Helpers
    private fun renderHeaderStats(canvas: Canvas, width: Float, height: Float, summary: ActivitySummary, locationTag: String) {
        val scaleFactor = width / 1080f

        val distText = String.format(Locale.US, "%.2f", summary.totalDistanceKm)
        val distValPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = 110f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(distText, width * 0.08f, height * 0.08f, distValPaint)

        val distLabelPaint = Paint().apply {
            color = VoltLime.toArgb()
            textSize = 26f * scaleFactor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        canvas.drawText("KILOMETERS COMPLETED  •  $locationTag", width * 0.08f, height * 0.11f, distLabelPaint)

        val metricValPaint = Paint().apply {
            color = TextPrimary.toArgb()
            textSize = 44f * scaleFactor
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
            val normalizedRatio = 0.35f + 0.65f * ((maxPace - split.avgPaceSecPerKm) / paceRange).toFloat()
            val barHeight = maxBarHeight * normalizedRatio
            val barTop = chartBottom - barHeight

            val barRect = RectF(x, barTop, x + barWidth, chartBottom)

            val barPaint = Paint().apply {
                color = if (split.isFastest) VoltLime.toArgb() else MutedOlive.toArgb()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(barRect, barWidth / 2f, barWidth / 2f, barPaint)

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
