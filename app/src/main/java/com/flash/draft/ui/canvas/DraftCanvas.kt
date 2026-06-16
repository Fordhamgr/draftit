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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
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
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.flash.draft.ConnectorStyleMenu
import com.flash.draft.PenStudioPanel
import com.flash.draft.model.CanvasElement
import com.flash.draft.model.InkPoint
import com.flash.draft.utils.GridBackground
import com.flash.draft.utils.ViewportMath

import com.flash.draft.ui.canvas.item.RenderCanvasElements
import com.flash.draft.ui.canvas.item.CanvasBackgroundDrawPipeline
import com.flash.draft.ui.canvas.item.CanvasForegroundDrawPipeline
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
    viewModel: CanvasViewModel,
    onBack: () -> Unit = {},
    onNavigateToBoard: (String) -> Unit = {}
) {
    var scale by remember { mutableStateOf(0.7f) }
    var lastZoomTime by remember { mutableLongStateOf(0L) }
    var isZooming by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var centerX by remember { mutableStateOf(0f) }
    var centerY by remember { mutableStateOf(0f) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var selectedConnectorId by remember { mutableStateOf<String?>(null) }
    var defaultConnectorStyle by remember { mutableStateOf(CanvasElement.Connector(id = "", startId = "", endId = "")) }

    var isDarkTheme by remember { mutableStateOf(false) }

    var isScenesMenuOpen by remember { mutableStateOf(false) }
    var sceneToExport by remember { mutableStateOf<com.flash.draft.model.Scene?>(null) }

    var isDrawMenuOpen by remember { mutableStateOf(false) }
    var isInsertMenuOpen by remember { mutableStateOf(false) }
    var activeSubMenu by remember { mutableStateOf<String?>(null) } // Tracks "Thickness", "Color", or "Brush"

    var isDrawingMode by remember { mutableStateOf(false) }

    var isConnectingMode by remember { mutableStateOf(false) }
    var connectingStartNodeId by remember { mutableStateOf<String?>(null) }

    var isLassoMode by remember { mutableStateOf(false) }
    val currentDrawingPath = remember { mutableStateListOf<InkPoint>() }
    val lassoPath = remember { mutableStateListOf<Offset>() }
    var selectedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var activeDrawColor by remember { mutableStateOf(Color.Black) }
    var activeStrokeWidth by remember { mutableStateOf(6f) }
    var activeToolType by remember { mutableStateOf("Pen") }
    var activeAlpha by remember { mutableStateOf(1f) }

    // --- LIQUID STATE CONTROL ---
    val liquidState = rememberLiquidState()
    val liquidRefraction by viewModel.liquidRefraction.collectAsStateWithLifecycle(initialValue = 0.5f)
    val liquidCurve by viewModel.liquidCurve.collectAsStateWithLifecycle(initialValue = 0.5f)
    var isExportMenuOpen by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var panJob: Job? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // PERF LOGGING: Measure entire canvas recomposition
    val recomposeStart = System.nanoTime()
    DisposableEffect(Unit) {
        onDispose { 
            viewModel.triggerImmediateSave()
        }
    }
    SideEffect {
        val elapsed = (System.nanoTime() - recomposeStart) / 1_000_000f
        com.flash.draft.ui.canvas.item.CanvasProfiler.recomposeCount++
    }
    
    // PERF LOGGING: Monitor actual frame rate and detect stutters
    var frameCount by remember { mutableStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.nanoTime()) }
    var maxFrameDelta by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrameTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { time ->
                frameCount++
                val dtMs = (time - lastFrameTime) / 1_000_000f
                if (dtMs > maxFrameDelta) maxFrameDelta = dtMs
                if (dtMs > 18f && lastFrameTime != 0L) { // Stutter detected
                    Log.w("DraftCanvasPerf", "Frame Drop: ${dtMs}ms")
                }
                lastFrameTime = time
                
                if (time - lastFpsTime > 1_000_000_000L) {
                    val avgBg = if (com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount > 0) com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawTimeSum / com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount else 0f
                    val avgFg = if (com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount > 0) com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawTimeSum / com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount else 0f
                    val totalCpu = avgBg + avgFg
                    val totalCalls = com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount + com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount
                    
                    Log.i("DraftCanvasPerf", "[PERF] FPS: $frameCount | MaxFrame: ${String.format("%.1f", maxFrameDelta)}ms | CPU Draw: ${String.format("%.2f", totalCpu)}ms (BG: ${String.format("%.2f", avgBg)}, FG: ${String.format("%.2f", avgFg)}) | Recomp: ${com.flash.draft.ui.canvas.item.CanvasProfiler.recomposeCount} | Calls: $totalCalls")
                    
                    frameCount = 0
                    maxFrameDelta = 0f
                    lastFpsTime = time
                    com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawTimeSum = 0f
                    com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount = 0
                    com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawTimeSum = 0f
                    com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount = 0
                    com.flash.draft.ui.canvas.item.CanvasProfiler.recomposeCount = 0
                }
            }
        }
    }


    val elementsState by viewModel.elements.collectAsStateWithLifecycle()
    val elements = remember { mutableStateListOf<CanvasElement>() }

    var isUiVisible by remember { mutableStateOf(true) }
    val view = LocalView.current
    val graphicsLayer = rememberGraphicsLayer()

    fun navigateToScene(scene: com.flash.draft.model.Scene) {
        panJob?.cancel()
        coroutineScope.launch {
            val startScale = scale
            val startOffset = offset
            val targetOffset = Offset(scene.offsetX, scene.offsetY)
            val targetScale = scene.scale
            androidx.compose.animation.core.animate(
                initialValue = 0f, 
                targetValue = 1f, 
                animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) { progress, _ ->
                scale = androidx.compose.ui.util.lerp(startScale, targetScale, progress)
                offset = androidx.compose.ui.geometry.lerp(startOffset, targetOffset, progress)
            }
        }
    }

    fun zoomToFit() {
        panJob?.cancel()
        if (elements.isEmpty()) return
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        elements.forEach { el ->
            if (el is CanvasElement.Connector) return@forEach
            val center = getCenterPoint(el.id, elements) ?: return@forEach
            val dim = getElementDimensions(el.id, elements)
            val halfW = dim.first / 2f; val halfH = dim.second / 2f
            if (center.x - halfW < minX) minX = center.x - halfW
            if (center.y - halfH < minY) minY = center.y - halfH
            if (center.x + halfW > maxX) maxX = center.x + halfW
            if (center.y + halfH > maxY) maxY = center.y + halfH
        }
        if (minX == Float.MAX_VALUE) return // Only connectors
        val padding = 100f
        val contentW = maxX - minX + (padding * 2)
        val contentH = maxY - minY + (padding * 2)
        val sw = view.width.toFloat(); val sh = view.height.toFloat()
        if (sw == 0f || sh == 0f) return
        val targetScale = minOf(sw / contentW, sh / contentH).coerceIn(0.01f, 3f)
        val contentCenter = Offset(minX + (maxX - minX) / 2f, minY + (maxY - minY) / 2f)
        val targetOffset = Offset(sw / 2f, sh / 2f) - (contentCenter * targetScale)

        coroutineScope.launch {
            val startScale = scale
            val startOffset = offset
            androidx.compose.animation.core.animate(
                initialValue = 0f, 
                targetValue = 1f, 
                animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) { progress, _ ->
                scale = androidx.compose.ui.util.lerp(startScale, targetScale, progress)
                offset = androidx.compose.ui.geometry.lerp(startOffset, targetOffset, progress)
            }
        }
    }

    LaunchedEffect(elementsState) {
        if (elements.toList() != elementsState) {
            elements.clear()
            elements.addAll(elementsState)
        }
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
            viewModel.updateElements(elements.toList())
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)
            
            // Calculate original dimensions
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
            }
            val origW = options.outWidth.toFloat().takeIf { it > 0 } ?: 300f
            val origH = options.outHeight.toFloat().takeIf { it > 0 } ?: 300f
            
            val maxDim = 400f
            val ratio = origW / origH
            val (scaledW, scaledH) = if (origW > origH) {
                if (origW > maxDim) maxDim to maxDim / ratio else origW to origH
            } else {
                if (origH > maxDim) maxDim * ratio to maxDim else origW to origH
            }
            
            elements.add(CanvasElement.ImageCard(id = UUID.randomUUID().toString(), position = dropPos, uriString = uri.toString(), width = scaledW, height = scaledH, originalWidth = origW, originalHeight = origH))
            isDrawingMode = false; isConnectingMode = false; isLassoMode = false
            viewModel.updateElements(elements.toList())
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                // Ensure immediate save when app goes to background
                viewModel.triggerImmediateSave()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.triggerImmediateSave()
        }
    }

    val exportProjectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val repo = com.flash.draft.data.NoteRepository(context)
                    repo.exportProject(context, viewModel.projectId, stream)
                }
                Toast.makeText(context, "Project exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error exporting project.", Toast.LENGTH_SHORT).show()
            }
            isExportMenuOpen = false
        }
    }

    val exportPictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isExporting = true
                    delay(100) // Allow recomposition and drawWithContent to record the layer
                    val hwBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    isExporting = false
                    
                    val swBitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    val finalBitmap = android.graphics.Bitmap.createBitmap(swBitmap.width, swBitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(finalBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.drawBitmap(swBitmap, 0f, 0f, null)

                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    Toast.makeText(context, "Picture exported successfully!", Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    isExporting = false
                    Toast.makeText(context, "Error exporting picture.", Toast.LENGTH_SHORT).show()
                }
            }
            isExportMenuOpen = false
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isExporting = true
                    delay(100) // Allow recomposition and drawWithContent to record the layer
                    val hwBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    isExporting = false
                    
                    val swBitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    val finalBitmap = android.graphics.Bitmap.createBitmap(swBitmap.width, swBitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(finalBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.drawBitmap(swBitmap, 0f, 0f, null)

                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(finalBitmap.width, finalBitmap.height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        pdfDocument.writeTo(stream)
                    }
                    pdfDocument.close()
                    Toast.makeText(context, "PDF exported successfully!", Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    isExporting = false
                    Toast.makeText(context, "Error exporting PDF.", Toast.LENGTH_SHORT).show()
                }
            }
            isExportMenuOpen = false
        }
    }

    val exportScenePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && sceneToExport != null) {
            val scene = sceneToExport!!
            val sw = view.width
            val sh = view.height
            if (sw > 0 && sh > 0) {
                coroutineScope.launch {
                    try {
                        val originalScale = scale
                        val originalOffset = offset
                        
                        scale = scene.scale
                        offset = androidx.compose.ui.geometry.Offset(scene.offsetX, scene.offsetY)
                        kotlinx.coroutines.delay(400) // Wait for recomposition
                        
                        val hwBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        val swBitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                        val finalBitmap = android.graphics.Bitmap.createBitmap(sw, sh, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(finalBitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        canvas.drawBitmap(swBitmap, 0f, 0f, null)
                        
                        val pdfDocument = android.graphics.pdf.PdfDocument()
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(sw, sh, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)
                        
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            pdfDocument.writeTo(stream)
                        }
                        pdfDocument.close()
                        Toast.makeText(context, "Scene exported to PDF successfully!", Toast.LENGTH_SHORT).show()
                        
                        // Restore original camera
                        scale = originalScale
                        offset = originalOffset
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error exporting Scene PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            sceneToExport = null
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
                if (el is CanvasElement.StickyNote && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.ImageCard && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.PdfCard && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.Sketch && !el.isLocked) {
                    val newX = FloatArray(el.xCoords.size) { i -> el.xCoords[i] + delta.x }
                    val newY = FloatArray(el.yCoords.size) { i -> el.yCoords[i] + delta.y }
                    val newEl = el.copy(xCoords = newX, yCoords = newY)
                    newEl.cachedStroke = null
                    elements[idx] = newEl
                }
                if (el is CanvasElement.CodeBlock && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.AudioNote && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.ShapeNode && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.LabelNode && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.FrameNode && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
                if (el is CanvasElement.TaskListNode && !el.isLocked) elements[idx] = el.copy(position = el.position + delta)
            }
        }
    }

    fun finishConnection(endNodeId: String) {
        val startId = connectingStartNodeId ?: return
        elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = startId, endId = endNodeId, color = defaultConnectorStyle.color, strokeWidth = defaultConnectorStyle.strokeWidth, lineStyle = defaultConnectorStyle.lineStyle, arrowDirection = defaultConnectorStyle.arrowDirection, arrowShape = defaultConnectorStyle.arrowShape))
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
                    val touchSlop = viewConfiguration.touchSlop
                    var pastSlop = false
                    var zoomAccumulator = 1f
                    var panAccumulator = Offset.Zero
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed }) { panJob?.cancel() }

                            val activePointers = event.changes.count { it.pressed }

                            if (activePointers > 1) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid()

                                if (!pastSlop) {
                                    zoomAccumulator *= zoomChange
                                    panAccumulator += panChange
                                    if (zoomAccumulator < 0.97f || zoomAccumulator > 1.03f || panAccumulator.getDistance() > touchSlop) {
                                        pastSlop = true
                                    }
                                }

                                if (pastSlop) {
                                    val (newScale, newOffset) = ViewportMath.calculateCameraTransform(scale, offset, centroid, panChange, zoomChange)
                                    scale = newScale; offset = newOffset
                                }
                                event.changes.forEach { if (it.pressed) it.consume() }
                            } else if (activePointers == 1 && pastSlop) {
                                // One finger still down after a pinch — consume to prevent
                                // the remaining finger from triggering a card tap or drag.
                                event.changes.forEach { it.consume() }
                            } else if (activePointers == 0) {
                                // All fingers released — reset for the next gesture.
                                pastSlop = false
                                zoomAccumulator = 1f
                                panAccumulator = Offset.Zero
                            }
                        }
                    }
                }
                .pointerInput(isDrawingMode, isLassoMode) {
                    if (isDrawingMode || isLassoMode) {
                        detectDragGestures(
                            onDragStart = { startPos ->
                                if (isLassoMode) { selectedGroupIds = emptySet(); lassoPath.clear(); lassoPath.add(ViewportMath.screenToWorld(startPos, offset, scale)) }
                                else { 
                                    currentDrawingPath.clear()
                                    val worldPoint = ViewportMath.screenToWorld(startPos, offset, scale)
                                    // Use a default time and pressure for the start point since detectDragGestures doesn't provide them initially
                                    currentDrawingPath.add(InkPoint(worldPoint.x, worldPoint.y, 1f, android.os.SystemClock.uptimeMillis())) 
                                }
                            },
                            onDrag = { change, _ ->
                                val newPoint = ViewportMath.screenToWorld(change.position, offset, scale)
                                if (isLassoMode) { lassoPath.add(newPoint) }
                                else {
                                    if (activeToolType == "Eraser") {
                                        val eraseRadius = activeStrokeWidth * 1.5f
                                        elements.removeAll { el ->
                                            el is CanvasElement.Sketch && (0 until el.xCoords.size).any { i -> (Offset(el.xCoords[i], el.yCoords[i]) - newPoint).getDistance() < eraseRadius }
                                        }
                                        // Defer viewModel.updateElements to onDragEnd for smooth erasing!
                                    } else {
                                        val lastPoint = currentDrawingPath.lastOrNull()
                                        if (lastPoint == null || (Offset(newPoint.x, newPoint.y) - Offset(lastPoint.x, lastPoint.y)).getDistance() > (3f / scale)) { 
                                            currentDrawingPath.add(InkPoint(newPoint.x, newPoint.y, change.pressure, change.uptimeMillis)) 
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                if (activeToolType == "Eraser") {
                                    val vmElements = viewModel.elements.value
                                    elements.clear()
                                    elements.addAll(vmElements)
                                }
                                currentDrawingPath.clear()
                                lassoPath.clear()
                            },
                            onDragEnd = {
                                if (isLassoMode && lassoPath.size > 2) {
                                    val capturedIds = mutableSetOf<String>()
                                    elements.forEach { el ->
                                        val center = getCenterPoint(el.id, elements)
                                        if (center != null && ViewportMath.isPointInPolygon(center, lassoPath)) capturedIds.add(el.id)
                                        else if (el is CanvasElement.Sketch && el.xCoords.isNotEmpty() && ViewportMath.isPointInPolygon(Offset(el.xCoords.first(), el.yCoords.first()), lassoPath)) capturedIds.add(el.id)
                                    }
                                    selectedGroupIds = capturedIds; lassoPath.clear(); isLassoMode = false
                                } else if (currentDrawingPath.size > 1 && activeToolType != "Eraser") {
                                    fun rdp(pts: List<InkPoint>, eps: Float): List<InkPoint> {
                                        if (pts.size < 3) return pts
                                        var dmax = 0f; var index = 0
                                        val first = pts.first(); val last = pts.last()
                                        for (i in 1 until pts.lastIndex) {
                                            val d = distancePointToSegment(Offset(pts[i].x, pts[i].y), Offset(first.x, first.y), Offset(last.x, last.y))
                                            if (d > dmax) { index = i; dmax = d }
                                        }
                                        if (dmax > eps) {
                                            val res1 = rdp(pts.subList(0, index + 1), eps)
                                            val res2 = rdp(pts.subList(index, pts.size), eps)
                                            return res1.dropLast(1) + res2
                                        }
                                        return listOf(first, last)
                                    }
                                    val simplified = rdp(currentDrawingPath.toList(), 1f / scale)
                                    val size = simplified.size
                                    val xArr = FloatArray(size)
                                    val yArr = FloatArray(size)
                                    val pArr = FloatArray(size)
                                    val tArr = LongArray(size)
                                    for (i in 0 until size) {
                                        val p = simplified[i]
                                        xArr[i] = p.x; yArr[i] = p.y; pArr[i] = p.pressure; tArr[i] = p.timeMillis
                                    }
                                    elements.add(CanvasElement.Sketch(id = UUID.randomUUID().toString(), xCoords = xArr, yCoords = yArr, pressures = pArr, times = tArr, color = activeDrawColor, strokeWidth = activeStrokeWidth, toolType = activeToolType, alpha = activeAlpha))
                                    viewModel.updateElements(elements.toList())
                                    currentDrawingPath.clear()
                                } else if (activeToolType == "Eraser") {
                                    viewModel.updateElements(elements.toList())
                                    currentDrawingPath.clear()
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
                            if (tappedConnector != null) {
                                selectedConnectorId = tappedConnector.id; selectedNoteId = null; selectedGroupIds = emptySet()
                            } else {
                                selectedConnectorId = null; selectedNoteId = null; connectingStartNodeId = null; isConnectingMode = false; selectedGroupIds = emptySet(); focusManager.clearFocus()
                            }
                        }
                    )
                }
        ) {
                SideEffect {
                    Log.d("DraftCanvasPerf", "BoxWithConstraints lambda recomposed!")
                }

                // Culling removed: GPU graphicsLayer efficiently clips off-screen elements natively.

                // GridBackground(scaleProvider = { scale }, offsetProvider = { offset })

                // --- 1. RECORDING BACKGROUND ---
                Box(modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(liquidState)
                    .drawWithContent {
                        val drawStart = System.nanoTime()
                        if (isExporting) {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                        }
                        drawContent()
                        val elapsed = (System.nanoTime() - drawStart) / 1_000_000f
                        if (elapsed > 5f && isExporting) {
                            Log.d("DraftCanvasPerf", "drawWithContent (graphicsLayer.record) took ${elapsed}ms")
                        }
                    }
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        }
                    ) {
                        CanvasBackgroundDrawPipeline(
                            elements = elements, scale = 1f, offset = Offset.Zero, selectedConnectorId = selectedConnectorId,
                            isDarkTheme = isDarkTheme
                        )

                        RenderCanvasElements(
                            elements = elements,
                            scale = 1f,
                            offset = androidx.compose.ui.geometry.Offset.Zero,
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
                    onUpdateScaleAndOffset = { newScale: Float, newOffset: Offset -> scale = newScale; offset = newOffset },
                    onStartConnection = { startNodeId: String -> 
                        isConnectingMode = true
                        connectingStartNodeId = startNodeId
                    },
                    onNavigateToScene = { sceneIdentifier ->
                        val scene = viewModel.scenes.value.find { it.id == sceneIdentifier || it.name == sceneIdentifier }
                        if (scene != null) navigateToScene(scene)
                    },
                    onNavigateToBoard = onNavigateToBoard
                )

                        CanvasForegroundDrawPipeline(
                            elements = elements,
                            cameraScaleProvider = { scale },
                            cameraOffsetProvider = { offset },
                            isDarkTheme = isDarkTheme,
                            selectedGroupIds = selectedGroupIds,
                            currentDrawingPath = currentDrawingPath,
                            activeToolType = activeToolType,
                            activeDrawColor = activeDrawColor,
                            activeAlpha = activeAlpha,
                            activeStrokeWidth = activeStrokeWidth,
                            lassoPath = lassoPath
                        )
                    }
                }

            if (isUiVisible) {

                // --- TOP LEFT ACTION BAR (BACK + UNDO/REDO + ZOOM) ---
                Box(modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                            LiquidGlassPill(
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            // HORIZONTAL UNDO / REDO PILL
                            LiquidGlassPill(
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    IconButton(onClick = { viewModel.undo() }, modifier = Modifier.size(28.dp)) {
                                        Icon(painterResource(id = LucideR.drawable.lucide_ic_undo), contentDescription = "Undo", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.redo() }, modifier = Modifier.size(28.dp)) {
                                        Icon(painterResource(id = LucideR.drawable.lucide_ic_redo), contentDescription = "Redo", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // EPHEMERAL ZOOM INDICATOR
                LaunchedEffect(lastZoomTime) {
                    if (lastZoomTime > 0) {
                        isZooming = true
                        kotlinx.coroutines.delay(1500)
                        isZooming = false
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = isZooming,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                ) {
                    LiquidGlassPill(shape = CircleShape, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("${(scale * 100).toInt()}%", color = if (isDarkTheme) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // CLICK OUTSIDE OVERLAY FOR ALL MENUS
                if (isExportMenuOpen || isScenesMenuOpen || isInsertMenuOpen || (isDrawingMode && isDrawMenuOpen)) {
                    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures {
                            isExportMenuOpen = false
                            isScenesMenuOpen = false
                            isInsertMenuOpen = false
                            isDrawMenuOpen = false
                            if (activeSubMenu == "CustomColor" || activeSubMenu == "Color" || activeSubMenu == "Thickness") {
                                activeSubMenu = null
                            }
                        }
                    })
                }

                // --- TOP RIGHT MENU (3 DOTS + EXPORT + SCENES) ---
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp)) {
                    
                    // LEFT COLUMN: MORE OPTIONS (3 DOTS)
                    val threeDotsAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen && !isScenesMenuOpen) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    val threeDotsScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen && !isScenesMenuOpen) 1f else 0.8f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    Box(
                        modifier = Modifier
                            .padding(end = 64.dp) // 48dp (column pill) + 16dp (spacing)
                            .align(Alignment.TopEnd)
                            .graphicsLayer {
                                alpha = threeDotsAlpha
                                scaleX = threeDotsScale
                                scaleY = threeDotsScale
                            }
                    ) {
                        LiquidGlassPill(
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            IconButton(onClick = { if (!isExportMenuOpen && !isScenesMenuOpen) { /* TODO: Open settings menu */ } }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "More Options", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // EXPORT BUTTON / MENU
                    val exportAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isScenesMenuOpen) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    val exportScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isScenesMenuOpen) 1f else 0.8f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .graphicsLayer {
                                alpha = exportAlpha
                                scaleX = exportScale
                                scaleY = exportScale
                            }
                    ) {
                        LiquidGlassPill(
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                        Box(modifier = Modifier.animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f))) {
                            if (!isExportMenuOpen) {
                                IconButton(
                                    onClick = { if (!isScenesMenuOpen) isExportMenuOpen = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.IosShare, "Export", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Column(
                                    modifier = Modifier.width(180.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { exportProjectLauncher.launch("Project.draftit") }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("As a Project", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isDarkTheme) Color.White else Color.Black)
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { exportPdfLauncher.launch("Board.pdf") }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("As PDF", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isDarkTheme) Color.White else Color.Black)
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { exportPictureLauncher.launch("Board.png") }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("As Picture", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isDarkTheme) Color.White else Color.Black)
                                        Icon(Icons.Default.Image, contentDescription = null, tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        }
                    }
                    
                    // SCENES MENU (Morphing)
                    val scenesAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    val scenesScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen) 1f else 0.8f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 64.dp) // 48dp (Export pill height) + 16dp gap
                            .align(Alignment.TopEnd)
                            .graphicsLayer {
                                alpha = scenesAlpha
                                scaleX = scenesScale
                                scaleY = scenesScale
                            }
                    ) {
                        LiquidGlassPill(
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                        Box(modifier = Modifier.animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f))) {
                            if (!isScenesMenuOpen) {
                                IconButton(onClick = { if (!isExportMenuOpen) isScenesMenuOpen = true }, modifier = Modifier.size(48.dp)) {
                                    Icon(painterResource(id = LucideR.drawable.lucide_ic_aperture), contentDescription = "Scenes", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                val scenes by viewModel.scenes.collectAsStateWithLifecycle()
                                ScenesMenu(
                                    scenes = scenes,
                                    projectId = viewModel.projectId,
                                    onClose = { isScenesMenuOpen = false },
                                    onCaptureScene = {
                                        coroutineScope.launch {
                                            try {
                                                viewModel.addScene("Scene ${scenes.size + 1}", offset.x, offset.y, scale)
                                            } catch (e: Exception) {
                                                // Ignore thumbnail failure
                                            }
                                        }
                                    },
                                    onNavigateToScene = { navigateToScene(it) },
                                    onRenameScene = { id, newName -> viewModel.renameScene(id, newName) },
                                    onDeleteScene = { viewModel.removeScene(it) }
                                )
                            }
                        }
                        }
                    }
                }

                val showConnectorMenu = selectedConnectorId != null || isConnectingMode
                val activeConnector = when {
                    selectedConnectorId != null -> {
                        val connIndex = elements.indexOfFirst { it.id == selectedConnectorId }
                        if (connIndex != -1) elements[connIndex] as CanvasElement.Connector else null
                    }
                    isConnectingMode -> defaultConnectorStyle
                    else -> null
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showConnectorMenu && activeConnector != null,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { 68 }, // Starts inside the main bottom nav bar
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { 68 },
                        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp)
                ) {
                    if (activeConnector != null) {
                        val isEditingExisting = selectedConnectorId != null
                        ConnectorStyleMenu(
                            connector = activeConnector,
                            onUpdate = { updated ->
                                if (isEditingExisting) {
                                    val connIndex = elements.indexOfFirst { it.id == selectedConnectorId }
                                    if (connIndex != -1) { elements[connIndex] = updated; viewModel.updateElements(elements.toList()) }
                                } else {
                                    defaultConnectorStyle = updated
                                }
                            },
                            onDelete = {
                                if (isEditingExisting) {
                                    val connIndex = elements.indexOfFirst { it.id == selectedConnectorId }
                                    if (connIndex != -1) { elements.removeAt(connIndex); selectedConnectorId = null; viewModel.updateElements(elements.toList()) }
                                } else {
                                    isConnectingMode = false; connectingStartNodeId = null
                                }
                            },
                            onClose = {
                                if (isEditingExisting) { selectedConnectorId = null }
                                else { isConnectingMode = false; connectingStartNodeId = null }
                            }
                        )
                    }
                }

                // --- PREMIUM BOTTOM WORKSPACE BAR ---
                Box(modifier = Modifier.fillMaxSize()) {
                    val activeMainTool = when {
                        isDrawingMode -> "Draw"
                        else -> "Pan"
                    }
                    val deselectAll = { isDrawingMode = false; isConnectingMode = false; isLassoMode = false; connectingStartNodeId = null; selectedGroupIds = emptySet(); isInsertMenuOpen = false }

                    // --- NEW: THE VERTICAL DRAW TOOL STACK & POPOUTS ---
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 108.dp).zIndex(2f)) {
                        com.flash.draft.ui.canvas.tools.DrawToolStack(
                            isOpen = isDrawingMode && isDrawMenuOpen,
                            activeColor = activeDrawColor,
                            activeToolType = activeToolType,
                            activeSubMenu = activeSubMenu,
                            onToolChange = { tool ->
                                activeToolType = tool
                                activeSubMenu = if (tool != "Eraser") "Brush" else null
                            },
                            onColorClick = { activeSubMenu = if (activeSubMenu == "Color") null else "Color" },
                            onThicknessClick = { activeSubMenu = if (activeSubMenu == "Thickness") null else "Thickness" },
                            activeStrokeWidth = activeStrokeWidth,
                            onStrokeWidthChange = { activeStrokeWidth = it },
                            onColorSelect = { activeDrawColor = it },
                            onRainbowClick = { activeSubMenu = "CustomColor" }
                        )
                    }

                    // Group Selection Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedGroupIds.isNotEmpty(),
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -50 }),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -50 }),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
                    ) {
                        LiquidGlassPill {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${selectedGroupIds.size} Items Selected",
                                    color = if (isDarkTheme) Color.White else Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                IconButton(
                                    onClick = {
                                        // Remove all selected elements
                                        elements.removeAll { it.id in selectedGroupIds }
                                        // Also clean up any connectors attached to them
                                        elements.removeAll { it is CanvasElement.Connector && (it.startId in selectedGroupIds || it.endId in selectedGroupIds) }
                                        viewModel.updateElements(elements.toList())
                                        selectedGroupIds = emptySet()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                                        contentDescription = "Delete Group",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f)) {
                        BottomNavBar(
                            activeTool = activeMainTool,
                            onToolSelect = { tool ->
                                deselectAll()
                                isDrawMenuOpen = false // Close menu when switching tools
                                when (tool) {
                                    "Draw" -> isDrawingMode = true
                                    "Pan" -> { /* Defaults to panning */ }
                                    "Text" -> { elements.add(CanvasElement.LabelNode(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale))); viewModel.updateElements(elements.toList()) }
                                }
                            },
                            isInsertMenuOpen = isInsertMenuOpen,
                            onAddClick = {
                                isInsertMenuOpen = !isInsertMenuOpen
                                isDrawMenuOpen = false
                                if (isInsertMenuOpen && isDrawingMode) {
                                    isDrawingMode = false
                                }
                            },
                            showDynamicLeft = isDrawingMode,
                            onDynamicLeftClick = {
                                isDrawMenuOpen = !isDrawMenuOpen // Toggles the vertical stack!
                            },
                            onPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")); isInsertMenuOpen = false },
                            onMediaClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); isInsertMenuOpen = false },
                            onCodeClick = {
                                elements.add(CanvasElement.CodeBlock(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)))
                                viewModel.updateElements(elements.toList())
                                isInsertMenuOpen = false
                            },
                            onAudioClick = {
                                elements.add(CanvasElement.AudioNote(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)))
                                viewModel.updateElements(elements.toList())
                                isInsertMenuOpen = false
                            },
                            onTaskClick = {
                                elements.add(CanvasElement.TaskListNode(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)))
                                viewModel.updateElements(elements.toList())
                                isInsertMenuOpen = false
                            },
                            onNoteClick = {
                                elements.add(CanvasElement.StickyNote(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), offset, scale)))
                                viewModel.updateElements(elements.toList())
                                isInsertMenuOpen = false
                            },
                            dynamicLeftContent = {
                                AnimatedContent(
                                    targetState = isDrawMenuOpen to activeSubMenu,
                                    transitionSpec = {
                                        (androidx.compose.animation.scaleIn(tween(200)) + fadeIn(tween(200)))
                                            .togetherWith(androidx.compose.animation.scaleOut(tween(200)) + fadeOut(tween(200)))
                                    },
                                    label = "iconMorph"
                                ) { (isOpen, subMenu) ->
                                    if (isOpen && (subMenu == "Color" || subMenu == "CustomColor")) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(activeDrawColor, CircleShape)
                                                .border(2.dp, if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f), CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isOpen) Icons.Default.Close else when (activeToolType) {
                                                "Eraser" -> Icons.Default.CleaningServices
                                                "Pen" -> Icons.Default.Create
                                                "Brush" -> Icons.Default.Brush
                                                "Highlighter" -> Icons.Default.AutoFixHigh
                                                else -> Icons.Default.Brush
                                            },
                                            contentDescription = "Draw Menu",
                                            tint = if (isOpen) (if (isDarkTheme) Color.White else Color.Black) else activeDrawColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            } // END OF UI VISIBILITY BLOCK
        }
    }
}