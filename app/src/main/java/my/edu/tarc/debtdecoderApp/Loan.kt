package my.edu.yyass

data class Loan(
    val id: Int,
    val loan_category: String,
    val creditor: String,
    val loan_name: String,
    val amount: Double,
    val interest: Double,
    val maturity_date: String,
    val type: String,
    val amount_paid: Double = 0.0
)