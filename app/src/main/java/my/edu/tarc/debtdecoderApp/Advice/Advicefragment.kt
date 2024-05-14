package my.edu.tarc.debtdecoderApp.Advice

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentAdvicefragmentBinding
import my.edu.tarc.debtdecoderApp.MainActivity


class Advicefragment : Fragment() {
    private var _binding: FragmentAdvicefragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAdvicefragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ViewPagerAdapter(this)
        binding.adviceViewPager.adapter = adapter
        TabLayoutMediator(binding.adviceTabLayout, binding.adviceViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.video)
                1 -> getString(R.string.article)
                2 -> getString(R.string.podcast)
                3 -> getString(R.string.quiz)
                else -> "Tab ${position + 1}"
            }
        }.attach()

        // false to Clicks on Tabs, true (default) only allows swipe
        binding.adviceViewPager.isUserInputEnabled = false

//        // Access the Toolbar from MainActivity and set its title
//        (requireActivity() as MainActivity).binding.toolbar.headerTitle.text =
//            getString(R.string.advice)

    }
    // Setup ViewPager and TabLayout


    private class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val fragments = arrayOf(
            AdviceVideoFragment(),
            AdviceArticleFragment(),
            AdvicePodcastFragment(),
            AdviceQuizFragment()
        )

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }
}