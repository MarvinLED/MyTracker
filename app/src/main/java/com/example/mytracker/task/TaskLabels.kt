package com.example.mytracker.task

import com.example.mytracker.core.util.DateUtils
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN)

/** "Mo", "Di", … — the short form the weekday chips and the rhythm summary both use. */
fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Di"
    DayOfWeek.WEDNESDAY -> "Mi"
    DayOfWeek.THURSDAY -> "Do"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "So"
}

fun formatEpochDay(epochDay: Long): String = DateUtils.localDateOfEpochDay(epochDay).format(dateFormatter)

/**
 * The rhythm in one line, as it reads under the task's name. Interval kinds say "alle 2 Wochen"
 * rather than "alle 1 Wochen" for the common case — "wöchentlich" is what a person would say.
 */
fun Task.recurrenceLabel(): String = when (recurrence) {
    TaskRecurrence.ONCE -> "Einmalig am ${formatEpochDay(startEpochDay)}"
    TaskRecurrence.EVERY_N_DAYS ->
        if (intervalCount == 1) "Täglich" else "Alle $intervalCount Tage"
    TaskRecurrence.EVERY_N_WEEKS -> {
        val weekday = DateUtils.localDateOfEpochDay(startEpochDay).dayOfWeek.shortLabel()
        if (intervalCount == 1) "Wöchentlich ($weekday)" else "Alle $intervalCount Wochen ($weekday)"
    }
    TaskRecurrence.WEEKDAYS ->
        weekdayMask.weekdays().joinToString(", ") { it.shortLabel() }.ifEmpty { "Kein Wochentag gewählt" }
    TaskRecurrence.DAY_OF_MONTH ->
        if (intervalCount == 1) {
            "Monatlich am $dayOfMonth."
        } else {
            "Alle $intervalCount Monate am $dayOfMonth."
        }
}

/**
 * How an open occurrence reads against [today]. Overdue is counted in days rather than shown as a
 * date, because "seit 3 Tagen fällig" is the part that decides whether it gets done now.
 */
fun dueLabel(dueEpochDay: Long, today: Long): String = when (val overdueDays = today - dueEpochDay) {
    0L -> "heute fällig"
    1L -> "seit gestern fällig"
    in 2L..Long.MAX_VALUE -> "seit $overdueDays Tagen fällig"
    // Not due yet — the task screen lists those too, so they say when their turn comes.
    else -> "fällig am ${formatEpochDay(dueEpochDay)}"
}
