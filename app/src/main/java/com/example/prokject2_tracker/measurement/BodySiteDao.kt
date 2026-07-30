package com.example.prokject2_tracker.measurement

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodySiteDao {
    @Query("SELECT * FROM body_sites ORDER BY sortOrder")
    fun observeAll(): Flow<List<BodySite>>

    @Query("SELECT * FROM body_sites ORDER BY sortOrder")
    suspend fun getAllOnce(): List<BodySite>

    @Query("SELECT * FROM body_sites WHERE id = :id")
    suspend fun getById(id: String): BodySite?

    @Upsert
    suspend fun upsert(site: BodySite)

    @Delete
    suspend fun delete(site: BodySite)
}
