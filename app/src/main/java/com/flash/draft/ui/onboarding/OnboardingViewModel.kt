package com.flash.draft.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flash.draft.data.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val KEY_CURRENT_PAGE = "onboarding_current_page"
    }

    private val navigated = AtomicBoolean(false)

    private val initialCards = listOf(
        OnboardingCardData(
            id = "infinite_canvas",
            title = "Infinite Creative Canvas",
            description = "Draw, sketch, and think without boundaries. Pan and zoom across a vast limitless canvas with responsive precision.",
            illustrationType = DemoIllustrationType.INFINITE_CANVAS
        ),
        OnboardingCardData(
            id = "fluid_studio",
            title = "Fluid Studio Dock",
            description = "Intuitive floating toolsets, expressive brush mechanics, and liquid gestures designed for total focus.",
            illustrationType = DemoIllustrationType.FLUID_STUDIO
        )
    )

    private val initialPage: Int = savedStateHandle[KEY_CURRENT_PAGE] ?: 0

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            currentPage = initialPage.coerceIn(0, initialCards.size - 1),
            totalPages = initialCards.size,
            cards = initialCards,
            isActionEnabled = true
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effect: Flow<OnboardingEffect> = _effect.receiveAsFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.NextClicked -> {
                val current = _uiState.value.currentPage
                if (current < _uiState.value.totalPages - 1) {
                    setPage(current + 1)
                } else {
                    completeOnboarding()
                }
            }
            is OnboardingUiEvent.BackClicked -> {
                val current = _uiState.value.currentPage
                if (current > 0) {
                    setPage(current - 1)
                }
            }
            is OnboardingUiEvent.SkipClicked -> {
                completeOnboarding()
            }
            is OnboardingUiEvent.PageSelected -> {
                setPage(event.page)
            }
        }
    }

    private fun setPage(page: Int) {
        val clamped = page.coerceIn(0, _uiState.value.totalPages - 1)
        savedStateHandle[KEY_CURRENT_PAGE] = clamped
        _uiState.update { it.copy(currentPage = clamped) }
    }

    private fun completeOnboarding() {
        if (navigated.compareAndSet(false, true)) {
            _uiState.update { it.copy(isActionEnabled = false) }
            settingsRepository.setOnboardingCompleted(true)
            viewModelScope.launch {
                _effect.send(OnboardingEffect.NavigateToDashboard)
            }
        }
    }
}

class OnboardingViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(settingsRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(settingsRepository, SavedStateHandle()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
