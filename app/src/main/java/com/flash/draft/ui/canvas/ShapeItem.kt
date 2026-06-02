package com.flash.draft.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.flash.draft.model.CanvasElement

@Composable
fun ShapeItem(
    shapeNode: CanvasElement.ShapeNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean,
    onUpdate: (CanvasElement.ShapeNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    CanvasItemWrapper(
        position = shapeNode.position, width = shapeNode.width, height = shapeNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting,
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        hasDefaultBackground = false,
        onDelete = onDelete, onSaveRequest = onSaveRequest,
        customMenuContent = {
            // Context Menu: Shape Types & Fill Toggle
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CropSquare, "Rectangle", modifier = Modifier.size(24.dp).clickable { onUpdate(shapeNode.copy(shapeType = "Rectangle")); onSaveRequest() })
                Icon(Icons.Default.Circle, "Circle", modifier = Modifier.size(24.dp).clickable { onUpdate(shapeNode.copy(shapeType = "Circle")); onSaveRequest() })
                Icon(Icons.Default.ChangeHistory, "Triangle", modifier = Modifier.size(24.dp).clickable { onUpdate(shapeNode.copy(shapeType = "Triangle")); onSaveRequest() })
                Box(modifier = Modifier.size(1.dp, 24.dp).background(Color.LightGray)) // Divider
                Icon(if (shapeNode.isFilled) Icons.Default.FormatColorFill else Icons.Outlined.FormatColorReset, "Toggle Fill", modifier = Modifier.size(24.dp).clickable { onUpdate(shapeNode.copy(isFilled = !shapeNode.isFilled)); onSaveRequest() })
            }
            // Context Menu: Colors
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0xFF2196F3, 0xFF4CAF50, 0xFFF44336, 0xFFFF9800, 0xFF9C27B0, 0xFF000000).forEach { colorVal ->
                    val isSelectedColor = shapeNode.color == Color(colorVal)
                    Box(modifier = Modifier.size(24.dp).background(Color(colorVal), CircleShape)
                        .border(if (isSelectedColor) 2.dp else 1.dp, if (isSelectedColor) Color.Black else Color.LightGray, CircleShape)
                        .clickable { onUpdate(shapeNode.copy(color = Color(colorVal))); onSaveRequest() }
                    )
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val drawStyle = if (shapeNode.isFilled) Fill else Stroke(width = 8f)
            when (shapeNode.shapeType) {
                "Rectangle" -> drawRect(color = shapeNode.color, style = drawStyle)
                "Circle" -> drawCircle(color = shapeNode.color, style = drawStyle)
                "Triangle" -> {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = path, color = shapeNode.color, style = drawStyle)
                }
            }
        }
    }
}