package com.example.audioprocessorsample

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@UnstableApi
class NoopAudioProcessor : BaseAudioProcessor() {

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
    )

    private external fun processBufferNative(
        inputBuffer: ByteBuffer,
        sampleRate: Int,
        channelCount: Int,
        bytesPerFrame: Int,
    ): ByteBuffer

    private external fun onResetNative()

    private external fun onFlushNative()

    private external fun onQueueEndOfStreamNative()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        onConfigureNative(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            inputAudioFormat.bytesPerFrame,
        )
        return super.onConfigure(inputAudioFormat)
    }

    override fun onReset() {
        onResetNative()
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

        // 调用 native 方法处理
        val processedBuffer = processBufferNative(
            inputBuffer,
            sampleRate = inputAudioFormat.sampleRate,
            channelCount = inputAudioFormat.channelCount,
            bytesPerFrame = inputAudioFormat.bytesPerFrame
        )

        val remaining = processedBuffer.remaining()
        replaceOutputBuffer(remaining).put(processedBuffer).flip()
    }
}