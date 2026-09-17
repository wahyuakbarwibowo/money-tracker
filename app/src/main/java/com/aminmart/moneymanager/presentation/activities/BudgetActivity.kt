package com.aminmart.moneymanager.presentation.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.data.local.CategoryStore
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.presentation.adapters.BudgetAdapter
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import com.aminmart.moneymanager.presentation.ui.formatWholeAmount
import com.aminmart.moneymanager.presentation.ui.parseWholeAmount
import com.aminmart.moneymanager.presentation.viewmodels.BudgetViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Locale
import java.util.Calendar
import kotlinx.coroutines.launch

/**
 * Budget Activity - Manage monthly budgets
 */
class BudgetActivity : BottomNavigationActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: BudgetViewModel
    private lateinit var categoryStore: CategoryStore

    private lateinit var toolbar: Toolbar
    private lateinit var textMonth: TextView
    private lateinit var progressTotal: LinearProgressIndicator
    private lateinit var textTotalBudget: TextView
    private lateinit var textTotalSpent: TextView
    private lateinit var textRemaining: TextView
    private lateinit var recyclerBudgets: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var viewEmpty: View

    private lateinit var adapter: BudgetAdapter
    private val currencyFormat = CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        app = application as MoneyManagerApplication
        viewModel = BudgetViewModel(
            app.getBudgetsPageUseCase,
            app.getBudgetsCountUseCase,
            app.saveBudgetUseCase,
            app.deleteBudgetUseCase,
            app.getBudgetByCategoryUseCase
        )
        categoryStore = CategoryStore(this)

        initViews()
        setupRecyclerView()
        observeData()

        fabAdd.setOnClickListener {
            showAddBudgetDialog()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_budget)
        textMonth = findViewById(R.id.text_budget_month)
        progressTotal = findViewById(R.id.progress_budget_total)
        textTotalBudget = findViewById(R.id.text_budget_total_amount)
        textTotalSpent = findViewById(R.id.text_budget_total_spent)
        textRemaining = findViewById(R.id.text_budget_total_remaining)
        recyclerBudgets = findViewById(R.id.recycler_budgets)
        fabAdd = findViewById(R.id.fab_budget_add)
        viewEmpty = findViewById(R.id.view_budget_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Budget Planner"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.navigationIcon = null
        setupBottomNavigation(R.id.nav_budget)

        findViewById<View>(R.id.button_budget_prev_month).setOnClickListener { shiftMonth(-1) }
        findViewById<View>(R.id.button_budget_next_month).setOnClickListener { shiftMonth(1) }
    }

    private fun setupRecyclerView() {
        adapter = BudgetAdapter(
            onItemClick = { budget ->
                showBudgetDetail(budget)
            },
            onEditClick = { budget ->
                showEditBudgetDialog(budget)
            }
        )
        recyclerBudgets.layoutManager = LinearLayoutManager(this)
        recyclerBudgets.adapter = adapter
        recyclerBudgets.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (lm.findLastVisibleItemPosition() >= lm.itemCount - 5 && viewModel.hasMoreData()) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun observeData() {
        viewModel.budgets.collectInScope { budgets ->
            adapter.submitList(budgets)
            viewEmpty.visibility = if (budgets.isEmpty()) View.VISIBLE else View.GONE
            updateSummary()
        }

        viewModel.currentMonth.collectInScope { month ->
            textMonth.text = formatMonth(month)
        }
    }

    private fun updateSummary() {
        textTotalBudget.text = currencyFormat.format(viewModel.getTotalBudget())
        textTotalSpent.text = currencyFormat.format(viewModel.getTotalSpent())
        textRemaining.text = currencyFormat.format(viewModel.getRemainingBudget())
        
        val progress = viewModel.getBudgetProgress()
        progressTotal.progress = (progress * 100).toInt()
        
        // Set color based on progress
        val color = when {
            progress >= 1.0f -> getColor(R.color.expense_red)
            progress >= 0.8f -> getColor(R.color.warning_orange)
            else -> getColor(R.color.income_green)
        }
        progressTotal.setIndicatorColor(color)
    }

    private fun formatMonth(month: String): String {
        return try {
            val parts = month.split("-")
            val year = parts[0]
            val monthNum = parts[1].toInt()
            val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year.toInt())
            calendar.set(Calendar.MONTH, monthNum - 1)
            monthFormat.format(calendar.time)
        } catch (e: Exception) {
            month
        }
    }

    private fun shiftMonth(delta: Int) {
        val (year, month) = viewModel.currentMonth.value.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, 1)
            add(Calendar.MONTH, delta)
        }
        viewModel.setMonth(
            String.format("%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        )
    }

    private fun showAddBudgetDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_budget, null)
        val editCategory = view.findViewById<AutoCompleteTextView>(R.id.dropdown_budget_category)
        val editAmount = view.findViewById<EditText>(R.id.edit_budget_amount)

        // Setup category dropdown
        val categories = categoryStore.getExpenseCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        editCategory.setAdapter(adapter)

        AlertDialog.Builder(this)
            .setTitle("Add Budget")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val category = editCategory.text.toString()
                val amount = parseWholeAmount(editAmount.text)
                
                if (category.isNotEmpty() && amount != null && amount > 0) {
                    val budget = Budget(
                        category = category,
                        monthlyBudget = amount,
                        month = viewModel.currentMonth.value
                    )
                    activityScope.launch {
                        viewModel.saveBudget(budget)
                        viewModel.loadInitialBudgets()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditBudgetDialog(budget: Budget) {
        val view = layoutInflater.inflate(R.layout.dialog_add_budget, null)
        val editCategory = view.findViewById<AutoCompleteTextView>(R.id.dropdown_budget_category)
        val editAmount = view.findViewById<EditText>(R.id.edit_budget_amount)

        editCategory.setText(budget.category, false)
        editAmount.setText(formatWholeAmount(budget.monthlyBudget))

        AlertDialog.Builder(this)
            .setTitle("Edit Budget")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val amount = parseWholeAmount(editAmount.text)
                
                if (amount != null && amount > 0) {
                    val updatedBudget = budget.copy(
                        category = editCategory.text.toString(),
                        monthlyBudget = amount
                    )
                    activityScope.launch {
                        viewModel.saveBudget(updatedBudget)
                        viewModel.loadInitialBudgets()
                    }
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                confirmDelete(budget)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(budget: Budget) {
        AlertDialog.Builder(this)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete this budget?")
            .setPositiveButton("Delete") { _, _ ->
                activityScope.launch {
                    viewModel.deleteBudget(budget.id)
                    viewModel.loadInitialBudgets()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBudgetDetail(budget: Budget) {
        val message = """
            Category: ${budget.category}
            Monthly Budget: ${currencyFormat.format(budget.monthlyBudget)}
            Spent: ${currencyFormat.format(budget.spent)}
            Remaining: ${currencyFormat.format(budget.remaining)}
            Progress: ${(budget.percentageUsed * 100).toInt()}%
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Budget Detail")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_budget, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_rollover -> {
                confirmRollover()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmRollover() {
        AlertDialog.Builder(this)
            .setTitle(R.string.budget_rollover)
            .setMessage(R.string.budget_rollover_confirm)
            .setPositiveButton(R.string.budget_rollover) { _, _ ->
                activityScope.launch {
                    val created = app.rolloverBudgetsUseCase()
                    android.widget.Toast.makeText(
                        this@BudgetActivity,
                        getString(R.string.budget_rollover_done, created),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    viewModel.loadInitialBudgets()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadInitialBudgets()
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            viewModel.clear()
        }
        super.onDestroy()
    }
}
