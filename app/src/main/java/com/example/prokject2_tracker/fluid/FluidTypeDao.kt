package com.example.prokject2_tracker.fluid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FluidTypeDao {
    @Query("SELECT * FROM fluid_types ORDER BY sortOrder")
    fun observeAll(): Flow<List<FluidType>>

    @Query("SELECT * FROM fluid_types ORDER BY sortOrder")
    suspend fun getAllOnce(): List<FluidType>

    @Query("SELECT * FROM fluid_types WHERE id = :id")
    suspend fun getById(id: String): FluidType?

    @Upsert
    suspend fun upsert(type: FluidType)

    @Upsert
    suspend fun upsertAll(types: List<FluidType>)

    @Delete
    suspend fun delete(type: FluidType)

    @Query("SELECT EXISTS(SELECT 1 FROM fluid_entries WHERE fluidTypeId = :typeId)")
    suspend fun isUsedInAnyEntry(typeId: String): Boolean
}
