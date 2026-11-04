package com.meugestor.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Utilitários de data baseados em java.time (thread-safe, ao contrário do SimpleDateFormat
 * compartilhado que era usado antes). Todas as datas persistidas usam o formato ISO yyyy-MM-dd.
 */
object DateUtils {
    private val brLocale = Locale("pt", "BR")
    private val isoFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", brLocale)
    private val monthYearFormat = DateTimeFormatter.ofPattern("MMMM yyyy", brLocale)
    private val dayMonthFormat = DateTimeFormatter.ofPattern("dd MMM", brLocale)
    private val shortMonthFormat = DateTimeFormatter.ofPattern("MMM", brLocale)

    fun todayDate(): LocalDate = LocalDate.now()

    fun today(): String = toIso(todayDate())

    fun toIso(date: LocalDate): String = date.format(isoFormat)

    fun parse(isoDate: String?): LocalDate? {
        if (isoDate.isNullOrBlank()) return null
        return try {
            LocalDate.parse(isoDate.trim().take(10), isoFormat)
        } catch (e: Exception) {
            null
        }
    }

    fun formatDate(isoDate: String): String = parse(isoDate)?.format(displayFormat) ?: isoDate

    fun formatDate(date: LocalDate): String = date.format(displayFormat)

    fun formatMonthYear(isoDate: String): String {
        val date = parse(isoDate) ?: return isoDate
        return formatYearMonth(YearMonth.from(date))
    }

    fun formatYearMonth(yearMonth: YearMonth): String =
        yearMonth.atDay(1).format(monthYearFormat).replaceFirstChar { it.uppercase(brLocale) }

    fun formatShortMonth(yearMonth: YearMonth): String =
        yearMonth.atDay(1).format(shortMonthFormat)
            .replace(".", "")
            .replaceFirstChar { it.uppercase(brLocale) }

    fun formatDayMonth(isoDate: String): String = parse(isoDate)?.format(dayMonthFormat) ?: isoDate

    fun getStartOfMonth(): String = toIso(YearMonth.now().atDay(1))

    fun getEndOfMonth(): String = toIso(YearMonth.now().atEndOfMonth())

    fun startOfMonth(yearMonth: YearMonth): String = toIso(yearMonth.atDay(1))

    fun endOfMonth(yearMonth: YearMonth): String = toIso(yearMonth.atEndOfMonth())

    /** Segunda-feira da semana corrente (antes dependia do primeiro dia da semana do locale). */
    fun getStartOfWeek(): String =
        toIso(todayDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))

    fun getEndOfWeek(): String =
        toIso(todayDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)))

    fun getStartOfYear(): String = toIso(todayDate().withDayOfYear(1))

    fun getEndOfYear(): String = toIso(todayDate().with(TemporalAdjusters.lastDayOfYear()))

    /** Vencido somente a partir do dia seguinte ao vencimento (o próprio dia não é atraso). */
    fun isOverdue(dueDate: String): Boolean {
        val due = parse(dueDate) ?: return false
        return due.isBefore(todayDate())
    }

    /** Dias corridos entre hoje e a data (negativo quando já passou). */
    fun daysUntilDue(dueDate: String): Int {
        val due = parse(dueDate) ?: return 0
        return ChronoUnit.DAYS.between(todayDate(), due).toInt()
    }

    fun addMonths(isoDate: String, months: Int): String {
        val date = parse(isoDate) ?: return isoDate
        return toIso(date.plusMonths(months.toLong()))
    }

    fun currentYear(): Int = todayDate().year

    fun toIsoDate(year: Int, month: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    /** Conversões usadas pelo DatePicker do Material3, que trabalha com milissegundos em UTC. */
    fun toUtcMillis(isoDate: String): Long? =
        parse(isoDate)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    fun fromUtcMillis(millis: Long): String =
        toIso(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
}
