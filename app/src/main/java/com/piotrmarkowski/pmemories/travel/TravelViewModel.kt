package com.piotrmarkowski.pmemories.travel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piotrmarkowski.pmemories.data.AppDatabase
import com.piotrmarkowski.pmemories.data.StopEntity
import com.piotrmarkowski.pmemories.data.TripEntity
import com.piotrmarkowski.pmemories.data.TripWithStops
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** One stop while a trip is being BUILT, before it's persisted — Android
 * analog of iOS's in-memory `TripStop` (`TravelMap.swift`). */
data class DraftStop(
    val localId: String = UUID.randomUUID().toString(),
    val cityName: String,
    val country: String?,
    val countryCode: String?,
    val administrativeArea: String?,
    val latitude: Double,
    val longitude: Double,
    val transport: TransportMode,
    val arrivalDateMillis: Long? = null,
    val representativePhotoUri: String? = null
)

data class TravelUiState(
    val trips: List<TripWithStops> = emptyList(),
    val draftStops: List<DraftStop> = emptyList(),
    val editingTripId: String? = null,
    val isDetecting: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val dao = db.tripDao()

    private val draftStops = MutableStateFlow<List<DraftStop>>(emptyList())
    private val editingTripId = MutableStateFlow<String?>(null)
    private val isDetecting = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val trips = dao.observeAllWithStops()

    val uiState: StateFlow<TravelUiState> = kotlinx.coroutines.flow.combine(
        trips, draftStops, editingTripId
    ) { tripsValue, draft, editing ->
        Triple(tripsValue, draft, editing)
    }.let { combined ->
        kotlinx.coroutines.flow.combine(combined, isDetecting, isSaving, errorMessage) { (t, d, e), detecting, saving, error ->
            TravelUiState(t, d, e, detecting, saving, error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TravelUiState())

    fun addManualStop(result: CityResult, transport: TransportMode) {
        draftStops.value = draftStops.value + DraftStop(
            cityName = result.cityName,
            country = result.country,
            countryCode = result.countryCode,
            administrativeArea = result.administrativeArea,
            latitude = result.latitude,
            longitude = result.longitude,
            transport = transport
        )
    }

    fun removeDraftStop(localId: String) {
        draftStops.value = draftStops.value.filterNot { it.localId == localId }
    }

    fun moveDraftStop(fromIndex: Int, toIndex: Int) {
        val current = draftStops.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        draftStops.value = current
    }

    fun updateDraftTransport(localId: String, transport: TransportMode) {
        draftStops.value = draftStops.value.map {
            if (it.localId == localId) it.copy(transport = transport) else it
        }
    }

    fun detectFromPhotos(uris: List<Uri>) {
        isDetecting.value = true
        viewModelScope.launch {
            try {
                val detected = SmartRouteDetector.detectStops(getApplication(), uris)
                draftStops.value = detected.map { it.toDraftStop() }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Detection failed"
            } finally {
                isDetecting.value = false
            }
        }
    }

    fun detectFromAllPhotos() {
        isDetecting.value = true
        viewModelScope.launch {
            try {
                val detected = SmartRouteDetector.detectStopsFromAllPhotos(getApplication())
                draftStops.value = detected.map { it.toDraftStop() }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Detection failed"
            } finally {
                isDetecting.value = false
            }
        }
    }

    private fun DetectedStop.toDraftStop() = DraftStop(
        cityName = cityName, country = country, countryCode = countryCode,
        administrativeArea = administrativeArea, latitude = latitude, longitude = longitude,
        transport = transport, arrivalDateMillis = arrivalDateMillis, representativePhotoUri = representativePhotoUri
    )

    fun startEditing(tripId: String?) {
        editingTripId.value = tripId
        if (tripId == null) {
            draftStops.value = emptyList()
            return
        }
        viewModelScope.launch {
            val existing = dao.getWithStops(tripId) ?: return@launch
            draftStops.value = existing.orderedStops.map { stop ->
                DraftStop(
                    localId = stop.id, cityName = stop.cityName, country = stop.country,
                    countryCode = stop.countryCode, administrativeArea = stop.administrativeArea,
                    latitude = stop.latitude, longitude = stop.longitude,
                    transport = TransportMode.fromRaw(stop.transportRawValue),
                    arrivalDateMillis = stop.arrivalDate, representativePhotoUri = stop.representativePhotoUri
                )
            }
        }
    }

    fun discardDraft() {
        draftStops.value = emptyList()
        editingTripId.value = null
    }

    /** Persists the draft — resolves each leg's real distance (and, for
     * hiking legs, elevation profile) via `RouteProvider`/`ElevationProvider`
     * before writing, same as iOS `TravelMapView.persistTrip`. */
    fun saveTrip(title: String) {
        val stops = draftStops.value
        if (stops.isEmpty()) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                val tripId = editingTripId.value ?: UUID.randomUUID().toString()
                val entities = mutableListOf<StopEntity>()
                var previous: LatLngPoint? = null

                stops.forEachIndexed { index, stop ->
                    val point = LatLngPoint(stop.latitude, stop.longitude)
                    var legDistanceKm = 0.0
                    var gain: Double? = null
                    var highest: Double? = null
                    if (previous != null) {
                        val route = RouteProvider.route(previous!!, point)
                        legDistanceKm = route.distanceKm
                        if (stop.transport == TransportMode.HIKING) {
                            ElevationProvider.profile(route.path)?.let {
                                gain = it.gainMeters
                                highest = it.highestMeters
                            }
                        }
                    }
                    previous = point
                    entities += StopEntity(
                        id = stop.localId, tripId = tripId, cityName = stop.cityName,
                        country = stop.country, countryCode = stop.countryCode,
                        latitude = stop.latitude, longitude = stop.longitude,
                        transportRawValue = stop.transport.rawValue, order = index,
                        arrivalDate = stop.arrivalDateMillis, legDistanceKm = legDistanceKm,
                        elevationGainMeters = gain, highestElevationMeters = highest,
                        representativePhotoUri = stop.representativePhotoUri,
                        administrativeArea = stop.administrativeArea
                    )
                }

                val resolvedTitle = title.ifBlank {
                    val first = stops.first().cityName
                    val last = stops.last().cityName
                    if (stops.size >= 2) "$first → $last" else first
                }

                if (editingTripId.value != null) {
                    dao.updateTrip(TripEntity(id = tripId, title = resolvedTitle))
                } else {
                    dao.upsertTrip(TripEntity(id = tripId, title = resolvedTitle))
                }
                dao.replaceStops(tripId, entities)
                draftStops.value = emptyList()
                editingTripId.value = null
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Save failed"
            } finally {
                isSaving.value = false
            }
        }
    }

    fun deleteTrip(trip: TripWithStops) {
        viewModelScope.launch { dao.deleteTrip(trip.trip) }
    }

    fun toggleFavorite(trip: TripWithStops) {
        viewModelScope.launch { dao.updateTrip(trip.trip.copy(isFavorite = !trip.trip.isFavorite)) }
    }

    fun clearError() {
        errorMessage.value = null
    }
}
