package com.example.mytracker.achievements

/**
 * Where a value stands on a milestone ladder: the rung below it, and the one it is climbing towards.
 *
 * Both rungs are nullable on purpose. [reached] is null before the first one — nothing has been
 * earned yet, and awarding a milestone for zero would make the whole wall meaningless. [next] is
 * null once the last rung is behind you, the one case where a ladder really is finished.
 */
data class Tier(val value: Double, val reached: Double?, val next: Double?) {
    /** How far from the rung below to the next one, for a bar. Full once there is no next one. */
    val fraction: Float
        get() {
            val target = next ?: return 1f
            val floor = reached ?: 0.0
            if (target <= floor) return 1f
            return ((value - floor) / (target - floor)).toFloat().coerceIn(0f, 1f)
        }
}

/**
 * Which rung [value] stands on. [steps] must be ascending.
 *
 * The ladders are deliberately open-ended and widening (100, 250, 500, 1000, 2500 …): a fixed set of
 * badges is finished after a few months and turns into dead content, while a rung that always costs
 * more than the one before keeps the next one both visible and worth something.
 */
fun tierFor(value: Double, steps: List<Double>): Tier = Tier(
    value = value,
    reached = steps.lastOrNull { it <= value },
    next = steps.firstOrNull { it > value },
)

/** Days on which anything at all was logged. The one ladder every area of the app feeds. */
val LoggedDayTiers = listOf(7.0, 30.0, 100.0, 250.0, 500.0, 1000.0, 2500.0)

/** Kilograms moved, all exercises and all time. Tonnes read better, so the screen divides by 1000. */
val TotalVolumeTiers = listOf(
    10_000.0, 25_000.0, 50_000.0, 100_000.0, 250_000.0, 500_000.0, 1_000_000.0, 2_500_000.0,
)

/** Logged strength sessions and cardio sessions together. */
val SessionTiers = listOf(10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)

/** Days in a row with something logged — the ladder the Erfassungsserie climbs. */
val LoggingStreakTiers = listOf(7.0, 14.0, 30.0, 60.0, 100.0, 200.0, 365.0)
