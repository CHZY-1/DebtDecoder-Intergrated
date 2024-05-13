package my.edu.tarc.debtdecoderApp.util

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.expenses_and_budget_mobileassignment.util.FirebaseStorageManager
import my.edu.tarc.debtdecoder.R

// https://firebase.google.com/docs/storage/android/download-files#downloading_images_with_firebaseui
// https://egemenhamutcu.medium.com/displaying-images-from-firebase-storage-using-glide-for-kotlin-projects-3e4950f6c103
class GlideImageLoader {

    private val storage = FirebaseStorageManager.getDefaultStorage()
    fun loadCategoryImage(imageUrl: String, imageView: ImageView, context: Context) {
        if (imageUrl.isNotEmpty()) {
            val gsReference = storage.getReferenceFromUrl(imageUrl)
            gsReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(context)
                    .load(uri.toString())
                    .placeholder(R.drawable.icon_close_button_bright)
                    .error(R.drawable.icon_close_button_bright)
                    .centerCrop()
                    .into(imageView)
            }.addOnFailureListener { exception ->
                imageView.setImageResource(R.drawable.icon_close_button_bright)
                Log.e("ImageLoader", "Error downloading image", exception)
            }
        } else {
            imageView.setImageResource(R.drawable.icon_close_button_bright)
        }
    }

    fun prefetchImage(imageUrl: String, context: Context) {
        if (imageUrl.isNotEmpty()) {
            val gsReference = storage.getReferenceFromUrl(imageUrl)
            gsReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(context)
                    .load(uri.toString())
                    .preload()
            }
        }
    }

}