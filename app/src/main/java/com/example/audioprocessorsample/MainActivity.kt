package com.example.audioprocessorsample

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.PlayerView
import com.example.audioprocessorsample.ui.theme.AudioProcessorSampleTheme
import java.io.File

@ExperimentalMaterial3Api
@UnstableApi
class MainActivity : ComponentActivity() {

    private val samplesPath by lazy { File(application.filesDir, "samples") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        copyAssets()
        val audioProcessor = LoudnessReducerAudioProcessor()
        val factory = object : DefaultRenderersFactory(this) {

            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                val audioSinkBuilder = DefaultAudioSink.Builder(this@MainActivity)
                audioSinkBuilder.setAudioProcessors(listOf(audioProcessor).toTypedArray())
                return audioSinkBuilder.build()
            }
        }
        setContent {
            AudioProcessorSampleTheme {
                ExoPlayerScreen(
                    Uri.fromFile(samplesPath.listFiles()!!.first()),
                    factory,
                    audioProcessor,
                )
            }
        }
    }

    private fun copyAssets() {
        samplesPath.delete()
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

/**
 * Main composable function that hosts the ExoPlayer UI.
 *
 * This component manages the lifecycle of the ExoPlayer instance and uses
 * AndroidView to bridge the traditional PlayerView (a standard Android View)
 * into the Jetpack Compose hierarchy.
 */
@ExperimentalMaterial3Api
@UnstableApi
@Composable
fun ExoPlayerScreen(
    uri: Uri,
    factory: DefaultRenderersFactory,
    audioProcessor: LoudnessReducerAudioProcessor,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Initialize ExoPlayer using remember
    // remember ensures the ExoPlayer instance is created only once
    // and retained across recompositions.
    val exoPlayer = remember {
        ExoPlayer.Builder(context,factory).build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true // Start playing immediately
        }
    }

    // State to track if the player is currently playing
    val isPlayingState = remember { mutableStateOf(true) }
    val isPlaying = isPlayingState.value
    val isAudioProcessorEnabledState = remember { mutableStateOf(false) }
    val isAudioProcessorEnabled = isAudioProcessorEnabledState.value

    // 2. Manage ExoPlayer lifecycle with DisposableEffect
    // This releases the player resources when the composable leaves the screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Pause when the app goes into the background
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                // Resume when the app comes back to the foreground
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                // Release player resources when the Composable is disposed (screen closed)
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> Unit
            }
        }
        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup: remove the observer and release the player when the effect leaves composition
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ExoPlayer Compose Demo") }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 3. Use AndroidView to host the PlayerView
                // PlayerView is the standard Android UI component for ExoPlayer
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f), // Standard video aspect ratio
                    factory = {
                        // Create the PlayerView
                        PlayerView(context).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        // Optional: Update logic if the player or controls need to change
                        view.player = exoPlayer
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Custom Compose Controls (Play/Pause Button)
                Button(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlayingState.value = false
                        } else {
                            exoPlayer.play()
                            isPlayingState.value = true
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(0.6f)
                ) {
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 5. Enable/Disable AudioProcessor
                Button(
                    onClick = {
                        val toUpdate = !audioProcessor.isEnabled()
                        audioProcessor.setEnable(toUpdate)
                        isAudioProcessorEnabledState.value = toUpdate
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(0.6f)
                ) {
                    Text(
                        text = if (isAudioProcessorEnabled) "To Disable AudioProcessor" else "To Enable AudioProcessor",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    )
}