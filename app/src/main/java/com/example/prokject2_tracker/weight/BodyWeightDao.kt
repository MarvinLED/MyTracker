package com.example.prokject2_tracker.weight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyWeightEntry>>

    @Query("SELECT * FROM body_weight_entries WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<BodyWeightEntry?>

    @Query(
        "SELECT * FROM body_weight_entries WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "ORDER BY epochDay",
    )
    fun observeRange(startInclusive: Long, endInclusive: Long): Flow<List<BodyWeightEntry>>

    @Upsert
    suspend fun upsert(entry: BodyWeightEntry)

    @Delete
    suspend fun delete(entry: BodyWeightEntry)

    @Query("SELECT * FROM body_weight_entries ORDER BY epochDay")
    suspend fun getAllOnce(): List<BodyWeightEntry>

    /**
     * The day's entry, if there is one. A backup is matched against this rather than against the id:
     * `epochDay` is unique, so importing a second row for a day the device already has would break
     * on the index no matter what the row is called.
     */
    @Query("SELECT * FROM body_weight_entries WHERE epochDay = :epochDay")
    suspend fun getForDayOnce(epochDay: Long): BodyWeightEntry?

    /** Wipes the Gewichtsverlauf for a replacing import. */
    @Query("DELETE FROM body_weight_entries")
    suspend fun deleteAll()
}
