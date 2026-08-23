package com.example.mytracker.fitness

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.cardio.CardioDao
import com.example.mytracker.fitness.strength.ExerciseMaxWeight
import com.example.mytracker.fitness.strength.MovementDirection
import com.example.mytracker.fitness.strength.StrengthSetDao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FitnessGoalRepository @Inject constructor(
    private val fitnessGoalDao: FitnessGoalDao,
    private val cardioDao: CardioDao,
    private val strengthSetDao: StrengthSetDao,
    private val maxWeightGoalDao: StrengthMaxWeightGoalDao,
) {
    fun observeAll(): Flow<List<FitnessGoal>> = fitnessGoalDao.observeAll()

    private fun goalId(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        exerciseId: String?,
    ): String = listOfNotNull(metric.name, period.name, muscleGroupId, movementDirection?.name, exerciseId)
        .joinToString("-")

    suspend fun setGoal(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        targetValue: Double,
        exerciseId: String? = null,
    ) {
        fitnessGoalDao.upsert(
            FitnessGoal(
                id = goalId(metric, period, muscleGroupId, movementDirection, exerciseId),
                metric = metric,
                period = period,
                muscleGroupId = muscleGroupId,
                targetValue = targetValue,
                createdAt = Instant.now(),
                movementDirection = movementDirection,
                exerciseId = exerciseId,
            ),
        )
    }

    /**
     * Removes the goal for exactly this combination, if there is one. The counterpart to [setGoal]
     * for a screen that lists every possible goal: an emptied field means "kein Ziel", and that has
     * to delete the row rather than store a zero, which would read as a target of nothing.
     */
    suspend fun clearGoal(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        exerciseId: String? = null,
    ) = fitnessGoalDao.delete(goalId(metric, period, muscleGroupId, movementDirection, exerciseId))

    suspend fun deleteGoal(id: String) = fitnessGoalDao.delete(id)

    /**
     * How far this goal has come in its current period.
     *
     * For the counting metrics that is simply the count so far. For the two Steigerungen it is a
     * **difference**, and each is measured against what it is actually meant to beat: the top set
     * against the best there has ever been before this period, the volume against the whole period
     * before. See [FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE] and
     * [com.example.mytracker.fitness.strength.StrengthSetDao.maxWeightBeforeForExercise].
     */
    suspend fun getPeriodProgress(goal: FitnessGoal, today: Long = DateUtils.todayEpochDay()): Double {
        val current = currentPeriod(goal.period, today)
        val start = current.first
        return when (goal.metric) {
            FitnessGoalMetric.CARDIO_SESSIONS -> cardioDao.countSessionsBetween(start, today).toDouble()
            FitnessGoalMetric.CARDIO_DURATION_MINUTES -> cardioDao.sumDurationMinutesBetween(start, today)
            FitnessGoalMetric.STRENGTH_SETS_TOTAL -> strengthSetDao.countBetween(start, today).toDouble()
            FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP ->
                strengthSetDao.countBetweenForMuscleGroup(goal.muscleGroupId!!, start, today).toDouble()
            FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION ->
                strengthSetDao.countBetweenForMovementDirection(goal.movementDirection!!.name, start, today).toDouble()

            FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE -> {
                val exerciseId = goal.exerciseId ?: return 0.0
                val best = strengthSetDao.maxWeightBetweenForExercise(exerciseId, start, today)
                val previousBest = strengthSetDao.maxWeightBeforeForExercise(exerciseId, start)
                // Nothing to beat yet is not a gain of the whole bar: the first time an exercise is
                // trained, "how much heavier than before" has no answer, and 0 is the honest one.
                if (best == null || previousBest == null) 0.0 else (best - previousBest)
            }

            FitnessGoalMetric.STRENGTH_VOLUME_INCREASE -> {
                val exerciseId = goal.exerciseId ?: return 0.0
                val previous = previousPeriod(goal.period, today)
                val done = strengthSetDao.volumeBetweenForExercise(exerciseId, start, today)
                val before = strengthSetDao.volumeBetweenForExercise(exerciseId, previous.first, previous.last)
                done - before
            }
        }
    }

    fun observeMaxWeightGoals(): Flow<List<StrengthMaxWeightGoal>> = maxWeightGoalDao.observeAll()

    /** Every exercise's all-time top set, keyed by exercise — the "aktuell" a long-term goal is read against. */
    fun observeMaxWeightPerExercise(): Flow<Map<String, Double>> =
        strengthSetDao.observeMaxWeightPerExercise().map { rows ->
            rows.mapNotNull { row: ExerciseMaxWeight -> row.value?.let { row.exerciseId to it } }.toMap()
        }

    /**
     * Sets or moves one exercise's long-term target. An existing goal keeps its starting point:
     * raising the target or pushing the date must not quietly re-anchor the plan to today's weight,
     * which would erase every kilo already gained towards it.
     */
    suspend fun setMaxWeightGoal(exerciseId: String, targetWeightKg: Double, targetEpochDay: Long) {
        val existing = maxWeightGoalDao.getForExercise(exerciseId)
        val today = DateUtils.todayEpochDay()
        maxWeightGoalDao.upsert(
            StrengthMaxWeightGoal(
                id = existing?.id ?: "maxweight-$exerciseId",
                exerciseId = exerciseId,
                targetWeightKg = targetWeightKg,
                targetEpochDay = targetEpochDay,
                startWeightKg = existing?.startWeightKg
                    ?: strengthSetDao.maxWeightForExercise(exerciseId)
                    ?: 0.0,
                startEpochDay = existing?.startEpochDay ?: today,
                createdAt = existing?.createdAt ?: Instant.now(),
            ),
        )
    }

    suspend fun clearMaxWeightGoal(exerciseId: String) = maxWeightGoalDao.deleteForExercise(exerciseId)
}
