package my.edu.tarc.debtdecoderApp.expenses

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import my.edu.tarc.debtdecoderApp.data.Expense
import my.edu.tarc.debtdecoderApp.data.ExpenseCategory
import my.edu.tarc.debtdecoderApp.data.FirebaseExpensesHelper
import com.example.expenses_and_budget_mobileassignment.expenses.CategoryPickerFragment
import my.edu.tarc.debtdecoderApp.util.DateFormatter
import my.edu.tarc.debtdecoderApp.util.GlideImageLoader
import com.example.expenses_and_budget_mobileassignment.util.getFirebaseHelperInstance
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentAddExpenseBinding
import my.edu.tarc.debtdecoderApp.MainActivity
import java.util.Calendar

// interface for update add expense UI when an expense category is selected in ExpenseCategoryPicker
// https://stackoverflow.com/questions/58418931/how-does-an-interface-in-android-studio-work
interface CategorySelectionListener {
    fun onCategorySelected(category: ExpenseCategory)
}

class AddExpenseFragment : Fragment(), CategorySelectionListener {
    private var _binding: FragmentAddExpenseBinding? = null
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
        // Inflate the layout with view binding
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isAdded) {
            (requireActivity() as MainActivity).toggleHeaderSyncBtnVisibility(true)

            binding.cardExpenseCategory.setOnClickListener {
                showCategoryPicker()
            }

            // Payment Method spinner
            val spinner = binding.spinnerExpensePayment
            spinner.setSelection(0)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    // Update the icon based on the selected item
                    val selectedMethod = parent.getItemAtPosition(position) as String
                    val iconResId = when (selectedMethod) {
                        "Cash" -> R.drawable.icon_payment_cash
                        "E Wallet" -> R.drawable.icon_payment_ewallet
                        "Credit/Debit Card" -> R.drawable.icon_payment_creditcard
                        "Online Banking" -> R.drawable.icon_payment_onlinebanking
                        else -> android.R.drawable.arrow_up_float // Default icon
                    }
                    binding.ivExpensePayment.setImageResource(iconResId)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }

            // Date Picker
            binding.ibtnCalendar.setOnClickListener {
                showDatePicker()
            }

            // Add expense button
            binding.btnAddExpense.setOnClickListener {
                if (inputsNotEmpty()) {
                    val amount = binding.etExpenseAmount.text.toString().toDouble()
                    val category = binding.tvExpenseCategoryName.text.toString().trim()
                    val paymentMethod = binding.spinnerExpensePayment.selectedItem.toString().trim()
                    val userInputDate = binding.tvExpenseDate.text.toString()
                    val formattedDate = DateFormatter.formatForFirebase(userInputDate)
                    val recurrence = binding.spinnerExpenseRecurrentDate.selectedItem.toString()
                    val remark = binding.etExpenseRemark.text.toString().trim()

                    if (formattedDate != null) {
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let { firebaseUser ->
                            val userID = firebaseUser.uid
                            val expense = Expense(
                                "",
                                amount,
                                category,
                                paymentMethod,
                                formattedDate,
                                recurrence,
                                remark
                            )

                            addExpense(userID, expense) { isSuccess ->
                                if (isAdded) {
                                    if (isSuccess) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Expense added successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        findNavController().popBackStack()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to add expense",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "No Date Selected", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please fill in all fields. Remark is optional.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }

    private fun showCategoryPicker() {
        // Implement category picker logic
        val pickerFragment = CategoryPickerFragment(this)
        pickerFragment.show(parentFragmentManager, pickerFragment.tag)
    }

    override fun onCategorySelected(category: ExpenseCategory) {
        binding.tvExpenseCategoryName.text = category.categoryName
        GlideImageLoader().loadCategoryImage(category.imageUrl, binding.ivExpenseCategory, requireContext())
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
//            R.style.DialogButtonStyle,
            { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            binding.tvExpenseDate.text = selectedDate
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun inputsNotEmpty(): Boolean {
        return binding.etExpenseAmount.text?.isNotBlank() ?: false &&
                binding.tvExpenseCategoryName.text.isNotBlank() &&
                binding.tvExpenseDate.text.isNotBlank()
    }

    private fun addExpense(userID: String, expense: Expense, callback: (Boolean) -> Unit) {
        expensesFirebaseHelper.addExpense(userID, expense) { isSuccess, expenseId ->
            if (isAdded && _binding != null) {
                if (isSuccess) {
                    Log.d(
                        "AddExpense",
                        "Expense added successfully with Amount: ${expense.amount} and Category Name: ${expense.category}"
                    )
                } else {
                    Log.e("AddExpense", "Failed to add expense")
                }
                callback(isSuccess)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}