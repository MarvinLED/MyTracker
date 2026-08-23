package com.example.mytracker.goals

import com.example.mytracker.fitness.FitnessGoalChange
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Zieländerungs-Historie. The log stores *states*; what anyone reads is the step between two of
 * them, which is the whole job of this file — and the reason a row has to know its own predecessor
 * rather than the one that happens to sit above it on screen.
 */
class GoalChangeRowsTest {
    private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

    private fun change(
        id: String,
        goalKey: String = "goal-1",
        label: String = "Bankdrücken · Steigerung Gesamtvolumen · Wöchentlich",
        day: String,
        target: Double?,
        isPercent: Boolean = false,
        targetEpochDay: Long? = null,
        changedAtSeconds: Long = 0,
    ) = FitnessGoalChange(
        id = id,
        goalKey = goalKey,
        label = label,
        effectiveFromEpochDay = day(day),
        targetValue = target,
        isPercent = isPercent,
        targetEpochDay = targetEpochDay,
        changedAt = Instant.EPOCH.plusSeconds(changedAtSeconds),
    )

    @Test
    fun theFirstEntryOfAGoalIsSetRatherThanAStepFromNothing() {
        val rows = goalChangeRows(listOf(change("c1", day = "2026-05-03", target = 300.0)))

        assertEquals("300 gesetzt", rows.single().changeText)
        assertEquals("3. Mai 2026", rows.single().dateText)
    }

    @Test
    fun aLaterEntryReadsAsTheStepFromTheOneBefore() {
        val rows = goalChangeRows(
            listOf(
                change("c1", day = "2026-05-03", target = 300.0, changedAtSeconds = 1),
                change("c2", day = "2026-06-01", target = 400.0, changedAtSeconds = 2),
            ),
        )

        // Newest first, and the newest is the one that says where it came from.
        assertEquals(listOf("300 → 400", "300 gesetzt"), rows.map { it.changeText })
    }

    @Test
    fun aClearedGoalIsAStepToNoGoal() {
        val rows = goalChangeRows(
            listOf(
                change("c1", day = "2026-05-03", target = 300.0, changedAtSeconds = 1),
                change("c2", day = "2026-06-01", target = null, changedAtSeconds = 2),
            ),
        )

        // Worth keeping: it is what explains a run of unmet weeks ending.
        assertEquals("300 → kein Ziel", rows.first().changeText)
    }

    @Test
    fun eachGoalFindsItsOwnPredecessorRatherThanTheRowAboveIt() {
        val rows = goalChangeRows(
            listOf(
                change("a1", goalKey = "goal-a", label = "A", day = "2026-05-03", target = 300.0, changedAtSeconds = 1),
                change("b1", goalKey = "goal-b", label = "B", day = "2026-05-04", target = 20.0, changedAtSeconds = 2),
                change("a2", goalKey = "goal-a", label = "A", day = "2026-05-05", target = 400.0, changedAtSeconds = 3),
            ),
        )

        // Two goals interleaved in time: "300 → 400" must not become "20 → 400".
        val newest = rows.first()
        assertEquals("A", newest.label)
        assertEquals("300 → 400", newest.changeText)
        assertEquals("20 gesetzt", rows.single { it.id == "b1" }.changeText)
    }

    @Test
    fun percentAndTargetDatesTravelWithTheEntry() {
        val rows = goalChangeRows(
            listOf(
                change("c1", day = "2026-05-03", target = 5.0, isPercent = true, changedAtSeconds = 1),
                change(
                    "c2",
                    goalKey = "maxweight-bench",
                    label = "Bankdrücken · Langfristiges Maximalgewicht",
                    day = "2026-05-04",
                    target = 100.0,
                    targetEpochDay = day("2026-12-31"),
                    changedAtSeconds = 2,
                ),
            ),
        )

        assertEquals("5 % gesetzt", rows.single { it.id == "c1" }.changeText)
        // A long-term goal's date can move without its target changing, so the date rides along.
        // The month abbreviation comes from the JDK's German locale data, so only the shape of the
        // suffix is asserted — the point is that the date rides along at all.
        assertTrue(rows.single { it.id == "c2" }.changeText.startsWith("100 gesetzt (bis 31. Dez"))
    }

    @Test
    fun theHistoryIsCappedSoItStaysAHistoryAndNotALogbook() {
        val many = (1..GOAL_CHANGE_HISTORY_LIMIT + 10).map { index ->
            change("c$index", day = "2026-05-03", target = index.toDouble(), changedAtSeconds = index.toLong())
        }

        assertEquals(GOAL_CHANGE_HISTORY_LIMIT, goalChangeRows(many).size)
        // And what survives the cap is the newest end of it.
        assertEquals("c${GOAL_CHANGE_HISTORY_LIMIT + 10}", goalChangeRows(many).first().id)
    }
}
