package com.piotrmarkowski.pmemories.studio

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Media3 Transformer has no built-in "just multiply volume" processor —
 * this fills the gap for `SavedMediaItem.originalVolume` /
 * `SavedProject.musicVolume` mixing (16-bit PCM only, which is what the
 * Transformer decode pipeline produces).
 */
class GainAudioProcessor(@Volatile var gain: Float = 1f) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        inputAudioFormat

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        val outputBuffer = replaceOutputBuffer(remaining)
        val input = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val output = outputBuffer.asShortBuffer()
        while (input.hasRemaining()) {
            val scaled = (input.get() * gain).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.put(scaled.toShort())
        }
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.position(remaining)
        outputBuffer.flip()
    }
}
