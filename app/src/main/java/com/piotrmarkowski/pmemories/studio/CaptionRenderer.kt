package com.piotrmarkowski.pmemories.studio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.piotrmarkowski.pmemories.data.CaptionEntity

/**
 * Burns caption text into a transparent bitmap the same size as the export
 * canvas, positioned by `CaptionEntity.positionRawValue` (top/bottom/center
 * — same convention as iOS `CaptionPosition`). One overlay per clip, built
 * from whichever captions overlap that clip's time range (see
 * `StudioExporter` — clip-level granularity: a caption gets shown for the
 * WHOLE clip it overlaps, not trimmed to its exact sub-second range within
 * that clip. Precise intra-clip timing would need splitting the clip at
 * the caption boundary, same idea as `StudioViewModel.splitItem` — not
 * done yet, tracked as a known limitation).
 */
object CaptionRenderer {

    fun overlayFor(captions: List<CaptionEntity>, width: Int, height: Int): BitmapOverlay? {
        if (captions.isEmpty()) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = height * 0.05f
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }

        val margin = height * 0.06f
        val grouped = captions.groupBy { it.positionRawValue }
        grouped.forEach { (position, group) ->
            val y = when (position) {
                "top" -> margin + textPaint.textSize
                "center" -> height / 2f
                else -> height - margin
            }
            group.forEachIndexed { index, caption ->
                canvas.drawText(caption.text, width / 2f, y - index * textPaint.textSize * 1.3f, textPaint)
            }
        }

        return BitmapOverlay.createStaticBitmapOverlay(
            bitmap,
            OverlaySettings.Builder().build()
        )
    }
}
