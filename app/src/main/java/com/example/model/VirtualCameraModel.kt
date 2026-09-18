package com.example.model

import android.net.Uri

/**
 * Camera source selection: Physical Camera hardware or Virtual Video Stream.
 */
enum class CameraSourceMode {
    PHYSICAL_CAMERA,
    VIRTUAL_FEED
}

/**
 * Camera lens facing for physical camera.
 */
enum class CameraLens {
    BACK,
    FRONT
}

/**
 * Aspect ratio display mode for preview rendering.
 */
enum class AspectRatioMode {
    FIT,
    FILL_CROP
}

/**
 * Metadata for a selected video source.
 */
data class VideoMetadata(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

/**
 * UI State for the Virtual Camera Studio.
 */
data class VirtualCameraUiState(
    val sourceMode: CameraSourceMode = CameraSourceMode.VIRTUAL_FEED,
    val selectedLens: CameraLens = CameraLens.BACK,
    val videoMetadata: VideoMetadata? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isLooping: Boolean = true,
    val isMuted: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isVideoLoading: Boolean = false,
    val isCameraPermissionGranted: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showArchitectureDialog: Boolean = false
)
