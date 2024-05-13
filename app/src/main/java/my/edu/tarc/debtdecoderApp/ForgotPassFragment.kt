package com.example.expenses_and_budget_mobileassignment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentForgotPassBinding

class ForgotPassFragment : Fragment() {
    private var _binding: FragmentForgotPassBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPassBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

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
        binding.backLI.setOnClickListener {
            findNavController().navigate(R.id.action_forgot_to_login)
        }

        binding.btnNext.setOnClickListener {
            val email = binding.edtFEmail.text.toString()
            if (email.isNotEmpty()) {
                sendPasswordReset(email)
            } else {
                Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
