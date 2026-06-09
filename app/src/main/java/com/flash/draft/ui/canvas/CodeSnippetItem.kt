package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.CanvasElement

@Composable
fun CodeSnippetItem(
    codeBlock: CanvasElement.CodeBlock, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.CodeBlock) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    CanvasItemWrapper(
        position = codeBlock.position, width = codeBlock.width, height = codeBlock.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = codeBlock.isLocked, onLockedChange = { onUpdate(codeBlock.copy(isLocked = it)) },
        isFullyDraggable = false, // Critical so TextField works normally
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, hyperlinkTarget = codeBlock.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(codeBlock.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            Icon(Icons.Default.ContentCopy, "Copy Code", modifier = Modifier.size(24.dp).clickable {
                clipboardManager.setText(AnnotatedString(codeBlock.code))
            })
        }
    ) { isLocked ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
            // Drag Header (macOS Terminal style)
            Box(
                modifier = Modifier.fillMaxWidth().height(28.dp).background(Color(0xFF2D2D2D))
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
                Row(modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterStart), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))
                }
                Icon(Icons.Default.DragHandle, contentDescription = "Drag", tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
            }

            // The Code Editor
            BasicTextField(
                value = codeBlock.code,
                onValueChange = { onUpdate(codeBlock.copy(code = it)); onSaveRequest() },
                enabled = isSelected, // Fixes keyboard popping up on first tap
                modifier = Modifier.fillMaxSize().padding(12.dp).onFocusChanged { if (it.isFocused) onSelect() },
                textStyle = LocalTextStyle.current.merge(
                    TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFD4D4D4))
                )
            )
        }
    }
}