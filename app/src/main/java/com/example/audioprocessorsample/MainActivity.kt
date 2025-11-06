package com.example.audioprocessorsample

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.audioprocessorsample.ui.theme.AudioProcessorSampleTheme
import java.io.File

@UnstableApi
class MainActivity : ComponentActivity() {

    private val samplesPath by lazy { File(application.filesDir, "samples") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioProcessorSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        copyAssets()
        val factory = object : DefaultRenderersFactory(this) {

            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                val audioSinkBuilder = DefaultAudioSink.Builder(this@MainActivity)
                audioSinkBuilder.setAudioProcessors(listOf(NoopAudioProcessor()).toTypedArray())
                return audioSinkBuilder.build()
            }
        }
        val player = ExoPlayer.Builder(this, factory).build()
        samplesPath.listFiles()?.firstOrNull()
            ?.let { MediaItem.fromUri(Uri.fromFile(it)) }
            ?.let {
                player.addMediaItem(it)
                player.prepare()
                player.playWhenReady = true
            }
    }

    private fun copyAssets() {
        samplesPath.mkdirs()
        application.copyData(assetDirName = "samples", destDir = samplesPath)
    }

    private fun Context.copyData(assetDirName: String, destDir: File) {
        assets.list(assetDirName)?.forEach { name ->
            val assetPath = "$assetDirName/$name"
            val destination = File(destDir, name)
            assets.open(assetPath).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AudioProcessorSampleTheme {
        Greeting("Android")
    }
}