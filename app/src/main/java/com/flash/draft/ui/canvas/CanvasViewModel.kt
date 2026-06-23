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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CanvasState(
    val elements: List<CanvasElement>,
    val scenes: List<Scene>
)

class CanvasViewModel(
    private val repository: NoteRepository,
    private val settingsRepo: com.flash.draft.data.SettingsRepository,
    val projectId: String // <--- NEW: The Brain now knows WHICH board it is managing!
) : ViewModel() {

    private val _elements = MutableStateFlow<List<CanvasElement>>(emptyList())
    val elements = _elements.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = settingsRepo.getDarkModeFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    val isPerformanceMode: StateFlow<Boolean> = settingsRepo.getPerformanceModeFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    val isHapticFeedbackEnabled: StateFlow<Boolean> = settingsRepo.getHapticFeedbackFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)


    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    val scenes = _scenes.asStateFlow()

    private val undoStack = ArrayDeque<CanvasState>()
    private val redoStack = ArrayDeque<CanvasState>()
    private var maxHistorySize = 50 // Will be updated by settings

    private var saveJob: Job? = null



    private fun pushToUndoStack() {
        undoStack.addLast(CanvasState(_elements.value.toList(), _scenes.value.toList()))
        while (undoStack.size > maxHistorySize) undoStack.removeFirst()
        redoStack.clear()
    }

    fun updateElements(newElements: List<CanvasElement>) {
        if (_elements.value.isNotEmpty() || _scenes.value.isNotEmpty()) {
            pushToUndoStack()
        }
        _elements.value = newElements
        triggerAutoSave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            com.flash.draft.utils.DraftLogger.logTrigger("Undo", payload = "Remaining stack: ${undoStack.size - 1}")
            redoStack.addLast(CanvasState(_elements.value.toList(), _scenes.value.toList()))
            val prevState = undoStack.removeLast()
            _elements.value = prevState.elements
            _scenes.value = prevState.scenes
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            com.flash.draft.utils.DraftLogger.logTrigger("Redo", payload = "Remaining stack: ${redoStack.size - 1}")
            undoStack.addLast(CanvasState(_elements.value.toList(), _scenes.value.toList()))
            val nextState = redoStack.removeLast()
            _elements.value = nextState.elements
            _scenes.value = nextState.scenes
            triggerAutoSave()
        }
    }

    private fun triggerAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            // Immediate temp save for crash recovery
            val elementsSnapshot = _elements.value
            val scenesSnapshot = _scenes.value
            
            // Fast debounce for the actual file
            delay(500)
            com.flash.draft.utils.DraftLogger.logDb("AutoSave Triggered", details = "Project: $projectId")
            repository.saveElements(projectId, elementsSnapshot)
            repository.saveScenes(projectId, scenesSnapshot)
        }
    }

    fun addScene(name: String, offsetX: Float, offsetY: Float, scale: Float): String {
        com.flash.draft.utils.DraftLogger.logTrigger("Add Scene", payload = name)
        pushToUndoStack()
        val newScene = Scene(name = name, offsetX = offsetX, offsetY = offsetY, scale = scale)
        _scenes.value = _scenes.value + newScene
        triggerAutoSave()
        return newScene.id
    }

    fun removeScene(id: String) {
        com.flash.draft.utils.DraftLogger.logTrigger("Remove Scene", payload = id)
        pushToUndoStack()
        _scenes.value = _scenes.value.filterNot { it.id == id }
        triggerAutoSave()
    }

    fun renameScene(id: String, newName: String) {
        com.flash.draft.utils.DraftLogger.logTrigger("Rename Scene", payload = "$id -> $newName")
        pushToUndoStack()
        _scenes.value = _scenes.value.map { if (it.id == id) it.copy(name = newName) else it }
        triggerAutoSave()
    }

    fun triggerImmediateSave() {
        saveJob?.cancel()
        // Fire-and-forget save in an independent scope so it survives ViewModel death (e.g. popping backstack)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            com.flash.draft.utils.DraftLogger.logDb("ImmediateSave Triggered", details = "Project: $projectId")
            repository.saveElements(projectId, _elements.value)
            repository.saveScenes(projectId, _scenes.value)
        }
    }

    fun clearCanvas() {
        com.flash.draft.utils.DraftLogger.logTrigger("Clear Canvas")
        pushToUndoStack()
        _elements.value = emptyList()
        triggerAutoSave()
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

    init {
        // Crash Recovery: Instantly load THIS specific project when the Canvas boots
        viewModelScope.launch(Dispatchers.IO) {
            val savedData = repository.loadElements(projectId) // <--- Updated
            _elements.value = savedData
            val savedScenes = repository.loadScenes(projectId)
            _scenes.value = savedScenes
        }

        // Observe Settings
        viewModelScope.launch {
            settingsRepo.getUndoHistoryLimitFlow().collect { limit ->
                maxHistorySize = limit
                while (undoStack.size > maxHistorySize) undoStack.removeFirst()
                while (redoStack.size > maxHistorySize) redoStack.removeFirst()
            }
        }
        viewModelScope.launch {
            settingsRepo.getLiquidCurveFlow().collect { level ->
                setLiquidCurve(level)
            }
        }
        viewModelScope.launch {
            settingsRepo.getLiquidRefractionFlow().collect { level ->
                setLiquidRefraction(level)
            }
        }
    }

    // Removed exportPaginatedPdf
}