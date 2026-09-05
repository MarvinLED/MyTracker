package com.example.mytracker.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The milestone ladders: which rung a value stands on, and how far the next one still is. */
class AchievementTiersTest {
    private val steps = listOf(10.0, 25.0, 50.0)

    @Test
    fun belowTheFirstRungNothingIsClaimed() {
        val tier = tierFor(4.0, steps)

        // Not "Meilenstein 0 erreicht": awarding one for having done nothing would make every other
        // milestone worth nothing either.
        assertNull(tier.reached)
        assertEquals(10.0, tier.next!!, 0.0)
        assertEquals(0.4f, tier.fraction, 0.0001f)
    }

    @Test
    fun landingExactlyOnARungReachesIt() {
        val tier = tierFor(25.0, steps)

        assertEquals(25.0, tier.reached!!, 0.0)
        assertEquals(50.0, tier.next!!, 0.0)
        // Just arrived at 25, so the way to 50 has not been started.
        assertEquals(0f, tier.fraction, 0.0001f)
    }

    @Test
    fun theBarMeasuresFromTheRungBelowNotFromZero() {
        val tier = tierFor(40.0, steps)

        // 40 of the way from 25 to 50 is 60 %, not 80 % of 50 — otherwise every bar would look
        // nearly full for the whole of a long climb.
        assertEquals(0.6f, tier.fraction, 0.0001f)
    }

    @Test
    fun pastTheLastRungThereIsNoNextOne() {
        val tier = tierFor(120.0, steps)

        assertEquals(50.0, tier.reached!!, 0.0)
        assertNull(tier.next)
        assertEquals(1f, tier.fraction, 0.0001f)
    }

    @Test
    fun everyLadderKeepsWidening() {
        // The property that keeps the wall from ever being finished: each rung costs more than the
        // one before, so there is always a next one worth something.
        listOf(LoggedDayTiers, TotalVolumeTiers, SessionTiers, LoggingStreakTiers).forEach { ladder ->
            ladder.zipWithNext().forEach { (lower, higher) ->
                assertEquals(true, higher > lower)
            }
        }
    }
}
