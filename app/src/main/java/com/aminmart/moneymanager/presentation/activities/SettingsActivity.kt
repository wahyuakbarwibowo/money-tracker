package com.aminmart.moneymanager.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.security.BiometricGate
import com.aminmart.moneymanager.presentation.security.SecurityPreference
import com.aminmart.moneymanager.presentation.ui.ThemePreference
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : BottomNavigationActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var textThemeValue: TextView
    private lateinit var switchAppLock: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.navigationIcon = null

        setupBottomNavigation(R.id.nav_settings)

        findViewById<View>(R.id.button_settings_data_management).setOnClickListener {
            startActivity(Intent(this, DataManagementActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_manage_categories).setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_debt_credit).setOnClickListener {
            startActivity(Intent(this, DebtActivity::class.java))
        }

        textThemeValue = findViewById(R.id.text_settings_theme_value)
        updateThemeLabel()
        findViewById<View>(R.id.button_settings_theme).setOnClickListener {
            showThemeDialog()
        }

        findViewById<View>(R.id.button_settings_riba_report).setOnClickListener {
            startActivity(Intent(this, RibaReportActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_recurring).setOnClickListener {
            startActivity(Intent(this, RecurringActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_accounts).setOnClickListener {
            startActivity(Intent(this, AccountsActivity::class.java))
        }

        setupAppLockSwitch()
    }

    private fun setupAppLockSwitch() {
        switchAppLock = findViewById(R.id.switch_settings_app_lock)
        switchAppLock.isChecked = SecurityPreference.isLockEnabled(this)
        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !BiometricGate.canAuthenticate(this)) {
                // Can't enable lock without a biometric/device credential set up.
                Toast.makeText(this, R.string.app_lock_unavailable, Toast.LENGTH_LONG).show()
                switchAppLock.isChecked = false
                return@setOnCheckedChangeListener
            }
            SecurityPreference.setLockEnabled(this, isChecked)
        }
    }

    private fun themeLabels() = arrayOf(
        getString(R.string.theme_system),
        getString(R.string.theme_light),
        getString(R.string.theme_dark)
    )

    private fun updateThemeLabel() {
        textThemeValue.text = themeLabels()[ThemePreference.getMode(this)]
    }

    private fun showThemeDialog() {
        val current = ThemePreference.getMode(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.theme_dialog_title)
            .setSingleChoiceItems(themeLabels(), current) { dialog, which ->
                ThemePreference.setMode(this, which)
                updateThemeLabel()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
