package com.flash.draft.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("draftit_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_PERFORMANCE_MODE = "performance_mode"
        private const val KEY_FRAMERATE = "framerate"
        private const val KEY_LIQUID_CURVE = "liquid_curve"
        private const val KEY_LIQUID_REFRACTION = "liquid_refraction"
        private const val KEY_UNDO_LIMIT = "undo_limit"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    }

    // Default values
    private val defaultDarkMode = false
    private val defaultPerformanceMode = false
    private val defaultFramerate = 60 // 0 = Unlocked
    private val defaultLiquidCurve = 0.5f
    private val defaultLiquidRefraction = 0.5f
    private val defaultUndoLimit = 50
    private val defaultHapticFeedback = true

    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    fun getDarkModeFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DARK_MODE) trySend(prefs.getBoolean(KEY_DARK_MODE, defaultDarkMode))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_DARK_MODE, defaultDarkMode))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setPerformanceMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_PERFORMANCE_MODE, enabled).apply()
    fun getPerformanceModeFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PERFORMANCE_MODE) trySend(prefs.getBoolean(KEY_PERFORMANCE_MODE, defaultPerformanceMode))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_PERFORMANCE_MODE, defaultPerformanceMode))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setFramerate(framerate: Int) = prefs.edit().putInt(KEY_FRAMERATE, framerate).apply()
    fun getFramerateFlow(): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FRAMERATE) trySend(prefs.getInt(KEY_FRAMERATE, defaultFramerate))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_FRAMERATE, defaultFramerate))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }



    fun setLiquidCurve(level: Float) = prefs.edit().putFloat(KEY_LIQUID_CURVE, level).apply()
    fun getLiquidCurveFlow(): Flow<Float> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LIQUID_CURVE) trySend(prefs.getFloat(KEY_LIQUID_CURVE, defaultLiquidCurve))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getFloat(KEY_LIQUID_CURVE, defaultLiquidCurve))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setLiquidRefraction(level: Float) = prefs.edit().putFloat(KEY_LIQUID_REFRACTION, level).apply()
    fun getLiquidRefractionFlow(): Flow<Float> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LIQUID_REFRACTION) trySend(prefs.getFloat(KEY_LIQUID_REFRACTION, defaultLiquidRefraction))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getFloat(KEY_LIQUID_REFRACTION, defaultLiquidRefraction))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setUndoHistoryLimit(limit: Int) = prefs.edit().putInt(KEY_UNDO_LIMIT, limit).apply()
    fun getUndoHistoryLimitFlow(): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_UNDO_LIMIT) trySend(prefs.getInt(KEY_UNDO_LIMIT, defaultUndoLimit))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_UNDO_LIMIT, defaultUndoLimit))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    fun getHapticFeedbackFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HAPTIC_FEEDBACK) trySend(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, defaultHapticFeedback))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, defaultHapticFeedback))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
