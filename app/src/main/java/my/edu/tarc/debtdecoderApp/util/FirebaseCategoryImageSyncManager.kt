package my.edu.tarc.debtdecoderApp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseSynchronizationManager(private val firebaseStorage: FirebaseStorage, private val firebaseDatabase: FirebaseDatabase, private val context: Context) {
    suspend fun listRelevantImages(): List<Pair<String, String>> {  // Pair of URL and Category Name
        val storageRef = firebaseStorage.reference.child("Expense Categories")
        val imagesList = mutableListOf<Pair<String, String>>()
        val listResult = storageRef.listAll().await()

        listResult.items.forEach { itemRef ->
            if (itemRef.name.endsWith(".png") || itemRef.name.endsWith(".jpg")) {
                val url = itemRef.downloadUrl.await().toString()
                val categoryName = itemRef.name.substringBeforeLast('.')
                imagesList.add(url to categoryName)
            }
        }
        return imagesList
    }

    suspend fun synchronizeCategoryImages() {
        try {
            val imageCategoryPairs = listRelevantImages()
            val dbRef = firebaseDatabase.getReference("expense_categories")
            val dataSnapshot = dbRef.get().await()

            if (!dataSnapshot.exists()) {
                Log.d("SyncInfo", "No existing categories found. Creating new categories.")
            }

//            Log.d("SyncInfo", "Categories found in storage $imageCategoryPairs")

            // Iterate over each image URL and category name pair
            for ((imageUrl, categoryName) in imageCategoryPairs) {
                val imageRef = dataSnapshot.child(categoryName).child("image_url")
                val currentImageUrl = imageRef.getValue(String::class.java)

                Log.d("SyncInfo", "Image Ref $currentImageUrl")
                Log.d("SyncInfo", "Image Url $imageUrl")

                // Check if the image_url exists and if the URL is different from the current URL
                if (imageRef.exists() && currentImageUrl?.trim() != imageUrl.trim()) {
                    // Update only if the existing URL is different
                    imageRef.ref.setValue(imageUrl).await()
                    Log.d("SyncInfo", "Updated image URL for $categoryName")

                } else{
                    // Create the category if it doesn't exist with default values
                    // or insert again if the URL is same (item already exists)
                    dbRef.child(categoryName).setValue(
                        mapOf("display" to true, "image_url" to imageUrl)
                    ).await()
                    Log.d("SyncInfo", "Created new category $categoryName with image URL")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncError", "Failed to synchronize category images: ${e.localizedMessage}", e)
        }
    }
    suspend fun performFullSynchronization() {
        try {
            synchronizeCategoryImages()
            Toast.makeText(context, "Categories Synchronization complete", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("SyncTask", "Error during synchronization", e)
            Toast.makeText(context, "Synchronization failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}