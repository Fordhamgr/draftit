package com.flash.draft.ui.canvas.item
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import com.flash.draft.model.CanvasElement
import kotlin.math.abs
import kotlin.math.min

val LocalDarkTheme = compositionLocalOf { false }
val LocalPerformanceMode = compositionLocalOf { false }
val LocalCameraScaleProvider = compositionLocalOf<() -> Float> { { 1f } }

fun getAdaptiveColor(originalColor: Color, isDark: Boolean): Color {
    if (!isDark) {
        if (originalColor == Color.White) return Color.Black
        return originalColor
    }
    
    // In Dark Mode:
    return when (originalColor) {
        Color.Black -> Color.White
        Color.White -> Color(0xFF1E1E1E)
        Color.DarkGray -> Color.LightGray
        // Map bright pastels to darker pastel shades
        Color(0xFFFFF9C4) -> Color(0xFF4A4835) // Dark Yellow
        Color(0xFFFFCDD2) -> Color(0xFF4A3739) // Dark Pink
        Color(0xFFBBDEFB) -> Color(0xFF2E3B46) // Dark Blue
        Color(0xFFC8E6C9) -> Color(0xFF354136) // Dark Green
        Color(0xFFE1BEE7) -> Color(0xFF413244) // Dark Purple
        Color(0xFFE3F2FD) -> Color(0xFF253440) // Another Blue from TaskList
        Color(0xFFE8F5E9) -> Color(0xFF2C3B2E) // Another Green from TaskList
        Color(0xFFFCE4EC) -> Color(0xFF4A353E) // Another Pink from TaskList
        else -> originalColor
    }
}

fun distancePointToSegment(p: Offset, v: Offset, w: Offset): Float {
    val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
    if (l2 == 0f) return (p - v).getDistance()
    var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
    t = Math.max(0f, Math.min(1f, t))
    val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
    return (p - proj).getDistance()
}

fun getCenterPoint(el: CanvasElement): Offset {
    return when (el) {
        is CanvasElement.StickyNote -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.ImageCard -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.PdfCard -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.CodeBlock -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.AudioNote -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.ShapeNode -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.LabelNode -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.FrameNode -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        is CanvasElement.TaskListNode -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
        else -> Offset.Unspecified
    }
}

fun getCenterPoint(elementId: String, elements: List<CanvasElement>): Offset {
    val el = elements.find { it.id == elementId } ?: return Offset.Unspecified
    return getCenterPoint(el)
}

fun getElementDimensions(el: CanvasElement): Size {
    return when (el) {
        is CanvasElement.StickyNote -> Size(el.width, el.height)
        is CanvasElement.ImageCard -> Size(el.width, el.height)
        is CanvasElement.PdfCard -> Size(el.width, el.height)
        is CanvasElement.CodeBlock -> Size(el.width, el.height)
        is CanvasElement.AudioNote -> Size(el.width, el.height)
        is CanvasElement.ShapeNode -> Size(el.width, el.height)
        is CanvasElement.LabelNode -> Size(el.width, el.height)
        is CanvasElement.FrameNode -> Size(el.width, el.height)
        is CanvasElement.TaskListNode -> Size(el.width, el.height)
        else -> Size(50f, 50f)
    }
}

fun getElementDimensions(elementId: String, elements: List<CanvasElement>): Size {
    val el = elements.find { it.id == elementId } ?: return Size(50f, 50f)
    return getElementDimensions(el)
}

fun getRectangleIntersection(centerStart: Offset, centerEnd: Offset, width: Float, height: Float, padding: Float = 25f): Offset {
    val dx = centerEnd.x - centerStart.x
    val dy = centerEnd.y - centerStart.y
    if (dx == 0f && dy == 0f) return centerStart
    val halfW = (width / 2f) + padding; val halfH = (height / 2f) + padding
    val absDx = abs(dx); val absDy = abs(dy)
    val scaleX = if (absDx > 0) halfW / absDx else Float.MAX_VALUE
    val scaleY = if (absDy > 0) halfH / absDy else Float.MAX_VALUE
    val intersectScale = min(scaleX, scaleY); val finalScale = min(intersectScale, 1f)
    return Offset(centerStart.x + dx * finalScale, centerStart.y + dy * finalScale)
}
