package com.flash.draft.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

import io.mockk.mockk
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import android.util.AtomicFile
import java.io.FileOutputStream

class NoteRepositoryTest {

    @Before
    fun setup() {
        mockkConstructor(AtomicFile::class)
        every { anyConstructed<AtomicFile>().startWrite() } answers {
            val tempDir = System.getProperty("java.io.tmpdir")
            val file = File(tempDir, "draftit_test_files/projects/test_project_style.json")
            file.parentFile.mkdirs()
            FileOutputStream(file)
        }
        every { anyConstructed<AtomicFile>().finishWrite(any()) } answers {
            val stream = it.invocation.args[0] as? FileOutputStream
            stream?.close()
        }
        every { anyConstructed<AtomicFile>().failWrite(any()) } answers {}
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun getMockContext(): Context {
        val tempDir = System.getProperty("java.io.tmpdir")
        val filesDir = File(tempDir, "draftit_test_files").apply { mkdirs() }
        val context = mockk<Context>(relaxed = true)
        every { context.filesDir } returns filesDir
        return context
    }

    @Test
    fun `test backgroundStyle is saved and retrieved from meta json`() {
        val context = getMockContext()
        val repository = NoteRepository(context)
        val projectId = "test_project_bg"

        // Save
        repository.updateProjectBackgroundStyle(projectId, com.flash.draft.model.BackgroundStyle.GraphPaper)

        // Retrieve
        val retrievedStyle = repository.getProjectBackgroundStyle(projectId)
        
        assertEquals(com.flash.draft.model.BackgroundStyle.GraphPaper, retrievedStyle)
    }

    @Test
    fun `test sticky note style is saved and retrieved correctly`() {
        val context = getMockContext()
        val repository = NoteRepository(context)
        val projectId = "test_project_style"

        // Save
        val originalNote = com.flash.draft.model.CanvasElement.StickyNote(
            id = "note_1",
            position = androidx.compose.ui.geometry.Offset(10f, 10f),
            text = "Test Note",
            style = "Glass"
        )
        repository.saveElements(projectId, listOf(originalNote))

        // Retrieve
        val loadedElements = repository.loadElements(projectId)
        val loadedNote = loadedElements.first() as com.flash.draft.model.CanvasElement.StickyNote
        
        assertEquals("Glass", loadedNote.style)
    }

    @Test
    fun `test sticky note missing style defaults to Classic`() {
        val context = getMockContext()
        val repository = NoteRepository(context)
        val projectId = "test_project_missing_style"
        
        // Manually write a JSON string missing the style property
        val legacyJson = """
            [
                {
                    "type": "note",
                    "id": "note_2",
                    "x": 20.0,
                    "y": 20.0,
                    "text": "Legacy Note",
                    "color": "4294965700",
                    "fontSize": 16.0,
                    "fontFamily": "SansSerif",
                    "width": 200.0,
                    "height": 200.0
                }
            ]
        """.trimIndent()
        
        val projectsDir = File(context.filesDir, "projects")
        projectsDir.mkdirs()
        val file = File(projectsDir, "${projectId}.json")
        file.writeText(legacyJson)

        // Retrieve and check default
        val loadedElements = repository.loadElements(projectId)
        val loadedNote = loadedElements.first() as com.flash.draft.model.CanvasElement.StickyNote
        
        assertEquals("Classic", loadedNote.style)
    }
}
