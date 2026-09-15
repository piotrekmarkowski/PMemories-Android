package com.piotrmarkowski.pmemories.studio

import android.util.Pair
import androidx.media3.common.util.Size
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.VideoCompositorSettings
import com.piotrmarkowski.pmemories.data.OverlayItemEntity

/**
 * Places each PiP overlay sequence (input index 1..N — index 0 is always
 * the main timeline) in a screen corner at a fixed scale, 1:1 with iOS
 * `OverlayCorner`/`SavedOverlayItem.sizeScale`. Static per overlay (doesn't
 * vary with time) — the "only visible during its own time window" behavior
 * comes from padding that overlay's OWN sequence with a transparent filler
 * clip outside its active range (see `StudioExporter`), not from alpha
 * animation here.
 */
class PipCompositorSettings(private val overlays: List<OverlayItemEntity>) : VideoCompositorSettings {

    override fun getOutputSize(inputSizes: MutableList<Size>): Size = inputSizes[0]

    override fun getOverlaySettings(inputIndex: Int, presentationTimeUs: Long): OverlaySettings {
        if (inputIndex == 0) return OverlaySettings.Builder().build()
        val overlay = overlays.getOrNull(inputIndex - 1) ?: return OverlaySettings.Builder().build()
        val scale = overlay.sizeScale.toFloat()

        // NDC-style anchors: (-1,-1) bottom-left, (1,1) top-right on both
        // the background frame and the overlay's own frame — matching one
        // anchor point on each aligns the overlay into that screen corner.
        val (bgX, bgY, ovX, ovY) = when (overlay.cornerRawValue) {
            "topLeading" -> Quad(-1f, 1f, -1f, 1f)
            "topTrailing" -> Quad(1f, 1f, 1f, 1f)
            "bottomLeading" -> Quad(-1f, -1f, -1f, -1f)
            else -> Quad(1f, -1f, 1f, -1f) // bottomTrailing default
        }

        return OverlaySettings.Builder()
            .setBackgroundFrameAnchor(bgX, bgY)
            .setOverlayFrameAnchor(ovX, ovY)
            .setScale(scale, scale)
            .build()
    }

    private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)
}
