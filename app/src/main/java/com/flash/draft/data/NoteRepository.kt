package com.flash.draft.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.flash.draft.model.CanvasElement
import com.flash.draft.model.InkPoint
import com.flash.draft.model.Project
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

// Synchronization lock to prevent AtomicFile concurrent writes
private val saveLock = Any()

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
        return files.filter { it.extension == "json" && !it.name.contains("_scenes") && !it.name.contains("_meta") }
            .mapNotNull { file ->
                val projectId = file.nameWithoutExtension
                val elements = loadElements(projectId)
                val scenes = loadScenes(projectId)
                val metaFile = File(projectsDir, "${projectId}_meta.json")
                
                // Garbage Collection for empty ghost projects
                if (elements.isEmpty() && scenes.isEmpty() && !metaFile.exists()) {
                    com.flash.draft.utils.DraftLogger.logDb("Garbage Collection", details = "Deleting ghost project $projectId")
                    file.delete()
                    File(projectsDir, "${projectId}_scenes.json").delete()
                    return@mapNotNull null
                }

                val title = if (metaFile.exists()) {
                    try { JSONObject(metaFile.readText()).getString("title") } catch(e: Exception) { "Untitled Canvas" }
                } else {
                    val titleNode = elements.filterIsInstance<CanvasElement.LabelNode>().firstOrNull()
                    titleNode?.text ?: "Untitled Canvas"
                }

                Project(
                    id = projectId,
                    title = title,
                    lastModifiedMs = file.lastModified(),
                    previewElements = elements.take(15) // Only load a few for the thumbnail
                )
            }.sortedByDescending { it.lastModifiedMs } // Newest first
    }

    fun createNewProject(title: String): String {
        val newId = java.util.UUID.randomUUID().toString()
        com.flash.draft.utils.DraftLogger.logDb("Create Project", details = "ID: $newId")
        val metaFile = File(projectsDir, "${newId}_meta.json")
        val json = JSONObject().apply { put("title", title) }
        metaFile.writeText(json.toString())
        return newId
    }

    fun renameProject(projectId: String, newName: String) {
        val metaFile = File(projectsDir, "${projectId}_meta.json")
        val json = JSONObject().apply { put("title", newName) }
        metaFile.writeText(json.toString())
        
        // Remove any hacked LabelNode that might have been saved as title 
        // to avoid legacy junk if they rename it again
        val elements = loadElements(projectId).toMutableList()
        val titleNodeIndex = elements.indexOfFirst { it is CanvasElement.LabelNode && it.text == newName && it.position.y == -100f }
        if (titleNodeIndex != -1) {
            elements.removeAt(titleNodeIndex)
            saveElements(projectId, elements)
        }
    }

    fun deleteProject(projectId: String) {
        File(projectsDir, "$projectId.json").takeIf { it.exists() }?.delete()
        File(projectsDir, "${projectId}_scenes.json").takeIf { it.exists() }?.delete()
        File(projectsDir, "${projectId}_meta.json").takeIf { it.exists() }?.delete()
    }

    fun duplicateProject(projectId: String): String {
        val newId = java.util.UUID.randomUUID().toString()
        val elements = loadElements(projectId)
        val scenes = loadScenes(projectId)
        
        com.flash.draft.utils.DraftLogger.logDb("Duplicate Project", details = "$projectId -> $newId")
        saveElements(newId, elements)
        saveScenes(newId, scenes)
        
        val metaFile = File(projectsDir, "${projectId}_meta.json")
        if (metaFile.exists()) {
            try {
                val oldJson = JSONObject(metaFile.readText())
                val oldTitle = oldJson.getString("title")
                val newMetaFile = File(projectsDir, "${newId}_meta.json")
                val newJson = JSONObject().apply { 
                    put("title", "$oldTitle Copy")
                    if (oldJson.has("backgroundStyle")) {
                        put("backgroundStyle", oldJson.getString("backgroundStyle"))
                    }
                }
                newMetaFile.writeText(newJson.toString())
            } catch(e: Exception) {}
        }
        return newId
    }

    fun getProjectBackgroundStyle(projectId: String): com.flash.draft.ui.canvas.BackgroundStyle {
        val metaFile = File(projectsDir, "${projectId}_meta.json")
        if (metaFile.exists()) {
            try {
                val json = JSONObject(metaFile.readText())
                if (json.has("backgroundStyle")) {
                    return com.flash.draft.ui.canvas.BackgroundStyle.valueOf(json.getString("backgroundStyle"))
                }
            } catch (e: Exception) {}
        }
        return com.flash.draft.ui.canvas.BackgroundStyle.Solid
    }

    fun updateProjectBackgroundStyle(projectId: String, style: com.flash.draft.ui.canvas.BackgroundStyle) {
        val metaFile = File(projectsDir, "${projectId}_meta.json")
        val json = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch(e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }
        json.put("backgroundStyle", style.name)
        metaFile.writeText(json.toString())
    }

    // --- .DRAFTIT ZIP EXPORT / IMPORT ---

    fun saveProjectThumbnail(projectId: String, swBitmap: android.graphics.Bitmap) {
        try {
            val file = java.io.File(projectsDir, "${projectId}_thumb.png")

            val maxDim = 2000f
            val scale = if (swBitmap.width > swBitmap.height) maxDim / swBitmap.width else maxDim / swBitmap.height
            val scaledBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(swBitmap, (swBitmap.width * scale).toInt(), (swBitmap.height * scale).toInt(), true)
            } else swBitmap

            java.io.FileOutputStream(file).use { out: java.io.FileOutputStream ->
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            if (scaledBitmap != swBitmap) {
                scaledBitmap.recycle()
            }
            swBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportProject(context: Context, projectId: String, outputStream: java.io.OutputStream) {
        val projectJson = getRawJson(projectId) ?: "[]"
        val scenesJson = try {
            File(projectsDir, "${projectId}_scenes.json").readText()
        } catch (e: Exception) { null }
        val metaJson = try {
            File(projectsDir, "${projectId}_meta.json").readText()
        } catch (e: Exception) { null }
        
        com.flash.draft.utils.ProjectArchiveHelper.exportToZip(
            context, projectId, projectJson, scenesJson, metaJson, outputStream
        )
    }

    fun importProjectArchive(context: Context, inputStream: java.io.InputStream): String? {
        return try {
            val bytes = inputStream.readBytes()
            var projectJson: String? = null
            var scenesJson: String? = null
            var metaJson: String? = null
            
            try {
                // Try to parse as ZIP
                val zipResult = com.flash.draft.utils.ProjectArchiveHelper.importFromZip(context, bytes.inputStream())
                projectJson = zipResult.first
                scenesJson = zipResult.second
                metaJson = zipResult.third
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (projectJson == null) {
                // Fallback: try parsing the whole thing as a raw JSON array
                val rawContent = String(bytes)
                if (rawContent.trim().startsWith("[")) {
                    projectJson = rawContent
                }
            }

            if (projectJson != null) {
                val newId = java.util.UUID.randomUUID().toString()
                File(projectsDir, "$newId.json").writeText(projectJson)
                if (scenesJson != null) {
                    File(projectsDir, "${newId}_scenes.json").writeText(scenesJson)
                }
                if (metaJson != null) {
                    File(projectsDir, "${newId}_meta.json").writeText(metaJson)
                }
                newId
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- UPGRADED: ELEMENT SAVING/LOADING ---

    fun saveElements(projectId: String, elements: List<CanvasElement>) {
        val start = System.nanoTime()
        val file = File(projectsDir, "$projectId.json")
        synchronized(saveLock) {
            val atomicFile = AtomicFile(file)
            var stream: FileOutputStream? = null
            try {
                stream = atomicFile.startWrite()
                val writer = java.io.OutputStreamWriter(stream, "UTF-8").buffered()
                writer.write("[")
                elements.forEachIndexed { index, element ->
                    val jsonObject = JSONObject()
            jsonObject.put("id", element.id)
            element.hyperlinkTarget?.let { jsonObject.put("hyperlinkTarget", it) }

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
                    val xArr = JSONArray()
                    val yArr = JSONArray()
                    val pArr = JSONArray()
                    val tArr = JSONArray()
                    for (i in element.xCoords.indices) {
                        xArr.put(element.xCoords[i].toDouble())
                        yArr.put(element.yCoords[i].toDouble())
                        pArr.put(element.pressures[i].toDouble())
                        tArr.put(element.times[i])
                    }
                    jsonObject.put("xCoords", xArr)
                    jsonObject.put("yCoords", yArr)
                    jsonObject.put("pressures", pArr)
                    jsonObject.put("times", tArr)
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
                } // end when
                writer.write(jsonObject.toString())
                if (index < elements.size - 1) writer.write(",")
            } // end forEachIndexed
            writer.write("]")
            writer.flush()
            atomicFile.finishWrite(stream)
            val elapsed = (System.nanoTime() - start) / 1_000_000f
            com.flash.draft.utils.DraftLogger.logDb("Save Elements", timeMs = elapsed, details = "Streamed ${elements.size} elements to $projectId.json")
        } catch (e: Exception) {
            if (stream != null) atomicFile.failWrite(stream)
            com.flash.draft.utils.DraftLogger.logDbError("Save Elements for $projectId", e)
            e.printStackTrace()
        }
        } // end synchronized
    }

    fun loadElements(projectId: String): List<CanvasElement> {
        val start = System.nanoTime()
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
                        "pdf" -> elements.add(CanvasElement.PdfCard(id, pos, obj.getString("uriString"), obj.getString("fileName"), obj.getString("fileSize"), obj.optDouble("width", 260.0).toFloat(), obj.optDouble("height", 140.0).toFloat(), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "code" -> elements.add(CanvasElement.CodeBlock(id, pos, obj.optString("code", ""), obj.optDouble("width", 280.0).toFloat(), obj.optDouble("height", 180.0).toFloat(), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "note" -> elements.add(CanvasElement.StickyNote(id, pos, obj.optString("text", ""), Color(obj.optString("color", "4294965700").toULong()), obj.optDouble("width", 200.0).toFloat(), obj.optDouble("height", 200.0).toFloat(), obj.optDouble("fontSize", 16.0).toFloat(), obj.optString("fontFamily", "SansSerif"), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "sketch" -> {
                            val xCoords: FloatArray
                            val yCoords: FloatArray
                            val pressures: FloatArray
                            val times: LongArray
                            
                            if (obj.has("xCoords")) {
                                val xArr = obj.getJSONArray("xCoords")
                                val yArr = obj.getJSONArray("yCoords")
                                val pArr = obj.getJSONArray("pressures")
                                val tArr = obj.getJSONArray("times")
                                val size = xArr.length()
                                xCoords = FloatArray(size)
                                yCoords = FloatArray(size)
                                pressures = FloatArray(size)
                                times = LongArray(size)
                                for (j in 0 until size) {
                                    xCoords[j] = xArr.getDouble(j).toFloat()
                                    yCoords[j] = yArr.getDouble(j).toFloat()
                                    pressures[j] = pArr.getDouble(j).toFloat()
                                    times[j] = tArr.getLong(j)
                                }
                            } else {
                                val pointsArray = obj.getJSONArray("points")
                                val size = pointsArray.length()
                                xCoords = FloatArray(size)
                                yCoords = FloatArray(size)
                                pressures = FloatArray(size)
                                times = LongArray(size)
                                for (j in 0 until size) { 
                                    val pObj = pointsArray.getJSONObject(j)
                                    xCoords[j] = pObj.getDouble("x").toFloat()
                                    yCoords[j] = pObj.getDouble("y").toFloat()
                                    pressures[j] = pObj.optDouble("pressure", 1.0).toFloat()
                                    times[j] = pObj.optLong("timeMillis", 0L)
                                }
                            }
                            elements.add(CanvasElement.Sketch(id, xCoords, yCoords, pressures, times, Color(obj.optString("color", "4278190080").toULong()), obj.optDouble("strokeWidth", 6.0).toFloat(), obj.optString("toolType", "Pen"), obj.optDouble("alpha", 1.0).toFloat(), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        }
                        "image" -> elements.add(CanvasElement.ImageCard(id, pos, obj.getString("uri"), obj.optDouble("width", 200.0).toFloat(), obj.optDouble("height", 200.0).toFloat(), obj.optDouble("originalWidth", obj.optDouble("width", 200.0)).toFloat(), obj.optDouble("originalHeight", obj.optDouble("height", 200.0)).toFloat(), obj.optDouble("alpha", 1.0).toFloat(), obj.optDouble("rotation", 0.0).toFloat(), obj.optDouble("scaleX", 1.0).toFloat(), obj.optDouble("scaleY", 1.0).toFloat(), obj.optBoolean("isLocked", false), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "audio" -> elements.add(CanvasElement.AudioNote(id, pos, obj.optString("audioPath", ""), obj.optInt("durationMs", 0), false, obj.optDouble("width", 240.0).toFloat(), obj.optDouble("height", 80.0).toFloat(), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "shape" -> elements.add(CanvasElement.ShapeNode(id, pos, obj.optDouble("width", 150.0).toFloat(), obj.optDouble("height", 150.0).toFloat(), obj.optString("shapeType", "Rectangle"), Color(obj.optString("color", "4280399603").toULong()), obj.optBoolean("isFilled", true), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "connector" -> elements.add(CanvasElement.Connector(id, obj.getString("startId"), obj.getString("endId"), Color(obj.optString("color", "4278190080").toULong()), obj.optDouble("strokeWidth", 4.0).toFloat(), obj.optString("lineStyle", "Solid"), obj.optString("arrowDirection", "End"), obj.optString("arrowShape", "Filled"), obj.optString("routing", "Straight"), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "label" -> elements.add(CanvasElement.LabelNode(id, pos, obj.optString("text", ""), obj.optDouble("width", 250.0).toFloat(), obj.optDouble("height", 80.0).toFloat(), Color(obj.optString("color", "4278190080").toULong()), obj.optDouble("fontSize", 24.0).toFloat(), obj.optBoolean("isBold", false), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "frame" -> elements.add(CanvasElement.FrameNode(id, pos, obj.optDouble("width", 400.0).toFloat(), obj.optDouble("height", 300.0).toFloat(), obj.optString("title", "New Frame"), Color(obj.optString("color", "4288585374").toULong()), obj.optBoolean("isMagnetic", false), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        "tasklist" -> {
                            val tasksList = mutableListOf<CanvasElement.TaskItem>()
                            if (obj.has("tasks")) {
                                val tasksArray = obj.getJSONArray("tasks")
                                for (j in 0 until tasksArray.length()) { val tObj = tasksArray.getJSONObject(j); tasksList.add(CanvasElement.TaskItem(tObj.getString("id"), tObj.getString("text"), tObj.getBoolean("isCompleted"))) }
                            }
                            elements.add(CanvasElement.TaskListNode(id, pos, obj.optDouble("width", 280.0).toFloat(), obj.optDouble("height", 350.0).toFloat(), obj.optString("title", "To-Do List"), if (tasksList.isEmpty()) listOf(CanvasElement.TaskItem(text = "New task")) else tasksList, Color(obj.optString("color", "4294967295").toULong()), hyperlinkTarget = if (obj.has("hyperlinkTarget")) obj.getString("hyperlinkTarget") else null))
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed individual element and continue to parse the rest
                }
            }
        } catch (e: Exception) {
            // Log full file corruption if needed, return whatever was parsed so far
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000f
        com.flash.draft.utils.DraftLogger.logDb("Load Elements", timeMs = elapsed, details = "Loaded ${elements.size} elements for $projectId")
        return elements
    }

    fun getRawJson(projectId: String): String? {
        val file = File(projectsDir, "$projectId.json")
        if (!file.exists()) return null
        return try { file.readText() } catch (e: Exception) { null }
    }

    fun importProject(jsonContent: String): String? {
        return try {
            // Validate it's a JSON array format expected for our projects
            org.json.JSONArray(jsonContent)
            val newId = java.util.UUID.randomUUID().toString()
            val file = File(projectsDir, "$newId.json")
            file.writeText(jsonContent)
            newId
        } catch (e: Exception) {
            null
        }
    }

    fun saveScenes(projectId: String, scenes: List<com.flash.draft.model.Scene>) {
        val file = File(projectsDir, "${projectId}_scenes.json")
        val jsonArray = JSONArray()
        scenes.forEach { scene ->
            val obj = JSONObject()
            obj.put("id", scene.id)
            obj.put("name", scene.name)
            obj.put("offsetX", scene.offsetX.toDouble())
            obj.put("offsetY", scene.offsetY.toDouble())
            obj.put("scale", scene.scale.toDouble())
            jsonArray.put(obj)
        }
        val atomicFile = AtomicFile(file)
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(jsonArray.toString().toByteArray())
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            if (stream != null) atomicFile.failWrite(stream)
            e.printStackTrace()
        }
    }

    fun loadScenes(projectId: String): List<com.flash.draft.model.Scene> {
        val start = System.nanoTime()
        val file = File(projectsDir, "${projectId}_scenes.json")
        if (!file.exists()) return emptyList()
        val jsonString = file.readText()
        if (jsonString.isBlank()) return emptyList()
        
        val scenes = mutableListOf<com.flash.draft.model.Scene>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                scenes.add(
                    com.flash.draft.model.Scene(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        offsetX = obj.getDouble("offsetX").toFloat(),
                        offsetY = obj.getDouble("offsetY").toFloat(),
                        scale = obj.getDouble("scale").toFloat()
                    )
                )
            }
        } catch (e: Exception) {
            // Log or ignore
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000f
        com.flash.draft.utils.DraftLogger.logDb("Load Scenes", timeMs = elapsed, details = "Loaded ${scenes.size} scenes for $projectId")
        return scenes
    }
}