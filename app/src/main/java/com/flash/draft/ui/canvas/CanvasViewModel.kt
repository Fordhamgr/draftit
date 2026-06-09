package com.flash.draft.ui.canvas

import androidx.compose.ui.graphics.asAndroidBitmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.draft.data.NoteRepository
import com.flash.draft.model.CanvasElement
import com.flash.draft.model.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CanvasViewModel(
    private val repository: NoteRepository,
    val projectId: String // <--- NEW: The Brain now knows WHICH board it is managing!
) : ViewModel() {

    private val _elements = MutableStateFlow<List<CanvasElement>>(emptyList())
    val elements = _elements.asStateFlow()

    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    val scenes = _scenes.asStateFlow()

    private val undoStack = ArrayDeque<List<CanvasElement>>()
    private val redoStack = ArrayDeque<List<CanvasElement>>()
    private val maxHistorySize = 50

    private var saveJob: Job? = null

    init {
        // Crash Recovery: Instantly load THIS specific project when the Canvas boots
        viewModelScope.launch(Dispatchers.IO) {
            val savedData = repository.loadElements(projectId) // <--- Updated
            _elements.value = savedData
            val savedScenes = repository.loadScenes(projectId)
            _scenes.value = savedScenes
        }
    }

    fun updateElements(newElements: List<CanvasElement>) {
        if (_elements.value.isNotEmpty()) {
            undoStack.addLast(_elements.value.toList())
            if (undoStack.size > maxHistorySize) undoStack.removeFirst()
        }

        redoStack.clear()
        _elements.value = newElements

        triggerAutoSave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(_elements.value.toList())
            _elements.value = undoStack.removeLast()
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(_elements.value.toList())
            _elements.value = redoStack.removeLast()
            triggerAutoSave()
        }
    }

    private fun triggerAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            // Background Auto-Save specifically to THIS project's file
            repository.saveElements(projectId, _elements.value) // <--- Updated
            repository.saveScenes(projectId, _scenes.value)
        }
    }

    fun addScene(name: String, offsetX: Float, offsetY: Float, scale: Float): String {
        val newScene = Scene(name = name, offsetX = offsetX, offsetY = offsetY, scale = scale)
        _scenes.value = _scenes.value + newScene
        triggerAutoSave()
        return newScene.id
    }

    fun removeScene(id: String) {
        _scenes.value = _scenes.value.filterNot { it.id == id }
        triggerAutoSave()
    }

    fun renameScene(id: String, newName: String) {
        _scenes.value = _scenes.value.map { if (it.id == id) it.copy(name = newName) else it }
        triggerAutoSave()
    }

    fun triggerImmediateSave() {
        saveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveElements(projectId, _elements.value)
            repository.saveScenes(projectId, _scenes.value)
        }
    }
    // --- LIQUID PHYSICS STATE ---
    private val _liquidRefraction = kotlinx.coroutines.flow.MutableStateFlow(0.5f)
    val liquidRefraction = _liquidRefraction.asStateFlow()

    private val _liquidCurve = kotlinx.coroutines.flow.MutableStateFlow(0.5f)
    val liquidCurve = _liquidCurve.asStateFlow()

    fun setLiquidRefraction(value: Float) { _liquidRefraction.value = value }
    fun setLiquidCurve(value: Float) { _liquidCurve.value = value }
    
    private val _liquidFrost = kotlinx.coroutines.flow.MutableStateFlow(2f)
    val liquidFrost = _liquidFrost.asStateFlow()

    private val _liquidDispersion = kotlinx.coroutines.flow.MutableStateFlow(0.05f)
    val liquidDispersion = _liquidDispersion.asStateFlow()

    fun setLiquidFrost(value: Float) { _liquidFrost.value = value }
    fun setLiquidDispersion(value: Float) { _liquidDispersion.value = value }

    fun getRawProjectJson(): String? {
        return repository.getRawJson(projectId)
    }

    // Removed exportPaginatedPdf
}