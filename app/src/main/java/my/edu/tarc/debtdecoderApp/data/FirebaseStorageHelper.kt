package com.example.expenses_and_budget_mobileassignment.data

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseStorageHelper(private val storage: FirebaseStorage) {

    // Function to check if an item exists in Firebase Cloud Storage
    suspend fun checkItemExists(storagePath: String): Boolean {
        return try {
            storage.getReference(storagePath).metadata.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Function to compare and add missing URLs to the real-time database
    suspend fun addMissingUrls(cloudStorageUrls: List<String>, realTimeDbUrls: List<String>, callback: (String) -> Unit) {
        cloudStorageUrls.forEach { cloudUrl ->
            if (!realTimeDbUrls.contains(cloudUrl)) {
                callback(cloudUrl)
                // Add logic to update the real-time database with the missing URL
            }
        }
    }
}
