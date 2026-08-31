package com.flash.draft.ui.home

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import com.flash.draft.ui.theme.BentoLightBackground
import com.flash.draft.ui.theme.BentoDarkBackground

// ─────────────────────────────────────────────────────────────────────────────
// Bento Box Dashboard
// ─────────────────────────────────────────────────────────────────────────────

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

    // ── Color tokens ────────────────────────────────────────────────────────
    val bgColor = if (isDark) BentoDarkBackground else BentoLightBackground
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)
    val menuBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val context = LocalContext.current
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToExport by remember { mutableStateOf<Project?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("") }

    // ── Responsive breakpoints ──────────────────────────────────────────────
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600
    val isLargeTablet = screenWidth >= 840
    val columns = when {
        isLargeTablet -> 4
        isTablet -> 3
        else -> 2
    }
    val gutterSize = if (isTablet) 16.dp else 12.dp
    val edgePadding = if (isTablet) 20.dp else 16.dp
    val topButtonSize = if (isTablet) 48.dp else 44.dp
    val topSpacerHeight = if (isTablet) 36.dp else 24.dp

    // ── Entrance Animation State ────────────────────────────────────────────
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val gridAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "grid_alpha"
    )
    val gridOffsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 40.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "grid_offset"
    )
    val dockOffsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 80.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "dock_offset"
    )

    // ── Import launcher ─────────────────────────────────────────────────────
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

    // ── Root container ──────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = edgePadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Spacer(modifier = Modifier.height(topSpacerHeight))

            // ── 1. Header Region ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title + Subtitle
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "All Canvas",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (projects.isEmpty()) "No projects yet"
                        else "${projects.size} project${if (projects.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Top-right controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // About "?" button
                    FilledTonalIconButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier.size(topButtonSize),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textColor.copy(alpha = 0.05f),
                            contentColor = textColor
                        )
                    ) {
                        Text(
                            "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isTablet) 20.sp else 18.sp
                        )
                    }

                    // Settings button
                    FilledTonalIconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(topButtonSize),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textColor.copy(alpha = 0.05f),
                            contentColor = textColor
                        )
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 2. Project Grid / Empty State ───────────────────────────────
            if (projects.isEmpty()) {
                // Empty state — warm Bento tile
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = textColor.copy(alpha = 0.05f),
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📐", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your canvas awaits",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first project\nto get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // ── Bento Grid ──────────────────────────────────────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = gridAlpha
                            this.translationY = gridOffsetY.toPx()
                        },
                    horizontalArrangement = Arrangement.spacedBy(gutterSize),
                    verticalArrangement = Arrangement.spacedBy(gutterSize),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    items(
                        items = projects,
                        key = { it.id },
                        span = { project ->
                            // First project = hero card, spans full width
                            if (projects.indexOf(project) == 0) {
                                GridItemSpan(maxLineSpan)
                            } else {
                                GridItemSpan(1)
                            }
                        }
                    ) { project ->
                        val isHero = projects.indexOf(project) == 0
                        val cardAspectRatio = when {
                            isHero -> 16f / 10f  // Hero: landscape
                            isTablet -> 1f       // Tablet standard: square
                            else -> 3f / 4f      // Phone standard: portrait
                        }

                        ProjectCard(
                            project = project,
                            isDark = isDark,
                            thumbnailAspectRatio = cardAspectRatio,
                            onClick = { onProjectClick(project.id) },
                            onDelete = {
                                repository.deleteProject(project.id)
                                projects = repository.getAllProjects()
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
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // ── Create Project Dialog ───────────────────────────────────────────
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

        // ── Rename Dialog ───────────────────────────────────────────────────
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

        // ── 3. Bottom Gradient Scrim ────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            bgColor.copy(alpha = 0.7f),
                            bgColor
                        )
                    )
                )
        )

        // ── 4. Floating Action Pill ─────────────────────────────────────────
        val pillContainerColor = if (isDark)
            MaterialTheme.colorScheme.surfaceContainerHigh
        else
            MaterialTheme.colorScheme.surface

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = dockOffsetY)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            shape = RoundedCornerShape(50),
            color = pillContainerColor,
            shadowElevation = if (isDark) 0.dp else 8.dp,
            tonalElevation = if (isDark) 4.dp else 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import button (text button style)
                TextButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                ) {
                    Icon(
                        Icons.Filled.FileDownload,
                        contentDescription = "Import",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Import",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // New Project button (filled tonal style)
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor = bgColor
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Icon(
                        painterResource(id = LucideR.drawable.lucide_ic_plus),
                        contentDescription = "New Project",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "New Project",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // ── Context Menu Overlay (UNCHANGED) ────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Bento Project Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectCard(
    project: Project,
    isDark: Boolean,
    thumbnailAspectRatio: Float = 1f,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onLongPress: (Offset, IntSize) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark)
        MaterialTheme.colorScheme.surfaceContainer
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    val formatter = SimpleDateFormat("MMM d, yyyy", LocalLocale.current.platformLocale)
    val dateString = formatter.format(Date(project.lastModifiedMs))

    val context = LocalContext.current
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    // ── Press animation ─────────────────────────────────────────────────
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "card_press_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                currentPosition = layoutCoordinates.positionInRoot()
                currentSize = layoutCoordinates.size
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isDark) 0.dp else 12.dp,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg, RoundedCornerShape(24.dp))
            .pointerInput(project.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress(currentPosition, currentSize) }
                )
            }
    ) {
        // ── Thumbnail Area ──────────────────────────────────────────────
        val thumbDir = java.io.File(context.filesDir, "projects")
        val thumbFile = java.io.File(thumbDir, "${project.id}_thumb.png")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(thumbnailAspectRatio)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbFile.exists()) {
                val imageRequest = coil.request.ImageRequest.Builder(context)
                    .data(thumbFile)
                    .memoryCacheKey("${project.id}_${thumbFile.lastModified()}")
                    .diskCacheKey("${project.id}_${thumbFile.lastModified()}")
                    .crossfade(true)
                    .crossfade(300)
                    .build()

                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(model = imageRequest),
                    contentDescription = "Project Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Tonal placeholder
                val placeholderBg = if (isDark)
                    MaterialTheme.colorScheme.surfaceContainerHighest
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${project.previewElements.size} items",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ── Card Footer ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                IconButton(
                    onClick = { onLongPress(currentPosition, currentSize) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Project Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Context Menu (UNCHANGED — preserved exactly from original)
// ─────────────────────────────────────────────────────────────────────────────

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
    
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.4f else 0f,
        animationSpec = tween(300),
        label = "scrim_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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

        // Side Pills
        val pillSpacing = 12.dp
        val pillContainerWidth = 180.dp // widened slightly to fit Duplicate text
        
        // Ensure pills do not go off-screen on the right side
        val pillsXOffset = when {
            xDp + widthDp + pillContainerWidth + 32.dp <= screenWidth -> xDp + widthDp + 16.dp
            xDp - pillContainerWidth - 16.dp >= 0.dp -> xDp - pillContainerWidth - 16.dp
            else -> screenWidth - pillContainerWidth - 32.dp
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
        enter = fadeIn(tween(200)) + scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0f),
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