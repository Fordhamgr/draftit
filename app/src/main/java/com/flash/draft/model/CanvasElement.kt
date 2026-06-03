package com.flash.draft.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

sealed class CanvasElement {
    abstract val id: String

    data class StickyNote(
        override val id: String,
        val position: Offset,
        val text: String = "",
        val color: Color = Color(0xFFFFF9C4),
        val width: Float = 200f,
        val height: Float = 200f,
        val fontSize: Float = 16f,
        val fontFamily: String = "SansSerif",
        val isLocked: Boolean = false
    ) : CanvasElement()

    // NEW: The data structure for a freehand drawing
    data class Sketch(
        override val id: String,
        val points: List<Offset>,
        val color: Color = Color.Companion.Black,
        val strokeWidth: Float = 6f,
        val toolType: String = "Pen", // "Pen", "Highlighter"
        val alpha: Float = 1f,
        val isLocked: Boolean = false
    ) : CanvasElement()

    // NEW: The Image Element
    data class ImageCard(
        override val id: String,
        val position: Offset,
        val uriString: String,
        val width: Float = 300f,
        val height: Float = 300f,
        val originalWidth: Float = 300f,
        val originalHeight: Float = 300f,
        val alpha: Float = 1f,
        val rotation: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class Connector(
        override val id: String,
        val startId: String,
        val endId: String,
        val color: Color = Color.Companion.Black,
        val strokeWidth: Float = 4f,
        val lineStyle: String = "Solid", // "Solid", "Dashed", "Dotted"
        val arrowDirection: String = "Both", // "None", "End", "Both"
        val arrowShape: String = "Filled", // "Open", "Filled", "Circle", "Diamond"
        val routing: String = "Straight", // Prepping for Tier 2 curvature!
        val isLocked: Boolean = false
    ) : CanvasElement()
    data class PdfCard(
        override val id: String,
        val position: Offset,
        val uriString: String,
        val fileName: String,
        val fileSize: String,
        val width: Float = 260f,
        val height: Float = 140f,
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class CodeBlock(
        override val id: String,
        val position: Offset,
        val code: String = "// Paste your code here\nfun start() {\n    \n}",
        val width: Float = 280f,
        val height: Float = 180f,
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class AudioNote(
        override val id: String,
        val position: Offset,
        val audioPath: String = "",
        val durationMs: Int = 0,
        val isRecording: Boolean = false,
        val width: Float = 240f,
        val height: Float = 80f,
        val title: String = "Voice Memo",
        val color: Color = Color(0xFFE8F5E9),
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class ShapeNode(
        override val id: String,
        val position: Offset,
        val width: Float = 150f,
        val height: Float = 150f,
        val shapeType: String = "Rectangle", // "Rectangle", "Circle", "Triangle"
        val color: Color = Color(0xFF2196F3),
        val isFilled: Boolean = true,
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class LabelNode(
        override val id: String,
        val position: Offset,
        val text: String = "Double tap to edit",
        val width: Float = 250f,
        val height: Float = 80f,
        val color: Color = Color.Companion.Black,
        val fontSize: Float = 24f,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val fontFamily: String = "SansSerif",
        val textAlign: String = "Left",
        val backgroundColor: Color = Color.Companion.Transparent,
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class FrameNode(
        override val id: String,
        val position: Offset,
        val width: Float = 400f,
        val height: Float = 300f,
        val title: String = "New Frame",
        val color: Color = Color(0xFF9E9E9E),
        val isMagnetic: Boolean = false, // FIX: Off by default!
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class TaskItem(
        val id: String = UUID.randomUUID().toString(),
        val text: String = "",
        val isCompleted: Boolean = false
    )

    data class TaskListNode(
        override val id: String,
        val position: Offset,
        val width: Float = 280f,
        val height: Float = 350f,
        val title: String = "To-Do List",
        val tasks: List<TaskItem> = listOf(TaskItem(text = "New task")),
        val color: Color = Color.Companion.White,
        val isLocked: Boolean = false
    ) : CanvasElement()
}