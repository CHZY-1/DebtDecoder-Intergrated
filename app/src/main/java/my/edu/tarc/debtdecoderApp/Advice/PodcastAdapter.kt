import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.debtdecoderApp.Advice.PodcastEpisode
import my.edu.tarc.debtdecoder.databinding.ItemPodcastBinding

class PodcastAdapter(private val episodes: List<PodcastEpisode>) : RecyclerView.Adapter<PodcastAdapter.PodcastViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val binding = ItemPodcastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PodcastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val episode = episodes[position]
        holder.bind(episode)
    }

    override fun getItemCount(): Int = episodes.size

    class PodcastViewHolder(private val binding: ItemPodcastBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: PodcastEpisode) {
            binding.textViewTitle.text = episode.title
            Glide.with(binding.imageView.context).load(episode.imageUrl).into(binding.imageView)

            // Set up the button click listener to open a URL
            binding.button.setOnClickListener {
                val context = it.context
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(episode.buttonUrl))
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No application available to view this content", Toast.LENGTH_SHORT).show()
                }

            }

        }
    }

}
