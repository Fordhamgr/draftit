package com.flash.draft.ui.canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CheckCircle

import com.flash.draft.model.BackgroundStyle
import com.flash.draft.ui.canvas.CanvasBackgroundTokens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import android.widget.Toast
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
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
import com.flash.draft.utils.ViewportMath

import com.flash.draft.ui.canvas.item.RenderCanvasElements
import com.flash.draft.ui.canvas.item.CanvasForegroundDrawPipeline
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.distancePointToSegment
import com.flash.draft.ui.canvas.item.getCenterPoint
import com.flash.draft.ui.canvas.item.getElementDimensions
import com.flash.draft.ui.canvas.item.getRectangleIntersection
import com.flash.draft.ui.canvas.item.isPointInElementBounds
import com.flash.draft.ui.canvas.item.findHitConnector

// --- LIQUID PLUGIN IMPORTS ---
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.IntSize

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

data class BoundingBox(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)
fun getElementsBoundingBox(elements: List<CanvasElement>, density: Float = 1f): BoundingBox? {
    if (elements.isEmpty()) return null
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
    var hasValidBounds = false

    elements.forEach { el ->
        when (el) {
            is CanvasElement.Connector -> {
                val startEl = elements.find { it.id == el.startId }
                val endEl = elements.find { it.id == el.endId }
                if (startEl != null && endEl != null) {
                    val p1 = getCenterPoint(startEl)
                    val p2 = getCenterPoint(endEl)
                    if (p1 != Offset.Unspecified && p2 != Offset.Unspecified) {
                        if (p1.x < minX) minX = p1.x
                        if (p1.x > maxX) maxX = p1.x
                        if (p1.y < minY) minY = p1.y
                        if (p1.y > maxY) maxY = p1.y
                        if (p2.x < minX) minX = p2.x
                        if (p2.x > maxX) maxX = p2.x
                        if (p2.y < minY) minY = p2.y
                        if (p2.y > maxY) maxY = p2.y
                        hasValidBounds = true
                    }
                }
            }
            is CanvasElement.Sketch -> {
                val halfStroke = (el.strokeWidth * density) / 2f
                for (i in el.xCoords.indices) {
                    val x = el.xCoords[i]
                    val y = el.yCoords[i]
                    if (x - halfStroke < minX) minX = x - halfStroke
                    if (x + halfStroke > maxX) maxX = x + halfStroke
                    if (y - halfStroke < minY) minY = y - halfStroke
                    if (y + halfStroke > maxY) maxY = y + halfStroke
                    hasValidBounds = true
                }
            }
            else -> {
                val pos = when (el) {
                    is CanvasElement.StickyNote -> el.position
                    is CanvasElement.ImageCard -> el.position
                    is CanvasElement.PdfCard -> el.position
                    is CanvasElement.CodeBlock -> el.position
                    is CanvasElement.AudioNote -> el.position
                    is CanvasElement.ShapeNode -> el.position
                    is CanvasElement.LabelNode -> el.position
                    is CanvasElement.FrameNode -> el.position
                    is CanvasElement.TaskListNode -> el.position
                    else -> null
                }
                if (pos != null) {
                    val dim = getElementDimensions(el)
                    val widthPx = dim.width * density
                    val heightPx = dim.height * density
                    val left = pos.x
                    val top = pos.y
                    val right = pos.x + widthPx
                    val bottom = pos.y + heightPx

                    if (left < minX) minX = left
                    if (top < minY) minY = top
                    if (right > maxX) maxX = right
                    if (bottom > maxY) maxY = bottom
                    hasValidBounds = true
                }
            }
        }
    }
    if (!hasValidBounds) return null
    return BoundingBox(minX, minY, maxX, maxY)
}

// Global Composition Locals
val LocalLiquidState = compositionLocalOf<LiquidState?> { null }
val LocalLiquidRefraction = compositionLocalOf { 0.5f }
val LocalLiquidCurve = compositionLocalOf { 0.5f }
val LocalLiquidFrost = compositionLocalOf { 0.0f }

@Composable
fun DraftCanvas(
    viewModel: CanvasViewModel,
    onBack: () -> Unit = {},
    onNavigateToBoard: (String) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(0.7f) }
    var lastZoomTime by remember { mutableLongStateOf(0L) }
    var isZooming by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    

    var centerX by remember { mutableFloatStateOf(0f) }
    var centerY by remember { mutableFloatStateOf(0f) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var selectedConnectorId by remember { mutableStateOf<String?>(null) }
    var defaultConnectorStyle by remember { mutableStateOf(CanvasElement.Connector(id = "", startId = "", endId = "", arrowDirection = "Both")) }

    val isDarkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
    val isLiquidDarkTheme by viewModel.isLiquidDarkTheme.collectAsStateWithLifecycle(initialValue = true)
    val isPerformanceMode by viewModel.isPerformanceMode.collectAsStateWithLifecycle(initialValue = false)
    val isStylusSupportEnabled by viewModel.isStylusSupportEnabled.collectAsStateWithLifecycle(initialValue = false)
    val recentColors by viewModel.recentColors.collectAsStateWithLifecycle(initialValue = emptyList())

    var isScenesMenuOpen by remember { mutableStateOf(false) }
    var isThreeDotsMenuOpen by remember { mutableStateOf(false) }
    var isCanvasLocked by remember { mutableStateOf(false) }
    var isBgMenuOpen by remember { mutableStateOf(false) }
    var sceneToExport by remember { mutableStateOf<com.flash.draft.model.Scene?>(null) }

    var isDrawMenuOpen by remember { mutableStateOf(false) }
    var isInsertMenuOpen by remember { mutableStateOf(false) }
    var activeSubMenu by remember { mutableStateOf<String?>(null) } // Tracks "Thickness", "Color", or "Brush"

    var isDrawingMode by remember { mutableStateOf(false) }

    var isConnectingMode by remember { mutableStateOf(false) }
    var connectingStartNodeId by remember { mutableStateOf<String?>(null) }

    var isLassoMode by remember { mutableStateOf(false) }
    val currentDrawingPath = remember { mutableListOf<InkPoint>() }
    val persistentBatch = remember { androidx.ink.strokes.MutableStrokeInputBatch() }
    var inFlightStrokeCount by remember { mutableStateOf(0) }
    val lassoPath = remember { mutableStateListOf<Offset>() }
    var selectedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newlySpawnedElementId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(newlySpawnedElementId) {
        if (newlySpawnedElementId != null) {
            kotlinx.coroutines.delay(2500)
            newlySpawnedElementId = null
        }
    }

    var activeDrawColor by remember { mutableStateOf(Color.Black) }
    var activeStrokeWidth by remember { mutableFloatStateOf(6f) }
    var activeToolType by remember { mutableStateOf("Pen") }
    var activeAlpha by remember { mutableFloatStateOf(1f) }

    // --- LIQUID STATE CONTROL ---
    val liquidState = rememberLiquidState()
    val liquidRefraction by viewModel.liquidRefraction.collectAsStateWithLifecycle(initialValue = 0.5f)
    val liquidCurve by viewModel.liquidCurve.collectAsStateWithLifecycle(initialValue = 0.5f)
    val liquidFrost by viewModel.liquidFrost.collectAsStateWithLifecycle(initialValue = 0.0f)
    var isExportMenuOpen by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isCapturingThumbnail by remember { mutableStateOf(false) }

    val closeAllMenus = {
        isExportMenuOpen = false
        isScenesMenuOpen = false
        isThreeDotsMenuOpen = false
        isInsertMenuOpen = false
        isDrawMenuOpen = false
        isBgMenuOpen = false
        if (activeSubMenu == "CustomColor" || activeSubMenu == "Color" || activeSubMenu == "Thickness") {
            activeSubMenu = null
        }
    }

    val resScale = if (isPerformanceMode && !isExporting) 0.7f else 1f
    val inverseScale = 1f / resScale

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    var panJob: Job? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()
    
    DisposableEffect(Unit) {
        onDispose { 
            // Removed redundant triggerImmediateSave to prevent race conditions during exit
        }
    }
    if (com.flash.draft.ui.canvas.item.CanvasProfiler.ENABLED) {
        SideEffect {
            com.flash.draft.ui.canvas.item.CanvasProfiler.recomposeCount++
        }
    }
    
    // PERF LOGGING: Monitor actual frame rate and detect stutters (gated by compile-time flag)
    if (com.flash.draft.ui.canvas.item.CanvasProfiler.ENABLED) {
        var frameCount by remember { mutableIntStateOf(0) }
        var lastFpsTime by remember { mutableLongStateOf(System.nanoTime()) }
        var maxFrameDelta by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            var lastFrameTime = System.nanoTime()
            while (isActive) {
                withFrameNanos { time ->
                    frameCount++
                    val dtMs = (time - lastFrameTime) / 1_000_000f
                    if (dtMs > maxFrameDelta) maxFrameDelta = dtMs
                    if (dtMs > 18f && lastFrameTime != 0L) {
                        Log.w("DraftCanvasPerf", "Frame Drop: ${dtMs}ms")
                    }
                    lastFrameTime = time
                    
                    if (time - lastFpsTime > 1_000_000_000L) {
                        val avgBg = if (com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount > 0) com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawTimeSum / com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount else 0f
                        val avgFg = if (com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount > 0) com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawTimeSum / com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount else 0f
                        val totalCpu = avgBg + avgFg
                        val totalCalls = com.flash.draft.ui.canvas.item.CanvasProfiler.bgDrawCount + com.flash.draft.ui.canvas.item.CanvasProfiler.fgDrawCount
                        
                        com.flash.draft.utils.DraftLogger.logUi("CanvasProfiler", "FPS: $frameCount | MaxFrame: ${String.format(java.util.Locale.US, "%.1f", maxFrameDelta)}ms | CPU Draw: ${String.format(java.util.Locale.US, "%.2f", totalCpu)}ms (BG: ${String.format(java.util.Locale.US, "%.2f", avgBg)}, FG: ${String.format(java.util.Locale.US, "%.2f", avgFg)}) | Recomp: ${com.flash.draft.ui.canvas.item.CanvasProfiler.recomposeCount} | Calls: $totalCalls")
                        
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
    }


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stableElements = remember { mutableStateOf<List<CanvasElement>>(emptyList()) }
    val elements = remember { mutableStateListOf<CanvasElement>() }

    var isUiVisible by remember { mutableStateOf(true) }
    val view = LocalView.current
    val graphicsLayer = rememberGraphicsLayer()

    fun navigateToScene(scene: com.flash.draft.model.Scene) {
        panJob?.cancel()
        coroutineScope.launch {
            val startScale = scale
            val startOffset = Offset(offsetX, offsetY)
            val targetOffset = Offset(scene.offsetX, scene.offsetY)
            val targetScale = scene.scale
            androidx.compose.animation.core.animate(
                initialValue = 0f, 
                targetValue = 1f, 
                animationSpec = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) { progress, _ ->
                scale = androidx.compose.ui.util.lerp(startScale, targetScale, progress)
                val lerped = androidx.compose.ui.geometry.lerp(startOffset, targetOffset, progress); offsetX = lerped.x; offsetY = lerped.y
            }
        }
    }

    @Suppress("unused")
    fun zoomToFit() {
        panJob?.cancel()
        if (elements.isEmpty()) return
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        elements.forEach { el ->
            if (el is CanvasElement.Connector) return@forEach
            val center = getCenterPoint(el.id, elements)
            if (center == Offset.Unspecified) return@forEach
            val dim = getElementDimensions(el.id, elements)
            val halfW = dim.width / 2f; val halfH = dim.height / 2f
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
            val startOffset = Offset(offsetX, offsetY)
            androidx.compose.animation.core.animate(
                initialValue = 0f, 
                targetValue = 1f, 
                animationSpec = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) { progress, _ ->
                scale = androidx.compose.ui.util.lerp(startScale, targetScale, progress)
                val lerped = androidx.compose.ui.geometry.lerp(startOffset, targetOffset, progress); offsetX = lerped.x; offsetY = lerped.y
            }
        }
    }

    LaunchedEffect(uiState.elements) {
        if (elements.toList() != uiState.elements) {
            elements.clear()
            elements.addAll(uiState.elements)
            stableElements.value = uiState.elements
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
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.w("DraftCanvas", "Failed to take persistable URI permission for PDF: ${e.message}")
            }
            val (fileName, fileSize) = getFileInfo(uri)
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)
            elements.add(CanvasElement.PdfCard(id = UUID.randomUUID().toString(), position = dropPos, uriString = uri.toString(), fileName = fileName, fileSize = fileSize))
            val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.w("DraftCanvas", "Failed to take persistable URI permission for Image: ${e.message}")
            }
            val dropPos = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)
            
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
            val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
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
            } catch (_: Exception) {
                Toast.makeText(context, "Error exporting project.", Toast.LENGTH_SHORT).show()
            }
            isExportMenuOpen = false
        }
    }

    var isExportingFull by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf<String?>(null) }
    var exportBbox by remember { mutableStateOf<BoundingBox?>(null) }
    var exportUri by remember { mutableStateOf<Uri?>(null) }
    var fullExportScale by remember { mutableFloatStateOf(1f) }
    var fullExportW by remember { mutableIntStateOf(0) }
    var fullExportH by remember { mutableIntStateOf(0) }
    val fullExportLayer = rememberGraphicsLayer()
    
    var exportResultMessage by remember { mutableStateOf<String?>(null) }
    var exportResultIsError by remember { mutableStateOf(false) }

    LaunchedEffect(exportResultMessage) {
        if (exportResultMessage != null) {
            delay(2500)
            exportResultMessage = null
        }
    }
    
    LaunchedEffect(isExportingFull) {
        if (isExportingFull && exportUri != null) {
            delay(300.milliseconds) // wait for recomposition and layout
            var swBitmap: android.graphics.Bitmap? = null
            try {
                val hwBitmap = fullExportLayer.toImageBitmap().asAndroidBitmap()
                swBitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                val bitmapToExport = swBitmap
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openOutputStream(exportUri!!)?.use { stream ->
                        if (exportFormat == "PDF") {
                            val pdfDocument = android.graphics.pdf.PdfDocument()
                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmapToExport.width, bitmapToExport.height, 1).create()
                            val page = pdfDocument.startPage(pageInfo)
                            page.canvas.drawBitmap(bitmapToExport, 0f, 0f, null)
                            pdfDocument.finishPage(page)
                            pdfDocument.writeTo(stream)
                            pdfDocument.close()
                        } else {
                            bitmapToExport.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        }
                    }
                }
                exportResultMessage = "Exported Successfully!"
                exportResultIsError = false
            } catch (e: Exception) {
                exportResultMessage = "Export Failed: ${e.message}"
                exportResultIsError = true
            } finally {
                swBitmap?.recycle()
                isExportingFull = false
                exportUri = null
                exportFormat = null
            }
        }
    }

    val exportPictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            val bbox = getElementsBoundingBox(elements.toList(), density)
            if (bbox != null) {
                exportBbox = bbox
                val padding = 400f
                val contentW = bbox.maxX - bbox.minX + (padding * 2)
                val contentH = bbox.maxY - bbox.minY + (padding * 2)
                fullExportScale = minOf(1f, 4000f / maxOf(contentW, contentH))
                fullExportW = (contentW * fullExportScale).toInt()
                fullExportH = (contentH * fullExportScale).toInt()
                exportUri = uri
                exportFormat = "PNG"
                isExportingFull = true
            } else {
                exportResultMessage = "Canvas is empty"
                exportResultIsError = true
            }
            isExportMenuOpen = false
        }
    }

    // Capture a thumbnail once when the user exits the canvas
    val handleExit: () -> Unit = {
        coroutineScope.launch {
            try {
                // 1. Briefly enable exporting so drawWithContent records into graphicsLayer
                isCapturingThumbnail = true
                delay(100.milliseconds)

                // 2. Capture the recorded layer
                val hwBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                val swBitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                isCapturingThumbnail = false

                // 3. Save thumbnail + project data
                if (swBitmap != null) {
                    viewModel.saveProjectThumbnailSync(swBitmap)
                }
                viewModel.triggerImmediateSaveSync()
            } catch (_: Exception) {
                // Ensure we always navigate even if capture fails
                isCapturingThumbnail = false
                viewModel.triggerImmediateSave()
            }
            onBack()
        }
        Unit
    }

    androidx.activity.compose.BackHandler {
        handleExit()
    }

    val quickSave = {
        coroutineScope.launch {
            var swBitmap: android.graphics.Bitmap? = null
            try {
                isExporting = true
                delay(100.milliseconds)
                val hwBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                isExporting = false
                
                swBitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Draft_${System.currentTimeMillis()}.png")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DraftIt")
                }
                
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val bitmapToSave = swBitmap
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            bitmapToSave.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        }
                    }
                    Toast.makeText(context, "Quick Saved to Gallery!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to create gallery entry.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                isExporting = false
                Toast.makeText(context, "Error saving picture.", Toast.LENGTH_SHORT).show()
            } finally {
                swBitmap?.recycle()
            }
        }
    }

    val exportScenePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val bbox = getElementsBoundingBox(elements.toList(), density)
            if (bbox != null) {
                exportBbox = bbox
                val padding = 400f
                val contentW = bbox.maxX - bbox.minX + (padding * 2)
                val contentH = bbox.maxY - bbox.minY + (padding * 2)
                fullExportScale = minOf(1f, 4000f / maxOf(contentW, contentH))
                fullExportW = (contentW * fullExportScale).toInt()
                fullExportH = (contentH * fullExportScale).toInt()
                exportUri = uri
                exportFormat = "PDF"
                isExportingFull = true
            } else {
                exportResultMessage = "Canvas is empty"
                exportResultIsError = true
            }
            sceneToExport = null
        }
    }

    fun bringToFront(noteId: String) {
        val index = elements.indexOfFirst { it.id == noteId }
        if (index > -1 && index != elements.lastIndex) {
            val note = elements.removeAt(index)
            elements.add(note)
            val snap = elements.toList()
            stableElements.value = snap
            viewModel.updateElements(snap)
        }
    }

    fun sendToBack(noteId: String) {
        val index = elements.indexOfFirst { it.id == noteId }
        if (index > 0) {
            val note = elements.removeAt(index)
            elements.add(0, note)
            val snap = elements.toList()
            stableElements.value = snap
            viewModel.updateElements(snap)
        }
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
        val snap = elements.toList()
        stableElements.value = snap
    }

    fun finishConnection(endNodeId: String) {
        val startId = connectingStartNodeId ?: return
        elements.add(CanvasElement.Connector(id = UUID.randomUUID().toString(), startId = startId, endId = endNodeId, color = defaultConnectorStyle.color, strokeWidth = defaultConnectorStyle.strokeWidth, lineStyle = defaultConnectorStyle.lineStyle, arrowDirection = defaultConnectorStyle.arrowDirection, arrowShape = defaultConnectorStyle.arrowShape))
        val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
        connectingStartNodeId = null; isConnectingMode = false
    }

    CompositionLocalProvider(
        LocalDarkTheme provides isDarkTheme,
        com.flash.draft.ui.canvas.item.LocalLiquidDarkTheme provides isLiquidDarkTheme,
        com.flash.draft.ui.canvas.item.LocalBackgroundStyle provides uiState.backgroundStyle,
        com.flash.draft.ui.canvas.item.LocalPerformanceMode provides (isPerformanceMode && !isExporting),

        LocalLiquidState provides liquidState,
        LocalLiquidRefraction provides liquidRefraction,
        LocalLiquidCurve provides liquidCurve,
        LocalLiquidFrost provides liquidFrost
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF18181A) else Color(0xFFF0F0F0))
                .onSizeChanged { newSize ->
                    centerX = newSize.width / 2f; centerY = newSize.height / 2f
                    if (offsetX == 0f && offsetY == 0f) { offsetX = centerX; offsetY = centerY }
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
                                    if (zoomAccumulator !in 0.97f..1.03f || panAccumulator.getDistance() > touchSlop) {
                                        pastSlop = true
                                        closeAllMenus()
                                    }
                                }

                                if (pastSlop) {
                                    val newScale = (scale * zoomChange).coerceIn(0.01f, 5f)
                                    val zoomFactor = newScale / scale
                                    val newOffsetX = centroid.x + (offsetX + panChange.x - centroid.x) * zoomFactor
                                    val newOffsetY = centroid.y + (offsetY + panChange.y - centroid.y) * zoomFactor
                                    offsetX = newOffsetX
                                    offsetY = newOffsetY
                                    scale = newScale
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
                .pointerInput(isDrawingMode, isLassoMode, isCanvasLocked, isStylusSupportEnabled) {
                    if (!isCanvasLocked && (isDrawingMode || isLassoMode)) {
                        detectDragGestures(
                            onDragStart = { startPos ->
                                closeAllMenus()
                                if (isLassoMode) { selectedGroupIds = emptySet(); lassoPath.clear(); lassoPath.add(ViewportMath.screenToWorld(startPos, Offset(offsetX, offsetY), scale)) }
                                else { 
                                    currentDrawingPath.clear(); persistentBatch.clear(); inFlightStrokeCount = 0; persistentBatch.clear(); inFlightStrokeCount = 0
                                    val worldPoint = ViewportMath.screenToWorld(startPos, Offset(offsetX, offsetY), scale)
                                    // Use a default time and pressure for the start point since detectDragGestures doesn't provide them initially
                                    currentDrawingPath.add(InkPoint(worldPoint.x, worldPoint.y, 1f, android.os.SystemClock.uptimeMillis())); persistentBatch.addOrThrow(androidx.ink.brush.InputToolType.UNKNOWN, worldPoint.x, worldPoint.y, android.os.SystemClock.uptimeMillis(), 1f); inFlightStrokeCount++
                                }
                            },
                            onDrag = { change, _ ->
                                // Optional Palm Rejection: If Stylus Support is on, and it's a touch event, we could ignore it, but for now we just take the pressure.
                                val isStylus = isStylusSupportEnabled && (change.type == PointerType.Stylus || change.type == PointerType.Eraser)
                                
                                val newPoint = ViewportMath.screenToWorld(change.position, Offset(offsetX, offsetY), scale)
                                if (isLassoMode) { lassoPath.add(newPoint) }
                                else {
                                    if (activeToolType == "Eraser" || (isStylus && change.type == PointerType.Eraser)) {
                                        val eraseRadius = activeStrokeWidth * 1.5f
                                        elements.removeAll { el ->
                                            el is CanvasElement.Sketch && el.xCoords.indices.any { i -> (Offset(el.xCoords[i], el.yCoords[i]) - newPoint).getDistance() < eraseRadius }
                                        }
                                        // Defer viewModel.updateElements to onDragEnd for smooth erasing!
                                    } else {
                                        val lastPoint = currentDrawingPath.lastOrNull()
                                        if (lastPoint == null || (Offset(newPoint.x, newPoint.y) - Offset(lastPoint.x, lastPoint.y)).getDistance() > (3f / scale)) { 
                                            val pressure = if (isStylus) change.pressure else 1f
                                            currentDrawingPath.add(InkPoint(newPoint.x, newPoint.y, pressure, change.uptimeMillis)); persistentBatch.addOrThrow(androidx.ink.brush.InputToolType.UNKNOWN, newPoint.x, newPoint.y, change.uptimeMillis, pressure); inFlightStrokeCount++ 
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                if (activeToolType == "Eraser") {
                                    val vmElements = uiState.elements
                                    elements.clear()
                                    elements.addAll(vmElements)
                                    stableElements.value = vmElements
                                }
                                currentDrawingPath.clear(); persistentBatch.clear(); inFlightStrokeCount = 0; persistentBatch.clear(); inFlightStrokeCount = 0
                                lassoPath.clear()
                            },
                            onDragEnd = {
                                if (isLassoMode && lassoPath.size > 2) {
                                    val capturedIds = mutableSetOf<String>()
                                    elements.forEach { el ->
                                        val center = getCenterPoint(el.id, elements)
                                        if (center != Offset.Unspecified && ViewportMath.isPointInPolygon(center, lassoPath)) capturedIds.add(el.id)
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
                                    val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                    currentDrawingPath.clear(); persistentBatch.clear(); inFlightStrokeCount = 0; persistentBatch.clear(); inFlightStrokeCount = 0
                                } else if (activeToolType == "Eraser") {
                                    val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                    currentDrawingPath.clear(); persistentBatch.clear(); inFlightStrokeCount = 0; persistentBatch.clear(); inFlightStrokeCount = 0
                                }
                            }
                        )
                    } else {
                        val velocityTracker = VelocityTracker()
                        detectDragGestures(
                            onDragStart = { panJob?.cancel(); velocityTracker.resetTracking(); closeAllMenus() },
                            onDrag = { change, dragAmount -> velocityTracker.addPosition(change.uptimeMillis, change.position); offsetX += dragAmount.x; offsetY += dragAmount.y },
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
                                            offsetX += currentVelocity.x * dt
                                            offsetY += currentVelocity.y * dt
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
                            val worldTap = ViewportMath.screenToWorld(tapPos, Offset(offsetX, offsetY), scale)
                            val hitConnector = findHitConnector(worldTap, elements, (24f / scale).coerceIn(12f, 120f))
                            if (hitConnector != null) {
                                selectedConnectorId = hitConnector.id
                                selectedNoteId = null
                                selectedGroupIds = emptySet()
                            } else {
                                val isTappingCard = elements.any { el ->
                                    el !is CanvasElement.Connector && el !is CanvasElement.Sketch && isPointInElementBounds(worldTap, el)
                                }
                                if (!isTappingCard) {
                                    selectedConnectorId = null
                                    selectedNoteId = null
                                    connectingStartNodeId = null
                                    isConnectingMode = false
                                    selectedGroupIds = emptySet()
                                    focusManager.clearFocus()
                                }
                            }
                        }
                    )
                }
        ) {
                // Recomposition log removed — was firing every frame during pan/zoom, adding overhead

                // Culling removed: GPU graphicsLayer efficiently clips off-screen elements natively.

                // GridBackground(scaleProvider = { scale }, offsetProvider = { Offset(offsetX, offsetY) })

                // --- 1. RECORDING BACKGROUND ---
                val colorSchemeRec = androidx.compose.material3.MaterialTheme.colorScheme
                Box(modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(liquidState)
                    .drawWithContent {
                        if (isExporting || isCapturingThumbnail) {
                            // Only record into graphicsLayer when actually exporting.
                            // This eliminates a full canvas double-draw on every frame.
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                        }
                        drawContent()
                    }
                    .draftBackground(
                        style = uiState.backgroundStyle,
                        isDarkTheme = isDarkTheme,
                        colorScheme = colorSchemeRec,
                        scaleProvider = { scale },
                        offsetProvider = { Offset(offsetX, offsetY) }
                    )
                ) {
                // Outer Box: Scales up the inner content
                Box(modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = inverseScale
                        scaleY = inverseScale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    }
                ) {
                    // Inner Box: Render at lower resolution
                    Box(modifier = Modifier
                        .fillMaxWidth(resScale)
                        .fillMaxHeight(resScale)
                        .then(
                            if (isPerformanceMode && !isExporting) Modifier.graphicsLayer {
                                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                            } else Modifier
                        )
                    ) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offsetX * resScale
                                translationY = offsetY * resScale
                                scaleX = scale * resScale
                                scaleY = scale * resScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            }
                        ) {
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
                    onUpdateSelectedNoteId = { updatedSelectedNoteId: String? ->
                        selectedNoteId = updatedSelectedNoteId
                        if (updatedSelectedNoteId != null) selectedConnectorId = null
                    },
                    onUpdateConnectingStartNodeId = { updatedConnectingStartNodeId: String? -> connectingStartNodeId = updatedConnectingStartNodeId },
                    onUpdateScaleAndOffset = { newScale: Float, newOffset: Offset -> scale = newScale; offsetX = newOffset.x; offsetY = newOffset.y },
                    onStartConnection = { startNodeId: String -> 
                        isConnectingMode = true
                        connectingStartNodeId = startNodeId
                    },
                    onNavigateToScene = { sceneIdentifier ->
                        val scene = uiState.scenes.find { it.id == sceneIdentifier || it.name == sceneIdentifier }
                        if (scene != null) navigateToScene(scene)
                    },
                    onNavigateToBoard = onNavigateToBoard,
                    isCanvasLocked = isCanvasLocked,
                    highlightedElementId = newlySpawnedElementId,
                    selectedConnectorId = selectedConnectorId,
                    onUpdateSelectedConnectorId = { selectedConnectorId = it }
                )

                        CanvasForegroundDrawPipeline(
                            elements = elements,
                            cameraOffsetProvider = { Offset(offsetX, offsetY) },
                            cameraScaleProvider = { scale },
                            isDarkTheme = isDarkTheme,
                            selectedGroupIds = selectedGroupIds,
                            inFlightStrokeBatch = persistentBatch, inFlightStrokeCount = inFlightStrokeCount,
                            activeToolType = activeToolType,
                            activeDrawColor = activeDrawColor,
                            activeAlpha = activeAlpha,
                            activeStrokeWidth = activeStrokeWidth,
                            lassoPath = lassoPath
                        )
                    }
                    } // End inner resScale Box
                } // End outer inverseScale Box
            } // End RECORDING BACKGROUND drawWithContent Box

            // --- 3. UI OVERLAYS (Outside the recording layer so UI doesn't get captured!) ---
            if (isUiVisible) {

                // --- TOP LEFT ACTION BAR (BACK + UNDO/REDO + ZOOM) ---
                Box(modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                            LiquidGlassPill(
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                IconButton(onClick = { handleExit() }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isCanvasLocked && !isThreeDotsMenuOpen,
                                enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                            ) {
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
                                            Icon(painterResource(id = LucideR.drawable.lucide_ic_undo), contentDescription = "Undo", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { viewModel.redo() }, modifier = Modifier.size(28.dp)) {
                                            Icon(painterResource(id = LucideR.drawable.lucide_ic_redo), contentDescription = "Redo", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                        }
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
                    visible = isZooming && !isCanvasLocked,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                ) {
                    LiquidGlassPill(shape = CircleShape, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("${(scale * 100).toInt()}%", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // CLICK OUTSIDE OVERLAY FOR ALL MENUS
                if (isExportMenuOpen || isScenesMenuOpen || isThreeDotsMenuOpen || isInsertMenuOpen || (isDrawingMode && isDrawMenuOpen) || isBgMenuOpen) {
                    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { closeAllMenus() }
                        )
                    })
                }

                // --- TOP RIGHT MENU (3 DOTS + EXPORT + SCENES) ---
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp)) {
                    
                    // LEFT COLUMN: MORE OPTIONS (3 DOTS)
                    val threeDotsAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen && !isScenesMenuOpen && !isCanvasLocked) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    val threeDotsScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen && !isScenesMenuOpen && !isCanvasLocked) 1f else 0.8f,
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
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(modifier = Modifier.animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f))) {
                                if (!isThreeDotsMenuOpen) {
                                    IconButton(
                                        onClick = { if (!isExportMenuOpen && !isScenesMenuOpen && !isCanvasLocked) isThreeDotsMenuOpen = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.MoreHoriz, contentDescription = "More Options", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.width(180.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { isCanvasLocked = true; isThreeDotsMenuOpen = false }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Lock Canvas", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { quickSave(); isThreeDotsMenuOpen = false }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Quick Save", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { viewModel.clearCanvas(); isThreeDotsMenuOpen = false }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Reset Canvas", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
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
                            if (isCanvasLocked) {
                                IconButton(
                                    onClick = { isCanvasLocked = false },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, "Unlock", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                }
                            } else if (!isExportMenuOpen) {
                                IconButton(
                                    onClick = { if (!isScenesMenuOpen && !isThreeDotsMenuOpen) isExportMenuOpen = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.IosShare, "Export", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Column(
                                    modifier = Modifier.width(180.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { exportProjectLauncher.launch("Project.draftit") }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("As a Project", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { 
                                        sceneToExport = com.flash.draft.model.Scene(id = "current", name = "Current", offsetX = offsetX, offsetY = offsetY, scale = scale)
                                        exportScenePdfLauncher.launch("Board.pdf") 
                                    }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("As PDF", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { exportPictureLauncher.launch("Board.png") }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("As Picture", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.Default.Image, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        }
                    }
                    
                    // SCENES MENU (Morphing)
                    val scenesAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen && !isCanvasLocked) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                    val scenesScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (!isExportMenuOpen && !isCanvasLocked) 1f else 0.8f,
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
                                    Icon(painterResource(id = LucideR.drawable.lucide_ic_aperture), contentDescription = "Scenes", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                val scenes = uiState.scenes
                                ScenesMenu(
                                    scenes = scenes,
                                    projectId = viewModel.projectId,
                                    onClose = { isScenesMenuOpen = false },
                                    onCaptureScene = {
                                        coroutineScope.launch {
                                            try {
                                                viewModel.addScene("Scene ${scenes.size + 1}", Offset(offsetX, offsetY).x, Offset(offsetX, offsetY).y, scale)
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
                    
                } // End of TopEnd container Box

                // BACKGROUND MENU (Morphing)
                val isPanMode = !isDrawingMode && !isConnectingMode && !isLassoMode
                val shouldShowBgMenu = isPanMode && !isInsertMenuOpen && !isDrawMenuOpen && !isCanvasLocked
                val bgMenuAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (shouldShowBgMenu) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(200)
                )
                val bgMenuScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (shouldShowBgMenu) 1f else 0.8f,
                    animationSpec = androidx.compose.animation.core.tween(200)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 140.dp, end = 24.dp)
                        .graphicsLayer {
                            alpha = bgMenuAlpha
                            scaleX = bgMenuScale
                            scaleY = bgMenuScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                        }
                ) {
                    LiquidGlassPill(
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(modifier = Modifier.animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f))) {
                            if (!isBgMenuOpen) {
                                IconButton(
                                    onClick = { isBgMenuOpen = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(painterResource(id = LucideR.drawable.lucide_ic_palette), contentDescription = "Background Style", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Column(
                                    modifier = Modifier.width(160.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    BackgroundStyle.entries.forEach { style ->
                                        val isSelected = style == uiState.backgroundStyle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { 
                                                    viewModel.onEvent(CanvasUiEvent.ChangeBackgroundStyle(style))
                                                    isBgMenuOpen = false
                                                }
                                                .background(if (isSelected) (if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)) else Color.Transparent)
                                                .padding(vertical = 14.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically, 
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val displayName = when (style) {
                                                BackgroundStyle.Solid -> "Solid"
                                                BackgroundStyle.GraphPaper -> "Graph Paper"
                                                BackgroundStyle.Blueprint -> "Blueprint"
                                                BackgroundStyle.Greenprint -> "Greenprint"
                                                BackgroundStyle.BlackPaper -> "Black Paper"
                                            }
                                            Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                            if (isSelected) {
                                                Icon(painterResource(id = LucideR.drawable.lucide_ic_check), contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // HIGH QUALITY OFF-SCREEN EXPORT
                if (isExportingFull && exportBbox != null) {
                    androidx.activity.compose.BackHandler(enabled = isExportingFull) { }

                    Box(
                        modifier = Modifier
                            .offset(x = 10000.dp)
                            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                            .layout { measurable, _ ->
                                val placeable = measurable.measure(
                                    androidx.compose.ui.unit.Constraints.fixed(fullExportW, fullExportH)
                                )
                                layout(fullExportW, fullExportH) {
                                    placeable.place(0, 0)
                                }
                            }
                            .drawWithContent {
                                fullExportLayer.record(size = IntSize(fullExportW, fullExportH)) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    ) {
                        // Layer 1: Background and grid covering full export target size
                        Box(
                            modifier = Modifier
                                .layout { measurable, _ ->
                                    val placeable = measurable.measure(
                                        androidx.compose.ui.unit.Constraints.fixed(fullExportW, fullExportH)
                                    )
                                    layout(fullExportW, fullExportH) {
                                        placeable.place(0, 0)
                                    }
                                }
                                .drawExportBackground(
                                    style = uiState.backgroundStyle,
                                    isDarkTheme = isDarkTheme,
                                    colorScheme = colorSchemeRec,
                                    exportScale = fullExportScale,
                                    worldOriginX = exportBbox!!.minX - 400f,
                                    worldOriginY = exportBbox!!.minY - 400f,
                                    exportWidth = fullExportW.toFloat(),
                                    exportHeight = fullExportH.toFloat()
                                )
                        )

                        // Layer 2: Transformed content elements
                        Box(
                            modifier = Modifier
                                .layout { measurable, _ ->
                                    val placeable = measurable.measure(
                                        androidx.compose.ui.unit.Constraints.fixed(fullExportW, fullExportH)
                                    )
                                    layout(fullExportW, fullExportH) {
                                        placeable.place(0, 0)
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = fullExportScale
                                    scaleY = fullExportScale
                                    translationX = - (exportBbox!!.minX - 400f) * fullExportScale
                                    translationY = - (exportBbox!!.minY - 400f) * fullExportScale
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                                }
                        ) {
                            RenderCanvasElements(
                                elements = elements,
                                scale = 1f,
                                offset = Offset.Zero,
                                selectedNoteId = null,
                                selectedGroupIds = emptySet(),
                                connectingStartNodeId = null,
                                isConnectingMode = false,
                                isDrawingMode = false,
                                isLassoMode = false,
                                centerX = fullExportW / 2f,
                                centerY = fullExportH / 2f,
                                context = context,
                                viewModel = viewModel,
                                onUpdateElements = {},
                                onMoveGroup = { _, _ -> },
                                onFinishConnection = {},
                                onSendToBack = {},
                                onBringToFront = {},
                                onResetSelectedNoteId = {},
                                onResetConnectingStartNodeId = {},
                                onResetIsConnectingMode = {},
                                onUpdateSelectedGroupIds = {},
                                onUpdateSelectedNoteId = {},
                                onUpdateConnectingStartNodeId = {},
                                onUpdateScaleAndOffset = { _, _ -> },
                                onStartConnection = {},
                                onNavigateToScene = {},
                                onNavigateToBoard = {},
                                isCanvasLocked = true
                            )
                            CanvasForegroundDrawPipeline(
                                elements = elements,
                                cameraScaleProvider = { 1f },
                                cameraOffsetProvider = { Offset.Zero },
                                isDarkTheme = isDarkTheme,
                                selectedGroupIds = emptySet(),
                                inFlightStrokeBatch = null, inFlightStrokeCount = 0,
                                activeToolType = "Pen",
                                activeDrawColor = Color.Black,
                                activeAlpha = 1f,
                                activeStrokeWidth = 1f,
                                lassoPath = emptyList()
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(10f)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent().changes.forEach { it.consume() }
                                    }
                                }
                            }, 
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassPill(
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "Exporting High-Quality Canvas...", 
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, 
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = exportResultMessage != null,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                    modifier = Modifier.align(Alignment.Center).zIndex(5f)
                ) {
                    LiquidGlassPill(
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = if (exportResultIsError) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (exportResultIsError) androidx.compose.material3.MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = exportResultMessage ?: "",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
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
                    visible = showConnectorMenu && activeConnector != null && !isCanvasLocked,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { 68 }, // Starts inside the main bottom nav bar
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { 68 },
                        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    if (activeConnector != null) {
                        val isEditingExisting = selectedConnectorId != null
                        ConnectorStyleMenu(
                            connector = activeConnector,
                            onUpdate = { updated ->
                                if (isEditingExisting) {
                                    val connIndex = elements.indexOfFirst { it.id == selectedConnectorId }
                                    if (connIndex != -1) { elements[connIndex] = updated; val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap) }
                                } else {
                                    defaultConnectorStyle = updated
                                }
                            },
                            onDelete = {
                                if (isEditingExisting) {
                                    val connIndex = elements.indexOfFirst { it.id == selectedConnectorId }
                                    if (connIndex != -1) { elements.removeAt(connIndex); selectedConnectorId = null; val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap) }
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isCanvasLocked,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val activeMainTool = when {
                        isDrawingMode -> "Draw"
                        else -> "Pan"
                    }
                    val deselectAll = { isDrawingMode = false; isConnectingMode = false; isLassoMode = false; connectingStartNodeId = null; selectedGroupIds = emptySet(); isInsertMenuOpen = false }

                    // --- NEW: THE VERTICAL DRAW TOOL STACK & POPOUTS ---
                    // Draw tools are now embedded inside the morphing BottomNavBar

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
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
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
                                        val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
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

                    Box(modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f), contentAlignment = Alignment.BottomCenter) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !(showConnectorMenu && activeConnector != null && !isCanvasLocked),
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 })
                        ) {
                            BottomNavBar(
                                activeTool = activeMainTool,
                            onToolSelect = { tool ->
                                deselectAll()
                                isDrawMenuOpen = false // Close menu when switching tools
                                when (tool) {
                                    "Draw" -> isDrawingMode = true
                                    "Pan" -> { /* Defaults to panning */ }
                                    "Text" -> {
                                        val newId = UUID.randomUUID().toString()
                                        elements.add(
                                            CanvasElement.LabelNode(
                                                id = newId,
                                                position = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)
                                            )
                                        )
                                        val snap = elements.toList()
                                        stableElements.value = snap
                                        viewModel.updateElements(snap)
                                        newlySpawnedElementId = newId
                                    }
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
                                isDrawMenuOpen = !isDrawMenuOpen // Toggles the morphing stack!
                            },
                            isDrawToolsExpanded = isDrawingMode && isDrawMenuOpen,
                            expandedToolsContent = {
                                com.flash.draft.ui.canvas.tools.DrawToolsContent(
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
                                    onRainbowClick = { activeSubMenu = "CustomColor" },
                                    onClose = { isDrawMenuOpen = false },
                                    recentColors = recentColors,
                                    onCommitRecentColor = { viewModel.addRecentColor(it) }
                                )
                            },
                            onPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")); isInsertMenuOpen = false },
                            onMediaClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); isInsertMenuOpen = false },
                            onCodeClick = {
                                elements.add(CanvasElement.CodeBlock(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)))
                                val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                isInsertMenuOpen = false
                            },
                            onAudioClick = {
                                elements.add(CanvasElement.AudioNote(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)))
                                val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                isInsertMenuOpen = false
                            },
                            onTaskClick = {
                                elements.add(CanvasElement.TaskListNode(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)))
                                val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                isInsertMenuOpen = false
                            },
                            onNoteClick = {
                                elements.add(CanvasElement.StickyNote(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)))
                                val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                isInsertMenuOpen = false
                            },
                            onShapeClick = {
                                elements.add(CanvasElement.ShapeNode(id = UUID.randomUUID().toString(), position = ViewportMath.screenToWorld(Offset(centerX, centerY), Offset(offsetX, offsetY), scale)))
                                val snap = elements.toList(); stableElements.value = snap; viewModel.updateElements(snap)
                                isInsertMenuOpen = false
                            },
                            forceCenter = showConnectorMenu && activeConnector != null && !isCanvasLocked,
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
                                                .background(com.flash.draft.ui.canvas.item.getAdaptiveColor(activeDrawColor, isDarkTheme), CircleShape)
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
                                            tint = if (isOpen) (androidx.compose.material3.MaterialTheme.colorScheme.onSurface) else com.flash.draft.ui.canvas.item.getAdaptiveColor(activeDrawColor, isDarkTheme),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        )
                        }

                        // Floating Draw Tools (rendered outside dock for 3D effect)
                        com.flash.draft.ui.canvas.tools.FloatingDrawTools(
                            isVisible = isDrawingMode && isDrawMenuOpen && activeSubMenu != "Color" && activeSubMenu != "CustomColor" && activeSubMenu != "Thickness",
                            activeToolType = activeToolType,
                            onToolChange = { tool ->
                                activeToolType = tool
                                activeSubMenu = if (tool != "Eraser") "Brush" else null
                            },
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(bottom = 84.dp)
                        )
                    }
                }
                } // END OF BOTTOM WORKSPACE BAR VISIBILITY
            } // END OF UI VISIBILITY BLOCK
        }
    }
}










