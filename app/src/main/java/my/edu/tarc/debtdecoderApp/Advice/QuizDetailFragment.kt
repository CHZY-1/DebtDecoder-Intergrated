package my.edu.tarc.debtdecoderApp.Advice

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import my.edu.tarc.debtdecoder.databinding.FragmentQuizDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale
import my.edu.tarc.debtdecoder.R



class QuizDetailFragment : Fragment() {
    private var _binding: FragmentQuizDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseReference: DatabaseReference
    private var currentQuestionIndex = 0
    private lateinit var questions: List<Question>
    private var correctAttempts = 0
    private lateinit var countdownTimer: CountDownTimer
    private var quizDuration: Long = 0 // Duration in milliseconds
    private lateinit var quizId: String
    private lateinit var testName: String
    private var quizStartTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQuizDetailBinding.inflate(inflater, container, false)

        quizId = arguments?.getString("quizId") ?: run {
            Toast.makeText(context, "Quiz ID not found", Toast.LENGTH_LONG).show()
            return binding.root  // Exit if no quizId is found
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("Quizzes/$quizId/Questions")
        loadQuestions {
            displayCurrentQuestion()
        }
        loadQuizDetails(quizId)
        binding.btnNextQuestion.setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                displayCurrentQuestion()
            }
        }


        return binding.root
    }


    private fun loadQuizDetails(quizId: String) {
        val quizRef = FirebaseDatabase.getInstance().getReference("Quizzes/$quizId")
        quizRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                testName = dataSnapshot.child("testName").getValue(String::class.java) ?: "No Name"
                val durationString = dataSnapshot.child("duration").getValue(String::class.java) ?: "0m"
                quizDuration = durationString.filter { it.isDigit() }.toLong()
                binding.testNameTextView.text = testName  // Set the test name in the UI
                initCountdownTimer(quizDuration * 60 * 1000) // Convert minutes to milliseconds
                loadQuestions {
                    displayCurrentQuestion()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to load quiz details: ${databaseError.message}", Toast.LENGTH_LONG).show()
            }
        })
    }



    private fun initCountdownTimer(timeInMillis: Long) {
        quizStartTime = System.currentTimeMillis()  // Record the start time when initializing the timer

        // Cancel the existing timer if it's already running
        if (this::countdownTimer.isInitialized) {
            countdownTimer.cancel()
        }

        countdownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerUI(millisUntilFinished / 1000)
            }

            override fun onFinish() {
                showResults()
            }
        }.start()
    }






    private fun updateTimerUI(secondsLeft: Long) {
        binding.timerTextView.text = String.format("CountDown: %02d:%02d", secondsLeft / 60, secondsLeft % 60)
    }




    private fun loadQuestions(onComplete: () -> Unit) {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<Question>()
                snapshot.children.forEach { postSnapshot ->
                    postSnapshot.getValue(Question::class.java)?.let {
                        tempList.add(it)
                    }
                }
                // Shuffle the list of questions here
                tempList.shuffle()
                questions = tempList
                currentQuestionIndex = 0  // Reset the question index to start from the first question
                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load questions: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun displayCurrentQuestion() {
        if (questions.isEmpty()) {
            Toast.makeText(context, "No questions available.", Toast.LENGTH_SHORT).show()
            return
        }

        val question = questions[currentQuestionIndex]
        val questionText = "${currentQuestionIndex + 1}. ${question.question}" // Prepend the index
        binding.tvQuestion.text = questionText
        // Update progress bar
        binding.progressBar.max = questions.size
        binding.progressBar.progress = currentQuestionIndex + 1  // currentQuestionIndex is 0-based
        val shuffledOptions = question.options!!.shuffled() // Ensure options are shuffled if applicable
        val correctAnswer = question.answer

        // Resetting button states and visibility for safety
        binding.btnNextQuestion.isEnabled = false // Disabled until an answer is selected
        binding.btnNextQuestion.visibility = View.VISIBLE // Ensure visibility

        listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4).forEachIndexed { index, button ->
            button.text = shuffledOptions[index]
            button.isEnabled = true
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            button.setOnClickListener {
                if (correctAnswer != null) {
                    handleAnswerSelected(button, shuffledOptions[index], correctAnswer)
                }
                binding.btnNextQuestion.isEnabled = true // Enable after an answer is selected
            }
        }

        // Update the text and action of the "Next Question" button based on quiz progress
        binding.btnNextQuestion.text = if (currentQuestionIndex < questions.size - 1) getString(R.string.next_question) else getString(R.string.see_result)
        binding.btnNextQuestion.setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                displayCurrentQuestion()
            } else {
                showResults()
            }
        }
    }



    private fun showResults() {
        val endTime = System.currentTimeMillis()
        val timeUsed = endTime - quizStartTime
        val minutesUsed = timeUsed / 1000 / 60
        val secondsUsed = (timeUsed / 1000) % 60
        val timeUsedString = String.format("%02d:%02d", minutesUsed, secondsUsed)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(System.currentTimeMillis())
        val message = "You answered $correctAttempts out of ${questions.size} questions correctly.\nTime used: $timeUsedString"
        // Create the QuizResult object
        val result = QuizResult(
            quizTitle = testName,
            totalQuestions = questions.size,
            correctAnswers = correctAttempts,
            timeUsed = timeUsedString,
            date = formattedDate  // Store the formatted date
        )

        // Save the result to Firebase
        saveResultToFirebase(result)
        // Create an AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Quiz Results")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Retake", null)
            .create()

        dialog.show()

        // Setup button listeners
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            requireActivity().onBackPressed()
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            // Reset the quiz for retaking
            currentQuestionIndex = 0
            correctAttempts = 0
            loadQuizDetails(quizId)  // This also restarts the timer
            dialog.dismiss()
        }

        // Set button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }



    private fun saveResultToFirebase(result: QuizResult) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d("Firebase", "User not logged in, cannot save quiz result.")
            return  // Exit the function if no user is logged in
        }

        // Reference to the user-specific path for storing quiz results
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/quizresults")
        val resultId = databaseReference.push().key  // Generate a unique key for each result

        resultId?.let {
            databaseReference.child(it).setValue(result)
                .addOnSuccessListener {
                    Log.d("Firebase", "Quiz result saved successfully.")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to save quiz result.", it)
                }
        } ?: Log.e("Firebase", "Failed to generate a unique key for the quiz result.")
    }





    private fun handleAnswerSelected(selectedButton: Button, selectedAnswer: String, correctAnswer: String) {
        // Disable all option buttons to prevent further selections
        listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4).forEach { button ->
            button.isEnabled = false
        }

        // Set color based on whether the answer is correct
        if (selectedAnswer == correctAnswer) {
            selectedButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.greenosh))
            correctAttempts++  // Increase count of correct answers
        } else {
            selectedButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.redosh))
            // Highlight the correct answer
            listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4).forEach { button ->
                if (button.text == correctAnswer) {
                    button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.greenosh))
                }
            }
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        if (this::countdownTimer.isInitialized) {
            countdownTimer.cancel()
        }
        _binding = null
    }


    companion object {
        fun newInstance(quizId: String) = QuizDetailFragment().apply {
            arguments = Bundle().apply {
                putString("QUIZ_ID", quizId)
            }
        }
    }
}
