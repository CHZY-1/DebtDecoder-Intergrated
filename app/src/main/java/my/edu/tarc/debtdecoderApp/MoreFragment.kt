package my.edu.tarc.debtdecoderApp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import my.edu.tarc.debtdecoder.R

class MoreFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var profileImageView: ImageView? = null
    private var emailTextView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    //Test
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize the NavController for navigation
        val navController = findNavController()

        // Find the TextView for email
        emailTextView = view.findViewById(R.id.tvUserEmail)

        // Find the ImageView for the profile picture (remove the click listener)
        profileImageView = view.findViewById(R.id.profilePicture)

        // Set the user's email and profile picture
        setUserDetails()

        // Set up the sign-out action
        val signOutLayout: View = view.findViewById(R.id.layoutSignOut)
        signOutLayout.setOnClickListener {
            signOutUser()
        }

        // Set up navigation actions for other layout items
        val myAccountLayout: View = view.findViewById(R.id.layoutMyAccount)
        myAccountLayout.setOnClickListener {
            navController.navigate(R.id.action_more_to_myAcc)
        }

        val incomeLayout: View = view.findViewById(R.id.layoutIncome)
        incomeLayout.setOnClickListener {
            navController.navigate(R.id.action_more_to_income)
        }

        val notificationLayout: View = view.findViewById(R.id.layoutNotificationSettings)
        notificationLayout.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_notification_settings, null)
            val inAppCheckBox = dialogView.findViewById<CheckBox>(R.id.cbInApp)
            val noneCheckBox = dialogView.findViewById<CheckBox>(R.id.cbNone)

            // Fetch user preferences from Firebase and update checkboxes
            if (userId != null) {
                val userPrefRef = FirebaseDatabase.getInstance().getReference("users/$userId/preferences")
                userPrefRef.child("customNotify").get().addOnSuccessListener { dataSnapshot ->
                    when (dataSnapshot.value) {
                        "App" -> {
                            inAppCheckBox.isChecked = true
                        }
                        "No" -> {
                            noneCheckBox.isChecked = true
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to fetch settings", Toast.LENGTH_SHORT).show()
                }
            }

            // Logic for 'None' checkbox
            noneCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    inAppCheckBox.isChecked = false
                }
            }

            // Logic to prevent 'None' from being selected with other options
            val checkListener = CompoundButton.OnCheckedChangeListener { _, _ ->
                if (inAppCheckBox.isChecked) {
                    noneCheckBox.isChecked = false
                }
            }

            inAppCheckBox.setOnCheckedChangeListener(checkListener)

            // Display the dialog
            AlertDialog.Builder(context).setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    // Construct the notification preference based on checkbox states
                    val notifyPreference = when {
                        inAppCheckBox.isChecked -> "App"
                        else -> "No"
                    }

                    // Update the preference in Firebase
                    if (userId != null) {
                        savePriorityToFirebase(userId, notifyPreference)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
                .show()
        }

    }

    private fun savePriorityToFirebase(userId: String, notify: String) {
        val userPreferences = mapOf("customNotify" to notify)

        FirebaseDatabase.getInstance().getReference("users").child(userId).child("preferences")
            .updateChildren(userPreferences)
            .addOnSuccessListener {
                Toast.makeText(context, "Notification Option saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update settings", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Find the toolbar by its ID and set its visibility to GONE
        val toolbar = activity?.findViewById<View>(R.id.toolbar)
        toolbar?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // Restore the toolbar visibility by setting it to VISIBLE
        val toolbar = activity?.findViewById<View>(R.id.toolbar)
        toolbar?.visibility = View.VISIBLE
    }

    private fun signOutUser() {
        // Sign out using Firebase Authentication
        auth.signOut()

        // Navigate to the authentication screen (e.g., LoginFragment)
        val navController = findNavController()
        navController.navigate(R.id.loginFragment)

        Toast.makeText(requireContext(), "Successfully signed out!", Toast.LENGTH_SHORT).show()
    }

    private fun setUserDetails() {
        val currentUser = auth.currentUser

        currentUser?.let {
            // Retrieve the user's email directly from Firebase Auth
            val email = currentUser.email
            emailTextView?.text = email

            // Construct the reference to the profile picture based on user ID
            val storageRef = storage.reference
            val profileRef = storageRef.child("profile_pictures/${currentUser.uid}.jpg")

            // Retrieve the actual download URL from Firebase Storage
            profileRef.downloadUrl.addOnSuccessListener { uri ->
                Log.d("MoreFragment", "Retrieved profile picture URL: $uri")
                // Load the profile picture using Glide
                if(isAdded){
                profileImageView?.let { imageView ->
                    Glide.with(this)
                        .load(uri)
                        .error(R.drawable.ic_account)  // Fallback image
                        .into(imageView)
                }
                    }
            }.addOnFailureListener { exception ->
                Log.e("MoreFragment", "Error retrieving profile picture from Firebase Storage", exception)
                // Set the default image if fetching fails
                profileImageView?.setImageResource(R.drawable.ic_account)
            }
        }
    }

}
