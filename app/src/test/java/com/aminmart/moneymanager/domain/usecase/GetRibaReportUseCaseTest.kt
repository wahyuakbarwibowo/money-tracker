package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetRibaReportUseCaseTest {

    /** Fake that only serves getAllTransactions; unused methods are never called. */
    private class FakeTransactionRepository(
        private val all: List<Transaction>
    ) : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(all)

        override fun getTransactionsByType(type: Transaction.TransactionType) = flowOf(emptyList<Transaction>())
        override fun getTransactionsByMonth(year: Int, month: Int) = flowOf(emptyList<Transaction>())
        override fun getTransactionsByCategory(category: String) = flowOf(emptyList<Transaction>())
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long) = flowOf(emptyList<Transaction>())
        override fun getRecentTransactions(limit: Int) = flowOf(emptyList<Transaction>())
        override suspend fun getTransactionsPage(limit: Int, offset: Int, type: Transaction.TransactionType?, category: String?) = emptyList<Transaction>()
        override suspend fun getTransactionsCount(type: Transaction.TransactionType?, category: String?) = 0
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun insertTransaction(transaction: Transaction): Long = 0
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(id: Long) {}
        override suspend fun deleteAllTransactions() {}
        override suspend fun getTotalIncome(startDate: Long, endDate: Long) = 0.0
        override suspend fun getTotalExpense(startDate: Long, endDate: Long) = 0.0
        override suspend fun getExpenseByCategory(startDate: Long, endDate: Long) = emptyMap<String, Double>()
        override suspend fun getMonthlyExpenses(months: Int) = emptyMap<String, Double>()
        override suspend fun transactionExists(date: Long, amount: Double, description: String) = false
        override suspend fun insertTransactions(transactions: List<Transaction>) = emptyList<Long>()
    }

    private fun tx(amount: Double, isRiba: Boolean) = Transaction(
        type = Transaction.TransactionType.INCOME,
        amount = amount,
        category = "Interest",
        description = "",
        date = 0L,
        isRiba = isRiba
    )

    @Test
    fun sumsAndCountsOnlyRibaTransactions() = runBlocking {
        val repo = FakeTransactionRepository(
            listOf(tx(100.0, true), tx(50.0, false), tx(25.0, true))
        )
        val report = GetRibaReportUseCase(repo).invoke().first()

        assertEquals(2, report.count)
        assertEquals(125.0, report.totalAmount, 0.0001)
        assertEquals(2, report.transactions.size)
    }

    @Test
    fun emptyReport_whenNoRibaFlagged() = runBlocking {
        val repo = FakeTransactionRepository(listOf(tx(100.0, false)))
        val report = GetRibaReportUseCase(repo).invoke().first()

        assertEquals(0, report.count)
        assertEquals(0.0, report.totalAmount, 0.0001)
    }
}
