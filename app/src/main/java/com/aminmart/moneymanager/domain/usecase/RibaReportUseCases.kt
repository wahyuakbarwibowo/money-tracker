package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Aggregated view of transactions flagged as riba. */
data class RibaReport(
    val totalAmount: Double,
    val count: Int,
    val transactions: List<Transaction>
)

/**
 * Builds a riba report from the existing is_riba flag on transactions.
 * Filters in memory so no extra DB query/schema change is needed.
 */
class GetRibaReportUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<RibaReport> =
        transactionRepository.getAllTransactions().map { all ->
            val riba = all.filter { it.isRiba }
            RibaReport(
                totalAmount = riba.sumOf { it.amount },
                count = riba.size,
                transactions = riba
            )
        }
}
