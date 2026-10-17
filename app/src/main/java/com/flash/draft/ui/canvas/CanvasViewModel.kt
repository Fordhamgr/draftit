package com.flash.draft.ui.canvas

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.flash.draft.model.BackgroundStyle

data class CanvasUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.Solid,
    val elements: List<CanvasElement> = emptyList(),
    val scenes: List<Scene> = emptyList()
)

sealed interface CanvasUiEvent {
    data class ChangeBackgroundStyle(val style: BackgroundStyle) : CanvasUiEvent
}

data class CanvasState(
    val elements: List<CanvasElement>,
    val scenes: List<Scene>
)

class CanvasViewModel(
    private val repository: NoteRepository,
    private val settingsRepo: com.flash.draft.data.SettingsRepository,
    val projectId: String // <--- NEW: The Brain now knows WHICH board it is managing!
) : ViewModel() {

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()


    val isDarkMode: StateFlow<Boolean> = settingsRepo.getDarkModeFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), settingsRepo.isDarkMode())

    val isLiquidDarkTheme: StateFlow<Boolean> = settingsRepo.getLiquidDarkThemeFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), settingsRepo.isLiquidDarkTheme())

    val isPerformanceMode: StateFlow<Boolean> = settingsRepo.getPerformanceModeFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), settingsRepo.isPerformanceMode())

    val isHapticFeedbackEnabled: StateFlow<Boolean> = settingsRepo.getHapticFeedbackFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), settingsRepo.isHapticFeedbackEnabled())

    val isStylusSupportEnabled: StateFlow<Boolean> = settingsRepo.getStylusSupportFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), settingsRepo.isStylusSupportEnabled())

    val recentColors: StateFlow<List<Color>> = settingsRepo.getRecentColorsFlow()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), settingsRepo.getRecentColors())

    fun addRecentColor(color: Color) {
        settingsRepo.addRecentColor(color)
    }


    fun onEvent(event: CanvasUiEvent) {
        when (event) {
            is CanvasUiEvent.ChangeBackgroundStyle -> {
                _uiState.value = _uiState.value.copy(backgroundStyle = event.style)
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateProjectBackgroundStyle(projectId, event.style)
                }
            }
        }
    }

    private val undoStack = ArrayDeque<CanvasState>()
    private val redoStack = ArrayDeque<CanvasState>()
    private var maxHistorySize = 50 // Will be updated by settings

    private var saveJob: Job? = null



    private fun pushToUndoStack() {
        undoStack.addLast(CanvasState(_uiState.value.elements.toList(), _uiState.value.scenes.toList()))
        if (maxHistorySize > 0) {
            while (undoStack.size > maxHistorySize) undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun updateElements(newElements: List<CanvasElement>) {
        if (_uiState.value.elements.isNotEmpty() || _uiState.value.scenes.isNotEmpty()) {
            pushToUndoStack()
        }
        _uiState.update { it.copy(elements = newElements) }
        triggerAutoSave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            com.flash.draft.utils.DraftLogger.logTrigger("Undo", payload = "Remaining stack: ${undoStack.size - 1}")
            redoStack.addLast(CanvasState(_uiState.value.elements.toList(), _uiState.value.scenes.toList()))
            val prevState = undoStack.removeLast()
            _uiState.update { it.copy(elements = prevState.elements, scenes = prevState.scenes) }
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            com.flash.draft.utils.DraftLogger.logTrigger("Redo", payload = "Remaining stack: ${redoStack.size - 1}")
            undoStack.addLast(CanvasState(_uiState.value.elements.toList(), _uiState.value.scenes.toList()))
            val nextState = redoStack.removeLast()
            _uiState.update { it.copy(elements = nextState.elements, scenes = nextState.scenes) }
            triggerAutoSave()
        }
    }

    private fun triggerAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            // Immediate temp save for crash recovery
            val elementsSnapshot = _uiState.value.elements
            val scenesSnapshot = _uiState.value.scenes
            
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
        _uiState.update { it.copy(scenes = it.scenes + newScene) }
        triggerAutoSave()
        return newScene.id
    }

    fun removeScene(id: String) {
        com.flash.draft.utils.DraftLogger.logTrigger("Remove Scene", payload = id)
        pushToUndoStack()
        _uiState.update { it.copy(scenes = it.scenes.filterNot { s -> s.id == id }) }
        triggerAutoSave()
    }

    fun renameScene(id: String, newName: String) {
        com.flash.draft.utils.DraftLogger.logTrigger("Rename Scene", payload = "$id -> $newName")
        pushToUndoStack()
        _uiState.update { s -> s.copy(scenes = s.scenes.map { if (it.id == id) it.copy(name = newName) else it }) }
        triggerAutoSave()
    }

    fun triggerImmediateSave() {
        saveJob?.cancel()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            com.flash.draft.utils.DraftLogger.logDb("ImmediateSave Triggered", details = "Project: $projectId")
            repository.saveElements(projectId, _uiState.value.elements)
            repository.saveScenes(projectId, _uiState.value.scenes)
        }
    }

    suspend fun triggerImmediateSaveSync() {
        saveJob?.cancel()
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            com.flash.draft.utils.DraftLogger.logDb("ImmediateSaveSync Triggered", details = "Project: $projectId")
            repository.saveElements(projectId, _uiState.value.elements)
            repository.saveScenes(projectId, _uiState.value.scenes)
        }
    }

    fun saveProjectThumbnail(bitmap: android.graphics.Bitmap) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            repository.saveProjectThumbnail(projectId, bitmap)
        }
    }

    suspend fun saveProjectThumbnailSync(bitmap: android.graphics.Bitmap) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            repository.saveProjectThumbnail(projectId, bitmap)
        }
    }

    fun clearCanvas() {
        com.flash.draft.utils.DraftLogger.logTrigger("Clear Canvas")
        pushToUndoStack()
        _uiState.update { it.copy(elements = emptyList()) }
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

    val framerate: kotlinx.coroutines.flow.StateFlow<Int> = settingsRepo.getFramerateFlow().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 60
    )

    fun getRawProjectJson(): String? {
        return repository.getRawJson(projectId)
    }

    init {
        // Crash Recovery: Instantly load THIS specific project when the Canvas boots
        viewModelScope.launch(Dispatchers.IO) {
            val savedData = repository.loadElements(projectId) // <--- Updated
            val savedScenes = repository.loadScenes(projectId)
            
            val bgStyle = repository.getProjectBackgroundStyle(projectId)
            _uiState.value = _uiState.value.copy(
                backgroundStyle = bgStyle,
                isLoading = false,
                elements = savedData,
                scenes = savedScenes
            )
        }

        // Observe Settings
        viewModelScope.launch {
            settingsRepo.getUndoHistoryLimitFlow().collect { limit ->
                maxHistorySize = limit
                if (maxHistorySize > 0) {
                    while (undoStack.size > maxHistorySize) undoStack.removeFirst()
                    while (redoStack.size > maxHistorySize) redoStack.removeFirst()
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.getLiquidCurveFlow().collect { level ->
                setLiquidCurve(level)
            }
        }
        viewModelScope.launch {
            settingsRepo.getLiquidFrostFlow().collect { level ->
                setLiquidFrost(level)
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
