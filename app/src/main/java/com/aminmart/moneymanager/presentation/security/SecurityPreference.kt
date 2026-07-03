package com.aminmart.moneymanager.presentation.security

import android.content.Context

/** Stores whether biometric/device-credential app lock is enabled. */
object SecurityPreference {

    private const val PREFS_NAME = "security_prefs"
    private const val KEY_LOCK_ENABLED = "app_lock_enabled"

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Process-scoped auth state. Resets to false on a fresh process (cold start),
 * so the lock is shown once per app launch.
 */
object AppLockState {
    @Volatile
    var authenticated: Boolean = false
}
