package com.aminmart.moneymanager.presentation.activities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Account
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.adapters.AccountAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountsActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var adapter: AccountAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        app = application as MoneyManagerApplication

        findViewById<Toolbar>(R.id.toolbar_accounts).setNavigationOnClickListener { finish() }
        recycler = findViewById(R.id.recycler_accounts)
        emptyView = findViewById(R.id.view_accounts_empty)

        adapter = AccountAdapter { account -> showAccountOptions(account) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_accounts_add).setOnClickListener { showAddDialog() }

        scope.launch {
            app.accountUseCases.getAccounts().collect { accounts ->
                adapter.submitList(accounts)
                emptyView.visibility = if (accounts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }
        val nameInput = EditText(this).apply { hint = getString(R.string.account_name_hint) }
        val balanceInput = EditText(this).apply {
            hint = getString(R.string.account_initial_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(nameInput)
        container.addView(balanceInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.account_add)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val initial = balanceInput.text.toString().toDoubleOrNull() ?: 0.0
                scope.launch {
                    withContext(Dispatchers.IO) {
                        app.accountUseCases.addAccount(Account(name = name, initialBalance = initial))
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAccountOptions(account: Account) {
        if (account.id == Transaction.DEFAULT_ACCOUNT_ID) {
            Toast.makeText(this, R.string.account_default_locked, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(account.name)
            .setItems(arrayOf(getString(R.string.delete))) { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) { app.accountUseCases.deleteAccount(account.id) }
                }
            }
            .show()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
