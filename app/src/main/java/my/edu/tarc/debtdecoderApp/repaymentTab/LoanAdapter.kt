//package my.edu.tarc.debtdecoderApp.repaymentTab
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import my.edu.tarc.debtdecoder.databinding.ItemLoanBinding
//import my.edu.yyass.Loan
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.concurrent.TimeUnit
//
//class LoanAdapter : RecyclerView.Adapter<LoanAdapter.LoanViewHolder>() {
//
//    private var loans: List<Loan> = listOf()
//    var strategyType: String = ""
//
//    //Mock Value to Test Strategies
//    private val monthlyIncome = 5000.00  // Mock income
//    private val repaymentPercentage = 0.35  // 35% of income goes to debt repayment
//    private val availableForRepayment = monthlyIncome * repaymentPercentage
//    private var suggestedInstallments: MutableMap<Int, Double> = mutableMapOf()  // Map Loan ID to Suggested Installment
//
//    class LoanViewHolder(val binding: ItemLoanBinding) : RecyclerView.ViewHolder(binding.root)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
//        val binding = ItemLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return LoanViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
//        val loan = loans[position]
//        val monthlyInstallment = calculateMonthlyInstallment(loan)
//        val suggestedInstallment = suggestedInstallments[loan.id] ?: 0.0  // Default to 0 if not set
//
//        with(holder.binding) {
//            loanNameTextView.text = loan.loan_name
//            loanAmountTextView.text = "${loan.amount}"
//            loanInterestTextView.text = "${((loan.interest.toDouble() * 100).toInt())}%"
//            loanMaturityTextView.text = loan.maturity_date
//            loanMonthlyInstallmentTextView.text = "${String.format("%.2f", monthlyInstallment)}"
//
//            // Handling visibility and display of suggested installment based on strategy
//            if (strategyType == "Avalanche" || strategyType == "Snowball") {
//                suggestedInstallmentTextView.visibility = View.VISIBLE
//                suggestedInstallmentTextView.text = "${String.format("%.2f", suggestedInstallment)}"
//            } else {
//                suggestedKey.visibility = View.GONE
//                suggestedInstallmentTextView.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun calculateSuggestedInstallment(loan: Loan): Double {
//        // Placeholder for actual calculation logic
//        // This could depend on the user's income, loan amount, interest, etc.
//        return loan.amount * 0.05  // Example: 5% of the remaining principal
//    }
//
//    override fun getItemCount() = loans.size
//
//    fun submitList(newLoans: List<Loan>) {
//        loans = newLoans
//        distributePayments()
//        notifyDataSetChanged()
//    }
//
//    private fun calculateMonthlyInstallment(loan: Loan): Double {
//        val remainingPrincipal = loan.amount - loan.amount_paid  // Subtract amount already paid from the original amount
//        val monthlyInterestRate = loan.interest / 12
//        val totalPayments = monthsBetweenDates(loan.maturity_date)  // Calculate the number of months left until the loan is due
//
//        return if (totalPayments > 0 && remainingPrincipal > 0) {
//            if (loan.interest > 0) {
//                // Normal installment calculation for interest-bearing loans
//                val monthlyPayment = remainingPrincipal * (monthlyInterestRate * Math.pow(1 + monthlyInterestRate, totalPayments.toDouble())) /
//                        (Math.pow(1 + monthlyInterestRate, totalPayments.toDouble()) - 1)
//                monthlyPayment
//            } else {
//                // Simple division for zero-interest loans
//                remainingPrincipal / totalPayments
//            }
//        } else if (totalPayments == 0 && remainingPrincipal > 0) {
//            // If the loan is due this month, return the remaining principal
//            remainingPrincipal
//        } else {
//            // If there are no payments left or nothing is owed
//            0.0
//        }
//    }
//
//
//    private fun monthsBetweenDates(maturityDate: String): Int {
//        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val endDate = formatter.parse(maturityDate)
//        val startDate = Date()
//        val duration = endDate.time - startDate.time
//
//        return TimeUnit.MILLISECONDS.toDays(duration).toInt() / 30
//    }
//
//    private fun distributePayments() {
//        var remainingBudget = availableForRepayment
//        suggestedInstallments.clear()  // Clear previous calculations
//
//        val sortedLoans = when (strategyType) {
//            "Avalanche" -> loans.sortedByDescending { it.interest }
//            "Snowball" -> loans.sortedBy { it.amount }
//            else -> loans
//        }
//
//        for (loan in sortedLoans) {
//            val minimumPayment = calculateMonthlyInstallment(loan)
//            if (remainingBudget >= minimumPayment) {
//                suggestedInstallments[loan.id] = minimumPayment
//                remainingBudget -= minimumPayment
//            } else {
//                suggestedInstallments[loan.id] = remainingBudget
//                break
//            }
//        }
//    }
//}

package my.edu.tarc.debtdecoderApp.repaymentTab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoder.databinding.ItemLoanBinding
import my.edu.yyass.Loan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

class LoanAdapter(private val repaymentPercentage: Double = 0.35) : RecyclerView.Adapter<LoanAdapter.LoanViewHolder>() {

    private var loans: List<Loan> = listOf()
    var strategyType: String = ""
    private var monthlyIncome: Double = 5000.00  // Default income
    private var suggestedInstallments: MutableMap<Int, Double> = mutableMapOf()

    class LoanViewHolder(val binding: ItemLoanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val binding = ItemLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        val loan = loans[position]
        val monthlyInstallment = calculateMonthlyInstallment(loan)
        val suggestedInstallment = suggestedInstallments[loan.id] ?: 0.0

        with(holder.binding) {
            loanNameTextView.text = loan.loan_name
            loanAmountTextView.text = "${loan.amount}"
            loanInterestTextView.text = "${((loan.interest * 100).toInt())}%"
            loanMaturityTextView.text = loan.maturity_date
            loanMonthlyInstallmentTextView.text = "${String.format("%.2f", monthlyInstallment)}"

            if (strategyType == "Avalanche" || strategyType == "Snowball") {
                suggestedInstallmentTextView.visibility = View.VISIBLE
                suggestedInstallmentTextView.text = "${String.format("%.2f", suggestedInstallment)}"
            } else {
                suggestedKey.visibility = View.GONE
                suggestedInstallmentTextView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = loans.size

    fun updateIncome(newIncome: Double) {
        monthlyIncome = newIncome
        distributePayments()  // Recalculate payments whenever income updates
        notifyDataSetChanged()
    }

    fun submitList(newLoans: List<Loan>) {
        loans = newLoans
        distributePayments()
        notifyDataSetChanged()
    }

    private fun calculateMonthlyInstallment(loan: Loan): Double {
        val remainingPrincipal = loan.amount - loan.amount_paid
        val monthlyInterestRate = loan.interest / 12
        val totalPayments = monthsBetweenDates(loan.maturity_date)

        return if (totalPayments > 0 && remainingPrincipal > 0) {
            if (loan.interest > 0) {
                val monthlyPayment = remainingPrincipal * (monthlyInterestRate * Math.pow(1 + monthlyInterestRate, totalPayments.toDouble())) /
                        (Math.pow(1 + monthlyInterestRate, totalPayments.toDouble()) - 1)
                monthlyPayment
            } else {
                remainingPrincipal / totalPayments
            }
        } else if (totalPayments == 0 && remainingPrincipal > 0) {
            remainingPrincipal
        } else {
            0.0
        }
    }

    private fun monthsBetweenDates(maturityDate: String): Int {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = formatter.parse(maturityDate)
        val startDate = Date()
        val duration = endDate.time - startDate.time
        return TimeUnit.MILLISECONDS.toDays(duration).toInt() / 30
    }

    private fun distributePayments() {
        var remainingBudget = monthlyIncome * repaymentPercentage
        suggestedInstallments.clear()

        val sortedLoans = when (strategyType) {
            "Avalanche" -> loans.sortedByDescending { it.interest }
            "Snowball" -> loans.sortedBy { it.amount - it.amount_paid } // consider unpaid balance
            else -> loans
        }

        // First, cover all minimum payments
        for (loan in sortedLoans) {
            val minimumPayment = calculateMonthlyInstallment(loan)
            val payment = min(remainingBudget, minimumPayment)
            suggestedInstallments[loan.id] = payment
            remainingBudget -= payment
        }

        // Then, use any remaining budget to pay down loans starting from highest priority
        if (remainingBudget > 0) {
            for (loan in sortedLoans) {
                val alreadyPaid = suggestedInstallments[loan.id] ?: 0.0
                val unpaidBalance = loan.amount - loan.amount_paid - alreadyPaid
                if (unpaidBalance > 0 && remainingBudget > 0) {
                    val extraPayment = min(remainingBudget, unpaidBalance)
                    suggestedInstallments[loan.id] = alreadyPaid + extraPayment
                    remainingBudget -= extraPayment
                }

                if (remainingBudget <= 0) break
            }
        }
    }
}

