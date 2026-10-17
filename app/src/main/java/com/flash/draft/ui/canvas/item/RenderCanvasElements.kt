package com.flash.draft.ui.canvas.item

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.flash.draft.model.CanvasElement
import com.flash.draft.model.CanvasElementCloner
import com.flash.draft.ui.canvas.* // Pulls in FrameItem, StickyNoteItem, etc.
import com.flash.draft.utils.StickerMaker
import java.util.UUID
import androidx.compose.runtime.getValue

@Composable
fun RenderCanvasElements(
    elements: MutableList<CanvasElement>,
    scale: Float,
    offset: Offset,
    selectedNoteId: String?,
    selectedGroupIds: Set<String>,
    connectingStartNodeId: String?,
    isConnectingMode: Boolean,
    isDrawingMode: Boolean,
    isLassoMode: Boolean,
    centerX: Float,
    centerY: Float,
    context: Context,
    viewModel: CanvasViewModel,
    onUpdateElements: (List<CanvasElement>) -> Unit,
    onMoveGroup: (Offset, String) -> Unit,
    onFinishConnection: (String) -> Unit,
    onSendToBack: (String) -> Unit,
    onBringToFront: (String) -> Unit,
    onResetSelectedNoteId: () -> Unit,
    onResetConnectingStartNodeId: () -> Unit,
    onResetIsConnectingMode: () -> Unit,
    onUpdateSelectedGroupIds: (Set<String>) -> Unit,
    onUpdateSelectedNoteId: (String?) -> Unit,
    onUpdateConnectingStartNodeId: (String?) -> Unit,
    onUpdateScaleAndOffset: (Float, Offset) -> Unit,
    onStartConnection: (String) -> Unit,
    onNavigateToScene: (String) -> Unit,
    onNavigateToBoard: (String) -> Unit,
    isCanvasLocked: Boolean,
    highlightedElementId: String? = null,
    selectedConnectorId: String? = null,
    onUpdateSelectedConnectorId: ((String?) -> Unit)? = null
) {
    // GPU graphicsLayer natively handles viewport culling, so we don't need CPU-side cull bounds.
    val currentSelectedGroupIds by androidx.compose.runtime.rememberUpdatedState(selectedGroupIds)
    val currentConnectingStartNodeId by androidx.compose.runtime.rememberUpdatedState(connectingStartNodeId)
    val currentIsConnectingMode by androidx.compose.runtime.rememberUpdatedState(isConnectingMode)
    val currentIsDrawingMode by androidx.compose.runtime.rememberUpdatedState(isDrawingMode)
    val currentIsLassoMode by androidx.compose.runtime.rememberUpdatedState(isLassoMode)

    val duplicateElementById: (String) -> Unit = remember {
        { targetId ->
            val idx = elements.indexOfFirst { it.id == targetId }
            if (idx != -1) {
                val liveInstance = elements[idx]
                val cloned = CanvasElementCloner.deepClone(liveInstance)
                elements.add(cloned)
                onUpdateSelectedGroupIds(emptySet())
                onUpdateSelectedNoteId(cloned.id)
                onUpdateSelectedConnectorId?.invoke(null)
                onUpdateElements(elements.toList())
            }
        }
    }

    // Partition elements into backdrop elements and foreground elements.
    // Backdrop: Any FrameNode, plus any ShapeNode positioned at the back (before non-backdrop items)
    val leadingBackdropCount = elements.takeWhile { it is CanvasElement.FrameNode || it is CanvasElement.ShapeNode }.size
    val backdropElements = mutableListOf<CanvasElement>()
    val foregroundElements = mutableListOf<CanvasElement>()

    elements.forEachIndexed { index, el ->
        if (el is CanvasElement.Connector || el is CanvasElement.Sketch) {
            return@forEachIndexed
        }
        if (index < leadingBackdropCount || el is CanvasElement.FrameNode) {
            backdropElements.add(el)
        } else {
            foregroundElements.add(el)
        }
    }

    @Composable
    fun RenderSingleElement(element: CanvasElement) {
        when (element) {
            is CanvasElement.StickyNote -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    StickyNoteItem(
                        note = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedNote: CanvasElement.StickyNote ->
                            if (updatedNote.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedNote
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.StickyNote
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(100f),
                                    height = (el.height + delta.y).coerceAtLeast(100f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.CodeBlock -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    CodeSnippetItem(
                        codeBlock = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedBlock: CanvasElement.CodeBlock ->
                            if (updatedBlock.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedBlock
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.CodeBlock
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(150f),
                                    height = (el.height + delta.y).coerceAtLeast(100f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.ImageCard -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    ImageItem(
                        imageCard = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedImage: CanvasElement.ImageCard ->
                            if (updatedImage.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedImage
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.ImageCard
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(100f),
                                    height = (el.height + delta.y).coerceAtLeast(100f)
                                )
                            }
                        } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onMakeSticker = {
                            Toast.makeText(context, "Extracting subject...", Toast.LENGTH_SHORT).show()
                            StickerMaker.extractSubject(context, element.uriString) { newUri ->
                                if (newUri != null) {
                                    elements.add(CanvasElement.ImageCard(id = UUID.randomUUID().toString(), position = element.position + Offset(80f, 80f), uriString = newUri, width = element.width, height = element.height, originalWidth = element.originalWidth, originalHeight = element.originalHeight))
                                    onUpdateElements(elements.toList())
                                } else {
                                    Toast.makeText(context, "No clear subject found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.TaskListNode -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    TaskListItem(
                        taskNode = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedNode: CanvasElement.TaskListNode ->
                            if (updatedNode.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedNode
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.TaskListNode
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(150f),
                                    height = (el.height + delta.y).coerceAtLeast(150f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.PdfCard -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    PdfItem(
                        pdfCard = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedPdf: CanvasElement.PdfCard ->
                            if (updatedPdf.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedPdf
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.PdfCard
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(100f),
                                    height = (el.height + delta.y).coerceAtLeast(80f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.AudioNote -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    AudioItem(
                        audioNote = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedAudio: CanvasElement.AudioNote ->
                            if (updatedAudio.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedAudio
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.AudioNote
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(160f),
                                    height = (el.height + delta.y).coerceAtLeast(60f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.ShapeNode -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    ShapeItem(
                        shapeNode = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedShape: CanvasElement.ShapeNode ->
                            if (updatedShape.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedShape
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.ShapeNode
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(50f),
                                    height = (el.height + delta.y).coerceAtLeast(50f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } },
                        onSelectWithOffset = remember(element.id, scale) { { localOffset ->
                            val worldTap = element.position + localOffset
                            val hitConnector = findHitConnector(worldTap, elements, (24f / scale).coerceIn(12f, 120f))
                            if (hitConnector != null) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(null)
                                onUpdateSelectedConnectorId?.invoke(hitConnector.id)
                            } else {
                                if (currentIsConnectingMode) {
                                    if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                    else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                                } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                    if (!currentSelectedGroupIds.contains(element.id)) {
                                        onUpdateSelectedGroupIds(emptySet())
                                        onUpdateSelectedNoteId(element.id)
                                        onUpdateSelectedConnectorId?.invoke(null)
                                    }
                                }
                            }
                        } },
                        onSendToBack = remember(element.id) { { onSendToBack(element.id) } },
                        onBringToFront = remember(element.id) { { onBringToFront(element.id) } }
                    )
                }
            }
            is CanvasElement.LabelNode -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    LabelItem(
                        labelNode = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        isNewlySpawned = highlightedElementId == element.id,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedNode: CanvasElement.LabelNode ->
                            if (updatedNode.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedNode
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.LabelNode
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(100f),
                                    height = (el.height + delta.y).coerceAtLeast(40f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } }
                    )
                }
            }
            is CanvasElement.FrameNode -> {
                key(element.id) {
                    val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                    val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                    FrameItem(
                        frameNode = element,
                        scale = 1f,
                        offset = Offset.Zero,
                        isSelected = selectedNoteId == element.id,
                        isConnecting = isGroupedOrConnecting,
                        isConnectionTarget = isConnectionTarget,
                        isCanvasLocked = isCanvasLocked,
                        onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                        onDuplicate = remember(element.id) { { duplicateElementById(element.id) } },
                        onUpdate = remember(element.id, element.hyperlinkTarget) { { updatedFrame: CanvasElement.FrameNode ->
                            if (updatedFrame.hyperlinkTarget == "TELEPORT") {
                                val target = element.hyperlinkTarget!!
                                if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                            } else {
                                val currentIndex = elements.indexOfFirst { it.id == element.id }
                                if (currentIndex != -1) elements[currentIndex] = updatedFrame
                            }
                        } },
                        onPositionDelta = remember(element.id) { { delta: Offset -> onMoveGroup(delta, element.id) } },
                        onSizeDelta = remember(element.id) { { delta: Offset ->
                            val currentIndex = elements.indexOfFirst { it.id == element.id }
                            if (currentIndex != -1) {
                                val el = elements[currentIndex] as CanvasElement.FrameNode
                                elements[currentIndex] = el.copy(
                                    width = (el.width + delta.x).coerceAtLeast(100f),
                                    height = (el.height + delta.y).coerceAtLeast(100f)
                                )
                            }
                        } },
                        onSaveRequest = remember(element.id) { { onUpdateElements(elements.toList()) } },
                        onDelete = remember(element.id) { {
                            elements.removeAll { it.id == element.id }
                            elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                            onResetSelectedNoteId()
                            onUpdateElements(elements.toList())
                        } },
                        onSelect = remember(element.id) { {
                            if (currentIsConnectingMode) {
                                if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                            } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                if (!currentSelectedGroupIds.contains(element.id)) {
                                    onUpdateSelectedGroupIds(emptySet())
                                    onUpdateSelectedNoteId(element.id)
                                    onUpdateSelectedConnectorId?.invoke(null)
                                }
                            }
                        } },
                        onSelectWithOffset = remember(element.id, scale) { { localOffset ->
                            val worldTap = element.position + localOffset
                            val hitConnector = findHitConnector(worldTap, elements, (24f / scale).coerceIn(12f, 120f))
                            if (hitConnector != null) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(null)
                                onUpdateSelectedConnectorId?.invoke(hitConnector.id)
                            } else {
                                if (currentIsConnectingMode) {
                                    if (currentConnectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                                    else if (currentConnectingStartNodeId != element.id) onFinishConnection(element.id)
                                } else if (!currentIsDrawingMode && !currentIsLassoMode) {
                                    if (!currentSelectedGroupIds.contains(element.id)) {
                                        onUpdateSelectedGroupIds(emptySet())
                                        onUpdateSelectedNoteId(element.id)
                                        onUpdateSelectedConnectorId?.invoke(null)
                                    }
                                }
                            }
                        } }
                    )
                }
            }
            else -> {}
        }
    }

    // ── 1. BACKDROP ELEMENTS (Pass 1) ──
    backdropElements.forEach { element ->
        RenderSingleElement(element)
    }

    // ── 2. BACKSIDE CONNECTORS LAYER (Pass 2) ──
    BacksideConnectorsLayer(
        elements = elements,
        selectedConnectorId = selectedConnectorId,
        isDarkTheme = LocalDarkTheme.current
    )

    // ── 3. FOREGROUND ELEMENTS (Pass 3) ──
    foregroundElements.forEach { element ->
        RenderSingleElement(element)
    }

    // ── 4. TOP-LEVEL SELECTION CHROME & TOOLSTACK OVERLAY ───────────────────
    // Rendered on top of all cards & ink strokes so that the toolstack is ALWAYS accessible,
    // even if the item's layer is in the back.
    if (selectedNoteId != null && !isCanvasLocked) {
        val selectedEl = elements.find { it.id == selectedNoteId }
        if (selectedEl != null) {
            key(selectedEl.id) {
                CanvasSelectionChromeLayer(
                    selectedElement = selectedEl,
                    scale = scale,
                    isCanvasLocked = isCanvasLocked,
                    context = context,
                    onUpdateElement = { updated ->
                        val idx = elements.indexOfFirst { it.id == updated.id }
                        if (idx != -1) {
                            elements[idx] = updated
                            onUpdateElements(elements.toList())
                        }
                    },
                    onDeleteElement = { idToDelete ->
                        elements.removeAll { it.id == idToDelete }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == idToDelete || it.endId == idToDelete) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onDuplicateElement = { elToDup ->
                        duplicateElementById(elToDup.id)
                    },
                    onStartConnection = onStartConnection,
                    onSizeDelta = { delta, id ->
                        val idx = elements.indexOfFirst { it.id == id }
                        if (idx != -1) {
                            val el = elements[idx]
                            val updated = when (el) {
                                is CanvasElement.StickyNote -> el.copy(width = (el.width + delta.x).coerceAtLeast(100f), height = (el.height + delta.y).coerceAtLeast(100f))
                                is CanvasElement.CodeBlock -> el.copy(width = (el.width + delta.x).coerceAtLeast(150f), height = (el.height + delta.y).coerceAtLeast(100f))
                                is CanvasElement.ImageCard -> el.copy(width = (el.width + delta.x).coerceAtLeast(100f), height = (el.height + delta.y).coerceAtLeast(100f))
                                is CanvasElement.TaskListNode -> el.copy(width = (el.width + delta.x).coerceAtLeast(150f), height = (el.height + delta.y).coerceAtLeast(150f))
                                is CanvasElement.PdfCard -> el.copy(width = (el.width + delta.x).coerceAtLeast(100f), height = (el.height + delta.y).coerceAtLeast(80f))
                                is CanvasElement.AudioNote -> el.copy(width = (el.width + delta.x).coerceAtLeast(160f), height = (el.height + delta.y).coerceAtLeast(60f))
                                is CanvasElement.ShapeNode -> el.copy(width = (el.width + delta.x).coerceAtLeast(50f), height = (el.height + delta.y).coerceAtLeast(50f))
                                is CanvasElement.LabelNode -> el.copy(width = (el.width + delta.x).coerceAtLeast(100f), height = (el.height + delta.y).coerceAtLeast(40f))
                                is CanvasElement.FrameNode -> el.copy(width = (el.width + delta.x).coerceAtLeast(100f), height = (el.height + delta.y).coerceAtLeast(100f))
                                else -> el
                            }
                            elements[idx] = updated
                            onUpdateElements(elements.toList())
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onSendToBack = onSendToBack,
                    onBringToFront = onBringToFront,
                    onNavigateToScene = onNavigateToScene,
                    onNavigateToBoard = onNavigateToBoard,
                    onMakeSticker = { card ->
                        Toast.makeText(context, "Extracting subject...", Toast.LENGTH_SHORT).show()
                        StickerMaker.extractSubject(context, card.uriString) { newUri ->
                            if (newUri != null) {
                                elements.add(CanvasElement.ImageCard(id = UUID.randomUUID().toString(), position = card.position + Offset(80f, 80f), uriString = newUri, width = card.width, height = card.height, originalWidth = card.originalWidth, originalHeight = card.originalHeight))
                                onUpdateElements(elements.toList())
                            } else {
                                Toast.makeText(context, "No clear subject found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
