package my.edu.yyass.repaymentTab.individualTab

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import my.edu.yyass.Loan
import my.edu.yyass.R
import my.edu.yyass.databinding.FragmentEditLoanDialogBinding
import java.util.Calendar

class EditLoanDialogFragment(private val loan: Loan) : DialogFragment() {

    private var _binding: FragmentEditLoanDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentEditLoanDialogBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext(), R.style.DarkAlertDialogStyle)

        // Initialize the fields with loan details
        with(binding) {
            editTextLoanName.setText(loan.loan_name)
            editTextLoanAmount.setText(loan.amount.toString())
            editTextLoanInterest.setText(loan.interest.toString())
            editTextLoanMaturity.text = loan.maturity_date
            editTextLoanMaturity.setOnClickListener { showDatePickerDialog() }
            editTextAmountPaid.setText(loan.amount_paid.toString())
        }

        // Configure the dialog buttons
        builder.setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                // Collect data from the binding and update the loan
                val updatedLoan = loan.copy(
                    loan_name = binding.editTextLoanName.text.toString(),
                    amount = binding.editTextLoanAmount.text.toString().toDouble(),
                    interest = binding.editTextLoanInterest.text.toString().toDouble(),
                    maturity_date = binding.editTextLoanMaturity.text.toString(),
                    amount_paid = binding.editTextAmountPaid.text.toString().toDouble()
                )
                (parentFragment as IndividualLoanFragment).updateLoanDetails(updatedLoan)
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
            val selectedDate = String.format("%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            binding.editTextLoanMaturity.text = selectedDate
        }, year, month, day)

        dpd.datePicker.minDate = System.currentTimeMillis() - 1000  // This will disable past dates including today
        dpd.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks
    }
}
