package com.example.expenses_and_budget_mobileassignment.expenses

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.expenses_and_budget_mobileassignment.util.ExpenseInsightCalculator
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentInsightMonthlyExpensesBinding
import java.time.YearMonth

class InsightMonthlyExpensesFragment : Fragment() {
    private var _binding: FragmentInsightMonthlyExpensesBinding? = null
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
        _binding = FragmentInsightMonthlyExpensesBinding.inflate(inflater, container, false)
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
            if (isAdded && _binding != null) {
                val currentYear = YearMonth.now().year
                val currentMonth = YearMonth.now().monthValue
                val previousMonth = if (currentMonth == 1) 12 else currentMonth - 1
                val previousYear = if (currentMonth == 1) currentYear - 1 else currentYear

                val currentMonthData = prepareChartData(currentYear, currentMonth)
                val previousMonthData = prepareChartData(previousYear, previousMonth)

                if (currentMonthData.isEmpty() || previousMonthData.isEmpty()) {
                    Toast.makeText(requireContext(), "No expenses found for the current month.", Toast.LENGTH_SHORT).show()
                } else {
                    updateChart(currentMonthData, previousMonthData)
                }
            }
        }

    }

    fun prepareChartData(year: Int, month: Int, fillMethod: String = "lastObservation"): List<Entry> {
        val filteredExpenses = expenseInsightCalculator.filterExpensesByMonth(year, month)
        val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
        val entries = mutableListOf<Entry>()
        var lastValue = 0f

        if (filteredExpenses.isEmpty()) {
            return emptyList()
        }

        for (day in 1..daysInMonth) {
            val dayTotal = filteredExpenses.filter {
                it.date.endsWith("${year}-${month.pad()}-${day.pad()}")
            }.sumOf { it.amount }

            if (dayTotal > 0) lastValue = dayTotal.toFloat()
            entries.add(Entry(day.toFloat(), if (dayTotal > 0) dayTotal.toFloat() else lastValue))
        }

        return entries
    }

    private fun updateChart(currentData: List<Entry>, previousData: List<Entry>) {
        val currentDataSet = LineDataSet(currentData, "Current Month").apply {
            setDrawValues(false)
            color = ContextCompat.getColor(context!!, R.color.dark_blue)
            setCircleColor(ContextCompat.getColor(context!!, R.color.dark_blue))
            lineWidth = 2.5f
            setDrawFilled(false)
            valueTextSize = 10f
        }

        val previousDataSet = LineDataSet(previousData, "Previous Month").apply {
            setDrawValues(false)
            color = ContextCompat.getColor(context!!, R.color.dark_red)
            setCircleColor(ContextCompat.getColor(context!!, R.color.dark_red))
            lineWidth = 2.5f
            setDrawFilled(false)
            valueTextSize = 10f
            fillAlpha = 50
        }

        val lineData = LineData(currentDataSet, previousDataSet)
        binding.lineChartMonthlyExpense.data = lineData

        with(binding.lineChartMonthlyExpense) {
            axisRight.isEnabled = false
            xAxis.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)
            invalidate()
        }
    }

    fun Int.pad(): String = this.toString().padStart(2, '0')


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}