package com.piotrmarkowski.pmemories.travel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PeakResult(val name: String, val latitude: Double, val longitude: Double, val elevationMeters: Double?)

/**
 * 15.09.2026 — search-by-name peak lookup via the Overpass API
 * (OpenStreetMap), same free, no-key data source iOS uses
 * (`PeakDetector.searchPeaks`). Deliberately name-search only for v1 — iOS
 * also has a GPS-based "nearest peak to where I'm standing" mode
 * (`PeakDetector`'s live-location path); that needs `ACCESS_FINE_LOCATION`
 * and a one-time location fetch, left for a follow-up rather than blocking
 * the whole Travel Map build on it (name search alone unblocks route
 * building from anywhere, which is the more common case per iOS's own
 * 09.09.2026 finding — see `TravelMapView.swift` comment on why "search by
 * name" was added alongside "from my location").
 */
object PeakSearchProvider {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    suspend fun search(query: String, limit: Int = 15): List<PeakResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val escaped = query.replace("\"", "\\\"")
            val ql = """
                [out:json][timeout:15];
                node["natural"="peak"]["name"~"$escaped",i];
                out body $limit;
            """.trimIndent()

            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            connection.outputStream.use {
                it.write("data=${URLEncoder.encode(ql, "UTF-8")}".toByteArray())
            }
            if (connection.responseCode !in 200..299) return@withContext emptyList()

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val elements = JSONObject(response).getJSONArray("elements")
            (0 until elements.length()).mapNotNull { i ->
                val element = elements.getJSONObject(i)
                val tags = element.optJSONObject("tags") ?: return@mapNotNull null
                val name = tags.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val elevation = tags.optString("ele").toDoubleOrNull()
                PeakResult(
                    name = name,
                    latitude = element.getDouble("lat"),
                    longitude = element.getDouble("lon"),
                    elevationMeters = elevation
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
