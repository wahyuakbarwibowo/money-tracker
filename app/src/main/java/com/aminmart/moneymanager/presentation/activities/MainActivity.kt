package com.aminmart.moneymanager.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.security.AppLockState
import com.aminmart.moneymanager.presentation.security.BiometricGate
import com.aminmart.moneymanager.presentation.security.SecurityPreference

/**
 * Launcher gate: runs the optional app-lock prompt, then hands off to
 * [DashboardActivity] and finishes so it never sits under the back stack.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            if (needsAppLock()) {
                authenticateThenStart()
            } else {
                showDashboard()
            }
        }
    }

    private fun needsAppLock(): Boolean =
        SecurityPreference.isLockEnabled(this) &&
            !AppLockState.authenticated &&
            BiometricGate.canAuthenticate(this)

    private fun authenticateThenStart() {
        BiometricGate.prompt(
            activity = this,
            title = getString(R.string.app_lock_title),
            subtitle = getString(R.string.app_lock_subtitle),
            onSuccess = { showDashboard() },
            onFailure = { finishAffinity() }
        )
    }

    private fun showDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
