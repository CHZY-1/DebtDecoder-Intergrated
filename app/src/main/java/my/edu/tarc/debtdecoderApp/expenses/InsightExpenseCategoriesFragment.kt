package com.example.expenses_and_budget_mobileassignment.expenses

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expenses_and_budget_mobileassignment.data.ExpenseTopCategoryItem
import com.example.expenses_and_budget_mobileassignment.util.DateFormatter
import com.example.expenses_and_budget_mobileassignment.util.ExpenseInsightCalculator
import com.example.expenses_and_budget_mobileassignment.util.GlideImageLoader
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import my.edu.tarc.debtdecoder.databinding.FragmentDisplayExpenseCategoriesBinding

class DisplayExpenseCategoriesFragment : Fragment() {
    private var _binding: FragmentDisplayExpenseCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var expenseInsightCalculator: ExpenseInsightCalculator
    private lateinit var adapter: TopSpendingCategoriesAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayExpenseCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isAdded || _binding == null) {
            return
        }

        val userId = "userId1"
        expenseInsightCalculator = ExpenseInsightCalculator(userId)

        expenseInsightCalculator.fetchExpenses { expenses ->
            expenseInsightCalculator.getExpenseTopCategoryItems { topExpenseCategories ->
                // make sure fragment is still added and the binding is available
                if (isAdded && _binding != null) {
//                    Log.d("CategoryInsights", "topExpenseCategories recycle view: $topExpenseCategories")
                    setupRecycleView(topExpenseCategories)

                    if (topExpenseCategories.isEmpty()) {
//                        Log.d("ExpenseDebug", "No categories to display.")
                    } else {
//                    Log.d("ExpenseDebug", "Adapter set with data.")
                        setupCategoryPieChart(topExpenseCategories)
                        setupDateSpinner()
                    }

                }
            }
        }

    }

    // Set up the RecyclerView with the top spending categories
    private fun setupRecycleView(List: List<ExpenseTopCategoryItem>){
        if (!isAdded || _binding == null) {
            return
        }
        binding.recyclerViewCategories.layoutManager = LinearLayoutManager(context)
        adapter = TopSpendingCategoriesAdapter(List, GlideImageLoader())
        binding.recyclerViewCategories.adapter = adapter
    }

    // Set up the Spinner with the available months and years
    private fun setupDateSpinner() {
        // Add the "All Data" option as the first item
        val dateStrings = mutableListOf<String>()
        dateStrings.add("All Data")

        // Set up the Spinner with the date strings
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dateStrings
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerDateSelection.adapter = adapter
        binding.spinnerDateSelection.setSelection(0)

        // Listen for selection changes
        binding.spinnerDateSelection.onItemSelectedListener =
            createSpinnerItemSelectedListener(dateStrings)

        // Get available months and years from the ExpenseInsightCalculator
        expenseInsightCalculator.fetchExpenses { expenses ->
            expenseInsightCalculator.getMonthsAndYears { months, years, _, _ ->
                val combinedDates =
                    months.zip(years) { month, year -> "$month, $year" }.toMutableList()

                if (months.size > years.size) {
                    val lastYear = years.lastOrNull() ?: "Unknown Year"
                    combinedDates.addAll(
                        months.drop(years.size).map { month -> "$month, $lastYear" })
                }

                dateStrings.addAll(combinedDates)

                adapter.notifyDataSetChanged()
            }
        }
    }

    // Create a listener for the Spinner to handle selection changes
    private fun createSpinnerItemSelectedListener(dateStrings: List<String>): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isAdded || _binding == null) {
                    return
                }

                if (position == 0) {
                    // If "All Data" is selected, update the RecyclerView and chart with all data
                    updateDataForAllDataSelection()
                } else {
                    // Parse the selected date string to extract the month and year
                    val (selectedMonth, selectedYear) = dateStrings[position].split(", ")
                    val year = selectedYear.toInt()
                    val month = DateFormatter.getMonthNumber(selectedMonth)

                    // Filter the data for the selected month and year
                    updateDataForSelectedMonth(year, month)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun updateDataForAllDataSelection() {
        if (!isAdded || _binding == null) {
            return
        }

        expenseInsightCalculator.getExpenseTopCategoryItems { topExpenseCategories ->
            adapter.updateData(topExpenseCategories)
            setupCategoryPieChart(topExpenseCategories)
        }
    }

    private fun updateDataForSelectedMonth(year: Int, month: Int) {
        if (!isAdded || _binding == null) {
            return
        }

        expenseInsightCalculator.getExpenseTopCategoryItemsForMonth(year, month) { topExpenseCategories ->
            adapter.updateData(topExpenseCategories)
            setupCategoryPieChart(topExpenseCategories)
        }
    }

    private fun setupCategoryPieChart(topExpenseCategories: List<ExpenseTopCategoryItem>) {
        val pieChart = binding.pieChart

        // Configure pie chart
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(true)
            animateY(1400, Easing.EaseInOutQuad)

            // For donut chart
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            transparentCircleRadius = 55f
        }

        // Prepare data entries
        val entries = topExpenseCategories.map { entry ->
            PieEntry(entry.percentage.toFloat(), entry.category)
        }

        // Create data set
        val dataSet = PieDataSet(entries, "Expense Categories").apply {
            setColors(*ColorTemplate.COLORFUL_COLORS)
            valueTextSize = 16f
            valueTextColor = Color.BLACK
            sliceSpace = 2f
            setDrawValues(true)
        }

        // Create pie data and set formatter
        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }

        // Set data to chart
        pieChart.data = data
        pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}