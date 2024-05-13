package my.edu.yyass.statusTab

import my.edu.yyass.repaymentTab.LoanDatabaseHelper
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import my.edu.yyass.Loan
import my.edu.yyass.databinding.FragmentToReceiveBinding

class ToReceiveFragment : Fragment() {
    private var _binding: FragmentToReceiveBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: LoanDatabaseHelper
    private var loans = listOf<Loan>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentToReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadLoans("lend")  // Reload loans whenever the fragment resumes
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = LoanDatabaseHelper(requireContext())
        loadLoans("lend")
    }

    private fun loadLoans(type: String) {
        loans = dbHelper.getAllLoans().filter { it.type == type }
        Log.d("ToReceiveFragment", "Loaded loans: ${loans.size}")

        // Calculate total remaining for each category
        val categoryTotals = loans.groupBy { it.loan_category }
            .mapValues { entry ->
                entry.value.sumOf { it.amount - it.amount_paid }
            }

        setupDonutChart(loans, categoryTotals)
        setupRecyclerView(loans)
    }

    private fun setupDonutChart(loans: List<Loan>, categoryTotals: Map<String, Double>) {
        val entries = ArrayList<PieEntry>()
        val totalAmount = categoryTotals.values.sum()
        val mutableCategoryTotals = categoryTotals.toMutableMap()
        val threshold = totalAmount * 0.05
        var miscellaneousDetails = mutableListOf<ToPayFragment.CategoryDetail>()

        categoryTotals.forEach { (category, amount) ->
            if (amount < threshold) {
                miscellaneousDetails.add(ToPayFragment.CategoryDetail(category, amount))
                mutableCategoryTotals.remove(category)
            }
        }

        if (miscellaneousDetails.isNotEmpty()) {
            val miscellaneousTotal = miscellaneousDetails.sumOf { it.amount }
            mutableCategoryTotals["Miscellaneous"] = miscellaneousTotal
            entries.add(PieEntry((miscellaneousTotal / totalAmount).toFloat(), "Miscellaneous", miscellaneousDetails))
        }

        mutableCategoryTotals.forEach { (category, amount) ->
            if (category != "Miscellaneous") {
                entries.add(PieEntry((amount / totalAmount).toFloat(), category, listOf(
                    ToPayFragment.CategoryDetail(category, amount)
                )))
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            sliceSpace = 3f
            selectionShift = 5f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter()  // Continue to display only percentages
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.donutChartToReceive))  // Set formatter to display percentages
        }

        binding.donutChartToReceive.apply {
            legend.textColor = Color.WHITE
            this.data = data
            setHoleColor(Color.BLACK)
            setCenterTextColor(Color.WHITE)
            description.isEnabled = false
            centerText = "Total Debt: RM ${String.format("%.2f", totalAmount)}"
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            animateY(1400, Easing.EaseInOutQuad)

            val mv = CustomMarkerView(requireContext())
            mv.chartView = this  // Bind the marker view to the chart
            marker = mv

            invalidate()  // Refresh the chart
        }
    }

    private fun setupRecyclerView(loans: List<Loan>) {
        val adapter = StatusLoanAdapter(loans)
        binding.recyclerViewToReceiveCards.adapter = adapter
        binding.recyclerViewToReceiveCards.layoutManager = LinearLayoutManager(context)
        if (loans.isEmpty()) {
            Log.d("ToReceiveFragment", "RecyclerView has no loans to display.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}