package com.example.prokject2_tracker.fluid

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FluidRepository @Inject constructor(
    private val fluidDao: FluidDao,
) {
    fun observeForDay(epochDay: Long): Flow<List<FluidEntry>> = fluidDao.observeForDay(epochDay)

    fun observeDayTotalMl(epochDay: Long): Flow<Double> = fluidDao.observeDayTotalMl(epochDay)

    fun observeDailyMlTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyMlTotal>> =
        fluidDao.observeDailyMlTotals(startInclusive, endInclusive)

    suspend fun logFluid(epochDay: Long, type: FluidType, amountMl: Double) {
        fluidDao.upsert(
            FluidEntry(
                id = IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = Instant.now(),
                type = type,
                amountMl = amountMl,
            ),
        )
    }

    suspend fun delete(entry: FluidEntry) {
        fluidDao.delete(entry)
    }
}
