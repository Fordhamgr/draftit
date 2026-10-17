package com.flash.draft.ui.canvas

import androidx.annotation.ColorInt
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.flash.draft.model.BackgroundStyle

object CanvasBackgroundTokens {
    val DarkBase: Color = Color(0xFF18181A)
    val LightBase: Color = Color(0xFFF0F0F0)
    val BlueprintBase: Color = Color(0xFF1E3A8A)
    val GreenprintBase: Color = Color(0xFF314F40)
    val BlackPaperBase: Color = Color(0xFF0D0D0D)

    val BlueprintLine: Color = Color.White.copy(alpha = 0.20f)
    val GreenprintLine: Color = Color.White.copy(alpha = 0.15f)
    val BlackPaperLine: Color = Color.White.copy(alpha = 0.15f)

    const val BASE_GRID_SPACING: Float = 100f
    const val GRID_STROKE_WIDTH: Float = 1f

    fun getBaseColor(style: BackgroundStyle, isDarkTheme: Boolean): Color {
        return when (style) {
            BackgroundStyle.Blueprint -> BlueprintBase
            BackgroundStyle.Greenprint -> GreenprintBase
            BackgroundStyle.BlackPaper -> BlackPaperBase
            BackgroundStyle.Solid,
            BackgroundStyle.GraphPaper -> if (isDarkTheme) DarkBase else LightBase
        }
    }

    @ColorInt
    fun getBaseColorArgb(style: BackgroundStyle, isDarkTheme: Boolean): Int {
        return when (style) {
            BackgroundStyle.Blueprint -> android.graphics.Color.parseColor("#1E3A8A")
            BackgroundStyle.Greenprint -> android.graphics.Color.parseColor("#314F40")
            BackgroundStyle.BlackPaper -> android.graphics.Color.parseColor("#0D0D0D")
            BackgroundStyle.Solid,
            BackgroundStyle.GraphPaper -> if (isDarkTheme) {
                android.graphics.Color.parseColor("#18181A")
            } else {
                android.graphics.Color.parseColor("#F0F0F0")
            }
        }
    }

    fun getPatternLineColor(style: BackgroundStyle, colorScheme: ColorScheme): Color {
        return when (style) {
            BackgroundStyle.GraphPaper -> colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            BackgroundStyle.Blueprint -> BlueprintLine
            BackgroundStyle.Greenprint -> GreenprintLine
            BackgroundStyle.BlackPaper -> BlackPaperLine
            BackgroundStyle.Solid -> Color.Transparent
        }
    }

    fun drawCanvasBackground(
        drawScope: DrawScope,
        style: BackgroundStyle,
        isDarkTheme: Boolean,
        colorScheme: ColorScheme,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        exportWidth: Float = drawScope.size.width,
        exportHeight: Float = drawScope.size.height
    ) {
        with(drawScope) {
            drawRect(
                color = getBaseColor(style, isDarkTheme),
                size = androidx.compose.ui.geometry.Size(exportWidth, exportHeight)
            )
            if (style == BackgroundStyle.Solid) return

            val patternColor = getPatternLineColor(style, colorScheme)
            if (patternColor == Color.Transparent || patternColor.alpha <= 0f) return

            var spacing = BASE_GRID_SPACING * scale
            if (spacing <= 0f) return

            while (spacing < 40f) {
                spacing *= 2f
            }
            while (spacing > 160f) {
                spacing /= 2f
            }

            val startX = (offsetX.rem(spacing) + spacing).rem(spacing)
            val startY = (offsetY.rem(spacing) + spacing).rem(spacing)

            var i = 0
            while (true) {
                val x = startX + i * spacing
                if (x > exportWidth) break
                drawLine(
                    color = patternColor,
                    start = Offset(x, 0f),
                    end = Offset(x, exportHeight),
                    strokeWidth = GRID_STROKE_WIDTH
                )
                i++
            }

            var j = 0
            while (true) {
                val y = startY + j * spacing
                if (y > exportHeight) break
                drawLine(
                    color = patternColor,
                    start = Offset(0f, y),
                    end = Offset(exportWidth, y),
                    strokeWidth = GRID_STROKE_WIDTH
                )
                j++
            }
        }
    }
}

fun Modifier.draftBackground(
    style: BackgroundStyle,
    isDarkTheme: Boolean,
    colorScheme: ColorScheme,
    scaleProvider: () -> Float,
    offsetProvider: () -> Offset
): Modifier = this.drawBehind {
    val scale = scaleProvider()
    val offset = offsetProvider()
    CanvasBackgroundTokens.drawCanvasBackground(
        drawScope = this,
        style = style,
        isDarkTheme = isDarkTheme,
        colorScheme = colorScheme,
        scale = scale,
        offsetX = offset.x,
        offsetY = offset.y
    )
}

fun Modifier.drawExportBackground(
    style: BackgroundStyle,
    isDarkTheme: Boolean,
    colorScheme: ColorScheme,
    exportScale: Float,
    worldOriginX: Float,
    worldOriginY: Float,
    exportWidth: Float? = null,
    exportHeight: Float? = null
): Modifier = this.drawBehind {
    CanvasBackgroundTokens.drawCanvasBackground(
        drawScope = this,
        style = style,
        isDarkTheme = isDarkTheme,
        colorScheme = colorScheme,
        scale = exportScale,
        offsetX = -worldOriginX * exportScale,
        offsetY = -worldOriginY * exportScale,
        exportWidth = exportWidth ?: size.width,
        exportHeight = exportHeight ?: size.height
    )
}

fun DrawScope.drawCanvasBackground(
    style: BackgroundStyle,
    isDarkTheme: Boolean,
    colorScheme: ColorScheme,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    exportWidth: Float = size.width,
    exportHeight: Float = size.height
) {
    CanvasBackgroundTokens.drawCanvasBackground(
        drawScope = this,
        style = style,
        isDarkTheme = isDarkTheme,
        colorScheme = colorScheme,
        scale = scale,
        offsetX = offsetX,
        offsetY = offsetY,
        exportWidth = exportWidth,
        exportHeight = exportHeight
    )
}
