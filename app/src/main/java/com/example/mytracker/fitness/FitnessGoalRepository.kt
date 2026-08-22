package com.example.mytracker.fitness

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.cardio.CardioDao
import com.example.mytracker.fitness.strength.MovementDirection
import com.example.mytracker.fitness.strength.StrengthSetDao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FitnessGoalRepository @Inject constructor(
    private val fitnessGoalDao: FitnessGoalDao,
    private val cardioDao: CardioDao,
    private val strengthSetDao: StrengthSetDao,
) {
    fun observeAll(): Flow<List<FitnessGoal>> = fitnessGoalDao.observeAll()

    private fun goalId(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
    ): String = listOfNotNull(metric.name, period.name, muscleGroupId, movementDirection?.name).joinToString("-")

    suspend fun setGoal(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        targetValue: Double,
    ) {
        fitnessGoalDao.upsert(
            FitnessGoal(
                id = goalId(metric, period, muscleGroupId, movementDirection),
                metric = metric,
                period = period,
                muscleGroupId = muscleGroupId,
                targetValue = targetValue,
                createdAt = Instant.now(),
                movementDirection = movementDirection,
            ),
        )
    }

    suspend fun deleteGoal(id: String) = fitnessGoalDao.delete(id)

    suspend fun getPeriodProgress(goal: FitnessGoal, today: Long = DateUtils.todayEpochDay()): Double {
        val start = when (goal.period) {
            GoalPeriod.WEEKLY -> DateUtils.startOfWeekEpochDay(today)
            GoalPeriod.MONTHLY -> DateUtils.startOfMonthEpochDay(today)
            GoalPeriod.DAILY -> today
        }
        return when (goal.metric) {
            FitnessGoalMetric.CARDIO_SESSIONS -> cardioDao.countSessionsBetween(start, today).toDouble()
            FitnessGoalMetric.CARDIO_DURATION_MINUTES -> cardioDao.sumDurationMinutesBetween(start, today)
            FitnessGoalMetric.STRENGTH_SETS_TOTAL -> strengthSetDao.countBetween(start, today).toDouble()
            FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP ->
                strengthSetDao.countBetweenForMuscleGroup(goal.muscleGroupId!!, start, today).toDouble()
            FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION ->
                strengthSetDao.countBetweenForMovementDirection(goal.movementDirection!!.name, start, today).toDouble()
        }
    }
}
