package my.edu.tarc.debtdecoderApp.Advice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentAdviceQuizBinding

class AdviceQuizFragment : Fragment() {

    private var _binding: FragmentAdviceQuizBinding? = null
    private val binding get() = _binding!!
    private lateinit var quizAdapter: QuizAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdviceQuizBinding.inflate(inflater, container, false)
        initRecyclerView()
        loadDataFromFirebase()

        // Set click listener for TextView
        binding.tvCheckHistory.setOnClickListener {
            findNavController().navigate(R.id.action_toQuizHistoryFragment)
        }
        return binding.root
    }

    private fun initRecyclerView() {
        quizAdapter = QuizAdapter(emptyList(), object : QuizAdapter.OnQuizClickListener {
            override fun onQuizClick(quiz: Quiz) {
                val bundle = Bundle()
                bundle.putString("quizId", quiz.quizId)  // Assume quizId is a String and not null
                findNavController().navigate(R.id.action_toQuizDetailFragment, bundle)
                Toast.makeText(context, "Clicked on ${quiz.testName}", Toast.LENGTH_SHORT).show()
            }
        })
        binding.rvQuizList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = quizAdapter
        }
    }

    private fun loadDataFromFirebase() {
        val ref = FirebaseDatabase.getInstance().getReference("Quizzes")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val quizzes = mutableListOf<Quiz>()
                dataSnapshot.children.forEach { child ->
                    val quiz = child.getValue(Quiz::class.java)
                    quiz?.let {
                        it.quizId = child.key  // Store the key as quizId
                        quizzes.add(it)
                    }
                }
                quizAdapter.quizzes = quizzes
                binding.rvQuizList.adapter = quizAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to load quizzes: ${databaseError.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
