package my.edu.tarc.debtdecoderApp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentPreferences2Binding

class Preferences2Fragment : Fragment() {
    private var _binding: FragmentPreferences2Binding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreferences2Binding.inflate(inflater, container, false)

        // Initialize Firebase Realtime Database
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
        val optionViews = listOf(binding.layoutNotFamiliar, binding.layoutSomewhatFamiliar, binding.layoutVeryFamiliar)
        optionViews.forEach { layout ->
            layout.setOnClickListener {
                clearSelections()
                layout.isSelected = true  // Manually handle selection state
                updateBackground(layout)
            }
        }

        binding.buttonNext.setOnClickListener {
            val selectedFamiliarity = when {
                binding.layoutNotFamiliar.isSelected -> "No"
                binding.layoutSomewhatFamiliar.isSelected -> "Moderate"
                binding.layoutVeryFamiliar.isSelected -> "Yes"
                else -> ""
            }

            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                saveFamiliarityToFirebase(uid, selectedFamiliarity)
                findNavController().navigate(R.id.action_preferences2_to_preferences3)
            }
        }
    }

    private fun clearSelections() {
        listOf(binding.layoutNotFamiliar, binding.layoutSomewhatFamiliar, binding.layoutVeryFamiliar).forEach {
            it.isSelected = false
            it.background = ContextCompat.getDrawable(requireContext(), R.drawable.unselected_background)
        }
    }

    private fun updateBackground(layout: View) {
        layout.background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_option_background)
    }

    private fun saveFamiliarityToFirebase(userId: String, familiarity: String) {
        val userPreferences = mapOf("budgetFamiliarity" to familiarity)
        database.child("users").child(userId).child("preferences")
            .updateChildren(userPreferences)
            .addOnSuccessListener {
                Toast.makeText(context, "Familiarity Saved Successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to Save", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
