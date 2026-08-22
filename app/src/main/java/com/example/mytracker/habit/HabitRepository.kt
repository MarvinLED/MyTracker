package com.example.mytracker.habit

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitCheckInDao: HabitCheckInDao,
    private val habitGoalDao: HabitGoalDao,
) {
    fun observeActive(): Flow<List<Habit>> = habitDao.observeActive()

    fun observeCheckInsForDay(epochDay: Long): Flow<List<HabitCheckIn>> = habitCheckInDao.observeForDay(epochDay)

    fun observeDailyCompletedCounts(startInclusive: Long, endInclusive: Long): Flow<List<DailyCompletedCount>> =
        habitCheckInDao.observeDailyCompletedCounts(startInclusive, endInclusive)

    fun observeGoalsByHabitId(): Flow<Map<String, List<HabitGoal>>> =
        habitGoalDao.observeAll().map { goals -> goals.groupBy { it.habitId } }

    suspend fun createHabit(name: String, type: HabitType) {
        val now = Instant.now()
        habitDao.upsert(
            Habit(id = IdGenerator.newId(), name = name, archived = false, createdAt = now, updatedAt = now, type = type),
        )
    }

    suspend fun renameHabit(habit: Habit, name: String) {
        habitDao.upsert(habit.copy(name = name, updatedAt = Instant.now()))
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.delete(habit)
    }

    suspend fun setCheckedIn(habitId: String, epochDay: Long, checked: Boolean) {
        if (checked) {
            // Deterministic id keyed on (habitId, epochDay) so a repeated check-in upserts in
            // place instead of violating the unique index below.
            habitCheckInDao.upsert(
                HabitCheckIn(id = "$habitId-$epochDay", habitId = habitId, epochDay = epochDay, createdAt = Instant.now()),
            )
        } else {
            habitCheckInDao.deleteForHabitAndDay(habitId, epochDay)
        }
    }

    /** Upsert-with-value for COUNT/DURATION habits, or delete-for-day when [value] is null. */
    suspend fun logValue(habitId: String, epochDay: Long, value: Double?) {
        if (value != null) {
            habitCheckInDao.upsert(
                HabitCheckIn(
                    id = "$habitId-$epochDay",
                    habitId = habitId,
                    epochDay = epochDay,
                    createdAt = Instant.now(),
                    value = value,
                ),
            )
        } else {
            habitCheckInDao.deleteForHabitAndDay(habitId, epochDay)
        }
    }

    suspend fun setGoal(habit: Habit, period: GoalPeriod, target: Double?) {
        if (target != null) {
            habitGoalDao.upsert(
                HabitGoal(
                    id = "${habit.id}-${period.name}",
                    habitId = habit.id,
                    period = period,
                    targetValue = target,
                    createdAt = Instant.now(),
                ),
            )
        } else {
            habitGoalDao.deleteForHabitAndPeriod(habit.id, period)
        }
    }

    suspend fun getCurrentStreak(habit: Habit, today: Long = DateUtils.todayEpochDay()): Int {
        val byDay = habitCheckInDao.getAllForHabit(habit.id).associateBy { it.epochDay }
        val dailyGoal = habitGoalDao.getForHabitAndPeriod(habit.id, GoalPeriod.DAILY)
        fun qualifies(day: Long): Boolean {
            val c = byDay[day] ?: return false
            return when (habit.type) {
                HabitType.YES_NO -> true
                HabitType.COUNT, HabitType.DURATION ->
                    if (dailyGoal == null) c.value != null else (c.value ?: 0.0) >= dailyGoal.targetValue
            }
        }
        var streak = 0
        var day = today
        while (qualifies(day)) {
            streak++
            day--
        }
        return streak
    }

    suspend fun getPeriodProgress(habit: Habit, goal: HabitGoal, today: Long = DateUtils.todayEpochDay()): Double {
        val start = when (goal.period) {
            GoalPeriod.DAILY -> today
            GoalPeriod.WEEKLY -> DateUtils.startOfWeekEpochDay(today)
            GoalPeriod.MONTHLY -> DateUtils.startOfMonthEpochDay(today)
        }
        val checkIns = habitCheckInDao.getAllForHabit(habit.id).filter { it.epochDay in start..today }
        return when (habit.type) {
            HabitType.YES_NO -> checkIns.size.toDouble()
            HabitType.COUNT, HabitType.DURATION -> checkIns.sumOf { it.value ?: 0.0 }
        }
    }
}
