package com.example.linefit.renderer

import androidx.compose.ui.geometry.Offset
import com.example.linefit.data.TrackPoint
import kotlin.math.PI
import kotlin.math.log
import kotlin.math.tan

object MercatorProjector {

    /**
     * Converts latitude and longitude to 2D Mercator projection coordinates.
     */
    fun toMercator(lat: Double, lon: Double): Pair<Double, Double> {
        val x = Math.toRadians(lon)
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        val y = log(tan(PI / 4 + latRad / 2), Math.E)
        return Pair(x, y)
    }

    /**
     * Projects a list of TrackPoints to 2D Offsets fitted into canvas bounds (width x height)
     * maintaining aspect ratio with uniform padding (default 15%).
     */
    fun projectToCanvas(
        trackPoints: List<TrackPoint>,
        canvasWidth: Float,
        canvasHeight: Float,
        paddingPercentage: Float = 0.15f
    ): List<Offset> {
        if (trackPoints.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) return emptyList()

        val rawMercatorPoints = trackPoints.map { toMercator(it.lat, it.lon) }

        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for ((x, y) in rawMercatorPoints) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        val routeWidth = maxX - minX
        val routeHeight = maxY - minY

        // Avoid divide by zero for single point or line
        val safeRouteWidth = if (routeWidth == 0.0) 0.0001 else routeWidth
        val safeRouteHeight = if (routeHeight == 0.0) 0.0001 else routeHeight

        val paddedWidth = canvasWidth * (1f - 2f * paddingPercentage)
        val paddedHeight = canvasHeight * (1f - 2f * paddingPercentage)

        // Calculate scaling factor to maintain aspect ratio
        val scaleX = paddedWidth / safeRouteWidth
        val scaleY = paddedHeight / safeRouteHeight
        val scale = minOf(scaleX, scaleY)

        // Center route within canvas
        val scaledRouteWidth = safeRouteWidth * scale
        val scaledRouteHeight = safeRouteHeight * scale

        val offsetX = (canvasWidth - scaledRouteWidth) / 2.0
        val offsetY = (canvasHeight - scaledRouteHeight) / 2.0

        return rawMercatorPoints.map { (x, y) ->
            // Note: In Mercator Y increases upwards, canvas Y increases downwards
            val projX = (offsetX + (x - minX) * scale).toFloat()
            val projY = (canvasHeight - (offsetY + (y - minY) * scale)).toFloat()
            Offset(projX, projY)
        }
    }
}
