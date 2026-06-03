package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.AppFonts
import com.flash.draft.model.CanvasElement

@Composable
fun StickyNoteItem(
    note: CanvasElement.StickyNote, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.StickyNote) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    val currentFont = when (note.fontFamily) {
        "Serif" -> AppFonts.Serif
        "Mono" -> AppFonts.Mono
        "Cursive" -> AppFonts.Handwriting
        else -> AppFonts.Sans
    }

    CanvasItemWrapper(
        position = note.position, width = note.width, height = note.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = note.isLocked, onLockedChange = { onUpdate(note.copy(isLocked = it)) },
        isFullyDraggable = false, backgroundAlpha = 1f, // Critical so we don't block the TextField, and 1f to block out arrows behind
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest,
        customMenuContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("Sans", "Serif", "Mono", "Cursive").forEach { fontName ->
                        val isSelectedFont = note.fontFamily == fontName || (note.fontFamily == "SansSerif" && fontName == "Sans")
                        Text(
                            text = fontName, fontSize = 12.sp, fontWeight = if (isSelectedFont) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelectedFont) Color.White else Color.Black,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (isSelectedFont) Color.Blue else Color.LightGray.copy(alpha = 0.5f))
                                .clickable { onUpdate(note.copy(fontFamily = fontName)); onSaveRequest() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Box(modifier = Modifier.size(1.dp, 16.dp).background(Color.LightGray))
                    Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(20.dp).clickable { if (note.fontSize > 10f) { onUpdate(note.copy(fontSize = note.fontSize - 2f)); onSaveRequest() } })
                    Text("${note.fontSize.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Add, contentDescription = "Bigger", modifier = Modifier.size(20.dp).clickable { if (note.fontSize < 48f) { onUpdate(note.copy(fontSize = note.fontSize + 2f)); onSaveRequest() } })
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val colors = listOf(0xFFFFF9C4, 0xFFFFCDD2, 0xFFBBDEFB, 0xFFC8E6C9, 0xFFE1BEE7, 0xFFFFFFFF)
                    colors.forEach { colorVal ->
                        val isSelectedColor = note.color == Color(colorVal)
                        Box(modifier = Modifier.size(24.dp).background(Color(colorVal), CircleShape)
                            .border(if (isSelectedColor) 2.dp else 1.dp, if (isSelectedColor) Color.Blue else Color.LightGray, CircleShape)
                            .clickable { onUpdate(note.copy(color = Color(colorVal))); onSaveRequest() }
                        )
                    }
                }
            }
        }
    ) { isLocked ->
        Column(modifier = Modifier.fillMaxSize().background(note.color)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(32.dp).background(Color.Black.copy(alpha = 0.1f))
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
                    }
            ) {
                Icon(Icons.Default.DragHandle, contentDescription = "Drag", tint = Color.DarkGray, modifier = Modifier.align(Alignment.Center))
            }
            BasicTextField(
                value = note.text,
                enabled = isSelected,
                onValueChange = { onUpdate(note.copy(text = it)); onSaveRequest() },
                modifier = Modifier.fillMaxSize().padding(8.dp).onFocusChanged { if (it.isFocused) onSelect() },
                textStyle = LocalTextStyle.current.merge(TextStyle(fontSize = note.fontSize.sp, fontFamily = currentFont, color = Color.Black))
            )
        }
    }
}