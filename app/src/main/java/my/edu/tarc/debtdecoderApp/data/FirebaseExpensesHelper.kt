package my.edu.tarc.debtdecoderApp.data

import android.util.Log
import my.edu.tarc.debtdecoderApp.util.DateFormatter
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FirebaseExpensesHelper(private val database: FirebaseDatabase) {

    // Function to add an expense for a specific user
    fun addExpense(userId: String, expense: Expense, onComplete: (Boolean, String?) -> Unit) {
        // Using push that create a unique ID for the new expense
        val ref = database.reference.child("users").child(userId).child("expenses").push()
        // setting expenseId back to expense object
        expense.expenseId = ref.key ?: return onComplete(false, null)
        // Set the value of the expense in the database and handle the success or failure cases
        ref.setValue(expense)
            .addOnSuccessListener { onComplete(true, ref.key) }  // Return the new unique key if success
            .addOnFailureListener { onComplete(false, null) }
    }

    // Function to retrieve all expenses for a specific user
    fun getExpenses(userId: String, onExpensesReceived: (List<Expense>, Boolean) -> Unit) {
        // Attach a listener to the user's expenses node to read data
        database.reference.child("users").child(userId).child("expenses").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // No data was found at the path
                        onExpensesReceived(emptyList(), false)
                        return
                    }

                    val expenses = mutableListOf<Expense>()
                    // Iterate through each expense entry
                    snapshot.children.forEach {
                        val expense = it.getValue(Expense::class.java)
                        expense?.let { expenses.add(it) }
                    }

                    // Pass the list of expenses to the callback
                    onExpensesReceived(expenses, true)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Operation cancelled due to a database error: ${error.message}", error.toException())
                }
            })
    }

    fun getExpensesForLastDays(userId: String, numberOfDays: Int, onExpensesReceived: (List<Expense>) -> Unit) {

        // Calculate the date range based on today - number of days
        val (startDate, endDate) = DateFormatter.getDateRange(numberOfDays)

        // https://firebase.google.com/docs/database/rest/retrieve-data
        database.reference.child("users").child(userId).child("expenses")
            .orderByChild("date").startAt(startDate).endAt(endDate + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenses = mutableListOf<Expense>()
                    snapshot.children.forEach {
                        val expense = it.getValue(Expense::class.java)
                        expense?.let { expenses.add(it) }
                    }
                    onExpensesReceived(expenses)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to load expenses: ${error.message}", error.toException())
                }
            })
    }

    // Function to retrieve category details from the database
    fun getCategoryDetails(onCategoriesReceived: (Map<String, ExpenseCategory>) -> Unit) {
        database.reference.child("expense_categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = snapshot.children.mapNotNull { childSnapshot ->
                    // Extract the key which is the category name such as "Food", "Transport", etc.
                    val key = childSnapshot.key
                    // Attempt to get an ExpenseCategory object from the snapshot; apply to set categoryName if not null
                    val category = childSnapshot.getValue(ExpenseCategory::class.java)?.apply {
                        // Set the categoryName to the key (assuming it is not null)
                        categoryName = key ?: ""
                    }
                    // Create a pair only if both key and category are non-null
                    if (key != null && category != null) key to category else null
                }.toMap()  // Converts the list of non-null pairs to a Map

                // Pass the map of categories (ensuring all are non-null) to the callback function
                onCategoriesReceived(categories)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible cancellation and log the error
                Log.e("FirebaseError", "Operation cancelled due to a database error: ${error.message}", error.toException())
            }
        })
    }

    // Return a list of expense objects for a specific user and date
    fun getExpensesByDate(userId: String, date: String, onExpensesReceived: (List<Expense>) -> Unit) {
        database.reference.child("users").child(userId).child("expenses")
            .orderByChild("date").equalTo(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenses = mutableListOf<Expense>()
                    snapshot.children.forEach {
                        val expense = it.getValue(Expense::class.java)
                        expense?.let { expenses.add(it) }
                    }
                    onExpensesReceived(expenses)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to load expenses: ${error.message}", error.toException())
                }
            })
    }

    fun getExpensesByMonth(userId: String, year: Int, month: Int, onExpensesReceived: (List<Expense>) -> Unit) {
        val (startDate, endDate) = DateFormatter.getStartAndEndDatesForMonth(year, month)

        database.reference.child("users").child(userId).child("expenses")
            .orderByChild("date").startAt(startDate).endAt(endDate + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenses = mutableListOf<Expense>()
                    snapshot.children.forEach {
                        val expense = it.getValue(Expense::class.java)
                        expense?.let { expenses.add(it) }
                    }
                    onExpensesReceived(expenses)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to load expenses: ${error.message}", error.toException())
                }
            })
    }

    fun deleteSingleExpense(userId: String, expenseId: String, onComplete: (Boolean) -> Unit) {
        val expenseRef = database.reference.child("users").child(userId).child("expenses").child(expenseId)
        expenseRef.removeValue()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteExpenses(userId: String, expenses: List<Expense>, onComplete: (Boolean) -> Unit) {
        val expensesRef = database.reference.child("users").child(userId).child("expenses")
        val deleteOperations = expenses.map { expense ->
            expensesRef.child(expense.expenseId).removeValue()
        }

        Tasks.whenAll(deleteOperations)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}