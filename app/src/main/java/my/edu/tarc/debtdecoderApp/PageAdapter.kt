package my.edu.yyass

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import my.edu.tarc.debtdecoderApp.addLoansTab.AddLoanFragment
import my.edu.yyass.repaymentTab.RepaymentFragment
import my.edu.tarc.debtdecoderApp.statusTab.StatusFragment

class PageAdapter(fragmentManager:FragmentManager,lifecycle: Lifecycle):
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 3                //Because we have 3 tabs
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatusFragment()
            1 -> AddLoanFragment()
            2 -> RepaymentFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}