package com.piotrmarkowski.pmemories.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun forProject(projectId: String): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Delete
    suspend fun delete(item: MediaItemEntity)
}
