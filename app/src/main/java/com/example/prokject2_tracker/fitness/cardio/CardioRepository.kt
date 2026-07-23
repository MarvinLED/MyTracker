package com.example.prokject2_tracker.fitness.cardio

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CardioRepository @Inject constructor(
    private val cardioDao: CardioDao,
) {
    fun observeAll(): Flow<List<CardioSession>> = cardioDao.observeAll()

    suspend fun getById(id: String): CardioSession? = cardioDao.getById(id)

    fun observeDailyMinutesTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyMinutesTotal>> =
        cardioDao.observeDailyMinutesTotals(startInclusive, endInclusive)

    fun observeDailyCaloriesBurnedTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyCaloriesBurnedTotal>> =
        cardioDao.observeDailyCaloriesBurnedTotals(startInclusive, endInclusive)

    suspend fun save(
        existing: CardioSession?,
        epochDay: Long,
        activityType: CardioActivityType,
        durationMinutes: Double,
        distanceKm: Double?,
        caloriesBurned: Double,
        note: String?,
    ) {
        cardioDao.upsert(
            CardioSession(
                id = existing?.id ?: IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = existing?.createdAt ?: Instant.now(),
                activityType = activityType,
                durationMinutes = durationMinutes,
                distanceKm = distanceKm,
                caloriesBurned = caloriesBurned,
                note = note,
            ),
        )
    }

    suspend fun delete(session: CardioSession) {
        cardioDao.delete(session)
    }
}
