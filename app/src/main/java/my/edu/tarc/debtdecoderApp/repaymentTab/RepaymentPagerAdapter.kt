package my.edu.yyass.repaymentTab

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import my.edu.yyass.repaymentTab.individualTab.IndividualLoanFragment
import my.edu.tarc.debtdecoderApp.repaymentTab.strategiesTab.RepaymentStrategiesFragment

class RepaymentPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    companion object {
        val TAB_TITLES = arrayOf(
            "Individual Loan",
            "Strategies"
        )
    }
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IndividualLoanFragment()
            1 -> RepaymentStrategiesFragment()
            else -> throw IllegalArgumentException("Unexpected position: $position")
        }
    }
}