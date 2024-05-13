package my.edu.tarc.debtdecoder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import my.edu.tarc.debtdecoder.databinding.FragmentLogInBinding
import my.edu.tarc.debtdecoder.R

class LoginFragment : Fragment() {
    // View Binding variable
    private var _binding: FragmentLogInBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        setupListeners()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Find the toolbar by its ID and set its visibility to GONE
        val toolbar = activity?.findViewById<View>(R.id.toolbar)
        val footer = activity?.findViewById<View>(R.id.bottom_navigation)
        toolbar?.visibility = View.GONE
        footer?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // Restore the toolbar visibility by setting it to VISIBLE
        val toolbar = activity?.findViewById<View>(R.id.toolbar)
        val footer = activity?.findViewById<View>(R.id.bottom_navigation)
        toolbar?.visibility = View.VISIBLE
        footer?.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnLog.setOnClickListener {
            val email = binding.edtSIEmail.text.toString().trim()
            val password = binding.edtLPass.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable the login button to prevent multiple clicks
            binding.btnLog.isEnabled = false

            // Call login function
            loginUser(email, password)
        }

        // Navigate to the Sign-Up page
        binding.txtSU.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }

        binding.txtForg.setOnClickListener{
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }
    }

    private fun loginUser(email: String, password: String) {
        // Disable the login button to prevent repeated clicks
        binding.btnLog.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Re-enable the login button regardless of success or failure
                binding.btnLog.isEnabled = true

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userID = user?.uid

                    if (userID != null) {
                        // Retrieve data from Firebase Realtime Database
                        database.child("users").child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    // Extract user data (e.g., full name, age, income)
                                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Unknown"
                                    val age = snapshot.child("age").getValue(String::class.java) ?: "Unknown"
                                    val income = snapshot.child("income").getValue(String::class.java) ?: "0"

                                    // Example: Print the data to the log (optional)
                                    Log.d("LoginFragment", "Full Name: $fullName, Age: $age, Income: $income")

                                    // Retrieve preferences if needed (adjust your path structure accordingly)
                                    val preferences1 = snapshot.child("priority").children.map {
                                        it.key to it.value
                                    }.toMap()
                                    val preferences2 = snapshot.child("familiarity").children.map {
                                        it.key to it.value
                                    }.toMap()
                                    val preferences3 = snapshot.child("notification").children.map {
                                        it.key to it.value
                                    }.toMap()

                                    // Navigate to the dashboard
                                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_login_to_navigationdash)
                                } else {
                                    Toast.makeText(requireContext(), "com.example.expenses_and_budget_mobileassignment.User data not found", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("LoginFragment", "Database error: ${error.message}")
                                Toast.makeText(requireContext(), "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(requireContext(), "Failed to obtain user ID", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Extract and show a specific error message
                    val errorMessage = task.exception?.localizedMessage ?: "Login failed"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
