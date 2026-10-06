package com.flash.draft.model

data class Project(
    val id: String,
    val title: String,
    val lastModifiedMs: Long,
    val previewElements: List<CanvasElement> = emptyList(), // Used to draw the homescreen thumbnails
    val backgroundStyle: BackgroundStyle = BackgroundStyle.Solid
)