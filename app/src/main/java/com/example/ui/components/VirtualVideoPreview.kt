package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AspectRatioMode
import com.example.model.VideoMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VirtualVideoPreview(
    videoMetadata: VideoMetadata?,
    isPlaying: Boolean,
    isLooping: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    aspectRatioMode: AspectRatioMode,
    currentPositionMs: Long,
    isLoading: Boolean,
    onProgressUpdate: (Long, Long) -> Unit,
    onError: (String) -> Unit,
    onSelectVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceTexture by remember { mutableStateOf<SurfaceTexture?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableIntStateOf(1280) }
    var videoHeight by remember { mutableIntStateOf(720) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentIsLooping by rememberUpdatedState(isLooping)
    val currentIsMuted by rememberUpdatedState(isMuted)
    val currentSpeed by rememberUpdatedState(playbackSpeed)
    val currentAspectMode by rememberUpdatedState(aspectRatioMode)

    // Helper to adjust TextureView scale matrix for FIT vs FILL_CROP
    fun updateTextureMatrix(textureView: TextureView, vWidth: Int, vHeight: Int, mode: AspectRatioMode) {
        val viewWidth = textureView.width.toFloat()
        val viewHeight = textureView.height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0 || vWidth <= 0 || vHeight <= 0) return

        val matrix = Matrix()
        val sx: Float
        val sy: Float

        val videoRatio = vWidth.toFloat() / vHeight.toFloat()
        val viewRatio = viewWidth / viewHeight

        if (mode == AspectRatioMode.FILL_CROP) {
            if (videoRatio > viewRatio) {
                sx = videoRatio / viewRatio
                sy = 1.0f
            } else {
                sx = 1.0f
                sy = viewRatio / videoRatio
            }
        } else {
            // FIT
            if (videoRatio > viewRatio) {
                sx = 1.0f
                sy = (viewWidth / videoRatio) / viewHeight
            } else {
                sx = (viewHeight * videoRatio) / viewWidth
                sy = 1.0f
            }
        }

        matrix.setScale(sx, sy, viewWidth / 2f, viewHeight / 2f)
        textureView.setTransform(matrix)
    }

    // Lifecycle observer to pause/resume playback appropriately
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    try {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.pause()
                        }
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        if (currentIsPlaying && isPrepared) {
                            mediaPlayer?.start()
                        }
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            isPrepared = false
        }
    }

    // Initialize or re-create MediaPlayer when video Uri or SurfaceTexture changes
    LaunchedEffect(videoMetadata?.uri, surfaceTexture) {
        val uri = videoMetadata?.uri
        val surfaceTex = surfaceTexture
        if (uri == null || surfaceTex == null) {
            isPrepared = false
            return@LaunchedEffect
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}

        isPrepared = false
        val player = MediaPlayer().apply {
            setSurface(Surface(surfaceTex))
            try {
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    videoWidth = mp.videoWidth.takeIf { it > 0 } ?: 1280
                    videoHeight = mp.videoHeight.takeIf { it > 0 } ?: 720
                    mp.isLooping = currentIsLooping
                    val vol = if (currentIsMuted) 0.0f else 1.0f
                    mp.setVolume(vol, vol)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            mp.playbackParams = PlaybackParams().apply { speed = currentSpeed }
                        } catch (_: Exception) {}
                    }

                    if (currentIsPlaying) {
                        mp.start()
                    }
                    onProgressUpdate(mp.currentPosition.toLong(), mp.duration.toLong())

                    textureViewRef?.let { tv ->
                        updateTextureMatrix(tv, videoWidth, videoHeight, currentAspectMode)
                    }
                }
                setOnCompletionListener {
                    if (!currentIsLooping) {
                        onProgressUpdate(duration.toLong(), duration.toLong())
                    }
                }
                setOnErrorListener { _, what, extra ->
                    onError("Playback error (code: $what, extra: $extra)")
                    true
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        videoWidth = width
                        videoHeight = height
                        textureViewRef?.let { tv ->
                            updateTextureMatrix(tv, width, height, currentAspectMode)
                        }
                    }
                }
                prepareAsync()
            } catch (e: Exception) {
                onError("Failed to load video: ${e.localizedMessage}")
            }
        }
        mediaPlayer = player
    }

    // Synchronize play / pause
    LaunchedEffect(isPlaying, isPrepared) {
        val player = mediaPlayer ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        try {
            if (isPlaying && !player.isPlaying) {
                player.start()
            } else if (!isPlaying && player.isPlaying) {
                player.pause()
            }
        } catch (_: Exception) {}
    }

    // Synchronize looping
    LaunchedEffect(isLooping, isPrepared) {
        try {
            mediaPlayer?.isLooping = isLooping
        } catch (_: Exception) {}
    }

    // Synchronize mute
    LaunchedEffect(isMuted, isPrepared) {
        try {
            val vol = if (isMuted) 0.0f else 1.0f
            mediaPlayer?.setVolume(vol, vol)
        } catch (_: Exception) {}
    }

    // Synchronize playback speed
    LaunchedEffect(playbackSpeed, isPrepared) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (isPrepared) {
                        val isCurrentlyPlaying = mp.isPlaying
                        mp.playbackParams = mp.playbackParams.apply { speed = playbackSpeed }
                        if (!isCurrentlyPlaying && mp.isPlaying) {
                            mp.pause()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Synchronize aspect ratio mode changes
    LaunchedEffect(aspectRatioMode, videoWidth, videoHeight) {
        textureViewRef?.let { tv ->
            updateTextureMatrix(tv, videoWidth, videoHeight, aspectRatioMode)
        }
    }

    // Poll position smoothly during active playback
    LaunchedEffect(isPlaying, isPrepared) {
        while (isActive && isPlaying && isPrepared) {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        onProgressUpdate(mp.currentPosition.toLong(), mp.duration.toLong())
                    }
                } catch (_: Exception) {}
            }
            delay(150)
        }
    }

    // Main layout
    if (videoMetadata == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = "Select Video Feed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Virtual Video Feed Source",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select an MP4, MKV, or WebM video file from device storage to use as your simulated camera feed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSelectVideo,
                    modifier = Modifier.testTag("select_video_button")
                ) {
                    Text("Select Video File")
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("virtual_video_texture_view"),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                surfaceTexture = st
                                textureViewRef = this@apply
                                updateTextureMatrix(this@apply, videoWidth, videoHeight, currentAspectMode)
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                                updateTextureMatrix(this@apply, videoWidth, videoHeight, currentAspectMode)
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                surfaceTexture = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                },
                update = { tv ->
                    textureViewRef = tv
                    updateTextureMatrix(tv, videoWidth, videoHeight, currentAspectMode)
                }
            )

            if (isLoading || !isPrepared) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
