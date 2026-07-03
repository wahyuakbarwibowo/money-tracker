package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveBudgetUseCaseTest {

    /** In-memory fake; only the methods SaveBudgetUseCase touches are real. */
    private class FakeBudgetRepository(
        private val existing: Budget? = null
    ) : BudgetRepository {
        var inserted: Budget? = null
        var updated: Budget? = null

        override suspend fun getBudgetByCategory(category: String, month: String): Budget? = existing
        override suspend fun insertBudget(budget: Budget): Long {
            inserted = budget
            return 99L
        }
        override suspend fun updateBudget(budget: Budget) { updated = budget }

        override fun getAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())
        override fun getBudgetsByMonth(month: String): Flow<List<Budget>> = flowOf(emptyList())
        override suspend fun getBudgetById(id: Long): Budget? = null
        override suspend fun deleteBudget(id: Long) {}
        override suspend fun updateBudgetSpent(category: String, month: String, spent: Double) {}
        override suspend fun getTotalBudgetForMonth(month: String): Double = 0.0
        override suspend fun getTotalSpentForMonth(month: String): Double = 0.0
        override suspend fun getBudgetsPage(month: String, limit: Int, offset: Int): List<Budget> = emptyList()
        override suspend fun getBudgetsCount(month: String): Int = 0
    }

    private val newBudget = Budget(category = "Food", monthlyBudget = 1000.0, month = "2026-06")

    @Test
    fun insertsWhenNoExistingBudget() = runBlocking {
        val repo = FakeBudgetRepository(existing = null)
        val id = SaveBudgetUseCase(repo).invoke(newBudget)

        assertEquals(99L, id)
        assertEquals(newBudget, repo.inserted)
        assertNull(repo.updated)
    }

    @Test
    fun updatesAndKeepsExistingId_whenBudgetAlreadyExists() = runBlocking {
        val existing = newBudget.copy(id = 7L, monthlyBudget = 500.0)
        val repo = FakeBudgetRepository(existing = existing)

        val id = SaveBudgetUseCase(repo).invoke(newBudget)

        assertEquals(7L, id)
        assertEquals(7L, repo.updated?.id)
        assertEquals(1000.0, repo.updated?.monthlyBudget!!, 0.0001)
        assertNull(repo.inserted)
    }
}
