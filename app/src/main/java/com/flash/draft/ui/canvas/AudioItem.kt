package com.flash.draft.ui.canvas

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.CanvasElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun AudioItem(
    audioNote: CanvasElement.AudioNote, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.AudioNote) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit, onDuplicate: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackPosition by remember { mutableStateOf(0f) }
    var totalDuration by remember { mutableStateOf(audioNote.durationMs.toFloat()) }

    var recordingSeconds by remember { mutableStateOf(0) }

    // Cleanup resources on disposal
    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
        }
    }

    // Playback progression ticker loop
    LaunchedEffect(isPlaying) {
        if (isPlaying && player != null) {
            while (player!!.isPlaying) {
                currentPlaybackPosition = player!!.currentPosition.toFloat()
                delay(100)
            }
            isPlaying = false
            currentPlaybackPosition = 0f
        }
    }

    fun startRecording() {
        // 1. Create the file FIRST (using .m4a for the new high-quality MPEG-4 format)
        val file = File(context.cacheDir, "audio_${UUID.randomUUID()}.m4a")

        // 2. Initialize the recorder with high-res settings
        recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.media.MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            android.media.MediaRecorder()
        }.apply {
            setAudioSource(android.media.MediaRecorder.AudioSource.MIC)

            setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000) // 128kbps Bitrate
            setAudioSamplingRate(44100)     // 44.1kHz Sample Rate

            setOutputFile(file.absolutePath)
            prepare()
            start()
        }


        onUpdate(audioNote.copy(audioPath = file.absolutePath, isRecording = true))

        recordingSeconds = 0
        scope.launch {
            while (recorder != null) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) { e.printStackTrace() }
        recorder?.release()
        recorder = null

        val duration = (recordingSeconds * 1000).coerceAtLeast(1000)
        totalDuration = duration.toFloat()
        onUpdate(audioNote.copy(isRecording = false, durationMs = duration))
        onSaveRequest()
    }

    fun togglePlayback() {
        if (isPlaying) {
            player?.pause()
            isPlaying = false
        } else {
            if (player == null) {
                player = MediaPlayer().apply {
                    setDataSource(audioNote.audioPath)
                    prepare()
                    setOnCompletionListener {
                        isPlaying = false
                        currentPlaybackPosition = 0f
                    }
                }
                totalDuration = player!!.duration.toFloat()
            }
            player?.start()
            isPlaying = true
        }
    }

    CanvasItemWrapper(
        position = audioNote.position, width = audioNote.width, height = audioNote.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = audioNote.isLocked, onLockedChange = { onUpdate(audioNote.copy(isLocked = it)) },
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, onDuplicate = onDuplicate, hyperlinkTarget = audioNote.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(audioNote.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Title editing
                    androidx.compose.foundation.text.BasicTextField(
                        value = audioNote.title,
                        onValueChange = { onUpdate(audioNote.copy(title = it)) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        modifier = Modifier.background(Color.White.copy(alpha = 0.8f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp).width(120.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Color picking
                    val colors = listOf(Color(0xFFE8F5E9), Color(0xFFE3F2FD), Color(0xFFFFF9C4), Color(0xFFFCE4EC), Color(0xFFF3E5F5))
                    colors.forEach { colorOption ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(colorOption, CircleShape)
                                .border(if (audioNote.color == colorOption) 2.dp else 1.dp, if (audioNote.color == colorOption) Color.Blue else Color.LightGray, CircleShape)
                                .clickable {
                                    onUpdate(audioNote.copy(color = colorOption))
                                    onSaveRequest()
                                }
                        )
                    }
                }
            }
        }
    ) { isLocked ->
        Row(
            modifier = Modifier.fillMaxSize().background(audioNote.color).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (audioNote.audioPath.isEmpty() && !audioNote.isRecording) {
                // State A: Unrecorded capsule trigger
                IconButton(onClick = { startRecording() }, modifier = Modifier.background(Color(0xFF25D366), CircleShape).size(36.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White)
                }
                Text("Tap icon to record audio note", fontSize = 12.sp, color = Color.DarkGray)
            } else if (audioNote.isRecording) {
                // State B: Recording timeline track style
                IconButton(onClick = { stopRecording() }, modifier = Modifier.background(Color.Red, CircleShape).size(36.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
                Text("Recording: ${recordingSeconds}s", fontSize = 13.sp, color = Color.Red)
            } else {
                // State C: WhatsApp Audio playback row UI setup
                IconButton(onClick = { togglePlayback() }, modifier = Modifier.background(Color(0xFF128C7E), CircleShape).size(36.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = currentPlaybackPosition,
                        onValueChange = {
                            currentPlaybackPosition = it
                            player?.seekTo(it.toInt())
                        },
                        valueRange = 0f..totalDuration.coerceAtLeast(1f),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val displayTime = if (isPlaying) currentPlaybackPosition.toInt() / 1000 else audioNote.durationMs / 1000
                        Text(text = String.format("%02d:%02d", displayTime / 60, displayTime % 60), fontSize = 10.sp, color = Color.Gray)
                        Text(text = audioNote.title, fontSize = 10.sp, color = Color.Gray, maxLines = 1, modifier = Modifier.widthIn(max = 100.dp), overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
