package com.aminmart.moneymanager.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Account
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter

class AccountAdapter(
    private val onItemClick: (Account) -> Unit
) : ListAdapter<Account, AccountAdapter.AccountViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        holder.bind(account)
        holder.itemView.setOnClickListener { onItemClick(account) }
    }

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.text_account_name)
        private val balance: TextView = itemView.findViewById(R.id.text_account_balance)

        fun bind(account: Account) {
            name.text = account.name
            balance.text = CurrencyFormatter.format(account.currentBalance)
            val color = if (account.currentBalance < 0) R.color.expense_red else R.color.text_primary
            balance.setTextColor(itemView.context.getColor(color))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Account, newItem: Account) = oldItem == newItem
    }
}
