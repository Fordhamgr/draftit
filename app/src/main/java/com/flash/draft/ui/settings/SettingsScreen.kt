package com.flash.draft.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LensBlur
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flash.draft.ui.theme.LocalWindowSizeInfo
import com.flash.draft.ui.theme.WindowWidthClass
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { paddingValues ->
        val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val windowSizeInfo = LocalWindowSizeInfo.current
        val isExpanded = windowSizeInfo.widthClass == WindowWidthClass.EXPANDED

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 32.dp + navBarsBottom
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            AppearanceSection(uiState = uiState, onEvent = onEvent)
                            PerformanceSection(uiState = uiState, onEvent = onEvent)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            DrawingPhysicsSection(uiState = uiState, onEvent = onEvent)
                            FeedbackSection(uiState = uiState, onEvent = onEvent)
                            ExperimentalSection(uiState = uiState, onEvent = onEvent)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        AppearanceSection(uiState = uiState, onEvent = onEvent)
                        PerformanceSection(uiState = uiState, onEvent = onEvent)
                        DrawingPhysicsSection(uiState = uiState, onEvent = onEvent)
                        FeedbackSection(uiState = uiState, onEvent = onEvent)
                        ExperimentalSection(uiState = uiState, onEvent = onEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(title = "Appearance", modifier = modifier) {
        ToggleRow(
            label = "Dark mode",
            icon = Icons.Default.DarkMode,
            checked = uiState.isDarkMode,
            onCheckedChange = { onEvent(SettingsUiEvent.ToggleDarkMode(it)) },
            showDivider = true
        )
        ToggleRow(
            label = "Fluid dark theme",
            icon = Icons.Default.WaterDrop,
            checked = uiState.isLiquidDarkTheme,
            onCheckedChange = { onEvent(SettingsUiEvent.ToggleLiquidDarkTheme(it)) },
            showDivider = false
        )
    }
}

@Composable
private fun PerformanceSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(title = "Performance", modifier = modifier) {
        ToggleRow(
            label = "Performance mode",
            icon = Icons.Default.Speed,
            checked = uiState.isPerformanceMode,
            onCheckedChange = { onEvent(SettingsUiEvent.TogglePerformanceMode(it)) },
            showDivider = true
        )
        SegmentedControlRow(
            label = "Framerate",
            icon = Icons.Default.Timer,
            options = listOf(30, 60, 120, 0),
            optionLabels = listOf("30", "60", "120", "Unlocked"),
            selectedOption = uiState.framerate,
            onOptionSelected = { onEvent(SettingsUiEvent.SetFramerate(it)) },
            showDivider = false
        )
    }
}

@Composable
private fun DrawingPhysicsSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(title = "Drawing & physics", modifier = modifier) {
        AnimatedVisibility(
            visible = !uiState.isPerformanceMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                SliderRow(
                    label = "Liquid curve",
                    icon = Icons.Default.Timeline,
                    value = uiState.liquidCurve,
                    onValueChange = { onEvent(SettingsUiEvent.SetLiquidCurve(it)) },
                    valueRange = 0f..1f,
                    steps = 0,
                    showDivider = true
                )
                SliderRow(
                    label = "Refraction",
                    icon = Icons.Default.LensBlur,
                    value = uiState.liquidRefraction,
                    onValueChange = { onEvent(SettingsUiEvent.SetLiquidRefraction(it)) },
                    valueRange = 0f..1f,
                    steps = 0,
                    showDivider = true
                )
                SliderRow(
                    label = "Frost (blur)",
                    icon = Icons.Default.BlurOn,
                    value = uiState.liquidFrost,
                    onValueChange = { onEvent(SettingsUiEvent.SetLiquidFrost(it)) },
                    valueRange = 0f..1f,
                    steps = 0,
                    showDivider = true
                )
            }
        }
        SegmentedControlRow(
            label = "Undo limit",
            icon = Icons.Default.History,
            options = listOf(20, 50, 100, 0),
            optionLabels = listOf("20", "50", "100", "No limit"),
            selectedOption = uiState.undoHistoryLimit,
            onOptionSelected = { onEvent(SettingsUiEvent.SetUndoHistoryLimit(it)) },
            showDivider = false
        )
    }
}

@Composable
private fun FeedbackSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(title = "Feedback", modifier = modifier) {
        ToggleRow(
            label = "Haptic feedback",
            icon = Icons.Default.Vibration,
            checked = uiState.isHapticFeedbackEnabled,
            onCheckedChange = { onEvent(SettingsUiEvent.ToggleHapticFeedback(it)) },
            showDivider = false
        )
    }
}

@Composable
private fun ExperimentalSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(title = "Experimental", modifier = modifier) {
        ToggleRow(
            label = "Stylus support",
            icon = Icons.Default.Create,
            checked = uiState.isStylusSupportEnabled,
            onCheckedChange = { onEvent(SettingsUiEvent.ToggleStylusSupport(it)) },
            showDivider = false
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 52.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
fun ToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Switch
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    uncheckedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD1D5DB),
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (showDivider) {
            SettingsDivider()
        }
    }
}

@Composable
fun SegmentedControlRow(
    label: String,
    icon: ImageVector,
    options: List<Int>,
    optionLabels: List<String>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Top line: icon (20dp) + label (16sp, onSurface)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom line: full-width segmented track (44dp tall, 12dp corners)
            val trackColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(trackColor)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == option
                    val optionLabel = optionLabels.getOrNull(index) ?: option.toString()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (isSelected) {
                                    val pillBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                    Modifier
                                        .shadow(2.dp, RoundedCornerShape(10.dp), clip = false)
                                        .background(pillBg, RoundedCornerShape(10.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOptionSelected(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        val textColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = optionLabel,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (showDivider) {
            SettingsDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderRow(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showDivider: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE2E8F0)
    val percentText = (value * 100).roundToInt().toString()

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top line: icon + label on left, integer percentage value ("55", "0") on right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = percentText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom line: full-width slider, primary blue active track, light gray inactive track, solid blue circular thumb
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .shadow(2.dp, CircleShape, clip = false)
                            .background(primaryColor, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        colors = SliderDefaults.colors(
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = inactiveTrackColor
                        ),
                        sliderState = sliderState,
                        modifier = Modifier.height(4.dp)
                    )
                }
            )
        }

        if (showDivider) {
            SettingsDivider()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentLightPreview() {
    com.flash.draft.ui.theme.DraftitTheme(darkTheme = false) {
        SettingsContent(
            uiState = SettingsUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentDarkPreview() {
    com.flash.draft.ui.theme.DraftitTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(isDarkMode = true),
            onEvent = {},
            onBack = {}
        )
    }
}
