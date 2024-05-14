package my.edu.tarc.debtdecoderApp.Advice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.debtdecoder.databinding.ItemArticleBinding

class ArticleAdapter(private val articles: List<Article>, private val onClick: (Article) -> Unit) : RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.binding.apply {
            textViewTitle.text = article.title
            Glide.with(imageViewArticle.context)
                .load(article.imageUrl)
                .into(imageViewArticle)
            root.setOnClickListener { onClick(article) }
        }
    }

    override fun getItemCount() = articles.size

    class ArticleViewHolder(val binding: ItemArticleBinding) : RecyclerView.ViewHolder(binding.root)
}
