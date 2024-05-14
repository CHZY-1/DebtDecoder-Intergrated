package my.edu.tarc.debtdecoderApp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentPreferences1Binding

class Preferences1Fragment : Fragment() {
    private var _binding: FragmentPreferences1Binding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreferences1Binding.inflate(inflater, container, false)

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
        val optionViews = listOf(
            binding.layoutSavings,
            binding.layoutStudentLoans,
        )

        optionViews.forEach { layout ->
            layout.setOnClickListener { view ->
                clearSelections()
                setSelected(view as LinearLayout, true)
            }
        }

        binding.buttonNext1.setOnClickListener {
            val selectedPriority = getSelectedPriority()
            if (selectedPriority.isEmpty()) {
                Toast.makeText(context, "Please select a financial priority", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                savePriorityToFirebase(uid, selectedPriority)
                findNavController().navigate(R.id.action_preferences1_to_preferences2)
            } ?: run {
                Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearSelections() {
        binding.layoutSavings.isSelected = false
        binding.layoutStudentLoans.isSelected = false
        resetSelectedStyles()
    }

    private fun setSelected(layout: LinearLayout, selected: Boolean) {
        layout.isSelected = selected
        val textView = layout.getChildAt(1) as TextView
        if (selected) {
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))  // Selected color
            layout.background = ContextCompat.getDrawable(requireContext(), R.drawable.selected_option_background)  // Assuming you have a drawable for selected state
        } else {
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.inactive_color))  // Change to your default text color
            layout.background = ContextCompat.getDrawable(requireContext(), R.drawable.selectable_item_background)
        }
    }


    private fun getSelectedPriority(): String = when {
        binding.layoutSavings.isSelected -> "Avalanche"
        binding.layoutStudentLoans.isSelected -> "Snowball"
        else -> ""
    }

    private fun resetSelectedStyles() {
        listOf(binding.layoutSavings, binding.layoutStudentLoans).forEach {
            setSelected(it, false)
        }
    }

    private fun savePriorityToFirebase(userId: String, priority: String) {
        val userPreferences = mapOf("financialPriority" to priority)
        database.child("users").child(userId).child("preferences")
            .updateChildren(userPreferences)
            .addOnSuccessListener {
                Toast.makeText(context, "Priority saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save priority", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
