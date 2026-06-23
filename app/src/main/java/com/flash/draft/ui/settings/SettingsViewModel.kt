package com.flash.draft.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flash.draft.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = repository.getDarkModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPerformanceMode: StateFlow<Boolean> = repository.getPerformanceModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val framerate: StateFlow<Int> = repository.getFramerateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val liquidCurve: StateFlow<Float> = repository.getLiquidCurveFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)

    val liquidRefraction: StateFlow<Float> = repository.getLiquidRefractionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)

    val undoHistoryLimit: StateFlow<Int> = repository.getUndoHistoryLimitFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    val isHapticFeedbackEnabled: StateFlow<Boolean> = repository.getHapticFeedbackFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleDarkMode(enabled: Boolean) = repository.setDarkMode(enabled)
    fun togglePerformanceMode(enabled: Boolean) = repository.setPerformanceMode(enabled)
    fun setFramerate(framerate: Int) = repository.setFramerate(framerate)

    fun setLiquidCurve(level: Float) = repository.setLiquidCurve(level)
    fun setLiquidRefraction(level: Float) = repository.setLiquidRefraction(level)
    fun setUndoHistoryLimit(limit: Int) = repository.setUndoHistoryLimit(limit)
    fun toggleHapticFeedback(enabled: Boolean) = repository.setHapticFeedbackEnabled(enabled)
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
