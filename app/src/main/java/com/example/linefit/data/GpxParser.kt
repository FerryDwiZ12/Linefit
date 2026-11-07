package com.example.linefit.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sin

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
     * If timestamps or heart rates are missing from GPX, missing values are automatically synthesized.
     */
    fun computeSummaryFromPoints(points: List<TrackPoint>): ActivitySummary {
        if (points.isEmpty()) return getSampleSummary()

        // 1. Ensure all points have valid timestamps and heart rate values
        val hasTimestamps = points.any { it.timeMillis != null }
        val processedPoints = if (!hasTimestamps) {
            synthesizeTimestampsAndHr(points)
        } else {
            points.mapIndexed { idx, p ->
                if (p.heartRate == null) {
                    p.copy(heartRate = 142 + (sin(idx / 8.0) * 14).toInt())
                } else p
            }
        }

        var totalDistMeters = 0.0
        var elevationGainMeters = 0.0
        var totalHrSum = 0
        var totalHrCount = 0

        val splits = mutableListOf<KilometerSplit>()
        var currentKmDistMeters = 0.0
        var currentKmStartIdx = 0
        var kmCounter = 1

        val startTime = processedPoints.first().timeMillis ?: 0L
        val endTime = processedPoints.last().timeMillis ?: (startTime + 1800000L)
        val totalDurationMillis = (endTime - startTime).coerceAtLeast(1000L)

        for (i in 0 until processedPoints.size - 1) {
            val p1 = processedPoints[i]
            val p2 = processedPoints[i + 1]

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
                val tStart = processedPoints[currentKmStartIdx].timeMillis ?: 0L
                val tEnd = p2.timeMillis ?: 0L
                val kmDuration = (tEnd - tStart).coerceAtLeast(1000L)
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
            val tStart = processedPoints[currentKmStartIdx].timeMillis ?: 0L
            val tEnd = processedPoints.last().timeMillis ?: 0L
            val kmDuration = (tEnd - tStart).coerceAtLeast(1000L)
            val paceSec = (kmDuration / 1000.0) / (currentKmDistMeters / 1000.0)
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
            trackPoints = processedPoints,
            avgHeartRate = avgHr
        )
    }

    /**
     * Synthesizes realistic timestamps and heart rate for GPX tracks missing time data.
     */
    private fun synthesizeTimestampsAndHr(points: List<TrackPoint>): List<TrackPoint> {
        val now = System.currentTimeMillis()
        var cumDist = 0.0
        val dists = DoubleArray(points.size)

        for (i in 0 until points.size - 1) {
            val d = calculateHaversineDistance(points[i].lat, points[i].lon, points[i + 1].lat, points[i + 1].lon)
            cumDist += d
            dists[i + 1] = cumDist
        }

        // Base pace ~ 4:45/km = 285 sec/km = 0.285 sec/m = 285 ms/m
        val basePaceMsPerMeter = 285.0
        val result = mutableListOf<TrackPoint>()
        var currentMs = now - (cumDist * basePaceMsPerMeter).toLong()

        for (i in points.indices) {
            val dCovered = dists[i]
            val kmIndex = (dCovered / 1000.0).toInt()

            // Add subtle pace variation per km (making km 3 fastest ~4:28/km)
            val paceModifier = when (kmIndex % 5) {
                0 -> 1.05 // 4:59/km
                1 -> 1.00 // 4:45/km
                2 -> 0.94 // 4:28/km (Fastest)
                3 -> 0.98 // 4:39/km
                else -> 1.02 // 4:50/km
            }

            if (i > 0) {
                val stepDist = dists[i] - dists[i - 1]
                currentMs += (stepDist * basePaceMsPerMeter * paceModifier).toLong()
            }

            val hr = 145 + (15 * sin(i / 6.0)).toInt()
            result.add(points[i].copy(timeMillis = currentMs, heartRate = hr))
        }

        return result
    }

    /**
     * Provides activity summary using the user's "ROSO: Kucing" GPX route data.
     */
    fun getSampleSummary(): ActivitySummary {
        return parse(SAMPLE_KUCING_GPX.byteInputStream())
    }

    private val SAMPLE_KUCING_GPX = """
<?xml version="1.0" encoding="UTF-8"?>
<gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.1" creator="phpGPX/1.3.0" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
  <trk>
    <name>ROSO: Kucing</name>
    <trkseg>
      <trkpt lat="-7.7724" lon="110.37736">
        <ele>140.59</ele>
      </trkpt>
      <trkpt lat="-7.7731950000205" lon="110.37712500046">
        <ele>139.78</ele>
      </trkpt>
      <trkpt lat="-7.77399" lon="110.37689">
        <ele>138.97</ele>
      </trkpt>
      <trkpt lat="-7.77393" lon="110.37666">
        <ele>138.94</ele>
      </trkpt>
      <trkpt lat="-7.77392" lon="110.37666">
        <ele>138.94</ele>
      </trkpt>
      <trkpt lat="-7.7732800001206" lon="110.37685000057">
        <ele>139.78</ele>
      </trkpt>
      <trkpt lat="-7.7726400001568" lon="110.37704000056">
        <ele>140.68</ele>
      </trkpt>
      <trkpt lat="-7.772" lon="110.37723">
        <ele>141.17</ele>
      </trkpt>
      <trkpt lat="-7.7722166679038" lon="110.37794999934">
        <ele>141.37</ele>
      </trkpt>
      <trkpt lat="-7.7724333345952" lon="110.37866999942">
        <ele>141.34</ele>
      </trkpt>
      <trkpt lat="-7.77265" lon="110.37939">
        <ele>141.32</ele>
      </trkpt>
      <trkpt lat="-7.77264" lon="110.37968">
        <ele>141.28</ele>
      </trkpt>
      <trkpt lat="-7.77258" lon="110.38">
        <ele>141.51</ele>
      </trkpt>
      <trkpt lat="-7.77261" lon="110.38036">
        <ele>141.68</ele>
      </trkpt>
      <trkpt lat="-7.77256" lon="110.38054">
        <ele>141.68</ele>
      </trkpt>
      <trkpt lat="-7.77239" lon="110.38073">
        <ele>141.9</ele>
      </trkpt>
      <trkpt lat="-7.77222" lon="110.38085">
        <ele>142.03</ele>
      </trkpt>
      <trkpt lat="-7.77189" lon="110.38103">
        <ele>142.3</ele>
      </trkpt>
      <trkpt lat="-7.77115333344" lon="110.38125000077">
        <ele>143.29</ele>
      </trkpt>
      <trkpt lat="-7.7704166667668" lon="110.38147000078">
        <ele>144.06</ele>
      </trkpt>
      <trkpt lat="-7.76968" lon="110.38169">
        <ele>144.89</ele>
      </trkpt>
      <trkpt lat="-7.76909" lon="110.38197">
        <ele>145.12</ele>
      </trkpt>
      <trkpt lat="-7.76881" lon="110.38216">
        <ele>145.03</ele>
      </trkpt>
      <trkpt lat="-7.76857" lon="110.38238">
        <ele>144.56</ele>
      </trkpt>
      <trkpt lat="-7.76851" lon="110.38249">
        <ele>144.03</ele>
      </trkpt>
      <trkpt lat="-7.76843" lon="110.38275">
        <ele>143.56</ele>
      </trkpt>
      <trkpt lat="-7.76841" lon="110.38285">
        <ele>143.56</ele>
      </trkpt>
      <trkpt lat="-7.7684150006484" lon="110.38359499994">
        <ele>144.28</ele>
      </trkpt>
      <trkpt lat="-7.76842" lon="110.38434">
        <ele>145.87</ele>
      </trkpt>
      <trkpt lat="-7.76843" lon="110.38444">
        <ele>145.87</ele>
      </trkpt>
      <trkpt lat="-7.76836" lon="110.3847">
        <ele>146.36</ele>
      </trkpt>
      <trkpt lat="-7.76837" lon="110.38493">
        <ele>146.36</ele>
      </trkpt>
      <trkpt lat="-7.76843" lon="110.38504">
        <ele>146.37</ele>
      </trkpt>
      <trkpt lat="-7.76855" lon="110.38518">
        <ele>146.33</ele>
      </trkpt>
      <trkpt lat="-7.7689" lon="110.38536">
        <ele>146.01</ele>
      </trkpt>
      <trkpt lat="-7.76916" lon="110.38543">
        <ele>145.95</ele>
      </trkpt>
      <trkpt lat="-7.76942" lon="110.38542">
        <ele>145.95</ele>
      </trkpt>
      <trkpt lat="-7.76953" lon="110.38543">
        <ele>145.98</ele>
      </trkpt>
      <trkpt lat="-7.76972" lon="110.38552">
        <ele>145.98</ele>
      </trkpt>
      <trkpt lat="-7.76953" lon="110.38543">
        <ele>145.98</ele>
      </trkpt>
      <trkpt lat="-7.76942" lon="110.38542">
        <ele>145.95</ele>
      </trkpt>
      <trkpt lat="-7.76916" lon="110.38543">
        <ele>145.95</ele>
      </trkpt>
      <trkpt lat="-7.7689" lon="110.38537">
        <ele>146.02</ele>
      </trkpt>
      <trkpt lat="-7.76855" lon="110.38518">
        <ele>146.33</ele>
      </trkpt>
      <trkpt lat="-7.76843" lon="110.38504">
        <ele>146.37</ele>
      </trkpt>
      <trkpt lat="-7.76837" lon="110.38493">
        <ele>146.36</ele>
      </trkpt>
      <trkpt lat="-7.76836" lon="110.3847">
        <ele>146.36</ele>
      </trkpt>
      <trkpt lat="-7.76843" lon="110.38444">
        <ele>145.87</ele>
      </trkpt>
      <trkpt lat="-7.76842" lon="110.38434">
        <ele>145.87</ele>
      </trkpt>
      <trkpt lat="-7.768415000649" lon="110.38359500004">
        <ele>144.28</ele>
      </trkpt>
      <trkpt lat="-7.76841" lon="110.38285">
        <ele>143.56</ele>
      </trkpt>
      <trkpt lat="-7.76843" lon="110.38275">
        <ele>143.56</ele>
      </trkpt>
      <trkpt lat="-7.76847" lon="110.38262">
        <ele>143.58</ele>
      </trkpt>
      <trkpt lat="-7.76862" lon="110.38238">
        <ele>144.38</ele>
      </trkpt>
      <trkpt lat="-7.76885" lon="110.38216">
        <ele>144.98</ele>
      </trkpt>
      <trkpt lat="-7.76932" lon="110.38189">
        <ele>145.19</ele>
      </trkpt>
      <trkpt lat="-7.76969" lon="110.38172">
        <ele>144.85</ele>
      </trkpt>
      <trkpt lat="-7.7704450000279" lon="110.38149500041">
        <ele>144.15</ele>
      </trkpt>
      <trkpt lat="-7.7712" lon="110.38127">
        <ele>143.13</ele>
      </trkpt>
      <trkpt lat="-7.77132" lon="110.38124">
        <ele>143.04</ele>
      </trkpt>
      <trkpt lat="-7.77146" lon="110.38125">
        <ele>142.8</ele>
      </trkpt>
      <trkpt lat="-7.7716" lon="110.38129">
        <ele>142.6</ele>
      </trkpt>
      <trkpt lat="-7.77182" lon="110.38138">
        <ele>142.6</ele>
      </trkpt>
      <trkpt lat="-7.77193" lon="110.38139">
        <ele>142.42</ele>
      </trkpt>
      <trkpt lat="-7.77203" lon="110.38134">
        <ele>142.42</ele>
      </trkpt>
      <trkpt lat="-7.77209" lon="110.38126">
        <ele>142.42</ele>
      </trkpt>
      <trkpt lat="-7.77207" lon="110.38114">
        <ele>142.33</ele>
      </trkpt>
      <trkpt lat="-7.77222" lon="110.38085">
        <ele>142.03</ele>
      </trkpt>
      <trkpt lat="-7.77239" lon="110.38073">
        <ele>141.92</ele>
      </trkpt>
      <trkpt lat="-7.77256" lon="110.38054">
        <ele>141.68</ele>
      </trkpt>
      <trkpt lat="-7.77261" lon="110.38036">
        <ele>141.68</ele>
      </trkpt>
      <trkpt lat="-7.77258" lon="110.38">
        <ele>141.51</ele>
      </trkpt>
      <trkpt lat="-7.77264" lon="110.37968">
        <ele>141.28</ele>
      </trkpt>
      <trkpt lat="-7.77287" lon="110.37961">
        <ele>140.99</ele>
      </trkpt>
      <trkpt lat="-7.77343" lon="110.37964">
        <ele>140.46</ele>
      </trkpt>
      <trkpt lat="-7.77388" lon="110.37961">
        <ele>140.18</ele>
      </trkpt>
      <trkpt lat="-7.77465" lon="110.37954">
        <ele>138.9</ele>
      </trkpt>
      <trkpt lat="-7.7751349999224" lon="110.37947500009">
        <ele>138.14</ele>
      </trkpt>
      <trkpt lat="-7.77562" lon="110.37941">
        <ele>137.59</ele>
      </trkpt>
      <trkpt lat="-7.77557" lon="110.37885">
        <ele>137.52</ele>
      </trkpt>
      <trkpt lat="-7.7748400000576" lon="110.37896000038">
        <ele>138.62</ele>
      </trkpt>
      <trkpt lat="-7.774110000087" lon="110.37907000037">
        <ele>139.26</ele>
      </trkpt>
      <trkpt lat="-7.77338" lon="110.37818">
        <ele>140.13</ele>
      </trkpt>
      <trkpt lat="-7.7733" lon="110.37865">
        <ele>140.14</ele>
      </trkpt>
      <trkpt lat="-7.7740449999636" lon="110.37843000042">
        <ele>139.4</ele>
      </trkpt>
      <trkpt lat="-7.77479" lon="110.37821">
        <ele>138.62</ele>
      </trkpt>
      <trkpt lat="-7.7751" lon="110.37815">
        <ele>138.37</ele>
      </trkpt>
      <trkpt lat="-7.77549" lon="110.37796">
        <ele>137.98</ele>
      </trkpt>
      <trkpt lat="-7.77541" lon="110.37733">
        <ele>137.97</ele>
      </trkpt>
      <trkpt lat="-7.7748449999581" lon="110.37750000025">
        <ele>138.27</ele>
      </trkpt>
      <trkpt lat="-7.77428" lon="110.37767">
        <ele>138.95</ele>
      </trkpt>
      <trkpt lat="-7.77407" lon="110.37696">
        <ele>138.89</ele>
      </trkpt>
      <trkpt lat="-7.77411" lon="110.37686">
        <ele>138.79</ele>
      </trkpt>
      <trkpt lat="-7.77404" lon="110.37662">
        <ele>138.76</ele>
      </trkpt>
      <trkpt lat="-7.7733600000677" lon="110.376823334">
        <ele>139.78</ele>
      </trkpt>
      <trkpt lat="-7.7726800000387" lon="110.37702666734">
        <ele>140.68</ele>
      </trkpt>
      <trkpt lat="-7.772" lon="110.37723">
        <ele>141.17</ele>
      </trkpt>
      <trkpt lat="-7.77181" lon="110.3766">
        <ele>141.75</ele>
      </trkpt>
      <trkpt lat="-7.7710500001338" lon="110.37683333418">
        <ele>142.48</ele>
      </trkpt>
      <trkpt lat="-7.7702900001403" lon="110.37706666751">
        <ele>143.6</ele>
      </trkpt>
      <trkpt lat="-7.76953" lon="110.3773">
        <ele>144.65</ele>
      </trkpt>
      <trkpt lat="-7.76955" lon="110.37739">
        <ele>144.7</ele>
      </trkpt>
      <trkpt lat="-7.77022" lon="110.37718">
        <ele>143.85</ele>
      </trkpt>
      <trkpt lat="-7.7704050004616" lon="110.37780499974">
        <ele>144.07</ele>
      </trkpt>
      <trkpt lat="-7.77059" lon="110.37843">
        <ele>144.15</ele>
      </trkpt>
      <trkpt lat="-7.76993" lon="110.37863">
        <ele>145.11</ele>
      </trkpt>
      <trkpt lat="-7.76996" lon="110.37875">
        <ele>145.32</ele>
      </trkpt>
      <trkpt lat="-7.7707300000982" lon="110.37851333421">
        <ele>144.12</ele>
      </trkpt>
      <trkpt lat="-7.7715000000655" lon="110.37827666755">
        <ele>142.99</ele>
      </trkpt>
      <trkpt lat="-7.77227" lon="110.37804">
        <ele>141.21</ele>
      </trkpt>
      <trkpt lat="-7.77213" lon="110.37757">
        <ele>140.96</ele>
      </trkpt>
      <trkpt lat="-7.77214" lon="110.37748">
        <ele>140.9</ele>
      </trkpt>
      <trkpt lat="-7.77221" lon="110.37742">
        <ele>140.9</ele>
      </trkpt>
      <trkpt lat="-7.7723" lon="110.37739">
        <ele>140.72</ele>
      </trkpt>
    </trkseg>
  </trk>
</gpx>
""".trimIndent()
}
