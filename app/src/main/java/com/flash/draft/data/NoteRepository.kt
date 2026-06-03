package com.flash.draft.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.flash.draft.model.CanvasElement
import com.flash.draft.model.Project
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NoteRepository(context: Context) {
    // We now save to a dedicated "projects" folder inside filesDir
    private val projectsDir = File(context.filesDir, "projects")

    init {
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
    }

    // --- NEW: PROJECT MANAGEMENT ---

    fun getAllProjects(): List<Project> {
        val files = projectsDir.listFiles() ?: return emptyList()
        return files.filter { it.extension == "json" }
            .map { file ->
                val projectId = file.nameWithoutExtension
                val elements = loadElements(projectId)

                // Try to find a title from a Label or Frame, otherwise default
                val titleNode = elements.filterIsInstance<CanvasElement.LabelNode>().firstOrNull()
                val title = titleNode?.text ?: "Untitled Canvas"

                Project(
                    id = projectId,
                    title = title,
                    lastModifiedMs = file.lastModified(),
                    previewElements = elements.take(15) // Only load a few for the thumbnail
                )
            }.sortedByDescending { it.lastModifiedMs } // Newest first
    }

    fun createNewProject(): String {
        val newId = java.util.UUID.randomUUID().toString()
        saveElements(newId, emptyList()) // Create empty file
        return newId
    }

    fun deleteProject(projectId: String) {
        val file = File(projectsDir, "$projectId.json")
        if (file.exists()) file.delete()
    }

    // --- UPGRADED: ELEMENT SAVING/LOADING ---

    fun saveElements(projectId: String, elements: List<CanvasElement>) {
        val file = File(projectsDir, "$projectId.json")
        val jsonArray = JSONArray()
        elements.forEach { element ->
            val jsonObject = JSONObject()
            jsonObject.put("id", element.id)

            when (element) {
                is CanvasElement.StickyNote -> {
                    jsonObject.put("type", "note")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("text", element.text)
                    jsonObject.put("color", element.color.value.toULong().toString())
                    jsonObject.put("fontSize", element.fontSize.toDouble())
                    jsonObject.put("fontFamily", element.fontFamily)
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                }
                is CanvasElement.ImageCard -> {
                    jsonObject.put("type", "image")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("uri", element.uriString)
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                    jsonObject.put("originalWidth", element.originalWidth.toDouble())
                    jsonObject.put("originalHeight", element.originalHeight.toDouble())
                    jsonObject.put("alpha", element.alpha.toDouble())
                    jsonObject.put("rotation", element.rotation.toDouble())
                    jsonObject.put("scaleX", element.scaleX.toDouble())
                    jsonObject.put("scaleY", element.scaleY.toDouble())
                    jsonObject.put("isLocked", element.isLocked)
                }
                is CanvasElement.PdfCard -> {
                    jsonObject.put("type", "pdf")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("uriString", element.uriString)
                    jsonObject.put("fileName", element.fileName)
                    jsonObject.put("fileSize", element.fileSize)
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                }
                is CanvasElement.CodeBlock -> {
                    jsonObject.put("type", "code")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("code", element.code)
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                }
                is CanvasElement.Sketch -> {
                    jsonObject.put("type", "sketch")
                    jsonObject.put("color", element.color.value.toULong().toString())
                    jsonObject.put("strokeWidth", element.strokeWidth.toDouble())
                    jsonObject.put("toolType", element.toolType)
                    jsonObject.put("alpha", element.alpha.toDouble())
                    val pointsArray = JSONArray()
                    element.points.forEach { point ->
                        val pointObj = JSONObject()
                        pointObj.put("x", point.x.toDouble())
                        pointObj.put("y", point.y.toDouble())
                        pointsArray.put(pointObj)
                    }
                    jsonObject.put("points", pointsArray)
                }
                is CanvasElement.AudioNote -> {
                    jsonObject.put("type", "audio")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("audioPath", element.audioPath)
                    jsonObject.put("durationMs", element.durationMs)
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                }
                is CanvasElement.ShapeNode -> {
                    jsonObject.put("type", "shape")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                    jsonObject.put("shapeType", element.shapeType)
                    jsonObject.put("color", element.color.value.toULong().toString())
                    jsonObject.put("isFilled", element.isFilled)
                }
                is CanvasElement.Connector -> {
                    jsonObject.put("type", "connector")
                    jsonObject.put("startId", element.startId)
                    jsonObject.put("endId", element.endId)
                    jsonObject.put("color", element.color.value.toULong().toString())
                    jsonObject.put("strokeWidth", element.strokeWidth.toDouble())
                    jsonObject.put("lineStyle", element.lineStyle)
                    jsonObject.put("arrowDirection", element.arrowDirection)
                    jsonObject.put("arrowShape", element.arrowShape)
                    jsonObject.put("routing", element.routing)
                }
                is CanvasElement.LabelNode -> {
                    jsonObject.put("type", "label")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                    jsonObject.put("text", element.text)
                    jsonObject.put("color", element.color.value.toULong().toString())
                    jsonObject.put("fontSize", element.fontSize.toDouble())
                    jsonObject.put("isBold", element.isBold)
                }
                is CanvasElement.FrameNode -> {
                    jsonObject.put("type", "frame")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                    jsonObject.put("title", element.title)
                    jsonObject.put("color", element.color.value.toULong().toString())
                    jsonObject.put("isMagnetic", element.isMagnetic)
                }
                is CanvasElement.TaskListNode -> {
                    jsonObject.put("type", "tasklist")
                    jsonObject.put("x", element.position.x.toDouble())
                    jsonObject.put("y", element.position.y.toDouble())
                    jsonObject.put("width", element.width.toDouble())
                    jsonObject.put("height", element.height.toDouble())
                    jsonObject.put("title", element.title)
                    jsonObject.put("color", element.color.value.toULong().toString())
                    val tasksArray = JSONArray()
                    element.tasks.forEach { task ->
                        val tObj = JSONObject()
                        tObj.put("id", task.id)
                        tObj.put("text", task.text)
                        tObj.put("isCompleted", task.isCompleted)
                        tasksArray.put(tObj)
                    }
                    jsonObject.put("tasks", tasksArray)
                }
            }
            jsonArray.put(jsonObject)
        }
        file.writeText(jsonArray.toString())
    }

    fun loadElements(projectId: String): List<CanvasElement> {
        val file = File(projectsDir, "$projectId.json")
        if (!file.exists()) return emptyList()
        val jsonString = file.readText()
        if (jsonString.isBlank()) return emptyList()

        val elements = mutableListOf<CanvasElement>()
        try {
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)
                    val type = if (obj.has("type")) obj.getString("type") else "note"
                    val id = obj.getString("id")
                    val pos = Offset(obj.optDouble("x", 0.0).toFloat(), obj.optDouble("y", 0.0).toFloat())

                    when (type) {
                        "pdf" -> elements.add(CanvasElement.PdfCard(id, pos, obj.getString("uriString"), obj.getString("fileName"), obj.getString("fileSize"), obj.optDouble("width", 260.0).toFloat(), obj.optDouble("height", 140.0).toFloat()))
                        "code" -> elements.add(CanvasElement.CodeBlock(id, pos, obj.optString("code", ""), obj.optDouble("width", 280.0).toFloat(), obj.optDouble("height", 180.0).toFloat()))
                        "note" -> elements.add(CanvasElement.StickyNote(id, pos, obj.optString("text", ""), Color(obj.optString("color", "4294965700").toULong()), obj.optDouble("width", 200.0).toFloat(), obj.optDouble("height", 200.0).toFloat(), obj.optDouble("fontSize", 16.0).toFloat(), obj.optString("fontFamily", "SansSerif")))
                        "sketch" -> {
                            val pointsArray = obj.getJSONArray("points")
                            val points = mutableListOf<Offset>()
                            for (j in 0 until pointsArray.length()) { val pObj = pointsArray.getJSONObject(j); points.add(Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat())) }
                            elements.add(CanvasElement.Sketch(id, points, Color(obj.optString("color", "4278190080").toULong()), obj.optDouble("strokeWidth", 6.0).toFloat(), obj.optString("toolType", "Pen"), obj.optDouble("alpha", 1.0).toFloat()))
                        }
                        "image" -> elements.add(CanvasElement.ImageCard(id, pos, obj.getString("uri"), obj.optDouble("width", 200.0).toFloat(), obj.optDouble("height", 200.0).toFloat(), obj.optDouble("originalWidth", obj.optDouble("width", 200.0)).toFloat(), obj.optDouble("originalHeight", obj.optDouble("height", 200.0)).toFloat(), obj.optDouble("alpha", 1.0).toFloat(), obj.optDouble("rotation", 0.0).toFloat(), obj.optDouble("scaleX", 1.0).toFloat(), obj.optDouble("scaleY", 1.0).toFloat(), obj.optBoolean("isLocked", false)))
                        "audio" -> elements.add(CanvasElement.AudioNote(id, pos, obj.optString("audioPath", ""), obj.optInt("durationMs", 0), false, obj.optDouble("width", 240.0).toFloat(), obj.optDouble("height", 80.0).toFloat()))
                        "shape" -> elements.add(CanvasElement.ShapeNode(id, pos, obj.optDouble("width", 150.0).toFloat(), obj.optDouble("height", 150.0).toFloat(), obj.optString("shapeType", "Rectangle"), Color(obj.optString("color", "4280399603").toULong()), obj.optBoolean("isFilled", true)))
                        "connector" -> elements.add(CanvasElement.Connector(id, obj.getString("startId"), obj.getString("endId"), Color(obj.optString("color", "4278190080").toULong()), obj.optDouble("strokeWidth", 4.0).toFloat(), obj.optString("lineStyle", "Solid"), obj.optString("arrowDirection", "End"), obj.optString("arrowShape", "Filled"), obj.optString("routing", "Straight")))
                        "label" -> elements.add(CanvasElement.LabelNode(id, pos, obj.optString("text", ""), obj.optDouble("width", 250.0).toFloat(), obj.optDouble("height", 80.0).toFloat(), Color(obj.optString("color", "4278190080").toULong()), obj.optDouble("fontSize", 24.0).toFloat(), obj.optBoolean("isBold", false)))
                        "frame" -> elements.add(CanvasElement.FrameNode(id, pos, obj.optDouble("width", 400.0).toFloat(), obj.optDouble("height", 300.0).toFloat(), obj.optString("title", "New Frame"), Color(obj.optString("color", "4288585374").toULong()), obj.optBoolean("isMagnetic", false)))
                        "tasklist" -> {
                            val tasksList = mutableListOf<CanvasElement.TaskItem>()
                            if (obj.has("tasks")) {
                                val tasksArray = obj.getJSONArray("tasks")
                                for (j in 0 until tasksArray.length()) { val tObj = tasksArray.getJSONObject(j); tasksList.add(CanvasElement.TaskItem(tObj.getString("id"), tObj.getString("text"), tObj.getBoolean("isCompleted"))) }
                            }
                            elements.add(CanvasElement.TaskListNode(id, pos, obj.optDouble("width", 280.0).toFloat(), obj.optDouble("height", 350.0).toFloat(), obj.optString("title", "To-Do List"), if (tasksList.isEmpty()) listOf(CanvasElement.TaskItem(text = "New task")) else tasksList, Color(obj.optString("color", "4294967295").toULong())))
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed individual element and continue to parse the rest
                }
            }
        } catch (e: Exception) {
            // Log full file corruption if needed, return whatever was parsed so far
        }
        return elements
    }
}