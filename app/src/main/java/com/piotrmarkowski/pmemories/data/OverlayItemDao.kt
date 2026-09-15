package com.piotrmarkowski.pmemories.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OverlayItemDao {
    @Query("SELECT * FROM overlay_items WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun forProject(projectId: String): List<OverlayItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(overlays: List<OverlayItemEntity>)

    @Delete
    suspend fun delete(overlay: OverlayItemEntity)
}
