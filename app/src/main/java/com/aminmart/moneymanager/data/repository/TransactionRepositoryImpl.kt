package com.aminmart.moneymanager.data.repository

import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of TransactionRepository using SQLite database
 */
class TransactionRepositoryImpl(
    private val database: MoneyDatabase
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        database.getAllTransactions()

    override fun getTransactionsByType(type: Transaction.TransactionType): Flow<List<Transaction>> =
        database.getTransactionsByType(type)

    override fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> =
        database.getTransactionsByMonth(year, month)

    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        database.getTransactionsByCategory(category)

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        database.getTransactionsByDateRange(startDate, endDate)

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        database.getRecentTransactions(limit)

    override suspend fun getTransactionsPage(
        limit: Int,
        offset: Int,
        type: Transaction.TransactionType?,
        category: String?,
        query: String?
    ): List<Transaction> =
        database.getTransactionsPage(limit, offset, type, category, query)

    override suspend fun getTransactionsCount(
        type: Transaction.TransactionType?,
        category: String?,
        query: String?
    ): Int =
        database.getTransactionsCount(type, category, query)

    override suspend fun getTransactionById(id: Long): Transaction? =
        database.getTransactionById(id)

    override suspend fun insertTransaction(transaction: Transaction): Long =
        database.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: Transaction) =
        database.updateTransaction(transaction)

    override suspend fun deleteTransaction(id: Long) =
        database.deleteTransaction(id)

    override suspend fun deleteAllTransactions() =
        database.deleteAllTransactions()

    override suspend fun getTotalIncome(startDate: Long, endDate: Long): Double =
        database.getTotalIncome(startDate, endDate)

    override suspend fun getTotalExpense(startDate: Long, endDate: Long): Double =
        database.getTotalExpense(startDate, endDate)

    override suspend fun getExpenseByCategory(startDate: Long, endDate: Long): Map<String, Double> =
        database.getExpenseByCategory(startDate, endDate)

    override suspend fun getMonthlyExpenses(months: Int): Map<String, Double> =
        database.getMonthlyExpenses(months)

    override suspend fun transactionExists(date: Long, amount: Double, description: String): Boolean =
        database.transactionExists(date, amount, description)

    override suspend fun insertTransactions(transactions: List<Transaction>): List<Long> =
        database.insertTransactions(transactions)
}
