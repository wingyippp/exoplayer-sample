package com.example.audioprocessorsample

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

private const val TAG = "NoopAudioProcessor"

@UnstableApi
class NoopAudioProcessor : BaseAudioProcessor() {

    private var enabled: Boolean = false
    private var instancePointer: Long = 0L

    // 加载 native 库
    companion object {
        init {
            System.loadLibrary("audio_processor")
        }
    }

    private external fun onConfigureNative(
        sampleRate: Int,
        channelCount: Int,
        bytesPerFrame: Int,
    ): Long

    private external fun processBufferNative(
        inputBuffer: ByteBuffer,
        sampleRate: Int,
        channelCount: Int,
        bytesPerFrame: Int,
        instancePointer: Long,
    ): ByteBuffer?

    private external fun onResetNative(instancePointer: Long)

    private external fun onFlushNative()

    private external fun onQueueEndOfStreamNative()

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
        if (!inputBuffer.hasRemaining()) {
            return
        }
        val processedBuffer = inputBuffer.takeIf { enabled }
            ?.let {
                processBufferNative(
                    it,
                    sampleRate = inputAudioFormat.sampleRate,
                    channelCount = inputAudioFormat.channelCount,
                    bytesPerFrame = inputAudioFormat.bytesPerFrame,
                    instancePointer = instancePointer,
                )
            }
            ?: inputBuffer
        val remaining = processedBuffer.remaining()
        if (remaining > 0) {
            replaceOutputBuffer(remaining).put(processedBuffer).flip()
        }
    }

    fun isEnabled(): Boolean = this.enabled

    fun setEnable(enable: Boolean) {
        this.enabled = enable
    }
}