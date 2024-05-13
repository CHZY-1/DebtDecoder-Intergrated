package com.example.expenses_and_budget_mobileassignment.More

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoder.R

class AdHocIncomeAdapter(
    private var items: MutableList<AdHocIncomeItem>,
    private val onItemClick: (AdHocIncomeItem) -> Unit  // Callback for item clicks
) : RecyclerView.Adapter<AdHocIncomeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ad_hoc_income, parent, false)
        return ViewHolder(view, onItemClick)  // Pass the click listener to the ViewHolder
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
    // Adapter method to update data
    fun updateData(newItems: List<AdHocIncomeItem>) {
        if (items != newItems) {  // This check is simplistic; in practice, you might need more complex logic
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()  // Notify the RecyclerView that the entire dataset has changed
        }
    }



    class ViewHolder(itemView: View, private val onItemClick: (AdHocIncomeItem) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text)
        private val dateTextView: TextView = itemView.findViewById(R.id.date_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.amount_text)

        fun bind(item: AdHocIncomeItem) {
            titleTextView.text = item.title
            dateTextView.text = item.date
            amountTextView.text = "RM${item.amount}"

            // Use the item within the bind method for setting the click listener
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}





