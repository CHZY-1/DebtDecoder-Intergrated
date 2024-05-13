package com.example.expenses_and_budget_mobileassignment

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentSignUpBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)

        // Initialize Firebase authentication and database
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
        binding.btnSU.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPass.text.toString().trim()
            val fullName = binding.edtName.text.toString().trim()
            val age = binding.edtAge.text.toString().trim()
            val income = binding.edtIncome.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.user
                            user?.let {
                                saveUserInfo(it, fullName, age, income)
                                // Navigate to Preferences1Fragment after sign-up
                            }
                        } else {
                            Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtLayoutPassword.setEndIconOnClickListener {
            val type = binding.edtPass.inputType
            if (type == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                binding.edtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.edtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.edtPass.setSelection(binding.edtPass.text.length)
        }

        binding.imgSI.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
    }

    private fun saveUserInfo(user: FirebaseUser, fullName: String, age: String, income: String) {
        // Create a map to hold the user's basic info
        val userInfo = mapOf(
            "email" to user.email,
            "fullName" to fullName,
            "age" to age,
            "income" to income
        )

        // Save the user's basic information to the Firebase Realtime Database
        database.child("users").child(user.uid).setValue(userInfo)
            .addOnSuccessListener {
                Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                // Save the past four months of income
                savePastIncome(user.uid, income.toDouble())
                findNavController().navigate(R.id.action_signup_to_preferences1)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to store user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePastIncome(userId: String, monthlyIncome: Double) {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Reference for the user's basic monthly income by month
        for (i in 0 until 4) {
            val month = dateFormat.format(calendar.time)
            val userIncomeRef = database.child("users/$userId/basicIncome/$month").push()

            // Create the income map
            val incomeMap = mapOf(
                "title" to "Basic Income for $month",
                "amount" to monthlyIncome,
                "date" to month,
                "category" to "basic"
            )

            userIncomeRef.setValue(incomeMap).addOnFailureListener {
                Log.e("SignupFragment", "Error saving basic income data for $month: ${it.message}")
            }

            // Move to the previous month
            calendar.add(Calendar.MONTH, -1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
