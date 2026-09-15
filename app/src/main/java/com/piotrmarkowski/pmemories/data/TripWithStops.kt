package com.piotrmarkowski.pmemories.data

import androidx.room.Embedded
import androidx.room.Relation

data class TripWithStops(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val stops: List<StopEntity>
) {
    /** Stops in trip order, not DB insertion order — Room doesn't guarantee
     * relation ordering (same reasoning as `StopEntity.order`'s own doc). */
    val orderedStops: List<StopEntity> get() = stops.sortedBy { it.order }
}
