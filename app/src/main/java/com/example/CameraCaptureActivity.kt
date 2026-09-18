package com.example

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

class CameraCaptureActivity : ComponentActivity() {

    private var outputUri: Uri? = null
    private var isVideoAction: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val action = intent?.action
        isVideoAction = action == MediaStore.ACTION_VIDEO_CAPTURE
        outputUri = intent?.getParcelableExtra(MediaStore.EXTRA_OUTPUT)

        setContent {
            MyApplicationTheme {
                CameraCaptureScreen(
                    isVideoAction = isVideoAction,
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onConfirmCapture = { selectedUri, previewBitmap ->
                        confirmResult(selectedUri, previewBitmap)
                    }
                )
            }
        }
    }

    private fun confirmResult(selectedUri: Uri?, previewBitmap: Bitmap?) {
        val resultIntent = Intent()
        val targetUri = outputUri

        try {
            if (isVideoAction) {
                if (selectedUri != null && targetUri != null) {
                    contentResolver.openInputStream(selectedUri)?.use { input ->
                        contentResolver.openOutputStream(targetUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    resultIntent.data = targetUri
                } else if (selectedUri != null) {
                    resultIntent.data = selectedUri
                }
            } else {
                // Image action
                if (targetUri != null && previewBitmap != null) {
                    contentResolver.openOutputStream(targetUri)?.use { out: OutputStream ->
                        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    resultIntent.data = targetUri
                } else if (previewBitmap != null) {
                    resultIntent.putExtra("data", previewBitmap)
                }
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export capture: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun CameraCaptureScreen(
    isVideoAction: Boolean,
    onCancel: () -> Unit,
    onConfirmCapture: (Uri?, Bitmap?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var extractedFrame by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val videoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            coroutineScope.launch {
                statusMessage = "Extracting snapshot frame..."
                val frame = extractFrame(context, uri)
                extractedFrame = frame
                statusMessage = if (frame != null) "Frame ready for capture" else "Loaded video"
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isVideoAction) "Virtual Video Camera Capture" else "Virtual Photo Camera Capture",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Requested by another application via Android MediaStore Intent",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                val frame = extractedFrame
                if (frame != null) {
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = "Virtual Camera Snapshot",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedUri != null) "Video selected" else "Select video source for capture",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            modifier = Modifier.testTag("intent_picker_select_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Video")
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("intent_cancel_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onConfirmCapture(selectedUri, extractedFrame)
                    },
                    enabled = selectedUri != null || extractedFrame != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("intent_confirm_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send Capture")
                }
            }
        }
    }
}

private suspend fun extractFrame(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        retriever.getFrameAtTime(1000000) // At 1.0 second
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {}
    }
}
