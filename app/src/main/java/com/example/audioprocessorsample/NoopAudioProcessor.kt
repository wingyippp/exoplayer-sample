package com.example.audioprocessorsample

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

    // 声明 native 方法
    private external fun processBufferNative(
        inputBuffer: ByteBuffer,
        sampleRate: Int,
        channelCount: Int,
        bytesPerFrame: Int,
    ): ByteBuffer

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