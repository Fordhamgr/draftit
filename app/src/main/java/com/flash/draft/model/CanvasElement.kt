package com.flash.draft.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID
import androidx.compose.runtime.Immutable

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timeMillis: Long = 0L
)

@Immutable
sealed class CanvasElement {
    abstract val id: String
    abstract val hyperlinkTarget: String?  // null = no link, "scene:<id>" or "board:<id>"

    data class StickyNote(
        override val id: String,
        val position: Offset,
        val text: String = "",
        val color: Color = Color(0xFFFFF9C4),
        val width: Float = 200f,
        val height: Float = 200f,
        val fontSize: Float = 16f,
        val fontFamily: String = "SansSerif",
        val style: String = "Classic",
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement()

    // NEW: The data structure for a freehand drawing
    data class Sketch(
        override val id: String,
        val xCoords: FloatArray,
        val yCoords: FloatArray,
        val pressures: FloatArray,
        val times: LongArray,
        val color: Color = Color.Companion.Black,
        val strokeWidth: Float = 6f,
        val toolType: String = "Pen", // "Pen", "Highlighter"
        val alpha: Float = 1f,
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement() {
        @Transient
        var cachedStroke: androidx.ink.strokes.Stroke? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Sketch

            if (id != other.id) return false
            if (!xCoords.contentEquals(other.xCoords)) return false
            if (!yCoords.contentEquals(other.yCoords)) return false
            if (!pressures.contentEquals(other.pressures)) return false
            if (!times.contentEquals(other.times)) return false
            if (color != other.color) return false
            if (strokeWidth != other.strokeWidth) return false
            if (toolType != other.toolType) return false
            if (alpha != other.alpha) return false
            if (isLocked != other.isLocked) return false
            if (hyperlinkTarget != other.hyperlinkTarget) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + xCoords.contentHashCode()
            result = 31 * result + yCoords.contentHashCode()
            result = 31 * result + pressures.contentHashCode()
            result = 31 * result + times.contentHashCode()
            result = 31 * result + color.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            result = 31 * result + toolType.hashCode()
            result = 31 * result + alpha.hashCode()
            result = 31 * result + isLocked.hashCode()
            result = 31 * result + (hyperlinkTarget?.hashCode() ?: 0)
            return result
        }
    }

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
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
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
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement()
    data class PdfCard(
        override val id: String,
        val position: Offset,
        val uriString: String,
        val fileName: String,
        val fileSize: String,
        val width: Float = 260f,
        val height: Float = 140f,
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement()

    data class CodeBlock(
        override val id: String,
        val position: Offset,
        val code: String = "// Paste your code here\nfun start() {\n    \n}",
        val width: Float = 280f,
        val height: Float = 180f,
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
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
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement()

    data class ShapeNode(
        override val id: String,
        val position: Offset,
        val width: Float = 150f,
        val height: Float = 150f,
        val shapeType: String = "Rectangle", // "Rectangle", "Circle", "Triangle"
        val color: Color = Color(0xFF2196F3),
        val isFilled: Boolean = true,
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
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
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement()

    data class FrameNode(
        override val id: String,
        val position: Offset,
        val width: Float = 400f,
        val height: Float = 300f,
        val title: String = "New Frame",
        val color: Color = Color(0xFF9E9E9E),
        val isMagnetic: Boolean = false, // FIX: Off by default!
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
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
        val isLocked: Boolean = false,
        override val hyperlinkTarget: String? = null
    ) : CanvasElement()
}