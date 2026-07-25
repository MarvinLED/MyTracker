package com.example.prokject2_tracker.fluid

import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.core.util.formatCompact
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

/** Seeded once on first run; the user can rename/add/remove freely afterwards (e.g. add "Glas" = 400 ml). */
private val DEFAULT_FLUID_UNITS = listOf(100.0, 150.0, 200.0, 250.0, 300.0, 330.0, 400.0, 500.0, 750.0, 1000.0)

@Singleton
class FluidRepository @Inject constructor(
    private val fluidDao: FluidDao,
    private val fluidTypeDao: FluidTypeDao,
    private val fluidUnitDao: FluidUnitDao,
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

    suspend fun createType(name: String, defaultQuickAddMl: Double, colorArgb: Int?) {
        val maxSortOrder = fluidTypeDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1
        fluidTypeDao.upsert(
            FluidType(
                id = IdGenerator.newId(),
                name = name,
                defaultQuickAddMl = defaultQuickAddMl,
                sortOrder = maxSortOrder + 1,
                createdAt = Instant.now(),
                colorArgb = colorArgb,
            ),
        )
    }

    suspend fun updateType(existing: FluidType, name: String, defaultQuickAddMl: Double, colorArgb: Int?) {
        fluidTypeDao.upsert(
            existing.copy(name = name, defaultQuickAddMl = defaultQuickAddMl, colorArgb = colorArgb),
        )
    }

    suspend fun canDeleteType(typeId: String): Boolean = !fluidTypeDao.isUsedInAnyEntry(typeId)

    suspend fun deleteType(type: FluidType) {
        fluidTypeDao.delete(type)
    }

    suspend fun updateTypeGoals(type: FluidType, dailyGoalMinMl: Double?, dailyGoalMaxMl: Double?) {
        fluidTypeDao.upsert(type.copy(dailyGoalMinMl = dailyGoalMinMl, dailyGoalMaxMl = dailyGoalMaxMl))
    }

    fun observeUnits(): Flow<List<FluidUnit>> = fluidUnitDao.observeAll()

    suspend fun ensureDefaultUnitsSeeded() {
        if (fluidUnitDao.getAllOnce().isNotEmpty()) return
        val now = Instant.now()
        fluidUnitDao.upsertAll(
            DEFAULT_FLUID_UNITS.mapIndexed { index, amountMl ->
                FluidUnit(
                    id = IdGenerator.newId(),
                    name = "${amountMl.formatCompact()} ml",
                    amountMl = amountMl,
                    sortOrder = index,
                    createdAt = now,
                )
            },
        )
    }

    suspend fun createUnit(name: String, amountMl: Double) {
        val maxSortOrder = fluidUnitDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1
        fluidUnitDao.upsert(
            FluidUnit(
                id = IdGenerator.newId(),
                name = name,
                amountMl = amountMl,
                sortOrder = maxSortOrder + 1,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun updateUnit(existing: FluidUnit, name: String, amountMl: Double) {
        fluidUnitDao.upsert(existing.copy(name = name, amountMl = amountMl))
    }

    suspend fun canDeleteUnit(unitId: String): Boolean = !fluidUnitDao.isUsedInAnyEntry(unitId)

    suspend fun deleteUnit(unit: FluidUnit) {
        fluidUnitDao.delete(unit)
    }

    suspend fun logFluid(epochDay: Long, type: FluidType, amountMl: Double, unit: FluidUnit? = null) {
        fluidDao.upsert(
            FluidEntry(
                id = IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = Instant.now(),
                fluidTypeId = type.id,
                fluidTypeName = type.name,
                amountMl = amountMl,
                fluidUnitId = unit?.id,
                fluidUnitName = unit?.name,
            ),
        )
    }

    suspend fun delete(entry: FluidEntry) {
        fluidDao.delete(entry)
    }

    /**
     * Mirrors a Tagebuch entry whose Lebensmittel is linked to a Getränkeart into the fluid log,
     * replacing whatever that diary entry produced before. Called by the diary side on every
     * log/update; [typeId] `null` or a non-positive [amountMl] just clears the mirrored row (the
     * food's link was removed, or the entry no longer contributes any fluid).
     */
    suspend fun syncFromDiaryEntry(diaryEntryId: String, epochDay: Long, typeId: String?, amountMl: Double) {
        fluidDao.deleteForDiaryEntry(diaryEntryId)
        if (typeId == null || amountMl <= 0.0) return
        val type = fluidTypeDao.getById(typeId) ?: return
        fluidDao.upsert(
            FluidEntry(
                id = IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = Instant.now(),
                fluidTypeId = type.id,
                fluidTypeName = type.name,
                amountMl = amountMl,
                sourceDiaryEntryId = diaryEntryId,
            ),
        )
    }

    suspend fun deleteForDiaryEntry(diaryEntryId: String) {
        fluidDao.deleteForDiaryEntry(diaryEntryId)
    }
}
