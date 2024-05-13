package com.example.expenses_and_budget_mobileassignment.util

import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {
    private var instance: FirebaseDatabase? = null

    fun getInstance(databaseUrl: String?): FirebaseDatabase {
        synchronized(this) {
            if (instance == null) {
                if (databaseUrl != null) {
                    instance = FirebaseDatabase.getInstance(databaseUrl)
                } else {
                    instance = FirebaseDatabase.getInstance()
                }
                instance!!.setPersistenceEnabled(true)
            }
            return instance!!
        }
    }
}