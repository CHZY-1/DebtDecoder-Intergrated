package my.edu.yyass.repaymentTab.individualTab

import my.edu.yyass.repaymentTab.LoanDatabaseHelper
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentIndividualLoanBinding
import my.edu.tarc.debtdecoderApp.repaymentTab.individualTab.EditLoanDialogFragment
import my.edu.yyass.Loan

class IndividualLoanFragment : Fragment() {

    private var _binding: FragmentIndividualLoanBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: LoanDatabaseHelper
    private var loans = listOf<Loan>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIndividualLoanBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupRadioGroupListener() {
        binding.loanTypeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioBorrow -> loadLoans("borrow")
                R.id.radioLend -> loadLoans("lend")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = LoanDatabaseHelper(requireContext())
        setupRadioGroupListener()
        loadLoans("borrow")  // Default to loading "borrow" loans initially
        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        loadLoans("borrow")
        binding.loanTypeRadioGroup.check(R.id.radioBorrow)
    }

    private fun loadLoans(type: String) {
        loans = dbHelper.getAllLoans().filter { it.type == type }
        if (loans.isNotEmpty()) {
            setupSpinner(loans)
        } else {
            // Handle case where no loans are available for the selected type
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf<String>())
            binding.loanSpinner.adapter = adapter
        }
    }

    private fun setupSpinner(loans: List<Loan>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, loans.map { it.loan_name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.loanSpinner.adapter = adapter
        binding.loanSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                displayLoanDetails(loans[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun displayLoanDetails(loan: Loan) {
        val progress = calculateProgress(loan)
        binding.loanProgress.progress = progress
        binding.progressText.text = "$progress%"
        binding.remainingText.text = "${100 - progress}%"
        // Setting loan name if necessary, and updating all detail TextViews
        binding.loanTitle.text = loan.loan_name

        // Updating loan detail fields
        binding.categoryValue.text = loan.loan_category
        binding.creditorValue.text = loan.creditor
        binding.amountValue.text = "RM${loan.amount}"
        binding.interestRateValue.text = "${(loan.interest.toDouble()*100).toInt()}%"
        binding.maturityValue.text = loan.maturity_date

        // Display total repayment and remaining amount
        binding.totalRepaymentAmount.text = String.format("RM%.2f", loan.amount_paid)
        val remainingAmount = loan.amount - loan.amount_paid
        binding.remainingAmount.text = String.format("RM%.2f", remainingAmount)

    }

    private fun calculateProgress(loan: Loan): Int {
        // Placeholder function to calculate progress
        return (loan.amount_paid / loan.amount * 100).toInt()
    }

    private fun setupButtonListeners() {
        binding.editLoanButton.setOnClickListener {
            Log.d("IndividualLoanFragment", "Edit button clicked")
            val selectedLoan = loans.getOrNull(binding.loanSpinner.selectedItemPosition)
            selectedLoan?.let {
                showEditLoanDialog(it)
            }
        }

        binding.payLoanButton.setOnClickListener {
            Log.d("IndividualLoanFragment", "Pay button clicked")
            val selectedLoan = loans.getOrNull(binding.loanSpinner.selectedItemPosition)
            selectedLoan?.let {
                promptForPayment(it)
            }
        }

        binding.deleteLoanButton.setOnClickListener {  // Assuming this is the new button you want to function as delete
            Log.d("IndividualLoanFragment", "Delete button clicked")
            val selectedLoan = loans.getOrNull(binding.loanSpinner.selectedItemPosition)
            selectedLoan?.let {
                confirmAndDeleteLoan(it)
            }
        }
    }

    private fun promptForPayment(loan: Loan) {
        val remainingAmount = loan.amount - loan.amount_paid

        if (remainingAmount > 0) {
            val input = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                hint = "Enter payment amount"
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setHintTextColor(ContextCompat.getColor(context, R.color.white))
            }

            AlertDialog.Builder(context, R.style.DarkAlertDialogStyle)
                .setTitle("How much have you paid?")
                .setView(input)
                .setPositiveButton("Submit") { dialog, which ->
                    val paymentAmount = input.text.toString().toDoubleOrNull()
                    if (paymentAmount != null && paymentAmount <= remainingAmount) {
                        updatePayment(loan.id, paymentAmount)
                    } else {
                        Toast.makeText(context, "Invalid or excessive amount", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(context, "Loan has been fully repaid", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePayment(loanId: Int, paymentAmount: Double) {
        dbHelper.updatePayment(loanId, paymentAmount)
        Toast.makeText(context, "Payment updated successfully", Toast.LENGTH_SHORT).show()
        loadLoans("borrow")  // Reload loans to reflect the payment
    }

    fun showEditLoanDialog(selectedLoan: Loan) {
        val dialog = EditLoanDialogFragment(selectedLoan)
        dialog.show(childFragmentManager, "editLoan")
    }

    fun updateLoanDetails(updatedLoan: Loan) {
        dbHelper.updateLoan(updatedLoan)
        Toast.makeText(context, "Loan updated successfully", Toast.LENGTH_SHORT).show()
        loadLoans("borrow")  // Refresh the spinner and details view
    }

    fun confirmAndDeleteLoan(loan: Loan) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this loan?")
            .setPositiveButton("Delete") { dialog, which ->
                dbHelper.deleteLoan(loan.id)
                Toast.makeText(context, "Loan deleted successfully", Toast.LENGTH_SHORT).show()
                loadLoans("borrow")  // Reload loans after deletion
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
