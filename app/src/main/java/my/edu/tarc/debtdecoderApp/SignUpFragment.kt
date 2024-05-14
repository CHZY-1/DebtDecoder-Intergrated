package my.edu.tarc.debtdecoderApp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
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
            val confirmPassword = binding.edtConPass.text.toString().trim()
            val fullName = binding.edtName.text.toString().trim()
            val ageStr = binding.edtAge.text.toString().trim()
            val incomeStr = binding.edtIncome.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || fullName.isEmpty() || ageStr.isEmpty() || incomeStr.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageStr.toIntOrNull()
            if (age == null || age <= 0) {
                Toast.makeText(context, "Please enter a valid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val income = incomeStr.toDoubleOrNull()
            if (income == null || income <= 0) {
                Toast.makeText(context, "Please enter a valid income", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.checkBox.isChecked) {
                Toast.makeText(context, "You must agree to the terms of use", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        user?.let {
                            saveUserInfo(it, fullName, age, income)
                        }
                    } else {
                        Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                    }
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

        binding.txtTerm.setOnClickListener {
            val url = "https://policies.google.com/terms?hl=en-US"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun saveUserInfo(user: FirebaseUser, fullName: String, age: Int, income: Double) {
        // Create a map to hold the user's basic info
        val userInfo = mapOf(
            "email" to user.email,
            "fullName" to fullName,
            "age" to age.toString(),
            "income" to income.toString()
        )

        // Save the user's basic information to the Firebase Realtime Database
        database.child("users").child(user.uid).setValue(userInfo)
            .addOnSuccessListener {
                Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                // Save the past four months of income
                savePastIncome(user.uid, income)
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
