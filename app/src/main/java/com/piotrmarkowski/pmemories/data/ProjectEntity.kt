package com.piotrmarkowski.pmemories.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 1:1 field parity with iOS `SavedProject` (`ProjectPersistence.swift`).
 * We never store the media itself (heavy, lives in MediaStore already) —
 * only `mediaUri` per item, the Android analog of `assetLocalIdentifier`.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Manual travel-date override; null = use auto-detected date from media. */
    val manualTripDate: Long? = null,
    /** Raw MemoryCategory value; null = uncategorized. */
    val categoryRaw: String? = null,
    /** Comma-separated TransitionStyle raw values enabled for auto-layout; null = all allowed. */
    val enabledTransitionStylesRaw: String? = null,
    /** Raw ColorStyle value; null = no filter, single-pass export. */
    val colorStyleRaw: String? = null,
    /** Persistable content URI of the chosen background track; null = none. */
    val musicUri: String? = null,
    val musicVolume: Double = 1.0,
    /** Content URI of the last exported video for this project; null = not exported yet. */
    val exportedMediaUri: String? = null
)
