package com.flash.draft.ui.canvas.item
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.flash.draft.model.CanvasElement
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
    onNavigateToBoard: (String) -> Unit
) {
    // GPU graphicsLayer natively handles viewport culling, so we don't need CPU-side cull bounds.
    val currentSelectedGroupIds by androidx.compose.runtime.rememberUpdatedState(selectedGroupIds)
    val currentConnectingStartNodeId by androidx.compose.runtime.rememberUpdatedState(connectingStartNodeId)
    val currentIsConnectingMode by androidx.compose.runtime.rememberUpdatedState(isConnectingMode)
    val currentIsDrawingMode by androidx.compose.runtime.rememberUpdatedState(isDrawingMode)
    val currentIsLassoMode by androidx.compose.runtime.rememberUpdatedState(isLassoMode)

    elements.forEach { element ->
        if (element is CanvasElement.StickyNote) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                StickyNoteItem(
                    note = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.CodeBlock) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                CodeSnippetItem(
                    codeBlock = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.ImageCard) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                ImageItem(
                    imageCard = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.ImageCard
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(100f),
                                height = (el.height + delta.y).coerceAtLeast(100f)
                            )
                        }
 } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.TaskListNode) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                TaskListItem(
                    taskNode = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.PdfCard) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                PdfItem(
                    pdfCard = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.AudioNote) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                AudioItem(
                    audioNote = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.ShapeNode) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                ShapeItem(
                    shapeNode = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }

        if (element is CanvasElement.LabelNode) {

            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                LabelItem(
                    labelNode = element,
                    scale = 1f,
                    offset = androidx.compose.ui.geometry.Offset.Zero,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = remember(element.id) { { onStartConnection(element.id) } },
                    onDuplicate = remember(element.id) { {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    } },
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
                    onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> onMoveGroup(delta, element.id) } },
                    onSizeDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset ->
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
                                onBringToFront(element.id)
                            }
                        }
                    } }
                )
            }
        }
    }
}
