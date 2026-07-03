package com.aminmart.moneymanager.domain.model

/**
 * Transaction model representing income or expense
 */
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long, // Timestamp in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val isRiba: Boolean = false,
    val accountId: Long = DEFAULT_ACCOUNT_ID
) {
    companion object {
        const val DEFAULT_ACCOUNT_ID = 1L
    }


    enum class TransactionType {
        INCOME,
        EXPENSE
    }
}
