package com.meugestor.app.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val brLocale = Locale("pt", "BR")
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", brLocale)
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", brLocale)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", brLocale)
    private val dayMonthFormat = SimpleDateFormat("dd MMM", brLocale)

    fun today(): String = isoFormat.format(Date())

    fun formatDate(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            displayFormat.format(date)
        } catch (e: Exception) { isoDate }
    }

    fun formatMonthYear(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            monthYearFormat.format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) { isoDate }
    }

    fun formatDayMonth(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            dayMonthFormat.format(date)
        } catch (e: Exception) { isoDate }
    }

    fun getStartOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return isoFormat.format(cal.time)
    }

    fun getEndOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return isoFormat.format(cal.time)
    }

    fun getStartOfWeek(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return isoFormat.format(cal.time)
    }

    fun getStartOfYear(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return isoFormat.format(cal.time)
    }

    fun getEndOfYear(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        return isoFormat.format(cal.time)
    }

    fun isOverdue(dueDate: String): Boolean {
        return try {
            val due = isoFormat.parse(dueDate) ?: return false
            due.before(Date())
        } catch (e: Exception) { false }
    }

    fun daysUntilDue(dueDate: String): Int {
        return try {
            val due = isoFormat.parse(dueDate) ?: return 0
            val diff = due.time - Date().time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) { 0 }
    }

    fun addMonths(isoDate: String, months: Int): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, months)
            isoFormat.format(cal.time)
        } catch (e: Exception) { isoDate }
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun toIsoDate(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }
}
