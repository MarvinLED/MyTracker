package com.example.mytracker.fitness.strength

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How many sessions before the selected day the card knows about at all: [PINNED_SESSION_ROWS] minus
 * the current one are shown outright, the rest wait behind "Frühere Einheiten".
 */
const val MAX_EARLIER_SESSIONS = 6

/** Rows shown without unfolding: this session, the last one, and the one before that. */
const val PINNED_SESSION_ROWS = 3

/**
 * How one session's heaviest set compares to the session before it.
 *
 * [MATCHED] is deliberately its own state rather than being folded into [DECLINED]: repeating last
 * week's top set is not a step back, and colouring it as one would call a held plateau a failure.
 * [UNKNOWN] covers both ends — nothing logged, a bodyweight-only day with no external weight, or no
 * earlier session to measure against.
 */
enum class MaxWeightTrend { IMPROVED, MATCHED, DECLINED, UNKNOWN }

/**
 * One line of the session list: what was done, how heavy it got, and whether that beat the time
 * before. [dateText] is carried but not shown until the row is tapped — the relative [label] is what
 * anyone actually reads ("vor 3 Tagen"), and the calendar date is the detail behind it.
 */
data class SessionRow(
    val epochDay: Long,
    /** "Dieses", "gestern", "vor 3 Tagen" — always relative to the selected day, not to today. */
    val label: String,
    val dateText: String,
    /** Null when nothing is logged for that day — only possible for the selected day's own row. */
    val setSummary: String?,
    val maxWeightText: String?,
    val volumeText: String?,
    val trend: MaxWeightTrend,
) {
    val hasSession: Boolean get() = setSummary != null
}

private val rowDateFormatter = DateTimeFormatter.ofPattern("EEE, d. MMMM yyyy", Locale.GERMAN)

/**
 * The session list of the comparison card, newest first, starting with the selected day.
 *
 * [older] must be the sessions strictly before the selected day, newest first, and should hold one
 * more than [limit]: the oldest row still needs something to be judged against, and without that
 * extra element it would report [MaxWeightTrend.UNKNOWN] merely for being last in the list.
 */
fun sessionRows(
    selectedEpochDay: Long,
    current: SessionStats?,
    older: List<SessionStats>,
    limit: Int = MAX_EARLIER_SESSIONS,
): List<SessionRow> {
    val rows = mutableListOf(
        rowOf(
            epochDay = selectedEpochDay,
            label = "Dieses",
            session = current,
            reference = older.firstOrNull(),
        ),
    )
    older.take(limit).forEachIndexed { index, session ->
        rows += rowOf(
            epochDay = session.epochDay,
            label = DateUtils.formatDaysSince(
                DateUtils.daysBetweenEpochDays(session.epochDay, selectedEpochDay),
            ),
            session = session,
            reference = older.getOrNull(index + 1),
        )
    }
    return rows
}

private fun rowOf(
    epochDay: Long,
    label: String,
    session: SessionStats?,
    reference: SessionStats?,
): SessionRow = SessionRow(
    epochDay = epochDay,
    label = label,
    dateText = DateUtils.localDateOfEpochDay(epochDay).format(rowDateFormatter),
    setSummary = session?.let { formatSetSummary(it.sets) },
    maxWeightText = session?.maxWeightKg?.let { weightLabel(it) },
    volumeText = session?.let { "${it.volumeKg.formatCompact()} kg" },
    trend = trendOf(session, reference),
)

private fun trendOf(session: SessionStats?, reference: SessionStats?): MaxWeightTrend {
    // A bodyweight-only day has no external weight to compare, at either end of the comparison.
    val current = session?.maxWeightKg ?: return MaxWeightTrend.UNKNOWN
    val previous = reference?.maxWeightKg ?: return MaxWeightTrend.UNKNOWN
    return when {
        current > previous -> MaxWeightTrend.IMPROVED
        current < previous -> MaxWeightTrend.DECLINED
        else -> MaxWeightTrend.MATCHED
    }
}
