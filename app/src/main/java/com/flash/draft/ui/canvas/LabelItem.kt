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
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
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
    labelNode: CanvasElement.LabelNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, isCanvasLocked: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.LabelNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit, onDuplicate: (() -> Unit)? = null
) {
    var isDragging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    CanvasItemWrapper(
        position = labelNode.position, width = labelNode.width, height = labelNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, isCanvasLocked = isCanvasLocked, onConnectRequest = onConnectRequest,
        isLocked = labelNode.isLocked || isCanvasLocked, onLockedChange = { onUpdate(labelNode.copy(isLocked = it)) },
        hasDefaultBackground = false, // <-- Makes it transparent!
        isFullyDraggable = false, isChildDragging = isDragging, // Critical for text editing
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, onDuplicate = onDuplicate, hyperlinkTarget = labelNode.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(labelNode.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            var activeColorPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            val isDark = LocalDarkTheme.current
            val btnBg = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            val activeTabBg = androidx.compose.material3.MaterialTheme.colorScheme.primary
            val activeTabText = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
            val iconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            val inactiveIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Typography & Alignment & Fonts
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // BIU
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        @Composable
                        fun FormatIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit) {
                            Box(
                                modifier = Modifier.size(28.dp).background(if (isActive) btnBg else Color.Transparent, CircleShape).clickable { onClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isActive) iconColor else inactiveIconColor)
                            }
                        }
                        FormatIcon(Icons.Default.FormatBold, labelNode.isBold) { onUpdate(labelNode.copy(isBold = !labelNode.isBold)); onSaveRequest() }
                        FormatIcon(Icons.Default.FormatItalic, labelNode.isItalic) { onUpdate(labelNode.copy(isItalic = !labelNode.isItalic)); onSaveRequest() }
                        FormatIcon(Icons.Default.FormatUnderlined, labelNode.isUnderline) { onUpdate(labelNode.copy(isUnderline = !labelNode.isUnderline)); onSaveRequest() }
                    }
                    
                    // Alignment
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        @Composable
                        fun AlignIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit) {
                            Box(
                                modifier = Modifier.size(28.dp).background(if (isActive) btnBg else Color.Transparent, CircleShape).clickable { onClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isActive) iconColor else inactiveIconColor)
                            }
                        }
                        AlignIcon(Icons.Default.FormatAlignLeft, labelNode.textAlign == "Left") { onUpdate(labelNode.copy(textAlign = "Left")); onSaveRequest() }
                        AlignIcon(Icons.Default.FormatAlignCenter, labelNode.textAlign == "Center") { onUpdate(labelNode.copy(textAlign = "Center")); onSaveRequest() }
                        AlignIcon(Icons.Default.FormatAlignRight, labelNode.textAlign == "Right") { onUpdate(labelNode.copy(textAlign = "Right")); onSaveRequest() }
                    }
                    
                    // Fonts Group
                    Row(
                        modifier = Modifier.background(btnBg, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Sans", "Serif", "Mono", "Curs").forEach { fontName ->
                            val isSelected = labelNode.fontFamily == fontName || (labelNode.fontFamily == "SansSerif" && fontName == "Sans")
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) activeTabBg else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                    .clickable { onUpdate(labelNode.copy(fontFamily = fontName)); onSaveRequest() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = fontName, 
                                    fontSize = 11.sp, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeTabText else inactiveIconColor
                                )
                            }
                        }
                    }
                }
                
                // Row 2: Size & Colors
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Slider
                    Row(
                        modifier = Modifier.background(btnBg, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inactiveIconColor)
                        Spacer(Modifier.width(6.dp))
                        Slider(
                            value = labelNode.fontSize,
                            onValueChange = { onUpdate(labelNode.copy(fontSize = it)) },
                            onValueChangeFinished = { onSaveRequest() },
                            valueRange = 12f..72f,
                            modifier = Modifier.width(100.dp).height(24.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = inactiveIconColor)
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    
                    // Color Toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // FG
                        Box(
                            modifier = Modifier.size(32.dp).background(if (activeColorPicker == "fg") btnBg else Color.Transparent, CircleShape).clickable { activeColorPicker = if (activeColorPicker == "fg") null else "fg" },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("T", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = iconColor)
                                Spacer(Modifier.height(2.dp))
                                Box(Modifier.size(12.dp, 2.dp).background(labelNode.color, CircleShape))
                            }
                        }
                        
                        // BG
                        Box(
                            modifier = Modifier.size(32.dp).background(if (activeColorPicker == "bg") btnBg else Color.Transparent, CircleShape).clickable { activeColorPicker = if (activeColorPicker == "bg") null else "bg" },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FormatColorFill, contentDescription = "Bg Color", modifier = Modifier.size(16.dp), tint = iconColor)
                                Spacer(Modifier.height(2.dp))
                                Box(Modifier.size(12.dp, 2.dp).background(if (labelNode.backgroundColor.alpha == 0f) Color.Transparent else labelNode.backgroundColor, CircleShape).border(0.5.dp, Color.Gray, CircleShape))
                            }
                        }
                    }
                }

                // Row 3: Color Picker (Animated Expansion)
                androidx.compose.animation.AnimatedVisibility(visible = activeColorPicker != null) {
                    val isFg = activeColorPicker == "fg"
                    val colors = if (isFg) listOf(0xFF000000, 0xFFFFFFFF, 0xFF2196F3, 0xFF4CAF50, 0xFFF44336) else listOf(0x00000000, 0xFFFFFFFF, 0xFFFFF9C4, 0xFFBBDEFB, 0xFFC8E6C9, 0xFF1A1A1A)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(btnBg, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { colorVal ->
                            val color = Color(colorVal.toLong())
                            val isSelected = if (isFg) labelNode.color == color else labelNode.backgroundColor == color
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(color, CircleShape)
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF2196F3) else Color.LightGray, CircleShape)
                                    .clickable { 
                                        if (isFg) onUpdate(labelNode.copy(color = color)) else onUpdate(labelNode.copy(backgroundColor = color))
                                        onSaveRequest()
                                    }
                            )
                        }
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

            // Expanded Drag Handle Stroke for Text Box
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .layout { measurable, constraints ->
                            // Expand the hit area by 32dp (16dp on each side)
                            val paddingPx = 32.dp.roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = constraints.maxWidth + paddingPx,
                                    maxWidth = constraints.maxWidth + paddingPx,
                                    minHeight = constraints.maxHeight + paddingPx,
                                    maxHeight = constraints.maxHeight + paddingPx
                                )
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(
                                    (constraints.maxWidth - placeable.width) / 2,
                                    (constraints.maxHeight - placeable.height) / 2
                                )
                            }
                        }
                        .pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { 
                                    isDragging = true
                                    if (!isLocked) onSelect() 
                                }, 
                                onDragEnd = { 
                                    isDragging = false
                                    if (!isLocked) onSaveRequest() 
                                },
                                onDragCancel = {
                                    isDragging = false
                                    if (!isLocked) onSaveRequest()
                                },
                                onDrag = { change, dragAmount -> 
                                    if (!isLocked) {
                                        change.consume()
                                        onPositionDelta(dragAmount)
                                    }
                                }
                            )
                        }
                        .border(4.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                )
            }

        }
    }
}



