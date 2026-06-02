package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.getAdaptiveColor

@Composable
fun LabelItem(
    labelNode: CanvasElement.LabelNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean,
    onUpdate: (CanvasElement.LabelNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    CanvasItemWrapper(
        position = labelNode.position, width = labelNode.width, height = labelNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting,
        hasDefaultBackground = false, // <-- Makes it transparent!
        isFullyDraggable = false, // Critical for text editing
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest,
        customMenuContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Font Size Slider
                Text("Size", fontSize = 12.sp, color = Color.Gray)
                Slider(
                    value = labelNode.fontSize,
                    onValueChange = { onUpdate(labelNode.copy(fontSize = it)) },
                    onValueChangeFinished = { onSaveRequest() },
                    valueRange = 12f..72f,
                    modifier = Modifier.width(100.dp).height(24.dp)
                )

                Box(modifier = Modifier.size(1.dp, 24.dp).background(Color.LightGray)) // Divider

                // Bold Toggle
                Icon(
                    Icons.Default.FormatBold, "Bold",
                    modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(isBold = !labelNode.isBold)); onSaveRequest() },
                    tint = if (labelNode.isBold) Color.Blue else Color.Gray
                )
            }

            // Text Colors
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf(0xFF000000, 0xFFFFFFFF, 0xFF2196F3, 0xFF4CAF50, 0xFFF44336).forEach { colorVal ->
                    val color = Color(colorVal)
                    Box(
                        modifier = Modifier
                            .size(24.dp).background(color, CircleShape)
                            .border(if (labelNode.color == color) 2.dp else 1.dp, if (color == Color(0xFFFFFFFF)) Color.LightGray else Color.Transparent, CircleShape)
                            .clickable { onUpdate(labelNode.copy(color = color)); onSaveRequest() }
                    )
                }
            }
        }
    ) {
        // The Text Field
        BasicTextField(
            value = labelNode.text,
            onValueChange = { onUpdate(labelNode.copy(text = it)) },
            enabled = isSelected, // Fixes the text-trap bug
            modifier = Modifier.fillMaxSize().padding(8.dp),
            textStyle = TextStyle(
                color = getAdaptiveColor(labelNode.color, LocalDarkTheme.current),
                fontSize = labelNode.fontSize.sp,
                fontWeight = if (labelNode.isBold) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}