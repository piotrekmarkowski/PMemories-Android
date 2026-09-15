package com.piotrmarkowski.pmemories.studio

import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix

/**
 * Project-wide color filter — 1:1 with iOS `ColorStyle` (`SavedProject.colorStyleRaw`).
 * Applied uniformly to every clip's video effects at export time, not a
 * genuine second encoding pass like on iOS (AVFoundation-specific detail) —
 * same visual result, simpler pipeline.
 */
enum class ColorStyle(val rawValue: String) {
    NONE("none"),
    VINTAGE("vintage"),
    BLACK_AND_WHITE("blackAndWhite"),
    VIBRANT("vibrant"),
    CINEMATIC("cinematic"),
    WARM("warm");

    companion object {
        fun fromRaw(raw: String?): ColorStyle = entries.firstOrNull { it.rawValue == raw } ?: NONE
    }
}

/** Fixed color-transform matrix, same value regardless of timestamp/HDR. */
private class StaticColorMatrix(private val matrix: FloatArray) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
}

fun ColorStyle.toRgbMatrix(): RgbMatrix? = when (this) {
    ColorStyle.NONE -> null
    ColorStyle.BLACK_AND_WHITE -> RgbFilter.createGrayscaleFilter()
    // Column-major 4x4 matrices (Media3 RgbMatrix convention), applied to (r,g,b,1).
    ColorStyle.VINTAGE -> StaticColorMatrix(
        floatArrayOf(
            0.6f, 0.35f, 0.25f, 0f,
            0.55f, 0.5f, 0.3f, 0f,
            0.4f, 0.3f, 0.35f, 0f,
            0f, 0f, 0f, 1f
        )
    )
    ColorStyle.VIBRANT -> StaticColorMatrix(
        floatArrayOf(
            1.3f, -0.15f, -0.15f, 0f,
            -0.15f, 1.3f, -0.15f, 0f,
            -0.15f, -0.15f, 1.3f, 0f,
            0f, 0f, 0f, 1f
        )
    )
    ColorStyle.CINEMATIC -> StaticColorMatrix(
        floatArrayOf(
            0.9f, 0.05f, 0.05f, 0f,
            0.05f, 0.85f, 0.05f, 0f,
            0.1f, 0.1f, 1.05f, 0f,
            0f, 0f, 0f, 1f
        )
    )
    ColorStyle.WARM -> StaticColorMatrix(
        floatArrayOf(
            1.15f, 0f, 0f, 0f,
            0f, 1.05f, 0f, 0f,
            0f, 0f, 0.85f, 0f,
            0f, 0f, 0f, 1f
        )
    )
}
