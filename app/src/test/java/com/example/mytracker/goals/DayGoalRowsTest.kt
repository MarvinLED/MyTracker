package com.example.mytracker.goals

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalMetric
import com.example.mytracker.fitness.FitnessGoalProgress
import com.example.mytracker.fluid.FluidType
import com.example.mytracker.habit.Habit
import com.example.mytracker.habit.HabitGoal
import com.example.mytracker.habit.HabitType
import com.example.mytracker.task.Task
import com.example.mytracker.task.TaskCompletion
import com.example.mytracker.task.TaskRecurrence
import com.example.mytracker.task.taskStatuses
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val AnInstant: Instant = Instant.ofEpochMilli(1_700_000_000_000)

class DayGoalRowsTest {
    @Test
    fun nutrientRows_coverOnlyTheNutrientsWithAGoal() {
        val rows = nutrientGoalRows(
            goals = mapOf(
                Nutrient.PROTEIN to NutrientGoal(min = 100.0),
                Nutrient.FAT to NutrientGoal(),
            ),
            consumed = mapOf(Nutrient.PROTEIN to 60.0, Nutrient.CARBS to 200.0),
        )

        assertEquals(1, rows.size)
        assertEquals("Protein", rows.single().label)
        assertEquals("60 / ≥100 g", rows.single().valueText)
    }

    @Test
    fun aLowerBoundTurnsMetOnlyOnceItIsReached() {
        val goals = mapOf(Nutrient.PROTEIN to NutrientGoal(min = 100.0))

        val short = nutrientGoalRows(goals, mapOf(Nutrient.PROTEIN to 60.0)).single()
        assertFalse(short.isMet)
        assertEquals(0.6f, short.fraction!!, 0.0001f)

        val reached = nutrientGoalRows(goals, mapOf(Nutrient.PROTEIN to 100.0)).single()
        assertTrue(reached.isMet)
    }

    @Test
    fun anUpperBoundIsMetUntilItIsBlown() {
        val goals = mapOf(Nutrient.SUGAR to NutrientGoal(max = 50.0))

        assertTrue(nutrientGoalRows(goals, mapOf(Nutrient.SUGAR to 0.0)).single().isMet)
        assertTrue(nutrientGoalRows(goals, mapOf(Nutrient.SUGAR to 50.0)).single().isMet)
        assertFalse(nutrientGoalRows(goals, mapOf(Nutrient.SUGAR to 51.0)).single().isMet)
    }

    @Test
    fun fluidRows_areTheDailyGoalPlusTheDrinksWithOneOfTheirOwn() {
        val wasser = fluidType(id = "type-1", name = "Wasser", min = 1500.0, max = null)
        val limo = fluidType(id = "type-2", name = "Limonade", min = null, max = 200.0)
        val kaffee = fluidType(id = "type-3", name = "Kaffee", min = null, max = null)

        val rows = fluidGoalRows(
            dailyGoalMl = 2000.0,
            totalMl = 2100.0,
            types = listOf(wasser, limo, kaffee),
            totalsByTypeId = mapOf("type-1" to 1600.0, "type-2" to 330.0),
        )

        assertEquals(listOf("Flüssigkeit gesamt", "Wasser", "Limonade"), rows.map { it.label })
        assertTrue(rows[0].isMet)
        assertTrue(rows[1].isMet)
        assertFalse(rows[2].isMet)
        assertEquals("330 / 200 ml", rows[2].valueText)
    }

    @Test
    fun fluidRows_dropTheOverallGoalWhenThereIsNone() {
        val rows = fluidGoalRows(dailyGoalMl = 0.0, totalMl = 500.0, types = emptyList(), totalsByTypeId = emptyMap())
        assertEquals(emptyList<DayGoalRow>(), rows)
    }

    @Test
    fun aYesNoHabitGetsNoBarAtAll() {
        val habit = habit(id = "habit-1", name = "Meditieren", type = HabitType.YES_NO)
        val goals = mapOf(habit.id to habitGoal(habit.id, 1.0))

        val open = habitGoalRows(listOf(habit), goals, emptySet(), emptyMap()).single()
        assertNull(open.fraction)
        assertFalse(open.isMet)
        assertEquals("offen", open.valueText)

        val done = habitGoalRows(listOf(habit), goals, setOf(habit.id), emptyMap()).single()
        assertTrue(done.isMet)
        assertEquals("erledigt", done.valueText)
    }

    @Test
    fun aCountingHabitIsMeasuredAgainstItsDailyTarget() {
        val habit = habit(id = "habit-2", name = "Lesen", type = HabitType.DURATION)
        val rows = habitGoalRows(
            habits = listOf(habit),
            dailyGoalsByHabitId = mapOf(habit.id to habitGoal(habit.id, 45.0)),
            checkedInHabitIds = emptySet(),
            valuesByHabitId = mapOf(habit.id to 30.0),
        )

        assertEquals("30 / ≥45 min", rows.single().valueText)
        assertEquals(30f / 45f, rows.single().fraction!!, 0.0001f)
        assertFalse(rows.single().isMet)
    }

    @Test
    fun habitsWithoutADailyGoalAreNotTodaysBusiness() {
        val habit = habit(id = "habit-3", name = "Joggen", type = HabitType.YES_NO)
        assertEquals(emptyList<DayGoalRow>(), habitGoalRows(listOf(habit), emptyMap(), emptySet(), emptyMap()))
    }

    @Test
    fun fitnessRows_nameWhatTheGoalIsScopedTo() {
        val goal = FitnessGoal(
            id = "goal-1",
            metric = FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP,
            period = GoalPeriod.DAILY,
            muscleGroupId = "mg-1",
            targetValue = 6.0,
            createdAt = AnInstant,
        )

        val row = fitnessGoalRows(
            goals = listOf(goal),
            progressByGoalId = mapOf("goal-1" to FitnessGoalProgress(value = 6.0, target = 6.0)),
            muscleGroupNames = mapOf("mg-1" to "Rücken"),
            today = 20_000L,
        ).single()

        assertEquals("Kraft-Sätze · Rücken · Täglich", row.label)
        assertEquals("6 / 6", row.valueText)
        assertTrue(row.isMet)
    }

    @Test
    fun fitnessRows_showAWeeklyGoalWithTheDaysLeftToDoSomethingAboutIt() {
        val goal = FitnessGoal(
            id = "goal-2",
            metric = FitnessGoalMetric.STRENGTH_SETS_TOTAL,
            period = GoalPeriod.WEEKLY,
            targetValue = 40.0,
            createdAt = AnInstant,
        )
        // 2026-08-19 is a Wednesday, so Sunday is four days off.
        val wednesday = LocalDate.parse("2026-08-19").toEpochDay()

        val row = fitnessGoalRows(
            goals = listOf(goal),
            progressByGoalId = mapOf("goal-2" to FitnessGoalProgress(value = 24.0, target = 40.0)),
            muscleGroupNames = emptyMap(),
            today = wednesday,
        ).single()

        // A weekly goal without its deadline is one nobody can act on today.
        assertEquals("Kraft-Sätze gesamt · Wöchentlich", row.label)
        assertEquals("24 / 40 · noch 4 Tage", row.valueText)
        assertFalse(row.isMet)
    }

    @Test
    fun fitnessRows_sayWhenAnIncreaseGoalIsOnlyPaused() {
        val goal = FitnessGoal(
            id = "goal-3",
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.WEEKLY,
            exerciseId = "bench",
            targetValue = 300.0,
            createdAt = AnInstant,
        )

        val row = fitnessGoalRows(
            goals = listOf(goal),
            progressByGoalId = mapOf(
                "goal-3" to FitnessGoalProgress(
                    value = 0.0,
                    target = 300.0,
                    isPaused = true,
                    hasReference = false,
                ),
            ),
            muscleGroupNames = emptyMap(),
            exerciseNames = mapOf("bench" to "Bankdrücken"),
            today = LocalDate.parse("2026-08-19").toEpochDay(),
        ).single()

        // A deload week is not a failure, and "0 von 300 kg" would be a red bar for having rested.
        assertEquals("Steigerung Gesamtvolumen · Bankdrücken · Wöchentlich", row.label)
        assertTrue(row.valueText.startsWith("Pausiert"))
        assertFalse(row.isMet)
        assertEquals(0f, row.fraction)
    }

    @Test
    fun uiState_countsWhatIsMetAcrossAllSections() {
        val state = DayGoalsUiState(
            sections = listOf(
                DayGoalSection("Ernährung", nutrientGoalRows(
                    mapOf(Nutrient.PROTEIN to NutrientGoal(min = 100.0), Nutrient.SUGAR to NutrientGoal(max = 50.0)),
                    mapOf(Nutrient.PROTEIN to 120.0, Nutrient.SUGAR to 80.0),
                )),
            ),
        )

        assertEquals(2, state.total)
        assertEquals(1, state.metCount)
        assertFalse(state.isEmpty)
    }

    private fun fluidType(id: String, name: String, min: Double?, max: Double?) = FluidType(
        id = id,
        name = name,
        defaultQuickAddMl = 250.0,
        sortOrder = 0,
        createdAt = AnInstant,
        dailyGoalMinMl = min,
        dailyGoalMaxMl = max,
    )

    @Test
    fun taskRows_showWhatIsOwedAndHowLateItIs() {
        val today = LocalDate.parse("2026-08-05").toEpochDay()
        val tasks = listOf(
            task("t-1", "Müll rausbringen", start = "2026-08-05"),
            task("t-2", "Rechnung zahlen", start = "2026-08-02"),
        )

        val rows = taskRows(taskStatuses(tasks, completions = emptyList(), today = today))

        // Oldest debt first, so the row that has been waiting longest is the one read first.
        assertEquals(listOf("Rechnung zahlen", "Müll rausbringen"), rows.map { it.label })
        assertEquals("seit 3 Tagen fällig", rows[0].valueText)
        assertEquals("heute fällig", rows[1].valueText)
        // Not a matter of degree: a Haken, never a bar.
        assertNull(rows[0].fraction)
        assertFalse(rows[0].isMet)
    }

    @Test
    fun taskRows_keepATaskTickedOffTodayInsteadOfDroppingIt() {
        val today = LocalDate.parse("2026-08-05").toEpochDay()
        val tasks = listOf(task("t-1", "Müll rausbringen", start = "2026-08-05"))
        val completions = listOf(
            TaskCompletion(
                id = "t-1-${today}",
                taskId = "t-1",
                dueEpochDay = today,
                completedEpochDay = today,
                createdAt = AnInstant,
            ),
        )

        val row = taskRows(taskStatuses(tasks, completions, today)).single()

        // The row stays so the day's count does not drop by one the moment something gets done.
        assertTrue(row.isMet)
        assertEquals("erledigt", row.valueText)
    }

    @Test
    fun taskRows_leaveOutWhatIsNotDueYet() {
        val today = LocalDate.parse("2026-08-05").toEpochDay()
        val tasks = listOf(task("t-1", "Zahnarzt", start = "2026-09-01"))

        assertTrue(taskRows(taskStatuses(tasks, completions = emptyList(), today = today)).isEmpty())
    }

    private fun task(id: String, name: String, start: String) = Task(
        id = id,
        name = name,
        recurrence = TaskRecurrence.ONCE,
        startEpochDay = LocalDate.parse(start).toEpochDay(),
        createdAt = AnInstant,
        updatedAt = AnInstant,
    )

    private fun habit(id: String, name: String, type: HabitType) = Habit(
        id = id,
        name = name,
        createdAt = AnInstant,
        updatedAt = AnInstant,
        type = type,
    )

    private fun habitGoal(habitId: String, target: Double) = HabitGoal(
        id = "$habitId-DAILY",
        habitId = habitId,
        period = GoalPeriod.DAILY,
        targetValue = target,
        createdAt = AnInstant,
    )
}
