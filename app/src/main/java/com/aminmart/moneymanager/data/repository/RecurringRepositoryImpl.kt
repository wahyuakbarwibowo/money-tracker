package com.aminmart.moneymanager.data.repository

import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

class RecurringRepositoryImpl(
    private val database: MoneyDatabase
) {

    fun getAllRules(): Flow<List<RecurringRule>> = database.getAllRecurringRules()

    suspend fun getDueRules(now: Long): List<RecurringRule> =
        database.getDueRecurringRules(now)

    suspend fun getRuleById(id: Long): RecurringRule? =
        database.getRecurringRuleById(id)

    suspend fun insertRule(rule: RecurringRule): Long =
        database.insertRecurringRule(rule)

    suspend fun updateRule(rule: RecurringRule) =
        database.updateRecurringRule(rule)

    suspend fun deleteRule(id: Long) =
        database.deleteRecurringRule(id)
}
