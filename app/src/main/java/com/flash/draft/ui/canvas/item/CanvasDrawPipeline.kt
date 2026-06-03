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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

                    val shadowOffset = Offset(8f, 16f)
                    val shadowColor = Color.Black.copy(alpha = 0.15f)

                    if (isSelected) drawLine(color = Color.Blue.copy(alpha = 0.2f), start = start, end = end, strokeWidth = drawStroke + 24f, cap = StrokeCap.Round)
                    val effect = when (connector.lineStyle) {
                        "Dashed" -> PathEffect.dashPathEffect(floatArrayOf(40f / scale, 20f / scale), 0f)
                        "Dotted" -> PathEffect.dashPathEffect(floatArrayOf(1f / scale, 24f / scale), 0f)
                        else -> null
                    }
                    
                    val shadowStrokePaint = androidx.compose.ui.graphics.Paint().apply {
                        color = shadowColor
                        style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                        strokeWidth = drawStroke
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        pathEffect = effect
                        asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(12f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                    val shadowFillPaint = androidx.compose.ui.graphics.Paint().apply {
                        color = shadowColor
                        style = androidx.compose.ui.graphics.PaintingStyle.Fill
                        asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(12f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }

                    // Draw Line Shadow
                    drawIntoCanvas { canvas ->
                        val linePath = Path().apply {
                            moveTo(start.x + shadowOffset.x, start.y + shadowOffset.y)
                            lineTo(end.x + shadowOffset.x, end.y + shadowOffset.y)
                        }
                        canvas.drawPath(linePath, shadowStrokePaint)
                    }
                    // Draw Line Main
                    drawLine(color = drawColor, start = start, end = end, strokeWidth = drawStroke, pathEffect = effect, cap = StrokeCap.Round)

                    fun drawArrow(tip: Offset, base: Offset, isShadow: Boolean = false) {
                        val angle = atan2(tip.y - base.y, tip.x - base.x)
                        val arrowLen = 20f + (drawStroke * 1.5f)
                        val arrowAng = Math.PI / 6
                        
                        val tX = if (isShadow) tip.x + shadowOffset.x else tip.x
                        val tY = if (isShadow) tip.y + shadowOffset.y else tip.y
                        val colorToDraw = if (isShadow) shadowColor else drawColor

                        when (connector.arrowShape) {
                            "Open" -> {
                                val path = Path().apply {
                                    moveTo(tX - arrowLen * cos(angle - arrowAng).toFloat(), tY - arrowLen * sin(angle - arrowAng).toFloat())
                                    lineTo(tX, tY)
                                    lineTo(tX - arrowLen * cos(angle + arrowAng).toFloat(), tY - arrowLen * sin(angle + arrowAng).toFloat())
                                }
                                if (isShadow) {
                                    drawIntoCanvas { it.drawPath(path, shadowStrokePaint) }
                                } else {
                                    drawPath(path, colorToDraw, style = Stroke(width = drawStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                            }
                            "Filled", "Diamond" -> {
                                val path = Path().apply {
                                    if (connector.arrowShape == "Filled") {
                                        moveTo(tX, tY)
                                        lineTo(tX - arrowLen * cos(angle - arrowAng).toFloat(), tY - arrowLen * sin(angle - arrowAng).toFloat())
                                        lineTo(tX - arrowLen * cos(angle + arrowAng).toFloat(), tY - arrowLen * sin(angle + arrowAng).toFloat())
                                        close()
                                    } else {
                                        val midX = tX - (arrowLen / 1.5f) * cos(angle).toFloat()
                                        val midY = tY - (arrowLen / 1.5f) * sin(angle).toFloat()
                                        val endX = tX - (arrowLen * 1.3f) * cos(angle).toFloat()
                                        val endY = tY - (arrowLen * 1.3f) * sin(angle).toFloat()
                                        moveTo(tX, tY)
                                        lineTo(midX - (arrowLen / 2) * sin(angle).toFloat(), midY + (arrowLen / 2) * cos(angle).toFloat())
                                        lineTo(endX, endY)
                                        lineTo(midX + (arrowLen / 2) * sin(angle).toFloat(), midY - (arrowLen / 2) * cos(angle).toFloat())
                                        close()
                                    }
                                }
                                if (isShadow) {
                                    drawIntoCanvas { it.drawPath(path, shadowFillPaint) }
                                } else {
                                    drawPath(path, colorToDraw, style = Fill)
                                }
                            }
                            "Circle" -> {
                                val center = Offset(tX - (arrowLen / 2) * cos(angle).toFloat(), tY - (arrowLen / 2) * sin(angle).toFloat())
                                if (isShadow) {
                                    val path = Path().apply {
                                        addOval(androidx.compose.ui.geometry.Rect(center = center, radius = arrowLen / 2))
                                    }
                                    drawIntoCanvas { it.drawPath(path, shadowFillPaint) }
                                } else {
                                    drawCircle(color = colorToDraw, radius = arrowLen / 2, center = center)
                                }
                            }
                        }
                    }
                    
                    // Draw Arrow Shadows
                    if (connector.arrowDirection == "End" || connector.arrowDirection == "Both") drawArrow(end, start, isShadow = true)
                    if (connector.arrowDirection == "Both") drawArrow(start, end, isShadow = true)
                    
                    // Draw Arrow Main
                    if (connector.arrowDirection == "End" || connector.arrowDirection == "Both") drawArrow(end, start, isShadow = false)
                    if (connector.arrowDirection == "Both") drawArrow(start, end, isShadow = false)
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
