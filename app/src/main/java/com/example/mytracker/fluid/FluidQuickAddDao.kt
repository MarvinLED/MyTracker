package com.example.mytracker.fluid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FluidQuickAddDao {
    @Query("SELECT * FROM fluid_quick_adds ORDER BY sortOrder")
    fun observeAll(): Flow<List<FluidQuickAdd>>

    @Query("SELECT * FROM fluid_quick_adds ORDER BY sortOrder")
    suspend fun getAllOnce(): List<FluidQuickAdd>

    @Query("SELECT * FROM fluid_quick_adds WHERE id = :id")
    suspend fun getById(id: String): FluidQuickAdd?

    @Upsert
    suspend fun upsert(quickAdd: FluidQuickAdd)

    @Delete
    suspend fun delete(quickAdd: FluidQuickAdd)

    /** Wipes the Schnellauswahl for a replacing import. */
    @Query("DELETE FROM fluid_quick_adds")
    suspend fun deleteAll()
}
