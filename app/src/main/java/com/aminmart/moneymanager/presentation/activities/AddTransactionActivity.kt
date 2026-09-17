package com.aminmart.moneymanager.presentation.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.AdapterView
import java.util.Date
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.data.local.CategoryStore
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.ui.formatWholeAmount
import com.aminmart.moneymanager.presentation.ui.parseWholeAmount
import com.aminmart.moneymanager.presentation.viewmodels.AddTransactionViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Add/Edit Transaction Activity
 */
class AddTransactionActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: AddTransactionViewModel
    private lateinit var categoryStore: CategoryStore

    private lateinit var toolbar: Toolbar
    private lateinit var radioType: RadioGroup
    private lateinit var radioIncome: RadioButton
    private lateinit var radioExpense: RadioButton
    private lateinit var editAmount: TextInputEditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var editDescription: TextInputEditText
    private lateinit var buttonDate: Button
    private lateinit var ribaSwitch: SwitchMaterial
    private lateinit var buttonSave: Button

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    private var transactionId: Long = 0L
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isBindingAmount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        app = application as MoneyManagerApplication
        viewModel = AddTransactionViewModel(
            app.getTransactionByIdUseCase,
            app.addTransactionUseCase,
            app.updateTransactionUseCase
        )
        categoryStore = CategoryStore(this)
        refreshCategories()

        transactionId = intent.getLongExtra("transaction_id", 0L)
        if (transactionId != 0L) {
            viewModel.editTransaction(transactionId)
        }

        initViews()
        setupListeners()
        observeData()
        setupAccountSpinner()
    }

    private var accounts: List<com.aminmart.moneymanager.domain.model.Account> = emptyList()

    private fun setupAccountSpinner() {
        val spinner = findViewById<android.widget.Spinner>(R.id.spinner_transaction_account)
        activityScope.launch {
            accounts = withContext(Dispatchers.IO) {
                app.accountUseCases.getAccounts().first()
            }
            if (accounts.isEmpty()) return@launch

            spinner.adapter = android.widget.ArrayAdapter(
                this@AddTransactionActivity,
                android.R.layout.simple_spinner_dropdown_item,
                accounts.map { it.name }
            )
            // Reflect the transaction's current account (default or the edited one).
            val currentId = viewModel.transactionState.value.accountId
            accounts.indexOfFirst { it.id == currentId }.takeIf { it >= 0 }?.let {
                spinner.setSelection(it)
            }
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    viewModel.setAccountId(accounts[position].id)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun refreshCategories() {
        viewModel.updateCategories(
            income = categoryStore.getIncomeCategories(),
            expense = categoryStore.getExpenseCategories()
        )
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_add_transaction)
        radioType = findViewById(R.id.radio_transaction_type)
        radioIncome = findViewById(R.id.radio_income)
        radioExpense = findViewById(R.id.radio_expense)
        editAmount = findViewById(R.id.edit_transaction_amount)
        categoryDropdown = findViewById(R.id.dropdown_transaction_category)
        editDescription = findViewById(R.id.edit_transaction_description)
        buttonDate = findViewById(R.id.button_transaction_date)
        ribaSwitch = findViewById(R.id.switch_transaction_riba)
        buttonSave = findViewById(R.id.button_transaction_save)

        setSupportActionBar(toolbar)
        supportActionBar?.title = if (transactionId == 0L) "Add Transaction" else "Edit Transaction"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListeners() {
        radioType.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.radio_income) {
                Transaction.TransactionType.INCOME
            } else {
                Transaction.TransactionType.EXPENSE
            }
            viewModel.setTransactionType(type)
        }

        editAmount.doOnTextChanged { text, _, _, _ ->
            if (!isBindingAmount) {
                viewModel.setAmount(parseWholeAmount(text) ?: 0.0)
            }
        }

        categoryDropdown.doOnTextChanged { text, _, _, _ ->
            viewModel.setCategory(text?.toString().orEmpty())
        }

        editDescription.doOnTextChanged { text, _, _, _ ->
            viewModel.setDescription(text.toString())
        }

        ribaSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setRiba(isChecked)
        }

        buttonDate.setOnClickListener { showDateTimePicker() }

        buttonSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun observeData() {
        viewModel.transactionState.collectInScope { transaction ->
            renderAmount(transaction.amount)
            if (editDescription.text.toString() != transaction.description) {
                editDescription.setText(transaction.description)
            }
            if (ribaSwitch.isChecked != transaction.isRiba) {
                ribaSwitch.isChecked = transaction.isRiba
            }
            if (transaction.type == Transaction.TransactionType.INCOME) {
                if (!radioIncome.isChecked) radioIncome.isChecked = true
            } else {
                if (!radioExpense.isChecked) radioExpense.isChecked = true
            }
            buttonDate.text = dateFormat.format(Date(transaction.date))
            updateCategoryDropdown(transaction.category)
        }

        viewModel.uiState.collectInScope { state ->
            buttonSave.isEnabled = !state.isLoading
            if (state.successMessage != null) {
                showToast(state.successMessage)
                setResult(RESULT_OK)
                finish()
            }
            if (state.error != null) {
                showToast(state.error, isError = true)
            }
        }
    }

    private fun renderAmount(amount: Double) {
        if (editAmount.isFocused) {
            return
        }

        val formattedAmount = formatWholeAmount(amount)
        if (editAmount.text?.toString() == formattedAmount) {
            return
        }

        isBindingAmount = true
        editAmount.setText(formattedAmount)
        editAmount.setSelection(formattedAmount.length)
        isBindingAmount = false
    }

    private fun updateCategoryDropdown(selectedCategory: String) {
        val categories = viewModel.currentCategories
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categoryDropdown.setAdapter(adapter)

        if (categoryDropdown.text.toString() != selectedCategory) {
            categoryDropdown.setText(selectedCategory, false)
        }

        categoryDropdown.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            viewModel.setCategory(categories[position])
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = viewModel.transactionState.value.date
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                showTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(calendar: Calendar) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                viewModel.setDate(calendar.timeInMillis)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveTransaction() {
        activityScope.launch {
            viewModel.saveTransaction()
        }
    }

    private fun showToast(message: String, isError: Boolean = false) {
        Toast.makeText(this, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refreshCategories()
    }

    private fun <T> Flow<T>.collectInScope(action: suspend (T) -> Unit) {
        activityScope.launch {
            collect { action(it) }
        }
    }
}
