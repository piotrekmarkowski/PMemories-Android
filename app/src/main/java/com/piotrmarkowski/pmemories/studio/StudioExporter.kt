package com.piotrmarkowski.pmemories.studio

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import com.piotrmarkowski.pmemories.data.CaptionEntity
import com.piotrmarkowski.pmemories.data.MediaItemEntity
import com.piotrmarkowski.pmemories.data.OverlayItemEntity
import java.io.File

/**
 * Etap 2 core: sequential concatenation of clips into one MP4 via Media3
 * Transformer. Deliberately NOT yet implemented here (follow-up within
 * Etap 2, tracked in HISTORIA.md): transitions between clips (hard cuts
 * only for now), drag&drop reorder, bitrate control. Trim, split, speed,
 * rotation, crop-fill, resolution, color filters, captions, PiP overlays
 * (clip-level/globalStartTime granularity) and volume (clip + music) ARE
 * real.
 */
object StudioExporter {
    private const val IMAGE_FRAME_RATE = 30f

    fun export(
        context: Context,
        items: List<MediaItemEntity>,
        captions: List<CaptionEntity>,
        overlays: List<OverlayItemEntity>,
        colorStyle: ColorStyle,
        outputWidth: Int,
        outputHeight: Int,
        bitrate: Int,
        musicUri: String?,
        musicVolume: Double,
        outputFile: File,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ): Transformer {
        val sortedItems = items.sortedBy { it.order }

        // Absolute start time of each clip in the final timeline — needed to
        // know which captions (global start/end time, same convention as
        // iOS SavedCaption) overlap which clip.
        var cursorSeconds = 0.0
        val clipRanges = sortedItems.map { item ->
            val renderedDuration = if (item.isVideo && item.speed != 1.0) item.duration / item.speed else item.duration
            val range = cursorSeconds to (cursorSeconds + renderedDuration)
            cursorSeconds += renderedDuration
            range
        }
        val totalDurationSeconds = cursorSeconds
        val totalDurationMs = (totalDurationSeconds * 1000).toLong()

        val sequenceItems = sortedItems.mapIndexed { index, entity ->
            val (rangeStart, rangeEnd) = clipRanges[index]
            val overlappingCaptions = captions.filter { it.startTime < rangeEnd && it.endTime > rangeStart }
            toEditedMediaItem(
                entity, colorStyle, outputWidth, outputHeight, overlappingCaptions,
                isFirst = index == 0, itemStartTimeUs = (rangeStart * 1_000_000).toLong()
            )
        }
        val videoSequence = EditedMediaItemSequence(sequenceItems)

        val sequences = mutableListOf(videoSequence)

        if (!musicUri.isNullOrBlank() && totalDurationMs > 0) {
            val musicItem = EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(Uri.parse(musicUri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setEndPositionMs(totalDurationMs)
                            .build()
                    )
                    .build()
            )
                .setEffects(Effects(listOf<AudioProcessor>(GainAudioProcessor(musicVolume.toFloat())), emptyList()))
                .build()
            sequences += EditedMediaItemSequence(listOf(musicItem))
        }

        val sortedOverlays = overlays.sortedBy { it.order }
        sortedOverlays.forEach { overlay ->
            sequences += buildPipSequence(context, overlay, totalDurationSeconds)
        }

        val compositionBuilder = Composition.Builder(sequences)
        if (sortedOverlays.isNotEmpty()) {
            compositionBuilder.setVideoCompositorSettings(PipCompositorSettings(sortedOverlays))
        }

        // Etap 2 bitrate control — `DefaultEncoderFactory` + explicit
        // `VideoEncoderSettings.setBitrate` is the documented, public Media3
        // API for this (unlike the PiP multi-sequence compositor, this is a
        // straightforward encoder-configuration knob, not custom compositing
        // — genuinely low risk). `setEnableFallback(true)` (the default)
        // lets Media3 fall back to a device-supported bitrate/profile if the
        // exact requested value isn't supported, instead of failing outright.
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(bitrate).build()
            )
            .build()

        val transformer = Transformer.Builder(context)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onCompleted()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    val causeChain = generateSequence(exportException as Throwable) { it.cause }
                        .joinToString(" <- ") { it.javaClass.simpleName + ": " + it.message }
                    onError(causeChain)
                }
            })
            .build()

        transformer.start(compositionBuilder.build(), outputFile.absolutePath)
        return transformer
    }

    /** A PiP overlay only occupies part of the timeline (`globalStartTime`
     * .. `+duration`) but Media3's video compositor blends ALL sequences
     * for the full composition length — so outside its active window we
     * pad with a transparent filler image instead, rather than needing any
     * time-varying alpha logic in `PipCompositorSettings`. */
    private fun buildPipSequence(context: Context, overlay: OverlayItemEntity, totalDurationSeconds: Double): EditedMediaItemSequence {
        val fillerUri = Uri.fromFile(getOrCreateFillerFile(context))
        val items = mutableListOf<EditedMediaItem>()

        val preFiller = overlay.globalStartTime
        if (preFiller > 0.0) items += fillerItem(fillerUri, preFiller)

        val overlayMediaItem = MediaItem.Builder().setUri(Uri.parse(overlay.mediaUri))
        var overlayBuilder = if (overlay.isVideo) {
            EditedMediaItem.Builder(
                overlayMediaItem.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs((overlay.duration * 1000).toLong())
                        .build()
                ).build()
            )
        } else {
            EditedMediaItem.Builder(overlayMediaItem.build())
                .setDurationUs((overlay.duration * 1_000_000).toLong())
                .setFrameRate(IMAGE_FRAME_RATE.toInt())
        }
        overlayBuilder = overlayBuilder.setRemoveAudio(true)
        items += overlayBuilder.build()

        val postFiller = (totalDurationSeconds - overlay.globalStartTime - overlay.duration).coerceAtLeast(0.0)
        if (postFiller > 0.0) items += fillerItem(fillerUri, postFiller)

        return EditedMediaItemSequence(items)
    }

    /** A tiny transparent PNG written once to the app's cache dir and
     * referenced via `file://` — a bundled `android.resource://` drawable
     * URI was tried first and failed with "asset loader has no track to
     * output" (Media3's default asset loader apparently doesn't resolve
     * that scheme for single-image inputs); `file://` is the same scheme
     * already proven to work for every other clip in this exporter. */
    private fun getOrCreateFillerFile(context: Context): File {
        val file = File(context.cacheDir, "pip_filler.png")
        if (!file.exists()) {
            val bitmap = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        return file
    }

    private fun fillerItem(uri: Uri, durationSeconds: Double): EditedMediaItem =
        EditedMediaItem.Builder(MediaItem.fromUri(uri))
            .setDurationUs((durationSeconds * 1_000_000).toLong())
            .setFrameRate(IMAGE_FRAME_RATE.toInt())
            .setRemoveAudio(true)
            .build()

    private fun toEditedMediaItem(
        entity: MediaItemEntity,
        colorStyle: ColorStyle,
        outputWidth: Int,
        outputHeight: Int,
        overlappingCaptions: List<CaptionEntity>,
        isFirst: Boolean,
        itemStartTimeUs: Long
    ): EditedMediaItem {
        val uri = Uri.parse(entity.mediaUri)
        var mediaItem = MediaItem.Builder().setUri(uri)

        if (entity.isVideo) {
            val startMs = (entity.trimStart * 1000).toLong()
            val endMs = startMs + (entity.duration * 1000).toLong()
            mediaItem = mediaItem.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
        }

        val videoEffects = mutableListOf<Effect>()
        if (entity.rotationDegrees != 0) {
            videoEffects += ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(entity.rotationDegrees.toFloat())
                .build()
        }
        if (entity.isVideo && entity.speed != 1.0) {
            videoEffects += SpeedChangeEffect(entity.speed.toFloat())
        }
        // Fixed output canvas — also where crop-fill vs letterbox is decided,
        // has to come before overlay/color so those apply post-crop.
        videoEffects += Presentation.createForWidthAndHeight(
            outputWidth,
            outputHeight,
            if (entity.cropFill) Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP else Presentation.LAYOUT_SCALE_TO_FIT
        )
        // Etap 2 transitions — entrance-only animation on the INCOMING clip
        // (see `TransitionStyle` doc for why not a real two-clip blend).
        // After `Presentation` so translate/scale/rotate operate on the
        // already canvas-sized frame, before color/captions so a fading-in
        // frame still gets the right color grade and caption burned in.
        if (!isFirst) {
            TransitionStyle.fromRaw(entity.transitionStyleRawValue)?.let { style ->
                videoEffects += if (style == TransitionStyle.CROSSFADE) {
                    TransitionEntranceFade(itemStartTimeUs, TRANSITION_DURATION_US)
                } else {
                    TransitionEntranceMatrix(style, itemStartTimeUs, TRANSITION_DURATION_US)
                }
            }
        }
        colorStyle.toRgbMatrix()?.let { videoEffects += it }
        CaptionRenderer.overlayFor(overlappingCaptions, outputWidth, outputHeight)?.let {
            videoEffects += OverlayEffect(ImmutableList.of(it))
        }

        val audioProcessors = mutableListOf<AudioProcessor>()
        if (entity.isVideo && entity.originalVolume != 1.0) {
            audioProcessors += GainAudioProcessor(entity.originalVolume.toFloat())
        }

        val builder = EditedMediaItem.Builder(mediaItem.build())
            .setEffects(Effects(audioProcessors, videoEffects))
            .setRemoveAudio(!entity.isVideo)

        if (!entity.isVideo) {
            builder.setDurationUs((entity.duration * 1_000_000).toLong())
            builder.setFrameRate(IMAGE_FRAME_RATE.toInt())
        }

        return builder.build()
    }
}
