package com.aminmart.moneymanager.domain.model

/**
 * A rule that generates [Transaction]s on a schedule (e.g. monthly salary,
 * weekly allowance). [nextRun] is the next due timestamp; the generator advances
 * it by [frequency] x [intervalCount] each time it fires.
 */
data class RecurringRule(
    val id: Long = 0,
    val type: Transaction.TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val isRiba: Boolean = false,
    val frequency: Frequency,
    val intervalCount: Int = 1,
    val nextRun: Long,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}
