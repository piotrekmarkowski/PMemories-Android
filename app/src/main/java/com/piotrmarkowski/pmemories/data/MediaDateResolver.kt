package com.piotrmarkowski.pmemories.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/**
 * Android analog of iOS `MediaAssetLoader.earliestCreationDate` (used by
 * `LibraryView` for grouping-by-year and as the "Edit Date" starting value).
 * There `PHAsset.creationDate` comes straight from Photos; here we query
 * `MediaStore.MediaColumns.DATE_TAKEN` for each item's content URI instead.
 * The Photo Picker's `content://media/picker/...` URIs are the same
 * query-compatible façade as regular MediaStore URIs — no extra permission
 * needed beyond the per-URI read grant the picker already attaches.
 */
object MediaDateResolver {
    /** Earliest capture date across all given content URIs, in epoch millis — `null` if none resolve (e.g. offline/stubbed test data). */
    fun earliestCreationDate(context: Context, uris: List<String>): Long? {
        var earliest: Long? = null
        for (uriString in uris) {
            val date = creationDate(context, uriString) ?: continue
            if (earliest == null || date < earliest) earliest = date
        }
        return earliest
    }

    private fun creationDate(context: Context, uriString: String): Long? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED),
                null, null, null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val takenIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                if (taken > 0) return@use taken
                // Many images (esp. screenshots, downloaded/shared photos)
                // have no DATE_TAKEN at all — DATE_ADDED as fallback, same
                // spirit as iOS falling back to `updatedAt` one level up in
                // `LibraryViewModel`. Stored in SECONDS, not millis, unlike
                // DATE_TAKEN — easy to silently get a 1970 date wrong here.
                val addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val added = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L
                if (added > 0) added * 1000 else null
            }
        }.getOrNull()
    }
}
