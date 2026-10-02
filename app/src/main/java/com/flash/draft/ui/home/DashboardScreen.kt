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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.CompositionLocalProvider
import io.github.fletchmckee.liquid.rememberLiquidState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.flash.draft.data.NoteRepository
import com.flash.draft.model.Project
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flash.draft.ui.canvas.LiquidGlassPill
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign

// ─────────────────────────────────────────────────────────────────────────────
// Relative Date Formatter
// ─────────────────────────────────────────────────────────────────────────────

private fun formatRelativeDate(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestampMs
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        diffMinutes < 1 -> "JUST NOW"
        diffMinutes < 60 -> "${diffMinutes} MIN AGO"
        diffHours < 24 -> "${diffHours} HRS AGO"
        diffDays < 2 -> "YESTERDAY"
        else -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            val nowCal = Calendar.getInstance()
            val sdf = if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
                SimpleDateFormat("MMM dd", Locale.getDefault())
            } else {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            }
            sdf.format(Date(timestampMs)).uppercase(Locale.getDefault())
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard Screen
// ─────────────────────────────────────────────────────────────────────────────

enum class FabState { COLLAPSED, EXPANDED, CREATE }

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
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.background
    val textColor = colorScheme.onBackground
    val secondaryTextColor = colorScheme.onSurfaceVariant
    val menuBg = colorScheme.surface
    val searchBarBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val accentBlue = Color(0xFF3478F6)
    val accentRed = colorScheme.primary

    val context = LocalContext.current
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToExport by remember { mutableStateOf<Project?>(null) }
    var fabState by remember { mutableStateOf(FabState.COLLAPSED) }

    val liquidState = rememberLiquidState()
    var searchQuery by remember { mutableStateOf("") }
    var showAllProjects by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }

    // ── Responsive breakpoints ──────────────────────────────────────────────
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600
    val isLargeTablet = screenWidth >= 840
    val columns = when {
        isLargeTablet -> 4
        isTablet -> 3
        else -> 2
    }
    val gutterSize = when {
        isLargeTablet -> 20.dp
        isTablet -> 16.dp
        else -> 12.dp
    }
    val edgePadding = when {
        isLargeTablet -> 28.dp
        isTablet -> 24.dp
        else -> 16.dp
    }

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
    val backgroundBlur by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (fabState != FabState.COLLAPSED || contextMenuProject != null) 12.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "bg_blur"
    )
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSize by remember { mutableStateOf(IntSize.Zero) }

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

    // ── Filter projects ─────────────────────────────────────────────────────
    val filteredProjects = remember(projects, searchQuery, showAllProjects) {
        val queried = if (searchQuery.isBlank()) projects
        else projects.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (showAllProjects || searchQuery.isNotBlank()) queried else queried.take(6)
    }
    val totalProjectCount = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects.size
        else projects.count { it.title.contains(searchQuery, ignoreCase = true) }
    }

    // ── Root container ──────────────────────────────────────────────────────
    CompositionLocalProvider(
        com.flash.draft.ui.canvas.LocalLiquidState provides liquidState,
        com.flash.draft.ui.canvas.LocalLiquidRefraction provides 0.5f,
        com.flash.draft.ui.canvas.LocalLiquidCurve provides 0.5f
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                .then(if (backgroundBlur > 0.dp) Modifier.blur(backgroundBlur) else Modifier)
        ) {
            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 12.dp))

            // ── 1. Top Bar: Avatar + "Projects" + Settings Gear ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgePadding)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile avatar placeholder
                Surface(
                    modifier = Modifier
                        .size(if (isTablet) 56.dp else 40.dp)
                        .clickable { onNavigateToAbout() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "D",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = if (isTablet) 24.sp else 18.sp
                        )
                    }
                }

                // Centered title
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = if (isTablet) 32.sp else 24.sp
                )

                // Settings Action
                Surface(
                    modifier = Modifier
                        .size(if (isTablet) 56.dp else 40.dp)
                        .clickable { onNavigateToSettings() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isTablet) 28.dp else 22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))

            // ── 2. Search Bar ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgePadding)
                    .height(if (isTablet) 48.dp else 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(searchBarBg),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(if (isTablet) 22.dp else 18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = if (isTablet) 16.sp else 14.sp
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search projects...",
                                        color = secondaryTextColor,
                                        fontSize = if (isTablet) 16.sp else 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 28.dp))

            // ── 3. Section Header: "Recent Files" + "View all" ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgePadding)
                    .padding(bottom = if (isTablet) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Search Results" else "Recent Files",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = if (isTablet) 22.sp else 20.sp
                )

                if (searchQuery.isBlank() && projects.size > 6) {
                    Text(
                        text = if (showAllProjects) "Show less" else "View all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentBlue,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        modifier = Modifier.clickable { showAllProjects = !showAllProjects }
                    )
                }
            }

            // ── 4. Project Grid / Empty State ───────────────────────────
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = edgePadding),
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
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredProjects.isEmpty() && searchQuery.isNotBlank()) {
                // No search results
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = edgePadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔍", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No projects found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                    }
                }
            } else {
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
                    contentPadding = PaddingValues(start = edgePadding, end = edgePadding, bottom = 140.dp)
                ) {
                    items(
                        items = filteredProjects,
                        key = { it.id }
                    ) { project ->
                        ProjectCard(
                            project = project,
                            isDark = isDark,
                            isTablet = isTablet,
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

        // ── Bottom Gradient Scrim ───────────────────────────────────────────
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

        // Scrim for Expanded/Create mode
        val scrimAlpha by animateFloatAsState(targetValue = if (fabState != FabState.COLLAPSED) 0.4f else 0f)
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { fabState = FabState.COLLAPSED }
                    )
            )
        }

        // ── Morphing FAB ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .offset(y = dockOffsetY)
        ) {
            val width by animateDpAsState(
                targetValue = when (fabState) {
                    FabState.COLLAPSED -> if (isTablet) 72.dp else 56.dp
                    FabState.EXPANDED -> if (isTablet) 240.dp else 180.dp
                    FabState.CREATE -> if (isTablet) 400.dp else 320.dp
                },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            )
            val height by animateDpAsState(
                targetValue = when (fabState) {
                    FabState.COLLAPSED -> if (isTablet) 72.dp else 56.dp
                    FabState.EXPANDED -> if (isTablet) 140.dp else 112.dp
                    FabState.CREATE -> if (isTablet) 220.dp else 180.dp
                },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            )
            val cornerRadius by animateDpAsState(
                targetValue = when (fabState) {
                    FabState.COLLAPSED -> if (isTablet) 36.dp else 28.dp
                    FabState.EXPANDED -> if (isTablet) 20.dp else 16.dp
                    FabState.CREATE -> if (isTablet) 24.dp else 20.dp
                },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            )
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = accentRed,
                shadowElevation = if (fabState == FabState.COLLAPSED) 6.dp else 16.dp,
                modifier = Modifier.size(width, height)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val collapsedAlpha by animateFloatAsState(targetValue = if (fabState == FabState.COLLAPSED) 1f else 0f, animationSpec = tween(200))
                    val expandedAlpha by animateFloatAsState(targetValue = if (fabState == FabState.EXPANDED) 1f else 0f, animationSpec = tween(200))
                    val createAlpha by animateFloatAsState(targetValue = if (fabState == FabState.CREATE) 1f else 0f, animationSpec = tween(200))

                    if (collapsedAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 72.dp else 56.dp)
                                .graphicsLayer { alpha = collapsedAlpha }
                                .clickable { fabState = FabState.EXPANDED },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(id = LucideR.drawable.lucide_ic_plus),
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(if (isTablet) 32.dp else 24.dp)
                            )
                        }
                    }

                    if (expandedAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .requiredSize(if (isTablet) 240.dp else 180.dp, if (isTablet) 140.dp else 112.dp)
                                .graphicsLayer { alpha = expandedAlpha }
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().weight(1f).clickable { fabState = FabState.CREATE }.padding(horizontal = if (isTablet) 24.dp else 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painterResource(id = LucideR.drawable.lucide_ic_plus), contentDescription = null, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                                    Text("New Project", fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, fontSize = if (isTablet) 18.sp else TextUnit.Unspecified)
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth().weight(1f).clickable { 
                                        fabState = FabState.COLLAPSED
                                        importLauncher.launch(arrayOf("application/json", "*/*")) 
                                    }.padding(horizontal = if (isTablet) 24.dp else 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                                    Text("Import", fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, fontSize = if (isTablet) 18.sp else TextUnit.Unspecified)
                                }
                            }
                        }
                    }

                    if (createAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .requiredSize(if (isTablet) 400.dp else 320.dp, if (isTablet) 220.dp else 180.dp)
                                .graphicsLayer { alpha = createAlpha }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(if (isTablet) 28.dp else 20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedTextField(
                                    value = newProjectName,
                                    onValueChange = { newProjectName = it },
                                    placeholder = { Text("Project Name", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium, fontSize = if (isTablet) 18.sp else TextUnit.Unspecified) },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = if (isTablet) 18.sp else TextUnit.Unspecified),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    TextButton(
                                        onClick = { 
                                            fabState = FabState.COLLAPSED 
                                            newProjectName = ""
                                        },
                                        modifier = Modifier.weight(1f).height(if (isTablet) 56.dp else 44.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Cancel", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = if (isTablet) 18.sp else TextUnit.Unspecified)
                                    }
                                    Button(
                                        onClick = {
                                            val finalName = if (newProjectName.isNotBlank()) newProjectName else "Untitled Canvas"
                                            fabState = FabState.COLLAPSED
                                            newProjectName = ""
                                            onCreateNewProject(finalName)
                                        },
                                        modifier = Modifier.weight(1f).height(if (isTablet) 56.dp else 44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = accentRed),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = ButtonDefaults.buttonElevation(0.dp)
                                    ) {
                                        Text("Create", fontWeight = FontWeight.Bold, fontSize = if (isTablet) 18.sp else TextUnit.Unspecified)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Context Menu Overlay ────────────────────────────────────────────
        if (contextMenuProject != null) {
            ProjectContextMenuOverlay(
                project = contextMenuProject!!,
                isDark = isDark,
                isTablet = isTablet,
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Card (Redesigned to match reference)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectCard(
    project: Project,
    isDark: Boolean,
    isTablet: Boolean = false,
    isOverlay: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onLongPress: (Offset, IntSize) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainer

    val dateString = formatRelativeDate(project.lastModifiedMs)

    val context = LocalContext.current
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(if (isOverlay) 0.97f else 1f) }
    LaunchedEffect(isPressed, isOverlay) {
        val target = if (isPressed && !isOverlay) 0.97f else 1f
        scaleAnim.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 400f))
    }
    val scale = scaleAnim.value

    val cardShape = RoundedCornerShape(16.dp)

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
            .steadyShadow(
                color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.15f),
                borderRadius = 16.dp,
                blurRadius = 12.dp,
                offsetY = 6.dp
            )
            .clip(cardShape)
            .background(cardBg, cardShape)
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

        // Consistent aspect ratio — slightly portrait like the reference
        val thumbnailAspectRatio = if (isTablet) 4f / 3f else 4f / 3.5f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(thumbnailAspectRatio)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else Color(0xFFF8F8F8))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
        val cacheKey = remember(project.id, project.lastModifiedMs) {
            "${project.id}_${thumbFile.lastModified()}"
        }
        val thumbExists = remember(project.id, project.lastModifiedMs) {
            thumbFile.exists()
        }

        if (thumbExists) {
            val imageRequest = coil.request.ImageRequest.Builder(context)
                .data(thumbFile)
                .size(coil.size.Size.ORIGINAL)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .placeholderMemoryCacheKey(coil.memory.MemoryCache.Key(cacheKey))
                .apply {
                    if (isOverlay) {
                        crossfade(false)
                    } else {
                        crossfade(300)
                    }
                }
                .build()

            androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(model = imageRequest),
                    contentDescription = "Project Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val placeholderBg = if (isDark)
                    MaterialTheme.colorScheme.surfaceContainerHighest
                else
                    Color(0xFFF2F2F7)

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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateString,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                letterSpacing = 0.3.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Context Menu (preserved from original)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectContextMenuOverlay(
    project: Project,
    isDark: Boolean,
    isTablet: Boolean = false,
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
    val heightDp = with(density) { cardSize.height.toDp() }
    
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
        // Sharp project card overlay
        if (isVisible) {
            Box(
                modifier = Modifier
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(
                            androidx.compose.ui.unit.Constraints.fixed(cardSize.width, cardSize.height)
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(
                                x = kotlin.math.round(cardOffset.x).toInt(),
                                y = kotlin.math.round(cardOffset.y).toInt()
                            )
                        }
                    }
            ) {
                ProjectCard(
                    project = project,
                    isDark = isDark,
                    isTablet = isTablet,
                    isOverlay = true,
                    onClick = { },
                    onDelete = { },
                    onDuplicate = { },
                    onExport = { },
                    onRename = { }
                )
            }
        }

        // Side Pills
        val pillSpacing = 12.dp
        val pillContainerWidth = 180.dp
        
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
                icon = Icons.Filled.Edit,
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
                icon = Icons.Filled.FileCopy,
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
                icon = Icons.Filled.Delete,
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
                Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

fun Modifier.steadyShadow(
    color: Color = Color.Black.copy(alpha = 0.15f),
    borderRadius: androidx.compose.ui.unit.Dp = 16.dp,
    blurRadius: androidx.compose.ui.unit.Dp = 8.dp,
    offsetY: androidx.compose.ui.unit.Dp = 4.dp,
    offsetX: androidx.compose.ui.unit.Dp = 0.dp
) = this.drawBehind {
    val paint = androidx.compose.ui.graphics.Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    frameworkPaint.color = android.graphics.Color.TRANSPARENT
    frameworkPaint.setShadowLayer(
        blurRadius.toPx(),
        offsetX.toPx(),
        offsetY.toPx(),
        color.toArgb()
    )
    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}