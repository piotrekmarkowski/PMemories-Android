package com.piotrmarkowski.pmemories.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 1:1 field parity with iOS `SavedMediaItem`. `mediaUri` is the Android
 * analog of `assetLocalIdentifier` (PHAsset) — a persistable MediaStore
 * content URI obtained via the Photo Picker, with a persisted URI permission
 * so it survives across app restarts.
 *
 * `isLivePhoto` has no true Android equivalent yet (Motion Photo detection
 * is a later, separate concern) — field kept for schema parity, always
 * false until that lands.
 */
@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true) val projectId: String?,
    val mediaUri: String = "",
    val isLivePhoto: Boolean = false,
    val isVideo: Boolean = false,
    val useMotion: Boolean = false,
    val duration: Double = 3.0,
    /** Position in the timeline — Room doesn't guarantee relation order either. */
    val order: Int = 0,
    val trimStart: Double = 0.0,
    val isManuallyTrimmed: Boolean = false,
    val speed: Double = 1.0,
    val rotationDegrees: Int = 0,
    val cropFill: Boolean = false,
    val originalVolume: Double = 1.0,
    /** Raw TransitionStyle value; null = auto-assigned. */
    val transitionStyleRawValue: String? = null
)
