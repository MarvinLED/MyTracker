package com.example.mytracker.achievements

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDayPointsDao {
    @Query("SELECT * FROM game_day_points ORDER BY epochDay")
    fun observeAll(): Flow<List<GameDayPoints>>

    @Upsert
    suspend fun upsertAll(rows: List<GameDayPoints>)

    /**
     * The days already settled. Only the days matter, not the rows — a booked day always writes one
     * row per attribute, so a day appearing here at all means it is done with.
     */
    @Query("SELECT DISTINCT epochDay FROM game_day_points")
    suspend fun bookedDays(): List<Long>

    @Query("SELECT MIN(epochDay) FROM game_day_points")
    suspend fun firstBookedDay(): Long?

    @Query("SELECT * FROM game_day_points ORDER BY epochDay")
    suspend fun getAllOnce(): List<GameDayPoints>

    @Query("DELETE FROM game_day_points")
    suspend fun deleteAll()
}
