package com.example.expenses_and_budget_mobileassignment.expenses

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.expenses_and_budget_mobileassignment.util.ExpenseCategoryManager
import com.google.android.material.tabs.TabLayoutMediator
import my.edu.tarc.debtdecoder.databinding.FragmentExpensesBinding
import my.edu.tarc.debtdecoderApp.MainActivity
import my.edu.tarc.debtdecoderApp.expenses.ExpensesInsightFragment
import my.edu.tarc.debtdecoderApp.expenses.ExpensesTimelineFragment
import my.edu.tarc.debtdecoderApp.expenses.TrackExpenseFragment

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).toggleHeaderSyncBtnVisibility(false)

        // Prefetch category images
        context?.let { context ->
            ExpenseCategoryManager.prefetchCategoryImages(context)
        }

        //Setup ViewPager and TabLayout
        val adapter = ViewPagerAdapter(this@ExpensesFragment)
        binding.expensesViewPager.adapter = adapter
        TabLayoutMediator(binding.expensesTabLayout, binding.expensesViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Timeline"
                1 -> "Track Expense"
                2 -> "Insight"
                else -> "Tab ${position + 1}"
            }
        }.attach()

        // false to allow Clicks on Tabs, true (default) only allows swipe
        binding.expensesViewPager.isUserInputEnabled = false

//        (activity as? AppCompatActivity)?.supportActionBar?.title = "Expense"
    }

    // Adapter class for managing the fragments in ViewPager2
    private class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val fragments = arrayOf(
            ExpensesTimelineFragment(),
            TrackExpenseFragment(),
            ExpensesInsightFragment()
        )

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemId(position: Int): Long {
            // Return a unique identifier for each fragment
            return fragments[position].hashCode().toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            // Check if the adapter contains a fragment with the given itemId
            return fragments.any { it.hashCode().toLong() == itemId }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}