package com.flash.draft.ui.canvas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.Scene
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun ScenesMenu(
    projectId: String,
    scenes: List<Scene>,
    onClose: () -> Unit,
    onCaptureScene: () -> Unit,
    onNavigateToScene: (Scene) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteScene: (String) -> Unit
) {
    val isDarkTheme = LocalDarkTheme.current
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val dividerColor = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x1A000000)
    var showTutorial by remember { mutableStateOf(false) }
    val subtleBg = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0x33000000)
    val subtleBorder = if (isDarkTheme) Color(0x22FFFFFF) else Color(0x1A000000)

    Column(
        modifier = Modifier
            .width(260.dp)
            .heightIn(max = 420.dp)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scenes", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                // Morphing Tutorial Pill
                androidx.compose.animation.AnimatedContent(
                    targetState = showTutorial,
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) using androidx.compose.animation.SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.TopStart
                ) { isShowing ->
                    if (isShowing) {
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .background(subtleBg, RoundedCornerShape(12.dp))
                                .border(1.dp, subtleBorder, RoundedCornerShape(12.dp))
                                .clickable { showTutorial = false }
                                .padding(12.dp)
                        ) {
                            Text(
                                "Scenes let you save your current view and jump back to it later.",
                                color = textColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(subtleBg, CircleShape)
                                .border(1.dp, subtleBorder, CircleShape)
                                .clickable { showTutorial = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            IconButton(onClick = onCaptureScene, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.CameraAlt, "Capture", tint = textColor, modifier = Modifier.size(18.dp))
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
        
        // Scene List
        if (scenes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text("No scenes yet", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(scenes, key = { it.id }) { scene ->
                    SceneRow(
                        scene = scene,
                        onNavigate = { onNavigateToScene(scene) },
                        onRename = { newName -> onRenameScene(scene.id, newName) },
                        onDelete = { onDeleteScene(scene.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneRow(
    scene: Scene,
    onNavigate: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(scene.name) }
    
    val isDarkTheme = LocalDarkTheme.current
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val inputBg = if (isDarkTheme) Color(0xFF333333) else Color(0x1A000000)
    val nameBg = if (isDarkTheme) Color(0x1AFFFFFF) else Color(0x0D000000)
    val dividerColor = if (isDarkTheme) Color(0x1AFFFFFF) else Color(0x0D000000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isEditing) onNavigate() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            BasicTextField(
                value = editName,
                onValueChange = { editName = it },
                textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                cursorBrush = SolidColor(textColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (editName.isNotBlank()) onRename(editName)
                        isEditing = false
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .background(inputBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (editName.isNotBlank()) onRename(editName)
                    isEditing = false
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Close, "Done", tint = textColor, modifier = Modifier.size(16.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(nameBg, RoundedCornerShape(6.dp))
                    .clickable { isEditing = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = scene.name,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onNavigate, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.PlayArrow, "Teleport", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
}
