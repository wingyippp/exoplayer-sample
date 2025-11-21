package com.example.audioprocessorsample

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "LoudnessReducerAudioProcessor"
private const val DEFAULT_ENABLE = false

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

    private var enabled: Boolean = DEFAULT_ENABLE
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
        instance: Long
    )

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        instancePointer = onConfigureNative(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            inputAudioFormat.bytesPerFrame,
        )
        // If input is PCM_16BIT, output as PCM_FLOAT
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            AudioProcessor.AudioFormat(
                inputAudioFormat.sampleRate,
                inputAudioFormat.channelCount,
                C.ENCODING_PCM_FLOAT  // Change encoding
            )
        } else {
            inputAudioFormat
        }
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
        }

        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val inputSize = limit - position

        // Calculate output size based on format conversion
        val outputSize = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> inputSize * 2  // int16 (2 bytes) -> float (4 bytes)
            else -> inputSize
        }

        // Allocate output buffer
        val outputBuffer = replaceOutputBuffer(outputSize)

        // Set output buffer position to write from the beginning
        outputBuffer.position(0)
        outputBuffer.limit(outputSize)

        if (isEnabled()) {
            // Call native method to process
            processPcm(inputBuffer, outputBuffer, position, limit, inputAudioFormat.encoding, instancePointer)
        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            // Convert PCM_16BIT to PCM_FLOAT
            convertPcm16BitToFloat(inputBuffer, outputBuffer, position, limit)
        } else {
            // Processor disabled and no conversion needed: pass through
            // Input and output are same format (e.g., both PCM_FLOAT)
            inputBuffer.position(position)
            inputBuffer.limit(limit)
            outputBuffer.put(inputBuffer)
            inputBuffer.limit(inputBuffer.capacity())  // Restore original limit
        }

        // Update input buffer position
        inputBuffer.position(limit)

        // Set output buffer position back to 0 and limit to size for reading
        outputBuffer.position(0)
        outputBuffer.limit(outputSize)
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
            instancePointer
        )
    }

    private fun convertPcm16BitToFloat(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        position: Int,
        limit: Int
    ) {
        // Set input buffer to little-endian (standard for PCM)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // Save original position
        val originalPosition = inputBuffer.position()

        // Set position for reading
        inputBuffer.position(position)

        // Convert each 16-bit sample to float
        while (inputBuffer.position() < limit) {
            // Read int16 sample
            val sample = inputBuffer.short

            // Convert to float in range [-1.0, 1.0]
            val floatSample = sample.toFloat() / 32768.0f

            // Write float sample
            outputBuffer.putFloat(floatSample)
        }

        // Restore input buffer position
        inputBuffer.position(originalPosition)
    }
}