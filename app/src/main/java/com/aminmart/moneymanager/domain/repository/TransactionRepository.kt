package com.aminmart.moneymanager.domain.repository

import com.aminmart.moneymanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Transaction operations
 * Defines the contract for data operations
 */
interface TransactionRepository {
    
    /**
     * Get all transactions as Flow
     */
    fun getAllTransactions(): Flow<List<Transaction>>
    
    /**
     * Get transactions by type
     */
    fun getTransactionsByType(type: Transaction.TransactionType): Flow<List<Transaction>>
    
    /**
     * Get transactions for a specific month
     */
    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>>
    
    /**
     * Get transactions by category
     */
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    
    /**
     * Get transactions by date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    /**
     * Get recent transactions
     */
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>
    suspend fun getTransactionsPage(
        limit: Int,
        offset: Int,
        type: Transaction.TransactionType? = null,
        category: String? = null,
        query: String? = null
    ): List<Transaction>
    suspend fun getTransactionsCount(
        type: Transaction.TransactionType? = null,
        category: String? = null,
        query: String? = null
    ): Int
    
    /**
     * Get transaction by ID
     */
    suspend fun getTransactionById(id: Long): Transaction?
    
    /**
     * Insert a new transaction
     */
    suspend fun insertTransaction(transaction: Transaction): Long
    
    /**
     * Update an existing transaction
     */
    suspend fun updateTransaction(transaction: Transaction)
    
    /**
     * Delete a transaction
     */
    suspend fun deleteTransaction(id: Long)
    
    /**
     * Delete all transactions
     */
    suspend fun deleteAllTransactions()
    
    /**
     * Get total income for a date range
     */
    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double
    
    /**
     * Get total expense for a date range
     */
    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double
    
    /**
     * Get expense by category for a date range
     */
    suspend fun getExpenseByCategory(startDate: Long, endDate: Long): Map<String, Double>
    
    /**
     * Get monthly expense totals
     */
    suspend fun getMonthlyExpenses(months: Int = 12): Map<String, Double>
    
    /**
     * Check if transaction exists (for duplicate detection)
     */
    suspend fun transactionExists(date: Long, amount: Double, description: String): Boolean
    
    /**
     * Insert multiple transactions (batch)
     */
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>
}
