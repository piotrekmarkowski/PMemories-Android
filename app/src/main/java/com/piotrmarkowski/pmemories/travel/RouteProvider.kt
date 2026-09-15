package com.piotrmarkowski.pmemories.travel

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(val latitude: Double, val longitude: Double)
data class RouteResult(val distanceKm: Double, val path: List<LatLngPoint>)

/**
 * 15.09.2026 — Android analog of iOS `RouteProvider`, deliberately
 * SIMPLIFIED for v1: a great-circle (Haversine) straight line between two
 * points, not real turn-by-turn routing. iOS uses Apple's `MKDirections`
 * (free, no key, built into MapKit) — there's no equivalent free API on
 * Android; the real equivalent (Google Directions API) needs a BILLED
 * Google Cloud API key, the same kind of blocker as Maps itself. Distance
 * and animation path both use the great-circle line for now — honest,
 * working v1, not real road/flight routing. Upgrade path: swap this
 * function's body for a Directions API call once billing is set up,
 * without touching any caller (same `RouteResult` shape).
 */
object RouteProvider {
    private const val EARTH_RADIUS_KM = 6371.0

    fun route(from: LatLngPoint, to: LatLngPoint): RouteResult {
        val distanceKm = haversineKm(from, to)
        val path = interpolateGreatCircle(from, to, steps = 64)
        return RouteResult(distanceKm, path)
    }

    fun haversineKm(from: LatLngPoint, to: LatLngPoint): Double {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val a = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /** Evenly-spaced points along the great-circle arc — used to animate the
     * route line/camera in `RouteVideoRenderer`, same role as MapKit's
     * polyline points on iOS (just geometrically simpler: a straight
     * great-circle line rather than a real routed polyline). */
    private fun interpolateGreatCircle(from: LatLngPoint, to: LatLngPoint, steps: Int): List<LatLngPoint> {
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)

        val d = 2 * atan2(
            sqrt(sin((lat2 - lat1) / 2).let { it * it } + cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).let { it * it }),
            sqrt(1 - (sin((lat2 - lat1) / 2).let { it * it } + cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).let { it * it }))
        )
        if (d == 0.0) return listOf(from, to)

        return (0..steps).map { i ->
            val f = i.toDouble() / steps
            val a = sin((1 - f) * d) / sin(d)
            val b = sin(f * d) / sin(d)
            val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
            val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
            val z = a * sin(lat1) + b * sin(lat2)
            val lat = atan2(z, sqrt(x * x + y * y))
            val lon = atan2(y, x)
            LatLngPoint(Math.toDegrees(lat), Math.toDegrees(lon))
        }
    }
}
