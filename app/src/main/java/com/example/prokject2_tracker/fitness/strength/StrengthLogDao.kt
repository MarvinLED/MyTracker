package com.example.prokject2_tracker.fitness.strength

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthLogDao {
    @Query("SELECT * FROM strength_log_entries ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<StrengthLogEntry>>

    @Query("SELECT * FROM strength_log_entries WHERE id = :id")
    suspend fun getById(id: String): StrengthLogEntry?

    @Upsert
    suspend fun upsert(entry: StrengthLogEntry)

    @Delete
    suspend fun delete(entry: StrengthLogEntry)
}
