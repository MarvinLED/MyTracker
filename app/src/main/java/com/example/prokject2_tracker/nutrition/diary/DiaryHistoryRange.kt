package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.metrics.ChartRange
import com.example.prokject2_tracker.core.metrics.EpochDayRange

/** The three spans the Verlauf offers. Woche is left out — a week of daily bars is the Tagebuch. */
val DiaryHistoryRanges: List<ChartRange> = listOf(ChartRange.MONTH, ChartRange.YEAR, ChartRange.ALL)

/**
 * The window the Verlauf charts, always ending **today**.
 *
 * Deliberately different from how [ChartRange] is anchored elsewhere in the app, where a window
 * hangs off the last logged point so a training chart survives a week off. A food diary is kept
 * daily and a gap in it is itself the information — "Monat" here means the last 30 days, and a week
 * of not logging should show as a week of nothing, not be scrolled out of view.
 *
 * [ChartRange.ALL] starts at [firstLoggedDay]; with nothing logged at all it collapses onto today,
 * and the chart falls back to its own "not enough data" state.
 */
fun diaryHistoryRange(range: ChartRange, firstLoggedDay: Long?, today: Long): EpochDayRange {
    val days = range.days
    val start = if (days != null) today - (days - 1) else firstLoggedDay ?: today
    return EpochDayRange(startInclusive = start.coerceAtMost(today), endInclusive = today)
}
