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
import androidx.compose.material.icons.filled.Opacity
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.LocalPerformanceMode

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
        onDelete = onDelete, onSaveRequest = onSaveRequest, hyperlinkTarget = imageCard.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(imageCard.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            val isDark = LocalDarkTheme.current
            val btnBg = if (isDark) Color(0xFF2A2A2E) else Color(0xFFF0F0F0)
            val textColor = if (isDark) Color.White else Color.Black
            val highlightBg = Color(0xFF2979FF)
            
            Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Opacity Row
                    Row(
                        modifier = Modifier
                            .width(408.dp)
                            .height(56.dp)
                            .background(btnBg, RoundedCornerShape(28.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Opacity, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("OPACITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 1.sp)
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Slider(
                            value = imageCard.alpha, 
                            onValueChange = { onUpdate(imageCard.copy(alpha = it)) }, 
                            onValueChangeFinished = onSaveRequest, 
                            valueRange = 0.1f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${(imageCard.alpha * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                    
                    // Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        @Composable
                        fun ToolBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isHighlighted: Boolean = false, onClick: () -> Unit) {
                            Column(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(if (isHighlighted) highlightBg else btnBg, RoundedCornerShape(20.dp))
                                    .clickable { onClick() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(icon, contentDescription = label, tint = if (isHighlighted) Color.White else textColor, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isHighlighted) Color.White else textColor)
                            }
                        }

                        ToolBtn(Icons.Default.SwapHoriz, "Flip X") { onUpdate(imageCard.copy(scaleX = imageCard.scaleX * -1f)); onSaveRequest() }
                        ToolBtn(Icons.Default.SwapVert, "Flip Y") { onUpdate(imageCard.copy(scaleY = imageCard.scaleY * -1f)); onSaveRequest() }
                        ToolBtn(Icons.Default.AspectRatio, "Aspect") {
                            val ratio = imageCard.originalWidth / imageCard.originalHeight
                            val newHeight = imageCard.width / ratio
                            onUpdate(imageCard.copy(height = newHeight, scaleX = 1f, scaleY = 1f))
                            onSaveRequest()
                        }
                        ToolBtn(Icons.Default.ContentCopy, "Copy") { onDuplicate() }
                        ToolBtn(Icons.Default.AutoFixHigh, "Eraser", isHighlighted = true) { onMakeSticker() }
                    }
                }
        }
    ) { isLocked ->
        val context = LocalContext.current
        val isPerformance = LocalPerformanceMode.current
        val imageRequest = androidx.compose.runtime.remember(imageCard.uriString, isPerformance) {
            ImageRequest.Builder(context)
                .data(imageCard.uriString)
                .apply {
                    if (isPerformance) {
                        size(800) // Lower resolution for preview
                    }
                }
                .build()
        }

        // --- APPLY THE TRANSFORMS TO THE IMAGE ---
        AsyncImage(
            model = imageRequest,
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
