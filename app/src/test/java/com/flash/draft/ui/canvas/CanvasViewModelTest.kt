package com.flash.draft.ui.canvas

import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.mockk.mockkObject
import io.mockk.unmockkObject
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
        mockkObject(com.flash.draft.utils.DraftLogger)
        every { com.flash.draft.utils.DraftLogger.logTrigger(any(), any()) } returns Unit
        every { com.flash.draft.utils.DraftLogger.logDb(any(), any()) } returns Unit
        
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

        every { noteRepo.getProjectBackgroundStyle("test_project") } returns com.flash.draft.model.BackgroundStyle.Solid

        viewModel = CanvasViewModel(noteRepo, settingsRepo, "test_project")
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        unmockkObject(com.flash.draft.utils.DraftLogger)
    }

    @Test
    fun `test ChangeBackgroundStyle event updates uiState and repository`() {
        viewModel.onEvent(CanvasUiEvent.ChangeBackgroundStyle(com.flash.draft.model.BackgroundStyle.Blueprint))
        
        assertEquals(com.flash.draft.model.BackgroundStyle.Blueprint, viewModel.uiState.value.backgroundStyle)
        verify { noteRepo.updateProjectBackgroundStyle("test_project", com.flash.draft.model.BackgroundStyle.Blueprint) }
    }

    @Test
    fun `test updateElements and undo redo updates uiState`() {
        // Mock a single element
        val element1 = mockk<com.flash.draft.model.CanvasElement>(relaxed = true)
        val element2 = mockk<com.flash.draft.model.CanvasElement>(relaxed = true)

        // initial empty state is not pushed. update with element1
        viewModel.updateElements(listOf(element1))
        assertEquals(listOf(element1), viewModel.uiState.value.elements)

        // update with element2, this pushes listOf(element1) to undoStack
        viewModel.updateElements(listOf(element1, element2))
        assertEquals(listOf(element1, element2), viewModel.uiState.value.elements)

        // Undo
        viewModel.undo()
        assertEquals(listOf(element1), viewModel.uiState.value.elements)

        // Redo
        viewModel.redo()
        assertEquals(listOf(element1, element2), viewModel.uiState.value.elements)
    }

    @Test
    fun `test addScene and undo redo updates uiState`() {
        // Initial state
        assertEquals(emptyList<com.flash.draft.model.Scene>(), viewModel.uiState.value.scenes)

        // We first force an undo stack push by having a non-empty state
        val dummyElement = mockk<com.flash.draft.model.CanvasElement>(relaxed = true)
        viewModel.updateElements(listOf(dummyElement))

        // Add a scene
        val sceneId = viewModel.addScene("Scene 1", 0f, 0f, 1f)
        assertEquals(1, viewModel.uiState.value.scenes.size)
        assertEquals("Scene 1", viewModel.uiState.value.scenes.first().name)

        // Undo
        viewModel.undo()
        assertEquals(emptyList<com.flash.draft.model.Scene>(), viewModel.uiState.value.scenes)

        // Redo
        viewModel.redo()
        assertEquals(1, viewModel.uiState.value.scenes.size)
        assertEquals("Scene 1", viewModel.uiState.value.scenes.first().name)
    }
}
