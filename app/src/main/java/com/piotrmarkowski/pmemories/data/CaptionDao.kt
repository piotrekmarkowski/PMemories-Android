package com.piotrmarkowski.pmemories.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CaptionDao {
    @Query("SELECT * FROM captions WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun forProject(projectId: String): List<CaptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(captions: List<CaptionEntity>)

    @Delete
    suspend fun delete(caption: CaptionEntity)
}
