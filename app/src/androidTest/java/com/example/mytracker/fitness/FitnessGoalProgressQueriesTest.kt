package com.example.mytracker.fitness

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mytracker.core.database.AppDatabase
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.strength.MuscleGroup
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fitness.strength.StrengthLogEntry
import com.example.mytracker.fitness.strength.StrengthSet
import com.example.mytracker.weight.BodyWeightEntry
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two Steigerungen against real SQL. The arithmetic is tested without a database in
 * `FitnessGoalProgressTest`; what needs a real Room here is what the queries return at the edges —
 * `MAX` over a window with no sets is null, not zero, and that difference decides whether a first
 * training session counts as a record gain of the whole bar.
 */
@RunWith(AndroidJUnit4::class)
class FitnessGoalProgressQueriesTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: FitnessGoalRepository

    /** A Wednesday, so "this week" and "the week before" are both partly in the fixture. */
    private val today = LocalDate.parse("2026-08-19").toEpochDay()
    private val thisMonday = LocalDate.parse("2026-08-17").toEpochDay()
    private val lastWednesday = LocalDate.parse("2026-08-12").toEpochDay()

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FitnessGoalRepository(
            db.fitnessGoalDao(),
            db.cardioDao(),
            db.strengthSetDao(),
            db.strengthMaxWeightGoalDao(),
            db.fitnessGoalChangeDao(),
            db.bodyWeightDao(),
        )
        db.strengthExerciseDao().upsert(
            StrengthExercise(
                id = "bench",
                name = "Bankdrücken",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    /** A session with no external weight at all — training that carries no volume. */
    private suspend fun logBodyweightSession(epochDay: Long, reps: Int, sets: Int) =
        logSession(epochDay, weightKg = null, reps = reps, sets = sets)

    /** One session of [reps] × [weightKg], [sets] sets, on [epochDay]. */
    private suspend fun logSession(epochDay: Long, weightKg: Double?, reps: Int, sets: Int) {
        val entryId = "entry-$epochDay-$weightKg"
        db.strengthLogDao().upsert(
            StrengthLogEntry(
                id = entryId,
                epochDay = epochDay,
                createdAt = Instant.EPOCH,
                exerciseId = "bench",
                exerciseName = "Bankdrücken",
            ),
        )
        db.strengthSetDao().upsertAll(
            (0 until sets).map { index ->
                StrengthSet(
                    id = "$entryId-$index",
                    logEntryId = entryId,
                    epochDay = epochDay,
                    exerciseId = "bench",
                    setIndex = index,
                    reps = reps,
                    weightKg = weightKg,
                )
            },
        )
    }

    private suspend fun goal(metric: FitnessGoalMetric, target: Double): FitnessGoal {
        repository.setGoal(
            metric = metric,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = null,
            movementDirection = null,
            targetValue = target,
            exerciseId = "bench",
        )
        return repository.observeAll().first().single { it.metric == metric }
    }

    @Test
    fun maxWeightIncrease_isMeasuredAgainstTheBestBeforeThisWeek() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 82.5, reps = 5, sets = 3)

        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(2.5, progress.value, 0.0001)
    }

    @Test
    fun maxWeightIncrease_isZeroWhileTheExerciseHasNoHistoryToBeat() = runBlocking {
        logSession(thisMonday, weightKg = 82.5, reps = 5, sets = 3)

        // Null and not 0 kg is what the query returns for "never trained before" — a first session
        // is not a gain of 82,5 kg, and reporting one would tick off any Steigerungsziel at once.
        assertNull(db.strengthSetDao().maxWeightBeforeForExercise("bench", thisMonday))
        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(0.0, progress.value, 0.0001)
        assertFalse(progress.hasReference)
    }

    @Test
    fun maxWeightIncrease_isMissedInAWeekWithoutTraining() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)

        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        // A week off gained nothing: "±0 von +5 kg" and not reached — there is something to beat,
        // it just was not beaten.
        assertEquals(0.0, progress.value, 0.0001)
        assertTrue(progress.hasReference)
        assertFalse(progress.isMet)
        assertEquals(0f, progress.fraction, 0.0001f)
    }

    @Test
    fun maxWeightIncrease_goesNegativeWhenTheTopSetDrops() = runBlocking {
        logSession(lastWednesday, weightKg = 85.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 3)

        // A lighter week is a step back, and the goal card says so rather than rounding it to zero.
        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(-5.0, progress.value, 0.0001)
    }

    @Test
    fun volumeIncrease_comparesThisWeekWithTheWholeWeekBefore() = runBlocking {
        // 3 × 5 × 80 = 1200 kg last week, 4 × 5 × 80 = 1600 kg this week.
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 4)

        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 300.0), today)

        assertEquals(400.0, progress.value, 0.0001)
        assertTrue(progress.isIncrease)
        assertTrue(progress.isMet)
    }

    @Test
    fun volumeIncrease_isNegativeWhenLessWasDoneThanTheWeekBefore() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 4)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 1)

        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 300.0), today)

        assertEquals(-1200.0, progress.value, 0.0001)
    }

    @Test
    fun longTermGoal_keepsItsStartingPointWhenTheTargetIsMovedLater() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)

        repository.setMaxWeightGoal("bench", targetWeightKg = 100.0, targetEpochDay = today + 200)
        val first = repository.observeMaxWeightGoals().first().single()
        assertEquals(80.0, first.startWeightKg, 0.0001)

        logSession(thisMonday, weightKg = 90.0, reps = 5, sets = 3)
        repository.setMaxWeightGoal("bench", targetWeightKg = 110.0, targetEpochDay = today + 400)

        // Re-anchoring to today's 90 kg would erase the ten kilos already gained towards the goal.
        val moved = repository.observeMaxWeightGoals().first().single()
        assertEquals(80.0, moved.startWeightKg, 0.0001)
        assertEquals(first.startEpochDay, moved.startEpochDay)
        assertEquals(110.0, moved.targetWeightKg, 0.0001)
        assertEquals(today + 400, moved.targetEpochDay)
    }

    @Test
    fun longTermGoal_isOnePerExerciseAndCanBeTakenOffAgain() = runBlocking {
        repository.setMaxWeightGoal("bench", targetWeightKg = 100.0, targetEpochDay = today + 200)
        repository.setMaxWeightGoal("bench", targetWeightKg = 105.0, targetEpochDay = today + 200)

        assertEquals(1, repository.observeMaxWeightGoals().first().size)

        repository.clearMaxWeightGoal("bench")
        assertEquals(0, repository.observeMaxWeightGoals().first().size)
    }

    @Test
    fun volumeIncrease_hasNoReferenceWhenTheWeekBeforeWasEmpty() = runBlocking {
        // Trained three weeks ago and again this week; the two weeks between were a break.
        logSession(LocalDate.parse("2026-07-29").toEpochDay(), weightKg = 80.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 4)

        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 300.0), today)

        // A Steigerung is measured against the week right before or against nothing: reaching past
        // the break for a week from a month ago would compare two different phases of training.
        assertFalse(progress.hasReference)
        assertEquals(0.0, progress.value, 0.0001)
        assertFalse(progress.isMet)
    }

    @Test
    fun volumeIncrease_countsABodyweightWeekAsTrainedEvenThoughItHasNoVolume() = runBlocking {
        logBodyweightSession(lastWednesday, reps = 10, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 3)

        val progress = repository.getProgress(goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 300.0), today)

        // Whether the week before counts is read from the set count, not the volume: a
        // Klimmzug-Woche is training, and calling it untrained would leave the goal without any
        // comparison at all.
        assertTrue(progress.hasReference)
        assertEquals(1200.0, progress.value, 0.0001)
    }

    @Test
    fun percentIncrease_readsTheGainAgainstTheReferencePeriod() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 4)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 5)

        repository.setGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = null,
            movementDirection = null,
            targetValue = 20.0,
            exerciseId = "bench",
            isPercent = true,
        )
        val stored = repository.observeAll().first()
            .single { it.metric == FitnessGoalMetric.STRENGTH_VOLUME_INCREASE }

        // 1600 kg against 2000 kg is a quarter more, and the goal was 20 %.
        val progress = repository.getProgress(stored, today)
        assertEquals(25.0, progress.value, 0.0001)
        assertTrue(progress.isPercent)
        assertTrue(progress.isMet)
    }

    @Test
    fun muscleGroupVolumeIncrease_addsUpAcrossTheExercisesOfThatGroup() = runBlocking {
        db.muscleGroupDao().upsert(
            MuscleGroup(id = "back", name = "Rücken", sortOrder = 0, createdAt = Instant.EPOCH),
        )
        db.strengthExerciseDao().replaceMuscleGroupsForExercise("bench", listOf("back"))
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 4)

        repository.setGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = "back",
            movementDirection = null,
            targetValue = 300.0,
        )
        val stored = repository.observeAll().first()
            .single { it.metric == FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP }

        val progress = repository.getProgress(stored, today)
        assertEquals(400.0, progress.value, 0.0001)
    }

    @Test
    fun changingAGoalIsWrittenToTheLogAndClearingItToo() = runBlocking {
        repository.setGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = null,
            movementDirection = null,
            targetValue = 300.0,
            exerciseId = "bench",
            label = "Bankdrücken · Steigerung Gesamtvolumen · Wöchentlich",
        )
        // Saving the same target again is not a change: the Ziele screen writes every row on every
        // save, and a log full of identical rows would answer nothing.
        repository.setGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = null,
            movementDirection = null,
            targetValue = 300.0,
            exerciseId = "bench",
        )
        repository.setGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = null,
            movementDirection = null,
            targetValue = 400.0,
            exerciseId = "bench",
        )
        repository.clearGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.WEEKLY,
            muscleGroupId = null,
            movementDirection = null,
            exerciseId = "bench",
        )

        val changes = repository.observeGoalChanges().first()
        assertEquals(listOf(300.0, 400.0, null), changes.sortedBy { it.changedAt }.map { it.targetValue })
        assertEquals(
            "Bankdrücken · Steigerung Gesamtvolumen · Wöchentlich",
            changes.minByOrNull { it.changedAt }?.label,
        )
    }

    @Test
    fun clearingAGoalThatWasNeverSetIsNotAChange() = runBlocking {
        repository.clearGoal(
            metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
            period = GoalPeriod.MONTHLY,
            muscleGroupId = null,
            movementDirection = null,
            exerciseId = "bench",
        )

        // The Ziele screen saves a screen full of empty fields on every save; filling the log with
        // "kein Ziel" rows for goals nobody ever set would bury the changes that happened.
        assertTrue(repository.observeGoalChanges().first().isEmpty())
    }

    @Test
    fun aRelativeLongTermGoalTravelsWithTheBodyWeight() = runBlocking {
        db.bodyWeightDao().upsert(
            BodyWeightEntry(id = "w-1", epochDay = today - 1, weightKg = 80.0, createdAt = Instant.EPOCH),
        )
        logSession(lastWednesday, weightKg = 100.0, reps = 5, sets = 3)

        repository.setMaxWeightGoal(
            exerciseId = "bench",
            targetWeightKg = 120.0,
            targetEpochDay = today + 200,
            targetBodyweightMultiple = 1.5,
        )

        val goal = repository.observeMaxWeightGoals().first().single()
        val bodyWeight = repository.observeLatestBodyWeightKg().first()
        assertEquals(80.0, bodyWeight!!, 0.0001)

        val progress = maxWeightGoalProgress(goal, currentMaxKg = 100.0, bodyWeightKg = bodyWeight, today = today)
        assertEquals(120.0, progress.targetKg, 0.0001)
        assertEquals(1.25, progress.relativeStrength!!, 0.0001)
        assertFalse(progress.isReached)
    }

    @Test
    fun streak_isBrokenByTheWeekWithoutTraining() = runBlocking {
        // Four weeks: a starting week, one with a gain over it, one week off, and the week back.
        logSession(LocalDate.parse("2026-07-22").toEpochDay(), weightKg = 80.0, reps = 5, sets = 3) // 1200 kg
        logSession(LocalDate.parse("2026-07-29").toEpochDay(), weightKg = 80.0, reps = 5, sets = 5) // 2000 kg
        // Week of 2026-08-03: nothing at all.
        logSession(LocalDate.parse("2026-08-12").toEpochDay(), weightKg = 80.0, reps = 5, sets = 7) // 2800 kg

        val streak = repository.getStreak(
            goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 500.0),
            today = today,
            periods = 4,
        )

        // Only the week of 2026-07-27 gained anything over the week before it. The empty week is a
        // missed one, the week back after it had nothing to be measured against, and the first
        // trained week had nothing to beat.
        assertEquals(1, streak.met)
        assertEquals(2, streak.considered)
        // Nothing am Stück: the most recent finished week is not a met one, so there is no run.
        assertEquals(0, streak.currentRun)
    }

    @Test
    fun streak_countsTheMetWeeksThatFollowEachOther() = runBlocking {
        // Four weeks straight, each one 800 kg above the last.
        logSession(LocalDate.parse("2026-07-22").toEpochDay(), weightKg = 80.0, reps = 5, sets = 3) // 1200 kg
        logSession(LocalDate.parse("2026-07-29").toEpochDay(), weightKg = 80.0, reps = 5, sets = 5) // 2000 kg
        logSession(LocalDate.parse("2026-08-05").toEpochDay(), weightKg = 80.0, reps = 5, sets = 7) // 2800 kg
        logSession(LocalDate.parse("2026-08-12").toEpochDay(), weightKg = 80.0, reps = 5, sets = 9) // 3600 kg

        val streak = repository.getStreak(
            goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 500.0),
            today = today,
            periods = 4,
        )

        // Three weeks gained over the week before them; the first trained week had nothing to beat.
        assertEquals(3, streak.met)
        assertEquals(3, streak.considered)
        assertEquals(3, streak.currentRun)
    }

    @Test
    fun streak_leavesTheRunningPeriodOut() = runBlocking {
        // Only this week has any training at all: nothing finished yet to judge.
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 3)

        val streak = repository.getStreak(
            goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 500.0),
            today = today,
            periods = 4,
        )

        // A half-finished week is neither met nor missed, so it stays out of the count entirely.
        assertEquals(0, streak.met)
        assertEquals(0, streak.considered)
    }
}
