package com.example.mytracker.core.metrics

/**
 * The span a chart's x-axis covers, as a number of days back from the **last logged point** — not
 * from today. Anchoring on the last measurement is what makes a short window useful: after a week
 * off, "Woche" anchored at today would be an empty chart, while anchored at the last session it
 * still shows that week of training.
 *
 * [ALL] spans the first logged point to the last, so it always ends on real data too.
 */
enum class ChartRange(val days: Int?) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null),
}

fun ChartRange.label(): String = when (this) {
    ChartRange.WEEK -> "Woche"
    ChartRange.MONTH -> "Monat"
    ChartRange.YEAR -> "Jahr"
    ChartRange.ALL -> "Insgesamt"
}

/**
 * How finely the points are bucketed for a window of [spanDays]. The resolution has to follow the
 * window: weekly points across a one-week axis would be a single dot, and per-session points across
 * five years would be an unreadable band. [spanDays] matters only for [ChartRange.ALL], whose span
 * depends on how long the user has been logging.
 */
fun ChartRange.granularityFor(spanDays: Long): Granularity = when (this) {
    // A training session is one day, so DAILY is "one point per session" here.
    ChartRange.WEEK, ChartRange.MONTH -> Granularity.DAILY
    ChartRange.YEAR -> Granularity.WEEKLY
    ChartRange.ALL -> if (spanDays <= 730) Granularity.WEEKLY else Granularity.MONTHLY
}

/** What a single point means, for the chart header — so the y values are never ambiguous. */
fun Granularity.pointLabel(): String = when (this) {
    Granularity.DAILY -> "pro Training"
    Granularity.WEEKLY -> "pro Woche"
    Granularity.MONTHLY -> "pro Monat"
}
