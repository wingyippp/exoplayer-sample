package com.example.audioprocessorsample

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain

class SingleAudioProcessChain(
    private val audioProcessors: Array<out AudioProcessor>
) : AudioProcessorChain {
    override fun getAudioProcessors(): Array<out AudioProcessor> = audioProcessors

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
        return playbackParameters
    }

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean = false

    override fun getMediaDuration(playoutDuration: Long): Long = playoutDuration

    override fun getSkippedOutputFrameCount(): Long = 0L
}