package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to get all transactions
 */
class GetAllTransactionsUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAllTransactions()
}

/**
 * Use case to get recent transactions
 */
class GetRecentTransactionsUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<Transaction>> = 
        repository.getRecentTransactions(limit)
}

/**
 * Use case to get transactions by month
 */
class GetTransactionsByMonthUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<List<Transaction>> = 
        repository.getTransactionsByMonth(year, month)
}

/**
 * Use case to get transactions by category
 */
class GetTransactionsByCategoryUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(category: String): Flow<List<Transaction>> = 
        repository.getTransactionsByCategory(category)
}

class GetTransactionsPageUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(
        limit: Int,
        offset: Int,
        type: Transaction.TransactionType? = null,
        category: String? = null,
        query: String? = null
    ): List<Transaction> = repository.getTransactionsPage(limit, offset, type, category, query)
}

class GetTransactionsCountUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(
        type: Transaction.TransactionType? = null,
        category: String? = null,
        query: String? = null
    ): Int = repository.getTransactionsCount(type, category, query)
}
