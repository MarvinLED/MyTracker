package com.example.mytracker.fluid

import com.example.mytracker.core.util.IdGenerator
import com.example.mytracker.core.util.formatCompact
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

/** One drink type's share of what a Tagebuch entry contributes to the fluid log. */
data class FluidContribution(val typeId: String, val amountMl: Double)

@Singleton
class FluidRepository @Inject constructor(
    private val fluidDao: FluidDao,
    private val fluidTypeDao: FluidTypeDao,
    private val fluidUnitDao: FluidUnitDao,
    private val fluidQuickAddDao: FluidQuickAddDao,
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

    /** Returns the row it wrote, so a caller that offers an undo has something to take back. */
    suspend fun logFluid(
        epochDay: Long,
        type: FluidType,
        amountMl: Double,
        unit: FluidUnit? = null,
    ): FluidEntry {
        val entry = FluidEntry(
            id = IdGenerator.newId(),
            epochDay = epochDay,
            createdAt = Instant.now(),
            fluidTypeId = type.id,
            fluidTypeName = type.name,
            amountMl = amountMl,
            fluidUnitId = unit?.id,
            fluidUnitName = unit?.name,
        )
        fluidDao.upsert(entry)
        return entry
    }

    fun observeQuickAdds(): Flow<List<FluidQuickAdd>> = fluidQuickAddDao.observeAll()

    /**
     * Logs what one Schnellauswahl button stands for. Returns null when its drink type is gone —
     * the cascade normally takes the button with it, so this only covers the race where the type
     * was deleted between the screen reading the buttons and the tap.
     */
    suspend fun logQuickAdd(epochDay: Long, quickAdd: FluidQuickAdd): FluidEntry? {
        val type = fluidTypeDao.getById(quickAdd.fluidTypeId) ?: return null
        return logFluid(epochDay, type, quickAdd.amountMl)
    }

    /** Silently a no-op past [FluidQuickAddLimit] — two rows is all the Tagebuch area draws. */
    suspend fun createQuickAdd(fluidTypeId: String, symbol: FluidQuickAddSymbol, amountMl: Double) {
        val existing = fluidQuickAddDao.getAllOnce()
        if (existing.size >= FluidQuickAddLimit) return
        fluidQuickAddDao.upsert(
            FluidQuickAdd(
                id = IdGenerator.newId(),
                fluidTypeId = fluidTypeId,
                symbol = symbol,
                amountMl = quickAddAmountFor(symbol, amountMl),
                sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun updateQuickAdd(
        existing: FluidQuickAdd,
        fluidTypeId: String,
        symbol: FluidQuickAddSymbol,
        amountMl: Double,
    ) {
        fluidQuickAddDao.upsert(
            existing.copy(
                fluidTypeId = fluidTypeId,
                symbol = symbol,
                amountMl = quickAddAmountFor(symbol, amountMl),
            ),
        )
    }

    suspend fun deleteQuickAdd(quickAdd: FluidQuickAdd) {
        fluidQuickAddDao.delete(quickAdd)
    }

    /** A "100" button logs 100 ml whatever else it is handed; the symbol says so on the button. */
    private fun quickAddAmountFor(symbol: FluidQuickAddSymbol, amountMl: Double): Double =
        if (symbol == FluidQuickAddSymbol.ML_100) FluidQuickAdd100Ml else amountMl

    /** Corrects a mistyped amount; the type and the Maßeinheit it was logged with stay as they were. */
    suspend fun updateEntryAmount(entry: FluidEntry, amountMl: Double) {
        fluidDao.upsert(entry.copy(amountMl = amountMl))
    }

    suspend fun delete(entry: FluidEntry) {
        fluidDao.delete(entry)
    }

    /**
     * Mirrors the fluid a Tagebuch entry contributes into the fluid log, replacing whatever that
     * diary entry produced before. Called by the diary side on every log/update. A Lebensmittel
     * contributes at most one type; a Rezept contributes one per drink-linked ingredient, hence the
     * list. An empty list (or only non-positive amounts) just clears the mirrored rows — the food's
     * link was removed, or the entry no longer contributes any fluid.
     */
    suspend fun syncFromDiaryEntry(diaryEntryId: String, epochDay: Long, contributions: List<FluidContribution>) {
        fluidDao.deleteForDiaryEntry(diaryEntryId)
        val now = Instant.now()
        contributions
            .filter { it.amountMl > 0.0 }
            .groupBy { it.typeId }
            .forEach { (typeId, sameType) ->
                val type = fluidTypeDao.getById(typeId) ?: return@forEach
                fluidDao.upsert(
                    FluidEntry(
                        id = IdGenerator.newId(),
                        epochDay = epochDay,
                        createdAt = now,
                        fluidTypeId = type.id,
                        fluidTypeName = type.name,
                        amountMl = sameType.sumOf { it.amountMl },
                        sourceDiaryEntryId = diaryEntryId,
                    ),
                )
            }
    }

    suspend fun deleteForDiaryEntry(diaryEntryId: String) {
        fluidDao.deleteForDiaryEntry(diaryEntryId)
    }
}
