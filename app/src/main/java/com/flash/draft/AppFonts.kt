package com.flash.draft

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.flash.draft.R

object AppFonts {
    // Reference the local .ttf files in your res/font folder
    val Sans = FontFamily(Font(R.font.inter))
    val Serif = FontFamily(Font(R.font.playfair))
    val Mono = FontFamily(Font(R.font.ciracode))
    val Handwriting = FontFamily(Font(R.font.caveatt))
}