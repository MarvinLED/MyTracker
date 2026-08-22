package com.example.prokject2_tracker.fluid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FluidUnitDao {
    @Query("SELECT * FROM fluid_units ORDER BY sortOrder")
    fun observeAll(): Flow<List<FluidUnit>>

    @Query("SELECT * FROM fluid_units ORDER BY sortOrder")
    suspend fun getAllOnce(): List<FluidUnit>

    @Query("SELECT * FROM fluid_units WHERE id = :id")
    suspend fun getById(id: String): FluidUnit?

    @Upsert
    suspend fun upsert(unit: FluidUnit)

    @Upsert
    suspend fun upsertAll(units: List<FluidUnit>)

    @Delete
    suspend fun delete(unit: FluidUnit)

    @Query("SELECT EXISTS(SELECT 1 FROM fluid_entries WHERE fluidUnitId = :unitId)")
    suspend fun isUsedInAnyEntry(unitId: String): Boolean

    /** Wipes the Maßeinheiten for a replacing import. */
    @Query("DELETE FROM fluid_units")
    suspend fun deleteAll()
}
