package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Category
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.AddTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.GetTransactionByIdUseCase
import com.aminmart.moneymanager.domain.usecase.UpdateTransactionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Add/Edit Transaction screen
 */
class AddTransactionViewModel(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()
    private val _transactionState = MutableStateFlow(
        Transaction(
            type = Transaction.TransactionType.EXPENSE,
            amount = 0.0,
            category = Category.DEFAULT_EXPENSE_CATEGORIES.first().name,
            description = "",
            date = System.currentTimeMillis()
        )
    )
    val transactionState: StateFlow<Transaction> = _transactionState.asStateFlow()
    private var incomeCategories = Category.DEFAULT_INCOME_CATEGORIES.map { it.name }
    private var expenseCategories = Category.DEFAULT_EXPENSE_CATEGORIES.map { it.name }

    val currentCategories: List<String>
        get() = if (_transactionState.value.type == Transaction.TransactionType.INCOME) {
            incomeCategories
        } else {
            expenseCategories
        }

    fun setTransactionType(type: Transaction.TransactionType) {
        val newCategory = if (type == Transaction.TransactionType.INCOME) {
            incomeCategories.firstOrNull()
        } else {
            expenseCategories.firstOrNull()
        }
        _transactionState.value = _transactionState.value.copy(
            type = type,
            category = newCategory ?: ""
        )
    }

    fun updateCategories(income: List<String>, expense: List<String>) {
        incomeCategories = income.ifEmpty { Category.DEFAULT_INCOME_CATEGORIES.map { it.name } }
        expenseCategories = expense.ifEmpty { Category.DEFAULT_EXPENSE_CATEGORIES.map { it.name } }

        val current = _transactionState.value
        val validCategories = if (current.type == Transaction.TransactionType.INCOME) {
            incomeCategories
        } else {
            expenseCategories
        }

        if (current.category !in validCategories) {
            _transactionState.value = current.copy(category = validCategories.firstOrNull().orEmpty())
        }
    }

    fun setAmount(amount: Double) {
        _transactionState.value = _transactionState.value.copy(amount = amount)
    }

    fun setCategory(category: String) {
        _transactionState.value = _transactionState.value.copy(category = category)
    }

    fun setDescription(description: String) {
        _transactionState.value = _transactionState.value.copy(description = description)
    }

    fun setDate(date: Long) {
        _transactionState.value = _transactionState.value.copy(date = date)
    }

    fun setRiba(isRiba: Boolean) {
        _transactionState.value = _transactionState.value.copy(isRiba = isRiba)
    }

    fun setAccountId(accountId: Long) {
        _transactionState.value = _transactionState.value.copy(accountId = accountId)
    }

    fun editTransaction(transactionId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val transaction = withContext(Dispatchers.IO) {
                    getTransactionByIdUseCase(transactionId)
                }
                transaction?.let {
                    _transactionState.value = it
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    suspend fun saveTransaction() {
        val transaction = _transactionState.value
        if (transaction.amount <= 0) {
            _uiState.value = _uiState.value.copy(error = "Amount must be greater than zero.")
            return
        }
        if (transaction.category.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Category cannot be empty.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            withContext(Dispatchers.IO) {
                if (transaction.id == 0L) {
                    addTransactionUseCase(transaction)
                } else {
                    updateTransactionUseCase(transaction)
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Transaction saved!")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
        }
    }
}

/**
 * UI State for Add Transaction
 */
data class AddTransactionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
