package com.example.expenses_and_budget_mobileassignment.expenses

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expenses_and_budget_mobileassignment.data.ExpenseCategory
import com.example.expenses_and_budget_mobileassignment.data.ExpenseTopCategoryItem
import com.example.expenses_and_budget_mobileassignment.util.ExpenseCategoryManager
import com.example.expenses_and_budget_mobileassignment.util.GlideImageLoader
import my.edu.tarc.debtdecoder.databinding.ItemSumExpenseTopCategoryBinding

class TopSpendingCategoriesAdapter(
    private var expenses: List<ExpenseTopCategoryItem>,
    private val imageLoader: GlideImageLoader
) : RecyclerView.Adapter<TopSpendingCategoriesAdapter.CategoryViewHolder>() {

    // Map to store category names and their corresponding ExpenseCategory objects
    // Use for loading category images url from firebase storage
    private var categoryMap: Map<String, ExpenseCategory> = emptyMap()

    init {
        ExpenseCategoryManager.getCategories { categories ->
            categoryMap = categories
            Log.d("CategoryViewHolder", "Top Category recycle view categoryMap : ${categoryMap}")
            notifyDataSetChanged()
        }
    }

    class CategoryViewHolder(private val binding: ItemSumExpenseTopCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: ExpenseTopCategoryItem, imageLoader: GlideImageLoader, categoryMap: Map<String, ExpenseCategory>) {
            // Top expense category
            binding.tvExpenseCategoryName.text = expense.category
            Log.d("CategoryViewHolder", "Top Category recycle view binding item : ${expense.category}")
            binding.tvExpenseCategoryPercentage.text = String.format("%.2f%%", expense.percentage)
            binding.tvExpenseCategorySumAmount.text = "RM ${String.format("%.2f", expense.amount)}"

            // Load category image
            val categoryImage = categoryMap[expense.category]?.imageUrl ?: ""
            imageLoader.loadCategoryImage(categoryImage, binding.ivExpenseCategoryIcon, itemView.context)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSumExpenseTopCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(expenses[position], imageLoader, categoryMap)
    }

    // Function to refresh adapter
    fun updateData(newExpenses: List<ExpenseTopCategoryItem>) {
        this.expenses = newExpenses
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = expenses.size
}