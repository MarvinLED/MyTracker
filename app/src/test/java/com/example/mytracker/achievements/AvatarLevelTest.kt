package com.example.mytracker.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The figure's arithmetic: how points become a level, and how a level decays without ever taking the
 * record with it. This is where the promise "nothing is ever lost, only the current shape changes"
 * either holds or does not.
 */
class AvatarLevelTest {
    private val today = 20_000L

    private fun ledger(vararg entries: Pair<Long, Double>): Map<AvatarAttribute, Map<Long, Double>> =
        mapOf(AvatarAttribute.KRAFT to entries.toMap())

    private fun kraft(levels: List<AttributeLevel>) = levels.first { it.attribute == AvatarAttribute.KRAFT }

    @Test
    fun theFirstLevelCostsOneBase() {
        assertEquals(0, levelFor(LEVEL_BASE_POINTS - 1))
        assertEquals(1, levelFor(LEVEL_BASE_POINTS))
    }

    @Test
    fun everyLevelCostsMoreThanTheOneBeforeIt() {
        // 1·base, 3·base, 6·base, 10·base … so there is always a next level and it is always harder.
        assertEquals(1, levelFor(2 * LEVEL_BASE_POINTS))
        assertEquals(2, levelFor(3 * LEVEL_BASE_POINTS))
        assertEquals(3, levelFor(6 * LEVEL_BASE_POINTS))
        assertEquals(4, levelFor(10 * LEVEL_BASE_POINTS))
    }

    @Test
    fun theLevelCurveAndItsThresholdsAgree() {
        (0..12).forEach { level ->
            assertEquals(level, levelFor(pointsForLevel(level)))
            // One point short is always the level below — the boundary is where it is claimed to be.
            if (level > 0) assertEquals(level - 1, levelFor(pointsForLevel(level) - 0.001))
        }
    }

    @Test
    fun theBarMeasuresFromTheCurrentLevelToTheNext() {
        val midway = (pointsForLevel(3) + pointsForLevel(4)) / 2.0

        assertEquals(0.5f, levelFraction(midway), 0.001f)
        assertEquals(0f, levelFraction(pointsForLevel(3)), 0.001f)
    }

    @Test
    fun onlyTheLastThirtyDaysShapeTheFigure() {
        val levels = attributeLevels(
            // Plenty of points, but all of them older than the window.
            ledger((today - 200) to 5000.0),
            today = today,
            firstBookedDay = today - 200,
        )

        assertEquals(0, kraft(levels).level)
    }

    @Test
    fun theRecordSurvivesThePauseThatEndedIt() {
        val strongMonth = (0..29).associate { (today - 200 + it) to 40.0 }
        val levels = attributeLevels(
            mapOf(AvatarAttribute.KRAFT to strongMonth),
            today = today,
            firstBookedDay = today - 200,
        )

        val kraft = kraft(levels)
        // The form is gone — 1200 points earned half a year ago say nothing about now.
        assertEquals(0, kraft.level)
        // The mark is not. That is what makes the decay bearable.
        assertEquals(levelFor(1200.0), kraft.record)
        assertTrue(kraft.record > 0)
    }

    @Test
    fun aRunningStreakIsItsOwnRecord() {
        val lastMonth = (0..29).associate { (today - 29 + it) to 40.0 }
        val levels = attributeLevels(
            mapOf(AvatarAttribute.KRAFT to lastMonth),
            today = today,
            firstBookedDay = today - 29,
        )

        val kraft = kraft(levels)
        assertEquals(levelFor(1200.0), kraft.level)
        // Never behind the current form: the best includes the window ending today.
        assertEquals(kraft.level, kraft.record)
    }

    @Test
    fun theRecordIsAWindowThatWasActuallyHeldNotEverythingEverEarned() {
        // A point a day for a year adds up to a lot, but it was never once a strong month.
        val trickle = (0..364).associate { (today - 364 + it) to 4.0 }
        val levels = attributeLevels(
            mapOf(AvatarAttribute.KRAFT to trickle),
            today = today,
            firstBookedDay = today - 364,
        )

        // 30 days × 4 points = 120, exactly one level — not the 1460 points of the whole year.
        assertEquals(levelFor(120.0), kraft(levels).record)
    }

    @Test
    fun anEmptyLedgerLeavesEveryAttributeAtZero() {
        val levels = attributeLevels(emptyMap(), today = today, firstBookedDay = null)

        assertEquals(AvatarAttribute.entries.size, levels.size)
        assertTrue(levels.all { it.level == 0 && it.record == 0 })
    }
}
