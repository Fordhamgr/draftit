package com.flash.draft.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ProjectArchiveHelper {

    /**
     * Exports a project and its associated media to a ZIP output stream.
     */
    fun exportToZip(
        context: Context,
        projectId: String,
        elementsJson: String,
        scenesJson: String?,
        metaJson: String?,
        outputStream: OutputStream
    ) {
        ZipOutputStream(outputStream).use { zos ->
            val modifiedJson = processAndZipMedia(context, elementsJson, zos)
            
            // Write project.json
            zos.putNextEntry(ZipEntry("project.json"))
            zos.write(modifiedJson.toByteArray())
            zos.closeEntry()

            // Write scenes.json
            if (!scenesJson.isNullOrBlank()) {
                zos.putNextEntry(ZipEntry("scenes.json"))
                zos.write(scenesJson.toByteArray())
                zos.closeEntry()
            }
            
            // Write meta.json
            if (!metaJson.isNullOrBlank()) {
                zos.putNextEntry(ZipEntry("meta.json"))
                zos.write(metaJson.toByteArray())
                zos.closeEntry()
            }
        }
    }

    /**
     * Imports a .draftit ZIP file, extracts media to local storage, and returns the extracted JSON contents.
     */
    fun importFromZip(
        context: Context,
        inputStream: InputStream
    ): Triple<String?, String?, String?> {
        val mediaDir = File(context.filesDir, "projects/media")
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }

        var projectJson: String? = null
        var scenesJson: String? = null
        var metaJson: String? = null
        val mediaMap = mutableMapOf<String, String>() // mapping "media/filename" to actual saved path

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "project.json") {
                    projectJson = zis.readBytes().toString(Charsets.UTF_8)
                } else if (entry.name == "scenes.json") {
                    scenesJson = zis.readBytes().toString(Charsets.UTF_8)
                } else if (entry.name == "meta.json") {
                    metaJson = zis.readBytes().toString(Charsets.UTF_8)
                } else if (entry.name.startsWith("media/")) {
                    val fileName = entry.name.substringAfter("media/")
                    if (fileName.isNotBlank()) {
                        val outputFile = File(mediaDir, fileName)
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        mediaMap[entry.name] = Uri.fromFile(outputFile).toString()
                    }
                }
                entry = zis.nextEntry
            }
        }

        // Restore local paths in the JSON
        if (projectJson != null) {
            projectJson = restoreMediaUris(projectJson!!, mediaMap)
        }

        return Triple(projectJson, scenesJson, metaJson)
    }

    private fun processAndZipMedia(context: Context, json: String, zos: ZipOutputStream): String {
        try {
            val jsonArray = JSONArray(json)
            val resolver = context.contentResolver

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val type = obj.optString("type", "")

                when (type) {
                    "image" -> {
                        val uriStr = obj.optString("uri", "")
                        if (uriStr.isNotBlank()) {
                            val newPath = zipMediaFile(context, uriStr, zos)
                            if (newPath != null) {
                                obj.put("uri", newPath)
                            }
                        }
                    }
                    "pdf" -> {
                        val uriStr = obj.optString("uriString", "")
                        if (uriStr.isNotBlank()) {
                            val newPath = zipMediaFile(context, uriStr, zos)
                            if (newPath != null) {
                                obj.put("uriString", newPath)
                            }
                        }
                    }
                    "audio" -> {
                        val pathStr = obj.optString("audioPath", "")
                        if (pathStr.isNotBlank()) {
                            val file = File(pathStr)
                            if (file.exists()) {
                                val newName = "media/${file.name}"
                                zos.putNextEntry(ZipEntry(newName))
                                FileInputStream(file).use { it.copyTo(zos) }
                                zos.closeEntry()
                                obj.put("audioPath", newName)
                            }
                        }
                    }
                }
            }
            return jsonArray.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return json // Return original on failure
        }
    }

    private fun zipMediaFile(context: Context, uriString: String, zos: ZipOutputStream): String? {
        try {
            val uri = Uri.parse(uriString)
            val extension = if (uriString.contains(".pdf", ignoreCase = true)) "pdf" else "jpg" // Simplified
            val newName = "media/${UUID.randomUUID()}.$extension"
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                zos.putNextEntry(ZipEntry(newName))
                inputStream.copyTo(zos)
                zos.closeEntry()
                return newName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun restoreMediaUris(json: String, mediaMap: Map<String, String>): String {
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val type = obj.optString("type", "")

                when (type) {
                    "image" -> {
                        val uriStr = obj.optString("uri", "")
                        if (uriStr.startsWith("media/")) {
                            mediaMap[uriStr]?.let { obj.put("uri", it) }
                        }
                    }
                    "pdf" -> {
                        val uriStr = obj.optString("uriString", "")
                        if (uriStr.startsWith("media/")) {
                            mediaMap[uriStr]?.let { obj.put("uriString", it) }
                        }
                    }
                    "audio" -> {
                        val pathStr = obj.optString("audioPath", "")
                        if (pathStr.startsWith("media/")) {
                            mediaMap[pathStr]?.let { 
                                // audioPath expects absolute path, not URI string
                                val actualPath = Uri.parse(it).path ?: it
                                obj.put("audioPath", actualPath) 
                            }
                        }
                    }
                }
            }
            return jsonArray.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return json
        }
    }
}
