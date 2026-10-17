package com.flash.draft.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.flash.draft.AppConfig
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * About screen redesign.
 *
 * WIRING NOTES FOR THE CODING AGENT (search for "AGENT:" in this file):
 *  - Colors come from MaterialTheme.colorScheme. The app's brand color is a muted dark red and is
 *    expected to be colorScheme.primary. If it is not, point `primary` at the brand red in the theme.
 *    Do NOT hardcode a hex in this file (the amber star color is the only intentional literal).
 *  - Card surfaces use colorScheme.surface plus a hairline outlineVariant border. If the app's
 *    background and surface are the same color, the cards will only be defined by the border;
 *    in that case use the surface/container color the Dashboard cards use.
 *  - The app icon is read from the installed app (PackageManager), so no drawable wiring is needed.
 */

// AGENT: this replaces AppConfig.DESCRIPTION on this screen. If the string is localized, move it to
// strings.xml (or update AppConfig.DESCRIPTION to this text) and read it from there instead.
private const val ABOUT_DESCRIPTION =
    "Draft It is a visual workspace for planning and designing, with notes, tasks, code, images " +
        "and PDFs on one free-form canvas. Built by a solo developer, with new features and " +
        "updates shaped by your feedback."

// Semantic color (a rating star is always amber), intentionally not part of the theme.
private val StarAmber = Color(0xFFF5B301)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    // AGENT: optional hooks so existing call sites keep compiling. Wire these to analytics and to
    // persistence if wanted (e.g. save "rate_prompt_dismissed" in DataStore / SharedPreferences so
    // the rate card stays hidden after "Maybe later" or after the user has rated).
    onRateClicked: () -> Unit = {},
    onMaybeLater: () -> Unit = {},
    // AGENT: pass false from the caller if the user already rated or dismissed the prompt before.
    showRatePrompt: Boolean = true,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var rateCardVisible by rememberSaveable { mutableStateOf(showRatePrompt) }

    // Respect the system "remove animations" setting: stars are shown filled and static.
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
            ) {
                // ---------- Identity: icon, name, version, description, developer ----------
                item {
                    Reveal(delayMillis = 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppIcon(size = 72)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                // AGENT: AppConfig.APP_NAME currently renders "DraftIt" (no space).
                                // Change it to "Draft It" in AppConfig if the spaced name is wanted.
                                text = AppConfig.APP_NAME,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Version ${AppConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = ABOUT_DESCRIPTION,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Developed by ${AppConfig.DEVELOPER_NAME}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---------- Rate card ----------
                item {
                    Reveal(delayMillis = 150) {
                        AnimatedVisibility(
                            visible = rateCardVisible,
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                        ) {
                            Column {
                                Spacer(Modifier.height(24.dp))
                                RateCard(
                                    reduceMotion = reduceMotion,
                                    onRate = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onRateClicked()
                                        openPlayStore(context)
                                    },
                                    onLater = {
                                        onMaybeLater()
                                        rateCardVisible = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ---------- Support ----------
                item {
                    Reveal(delayMillis = 250) {
                        Column {
                            SectionLabel("Support")
                            AboutCard {
                                AboutActionRow(
                                    icon = Icons.Outlined.Email,
                                    label = "Send feedback",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        sendEmail(context, AppConfig.gmail, "Feedback")
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                                AboutActionRow(
                                    icon = Icons.Outlined.Flag,
                                    label = "Report an issue",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        sendEmail(
                                            context,
                                            AppConfig.gmail,
                                            "Reporting an issue with the application"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // ---------- Legal ----------
                item {
                    Reveal(delayMillis = 300) {
                        Column {
                            SectionLabel("Legal")
                            AboutCard {
                                AboutActionRow(
                                    icon = Icons.Outlined.PrivacyTip,
                                    label = "Privacy policy",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openUrl(context, AppConfig.Privacyandterms)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// Rate card
// ============================================================================================

@Composable
private fun RateCard(
    reduceMotion: Boolean,
    onRate: () -> Unit,
    onLater: () -> Unit,
) {
    AboutCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedStarRating(reduceMotion = reduceMotion)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Enjoying Draft It?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A quick rating helps a solo developer reach more people.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Rate on Play Store",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onLater, modifier = Modifier.height(44.dp)) {
                Text(
                    text = "Maybe later",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Five stars that fill with amber one after another (pop to 1.3x then settle), hold, fade out and
 * repeat, with a small sparkle burst when the last star fills. Pure Compose, no Lottie needed.
 */
@Composable
private fun AnimatedStarRating(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val starCount = 5
    val starSize = 40.dp
    val gap = 6.dp
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val transition = rememberInfiniteTransition(label = "stars")

    Box(
        modifier = modifier.semantics { contentDescription = "Five star rating" },
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            repeat(starCount) { index ->
                val startOffset = StartOffset(600 + index * 250)

                val animatedAlpha by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 5000
                            0f at 0
                            1f at 300 using FastOutSlowInEasing
                            1f at 3900
                            0f at 4500
                            0f at 5000
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = startOffset
                    ),
                    label = "star_alpha_$index"
                )
                val animatedScale by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 5000
                            0.4f at 0
                            1.3f at 300 using FastOutSlowInEasing
                            1f at 600 using FastOutSlowInEasing
                            1f at 5000
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = startOffset
                    ),
                    label = "star_scale_$index"
                )

                val fillAlpha = if (reduceMotion) 1f else animatedAlpha
                val fillScale = if (reduceMotion) 1f else animatedScale

                Box(modifier = Modifier.size(starSize)) {
                    // Empty (grey) star
                    Canvas(Modifier.size(starSize)) {
                        drawStar(color = emptyColor)
                    }
                    // Filled (amber) star, animated on top
                    Canvas(
                        Modifier
                            .size(starSize)
                            .graphicsLayer {
                                alpha = fillAlpha
                                scaleX = fillScale
                                scaleY = fillScale
                            }
                    ) {
                        drawStar(color = StarAmber)
                    }
                }
            }
        }

        if (!reduceMotion) {
            val burst by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 5000
                        0f at 0
                        1f at 1500 using FastOutSlowInEasing
                        1f at 5000
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(1850)
                ),
                label = "burst"
            )
            val rowWidth = starSize * starCount + gap * (starCount - 1)
            Canvas(Modifier.size(width = rowWidth, height = starSize)) {
                val center = Offset(size.width - starSize.toPx() / 2f, size.height / 2f)
                val distance = 34.dp.toPx() * burst
                val dotAlpha = sin(PI * burst).toFloat().coerceIn(0f, 1f)
                val angles = listOf(-135.0, -45.0, 170.0, 10.0, -90.0)
                angles.forEach { deg ->
                    val rad = Math.toRadians(deg)
                    drawCircle(
                        color = StarAmber.copy(alpha = dotAlpha),
                        radius = 3.dp.toPx(),
                        center = Offset(
                            center.x + (distance * cos(rad)).toFloat(),
                            center.y + (distance * sin(rad)).toFloat()
                        )
                    )
                }
            }
        }
    }
}

/** Draws a 5-point star with softly rounded corners (fill + same-color round-join stroke). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(color: Color) {
    val s = size.minDimension
    val cx = s / 2f
    val cy = s / 2f * 1.04f
    val outer = s * 0.44f
    val inner = outer * 0.42f
    val path = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
    )
}

// ============================================================================================
// App icon
// ============================================================================================

/**
 * Shows the real app icon read from the installed app via PackageManager.
 */
@Composable
private fun AppIcon(size: Int) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.dp.roundToPx() }
    val icon: ImageBitmap? = remember(context, sizePx) {
        runCatching {
            context.packageManager
                .getApplicationIcon(context.packageName)
                .toBitmap(sizePx, sizePx)
                .asImageBitmap()
        }.getOrNull()
    }
    val shape = RoundedCornerShape(22.dp)

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = "App icon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(shape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

// ============================================================================================
// Shared building blocks
// ============================================================================================

/** Fades and slides a block in once, after [delayMillis]. Does not replay when scrolled back. */
@Composable
private fun Reveal(delayMillis: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(delayMillis.toLong())
            visible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { 50 }
        )
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(content = content)
    }
}

@Composable
private fun AboutActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "row_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ============================================================================================
// Intents
// ============================================================================================

private fun sendEmail(context: Context, email: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens this app's Play Store listing (market:// first, https fallback).
 * context.packageName is the applicationId, so no package name is hardcoded.
 */
private fun openPlayStore(context: Context) {
    val pkg = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    } catch (e: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen(onBack = {})
    }
}
