package com.example.prokject2_tracker.fitness.strength

import org.junit.Assert.assertEquals
import org.junit.Test

private fun session(volumeKg: Double, epochDay: Long = 20_000L) = SessionStats(
    epochDay = epochDay,
    sets = listOf(SetDraft(reps = 10, weightKg = volumeKg / 10)),
    maxWeightKg = volumeKg / 10,
    volumeKg = volumeKg,
)

/**
 * The rule behind the banner: a session counts as successful only when its total volume is *above*
 * the previous session's. The wording is pinned too — it is the only place the verdict is stated,
 * so a colour change alone must never be what tells the two apart.
 */
class VolumeTargetTest {
    @Test
    fun moreVolumeThanLastTimeIsASuccess() {
        val target = volumeTarget(current = session(1240.0), previous = session(1180.0))

        assertEquals(VolumeTargetStatus.REACHED, target.status)
        assertEquals("Geschafft: +60 kg Volumen", target.headline)
        assertEquals("1240 kg statt 1180 kg", target.detail)
    }

    @Test
    fun lessVolumeNamesWhatIsStillMissing() {
        val target = volumeTarget(current = session(1120.0), previous = session(1180.0))

        assertEquals(VolumeTargetStatus.MISSED, target.status)
        assertEquals("Noch 60 kg bis zum letzten Training", target.headline)
    }

    @Test
    fun equalVolumeIsNotASuccess() {
        // Gleichstand heißt: dasselbe Training, kein besseres.
        val target = volumeTarget(current = session(1180.0), previous = session(1180.0))

        assertEquals(VolumeTargetStatus.MISSED, target.status)
        assertEquals("Gleichstand — noch kein Plus", target.headline)
    }

    @Test
    fun withoutSetsTheTargetIsStatedRatherThanJudged() {
        val target = volumeTarget(current = null, previous = session(1180.0))

        assertEquals(VolumeTargetStatus.OPEN, target.status)
        assertEquals("Ziel: mehr als 1180 kg", target.headline)
    }

    @Test
    fun theFirstEverSessionHasNothingToBeat() {
        assertEquals(
            VolumeTargetStatus.NO_REFERENCE,
            volumeTarget(current = session(1000.0), previous = null).status,
        )
        assertEquals(
            VolumeTargetStatus.NO_REFERENCE,
            volumeTarget(current = null, previous = null).status,
        )
    }
}
