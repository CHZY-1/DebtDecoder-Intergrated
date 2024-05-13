package my.edu.tarc.debtdecoderApp.expenses

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import my.edu.tarc.debtdecoderApp.data.FirebaseExpensesHelper
import my.edu.tarc.debtdecoderApp.data.SharedDateViewModel
import my.edu.tarc.debtdecoderApp.util.DateFormatter
import my.edu.tarc.debtdecoderApp.util.GlideImageLoader
import com.example.expenses_and_budget_mobileassignment.util.getFirebaseHelperInstance
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.FragmentEditExpensesListingBinding
import java.util.Date

class EditExpensesListingFragment : Fragment() {

    private var _binding: FragmentEditExpensesListingBinding? = null
    private val binding get() = _binding!!

    private val dateViewModel: SharedDateViewModel by activityViewModels()
    private lateinit var adapter: ExpenseAdapter
    private lateinit var expensesFirebaseHelper: FirebaseExpensesHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        expensesFirebaseHelper = getFirebaseHelperInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditExpensesListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isAdded) {

            // delete image button
            binding.ivDelete.setOnClickListener {

                setupDeleteExpenseView(true, true, true, "Delete Expenses")
            }

            // Cancel delete button
            binding.btnCancel.setOnClickListener {
                setupDeleteExpenseView(false, false, false, "Expenses")
            }

            binding.btnDelete.setOnClickListener {
                // Get a list of expense object to delete
                val selectedExpenses = adapter.getSelectedExpenses()
                Log.d("SelectedExpenses", selectedExpenses.toString())

                // Msg for confirmation
                val count = selectedExpenses.size
                val message = if (count == 1) {
                    "Are you sure you want to delete 1 expense?"
                } else {
                    "Are you sure you want to delete $count expenses?"
                }

                // Confirmation dialog
                AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton("Delete") { dialog, which ->
                        val totalAmountBeforeDeletion = adapter.getTotalExpense()

                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let { firebaseUser ->
                            val userId = firebaseUser.uid
                            // Delete selected expenses from Firebase
                            expensesFirebaseHelper.deleteExpenses(
                                userId,
                                selectedExpenses
                            ) { success ->
                                if (success) {
                                    // Refresh the expenses adapter
                                    adapter.removeDeletedExpenses(selectedExpenses)

                                    // Calculate and display remaining total expense after deletion
                                    val totalAmountAfterDeletion = adapter.getTotalExpense()
                                    val remainingTotalAmount =
                                        totalAmountBeforeDeletion - totalAmountAfterDeletion
                                    Log.d(
                                        "ExpenseDeletion",
                                        "Successfully deleted $count expenses. Remaining total amount: $remainingTotalAmount"
                                    )

                                    val formattedDate = DateFormatter.formatToIso8601(
                                        dateViewModel.selectedDate.value?.time ?: Date()
                                    )
                                    displayExpenseHeader(formattedDate, remainingTotalAmount)

                                    // Show success message
                                    Toast.makeText(
                                        requireContext(),
                                        "Expenses deleted successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    setupDeleteExpenseView(false, false, false, "Expenses")

                                    // Navigate back if no expenses left for the selected date
                                    if (totalAmountAfterDeletion <= 0) {
//                                findNavController().navigate(R.id.action_editExpensesListingFragment_to_trackExpenseFragment)
                                        findNavController().popBackStack()
                                        Toast.makeText(
                                            requireContext(),
                                            "No expenses left for ${
                                                DateFormatter.formatForDisplay(formattedDate)
                                            }",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } else {
                                    // Show error message
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to delete expenses",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }

            setupRecyclerView()
            observeSelectedDate()

        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(emptyList(), GlideImageLoader())
        binding.rvEditExpenses.adapter = adapter
    }

    private fun setupDeleteExpenseView(selectionMode: Boolean, cancelVisible: Boolean, deleteVisible: Boolean, title: String){
        adapter.setSelectionMode(selectionMode)
        binding.btnCancel.visibility = if(cancelVisible) View.VISIBLE else View.GONE
        binding.btnDelete.visibility = if(deleteVisible) View.VISIBLE else View.GONE
        binding.tvShowExpenses.text = title
    }

    private fun observeSelectedDate() {
        dateViewModel.selectedDate.observe(viewLifecycleOwner) { calendar ->
//            val formattedDate = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(calendar.time)
            val formattedDate = DateFormatter.formatToIso8601(calendar.time)
            Log.d("EditExpensesListingFragment", "formattedDate: $formattedDate")

            val user = FirebaseAuth.getInstance().currentUser
            user?.let { firebaseUser ->
                val userId = firebaseUser.uid
                expensesFirebaseHelper.getExpensesByDate(userId, formattedDate) { expenses ->
                    // Update RecyclerView with fetched expenses
                    adapter.updateData(expenses)

                    // Calculate and display total expense for the selected date
                    val totalExpense = expenses.sumOf { it.amount }
                    displayExpenseHeader(formattedDate, totalExpense)
                }
            }
        }
    }

    private fun displayExpenseHeader(date: String, totalExpense: Double) {
        val displayDate = DateFormatter.formatForDisplay(date)
        binding.tvSectionHeader.text = getString(R.string.expenses_for_date, displayDate)
        binding.tvTotalAmount.text = getString(R.string.total_expense, totalExpense)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}