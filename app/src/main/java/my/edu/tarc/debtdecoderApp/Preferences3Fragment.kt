package com.example.expenses_and_budget_mobileassignment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentPreferences3Binding

class Preferences3Fragment : Fragment() {
    private var _binding: FragmentPreferences3Binding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreferences3Binding.inflate(inflater, container, false)
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
        val optionViews = listOf(binding.layoutInAppNotifications, binding.layoutNoNotifications)
        optionViews.forEach { layout ->
            layout.setOnClickListener {
                clearSelections()
                layout.isSelected = true
                updateSelection(layout)
            }
        }

        binding.buttonNext.setOnClickListener {
            val selectedOption = when {
                binding.layoutInAppNotifications.isSelected -> "App"
                binding.layoutNoNotifications.isSelected -> "No"
                else -> ""
            }
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                savePriorityToFirebase(uid, selectedOption)
                findNavController().navigate(R.id.action_preferences3_to_login)
            }
        }
    }

    private fun clearSelections() {
        listOf(binding.layoutInAppNotifications, binding.layoutNoNotifications).forEach {
            it.isSelected = false
            it.background = ContextCompat.getDrawable(requireContext(), R.drawable.unselected_background)
        }
    }

    private fun updateSelection(selectedLayout: LinearLayout) {
        selectedLayout.background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_option_background)
    }

    private fun savePriorityToFirebase(userId: String, notify: String) {
        val userPreferences = mapOf("customNotify" to notify)

        database.child("users").child(userId).child("preferences")
            .updateChildren(userPreferences)
            .addOnSuccessListener {
                Toast.makeText(context, "Notify Option saved Successful", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
