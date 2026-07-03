package com.aminmart.moneymanager.domain.model

/**
 * A wallet/account that transactions belong to (e.g. Cash, Bank, E-wallet).
 * [currentBalance] is derived: initialBalance + income - expense for this account.
 */
data class Account(
    val id: Long = 0,
    val name: String,
    val initialBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val currentBalance: Double = initialBalance
)
