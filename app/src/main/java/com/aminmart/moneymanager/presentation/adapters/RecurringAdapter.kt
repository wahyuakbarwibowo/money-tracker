package com.aminmart.moneymanager.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.RecurringRule
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter

class RecurringAdapter(
    private val onItemClick: (RecurringRule) -> Unit
) : ListAdapter<RecurringRule, RecurringAdapter.RuleViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recurring, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = getItem(position)
        holder.bind(rule)
        holder.itemView.setOnClickListener { onItemClick(rule) }
    }

    class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val category: TextView = itemView.findViewById(R.id.text_recurring_category)
        private val amount: TextView = itemView.findViewById(R.id.text_recurring_amount)
        private val schedule: TextView = itemView.findViewById(R.id.text_recurring_schedule)

        fun bind(rule: RecurringRule) {
            val context = itemView.context
            category.text = rule.category

            val isIncome = rule.type == Transaction.TransactionType.INCOME
            amount.text = CurrencyFormatter.formatSigned(rule.amount, isIncome)
            amount.setTextColor(
                context.getColor(if (isIncome) R.color.income_green else R.color.expense_red)
            )

            val freq = context.getString(
                when (rule.frequency) {
                    RecurringRule.Frequency.DAILY -> R.string.freq_daily
                    RecurringRule.Frequency.WEEKLY -> R.string.freq_weekly
                    RecurringRule.Frequency.MONTHLY -> R.string.freq_monthly
                    RecurringRule.Frequency.YEARLY -> R.string.freq_yearly
                }
            )
            schedule.text = context.getString(R.string.recurring_schedule_format, rule.intervalCount, freq)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RecurringRule>() {
        override fun areItemsTheSame(oldItem: RecurringRule, newItem: RecurringRule) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecurringRule, newItem: RecurringRule) =
            oldItem == newItem
    }
}
