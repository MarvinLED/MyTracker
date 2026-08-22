package com.example.mytracker.measurement

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One measurement of one [BodySite] on one day, in cm. Deterministic id
 * ("measurement-$bodySiteId-$epochDay") + upsert makes logging idempotent, the same convention as
 * [com.example.mytracker.weight.BodyWeightEntry] — re-measuring the same spot on the same
 * day corrects the value instead of adding a second point to the chart.
 *
 * Tracked/logged data, never exported; the site definitions are the library half (see
 * [BodySiteLibraryExportProvider]). Deleting a site takes its measurements with it — the numbers are
 * meaningless without the spot they were taken at.
 */
@Entity(
    tableName = "body_measurements",
    foreignKeys = [
        ForeignKey(
            entity = BodySite::class,
            parentColumns = ["id"],
            childColumns = ["bodySiteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bodySiteId", "epochDay"], unique = true),
        Index("epochDay"),
    ],
)
data class BodyMeasurement(
    @PrimaryKey val id: String,
    val bodySiteId: String,
    val epochDay: Long,
    val valueCm: Double,
    val createdAt: Instant,
)
