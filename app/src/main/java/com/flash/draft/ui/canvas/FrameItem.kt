package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.CanvasElement

@Composable
fun FrameItem(
    frameNode: CanvasElement.FrameNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.FrameNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    CanvasItemWrapper(
        position = frameNode.position, width = frameNode.width, height = frameNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = frameNode.isLocked, onLockedChange = { onUpdate(frameNode.copy(isLocked = it)) },
        hasDefaultBackground = false, // Transparent center!
        isFullyDraggable = true,
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, hyperlinkTarget = frameNode.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(frameNode.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val lockIcon = if (frameNode.isMagnetic) Icons.Default.Lock else Icons.Default.LockOpen
                val lockTint = if (frameNode.isMagnetic) Color.Blue else Color.Gray

                Icon(
                    imageVector = lockIcon,
                    contentDescription = "Toggle Magnetic",
                    tint = lockTint,
                    modifier = Modifier.size(24.dp).clickable {
                        onUpdate(frameNode.copy(isMagnetic = !frameNode.isMagnetic))
                        onSaveRequest()
                    }
                )

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f))) // Divider

                // Context Menu: Colors
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0xFF9E9E9E, 0xFF2196F3, 0xFF4CAF50, 0xFFF44336, 0xFFFF9800, 0xFF9C27B0).forEach { colorVal ->
                        val color = Color(colorVal)
                        Box(modifier = Modifier.size(24.dp).background(color, CircleShape)
                            .border(if (frameNode.color == color) 2.dp else 1.dp, if (frameNode.color == color) Color.Black else Color.LightGray, CircleShape)
                            .clickable { onUpdate(frameNode.copy(color = color)); onSaveRequest() }
                        )
                    }
                }
            }
        }
    ) { isLocked ->
        Column(modifier = Modifier.fillMaxSize()) {
            BasicTextField(
                value = frameNode.title,
                onValueChange = { onUpdate(frameNode.copy(title = it)) },
                enabled = isSelected,
                textStyle = TextStyle(color = frameNode.color, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
            )
            // The Box Boundary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(frameNode.color.copy(alpha = 0.2f)) // More visible for liquid lens
                    .border(3.dp, frameNode.color.copy(alpha = 0.6f))
            )
        }
    }
}