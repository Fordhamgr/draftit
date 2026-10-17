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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Matrix
import androidx.ink.strokes.*
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.flash.draft.model.CanvasElement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


fun findElement(elements: List<CanvasElement>, id: String): CanvasElement? {
    for (i in elements.indices) {
        if (elements[i].id == id) return elements[i]
    }
    return null
}

object CanvasProfiler {
    /** Set to true to enable per-frame profiling. False = dead-code-eliminated by compiler. */
    const val ENABLED = false

    var bgDrawTimeSum = 0f
    var bgDrawCount = 0
    var fgDrawTimeSum = 0f
    var fgDrawCount = 0
    var recomposeCount = 0
}
private val CONNECTOR_SHADOW_OFFSET = Offset(4f, 8f)

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSingleConnector(
    connector: CanvasElement.Connector,
    elements: List<CanvasElement>,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    isPerformanceMode: Boolean,
    dashedEffect: PathEffect?,
    dottedEffect: PathEffect?,
    sharedPath: Path = Path()
) {
    val startEl = findElement(elements, connector.startId) ?: return
    val endEl = findElement(elements, connector.endId) ?: return
    val centerStart = getCenterPoint(startEl)
    val centerEnd = getCenterPoint(endEl)
    if (centerStart == Offset.Unspecified || centerEnd == Offset.Unspecified) return

    val startDim = getElementDimensions(startEl)
    val endDim = getElementDimensions(endEl)
    val start = getRectangleIntersection(centerStart, centerEnd, startDim.width, startDim.height, 0f)
    val end = getRectangleIntersection(centerEnd, centerStart, endDim.width, endDim.height, 0f)

    val drawColor = getAdaptiveColor(connector.color, isDarkTheme)
    val drawStroke = connector.strokeWidth
    val shadowOffset = CONNECTOR_SHADOW_OFFSET
    val shadowColor = Color.Black

    val effect = when (connector.lineStyle) {
        "Dashed" -> dashedEffect
        "Dotted" -> dottedEffect
        else -> null
    }

    val angleLine = atan2(end.y - start.y, end.x - start.x)
    val cosA = cos(angleLine).toFloat()
    val sinA = sin(angleLine).toFloat()
    val arrowLen = (16f + drawStroke * 1.5f).coerceAtLeast(15f)
    val arrowHalfWidth = arrowLen * 0.55f
    val distance = kotlin.math.hypot(end.x - start.x, end.y - start.y)
    if (distance < 10f) return // Prevent rendering / inverted arrows when nodes overlap

    val hasStartArrow = connector.arrowDirection == "Start" || connector.arrowDirection == "Both"
    val hasEndArrow = connector.arrowDirection == "End" || connector.arrowDirection == "Both"

    val startRetraction = if (hasStartArrow) {
        when (connector.arrowShape) {
            "Filled", "Diamond" -> arrowLen * 0.90f
            "Circle" -> arrowLen * 0.5f
            "Open" -> drawStroke * 0.5f
            else -> 0f
        }
    } else 0f

    val endRetraction = if (hasEndArrow) {
        when (connector.arrowShape) {
            "Filled", "Diamond" -> arrowLen * 0.90f
            "Circle" -> arrowLen * 0.5f
            "Open" -> drawStroke * 0.5f
            else -> 0f
        }
    } else 0f

    // Clamp retraction so arrows never cross over when nodes are close
    val maxRetract = distance * 0.45f
    val effectiveStartRetract = minOf(startRetraction, maxRetract)
    val effectiveEndRetract = minOf(endRetraction, maxRetract)

    val lineStart = Offset(start.x + effectiveStartRetract * cosA, start.y + effectiveStartRetract * sinA)
    val lineEnd = Offset(end.x - effectiveEndRetract * cosA, end.y - effectiveEndRetract * sinA)

    if (isSelected) {
        val capsulePad = (drawStroke * 0.8f + 8f).coerceAtLeast(10f)
        val capsuleRadius = (drawStroke * 1.5f + 10f).coerceAtLeast(13f)
        val capsuleStart = Offset(start.x - cosA * capsulePad, start.y - sinA * capsulePad)
        val capsuleEnd = Offset(end.x + cosA * capsulePad, end.y + sinA * capsulePad)

        val capsuleFill = if (isDarkTheme) Color(0xFF60A5FA).copy(alpha = 0.32f) else Color(0xFF8EB8FF).copy(alpha = 0.38f)
        val capsuleBorder = if (isDarkTheme) Color(0xFF93C5FD).copy(alpha = 0.85f) else Color(0xFF8EB8FF).copy(alpha = 0.90f)

        // Draw capsule background fill
        drawLine(
            color = capsuleFill,
            start = capsuleStart,
            end = capsuleEnd,
            strokeWidth = capsuleRadius * 2f,
            cap = StrokeCap.Round
        )

        // Draw capsule perimeter border
        val nx = -sinA * capsuleRadius
        val ny = cosA * capsuleRadius
        val angleDeg = (angleLine * 180f / Math.PI.toFloat())

        sharedPath.rewind()
        sharedPath.moveTo(capsuleStart.x + nx, capsuleStart.y + ny)
        sharedPath.lineTo(capsuleEnd.x + nx, capsuleEnd.y + ny)
        sharedPath.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center = capsuleEnd,
                radius = capsuleRadius
            ),
            startAngleDegrees = angleDeg - 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        sharedPath.lineTo(capsuleStart.x - nx, capsuleStart.y - ny)
        sharedPath.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center = capsuleStart,
                radius = capsuleRadius
            ),
            startAngleDegrees = angleDeg + 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        sharedPath.close()

        drawPath(
            path = sharedPath,
            color = capsuleBorder,
            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Native Hardware Accelerated "Fake Blur" (Collapsed single pass)
    if (!isPerformanceMode && !isDarkTheme && !isSelected) {
        drawLine(color = shadowColor.copy(alpha = 0.08f), start = lineStart + shadowOffset, end = lineEnd + shadowOffset, strokeWidth = drawStroke + 8f, pathEffect = effect, cap = StrokeCap.Round)
    }

    // Main Line
    drawLine(color = drawColor, start = lineStart, end = lineEnd, strokeWidth = drawStroke, pathEffect = effect, cap = StrokeCap.Round)

    fun drawArrow(tip: Offset, base: Offset) {
        val angle = atan2(tip.y - base.y, tip.x - base.x)
        val cosT = cos(angle).toFloat()
        val sinT = sin(angle).toFloat()
        val normalX = -sinT * arrowHalfWidth
        val normalY = cosT * arrowHalfWidth
        val baseX = tip.x - cosT * arrowLen
        val baseY = tip.y - sinT * arrowLen

        when (connector.arrowShape) {
            "Open" -> {
                sharedPath.rewind()
                sharedPath.moveTo(baseX + normalX, baseY + normalY)
                sharedPath.lineTo(tip.x, tip.y)
                sharedPath.lineTo(baseX - normalX, baseY - normalY)

                if (!isPerformanceMode && !isDarkTheme && !isSelected) {
                    translate(left = shadowOffset.x, top = shadowOffset.y) {
                        drawPath(sharedPath, shadowColor.copy(alpha = 0.08f), style = Stroke(width = drawStroke + 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                drawPath(sharedPath, drawColor, style = Stroke(width = drawStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "Filled", "Diamond" -> {
                sharedPath.rewind()
                if (connector.arrowShape == "Filled") {
                    sharedPath.moveTo(tip.x, tip.y)
                    sharedPath.lineTo(baseX + normalX, baseY + normalY)
                    sharedPath.lineTo(baseX - normalX, baseY - normalY)
                    sharedPath.close()
                } else {
                    val midX = tip.x - (arrowLen / 1.5f) * cosT
                    val midY = tip.y - (arrowLen / 1.5f) * sinT
                    val endX = tip.x - (arrowLen * 1.3f) * cosT
                    val endY = tip.y - (arrowLen * 1.3f) * sinT
                    sharedPath.moveTo(tip.x, tip.y)
                    sharedPath.lineTo(midX + normalX, midY + normalY)
                    sharedPath.lineTo(endX, endY)
                    sharedPath.lineTo(midX - normalX, midY - normalY)
                    sharedPath.close()
                }

                if (!isPerformanceMode && !isDarkTheme && !isSelected) {
                    translate(left = shadowOffset.x, top = shadowOffset.y) {
                        drawPath(sharedPath, shadowColor.copy(alpha = 0.08f), style = Fill)
                    }
                }
                drawPath(sharedPath, drawColor, style = Fill)
            }
            "Circle" -> {
                val center = Offset(tip.x - (arrowLen / 2) * cosT, tip.y - (arrowLen / 2) * sinT)
                if (!isPerformanceMode && !isDarkTheme && !isSelected) {
                    drawCircle(color = shadowColor.copy(alpha = 0.08f), radius = (arrowLen / 2) + 4f, center = center + shadowOffset)
                }
                drawCircle(color = drawColor, radius = arrowLen / 2, center = center)
            }
        }
    }

    if (hasEndArrow) drawArrow(end, start)
    if (hasStartArrow) drawArrow(start, end)
}

@Composable
fun ConnectorItem(
    connector: CanvasElement.Connector,
    elements: List<CanvasElement>,
    isSelected: Boolean,
    isDarkTheme: Boolean
) {
    val isPerformanceMode = LocalPerformanceMode.current
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(40f, 20f), 0f) }
    val dottedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(1f, 24f), 0f) }
    val sharedPath = remember { Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawSingleConnector(
            connector = connector,
            elements = elements,
            isSelected = isSelected,
            isDarkTheme = isDarkTheme,
            isPerformanceMode = isPerformanceMode,
            dashedEffect = dashedEffect,
            dottedEffect = dottedEffect,
            sharedPath = sharedPath
        )
    }
}

@Composable
fun BacksideConnectorsLayer(
    elements: List<CanvasElement>,
    selectedConnectorId: String?,
    isDarkTheme: Boolean
) {
    val isPerformanceMode = LocalPerformanceMode.current
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(40f, 20f), 0f) }
    val dottedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(1f, 24f), 0f) }
    val sharedPath = remember { Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Pass 1: Draw all non-selected connectors
        for (i in elements.indices) {
            val el = elements[i]
            if (el is CanvasElement.Connector && el.id != selectedConnectorId) {
                drawSingleConnector(
                    connector = el,
                    elements = elements,
                    isSelected = false,
                    isDarkTheme = isDarkTheme,
                    isPerformanceMode = isPerformanceMode,
                    dashedEffect = dashedEffect,
                    dottedEffect = dottedEffect,
                    sharedPath = sharedPath
                )
            }
        }

        // Pass 2: Draw the selected connector (if any) on top with its halo capsule
        if (selectedConnectorId != null) {
            for (i in elements.indices) {
                val el = elements[i]
                if (el is CanvasElement.Connector && el.id == selectedConnectorId) {
                    drawSingleConnector(
                        connector = el,
                        elements = elements,
                        isSelected = true,
                        isDarkTheme = isDarkTheme,
                        isPerformanceMode = isPerformanceMode,
                        dashedEffect = dashedEffect,
                        dottedEffect = dottedEffect,
                        sharedPath = sharedPath
                    )
                    break
                }
            }
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
    inFlightStrokeBatch: androidx.ink.strokes.MutableStrokeInputBatch?,
    inFlightStrokeCount: Int,
    activeToolType: String,
    activeDrawColor: Color,
    activeAlpha: Float,
    activeStrokeWidth: Float,
    lassoPath: List<Offset>
) {
    val renderer = remember { CanvasStrokeRenderer.create() }
    val isPerformanceMode = LocalPerformanceMode.current

    // Cache a single Matrix instance to avoid allocating one per-sketch per-frame.
    // Previously: `renderer.draw(nativeCanvas, stroke, Matrix())` allocated N matrices per frame.
    val identityMatrix = remember { Matrix() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val drawStart = if (CanvasProfiler.ENABLED) System.nanoTime() else 0L

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            // Removed no-op nativeCanvas.save()/restore() — no transform is applied here
            // (scale=1f, offset=Zero), so the save/restore was pure overhead.

            // Render existing sketches (Let GPU handle Viewport Culling)
            for (i in elements.indices) {
            val element = elements[i]
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
                        // Reuse cached identityMatrix instead of allocating new Matrix() per sketch
                        renderer.draw(nativeCanvas, strokeToDraw, identityMatrix)
                    }
                }
            }

            // Render current drawing path
            if (inFlightStrokeCount > 1 && inFlightStrokeBatch != null) {
                val batch = inFlightStrokeBatch
                
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
                // Reuse cached identityMatrix
                renderer.draw(nativeCanvas, stroke, identityMatrix)
            }
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

        if (CanvasProfiler.ENABLED) {
            val elapsed = (System.nanoTime() - drawStart) / 1_000_000f
            CanvasProfiler.fgDrawTimeSum += elapsed
            CanvasProfiler.fgDrawCount++
        }
    }
}
