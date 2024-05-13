package my.edu.yyass.statusTab

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class StatusPagerAdapter(fragment: Fragment):FragmentStateAdapter(fragment) {
    companion object {
        val TAB_TITLES = arrayOf(
            "To Pay",
            "To Receive"
        )
    }
    override fun getItemCount(): Int =2

    override fun createFragment(position: Int): Fragment {
        return when (position){
            0 -> ToPayFragment()
            1 -> ToReceiveFragment()
            else -> throw IllegalArgumentException("Unexpected position: $position")
        }
    }
}