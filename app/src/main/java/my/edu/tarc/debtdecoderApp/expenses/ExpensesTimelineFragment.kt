package my.edu.tarc.debtdecoderApp.expenses
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import my.edu.tarc.debtdecoderApp.data.Expense
import my.edu.tarc.debtdecoderApp.data.ExpenseItem
import my.edu.tarc.debtdecoderApp.data.FirebaseExpensesHelper
import my.edu.tarc.debtdecoderApp.data.ListItem
import my.edu.tarc.debtdecoderApp.data.SectionHeader
import my.edu.tarc.debtdecoderApp.util.DateFormatter
import my.edu.tarc.debtdecoderApp.util.GlideImageLoader
import com.example.expenses_and_budget_mobileassignment.util.getFirebaseHelperInstance
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentExpensesTimelineBinding

class ExpensesTimelineFragment : Fragment() {
    private var _binding: FragmentExpensesTimelineBinding? = null
    private val binding get() = _binding!!
    private lateinit var expensesFirebaseHelper: FirebaseExpensesHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity()
        expensesFirebaseHelper = getFirebaseHelperInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesTimelineBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdded && _binding != null) {
            setupRecyclerView()
            fetchExpensesAndUpdateUI()
        }

        binding.btnAddExpense.setOnClickListener {
            if (isAdded && _binding != null) {
                try {
                    findNavController().navigate(R.id.action_expensesTimelineFragment_to_addExpenseFragment)
                } catch (e: Exception) {
                    Log.e("NavigationError", "Navigation failed to add an expense", e)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // Initially set an empty adapter that will be updated later
        binding.rvAllExpenses.layoutManager = LinearLayoutManager(context)

        val initialItems = mutableListOf<ListItem>()
        binding.rvAllExpenses.adapter = SectionedExpenseAdapter(initialItems, GlideImageLoader())
    }

    private fun fetchExpensesAndUpdateUI() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            val userId = firebaseUser.uid
            Log.d("UserID", "User ID: $userId")

            expensesFirebaseHelper.getExpensesForLastDays(userId, 7) { expenses ->
                if (isAdded && _binding != null) {
                    val sectionedExpenses = transformToSectionedExpenses(expenses)
                    updateRecyclerView(sectionedExpenses)

                    val expensesByDate = groupExpensesByDate(expenses)
                    val chartEntries = prepareChartData(expensesByDate)
                    updateBarChart(chartEntries)
                }
            }
        }
    }

    private fun transformToSectionedExpenses(expenses: List<Expense>): List<ListItem> {

        // convert date string (firebase) to date format and group expenses by date
        val groupedByDate = expenses.groupBy {
            DateFormatter.formatForDisplay(it.date) ?: "Unknown Date"
        }
        val sectionedItems = mutableListOf<ListItem>()

        // Order the grouped dates sorted by the display date.
        groupedByDate.toSortedMap(reverseOrder()).forEach { (displayDate, expensesList) ->
            val totalAmount = expensesList.sumOf { it.amount }
            sectionedItems.add(SectionHeader(displayDate, totalAmount.toFloat()))
            expensesList.forEach { expense ->
                sectionedItems.add(ExpenseItem(expense.category, expense.payment, expense.amount.toFloat()))
            }
        }
        return sectionedItems
    }

    private fun updateRecyclerView(expenses: List<ListItem>) {
        val adapter = binding.rvAllExpenses.adapter as SectionedExpenseAdapter
        adapter.updateData(expenses)
    }


    // Bar Chart
    private fun groupExpensesByDate(expenses: List<Expense>): Map<String, Double> {
        // Group expenses by their display date and calculate the sum of amounts for each date
        return expenses
            .groupBy { DateFormatter.formatForDisplay(it.date) ?: "Unknown Date"}
            .mapValues { (_, expensesList) -> expensesList.sumOf { it.amount } }
    }

    private fun prepareChartData(expensesByDate: Map<String, Double>): List<BarEntry> {
        // Sort dates to ensure the chart displays them in order
        val sortedDates = expensesByDate.keys.sorted()
        return sortedDates.mapIndexed { index, date ->
            // BarEntry expects float value
            BarEntry(index.toFloat(), (expensesByDate[date] ?: 0.0).toFloat())
        }
    }

    private fun updateBarChart(entries: List<BarEntry>) {
        val barDataSet = BarDataSet(entries, "Daily Expenses")
        barDataSet.color = 0xFF7AB6F5.toInt() // Light blue color
        barDataSet.valueTextColor = 0xFFFFFFFF.toInt() // White value labels
        barDataSet.valueTextSize = 12f

        val barData = BarData(barDataSet)
        barData.barWidth = 0.2f

        binding.barChartExpenses.apply {
            data = barData
            xAxis.apply {
                textColor = 0xFFFFFFFF.toInt()
                textSize = 12f
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(entries.mapIndexed { index, _ -> "Day ${index + 1}" })
                setDrawLabels(true)
                setDrawAxisLine(true)
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textSize = 12f
                textColor = 0xFFFFFFFF.toInt()
                setDrawGridLines(true)
                setDrawAxisLine(true)
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "RM %.0f".format(value)
                    }
                }
            }

            axisRight.isEnabled = false

            description.apply {
                text = "Spending over the last 7 days"
                textColor = 0xFFFFFFFF.toInt()
                isEnabled = false
            }

            legend.apply {
                textSize = 12f
                textColor = 0xFFFFFFFF.toInt()
                isEnabled = true
            }

            setFitBars(true)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.btnAddExpense.setOnClickListener(null)
        _binding = null
    }
}