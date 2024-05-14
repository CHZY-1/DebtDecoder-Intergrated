package my.edu.tarc.debtdecoderApp.income

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoder.R

class AdHocIncomeAdapter(
    private var items: MutableList<AdHocIncomeItem>,
    private val onItemClick: (AdHocIncomeItem) -> Unit
) : RecyclerView.Adapter<AdHocIncomeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ad_hoc_income, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun clearData() {
        items.clear()
        notifyDataSetChanged()
    }

    fun updateData(newItems: List<AdHocIncomeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (AdHocIncomeItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text)
        private val dateTextView: TextView = itemView.findViewById(R.id.date_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.amount_text)

        fun bind(item: AdHocIncomeItem) {
            titleTextView.text = item.title
            dateTextView.text = item.date
            amountTextView.text = "RM${item.amount}"

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
