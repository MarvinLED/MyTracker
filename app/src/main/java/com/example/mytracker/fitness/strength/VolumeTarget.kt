package com.example.mytracker.fitness.strength

import com.example.mytracker.core.util.formatCompact

/**
 * Whether this session beat the one before it. The bar is the previous session's total volume, and
 * it has to be *cleared*, not tied — equal volume is the same workout, not a better one.
 */
enum class VolumeTargetStatus {
    /** Volume is above the previous session: the training counts as successful. */
    REACHED,

    /** Sets are logged, but the total is still at or below the previous session. */
    MISSED,

    /** Nothing logged on this day yet — the target is stated, not judged. */
    OPEN,

    /** No earlier session of this exercise, so there is nothing to beat. */
    NO_REFERENCE,
}

/**
 * The banner above the session comparison: one line that answers "war das gut?" without reading the
 * table, plus the numbers behind it. [status] drives the colour and icon, but every state also says
 * in words where it stands — colour alone never carries the verdict.
 */
data class VolumeTarget(
    val status: VolumeTargetStatus,
    val headline: String,
    val detail: String,
)

/** Kilogram totals read as whole numbers here; a set is never logged to the gram. */
private fun kg(value: Double): String = "${value.formatCompact()} kg"

fun volumeTarget(current: SessionStats?, previous: SessionStats?): VolumeTarget {
    if (previous == null) {
        return VolumeTarget(
            status = VolumeTargetStatus.NO_REFERENCE,
            headline = "Kein Vergleich möglich",
            detail = current?.let { "Erstes Training: ${kg(it.volumeKg)} Volumen" }
                ?: "Noch kein früheres Training dieser Übung",
        )
    }
    val target = previous.volumeKg
    if (current == null) {
        return VolumeTarget(
            status = VolumeTargetStatus.OPEN,
            headline = "Ziel: mehr als ${kg(target)}",
            detail = "Volumen des letzten Trainings",
        )
    }
    val delta = current.volumeKg - target
    return if (delta > 0.0) {
        VolumeTarget(
            status = VolumeTargetStatus.REACHED,
            headline = "Geschafft: +${kg(delta)} Volumen",
            detail = "${kg(current.volumeKg)} statt ${kg(target)}",
        )
    } else {
        VolumeTarget(
            status = VolumeTargetStatus.MISSED,
            // A tie is not a success, but "noch 0 kg" would read like one — it gets its own wording.
            headline = if (delta == 0.0) {
                "Gleichstand — noch kein Plus"
            } else {
                "Noch ${kg(-delta)} bis zum letzten Training"
            },
            detail = "${kg(current.volumeKg)} von mehr als ${kg(target)}",
        )
    }
}
