package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioMode
import java.util.Locale

@Composable
fun PlaybackControlsBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isLooping: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    aspectRatioMode: AspectRatioMode,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleMute: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onSelectNewVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val currentSliderValue = if (isDraggingSlider) {
        dragSliderValue
    } else {
        (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = Color(0xFF1E293B).copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Seek bar with time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(if (isDraggingSlider) (dragSliderValue * safeDuration).toLong() else currentPositionMs),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = currentSliderValue,
                    onValueChange = { newValue ->
                        isDraggingSlider = true
                        dragSliderValue = newValue
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo((dragSliderValue * safeDuration).toLong())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("playback_seek_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Text(
                    text = formatDuration(durationMs),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Change Video Button
                IconButton(
                    onClick = onSelectNewVideo,
                    modifier = Modifier.testTag("change_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Select New Video",
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Loop Toggle
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.testTag("toggle_loop_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isLooping) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = if (isLooping) "Looping On" else "Looping Off",
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.testTag("stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Primary FAB
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("play_pause_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Mute / Unmute Toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("toggle_mute_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isMuted) Color.Red.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color(0xFFFF5252) else Color.White.copy(alpha = 0.85f)
                    )
                }

                // Speed Selector Chip
                IconButton(
                    onClick = onCycleSpeed,
                    modifier = Modifier.testTag("cycle_speed_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Playback Speed",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${playbackSpeed}x",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Aspect Ratio (Fit / Crop) Toggle
                IconButton(
                    onClick = onToggleAspectRatio,
                    modifier = Modifier.testTag("toggle_aspect_ratio_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (aspectRatioMode == AspectRatioMode.FILL_CROP) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Toggle Aspect Ratio (${aspectRatioMode.name})",
                        tint = if (aspectRatioMode == AspectRatioMode.FILL_CROP) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
