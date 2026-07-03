package com.aminmart.moneymanager.presentation.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.DashboardStats
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.adapters.TransactionAdapter
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import com.aminmart.moneymanager.presentation.viewmodels.DashboardViewModel
import com.aminmart.moneymanager.presentation.viewmodels.DebtSummary
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

/**
 * Dashboard Activity - Main screen showing summary and recent transactions
 */
class DashboardActivity : BottomNavigationActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: DashboardViewModel

    private lateinit var textTotalBalance: TextView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textMonthIncome: TextView
    private lateinit var textMonthExpense: TextView
    private lateinit var textTotalDebt: TextView
    private lateinit var textTotalCredit: TextView
    private lateinit var recyclerRecent: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var viewEmpty: View
    private lateinit var cardDebt: MaterialCardView

    private lateinit var adapter: TransactionAdapter

    private val addTransactionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadDashboardData() // Reload data if transaction was added/edited
        }
    }

    private val currencyFormat = CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        app = application as MoneyManagerApplication
        viewModel = DashboardViewModel(
            app.getDashboardStatsUseCase,
            app.getRecentTransactionsUseCase,
            app.debtUseCases,
            app.deleteTransactionUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()

        fabAdd.setOnClickListener {
            navigateToAddTransaction()
        }
    }

    private fun initViews() {
        textTotalBalance = findViewById(R.id.text_dashboard_total_balance)
        textTotalIncome = findViewById(R.id.text_dashboard_total_income)
        textTotalExpense = findViewById(R.id.text_dashboard_total_expense)
        textMonthIncome = findViewById(R.id.text_dashboard_month_income)
        textMonthExpense = findViewById(R.id.text_dashboard_month_expense)
        textTotalDebt = findViewById(R.id.text_dashboard_total_debt)
        textTotalCredit = findViewById(R.id.text_dashboard_total_credit)
        recyclerRecent = findViewById(R.id.recycler_dashboard_recent)
        fabAdd = findViewById(R.id.fab_dashboard_add)
        viewEmpty = findViewById(R.id.view_dashboard_empty)
        cardDebt = findViewById(R.id.card_dashboard_debt)
        setupBottomNavigation(R.id.nav_dashboard)

        cardDebt.setOnClickListener {
            startActivity(Intent(this, DebtActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetail(transaction)
            },
            onItemLongClick = { transaction ->
                showTransactionOptions(transaction)
            }
        )
        recyclerRecent.layoutManager = LinearLayoutManager(this)
        recyclerRecent.adapter = adapter
    }

    private fun observeData() {
        viewModel.stats.collectInScope { stats ->
            stats?.let { updateStats(it) }
        }

        viewModel.recentTransactions.collectInScope { transactions ->
            adapter.submitList(transactions)
            viewEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.debtSummary.collectInScope { summary ->
            updateDebtSummary(summary)
        }
    }

    private fun updateStats(stats: DashboardStats) {
        textTotalBalance.text = currencyFormat.format(stats.totalBalance)
        textTotalIncome.text = "+${currencyFormat.format(stats.totalIncome)}"
        textTotalExpense.text = "-${currencyFormat.format(stats.totalExpense)}"
        textMonthIncome.text = "+${currencyFormat.format(stats.monthIncome)}"
        textMonthExpense.text = "-${currencyFormat.format(stats.monthExpense)}"

        // Set colors
        textTotalBalance.setTextColor(
            getColor(if (stats.totalBalance >= 0) R.color.income_green else R.color.expense_red)
        )
    }

    private fun updateDebtSummary(summary: DebtSummary) {
        textTotalDebt.text = "-${currencyFormat.format(summary.totalDebt)}"
        textTotalCredit.text = "+${currencyFormat.format(summary.totalCredit)}"
    }

    private fun showTransactionDetail(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle(transaction.category)
            .setMessage(buildTransactionDetail(transaction))
            .setPositiveButton("Edit") { _, _ ->
                navigateToAddTransaction(transaction)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showTransactionOptions(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Transaction Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToAddTransaction(transaction)
                    1 -> confirmDelete(transaction)
                }
            }
            .show()
    }

    private fun confirmDelete(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
                viewModel.loadDashboardData() // Reload data after deletion
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildTransactionDetail(transaction: Transaction): String {
        val amountText = if (transaction.type == Transaction.TransactionType.INCOME) {
            "+${currencyFormat.format(transaction.amount)}"
        } else {
            "-${currencyFormat.format(transaction.amount)}"
        }
        
        val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        val dateStr = dateFormat.format(java.util.Date(transaction.date))

        return """
            Amount: $amountText
            Category: ${transaction.category}
            Date: $dateStr
            Description: ${transaction.description.ifEmpty { "-" }}
        """.trimIndent()
    }

    private fun navigateToAddTransaction(transaction: Transaction? = null) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        transaction?.let {
            intent.putExtra("transaction_id", it.id)
        }
        addTransactionLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            viewModel.clear()
        }
        super.onDestroy()
    }
}
