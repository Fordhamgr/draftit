package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.AppFonts
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.getAdaptiveColor

@Composable
fun LabelItem(
    labelNode: CanvasElement.LabelNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.LabelNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit, onDuplicate: (() -> Unit)? = null
) {
    var isDragging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    CanvasItemWrapper(
        position = labelNode.position, width = labelNode.width, height = labelNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = labelNode.isLocked, onLockedChange = { onUpdate(labelNode.copy(isLocked = it)) },
        hasDefaultBackground = false, // <-- Makes it transparent!
        isFullyDraggable = false, isChildDragging = isDragging, // Critical for text editing
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, onDuplicate = onDuplicate, hyperlinkTarget = labelNode.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(labelNode.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Typography tools & Alignment
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(isBold = !labelNode.isBold)); onSaveRequest() }, tint = if (labelNode.isBold) Color(0xFF2196F3) else Color.Gray)
                    Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(isItalic = !labelNode.isItalic)); onSaveRequest() }, tint = if (labelNode.isItalic) Color(0xFF2196F3) else Color.Gray)
                    Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(isUnderline = !labelNode.isUnderline)); onSaveRequest() }, tint = if (labelNode.isUnderline) Color(0xFF2196F3) else Color.Gray)
                    
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    
                    Icon(Icons.Default.FormatAlignLeft, "Left", modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(textAlign = "Left")); onSaveRequest() }, tint = if (labelNode.textAlign == "Left") Color(0xFF2196F3) else Color.Gray)
                    Icon(Icons.Default.FormatAlignCenter, "Center", modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(textAlign = "Center")); onSaveRequest() }, tint = if (labelNode.textAlign == "Center") Color(0xFF2196F3) else Color.Gray)
                    Icon(Icons.Default.FormatAlignRight, "Right", modifier = Modifier.size(24.dp).clickable { onUpdate(labelNode.copy(textAlign = "Right")); onSaveRequest() }, tint = if (labelNode.textAlign == "Right") Color(0xFF2196F3) else Color.Gray)
                }
                
                // Row 2: Fonts & Size
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("Sans", "Serif", "Mono", "Cursive").forEach { fontName ->
                        val isSelectedFont = labelNode.fontFamily == fontName || (labelNode.fontFamily == "SansSerif" && fontName == "Sans")
                        Text(
                            text = fontName.take(4), fontSize = 12.sp, fontWeight = if (isSelectedFont) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelectedFont) Color.White else Color.Gray,
                            modifier = Modifier.background(if (isSelectedFont) Color(0xFF2196F3) else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .clickable { onUpdate(labelNode.copy(fontFamily = fontName)); onSaveRequest() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Text("${labelNode.fontSize.toInt()}", fontSize = 12.sp, color = Color.Gray)
                    Slider(
                        value = labelNode.fontSize,
                        onValueChange = { onUpdate(labelNode.copy(fontSize = it)) },
                        onValueChangeFinished = { onSaveRequest() },
                        valueRange = 12f..72f,
                        modifier = Modifier.width(80.dp).height(24.dp)
                    )
                }

                // Row 3: Colors
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("T", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                    listOf(0xFF000000, 0xFFFFFFFF, 0xFF2196F3, 0xFF4CAF50, 0xFFF44336).forEach { colorVal ->
                        val color = Color(colorVal)
                        Box(
                            modifier = Modifier
                                .size(24.dp).background(color, CircleShape)
                                .border(if (labelNode.color == color) 2.dp else 1.dp, if (color == Color(0xFFFFFFFF)) Color.LightGray else Color.Transparent, CircleShape)
                                .clickable { onUpdate(labelNode.copy(color = color)); onSaveRequest() }
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)).padding(horizontal = 4.dp))
                    Text("Bg", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                    listOf(0x00000000, 0xFFFFFFFF, 0xFFFFF9C4, 0xFFBBDEFB, 0xFFC8E6C9, 0xFF1A1A1A).forEach { colorVal ->
                        val color = Color(colorVal.toLong())
                        Box(
                            modifier = Modifier
                                .size(24.dp).background(color, CircleShape)
                                .border(if (labelNode.backgroundColor == color) 2.dp else 1.dp, if (labelNode.backgroundColor == color) Color.DarkGray else Color.LightGray, CircleShape)
                                .clickable { onUpdate(labelNode.copy(backgroundColor = color)); onSaveRequest() }
                        )
                    }
                }
            }
        }
    ) { isLocked ->
        // Wrap in a box to capture drag gestures when not selected
        Box(
            modifier = Modifier.fillMaxSize()
                .background(getAdaptiveColor(labelNode.backgroundColor, LocalDarkTheme.current), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .pointerInput(isSelected, isLocked) {
                    if (!isSelected) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { if (!isLocked) onSaveRequest(); isDragging = false },
                            onDragCancel = { if (!isLocked) onSaveRequest(); isDragging = false },
                            onDrag = { change, dragAmount -> 
                                if (!isLocked) {
                                    change.consume()
                                    onPositionDelta(dragAmount)
                                }
                            }
                        )
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
                    fontWeight = if (labelNode.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (labelNode.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (labelNode.isUnderline) TextDecoration.Underline else TextDecoration.None,
                    textAlign = when (labelNode.textAlign) {
                        "Center" -> TextAlign.Center
                        "Right" -> TextAlign.Right
                        else -> TextAlign.Left
                    },
                    fontFamily = when (labelNode.fontFamily) {
                        "Serif" -> AppFonts.Serif
                        "Mono", "Monospace" -> AppFonts.Mono
                        "Cursive" -> AppFonts.Handwriting
                        else -> AppFonts.Sans
                    }
                )
            )

            // Drag Handle for Text Box (drawn last so it's on top of the text field)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp) // Shifts the touch target so the visible handle sits entirely above the box
                        .size(64.dp, 48.dp) // Massive touch slop area
                        .pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { if (!isLocked) onSelect() }, onDragEnd = { if (!isLocked) onSaveRequest() },
                                onDrag = { change, dragAmount -> 
                                    if (!isLocked) {
                                        change.consume()
                                        onPositionDelta(dragAmount)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp, 16.dp) // Visible handle
                            .background(Color.Gray.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.composables.icons.lucide.R.drawable.lucide_ic_grip_horizontal),
                            contentDescription = "Drag",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

        }
    }
}
