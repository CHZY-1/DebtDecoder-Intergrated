package my.edu.tarc.debtdecoderApp.Advice

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentQuizHistoryBinding

class QuizHistoryFragment : Fragment() {

    private var _binding: FragmentQuizHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentQuizHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadQuizResults() // This would fetch your data and update the adapter.
        binding.btnDeleteHistory.setOnClickListener {
            showDeleteDialog()
        }
        setupDeleteAllButton()  // Set up the delete all button

    }

    private fun showDeleteDialog() {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter index"
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Delete Quiz History")
            .setMessage("Enter the index of the history result to delete:")
            .setView(input)
            .setPositiveButton("Delete") { _, _ ->
                val index = input.text.toString().toIntOrNull()
                if (index != null) {
                    deleteQuizResult(index)
                } else {
                    Toast.makeText(context, "Invalid index", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Changing button text color to white
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }


    private fun deleteQuizResult(userIndex: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val index = userIndex - 1 // Adjust index as user inputs start from 1
        val adapter = binding.rvQuizResults.adapter as QuizResultsAdapter
        if (index >= 0 && index < adapter.itemCount) {
            val resultToDelete = adapter.results[index]

            val ref = FirebaseDatabase.getInstance().getReference("users/$userId/quizresults")
            ref.child(resultToDelete.id.toString()).removeValue().addOnSuccessListener {
                adapter.results.removeAt(index)
                adapter.notifyItemRemoved(index)
                adapter.notifyItemRangeChanged(index, adapter.itemCount - index)
                updateSummaryStats(adapter.results)
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                loadQuizResults()  // Reload the list to ensure all items are correctly updated.
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Invalid index", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupDeleteAllButton() {
        binding.btnDeleteAll.setOnClickListener {
            val dialog = AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete all quiz results?")
                .setPositiveButton("Delete All") { _, _ ->
                    deleteAllQuizResults()
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()

            // Set the button text colors to white
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
    }


    private fun deleteAllQuizResults() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("users/$userId/quizresults")
        ref.removeValue().addOnSuccessListener {
            Toast.makeText(context, "All quiz results deleted successfully.", Toast.LENGTH_SHORT).show()
            loadQuizResults()  // Clear the adapter and refresh the UI.
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to delete quiz results.", Toast.LENGTH_SHORT).show()
        }
    }




    private fun loadQuizResults() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("users/$userId/quizresults")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val results = mutableListOf<QuizResult>()
                dataSnapshot.children.forEach { child ->
                    val result = child.getValue(QuizResult::class.java)?.copy(id = child.key ?: "")
                    result?.let { results.add(it) }
                }
                val adapter = binding.rvQuizResults.adapter as QuizResultsAdapter
                adapter.updateResults(results)
                updateSummaryStats(results)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to load results: ${databaseError.message}", Toast.LENGTH_LONG).show()
            }
        })
    }






    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context) // Creates a vertical LinearLayoutManager
        binding.rvQuizResults.layoutManager = layoutManager // Sets the layout manager to the RecyclerView

        val adapter = QuizResultsAdapter(mutableListOf()) // Initializes the adapter with an empty list
        binding.rvQuizResults.adapter = adapter // Sets the adapter to the RecyclerView
    }

    private fun updateSummaryStats(results: MutableList<QuizResult>) {
        if (results.isEmpty()) {
            binding.tvSummaryStats.text = "No results to display."
            return
        }

        val totalQuestions = results.fold(0) { sum, quizResult -> sum + quizResult.totalQuestions!! }
        val correctAnswers = results.fold(0) { sum, quizResult -> sum + quizResult.correctAnswers!! }
        val percentage = if (totalQuestions > 0) (correctAnswers.toDouble() / totalQuestions * 100).toInt() else 0

        val summaryText = "Answered: $totalQuestions, Correct: $correctAnswers, Success Rate: $percentage%"
        binding.tvSummaryStats.text = summaryText
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
