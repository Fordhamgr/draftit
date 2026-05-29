package com.flash.draft

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun DraftCanvas(viewModel: DraftViewModel) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Observe database changes instantly
    val notes by viewModel.notesFlow.collectAsState(initial = emptyList())

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val centerX = constraints.maxWidth / 2f
        val centerY = constraints.maxHeight / 2f

        // 1. Background Layer (Pan & Zoom)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        offset += pan
                    }
                }
        ) {
            val spacing = 75f * scale
            val startX = (offset.x % spacing) - spacing
            val startY = (offset.y % spacing) - spacing

            var x = startX
            while (x < size.width + spacing) {
                var y = startY
                while (y < size.height + spacing) {
                    drawCircle(color = Color.LightGray, radius = 2f * scale, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }

        // 2. Interactive UI Layer (Sticky Notes)
        notes.forEach { note ->
            key(note.id) {
                val screenX = centerX + (note.x * scale) + offset.x
                val screenY = centerY + (note.y * scale) + offset.y

                Box(
                    modifier = Modifier
                        .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                        .scale(scale)
                        .size(note.width.dp, note.height.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(Color(note.colorValue), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(Color.Black.copy(alpha = 0.05f))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val updatedNote = note.copy(
                                            x = note.x + (dragAmount.x / scale),
                                            y = note.y + (dragAmount.y / scale)
                                        )
                                        viewModel.updateNote(updatedNote) // Auto-save on drag
                                    }
                                }
                        )

                        // Text Input
                        BasicTextField(
                            value = note.text,
                            onValueChange = { newText ->
                                viewModel.updateNote(note.copy(text = newText)) // Auto-save on type
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // 3. Floating Action Button
        FloatingActionButton(
            onClick = {
                val dropX = -offset.x / scale
                val dropY = -offset.y / scale
                viewModel.addNote(
                    NoteEntity(
                        id = UUID.randomUUID().toString(),
                        x = dropX,
                        y = dropY,
                        text = ""
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Note")
        }
    }
}