package com.flash.draft.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

import io.mockk.mockk
import io.mockk.every

class NoteRepositoryTest {

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
        repository.updateProjectBackgroundStyle(projectId, com.flash.draft.ui.canvas.BackgroundStyle.GraphPaper)

        // Retrieve
        val retrievedStyle = repository.getProjectBackgroundStyle(projectId)
        
        assertEquals(com.flash.draft.ui.canvas.BackgroundStyle.GraphPaper, retrievedStyle)
    }
}
