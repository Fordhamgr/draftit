package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flash.draft.model.CanvasElement

@Composable
fun ImageItem(
    imageCard: CanvasElement.ImageCard, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean,
    onUpdate: (CanvasElement.ImageCard) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onDuplicate: () -> Unit, onMakeSticker: () -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    CanvasItemWrapper(
        position = imageCard.position, width = imageCard.width, height = imageCard.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting,
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest,
        customMenuContent = {
            // --- ROW 1: ACTIONS (Flip, Rotate, Lock, Duplicate) ---
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, "Flip H", modifier = Modifier.size(24.dp).clickable { onUpdate(imageCard.copy(scaleX = imageCard.scaleX * -1f)); onSaveRequest() })
                Icon(Icons.Default.SwapVert, "Flip V", modifier = Modifier.size(24.dp).clickable { onUpdate(imageCard.copy(scaleY = imageCard.scaleY * -1f)); onSaveRequest() })
                Icon(Icons.Default.RotateRight, "Rotate", modifier = Modifier.size(24.dp).clickable { onUpdate(imageCard.copy(rotation = imageCard.rotation + 90f)); onSaveRequest() })

                Box(modifier = Modifier.size(1.dp, 24.dp).background(Color.LightGray)) // Divider

                Icon(if (imageCard.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, "Lock", tint = if (imageCard.isLocked) Color.Red else Color.Black, modifier = Modifier.size(24.dp).clickable { onUpdate(imageCard.copy(isLocked = !imageCard.isLocked)); onSaveRequest() })
                Icon(Icons.Default.ContentCopy, "Duplicate", modifier = Modifier.size(20.dp).clickable { onDuplicate() })
                Icon(Icons.Default.AutoFixHigh, "Make Sticker", tint = Color(0xFF673AB7), modifier = Modifier.size(24.dp).clickable { onMakeSticker() })
            }

            // --- ROW 2: SLIDERS (Opacity & Corner Radius) ---
            Column(modifier = Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Opacity Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alpha", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(40.dp))
                    Slider(value = imageCard.alpha, onValueChange = { onUpdate(imageCard.copy(alpha = it)) }, onValueChangeFinished = onSaveRequest, valueRange = 0.1f..1f, modifier = Modifier.height(24.dp))
                }
                // Corner Radius Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Curve", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(40.dp))
                    Slider(value = imageCard.cornerRadius, onValueChange = { onUpdate(imageCard.copy(cornerRadius = it)) }, onValueChangeFinished = onSaveRequest, valueRange = 0f..100f, modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        // --- APPLY THE TRANSFORMS TO THE IMAGE ---
        AsyncImage(
            model = imageCard.uriString,
            contentDescription = "Canvas Image",
            contentScale = ContentScale.Fit, // Or Crop if they prefer the card filled
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = imageCard.alpha
                    rotationZ = imageCard.rotation
                    scaleX = imageCard.scaleX
                    scaleY = imageCard.scaleY
                }
                .clip(RoundedCornerShape(imageCard.cornerRadius.dp)) // Cardify!
        )
    }
}