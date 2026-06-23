package com.flash.draft.utils

import androidx.compose.ui.geometry.Offset

object ViewportMath {

    // calculateCameraTransform was inlined into DraftCanvas to avoid Pair allocations during high-frequency gesture updates.

    fun screenToWorld(screenPos: Offset, cameraOffset: Offset, cameraScale: Float): Offset {
        return Offset(
            x = (screenPos.x - cameraOffset.x) / cameraScale,
            y = (screenPos.y - cameraOffset.y) / cameraScale
        )
    }

    fun worldToScreen(worldPos: Offset, cameraOffset: Offset, cameraScale: Float): Offset {
        return Offset(
            x = (worldPos.x * cameraScale) + cameraOffset.x,
            y = (worldPos.y * cameraScale) + cameraOffset.y
        )
    }

    // NEW: Ray-Casting algorithm to detect if a point is inside a custom drawn lasso polygon
    fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
        var isInside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.y > point.y) != (pj.y > point.y) &&
                point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }
}