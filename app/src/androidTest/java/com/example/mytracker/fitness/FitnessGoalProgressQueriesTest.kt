package com.example.mytracker.fitness

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mytracker.core.database.AppDatabase
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fitness.strength.StrengthLogEntry
import com.example.mytracker.fitness.strength.StrengthSet
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    /** One session of [reps] × [weightKg], [sets] sets, on [epochDay]. */
    private suspend fun logSession(epochDay: Long, weightKg: Double, reps: Int, sets: Int) {
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

        val progress = repository.getPeriodProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(2.5, progress, 0.0001)
    }

    @Test
    fun maxWeightIncrease_isZeroWhileTheExerciseHasNoHistoryToBeat() = runBlocking {
        logSession(thisMonday, weightKg = 82.5, reps = 5, sets = 3)

        // Null and not 0 kg is what the query returns for "never trained before" — a first session
        // is not a gain of 82,5 kg, and reporting one would tick off any Steigerungsziel at once.
        assertNull(db.strengthSetDao().maxWeightBeforeForExercise("bench", thisMonday))
        val progress = repository.getPeriodProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(0.0, progress, 0.0001)
    }

    @Test
    fun maxWeightIncrease_isZeroInAWeekWithoutTraining() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)

        val progress = repository.getPeriodProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(0.0, progress, 0.0001)
    }

    @Test
    fun maxWeightIncrease_goesNegativeWhenTheTopSetDrops() = runBlocking {
        logSession(lastWednesday, weightKg = 85.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 3)

        // A lighter week is a step back, and the goal card says so rather than rounding it to zero.
        val progress = repository.getPeriodProgress(goal(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, 5.0), today)

        assertEquals(-5.0, progress, 0.0001)
    }

    @Test
    fun volumeIncrease_comparesThisWeekWithTheWholeWeekBefore() = runBlocking {
        // 3 × 5 × 80 = 1200 kg last week, 4 × 5 × 80 = 1600 kg this week.
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 3)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 4)

        val progress = repository.getPeriodProgress(goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 300.0), today)

        assertEquals(400.0, progress, 0.0001)
    }

    @Test
    fun volumeIncrease_isNegativeWhenLessWasDoneThanTheWeekBefore() = runBlocking {
        logSession(lastWednesday, weightKg = 80.0, reps = 5, sets = 4)
        logSession(thisMonday, weightKg = 80.0, reps = 5, sets = 1)

        val progress = repository.getPeriodProgress(goal(FitnessGoalMetric.STRENGTH_VOLUME_INCREASE, 300.0), today)

        assertEquals(-1200.0, progress, 0.0001)
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
}
