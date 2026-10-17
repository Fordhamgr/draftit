package com.flash.draft.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.R as LucideR
import com.flash.draft.model.BackgroundStyle
import com.flash.draft.model.Project
import com.flash.draft.ui.canvas.LiquidGlassPill
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// 4. Design Tokens
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
private data class DashTokens(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val hairline: Color,
    val placeholder: Color,
    val chipIdleBorder: Color
)

@Composable
private fun rememberDashTokens(isDark: Boolean): DashTokens = remember(isDark) {
    if (isDark) DashTokens(
        background = Color(0xFF0B0D14),
        surface = Color(0xFF161A26),
        ink = Color(0xFFF2F4F8),
        muted = Color(0xFFA3AABF),
        hairline = Color.White.copy(alpha = 0.08f),
        placeholder = Color(0xFF1D2233),
        chipIdleBorder = Color.White.copy(alpha = 0.14f)
    ) else DashTokens(
        background = Color(0xFFF4F5F8),
        surface = Color.White,
        ink = Color(0xFF0E1325),
        muted = Color(0xFF5B6275),
        hairline = Color(0xFF0E1325).copy(alpha = 0.08f),
        placeholder = Color(0xFFEDEEF1),
        chipIdleBorder = Color(0xFF0E1325).copy(alpha = 0.10f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.9 Date Formatter
// ─────────────────────────────────────────────────────────────────────────────

private fun formatRelativeDate(timestampMs: Long): String {
    val diffMs = System.currentTimeMillis() - timestampMs
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 2 -> "Yesterday"
        else -> {
            val sameYear = Calendar.getInstance().apply { timeInMillis = timestampMs }
                .get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
            SimpleDateFormat(if (sameYear) "MMM d" else "MMM d, yyyy", Locale.getDefault())
                .format(Date(timestampMs))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.3 Filter Chips & 5.4 Content Modes
// ─────────────────────────────────────────────────────────────────────────────

private enum class DashFilter(val label: String) {
    All("All"),
    Today("Today"),
    ThisWeek("This week")
}

private data class DashSections(val hero: Project?, val today: List<Project>, val earlier: List<Project>)

private fun buildSections(projects: List<Project>, cap: Int?): DashSections {
    val sorted = projects.sortedByDescending { it.lastModifiedMs }
    val capped = if (cap != null) sorted.take(cap) else sorted
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val rest = capped.drop(1)
    return DashSections(
        hero = capped.firstOrNull(),
        today = rest.filter { it.lastModifiedMs >= startOfToday },
        earlier = rest.filter { it.lastModifiedMs < startOfToday }
    )
}

enum class FabState { COLLAPSED, EXPANDED, CREATE }

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    isDarkTheme: Boolean,
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tokens = rememberDashTokens(isDarkTheme)
    val context = LocalContext.current

    // Local UI State
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf(DashFilter.All) }
    var showAllProjects by rememberSaveable { mutableStateOf(false) }

    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToExport by remember { mutableStateOf<Project?>(null) }
    var fabState by remember { mutableStateOf(FabState.COLLAPSED) }

    val liquidState = rememberLiquidState()

    // Responsive breakpoints
    val windowSizeInfo = com.flash.draft.ui.theme.LocalWindowSizeInfo.current
    val isTablet = windowSizeInfo.isTablet
    val isLargeTablet = windowSizeInfo.isLargeTablet
    val isLandscapePhone = windowSizeInfo.isLandscapePhone
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
        isLargeTablet -> 32.dp
        isTablet -> 24.dp
        else -> 16.dp
    }

    // Entrance Animation State
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

    // Activity Result Launchers
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                onEvent(DashboardEvent.ImportProject(context, inputStream))
            } else {
                Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportArchiveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && projectToExport != null) {
            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                onEvent(DashboardEvent.ExportProject(context, projectToExport!!.id, outputStream))
            } else {
                Toast.makeText(context, "Could not create file", Toast.LENGTH_SHORT).show()
            }
            projectToExport = null
        }
    }

    var contextMenuProject by remember { mutableStateOf<Project?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSize by remember { mutableStateOf(IntSize.Zero) }

    val backgroundBlur by animateDpAsState(
        targetValue = if (fabState != FabState.COLLAPSED || contextMenuProject != null) 12.dp else 0.dp,
        animationSpec = tween(300),
        label = "bg_blur"
    )

    // Data partitioning
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOf7DaysAgo = remember {
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    }

    val isSectionedMode = state.searchQuery.isBlank() && filter == DashFilter.All

    val sections = remember(state.projects, showAllProjects) {
        buildSections(state.projects, if (showAllProjects) null else 6)
    }

    val flatProjects = remember(state.projects, state.searchQuery, filter) {
        var list = state.projects
        if (state.searchQuery.isNotBlank()) {
            list = list.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
        }
        when (filter) {
            DashFilter.All -> {}
            DashFilter.Today -> list = list.filter { it.lastModifiedMs >= startOfToday }
            DashFilter.ThisWeek -> list = list.filter { it.lastModifiedMs >= startOf7DaysAgo }
        }
        list.sortedByDescending { it.lastModifiedMs }
    }

    // Root composition
    CompositionLocalProvider(
        com.flash.draft.ui.canvas.LocalLiquidState provides liquidState,
        com.flash.draft.ui.canvas.LocalLiquidRefraction provides 0.5f,
        com.flash.draft.ui.canvas.LocalLiquidCurve provides 0.5f,
        com.flash.draft.ui.canvas.LocalLiquidFrost provides 0.0f
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .widthIn(max = 1100.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                    .then(if (backgroundBlur > 0.dp) Modifier.blur(backgroundBlur) else Modifier)
            ) {
                Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 8.dp))

                // ── 5.1 Header ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = edgePadding)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        val titleFontSize = when {
                            isLandscapePhone -> 28.sp
                            isLargeTablet -> 38.sp
                            isTablet -> 34.sp
                            else -> 34.sp
                        }
                        Text(
                            text = "Boards",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = titleFontSize,
                                letterSpacing = (-0.7).sp,
                                color = tokens.ink
                            )
                        )

                        if (state.projects.isNotEmpty()) {
                            val count = state.projects.size
                            val countString = if (count == 1) "1 board" else "$count boards"
                            val latestProject = state.projects.maxByOrNull { it.lastModifiedMs }
                            val relativeDate = if (latestProject != null) formatRelativeDate(latestProject.lastModifiedMs) else ""
                            val subtitleText = if (relativeDate.isNotBlank()) "$countString · edited $relativeDate" else countString

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = tokens.muted
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Settings Button
                        Surface(
                            shape = CircleShape,
                            color = tokens.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, tokens.chipIdleBorder),
                            modifier = Modifier
                                .size(44.dp)
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable { onNavigateToSettings() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = tokens.ink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Avatar Button
                        Surface(
                            shape = CircleShape,
                            color = tokens.ink,
                            modifier = Modifier
                                .size(44.dp)
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable { onNavigateToAbout() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "D",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = tokens.background
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 16.dp))

                // ── 5.2 Search Row ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = edgePadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Field
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tokens.hairline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = tokens.muted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { onEvent(DashboardEvent.OnSearchQueryChanged(it)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                textStyle = TextStyle(
                                    color = tokens.ink,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search boards",
                                                color = tokens.muted,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (state.searchQuery.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .clickable { onEvent(DashboardEvent.OnSearchQueryChanged("")) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = tokens.muted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // View Toggle
                    val toggleShape = RoundedCornerShape(16.dp)
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .clip(toggleShape)
                            .clickable { isGridView = !isGridView },
                        shape = toggleShape,
                        color = tokens.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tokens.hairline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    id = if (isGridView) LucideR.drawable.lucide_ic_layout_grid else LucideR.drawable.lucide_ic_list
                                ),
                                contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                                tint = tokens.ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 5.3 Filter Chips ────────────────────────────────────
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = edgePadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(DashFilter.values(), key = { it.name }) { item ->
                        val isSelected = filter == item
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) tokens.ink else tokens.surface,
                            animationSpec = tween(200),
                            label = "chip_bg"
                        )
                        val chipText = if (isSelected) tokens.background else tokens.ink.copy(alpha = 0.85f)

                        val chipShape = RoundedCornerShape(18.dp)
                        Surface(
                            shape = chipShape,
                            color = chipBg,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, tokens.chipIdleBorder),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(chipShape)
                                .clickable { filter = item }
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = chipText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))

                // ── 5.4 & 5.5 Content Body (Empty / Flat / Sectioned) ────
                if (state.projects.isEmpty()) {
                    // Empty State
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
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(tokens.ink.copy(alpha = 0.06f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = LucideR.drawable.lucide_ic_layout_grid),
                                    contentDescription = null,
                                    tint = tokens.ink,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Start your first board",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.ink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sketch, plan, and connect ideas on an infinite canvas.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { fabState = FabState.CREATE },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.ink,
                                    contentColor = tokens.background
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("New board", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                } else if (!isSectionedMode && flatProjects.isEmpty()) {
                    // No Search / Filter Results
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
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(tokens.ink.copy(alpha = 0.06f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = tokens.ink,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No boards found",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.ink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try a different search or filter.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Crossfade between Grid and List view
                    Crossfade(
                        targetState = isGridView,
                        animationSpec = tween(220),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = gridAlpha
                                translationY = gridOffsetY.toPx()
                            },
                        label = "grid_list_crossfade"
                    ) { gridMode ->
                        LazyVerticalGrid(
                            columns = if (gridMode) GridCells.Fixed(columns) else GridCells.Fixed(1),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(gutterSize),
                            verticalArrangement = Arrangement.spacedBy(gutterSize),
                            contentPadding = PaddingValues(
                                start = edgePadding,
                                end = edgePadding,
                                top = 8.dp,
                                bottom = 140.dp
                            )
                        ) {
                            if (isSectionedMode) {
                                // ── SECTIONED MODE ──
                                if (sections.hero != null) {
                                    item(key = "hdr_continue", span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader(title = "Continue", tokens = tokens)
                                    }
                                    item(
                                        key = "hero_card",
                                        span = { GridItemSpan(if (isTablet && gridMode) minOf(2, columns) else maxLineSpan) }
                                    ) {
                                        HeroCard(
                                            project = sections.hero,
                                            tokens = tokens,
                                            isDark = isDarkTheme,
                                            isTablet = isTablet,
                                            onClick = { onEvent(DashboardEvent.OpenProject(sections.hero.id)) },
                                            onLongPress = { offset, size ->
                                                contextMenuOffset = offset
                                                contextMenuSize = size
                                                contextMenuProject = sections.hero
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }

                                if (sections.today.isNotEmpty()) {
                                    item(key = "hdr_today", span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader(title = "Today", tokens = tokens)
                                    }
                                    items(sections.today, key = { it.id }) { project ->
                                        if (gridMode) {
                                            ProjectCard(
                                                project = project,
                                                tokens = tokens,
                                                isDark = isDarkTheme,
                                                isTablet = isTablet,
                                                onClick = { onEvent(DashboardEvent.OpenProject(project.id)) },
                                                onDelete = { onEvent(DashboardEvent.DeleteProject(project.id)) },
                                                onDuplicate = { onEvent(DashboardEvent.DuplicateProject(project.id)) },
                                                onExport = {
                                                    projectToExport = project
                                                    exportArchiveLauncher.launch("${project.title}.draftit")
                                                },
                                                onRename = { projectToRename = project },
                                                onLongPress = { offset, size ->
                                                    contextMenuOffset = offset
                                                    contextMenuSize = size
                                                    contextMenuProject = project
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        } else {
                                            ProjectListRow(
                                                project = project,
                                                tokens = tokens,
                                                isDark = isDarkTheme,
                                                onClick = { onEvent(DashboardEvent.OpenProject(project.id)) },
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

                                if (sections.earlier.isNotEmpty()) {
                                    item(key = "hdr_earlier", span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader(
                                            title = "Earlier",
                                            tokens = tokens,
                                            action = if (state.projects.size > 6) {
                                                {
                                                    Text(
                                                        text = if (showAllProjects) "Show less" else "View all",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = tokens.ink,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .clickable { showAllProjects = !showAllProjects }
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                    items(sections.earlier, key = { it.id }) { project ->
                                        if (gridMode) {
                                            ProjectCard(
                                                project = project,
                                                tokens = tokens,
                                                isDark = isDarkTheme,
                                                isTablet = isTablet,
                                                onClick = { onEvent(DashboardEvent.OpenProject(project.id)) },
                                                onDelete = { onEvent(DashboardEvent.DeleteProject(project.id)) },
                                                onDuplicate = { onEvent(DashboardEvent.DuplicateProject(project.id)) },
                                                onExport = {
                                                    projectToExport = project
                                                    exportArchiveLauncher.launch("${project.title}.draftit")
                                                },
                                                onRename = { projectToRename = project },
                                                onLongPress = { offset, size ->
                                                    contextMenuOffset = offset
                                                    contextMenuSize = size
                                                    contextMenuProject = project
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        } else {
                                            ProjectListRow(
                                                project = project,
                                                tokens = tokens,
                                                isDark = isDarkTheme,
                                                onClick = { onEvent(DashboardEvent.OpenProject(project.id)) },
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
                            } else {
                                // ── FLAT MODE (Search / Filter) ──
                                val headerTitle = if (state.searchQuery.isNotBlank()) "Search results" else filter.label
                                item(key = "hdr_flat", span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader(title = headerTitle, tokens = tokens)
                                }
                                items(flatProjects, key = { it.id }) { project ->
                                    if (gridMode) {
                                        ProjectCard(
                                            project = project,
                                            tokens = tokens,
                                            isDark = isDarkTheme,
                                            isTablet = isTablet,
                                            onClick = { onEvent(DashboardEvent.OpenProject(project.id)) },
                                            onDelete = { onEvent(DashboardEvent.DeleteProject(project.id)) },
                                            onDuplicate = { onEvent(DashboardEvent.DuplicateProject(project.id)) },
                                            onExport = {
                                                projectToExport = project
                                                exportArchiveLauncher.launch("${project.title}.draftit")
                                            },
                                            onRename = { projectToRename = project },
                                            onLongPress = { offset, size ->
                                                contextMenuOffset = offset
                                                contextMenuSize = size
                                                contextMenuProject = project
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    } else {
                                        ProjectListRow(
                                            project = project,
                                            tokens = tokens,
                                            isDark = isDarkTheme,
                                            onClick = { onEvent(DashboardEvent.OpenProject(project.id)) },
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
                    }
                }
            }

            // ── 5.14 Rename Dialog ──────────────────────────────────
            if (projectToRename != null) {
                var newName by remember { mutableStateOf(projectToRename!!.title) }
                Dialog(onDismissRequest = { projectToRename = null }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = tokens.surface,
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, tokens.hairline, RoundedCornerShape(24.dp)),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Rename board",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = tokens.ink
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = {
                                    Text(
                                        "Board name",
                                        color = tokens.muted,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tokens.ink,
                                    unfocusedBorderColor = tokens.chipIdleBorder,
                                    focusedTextColor = tokens.ink,
                                    unfocusedTextColor = tokens.ink,
                                    cursorColor = tokens.ink,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { projectToRename = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel", color = tokens.muted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                }
                                Button(
                                    onClick = {
                                        if (newName.isNotBlank()) {
                                            onEvent(DashboardEvent.RenameProject(projectToRename!!.id, newName))
                                        }
                                        projectToRename = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.ink, contentColor = tokens.background),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── 5.12 Bottom Scrim (Non-intercepting) ────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                tokens.background.copy(alpha = 0.85f),
                                tokens.background
                            )
                        )
                    )
            )

            // Scrim for Expanded / Create mode
            val scrimAlpha by animateFloatAsState(
                targetValue = if (fabState != FabState.COLLAPSED) 0.4f else 0f,
                label = "fab_scrim_alpha"
            )
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

            // ── 5.11 Morphing FAB ───────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = if (isTablet) 24.dp else 16.dp, bottom = 28.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .offset(y = dockOffsetY)
            ) {
                val width by animateDpAsState(
                    targetValue = when (fabState) {
                        FabState.COLLAPSED -> if (isTablet) 196.dp else 164.dp
                        FabState.EXPANDED -> if (isTablet) 240.dp else 180.dp
                        FabState.CREATE -> if (isTablet) 400.dp else 320.dp
                    },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "fab_w"
                )
                val height by animateDpAsState(
                    targetValue = when (fabState) {
                        FabState.COLLAPSED -> if (isTablet) 64.dp else 56.dp
                        FabState.EXPANDED -> if (isTablet) 140.dp else 112.dp
                        FabState.CREATE -> if (isTablet) 280.dp else 240.dp
                    },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "fab_h"
                )
                val cornerRadius by animateDpAsState(
                    targetValue = when (fabState) {
                        FabState.COLLAPSED -> if (isTablet) 32.dp else 28.dp
                        FabState.EXPANDED -> if (isTablet) 20.dp else 16.dp
                        FabState.CREATE -> if (isTablet) 24.dp else 20.dp
                    },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "fab_radius"
                )

                val fabShape = RoundedCornerShape(cornerRadius)
                Surface(
                    shape = fabShape,
                    color = tokens.ink,
                    shadowElevation = if (fabState == FabState.COLLAPSED) 6.dp else 16.dp,
                    modifier = Modifier.size(width, height)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(fabShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val collapsedAlpha by animateFloatAsState(targetValue = if (fabState == FabState.COLLAPSED) 1f else 0f, animationSpec = tween(200), label = "c_alpha")
                        val expandedAlpha by animateFloatAsState(targetValue = if (fabState == FabState.EXPANDED) 1f else 0f, animationSpec = tween(200), label = "e_alpha")
                        val createAlpha by animateFloatAsState(targetValue = if (fabState == FabState.CREATE) 1f else 0f, animationSpec = tween(200), label = "cr_alpha")

                        if (collapsedAlpha > 0f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = collapsedAlpha }
                                    .semantics {
                                        contentDescription = "New board or import"
                                        role = Role.Button
                                    }
                                    .clickable { fabState = FabState.EXPANDED },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = LucideR.drawable.lucide_ic_plus),
                                    contentDescription = null,
                                    tint = tokens.background,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "New board",
                                    color = tokens.background,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clickable { fabState = FabState.CREATE }
                                            .padding(horizontal = if (isTablet) 24.dp else 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = LucideR.drawable.lucide_ic_plus),
                                            contentDescription = null,
                                            modifier = Modifier.size(if (isTablet) 24.dp else 20.dp),
                                            tint = tokens.background
                                        )
                                        Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                                        Text(
                                            text = "New board",
                                            fontWeight = FontWeight.Medium,
                                            color = tokens.background,
                                            maxLines = 1,
                                            fontSize = if (isTablet) 18.sp else TextUnit.Unspecified
                                        )
                                    }
                                    HorizontalDivider(color = tokens.background.copy(alpha = 0.2f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clickable {
                                                fabState = FabState.COLLAPSED
                                                importLauncher.launch(arrayOf("application/json", "*/*"))
                                            }
                                            .padding(horizontal = if (isTablet) 24.dp else 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FileDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(if (isTablet) 24.dp else 20.dp),
                                            tint = tokens.background
                                        )
                                        Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                                        Text(
                                            text = "Import board",
                                            fontWeight = FontWeight.Medium,
                                            color = tokens.background,
                                            maxLines = 1,
                                            fontSize = if (isTablet) 18.sp else TextUnit.Unspecified
                                        )
                                    }
                                }
                            }
                        }

                        if (createAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(if (isTablet) 400.dp else 320.dp, if (isTablet) 280.dp else 240.dp)
                                    .graphicsLayer { alpha = createAlpha }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isTablet) 28.dp else 20.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(state.templates, key = { it }) { template ->
                                            val isSelected = state.selectedTemplate == template
                                            val templateShape = RoundedCornerShape(8.dp)
                                            Surface(
                                                modifier = Modifier
                                                    .clip(templateShape)
                                                    .clickable {
                                                        onEvent(DashboardEvent.OnTemplateSelected(template))
                                                    },
                                                shape = templateShape,
                                                color = if (isSelected) tokens.background.copy(alpha = 0.2f) else Color.Transparent,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) tokens.background else tokens.background.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Text(
                                                    text = template,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    color = tokens.background,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = state.newProjectName,
                                        onValueChange = { onEvent(DashboardEvent.OnNewProjectNameChanged(it)) },
                                        placeholder = {
                                            Text(
                                                "Board name",
                                                color = tokens.background.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = if (isTablet) 18.sp else TextUnit.Unspecified
                                            )
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = tokens.background,
                                            fontSize = if (isTablet) 18.sp else TextUnit.Unspecified
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tokens.background,
                                            unfocusedBorderColor = tokens.background.copy(alpha = 0.3f),
                                            focusedTextColor = tokens.background,
                                            unfocusedTextColor = tokens.background,
                                            cursorColor = tokens.background,
                                            focusedContainerColor = tokens.background.copy(alpha = 0.1f),
                                            unfocusedContainerColor = tokens.background.copy(alpha = 0.1f)
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
                                                onEvent(DashboardEvent.OnNewProjectNameChanged(""))
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(if (isTablet) 56.dp else 44.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                "Cancel",
                                                color = tokens.background.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = if (isTablet) 18.sp else TextUnit.Unspecified
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                if (state.newProjectName.isBlank()) {
                                                    onEvent(DashboardEvent.OnNewProjectNameChanged("Untitled Canvas"))
                                                }
                                                fabState = FabState.COLLAPSED
                                                onEvent(DashboardEvent.CreateProject)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(if (isTablet) 56.dp else 44.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = tokens.background,
                                                contentColor = tokens.ink
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Text(
                                                "Create",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isTablet) 18.sp else TextUnit.Unspecified
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 5.10 Context Menu Overlay ───────────────────────────
            if (contextMenuProject != null) {
                ProjectContextMenuOverlay(
                    project = contextMenuProject!!,
                    tokens = tokens,
                    isDark = isDarkTheme,
                    isTablet = isTablet,
                    cardOffset = contextMenuOffset,
                    cardSize = contextMenuSize,
                    onDismiss = { contextMenuProject = null },
                    onRename = {
                        contextMenuProject = null
                        projectToRename = it
                    },
                    onDuplicate = {
                        onEvent(DashboardEvent.DuplicateProject(it.id))
                        contextMenuProject = null
                    },
                    onExport = {
                        contextMenuProject = null
                        projectToExport = it
                        exportArchiveLauncher.launch("${it.title}.draftit")
                    },
                    onDelete = {
                        onEvent(DashboardEvent.DeleteProject(it.id))
                        contextMenuProject = null
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.4 Section Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    tokens: DashTokens,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = tokens.ink
            )
        )
        action?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Project Thumbnail Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectThumbnail(
    project: Project,
    isDark: Boolean,
    tokens: DashTokens,
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false
) {
    val context = LocalContext.current
    val thumbDir = remember(context) { java.io.File(context.filesDir, "projects") }
    val thumbFile = remember(project.id, thumbDir) { java.io.File(thumbDir, "${project.id}_thumb.png") }

    val cacheKey = remember(project.id, project.lastModifiedMs) {
        "${project.id}_${thumbFile.lastModified()}"
    }
    val thumbExists = remember(project.id, project.lastModifiedMs) {
        thumbFile.exists()
    }

    Box(
        modifier = modifier
            .background(tokens.placeholder)
            .drawBehind {
                val style = project.backgroundStyle
                if (style == BackgroundStyle.Solid) return@drawBehind

                val patternColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
                val spacing = 12.dp.toPx()

                when (style) {
                    BackgroundStyle.GraphPaper -> {
                        var x = 0f
                        while (x < size.width) {
                            drawLine(color = patternColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                            x += spacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(color = patternColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                            y += spacing
                        }
                    }
                    BackgroundStyle.Blueprint -> {
                        drawRect(color = Color(0xFF1E3A8A))
                        val bpColor = Color.White.copy(alpha = 0.2f)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(color = bpColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                            x += spacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(color = bpColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                            y += spacing
                        }
                    }
                    BackgroundStyle.Greenprint -> {
                        drawRect(color = Color(0xFF1B5E20))
                        val gpColor = Color.White.copy(alpha = 0.2f)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(color = gpColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                            x += spacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(color = gpColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                            y += spacing
                        }
                    }
                    BackgroundStyle.BlackPaper -> {
                        drawRect(color = Color(0xFF0D0D0D))
                        val dpColor = Color.White.copy(alpha = 0.15f)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(color = dpColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                            x += spacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(color = dpColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                            y += spacing
                        }
                    }
                    else -> {}
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (thumbExists) {
            val imageRequest = coil.request.ImageRequest.Builder(context)
                .data(thumbFile)
                .size(coil.size.Size.ORIGINAL)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .placeholderMemoryCacheKey(coil.memory.MemoryCache.Key(cacheKey))
                .apply {
                    if (isOverlay) crossfade(false) else crossfade(300)
                }
                .build()

            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(model = imageRequest),
                contentDescription = "Project Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = LucideR.drawable.lucide_ic_layout_grid),
                    contentDescription = null,
                    tint = tokens.muted.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${project.previewElements.size} items",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = tokens.muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.6 Hero Card ("Continue")
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    project: Project,
    tokens: DashTokens,
    isDark: Boolean,
    isTablet: Boolean,
    onClick: () -> Unit,
    onLongPress: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isPressed) {
        val target = if (isPressed) 0.97f else 1f
        scaleAnim.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 400f))
    }

    val cardShape = RoundedCornerShape(24.dp)
    val thumbHeight = if (isTablet) 220.dp else 160.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                currentPosition = layoutCoordinates.positionInRoot()
                currentSize = layoutCoordinates.size
            }
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .steadyShadow(
                color = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.06f),
                borderRadius = 24.dp,
                blurRadius = 24.dp,
                offsetY = 6.dp
            )
            .clip(cardShape)
            .background(tokens.surface, cardShape)
            .border(1.dp, tokens.hairline, cardShape)
            .pointerInput(project.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress(currentPosition, currentSize) }
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${project.title}, edited ${formatRelativeDate(project.lastModifiedMs)}"
                role = Role.Button
            }
    ) {
        ProjectThumbnail(
            project = project,
            isDark = isDark,
            tokens = tokens,
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = tokens.ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Edited ${formatRelativeDate(project.lastModifiedMs)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = tokens.muted
                    ),
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { onLongPress(currentPosition, currentSize) },
                modifier = Modifier
                    .size(44.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options for ${project.title}",
                    tint = tokens.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.7 Grid Card (ProjectCard)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectCard(
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
    tokens: DashTokens = rememberDashTokens(isDark),
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(if (isOverlay) 0.97f else 1f) }
    LaunchedEffect(isPressed, isOverlay) {
        val target = if (isPressed && !isOverlay) 0.97f else 1f
        scaleAnim.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 400f))
    }

    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                currentPosition = layoutCoordinates.positionInRoot()
                currentSize = layoutCoordinates.size
            }
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .steadyShadow(
                color = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.06f),
                borderRadius = 20.dp,
                blurRadius = 24.dp,
                offsetY = 6.dp
            )
            .clip(cardShape)
            .background(tokens.surface, cardShape)
            .border(1.dp, tokens.hairline, cardShape)
            .pointerInput(project.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress(currentPosition, currentSize) }
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${project.title}, edited ${formatRelativeDate(project.lastModifiedMs)}"
                role = Role.Button
            }
    ) {
        // 4:3 Aspect Ratio Thumbnail
        ProjectThumbnail(
            project = project,
            isDark = isDark,
            tokens = tokens,
            isOverlay = isOverlay,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        )

        // Card Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = tokens.ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Edited ${formatRelativeDate(project.lastModifiedMs)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    color = tokens.muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.8 List Mode Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectListRow(
    project: Project,
    tokens: DashTokens,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isPressed) {
        val target = if (isPressed) 0.98f else 1f
        scaleAnim.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 400f))
    }

    val rowShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .onGloballyPositioned { layoutCoordinates ->
                currentPosition = layoutCoordinates.positionInRoot()
                currentSize = layoutCoordinates.size
            }
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .steadyShadow(
                color = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.06f),
                borderRadius = 16.dp,
                blurRadius = 16.dp,
                offsetY = 4.dp
            )
            .clip(rowShape)
            .border(1.dp, tokens.hairline, rowShape)
            .pointerInput(project.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress(currentPosition, currentSize) }
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${project.title}, edited ${formatRelativeDate(project.lastModifiedMs)}"
                role = Role.Button
            },
        color = tokens.surface,
        shape = rowShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectThumbnail(
                project = project,
                isDark = isDark,
                tokens = tokens,
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = tokens.ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Edited ${formatRelativeDate(project.lastModifiedMs)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = tokens.muted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onLongPress(currentPosition, currentSize) },
                modifier = Modifier
                    .size(44.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options for ${project.title}",
                    tint = tokens.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5.10 Context Menu Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectContextMenuOverlay(
    project: Project,
    isDark: Boolean,
    isTablet: Boolean = false,
    cardOffset: Offset,
    cardSize: IntSize,
    onDismiss: () -> Unit,
    onRename: (Project) -> Unit,
    onDuplicate: (Project) -> Unit,
    onExport: (Project) -> Unit = {},
    onDelete: (Project) -> Unit,
    tokens: DashTokens = rememberDashTokens(isDark)
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

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
                    tokens = tokens,
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
            else -> (screenWidth - pillContainerWidth - 16.dp).coerceAtLeast(16.dp)
        }
        val pillsYOffset = (yDp + 16.dp).coerceIn(16.dp, (screenHeight - 280.dp).coerceAtLeast(16.dp))

        Column(
            modifier = Modifier
                .offset(x = pillsXOffset, y = pillsYOffset)
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
                text = "Export",
                icon = Icons.Filled.FileDownload,
                color = Color.Black,
                delayMs = 150,
                isVisible = isVisible,
                onClick = {
                    isVisible = false
                    coroutineScope.launch { delay(300); onExport(project) }
                }
            )
            ContextMenuItem(
                text = "Delete",
                icon = Icons.Filled.Delete,
                color = Color.Black,
                delayMs = 200,
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
        val itemShape = RoundedCornerShape(24.dp)
        LiquidGlassPill(
            shape = itemShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(itemShape)
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

// ─────────────────────────────────────────────────────────────────────────────
// Shadow Utility
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.steadyShadow(
    color: Color = Color.Black.copy(alpha = 0.15f),
    borderRadius: Dp = 16.dp,
    blurRadius: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
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
