package com.flash.draft

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed class CanvasElement {
    abstract val id: String
    abstract val position: Offset // Absolute coordinates on our infinite grid

    data class StickyNote(
        override val id: String,
        override val position: Offset,
        val text: String = "",
        val color: Color = Color(0xFFFFF9C4), // Default sticky yellow
        val width: Float = 200f,
        val height: Float = 200f
    ) : CanvasElement()

    // Future elements like Text, Shapes, or Images will go here
}