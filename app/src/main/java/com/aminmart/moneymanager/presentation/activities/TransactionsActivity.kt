package com.aminmart.moneymanager.presentation.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.adapters.TransactionAdapter
import com.aminmart.moneymanager.presentation.viewmodels.TransactionsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * Transactions Activity - List all transactions with filtering
 */
class TransactionsActivity : BottomNavigationActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: TransactionsViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerTransactions: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var viewEmpty: View
    private lateinit var textFilterInfo: TextView

    private lateinit var adapter: TransactionAdapter

    private val addTransactionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadInitialTransactions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        app = application as MoneyManagerApplication
        viewModel = TransactionsViewModel(
            app.getTransactionsPageUseCase,
            app.getTransactionsCountUseCase,
            app.addTransactionUseCase,
            app.updateTransactionUseCase,
            app.deleteTransactionUseCase,
            app.getTransactionByIdUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()

        fabAdd.setOnClickListener {
            navigateToAddTransaction()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_transactions)
        recyclerTransactions = findViewById(R.id.recycler_transactions)
        fabAdd = findViewById(R.id.fab_transactions_add)
        viewEmpty = findViewById(R.id.view_transactions_empty)
        textFilterInfo = findViewById(R.id.text_transactions_filter_info)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Transactions"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.navigationIcon = null
        setupBottomNavigation(R.id.nav_transactions)
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionOptions(transaction)
            },
            onItemLongClick = { transaction ->
                showTransactionOptions(transaction)
            }
        )
        recyclerTransactions.layoutManager = LinearLayoutManager(this)
        recyclerTransactions.adapter = adapter
        recyclerTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        viewModel.transactions.collectInScope { transactions ->
            adapter.submitList(transactions)
            viewEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            updateFilterInfo()
        }
    }

    private fun updateFilterInfo() {
        val type = viewModel.filterType.value
        val category = viewModel.filterCategory.value
        
        val filters = mutableListOf<String>()
        type?.let { filters.add(it.name) }
        category?.let { filters.add(it) }
        
        textFilterInfo.text = if (filters.isEmpty()) {
            ""
        } else {
            "Filtered: ${filters.joinToString(", ")}"
        }
        textFilterInfo.visibility = if (filters.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showTransactionOptions(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(transaction.category)
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
                activityScope.launch {
                    viewModel.deleteTransaction(transaction.id)
                    viewModel.loadInitialTransactions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToAddTransaction(transaction: Transaction? = null) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        transaction?.let {
            intent.putExtra("transaction_id", it.id)
        }
        addTransactionLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_transactions, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_transactions_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_clear_filter -> {
                viewModel.clearFilters()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_filter_transactions, null)
        val spinnerType = view.findViewById<Spinner>(R.id.spinner_filter_type)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinner_filter_category)

        // Setup Type Spinner
        val typeOptions = listOf("All") + Transaction.TransactionType.values().map { it.name }
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        viewModel.filterType.value?.let { currentType ->
            spinnerType.setSelection(typeOptions.indexOf(currentType.name))
        } ?: spinnerType.setSelection(0) // "All" is at index 0

        // Setup Category Spinner
        val categories = listOf("All Categories") + viewModel.getCategories()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        viewModel.filterCategory.value?.let { currentCategory ->
            spinnerCategory.setSelection(categories.indexOf(currentCategory))
        } ?: spinnerCategory.setSelection(0) // "All Categories" is at index 0

        AlertDialog.Builder(this)
            .setTitle("Filter Transactions")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val selectedType = if (spinnerType.selectedItemPosition > 0) {
                    Transaction.TransactionType.valueOf(typeOptions[spinnerType.selectedItemPosition])
                } else {
                    null
                }
                val selectedCategory = if (spinnerCategory.selectedItemPosition > 0) {
                    categories[spinnerCategory.selectedItemPosition]
                } else {
                    null
                }
                viewModel.setFilterType(selectedType)
                viewModel.setFilterCategory(selectedCategory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadInitialTransactions()
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            viewModel.clear()
        }
        super.onDestroy()
    }
}
