package com.flash.draft.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isPerformanceMode by viewModel.isPerformanceMode.collectAsStateWithLifecycle()
    val framerate by viewModel.framerate.collectAsStateWithLifecycle()

    val liquidCurve by viewModel.liquidCurve.collectAsStateWithLifecycle()
    val liquidRefraction by viewModel.liquidRefraction.collectAsStateWithLifecycle()
    val undoHistoryLimit by viewModel.undoHistoryLimit.collectAsStateWithLifecycle()
    val isHapticFeedbackEnabled by viewModel.isHapticFeedbackEnabled.collectAsStateWithLifecycle()

    val backgroundColor = if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val surfaceColor = if (isDarkMode) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    val actionBlue = Color(0xFF007AFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    SettingsGroup(title = "APPEARANCE", surfaceColor = surfaceColor, secondaryTextColor = secondaryTextColor) {
                        ToggleRow(
                            label = "Dark Mode",
                            icon = Icons.Default.DarkMode,
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            textColor = textColor
                        )
                    }
                }

                item {
                    SettingsGroup(title = "PERFORMANCE", surfaceColor = surfaceColor, secondaryTextColor = secondaryTextColor) {
                        ToggleRow(
                            label = "Performance Mode",
                            icon = Icons.Default.Speed,
                            checked = isPerformanceMode,
                            onCheckedChange = { viewModel.togglePerformanceMode(it) },
                            textColor = textColor,
                            showDivider = true
                        )
                        SegmentedControlRow(
                            label = "Set framerate",
                            icon = Icons.Default.Timer,
                            options = listOf(30, 60, 120, 0),
                            optionLabels = listOf("30", "60", "120", "Unlocked"),
                            selectedOption = framerate,
                            onOptionSelected = { viewModel.setFramerate(it) },
                            textColor = textColor,
                            actionBlue = actionBlue,
                            showDivider = false
                        )
                    }
                }

                item {
                    SettingsGroup(title = "DRAWING & PHYSICS", surfaceColor = surfaceColor, secondaryTextColor = secondaryTextColor) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isPerformanceMode,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column {
                                SliderRow(
                                    label = "Liquid Curve",
                                    icon = Icons.Default.Timeline,
                                    value = liquidCurve,
                                    onValueChange = { viewModel.setLiquidCurve(it) },
                                    valueRange = 0f..1f,
                                    steps = 0,
                                    textColor = textColor,
                                    actionBlue = actionBlue,
                                    showDivider = true
                                )
                                SliderRow(
                                    label = "Refraction",
                                    icon = Icons.Default.LensBlur,
                                    value = liquidRefraction,
                                    onValueChange = { viewModel.setLiquidRefraction(it) },
                                    valueRange = 0f..1f,
                                    steps = 0,
                                    textColor = textColor,
                                    actionBlue = actionBlue,
                                    showDivider = true
                                )
                            }
                        }
                        SegmentedControlRow(
                            label = "Undo Limit",
                            icon = Icons.Default.History,
                            options = listOf(20, 50, 100),
                            optionLabels = listOf("20", "50", "100"),
                            selectedOption = undoHistoryLimit,
                            onOptionSelected = { viewModel.setUndoHistoryLimit(it) },
                            textColor = textColor,
                            actionBlue = actionBlue,
                            showDivider = false
                        )
                    }
                }

                item {
                    SettingsGroup(title = "FEEDBACK", surfaceColor = surfaceColor, secondaryTextColor = secondaryTextColor) {
                        ToggleRow(
                            label = "Haptic Feedback",
                            icon = Icons.Default.Vibration,
                            checked = isHapticFeedbackEnabled,
                            onCheckedChange = { viewModel.toggleHapticFeedback(it) },
                            textColor = textColor
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    surfaceColor: Color,
    secondaryTextColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryTextColor,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColor)
        ) {
            content()
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = label, color = textColor, fontSize = 17.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759) // iOS Green
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp), // align with text
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )
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
    textColor: Color,
    actionBlue: Color,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = label, color = textColor, fontSize = 17.sp)
            }
            
            // Segmented Control Boxes
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.1f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) actionBlue else Color.Transparent)
                            .clickable { onOptionSelected(option) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionLabels[index],
                            color = if (isSelected) Color.White else textColor.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    textColor: Color,
    actionBlue: Color,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = label, color = textColor, fontSize = 17.sp)
            }
            
            // Constrained slider
            Box(
                modifier = Modifier.width(140.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = actionBlue,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.2f),
                        inactiveTickColor = Color.Transparent,
                        activeTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}
