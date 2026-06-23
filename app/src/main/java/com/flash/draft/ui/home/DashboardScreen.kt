package com.flash.draft.ui.home

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.data.NoteRepository
import com.flash.draft.model.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.blur
import kotlinx.coroutines.delay
import com.flash.draft.ui.canvas.LiquidGlassPill
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    repository: NoteRepository,
    isDarkTheme: Boolean,
    onProjectClick: (String) -> Unit,
    onCreateNewProject: (String) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var projects by remember { mutableStateOf(repository.getAllProjects()) }

    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                projects = repository.getAllProjects()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isDark = isDarkTheme

    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF4F5F9)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)
    val menuBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val context = LocalContext.current
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToExport by remember { mutableStateOf<Project?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("") }
    
    // Entrance Animation State
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val gridAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )
    val gridOffsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else 40.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )
    val dockOffsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
    )

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val newProjectId = repository.importProjectArchive(context, inputStream)
                    if (newProjectId != null) {
                        projects = repository.getAllProjects()
                        Toast.makeText(context, "Project imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid project file format.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var contextMenuProject by remember { mutableStateOf<Project?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Use */* to prevent Android from forcing a .zip extension when saving
    val exportArchiveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && projectToExport != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    repository.exportProject(context, projectToExport!!.id, stream)
                }
                Toast.makeText(context, "Project exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error exporting project.", Toast.LENGTH_SHORT).show()
            }
            projectToExport = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.9f)
                .align(Alignment.TopCenter)
        ) {
            Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(10f)) // dynamic spacer roughly 10% of width

            // Premium Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Canvas",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateContentSize(animationSpec = androidx.compose.animation.core.spring())
                ) {
                    val topPillSize = if (LocalConfiguration.current.screenWidthDp >= 600) 52.dp else 40.dp
                    
                    IconButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier
                            .height(topPillSize)
                            .aspectRatio(1f)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.8f), RoundedCornerShape(50))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                    ) {
                        val fontSize = if (LocalConfiguration.current.screenWidthDp >= 600) 24.sp else 20.sp
                        Text("?", fontWeight = FontWeight.Bold, fontSize = fontSize, color = textColor)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .height(topPillSize)
                            .aspectRatio(1f)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.8f), RoundedCornerShape(50))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = textColor, modifier = Modifier.fillMaxSize(0.6f))
                    }
                }
            }
            Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(20f))

            val screenWidth = LocalConfiguration.current.screenWidthDp
            val dynamicColumns = if (screenWidth >= 600) 4 else 2

            if (projects.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No boards yet. Create one to get started.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(dynamicColumns),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer { 
                            this.alpha = gridAlpha 
                            this.translationY = gridOffsetY.toPx() 
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 200.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            isDark = isDark,
                            onClick = { onProjectClick(project.id) },
                            onDelete = {
                                repository.deleteProject(project.id)
                                projects = repository.getAllProjects() // Refresh list
                            },
                            onDuplicate = {
                                repository.duplicateProject(project.id)
                                projects = repository.getAllProjects()
                            },
                            onExport = {
                                projectToExport = project
                                exportArchiveLauncher.launch("${project.title}.draftit")
                            },
                            onRename = {
                                projectToRename = project
                            },
                            onLongPress = { offset, size ->
                                contextMenuOffset = offset
                                contextMenuSize = size
                                contextMenuProject = project
                            }
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            var projectName by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showCreateDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = menuBg.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color.White.copy(alpha = if (isDark) 0.1f else 0.5f), RoundedCornerShape(20.dp)),
                    shadowElevation = 32.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            placeholder = { Text("Project Name", color = textColor.copy(alpha = 0.4f), fontWeight = FontWeight.Medium) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = textColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = textColor,
                                focusedContainerColor = textColor.copy(alpha = 0.03f),
                                unfocusedContainerColor = textColor.copy(alpha = 0.03f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TextButton(
                                onClick = { showCreateDialog = false },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = textColor.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = {
                                    val finalName = if (projectName.isNotBlank()) projectName else "Untitled Canvas"
                                    showCreateDialog = false
                                    onCreateNewProject(finalName)
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text("Create", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        if (projectToRename != null) {
            var newName by remember { mutableStateOf(projectToRename!!.title) }
            Dialog(onDismissRequest = { projectToRename = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = menuBg.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color.White.copy(alpha = if (isDark) 0.1f else 0.5f), RoundedCornerShape(20.dp)),
                    shadowElevation = 32.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("New Name", color = textColor.copy(alpha = 0.4f), fontWeight = FontWeight.Medium) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = textColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = textColor,
                                focusedContainerColor = textColor.copy(alpha = 0.03f),
                                unfocusedContainerColor = textColor.copy(alpha = 0.03f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TextButton(
                                onClick = { projectToRename = null },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = textColor.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        repository.renameProject(projectToRename!!.id, newName)
                                        projects = repository.getAllProjects()
                                    }
                                    projectToRename = null
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Fade Gradient (Darkened)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            bgColor.copy(alpha = 0.6f),
                            bgColor.copy(alpha = 0.95f),
                            bgColor
                        )
                    )
                )
        )

        // Floating Actions at the bottom (Pill)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = dockOffsetY)
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(bottom = 24.dp)
                .shadow(16.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.1f))
                .background(if (isDark) Color(0xFF1E1E1E) else Color.White, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Import", tint = textColor, modifier = Modifier.fillMaxWidth(0.15f).aspectRatio(1f))
                Spacer(modifier = Modifier.fillMaxWidth(0.1f))
                Text("Import", fontWeight = FontWeight.Medium, color = textColor, fontSize = 16.sp)
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = textColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_plus), contentDescription = "New Project", modifier = Modifier.fillMaxWidth(0.15f).aspectRatio(1f))
                Spacer(modifier = Modifier.fillMaxWidth(0.1f))
                Text("New Project", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Context Menu Overlay
        if (contextMenuProject != null) {
            ProjectContextMenuOverlay(
                project = contextMenuProject!!,
                isDark = isDark,
                cardOffset = contextMenuOffset,
                cardSize = contextMenuSize,
                onDismiss = { contextMenuProject = null },
                onRename = { 
                    contextMenuProject = null
                    projectToRename = it
                },
                onDuplicate = { 
                    repository.duplicateProject(it.id)
                    projects = repository.getAllProjects()
                    contextMenuProject = null
                },
                onDelete = { 
                    repository.deleteProject(it.id)
                    projects = repository.getAllProjects()
                    contextMenuProject = null
                }
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onLongPress: (Offset, IntSize) -> Unit = { _, _ -> }
) {
    val cardBg = Color.White.copy(alpha = if (isDark) 0.1f else 1.0f)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)
    val dateColor = if (isDark) Color.Gray else Color(0xFF86868B)
    val menuBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)

    val formatter = SimpleDateFormat("MMM d, yyyy", LocalLocale.current.platformLocale)
    val dateString = formatter.format(Date(project.lastModifiedMs))
    
    val context = LocalContext.current
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                currentPosition = layoutCoordinates.positionInRoot()
                currentSize = layoutCoordinates.size
            }
            .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.15f))
            .background(cardBg, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .pointerInput(project.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress(currentPosition, currentSize) }
                )
            }
    ) {
        // Thumbnail Area
        val thumbDir = java.io.File(context.filesDir, "projects")
        val thumbFile = remember(project.id) {
            thumbDir.listFiles { _, name -> name.startsWith("${project.id}_scene_") && name.endsWith("_thumb.png") }?.firstOrNull()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f), // Perfectly square thumbnail area
            contentAlignment = Alignment.Center
        ) {
            if (thumbFile != null && thumbFile.exists()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(thumbFile),
                    contentDescription = "Project Thumbnail",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback: Mock preview with item count
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE5E5EA).copy(alpha = if (isDark) 0.1f else 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    val dateColor = if (isDark) Color.Gray else Color(0xFF86868B)
                    Text("${project.previewElements.size} items", color = dateColor, fontSize = 12.sp)
                }
            }
        }

        // Card Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = project.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    fontSize = 14.sp,
                    color = dateColor,
                    maxLines = 1
                )
                
                IconButton(
                    onClick = { onLongPress(currentPosition, currentSize) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Project Options",
                        tint = dateColor
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectContextMenuOverlay(
    project: Project,
    isDark: Boolean,
    cardOffset: Offset,
    cardSize: IntSize,
    onDismiss: () -> Unit,
    onRename: (Project) -> Unit,
    onDuplicate: (Project) -> Unit,
    onDelete: (Project) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val xDp = with(density) { cardOffset.x.toDp() }
    val yDp = with(density) { cardOffset.y.toDp() }
    val widthDp = with(density) { cardSize.width.toDp() }
    
    val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 0.6f else 0f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isVisible) {
                        isVisible = false
                        coroutineScope.launch {
                            delay(300)
                            onDismiss()
                        }
                    }
                }
            )
    ) {
        // Floating Card Replica
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(200)) + scaleIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.95f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            modifier = Modifier
                .offset(x = xDp, y = yDp)
                .width(widthDp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(32.dp, RoundedCornerShape(32.dp), spotColor = Color.White.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(32.dp))
            ) {
                // We use a non-interactive clone of the card
                ProjectCard(
                    project = project,
                    isDark = isDark,
                    onClick = {},
                    onDelete = {},
                    onDuplicate = {},
                    onExport = {},
                    onRename = {},
                    onLongPress = { _, _ -> }
                )
            }
        }

        // Side Pills
        val pillSpacing = 12.dp
        val pillContainerWidth = 180.dp // widened slightly to fit Duplicate text
        
        // Ensure pills do not go off-screen on the right side
        val pillsXOffset = if (xDp + widthDp + pillContainerWidth + 32.dp > screenWidth) {
            xDp - pillContainerWidth - 16.dp
        } else {
            xDp + widthDp + 16.dp
        }
        
        Column(
            modifier = Modifier
                .offset(x = pillsXOffset, y = yDp + 32.dp)
                .width(pillContainerWidth),
            verticalArrangement = Arrangement.spacedBy(pillSpacing)
        ) {
            ContextMenuItem(
                text = "Rename",
                icon = androidx.compose.material.icons.Icons.Filled.Edit,
                color = Color.Black,
                delayMs = 50,
                isVisible = isVisible,
                onClick = { 
                    isVisible = false
                    coroutineScope.launch { delay(300); onRename(project) }
                }
            )
            ContextMenuItem(
                text = "Duplicate",
                icon = androidx.compose.material.icons.Icons.Filled.FileCopy,
                color = Color.Black,
                delayMs = 100,
                isVisible = isVisible,
                onClick = { 
                    isVisible = false
                    coroutineScope.launch { delay(300); onDuplicate(project) }
                }
            )
            ContextMenuItem(
                text = "Delete",
                icon = androidx.compose.material.icons.Icons.Filled.Delete,
                color = Color.Black,
                delayMs = 150,
                isVisible = isVisible,
                onClick = { 
                    isVisible = false
                    coroutineScope.launch { delay(300); onDelete(project) }
                }
            )
        }
    }
}

@Composable
fun ContextMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color,
    delayMs: Int,
    isVisible: Boolean,
    onClick: () -> Unit
) {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(delayMs.toLong())
            show = true
        } else {
            show = false
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(tween(200)) + scaleIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0f),
        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0f)
    ) {
        LiquidGlassPill(
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = text, modifier = Modifier.size(20.dp), tint = color)
                }
                Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = color, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}