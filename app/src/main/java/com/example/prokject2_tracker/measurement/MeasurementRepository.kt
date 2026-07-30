package com.example.prokject2_tracker.measurement

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MeasurementRepository @Inject constructor(
    private val bodySiteDao: BodySiteDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
) {
    fun observeSites(): Flow<List<BodySite>> = bodySiteDao.observeAll()

    fun observeMeasurements(): Flow<List<BodyMeasurement>> = bodyMeasurementDao.observeAll()

    /** Appended at the end of the library, so existing sites keep their row order and chart colour. */
    suspend fun createSite(name: String, measuringHint: String?) {
        val maxSortOrder = bodySiteDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1
        bodySiteDao.upsert(
            BodySite(
                id = IdGenerator.newId(),
                name = name.trim(),
                measuringHint = measuringHint?.trim()?.takeIf { it.isNotBlank() },
                sortOrder = maxSortOrder + 1,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun updateSite(existing: BodySite, name: String, measuringHint: String?) {
        bodySiteDao.upsert(
            existing.copy(
                name = name.trim(),
                measuringHint = measuringHint?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
    }

    /** Cascades to the site's measurements — see [BodyMeasurement]. */
    suspend fun deleteSite(site: BodySite) {
        bodySiteDao.delete(site)
    }

    /** How much history a delete would take with it, so the confirmation can say so. */
    suspend fun measurementCount(siteId: String): Int = bodyMeasurementDao.countForSite(siteId)

    /** Upserts at the deterministic id for (site, day), so re-measuring a day corrects it in place. */
    suspend fun logMeasurement(bodySiteId: String, epochDay: Long, valueCm: Double) {
        bodyMeasurementDao.upsert(
            BodyMeasurement(
                id = "measurement-$bodySiteId-$epochDay",
                bodySiteId = bodySiteId,
                epochDay = epochDay,
                valueCm = valueCm,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurement) {
        bodyMeasurementDao.delete(measurement)
    }
}
