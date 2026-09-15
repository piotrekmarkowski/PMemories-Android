package com.piotrmarkowski.pmemories.travel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ElevationProfile(val gainMeters: Double, val highestMeters: Double)

/**
 * 15.09.2026 — free, no-API-key elevation lookup via open-elevation.com
 * (public instance of the open-source Open-Elevation project). Android
 * analog of iOS `ElevationProvider` (which uses Apple's own elevation
 * service through MapKit) — same role (hiking-only stats: gain + highest
 * point along a path), different, free backend since Android has no
 * built-in equivalent. Returns `null` on any failure (offline, service
 * down) — same "don't guess" rule as iOS: hiking elevation stays absent
 * rather than fabricated.
 */
object ElevationProvider {
    private const val ENDPOINT = "https://api.open-elevation.com/api/v1/lookup"

    suspend fun profile(path: List<LatLngPoint>): ElevationProfile? = withContext(Dispatchers.IO) {
        if (path.isEmpty()) return@withContext null
        // Open-Elevation's free instance rate-limits large batches — a
        // route's 64-point interpolated path (`RouteProvider`) is downsampled
        // to a coarser set of samples, plenty for gain/highest-point stats.
        val sampled = if (path.size > 20) {
            val step = path.size / 20.0
            (0 until 20).map { path[(it * step).toInt().coerceAtMost(path.size - 1)] }
        } else path

        try {
            val locations = JSONArray().apply {
                sampled.forEach { point ->
                    put(JSONObject().apply {
                        put("latitude", point.latitude)
                        put("longitude", point.longitude)
                    })
                }
            }
            val body = JSONObject().apply { put("locations", locations) }.toString()

            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            connection.outputStream.use { it.write(body.toByteArray()) }

            if (connection.responseCode !in 200..299) return@withContext null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(response).getJSONArray("results")

            var gain = 0.0
            var highest = 0.0
            var previous: Double? = null
            for (i in 0 until results.length()) {
                val elevation = results.getJSONObject(i).getDouble("elevation")
                if (elevation > highest) highest = elevation
                previous?.let { prev -> if (elevation > prev) gain += elevation - prev }
                previous = elevation
            }
            ElevationProfile(gainMeters = gain, highestMeters = highest)
        } catch (_: Exception) {
            null
        }
    }
}
