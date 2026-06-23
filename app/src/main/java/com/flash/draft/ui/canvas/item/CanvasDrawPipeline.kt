package com.flash.draft.ui.canvas.item
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Matrix
import androidx.ink.strokes.*
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.flash.draft.model.CanvasElement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object CanvasProfiler {
    var bgDrawTimeSum = 0f
    var bgDrawCount = 0
    var fgDrawTimeSum = 0f
    var fgDrawCount = 0
    var recomposeCount = 0
}
@Composable
fun CanvasBackgroundDrawPipeline(
    elements: List<CanvasElement>,
    scale: Float,
    offset: Offset,
    selectedConnectorId: String?,
    isDarkTheme: Boolean
) {
    // DO NOT use remember here. We need the latest elements map every time it recomposes.
    // Building the map takes < 1ms for hundreds of elements.
    val elementMap = elements.associateBy { it.id }
    val isPerformanceMode = LocalPerformanceMode.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val drawStart = System.nanoTime()

        // HW-Accelerated Fake Blur Configuration
        val shadowOffset = Offset(4f, 8f)
        val shadowColor = Color.Black

        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(40f / scale, 20f / scale), 0f)
        val dottedEffect = PathEffect.dashPathEffect(floatArrayOf(1f / scale, 24f / scale), 0f)

        // Shared Path object to avoid allocating memory for every arrow
        val sharedPath = Path()

        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            elements.forEach { element ->
                if (element is CanvasElement.Connector) {
                    val connector = element
                    val startEl = elementMap[connector.startId]
                    val endEl = elementMap[connector.endId]
                if (startEl != null && endEl != null) {
                    val centerStart = getCenterPoint(startEl)
                    val centerEnd = getCenterPoint(endEl)
                    if (centerStart != Offset.Unspecified && centerEnd != Offset.Unspecified) {
                        val startDim = getElementDimensions(startEl)
                        val endDim = getElementDimensions(endEl)
                        val start = getRectangleIntersection(centerStart, centerEnd, startDim.width, startDim.height, 25f)
                        val end = getRectangleIntersection(centerEnd, centerStart, endDim.width, endDim.height, 25f)
                        val isSelected = selectedConnectorId == connector.id
                        val drawColor = getAdaptiveColor(connector.color, isDarkTheme)
                        val drawStroke = connector.strokeWidth

                        if (isSelected) {
                            val highlightColor = if (isDarkTheme) Color(0xFF64B5F6).copy(alpha = 0.5f) else Color.Blue.copy(alpha = 0.2f)
                            drawLine(color = highlightColor, start = start, end = end, strokeWidth = drawStroke + 24f, cap = StrokeCap.Round)
                        }
                        
                        val effect = when (connector.lineStyle) {
                            "Dashed" -> dashedEffect
                            "Dotted" -> dottedEffect
                            else -> null
                        }

                        // Native Hardware Accelerated "Fake Blur" (Collapsed single pass)
                        // Far faster than setShadowLayer because it entirely bypasses the GPU's Gaussian rasterization pass.
                        if (!isPerformanceMode) {
                            drawLine(color = shadowColor.copy(alpha = 0.08f), start = start + shadowOffset, end = end + shadowOffset, strokeWidth = drawStroke + 8f, pathEffect = effect, cap = StrokeCap.Round)
                        }
                        
                        // Main Line
                        drawLine(color = drawColor, start = start, end = end, strokeWidth = drawStroke, pathEffect = effect, cap = StrokeCap.Round)

                        fun drawArrow(tip: Offset, base: Offset) {
                            val angle = atan2(tip.y - base.y, tip.x - base.x)
                            val arrowLen = 20f + (drawStroke * 1.5f)
                            val arrowAng = Math.PI / 6
                            
                            // Cache trigonometric calls
                            val cosAng = cos(angle).toFloat()
                            val sinAng = sin(angle).toFloat()
                            val cosMinus = cos(angle - arrowAng).toFloat()
                            val sinMinus = sin(angle - arrowAng).toFloat()
                            val cosPlus = cos(angle + arrowAng).toFloat()
                            val sinPlus = sin(angle + arrowAng).toFloat()

                            when (connector.arrowShape) {
                                "Open" -> {
                                    sharedPath.rewind()
                                    sharedPath.moveTo(tip.x - arrowLen * cosMinus, tip.y - arrowLen * sinMinus)
                                    sharedPath.lineTo(tip.x, tip.y)
                                    sharedPath.lineTo(tip.x - arrowLen * cosPlus, tip.y - arrowLen * sinPlus)
                                    
                                    if (!isPerformanceMode) {
                                        withTransform({ translate(left = shadowOffset.x, top = shadowOffset.y) }) {
                                            drawPath(sharedPath, shadowColor.copy(alpha = 0.08f), style = Stroke(width = drawStroke + 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                        }
                                    }
                                    drawPath(sharedPath, drawColor, style = Stroke(width = drawStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                                "Filled", "Diamond" -> {
                                    sharedPath.rewind()
                                    if (connector.arrowShape == "Filled") {
                                        sharedPath.moveTo(tip.x, tip.y)
                                        sharedPath.lineTo(tip.x - arrowLen * cosMinus, tip.y - arrowLen * sinMinus)
                                        sharedPath.lineTo(tip.x - arrowLen * cosPlus, tip.y - arrowLen * sinPlus)
                                        sharedPath.close()
                                    } else {
                                        val midX = tip.x - (arrowLen / 1.5f) * cosAng
                                        val midY = tip.y - (arrowLen / 1.5f) * sinAng
                                        val endX = tip.x - (arrowLen * 1.3f) * cosAng
                                        val endY = tip.y - (arrowLen * 1.3f) * sinAng
                                        sharedPath.moveTo(tip.x, tip.y)
                                        sharedPath.lineTo(midX - (arrowLen / 2) * sinAng, midY + (arrowLen / 2) * cosAng)
                                        sharedPath.lineTo(endX, endY)
                                        sharedPath.lineTo(midX + (arrowLen / 2) * sinAng, midY - (arrowLen / 2) * cosAng)
                                        sharedPath.close()
                                    }
                                    
                                    if (!isPerformanceMode) {
                                        withTransform({ translate(left = shadowOffset.x, top = shadowOffset.y) }) {
                                            drawPath(sharedPath, shadowColor.copy(alpha = 0.08f), style = Fill)
                                        }
                                    }
                                    drawPath(sharedPath, drawColor, style = Fill)
                                }
                                "Circle" -> {
                                    val center = Offset(tip.x - (arrowLen / 2) * cosAng, tip.y - (arrowLen / 2) * sinAng)
                                    if (!isPerformanceMode) {
                                        drawCircle(color = shadowColor.copy(alpha = 0.08f), radius = (arrowLen / 2) + 4f, center = center + shadowOffset)
                                    }
                                    drawCircle(color = drawColor, radius = arrowLen / 2, center = center)
                                }
                            }
                        }
                        
                        // Draw Arrows (Main + Shadow in one pass)
                        if (connector.arrowDirection == "End" || connector.arrowDirection == "Both") drawArrow(end, start)
                        if (connector.arrowDirection == "Both") drawArrow(start, end)
                    }
                }
                }
            }
        }
        val elapsed = (System.nanoTime() - drawStart) / 1_000_000f
        CanvasProfiler.bgDrawTimeSum += elapsed
        CanvasProfiler.bgDrawCount++
    }
}

@Composable
fun CanvasForegroundDrawPipeline(
    elements: List<CanvasElement>,
    cameraScaleProvider: () -> Float,
    cameraOffsetProvider: () -> Offset,
    isDarkTheme: Boolean,
    selectedGroupIds: Set<String>,
    currentDrawingPath: List<com.flash.draft.model.InkPoint>,
    activeToolType: String,
    activeDrawColor: Color,
    activeAlpha: Float,
    activeStrokeWidth: Float,
    lassoPath: List<Offset>
) {
    val renderer = remember { CanvasStrokeRenderer.create() }
    val isPerformanceMode = LocalPerformanceMode.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val drawStart = System.nanoTime()

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()

            // Render existing sketches (Let GPU handle Viewport Culling)
            elements.forEach { element ->
                if (element is CanvasElement.Sketch && element.xCoords.size > 1) {
                    val sketch = element
                    if (sketch.cachedStroke == null) {
                        val batch = MutableStrokeInputBatch()
                        var lastTime = -1L
                        for (i in sketch.xCoords.indices) {
                            val safeTime = if (sketch.times[i] < lastTime) lastTime else sketch.times[i]
                            lastTime = safeTime
                            batch.addOrThrow(androidx.ink.brush.InputToolType.UNKNOWN, sketch.xCoords[i], sketch.yCoords[i], safeTime, sketch.pressures[i])
                        }
                        
                        val baseColor = getAdaptiveColor(sketch.color, isDarkTheme)
                        val family = when (sketch.toolType) {
                            "Highlighter" -> StockBrushes.highlighterV1
                            "Pencil" -> StockBrushes.markerV1
                            else -> StockBrushes.markerV1
                        }
                        val brushEpsilon = if (isPerformanceMode) 2.0f else 0.1f
                        val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
                            family,
                            baseColor.copy(alpha = sketch.alpha).toArgb(),
                            sketch.strokeWidth,
                            brushEpsilon
                        )
                        sketch.cachedStroke = Stroke(brush, batch)
                    }

                    var strokeToDraw = sketch.cachedStroke
                    val isSelected = selectedGroupIds.contains(sketch.id)
                    if (isSelected && strokeToDraw != null) {
                        val baseColor = getAdaptiveColor(sketch.color, isDarkTheme)
                        val family = when (sketch.toolType) {
                            "Highlighter" -> StockBrushes.highlighterV1
                            "Pencil" -> StockBrushes.markerV1
                            else -> StockBrushes.markerV1
                        }
                        val brushEpsilon = if (isPerformanceMode) 2.0f else 0.1f
                        val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
                            family,
                            baseColor.copy(alpha = 0.5f).toArgb(),
                            sketch.strokeWidth,
                            brushEpsilon
                        )
                        // Reuse the batch from the cached stroke!
                        val inputs = strokeToDraw.inputs
                        strokeToDraw = Stroke(brush, inputs)
                    }

                    if (strokeToDraw != null) {
                        renderer.draw(nativeCanvas, strokeToDraw, android.graphics.Matrix())
                    }
                }
            }

            // Render current drawing path
            if (currentDrawingPath.size > 1) {
                val batch = MutableStrokeInputBatch()
                var lastTime = -1L
                currentDrawingPath.forEach { p ->
                    val safeTime = if (p.timeMillis < lastTime) lastTime else p.timeMillis
                    lastTime = safeTime
                    batch.addOrThrow(androidx.ink.brush.InputToolType.UNKNOWN, p.x, p.y, safeTime, p.pressure)
                }
                
                val baseColor = getAdaptiveColor(activeDrawColor, isDarkTheme)
                val family = when (activeToolType) {
                    "Highlighter" -> StockBrushes.highlighterV1
                    "Pencil" -> StockBrushes.markerV1
                    else -> StockBrushes.markerV1
                }
                val brushEpsilon = if (isPerformanceMode) 2.0f else 0.1f
                val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
                    family,
                    baseColor.copy(alpha = activeAlpha).toArgb(),
                    activeStrokeWidth,
                    brushEpsilon
                )

                val stroke = Stroke(brush, batch)
                renderer.draw(nativeCanvas, stroke, Matrix())
            }
            
            nativeCanvas.restore()
        }

        // Draw lasso path in standard Compose layer (already translated/scaled by parent graphicsLayer)
        if (lassoPath.size > 1) {
            val path = Path().apply { 
                moveTo(lassoPath.first().x, lassoPath.first().y)
                for (i in 1 until lassoPath.size) lineTo(lassoPath[i].x, lassoPath[i].y)
                close() 
            }
            drawPath(path, color = Color.Blue.copy(alpha = 0.1f))
            val cameraScale = cameraScaleProvider()
            drawPath(path, color = Color.Blue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f / cameraScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f / cameraScale, 15f / cameraScale))))
        }
        val elapsed = (System.nanoTime() - drawStart) / 1_000_000f
        CanvasProfiler.fgDrawTimeSum += elapsed
        CanvasProfiler.fgDrawCount++
    }
}
