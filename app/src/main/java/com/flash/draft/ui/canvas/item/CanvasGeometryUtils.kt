package com.flash.draft.ui.canvas.item
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.flash.draft.model.CanvasElement
import kotlin.math.abs
import kotlin.math.min

val LocalDarkTheme = compositionLocalOf { false }

fun getAdaptiveColor(originalColor: Color, isDark: Boolean): Color {
    return when {
        originalColor == Color.Black && isDark -> Color.White
        originalColor == Color.White && isDark -> Color(0xFF1E1E1E)
        originalColor == Color.White && !isDark -> Color.Black
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

fun getCenterPoint(el: CanvasElement): Offset? {
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
        else -> null
    }
}

fun getCenterPoint(elementId: String, elements: List<CanvasElement>): Offset? {
    val el = elements.find { it.id == elementId } ?: return null
    return getCenterPoint(el)
}

fun getElementDimensions(el: CanvasElement): Pair<Float, Float> {
    return when (el) {
        is CanvasElement.StickyNote -> Pair(el.width, el.height)
        is CanvasElement.ImageCard -> Pair(el.width, el.height)
        is CanvasElement.PdfCard -> Pair(el.width, el.height)
        is CanvasElement.CodeBlock -> Pair(el.width, el.height)
        is CanvasElement.AudioNote -> Pair(el.width, el.height)
        is CanvasElement.ShapeNode -> Pair(el.width, el.height)
        is CanvasElement.LabelNode -> Pair(el.width, el.height)
        is CanvasElement.FrameNode -> Pair(el.width, el.height)
        is CanvasElement.TaskListNode -> Pair(el.width, el.height)
        else -> Pair(50f, 50f)
    }
}

fun getElementDimensions(elementId: String, elements: List<CanvasElement>): Pair<Float, Float> {
    val el = elements.find { it.id == elementId } ?: return Pair(50f, 50f)
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