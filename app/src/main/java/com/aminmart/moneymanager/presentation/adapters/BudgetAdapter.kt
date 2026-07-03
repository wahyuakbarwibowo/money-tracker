package com.aminmart.moneymanager.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter

/**
 * Adapter for displaying budgets in RecyclerView
 */
class BudgetAdapter(
    private var budgets: List<Budget> = emptyList(),
    private val onItemClick: (Budget) -> Unit,
    private val onEditClick: (Budget) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCategory: TextView = itemView.findViewById(R.id.text_budget_category)
        val textBudget: TextView = itemView.findViewById(R.id.text_budget_amount)
        val textSpent: TextView = itemView.findViewById(R.id.text_budget_spent)
        val textRemaining: TextView = itemView.findViewById(R.id.text_budget_remaining)
        val progressBudget: View = itemView.findViewById(R.id.progress_budget)
        val editButton: View = itemView.findViewById(R.id.button_budget_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgets[position]

        holder.textCategory.text = budget.category
        holder.textBudget.text = CurrencyFormatter.format(budget.monthlyBudget)
        holder.textSpent.text = "Spent: ${CurrencyFormatter.format(budget.spent)}"
        holder.textRemaining.text = "Remaining: ${CurrencyFormatter.format(budget.remaining)}"

        // Update progress bar
        val progress = budget.percentageUsed.coerceIn(0f, 1f)
        holder.progressBudget.layoutParams = (holder.progressBudget.layoutParams as ViewGroup.MarginLayoutParams).apply {
            width = (holder.itemView.width * progress).toInt()
        }

        // Set color based on budget status
        val context = holder.itemView.context
        val color = when {
            budget.isOverBudget -> context.getColor(R.color.expense_red)
            budget.isNearLimit -> context.getColor(R.color.warning_orange)
            else -> context.getColor(R.color.income_green)
        }
        holder.progressBudget.setBackgroundColor(color)
        holder.textRemaining.setTextColor(color)

        // Click listeners
        holder.itemView.setOnClickListener {
            onItemClick(budget)
        }

        holder.editButton.setOnClickListener {
            onEditClick(budget)
        }
    }

    override fun getItemCount(): Int = budgets.size

    fun submitList(newBudgets: List<Budget>) {
        budgets = newBudgets
        notifyDataSetChanged()
    }

    fun updateBudget(updatedBudget: Budget) {
        val index = budgets.indexOfFirst { it.id == updatedBudget.id }
        if (index != -1) {
            budgets = budgets.toMutableList().apply {
                this[index] = updatedBudget
            }
            notifyItemChanged(index)
        }
    }

    fun removeBudget(budgetId: Long) {
        val index = budgets.indexOfFirst { it.id == budgetId }
        if (index != -1) {
            budgets = budgets.filter { it.id != budgetId }
            notifyItemRemoved(index)
        }
    }
}
