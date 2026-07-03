package com.aminmart.moneymanager.presentation.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.security.AppLockState
import com.aminmart.moneymanager.presentation.security.BiometricGate
import com.aminmart.moneymanager.presentation.security.SecurityPreference
import com.google.android.material.bottomnavigation.BottomNavigationView

import androidx.activity.result.contract.ActivityResultContracts

/**
 * Main Activity with Bottom Navigation
 * Serves as the container for all fragments/screens
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var container: FrameLayout

    private val addTransactionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh logic after adding a transaction
            // For MainActivity, this usually means refreshing the currently displayed fragment/data
            // Since MainActivity acts as a host, it might need to notify its fragments or reload its own data if it displays any
            // For now, let's just trigger a generic refresh, which can be expanded later
            updateNavigationSelection() // This might indirectly trigger refresh in current fragment
        }
    }

    private val editTransactionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh logic after editing a transaction
            updateNavigationSelection() // This might indirectly trigger refresh in current fragment
        }
    }

    companion object {
        const val REQUEST_ADD_TRANSACTION = 100
        const val REQUEST_EDIT_TRANSACTION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        container = findViewById(R.id.fragment_container)

        setupBottomNavigation()
        requestNotificationPermissionIfNeeded()

        // Show dashboard by default
        if (savedInstanceState == null) {
            if (needsAppLock()) {
                authenticateThenStart()
            } else {
                showDashboard()
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    showDashboard()
                    true
                }
                R.id.nav_transactions -> {
                    showTransactions()
                    true
                }
                R.id.nav_budget -> {
                    showBudget()
                    true
                }
                R.id.nav_statistics -> {
                    showStatistics()
                    true
                }
                R.id.nav_settings -> {
                    showSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDashboard() {
        startActivityWithFade(Intent(this, DashboardActivity::class.java))
    }

    private fun showTransactions() {
        startActivityWithFade(Intent(this, TransactionsActivity::class.java))
    }

    private fun showBudget() {
        startActivityWithFade(Intent(this, BudgetActivity::class.java))
    }

    private fun showStatistics() {
        startActivityWithFade(Intent(this, StatisticsActivity::class.java))
    }

    private fun showSettings() {
        startActivityWithFade(Intent(this, SettingsActivity::class.java))
    }

    fun navigateToAddTransaction(transaction: Transaction? = null) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        transaction?.let {
            intent.putExtra("transaction_id", it.id)
            editTransactionLauncher.launch(intent)
        } ?: run {
            addTransactionLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Update bottom navigation selection based on current activity
        updateNavigationSelection()
    }

    private fun updateNavigationSelection() {
        val currentActivity = this::class.java.simpleName
        when {
            currentActivity.contains("Dashboard") -> bottomNavigation.selectedItemId = R.id.nav_dashboard
            currentActivity.contains("Transactions") -> bottomNavigation.selectedItemId = R.id.nav_transactions
            currentActivity.contains("Budget") -> bottomNavigation.selectedItemId = R.id.nav_budget
            currentActivity.contains("Statistics") -> bottomNavigation.selectedItemId = R.id.nav_statistics
            currentActivity.contains("Settings") -> bottomNavigation.selectedItemId = R.id.nav_settings
        }
    }

    private fun startActivityWithFade(intent: Intent) {
        val options = android.app.ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
        startActivity(intent, options)
    }
}
