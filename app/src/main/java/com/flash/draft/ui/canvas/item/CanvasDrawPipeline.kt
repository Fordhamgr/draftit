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
fun CanvasDrawPipeline(
    elements: List<CanvasElement>,
    scale: Float,
    offset: Offset,
    selectedConnectorId: String?,
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

            elements.filterIsInstance<CanvasElement.Sketch>().forEach { sketch ->
                if (sketch.points.size > 1) {
                    val baseColor = getAdaptiveColor(sketch.color, isDarkTheme)
                    val strokeColor = if (selectedGroupIds.contains(sketch.id)) Color.Blue else baseColor.copy(alpha = sketch.alpha)
                    val blend = if (sketch.toolType == "Highlighter") BlendMode.Multiply else BlendMode.SrcOver
                    for (i in 1 until sketch.points.size) {
                        val p1 = sketch.points[i - 1]; val p2 = sketch.points[i]
                        val distance = kotlin.math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y))
                        val targetWidth = (sketch.strokeWidth * (1f / (1f + (distance * 0.05f)))).coerceIn(sketch.strokeWidth * 0.3f, sketch.strokeWidth * 1.5f)
                        drawLine(color = strokeColor, start = p1, end = p2, strokeWidth = targetWidth, cap = StrokeCap.Round, blendMode = blend)
                    }
                }
            }

            if (currentDrawingPath.size > 1) {
                val blend = if (activeToolType == "Highlighter") BlendMode.Multiply else BlendMode.SrcOver
                val activeColor = getAdaptiveColor(activeDrawColor, isDarkTheme).copy(alpha = activeAlpha)
                for (i in 1 until currentDrawingPath.size) {
                    val p1 = currentDrawingPath[i - 1]; val p2 = currentDrawingPath[i]
                    val distance = kotlin.math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y))
                    val targetWidth = (activeStrokeWidth * (1f / (1f + (distance * 0.05f)))).coerceIn(activeStrokeWidth * 0.3f, activeStrokeWidth * 1.5f)
                    drawLine(color = activeColor, start = p1, end = p2, strokeWidth = targetWidth, cap = StrokeCap.Round, blendMode = blend)
                }
            }

            if (lassoPath.size > 1) {
                val path = Path().apply { moveTo(lassoPath.first().x, lassoPath.first().y); for (i in 1 until lassoPath.size) lineTo(lassoPath[i].x, lassoPath[i].y); close() }
                drawPath(path, color = Color.Blue.copy(alpha = 0.1f))
                drawPath(path, color = Color.Blue, style = Stroke(width = 2f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f / scale, 15f / scale))))
            }
        }
    }
}
