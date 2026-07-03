package com.aminmart.moneymanager.presentation.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Persists and applies the user's theme choice (system / light / dark).
 * Call [apply] once in Application.onCreate and again after the user changes it.
 */
object ThemePreference {

    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_MODE = "theme_mode"

    fun getMode(context: Context): Int =
        prefs(context).getInt(KEY_MODE, MODE_SYSTEM)

    fun setMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_MODE, mode).apply()
        apply(mode)
    }

    /** Applies the stored preference. */
    fun apply(context: Context) = apply(getMode(context))

    private fun apply(mode: Int) {
        val nightMode = when (mode) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
