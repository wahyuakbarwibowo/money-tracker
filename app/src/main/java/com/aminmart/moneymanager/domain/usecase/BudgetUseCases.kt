package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to get all budgets
 */
class GetAllBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> = repository.getAllBudgets()
}

/**
 * Use case to get budgets for current month
 */
class GetCurrentMonthBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> {
        val currentMonth = getCurrentMonth()
        return repository.getBudgetsByMonth(currentMonth)
    }
    
    private fun getCurrentMonth(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }
}

/**
 * Use case to add/update budget
 */
class SaveBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Long {
        val existing = repository.getBudgetByCategory(budget.category, budget.month)
        return if (existing != null) {
            val updated = budget.copy(id = existing.id)
            repository.updateBudget(updated)
            existing.id
        } else {
            repository.insertBudget(budget)
        }
    }
}

/**
 * Use case to delete budget
 */
class DeleteBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteBudget(id)
    }
}

/**
 * Use case to get budget by category
 */
class GetBudgetByCategoryUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(category: String, month: String): Budget? {
        return repository.getBudgetByCategory(category, month)
    }
}

class GetBudgetsPageUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(month: String, limit: Int, offset: Int): List<Budget> {
        return repository.getBudgetsPage(month, limit, offset)
    }
}

class GetBudgetsCountUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(month: String): Int {
        return repository.getBudgetsCount(month)
    }
}

/**
 * Carries last month's budgets into the current month. For each previous-month
 * budget that has no current-month counterpart, a fresh budget is created whose
 * allowance is the base plus any leftover (remaining) from last month — unused
 * budget rolls over. Returns the number of budgets created.
 */
class RolloverBudgetsUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(): Int {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = monthString(calendar)
        calendar.add(java.util.Calendar.MONTH, -1)
        val previousMonth = monthString(calendar)

        val previousBudgets = repository.getBudgetsPage(previousMonth, MAX_BUDGETS, 0)
        var created = 0
        for (prev in previousBudgets) {
            val existing = repository.getBudgetByCategory(prev.category, currentMonth)
            if (existing != null) continue

            val rolledAllowance = (prev.monthlyBudget + prev.remaining).coerceAtLeast(0.0)
            repository.insertBudget(
                Budget(
                    category = prev.category,
                    monthlyBudget = rolledAllowance,
                    month = currentMonth,
                    spent = 0.0
                )
            )
            created++
        }
        return created
    }

    private fun monthString(calendar: java.util.Calendar): String {
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    companion object {
        private const val MAX_BUDGETS = 1000
    }
}
