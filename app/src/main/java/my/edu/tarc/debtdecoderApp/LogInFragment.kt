package my.edu.tarc.debtdecoder

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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

class LoginFragment : Fragment() {
    private var _binding: FragmentLogInBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var isPasswordVisible: Boolean = false

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
        val toolbar = activity?.findViewById<View>(R.id.toolbar)
        val footer = activity?.findViewById<View>(R.id.bottom_navigation)
        toolbar?.visibility = View.GONE
        footer?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
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

            binding.btnLog.isEnabled = false
            loginUser(email, password)
        }

        binding.txtSU.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }

        binding.txtForg.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }

        binding.btnViewPass.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.edtLPass.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnViewPass.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.edtLPass.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnViewPass.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
        isPasswordVisible = !isPasswordVisible
        binding.edtLPass.setSelection(binding.edtLPass.text.length)
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLog.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnLog.isEnabled = true

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userID = user?.uid

                    if (userID != null) {
                        database.child("users").child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Unknown"
                                    val age = snapshot.child("age").getValue(String::class.java) ?: "Unknown"
                                    val income = snapshot.child("income").getValue(String::class.java) ?: "0"

                                    Log.d("LoginFragment", "Full Name: $fullName, Age: $age, Income: $income")

                                    val preferences1 = snapshot.child("priority").children.map {
                                        it.key to it.value
                                    }.toMap()
                                    val preferences2 = snapshot.child("familiarity").children.map {
                                        it.key to it.value
                                    }.toMap()

                                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_login_to_navigationdash)
                                } else {
                                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
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
