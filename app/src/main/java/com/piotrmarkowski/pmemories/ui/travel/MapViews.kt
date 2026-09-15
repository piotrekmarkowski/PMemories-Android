package com.piotrmarkowski.pmemories.ui.travel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.piotrmarkowski.pmemories.data.StopEntity
import com.piotrmarkowski.pmemories.data.TripWithStops

/**
 * A single trip's route — markers per stop + the great-circle line between
 * them (`RouteProvider`'s straight-line v1, see that file's doc for the
 * "no free turn-by-turn API on Android" trade-off). Needs a real
 * `MAPS_API_KEY` (`local.properties`) to show actual map tiles; without one
 * this renders Google's blank/watermarked placeholder tiles but the
 * markers/polyline logic is otherwise fully functional and testable once a
 * key is added.
 */
@Composable
fun TripRouteMap(trip: TripWithStops, modifier: Modifier = Modifier) {
    val stops = trip.orderedStops
    val cameraPositionState = rememberCameraPositionState {
        position = stops.firstOrNull()?.let {
            CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 4f)
        } ?: CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 1f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL)
    ) {
        stops.forEach { stop ->
            Marker(
                state = MarkerState(LatLng(stop.latitude, stop.longitude)),
                title = stop.cityName,
                snippet = stop.country
            )
        }
        if (stops.size >= 2) {
            Polyline(points = stops.map { LatLng(it.latitude, it.longitude) })
        }
    }
}

/**
 * All visited places across every trip, at once — Android analog of iOS
 * `WorldGlobeView` (Faza 1: pins only, no shaded country borders, same
 * scope iOS shipped first). `onStopTap` mirrors iOS's "tap a pin → jump to
 * Library" behaviour via `linkedProjectId`.
 */
@Composable
fun WorldGlobeMap(allStops: List<StopEntity>, onStopTap: (StopEntity) -> Unit, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 1.2f)
    }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.SATELLITE)
    ) {
        allStops.forEach { stop ->
            Marker(
                state = MarkerState(LatLng(stop.latitude, stop.longitude)),
                title = stop.cityName,
                snippet = stop.country,
                icon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) },
                onClick = { onStopTap(stop); true }
            )
        }
    }
}
