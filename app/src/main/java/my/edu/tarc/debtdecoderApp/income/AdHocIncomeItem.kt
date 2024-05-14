package my.edu.tarc.debtdecoderApp.income

import com.google.firebase.database.Exclude

// Updated data class representing an ad hoc income item

data class AdHocIncomeItem(
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val notes: String = "",
    @get:Exclude val key: String = ""  // Transient key for Firebase reference
)

