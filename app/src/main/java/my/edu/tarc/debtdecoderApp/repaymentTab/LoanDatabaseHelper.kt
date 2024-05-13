package my.edu.tarc.debtdecoderApp.repaymentTab
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import my.edu.yyass.Loan

class LoanDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "loans.db"
        private const val DATABASE_VERSION = 3 // Incremented version
    }

    private val SQL_CREATE_ENTRIES =
        """
        CREATE TABLE Loans (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            loan_category TEXT,
            creditor TEXT,
            loan_name TEXT,
            amount REAL,
            interest REAL,
            maturity_date TEXT,
            type TEXT,
            amount_paid REAL DEFAULT 0
        )
        """.trimIndent()

    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS Loans"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    fun insertLoan(loan_category: String, creditor: String, loan_name: String, amount: Double, interest: Double, maturity_date: String, type: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("loan_category", loan_category)
            put("creditor", creditor)
            put("loan_name", loan_name)
            put("amount", amount)
            put("interest", interest)
            put("maturity_date", maturity_date)
            put("type", type)  // Add type here
        }
        db.insert("Loans", null, values)
    }

    /////////////////////////////////////////// For Individual Loan Fragment (Repayment Tab) ///////////////////////////////

    //Method to update the repayment amount
    fun updatePayment(loanId: Int, paymentAmount: Double) {
        val db = writableDatabase

        // First, fetch the current amount paid
        val currentAmountPaidCursor = db.rawQuery("SELECT amount_paid FROM Loans WHERE id = ?", arrayOf(loanId.toString()))
        if (currentAmountPaidCursor.moveToFirst()) {
            val currentAmountPaid = currentAmountPaidCursor.getDouble(0)
            currentAmountPaidCursor.close()

            // Calculate the new total amount paid
            val newAmountPaid = currentAmountPaid + paymentAmount

            // Update the loan with the new amount paid
            val contentValues = ContentValues()
            contentValues.put("amount_paid", newAmountPaid)
            db.update("Loans", contentValues, "id = ?", arrayOf(loanId.toString()))
        } else {
            currentAmountPaidCursor.close()
        }
    }


    // Get All Loans
    fun getAllLoans(): List<Loan> {
        val loans = mutableListOf<Loan>()
        val db = readableDatabase
        val cursor = db.query("Loans", null, null, null, null, null, null)

        with(cursor) {
            while (moveToNext()) {
                val loan = Loan(
                    id = getInt(getColumnIndexOrThrow("id")),
                    loan_category = getString(getColumnIndexOrThrow("loan_category")),
                    creditor = getString(getColumnIndexOrThrow("creditor")),
                    loan_name = getString(getColumnIndexOrThrow("loan_name")),
                    amount = getDouble(getColumnIndexOrThrow("amount")),
                    interest = getDouble(getColumnIndexOrThrow("interest")),
                    maturity_date = getString(getColumnIndexOrThrow("maturity_date")),
                    type = getString(getColumnIndexOrThrow("type")),
                    amount_paid = getDouble(getColumnIndexOrThrow("amount_paid")) // Fetch amount paid
                )
                loans.add(loan)
            }
            close()
        }
        return loans
    }

    // Edit/Update Function
    fun updateLoan(loan: Loan) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("loan_category", loan.loan_category)
            put("creditor", loan.creditor)
            put("loan_name", loan.loan_name)
            put("amount", loan.amount)
            put("interest", loan.interest)
            put("maturity_date", loan.maturity_date)
            put("type", loan.type)
            put("amount_paid", loan.amount_paid)
        }
        val selection = "id = ?"
        val selectionArgs = arrayOf(loan.id.toString())
        db.update("Loans", values, selection, selectionArgs)
    }

    // Delete Function
    fun deleteLoan(id: Int) {
        val db = writableDatabase
        val selection = "id = ?"
        val selectionArgs = arrayOf(id.toString())
        db.delete("Loans", selection, selectionArgs)
    }

    //To fetch unique categories (Add Loans page)
    fun getUniqueCategories(): List<String> {
        val categories = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT loan_category FROM Loans ORDER BY loan_category", null)
        try {
            if (cursor.moveToFirst()) {
                do {
                    categories.add(cursor.getString(cursor.getColumnIndex("loan_category")))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        return categories
    }

    //To fetch unique creditors
    fun getUniqueCreditors(): List<String> {
        val creditors = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT creditor FROM Loans ORDER BY creditor", null)
        try {
            while (cursor.moveToNext()) {
                creditors.add(cursor.getString(0))
            }
        } finally {
            cursor.close()
        }
        return creditors
    }

    ////////////////////////////////////////////
    // Method to get the nearest due date for the "Custom" strategy (assuming it might be a simple filter)
// Method to get the nearest due date for borrowed loans
    fun getNearestDueBorrowed(): String {
        val db = readableDatabase
        val query = "SELECT MIN(maturity_date) as nearest_due FROM Loans WHERE type = 'borrow'"
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex("nearest_due")) ?: "No due date"
            }
        }
        return "No due date"
    }

    // Method to get the highest interest rate for loans (Avalanche method)
    fun getHighestInterestBorrowed(): String {
        val db = readableDatabase
        val query = "SELECT MAX(interest) as highest_interest FROM Loans WHERE type = 'borrow'"
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val highestInterest = cursor.getDouble(cursor.getColumnIndex("highest_interest"))
                return "$highestInterest"
            }
        }
        return "0%"
    }

    // Method to get the smallest balance for loans (Snowball method)
    fun getSmallestBalanceBorrowed(): String {
        val db = readableDatabase
        val query = """
            SELECT MIN(amount - amount_paid) as smallest_balance
            FROM Loans 
            WHERE type = 'borrow' AND amount > amount_paid
        """
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val smallestBalance = cursor.getDouble(cursor.getColumnIndex("smallest_balance"))
//                Log.e("Debug Repayment Strategy","Smallest Balance in DB:\n $smallestBalance")
                return "RM ${String.format("%.2f", smallestBalance)}"
            }
        }
        return "RM 0.00"
    }

    // Method to get the nearest due date for lent loans (Owed to You method)
    fun getNearestDueLent(): String {
        val db = readableDatabase
        val query = "SELECT MIN(maturity_date) as nearest_due FROM Loans WHERE type = 'lend'"
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex("nearest_due")) ?: "No due date"
            }
        }
        return "No due date"
    }

}
