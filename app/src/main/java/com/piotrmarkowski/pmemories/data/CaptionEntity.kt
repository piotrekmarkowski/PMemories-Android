package com.piotrmarkowski.pmemories.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

/** 1:1 field parity with iOS `SavedCaption`. */
@Entity(
    tableName = "captions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CaptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true) val projectId: String?,
    val text: String = "",
    val startTime: Double = 0.0,
    val endTime: Double = 3.0,
    val positionRawValue: String = "bottom",
    val fontRawValue: String = "helvetica",
    val order: Int = 0,
    /** JSON-encoded language-code -> translated-text map, same as iOS (Room has no native Map column type). */
    val translationsJson: String? = null
)
