package com.example.audioprocessorsample

import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@UnstableApi
class NoopAudioProcessor : BaseAudioProcessor() {
    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        replaceOutputBuffer(remaining).put(inputBuffer).flip()
    }
}