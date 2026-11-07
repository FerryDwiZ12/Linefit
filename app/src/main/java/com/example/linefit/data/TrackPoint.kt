package com.example.linefit.data

import java.util.Locale
import kotlin.math.*

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double = 0.0,
    val timeMillis: Long? = null,
    val heartRate: Int? = null
)

data class KilometerSplit(
    val kmIndex: Int, // 1-based (e.g., 1, 2, 3...)
    val distanceMeters: Double,
    val durationMillis: Long,
    val avgPaceSecPerKm: Double,
    val formattedPace: String,
    val isFastest: Boolean = false
)

data class ActivitySummary(
    val totalDistanceKm: Double,
    val durationMillis: Long,
    val durationFormatted: String,
    val avgPaceFormatted: String,
    val elevationGainMeters: Double,
    val splits: List<KilometerSplit>,
    val trackPoints: List<TrackPoint>,
    val avgHeartRate: Int? = null,
    val locationTag: String = "📍 Tambakboyo",
    val caloriesKcal: Int = (totalDistanceKm * 65).toInt().coerceAtLeast(100),
    val activityTitle: String = "Morning Walk"
)

/**
 * Helper to calculate distance between two coordinates using Haversine formula (in meters).
 */
fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/**
 * Formats duration in milliseconds into MM:SS or HH:MM:SS format.
 */
fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Formats pace in seconds per kilometer to "M:SS /km" format.
 */
fun formatPace(secPerKm: Double): String {
    if (secPerKm.isInfinite() || secPerKm.isNaN() || secPerKm <= 0) return "0:00 /km"
    val minutes = (secPerKm / 60).toInt()
    val seconds = (secPerKm % 60).toInt()
    return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
}
