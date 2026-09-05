package com.example.mytracker.achievements

/** What a met goal is worth before its difficulty is applied. */
const val BASE_POINTS = 10.0

/** The most a single goal can be multiplied by, however rarely it is reached. */
const val MAX_DIFFICULTY = 2.5

/** How far back a goal's hit rate is read. Eight weeks: long enough to be a habit, short enough to move. */
const val DIFFICULTY_WINDOW_DAYS = 56

/** One goal of one day, reduced to what the scoring needs to know about it. */
data class ScoredGoal(val id: String, val attribute: AvatarAttribute, val isMet: Boolean)

/** How often a goal was reached over the days before the one being scored. */
data class GoalHistory(val met: Int, val judged: Int)

/** What one day earned, split across the attributes it fed. */
data class DayScore(val epochDay: Long, val points: Map<AvatarAttribute, Double>) {
    val total: Double get() = points.values.sum()
}

/**
 * What a goal is worth to *this* person right now: the rarer they manage it, the more it pays.
 *
 * This is what keeps the score from going stale. A flat ten points per goal is dominated within
 * weeks by whichever goals are easy, and it says the same thing in month twelve as in month one.
 * Weighting by the personal hit rate makes the score re-calibrate itself: as protein turns into a
 * habit it quietly becomes worth less, and the number starts pointing at the next weak spot instead.
 *
 * A goal with no judged history is worth the plain base — a brand-new goal has not yet earned the
 * right to be called hard. A goal never once reached pays the maximum, which is the point: it is the
 * one most worth breaking open.
 */
fun difficultyFactor(history: GoalHistory?): Double {
    if (history == null || history.judged <= 0) return 1.0
    if (history.met <= 0) return MAX_DIFFICULTY
    val hitRate = history.met.toDouble() / history.judged
    return (1.0 / hitRate).coerceIn(1.0, MAX_DIFFICULTY)
}

/**
 * One day's points, per attribute. Only met goals pay — a missed goal costs nothing, since the day
 * it was missed is already its own answer.
 */
fun dayScore(
    epochDay: Long,
    goals: List<ScoredGoal>,
    history: Map<String, GoalHistory>,
): DayScore {
    val points = mutableMapOf<AvatarAttribute, Double>()
    goals.filter { it.isMet }.forEach { goal ->
        val earned = BASE_POINTS * difficultyFactor(history[goal.id])
        points[goal.attribute] = (points[goal.attribute] ?: 0.0) + earned
    }
    return DayScore(epochDay, points)
}
