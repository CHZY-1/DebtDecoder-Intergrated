package my.edu.tarc.debtdecoderApp.data
import com.google.firebase.database.PropertyName

// contain all the data model for the expense module

// Base interface for both headers and items
interface ListItem

// Header class representing a date
data class SectionHeader(val date: String,
                         val totalAmount: Float) : ListItem

// Data Model for Expenses
//data class Expense(val category: String, val paymentMethod: String, val amount: Float)
data class ExpenseItem(val category: String,
                       val paymentMethod: String,
                       val amount: Float) : ListItem

data class User(
    val username: String = "",
    val email: String = "",
    val expenses: Map<String, Expense> = emptyMap()
)

data class Expense(
    var expenseId: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val payment: String = "",
    val date: String = "",
    val recurrence: String = "",
    val remark: String = "",
    @get:PropertyName("selected") @set:PropertyName("selected") var isSelected: Boolean = false
)

data class ExpenseCategory(
    var categoryName: String = "",
    val display: Boolean = true,
    @get:PropertyName("image_url") @set:PropertyName("image_url") var imageUrl: String = "",
    var cachedUrl: String? = null
)

data class ExpenseTopCategoryItem(
    var category: String = "",
    var percentage: Double = 0.0,
    var amount: Double = 0.0,
    @get:PropertyName("image_url") @set:PropertyName("image_url") var imageUrl: String = ""
)
