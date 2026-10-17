package com.flash.draft.ui.onboarding

import androidx.compose.runtime.Immutable

enum class DemoIllustrationType {
    INFINITE_CANVAS,
    FLUID_STUDIO
}

@Immutable
data class OnboardingCardData(
    val id: String,
    val title: String,
    val description: String,
    val illustrationType: DemoIllustrationType
)

@Immutable
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 2,
    val cards: List<OnboardingCardData> = emptyList(),
    val isActionEnabled: Boolean = true
)

sealed interface OnboardingUiEvent {
    data object NextClicked : OnboardingUiEvent
    data object BackClicked : OnboardingUiEvent
    data object SkipClicked : OnboardingUiEvent
    data class PageSelected(val page: Int) : OnboardingUiEvent
}

sealed interface OnboardingEffect {
    data object NavigateToDashboard : OnboardingEffect
}
