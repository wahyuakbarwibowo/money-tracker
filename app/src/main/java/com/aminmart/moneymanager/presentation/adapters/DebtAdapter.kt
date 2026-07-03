package com.aminmart.moneymanager.presentation.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class DebtAdapter(
    private val onItemClick: (Debt) -> Unit
) : ListAdapter<Debt, DebtAdapter.DebtViewHolder>(DebtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        val debt = getItem(position)
        holder.bind(debt)
        holder.itemView.setOnClickListener { onItemClick(debt) }
    }

    class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val personName: TextView = itemView.findViewById(R.id.text_person_name)
        private val description: TextView = itemView.findViewById(R.id.text_debt_description)
        private val amount: TextView = itemView.findViewById(R.id.text_debt_amount)
        private val dueDate: TextView = itemView.findViewById(R.id.text_due_date)
        private val status: TextView = itemView.findViewById(R.id.text_debt_status)
        private val typeImage: ImageView = itemView.findViewById(R.id.image_debt_type)

        fun bind(debt: Debt) {
            personName.text = debt.personName
            description.text = debt.description
            amount.text = CurrencyFormatter.format(debt.amount)

            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dueDate.text = "Due: ${sdf.format(Date(debt.dueDate))}"

            if (debt.isPaid) {
                status.text = "PAID"
                status.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
            } else {
                status.text = "UNPAID"
                status.setBackgroundColor(Color.parseColor("#F44336")) // Red
            }

            if (debt.type == Debt.DebtType.DEBT) {
                // I owe money
                amount.setTextColor(Color.RED)
                typeImage.setImageResource(R.drawable.ic_dashboard) // Placeholder
            } else {
                // Others owe me
                amount.setTextColor(Color.GREEN)
                typeImage.setImageResource(R.drawable.ic_statistics) // Placeholder
            }
        }
    }
}

class DebtDiffCallback : DiffUtil.ItemCallback<Debt>() {
    override fun areItemsTheSame(oldItem: Debt, newItem: Debt): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Debt, newItem: Debt): Boolean {
        return oldItem == newItem
    }
}
