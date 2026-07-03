package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Account
import com.aminmart.moneymanager.data.repository.AccountRepositoryImpl
import kotlinx.coroutines.flow.Flow

data class AccountUseCases(
    val getAccounts: GetAccountsUseCase,
    val addAccount: AddAccountUseCase,
    val deleteAccount: DeleteAccountUseCase
)

class GetAccountsUseCase(private val repository: AccountRepositoryImpl) {
    operator fun invoke(): Flow<List<Account>> = repository.getAllAccounts()
}

class AddAccountUseCase(private val repository: AccountRepositoryImpl) {
    suspend operator fun invoke(account: Account): Long = repository.insertAccount(account)
}

class DeleteAccountUseCase(private val repository: AccountRepositoryImpl) {
    suspend operator fun invoke(id: Long) = repository.deleteAccount(id)
}
