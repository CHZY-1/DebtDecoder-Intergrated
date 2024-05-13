package com.example.expenses_and_budget_mobileassignment.expenses

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.expenses_and_budget_mobileassignment.util.DateFormatter
import com.example.expenses_and_budget_mobileassignment.util.ExpenseInsightCalculator
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentExpensesInsightBinding
import java.util.Calendar

class ExpensesInsightFragment : Fragment() {
    private var _binding: FragmentExpensesInsightBinding? = null
    private val binding get() = _binding!!
    private lateinit var expenseInsightCalculator: ExpenseInsightCalculator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity()
        _binding = FragmentExpensesInsightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = "userId1"

        val (startDate, endDate) = DateFormatter.getCurrentMonthDateRange()
        binding.tvInsightDateCategories.text = "$startDate - $endDate"
        binding.tvInsightDateMonthlyExpense.text = "$startDate - $endDate"

        expenseInsightCalculator = ExpenseInsightCalculator(userId)
        expenseInsightCalculator.fetchExpenses { expenses ->
            if (isAdded && _binding != null) {
                // get current year and month
                // only show insight for current month
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1

                if (expenses.isNotEmpty()) {
                    updateExpenseCategoriesSection(year, month)
                    updateMonthlyExpensesSection(year, month)
                } else {
                    hideInsightCategoriesSections()
                    hideInsightSpendingSections()
                }
            }
        }

        if(isAdded && _binding != null){
            binding.clChartCategories.setOnClickListener {
                findNavController().navigate(R.id.action_navigation_expense_to_displayExpenseCategoriesFragment)
            }

            binding.clChartMonthlyExpense.setOnClickListener {
                findNavController().navigate(R.id.action_navigation_expense_to_insightMonthlyExpensesFragment)
            }
        }
    }

    private fun updateExpenseCategoriesSection(year: Int, month: Int) {
        if (isAdded && _binding != null) {
            val totalSpending = expenseInsightCalculator.calculateTotalSpendingForMonth(year, month)
            val topSpending = expenseInsightCalculator.calculateTopSpendingCategory(year, month)

            if (totalSpending > 0) {
                binding.tvInsightCategories1.text =
                    String.format("Total Spending for %d-%d: RM %.2f", year, month, totalSpending)
                binding.tvInsightCategories1.visibility = View.VISIBLE

                if (topSpending != null) {
                    binding.tvInsightCategories2.text = String.format(
                        "Top Spending Category for %d-%d: %s RM %.2f",
                        year,
                        month,
                        topSpending.first,
                        topSpending.second
                    )
                    binding.tvInsightCategories2.visibility = View.VISIBLE
                } else {
                    binding.tvInsightCategories2.visibility = View.GONE
                }
            } else {
                hideInsightCategoriesSections()
            }
        }
    }

    private fun updateMonthlyExpensesSection(year: Int, month: Int) {
        if (isAdded && _binding != null) {
            val totalSpending = expenseInsightCalculator.calculateTotalSpendingForMonth(year, month)
            val averageDailyExpenses =
                expenseInsightCalculator.calculateAverageDailyExpenses(year, month)
            val (change, percentageChange) = expenseInsightCalculator.calculateChangeFromPreviousMonth(
                year,
                month
            )

            if (totalSpending > 0) {
                binding.tvInsightMonthlyExpense1.text =
                    String.format("Total Expenses: RM %.2f", totalSpending)
                binding.tvInsightMonthlyExpense1.visibility = View.VISIBLE

                binding.tvInsightMonthlyExpense2.text =
                    String.format("Average Daily Expenses: RM %.2f", averageDailyExpenses)
                binding.tvInsightMonthlyExpense2.visibility = View.VISIBLE

                binding.tvInsightMonthlyExpense3.text = String.format(
                    "Change from Previous Month: RM %.2f (%.2f%%)",
                    change,
                    percentageChange
                )
                binding.tvInsightMonthlyExpense3.visibility = View.VISIBLE

            } else {
                hideInsightSpendingSections()
            }
        }
    }

    private fun hideInsightCategoriesSections() {
        binding.tvInsightCategories1.visibility = View.GONE
        binding.tvInsightCategories2.visibility = View.GONE
        binding.tvInsightCategories3.visibility = View.GONE
    }

    private fun hideInsightSpendingSections() {
        binding.tvInsightMonthlyExpense1.visibility = View.GONE
        binding.tvInsightMonthlyExpense2.visibility = View.GONE
        binding.tvInsightMonthlyExpense3.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}