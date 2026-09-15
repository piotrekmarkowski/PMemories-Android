package com.piotrmarkowski.pmemories.travel

import android.content.Context
import android.net.Uri

data class DetectedStop(
    val cityName: String,
    val country: String?,
    val countryCode: String?,
    val administrativeArea: String?,
    val latitude: Double,
    val longitude: Double,
    val arrivalDateMillis: Long?,
    val representativePhotoUri: String?,
    val transport: TransportMode
)

/**
 * 1:1 port of iOS `SmartRouteDetector` — same explicit, non-ML thresholds
 * (20km cluster radius, 300km flight-vs-car guess), same greedy sequential
 * clustering against ALL existing clusters' current centroid (not just the
 * last one, matching iOS's 10.08.2026 fix for the "base + day-trip + back
 * to base" pattern splitting into 3 stops instead of 2).
 */
object SmartRouteDetector {
    private const val CLUSTER_RADIUS_KM = 20.0
    private const val FLIGHT_DISTANCE_THRESHOLD_KM = 300.0

    suspend fun detectStops(context: Context, uris: List<Uri>): List<DetectedStop> {
        val dated = PhotoLocationReader.read(context, uris).sortedBy { it.dateTakenMillis }
        return cluster(context, dated)
    }

    suspend fun detectStopsFromAllPhotos(context: Context, limit: Int = 2000): List<DetectedStop> {
        val dated = PhotoLocationReader.readAll(context, limit).sortedBy { it.dateTakenMillis }
        return cluster(context, dated)
    }

    private suspend fun cluster(context: Context, dated: List<DatedLocation>): List<DetectedStop> {
        if (dated.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<DatedLocation>>()
        for (point in dated) {
            var bestIndex: Int? = null
            var bestDistance = Double.MAX_VALUE
            for (index in clusters.indices) {
                val current = clusters[index]
                val centroid = LatLngPoint(
                    current.map { it.location.latitude }.average(),
                    current.map { it.location.longitude }.average()
                )
                val distance = RouteProvider.haversineKm(centroid, point.location)
                if (distance <= CLUSTER_RADIUS_KM && distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = index
                }
            }
            if (bestIndex != null) {
                clusters[bestIndex].add(point)
            } else {
                clusters.add(mutableListOf(point))
            }
        }

        val stops = mutableListOf<DetectedStop>()
        var previousCentroid: LatLngPoint? = null
        for (cluster in clusters) {
            val centroid = LatLngPoint(
                cluster.map { it.location.latitude }.average(),
                cluster.map { it.location.longitude }.average()
            )
            val resolved = CitySearchProvider.reverseGeocode(context, centroid) ?: continue
            val earliest = cluster.minByOrNull { it.dateTakenMillis }

            val transport = previousCentroid?.let { prev ->
                val distanceKm = RouteProvider.haversineKm(prev, centroid)
                if (distanceKm > FLIGHT_DISTANCE_THRESHOLD_KM) TransportMode.PLANE else TransportMode.CAR
            } ?: TransportMode.PLANE // origin stop — same convention as iOS's default order-0 value

            stops += DetectedStop(
                cityName = resolved.cityName,
                country = resolved.country,
                countryCode = resolved.countryCode,
                administrativeArea = resolved.administrativeArea,
                latitude = centroid.latitude,
                longitude = centroid.longitude,
                arrivalDateMillis = earliest?.dateTakenMillis,
                representativePhotoUri = earliest?.uri?.toString(),
                transport = transport
            )
            previousCentroid = centroid
        }
        return stops
    }
}
