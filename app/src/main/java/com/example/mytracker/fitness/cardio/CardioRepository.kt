package com.example.mytracker.fitness.cardio

import com.example.mytracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Seeded once on first run; the user can rename/add/remove freely afterwards. */
private val DEFAULT_CARDIO_ACTIVITY_TYPES = listOf(
    "Laufen",
    "Radfahren",
    "Schwimmen",
    "Gehen",
    "Wandern",
    "Rudern",
    "Sonstiges",
)

@Singleton
class CardioRepository @Inject constructor(
    private val cardioDao: CardioDao,
    private val cardioActivityTypeDao: CardioActivityTypeDao,
) {
    fun observeAll(): Flow<List<CardioSession>> = cardioDao.observeAll()

    suspend fun getById(id: String): CardioSession? = cardioDao.getById(id)

    fun observeDailyMinutesTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyMinutesTotal>> =
        cardioDao.observeDailyMinutesTotals(startInclusive, endInclusive)

    fun observeDailyCaloriesBurnedTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyCaloriesBurnedTotal>> =
        cardioDao.observeDailyCaloriesBurnedTotals(startInclusive, endInclusive)

    fun observeDailyAvgHeartRateTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyAvgHeartRateTotal>> =
        cardioDao.observeDailyAvgHeartRateTotals(startInclusive, endInclusive)

    fun observeActivityTypes(): Flow<List<CardioActivityType>> = cardioActivityTypeDao.observeAll()

    fun observeActivityTypesAlphabetical(): Flow<List<CardioActivityType>> =
        cardioActivityTypeDao.observeAllAlphabetical()

    fun observeForActivityType(activityTypeId: String): Flow<List<CardioSession>> =
        cardioDao.observeForActivityType(activityTypeId)

    fun observeLastSessionDayPerActivityType(): Flow<Map<String, Long>> =
        cardioDao.observeLastSessionDayPerActivityType()
            .map { rows -> rows.associate { it.activityTypeId to it.epochDay } }

    suspend fun ensureDefaultActivityTypesSeeded() {
        if (cardioActivityTypeDao.getAllOnce().isNotEmpty()) return
        val now = Instant.now()
        cardioActivityTypeDao.upsertAll(
            DEFAULT_CARDIO_ACTIVITY_TYPES.mapIndexed { index, name ->
                CardioActivityType(
                    id = IdGenerator.newId(),
                    name = name,
                    sortOrder = index,
                    createdAt = now,
                )
            },
        )
    }

    suspend fun createActivityType(name: String) {
        val maxSortOrder = cardioActivityTypeDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1
        cardioActivityTypeDao.upsert(
            CardioActivityType(
                id = IdGenerator.newId(),
                name = name,
                sortOrder = maxSortOrder + 1,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun updateActivityType(existing: CardioActivityType, name: String) {
        cardioActivityTypeDao.upsert(existing.copy(name = name))
    }

    suspend fun canDeleteActivityType(id: String): Boolean = !cardioActivityTypeDao.isUsedInAnyEntry(id)

    suspend fun deleteActivityType(type: CardioActivityType) {
        cardioActivityTypeDao.delete(type)
    }

    suspend fun save(
        existing: CardioSession?,
        epochDay: Long,
        activityTypeId: String,
        activityTypeName: String,
        durationMinutes: Double,
        distanceKm: Double?,
        caloriesBurned: Double?,
        avgHeartRateBpm: Int?,
        note: String?,
    ) {
        cardioDao.upsert(
            CardioSession(
                id = existing?.id ?: IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = existing?.createdAt ?: Instant.now(),
                activityTypeId = activityTypeId,
                activityTypeName = activityTypeName,
                durationMinutes = durationMinutes,
                distanceKm = distanceKm,
                caloriesBurned = caloriesBurned,
                avgHeartRateBpm = avgHeartRateBpm,
                note = note,
            ),
        )
    }

    suspend fun delete(session: CardioSession) {
        cardioDao.delete(session)
    }
}
