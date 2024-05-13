package my.edu.tarc.debtdecoderApp.statusTab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import my.edu.tarc.debtdecoder.databinding.FragmentStatusBinding
import my.edu.yyass.statusTab.StatusPagerAdapter

class StatusFragment : Fragment() {
    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val statusPagerAdapter = StatusPagerAdapter(this)
        binding.nestedViewPagerStatus.adapter = statusPagerAdapter
        TabLayoutMediator(binding.nestedTabLayout, binding.nestedViewPagerStatus) { tab, position ->
            tab.text = StatusPagerAdapter.TAB_TITLES[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}