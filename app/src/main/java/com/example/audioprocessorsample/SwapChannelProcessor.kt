package com.example.audioprocessorsample

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 把左右声道互换的简单示例
 */
@UnstableApi
class SwapChannelProcessor : AudioProcessor {

    private var inputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var pendingFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var active = false
    private var pendingOutputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        pendingFormat = inputAudioFormat
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            // 只处理 16-bit PCM
            return AudioProcessor.AudioFormat.NOT_SET
        }
        active = inputAudioFormat.channelCount == 2   // 必须是立体声
        return if (active) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun isActive(): Boolean = active

    override fun queueInput(input: ByteBuffer) {
        if (!input.hasRemaining()) return
        val frameCount = input.remaining() / 4   // 16-bit * 2 ch
        val outSize = frameCount * 4
        if (pendingOutputBuffer.capacity() < outSize) {
            pendingOutputBuffer = ByteBuffer.allocateDirect(outSize).order(ByteOrder.nativeOrder())
        }
        pendingOutputBuffer.clear()

        val shortIn = input.asShortBuffer()
        val shortOut = pendingOutputBuffer.asShortBuffer()
        repeat(frameCount) {
            val left = shortIn.get()
            val right = shortIn.get()
            shortOut.put(right)   // 互换
            shortOut.put(left)
        }
        input.position(input.limit())
        pendingOutputBuffer.limit(outSize)
        pendingOutputBuffer.flip()
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = pendingOutputBuffer
        pendingOutputBuffer = EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && pendingOutputBuffer === EMPTY_BUFFER

    override fun flush() {
        pendingOutputBuffer = EMPTY_BUFFER
        inputEnded = false
        outputFormat = pendingFormat
    }

    override fun reset() {
        flush()
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
        outputFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingFormat = AudioProcessor.AudioFormat.NOT_SET
        active = false
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}