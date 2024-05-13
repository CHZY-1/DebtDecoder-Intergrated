package my.edu.tarc.debtdecoderApp.expenses
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.debtdecoderApp.data.ExpenseCategory
import my.edu.tarc.debtdecoderApp.data.ExpenseItem
import my.edu.tarc.debtdecoderApp.data.ListItem
import my.edu.tarc.debtdecoderApp.data.SectionHeader
import com.example.expenses_and_budget_mobileassignment.util.ExpenseCategoryManager
import my.edu.tarc.debtdecoderApp.util.GlideImageLoader
import my.edu.tarc.debtdecoder.databinding.ExpensesItemSectionHeaderBinding
import my.edu.tarc.debtdecoder.databinding.ItemExpenseBinding

//An adapter that displays section headers and expense items in a RecyclerView.
class SectionedExpenseAdapter(private var items: MutableList<ListItem>, private val imageLoader: GlideImageLoader) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var categoryMap: Map<String, ExpenseCategory> = emptyMap()

    init {
        ExpenseCategoryManager.getCategories { categories ->
            categoryMap = categories
            // Refresh adapter
            notifyDataSetChanged()
        }
    }

    //     ViewHolder for Header
    class HeaderViewHolder(private val binding: ExpensesItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

//        Binds a section header item to the ViewHolder.
        fun bind(header: SectionHeader) {
            binding.tvSectionHeader.text = header.date
            binding.tvTotalAmount.text = "RM %.2f".format(header.totalAmount)
        }
    }

//     ViewHolder for Expense Item
    class ItemViewHolder(private val binding: ItemExpenseBinding,
                         private val imageLoader: GlideImageLoader,
                         private val categoryMap: Map<String, ExpenseCategory>) : RecyclerView.ViewHolder(binding.root) {

//    Binds an individual expense item to the ViewHolder.
        fun bind(item: ExpenseItem) {
            binding.tvExpenseCategory.text = item.category
            binding.tvExpensePaymentMethod.text = item.paymentMethod
            binding.tvExpenseAmount.text = "RM %.2f".format(item.amount)

            val categoryImage = categoryMap[item.category]?.imageUrl ?: ""
            imageLoader.loadCategoryImage(categoryImage, binding.ivExpenseIcon, itemView.context)

            Log.d("ItemViewHolder", "Category image URL: $categoryImage")

        }
    }

//    Determines the type of view (header or item) to create for a given position.
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SectionHeader -> 0
            is ExpenseItem -> 1
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

//    Creates and returns a ViewHolder object for the given view type.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val binding = ExpensesItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            1 -> {
                val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ItemViewHolder(binding, imageLoader, categoryMap)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

//    Binds data to the appropriate ViewHolder based on the item view type.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as SectionHeader)
            is ItemViewHolder -> holder.bind(items[position] as ExpenseItem)
        }
    }

    fun updateData(newItems: List<ListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
}