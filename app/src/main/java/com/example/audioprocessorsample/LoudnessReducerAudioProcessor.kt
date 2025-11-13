package com.example.audioprocessorsample

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min


/**
 * Audio processor that reduces loudness to 50% by applying a 0.5 gain multiplier.
 */
@UnstableApi
class LoudnessReducerAudioProcessor : BaseAudioProcessor() {

    companion object {
        private const val GAIN = 0.5f
        init {
            System.loadLibrary("audio_processor")
        }
    }

    private var enabled: Boolean = false

    private external fun processPcm(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer, position: Int, limit: Int, encoding: Int)


    @Throws(UnhandledAudioFormatException::class)
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Only process PCM encodings
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT
            && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }

        // Return the same format as input (we're not changing format, just values)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            return
        } else if (!isEnabled()) {
            val remaining = inputBuffer.remaining()
            if (remaining > 0) {
                replaceOutputBuffer(remaining).put(inputBuffer).flip()
            }
            return
        }

        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position

        // Allocate output buffer
        val outputBuffer = replaceOutputBuffer(size)

        // Process based on encoding type
        processPcm(inputBuffer, outputBuffer, position, limit, inputAudioFormat.encoding)

//        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
//            processPcm16Bit(inputBuffer, outputBuffer, position, limit)
//        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
//            processPcmFloat(inputBuffer, outputBuffer, position, limit)
//        }

        inputBuffer.position(limit)
        outputBuffer.flip()
    }

    /**
     * Process 16-bit PCM audio data.
     */
    private fun processPcm16Bit(input: ByteBuffer, output: ByteBuffer, position: Int, limit: Int) {
        // Process each 16-bit sample (2 bytes per sample)
        var i = position
        while (i < limit) {
            // Read 16-bit signed integer sample
            val sample = input.getShort(i)


            // Apply gain (reduce to 50%)
            var processedSample = (sample * GAIN).toInt()


            // Clamp to 16-bit range (shouldn't be necessary with 0.5 gain, but good practice)
            processedSample = max(
                Short.Companion.MIN_VALUE.toInt(),
                min(Short.Companion.MAX_VALUE.toInt(), processedSample)
            )


            // Write processed sample
            output.putShort(processedSample.toShort())
            i += 2
        }
    }

    /**
     * Process 32-bit float PCM audio data.
     */
    private fun processPcmFloat(input: ByteBuffer, output: ByteBuffer, position: Int, limit: Int) {
        // Process each 32-bit float sample (4 bytes per sample)
        var i = position
        while (i < limit) {
            // Read float sample (typically in range [-1.0, 1.0])
            val sample = input.getFloat(i)


            // Apply gain (reduce to 50%)
            var processedSample = sample * GAIN


            // Clamp to valid float range (shouldn't be necessary with 0.5 gain, but good practice)
            processedSample = max(-1.0f, min(1.0f, processedSample))


            // Write processed sample
            output.putFloat(processedSample)
            i += 4
        }
    }

    override fun onFlush() {
        // No internal state to flush
    }

    override fun onReset() {
        // No internal state to reset
    }

    fun isEnabled(): Boolean = this.enabled

    fun setEnable(enable: Boolean) {
        this.enabled = enable
    }
}