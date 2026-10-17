package com.flash.draft.ui.onboarding

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3,
    val isActionEnabled: Boolean = true,
    val isReducedMotion: Boolean = false
)

sealed interface OnboardingUiEvent {
    data object NextClicked : OnboardingUiEvent
    data object SkipClicked : OnboardingUiEvent
    data class PageSelected(val page: Int) : OnboardingUiEvent
    data class ReducedMotionDetected(val isReduced: Boolean) : OnboardingUiEvent
}

sealed interface OnboardingEffect {
    data class ScrollToPage(val page: Int) : OnboardingEffect
    data object NavigateToDashboard : OnboardingEffect
}
