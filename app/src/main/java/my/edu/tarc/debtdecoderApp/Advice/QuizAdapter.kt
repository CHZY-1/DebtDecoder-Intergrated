package my.edu.tarc.debtdecoderApp.Advice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.debtdecoder.databinding.ItemQuizBinding

class QuizAdapter(var quizzes: List<Quiz>, private val listener: OnQuizClickListener) : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

    interface OnQuizClickListener {
        fun onQuizClick(quiz: Quiz)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val binding = ItemQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuizViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.bind(quizzes[position])
    }

    override fun getItemCount(): Int = quizzes.size

    class QuizViewHolder(private val binding: ItemQuizBinding, private val listener: OnQuizClickListener) : RecyclerView.ViewHolder(binding.root) {
        fun bind(quiz: Quiz) {
            binding.apply {
                tvTestName.text = quiz.testName
                tvDuration.text = quiz.duration
                tvDifficulty.text = quiz.difficulty
                tvNumOfMCQ.text = "${quiz.numOfMCQ} MCQ"
                Glide.with(itemView.context).load(quiz.url).into(ivQuizImage)

                root.setOnClickListener {
                    listener.onQuizClick(quiz)
                }
            }
        }
    }
}
