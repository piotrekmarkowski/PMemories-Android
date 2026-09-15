package com.piotrmarkowski.pmemories.studio

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/**
 * Copies a finished export out of app-private storage into the public
 * gallery (Movies/PMemories) so it shows up like any other video — mirrors
 * iOS saving the export back into Photos. Two paths because scoped storage
 * (MediaStore insert + IS_PENDING) only exists from API 29; below that it's
 * a direct write to the public Movies dir plus a manual media-scan.
 */
object GallerySaver {

    fun saveVideo(context: Context, sourceFile: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, sourceFile)
        } else {
            saveLegacy(context, sourceFile)
        }
    }

    private fun saveViaMediaStore(context: Context, sourceFile: File): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PMemories")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert failed")

        resolver.openOutputStream(uri)?.use { out ->
            sourceFile.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("Could not open output stream for $uri")

        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun saveLegacy(context: Context, sourceFile: File): Uri {
        val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "PMemories")
        moviesDir.mkdirs()
        val destFile = File(moviesDir, sourceFile.name)
        sourceFile.copyTo(destFile, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("video/mp4"), null)
        return Uri.fromFile(destFile)
    }
}
