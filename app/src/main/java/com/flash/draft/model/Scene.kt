package com.flash.draft.model

import java.util.UUID

data class Scene(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Scene",
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float
)
