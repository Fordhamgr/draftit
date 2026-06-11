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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val drawStart = System.nanoTime()

        // 1. ALLOCATE ALL HEAVY OBJECTS OUTSIDE THE DRAW LOOP
        val shadowOffset = Offset(8f, 16f)
        val shadowColor = Color.Black.copy(alpha = 0.15f)
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(40f / scale, 20f / scale), 0f)
        val dottedEffect = PathEffect.dashPathEffect(floatArrayOf(1f / scale, 24f / scale), 0f)

        // Shared Path object to avoid allocating memory for every arrow
        val sharedPath = Path()

        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            elements.filterIsInstance<CanvasElement.Connector>().forEach { connector ->
                val startEl = elementMap[connector.startId]
                val endEl = elementMap[connector.endId]
                if (startEl != null && endEl != null) {
                    val centerStart = getCenterPoint(startEl)
                    val centerEnd = getCenterPoint(endEl)
                    if (centerStart != null && centerEnd != null) {
                        val (startW, startH) = getElementDimensions(startEl)
                        val (endW, endH) = getElementDimensions(endEl)
                        val start = getRectangleIntersection(centerStart, centerEnd, startW, startH, 25f)
                        val end = getRectangleIntersection(centerEnd, centerStart, endW, endH, 25f)
                        val isSelected = selectedConnectorId == connector.id
                        val drawColor = getAdaptiveColor(connector.color, isDarkTheme)
                        val drawStroke = connector.strokeWidth

                        if (isSelected) drawLine(color = Color.Blue.copy(alpha = 0.2f), start = start, end = end, strokeWidth = drawStroke + 24f, cap = StrokeCap.Round)
                        
                        val effect = when (connector.lineStyle) {
                            "Dashed" -> dashedEffect
                            "Dotted" -> dottedEffect
                            else -> null
                        }

                        // Draw Line Shadow (Native Compose is hardware accelerated, much faster than BlurMaskFilter)
                        drawLine(color = shadowColor, start = start + shadowOffset, end = end + shadowOffset, strokeWidth = drawStroke, pathEffect = effect, cap = StrokeCap.Round)
                        
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
                                    sharedPath.rewind()
                                    sharedPath.moveTo(tX - arrowLen * cos(angle - arrowAng).toFloat(), tY - arrowLen * sin(angle - arrowAng).toFloat())
                                    sharedPath.lineTo(tX, tY)
                                    sharedPath.lineTo(tX - arrowLen * cos(angle + arrowAng).toFloat(), tY - arrowLen * sin(angle + arrowAng).toFloat())
                                    
                                    drawPath(sharedPath, colorToDraw, style = Stroke(width = drawStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                                "Filled", "Diamond" -> {
                                    sharedPath.rewind()
                                    if (connector.arrowShape == "Filled") {
                                        sharedPath.moveTo(tX, tY)
                                        sharedPath.lineTo(tX - arrowLen * cos(angle - arrowAng).toFloat(), tY - arrowLen * sin(angle - arrowAng).toFloat())
                                        sharedPath.lineTo(tX - arrowLen * cos(angle + arrowAng).toFloat(), tY - arrowLen * sin(angle + arrowAng).toFloat())
                                        sharedPath.close()
                                    } else {
                                        val midX = tX - (arrowLen / 1.5f) * cos(angle).toFloat()
                                        val midY = tY - (arrowLen / 1.5f) * sin(angle).toFloat()
                                        val endX = tX - (arrowLen * 1.3f) * cos(angle).toFloat()
                                        val endY = tY - (arrowLen * 1.3f) * sin(angle).toFloat()
                                        sharedPath.moveTo(tX, tY)
                                        sharedPath.lineTo(midX - (arrowLen / 2) * sin(angle).toFloat(), midY + (arrowLen / 2) * cos(angle).toFloat())
                                        sharedPath.lineTo(endX, endY)
                                        sharedPath.lineTo(midX + (arrowLen / 2) * sin(angle).toFloat(), midY - (arrowLen / 2) * cos(angle).toFloat())
                                        sharedPath.close()
                                    }
                                    drawPath(sharedPath, colorToDraw, style = Fill)
                                }
                                "Circle" -> {
                                    val center = Offset(tX - (arrowLen / 2) * cos(angle).toFloat(), tY - (arrowLen / 2) * sin(angle).toFloat())
                                    drawCircle(color = colorToDraw, radius = arrowLen / 2, center = center)
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
        val elapsed = (System.nanoTime() - drawStart) / 1_000_000f
        if (elapsed > 2f) {
            android.util.Log.d("DraftCanvasPerf", "CanvasBackgroundDrawPipeline took ${elapsed}ms")
        }
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val drawStart = System.nanoTime()

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()

            // Render existing sketches (Let GPU handle Viewport Culling)
            elements.filterIsInstance<CanvasElement.Sketch>().forEach { sketch ->
                if (sketch.xCoords.size > 1) {
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
                        val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
                            family,
                            baseColor.copy(alpha = sketch.alpha).toArgb(),
                            sketch.strokeWidth,
                            0.1f
                        )
                        sketch.cachedStroke = Stroke(brush, batch)
                    }

                    var strokeToDraw = sketch.cachedStroke
                    val isSelected = selectedGroupIds.contains(sketch.id)
                    if (isSelected && strokeToDraw != null) {
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
                        val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
                            family,
                            baseColor.copy(alpha = 0.5f).toArgb(),
                            sketch.strokeWidth,
                            0.1f
                        )
                        strokeToDraw = Stroke(brush, batch)
                    }
                    if (strokeToDraw != null) {
                        renderer.draw(nativeCanvas, strokeToDraw, Matrix())
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
                val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
                    family,
                    baseColor.copy(alpha = activeAlpha).toArgb(),
                    activeStrokeWidth,
                    0.1f
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
        if (elapsed > 2f) {
            android.util.Log.d("DraftCanvasPerf", "CanvasForegroundDrawPipeline took ${elapsed}ms")
        }
    }
}
