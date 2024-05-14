package my.edu.tarc.debtdecoderApp.Advice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoder.databinding.ItemQuizResultBinding

class QuizResultsAdapter(var results: MutableList<QuizResult>) :
    RecyclerView.Adapter<QuizResultsAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemQuizResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: QuizResult, position: Int) {
            binding.textQuizIndex.text = "Index: ${position + 1}"
            binding.textQuizDate.text = "Date: ${result.date}" // Display the formatted date
            binding.textQuizTitle.text = "Title : ${result.quizTitle}"
            binding.textQuizScore.text = "Correct: ${result.correctAnswers}/${result.totalQuestions}"
            binding.textQuizTime.text = "Time: ${result.timeUsed}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuizResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.bind(result, position)
    }

    override fun getItemCount() = results.size

    fun updateResults(newResults: MutableList<QuizResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()  // Notify that all data has changed
    }
}
