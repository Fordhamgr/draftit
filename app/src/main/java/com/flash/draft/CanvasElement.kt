package com.flash.draft

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

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
        val fontFamily: String = "SansSerif"
    ) : CanvasElement()

    // NEW: The data structure for a freehand drawing
    data class Sketch(
        override val id: String,
        val points: List<Offset>,
        val color: Color = Color.Black,
        val strokeWidth: Float = 6f,
        val toolType: String = "Pen", // "Pen", "Highlighter"
        val alpha: Float = 1f
    ) : CanvasElement()

    // NEW: The Image Element
    data class ImageCard(
        override val id: String,
        val position: Offset,
        val uriString: String,
        val width: Float = 300f,
        val height: Float = 300f,
        val alpha: Float = 1f,
        val rotation: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val cornerRadius: Float = 8f,
        val isLocked: Boolean = false
    ) : CanvasElement()

    data class Connector(
        override val id: String,
        val startId: String,
        val endId: String,
        val color: Color = Color.Black,
        val strokeWidth: Float = 4f,
        val lineStyle: String = "Solid", // "Solid", "Dashed", "Dotted"
        val arrowStyle: String = "End"   // "None", "End", "Both"
    ) : CanvasElement()

    data class PdfCard(
        override val id: String,
        val position: Offset,
        val uriString: String,
        val fileName: String,
        val fileSize: String,
        val width: Float = 260f,
        val height: Float = 140f
    ) : CanvasElement()

    data class CodeBlock(
        override val id: String,
        val position: Offset,
        val code: String = "// Paste your code here\nfun start() {\n    \n}",
        val width: Float = 280f,
        val height: Float = 180f
    ) : CanvasElement()

    data class AudioNote(
        override val id: String,
        val position: Offset,
        val audioPath: String = "",
        val durationMs: Int = 0,
        val isRecording: Boolean = false,
        val width: Float = 240f,
        val height: Float = 80f
    ) : CanvasElement()

    data class ShapeNode(
        override val id: String,
        val position: Offset,
        val width: Float = 150f,
        val height: Float = 150f,
        val shapeType: String = "Rectangle", // "Rectangle", "Circle", "Triangle"
        val color: Color = Color(0xFF2196F3),
        val isFilled: Boolean = true
    ) : CanvasElement()


}