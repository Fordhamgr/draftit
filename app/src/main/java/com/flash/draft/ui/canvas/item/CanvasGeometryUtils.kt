package com.flash.draft.ui.canvas.item
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import com.flash.draft.model.CanvasElement
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val LocalDarkTheme = compositionLocalOf { false }
val LocalLiquidDarkTheme = compositionLocalOf { true }
val LocalBackgroundStyle = compositionLocalOf { com.flash.draft.model.BackgroundStyle.Solid }
val LocalPerformanceMode = compositionLocalOf { false }
val LocalCurvedConnectors = compositionLocalOf { false }


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

fun getRectangleIntersection(centerStart: Offset, centerEnd: Offset, width: Float, height: Float, padding: Float = 0f): Offset {
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

fun isPointInElementBounds(point: Offset, element: CanvasElement): Boolean {
    val pos = when (element) {
        is CanvasElement.StickyNote -> element.position
        is CanvasElement.ImageCard -> element.position
        is CanvasElement.PdfCard -> element.position
        is CanvasElement.CodeBlock -> element.position
        is CanvasElement.AudioNote -> element.position
        is CanvasElement.ShapeNode -> element.position
        is CanvasElement.LabelNode -> element.position
        is CanvasElement.FrameNode -> element.position
        is CanvasElement.TaskListNode -> element.position
        else -> return false
    }
    val dim = getElementDimensions(element)
    return point.x >= pos.x && point.x <= pos.x + dim.width &&
           point.y >= pos.y && point.y <= pos.y + dim.height
}

fun findHitConnector(
    worldPoint: Offset,
    elements: List<CanvasElement>,
    thresholdPx: Float
): CanvasElement.Connector? {
    for (i in elements.indices.reversed()) {
        val el = elements[i]
        if (el is CanvasElement.Connector) {
            val startEl = elements.find { it.id == el.startId } ?: continue
            val endEl = elements.find { it.id == el.endId } ?: continue
            val centerStart = getCenterPoint(startEl)
            val centerEnd = getCenterPoint(endEl)
            if (centerStart == Offset.Unspecified || centerEnd == Offset.Unspecified) continue

            val startDim = getElementDimensions(startEl)
            val endDim = getElementDimensions(endEl)
            val start = getRectangleIntersection(centerStart, centerEnd, startDim.width, startDim.height, 0f)
            val end = getRectangleIntersection(centerEnd, centerStart, endDim.width, endDim.height, 0f)

            val dist = distancePointToSegment(worldPoint, start, end)
            val arrowLen = (16f + el.strokeWidth * 1.5f).coerceAtLeast(15f)
            val distToStart = (worldPoint - start).getDistance()
            val distToEnd = (worldPoint - end).getDistance()

            if (dist <= thresholdPx || distToStart <= arrowLen || distToEnd <= arrowLen) {
                return el
            }
        }
    }
    return null
}



