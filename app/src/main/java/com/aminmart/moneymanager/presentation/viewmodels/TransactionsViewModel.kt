package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.AddTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.DeleteTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.GetTransactionsCountUseCase
import com.aminmart.moneymanager.domain.usecase.GetTransactionsPageUseCase
import com.aminmart.moneymanager.domain.usecase.GetTransactionByIdUseCase
import com.aminmart.moneymanager.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Transactions screen
 */
class TransactionsViewModel(
    private val getTransactionsPageUseCase: GetTransactionsPageUseCase,
    private val getTransactionsCountUseCase: GetTransactionsCountUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var currentOffset = 0
    private var totalCount = 0
    private val pageSize = 30

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _filterType = MutableStateFlow<Transaction.TransactionType?>(null)
    val filterType: StateFlow<Transaction.TransactionType?> = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory: StateFlow<String?> = _filterCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    init {
        loadInitialTransactions()
    }

    fun loadInitialTransactions() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                currentOffset = 0
                totalCount = withContext(Dispatchers.IO) {
                    getTransactionsCountUseCase(_filterType.value, _filterCategory.value, _searchQuery.value)
                }
                val firstPage = withContext(Dispatchers.IO) {
                    getTransactionsPageUseCase(
                        limit = pageSize,
                        offset = 0,
                        type = _filterType.value,
                        category = _filterCategory.value,
                        query = _searchQuery.value
                    )
                }
                _transactions.value = firstPage
                currentOffset = firstPage.size
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load transactions"
                )
            }
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading || currentOffset >= totalCount) return
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val page = withContext(Dispatchers.IO) {
                    getTransactionsPageUseCase(
                        limit = pageSize,
                        offset = currentOffset,
                        type = _filterType.value,
                        category = _filterCategory.value,
                        query = _searchQuery.value
                    )
                }
                if (page.isNotEmpty()) {
                    _transactions.value = _transactions.value + page
                    currentOffset += page.size
                }
                _uiState.value = _uiState.value.copy(isLoadingMore = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load more transactions"
                )
            }
        }
    }

    fun hasMoreData(): Boolean = currentOffset < totalCount

    fun setFilterType(type: Transaction.TransactionType?) {
        _filterType.value = type
        loadInitialTransactions()
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
        loadInitialTransactions()
    }

    fun setSearchQuery(query: String?) {
        val normalized = query?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == _searchQuery.value) return
        _searchQuery.value = normalized
        loadInitialTransactions()
    }

    fun clearFilters() {
        _filterType.value = null
        _filterCategory.value = null
        _searchQuery.value = null
        loadInitialTransactions()
    }

    suspend fun addTransaction(transaction: Transaction): Long {
        return withContext(Dispatchers.IO) { addTransactionUseCase(transaction) }
    }

    suspend fun updateTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) { updateTransactionUseCase(transaction) }
    }

    suspend fun deleteTransaction(id: Long) {
        withContext(Dispatchers.IO) { deleteTransactionUseCase(id) }
    }

    suspend fun getTransaction(id: Long): Transaction? {
        return withContext(Dispatchers.IO) { getTransactionByIdUseCase(id) }
    }

    fun getCategories(): List<String> {
        return _transactions.value.map { it.category }.distinct()
    }

    fun clear() {
        scope.cancel()
    }
}

/**
 * UI State for Transactions
 */
data class TransactionsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
