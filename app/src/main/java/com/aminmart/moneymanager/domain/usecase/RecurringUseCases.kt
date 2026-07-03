package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.RecurringRule
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.data.repository.RecurringRepositoryImpl
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class RecurringUseCases(
    val getRules: GetRecurringRulesUseCase,
    val addRule: AddRecurringRuleUseCase,
    val deleteRule: DeleteRecurringRuleUseCase,
    val generateDue: GenerateDueTransactionsUseCase
)

class GetRecurringRulesUseCase(private val repository: RecurringRepositoryImpl) {
    operator fun invoke(): Flow<List<RecurringRule>> = repository.getAllRules()
}

class AddRecurringRuleUseCase(private val repository: RecurringRepositoryImpl) {
    suspend operator fun invoke(rule: RecurringRule): Long = repository.insertRule(rule)
}

class DeleteRecurringRuleUseCase(private val repository: RecurringRepositoryImpl) {
    suspend operator fun invoke(id: Long) = repository.deleteRule(id)
}

/**
 * Materializes transactions for every rule whose [RecurringRule.nextRun] is due,
 * advancing each rule's next run forward until it is in the future. Safe to call
 * repeatedly (e.g. on app start and from the daily worker); it catches up on any
 * missed occurrences. Returns the number of transactions created.
 */
class GenerateDueTransactionsUseCase(
    private val recurringRepository: RecurringRepositoryImpl,
    private val addTransactionUseCase: AddTransactionUseCase
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): Int {
        var created = 0
        for (rule in recurringRepository.getDueRules(now)) {
            var nextRun = rule.nextRun
            // Catch up on every occurrence that is due (cap to avoid runaway loops).
            var guard = 0
            while (nextRun <= now && guard < MAX_CATCH_UP) {
                addTransactionUseCase(
                    Transaction(
                        type = rule.type,
                        amount = rule.amount,
                        category = rule.category,
                        description = rule.description,
                        date = nextRun,
                        isRiba = rule.isRiba
                    )
                )
                created++
                nextRun = advance(nextRun, rule.frequency, rule.intervalCount)
                guard++
            }
            recurringRepository.updateRule(rule.copy(nextRun = nextRun))
        }
        return created
    }

    private fun advance(from: Long, frequency: RecurringRule.Frequency, interval: Int): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = from }
        val field = when (frequency) {
            RecurringRule.Frequency.DAILY -> Calendar.DAY_OF_MONTH
            RecurringRule.Frequency.WEEKLY -> Calendar.WEEK_OF_YEAR
            RecurringRule.Frequency.MONTHLY -> Calendar.MONTH
            RecurringRule.Frequency.YEARLY -> Calendar.YEAR
        }
        calendar.add(field, interval)
        return calendar.timeInMillis
    }

    companion object {
        private const val MAX_CATCH_UP = 366
    }
}
