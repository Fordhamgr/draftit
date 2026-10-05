package com.flash.draft.ui.canvas

import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import com.flash.draft.data.NoteRepository
import com.flash.draft.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class CanvasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var noteRepo: NoteRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var viewModel: CanvasViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        noteRepo = mockk(relaxed = true)
        settingsRepo = mockk(relaxed = true)

        every { settingsRepo.getDarkModeFlow() } returns MutableStateFlow(false)
        every { settingsRepo.getPerformanceModeFlow() } returns MutableStateFlow(false)
        every { settingsRepo.getHapticFeedbackFlow() } returns MutableStateFlow(false)
        every { settingsRepo.getStylusSupportFlow() } returns MutableStateFlow(false)
        every { settingsRepo.getLiquidCurveFlow() } returns MutableStateFlow(0.5f)
        every { settingsRepo.getLiquidRefractionFlow() } returns MutableStateFlow(0.5f)
        every { settingsRepo.getFramerateFlow() } returns MutableStateFlow(60)
        every { settingsRepo.getUndoHistoryLimitFlow() } returns MutableStateFlow(50)

        every { noteRepo.getProjectBackgroundStyle("test_project") } returns BackgroundStyle.Solid

        viewModel = CanvasViewModel(noteRepo, settingsRepo, "test_project")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test ChangeBackgroundStyle event updates uiState and repository`() {
        viewModel.onEvent(CanvasUiEvent.ChangeBackgroundStyle(BackgroundStyle.Blueprint))
        
        assertEquals(BackgroundStyle.Blueprint, viewModel.uiState.value.backgroundStyle)
        verify { noteRepo.updateProjectBackgroundStyle("test_project", BackgroundStyle.Blueprint) }
    }
}
