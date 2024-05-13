package my.edu.yyass.repaymentTab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import my.edu.yyass.databinding.FragmentRepaymentBinding

class RepaymentFragment : Fragment() {
    private var _binding: FragmentRepaymentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRepaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = RepaymentPagerAdapter(this)
        binding.repaymentViewPager.adapter = adapter
        TabLayoutMediator(binding.repaymentTabLayout, binding.repaymentViewPager) { tab, position ->
            tab.text = RepaymentPagerAdapter.TAB_TITLES[position] // Corrected access
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}