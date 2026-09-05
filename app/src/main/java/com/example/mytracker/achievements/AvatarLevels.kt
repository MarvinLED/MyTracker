package com.example.mytracker.achievements

import kotlin.math.floor
import kotlin.math.sqrt

/** How many days of points the visible figure is made of. */
const val FORM_WINDOW_DAYS = 30

/** Points needed for the first level. Each one after costs more — see [levelFor]. */
const val LEVEL_BASE_POINTS = 120.0

/**
 * One attribute's standing: what it is now, and the best it has ever been.
 *
 * [record] is what makes the decay bearable. The visible figure follows the last
 * [FORM_WINDOW_DAYS] and therefore softens during a pause — which is the honest thing for a mirror
 * to do — but the best form is drawn behind it as a silhouette and never goes away. Nothing is ever
 * lost, only the current shape changes.
 */
data class AttributeLevel(val attribute: AvatarAttribute, val level: Int, val record: Int, val fraction: Float)

/**
 * The level a points total is worth. Level *n* needs `n·(n+1)/2 · base` points, so each level costs
 * one base more than the one before it: there is always a next level, and it is always harder than
 * the last. Solved rather than looped so it stays a plain function of the total.
 */
fun levelFor(points: Double): Int {
    if (points < LEVEL_BASE_POINTS) return 0
    val units = points / LEVEL_BASE_POINTS
    // n(n+1)/2 <= units  ⇒  n <= (-1 + sqrt(1 + 8·units)) / 2
    return floor((-1.0 + sqrt(1.0 + 8.0 * units)) / 2.0).toInt()
}

/** The points that level [level] starts at — what the bar to the next level is measured from. */
fun pointsForLevel(level: Int): Double = level * (level + 1) / 2.0 * LEVEL_BASE_POINTS

/** How far from the current level to the next one, for a bar. */
fun levelFraction(points: Double): Float {
    val level = levelFor(points)
    val floor = pointsForLevel(level)
    val ceiling = pointsForLevel(level + 1)
    return ((points - floor) / (ceiling - floor)).toFloat().coerceIn(0f, 1f)
}

/**
 * Every attribute's current and best level, from the whole booked ledger.
 *
 * The record is the highest the *same* rolling window ever stood at, not the sum of all points ever
 * earned. Anything else would let a long history masquerade as good form: the mark has to have been
 * reached in thirty days at some point, exactly as the current one is.
 *
 * [pointsByDay] need not be dense — days with nothing earned may simply be missing.
 */
fun attributeLevels(
    pointsByDay: Map<AvatarAttribute, Map<Long, Double>>,
    today: Long,
    firstBookedDay: Long?,
): List<AttributeLevel> = AvatarAttribute.entries.map { attribute ->
    val byDay = pointsByDay[attribute].orEmpty()
    // The window slid across the whole history in one pass: each day joins the sum, and the day that
    // has just dropped out of range leaves it. Recomputing every window from scratch would be
    // quadratic in the length of the history for no gain.
    var running = 0.0
    var record = 0.0
    for (day in (firstBookedDay ?: today)..today) {
        running += byDay[day] ?: 0.0
        running -= byDay[day - FORM_WINDOW_DAYS] ?: 0.0
        if (running > record) record = running
    }
    // Where the walk ends is the window ending today — the form as it stands right now.
    val current = running.coerceAtLeast(0.0)
    AttributeLevel(
        attribute = attribute,
        level = levelFor(current),
        record = levelFor(record),
        fraction = levelFraction(current),
    )
}
