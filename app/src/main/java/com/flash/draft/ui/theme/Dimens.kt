package com.flash.draft.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * Golden Ratio Design Tokens
 *
 * All spacing, sizing, and dimension values derive from a single base unit (8dp)
 * scaled by the golden ratio φ ≈ 1.618. This creates a naturally harmonious
 * visual rhythm across the entire app.
 *
 * Scale progression (base = 8dp):
 *   8 → 13 → 21 → 34 → 55 → 89 → 144
 *   (Fibonacci sequence, which converges to φ ratio between adjacent terms)
 *
 * We use Fibonacci numbers as they are the integer approximation of the golden
 * ratio scale and align perfectly with Android's 8dp grid system.
 */
object Dimens {

    // ── Core spacing scale (Fibonacci / golden ratio) ────────────────────
    /** Hairline spacing — separator lines, tight icon insets */
    val spacing2: Dp = 2.dp

    /** Micro spacing — between icon and label, tight inner padding */
    val spacing4: Dp = 4.dp

    /** XSmall spacing — compact arrangements, inner pill padding */
    val spacing5: Dp = 5.dp

    /** Small spacing — standard inner padding, small gaps */
    val spacing8: Dp = 8.dp

    /** Medium spacing — standard component gaps, section inner padding */
    val spacing13: Dp = 13.dp

    /** Large spacing — between major sections, outer padding (phone) */
    val spacing21: Dp = 21.dp

    /** XLarge spacing — generous section gaps, outer padding (tablet) */
    val spacing34: Dp = 34.dp

    /** XXLarge spacing — hero spacing, large section dividers */
    val spacing55: Dp = 55.dp

    // ── Component sizing scale ───────────────────────────────────────────
    /** Touch target (minimum) — icon buttons, small controls */
    val touchMin: Dp = 44.dp         // ≈ 34 × φ^0.5 — Apple HIG minimum, close to golden

    /** Icon size — standard toolbar icons */
    val iconSmall: Dp = 18.dp        // ≈ 13 × φ^0.5
    val iconMedium: Dp = 21.dp       // Fibonacci
    val iconStandard: Dp = 24.dp     // Material standard, ≈ 21 × φ^0.2
    val iconLarge: Dp = 28.dp        // ≈ 21 × φ^0.5

    /** Button/pill heights */
    val pillHeight: Dp = 55.dp       // Fibonacci — dock height, action bars
    val pillHeightCompact: Dp = 44.dp // Compact pill (search bars, small actions)

    /** Color swatch size */
    val swatchSize: Dp = 21.dp       // Fibonacci
    val swatchSizeLarge: Dp = 24.dp

    /** Card corner radii (golden ratio scale) */
    val radiusSmall: Dp = 8.dp       // Fibonacci
    val radiusMedium: Dp = 13.dp     // Fibonacci
    val radiusLarge: Dp = 21.dp      // Fibonacci
    val radiusPill: Dp = 34.dp       // Fibonacci — fully rounded pills

    /** Avatar sizes */
    val avatarSmall: Dp = 34.dp      // Fibonacci
    val avatarMedium: Dp = 44.dp

    /** Toolbar padding */
    val toolbarPadding: Dp = 13.dp   // Fibonacci — inner padding for glass pills

    /** Resize handle */
    val resizeHandle: Dp = 18.dp     // Golden-derived
    val resizeHitTarget: Dp = 55.dp  // Fibonacci — generous touch target

    /** Divider thickness */
    val divider: Dp = 1.dp
    val dividerLength: Dp = 21.dp    // Fibonacci

    // ── Responsive edge padding ─────────────────────────────────────────
    /** Phone edge padding — aligns to spacing21 (≈ φ × 13) */
    val edgePaddingPhone: Dp = spacing13

    /** Tablet edge padding — aligns to spacing21 */
    val edgePaddingTablet: Dp = spacing21

    /** Large tablet edge padding — aligns to spacing34 */
    val edgePaddingLargeTablet: Dp = spacing34

    // ── Grid gutter sizes ───────────────────────────────────────────────
    val gutterPhone: Dp = spacing8
    val gutterTablet: Dp = spacing13
    val gutterLargeTablet: Dp = spacing21

    // ── Typography scale (golden ratio) ─────────────────────────────────
    /** Font sizes following φ progression: 11 → 13 → 16 → 20 → 26 → 32 */
    val textXSmall: TextUnit = 11.sp
    val textSmall: TextUnit = 13.sp   // ≈ 11 × φ^0.4 — captions, dates
    val textBody: TextUnit = 16.sp    // ≈ 13 × φ^0.5 — body text (Material default)
    val textTitle: TextUnit = 20.sp   // ≈ 16 × φ^0.5 — section headers
    val textHeadline: TextUnit = 26.sp // ≈ 20 × φ^0.5 — screen titles
    val textDisplay: TextUnit = 34.sp // Fibonacci — hero display text

    // ── Bottom insets ───────────────────────────────────────────────────
    val bottomDockInset: Dp = spacing21
    val bottomContentPadding: Dp = 120.dp // ≈ 89 + 34 (Fibonacci sum) — clearance for FAB
}
