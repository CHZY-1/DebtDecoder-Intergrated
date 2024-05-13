package com.example.expenses_and_budget_mobileassignment.expenses

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.expenses_and_budget_mobileassignment.data.ExpenseCategory
import com.example.expenses_and_budget_mobileassignment.util.GlideImageLoader
import com.example.expenses_and_budget_mobileassignment.util.getFirebaseHelperInstance
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import my.edu.tarc.debtdecoder.databinding.FragmentExpenseCategoryPickerBinding
import my.edu.tarc.debtdecoder.databinding.ItemExpenseCategoryBinding

class CategoryPickerFragment(private val categorySelectionListener: CategorySelectionListener) : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentExpenseCategoryPickerBinding
    private lateinit var adapter: ExpenseCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpenseCategoryPickerBinding.inflate(inflater, container, false)

        // Initialize the adapter with an empty list
        // Update back the category icon and name in add expense fragment (via listener)
        adapter = ExpenseCategoryAdapter(emptyList()) { expenseCategory ->
            categorySelectionListener.onCategorySelected(expenseCategory)
            dismiss()
        }
        binding.recyclerViewCategories.adapter = adapter

        loadCategories()

        return binding.root
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val firebase = getFirebaseHelperInstance()
                firebase.getCategoryDetails { categories ->
                    if (categories.isEmpty()) {
                        Log.d("CategoryPicker", "No categories available")
                    } else {
                        Log.d("CategoryPicker", "Categories fetched: ${categories.size}")
                        if (isAdded) {
                            updateAdapter(categories.values.toList())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CategoryPickerFragment", "Error loading categories", e)
            }
        }
    }

    private fun updateAdapter(categories: List<ExpenseCategory>) {
        adapter.updateData(categories)
    }

    class ExpenseCategoryAdapter(
        private var categories: List<ExpenseCategory>,
        private val onCategorySelected: (ExpenseCategory) -> Unit
    ) : RecyclerView.Adapter<ExpenseCategoryAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemExpenseCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(category: ExpenseCategory, onClick: (ExpenseCategory) -> Unit) {
                binding.expenseCategoryName.text = category.categoryName
                Log.d("CategoryAdapter", "Setting category name: ${category.categoryName}")

                GlideImageLoader().loadCategoryImage(category.imageUrl, binding.expenseCategoryIcon, itemView.context)
                itemView.setOnClickListener { onClick(category) }
            }
        }

        fun updateData(newCategories: List<ExpenseCategory>) {
            categories = newCategories
            notifyDataSetChanged()
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemExpenseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            // Calculate the item width based on the number of columns
            val displayMetrics = holder.itemView.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val spanCount = 4
            val itemWidth = screenWidth / spanCount

            // Set the item view's width and height
            holder.itemView.layoutParams.width = itemWidth
            holder.itemView.layoutParams.height = itemWidth

            holder.bind(categories[position], onCategorySelected)
        }

        override fun getItemCount(): Int = categories.size
    }
}
