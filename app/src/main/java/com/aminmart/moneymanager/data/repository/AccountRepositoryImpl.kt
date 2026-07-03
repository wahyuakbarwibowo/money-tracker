package com.aminmart.moneymanager.data.repository

import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.domain.model.Account
import kotlinx.coroutines.flow.Flow

class AccountRepositoryImpl(
    private val database: MoneyDatabase
) {
    fun getAllAccounts(): Flow<List<Account>> = database.getAllAccounts()
    suspend fun getAccountById(id: Long): Account? = database.getAccountById(id)
    suspend fun insertAccount(account: Account): Long = database.insertAccount(account)
    suspend fun updateAccount(account: Account) = database.updateAccount(account)
    suspend fun deleteAccount(id: Long) = database.deleteAccount(id)
}
