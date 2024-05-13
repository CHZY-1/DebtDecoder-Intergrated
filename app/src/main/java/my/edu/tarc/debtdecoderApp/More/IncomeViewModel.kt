package my.edu.tarc.debtdecoderApp.More

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IncomeViewModel : ViewModel() {
    private val _currentMonthIncome = MutableLiveData<Float>()
    val currentMonthIncome: LiveData<Float> get() = _currentMonthIncome

    fun updateCurrentMonthIncome(newIncome: Float) {
        _currentMonthIncome.value = newIncome
    }
}
