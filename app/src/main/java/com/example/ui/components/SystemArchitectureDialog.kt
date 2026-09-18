package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SystemArchitectureDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Android Virtual Camera Architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "System-Wide Virtual Camera Reality on Android",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "On standard unrooted Android, third-party apps (such as WhatsApp, Instagram, Google Meet, Zoom) query the Camera2/CameraX APIs which connect directly to the Android OS 'cameraserver' and hardware Camera HAL (Hardware Abstraction Layer).\n\nAndroid's Linux-level sandbox isolation, SELinux enforcement, and driver permissions strictly prevent normal applications from intercepting or spoofing another app's camera sessions. Any app claiming 'system-wide virtual camera' on unrooted stock Android without custom ROM or root/Xposed hooking is inaccurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Legitimate Solutions Provided in This App",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Virtual Video Feed Player: Hardware-accelerated video rendering engine with full playback controls (Play, Pause, Stop, Seek, Loop, Mute, Playback Speed, and Fit/Crop Aspect Ratio).\n\n2. Real-Time Camera Studio: Live Physical Camera integration via CameraX with lens switching (Front/Back).\n\n3. System Camera Intent Responder: When other Android apps request a photo or video via standard MediaStore Intents (ACTION_IMAGE_CAPTURE / ACTION_VIDEO_CAPTURE), this app can act as the system camera provider to return a snapshot or clip from your selected virtual video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_architecture_dialog_button")
            ) {
                Text("Understood")
            }
        }
    )
}
