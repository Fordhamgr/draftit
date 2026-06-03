package com.flash.draft.ui.canvas.item
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.flash.draft.model.CanvasElement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CanvasBackgroundDrawPipeline(
    elements: List<CanvasElement>,
    scale: Float,
    offset: Offset,
    selectedConnectorId: String?,
    isDarkTheme: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            elements.filterIsInstance<CanvasElement.Connector>().forEach { connector ->
                val centerStart = getCenterPoint(connector.startId, elements)
                val centerEnd = getCenterPoint(connector.endId, elements)
                if (centerStart != null && centerEnd != null) {
                    val (startW, startH) = getElementDimensions(connector.startId, elements)
                    val (endW, endH) = getElementDimensions(connector.endId, elements)
                    val start = getRectangleIntersection(centerStart, centerEnd, startW, startH, 25f)
                    val end = getRectangleIntersection(centerEnd, centerStart, endW, endH, 25f)
                    val isSelected = selectedConnectorId == connector.id
                    val drawColor = getAdaptiveColor(connector.color, isDarkTheme)
                    val drawStroke = connector.strokeWidth

                    if (isSelected) drawLine(color = Color.Blue.copy(alpha = 0.2f), start = start, end = end, strokeWidth = drawStroke + 24f, cap = StrokeCap.Round)
                    val effect = when (connector.lineStyle) {
                        "Dashed" -> PathEffect.dashPathEffect(floatArrayOf(40f / scale, 20f / scale), 0f)
                        "Dotted" -> PathEffect.dashPathEffect(floatArrayOf(1f / scale, 24f / scale), 0f)
                        else -> null
                    }
                    drawLine(color = drawColor, start = start, end = end, strokeWidth = drawStroke, pathEffect = effect, cap = StrokeCap.Round)

                    fun drawArrow(tip: Offset, base: Offset) {
                        val angle = atan2(tip.y - base.y, tip.x - base.x)
                        val arrowLen = 20f + (drawStroke * 1.5f)
                        val arrowAng = Math.PI / 6
                        when (connector.arrowShape) {
                            "Open" -> {
                                val path = Path().apply {
                                    moveTo(tip.x - arrowLen * cos(angle - arrowAng).toFloat(), tip.y - arrowLen * sin(angle - arrowAng).toFloat())
                                    lineTo(tip.x, tip.y)
                                    lineTo(tip.x - arrowLen * cos(angle + arrowAng).toFloat(), tip.y - arrowLen * sin(angle + arrowAng).toFloat())
                                }
                                drawPath(path, drawColor, style = Stroke(width = drawStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                            "Filled" -> {
                                val path = Path().apply {
                                    moveTo(tip.x, tip.y)
                                    lineTo(tip.x - arrowLen * cos(angle - arrowAng).toFloat(), tip.y - arrowLen * sin(angle - arrowAng).toFloat())
                                    lineTo(tip.x - arrowLen * cos(angle + arrowAng).toFloat(), tip.y - arrowLen * sin(angle + arrowAng).toFloat())
                                    close()
                                }
                                drawPath(path, drawColor, style = Fill)
                            }
                            "Diamond" -> {
                                val midX = tip.x - (arrowLen / 1.5f) * cos(angle).toFloat()
                                val midY = tip.y - (arrowLen / 1.5f) * sin(angle).toFloat()
                                val endX = tip.x - (arrowLen * 1.3f) * cos(angle).toFloat()
                                val endY = tip.y - (arrowLen * 1.3f) * sin(angle).toFloat()
                                val path = Path().apply {
                                    moveTo(tip.x, tip.y)
                                    lineTo(midX - (arrowLen / 2) * sin(angle).toFloat(), midY + (arrowLen / 2) * cos(angle).toFloat())
                                    lineTo(endX, endY)
                                    lineTo(midX + (arrowLen / 2) * sin(angle).toFloat(), midY - (arrowLen / 2) * cos(angle).toFloat())
                                    close()
                                }
                                drawPath(path, drawColor, style = Fill)
                            }
                            "Circle" -> drawCircle(color = drawColor, radius = arrowLen / 2, center = Offset(tip.x - (arrowLen / 2) * cos(angle).toFloat(), tip.y - (arrowLen / 2) * sin(angle).toFloat()))
                        }
                    }
                    if (connector.arrowDirection == "End" || connector.arrowDirection == "Both") drawArrow(end, start)
                    if (connector.arrowDirection == "Both") drawArrow(start, end)
                }
            }
        }
    }
}

@Composable
fun CanvasForegroundDrawPipeline(
    elements: List<CanvasElement>,
    scale: Float,
    offset: Offset,
    isDarkTheme: Boolean,
    selectedGroupIds: Set<String>,
    currentDrawingPath: List<Offset>,
    activeToolType: String,
    activeDrawColor: Color,
    activeAlpha: Float,
    activeStrokeWidth: Float,
    lassoPath: List<Offset>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            // Helper function to build a smooth quadratic bezier path
            fun buildSmoothPath(points: List<Offset>): Path {
                val path = Path()
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val midX = (p1.x + p2.x) / 2
                        val midY = (p1.y + p2.y) / 2
                        path.quadraticTo(p1.x, p1.y, midX, midY)
                    }
                    path.lineTo(points.last().x, points.last().y)
                }
                return path
            }

            elements.filterIsInstance<CanvasElement.Sketch>().forEach { sketch ->
                if (sketch.points.size > 1) {
                    val path = buildSmoothPath(sketch.points)
                    
                    val baseColor = getAdaptiveColor(sketch.color, isDarkTheme)
                    val isSelected = selectedGroupIds.contains(sketch.id)
                    
                    // Tool-specific properties
                    val (strokeColor, blendMode, width) = when (sketch.toolType) {
                        "Highlighter" -> {
                            val c = if (isSelected) Color.Blue.copy(alpha = 0.5f) else baseColor.copy(alpha = 0.4f)
                            Triple(c, BlendMode.Multiply, sketch.strokeWidth * 3f)
                        }
                        "Pencil" -> {
                            val c = if (isSelected) Color.Blue else baseColor.copy(alpha = sketch.alpha * 0.4f)
                            Triple(c, BlendMode.SrcOver, sketch.strokeWidth * 0.3f)
                        }
                        else -> { // Pen
                            val c = if (isSelected) Color.Blue else baseColor.copy(alpha = sketch.alpha)
                            Triple(c, BlendMode.SrcOver, sketch.strokeWidth)
                        }
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = blendMode
                    )
                }
            }

            if (currentDrawingPath.size > 1) {
                val path = buildSmoothPath(currentDrawingPath)
                val baseColor = getAdaptiveColor(activeDrawColor, isDarkTheme)
                
                val (strokeColor, blendMode, width) = when (activeToolType) {
                    "Highlighter" -> Triple(baseColor.copy(alpha = 0.4f), BlendMode.Multiply, activeStrokeWidth * 3f)
                    "Pencil" -> Triple(baseColor.copy(alpha = activeAlpha * 0.4f), BlendMode.SrcOver, activeStrokeWidth * 0.3f)
                    else -> Triple(baseColor.copy(alpha = activeAlpha), BlendMode.SrcOver, activeStrokeWidth) // Pen
                }

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = blendMode
                )
            }

            if (lassoPath.size > 1) {
                val path = Path().apply { 
                    moveTo(lassoPath.first().x, lassoPath.first().y)
                    for (i in 1 until lassoPath.size) lineTo(lassoPath[i].x, lassoPath[i].y)
                    close() 
                }
                drawPath(path, color = Color.Blue.copy(alpha = 0.1f))
                drawPath(path, color = Color.Blue, style = Stroke(width = 2f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f / scale, 15f / scale))))
            }
        }
    }
}
