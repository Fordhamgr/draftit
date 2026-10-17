package com.flash.draft.model

import androidx.compose.ui.geometry.Offset
import java.util.UUID

object CanvasElementCloner {

    fun deepClone(element: CanvasElement, delta: Offset = Offset(40f, 40f)): CanvasElement {
        val newId = UUID.randomUUID().toString()
        return when (element) {
            is CanvasElement.StickyNote -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.Sketch -> {
                val newX = FloatArray(element.xCoords.size) { i -> element.xCoords[i] + delta.x }
                val newY = FloatArray(element.yCoords.size) { i -> element.yCoords[i] + delta.y }
                val newPressures = element.pressures.clone()
                val newTimes = element.times.clone()
                element.copy(
                    id = newId,
                    xCoords = newX,
                    yCoords = newY,
                    pressures = newPressures,
                    times = newTimes,
                    isLocked = false
                ).apply {
                    cachedStroke = null
                }
            }
            is CanvasElement.ImageCard -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.Connector -> {
                element.copy(
                    id = newId,
                    isLocked = false
                )
            }
            is CanvasElement.PdfCard -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.CodeBlock -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.AudioNote -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.ShapeNode -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.LabelNode -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.FrameNode -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    isLocked = false
                )
            }
            is CanvasElement.TaskListNode -> {
                element.copy(
                    id = newId,
                    position = element.position + delta,
                    tasks = element.tasks.map { task ->
                        task.copy(id = UUID.randomUUID().toString())
                    },
                    isLocked = false
                )
            }
        }
    }
}
