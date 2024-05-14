package my.edu.tarc.debtdecoderApp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private var basicIncomeMap = hashMapOf<String, Float>()
    private var adHocIncomeMap = hashMapOf<String, Float>()
    private val availableMonths = hashSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("Pie Chart Null pointer", "line chart set bg color ")
        setupLineChart()
        setupPieChart()
        fetchDataAndPlot()
    }

    private fun setupLineChart() {
        lineChart = binding.lineChart
        binding.textViewDashboard.text = "TOTAL SAVING PER MONTH"
        lineChart.setBackgroundColor(getResources().getColor(android.R.color.white))
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true
    }

    private fun setupPieChart() {
        pieChart = binding.pieChart
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = false
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK)
        pieChart.legend.isEnabled = true
    }

    private fun fetchDataAndPlot() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val savingsMap = hashMapOf<String, Float>()
                basicIncomeMap.clear()
                adHocIncomeMap.clear()
                availableMonths.clear() // Clear available months before populating

                Log.e("Pie Chart Null pointer", "savings Map $savingsMap ")

                processIncomeData(snapshot.child("basicIncome"), savingsMap, "yyyy-MM", 1, basicIncomeMap)
                processIncomeData(snapshot.child("adHocIncome"), savingsMap, "MMM dd, yyyy", 1, adHocIncomeMap)
                processExpenseData(snapshot.child("expenses"), savingsMap, "yyyy-MM-dd", -1)

                updateGraph(savingsMap)
                setupMonthSpinner()
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase data fetch cancelled or failed: ${error.message}")
            }
        })
    }

    private fun processIncomeData(node: DataSnapshot, dataMap: HashMap<String, Float>, dateFormatString: String, factor: Int, incomeMap: HashMap<String, Float>) {
        val sdf = SimpleDateFormat(dateFormatString, Locale.getDefault())
        node.children.forEach { group ->
            group.children.forEach { record ->
                val amount = record.child("amount").getValue(Int::class.java) ?: return@forEach
                val date = record.child("date").getValue(String::class.java) ?: return@forEach
                val parsedDate = sdf.parse(date) ?: return@forEach
                val monthKey = dateFormat.format(parsedDate)
                dataMap[monthKey] = (dataMap[monthKey] ?: 0f) + (amount * factor)
                incomeMap[monthKey] = (incomeMap[monthKey] ?: 0f) + amount
                availableMonths.add(monthKey) // Add month to available months set
            }
        }
    }

    private fun processExpenseData(node: DataSnapshot, dataMap: HashMap<String, Float>, dateFormatString: String, factor: Int) {
        val sdf = SimpleDateFormat(dateFormatString, Locale.getDefault())
        node.children.forEach {
            val amount = it.child("amount").getValue(Int::class.java) ?: return@forEach
            val date = it.child("date").getValue(String::class.java) ?: return@forEach
            val parsedDate = sdf.parse(date) ?: return@forEach
            val monthKey = dateFormat.format(parsedDate)
            dataMap[monthKey] = (dataMap[monthKey] ?: 0f) + (amount * factor)
            availableMonths.add(monthKey) // Add month to available months set
        }
    }

    private fun updateGraph(savingsMap: Map<String, Float>) {
        val savingsEntries = ArrayList<Entry>()
        val monthKeys = savingsMap.keys.sorted()
        val formattedMonths = ArrayList<String>()

        monthKeys.forEachIndexed { index, month ->
            savingsMap[month]?.let {
                savingsEntries.add(Entry(index.toFloat(), it))
            }
            formattedMonths.add(month)
        }

        val savingsDataSet = LineDataSet(savingsEntries, "Total Savings")
        savingsDataSet.color = getResources().getColor(R.color.redosh)
        savingsDataSet.setCircleColor(getResources().getColor(R.color.redosh))
        savingsDataSet.circleRadius = 4f
        savingsDataSet.setDrawCircleHole(false)
        savingsDataSet.setDrawValues(true)
        savingsDataSet.valueTextSize = 10f
        savingsDataSet.valueTextColor = getResources().getColor(R.color.black)

        // Custom ValueFormatter to adjust the position of labels
        savingsDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                // Adjust the position by prepending newline character to separate label from point
                return "\n${entry?.y}"
            }
        }

        val lineData = LineData(savingsDataSet)
        lineChart.data = lineData

        // Configure X-axis
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(formattedMonths)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true

        lineChart.invalidate() // Refresh the graph
    }

    private fun updatePieChart(basicIncomeMap: Map<String, Float>, adHocIncomeMap: Map<String, Float>, selectedMonth: String) {
        val entries = ArrayList<PieEntry>()
        val basicIncome = basicIncomeMap[selectedMonth] ?: 0f
        val adHocIncome = adHocIncomeMap[selectedMonth] ?: 0f

        if (basicIncome > 0) entries.add(PieEntry(basicIncome, "Basic Income"))
        if (adHocIncome > 0) entries.add(PieEntry(adHocIncome, "Ad-Hoc Income"))

        val dataSet = PieDataSet(entries, "Income Distribution")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.valueTextSize = 16f
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    private fun setupMonthSpinner() {
        val months = availableMonths.toList().sorted() // Sort the available months

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = adapter

        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedMonth = parent.getItemAtPosition(position).toString()
                // Fetch data again or update charts based on the selected month
                updatePieChart(basicIncomeMap, adHocIncomeMap, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
