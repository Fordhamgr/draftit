package com.flash.draft.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flash.draft.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val isLiquidDarkTheme: Boolean = true,
    val isPerformanceMode: Boolean = false,
    val framerate: Int = 0,
    val liquidCurve: Float = 0.5f,
    val liquidRefraction: Float = 0.5f,
    val liquidFrost: Float = 0.0f,
    val undoHistoryLimit: Int = 0,
    val isHapticFeedbackEnabled: Boolean = false,
    val isStylusSupportEnabled: Boolean = false
)

sealed interface SettingsUiEvent {
    data class ToggleDarkMode(val enabled: Boolean) : SettingsUiEvent
    data class ToggleLiquidDarkTheme(val enabled: Boolean) : SettingsUiEvent
    data class TogglePerformanceMode(val enabled: Boolean) : SettingsUiEvent
    data class SetFramerate(val framerate: Int) : SettingsUiEvent
    data class SetLiquidCurve(val level: Float) : SettingsUiEvent
    data class SetLiquidRefraction(val level: Float) : SettingsUiEvent
    data class SetLiquidFrost(val level: Float) : SettingsUiEvent
    data class SetUndoHistoryLimit(val limit: Int) : SettingsUiEvent
    data class ToggleHapticFeedback(val enabled: Boolean) : SettingsUiEvent
    data class ToggleStylusSupport(val enabled: Boolean) : SettingsUiEvent
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = repository.getDarkModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isDarkMode())

    val isLiquidDarkTheme: StateFlow<Boolean> = repository.getLiquidDarkThemeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isLiquidDarkTheme())

    val isPerformanceMode: StateFlow<Boolean> = repository.getPerformanceModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isPerformanceMode())

    val framerate: StateFlow<Int> = repository.getFramerateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getFramerate())

    val liquidCurve: StateFlow<Float> = repository.getLiquidCurveFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getLiquidCurve())

    val liquidRefraction: StateFlow<Float> = repository.getLiquidRefractionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getLiquidRefraction())

    val liquidFrost: StateFlow<Float> = repository.getLiquidFrostFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getLiquidFrost())

    val undoHistoryLimit: StateFlow<Int> = repository.getUndoHistoryLimitFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getUndoHistoryLimit())

    val isHapticFeedbackEnabled: StateFlow<Boolean> = repository.getHapticFeedbackFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isHapticFeedbackEnabled())

    val isStylusSupportEnabled: StateFlow<Boolean> = repository.getStylusSupportFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isStylusSupportEnabled())

    val uiState: StateFlow<SettingsUiState> = combine(
        listOf(
            repository.getDarkModeFlow(),
            repository.getLiquidDarkThemeFlow(),
            repository.getPerformanceModeFlow(),
            repository.getFramerateFlow(),
            repository.getLiquidCurveFlow(),
            repository.getLiquidRefractionFlow(),
            repository.getLiquidFrostFlow(),
            repository.getUndoHistoryLimitFlow(),
            repository.getHapticFeedbackFlow(),
            repository.getStylusSupportFlow()
        )
    ) { array ->
        SettingsUiState(
            isDarkMode = array[0] as Boolean,
            isLiquidDarkTheme = array[1] as Boolean,
            isPerformanceMode = array[2] as Boolean,
            framerate = array[3] as Int,
            liquidCurve = array[4] as Float,
            liquidRefraction = array[5] as Float,
            liquidFrost = array[6] as Float,
            undoHistoryLimit = array[7] as Int,
            isHapticFeedbackEnabled = array[8] as Boolean,
            isStylusSupportEnabled = array[9] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            isDarkMode = repository.isDarkMode(),
            isLiquidDarkTheme = repository.isLiquidDarkTheme(),
            isPerformanceMode = repository.isPerformanceMode(),
            framerate = repository.getFramerate(),
            liquidCurve = repository.getLiquidCurve(),
            liquidRefraction = repository.getLiquidRefraction(),
            liquidFrost = repository.getLiquidFrost(),
            undoHistoryLimit = repository.getUndoHistoryLimit(),
            isHapticFeedbackEnabled = repository.isHapticFeedbackEnabled(),
            isStylusSupportEnabled = repository.isStylusSupportEnabled()
        )
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ToggleDarkMode -> toggleDarkMode(event.enabled)
            is SettingsUiEvent.ToggleLiquidDarkTheme -> toggleLiquidDarkTheme(event.enabled)
            is SettingsUiEvent.TogglePerformanceMode -> togglePerformanceMode(event.enabled)
            is SettingsUiEvent.SetFramerate -> setFramerate(event.framerate)
            is SettingsUiEvent.SetLiquidCurve -> setLiquidCurve(event.level)
            is SettingsUiEvent.SetLiquidRefraction -> setLiquidRefraction(event.level)
            is SettingsUiEvent.SetLiquidFrost -> setLiquidFrost(event.level)
            is SettingsUiEvent.SetUndoHistoryLimit -> setUndoHistoryLimit(event.limit)
            is SettingsUiEvent.ToggleHapticFeedback -> toggleHapticFeedback(event.enabled)
            is SettingsUiEvent.ToggleStylusSupport -> toggleStylusSupport(event.enabled)
        }
    }

    fun toggleDarkMode(enabled: Boolean) = repository.setDarkMode(enabled)
    fun toggleLiquidDarkTheme(enabled: Boolean) = repository.setLiquidDarkTheme(enabled)
    fun togglePerformanceMode(enabled: Boolean) = repository.setPerformanceMode(enabled)
    fun setFramerate(framerate: Int) = repository.setFramerate(framerate)

    fun setLiquidCurve(level: Float) = repository.setLiquidCurve(level)
    fun setLiquidRefraction(level: Float) = repository.setLiquidRefraction(level)
    fun setLiquidFrost(level: Float) = repository.setLiquidFrost(level)
    fun setUndoHistoryLimit(limit: Int) = repository.setUndoHistoryLimit(limit)
    fun toggleHapticFeedback(enabled: Boolean) = repository.setHapticFeedbackEnabled(enabled)
    fun toggleStylusSupport(enabled: Boolean) = repository.setStylusSupportEnabled(enabled)
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
