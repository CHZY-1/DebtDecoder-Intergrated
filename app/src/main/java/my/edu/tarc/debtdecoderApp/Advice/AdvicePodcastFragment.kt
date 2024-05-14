package my.edu.tarc.debtdecoderApp.Advice

import PodcastAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.debtdecoder.databinding.FragmentAdvicePodcastBinding

class AdvicePodcastFragment : Fragment() {
    private var _binding: FragmentAdvicePodcastBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdvicePodcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().getReference("podcastEpisodes")
        val episodes = mutableListOf<PodcastEpisode>()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                episodes.clear()
                for (episodeSnapshot in snapshot.children) {
                    val episode = episodeSnapshot.getValue(PodcastEpisode::class.java)
                    episode?.let { episodes.add(it) }
                }
                binding.recyclerViewPodcasts.layoutManager = LinearLayoutManager(context)
                binding.recyclerViewPodcasts.adapter = PodcastAdapter(episodes)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load episodes", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}