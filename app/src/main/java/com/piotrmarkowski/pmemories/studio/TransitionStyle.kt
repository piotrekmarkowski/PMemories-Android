package com.piotrmarkowski.pmemories.studio

/**
 * 1:1 rawValue parity with iOS `TransitionStyle` (`MediaItemEntity.
 * transitionStyleRawValue` / `SavedMediaItem.transitionStyleRawValue`) — 10
 * styles, stored on the INCOMING clip (same convention as iOS: the style
 * describes the transition happening as THIS clip enters, `null` = hard cut).
 *
 * iOS implements these as a real simultaneous crossfade (two video layers
 * blended via `AVMutableVideoCompositionLayerInstruction` opacity/transform
 * ramps, both clips visible at once during the overlap). Android's Media3
 * 1.4.1 does NOT support that — the same multi-sequence video compositing
 * investigated for PiP (see `HISTORIA.md` 22.08.2026) was confirmed to not
 * actually blend two video sequences together. Rather than block transitions
 * entirely on that same wall, these are implemented as ENTRANCE-ONLY
 * animations applied to the incoming clip's own first [TRANSITION_DURATION_US]
 * (a time-varying `MatrixTransformation`/`RgbMatrix` on a SINGLE clip, no
 * second video track needed) — still a hard cut underneath, but the incoming
 * clip animates in instead of appearing instantly. Visually close for
 * slide/zoom/rotate styles; `CROSSFADE` specifically is the one style where
 * this is a real approximation (fade-from-black rather than a true dissolve
 * between the outgoing and incoming clip's pixels) — documented here rather
 * than silently claimed identical to iOS.
 */
enum class TransitionStyle(val rawValue: String) {
    CROSSFADE("crossfade"),
    SLIDE("slide"),
    ZOOM("zoom"),
    SLIDE_LEFT("slideLeft"),
    SLIDE_UP("slideUp"),
    SLIDE_DOWN("slideDown"),
    ZOOM_OUT("zoomOut"),
    ROTATE("rotate"),
    SQUEEZE("squeeze"),
    DIAGONAL("diagonal");

    companion object {
        fun fromRaw(raw: String?): TransitionStyle? = entries.firstOrNull { it.rawValue == raw }
    }
}
