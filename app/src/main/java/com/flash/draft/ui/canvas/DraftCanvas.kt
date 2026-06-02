package com.flash.draft.ui.canvas

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.flash.draft.ConnectorStyleMenu
import com.flash.draft.PenStudioPanel
import com.flash.draft.model.CanvasElement
import com.flash.draft.utils.GridBackground
import com.flash.draft.utils.ViewportMath

import com.flash.draft.ui.canvas.item.RenderCanvasElements
import com.flash.draft.ui.canvas.item.CanvasDrawPipeline
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.distancePointToSegment
import com.flash.draft.ui.canvas.item.getCenterPoint
import com.flash.draft.ui.canvas.item.getElementDimensions
import com.flash.draft.ui.canvas.item.getRectangleIntersection

// --- LIQUID PLUGIN IMPORTS ---
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

// Global Composition Locals
val LocalLiquidState = compositionLocalOf<LiquidState?> { null }
val LocalLiquidRefraction = compositionLocalOf { 0.5f }
val LocalLiquidCurve = compositionLocalOf { 0.5f }

@Composable
fun DraftCanvas(
    viewModel: CanvasViewModel
) {
    var scale by remember { mutableStateOf(0.7f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var centerX by remember { mutableStateOf(0f) }
    var centerY by remember { mutableStateOf(0f) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var selectedConnectorId by remember { mutableStateOf<String?>(null) }
    var defaultConnectorStyle by remember { mutableStateOf(CanvasElement.Connector(id = "", startId = "", endId = "")) }

    var isDarkTheme by remember { mutableStateOf(false) }

    var isDrawMenuOpen by remember { mutableStateOf(false) }
    var activeSubMenu by remember { mutableStateOf<String?>(null) } // Tracks "Thickness", "Color", or "Brush"

    var isDrawingMode by remember { mutableStateOf(false) }
    var currentDrawingPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var isConnectingMode by remember { mutableStateOf(false) }
    var connectingStartNodeId by remember { mutableStateOf<String?>(null) }

    var isLassoMode by remember { mutableStateOf(false) }
    var lassoPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var activeDrawColor by remember { mutableStateOf(Color.Black) }
    var activeStrokeWidth by remember { mutableStateOf(6f) }
    var activeToolType by remember { mutableStateOf("Pen") }
    var activeAlpha by remember { mutableStateOf(1f) }

    // --- LIQUID STATE CONTROL ---
    val liquidState = rememberLiquidState()
    val liquidRefraction by viewModel.liquidRefraction.collectAsStateWithLifecycle(initialValue = 0.5f)
    val liquidCurve by viewModel.liquidCurve.collectAsStateWithLifecycle(initialValue = 0.5f)
    var isLiquidMenuOpen by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var panJob: Job? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    val elementsState by viewModel.elements.collectAsStateWithLifecycle()
    val elements = remember { mutableStateListOf<CanvasElement>() }

    var isUiVisible by remember { mutableStateOf(true) }
    val view = LocalView.current

    LaunchedEffect(elementsState) {
        if (elements.toList() != elementsState) {
            elements.clear()
            elements.addAll(elementsState)
        }
    }

    fun logAction(action: String) {
        Log.d("CanvasEngine", "ACTION: $action | Zoom: $scale | Elements: ${elements.size}")
    }

    fun getFileInfo(uri: Uri): Pair<String, String> {
        var name = "Document.pdf"
        var size = "Unknown Size"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
                val sizeBytes = cursor.getLong(sizeIndex)
                size = "${sizeBytes / 1024} kB"
            }
        }
        return Pair(name, size)
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val (fileName, fileSize) = getFileInfo(uri)
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
            elements.add(CanvasElement.PdfCard(id = UUID.randomUUID().toString(), position = dropPos, uriString = uri.toString(), fileName = fileName, fileSize = fileSize))
            logAction("Imported PDF")
            viewModel.updateElements(elements.toList())
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
            elements.add(CanvasElement.ImageCard(id = UUID.randomUUID().toString(), position = dropPos, uriString = uri.toString()))
            isDrawingMode = false; isConnectingMode = false; isLassoMode = false
            logAction("Imported Photo")
            viewModel.updateElements(elements.toList())
        }
    }

    fun bringToFront(noteId: String) {
        val index = elements.indexOfFirst { it.id == noteId }
        if (index > -1 && index != elements.lastIndex) { val note = elements.removeAt(index); elements.add(note) }
    }

    fun sendToBack(noteId: String) {
        val index = elements.indexOfFirst { it.id == noteId }
        if (index > 0) { val note = elements.removeAt(index); elements.add(0, note) }
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
                if (el is CanvasElement.LabelNode) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.FrameNode) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.TaskListNode) elements[idx] = el.copy(position = el.position + delta)
            }
        }
    }

    fun finishConnection(endNodeId: String) {
        elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = connectingStartNodeId!!, endId = endNodeId, color = defaultConnectorStyle.color, strokeWidth = defaultConnectorStyle.strokeWidth, lineStyle = defaultConnectorStyle.lineStyle, arrowDirection = defaultConnectorStyle.arrowDirection, arrowShape = defaultConnectorStyle.arrowShape))
        logAction("Created Connection")
        viewModel.updateElements(elements.toList())
        connectingStartNodeId = null; isConnectingMode = false
    }

    CompositionLocalProvider(
        LocalDarkTheme provides isDarkTheme,
        LocalLiquidState provides liquidState,
        LocalLiquidRefraction provides liquidRefraction,
        LocalLiquidCurve provides liquidCurve
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF121212) else Color(0xFFF0F0F0))
                .onSizeChanged { size ->
                    centerX = size.width / 2f; centerY = size.height / 2f
                    if (offset == Offset.Zero) offset = Offset(centerX, centerY)
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed }) { panJob?.cancel() }

                            if (event.changes.size > 1) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid()

                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val (newScale, newOffset) = ViewportMath.calculateCameraTransform(scale, offset, centroid, panChange, zoomChange)
                                    scale = newScale; offset = newOffset
                                    event.changes.forEach { if (it.pressed) it.consume() }
                                }
                            }
                        }
                    }
                }
                .pointerInput(isDrawingMode, isConnectingMode, isLassoMode) {
                    if (isDrawingMode || isLassoMode) {
                        detectDragGestures(
                            onDragStart = { startPos ->
                                if (isLassoMode) { selectedGroupIds = emptySet(); lassoPath = listOf(ViewportMath.screenToWorld(startPos, offset, scale)) }
                                else { currentDrawingPath = listOf(ViewportMath.screenToWorld(startPos, offset, scale)) }
                            },
                            onDrag = { change, _ ->
                                val newPoint = ViewportMath.screenToWorld(change.position, offset, scale)
                                if (isLassoMode) { lassoPath = lassoPath + newPoint }
                                else {
                                    val lastPoint = currentDrawingPath.lastOrNull()
                                    if (lastPoint == null || (newPoint - lastPoint).getDistance() > (3f / scale)) { currentDrawingPath = currentDrawingPath + newPoint }
                                }
                            },
                            onDragEnd = {
                                if (isLassoMode && lassoPath.size > 2) {
                                    val capturedIds = mutableSetOf<String>()
                                    elements.forEach { el ->
                                        val center = getCenterPoint(el.id, elements)
                                        if (center != null && ViewportMath.isPointInPolygon(center, lassoPath)) capturedIds.add(el.id)
                                        else if (el is CanvasElement.Sketch && ViewportMath.isPointInPolygon(el.points.first(), lassoPath)) capturedIds.add(el.id)
                                    }
                                    selectedGroupIds = capturedIds; lassoPath = emptyList(); isLassoMode = false
                                } else if (currentDrawingPath.size > 1) {
                                    elements.add(CanvasElement.Sketch(id = UUID.randomUUID().toString(), points = currentDrawingPath, color = activeDrawColor, strokeWidth = activeStrokeWidth, toolType = activeToolType, alpha = activeAlpha))
                                    logAction("Saved Ink Stroke")
                                    viewModel.updateElements(elements.toList())
                                    currentDrawingPath = emptyList()
                                }
                            }
                        )
                    } else {
                        val velocityTracker = VelocityTracker()
                        detectDragGestures(
                            onDragStart = { panJob?.cancel(); velocityTracker.resetTracking() },
                            onDrag = { change, dragAmount -> velocityTracker.addPosition(change.uptimeMillis, change.position); offset += dragAmount },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity()
                                var currentVelocity = Offset(velocity.x, velocity.y)
                                if (currentVelocity.getDistance() > 300f) {
                                    panJob = coroutineScope.launch {
                                        var lastTime = androidx.compose.runtime.withFrameNanos { it }
                                        while (currentVelocity.getDistance() > 10f && isActive) {
                                            val time = androidx.compose.runtime.withFrameNanos { it }
                                            val dt = (time - lastTime) / 1_000_000_000f
                                            lastTime = time
                                            offset += currentVelocity * dt
                                            currentVelocity *= kotlin.math.exp(-6f * dt)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapPos ->
                            panJob?.cancel()
                            val worldTap = ViewportMath.screenToWorld(tapPos, offset, scale)
                            scale = 1f; offset = Offset(centerX, centerY) - worldTap
                            logAction("Double-Tap Zoom to Target")
                        },
                        onPress = { tapPos ->
                            val worldTap = ViewportMath.screenToWorld(tapPos, offset, scale)
                            val hitConnector = elements.filterIsInstance<CanvasElement.Connector>().any { conn ->
                                val centerStart = getCenterPoint(conn.startId, elements); val centerEnd = getCenterPoint(conn.endId, elements)
                                if (centerStart != null && centerEnd != null) {
                                    val (startW, startH) = getElementDimensions(conn.startId, elements); val (endW, endH) = getElementDimensions(conn.endId, elements)
                                    val start = getRectangleIntersection(centerStart, centerEnd, startW, startH, 25f); val end = getRectangleIntersection(centerEnd, centerStart, endW, endH, 25f)
                                    distancePointToSegment(worldTap, start, end) < (60f / scale)
                                } else false
                            }
                            if (!hitConnector) { selectedConnectorId = null; selectedNoteId = null; connectingStartNodeId = null; selectedGroupIds = emptySet(); focusManager.clearFocus() }
                        },
                        onTap = { tapPos ->
                            val worldTap = ViewportMath.screenToWorld(tapPos, offset, scale)
                            val tappedConnector = elements.filterIsInstance<CanvasElement.Connector>().find { conn ->
                                val centerStart = getCenterPoint(conn.startId, elements); val centerEnd = getCenterPoint(conn.endId, elements)
                                if (centerStart != null && centerEnd != null) {
                                    val (startW, startH) = getElementDimensions(conn.startId, elements); val (endW, endH) = getElementDimensions(conn.endId, elements)
                                    val start = getRectangleIntersection(centerStart, centerEnd, startW, startH, 25f); val end = getRectangleIntersection(centerEnd, centerStart, endW, endH, 25f)
                                    distancePointToSegment(worldTap, start, end) < (60f / scale)
                                } else false
                            }
                            if (tappedConnector != null) { selectedConnectorId = tappedConnector.id; selectedNoteId = null; selectedGroupIds = emptySet() }
                        }
                    )
                }
        ) {

            // --- 1. RECORDING BACKGROUND ---
            Box(modifier = Modifier.fillMaxSize().liquefiable(liquidState)) {
                GridBackground(scale = scale, offset = offset)

                CanvasDrawPipeline(
                    elements = elements, scale = scale, offset = offset, selectedConnectorId = selectedConnectorId,
                    isDarkTheme = isDarkTheme, selectedGroupIds = selectedGroupIds, currentDrawingPath = currentDrawingPath,
                    activeToolType = activeToolType, activeDrawColor = activeDrawColor, activeAlpha = activeAlpha,
                    activeStrokeWidth = activeStrokeWidth, lassoPath = lassoPath
                )

                RenderCanvasElements(
                    elements = elements,
                    scale = scale,
                    offset = offset,
                    selectedNoteId = selectedNoteId,
                    selectedGroupIds = selectedGroupIds,
                    connectingStartNodeId = connectingStartNodeId,
                    isConnectingMode = isConnectingMode,
                    isDrawingMode = isDrawingMode,
                    isLassoMode = isLassoMode,
                    centerX = centerX,
                    centerY = centerY,
                    context = context,
                    viewModel = viewModel,
                    onUpdateElements = { updatedElements: List<CanvasElement> -> viewModel.updateElements(updatedElements) },
                    onMoveGroup = { delta: Offset, triggerElementId: String -> moveGroup(delta, triggerElementId) },
                    onFinishConnection = { endNodeId: String -> finishConnection(endNodeId) },
                    onSendToBack = { elementId: String -> sendToBack(elementId) },
                    onBringToFront = { elementId: String -> bringToFront(elementId) },
                    onResetSelectedNoteId = { selectedNoteId = null },
                    onResetConnectingStartNodeId = { connectingStartNodeId = null },
                    onResetIsConnectingMode = { isConnectingMode = false },
                    onUpdateSelectedGroupIds = { updatedGroupIds: Set<String> -> selectedGroupIds = updatedGroupIds },
                    onUpdateSelectedNoteId = { updatedSelectedNoteId: String? -> selectedNoteId = updatedSelectedNoteId },
                    onUpdateConnectingStartNodeId = { updatedConnectingStartNodeId: String? -> connectingStartNodeId = updatedConnectingStartNodeId },
                    onUpdateScaleAndOffset = { newScale: Float, newOffset: Offset -> scale = newScale; offset = newOffset }
                )
            }

            if (isUiVisible) {

                // --- NEW: TOP LEFT ACTION BAR (BACK + UNDO/REDO) ---
                Box(modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. BACK BUTTON PILL
                        Box(
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(24.dp))
                                .liquid(liquidState) { refraction = liquidRefraction; curve = liquidCurve; shape = RoundedCornerShape(24.dp) }
                                .background(if (isDarkTheme) Color(0xCC2C2C2C) else Color(0xCCE0FFFFFF), RoundedCornerShape(24.dp))
                                .border(1.dp, if (isDarkTheme) Color(0x40FFFFFF) else Color(0x60FFFFFF), RoundedCornerShape(24.dp))
                                .clickable { /* TODO: Add Navigation Back */ }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                        }

                        // 2. UNDO / REDO PILL (Horizontal)
                        Row(
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(24.dp))
                                .liquid(liquidState) { refraction = liquidRefraction; curve = liquidCurve; shape = RoundedCornerShape(24.dp) }
                                .background(if (isDarkTheme) Color(0xCC2C2C2C) else Color(0xCCE0FFFFFF), RoundedCornerShape(24.dp))
                                .border(1.dp, if (isDarkTheme) Color(0x40FFFFFF) else Color(0x60FFFFFF), RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(onClick = { viewModel.undo() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (isDarkTheme) Color.White else Color.Black)
                            }
                            // Vertical Divider Line
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.3f)))

                            IconButton(onClick = { viewModel.redo() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (isDarkTheme) Color.White else Color.Black)
                            }
                        }
                    }
                }

                // LIQUID SETTINGS TOGGLE
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)) {
                    IconButton(
                        onClick = { isLiquidMenuOpen = !isLiquidMenuOpen },
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(12.dp, CircleShape)
                            .liquid(liquidState) { refraction = liquidRefraction; curve = liquidCurve; shape = CircleShape }
                            .background(if (isDarkTheme) Color(0xCC1A1A1A) else Color(0xCCD9FFFFFF), CircleShape)
                            .border(1.dp, if (isDarkTheme) Color(0x33FFFFFF) else Color(0x4DFFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, "Liquid Physics", tint = Color(0xFF2196F3))
                    }
                }

                // LIQUID SETTINGS PANEL
                AnimatedVisibility(
                    visible = isLiquidMenuOpen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                            .liquid(liquidState) { refraction = liquidRefraction; curve = liquidCurve; shape = RoundedCornerShape(24.dp) }
                            .background(if (isDarkTheme) Color(0xCC1C1C1E) else Color(0xCCF2F2F7), RoundedCornerShape(24.dp))
                            .border(1.dp, if (isDarkTheme) Color(0x33FFFFFF) else Color(0x4DFFFFFF), RoundedCornerShape(24.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Liquid Distortion", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkTheme) Color.White else Color.Black)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.2f)))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Refract", color = if (isDarkTheme) Color.LightGray else Color.DarkGray, fontSize = 13.sp)
                            Slider(value = liquidRefraction, onValueChange = { viewModel.setLiquidRefraction(it) }, modifier = Modifier.width(130.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Curve", color = if (isDarkTheme) Color.LightGray else Color.DarkGray, fontSize = 13.sp)
                            Slider(value = liquidCurve, onValueChange = { viewModel.setLiquidCurve(it) }, modifier = Modifier.width(130.dp))
                        }
                    }
                }

                val layerMenuAlpha by androidx.compose.animation.core.animateFloatAsState(if (selectedNoteId != null) 1f else 0f, androidx.compose.animation.core.tween(200), label = "layerAlpha")
                val layerMenuOffsetX by androidx.compose.animation.core.animateDpAsState(if (selectedNoteId != null) 0.dp else (-50).dp, androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "layerOffset")

                if (layerMenuAlpha > 0f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart).padding(start = 24.dp)
                            .graphicsLayer { alpha = layerMenuAlpha; translationX = layerMenuOffsetX.toPx() }
                            .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                            .liquid(liquidState) { refraction = liquidRefraction; curve = liquidCurve; shape = RoundedCornerShape(24.dp) }
                            .background(if (isDarkTheme) Color(0xCC1A1A1A) else Color(0xCCD9FFFFFF), RoundedCornerShape(24.dp))
                            .border(1.dp, if (isDarkTheme) Color(0x22FFFFFF) else Color(0x44FFFFFF), RoundedCornerShape(24.dp))
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { bringToFront(selectedNoteId!!); viewModel.updateElements(elements.toList()) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Bring Forward", tint = if (isDarkTheme) Color.White else Color.Black)
                        }
                        Box(modifier = Modifier.width(20.dp).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        IconButton(onClick = { sendToBack(selectedNoteId!!); viewModel.updateElements(elements.toList()) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Send Backward", tint = if (isDarkTheme) Color.White else Color.Black)
                        }
                    }
                }

                if (selectedConnectorId != null) {
                    val connIndex = elements.indexOfFirst { it.id == selectedConnectorId }
                    if (connIndex != -1) {
                        val conn = elements[connIndex] as CanvasElement.Connector
                        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)) {
                            ConnectorStyleMenu(connector = conn, onUpdate = { updated -> elements[connIndex] = updated; viewModel.updateElements(elements.toList()) }, onDelete = { elements.removeAt(connIndex); selectedConnectorId = null; viewModel.updateElements(elements.toList()) }, onClose = { selectedConnectorId = null })
                        }
                    }
                } else if (isConnectingMode) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)) {
                        ConnectorStyleMenu(connector = defaultConnectorStyle, onUpdate = { updated -> defaultConnectorStyle = updated }, onDelete = { isConnectingMode = false; connectingStartNodeId = null }, onClose = { isConnectingMode = false; connectingStartNodeId = null })
                    }
                }

                // --- PREMIUM BOTTOM WORKSPACE BAR ---
                Box(modifier = Modifier.fillMaxSize()) {
                    val activeMainTool = when {
                        isDrawingMode -> "Draw"
                        else -> "Pan"
                    }
                    val deselectAll = { isDrawingMode = false; isConnectingMode = false; isLassoMode = false; connectingStartNodeId = null; selectedGroupIds = emptySet() }

                    // --- NEW: THE VERTICAL DRAW TOOL STACK & POPOUTS ---
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 108.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom // Aligns the popouts perfectly with the tools
                        ) {
                            // 1. The Vertical Stack
                            com.flash.draft.ui.canvas.tools.DrawToolStack(
                                isOpen = isDrawingMode && isDrawMenuOpen,
                                activeColor = activeDrawColor,
                                onBrushClick = {
                                    activeToolType = "Pen"
                                    activeSubMenu = if (activeSubMenu == "Brush") null else "Brush"
                                },
                                onEraserClick = {
                                    activeToolType = "Eraser"
                                    activeSubMenu = null // Eraser has no sub-menu
                                },
                                onColorClick = { activeSubMenu = if (activeSubMenu == "Color") null else "Color" },
                                onThicknessClick = { activeSubMenu = if (activeSubMenu == "Thickness") null else "Thickness" }
                            )

                            // 2. The Horizontal Popouts
                            Box {
                                com.flash.draft.ui.canvas.tools.DrawThicknessMenu(
                                    isOpen = activeSubMenu == "Thickness" && isDrawMenuOpen,
                                    strokeWidth = activeStrokeWidth,
                                    onStrokeWidthChange = { activeStrokeWidth = it }
                                )

                                com.flash.draft.ui.canvas.tools.DrawColorMenu(
                                    isOpen = activeSubMenu == "Color" && isDrawMenuOpen,
                                    activeColor = activeDrawColor,
                                    onColorSelect = { activeDrawColor = it }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        BottomNavBar(
                            activeTool = activeMainTool,
                            onToolSelect = { tool ->
                                deselectAll()
                                isDrawMenuOpen = false // Close menu when switching tools
                                when (tool) {
                                    "Draw" -> isDrawingMode = true
                                    "Pan" -> { /* Defaults to panning */ }
                                    "Text" -> { elements.add(CanvasElement.LabelNode(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale))); viewModel.updateElements(elements.toList()) }
                                    "Note" -> { elements.add(CanvasElement.StickyNote(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale))); viewModel.updateElements(elements.toList()) }
                                }
                            },
                            onAddClick = {
                                // TODO: We will build the expanding Insert Menu next!
                            },
                            showDynamicLeft = isDrawingMode,
                            onDynamicLeftClick = {
                                isDrawMenuOpen = !isDrawMenuOpen // Toggles the vertical stack!
                            },
                            dynamicLeftContent = {
                                Icon(
                                    imageVector = Icons.Default.Brush, // Representing the Draw tool
                                    contentDescription = "Draw Menu",
                                    tint = if (isDarkTheme) Color.White else Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )
                    }
                }
            } // END OF UI VISIBILITY BLOCK
        }
    }
}