package com.piotrmarkowski.pmemories.travel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 15.09.2026 — shared plumbing for both `RouteVideoRenderer` and
 * `TravelReplayRenderer`: render a list of already-composed `Bitmap` frames
 * to temp PNG files, then encode them into one output video via the SAME
 * Media3 Transformer pipeline `StudioExporter` already uses for photos
 * (each frame becomes a short "image" `EditedMediaItem`, chained in
 * sequence — the exact technique `StudioExporter.fillerItem` already relies
 * on for photo durations, just applied to generated frames instead of
 * user-picked photos).
 */
object BitmapSequenceVideoRenderer {
    suspend fun render(
        context: Context,
        frames: List<Bitmap>,
        frameDurationSeconds: Double,
        outputFile: File,
        outputWidth: Int = 1080,
        outputHeight: Int = 1920
    ): Uri = suspendCancellableCoroutine { continuation ->
        val tempDir = File(context.cacheDir, "travel_frames_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val items = frames.mapIndexed { index, bitmap ->
                val file = File(tempDir, "frame_$index.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(file)))
                    .setDurationUs((frameDurationSeconds * 1_000_000).toLong())
                    .setFrameRate(30)
                    .setRemoveAudio(true)
                    .build()
            }

            val sequence = EditedMediaItemSequence(items)
            val composition = Composition.Builder(listOf(sequence)).build()

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        frames.forEach { it.recycle() }
                        tempDir.deleteRecursively()
                        if (continuation.isActive) continuation.resume(Uri.fromFile(outputFile))
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: androidx.media3.transformer.ExportException) {
                        frames.forEach { it.recycle() }
                        tempDir.deleteRecursively()
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()

            transformer.start(composition, outputFile.absolutePath)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }
}
