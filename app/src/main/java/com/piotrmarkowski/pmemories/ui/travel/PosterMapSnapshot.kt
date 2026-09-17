package com.piotrmarkowski.pmemories.ui.travel

import android.graphics.Bitmap
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.piotrmarkowski.pmemories.data.StopEntity

/**
 * Renderuje mapę wszystkich odwiedzonych miejsc (piny + trasy każdej
 * podróży osobno, ten sam duch co iOS `renderMapSnapshot`) i ODDAJE
 * gotowy bitmapowy zrzut przez `onSnapshot` — Android odpowiednik iOS
 * `MKMapSnapshotter`. Sama mapa jest tu TYLKO narzędziem produkcyjnym:
 * renderuje się raz (kontrolki UI wyłączone), robi zdjęcie, i wywołujący
 * ją composable ją chowa/zastępuje wynikowym Bitmapem — Plakat pokazuje
 * static obraz, nie żywą interaktywną mapę (to byłoby dziwne w
 * scrollowalnym, udostępnialnym plakacie).
 */
@Composable
fun PosterMapSnapshot(
    trips: List<com.piotrmarkowski.pmemories.data.TripWithStops>,
    widthDp: Int,
    heightDp: Int,
    onSnapshot: (Bitmap?) -> Unit,
) {
    val allStops = trips.flatMap { it.orderedStops }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(allStops) {
        if (allStops.isEmpty()) {
            onSnapshot(null)
            return@LaunchedEffect
        }
        val boundsBuilder = LatLngBounds.Builder()
        allStops.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
        // pojedynczy punkt -> LatLngBounds.Builder.build() rzuca wyjątkiem
        // (brak "obszaru" do dopasowania) — dokładamy mikroskopijny margines
        // dookoła, żeby kamera miała na czym ustawić zoom.
        if (allStops.size == 1) {
            val p = allStops.first()
            boundsBuilder.include(LatLng(p.latitude + 0.01, p.longitude + 0.01))
            boundsBuilder.include(LatLng(p.latitude - 0.01, p.longitude - 0.01))
        }
        // Kamera ustawiana WPROST na środek granic (poziom zoomu dobierany
        // przybliżeniem z rozpiętości) zamiast `newLatLngBounds(padding)`,
        // które wymaga już ZMIERZONEGO, dołączonego widoku mapy — w tym
        // miejscu (LaunchedEffect przed pierwszym layoutem) `GoogleMap`
        // jeszcze nie ma rozmiaru, `newLatLngBounds` z paddingiem rzuciłby
        // `IllegalStateException("Map size can't be 0")`.
        val bounds = boundsBuilder.build()
        val center = LatLng(
            (bounds.northeast.latitude + bounds.southwest.latitude) / 2,
            (bounds.northeast.longitude + bounds.southwest.longitude) / 2
        )
        val latSpan = bounds.northeast.latitude - bounds.southwest.latitude
        val lonSpan = bounds.northeast.longitude - bounds.southwest.longitude
        val maxSpan = maxOf(latSpan, lonSpan, 0.02)
        val zoom = (Math.log(360.0 / maxSpan) / Math.log(2.0)).toFloat().coerceIn(1f, 14f)
        cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(center, zoom)
    }

    GoogleMap(
        modifier = Modifier.width(widthDp.dp).height(heightDp.dp),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false, myLocationButtonEnabled = false,
                mapToolbarEnabled = false, compassEnabled = false, scrollGesturesEnabled = false,
                zoomGesturesEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false
            )
        },
        onMapLoaded = {
            // Załadowanie kafelków ≠ gotowość do zrzutu co do klatki — mała
            // zwłoka, żeby mapa faktycznie narysowała ostatnią klatkę przed
            // `snapshot()` (ten sam ostrożny wzorzec co iOS-owy komentarz o
            // "dziewiątej rundzie" walki z timingiem renderowania mapy).
        }
    ) {
        allStops.forEach { stop ->
            Marker(state = MarkerState(LatLng(stop.latitude, stop.longitude)), title = stop.cityName)
        }
        trips.forEach { trip ->
            val points = trip.orderedStops.map { LatLng(it.latitude, it.longitude) }
            if (points.size >= 2) Polyline(points = points)
        }
        MapEffect(allStops) { map ->
            map.setOnMapLoadedCallback {
                map.snapshot { bitmap -> onSnapshot(bitmap) }
            }
        }
    }
}
