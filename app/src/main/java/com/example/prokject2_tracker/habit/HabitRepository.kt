package com.example.prokject2_tracker.habit

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitCheckInDao: HabitCheckInDao,
) {
    fun observeActive(): Flow<List<Habit>> = habitDao.observeActive()

    fun observeCheckInsForDay(epochDay: Long): Flow<List<HabitCheckIn>> = habitCheckInDao.observeForDay(epochDay)

    fun observeDailyCompletedCounts(startInclusive: Long, endInclusive: Long): Flow<List<DailyCompletedCount>> =
        habitCheckInDao.observeDailyCompletedCounts(startInclusive, endInclusive)

    suspend fun createHabit(name: String) {
        val now = Instant.now()
        habitDao.upsert(
            Habit(id = IdGenerator.newId(), name = name, archived = false, createdAt = now, updatedAt = now),
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
}
