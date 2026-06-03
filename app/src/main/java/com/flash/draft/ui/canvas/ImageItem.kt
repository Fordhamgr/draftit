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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun ImageItem(
    imageCard: CanvasElement.ImageCard, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.ImageCard) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onDuplicate: () -> Unit, onMakeSticker: () -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    CanvasItemWrapper(
        position = imageCard.position, width = imageCard.width, height = imageCard.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = imageCard.isLocked, onLockedChange = { onUpdate(imageCard.copy(isLocked = it)) },
        hasDefaultBackground = false, // FIX: Allows PNG transparency to show!
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest,
        customMenuContent = {
            val isDark = LocalDarkTheme.current
            
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .background(if (isDark) Color(0x33000000) else Color(0x0D000000), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Alpha Slider (Miniaturized)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(100.dp)) {
                    Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                    Slider(
                        value = imageCard.alpha, 
                        onValueChange = { onUpdate(imageCard.copy(alpha = it)) }, 
                        onValueChangeFinished = onSaveRequest, 
                        valueRange = 0.1f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))

                IconButton(onClick = { onUpdate(imageCard.copy(scaleX = imageCard.scaleX * -1f)); onSaveRequest() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SwapHoriz, "Flip H", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onUpdate(imageCard.copy(scaleY = imageCard.scaleY * -1f)); onSaveRequest() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SwapVert, "Flip V", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onUpdate(imageCard.copy(rotation = imageCard.rotation + 90f)); onSaveRequest() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.RotateRight, "Rotate", modifier = Modifier.size(20.dp))
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))

                // Reset Ratio
                IconButton(onClick = {
                    val ratio = imageCard.originalWidth / imageCard.originalHeight
                    val newHeight = imageCard.width / ratio
                    onUpdate(imageCard.copy(height = newHeight, scaleX = 1f, scaleY = 1f))
                    onSaveRequest()
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AspectRatio, "Reset Ratio", modifier = Modifier.size(20.dp), tint = Color(0xFF007AFF))
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))

                IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, "Duplicate", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMakeSticker, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AutoFixHigh, "Make Sticker", tint = Color(0xFF673AB7), modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { isLocked ->
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
        )
    }
}