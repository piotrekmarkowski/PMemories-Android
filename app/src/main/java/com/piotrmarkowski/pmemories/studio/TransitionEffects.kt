package com.piotrmarkowski.pmemories.studio

import android.graphics.Matrix
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix

/** 0.4s — same value as iOS `VideoComposer.transitionDuration`. */
const val TRANSITION_DURATION_US: Long = 400_000L

/**
 * Entrance animation for an incoming clip — see `TransitionStyle` doc for
 * why this is entrance-only (no real second-clip blend). `t` ramps 0→1 over
 * the clip's first [transitionDurationUs] of LOCAL playback time (Media3
 * calls effects with each `EditedMediaItem`'s own timeline, starting near
 * zero, regardless of its position in the final sequence), then stays at
 * identity — cheap, side-effect-free, called every frame.
 *
 * Normalized (-1..1) GL space, same convention Media3's own
 * `ScaleAndRotateTransformation` uses — a translate of 2f moves a full
 * screen-width off-screen, rotate/scale pivot around the origin which is
 * screen-center in this space.
 */
class TransitionEntranceMatrix(
    private val style: TransitionStyle,
    // 25.08.2026 — REALNY bug, złapany dopiero przez pixel-diff dwóch
    // wyeksportowanych klatek (identyczne, zero różnicy): Media3 przekazuje
    // do `getMatrix`/`getMatrix` CZAS GLOBALNY danej kompozycji/sekwencji,
    // NIE lokalny czas liczony od zera dla każdego `EditedMediaItem` z
    // osobna (błędne założenie w pierwszej wersji tego pliku). Bez tego
    // offsetu `t` dla klipu #2 (zaczynającego się np. w 3.0s globalnego
    // czasu) natychmiast wychodził >1 i efekt był przycięty do "już
    // gotowe" od pierwszej klatki — wyglądało jak cichy fail identyczny z
    // kompozytorem PiP, dopóki nie porównano klatek pixel-po-pikselu.
    private val itemStartTimeUs: Long,
    private val transitionDurationUs: Long
) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val t = ((presentationTimeUs - itemStartTimeUs).toDouble() / transitionDurationUs).coerceIn(0.0, 1.0).toFloat()
        val matrix = Matrix()
        when (style) {
            TransitionStyle.SLIDE -> matrix.postTranslate(-2f * (1f - t), 0f)
            TransitionStyle.SLIDE_LEFT -> matrix.postTranslate(2f * (1f - t), 0f)
            TransitionStyle.SLIDE_UP -> matrix.postTranslate(0f, -2f * (1f - t))
            TransitionStyle.SLIDE_DOWN -> matrix.postTranslate(0f, 2f * (1f - t))
            TransitionStyle.DIAGONAL -> matrix.postTranslate(2f * (1f - t), 2f * (1f - t))
            TransitionStyle.ZOOM -> {
                val scale = 0.4f + 0.6f * t
                matrix.postScale(scale, scale)
            }
            TransitionStyle.ZOOM_OUT -> {
                val scale = 1.6f - 0.6f * t
                matrix.postScale(scale, scale)
            }
            TransitionStyle.SQUEEZE -> matrix.postScale(t.coerceAtLeast(0.01f), 1f)
            TransitionStyle.ROTATE -> matrix.postRotate(-25f * (1f - t))
            // Handled by TransitionEntranceFade (RgbMatrix), not geometry.
            TransitionStyle.CROSSFADE -> Unit
        }
        return matrix
    }
}

/**
 * `CROSSFADE`'s entrance effect — fades the incoming clip up from black
 * over its first [transitionDurationUs], since a real dissolve against the
 * OUTGOING clip's pixels isn't available (see `TransitionStyle` doc).
 * Same column-major 4x4 RGBA-scale convention as `ColorStyle.toRgbMatrix`.
 */
class TransitionEntranceFade(
    private val itemStartTimeUs: Long,
    private val transitionDurationUs: Long
) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val t = ((presentationTimeUs - itemStartTimeUs).toDouble() / transitionDurationUs).coerceIn(0.0, 1.0).toFloat()
        return floatArrayOf(
            t, 0f, 0f, 0f,
            0f, t, 0f, 0f,
            0f, 0f, t, 0f,
            0f, 0f, 0f, 1f
        )
    }
}
