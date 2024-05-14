package my.edu.tarc.debtdecoderApp.Advice

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.debtdecoder.databinding.FragmentAdviceArticleBinding


class AdviceArticleFragment : Fragment() {
    private var _binding: FragmentAdviceArticleBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdviceArticleBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        return binding.root
    }

    private fun setupRecyclerView() {
        val articles = mutableListOf<Article>()
        database.child("articles").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val article = child.getValue(Article::class.java)
                    article?.let { articles.add(it) }
                }
                binding.rvArticles.layoutManager = GridLayoutManager(context, 2)
                binding.rvArticles.adapter = ArticleAdapter(articles) { article ->
                    val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    try {
                        context!!.startActivity(urlIntent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            "No application available to view the web page",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load articles.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}