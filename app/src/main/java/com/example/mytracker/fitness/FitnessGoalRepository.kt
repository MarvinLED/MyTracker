package com.example.mytracker.fitness

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.cardio.CardioDao
import com.example.mytracker.fitness.strength.ExerciseMaxWeight
import com.example.mytracker.fitness.strength.MovementDirection
import com.example.mytracker.fitness.strength.StrengthSetDao
import com.example.mytracker.weight.BodyWeightDao
import java.time.Instant
import java.util.UUID
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
    private val goalChangeDao: FitnessGoalChangeDao,
    private val bodyWeightDao: BodyWeightDao,
) {
    fun observeAll(): Flow<List<FitnessGoal>> = fitnessGoalDao.observeAll()

    fun goalId(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        exerciseId: String?,
    ): String = listOfNotNull(metric.name, period.name, muscleGroupId, movementDirection?.name, exerciseId)
        .joinToString("-")

    /**
     * [label] is what the change log records this goal as — it is snapshotted there, so a scope that
     * is deleted later does not turn its own history into "(gelöscht)".
     */
    suspend fun setGoal(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        targetValue: Double,
        exerciseId: String? = null,
        isPercent: Boolean = false,
        label: String = metric.label(),
    ) {
        val id = goalId(metric, period, muscleGroupId, movementDirection, exerciseId)
        val existing = fitnessGoalDao.getById(id)
        fitnessGoalDao.upsert(
            FitnessGoal(
                id = id,
                metric = metric,
                period = period,
                muscleGroupId = muscleGroupId,
                targetValue = targetValue,
                // A goal that is only being re-saved keeps the day it was first set: this is when
                // the *target* changed, not when the form was last submitted.
                createdAt = existing?.createdAt ?: Instant.now(),
                movementDirection = movementDirection,
                exerciseId = exerciseId,
                isPercent = isPercent,
            ),
        )
        if (existing?.targetValue != targetValue || existing.isPercent != isPercent) {
            logChange(goalKey = id, label = label, targetValue = targetValue, isPercent = isPercent)
        }
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
        label: String = metric.label(),
    ) {
        val id = goalId(metric, period, muscleGroupId, movementDirection, exerciseId)
        // Only a goal that existed is a change: saving a screen full of empty fields must not fill
        // the log with "kein Ziel" rows for goals nobody ever set.
        val existing = fitnessGoalDao.getById(id) ?: return
        fitnessGoalDao.delete(id)
        logChange(goalKey = id, label = label, targetValue = null, isPercent = existing.isPercent)
    }

    suspend fun deleteGoal(id: String) = fitnessGoalDao.delete(id)

    private suspend fun logChange(
        goalKey: String,
        label: String,
        targetValue: Double?,
        isPercent: Boolean,
        targetEpochDay: Long? = null,
    ) {
        goalChangeDao.insert(
            FitnessGoalChange(
                id = UUID.randomUUID().toString(),
                goalKey = goalKey,
                label = label,
                effectiveFromEpochDay = DateUtils.todayEpochDay(),
                targetValue = targetValue,
                isPercent = isPercent,
                targetEpochDay = targetEpochDay,
                changedAt = Instant.now(),
            ),
        )
    }

    fun observeGoalChanges(): Flow<List<FitnessGoalChange>> = goalChangeDao.observeAll()

    /**
     * How far this goal has come in its current period.
     *
     * For the counting metrics that is simply the count so far. For the Steigerungen it is a
     * **difference**, and each is measured against what it is actually meant to beat: the top set
     * against the best there has ever been before this period, a volume against the last period
     * that was trained at all. Periods without training are skipped rather than counted as zero —
     * a deload week is not a collapse in volume, and treating it as one both breaks the run of met
     * weeks and hands the week after it a gain it did not earn.
     */
    suspend fun getProgress(goal: FitnessGoal, today: Long = DateUtils.todayEpochDay()): FitnessGoalProgress {
        val current = currentPeriod(goal.period, today)
        val start = current.first

        fun counted(value: Double) = FitnessGoalProgress(value = value, target = goal.targetValue)

        return when (goal.metric) {
            FitnessGoalMetric.CARDIO_SESSIONS -> counted(cardioDao.countSessionsBetween(start, today).toDouble())
            FitnessGoalMetric.CARDIO_DURATION_MINUTES -> counted(cardioDao.sumDurationMinutesBetween(start, today))
            FitnessGoalMetric.STRENGTH_SETS_TOTAL -> counted(strengthSetDao.countBetween(start, today).toDouble())
            FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP ->
                counted(strengthSetDao.countBetweenForMuscleGroup(goal.muscleGroupId!!, start, today).toDouble())
            FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION ->
                counted(
                    strengthSetDao.countBetweenForMovementDirection(goal.movementDirection!!.name, start, today)
                        .toDouble(),
                )

            FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE -> {
                val exerciseId = goal.exerciseId ?: return counted(0.0)
                val trained = strengthSetDao.countBetweenForExercise(exerciseId, start, today) > 0
                val best = strengthSetDao.maxWeightBetweenForExercise(exerciseId, start, today)
                val previousBest = strengthSetDao.maxWeightBeforeForExercise(exerciseId, start)
                when {
                    // Nothing lifted this period: a pause, not a lost kilo.
                    !trained -> FitnessGoalProgress(
                        value = 0.0,
                        target = goal.targetValue,
                        isPercent = goal.isPercent,
                        isPaused = true,
                        hasReference = false,
                    )
                    // Nothing to beat yet is not a gain of the whole bar: the first time an exercise
                    // is trained, "how much heavier than before" has no answer.
                    best == null || previousBest == null || (goal.isPercent && previousBest <= 0.0) ->
                        FitnessGoalProgress(
                            value = 0.0,
                            target = goal.targetValue,
                            isPercent = goal.isPercent,
                            hasReference = false,
                        )
                    else -> FitnessGoalProgress(
                        value = if (goal.isPercent) {
                            (best - previousBest) / previousBest * 100.0
                        } else {
                            best - previousBest
                        },
                        target = goal.targetValue,
                        isPercent = goal.isPercent,
                        referencePeriodsBack = 1,
                    )
                }
            }

            FitnessGoalMetric.STRENGTH_VOLUME_INCREASE -> {
                val exerciseId = goal.exerciseId ?: return counted(0.0)
                volumeIncrease(
                    goal = goal,
                    today = today,
                    trainedIn = { range -> strengthSetDao.countBetweenForExercise(exerciseId, range.first, range.last) > 0 },
                    volumeIn = { range -> strengthSetDao.volumeBetweenForExercise(exerciseId, range.first, range.last) },
                )
            }

            FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP -> {
                val muscleGroupId = goal.muscleGroupId ?: return counted(0.0)
                volumeIncrease(
                    goal = goal,
                    today = today,
                    trainedIn = { range ->
                        strengthSetDao.countBetweenForMuscleGroup(muscleGroupId, range.first, range.last) > 0
                    },
                    volumeIn = { range ->
                        strengthSetDao.volumeBetweenForMuscleGroup(muscleGroupId, range.first, range.last)
                    },
                )
            }

            FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MOVEMENT_DIRECTION -> {
                val direction = goal.movementDirection?.name ?: return counted(0.0)
                volumeIncrease(
                    goal = goal,
                    today = today,
                    trainedIn = { range ->
                        strengthSetDao.countBetweenForMovementDirection(direction, range.first, range.last) > 0
                    },
                    volumeIn = { range ->
                        strengthSetDao.volumeBetweenForMovementDirection(direction, range.first, range.last)
                    },
                )
            }
        }
    }

    /**
     * The volume gain of one scope, against the most recent period it was actually trained in.
     *
     * The lookups are passed in rather than switched on here because only the query differs between
     * an exercise, a muscle group and a movement direction — the rule about which period counts is
     * the same one, and it is the part worth having in exactly one place.
     */
    private suspend fun volumeIncrease(
        goal: FitnessGoal,
        today: Long,
        trainedIn: suspend (LongRange) -> Boolean,
        volumeIn: suspend (LongRange) -> Double,
    ): FitnessGoalProgress {
        val current = currentPeriod(goal.period, today)
        return increaseAgainstLastTrainedPeriod(
            currentValue = if (trainedIn(current)) volumeIn(current) else 0.0,
            currentTrained = trainedIn(current),
            target = goal.targetValue,
            isPercent = goal.isPercent,
            trainedIn = { back -> trainedIn(periodBefore(goal.period, today, back)) },
            valueIn = { back -> volumeIn(periodBefore(goal.period, today, back)) },
        )
    }

    fun observeMaxWeightGoals(): Flow<List<StrengthMaxWeightGoal>> = maxWeightGoalDao.observeAll()

    fun observeMaxWeightGoalForExercise(exerciseId: String): Flow<StrengthMaxWeightGoal?> =
        maxWeightGoalDao.observeForExercise(exerciseId)

    /** Every exercise's all-time top set, keyed by exercise — the "aktuell" a long-term goal is read against. */
    fun observeMaxWeightPerExercise(): Flow<Map<String, Double>> =
        strengthSetDao.observeMaxWeightPerExercise().map { rows ->
            rows.mapNotNull { row: ExerciseMaxWeight -> row.value?.let { row.exerciseId to it } }.toMap()
        }

    /** The latest logged body weight, for the goals that are read relative to it. Null until one is logged. */
    fun observeLatestBodyWeightKg(): Flow<Double?> = bodyWeightDao.observeLatest().map { it?.weightKg }

    /**
     * Sets or moves one exercise's long-term target. An existing goal keeps its starting point:
     * raising the target or pushing the date must not quietly re-anchor the plan to today's weight,
     * which would erase every kilo already gained towards it.
     *
     * [targetBodyweightMultiple] makes the target relative — [targetWeightKg] is then what that
     * multiple works out to at today's body weight, kept so the goal still reads as a number.
     */
    suspend fun setMaxWeightGoal(
        exerciseId: String,
        targetWeightKg: Double,
        targetEpochDay: Long,
        targetBodyweightMultiple: Double? = null,
        label: String = "Maximalgewicht",
    ) {
        val existing = maxWeightGoalDao.getForExercise(exerciseId)
        val today = DateUtils.todayEpochDay()
        maxWeightGoalDao.upsert(
            StrengthMaxWeightGoal(
                id = existing?.id ?: "maxweight-$exerciseId",
                exerciseId = exerciseId,
                targetWeightKg = targetWeightKg,
                targetBodyweightMultiple = targetBodyweightMultiple,
                targetEpochDay = targetEpochDay,
                startWeightKg = existing?.startWeightKg
                    ?: strengthSetDao.maxWeightForExercise(exerciseId)
                    ?: 0.0,
                startEpochDay = existing?.startEpochDay ?: today,
                createdAt = existing?.createdAt ?: Instant.now(),
            ),
        )
        val changed = existing == null ||
            existing.targetWeightKg != targetWeightKg ||
            existing.targetEpochDay != targetEpochDay ||
            existing.targetBodyweightMultiple != targetBodyweightMultiple
        if (changed) {
            logChange(
                goalKey = "maxweight-$exerciseId",
                label = label,
                targetValue = targetBodyweightMultiple ?: targetWeightKg,
                isPercent = false,
                targetEpochDay = targetEpochDay,
            )
        }
    }

    suspend fun clearMaxWeightGoal(exerciseId: String, label: String = "Maximalgewicht") {
        maxWeightGoalDao.getForExercise(exerciseId) ?: return
        maxWeightGoalDao.deleteForExercise(exerciseId)
        logChange(goalKey = "maxweight-$exerciseId", label = label, targetValue = null, isPercent = false)
    }
}
