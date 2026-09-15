package com.piotrmarkowski.pmemories.travel

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class DatedLocation(val uri: Uri, val location: LatLngPoint, val dateTakenMillis: Long)

/**
 * Reads GPS + capture date straight from a photo's EXIF data, via
 * `ExifInterface` on the MediaStore content URI. Android analog of iOS
 * `MediaAssetLoader.locationsAndDates` — `PHAsset.location`/`.creationDate`
 * come pre-parsed from Apple's Photos framework, Android has no such
 * higher-level API, so this reads the same underlying EXIF tags by hand.
 * Photos with no GPS tag (`latLong == null`) are skipped — same "don't
 * guess" rule as iOS.
 */
object PhotoLocationReader {
    private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    suspend fun read(context: Context, uris: List<Uri>): List<DatedLocation> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri -> readOne(context, uri) }
    }

    /** Scans the whole MediaStore images collection for entries with GPS —
     * used by "Auto-detect from all photos" rather than a pre-picked set,
     * the more common entry point for Smart Route (iOS's equivalent picks
     * from a user-selected set via `PhotosPicker`, but scanning everything
     * with a date-range filter is the more natural Android flow since
     * there's no first-class multi-select-then-hand-off UI pattern here). */
    suspend fun readAll(context: Context, limit: Int = 2000): List<DatedLocation> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DatedLocation>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC LIMIT $limit"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                readOne(context, uri)?.let { results += it }
            }
        }
        results
    }

    private fun readOne(context: Context, uri: Uri): DatedLocation? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLong = exif.latLong ?: return null
            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: return null
            val dateMillis = exifDateFormat.parse(dateString)?.time ?: return null
            DatedLocation(uri, LatLngPoint(latLong[0], latLong[1]), dateMillis)
        }
    } catch (_: Exception) {
        null
    }
}
