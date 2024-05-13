package com.example.expenses_and_budget_mobileassignment.expenses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expenses_and_budget_mobileassignment.data.Expense
import com.example.expenses_and_budget_mobileassignment.data.ExpenseCategory
import com.example.expenses_and_budget_mobileassignment.util.ExpenseCategoryManager
import com.example.expenses_and_budget_mobileassignment.util.GlideImageLoader
import my.edu.tarc.debtdecoder.databinding.ItemExpenseBinding

class ExpenseAdapter(private var expenses: List<Expense>, private val imageLoader: GlideImageLoader) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {
    private var categoryMap: Map<String, ExpenseCategory> = emptyMap()
    private var isSelectionMode = false

    init {
        ExpenseCategoryManager.getCategories { categories ->
            categoryMap = categories
            // Refresh adapter
            notifyDataSetChanged()
        }
    }

    class ExpenseViewHolder(private val binding: ItemExpenseBinding,
                            private val imageLoader: GlideImageLoader,
                            private val categoryMap: Map<String, ExpenseCategory>) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            // Set expense details
            binding.tvExpenseCategory.text = expense.category
            binding.tvExpenseAmount.text = String.format("RM %.2f", expense.amount)
            binding.tvExpensePaymentMethod.text = expense.payment

            // Load category image
            val categoryImage = categoryMap[expense.category]?.imageUrl ?: ""
            imageLoader.loadCategoryImage(categoryImage, binding.ivExpenseIcon, itemView.context)

            // Set checkbox state for deletion
            binding.checkboxSelect.setOnCheckedChangeListener(null)
            binding.checkboxSelect.isChecked = expense.isSelected
            binding.checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
                expense.isSelected = isChecked
            }
        }

        fun setCheckBoxVisibility(isVisible: Boolean) {
            binding.checkboxSelect.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding, imageLoader, categoryMap)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
        holder.setCheckBoxVisibility(isSelectionMode)

    }

    override fun getItemCount() = expenses.size

    // replace the old list of expenses with a new one for different date selection
    fun updateData(newExpenses: List<Expense>) {
        this.expenses = newExpenses
        notifyDataSetChanged()
    }

    fun setSelectionMode(selectionMode: Boolean) {
        isSelectionMode = selectionMode
        notifyDataSetChanged()
    }

    fun getSelectedExpenses(): List<Expense> {
        return expenses.filter { it.isSelected }
    }

    fun removeDeletedExpenses(expensesToRemove: List<Expense>) {
        expenses = expenses.filterNot { expensesToRemove.contains(it) }
        notifyDataSetChanged()
    }

    fun getTotalExpense(): Double {
        return expenses.sumOf { it.amount }
    }
}

