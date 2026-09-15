package com.piotrmarkowski.pmemories.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room doesn't support embedded relation lists directly on the entity like
 * SwiftData's `@Relationship` does — this wrapper is the query-time
 * equivalent of reading `project.items` / `project.captions` / `project.overlays`.
 */
data class ProjectWithDetails(
    @Embedded val project: ProjectEntity,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val items: List<MediaItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val captions: List<CaptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val overlays: List<OverlayItemEntity>
)
