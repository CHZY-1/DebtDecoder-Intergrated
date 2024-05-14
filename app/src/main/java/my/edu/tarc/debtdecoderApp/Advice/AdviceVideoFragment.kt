package my.edu.tarc.debtdecoderApp.Advice

import AdviceItem
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.debtdecoder.databinding.FragmentAdviceVideoBinding
import java.util.concurrent.atomic.AtomicInteger

class AdviceVideoFragment : Fragment() {
    private var _binding: FragmentAdviceVideoBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private val adviceList = mutableListOf<AdviceItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAdviceVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance().getReference("advice_videos/titles")
        loadDataFromFirebase()
    }

//    private fun loadDataFromFirebase() {
//        database = FirebaseDatabase.getInstance().getReference("advice_videos/titles")
//        database.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                adviceList.clear()
//                snapshot.children.forEach { titleSnapshot ->
//                    val titleText = titleSnapshot.child("text").getValue(String::class.java) ?: ""
//                    adviceList.add(AdviceItem.Title(titleText))
//                    titleSnapshot.child("courses").children.forEach { courseSnapshot ->
//                        val course = courseSnapshot.getValue(AdviceItem.Course::class.java)?.apply {
//                            this.titleId = titleSnapshot.key
//                            this.courseId = courseSnapshot.key
//                            this.isCompleted = courseSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false
//                        }
//                        course?.let { adviceList.add(it) }
//                    }
//                }
//                setupRecyclerView()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(requireContext(), "Failed to load data from database.", Toast.LENGTH_LONG).show()
//            }
//        })
//
//    }

//    private fun loadDataFromFirebase() {
//        val user = FirebaseAuth.getInstance().currentUser
//        user?.let { firebaseUser ->
//            val userId = firebaseUser.uid
//            val userCoursesRef = FirebaseDatabase.getInstance().getReference("users/$userId/coursecompleted")
//
//            database = FirebaseDatabase.getInstance().getReference("advice_videos/titles")
//            database.addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    adviceList.clear()
//                    snapshot.children.forEach { titleSnapshot ->
//                        val titleText = titleSnapshot.child("text").getValue(String::class.java) ?: ""
//                        adviceList.add(AdviceItem.Title(titleText))
//
//                        titleSnapshot.child("courses").children.forEach { courseSnapshot ->
//                            val courseId = courseSnapshot.key ?: ""
//                            val course = courseSnapshot.getValue(AdviceItem.Course::class.java)?.apply {
//                                this.titleId = titleSnapshot.key
//                                this.courseId = courseId
//                            }
//
//                            // Fetching the completion status, handle null as 'false'
//                            userCoursesRef.child(courseId).child("isCompleted").get().addOnCompleteListener { task ->
//                                val isCompleted = if (task.isSuccessful) {
//                                    task.result?.getValue(Boolean::class.java) ?: false
//                                } else {
//                                    false  // Default to false if the query failed or data doesn't exist
//                                }
//                                course?.isCompleted = isCompleted
//                                course?.let {
//                                    synchronized(adviceList) {
//                                        adviceList.add(it)
//                                    }
//                                }
//                                setupRecyclerView()  // Ensure this is called after the status is updated
//                            }
//                        }
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Toast.makeText(requireContext(), "Failed to load data from database.", Toast.LENGTH_LONG).show()
//                }
//            })
//        } ?: run {
//            Toast.makeText(requireContext(), "No user logged in.", Toast.LENGTH_LONG).show()
//        }
//    }

    private fun loadDataFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            val userId = firebaseUser.uid
            val userCoursesRef = FirebaseDatabase.getInstance().getReference("users/$userId/coursecompleted")

            database = FirebaseDatabase.getInstance().getReference("advice_videos/titles")
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    adviceList.clear()
                    val titleCount = AtomicInteger(snapshot.childrenCount.toInt())

                    snapshot.children.forEach { titleSnapshot ->
                        val titleText = titleSnapshot.child("text").getValue(String::class.java) ?: ""
                        val title = AdviceItem.Title(titleText)
                        val courses = mutableListOf<AdviceItem.Course>()

                        // Track the completion of course data fetching
                        val courseCount = AtomicInteger(titleSnapshot.child("courses").childrenCount.toInt())

                        if (courseCount.get() == 0) {
                            adviceList.add(title)
                            if (titleCount.decrementAndGet() == 0) {
                                setupRecyclerView()  // Update RecyclerView when all titles processed
                            }
                        }

                        titleSnapshot.child("courses").children.forEach { courseSnapshot ->
                            val courseId = courseSnapshot.key ?: ""
                            val course = courseSnapshot.getValue(AdviceItem.Course::class.java)?.apply {
                                this.titleId = titleSnapshot.key
                                this.courseId = courseId
                            }

                            userCoursesRef.child(courseId).child("isCompleted").get().addOnCompleteListener { task ->
                                val isCompleted = task.result?.getValue(Boolean::class.java) ?: false
                                course?.isCompleted = isCompleted
                                course?.let { courses.add(it) }

                                // Decrement the count and add to the list when all courses are processed
                                if (courseCount.decrementAndGet() == 0) {
                                    synchronized(adviceList) {
                                        adviceList.add(title)
                                        adviceList.addAll(courses)
                                    }
                                    // Update RecyclerView when all data for a title is processed
                                    if (titleCount.decrementAndGet() == 0) {
                                        setupRecyclerView()
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load data from database.", Toast.LENGTH_LONG).show()
                }
            })
        } ?: run {
            Toast.makeText(requireContext(), "No user logged in.", Toast.LENGTH_LONG).show()
        }
    }



    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = AdviceAdapter(adviceList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}