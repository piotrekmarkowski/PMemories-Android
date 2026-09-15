package com.piotrmarkowski.pmemories.travel

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class CityResult(
    val cityName: String,
    val country: String?,
    val countryCode: String?,
    val administrativeArea: String?,
    val latitude: Double,
    val longitude: Double
)

/**
 * 15.09.2026 — Android analog of iOS `CitySearchCompleter`, but using the
 * platform's built-in `Geocoder` (free, no API key — Google Places
 * Autocomplete would need a billed key, same class of blocker as Maps
 * Directions) instead of Apple's `MKLocalSearchCompleter`. Trade-off: no
 * live-typing suggestions, only a lookup fired on submit — same "search on
 * submit, not on every keystroke" pattern iOS already uses for
 * `PeakSearchView` (Overpass isn't cheap/fast enough for live typing
 * either), so the UX gap versus iOS's city search specifically is small.
 */
object CitySearchProvider {
    /** Reverse geocode — coordinates to city/country, used by
     * `SmartRouteDetector` to name each detected cluster (iOS's
     * `CityGeocoder.reverseResolveFull`, Android's `Geocoder` is the direct
     * platform equivalent for reverse lookups too). */
    suspend fun reverseGeocode(context: Context, point: LatLngPoint): CityResult? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            try {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val address = geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
                    ?: return@withContext null
                val cityName = address.locality ?: address.subAdminArea ?: address.adminArea ?: return@withContext null
                CityResult(
                    cityName = cityName,
                    country = address.countryName,
                    countryCode = address.countryCode,
                    administrativeArea = address.adminArea,
                    latitude = point.latitude,
                    longitude = point.longitude
                )
            } catch (_: Exception) {
                null
            }
        }

    suspend fun search(context: Context, query: String, maxResults: Int = 8): List<CityResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()
            try {
                @Suppress("DEPRECATION") // sync overload — simplest cross-API-level path, minSdk 26
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, maxResults) ?: emptyList()
                addresses.mapNotNull { address ->
                    val cityName = address.locality ?: address.subAdminArea ?: address.featureName ?: return@mapNotNull null
                    CityResult(
                        cityName = cityName,
                        country = address.countryName,
                        countryCode = address.countryCode,
                        administrativeArea = address.adminArea,
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
}
