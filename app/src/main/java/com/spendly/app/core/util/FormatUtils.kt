package com.spendly.app.core.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    private val amountFormatter = DecimalFormat("#,##,##0.00")
    private val wholeAmountFormatter = DecimalFormat("#,##,##0")

    fun formatCurrency(
        amountMinorUnits: Long,
        currencySymbol: String = "₹",
        showDecimals: Boolean = true
    ): String {
        val majorUnits = amountMinorUnits / 100.0
        val formatted = if (showDecimals || amountMinorUnits % 100 != 0L) {
            amountFormatter.format(majorUnits)
        } else {
            wholeAmountFormatter.format(majorUnits)
        }
        return "$currencySymbol$formatted"
    }

    fun parseMajorToMinorUnits(input: String): Long {
        val cleanInput = input.replace(",", "").trim()
        if (cleanInput.isBlank()) return 0L
        return try {
            val value = cleanInput.toDouble()
            (value * 100).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val itemDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, itemDate) -> "Today"
            isYesterday(now, itemDate) -> "Yesterday"
            now.get(Calendar.YEAR) == itemDate.get(Calendar.YEAR) -> {
                SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun getStartOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getStartOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, item: Calendar): Boolean {
        val yesterday = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, item)
    }
}
