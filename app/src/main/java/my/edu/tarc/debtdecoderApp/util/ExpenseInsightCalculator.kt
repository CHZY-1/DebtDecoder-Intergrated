package com.example.expenses_and_budget_mobileassignment.util

import android.util.Log
import my.edu.tarc.debtdecoderApp.data.Expense
import my.edu.tarc.debtdecoderApp.data.ExpenseTopCategoryItem
import my.edu.tarc.debtdecoderApp.data.FirebaseExpensesHelper
import java.text.DateFormatSymbols
import java.time.YearMonth

class ExpenseInsightCalculator (private val userId: String){

    private var expensesFirebaseHelper: FirebaseExpensesHelper = getFirebaseHelperInstance()
    private var expenses: List<Expense> = emptyList()

    // Public wrapper function to fetch all expenses from firebase
    fun fetchExpenses(onExpensesFetched: (List<Expense>) -> Unit) {
        loadExpenses {
            onExpensesFetched(expenses)
        }
    }

    // Fetch all expenses from firebase
    private fun loadExpenses(onLoaded: () -> Unit) {
        expensesFirebaseHelper.getExpenses(userId) { fetchedExpenses, _ ->
            expenses = fetchedExpenses
            Log.d("InsightCalculation", "Original Expenses Fetched: $fetchedExpenses")
            onLoaded()
        }
    }

    fun filterExpensesByMonth(year: Int, month: Int): List<Expense> {
        return expenses.filter { expense ->
            val expenseDate = expense.date
            val expenseYear = expenseDate.substringBefore("-").toIntOrNull()
            val expenseMonth = expenseDate.substringAfter("-").substringBefore("-").toIntOrNull()
            Log.d("FilterDebug", "Expense: $expense, Date: $expenseDate, Year: $expenseYear, Month: $expenseMonth")
            expenseYear == year && expenseMonth == month
        }
    }

    // find the category with the highest total spending for a specific month and year
    fun calculateTopSpendingCategory(year: Int, month: Int): Pair<String, Double>? {
        val filteredExpenses = filterExpensesByMonth(year, month)

        // Calculate total spending for each category in the given month
        val categoryTotalMap = filteredExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        // Find the category with the highest total spending
        val topCategory = categoryTotalMap.maxByOrNull { it.value }

        return topCategory?.toPair()
    }

    fun calculateTotalSpendingForMonth(year: Int, month: Int): Double {
        val filteredExpenses = filterExpensesByMonth(year, month)
        return filteredExpenses.sumOf { it.amount }
    }

    fun getExpenseTopCategoryItems(onPrepared: (List<ExpenseTopCategoryItem>) -> Unit) {
        // fetch categories detail from firebase and return a map of category name to ExpenseCategory (with image url)
        // Calculate total spending and percentage for each category for all expenses exist in the database
        ExpenseCategoryManager.getCategories { categoryMap ->
            val totalAmount = expenses.sumOf { it.amount }
//            Log.d("CategoryInsights", "Total amount of all expenses: $totalAmount")

            val expensesByCategory = expenses.groupBy { it.category }
//            Log.d("CategoryInsights", "Expenses grouped by category: $expensesByCategory")

            val preparedItems = expensesByCategory.map { (category, groupedExpenses) ->
                val sumAmount = groupedExpenses.sumOf { it.amount }
                val percentage = if (totalAmount > 0) (sumAmount / totalAmount) * 100 else 0.0
                // get correct image url key: category name, then access ExpenseCategory object imageUrl
                val imageUrl = categoryMap[category]?.imageUrl ?: ""
                ExpenseTopCategoryItem(category, percentage, sumAmount, imageUrl)
            }.sortedByDescending { it.percentage }
//            Log.d("CategoryInsights", "Prepared top category items: $preparedItems")
            onPrepared(preparedItems)
        }
    }

    fun getExpenseTopCategoryItemsForMonth(year: Int, month: Int, onPrepared: (List<ExpenseTopCategoryItem>) -> Unit) {
        // Calculate total spending for each category in the given month and year
        // Filter expenses by month
        val filteredExpenses = filterExpensesByMonth(year, month)

        // Calculate total spending for each category in the given month
        val categoryTotalMap = filteredExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        // Calculate total amount spent in the given month
        val totalAmount = filteredExpenses.sumOf { it.amount }

        // Prepare ExpenseTopCategoryItem list
        val preparedItems = categoryTotalMap.map { (category, sumAmount) ->
            val percentage = if (totalAmount > 0) (sumAmount / totalAmount) * 100 else 0.0
            ExpenseTopCategoryItem(category, percentage, sumAmount, "")
        }.sortedByDescending { it.percentage }

        // Call the callback with the prepared items (List<ExpenseTopCategoryItem>)
        onPrepared(preparedItems)
    }

    // Function to get available months and years from expenses data
    // Return an array of months and years, most recent month and year (Array<String>, Array<String>, String, String)
    fun getMonthsAndYears(onResult: (Array<String>, Array<String>, String, String) -> Unit) {
        val months = expenses.mapNotNull { expense ->
            // Extract the month from the date string
            val dateParts = expense.date.split("-")
            val monthIndex = dateParts.getOrNull(1)?.toIntOrNull()
            val month = if (monthIndex != null && monthIndex in 1..12) {
                DateFormatSymbols().months[monthIndex - 1]
            } else {
                null
            }
            month
        }.distinct().toTypedArray()

        val years = expenses.mapNotNull { expense ->
            // Extract the year from the date string
            val dateParts = expense.date.split("-")
            val year = dateParts.getOrNull(0)?.toIntOrNull()?.toString()
            year
        }.distinct().toTypedArray()

        // Find the most recent month and year by maxByOrNull
        val (mostRecentMonth, mostRecentYear) = expenses.maxByOrNull { it.date }?.let { expense ->
            val dateParts = expense.date.split("-")
            val month = dateParts.getOrNull(1)?.toIntOrNull()?.let { monthIndex ->
                DateFormatSymbols().months.getOrNull(monthIndex - 1) ?: ""
            } ?: ""
            val year = dateParts.getOrNull(0)?.toIntOrNull()?.toString() ?: ""
            Pair(month, year)
        } ?: Pair("", "")

        // Call the callback with the results
        onResult(months, years, mostRecentMonth, mostRecentYear)
    }

    // calculate the average daily expenses for a specific month and year
    fun calculateAverageDailyExpenses(year: Int, month: Int): Double {
        val filteredExpenses = filterExpensesByMonth(year, month)
        val totalAmount = filteredExpenses.sumOf { it.amount }

        // Use YearMonth to determine the number of days in the specified month and year.
        val daysInMonth = YearMonth.of(year, month).lengthOfMonth()

        // Only calculate the average if there are expenses; otherwise, return 0.0.
        return if (totalAmount > 0) totalAmount / daysInMonth else 0.0
    }

    // calculate changes from previous month to current month
    fun calculateChangeFromPreviousMonth(currentYear: Int, currentMonth: Int): Pair<Double, Double?> {
        // Calculate the expenses for the current month.
        val currentMonthExpenses = calculateTotalSpendingForMonth(currentYear, currentMonth)

        // Determine the year and month for the previous month's calculations.
        val previousMonth = if (currentMonth == 1) 12 else currentMonth - 1
        val previousYear = if (previousMonth == 12) currentYear - 1 else currentYear

        // Calculate the expenses for the previous month.
        val previousMonthExpenses = calculateTotalSpendingForMonth(previousYear, previousMonth)

        // Calculate the change in expenses from the previous month to the current month.
        val change = currentMonthExpenses - previousMonthExpenses

        // Calculate the percentage change.
        val percentageChange = when {
            previousMonthExpenses > 0 -> (change / previousMonthExpenses) * 100  // Normal case
            previousMonthExpenses == 0.0 && currentMonthExpenses > 0 -> 100.0     // Special case: from zero to positive, (one of the month has data)
            else -> null  // Indicates no change or not applicable (both months are zero)
        }

        // Return absolute change and percentage change.
        return Pair(change, percentageChange)
    }
}