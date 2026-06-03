package com.flash.draft.ui.canvas.item
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Offset
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.* // Pulls in FrameItem, StickyNoteItem, etc.
import com.flash.draft.utils.StickerMaker
import java.util.UUID

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
    onStartConnection: (String) -> Unit
) {
    val screenTopLeft = com.flash.draft.utils.ViewportMath.screenToWorld(Offset(0f, 0f), offset, scale)
    val screenBottomRight = com.flash.draft.utils.ViewportMath.screenToWorld(Offset(centerX * 2f, centerY * 2f), offset, scale)
    // Add a 500px buffer to prevent popping when panning
    val cullLeft = screenTopLeft.x - 500f
    val cullTop = screenTopLeft.y - 500f
    val cullRight = screenBottomRight.x + 500f
    val cullBottom = screenBottomRight.y + 500f

    elements.toList().forEach { element ->
        // --- VIEWPORT CULLING ---
        if (element !is CanvasElement.Connector && element !is CanvasElement.Sketch) {
            val center = getCenterPoint(element.id, elements)
            val dims = getElementDimensions(element.id, elements)
            if (center != null) {
                val halfW = dims.first / 2f
                val halfH = dims.second / 2f
                val elLeft = center.x - halfW
                val elTop = center.y - halfH
                val elRight = center.x + halfW
                val elBottom = center.y + halfH
                
                // If element is completely outside the cull bounds, skip rendering it
                if (elRight < cullLeft || elLeft > cullRight || elBottom < cullTop || elTop > cullBottom) {
                    return@forEach
                }
            }
        }

        if (element is CanvasElement.FrameNode) {

            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                FrameItem(
                    frameNode = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedFrame ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedFrame
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.FrameNode
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(150f),
                                height = (el.height + delta.y).coerceAtLeast(100f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                val newGroup = mutableSetOf(element.id)
                                if (element.isMagnetic) {
                                    val fx = element.position.x; val fy = element.position.y
                                    val fw = element.width; val fh = element.height
                                    elements.forEach { el ->
                                        if (el.id != element.id && el !is CanvasElement.Connector) {
                                            val center = getCenterPoint(el.id, elements)
                                            if (center != null && center.x >= fx && center.x <= fx + fw && center.y >= fy && center.y <= fy + fh) {
                                                newGroup.add(el.id)
                                            }
                                        }
                                    }
                                }
                                onUpdateSelectedGroupIds(newGroup)
                                onUpdateSelectedNoteId(element.id)
                                onSendToBack(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.StickyNote) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                StickyNoteItem(
                    note = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedNote ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedNote
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.StickyNote
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(100f),
                                height = (el.height + delta.y).coerceAtLeast(100f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.CodeBlock) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                CodeSnippetItem(
                    codeBlock = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedBlock ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedBlock
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.CodeBlock
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(150f),
                                height = (el.height + delta.y).coerceAtLeast(100f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.ImageCard) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                ImageItem(
                    imageCard = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedImage ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedImage
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.ImageCard
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(100f),
                                height = (el.height + delta.y).coerceAtLeast(100f)
                            )
                        }
                    },
                    onDuplicate = {
                        elements.add(element.copy(id = UUID.randomUUID().toString(), position = element.position + Offset(40f, 40f)))
                        onUpdateElements(elements.toList())
                    },
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
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.TaskListNode) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                TaskListItem(
                    taskNode = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedNode ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedNode
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.TaskListNode
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(150f),
                                height = (el.height + delta.y).coerceAtLeast(150f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.PdfCard) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                PdfItem(
                    pdfCard = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedPdf ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedPdf
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.PdfCard
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(100f),
                                height = (el.height + delta.y).coerceAtLeast(80f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.AudioNote) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                AudioItem(
                    audioNote = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedAudio ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedAudio
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.AudioNote
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(160f),
                                height = (el.height + delta.y).coerceAtLeast(60f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.ShapeNode) {


            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                ShapeItem(
                    shapeNode = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedShape ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedShape
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
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
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }

        if (element is CanvasElement.LabelNode) {
        if (element is CanvasElement.LabelNode) {

            key(element.id) {
                val isGroupedOrConnecting = connectingStartNodeId == element.id || selectedGroupIds.contains(element.id)
                val isConnectionTarget = isConnectingMode && connectingStartNodeId != null && connectingStartNodeId != element.id
                LabelItem(
                    labelNode = element,
                    scale = scale,
                    offset = offset,
                    isSelected = selectedNoteId == element.id,
                    isConnecting = isGroupedOrConnecting,
                    isConnectionTarget = isConnectionTarget,
                    onConnectRequest = { onStartConnection(element.id) },
                    onUpdate = { updatedNode ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) elements[currentIndex] = updatedNode
                    },
                    onPositionDelta = { delta -> onMoveGroup(delta, element.id) },
                    onSizeDelta = { delta ->
                        val currentIndex = elements.indexOfFirst { it.id == element.id }
                        if (currentIndex != -1) {
                            val el = elements[currentIndex] as CanvasElement.LabelNode
                            elements[currentIndex] = el.copy(
                                width = (el.width + delta.x).coerceAtLeast(100f),
                                height = (el.height + delta.y).coerceAtLeast(40f)
                            )
                        }
                    },
                    onSaveRequest = { onUpdateElements(elements.toList()) },
                    onDelete = {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    },
                    onSelect = {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    }
                )
            }
        }
    }
}
}
