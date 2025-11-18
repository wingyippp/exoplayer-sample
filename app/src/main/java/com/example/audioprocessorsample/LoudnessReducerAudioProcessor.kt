package com.example.audioprocessorsample

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

private const val TAG = "LoudnessReducerAudioProcessor"

/**
 * Audio processor that reduces loudness to 50% by applying a 0.5 gain multiplier.
 */
@UnstableApi
class LoudnessReducerAudioProcessor : BaseAudioProcessor() {

    companion object {
        init {
            System.loadLibrary("audio_processor")
        }
    }

    private var enabled: Boolean = false
    private var instancePointer: Long = 0L

    private external fun processPcm(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer, position: Int, limit: Int, encoding: Int, instance: Long)

    private external fun onConfigureNative(
        sampleRate: Int,
        channelCount: Int,
        bytesPerFrame: Int,
    ): Long

    private external fun onResetNative(instancePointer: Long)

    private external fun onFlushNative()

    private external fun onQueueEndOfStreamNative()

    private external fun setParamsNative(
        sampleRate: Int,
        gain: Float,
        frequency: Float,
        qValue: Float,
    )

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        instancePointer = onConfigureNative(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            inputAudioFormat.bytesPerFrame,
        )
        return inputAudioFormat
    }

    override fun onReset() {
        onResetNative(instancePointer)
        super.onReset()
    }

    override fun onQueueEndOfStream() {
        onQueueEndOfStreamNative()
        super.onQueueEndOfStream()
    }

    override fun onFlush() {
        onFlushNative()
        super.onFlush()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive) {
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

        // Set output buffer position to write from the beginning
        outputBuffer.position(0)
        outputBuffer.limit(size)

        // Call native method to process
        processPcm(inputBuffer, outputBuffer, position, limit, inputAudioFormat.encoding, instancePointer)

        // Update input buffer position
        inputBuffer.position(limit)

        // Set output buffer position back to 0 and limit to size for reading
        outputBuffer.position(0)
        outputBuffer.limit(size)
    }

    fun isEnabled(): Boolean = this.enabled

    fun setEnable(enable: Boolean) {
        this.enabled = enable
    }

    fun setParams(
        gain: Float,
        frequency: Float,
        qValue: Float,
    ) {
        setParamsNative(
            inputAudioFormat.sampleRate,
            gain,
            frequency,
            qValue,
        )
    }
}