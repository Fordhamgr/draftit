package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.getAdaptiveColor

@Composable
fun TaskListItem(
    taskNode: CanvasElement.TaskListNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.TaskListNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    CanvasItemWrapper(
        position = taskNode.position, width = taskNode.width, height = taskNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = taskNode.isLocked, onLockedChange = { onUpdate(taskNode.copy(isLocked = it)) },
        isFullyDraggable = true, // Critical so we can scroll the list and tap checkboxes
        hasDefaultBackground = true,
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, hyperlinkTarget = taskNode.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(taskNode.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        customMenuContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // TASK COLOR CUSTOMIZATION
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                    val colors = listOf(Color.White, Color(0xFFFFF9C4), Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFCE4EC))
                    colors.forEach { colorOption ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(colorOption, CircleShape)
                                .border(1.dp, Color.LightGray, CircleShape)
                                .clickable {
                                    onUpdate(taskNode.copy(color = colorOption))
                                    onSaveRequest()
                                }
                        )
                    }
                }
            }
        }
    ) { isLocked ->
        Column(modifier = Modifier.fillMaxSize().background(taskNode.color.copy(alpha = 0.65f)).padding(12.dp)) {
            // Title
            BasicTextField(
                value = taskNode.title,
                onValueChange = { onUpdate(taskNode.copy(title = it)) },
                enabled = isSelected,
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // Progress Bar
            val completed = taskNode.tasks.count { it.isCompleted }
            val progress = if (taskNode.tasks.isNotEmpty()) completed.toFloat() / taskNode.tasks.size else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4CAF50), trackColor = Color(0xFFE0E0E0)
            )
            Text("$completed/${taskNode.tasks.size} Completed", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

            // Tasks List
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(taskNode.tasks) { index, task ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { isChecked ->
                                val updatedTasks = taskNode.tasks.toMutableList().apply { this[index] = task.copy(isCompleted = isChecked) }
                                onUpdate(taskNode.copy(tasks = updatedTasks))
                                onSaveRequest()
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        BasicTextField(
                            value = task.text,
                            onValueChange = { newText ->
                                val updatedTasks = taskNode.tasks.toMutableList().apply { this[index] = task.copy(text = newText) }
                                onUpdate(taskNode.copy(tasks = updatedTasks))
                            },
                            enabled = isSelected,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = if (task.isCompleted) Color.Gray else getAdaptiveColor(Color.Black,
                                    LocalDarkTheme.current
                                ),
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Close, "Delete Task", tint = Color.LightGray,
                                modifier = Modifier.size(16.dp).clickable {
                                    val updatedTasks = taskNode.tasks.toMutableList().apply { removeAt(index) }
                                    onUpdate(taskNode.copy(tasks = updatedTasks))
                                    onSaveRequest()
                                }
                            )
                        }
                    }
                }
            }

            // Add Button
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        val updatedTasks = taskNode.tasks + CanvasElement.TaskItem(text = "")
                        onUpdate(taskNode.copy(tasks = updatedTasks))
                        onSaveRequest()
                    }.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, "Add", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Text("Add task", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}