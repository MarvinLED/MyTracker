package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.EpochDayRange

/** The three spans the Verlauf offers. Woche is left out — a week of daily bars is the Tagebuch. */
val DiaryHistoryRanges: List<ChartRange> = listOf(ChartRange.MONTH, ChartRange.YEAR, ChartRange.ALL)

/**
 * The window the Verlauf charts, ending **today** — or yesterday when [includeToday] is off.
 *
 * Deliberately different from how [ChartRange] is anchored elsewhere in the app, where a window
 * hangs off the last logged point so a training chart survives a week off. A food diary is kept
 * daily and a gap in it is itself the information — "Monat" here means the last 30 days, and a week
 * of not logging should show as a week of nothing, not be scrolled out of view.
 *
 * Dropping today keeps the span the same length rather than shortening it: the day still to be
 * logged is only ever half a day of eating, and read against a full day's Soll it drags the end of
 * every line down. The window slides back by one day instead of losing one.
 *
 * [ChartRange.ALL] starts at [firstLoggedDay]; with nothing logged at all it collapses onto the last
 * day, and the chart falls back to its own "not enough data" state.
 */
fun diaryHistoryRange(
    range: ChartRange,
    firstLoggedDay: Long?,
    today: Long,
    includeToday: Boolean = true,
): EpochDayRange {
    val end = if (includeToday) today else today - 1
    val days = range.days
    val start = if (days != null) end - (days - 1) else firstLoggedDay ?: end
    return EpochDayRange(startInclusive = start.coerceAtMost(end), endInclusive = end)
}
