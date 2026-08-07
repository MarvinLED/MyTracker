package com.example.prokject2_tracker.habit

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): Habit?

    @Query("SELECT * FROM habits ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<Habit>

    @Upsert
    suspend fun upsert(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    /** Wipes the Habits for a replacing import; their goals cascade with them. */
    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}
