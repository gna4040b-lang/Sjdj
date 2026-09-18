package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AspectRatioMode
import com.example.model.CameraLens
import com.example.model.CameraSourceMode
import com.example.model.VideoMetadata
import com.example.model.VirtualCameraUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VirtualCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VirtualCameraUiState())
    val uiState: StateFlow<VirtualCameraUiState> = _uiState.asStateFlow()

    fun setSourceMode(mode: CameraSourceMode) {
        _uiState.update { it.copy(sourceMode = mode, errorMessage = null) }
    }

    fun toggleSourceMode() {
        _uiState.update {
            val nextMode = if (it.sourceMode == CameraSourceMode.VIRTUAL_FEED) {
                CameraSourceMode.PHYSICAL_CAMERA
            } else {
                CameraSourceMode.VIRTUAL_FEED
            }
            it.copy(sourceMode = nextMode, errorMessage = null)
        }
    }

    fun toggleLens() {
        _uiState.update {
            val nextLens = if (it.selectedLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
            it.copy(selectedLens = nextLens)
        }
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }
    }

    fun onVideoSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                isVideoLoading = true,
                errorMessage = null,
                sourceMode = CameraSourceMode.VIRTUAL_FEED
            )
        }

        viewModelScope.launch {
            try {
                val metadata = extractVideoMetadata(getApplication<Application>(), uri)
                if (metadata != null) {
                    _uiState.update {
                        it.copy(
                            videoMetadata = metadata,
                            isVideoLoading = false,
                            isPlaying = true,
                            currentPositionMs = 0L,
                            durationMs = metadata.durationMs,
                            errorMessage = null,
                            infoMessage = "Loaded: ${metadata.displayName}"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isVideoLoading = false,
                            errorMessage = "Unable to read video file. Please check file format."
                        )
                    }
                }
            } catch (e: SecurityException) {
                _uiState.update {
                    it.copy(
                        isVideoLoading = false,
                        errorMessage = "Permission denied while accessing selected video."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVideoLoading = false,
                        errorMessage = "Error loading video: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    private suspend fun extractVideoMetadata(context: Context, uri: Uri): VideoMetadata? =
        withContext(Dispatchers.IO) {
            var displayName = "Selected Video"
            var sizeBytes = 0L

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                        if (sizeIndex != -1) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {
                displayName = uri.lastPathSegment ?: "Selected Video"
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

                val duration = durationStr?.toLongOrNull() ?: 0L
                val width = widthStr?.toIntOrNull() ?: 1280
                val height = heightStr?.toIntOrNull() ?: 720

                VideoMetadata(
                    uri = uri,
                    displayName = displayName,
                    durationMs = duration,
                    width = width,
                    height = height,
                    sizeBytes = sizeBytes
                )
            } catch (e: Exception) {
                // If retriever fails, we still return fallback metadata so user can attempt playback
                VideoMetadata(
                    uri = uri,
                    displayName = displayName,
                    durationMs = 0L,
                    width = 1280,
                    height = 720,
                    sizeBytes = sizeBytes
                )
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

    fun togglePlayPause() {
        val currentVideo = _uiState.value.videoMetadata
        if (currentVideo == null) {
            _uiState.update { it.copy(errorMessage = "Please select a video file first.") }
            return
        }
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun play() {
        if (_uiState.value.videoMetadata != null) {
            _uiState.update { it.copy(isPlaying = true) }
        }
    }

    fun pause() {
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun stop() {
        _uiState.update {
            it.copy(
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun seekTo(positionMs: Long) {
        _uiState.update {
            val clamped = positionMs.coerceIn(0L, it.durationMs.coerceAtLeast(0L))
            it.copy(currentPositionMs = clamped)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleAspectRatioMode() {
        _uiState.update {
            val nextMode = if (it.aspectRatioMode == AspectRatioMode.FIT) {
                AspectRatioMode.FILL_CROP
            } else {
                AspectRatioMode.FIT
            }
            it.copy(aspectRatioMode = nextMode)
        }
    }

    fun updatePlaybackProgress(positionMs: Long, durationMs: Long) {
        _uiState.update {
            it.copy(
                currentPositionMs = positionMs,
                durationMs = if (durationMs > 0) durationMs else it.durationMs
            )
        }
    }

    fun onPlaybackError(message: String) {
        _uiState.update {
            it.copy(
                isPlaying = false,
                isVideoLoading = false,
                errorMessage = message
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfo() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun setShowArchitectureDialog(show: Boolean) {
        _uiState.update { it.copy(showArchitectureDialog = show) }
    }
}
