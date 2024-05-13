package my.edu.tarc.debtdecoderApp.addLoansTab

import my.edu.yyass.repaymentTab.LoanDatabaseHelper
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentAddLoanBinding
import java.util.Calendar
import java.util.Locale

class AddLoanFragment : Fragment() {
    private var _binding: FragmentAddLoanBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: LoanDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddLoanBinding.inflate(inflater, container, false)
        dbHelper = LoanDatabaseHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCategories()
        loadCreditors()
        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        // Load predefined categories from strings.xml
        val predefinedCategories = resources.getStringArray(R.array.category_types).toList()
        // Load categories from the database
        val dbCategories = dbHelper.getUniqueCategories()

        // Combine and ensure uniqueness by converting all to lowercase, then back to a list
        val combinedCategories = (predefinedCategories + dbCategories)
            .map { it.lowercase(Locale.getDefault()) } // Normalize to lowercase for uniqueness
            .distinct() // Ensure uniqueness
            .sorted() // Optional: sort the list

        updateCategoryAdapter(combinedCategories)
    }

    private fun updateCategoryAdapter(categories: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.loanCategoryAutocomplete.setAdapter(adapter)

        // Adjust the height of the dropdown to show only three items at once
        val itemHeight = 160
        binding.loanCategoryAutocomplete.dropDownHeight = itemHeight * 3

        // Show all items when the AutoCompleteTextView gains focus
        binding.loanCategoryAutocomplete.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                (v as? AutoCompleteTextView)?.showDropDown()
            }
        }

        // Ensure the dropdown shows even when there's no text input yet (user taps and clears the text)
        binding.loanCategoryAutocomplete.setOnClickListener {
            if (!binding.loanCategoryAutocomplete.isPopupShowing) {
                binding.loanCategoryAutocomplete.showDropDown()
            }
        }
    }

    private fun loadCreditors() {
        val creditors = dbHelper.getUniqueCreditors()
        updateCreditorAdapter(creditors)
    }

    private fun updateCreditorAdapter(creditors: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, creditors)
        binding.loanCreditorAutoComplete.setAdapter(adapter)

        // Adjust the height of the dropdown to show only three items at once
        val itemHeight = 160
        binding.loanCreditorAutoComplete.dropDownHeight = itemHeight * 3

        // Show all items when the AutoCompleteTextView gains focus
        binding.loanCreditorAutoComplete.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                (v as AutoCompleteTextView).showDropDown()
            }
        }

        // Ensure the dropdown shows even when there's no text input yet (user taps and clears the text)
        binding.loanCreditorAutoComplete.setOnClickListener {
            if (!binding.loanCreditorAutoComplete.isPopupShowing) {
                binding.loanCreditorAutoComplete.showDropDown()
            }
        }
    }

    private fun setupButtonListeners() {
        binding.borrowButton.setOnClickListener {
            if (validateInput()) {
                insertLoanDataToDatabase("borrow")
            }
        }

        binding.lendButton.setOnClickListener {
            if (validateInput()) {
                insertLoanDataToDatabase("lend")
            }
        }
        binding.maturityDateButton.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(requireContext(), DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            // Format the date and set it as the button text
            val selectedDate = String.format("%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            binding.maturityDateButton.text = selectedDate
        }, year, month, day)

        dpd.datePicker.minDate = System.currentTimeMillis() - 1000  // This will disable past dates including today
        dpd.show()
    }

    private fun validateInput(): Boolean {
        with(binding) {
            if (loanCategoryAutocomplete.text.toString().trim().isEmpty()) {
                loanCategoryAutocomplete.error = "Please enter a category type"
                return false
            } else {
                loanCategoryAutocomplete.error = null // Clear error if it passes validation
            }
            if (loanCreditorAutoComplete.text.toString().trim().isEmpty()) {
                loanCreditorAutoComplete.error = "Please enter a creditor name"
                return false
            } else {
                loanCreditorAutoComplete.error = null // Clear error if it passes validation
            }

            if (loanNameTextInput.text.toString().trim().isEmpty()) {
                loanNameTextInput.error = "Please enter a loan name"
                return false
            } else {
                loanNameTextInput.error = null // Clear error if it passes validation
            }

            val amount = amountNumberInput.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                amountNumberInput.error = "Please enter a valid positive amount"
                return false
            } else {
                amountNumberInput.error = null // Clear error
            }

            val interestInput = interestNumberInput.text.toString().toDoubleOrNull()
            if (interestInput == null || interestInput < 0) {
                interestNumberInput.error = "Please enter a valid interest rate"
                return false
            } else {
                interestNumberInput.error = null
            }
            val interest = if (interestInput > 1||interestInput==1.0) interestInput / 100 else interestInput

            interestNumberInput.setText(interest.toString())  // Reflect the potentially transformed interest rate

            val selectedDateText = binding.maturityDateButton.text.toString()
            val defaultDateText = getString(R.string.enter_maturity_date)

            // Check if a date has been selected
            if (selectedDateText == defaultDateText) {
                Toast.makeText(context, "Please select a maturity date.", Toast.LENGTH_SHORT).show()
                return false
            }

            return true
        }
    }

    private fun insertLoanDataToDatabase(type: String) {
        with(binding) {
            val category = loanCategoryAutocomplete.text.toString().lowercase()
            val creditor = loanCreditorAutoComplete.text.toString().lowercase()
            val loanName = loanNameTextInput.text.toString()
            val amount = amountNumberInput.text.toString().toDouble()
            val interest = interestNumberInput.text.toString().toDouble()
            val maturityDate = maturityDateButton.text.toString()

            dbHelper.insertLoan(category, creditor, loanName, amount, interest, maturityDate, type)
            Toast.makeText(context, "Loan added successfully", Toast.LENGTH_SHORT).show()
            resetForm()
        }
    }

    private fun resetForm() {
        with(binding) {
            loanCategoryAutocomplete.setText("")
            loanCreditorAutoComplete.setText("")
            loanNameTextInput.setText("")
            amountNumberInput.setText("")
            interestNumberInput.setText("")
            maturityDateButton.setText(R.string.enter_maturity_date)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
