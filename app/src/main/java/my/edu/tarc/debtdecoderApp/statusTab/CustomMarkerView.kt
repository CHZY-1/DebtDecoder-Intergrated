package my.edu.yyass.statusTab
import android.content.Context
import android.view.LayoutInflater
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import my.edu.yyass.R
import my.edu.yyass.databinding.CustomMarkerViewBinding

class CustomMarkerView(context: Context) : MarkerView(context, R.layout.custom_marker_view) {
    private val binding = CustomMarkerViewBinding.inflate(LayoutInflater.from(context), this, true)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            if (it.data is List<*>) {
                val details = it.data as List<ToPayFragment.CategoryDetail>
                if (details.size == 1 && details[0].name != "Miscellaneous") {
                    binding.tvContent.text = String.format("RM %.2f", details[0].amount)
                } else {
                    // Handle the Miscellaneous case with multiple subcategories
                    binding.tvContent.text = details.joinToString(separator = "\n") { detail ->
                        "${detail.name}: RM ${String.format("%.2f", detail.amount)}"
                    }
                }
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

