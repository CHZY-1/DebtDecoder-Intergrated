package my.edu.yyass.statusTab

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.StatusLoanItemBinding
import my.edu.yyass.Loan
import java.util.Locale

class StatusLoanAdapter(private var loans: List<Loan>) : RecyclerView.Adapter<StatusLoanAdapter.LoanViewHolder>() {

    class LoanViewHolder(val binding: StatusLoanItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val binding = StatusLoanItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        val loan = loans[position]
        with(holder.binding) {
            tvLoanName.text = loan.loan_name
            // Calculate remaining amount
            val remainingAmount = loan.amount - loan.amount_paid
            tvLoanAmount.text = "RM ${String.format("%.2f", remainingAmount)}"

            // Set icon based on the loan category
            val iconResId = when (loan.loan_category.lowercase(Locale.ROOT)) {
                "personal loan" -> R.drawable.personal_loan_logo
                "car loan" -> R.drawable.car_loan_logo
                "student loan" -> R.drawable.student_loan_logo
                "house loan" -> R.drawable.house_loan_logo
                else -> R.drawable.other_loan_logo  // Covers "others" and any unforeseen categories
            }
            loanIcon.setImageResource(iconResId)
        }
    }

    override fun getItemCount() = loans.size

    fun updateLoans(newLoans: List<Loan>) {
        loans = newLoans
        notifyDataSetChanged()
    }
}
