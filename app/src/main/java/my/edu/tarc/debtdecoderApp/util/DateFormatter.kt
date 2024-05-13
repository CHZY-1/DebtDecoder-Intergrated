package my.edu.tarc.debtdecoderApp.util

import android.annotation.SuppressLint
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {
    private fun getDisplayFormat(): SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private fun getIso8601Format(): SimpleDateFormat {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // ISO 8601 (UTC)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    // Format a date to Iso8601 format
    fun formatToIso8601(date: Date): String {
        return getIso8601Format().format(date)
    }

    // Format a date string from Iso 8601 to display format
    fun formatForDisplay(date: String): String? {
        val iso8601Format = getIso8601Format()
        return try {
            val parsedDate = iso8601Format.parse(date)
            parsedDate?.let { getDisplayFormat().format(it) }
        } catch (e: Exception) {
            null
        }
    }

    // Format a date string for Firebase (Iso8601 format)
    fun formatForFirebase(date: String): String? {
        val userDateFormat = getDisplayFormat()
        return try {
            val parsedDate = userDateFormat.parse(date)
            parsedDate?.let { getIso8601Format().format(it) }
        } catch (e: Exception) {
            null
        }
    }

    // Get today's date in ISO 8601 format for Firebase
    fun getTodayInIso8601Format(): String {
        val today = Calendar.getInstance().time
        return getIso8601Format().format(today)
    }

    // return start date string and end date string in ISO 8601 format
    fun getDateRange(numberOfDays: Int): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = getTodayInIso8601Format()

        calendar.add(Calendar.DAY_OF_YEAR, -numberOfDays)
        val startDate = formatToIso8601(calendar.time)

        return Pair(startDate, endDate)
    }

    // get current month date range in 'dd/MM/yyyy' format
    @SuppressLint("DefaultLocale")
    fun getCurrentMonthDateRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        // Set calendar to the start of the month to get the first date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = String.format("%02d/%02d/%04d", calendar.get(Calendar.DAY_OF_MONTH), month + 1, year)

        // Set calendar to the end of the month to get the last date
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 0)
        val endDate = String.format("%02d/%02d/%04d", calendar.get(Calendar.DAY_OF_MONTH), month + 1, year)

        return Pair(startDate, endDate)
    }

    // convert monthName to int value
    // Jan -> 1, Feb -> 2, ...
    fun getMonthNumber(monthName: String): Int {
        val dateFormatSymbols = DateFormatSymbols()
        val months = dateFormatSymbols.months
        val monthIndex = months.indexOfFirst { it.equals(monthName, ignoreCase = true) }
        // Month index starts from 1 for calender
        return monthIndex + 1
    }

    // get start and end date for month
    fun getStartAndEndDatesForMonth(year: Int, month: Int): Pair<String, String> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val startDate = formatToIso8601(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endDate = formatToIso8601(calendar.time)

        return Pair(startDate, endDate)
    }
}

