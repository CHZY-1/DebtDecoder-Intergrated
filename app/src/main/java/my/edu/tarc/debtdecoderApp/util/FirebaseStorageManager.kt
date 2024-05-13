package com.example.expenses_and_budget_mobileassignment.util
import com.example.expenses_and_budget_mobileassignment.data.FirebaseStorageHelper
import com.google.firebase.storage.FirebaseStorage
//object FirebaseStorageManager {
//    private val instance: FirebaseStorage by lazy {
//        FirebaseStorage.getInstance()
//    }
//
//    fun getStorage(): FirebaseStorage {
//        return instance
//    }
//}
//


object FirebaseStorageManager {
    private var instance: FirebaseStorageHelper? = null

    fun getInstance(storage: FirebaseStorage): FirebaseStorageHelper {
        return instance ?: synchronized(this) {
            instance ?: FirebaseStorageHelper(storage).also { instance = it }
        }
    }

    fun getInstanceStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    fun getStorage(bucketName: String): FirebaseStorage {
        return FirebaseStorage.getInstance("gs://$bucketName")
    }

    fun getDefaultStorage(): FirebaseStorage {
        // Return Firebase Storage with a specific bucket
        val bucketName = "expensesandbudget-55687.appspot.com"
        val storage = FirebaseStorage.getInstance("gs://$bucketName")
        return storage
    }
}