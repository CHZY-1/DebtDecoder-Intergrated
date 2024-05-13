package com.example.expenses_and_budget_mobileassignment.util

import com.example.expenses_and_budget_mobileassignment.data.FirebaseExpensesHelper
import com.google.firebase.database.FirebaseDatabase

object FirebaseExpensesHelperManager {
    private var instance: FirebaseExpensesHelper? = null
    private lateinit var defaultDatabase: FirebaseDatabase

    fun initialize(database: FirebaseDatabase) {
        defaultDatabase = database
    }

    fun isInitialized(): Boolean {
        return this::defaultDatabase.isInitialized
    }

    fun getFirebaseDatabase(): FirebaseDatabase {
        if (!isInitialized()) {
            throw IllegalStateException("FirebaseExpensesHelperManager must be initialized before use")
        }
        return defaultDatabase
    }

    fun getInstance(): FirebaseExpensesHelper {
        if (!isInitialized()) {
            throw IllegalStateException("FirebaseExpensesHelperManager must be initialized before use")
        }
        return instance ?: synchronized(this) {
            instance ?: FirebaseExpensesHelper(defaultDatabase).also { instance = it }
        }
    }
}

fun getFirebaseHelperInstance(databaseUrl: String = "https://expensesandbudget-55687-default-rtdb.asia-southeast1.firebasedatabase.app"): FirebaseExpensesHelper {
    val firebaseManager = FirebaseExpensesHelperManager
    if (!firebaseManager.isInitialized()) {
        firebaseManager.initialize(FirebaseDatabase.getInstance(databaseUrl))
    }
    return firebaseManager.getInstance()
}

fun getFirebaseInstance(databaseUrl: String = "https://expensesandbudget-55687-default-rtdb.asia-southeast1.firebasedatabase.app"): FirebaseDatabase{
    val firebaseManager = FirebaseExpensesHelperManager
    if (!firebaseManager.isInitialized()) {
        firebaseManager.initialize(FirebaseDatabase.getInstance(databaseUrl))
    }
    return firebaseManager.getFirebaseDatabase()
}