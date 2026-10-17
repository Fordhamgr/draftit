package com.flash.draft.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.flash.draft.ui.canvas.item.findHitConnector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CanvasElementClonerTest {

    @Test
    fun testDeepCloneStickyNote() {
        val original = CanvasElement.StickyNote(
            id = "note-1",
            position = Offset(100f, 100f),
            text = "Test",
            isLocked = true
        )
        val cloned = CanvasElementCloner.deepClone(original, Offset(40f, 40f)) as CanvasElement.StickyNote
        assertNotEquals(original.id, cloned.id)
        assertEquals(Offset(140f, 140f), cloned.position)
        assertEquals("Test", cloned.text)
        assertFalse(cloned.isLocked)
    }

    @Test
    fun testDeepCloneTaskListNode() {
        val original = CanvasElement.TaskListNode(
            id = "task-list-1",
            position = Offset(50f, 50f),
            tasks = listOf(
                CanvasElement.TaskItem(id = "item-1", text = "Buy milk", isCompleted = false),
                CanvasElement.TaskItem(id = "item-2", text = "Write code", isCompleted = true)
            ),
            isLocked = true
        )
        val cloned = CanvasElementCloner.deepClone(original) as CanvasElement.TaskListNode
        assertNotEquals(original.id, cloned.id)
        assertEquals(Offset(90f, 90f), cloned.position)
        assertFalse(cloned.isLocked)
        assertEquals(2, cloned.tasks.size)
        assertNotEquals(original.tasks[0].id, cloned.tasks[0].id)
        assertEquals("Buy milk", cloned.tasks[0].text)
        assertNotEquals(original.tasks[1].id, cloned.tasks[1].id)
        assertEquals("Write code", cloned.tasks[1].text)
    }

    @Test
    fun testDeepCloneSketch() {
        val original = CanvasElement.Sketch(
            id = "sketch-1",
            xCoords = floatArrayOf(10f, 20f, 30f),
            yCoords = floatArrayOf(15f, 25f, 35f),
            pressures = floatArrayOf(1f, 1f, 1f),
            times = longArrayOf(100L, 200L, 300L),
            isLocked = true
        )
        val cloned = CanvasElementCloner.deepClone(original, Offset(10f, 20f)) as CanvasElement.Sketch
        assertNotEquals(original.id, cloned.id)
        assertFalse(cloned.isLocked)
        assertNull(cloned.cachedStroke)
        assertEquals(20f, cloned.xCoords[0], 0.001f)
        assertEquals(30f, cloned.xCoords[1], 0.001f)
        assertEquals(40f, cloned.xCoords[2], 0.001f)
        assertEquals(35f, cloned.yCoords[0], 0.001f)
        assertEquals(45f, cloned.yCoords[1], 0.001f)
        assertEquals(55f, cloned.yCoords[2], 0.001f)
    }

    @Test
    fun testFindHitConnector() {
        val note1 = CanvasElement.StickyNote(id = "n1", position = Offset(0f, 0f), width = 100f, height = 100f)
        val note2 = CanvasElement.StickyNote(id = "n2", position = Offset(300f, 0f), width = 100f, height = 100f)
        val connector = CanvasElement.Connector(id = "c1", startId = "n1", endId = "n2")

        val elements = listOf(note1, note2, connector)

        // Center between n1 (right edge ~100, 50) and n2 (left edge ~300, 50) is (200, 50)
        val hit = findHitConnector(worldPoint = Offset(200f, 50f), elements = elements, thresholdPx = 20f)
        assertNotNull(hit)
        assertEquals("c1", hit?.id)

        // Far away tap
        val miss = findHitConnector(worldPoint = Offset(200f, 500f), elements = elements, thresholdPx = 20f)
        assertNull(miss)
    }
}
