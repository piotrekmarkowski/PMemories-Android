package com.piotrmarkowski.pmemories.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.piotrmarkowski.pmemories.travel.TransportMode
import java.util.UUID

/**
 * 1:1 field parity with iOS `SavedStop` (`TripPersistence.swift`, Etap 7).
 * `order` (not relation ordering) decides sequence — same reason as iOS:
 * Room doesn't guarantee row order for a one-to-many relation either.
 * `legDistanceKm`/elevation fields are 0/null for `order == 0` (the
 * synthetic origin stop with no real leg before it) — same convention iOS
 * relies on throughout (`TravelJourneyPosterView.flightCount`,
 * `TravelAchievementsCalculator.explorerScore`'s `stopsWithRealLeg` filter).
 */
@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class StopEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val cityName: String = "",
    val country: String? = null,
    val countryCode: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val transportRawValue: String = TransportMode.PLANE.rawValue,
    val order: Int = 0,
    val arrivalDate: Long? = null,
    val legDistanceKm: Double = 0.0,
    val elevationGainMeters: Double? = null,
    val highestElevationMeters: Double? = null,
    /** Photo URI (MediaStore content URI) for the map marker thumbnail —
     * Android analog of iOS's `assetLocalIdentifier`-based
     * `representativePhotoIdentifier`. */
    val representativePhotoUri: String? = null,
    val linkedProjectId: String? = null,
    /** UK-only — see iOS `ukPassportRegion` for why (no separate ISO codes
     * for England/Scotland/Wales/Northern Ireland). */
    val administrativeArea: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isHiddenFromOnThisDay: Boolean = false
)
