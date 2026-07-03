package com.aminmart.moneymanager.presentation.activities

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.RecurringRule
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.adapters.RecurringAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecurringActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var adapter: RecurringAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val frequencies = RecurringRule.Frequency.values()
    private val types = Transaction.TransactionType.values()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring)

        app = application as MoneyManagerApplication

        findViewById<Toolbar>(R.id.toolbar_recurring).setNavigationOnClickListener { finish() }
        recycler = findViewById(R.id.recycler_recurring)
        emptyView = findViewById(R.id.view_recurring_empty)

        adapter = RecurringAdapter { rule -> showDeleteDialog(rule) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_recurring_add).setOnClickListener {
            showAddDialog()
        }

        observeRules()
    }

    private fun observeRules() {
        scope.launch {
            app.recurringUseCases.getRules().collect { rules ->
                adapter.submitList(rules)
                emptyView.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_recurring, null)
        val spinnerType = view.findViewById<Spinner>(R.id.spinner_recurring_type)
        val spinnerFreq = view.findViewById<Spinner>(R.id.spinner_recurring_frequency)
        val editAmount = view.findViewById<EditText>(R.id.edit_recurring_amount)
        val editCategory = view.findViewById<EditText>(R.id.edit_recurring_category)
        val editDescription = view.findViewById<EditText>(R.id.edit_recurring_description)
        val editInterval = view.findViewById<EditText>(R.id.edit_recurring_interval)

        spinnerType.adapter = simpleAdapter(types.map { it.name })
        spinnerFreq.adapter = simpleAdapter(frequencies.map { it.name })

        AlertDialog.Builder(this)
            .setTitle(R.string.recurring_add)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val amount = editAmount.text.toString().toDoubleOrNull()
                val category = editCategory.text.toString().trim()
                if (amount == null || amount <= 0.0 || category.isEmpty()) return@setPositiveButton

                val rule = RecurringRule(
                    type = types[spinnerType.selectedItemPosition],
                    amount = amount,
                    category = category,
                    description = editDescription.text.toString().trim(),
                    frequency = frequencies[spinnerFreq.selectedItemPosition],
                    intervalCount = editInterval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    nextRun = System.currentTimeMillis()
                )
                scope.launch {
                    withContext(Dispatchers.IO) { app.recurringUseCases.addRule(rule) }
                    // Generate immediately so the first occurrence appears now.
                    withContext(Dispatchers.IO) { app.recurringUseCases.generateDue() }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(rule: RecurringRule) {
        AlertDialog.Builder(this)
            .setTitle(rule.category)
            .setItems(arrayOf(getString(R.string.delete))) { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) { app.recurringUseCases.deleteRule(rule.id) }
                }
            }
            .show()
    }

    private fun simpleAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
