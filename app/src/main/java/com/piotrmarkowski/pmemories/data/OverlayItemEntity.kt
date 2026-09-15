package com.piotrmarkowski.pmemories.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 1:1 field parity with iOS `SavedOverlayItem` (picture-in-picture overlay).
 * `globalStartTime` is relative to the WHOLE final export, same convention
 * as `CaptionEntity.startTime` — not relative to main-track `order`.
 */
@Entity(
    tableName = "overlay_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OverlayItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true) val projectId: String?,
    val mediaUri: String = "",
    val isLivePhoto: Boolean = false,
    val isVideo: Boolean = false,
    val useMotion: Boolean = false,
    val globalStartTime: Double = 0.0,
    val duration: Double = 3.0,
    val cornerRawValue: String = "bottomTrailing",
    val sizeScale: Double = 0.35,
    val order: Int = 0
)
