package my.edu.tarc.debtdecoderApp.repaymentTab.strategiesTab

import my.edu.tarc.debtdecoderApp.repaymentTab.LoanDatabaseHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentRepaymentStrategiesBinding

class RepaymentStrategiesFragment : Fragment() {
    private lateinit var binding: FragmentRepaymentStrategiesBinding
    private lateinit var database: DatabaseReference
    private var userPreference: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRepaymentStrategiesBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference
        fetchUserPreferences()
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
        val highestInterestAvalanche = dbHelper.getHighestInterestBorrowed().toDouble() * 100
        val smallestBalanceSnowball = dbHelper.getSmallestBalanceBorrowed()
        val nearestDueOwed = dbHelper.getNearestDueLent()

        binding.textNearestDueCustom.text = "Nearest Due: $nearestDueCustom"
        binding.textHighestInterestAvalanche.text = "Highest Interest: ${highestInterestAvalanche.toInt()}%"
        binding.textHighestBalanceSnowball.text = "Smallest Balance:\n $smallestBalanceSnowball"
        binding.textNearestDueOwed.text = "Nearest Due: $nearestDueOwed"

        when (userPreference) {
            "Avalanche" -> highlightRecommendedStrategy(
                binding.cardAvalancheMethod.findViewById(R.id.avalancheLayout),
                binding.recommendedAvalanche
            )
            "Snowball" -> highlightRecommendedStrategy(
                binding.cardSnowballMethod.findViewById(R.id.snowballLayout),
                binding.recommendedSnowball
            )
        }
    }

    private fun highlightRecommendedStrategy(layout: LinearLayout, recommendedTextView: View) {
        layout.background = ContextCompat.getDrawable(requireContext(), R.drawable.border_highlight)
        recommendedTextView.visibility = View.VISIBLE
    }

    private fun fetchUserPreferences() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            database.child("users").child(uid).child("preferences").child("financialPriority")
                .get().addOnSuccessListener { dataSnapshot ->
                    userPreference = dataSnapshot.value as? String
                    updateCardDetails()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to fetch preferences", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
