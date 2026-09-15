package com.piotrmarkowski.pmemories.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 1:1 field parity with iOS `SavedTrip` (`TripPersistence.swift`, Etap 7).
 * `isFavorite` matches iOS's 12.09.2026 addition (fiancée feedback, poster
 * stamp priority) — carried over from the start here, not bolted on later.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
