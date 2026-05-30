package com.flash.draft

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DraftCanvas(
    initialElements: List<CanvasElement>,
    onSave: (List<CanvasElement>) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var centerX by remember { mutableStateOf(0f) }
    var centerY by remember { mutableStateOf(0f) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }

    // Modes
    var isDrawingMode by remember { mutableStateOf(false) }
    var currentDrawingPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var isConnectingMode by remember { mutableStateOf(false) }
    var connectingStartNodeId by remember { mutableStateOf<String?>(null) }

    var isLassoMode by remember { mutableStateOf(false) }
    var lassoPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // NEW: Pen Studio State
    var activeDrawColor by remember { mutableStateOf(Color.Black) }
    var activeStrokeWidth by remember { mutableStateOf(6f) }
    var activeToolType by remember { mutableStateOf("Pen") }
    var activeAlpha by remember { mutableStateOf(1f) }

    val focusManager = LocalFocusManager.current
    val elements = remember { mutableStateListOf<CanvasElement>().apply { addAll(initialElements) } }
    val context = LocalContext.current

    fun getFileInfo(uri: android.net.Uri): Pair<String, String> {
        var name = "Document.pdf"
        var size = "Unknown Size"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
                val sizeBytes = cursor.getLong(sizeIndex)
                size = "${sizeBytes / 1024} kB"
            }
        }
        return Pair(name, size)
    }

    // 2. USED HERE SECOND
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val (fileName, fileSize) = getFileInfo(uri) // Error is gone!
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)

            elements.add(
                CanvasElement.PdfCard(
                    id = UUID.randomUUID().toString(),
                    position = dropPos,
                    uriString = uri.toString(),
                    fileName = fileName,
                    fileSize = fileSize
                )
            )
            onSave(elements)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
            elements.add(CanvasElement.ImageCard(id = UUID.randomUUID().toString(), position = dropPos, uriString = uri.toString()))
            isDrawingMode = false
            isConnectingMode = false
            isLassoMode = false
            onSave(elements)
        }
    }

    fun bringToFront(noteId: String) {
        val index = elements.indexOfFirst { it.id == noteId }
        if (index > -1 && index != elements.lastIndex) {
            val note = elements.removeAt(index)
            elements.add(note)
        }
    }

    fun getCenterPoint(elementId: String): Offset? {
        val el = elements.find { it.id == elementId }
        return when (el) {
            is CanvasElement.StickyNote -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
            is CanvasElement.ImageCard -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
            is CanvasElement.PdfCard -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
            is CanvasElement.CodeBlock -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
            is CanvasElement.AudioNote -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
            is CanvasElement.ShapeNode -> Offset(el.position.x + (el.width / 2), el.position.y + (el.height / 2))
            else -> null
        }
    }

    fun moveGroup(delta: Offset, triggerElementId: String) {
        val idsToMove = if (selectedGroupIds.contains(triggerElementId)) selectedGroupIds else setOf(triggerElementId)
        idsToMove.forEach { id ->
            val idx = elements.indexOfFirst { it.id == id }
            if (idx != -1) {
                val el = elements[idx]
                if (el is CanvasElement.StickyNote) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.ImageCard) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.PdfCard) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.Sketch) elements[idx] = el.copy(points = el.points.map { it + delta })
                if (el is CanvasElement.CodeBlock) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.AudioNote) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.ShapeNode) elements[idx] = el.copy(position = el.position + delta)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
            .onSizeChanged { size ->
                centerX = size.width / 2f
                centerY = size.height / 2f
                if (offset == Offset.Zero) offset = Offset(centerX, centerY)
            }
            .pointerInput(isDrawingMode, isConnectingMode, isLassoMode) {
                if (isDrawingMode || isLassoMode) {
                    detectDragGestures(
                        onDragStart = { startPos ->
                            if (isLassoMode) {
                                selectedGroupIds = emptySet()
                                lassoPath = listOf(ViewportMath.screenToWorld(startPos, offset, scale))
                            } else {
                                currentDrawingPath = listOf(ViewportMath.screenToWorld(startPos, offset, scale))
                            }
                        },
                        onDrag = { change, _ ->
                            if (isLassoMode) {
                                lassoPath = lassoPath + ViewportMath.screenToWorld(change.position, offset, scale)
                            } else {
                                currentDrawingPath = currentDrawingPath + ViewportMath.screenToWorld(change.position, offset, scale)
                            }
                        },
                        onDragEnd = {
                            if (isLassoMode && lassoPath.size > 2) {
                                val capturedIds = mutableSetOf<String>()
                                elements.forEach { el ->
                                    val center = getCenterPoint(el.id)
                                    if (center != null && ViewportMath.isPointInPolygon(center, lassoPath)) {
                                        capturedIds.add(el.id)
                                    } else if (el is CanvasElement.Sketch && ViewportMath.isPointInPolygon(el.points.first(), lassoPath)) {
                                        capturedIds.add(el.id)
                                    }
                                }
                                selectedGroupIds = capturedIds
                                lassoPath = emptyList()
                                isLassoMode = false
                            } else if (currentDrawingPath.size > 1) {
                                // NEW: Save stroke with active color and thickness selections
                                elements.add(
                                    CanvasElement.Sketch(
                                        id = UUID.randomUUID().toString(),
                                        points = currentDrawingPath,
                                        color = activeDrawColor,
                                        strokeWidth = activeStrokeWidth,
                                        toolType = activeToolType,
                                        alpha = activeAlpha
                                    )
                                )
                                onSave(elements)
                                currentDrawingPath = emptyList()
                            }
                        }
                    )
                } else if (!isConnectingMode) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val (newScale, newOffset) = ViewportMath.calculateCameraTransform(scale, offset, centroid, pan, zoom)
                        scale = newScale
                        offset = newOffset
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    selectedNoteId = null
                    connectingStartNodeId = null
                    selectedGroupIds = emptySet()
                    focusManager.clearFocus()
                })
            }
    ) {
        GridBackground(scale = scale, offset = offset)

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({
                translate(left = offset.x, top = offset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                elements.filterIsInstance<CanvasElement.Connector>().forEach { connector ->
                    val start = getCenterPoint(connector.startId)
                    val end = getCenterPoint(connector.endId)
                    if (start != null && end != null) {
                        // Apply Dashed or Dotted effects
                        val effect = when (connector.lineStyle) {
                            "Dashed" -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
                            "Dotted" -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 15f), 0f)
                            else -> null
                        }

                        drawLine(
                            color = connector.color, start = start, end = end,
                            strokeWidth = connector.strokeWidth,
                            pathEffect = effect
                        )

                        // Function to draw an arrowhead
                        fun drawArrowHead(tip: Offset, base: Offset) {
                            val angle = kotlin.math.atan2(tip.y - base.y, tip.x - base.x)
                            val arrowLength = 30f
                            val arrowAngle = Math.PI / 6 // 30 degrees
                            val arrowPath = Path().apply {
                                moveTo(tip.x, tip.y)
                                lineTo(tip.x - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat(), tip.y - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat())
                                moveTo(tip.x, tip.y)
                                lineTo(tip.x - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat(), tip.y - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat())
                            }
                            drawPath(path = arrowPath, color = connector.color, style = Stroke(width = connector.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }

                        if (connector.arrowStyle == "End" || connector.arrowStyle == "Both") drawArrowHead(end, start)
                        if (connector.arrowStyle == "Both") drawArrowHead(start, end)
                    }
                }
                fun generateSmoothPath(points: List<Offset>): Path {
                    val path = Path()
                    if (points.isNotEmpty()) {
                        path.moveTo(points.first().x, points.first().y)
                        var currentX = points.first().x
                        var currentY = points.first().y
                        for (i in 1 until points.size) {
                            val nextX = points[i].x
                            val nextY = points[i].y
                            val midX = (currentX + nextX) / 2
                            val midY = (currentY + nextY) / 2
                            if (i == 1) {
                                path.lineTo(midX, midY)
                            } else {
                                path.quadraticBezierTo(currentX, currentY, midX, midY)
                            }
                            currentX = nextX
                            currentY = nextY
                        }
                        path.lineTo(currentX, currentY)
                    }
                    return path
                }

                elements.filterIsInstance<CanvasElement.Sketch>().forEach { sketch ->
                    if (sketch.points.size > 1) {
                        val path = generateSmoothPath(sketch.points)
                        val strokeColor = if (selectedGroupIds.contains(sketch.id)) Color.Blue else sketch.color.copy(alpha = sketch.alpha)
                        val blend = if (sketch.toolType == "Highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.BlendMode.SrcOver

                        drawPath(path = path, color = strokeColor, blendMode = blend, style = Stroke(width = sketch.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                // Render Live Drawing Preview
                if (currentDrawingPath.size > 1) {
                    val path = generateSmoothPath(currentDrawingPath)
                    val blend = if (activeToolType == "Highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.BlendMode.SrcOver
                    drawPath(path = path, color = activeDrawColor.copy(alpha = activeAlpha), blendMode = blend, style = Stroke(width = activeStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
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

        // Render Cards (Notes & Images)
        elements.toList().forEach { element ->
            if (element is CanvasElement.StickyNote) {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    StickyNoteItem(
                        note = element, scale = scale, offset = offset,
                        isSelected = selectedNoteId == element.id && selectedGroupIds.isEmpty(),
                        isConnecting = isGroupedOrConnecting,
                        onUpdate = { updatedNote ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) elements[currentIndex] = updatedNote
                        },
                        onPositionDelta = { delta -> moveGroup(delta, element.id) },
                        onSizeDelta = { delta ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.StickyNote
                                val newWidth = (el.width + delta.x).coerceAtLeast(100f)
                                val newHeight = (el.height + delta.y).coerceAtLeast(100f)
                                elements[currentIndex] = el.copy(width = newWidth, height = newHeight)
                            }
                        },
                        onSaveRequest = { onSave(elements) },
                        onDelete = {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            selectedNoteId = null
                            onSave(elements)
                        },
                        onSelect = {
                            if (isConnectingMode) {
                                if (connectingStartNodeId == null) {
                                    connectingStartNodeId = element.id
                                } else if (connectingStartNodeId != element.id) {
                                    elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = element.id))
                                    onSave(elements)
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                }
                            } else if (!isDrawingMode && !isLassoMode) {
                                if (!selectedGroupIds.contains(element.id)) {
                                    selectedGroupIds = emptySet()
                                    selectedNoteId = element.id
                                    bringToFront(element.id)
                                }
                            }
                        }
                    )
                }
            }
            if (element is CanvasElement.CodeBlock) {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    CodeSnippetItem(
                        codeBlock = element, scale = scale, offset = offset,
                        isSelected = selectedNoteId == element.id && selectedGroupIds.isEmpty(),
                        isConnecting = isGroupedOrConnecting,
                        onUpdate = { updatedBlock ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) elements[currentIndex] = updatedBlock
                        },
                        onPositionDelta = { delta -> moveGroup(delta, element.id) },
                        onSizeDelta = { delta ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.CodeBlock
                                val newWidth = (el.width + delta.x).coerceAtLeast(150f)
                                val newHeight = (el.height + delta.y).coerceAtLeast(100f)
                                elements[currentIndex] = el.copy(width = newWidth, height = newHeight)
                            }
                        },
                        onSaveRequest = { onSave(elements) },
                        onDelete = {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            selectedNoteId = null
                            onSave(elements)
                        },
                        onSelect = {
                            if (isConnectingMode) {
                                if (connectingStartNodeId == null) { connectingStartNodeId = element.id }
                                else if (connectingStartNodeId != element.id) {
                                    elements.add(CanvasElement.Connector(id = java.util.UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = element.id))
                                    onSave(elements)
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                }
                            } else if (!isDrawingMode && !isLassoMode) {
                                if (!selectedGroupIds.contains(element.id)) {
                                    selectedGroupIds = emptySet()
                                    selectedNoteId = element.id
                                    bringToFront(element.id)
                                }
                            }
                        }
                    )
                }
            }
            if (element is CanvasElement.ImageCard) {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    ImageItem(
                        imageCard = element, scale = scale, offset = offset,
                        isSelected = selectedNoteId == element.id && selectedGroupIds.isEmpty(),
                        isConnecting = isGroupedOrConnecting,
                        onUpdate = { updatedImage ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) elements[currentIndex] = updatedImage
                        },
                        onPositionDelta = { delta -> moveGroup(delta, element.id) },
                        onSizeDelta = { delta ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.ImageCard
                                val newWidth = (el.width + delta.x).coerceAtLeast(100f)
                                val newHeight = (el.height + delta.y).coerceAtLeast(100f)
                                elements[currentIndex] =
                                    el.copy(width = newWidth, height = newHeight)
                            }
                        },
                        onDuplicate = {
                            // Create a copy slightly offset so it doesn't perfectly overlap
                            val duplicate = element.copy(
                                id = UUID.randomUUID().toString(),
                                position = element.position + Offset(40f, 40f)
                            )
                            elements.add(duplicate)
                            onSave(elements)
                        },
                        onMakeSticker = {
                            android.widget.Toast.makeText(
                                context,
                                "Extracting subject...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()

                            StickerMaker.extractSubject(context, element.uriString) { newUri ->
                                if (newUri != null) {
                                    val stickerPos = element.position + Offset(80f, 80f)
                                    elements.add(
                                        CanvasElement.ImageCard(
                                            id = UUID.randomUUID().toString(),
                                            position = stickerPos,
                                            uriString = newUri,
                                            width = element.width,
                                            height = element.height
                                        )
                                    )
                                    onSave(elements)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "No clear subject found",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onSaveRequest = { onSave(elements) },
                        onDelete = {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            selectedNoteId = null
                            onSave(elements)
                        },
                        onSelect = {
                            if (isConnectingMode) {
                                if (connectingStartNodeId == null) {
                                    connectingStartNodeId = element.id
                                } else if (connectingStartNodeId != element.id) {
                                    elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = element.id))
                                    onSave(elements)
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                }
                            } else if (!isDrawingMode && !isLassoMode) {
                                if (!selectedGroupIds.contains(element.id)) {
                                    selectedGroupIds = emptySet()
                                    selectedNoteId = element.id
                                    bringToFront(element.id)
                                }
                            }
                        }
                    )
                }
            }
            if (element is CanvasElement.PdfCard) {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    PdfItem(
                        pdfCard = element, scale = scale, offset = offset,
                        isSelected = selectedNoteId == element.id && selectedGroupIds.isEmpty(),
                        isConnecting = isGroupedOrConnecting,
                        onUpdate = { updatedPdf ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) elements[currentIndex] = updatedPdf
                        },
                        onPositionDelta = { delta -> moveGroup(delta, element.id) },
                        onSizeDelta = { delta ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.PdfCard
                                val newWidth = (el.width + delta.x).coerceAtLeast(100f)
                                val newHeight = (el.height + delta.y).coerceAtLeast(80f)
                                elements[currentIndex] = el.copy(width = newWidth, height = newHeight)
                            }
                        },
                        onSaveRequest = { onSave(elements) },
                        onDelete = {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            selectedNoteId = null
                            onSave(elements)
                        },
                        onSelect = {
                            if (isConnectingMode) {
                                if (connectingStartNodeId == null) {
                                    connectingStartNodeId = element.id
                                } else if (connectingStartNodeId != element.id) {
                                    elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = element.id))
                                    onSave(elements)
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                }
                            } else if (!isDrawingMode && !isLassoMode) {
                                if (!selectedGroupIds.contains(element.id)) {
                                    selectedGroupIds = emptySet()
                                    selectedNoteId = element.id
                                    bringToFront(element.id)
                                }
                            }
                        }
                    )
                }
            }
            if (element is CanvasElement.AudioNote) {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    AudioItem(
                        audioNote = element, scale = scale, offset = offset,
                        isSelected = selectedNoteId == element.id && selectedGroupIds.isEmpty(),
                        isConnecting = isGroupedOrConnecting,
                        onUpdate = { updatedAudio ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) elements[currentIndex] = updatedAudio
                        },
                        onPositionDelta = { delta -> moveGroup(delta, element.id) },
                        onSizeDelta = { delta ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.AudioNote
                                val newWidth = (el.width + delta.x).coerceAtLeast(160f)
                                val newHeight = (el.height + delta.y).coerceAtLeast(60f)
                                elements[currentIndex] = el.copy(width = newWidth, height = newHeight)
                            }
                        },
                        onSaveRequest = { onSave(elements) },
                        onDelete = {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            selectedNoteId = null
                            onSave(elements)
                        },
                        onSelect = {
                            if (isConnectingMode) {
                                if (connectingStartNodeId == null) { connectingStartNodeId = element.id }
                                else if (connectingStartNodeId != element.id) {
                                    elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = element.id))
                                    onSave(elements)
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                }
                            } else if (!isDrawingMode && !isLassoMode) {
                                if (!selectedGroupIds.contains(element.id)) {
                                    selectedGroupIds = emptySet()
                                    selectedNoteId = element.id
                                    bringToFront(element.id)
                                }
                            }
                        }
                    )
                }
            }
            if (element is CanvasElement.ShapeNode) {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    ShapeItem(
                        shapeNode = element, scale = scale, offset = offset,
                        isSelected = selectedNoteId == element.id && selectedGroupIds.isEmpty(),
                        isConnecting = isGroupedOrConnecting,
                        onUpdate = { updatedShape ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) elements[currentIndex] = updatedShape
                        },
                        onPositionDelta = { delta -> moveGroup(delta, element.id) },
                        onSizeDelta = { delta ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.ShapeNode
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(50f),
                                    height = (el.height + delta.y).coerceAtLeast(50f)
                                )
                            }
                        },
                        onSaveRequest = { onSave(elements) },
                        onDelete = {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            selectedNoteId = null
                            onSave(elements)
                        },
                        onSelect = {
                            if (isConnectingMode) {
                                if (connectingStartNodeId == null) connectingStartNodeId = element.id
                                else if (connectingStartNodeId != element.id) {
                                    elements.add(CanvasElement.Connector(id = java.util.UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = element.id))
                                    onSave(elements)
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                }
                            } else if (!isDrawingMode && !isLassoMode) {
                                if (!selectedGroupIds.contains(element.id)) {
                                    selectedGroupIds = emptySet()
                                    selectedNoteId = element.id
                                    bringToFront(element.id)
                                }
                            }
                        }
                    )
                }
            }
        }
        // Top Controls Header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val deselectAll = {
                isDrawingMode = false; isConnectingMode = false; isLassoMode = false
                connectingStartNodeId = null; selectedGroupIds = emptySet()
            }

            CanvasToolbar(
                isDrawingMode = isDrawingMode,
                isConnectingMode = isConnectingMode,
                isLassoMode = isLassoMode,
                onPanClick = { deselectAll() },
                onDrawClick = { deselectAll(); isDrawingMode = true },
                onLassoClick = { deselectAll(); isLassoMode = true },
                onConnectClick = { deselectAll(); isConnectingMode = true },
                onImageClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onCodeClick = {
                    val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
                    elements.add(CanvasElement.CodeBlock(id = java.util.UUID.randomUUID().toString(), position = dropPos))
                    onSave(elements)
                },
                onMicClick = {
                    val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
                    elements.add(
                        CanvasElement.AudioNote(
                            id = java.util.UUID.randomUUID().toString(),
                            position = dropPos
                        )
                    )
                    onSave(elements)
                },
                onShapeClick = {
                    val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
                    elements.add(CanvasElement.ShapeNode(id = java.util.UUID.randomUUID().toString(), position = dropPos))
                    onSave(elements)
                },
            )

            if (isDrawingMode) {
                PenStudioPanel(
                    activeDrawColor = activeDrawColor,
                    activeStrokeWidth = activeStrokeWidth,
                    activeToolType = activeToolType,
                    activeAlpha = activeAlpha,
                    onColorChange = { activeDrawColor = it },
                    onStrokeWidthChange = { activeStrokeWidth = it },
                    onToolChange = { activeToolType = it },
                    onAlphaChange = { activeAlpha = it }
                )
            }
        }

        FloatingActionButton(
            onClick = {
                val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
                elements.add(CanvasElement.StickyNote(id = UUID.randomUUID().toString(), position = dropPos))
                isDrawingMode = false
                isConnectingMode = false
                isLassoMode = false
                onSave(elements)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Add Note") }
    }
}