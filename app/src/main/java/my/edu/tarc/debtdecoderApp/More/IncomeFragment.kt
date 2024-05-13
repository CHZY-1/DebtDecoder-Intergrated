package com.example.expenses_and_budget_mobileassignment.More

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoder.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private var allAdHocIncomeList = mutableListOf<AdHocIncomeItem>() // Holds all ad hoc income data for filtering
    private val basicIncomeTotals = mutableMapOf<String, Float>()
    private var selectedMonthForEditing: String? = null

    override fun onResume() {
        super.onResume()

//        activity?.findViewById<TextView>(R.id.header_title)?.let {
//            it.text = "Income"
//        }

        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_income, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure correct Firebase initialization
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize RecyclerView and adapter
        recyclerView = view.findViewById(R.id.ad_hoc_income_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdHocIncomeAdapter(allAdHocIncomeList) { adHocIncome ->
            showAdHocIncomeDetailsDialog(adHocIncome)
        }
        recyclerView.adapter = adapter

        val navController = findNavController()
        val adIncomeLayout: View = view.findViewById(R.id.addIncome_button)
        adIncomeLayout.setOnClickListener {
            navController.navigate(R.id.action_income_to_addIncome)
        }

//        val backButton = activity?.findViewById<ImageView>(R.id.back_button)
//        backButton?.setOnClickListener {
//            navController.navigate(R.id.action_income_to_more)
//        }

        basicIncomeTextView = view.findViewById(R.id.basic_income)

        // Initialize the chart
        monthlyIncomeChart = view.findViewById(R.id.monthly_income_chart)

        // ViewModel setup using ViewModelProvider for obtaining a ViewModel.
        incomeViewModel = ViewModelProvider(requireActivity()).get(IncomeViewModel::class.java)

        // Load aggregated income data
        allAdHocIncomeList = mutableListOf()
        loadIncomeData()
        editButton = view.findViewById(R.id.edit_button)
        editButton.setOnClickListener {
            selectedMonthForEditing?.let { month ->
                // Launch an edit dialog or fragment
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

    private fun loadIncomeData() {
        val currentUser = auth.currentUser ?: return

        // Load basic and ad hoc income data by month
        val userBasicIncomeRef = database.child("users/${currentUser.uid}/basicIncome")
        val userAdHocIncomeRef = database.child("users/${currentUser.uid}/adHocIncome")

        // Initialize structures to hold monthly totals
        val adHocIncomeTotals = mutableMapOf<String, Float>()
        basicIncomeTotals.clear()
        allAdHocIncomeList.clear()

        // Load basic income data by month
        userBasicIncomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { monthSnapshot ->
                    val month = monthSnapshot.key ?: return@forEach
                    monthSnapshot.children.forEach { incomeSnapshot ->
                        val amount = incomeSnapshot.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                        basicIncomeTotals[month] = basicIncomeTotals.getOrDefault(month, 0f) + amount
                    }
                }

                // Proceed to load ad hoc income data after basic income data is loaded
                loadAdHocIncomeData(userAdHocIncomeRef, adHocIncomeTotals)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomeFragment", "Error loading basic income data: ${error.message}")
            }
        })
    }

    private fun loadAdHocIncomeData(userAdHocIncomeRef: DatabaseReference, adHocIncomeTotals: MutableMap<String, Float>) {
        userAdHocIncomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempAdHocIncomeList = mutableListOf<AdHocIncomeItem>() // Temporary list to hold fetched items
                snapshot.children.forEach { monthSnapshot ->
                    monthSnapshot.children.forEach { incomeSnapshot ->
                        val adHocItem = incomeSnapshot.getValue(AdHocIncomeItem::class.java)
                        adHocItem?.let {
                            tempAdHocIncomeList.add(it) // Add to temporary list
                            val amount = it.amount.toFloat()
                            adHocIncomeTotals[monthSnapshot.key ?: ""] = adHocIncomeTotals.getOrDefault(monthSnapshot.key ?: "", 0f) + amount
                        }
                    }
                }

                // Update the master list of all ad hoc income items
                allAdHocIncomeList.addAll(tempAdHocIncomeList)

                // Aggregate the totals
                val finalIncomeTotals = mutableMapOf<String, Float>()
                (basicIncomeTotals.keys + adHocIncomeTotals.keys).forEach { month ->
                    val basicTotal = basicIncomeTotals.getOrDefault(month, 0f)
                    val adHocTotal = adHocIncomeTotals.getOrDefault(month, 0f)
                    finalIncomeTotals[month] = basicTotal + adHocTotal
                }

                // Populate the chart
                populateMonthlyIncomeChart(finalIncomeTotals)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomeFragment", "Error loading ad hoc income data: ${error.message}")
            }
        })
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
                Toast.makeText(requireContext(), "Deleted basic income for $month", Toast.LENGTH_SHORT).show()
                loadIncomeData()  // Refresh data
            } else {
                Toast.makeText(requireContext(), "Failed to delete basic income: ${it.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddBasicIncomeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Basic Income")

        // Custom view for the dialog
        val dialogView = layoutInflater.inflate(R.layout.add_income_dialog, null)
        val yearPicker: NumberPicker = dialogView.findViewById(R.id.yearPicker)
        val monthPicker: NumberPicker = dialogView.findViewById(R.id.monthPicker)
        val amountInput: EditText = dialogView.findViewById(R.id.amountInput)
        amountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Set up year and month pickers
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear

        val months = arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
        monthPicker.minValue = 0
        monthPicker.maxValue = months.size - 1
        monthPicker.displayedValues = months

        builder.setView(dialogView)

        // Define dialog actions
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
        val userBasicIncomeMonthRef = database.child("users/${currentUser.uid}/basicIncome/$monthYear")

        userBasicIncomeMonthRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // No entry exists, can add new
                    userBasicIncomeMonthRef.push().setValue(mapOf("amount" to amount))
                    loadIncomeData()  // Refresh data
                } else {
                    Toast.makeText(requireContext(), "Income already added for $monthYear", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IncomeFragment", "Failed to check existing income: ${error.message}")
            }
        })
    }

    private fun addItemsToAllAdHocIncomeList(items: List<AdHocIncomeItem>) {
        allAdHocIncomeList.addAll(items)
    }
    private fun populateMonthlyIncomeChart(monthlyIncome: Map<String, Float>) {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
        val currentMonthIncome = monthlyIncome[currentMonth] ?: 0f

        // Extract and sort the latest four months
        val latestMonths = monthlyIncome.keys.sortedDescending().take(4).reversed()
        val entries = latestMonths.mapIndexed { index, month ->
            BarEntry(index.toFloat(), monthlyIncome[month] ?: 0f)
        }

        // Create the bar data set
        val barDataSet = BarDataSet(entries, "Monthly Income")
        barDataSet.setDrawValues(true) // Display the values on top of each bar
        barDataSet.valueTextColor = Color.WHITE // Set bar value text color to white
        val barData = BarData(barDataSet)

        // Adjust the bar width for slimmer bars
        barData.barWidth = 0.4f

        // Set up the monthly income chart
        monthlyIncomeChart.apply {
            data = barData

            // X-Axis customization
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM // Place the labels at the bottom
                granularity = 1f // Ensure labels appear for each bar
                valueFormatter = IndexAxisValueFormatter(latestMonths) // Label format based on months
                textSize = 12f // Customize the text size for better readability
                textColor = Color.WHITE // Set the text color to white
                setDrawGridLines(false) // Optional: Hide grid lines on the x-axis
                labelRotationAngle = 0f // Display the labels horizontally
            }

            // Y-Axis customization (left side)
            axisLeft.apply {
                textSize = 12f // Customize the text size for better readability
                textColor = Color.WHITE // Set the text color to white
                setDrawGridLines(true) // Optional: Show grid lines for the y-axis
                granularity = 1f // Increment in whole numbers
                axisMinimum = 0f // Ensure bars start above the x-axis
            }

            description = Description().apply {
                text = "" // Clear description
            }

            // Disable the right axis (optional)
            axisRight.isEnabled = false

            setExtraTopOffset(30f) // Add some space at the top for the title

            legend.isEnabled = false // Optional: Hide the legend for a cleaner look

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
                }
            })

            // Refresh the chart to show updated data and customization
            invalidate()
            incomeViewModel.updateCurrentMonthIncome(currentMonthIncome)
        }
    }

    private fun showEditBasicIncomeDialog(month: String) {
        // Retrieve the current basic income amount for the selected month
        val currentAmount = basicIncomeTotals[month] ?: 0f

        // Build the edit dialog
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Basic Income for $month")

        // Set up an EditText for input
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentAmount.toString())
        }
        builder.setView(input)

        // Define dialog actions
        builder.setPositiveButton("Update") { dialog, _ ->
            // Retrieve the new value
            val newAmount = input.text.toString().toFloatOrNull()
            if (newAmount != null) {
                // Update Firebase with the appropriate identifier
                val currentUser = auth.currentUser ?: return@setPositiveButton
                val userBasicIncomeMonthRef =
                    database.child("users/${currentUser.uid}/basicIncome/$month")

                // Query the unique ID under the month path to update it
                userBasicIncomeMonthRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Assume only one entry per month (modify if there are multiple)
                        val child = snapshot.children.firstOrNull()
                        if (child != null) {
                            // Update the existing record
                            child.ref.child("amount").setValue(newAmount).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    // Update local data
                                    basicIncomeTotals[month] = newAmount
                                    // Refresh the displayed value in the TextView
                                    basicIncomeTextView.text = "RM$newAmount"
                                    // Refresh the data and update the chart
                                    loadIncomeData()
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

    // Function to filter ad hoc income for a specific month
    private fun filterAdHocIncomeForMonth(month: String) {
        // Start by filtering from the master list, which remains unchanged
        val filteredList = allAdHocIncomeList.filter {
            extractYearMonthFromDate(it.date) == month
        }
        // Update the RecyclerView adapter with the newly filtered list
        adapter.updateData(filteredList)
    }

    private fun showAdHocIncomeDetailsDialog(adHocIncome: AdHocIncomeItem) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ad Hoc Income Details")

        val message = """
        Title: ${adHocIncome.title}
        Amount: RM${adHocIncome.amount}
        Date: ${adHocIncome.date}
        Notes: ${adHocIncome.notes ?: "No additional notes"}
    """.trimIndent()

        builder.setMessage(message)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Function to extract the "yyyy-MM" portion from a given date string
    private fun extractYearMonthFromDate(date: String?): String {
        if (date == null) return "Unknown"
        // Assume the date is in a format like "MMM dd, yyyy"
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
