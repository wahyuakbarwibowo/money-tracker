package com.aminmart.moneymanager.presentation.activities

import android.content.Intent
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.aminmart.moneymanager.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BottomNavigationActivity : AppCompatActivity() {
    protected val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    protected fun setupBottomNavigation(@IdRes selectedItemId: Int) {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = selectedItemId
        bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                true
            } else {
                navigateToBottomMenu(item.itemId)
                true
            }
        }
    }

    private fun navigateToBottomMenu(@IdRes itemId: Int) {
        val target = when (itemId) {
            R.id.nav_dashboard -> DashboardActivity::class.java
            R.id.nav_transactions -> TransactionsActivity::class.java
            R.id.nav_budget -> BudgetActivity::class.java
            R.id.nav_statistics -> StatisticsActivity::class.java
            R.id.nav_settings -> SettingsActivity::class.java
            else -> return
        }

        val intent = Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val options = android.app.ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
        startActivity(intent, options)
        // Dashboard stays as the task root; other tabs are replaced so Back
        // always returns to Dashboard, then exits.
        if (this !is DashboardActivity) finish()
    }

    protected fun <T> Flow<T>.collectInScope(action: suspend (T) -> Unit) {
        activityScope.launch {
            collect { action(it) }
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
