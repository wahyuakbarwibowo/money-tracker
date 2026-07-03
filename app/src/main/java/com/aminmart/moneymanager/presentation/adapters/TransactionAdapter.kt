package com.aminmart.moneymanager.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying transactions in RecyclerView
 */
class TransactionAdapter(
    private var transactions: List<Transaction> = emptyList(),
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textDate: TextView = itemView.findViewById(R.id.text_transaction_date)
        val textCategory: TextView = itemView.findViewById(R.id.text_transaction_category)
        val textDescription: TextView = itemView.findViewById(R.id.text_transaction_description)
        val textAmount: TextView = itemView.findViewById(R.id.text_transaction_amount)
        val indicatorType: View = itemView.findViewById(R.id.indicator_transaction_type)
        val ribaIndicator: TextView = itemView.findViewById(R.id.text_riba_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.textDate.text = dateFormat.format(Date(transaction.date))
        holder.textCategory.text = transaction.category
        holder.textDescription.text = transaction.description.ifEmpty { "-" }
        
        val isIncome = transaction.type == Transaction.TransactionType.INCOME
        holder.textAmount.text = CurrencyFormatter.formatSigned(transaction.amount, isIncome)

        // Set color based on type
        val context = holder.itemView.context
        val color = if (transaction.type == Transaction.TransactionType.INCOME) {
            context.getColor(R.color.income_green)
        } else {
            context.getColor(R.color.expense_red)
        }
        holder.textAmount.setTextColor(color)
        holder.indicatorType.setBackgroundColor(color)

        // Show/hide Riba indicator
        holder.ribaIndicator.visibility = if (transaction.isRiba) View.VISIBLE else View.GONE

        // Click listeners
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun submitList(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            transactions = transactions.toMutableList().apply {
                this[index] = updatedTransaction
            }
            notifyItemChanged(index)
        }
    }

    fun removeTransaction(transactionId: Long) {
        val index = transactions.indexOfFirst { it.id == transactionId }
        if (index != -1) {
            transactions = transactions.filter { it.id != transactionId }
            notifyItemRemoved(index)
        }
    }
}
