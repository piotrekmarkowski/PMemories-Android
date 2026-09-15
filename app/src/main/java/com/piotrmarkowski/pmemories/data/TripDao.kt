package com.piotrmarkowski.pmemories.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Transaction
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeAllWithStops(): Flow<List<TripWithStops>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getWithStops(id: String): TripWithStops?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM stops WHERE tripId = :tripId")
    suspend fun deleteStopsForTrip(tripId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    /** Replaces a trip's whole stop list atomically — same pattern as iOS
     * `TravelMapView.persistTrip` (delete old stops, insert the new set). */
    @Transaction
    suspend fun replaceStops(tripId: String, stops: List<StopEntity>) {
        deleteStopsForTrip(tripId)
        insertStops(stops)
    }
}
