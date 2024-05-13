package my.edu.tarc.debtdecoderApp.repaymentTab.strategiesTab

import my.edu.tarc.debtdecoderApp.repaymentTab.LoanDatabaseHelper
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import my.edu.tarc.debtdecoder.databinding.FragmentRepaymentStrategiesBinding

class RepaymentStrategiesFragment : Fragment() {
    private lateinit var binding: FragmentRepaymentStrategiesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRepaymentStrategiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardClickListeners()
        updateCardDetails()
    }

    private fun setupCardClickListeners() {
        binding.cardCustomMethod.setOnClickListener {
            navigateToDetails("Borrow")
        }
        binding.cardAvalancheMethod.setOnClickListener {
            navigateToDetails("Avalanche")
        }
        binding.cardSnowballMethod.setOnClickListener {
            navigateToDetails("Snowball")
        }
        binding.cardOwedToYou.setOnClickListener {
            navigateToDetails("OwedToYou")
        }
    }

    private fun navigateToDetails(strategy: String) {
        // Implementation of navigation to details or dialog display
        showDetailsDialog(strategy)
    }

    private fun showDetailsDialog(strategy: String) {
        val dialog = StrategyDetailsDialogFragment.newInstance(strategy)
        dialog.arguments = Bundle().apply {
            putString("strategyType", strategy)
        }
        dialog.show(parentFragmentManager, "StrategyDetails")
    }
    private fun updateCardDetails() {
        val dbHelper = LoanDatabaseHelper(requireContext())

        val nearestDueCustom = dbHelper.getNearestDueBorrowed()
        val highestInterestAvalanche = dbHelper.getHighestInterestBorrowed().toDouble()*100
        val smallestBalanceSnowball = dbHelper.getSmallestBalanceBorrowed()
        val nearestDueOwed = dbHelper.getNearestDueLent()

//        Log.e("Debug Repayment Strategy","Smallest Balance:\n $smallestBalanceSnowball")

        binding.textNearestDueCustom.text = "Nearest Due: $nearestDueCustom"
        binding.textHighestInterestAvalanche.text = "Highest Interest: ${highestInterestAvalanche.toInt()}%"
        binding.textHighestBalanceSnowball.text = "Smallest Balance:\n $smallestBalanceSnowball"
        binding.textNearestDueOwed.text = "Nearest Due: $nearestDueOwed"
    }


}

