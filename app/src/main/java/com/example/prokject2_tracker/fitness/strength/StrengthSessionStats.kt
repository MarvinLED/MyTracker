package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.metrics.MetricPoint

/**
 * One exercise's work on one day. A *session* is the pair (exercise, day), not a single
 * [StrengthLogEntry] row: nothing in the schema stops a day from holding two entries for the same
 * exercise, and keying on one of them would show half the sets and half the volume as if that were
 * the whole day.
 */
data class SessionStats(
    val epochDay: Long,
    val sets: List<SetDraft>,
    /** Heaviest external weight lifted, or null when the day was bodyweight-only. */
    val maxWeightKg: Double?,
    val volumeKg: Double,
) {
    val setCount: Int get() = sets.size
}

/**
 * Volume counts a bodyweight set as 0 — consistent with `observeDailyVolumeTotals`' COALESCE and
 * with what the fitness goals already measure.
 */
fun volumeOf(sets: List<SetDraft>): Double = sets.sumOf { it.reps * (it.weightKg ?: 0.0) }

/**
 * Bodyweight sets are ignored entirely rather than counted as 0. Treating them as 0 would drop the
 * max-weight chart line to the floor on a bodyweight day and flatten the scale for every other week.
 */
fun maxWeightOf(sets: List<SetDraft>): Double? = sets.mapNotNull { it.weightKg }.maxOrNull()

/**
 * The sets logged for this exercise on [epochDay], in logging order, or null when there are none.
 * Null rather than an empty [SessionStats] so callers can't accidentally render "0 kg" for a day
 * that simply wasn't trained.
 */
fun List<StrengthSet>.sessionOn(epochDay: Long): SessionStats? {
    val drafts = filter { it.epochDay == epochDay }.map { it.toDraft() }
    if (drafts.isEmpty()) return null
    return SessionStats(
        epochDay = epochDay,
        sets = drafts,
        maxWeightKg = maxWeightOf(drafts),
        volumeKg = volumeOf(drafts),
    )
}

/**
 * The most recent day strictly before [day] that actually has sets. Based on sets rather than log
 * entries, so an entry left behind without sets can never be presented as a workout.
 */
fun List<StrengthSet>.previousSessionDay(before: Long): Long? =
    filter { it.epochDay < before }.maxOfOrNull { it.epochDay }

/** The most recent sessions, newest first — the "Frühere Einheiten" list. */
fun List<StrengthSet>.recentSessions(limit: Int): List<SessionStats> =
    map { it.epochDay }.distinct().sortedDescending().take(limit).mapNotNull { sessionOn(it) }

/** Daily totals, for feeding `bucketBy(WEEKLY, SUM)`. */
fun List<StrengthSet>.dailyVolumePoints(): List<MetricPoint> =
    groupByDay { sets -> volumeOf(sets) }

/**
 * Daily heaviest weight, for `bucketBy(WEEKLY, MAX)`. Bodyweight-only days emit no point at all
 * (see [maxWeightOf]) — the line skips them rather than dipping to zero.
 */
fun List<StrengthSet>.dailyMaxWeightPoints(): List<MetricPoint> =
    groupBy { it.epochDay }
        .mapNotNull { (day, sets) -> maxWeightOf(sets.map { it.toDraft() })?.let { MetricPoint(day, it) } }
        .sortedBy { it.epochDay }

private fun List<StrengthSet>.groupByDay(valueOf: (List<SetDraft>) -> Double): List<MetricPoint> =
    groupBy { it.epochDay }
        .map { (day, sets) -> MetricPoint(day, valueOf(sets.map { it.toDraft() })) }
        .sortedBy { it.epochDay }
