package my.edu.tarc.debtdecoderApp.Advice

import AdviceItem
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.databinding.ItemCourseBinding
import my.edu.tarc.debtdecoder.databinding.ItemTitleBinding

class AdviceAdapter(private val items: MutableList<AdviceItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeTitle = 0
    private val typeCourse = 1

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is AdviceItem.Title -> typeTitle
        is AdviceItem.Course -> typeCourse
        else -> throw IllegalStateException("Unknown type of item at position $position")
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typeTitle -> TitleViewHolder(ItemTitleBinding.inflate(inflater, parent, false))
            typeCourse -> CourseViewHolder(ItemCourseBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AdviceItem.Title -> (holder as TitleViewHolder).bind(item)
            is AdviceItem.Course -> (holder as CourseViewHolder).bind(item, position)
            else -> throw IllegalArgumentException("Unknown type of item at position $position")
        }
    }



    override fun getItemCount(): Int = items.size

    inner class TitleViewHolder(private val binding: ItemTitleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AdviceItem.Title) {
            binding.textViewTitle.text = item.text
        }
    }

    inner class CourseViewHolder(private val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AdviceItem.Course, position: Int) {
            // Set text for course title and description
            binding.textViewCourseTitle.text = item.title
            binding.textViewDescription.text = item.description

            // Set visibility of the tick icon based on the completion status
            binding.imageViewTick.visibility = if (item.isCompleted) View.VISIBLE else View.GONE

            // Load the course image using Glide
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .into(binding.imageViewStatus)

            // Set up a click listener for the video button
            binding.videoButton.setOnClickListener {
                // If already completed, do not change the status
                val newStatus = if (item.isCompleted) item.isCompleted else !item.isCompleted

                // Only process changes if there is an actual change in status
                if (newStatus != item.isCompleted) {
                    item.isCompleted = newStatus // Update the local model
                    notifyItemChanged(position) // Notify the adapter of item change to refresh the view

                    // Update the completion status in Firebase, making sure both titleId and courseId are not null
                    item.titleId?.let { titleId ->
                        item.courseId?.let { courseId ->
                            updateCourseCompletionStatusInFirebase(titleId, courseId, newStatus)
                        }
                    }
                }

                // Open the web page for the course URL if it's not null
                item.url?.let { url ->
                    openWebPage(url, itemView.context)
                }
            }
        }


//        private fun updateCourseCompletionStatusInFirebase(titleId: String, courseId: String, isCompleted: Boolean) {
//            val coursePath = "advice_videos/titles/$titleId/courses/$courseId/isCompleted"
//            val databaseReference = FirebaseDatabase.getInstance().getReference(coursePath)
//            databaseReference.setValue(isCompleted)
//                .addOnSuccessListener {
//                    Log.d("Firebase", "Data updated successfully")
//                    Toast.makeText(itemView.context, "Completion status updated", Toast.LENGTH_SHORT).show()
//                }
//                .addOnFailureListener {
//                    Log.e("Firebase", "Failed to update data", it)
//                    Toast.makeText(itemView.context, "Failed to update completion status", Toast.LENGTH_SHORT).show()
//                }
//        }

        private fun updateCourseCompletionStatusInFirebase(titleId: String, courseId: String, isCompleted: Boolean) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let { firebaseUser ->
                val userId = firebaseUser.uid
                val coursePath = "users/$userId/coursecompleted/$courseId/isCompleted"
                val databaseReference = FirebaseDatabase.getInstance().getReference(coursePath)
                databaseReference.setValue(isCompleted)
                    .addOnSuccessListener {
                        Log.d("Firebase", "User-specific data updated successfully")
                        Toast.makeText(itemView.context, "Completion status updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Log.e("Firebase", "Failed to update user-specific data", it)
                        Toast.makeText(itemView.context, "Failed to update completion status", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(itemView.context, "No user logged in", Toast.LENGTH_SHORT).show()
            }
        }


        private fun openWebPage(url: String, context: Context) {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "No application available to view the web page",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }
}