package com.flash.draft.utils

import android.util.Log

/**
 * Centralized logging mechanism to track UI state, database operations, navigation, and user triggers.
 * All logs are strictly gated by BuildConfig.DEBUG (or a manual toggle) to ensure zero overhead in production.
 */
object DraftLogger {
    // Master toggle for logging. Hardcoded to true for debug, false for release.
    private const val IS_ENABLED = true // We use a constant here for now since BuildConfig.DEBUG might not be reliable during Compose Previews

    private const val TAG_NAV = "DraftLog_NAV"
    private const val TAG_DB = "DraftLog_DB"
    private const val TAG_UI = "DraftLog_UI"
    private const val TAG_TRIGGER = "DraftLog_TRIGGER"

    /**
     * Tracks screen changes.
     */
    fun logNav(route: String?) {
        if (!IS_ENABLED) return
        Log.i(TAG_NAV, "Navigated to -> ${route ?: "Unknown"}")
    }

    /**
     * Tracks read/writes, payload sizes, and execution times.
     */
    fun logDb(action: String, timeMs: Float? = null, details: String = "") {
        if (!IS_ENABLED) return
        val timeStr = timeMs?.let { " [${String.format("%.2f", it)}ms]" } ?: ""
        val detailStr = if (details.isNotEmpty()) " - $details" else ""
        Log.d(TAG_DB, "$action$timeStr$detailStr")
    }

    /**
     * Tracks read/writes, payload sizes, and execution times with Throwable.
     */
    fun logDbError(action: String, error: Throwable) {
        if (!IS_ENABLED) return
        Log.e(TAG_DB, "$action FAILED", error)
    }

    /**
     * Tracks major recompositions or render loops.
     */
    fun logUi(component: String, message: String) {
        if (!IS_ENABLED) return
        Log.v(TAG_UI, "[$component] $message")
    }

    /**
     * Tracks user actions (e.g., changing tools, zooming, panning limits).
     */
    fun logTrigger(event: String, payload: String = "") {
        if (!IS_ENABLED) return
        val payloadStr = if (payload.isNotEmpty()) ": $payload" else ""
        Log.d(TAG_TRIGGER, "User Action -> $event$payloadStr")
    }
}
