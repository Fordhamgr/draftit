package com.flash.draft.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flash.draft.data.NoteRepository
import com.flash.draft.model.Project
import com.flash.draft.utils.TemplateFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val projects: List<Project> = emptyList(),
    val searchQuery: String = "",
    val newProjectName: String = "",
    val selectedTemplate: String = "Empty",
    val templates: List<String> = listOf("Empty", "Brainstorming", "Flowchart", "To-Do List")
)

sealed class DashboardEvent {
    data class OnSearchQueryChanged(val query: String) : DashboardEvent()
    data class OnNewProjectNameChanged(val name: String) : DashboardEvent()
    data class OnTemplateSelected(val template: String) : DashboardEvent()
    object CreateProject : DashboardEvent()
    data class DeleteProject(val id: String) : DashboardEvent()
    data class DuplicateProject(val id: String) : DashboardEvent()
    data class RenameProject(val id: String, val newName: String) : DashboardEvent()
    object LoadProjects : DashboardEvent()
    data class OpenProject(val id: String) : DashboardEvent()
    data class ImportProject(val context: android.content.Context, val inputStream: java.io.InputStream) : DashboardEvent()
    data class ExportProject(val context: android.content.Context, val projectId: String, val outputStream: java.io.OutputStream) : DashboardEvent()
}

sealed class DashboardEffect {
    data class NavigateToCanvas(val projectId: String) : DashboardEffect()
    data class ShowToast(val message: String) : DashboardEffect()
}

class DashboardViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DashboardEffect>()
    val effect: SharedFlow<DashboardEffect> = _effect.asSharedFlow()

    init {
        loadProjects()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                loadProjects()
            }
            is DashboardEvent.OnNewProjectNameChanged -> {
                _uiState.update { it.copy(newProjectName = event.name) }
            }
            is DashboardEvent.OnTemplateSelected -> {
                _uiState.update { it.copy(selectedTemplate = event.template) }
            }
            DashboardEvent.CreateProject -> {
                createProject()
            }
            is DashboardEvent.DeleteProject -> {
                deleteProject(event.id)
            }
            is DashboardEvent.DuplicateProject -> {
                duplicateProject(event.id)
            }
            is DashboardEvent.RenameProject -> {
                renameProject(event.id, event.newName)
            }
            DashboardEvent.LoadProjects -> {
                loadProjects()
            }
            is DashboardEvent.OpenProject -> {
                viewModelScope.launch {
                    _effect.emit(DashboardEffect.NavigateToCanvas(event.id))
                }
            }
            is DashboardEvent.ImportProject -> {
                importProject(event.context, event.inputStream)
            }
            is DashboardEvent.ExportProject -> {
                exportProject(event.context, event.projectId, event.outputStream)
            }
        }
    }

    private fun exportProject(context: android.content.Context, projectId: String, outputStream: java.io.OutputStream) {
        viewModelScope.launch {
            try {
                repository.exportProject(context, projectId, outputStream)
                _effect.emit(DashboardEffect.ShowToast("Project exported successfully"))
            } catch (e: Exception) {
                _effect.emit(DashboardEffect.ShowToast("Failed to export project: ${e.message}"))
            } finally {
                try {
                    outputStream.close()
                } catch (e: Exception) {}
            }
        }
    }

    private fun importProject(context: android.content.Context, inputStream: java.io.InputStream) {
        viewModelScope.launch {
            try {
                val newProjectId = repository.importProjectArchive(context, inputStream)
                if (newProjectId != null) {
                    loadProjects()
                    _effect.emit(DashboardEffect.NavigateToCanvas(newProjectId))
                } else {
                    _effect.emit(DashboardEffect.ShowToast("Failed to import project."))
                }
            } catch (e: Exception) {
                _effect.emit(DashboardEffect.ShowToast("Error importing project: ${e.message}"))
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {}
            }
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val projects = if (query.isBlank()) {
                repository.getAllProjects()
            } else {
                repository.getAllProjects().filter { it.title.contains(query, ignoreCase = true) }
            }
            _uiState.update { it.copy(projects = projects) }
        }
    }

    private fun createProject() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.newProjectName.isNotBlank()) {
                val newProjectId = repository.createNewProject(state.newProjectName)
                val elements = TemplateFactory.createElements(state.selectedTemplate)
                repository.saveElements(newProjectId, elements)
                
                _uiState.update { it.copy(newProjectName = "", selectedTemplate = "Empty") }
                loadProjects()
                _effect.emit(DashboardEffect.NavigateToCanvas(newProjectId))
            }
        }
    }

    private fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            loadProjects()
        }
    }

    private fun duplicateProject(id: String) {
        viewModelScope.launch {
            repository.duplicateProject(id)
            loadProjects()
        }
    }

    private fun renameProject(id: String, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.renameProject(id, newName)
                loadProjects()
            }
        }
    }
}

class DashboardViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
