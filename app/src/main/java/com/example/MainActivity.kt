package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CameraLens
import com.example.model.CameraSourceMode
import com.example.ui.components.PhysicalCameraPreview
import com.example.ui.components.PlaybackControlsBar
import com.example.ui.components.StudioTopBar
import com.example.ui.components.SystemArchitectureDialog
import com.example.ui.components.VirtualVideoPreview
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VirtualCameraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VirtualCameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                VirtualCameraScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Synchronize camera permission state
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermissionGranted(hasPermission)
    }
}

@Composable
fun VirtualCameraScreen(
    viewModel: VirtualCameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Media Picker for selecting video feed
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onVideoSelected(uri)
        }
    }

    // Fallback file picker for broader Android versions/documents
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onVideoSelected(uri)
        }
    }

    fun launchVideoPicker() {
        try {
            mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        } catch (_: Exception) {
            documentPickerLauncher.launch("video/*")
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermissionGranted(isGranted)
        if (!isGranted) {
            viewModel.onPlaybackError("Camera permission denied. Physical camera stream unavailable.")
        }
    }

    // Check initial permission
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermissionGranted(granted)
    }

    // Display error messages via Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Display info messages via Snackbar
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { info ->
            snackbarHostState.showSnackbar(info)
            viewModel.clearInfo()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_camera_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StudioTopBar(
                sourceMode = uiState.sourceMode,
                selectedLens = uiState.selectedLens,
                isPlaying = uiState.isPlaying,
                onToggleMode = { mode -> viewModel.setSourceMode(mode) },
                onToggleLens = { viewModel.toggleLens() },
                onShowInfo = { viewModel.setShowArchitectureDialog(true) }
            )
        },
        containerColor = Color(0xFF0B132B)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                when (uiState.sourceMode) {
                    CameraSourceMode.PHYSICAL_CAMERA -> {
                        PhysicalCameraPreview(
                            isPermissionGranted = uiState.isCameraPermissionGranted,
                            selectedLens = uiState.selectedLens,
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    CameraSourceMode.VIRTUAL_FEED -> {
                        VirtualVideoPreview(
                            videoMetadata = uiState.videoMetadata,
                            isPlaying = uiState.isPlaying,
                            isLooping = uiState.isLooping,
                            isMuted = uiState.isMuted,
                            playbackSpeed = uiState.playbackSpeed,
                            aspectRatioMode = uiState.aspectRatioMode,
                            currentPositionMs = uiState.currentPositionMs,
                            isLoading = uiState.isVideoLoading,
                            onProgressUpdate = { current, duration ->
                                viewModel.updatePlaybackProgress(current, duration)
                            },
                            onError = { error ->
                                viewModel.onPlaybackError(error)
                            },
                            onSelectVideo = { launchVideoPicker() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Status Overlay Badge (Top Left of viewfinder)
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = if (uiState.isPlaying || uiState.sourceMode == CameraSourceMode.PHYSICAL_CAMERA) {
                                Color(0xFFFF3B30)
                            } else {
                                Color.Gray
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.sourceMode == CameraSourceMode.PHYSICAL_CAMERA) {
                                "LIVE: CAM ${uiState.selectedLens.name}"
                            } else {
                                "FEED: ${uiState.videoMetadata?.displayName?.take(18) ?: "NO VIDEO"}"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Control Deck
            when (uiState.sourceMode) {
                CameraSourceMode.VIRTUAL_FEED -> {
                    PlaybackControlsBar(
                        isPlaying = uiState.isPlaying,
                        currentPositionMs = uiState.currentPositionMs,
                        durationMs = uiState.durationMs,
                        isLooping = uiState.isLooping,
                        isMuted = uiState.isMuted,
                        playbackSpeed = uiState.playbackSpeed,
                        aspectRatioMode = uiState.aspectRatioMode,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onStop = { viewModel.stop() },
                        onToggleLoop = { viewModel.toggleLoop() },
                        onToggleMute = { viewModel.toggleMute() },
                        onSeekTo = { pos -> viewModel.seekTo(pos) },
                        onCycleSpeed = {
                            val nextSpeed = when (uiState.playbackSpeed) {
                                0.5f -> 1.0f
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 0.5f
                            }
                            viewModel.setPlaybackSpeed(nextSpeed)
                        },
                        onToggleAspectRatio = { viewModel.toggleAspectRatioMode() },
                        onSelectNewVideo = { launchVideoPicker() }
                    )
                }
                CameraSourceMode.PHYSICAL_CAMERA -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.toggleLens() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("physical_switch_lens_button")
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Switch to ${if (uiState.selectedLens == CameraLens.BACK) "Front" else "Back"} Camera")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { viewModel.setSourceMode(CameraSourceMode.VIRTUAL_FEED) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("switch_to_virtual_feed_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Virtual Feed")
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showArchitectureDialog) {
        SystemArchitectureDialog(
            onDismiss = { viewModel.setShowArchitectureDialog(false) }
        )
    }
}

/**
 * Kept for UI test compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
