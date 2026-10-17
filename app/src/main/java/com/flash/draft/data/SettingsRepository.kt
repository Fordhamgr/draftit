package com.flash.draft.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("draftit_settings", Context.MODE_PRIVATE)

    // Keep strong references to listeners to prevent them from being garbage collected
    // SharedPreferences uses a WeakHashMap which can prematurely collect local listener variables
    private val activeListeners = java.util.concurrent.CopyOnWriteArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LIQUID_DARK_THEME = "liquid_dark_theme"
        private const val KEY_PERFORMANCE_MODE = "performance_mode"
        private const val KEY_FRAMERATE = "framerate"
        private const val KEY_LIQUID_CURVE = "liquid_curve"
        private const val KEY_LIQUID_REFRACTION = "liquid_refraction"
        private const val KEY_LIQUID_FROST = "liquid_frost"
        private const val KEY_UNDO_LIMIT = "undo_limit"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_STYLUS_SUPPORT = "stylus_support"
        private const val KEY_RECENT_DRAWING_COLORS = "recent_drawing_colors"
    }

    // Default values
    private val defaultDarkMode = false
    private val defaultLiquidDarkTheme = true
    private val defaultPerformanceMode = false
    private val defaultFramerate = 0 // 0 = Unlocked
    private val defaultLiquidCurve = 0.5f
    private val defaultLiquidRefraction = 0.5f
    private val defaultLiquidFrost = 0.0f
    private val defaultUndoLimit = 0
    private val defaultHapticFeedback = false
    private val defaultStylusSupport = false

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, defaultDarkMode)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    fun getDarkModeFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DARK_MODE) trySend(prefs.getBoolean(KEY_DARK_MODE, defaultDarkMode))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_DARK_MODE, defaultDarkMode))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun isLiquidDarkTheme(): Boolean = prefs.getBoolean(KEY_LIQUID_DARK_THEME, defaultLiquidDarkTheme)
    fun setLiquidDarkTheme(enabled: Boolean) = prefs.edit().putBoolean(KEY_LIQUID_DARK_THEME, enabled).apply()
    fun getLiquidDarkThemeFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LIQUID_DARK_THEME) trySend(prefs.getBoolean(KEY_LIQUID_DARK_THEME, defaultLiquidDarkTheme))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_LIQUID_DARK_THEME, defaultLiquidDarkTheme))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun isPerformanceMode(): Boolean = prefs.getBoolean(KEY_PERFORMANCE_MODE, defaultPerformanceMode)
    fun setPerformanceMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_PERFORMANCE_MODE, enabled).apply()
    fun getPerformanceModeFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PERFORMANCE_MODE) trySend(prefs.getBoolean(KEY_PERFORMANCE_MODE, defaultPerformanceMode))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_PERFORMANCE_MODE, defaultPerformanceMode))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun getFramerate(): Int = prefs.getInt(KEY_FRAMERATE, defaultFramerate)
    fun setFramerate(framerate: Int) = prefs.edit().putInt(KEY_FRAMERATE, framerate).apply()
    fun getFramerateFlow(): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FRAMERATE) trySend(prefs.getInt(KEY_FRAMERATE, defaultFramerate))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_FRAMERATE, defaultFramerate))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun getLiquidCurve(): Float = prefs.getFloat(KEY_LIQUID_CURVE, defaultLiquidCurve)
    fun setLiquidCurve(level: Float) = prefs.edit().putFloat(KEY_LIQUID_CURVE, level).apply()
    fun getLiquidCurveFlow(): Flow<Float> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LIQUID_CURVE) trySend(prefs.getFloat(KEY_LIQUID_CURVE, defaultLiquidCurve))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getFloat(KEY_LIQUID_CURVE, defaultLiquidCurve))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun getLiquidRefraction(): Float = prefs.getFloat(KEY_LIQUID_REFRACTION, defaultLiquidRefraction)
    fun setLiquidRefraction(level: Float) = prefs.edit().putFloat(KEY_LIQUID_REFRACTION, level).apply()
    fun getLiquidRefractionFlow(): Flow<Float> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LIQUID_REFRACTION) trySend(prefs.getFloat(KEY_LIQUID_REFRACTION, defaultLiquidRefraction))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getFloat(KEY_LIQUID_REFRACTION, defaultLiquidRefraction))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun getLiquidFrost(): Float = prefs.getFloat(KEY_LIQUID_FROST, defaultLiquidFrost)
    fun setLiquidFrost(level: Float) = prefs.edit().putFloat(KEY_LIQUID_FROST, level).apply()
    fun getLiquidFrostFlow(): Flow<Float> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LIQUID_FROST) trySend(prefs.getFloat(KEY_LIQUID_FROST, defaultLiquidFrost))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getFloat(KEY_LIQUID_FROST, defaultLiquidFrost))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun getUndoHistoryLimit(): Int = prefs.getInt(KEY_UNDO_LIMIT, defaultUndoLimit)
    fun setUndoHistoryLimit(limit: Int) = prefs.edit().putInt(KEY_UNDO_LIMIT, limit).apply()
    fun getUndoHistoryLimitFlow(): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_UNDO_LIMIT) trySend(prefs.getInt(KEY_UNDO_LIMIT, defaultUndoLimit))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_UNDO_LIMIT, defaultUndoLimit))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun isHapticFeedbackEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, defaultHapticFeedback)
    fun setHapticFeedbackEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    fun getHapticFeedbackFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HAPTIC_FEEDBACK) trySend(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, defaultHapticFeedback))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, defaultHapticFeedback))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }
    
    fun isStylusSupportEnabled(): Boolean = prefs.getBoolean(KEY_STYLUS_SUPPORT, defaultStylusSupport)
    fun setStylusSupportEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_STYLUS_SUPPORT, enabled).apply()
    fun getStylusSupportFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_STYLUS_SUPPORT) trySend(prefs.getBoolean(KEY_STYLUS_SUPPORT, defaultStylusSupport))
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_STYLUS_SUPPORT, defaultStylusSupport))
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    fun getRecentColors(): List<Color> {
        val raw = prefs.getString(KEY_RECENT_DRAWING_COLORS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .map { Color(it) }
    }

    fun addRecentColor(color: Color) {
        val argb = color.toArgb()
        val current = getRecentColors().map { it.toArgb() }.toMutableList()
        current.remove(argb)
        current.add(0, argb)
        val trimmed = current.take(8)
        prefs.edit().putString(KEY_RECENT_DRAWING_COLORS, trimmed.joinToString(",")).apply()
    }

    fun getRecentColorsFlow(): Flow<List<Color>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_RECENT_DRAWING_COLORS) trySend(getRecentColors())
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getRecentColors())
        awaitClose { 
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }
}
