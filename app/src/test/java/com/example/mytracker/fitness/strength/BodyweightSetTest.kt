package com.example.mytracker.fitness.strength

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun bodyweight(reps: Int, addedKg: Double? = null) =
    SetDraft(reps = reps, weightKg = addedKg, isBodyweight = true)

/**
 * Klimmzüge: the body is the load, and a belt may add to it. What the three cases a set can be —
 * plain weight, bodyweight, bodyweight plus added — read as, and what they count towards.
 */
class BodyweightSetTest {
    @Test
    fun aPlainBodyweightSetStillReadsAsKG() {
        assertEquals("KG", weightLabel(null, isBodyweight = true))
        // Sets logged before the flag existed carry no flag but also no weight.
        assertEquals("KG", weightLabel(null))
    }

    @Test
    fun addedWeightIsSpelledOutNextToTheKG() {
        assertEquals("KG +10 kg", weightLabel(10.0, isBodyweight = true))
        assertEquals("KG +2,5 kg", weightLabel(2.5, isBodyweight = true))
    }

    @Test
    fun anExternalWeightIsUnchangedByTheFlag() {
        assertEquals("60 kg", weightLabel(60.0))
        assertEquals("62,25 kg", weightLabel(62.25))
    }

    @Test
    fun theSummaryKeepsWeightedAndUnweightedSetsApart() {
        val sets = listOf(bodyweight(10), bodyweight(8, 10.0), bodyweight(8, 10.0), bodyweight(6))

        assertEquals("KG × 10 · KG +10 kg × 8, 8 · KG × 6", formatSetSummary(sets))
    }

    @Test
    fun volumeCountsTheAddedWeightAndOnlyThat() {
        // The body is not on the bar, so it stays out of the volume — as it always has.
        assertEquals(0.0, volumeOf(listOf(bodyweight(10), bodyweight(8))), 0.0001)
        assertEquals(80.0, volumeOf(listOf(bodyweight(8, 10.0))), 0.0001)
    }

    @Test
    fun maxWeightIsTheHeaviestBeltNotTheHeaviestSet() {
        assertNull(maxWeightOf(listOf(bodyweight(10), bodyweight(8))))
        assertEquals(15.0, maxWeightOf(listOf(bodyweight(8, 10.0), bodyweight(5, 15.0), bodyweight(12)))!!, 0.0001)
    }

    @Test
    fun theTopSetLineNamesTheAddedWeight() {
        assertEquals("KG +15 kg × 5", formatTopSets(listOf(bodyweight(8, 10.0), bodyweight(5, 15.0))))
        // Nothing added anywhere: the rep count is the headline, as before.
        assertEquals("2× 12 Wiederholungen", formatTopSets(listOf(bodyweight(12), bodyweight(12), bodyweight(8))))
    }
}
