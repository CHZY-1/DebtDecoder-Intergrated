package com.example.expenses_and_budget_mobileassignment.expenses

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.expenses_and_budget_mobileassignment.data.FirebaseExpensesHelper
import com.example.expenses_and_budget_mobileassignment.data.SharedDateViewModel
import com.example.expenses_and_budget_mobileassignment.util.DateFormatter
import com.example.expenses_and_budget_mobileassignment.util.getFirebaseHelperInstance
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentTrackExpenseBinding
import java.text.DateFormatSymbols
import java.util.Calendar

class TrackExpenseFragment: Fragment() {

    private val calendar = Calendar.getInstance()
    private var _binding: FragmentTrackExpenseBinding? = null
    private val binding get() = _binding!!
    private lateinit var expensesFirebaseHelper: FirebaseExpensesHelper

    private val dateViewModel: SharedDateViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        expensesFirebaseHelper = getFirebaseHelperInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getMonthsAndYearsArray("userId1") { months, years, mostRecentMonth, mostRecentYear ->

                binding.expenseSpinnerMonth.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    months
                )

                binding.expenseSpinnerYear.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    years
                )

                val defaultMonthIndex = mostRecentMonth?.let { months?.indexOf(it) } ?: 0
                val defaultYearIndex = mostRecentYear?.let { years?.indexOf(it) } ?: 2024

                // Set default month and year to most recent date
                binding.expenseSpinnerMonth.setSelection(defaultMonthIndex)
                binding.expenseSpinnerYear.setSelection(defaultYearIndex)

                // Handle selection changes
                val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        updateCalendar("userId1")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
                binding.expenseSpinnerMonth.onItemSelectedListener = onItemSelectedListener
                binding.expenseSpinnerYear.onItemSelectedListener = onItemSelectedListener
            }
    }

    // Get months and years that exist in the user expense list object
    private fun getMonthsAndYearsArray(userId: String, onResult: (Array<String>, Array<String>, String, String) -> Unit) {
        expensesFirebaseHelper.getExpenses(userId) { expenses, _ ->

            if(isAdded && _binding != null){
            val months = expenses.mapNotNull { expense ->
                val dateParts = expense.date.split("-")
                val monthIndex = dateParts.getOrNull(1)?.toIntOrNull()
                val month = if (monthIndex != null && monthIndex in 1..12) {
                    DateFormatSymbols().months[monthIndex - 1]
                } else {
                    null
                }
//                Log.d("MonthLog", "Expense date: ${expense.date}, Month: $month")
                month
            }.distinct().toTypedArray()

            val years = expenses.mapNotNull { expense ->
                val dateParts = expense.date.split("-")
                val year = dateParts.getOrNull(0)?.toIntOrNull()?.toString()
//                Log.d("YearLog", "Expense date: ${expense.date}, Year: $year")
                year
            }.distinct().toTypedArray()

            val (mostRecentMonth, mostRecentYear) = expenses.maxByOrNull { it.date }?.let { expense ->
                val dateParts = expense.date.split("-")
                val month = dateParts.getOrNull(1)?.toIntOrNull()?.let { monthIndex ->
                    DateFormatSymbols().months.getOrNull(monthIndex - 1) ?: ""
                } ?: ""
                val year = dateParts.getOrNull(0)?.toIntOrNull()?.toString() ?: ""
                Pair(month, year)
            } ?: Pair("", "")

            onResult(months, years, mostRecentMonth, mostRecentYear)
        }
            }
    }

    private fun updateCalendar(userId: String) {

//         // Clear the previous calendar grid
//        binding.expenseCalendarGrid.removeAllViews()

        // Important to prevent null pointer exception when fragment is not attached
        // Do not remove this line.
        if (!isAdded || _binding == null) {
            return
        }

        if(isAdded) {

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Get today's date
            val today = Calendar.getInstance()
            val todayYear = today.get(Calendar.YEAR)
            val todayMonth = today.get(Calendar.MONTH)
            val todayDay = today.get(Calendar.DAY_OF_MONTH)


            val month = DateFormatter.getMonthNumber(binding.expenseSpinnerMonth.selectedItem.toString())
            val year = binding.expenseSpinnerYear.selectedItem.toString().toInt()
//        Log.d("ExpenseLog", "Selected month: $month")
//        Log.d("ExpenseLog", "Selected Year: $year")

            expensesFirebaseHelper.getExpenses(userId) { expenses, _ ->

                if (!isAdded || _binding == null) {
                    return@getExpenses
                }


                // filter expenses by month and year
                val selectedMonthExpenses = expenses.filter { expense ->
                    val dateParts = expense.date.split("-")
                    val expenseYear = dateParts.getOrNull(0)?.toIntOrNull()
                    val expenseMonth = dateParts.getOrNull(1)?.toIntOrNull()
                    expenseYear == year && expenseMonth == month
                }

//            Log.d("TrackCreateDate", "CallBack")

//                Log.d("ExpenseLog", "Selected month expenses: $selectedMonthExpenses")
                binding.expenseCalendarGrid.removeAllViews()

                // Create and add date views
                for (day in 1..daysInMonth) {
                    val textView = createDateView(day)
//                Log.d("TrackCreateDate", "Created day :  $day")

                    val isToday =
                        (year == todayYear && month - 1 == todayMonth && day == todayDay)

                    // set border for today's date
                    if (isToday) {
                        textView.setBackgroundResource(R.drawable.expense_calender_frame_today)
                    }

                    //            val hasExpenses = day % 2 == 0

                    val hasExpenses = selectedMonthExpenses.any { expense ->
                        val dateParts = expense.date.split("-")
                        val expenseDay = dateParts.getOrNull(2)?.toIntOrNull()
                        expenseDay == day
                    }

                    if (hasExpenses) {
                        textView.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_blue_dark
                            )
                        )
                        textView.setOnClickListener {
//                            Log.d("TrackExpenseFragment", "Clicked on day $day")
                            dateViewModel.selectDate(year, month - 1, day)

                            // navigation to fragment that allows user to edit, or delete expenses
                            findNavController().navigate(R.id.action_navigation_expense_to_editExpensesListingFragment)
                        }
                    } else {
                        textView.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                android.R.color.darker_gray
                            )
                        )
                    }

                    binding.expenseCalendarGrid.addView(textView)
                }
            }
            }
        }


    private fun createDateView(day: Int): TextView {

        // Check if the fragment is attached
        if (!isAdded) {
            return TextView(requireContext()).apply {
                text = "N/A"
            }

        }

        val textView = TextView(requireContext())
        textView.text = day.toString()
        textView.gravity = android.view.Gravity.CENTER
        textView.setPadding(16, 16, 16, 16)
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 0
        layoutParams.height = 0
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.setMargins(4, 4, 4, 4)
        textView.layoutParams = layoutParams

        return textView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}