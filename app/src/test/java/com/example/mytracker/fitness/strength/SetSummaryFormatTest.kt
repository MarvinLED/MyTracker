package com.example.prokject2_tracker.fitness.strength

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetSummaryFormatTest {
    private fun set(reps: Int, weightKg: Double?) = SetDraft(reps, weightKg)

    @Test
    fun empty_rendersEmptyString() {
        assertEquals("", formatSetSummary(emptyList()))
    }

    @Test
    fun singleSet() {
        assertEquals("60 kg × 8", formatSetSummary(listOf(set(8, 60.0))))
    }

    @Test
    fun consecutiveEqualWeights_collapseIntoOneGroup() {
        val sets = listOf(set(5, 50.0), set(5, 50.0), set(8, 60.0), set(8, 60.0), set(8, 60.0))
        assertEquals("50 kg × 5, 5 · 60 kg × 8, 8, 8", formatSetSummary(sets))
    }

    @Test
    fun nonAdjacentEqualWeights_stayThreeGroups() {
        // The order is the workout: dropping back to 50 kg is not the same as having done both
        // 50 kg sets together.
        val sets = listOf(set(5, 50.0), set(8, 60.0), set(5, 50.0))
        assertEquals("50 kg × 5 · 60 kg × 8 · 50 kg × 5", formatSetSummary(sets))
    }

    @Test
    fun bodyweightSets_renderAsKgAndGroupTogether() {
        assertEquals("KG × 12, 12", formatSetSummary(listOf(set(12, null), set(12, null))))
    }

    @Test
    fun bodyweightAndWeighted_areSeparateGroups() {
        assertEquals("KG × 10 · 40 kg × 8", formatSetSummary(listOf(set(10, null), set(8, 40.0))))
    }

    @Test
    fun quarterKiloWeights_keepBothDecimals() {
        // formatCompact() would round this to "62,3 kg", which is not a weight the steppers produce.
        assertEquals("62,25 kg × 8", formatSetSummary(listOf(set(8, 62.25))))
    }

    @Test
    fun wholeWeights_dropTrailingZeros() {
        assertEquals("60 kg × 8", formatSetSummary(listOf(set(8, 60.0))))
        assertEquals("60,5 kg × 8", formatSetSummary(listOf(set(8, 60.5))))
    }

    @Test
    fun topSets_empty_isNull() {
        assertNull(formatTopSets(emptyList()))
    }

    @Test
    fun topSets_leadsWithTheHeaviestWeightAndItsReps() {
        val sets = listOf(set(8, 50.0), set(5, 70.0), set(5, 70.0), set(5, 70.0))
        assertEquals("70 kg × 5, 5, 5", formatTopSets(sets))
    }

    @Test
    fun topSets_collectsNonAdjacentSetsAtTheTopWeight() {
        // Deliberately unlike formatSetSummary: dropping to 60 kg in between doesn't make the two
        // 70 kg sets two separate facts when the question is "what did I top out at".
        val sets = listOf(set(5, 70.0), set(10, 60.0), set(4, 70.0))
        assertEquals("70 kg × 5, 4", formatTopSets(sets))
    }

    @Test
    fun topSets_singleHeaviestSet() {
        assertEquals("80 kg × 3", formatTopSets(listOf(set(10, 60.0), set(3, 80.0))))
    }

    @Test
    fun topSets_bodyweightOnly_fallsBackToTheBestRepCount() {
        val sets = listOf(set(12, null), set(10, null), set(12, null), set(12, null))
        assertEquals("3× 12 Wiederholungen", formatTopSets(sets))
    }

    @Test
    fun topSets_mixedSession_stillLeadsWithTheWeight() {
        // A weighted set present anywhere means there is a heaviest weight to report.
        assertEquals("40 kg × 8", formatTopSets(listOf(set(12, null), set(8, 40.0))))
    }

    @Test
    fun topSets_quarterKiloTopWeight_keepsBothDecimals() {
        assertEquals("62,25 kg × 8, 8", formatTopSets(listOf(set(8, 62.25), set(8, 62.25), set(8, 60.0))))
    }

    @Test
    fun weightLabel_coversBothCases() {
        assertEquals("KG", weightLabel(null))
        assertEquals("0 kg", weightLabel(0.0))
        assertEquals("62,25 kg", weightLabel(62.25))
    }
}
