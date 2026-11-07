package com.example.linefit.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sin
import kotlin.math.cos

object GpxParser {

    /**
     * Parses a GPX InputStream and returns an ActivitySummary object.
     */
    fun parse(inputStream: InputStream): ActivitySummary {
        val trackPoints = mutableListOf<TrackPoint>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEle = 0.0
        var currentTimeMillis: Long? = null
        var currentHr: Int? = null

        var insideTrkpt = false
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase(Locale.ROOT)
                    if (currentTag == "trkpt") {
                        insideTrkpt = true
                        currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        currentEle = 0.0
                        currentTimeMillis = null
                        currentHr = null
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideTrkpt) {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "ele" -> currentEle = text.toDoubleOrNull() ?: 0.0
                                "time" -> currentTimeMillis = parseIso8601ToMillis(text)
                                "hr", "gpxtpx:hr", "ns3:hr" -> currentHr = text.toIntOrNull()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name.lowercase(Locale.ROOT)
                    if (endTag == "trkpt") {
                        if (currentLat != null && currentLon != null) {
                            trackPoints.add(
                                TrackPoint(
                                    lat = currentLat,
                                    lon = currentLon,
                                    ele = currentEle,
                                    timeMillis = currentTimeMillis,
                                    heartRate = currentHr
                                )
                            )
                        }
                        insideTrkpt = false
                    }
                }
            }
            eventType = parser.next()
        }

        if (trackPoints.isEmpty()) {
            return getSampleSummary()
        }

        return computeSummaryFromPoints(trackPoints)
    }

    private fun parseIso8601ToMillis(isoString: String): Long? {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(isoString)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    /**
     * Computes distance, elevation, pace, and 1km splits from a list of track points.
     */
    fun computeSummaryFromPoints(points: List<TrackPoint>): ActivitySummary {
        if (points.isEmpty()) return getSampleSummary()

        var totalDistMeters = 0.0
        var elevationGainMeters = 0.0
        var totalHrSum = 0
        var totalHrCount = 0

        val splits = mutableListOf<KilometerSplit>()
        var currentKmDistMeters = 0.0
        var currentKmStartIdx = 0
        var kmCounter = 1

        val startTime = points.first().timeMillis
        val endTime = points.last().timeMillis
        val totalDurationMillis = if (startTime != null && endTime != null && endTime > startTime) {
            endTime - startTime
        } else {
            // Fallback duration based on 4:46/km estimate if timestamps missing
            (points.size * 2000L)
        }

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val dist = calculateHaversineDistance(p1.lat, p1.lon, p2.lat, p2.lon)
            totalDistMeters += dist
            currentKmDistMeters += dist

            val eleDiff = p2.ele - p1.ele
            if (eleDiff > 0) {
                elevationGainMeters += eleDiff
            }

            p1.heartRate?.let {
                totalHrSum += it
                totalHrCount++
            }

            // Check if accumulated 1.0 km split
            if (currentKmDistMeters >= 1000.0) {
                val tStart = points[currentKmStartIdx].timeMillis ?: 0L
                val tEnd = p2.timeMillis ?: 0L
                val kmDuration = if (tEnd > tStart) tEnd - tStart else (1000.0 / (totalDistMeters / totalDurationMillis)).toLong()
                val paceSec = (kmDuration / 1000.0) / (currentKmDistMeters / 1000.0)

                splits.add(
                    KilometerSplit(
                        kmIndex = kmCounter++,
                        distanceMeters = currentKmDistMeters,
                        durationMillis = kmDuration,
                        avgPaceSecPerKm = paceSec,
                        formattedPace = formatPace(paceSec)
                    )
                )
                currentKmDistMeters = 0.0
                currentKmStartIdx = i + 1
            }
        }

        // Remaining fractional kilometer split
        if (currentKmDistMeters > 50.0) {
            val tStart = points[currentKmStartIdx].timeMillis ?: 0L
            val tEnd = points.last().timeMillis ?: 0L
            val kmDuration = if (tEnd > tStart) tEnd - tStart else 0L
            val paceSec = if (kmDuration > 0) (kmDuration / 1000.0) / (currentKmDistMeters / 1000.0) else 0.0
            splits.add(
                KilometerSplit(
                    kmIndex = kmCounter,
                    distanceMeters = currentKmDistMeters,
                    durationMillis = kmDuration,
                    avgPaceSecPerKm = paceSec,
                    formattedPace = formatPace(paceSec)
                )
            )
        }

        // Mark the fastest split
        var minPace = Double.MAX_VALUE
        var fastestIdx = -1
        splits.forEachIndexed { idx, split ->
            if (split.avgPaceSecPerKm > 0 && split.avgPaceSecPerKm < minPace) {
                minPace = split.avgPaceSecPerKm
                fastestIdx = idx
            }
        }

        val updatedSplits = splits.mapIndexed { idx, split ->
            if (idx == fastestIdx) split.copy(isFastest = true) else split
        }

        val totalKm = totalDistMeters / 1000.0
        val avgPaceSec = if (totalKm > 0) (totalDurationMillis / 1000.0) / totalKm else 0.0
        val avgHr = if (totalHrCount > 0) totalHrSum / totalHrCount else null

        return ActivitySummary(
            totalDistanceKm = totalKm,
            durationMillis = totalDurationMillis,
            durationFormatted = formatDuration(totalDurationMillis),
            avgPaceFormatted = formatPace(avgPaceSec),
            elevationGainMeters = elevationGainMeters,
            splits = updatedSplits,
            trackPoints = points,
            avgHeartRate = avgHr
        )
    }

    /**
     * Provides mock/sample activity summary with real route shape for initial view or previews.
     */
    fun getSampleSummary(): ActivitySummary {
        val baseLat = -6.200000
        val baseLon = 106.816666
        val samplePoints = mutableListOf<TrackPoint>()
        val count = 200

        // Create a stylized loop route (figure-8 / athletic track pattern)
        for (i in 0 until count) {
            val t = i.toDouble() / count * 2.0 * Math.PI
            val dLat = 0.015 * sin(t)
            val dLon = 0.02 * sin(2 * t)
            val ele = 15.0 + 5.0 * sin(t * 3)
            val time = System.currentTimeMillis() - (count - i) * 11600L
            samplePoints.add(
                TrackPoint(
                    lat = baseLat + dLat,
                    lon = baseLon + dLon,
                    ele = ele,
                    timeMillis = time,
                    heartRate = 145 + (15 * sin(t)).toInt()
                )
            )
        }

        val mockSplits = listOf(
            KilometerSplit(1, 1000.0, 302000L, 302.0, "5:02 /km", false),
            KilometerSplit(2, 1000.0, 290000L, 290.0, "4:50 /km", false),
            KilometerSplit(3, 1000.0, 281000L, 281.0, "4:41 /km", false),
            KilometerSplit(4, 1000.0, 275000L, 275.0, "4:35 /km", true), // Fastest
            KilometerSplit(5, 1000.0, 288000L, 288.0, "4:48 /km", false),
            KilometerSplit(6, 1000.0, 294000L, 294.0, "4:54 /km", false),
            KilometerSplit(7, 1000.0, 284000L, 284.0, "4:44 /km", false),
            KilometerSplit(8, 1000.0, 280000L, 280.0, "4:40 /km", false),
            KilometerSplit(9, 120.0, 31000L, 260.0, "4:20 /km", false)
        )

        return ActivitySummary(
            totalDistanceKm = 8.12,
            durationMillis = 2322000L, // 38m 42s
            durationFormatted = "38:42",
            avgPaceFormatted = "4:46 /km",
            elevationGainMeters = 42.0,
            splits = mockSplits,
            trackPoints = samplePoints,
            avgHeartRate = 152
        )
    }
}
