package com.example.prokject2_tracker.task

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * A stop against a rule that can never fire — an empty weekday mask, say — turning into an endless
 * search. Every scan below is bounded, so a nonsensical task costs a few dozen iterations, not a
 * frozen screen.
 */
private const val MAX_WEEKDAY_SCAN = 7

/**
 * How many *completed* occurrences [oldestOpenOccurrence] will walk past before it gives up being
 * exact. The walk only advances over days already ticked off, so its real length is the number of
 * completions a task has — this is a safety valve, not a limit anyone reaches by using the app.
 */
private const val MAX_OCCURRENCE_SCAN = 2000

/**
 * The first day on or after [day] that this task falls due, or null if it never does again — which
 * only a finished one-off or a rhythm that can't fire (no weekday ticked) will answer.
 *
 * Nothing before [Task.startEpochDay] is ever due: the start day anchors the rhythm as well as
 * bounding it, so moving it forward reschedules the whole series.
 */
fun Task.nextDueOnOrAfter(day: Long): Long? {
    val from = maxOf(day, startEpochDay)
    val step = intervalCount.coerceAtLeast(1)
    return when (recurrence) {
        TaskRecurrence.ONCE -> startEpochDay.takeIf { from <= it }
        TaskRecurrence.EVERY_N_DAYS -> nextOnGrid(from = from, stepDays = step.toLong())
        TaskRecurrence.EVERY_N_WEEKS -> nextOnGrid(from = from, stepDays = step.toLong() * 7)
        TaskRecurrence.WEEKDAYS -> nextWeekday(from)
        TaskRecurrence.DAY_OF_MONTH -> nextDayOfMonth(from = from, everyNMonths = step)
    }
}

/** The next due day strictly after [day] — how the occurrence walk moves on. */
fun Task.nextDueAfter(day: Long): Long? = nextDueOnOrAfter(day + 1)

/**
 * The task's oldest due day up to [today] that is not ticked off, or null when it is all caught up.
 *
 * This is what "fällig" means everywhere in the app: a missed day does not evaporate, it stays as
 * the open occurrence until it is done. Only the *oldest* one is ever reported, so a task neglected
 * for a week shows up once rather than seven times.
 */
fun Task.oldestOpenOccurrence(today: Long, isDone: (Long) -> Boolean): Long? {
    var day = nextDueOnOrAfter(startEpochDay) ?: return null
    var scanned = 0
    while (day <= today) {
        if (!isDone(day)) return day
        day = nextDueAfter(day) ?: return null
        if (++scanned >= MAX_OCCURRENCE_SCAN) break
    }
    return null
}

/** Days laid on a fixed grid from the start day: the first grid point not before [from]. */
private fun Task.nextOnGrid(from: Long, stepDays: Long): Long {
    if (from <= startEpochDay) return startEpochDay
    val elapsed = from - startEpochDay
    // Round *up* to the next grid point, so a `from` that lands between two steps moves forward.
    val stepsTaken = (elapsed + stepDays - 1) / stepDays
    return startEpochDay + stepsTaken * stepDays
}

private fun Task.nextWeekday(from: Long): Long? {
    if (weekdayMask == 0) return null
    repeat(MAX_WEEKDAY_SCAN) { offset ->
        val candidate = from + offset
        if (weekdayMask.hasWeekday(LocalDate.ofEpochDay(candidate).dayOfWeek)) return candidate
    }
    return null
}

/**
 * The [Task.dayOfMonth] of the next month that is in phase with the start month. A day past the end
 * of a short month lands on that month's last day — "am 31." in February means the 28th, because
 * skipping the month entirely would silently drop an occurrence.
 */
private fun Task.nextDayOfMonth(from: Long, everyNMonths: Int): Long? {
    val anchorMonth = YearMonth.from(LocalDate.ofEpochDay(startEpochDay))
    var month = YearMonth.from(LocalDate.ofEpochDay(from))
    // At most one full interval to get in phase, and one more if this month's day is already past.
    repeat(everyNMonths * 2 + 2) {
        val monthsSinceAnchor = ChronoUnit.MONTHS.between(anchorMonth, month)
        if (monthsSinceAnchor >= 0 && monthsSinceAnchor % everyNMonths == 0L) {
            val candidate = month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth())).toEpochDay()
            if (candidate >= from) return candidate
        }
        month = month.plusMonths(1)
    }
    return null
}

/** The weekdays a mask stands for, Monday first — the order the chips are drawn in. */
fun Int.weekdays(): List<DayOfWeek> = DayOfWeek.entries.filter { hasWeekday(it) }
