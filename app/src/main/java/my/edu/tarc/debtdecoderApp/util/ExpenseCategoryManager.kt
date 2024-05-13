package com.example.expenses_and_budget_mobileassignment.util

import android.content.Context
import com.example.expenses_and_budget_mobileassignment.data.ExpenseCategory
import com.example.expenses_and_budget_mobileassignment.data.FirebaseExpensesHelper

object ExpenseCategoryManager {
    private var categories: Map<String, ExpenseCategory>? = null

    // initialize firebase helper class for calling getCategoryDetails function to get all categories
    private val expensesFirebaseHelper: FirebaseExpensesHelper by lazy {
        getFirebaseHelperInstance()
    }

    fun getCategories(onCategoriesReceived: (Map<String, ExpenseCategory>) -> Unit) {
        // Return cached categories if already exists
        if (categories != null) {
            onCategoriesReceived(categories!!)
            return
        }

        // Fetch categories if not cached
        expensesFirebaseHelper.getCategoryDetails { fetchedCategories ->
            categories = fetchedCategories
            onCategoriesReceived(categories!!)
        }
    }

    fun prefetchCategoryImages(context: Context){
        getCategories { categories ->
            categories.values.forEach { category ->
                if (category.imageUrl.isNotEmpty()) {
                    GlideImageLoader().prefetchImage(category.imageUrl, context)
                }
            }
        }
    }
}