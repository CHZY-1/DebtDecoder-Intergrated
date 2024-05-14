package my.edu.tarc.debtdecoderApp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoderApp.income.AdHocIncomeAdapter
import my.edu.tarc.debtdecoderApp.income.AdHocIncomeItem
import my.edu.tarc.debtdecoderApp.income.IncomeViewModel
import java.text.SimpleDateFormat
import java.util.*

class IncomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdHocIncomeAdapter
    private lateinit var monthlyIncomeChart: BarChart
    private lateinit var basicIncomeTextView: TextView
    private lateinit var incomeViewModel: IncomeViewModel
    private lateinit var editButton: Button
    private val adHocIncomeList = mutableListOf<AdHocIncomeItem>()
    private var allAdHocIncomeList =
        mutableListOf<AdHocIncomeItem>() // Holds all ad hoc income data for filtering
    private val basicIncomeTotals = mutableMapOf<String, Float>()
    private var selectedMonthForEditing: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_income, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        recyclerView = view.findViewById(R.id.ad_hoc_income_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdHocIncomeAdapter(mutableListOf()) { adHocIncome ->
            showAdHocIncomeOptionsDialog(adHocIncome)
        }
        recyclerView.adapter = adapter

        val navController = findNavController()
        val adIncomeLayout: View = view.findViewById(R.id.addIncome_button)
        adIncomeLayout.setOnClickListener {
            navController.navigate(R.id.action_income_to_addIncome)
        }

        basicIncomeTextView = view.findViewById(R.id.basic_income)

        monthlyIncomeChart = view.findViewById(R.id.monthly_income_chart)

        incomeViewModel = ViewModelProvider(requireActivity()).get(IncomeViewModel::class.java)

        allAdHocIncomeList = mutableListOf()
        loadIncomeData()
        editButton = view.findViewById(R.id.edit_button)
        editButton.setOnClickListener {
            selectedMonthForEditing?.let { month ->
                showEditBasicIncomeDialog(month)
            }
        }

        val addBasicIncomeButton: Button = view.findViewById(R.id.addBasicIncome_button)
        addBasicIncomeButton.setOnClickListener {
            showAddBasicIncomeDialog()
        }

        val viewAllBasicIncomeButton: Button = view.findViewById(R.id.viewAllBasicIncome_button)
        viewAllBasicIncomeButton.setOnClickListener {
            showAllBasicIncomeDialog()
        }
    }

    private fun loadIncomeData(selectedMonth: String? = null) {
        val currentUser = auth.currentUser ?: return

        val userBasicIncomeRef = database.child("users/${currentUser.uid}/basicIncome")
        val userAdHocIncomeRef = database.child("users/${currentUser.uid}/adHocIncome")

        val adHocIncomeTotals = mutableMapOf<String, Float>()
        basicIncomeTotals.clear()
        allAdHocIncomeList.clear()

        userBasicIncomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { monthSnapshot ->
                    val month = monthSnapshot.key ?: return@forEach
                    monthSnapshot.children.forEach { incomeSnapshot ->
                        val amount =
                            incomeSnapshot.child("amount").getValue(Double::class.java)?.toFloat()
                                ?: 0f
                        basicIncomeTotals[month] =
                            basicIncomeTotals.getOrDefault(month, 0f) + amount
                    }
                }
                loadAdHocIncomeData(userAdHocIncomeRef, adHocIncomeTotals, selectedMonth)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomeFragment", "Error loading basic income data: ${error.message}")
            }
        })
    }

    private fun loadAdHocIncomeData(
        userAdHocIncomeRef: DatabaseReference,
        adHocIncomeTotals: MutableMap<String, Float>,
        selectedMonth: String?
    ) {
        userAdHocIncomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempAdHocIncomeList = mutableListOf<AdHocIncomeItem>()
                snapshot.children.forEach { monthSnapshot ->
                    monthSnapshot.children.forEach { incomeSnapshot ->
                        try {
                            val adHocItem = incomeSnapshot.getValue(AdHocIncomeItem::class.java)
                                ?.copy(key = incomeSnapshot.key ?: "")
                            adHocItem?.let {
                                tempAdHocIncomeList.add(it)
                                val amount = it.amount.toFloat()
                                adHocIncomeTotals[monthSnapshot.key ?: ""] =
                                    adHocIncomeTotals.getOrDefault(
                                        monthSnapshot.key ?: "",
                                        0f
                                    ) + amount
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "IncomeFragment",
                                "Error parsing ad hoc income item: ${e.message}"
                            )
                        }
                    }
                }
                allAdHocIncomeList.addAll(tempAdHocIncomeList)

                val finalIncomeTotals = mutableMapOf<String, Float>()
                (basicIncomeTotals.keys + adHocIncomeTotals.keys).forEach { month ->
                    val basicTotal = basicIncomeTotals.getOrDefault(month, 0f)
                    val adHocTotal = adHocIncomeTotals.getOrDefault(month, 0f)
                    finalIncomeTotals[month] = basicTotal + adHocTotal
                }

                populateMonthlyIncomeChart(finalIncomeTotals)
                if (selectedMonth != null) {
                    filterAdHocIncomeForMonth(selectedMonth)
                } else {
                    adapter.updateData(mutableListOf())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomeFragment", "Error loading ad hoc income data: ${error.message}")
            }
        })
    }

    private fun showAdHocIncomeOptionsDialog(adHocIncome: AdHocIncomeItem) {
        val options = arrayOf("Edit", "Delete")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select an option")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditAdHocIncomeDialog(adHocIncome)
                1 -> confirmDeleteAdHocIncome(adHocIncome)
            }
        }
        builder.show()
    }

    private fun showEditAdHocIncomeDialog(adHocIncome: AdHocIncomeItem) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Ad Hoc Income")

        val dialogView = layoutInflater.inflate(R.layout.edit_ad_hoc_income_dialog, null)
        val titleInput: EditText = dialogView.findViewById(R.id.titleInput)
        val amountInput: EditText = dialogView.findViewById(R.id.amountInput)
        val dateInput: EditText = dialogView.findViewById(R.id.dateInput)
        val notesInput: EditText = dialogView.findViewById(R.id.notesInput)

        titleInput.setText(adHocIncome.title)
        amountInput.setText(adHocIncome.amount.toString())
        dateInput.setText(adHocIncome.date)
        notesInput.setText(adHocIncome.notes)

        builder.setView(dialogView)

        builder.setPositiveButton("Update") { dialog, _ ->
            val updatedTitle = titleInput.text.toString()
            val updatedAmount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
            val updatedDate = dateInput.text.toString()
            val updatedNotes = notesInput.text.toString()

            updateAdHocIncome(adHocIncome, updatedTitle, updatedAmount, updatedDate, updatedNotes)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun confirmDeleteAdHocIncome(adHocIncome: AdHocIncomeItem) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete this ad hoc income?")
        builder.setPositiveButton("Delete") { _, _ ->
            deleteAdHocIncome(adHocIncome)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updateAdHocIncome(
        adHocIncome: AdHocIncomeItem,
        title: String,
        amount: Double,
        date: String,
        notes: String?
    ) {
        val currentUser = auth.currentUser ?: return
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val month = monthFormat.format(
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(date) ?: Date()
        )

        val userAdHocIncomeRef =
            database.child("users/${currentUser.uid}/adHocIncome/$month/${adHocIncome.key}")

        val updatedAdHocIncome = AdHocIncomeItem(title, amount, date, notes ?: "")
        userAdHocIncomeRef.setValue(updatedAdHocIncome).addOnCompleteListener {
            if (it.isSuccessful) {
                selectedMonthForEditing?.let {
                    loadIncomeData(it)
                }
                Toast.makeText(requireContext(), "Ad Hoc Income updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to update Ad Hoc Income: ${it.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteAdHocIncome(adHocIncome: AdHocIncomeItem) {
        val currentUser = auth.currentUser ?: return
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val month = monthFormat.format(
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(adHocIncome.date) ?: Date()
        )

        val userAdHocIncomeRef =
            database.child("users/${currentUser.uid}/adHocIncome/$month/${adHocIncome.key}")

        userAdHocIncomeRef.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                selectedMonthForEditing?.let {
                    loadIncomeData(it)
                }
                Toast.makeText(requireContext(), "Ad Hoc Income deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to delete Ad Hoc Income: ${it.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showAllBasicIncomeDialog() {
        val items = basicIncomeTotals.map { (month, amount) ->
            "$month: RM$amount"
        }.toTypedArray()

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("All Basic Income")

        builder.setItems(items, null)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.listView.setOnItemClickListener { _, _, position, _ ->
            val month = items[position].split(":")[0]
            confirmDeleteBasicIncome(month)
        }
        dialog.show()
    }

    private fun confirmDeleteBasicIncome(month: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete the basic income for $month?")
        builder.setPositiveButton("Delete") { _, _ ->
            deleteBasicIncome(month)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun deleteBasicIncome(month: String) {
        val currentUser = auth.currentUser ?: return
        val userBasicIncomeMonthRef = database.child("users/${currentUser.uid}/basicIncome/$month")
        userBasicIncomeMonthRef.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                selectedMonthForEditing?.let {
                    loadIncomeData(it)
                }
                Toast.makeText(
                    requireContext(),
                    "Deleted basic income for $month",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to delete basic income: ${it.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showAddBasicIncomeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Basic Income")

        val dialogView = layoutInflater.inflate(R.layout.add_income_dialog, null)
        val yearPicker: NumberPicker = dialogView.findViewById(R.id.yearPicker)
        val monthPicker: NumberPicker = dialogView.findViewById(R.id.monthPicker)
        val amountInput: EditText = dialogView.findViewById(R.id.amountInput)
        amountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear

        val months = arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
        monthPicker.minValue = 0
        monthPicker.maxValue = months.size - 1
        monthPicker.displayedValues = months

        builder.setView(dialogView)

        builder.setPositiveButton("Add") { dialog, _ ->
            val year = yearPicker.value
            val month = months[monthPicker.value]
            val formattedDate = "$year-$month"
            val amount = amountInput.text.toString().toFloatOrNull()

            if (amount != null) {
                addBasicIncome(formattedDate, amount)
            } else {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", { dialog, which -> dialog.cancel() })

        builder.show()
    }

    private fun addBasicIncome(monthYear: String, amount: Float) {
        val currentUser = auth.currentUser ?: return
        val userBasicIncomeMonthRef =
            database.child("users/${currentUser.uid}/basicIncome/$monthYear")

        userBasicIncomeMonthRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    userBasicIncomeMonthRef.push().setValue(mapOf("amount" to amount))
                    selectedMonthForEditing?.let {
                        loadIncomeData(it)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Income already added for $monthYear",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomeFragment", "Failed to check existing income: ${error.message}")
            }
        })
    }

    private fun populateMonthlyIncomeChart(monthlyIncome: Map<String, Float>) {
        val currentMonth =
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
        val currentMonthIncome = monthlyIncome[currentMonth] ?: 0f

        val latestMonths = monthlyIncome.keys.sortedDescending().take(4).reversed()
        val entries = latestMonths.mapIndexed { index, month ->
            BarEntry(index.toFloat(), monthlyIncome[month] ?: 0f)
        }

        val barDataSet = BarDataSet(entries, "Monthly Income")
        barDataSet.setDrawValues(true)
        barDataSet.valueTextColor = Color.WHITE
        val barData = BarData(barDataSet)
        barData.barWidth = 0.4f

        monthlyIncomeChart.apply {
            data = barData
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(latestMonths)
                textSize = 12f
                textColor = Color.WHITE
                setDrawGridLines(false)
                labelRotationAngle = 0f
            }
            axisLeft.apply {
                textSize = 12f
                textColor = Color.WHITE
                setDrawGridLines(true)
                granularity = 1f
                axisMinimum = 0f
            }
            description = Description().apply {
                text = ""
            }
            axisRight.isEnabled = false
            setExtraTopOffset(30f)
            legend.isEnabled = false

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val index = it.x.toInt()
                        val selectedMonth = latestMonths[index]
                        Log.d("ChartSelection", "Month selected: $selectedMonth")
                        selectedMonthForEditing = selectedMonth
                        filterAdHocIncomeForMonth(selectedMonth)
                        basicIncomeTextView.text = "RM${basicIncomeTotals[selectedMonth] ?: 0f}"
                    }
                }

                override fun onNothingSelected() {
                    Log.d("ChartSelection", "No selection")
                    adapter.updateData(mutableListOf())  // Clear the list if no bar is selected
                }
            })

            invalidate()
            incomeViewModel.updateCurrentMonthIncome(currentMonthIncome)
        }
    }

    private fun showEditBasicIncomeDialog(month: String) {
        val currentAmount = basicIncomeTotals[month] ?: 0f

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Basic Income for $month")

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentAmount.toString())
        }
        builder.setView(input)

        builder.setPositiveButton("Update") { dialog, _ ->
            val newAmount = input.text.toString().toFloatOrNull()
            if (newAmount != null) {
                val currentUser = auth.currentUser ?: return@setPositiveButton
                val userBasicIncomeMonthRef =
                    database.child("users/${currentUser.uid}/basicIncome/$month")

                userBasicIncomeMonthRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val child = snapshot.children.firstOrNull()
                        if (child != null) {
                            child.ref.child("amount").setValue(newAmount).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    basicIncomeTotals[month] = newAmount
                                    basicIncomeTextView.text = "RM$newAmount"
                                    selectedMonthForEditing?.let {
                                        loadIncomeData(it)
                                    }
                                } else {
                                    Log.e(
                                        "IncomeFragment",
                                        "Error updating basic income: ${it.exception?.message}"
                                    )
                                }
                            }
                        } else {
                            Log.e("IncomeFragment", "No entry found to update for month: $month")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("IncomeFragment", "Error loading data for update: ${error.message}")
                    }
                })
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun filterAdHocIncomeForMonth(month: String) {
        val filteredList = allAdHocIncomeList.filter {
            extractYearMonthFromDate(it.date) == month
        }
        adapter.updateData(filteredList)
    }

    private fun extractYearMonthFromDate(date: String?): String {
        if (date == null) return "Unknown"
        val inputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        return try {
            val parsedDate = inputFormat.parse(date)
            parsedDate?.let {
                outputFormat.format(it)
            } ?: "Unknown"
        } catch (e: Exception) {
            Log.e("DateParsing", "Error parsing date: $date, Error: ${e.message}")
            "Unknown"
        }
    }
}
