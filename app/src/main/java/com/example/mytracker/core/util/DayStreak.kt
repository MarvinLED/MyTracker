package com.example.mytracker.core.util

/**
 * How a run of days is going: the one in progress, and the longest there has ever been.
 *
 * [best] is what keeps a broken run from vanishing without a trace. Without it the one bad day costs
 * everything that came before it, which is the surest way to make someone stop altogether; with the
 * mark left behind, there is still something to beat.
 */
data class DayStreak(val current: Int, val best: Int)

/**
 * Folds a set of days into its two runs — used both for a Habit that was kept and for the days
 * anything at all was logged. Pure over the set so it needs neither a database nor a clock.
 *
 * [today] need not be in [days]: a day that is still running is not a day that was missed, so the
 * current run is counted from yesterday when today is not in the set yet. This is the rule the
 * Fitness-Ziele already follow by leaving the running period out of the streak — without it a run
 * would read as broken every morning and mend itself again each evening.
 */
fun dayStreak(days: Set<Long>, today: Long): DayStreak {
    var current = 0
    // Today when it already counts, else yesterday — but no further back than that, or a run
    // abandoned last week would still be presented as running.
    var day = if (today in days) today else today - 1
    while (day in days) {
        current++
        day--
    }

    var best = 0
    // Only days whose predecessor is missing start a run, and each run is then walked forward once.
    // Every day is therefore visited at most twice, whatever order the set comes in.
    for (start in days) {
        if (start - 1 in days) continue
        var span = 0
        var walk = start
        while (walk in days) {
            span++
            walk++
        }
        if (span > best) best = span
    }

    return DayStreak(current = current, best = best)
}
