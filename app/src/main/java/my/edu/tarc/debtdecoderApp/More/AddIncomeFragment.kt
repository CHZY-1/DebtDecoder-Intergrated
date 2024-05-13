package my.edu.tarc.debtdecoderApp.More

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.debtdecoder.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var amountInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var datePickerButton: Button
    private lateinit var titleInput: EditText
    private lateinit var addButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_income, container, false)
    }

    override fun onResume() {
        super.onResume()
//        activity?.findViewById<ImageView>(R.id.back_button)?.let { imageView ->
//            imageView.setImageResource(R.drawable.back_income)
//        }
//
//        activity?.findViewById<TextView>(R.id.header_title)?.let {
//            it.text = "Add Income"
//        }

        // Hide the navigation bar and toolbar
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
//        activity?.findViewById<ImageView>(R.id.back_button)?.let { imageView ->
//            imageView.setImageResource(R.drawable.icon_back)
//        }

        // Restore visibility of navigation bar and toolbar
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize UI components
        amountInput = view.findViewById(R.id.amount_input)
        datePickerButton = view.findViewById(R.id.date_picker_button)
        titleInput = view.findViewById(R.id.title_input)
        addButton = view.findViewById(R.id.add_button)
        notesInput = view.findViewById(R.id.notes_input)

        // Initialize a date picker (default to today's date)
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        datePickerButton.text = dateFormat.format(today.time)

        // Handle date picking (set up a DatePickerDialog)
        datePickerButton.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val pickedDate = Calendar.getInstance()
                    pickedDate.set(year, month, dayOfMonth)
                    datePickerButton.text = dateFormat.format(pickedDate.time)
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Handle the add button click
        addButton.setOnClickListener {
            addIncome()
        }
//        val navController = findNavController()

//        val incomeLayout = activity?.findViewById<ImageView>(R.id.back_button)
//        incomeLayout?.setOnClickListener {
//            navController.navigate(R.id.action_addIncome_to_Income)
//        }
    }

    private fun addIncome() {
        val currentUser = auth.currentUser ?: return
        val amount = amountInput.text.toString().toDoubleOrNull()
        val date = datePickerButton.text.toString()
        val title = titleInput.text.toString()
        val notes = notesInput.text.toString()

        // Parse the selected date to determine the corresponding year and month
        val selectedDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val parsedDate = selectedDateFormat.parse(date) ?: Calendar.getInstance().time

        // Format the date to "yyyy-MM" to categorize income entries by month
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val month = monthFormat.format(parsedDate)

        // Validate the amount input
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare the data to be added
        val incomeData = mapOf(
            "title" to title,
            "amount" to amount,
            "date" to date,
            "notes" to notes,
            "category" to "ad hoc"
        )

        // Add to the "adHocIncome" under the correct monthly node
        val userIncomeRef = database.child("users/${currentUser.uid}/adHocIncome/$month").push()
        userIncomeRef.setValue(incomeData).addOnSuccessListener {
            Toast.makeText(requireContext(), "Income added successfully!", Toast.LENGTH_SHORT).show()
            clearFields()

            // Navigate back to the desired fragment
            findNavController().popBackStack()
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // Clear the input fields after adding income
    private fun clearFields() {
        titleInput.setText("")
        amountInput.setText("")
        datePickerButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        notesInput.setText("")
    }
}
