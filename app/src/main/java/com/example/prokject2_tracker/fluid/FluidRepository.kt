package com.example.prokject2_tracker.fluid

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Seeded once on first run; the user can rename/add/remove freely afterwards. */
private val DEFAULT_FLUID_TYPES = listOf(
    "Wasser" to 250.0,
    "Kaffee" to 125.0,
    "Tee" to 200.0,
    "Saft" to 200.0,
    "Limonade" to 330.0,
    "Milch" to 200.0,
    "Sonstiges" to 200.0,
)

@Singleton
class FluidRepository @Inject constructor(
    private val fluidDao: FluidDao,
    private val fluidTypeDao: FluidTypeDao,
) {
    fun observeForDay(epochDay: Long): Flow<List<FluidEntry>> = fluidDao.observeForDay(epochDay)

    fun observeDayTotalMl(epochDay: Long): Flow<Double> = fluidDao.observeDayTotalMl(epochDay)

    fun observeDailyMlTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyMlTotal>> =
        fluidDao.observeDailyMlTotals(startInclusive, endInclusive)

    fun observeTypes(): Flow<List<FluidType>> = fluidTypeDao.observeAll()

    suspend fun ensureDefaultTypesSeeded() {
        if (fluidTypeDao.getAllOnce().isNotEmpty()) return
        val now = Instant.now()
        fluidTypeDao.upsertAll(
            DEFAULT_FLUID_TYPES.mapIndexed { index, (name, defaultMl) ->
                FluidType(
                    id = IdGenerator.newId(),
                    name = name,
                    defaultQuickAddMl = defaultMl,
                    sortOrder = index,
                    createdAt = now,
                )
            },
        )
    }

    suspend fun createType(name: String, defaultQuickAddMl: Double) {
        val maxSortOrder = fluidTypeDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1
        fluidTypeDao.upsert(
            FluidType(
                id = IdGenerator.newId(),
                name = name,
                defaultQuickAddMl = defaultQuickAddMl,
                sortOrder = maxSortOrder + 1,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun updateType(existing: FluidType, name: String, defaultQuickAddMl: Double) {
        fluidTypeDao.upsert(existing.copy(name = name, defaultQuickAddMl = defaultQuickAddMl))
    }

    suspend fun canDeleteType(typeId: String): Boolean = !fluidTypeDao.isUsedInAnyEntry(typeId)

    suspend fun deleteType(type: FluidType) {
        fluidTypeDao.delete(type)
    }

    suspend fun logFluid(epochDay: Long, type: FluidType, amountMl: Double) {
        fluidDao.upsert(
            FluidEntry(
                id = IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = Instant.now(),
                fluidTypeId = type.id,
                fluidTypeName = type.name,
                amountMl = amountMl,
            ),
        )
    }

    suspend fun delete(entry: FluidEntry) {
        fluidDao.delete(entry)
    }
}
