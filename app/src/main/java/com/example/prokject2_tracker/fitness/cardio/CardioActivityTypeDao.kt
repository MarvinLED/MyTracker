package com.example.prokject2_tracker.fitness.cardio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardioActivityTypeDao {
    @Query("SELECT * FROM cardio_activity_types ORDER BY sortOrder")
    fun observeAll(): Flow<List<CardioActivityType>>

    @Query("SELECT * FROM cardio_activity_types ORDER BY sortOrder")
    suspend fun getAllOnce(): List<CardioActivityType>

    @Query("SELECT * FROM cardio_activity_types WHERE id = :id")
    suspend fun getById(id: String): CardioActivityType?

    @Upsert
    suspend fun upsert(type: CardioActivityType)

    @Upsert
    suspend fun upsertAll(types: List<CardioActivityType>)

    @Delete
    suspend fun delete(type: CardioActivityType)

    @Query("SELECT EXISTS(SELECT 1 FROM cardio_sessions WHERE activityTypeId = :id)")
    suspend fun isUsedInAnyEntry(id: String): Boolean
}
