package my.edu.tarc.debtdecoderApp.data
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar

class SharedDateViewModel : ViewModel() {
    private val _selectedDate = MutableLiveData<Calendar>()
    val selectedDate: LiveData<Calendar> get() = _selectedDate

    fun selectDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        _selectedDate.value = cal
    }
}