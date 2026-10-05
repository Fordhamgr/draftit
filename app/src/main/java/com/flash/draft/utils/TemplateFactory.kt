package com.flash.draft.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.flash.draft.model.CanvasElement
import java.util.UUID

object TemplateFactory {
    fun createElements(templateName: String): List<CanvasElement> {
        return when (templateName) {
            "Brainstorming" -> {
                listOf(
                    CanvasElement.FrameNode(
                        id = UUID.randomUUID().toString(),
                        position = Offset(-200f, -200f),
                        width = 800f,
                        height = 600f,
                        title = "Brainstorming Session",
                        color = Color(0xFFF3E5F5)
                    ),
                    CanvasElement.StickyNote(
                        id = UUID.randomUUID().toString(),
                        position = Offset(-150f, -100f),
                        text = "Idea 1: ...",
                        color = Color(0xFFFFF9C4)
                    ),
                    CanvasElement.StickyNote(
                        id = UUID.randomUUID().toString(),
                        position = Offset(100f, -100f),
                        text = "Idea 2: ...",
                        color = Color(0xFFC8E6C9)
                    ),
                    CanvasElement.StickyNote(
                        id = UUID.randomUUID().toString(),
                        position = Offset(-25f, 150f),
                        text = "Next Steps",
                        color = Color(0xFFFFCCBC)
                    )
                )
            }
            "Flowchart" -> {
                val startId = UUID.randomUUID().toString()
                val step1Id = UUID.randomUUID().toString()
                val endId = UUID.randomUUID().toString()
                
                listOf(
                    CanvasElement.ShapeNode(
                        id = startId,
                        position = Offset(0f, -200f),
                        shapeType = "Circle",
                        color = Color(0xFF81C784)
                    ),
                    CanvasElement.LabelNode(
                        id = UUID.randomUUID().toString(),
                        position = Offset(10f, -150f),
                        text = "Start",
                        fontSize = 20f
                    ),
                    CanvasElement.ShapeNode(
                        id = step1Id,
                        position = Offset(0f, 0f),
                        shapeType = "Rectangle",
                        color = Color(0xFF64B5F6),
                        width = 200f,
                        height = 100f
                    ),
                    CanvasElement.LabelNode(
                        id = UUID.randomUUID().toString(),
                        position = Offset(30f, 30f),
                        text = "Process",
                        fontSize = 20f
                    ),
                    CanvasElement.ShapeNode(
                        id = endId,
                        position = Offset(0f, 200f),
                        shapeType = "Circle",
                        color = Color(0xFFE57373)
                    ),
                    CanvasElement.LabelNode(
                        id = UUID.randomUUID().toString(),
                        position = Offset(20f, 250f),
                        text = "End",
                        fontSize = 20f
                    ),
                    CanvasElement.Connector(
                        id = UUID.randomUUID().toString(),
                        startId = startId,
                        endId = step1Id
                    ),
                    CanvasElement.Connector(
                        id = UUID.randomUUID().toString(),
                        startId = step1Id,
                        endId = endId
                    )
                )
            }
            "To-Do List" -> {
                listOf(
                    CanvasElement.TaskListNode(
                        id = UUID.randomUUID().toString(),
                        position = Offset(0f, 0f),
                        title = "Project Tasks",
                        width = 300f,
                        height = 400f,
                        color = Color(0xFFFFF3E0),
                        tasks = listOf(
                            CanvasElement.TaskItem(text = "Define requirements", isCompleted = true),
                            CanvasElement.TaskItem(text = "Design mockup", isCompleted = false),
                            CanvasElement.TaskItem(text = "Implement feature", isCompleted = false)
                        )
                    )
                )
            }
            else -> emptyList()
        }
    }
}
