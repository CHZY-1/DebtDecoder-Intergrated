package my.edu.tarc.debtdecoderApp.More

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import my.edu.tarc.debtdecoder.R

class MyAccountFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var profileImageView: ImageView? = null
    private var nicknameEditText: EditText? = null
    private var ageEditText: EditText? = null
    private var oldPasswordEditText: EditText? = null
    private var newPasswordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var submitButton: Button? = null

    // Callback to receive the selected image from the intent
    private val getImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let { uri ->
                // Set the new profile image temporarily
                profileImageView?.setImageURI(uri)
                // Upload the image to Firebase Storage
                uploadProfilePicture(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        initializeUI(view)
        loadUserData()

        // Set listeners
        submitButton?.setOnClickListener {
            updateNicknameAndAge()
        }

//        val navController = findNavController()
//        val backButton = activity?.findViewById<ImageView>(R.id.back_button)
//        backButton?.setOnClickListener {
//            // Ensure that navController is initialized properly (Assuming navController is already declared and available)
//            navController.navigate(R.id.action_myAcc_to_more)
//        }

    }

    override fun onResume() {
        super.onResume()

        // Find the TextView by its ID and set the title to "My Account"
//        activity?.findViewById<TextView>(R.id.header_title)?.let {
//            it.text = "My Account"
//        }

        // Hide the navigation bar and toolbar
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        // Restore visibility of navigation bar and toolbar
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }


    private fun initializeUI(view: View) {
        profileImageView = view.findViewById(R.id.profilePic)
        nicknameEditText = view.findViewById(R.id.etNicknameValue)
        ageEditText = view.findViewById(R.id.etAgeValue)
        oldPasswordEditText = view.findViewById(R.id.etOldPassword)
        newPasswordEditText = view.findViewById(R.id.etNewPassword)
        confirmPasswordEditText = view.findViewById(R.id.etConfirmPassword)
        submitButton = view.findViewById(R.id.btnSubmit)

        profileImageView?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            getImage.launch(intent)
        }

        view.findViewById<TextView>(R.id.tvDeleteAccount).setOnClickListener {
            deleteAccount()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        // Access the "users" collection in Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users/${currentUser.uid}")

        // Fetch the user data from Realtime Database
        databaseRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Retrieve the data from the snapshot
                val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                val age = snapshot.child("age").getValue(String::class.java) ?: ""
                val profilePictureUri = snapshot.child("profilePictureUri").getValue(String::class.java)

                // Set data to corresponding views
                nicknameEditText?.setText(fullName)
                ageEditText?.setText(age)

                // Load the profile picture with Glide and error handling
                profileImageView?.let { imageView ->
                    if (!profilePictureUri.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profilePictureUri)
                            .error(R.drawable.ic_account) // Default image on error
                            .into(imageView)
                    } else {
                        // Set to default if no profile picture is found
                        imageView.setImageResource(R.drawable.ic_account)
                    }
                }
            } else {
                Log.e("MyAccountFragment", "my.edu.tarc.debtdecoderApp.User data not found in Realtime Database")
                Toast.makeText(requireContext(), "my.edu.tarc.debtdecoderApp.User data not found", Toast.LENGTH_SHORT).show()
                profileImageView?.setImageResource(R.drawable.ic_account) // Set to default image
            }
        }.addOnFailureListener { e ->
            Log.e("MyAccountFragment", "Error retrieving user data: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            profileImageView?.setImageResource(R.drawable.ic_account) // Set to default image
        }
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Create a storage reference for the user's profile picture
        val storageRef = storage.reference
        val profileRef = storageRef.child("profile_pictures/$userId.jpg")

        // Upload the new profile picture to Firebase Storage
        profileRef.putFile(imageUri)
            .addOnSuccessListener {
                // Retrieve the download URL of the uploaded image
                profileRef.downloadUrl.addOnSuccessListener { uri ->
                    // Immediately update the profile picture URL in Realtime Database
                    updateProfilePictureUriInRealtimeDatabase(userId, uri.toString())
                }.addOnFailureListener { exception ->
                    Log.e("MyAccountFragment", "Error retrieving download URL: ${exception.message}", exception)
                    Toast.makeText(requireContext(), "Failed to retrieve profile picture URL", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MyAccountFragment", "Profile picture upload failed: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfilePictureUriInRealtimeDatabase(userId: String, downloadUrl: String) {
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users/$userId")
        databaseRef.updateChildren(mapOf("profilePictureUri" to downloadUrl))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("MyAccountFragment", "Updating profile picture URI in Realtime Database failed: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Update failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNicknameAndAge() {
        val currentUser = auth.currentUser ?: return

        // Get the new nickname and age from the EditText fields
        val nickname = nicknameEditText?.text.toString()
        val age = ageEditText?.text.toString().toIntOrNull() ?: return

        // Prepare the data to update in the Realtime Database
        val updates = mapOf(
            "fullName" to nickname,
            "age" to age.toString()
        )

        // Update the nickname and age in the Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users/${currentUser.uid}")
        databaseRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("MyAccountFragment", "Failed to update profile: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }

        // Check if password change is required and proceed only if necessary
        val oldPassword = oldPasswordEditText?.text.toString()
        val newPassword = newPasswordEditText?.text.toString()
        val confirmPassword = confirmPasswordEditText?.text.toString()

        // Update password only if the new password fields are filled and match
        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
            updatePassword(currentUser, oldPassword, newPassword)
        } else if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePassword(currentUser: FirebaseUser, oldPassword: String, newPassword: String) {
        val email = currentUser.email ?: return
        val credential = EmailAuthProvider.getCredential(email, oldPassword)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("MyAccountFragment", "Failed to update password: ${exception.message}", exception)
                        Toast.makeText(requireContext(), "Failed to update password", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("MyAccountFragment", "Re-authentication failed: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Re-authentication failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return

        currentUser.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                // Use the navigation component to navigate to the login screen
                val navController = findNavController()
                navController.navigate(R.id.action_myAcc_to_login)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
