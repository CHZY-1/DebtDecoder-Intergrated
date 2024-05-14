package my.edu.tarc.debtdecoderApp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import my.edu.tarc.debtdecoder.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid }
    private val expensesEntries = mutableListOf<Entry>()
    private val colors = arrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId?.let {
            database = FirebaseDatabase.getInstance().getReference("users/$it")
            fetchFinancialData()
        }
    }

    private fun fetchFinancialData() {
        fetchCategoryData("expenses", "Expenses")
    }

    private fun fetchCategoryData(category: String, label: String) {
        database.child(category).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                processExpenseData(dataSnapshot, label)
            }
        }.addOnFailureListener {
            Log.e("DashboardFragment", "Failed to fetch $label", it)
        }
    }

    private fun processExpenseData(dataSnapshot: DataSnapshot, label: String) {
        dataSnapshot.children.forEach { expenseSnapshot ->
            val amount = expenseSnapshot.child("amount").getValue(Float::class.java) ?: 0f
            val dateStr = expenseSnapshot.child("date").getValue(String::class.java) ?: "Unknown date"
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateStr)
                val timestamp = date?.time?.toFloat() ?: 0f
                expensesEntries.add(Entry(timestamp, amount))
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error parsing date or data", e)
            }
        }
        setupLineChart()
    }

    private fun setupLineChart() {
        val lineChart = binding.lineChart
        val dataSet = LineDataSet(expensesEntries, "Expenses Over Time").apply {
            color = Color.BLACK
            valueTextColor = Color.BLACK
            lineWidth = 2.5f
            setCircleColor(Color.DKGRAY)
            circleRadius = 5f
            fillAlpha = 65
            fillColor = Color.LTGRAY
            highLightColor = Color.YELLOW
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.valueFormatter = DateAxisValueFormatter()
        lineChart.description.isEnabled = false
        lineChart.setNoDataText("No expense data available")
        lineChart.animateXY(2000, 2000)
        lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DateAxisValueFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        private val dateFormat = SimpleDateFormat("MM-dd", Locale.US)

        override fun getFormattedValue(value: Float): String {
            return dateFormat.format(Date(value.toLong()))
        }
    }
}
